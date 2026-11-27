package io.synkra.kubernetes;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.ResourceRequirements;
import io.fabric8.kubernetes.api.model.ResourceRequirementsBuilder;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.synkra.kubernetes.api.ObjetosPais;
import io.synkra.kubernetes.api.PlanPais;
import io.synkra.kubernetes.api.PlanPais.PluginPlan;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/** Arma en memoria los objetos de un país según U08. No usa cliente: no puede hablar con la API. */
final class ConstructorObjetosPais {

    static final String NOMBRE_CORE = "synkra-core";
    static final String NOMBRE_CONFIG = "synkra-core-config";
    static final String ETIQUETA_GESTOR = "app.kubernetes.io/managed-by";
    static final String GESTOR = "synkra";
    static final String ETIQUETA_NOMBRE = "app.kubernetes.io/name";

    static final String CONTENEDOR_CORE = "core";
    static final String CONTENEDOR_INIT = "descarga";
    /** Valor por defecto de {@code synkra.kubernetes.imagen-init}: la imagen que sube {@code make install}. */
    static final String IMAGEN_INIT_POR_DEFECTO = "localhost:5001/synkra-init-descarga:1.0.0";
    static final String POLITICA_PULL = "IfNotPresent";
    /** UID/GID fijos de los dos contenedores; las imágenes deben usar este usuario numérico. */
    static final long UID_CONTENEDORES = 10001L;
    static final long GID_CONTENEDORES = 10001L;
    /** Registro según Docker: {@code localhost}, o un host con punto, o un host con puerto. */
    private static final String REGISTRO =
            "(?:localhost(?::[0-9]+)?|[A-Za-z0-9-]+(?:\\.[A-Za-z0-9-]+)+(?::[0-9]+)?|[A-Za-z0-9-]+:[0-9]+)";
    /** Repositorio: uno o más componentes en minúsculas separados por {@code /}. */
    private static final String REPOSITORIO = "[a-z0-9]+(?:[._-][a-z0-9]+)*(?:/[a-z0-9]+(?:[._-][a-z0-9]+)*)*";
    private static final Pattern IMAGEN_INIT_VALIDA = Pattern.compile(
            REGISTRO + "/" + REPOSITORIO + "(?::[A-Za-z0-9_][A-Za-z0-9_.-]{0,127}|@sha256:[0-9a-f]{64})");
    private static final String TAG_PROHIBIDO = ":latest";
    static final String COMANDO_INIT = "/usr/local/bin/descargar.sh";
    static final String VAR_URL = "SYNKRA_URL";
    static final String URL_CATALOGO = "http://synkra-backend.synkra.svc:8080";

    static final String VOLUMEN_PLUGINS = "plugins";
    static final String RUTA_PLUGINS = "/plugins";
    static final String VOLUMEN_CONFIG = "config";
    static final String RUTA_CONFIG = "/config";

    static final String PUERTO_NOMBRE = "http";
    static final int PUERTO = 8080;
    static final String RUTA_READINESS = "/actuator/health/readiness";

    /**
     * Clave del ConfigMap con los plugins activos. Formato canónico (el que lee el core, U02-T04; C-81): una
     * <b>lista YAML en la raíz</b> de mapas con exactamente dos claves de texto, {@code nombre} y {@code version}
     * (entre comillas, para que {@code 1.0} no se lea como número), o {@code []} sin plugins. No hay mapa
     * envolvente {@code plugins:}.
     */
    static final String CLAVE_PLUGINS = "plugins-activos.yaml";
    static final String CLAVE_ARTEFACTOS = "artefactos.txt";
    /** Claves de cada entrada de {@link #CLAVE_PLUGINS}. */
    static final String CLAVE_NOMBRE = "nombre";
    static final String CLAVE_VERSION = "version";
    static final String SUFIJO_JAR = ".jar";

    /** Nombre del jar de un plugin: la clave que une una entrada de {@link #CLAVE_PLUGINS} con su línea de {@link #CLAVE_ARTEFACTOS}. */
    static String nombreJar(String nombre, String version) {
        return nombre + "-" + version + SUFIJO_JAR;
    }

    private static final ResourceRequirements RECURSOS_CORE = recursos("256Mi", "512Mi", "250m", "500m");
    private static final ResourceRequirements RECURSOS_INIT = recursos("32Mi", "64Mi", "50m", "100m");

    private ConstructorObjetosPais() {}

    /** Exige registro y tag o digest; rechaza {@code :latest} y las referencias sin tag. */
    static String exigirImagenInit(String imagen) {
        if (imagen == null || !IMAGEN_INIT_VALIDA.matcher(imagen).matches() || imagen.endsWith(TAG_PROHIBIDO)) {
            throw new IllegalArgumentException(
                    "synkra.kubernetes.imagen-init debe llevar registro (con punto, puerto o localhost) y tag o digest (nunca :latest): " + imagen);
        }
        return imagen;
    }

    static ObjetosPais construir(PlanPais plan, String namespace, String imagenInit) {
        List<PluginPlan> plugins = plan.plugins().stream()
                .sorted(Comparator.comparing(PluginPlan::nombre).thenComparing(PluginPlan::version))
                .toList();
        return new ObjetosPais(deployment(plan, namespace, imagenInit), configMap(plugins, namespace), service(namespace));
    }

