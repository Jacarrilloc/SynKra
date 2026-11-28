package io.synkra.kubernetes;

import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.kubernetes.client.server.mock.KubernetesMixedDispatcher;
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;
import io.fabric8.mockwebserver.Context;
import io.fabric8.mockwebserver.MockWebServer;
import io.fabric8.mockwebserver.ServerRequest;
import io.fabric8.mockwebserver.dsl.HttpMethod;
import io.fabric8.mockwebserver.internal.SimpleRequest;
import io.fabric8.mockwebserver.ServerResponse;
import io.fabric8.mockwebserver.http.Headers;
import io.fabric8.mockwebserver.http.RecordedRequest;
import io.fabric8.mockwebserver.utils.BodyProvider;
import io.fabric8.mockwebserver.utils.ResponseProvider;
import io.fabric8.mockwebserver.utils.ResponseProviders;
import io.fabric8.kubernetes.client.utils.Serialization;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import io.fabric8.kubernetes.api.model.ContainerStateBuilder;
import io.fabric8.kubernetes.api.model.ContainerStatusBuilder;
import io.fabric8.kubernetes.api.model.EventBuilder;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.api.model.PodStatusBuilder;
import io.synkra.kubernetes.api.AdaptadorKubernetes;
import io.synkra.kubernetes.api.PlanPais;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.TimeUnit;

/**
 * U07-B: el servidor simulado de Fabric8 (modo CRUD) con un {@link AdaptadorKubernetes} real encima, para que las
 * pruebas de otros módulos lean lo que "corre" sin un clúster. Es de este paquete porque {@code AdaptadorFabric8} es
 * privado de paquete; su API pública no menciona ningún tipo de Fabric8. Fija una casa y un kubeconfig falsos mientras
 * vive (nunca se toca el kubeconfig real) y los restaura al cerrar.
 */
public final class SimuladorKubernetes implements AutoCloseable {

    private final KubernetesMockServer servidor;
    private final KubernetesClient cliente;
    private final AdaptadorFabric8 adaptador;
    private final Path casa;
    private final String propiedadCasa;
    private final String propiedadKubeconfig;
    private int leidas;
    private final Map<ServerRequest, Queue<ServerResponse>> respuestas;

    public SimuladorKubernetes() {
        try {
            casa = Files.createTempDirectory("simulador-k8s");
        } catch (java.io.IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
        propiedadCasa = System.getProperty("user.home");
        propiedadKubeconfig = System.getProperty("kubeconfig");
        System.setProperty("user.home", casa.toString());
        System.setProperty("kubeconfig", casa.resolve("kubeconfig-falso").toString());
        var respuestas = new HashMap<ServerRequest, Queue<ServerResponse>>();
        this.respuestas = respuestas;
        servidor = new KubernetesMockServer(new Context(), new MockWebServer(), respuestas,
                new KubernetesMixedDispatcher(respuestas), false);
        servidor.init();
        try (var base = servidor.createClient()) {
            cliente = new KubernetesClientBuilder().withConfig(
                    new ConfigBuilder(base.getConfiguration()).withRequestRetryBackoffLimit(0).build()).build();
        }
        adaptador = new AdaptadorFabric8(cliente);
    }

    /** Un adaptador real, hablando con el servidor simulado. */
    public AdaptadorKubernetes adaptador() {
        return adaptador;
    }

    /** Deployment y ConfigMap del país, tal como los arma {@code construir}. */
    public void siembra(PlanPais plan) {
        var ns = "synkra-" + plan.pais();
        var o = adaptador.construir(plan);
        cliente.apps().deployments().inNamespace(ns).resource(o.deployment()).create();
        cliente.configMaps().inNamespace(ns).resource(o.configMap()).create();
        drena();
    }

    /** Solo el Deployment (el país queda inconsistente para {@code leerPais}). */
    public void siembraSoloDeployment(PlanPais plan) {
        var o = adaptador.construir(plan);
        cliente.apps().deployments().inNamespace("synkra-" + plan.pais()).resource(o.deployment()).create();
        drena();
    }

    /** El {@code GET} del Deployment de ese país responde 500. */
    public void fallaLecturas(String pais) {
        // Una expectativa registrada antes (aceptaAplicar) se queda con el turno: se quita la de ese GET para que esta mande.
        respuestas.remove(new SimpleRequest(HttpMethod.GET,
                "/apis/apps/v1/namespaces/synkra-" + pais + "/deployments/" + ConstructorObjetosPais.NOMBRE_CORE));
        servidor.expect().get()
                .withPath("/apis/apps/v1/namespaces/synkra-" + pais + "/deployments/" + ConstructorObjetosPais.NOMBRE_CORE)
                .andReturn(500, "{\"kind\":\"Status\",\"code\":500}").always();
    }

    /**
     * El modo CRUD del simulado no entiende el server-side apply (responde 404 o 415 al {@code PATCH}). Esto registra
     * para el país los tres {@code PATCH} de {@code aplicar}: responden 200 y guardan lo aplicado, y el {@code GET} de
     * cada objeto lo devuelve (404 mientras no se haya aplicado), así que {@code leerPais} lee lo que se aplicó.
     */
    public void aceptaAplicar(String pais) {
        for (var ruta : rutasDeAplicar(pais)) {
            aceptaAplicarEn(ruta);
        }
    }

    /**
     * El {@code PATCH} del Deployment {@code synkra-core} de ese país (el de aplicar) responde 500 siempre. Los de
     * ConfigMap y Service se aceptan como en {@link #aceptaAplicar(String)}: así falla el último de los tres.
     */
    public void fallaAplicar(String pais) {
        var rutas = rutasDeAplicar(pais);
        aceptaAplicarEn(rutas.get(0));
        aceptaAplicarEn(rutas.get(1));
        servidor.expect().patch().withPath(rutas.get(2) + CONSULTA_APLICAR)
                .andReturn(500, "{\"kind\":\"Status\",\"code\":500}").always();
    }


    /** U07-E: lo que sirven los logs y el evento de {@link #marcaFallo(String, String)}. */
    public static final String LOG_CORE = "core: no arranca\n";
    public static final String LOG_INIT = "descarga: plugins listos\n";
    public static final String EVENTO = "Warning BackOff: Back-off restarting failed container";

    /**
     * El rollout terminó: {@code replicas}, {@code readyReplicas} y {@code updatedReplicas} del Deployment iguales a
     * {@code spec.replicas} (o 1), {@code observedGeneration} igual a {@code metadata.generation}; los pods del país
     * se reemplazan por uno corriendo y listo. Si el país se aplicó con {@link #aceptaAplicar(String)} el estado se
     * pone en lo aplicado (es lo que sirve el {@code GET}); si no, en el Deployment sembrado. En modo CRUD el
     * {@code status} puede sobrevivir a un {@code PATCH} posterior: no se afirma {@code EN_CURSO} después de esto.
     */
    public void marcaListo(String pais) {
        borraPods(pais);
        ponEstadoDeployment(pais, null, null, null);
        siembraPodListo(pais, "synkra-core-listo");
        drena();
    }

    /** Pod {@code synkra-core-fallo} con 3 reinicios, su evento y sus logs; {@code razon}: {@code CrashLoopBackOff} o {@code Init:Error}. */
    public void marcaFallo(String pais, String razon) {
        siembraPodFallo(pais, "synkra-core-fallo", razon);
        drena();
    }

    /** Rollout con un pod viejo listo y uno nuevo en {@code CrashLoopBackOff}: 1 deseada, 2 réplicas, 1 lista, 1 actualizada. */
    public void marcaRolloutConFallo(String pais) {
        borraPods(pais);
        ponEstadoDeployment(pais, 2, 1, 1);
        siembraPodListo(pais, "synkra-core-viejo");
        siembraPodFallo(pais, "synkra-core-nuevo", "CrashLoopBackOff");
        drena();
    }

    private void borraPods(String pais) {
        var ns = "synkra-" + pais;
        for (var p : cliente.pods().inNamespace(ns).list().getItems()) {
            cliente.pods().inNamespace(ns).withName(p.getMetadata().getName()).delete();
        }
    }

    @SuppressWarnings("unchecked")
    private void ponEstadoDeployment(String pais, Integer replicas, Integer listas, Integer actualizadas) {
        var ns = "synkra-" + pais;
        var ruta = rutasDeAplicar(pais).get(2);
        if (aplicados.containsKey(ruta)) {
            var d = (Map<String, Object>) Serialization.unmarshal(aplicados.get(ruta), Map.class);
            var spec = (Map<String, Object>) d.getOrDefault("spec", Map.of());
            var meta = (Map<String, Object>) d.getOrDefault("metadata", Map.of());
            int deseadas = spec.get("replicas") instanceof Number n ? n.intValue() : 1;
            var status = new HashMap<String, Object>();
            status.put("replicas", replicas != null ? replicas : deseadas);
            status.put("readyReplicas", listas != null ? listas : deseadas);
            status.put("updatedReplicas", actualizadas != null ? actualizadas : deseadas);
            if (meta.get("generation") != null) {
                status.put("observedGeneration", meta.get("generation"));
            }
            d.put("status", status);
            aplicados.put(ruta, Serialization.asJson(d));
            return;
        }
        var d = cliente.apps().deployments().inNamespace(ns).withName(ConstructorObjetosPais.NOMBRE_CORE).get();
        if (d == null) {
            throw new IllegalStateException("No hay Deployment sembrado ni aplicado en " + ns);
        }
        int deseadas = d.getSpec() != null && d.getSpec().getReplicas() != null ? d.getSpec().getReplicas() : 1;
        var generacion = d.getMetadata().getGeneration();
        var status = new io.fabric8.kubernetes.api.model.apps.DeploymentStatusBuilder()
                .withReplicas(replicas != null ? replicas : deseadas)
                .withReadyReplicas(listas != null ? listas : deseadas)
                .withUpdatedReplicas(actualizadas != null ? actualizadas : deseadas);
        if (generacion != null) {
            status.withObservedGeneration(generacion);
        }
        d.setStatus(status.build());
        d.getMetadata().setResourceVersion(null);
        cliente.apps().deployments().inNamespace(ns).resource(d).update();
    }

    private void siembraPodListo(String pais, String nombre) {
        var estado = new PodStatusBuilder().withPhase("Running")
                .withInitContainerStatuses(new ContainerStatusBuilder().withName(ConstructorObjetosPais.CONTENEDOR_INIT)
                        .withReady(true).withRestartCount(0).withState(new ContainerStateBuilder().withNewTerminated()
                                .withExitCode(0).endTerminated().build()).build())
                .withContainerStatuses(new ContainerStatusBuilder().withName(ConstructorObjetosPais.CONTENEDOR_CORE)
                        .withReady(true).withRestartCount(0)
                        .withState(new ContainerStateBuilder().withNewRunning().endRunning().build()).build()).build();
        crea(pais, nombre, estado);
        sinLogs(pais, nombre);
    }

    /** Un pod listo no tiene nada que contar: la API responde 400 a sus logs (el simulado, sin esto, contesta otra cosa). */
    private void sinLogs(String pais, String nombre) {
        var ruta = "/api/v1/namespaces/synkra-" + pais + "/pods/" + nombre + "/log?pretty=false&container=";
        for (var contenedor : List.of(ConstructorObjetosPais.CONTENEDOR_INIT,
                ConstructorObjetosPais.CONTENEDOR_CORE + "&tailLines=200",
                ConstructorObjetosPais.CONTENEDOR_CORE + "&previous=true&tailLines=200")) {
            servidor.expect().get().withPath(ruta + contenedor).andReturn(400, "sin log").always();
        }
    }

    private void siembraPodFallo(String pais, String nombre, String razon) {
        var ns = "synkra-" + pais;
        boolean init = razon.equals("Init:Error");
        if (!init && !razon.equals("CrashLoopBackOff")) {
            throw new IllegalArgumentException("razón no soportada: " + razon);
        }
        var estado = new PodStatusBuilder().withPhase("Running");
        if (init) {
            estado.withInitContainerStatuses(new ContainerStatusBuilder().withName(ConstructorObjetosPais.CONTENEDOR_INIT)
                    .withReady(false).withRestartCount(3).withState(new ContainerStateBuilder().withNewTerminated()
                            .withExitCode(1).withReason("Error").endTerminated().build()).build());
        } else {
            estado.withContainerStatuses(new ContainerStatusBuilder().withName(ConstructorObjetosPais.CONTENEDOR_CORE)
                    .withReady(false).withRestartCount(3).withState(new ContainerStateBuilder().withNewWaiting()
                            .withReason("CrashLoopBackOff").endWaiting().build()).build());
        }
        crea(pais, nombre, estado.build());
        var e = new EventBuilder()
                .withNewMetadata().withName("ev-" + nombre).withNamespace(ns).endMetadata()
                .withNewInvolvedObject().withKind("Pod").withName(nombre).withNamespace(ns).endInvolvedObject()
                .withType("Warning").withReason("BackOff").withMessage("Back-off restarting failed container")
                .withLastTimestamp("2026-01-01T00:00:02Z").build();
        cliente.v1().events().inNamespace(ns).resource(e).create();
        var ruta = "/api/v1/namespaces/" + ns + "/pods/" + nombre + "/log?pretty=false&container=";
        servidor.expect().get().withPath(ruta + ConstructorObjetosPais.CONTENEDOR_INIT).andReturn(200, LOG_INIT).always();
        if (!init) {
            servidor.expect().get().withPath(ruta + ConstructorObjetosPais.CONTENEDOR_CORE + "&tailLines=200")
                    .andReturn(200, LOG_CORE).always();
        } else {
            // El core nunca arrancó: su log (y el del intento anterior) no existen.
            servidor.expect().get().withPath(ruta + ConstructorObjetosPais.CONTENEDOR_CORE + "&tailLines=200")
                    .andReturn(400, "container is waiting to start").always();
            servidor.expect().get().withPath(ruta + ConstructorObjetosPais.CONTENEDOR_CORE + "&previous=true&tailLines=200")
                    .andReturn(400, "previous terminated container not found").always();
        }
    }

    private void crea(String pais, String nombre, io.fabric8.kubernetes.api.model.PodStatus estado) {
        var ns = "synkra-" + pais;
        var p = new PodBuilder().withNewMetadata().withName(nombre).withNamespace(ns)
                .addToLabels(ConstructorObjetosPais.ETIQUETA_NOMBRE, ConstructorObjetosPais.NOMBRE_CORE).endMetadata()
                .withStatus(estado).build();
        cliente.pods().inNamespace(ns).resource(p).create();
    }

    private static final String CONSULTA_APLICAR = "?fieldManager=" + ConstructorObjetosPais.GESTOR + "&force=true";
    private final Map<String, String> aplicados = new ConcurrentHashMap<>();

    /** ConfigMap, Service y Deployment del país, en el orden en que {@code aplicar} los envía. */
    private static List<String> rutasDeAplicar(String pais) {
        var ns = "/namespaces/synkra-" + pais;
        return List.of("/api/v1" + ns + "/configmaps/" + ConstructorObjetosPais.NOMBRE_CONFIG,
                "/api/v1" + ns + "/services/" + ConstructorObjetosPais.NOMBRE_CORE,
                "/apis/apps/v1" + ns + "/deployments/" + ConstructorObjetosPais.NOMBRE_CORE);
    }

    private void aceptaAplicarEn(String ruta) {
        servidor.expect().patch().withPath(ruta + CONSULTA_APLICAR).andReply(respuesta(200, peticion -> {
            var json = Serialization.asJson(Serialization.unmarshal(peticion.getUtf8Body(), Map.class));
            aplicados.put(ruta, json);
            return json;
        })).always();
        servidor.expect().get().withPath(ruta).andReply(new ResponseProvider<Object>() {
            private Headers cabeceras = new Headers.Builder().build();

            @Override
            public int getStatusCode(RecordedRequest peticion) {
                return aplicados.containsKey(ruta) ? 200 : 404;
            }

            @Override
            public Object getBody(RecordedRequest peticion) {
                return aplicados.getOrDefault(ruta, "{\"kind\":\"Status\",\"code\":404}");
            }

            @Override
            public Headers getHeaders() {
                return cabeceras;
            }

            @Override
            public void setHeaders(Headers nuevas) {
                cabeceras = nuevas;
            }
        }).always();
    }

    private static ResponseProvider<Object> respuesta(int estado, BodyProvider<Object> cuerpo) {
        return ResponseProviders.of(estado, cuerpo);
    }

    /** {@code "MÉTODO ruta"} de lo recibido desde la llamada anterior (la siembra no cuenta). */
    public List<String> nuevasPeticiones() {
        var r = new ArrayList<String>();
        try {
            while (leidas < servidor.getRequestCount()) {
                var p = servidor.takeRequest(5, TimeUnit.SECONDS);
                leidas++;
                r.add(p.getMethod() + " " + p.getPath());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
        return r;
    }

    private void drena() {
        nuevasPeticiones();
    }

    @Override
    public void close() {
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
}