    private static Map<String, String> etiquetaGestor() {
        return Map.of(ETIQUETA_GESTOR, GESTOR);
    }

    private static Map<String, String> selector() {
        return Map.of(ETIQUETA_NOMBRE, NOMBRE_CORE);
    }

    private static ResourceRequirements recursos(String memMin, String memMax, String cpuMin, String cpuMax) {
        return new ResourceRequirementsBuilder()
                .addToRequests("memory", new Quantity(memMin)).addToRequests("cpu", new Quantity(cpuMin))
                .addToLimits("memory", new Quantity(memMax)).addToLimits("cpu", new Quantity(cpuMax))
                .build();
    }

    private static ConfigMap configMap(List<PluginPlan> plugins, String namespace) {
        String activos = plugins.isEmpty()
                ? "[]\n"
                : plugins.stream()
                        .map(p -> "- " + CLAVE_NOMBRE + ": \"" + p.nombre() + "\"\n  " + CLAVE_VERSION + ": \"" + p.version() + "\"\n")
                        .collect(Collectors.joining());
        String artefactos = plugins.stream()
                .map(p -> p.sha256() + " " + nombreJar(p.nombre(), p.version()) + "\n")
                .collect(Collectors.joining());
        return new ConfigMapBuilder()
                .withNewMetadata().withName(NOMBRE_CONFIG).withNamespace(namespace).withLabels(etiquetaGestor()).endMetadata()
                .addToData(CLAVE_PLUGINS, activos)
                .addToData(CLAVE_ARTEFACTOS, artefactos)
                .build();
    }

    private static Service service(String namespace) {
        return new ServiceBuilder()
                .withNewMetadata().withName(NOMBRE_CORE).withNamespace(namespace).withLabels(etiquetaGestor()).endMetadata()
                .withNewSpec()
                .withType("ClusterIP")
                .withSelector(selector())
                .addNewPort().withName(PUERTO_NOMBRE).withPort(PUERTO)
                .withTargetPort(new IntOrString(PUERTO_NOMBRE)).withProtocol("TCP").endPort()
                .endSpec()
                .build();
    }

    private static Container init(String imagenInit) {
        return new ContainerBuilder()
                .withName(CONTENEDOR_INIT).withImage(imagenInit).withImagePullPolicy(POLITICA_PULL).withCommand(COMANDO_INIT)
                .addNewEnv().withName(VAR_URL).withValue(URL_CATALOGO).endEnv()
                .withResources(RECURSOS_INIT)
                .withNewSecurityContext().withRunAsNonRoot(true).withRunAsUser(UID_CONTENEDORES).withRunAsGroup(GID_CONTENEDORES)
                .withAllowPrivilegeEscalation(false).endSecurityContext()
                .addNewVolumeMount().withName(VOLUMEN_PLUGINS).withMountPath(RUTA_PLUGINS).endVolumeMount()
                .addNewVolumeMount().withName(VOLUMEN_CONFIG).withMountPath(RUTA_CONFIG).endVolumeMount()
                .build();
    }

    private static Container core(String imagen) {
        return new ContainerBuilder()
                .withName(CONTENEDOR_CORE).withImage(imagen)
                .addNewPort().withName(PUERTO_NOMBRE).withContainerPort(PUERTO).withProtocol("TCP").endPort()
                .withNewReadinessProbe().withNewHttpGet()
                .withPath(RUTA_READINESS).withPort(new IntOrString(PUERTO_NOMBRE)).endHttpGet().endReadinessProbe()
                .withResources(RECURSOS_CORE)
                .withNewSecurityContext().withRunAsNonRoot(true).withRunAsUser(UID_CONTENEDORES).withRunAsGroup(GID_CONTENEDORES)
                .withAllowPrivilegeEscalation(false).endSecurityContext()
                .addNewVolumeMount().withName(VOLUMEN_PLUGINS).withMountPath(RUTA_PLUGINS).endVolumeMount()
                .addNewVolumeMount().withName(VOLUMEN_CONFIG).withMountPath(RUTA_CONFIG).endVolumeMount()
                .build();
    }

    private static Deployment deployment(PlanPais plan, String namespace, String imagenInit) {
        return new DeploymentBuilder()
                .withNewMetadata().withName(NOMBRE_CORE).withNamespace(namespace).withLabels(etiquetaGestor()).endMetadata()
                .withNewSpec()
                .withReplicas(1)
                .withNewSelector().withMatchLabels(selector()).endSelector()
                .withNewTemplate()
                .withNewMetadata()
                .addToLabels(ETIQUETA_NOMBRE, NOMBRE_CORE).addToLabels(ETIQUETA_GESTOR, GESTOR)
                .endMetadata()
                .withNewSpec()
                .withAutomountServiceAccountToken(false)
                .withInitContainers(init(imagenInit))
                .withContainers(core(plan.imagenCore()))
                .addNewVolume().withName(VOLUMEN_PLUGINS).withNewEmptyDir().endEmptyDir().endVolume()
                .addNewVolume().withName(VOLUMEN_CONFIG).withNewConfigMap().withName(NOMBRE_CONFIG).endConfigMap().endVolume()
                .endSpec()
                .endTemplate()
                .endSpec()
                .build();
    }
}
