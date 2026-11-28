package io.synkra.kubernetes;

import io.fabric8.kubernetes.api.model.ContainerStateBuilder;
import io.fabric8.kubernetes.api.model.ContainerStatus;
import io.fabric8.kubernetes.api.model.ContainerStatusBuilder;
import io.fabric8.kubernetes.api.model.EventBuilder;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.api.model.PodStatusBuilder;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.kubernetes.client.server.mock.KubernetesMixedDispatcher;
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;
import io.fabric8.mockwebserver.Context;
import io.fabric8.mockwebserver.MockWebServer;
import io.fabric8.mockwebserver.ServerRequest;
import io.fabric8.mockwebserver.ServerResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Queue;

/**
 * Ayudante de U07-E para {@code EstadoRolloutTest} y {@code ContextoEventosTest}: el servidor simulado de Fabric8 en
 * modo CRUD con un {@link AdaptadorFabric8} encima, y constructores de Deployments, pods y eventos de {@code synkra-pe}.
 * Fija una casa falsa mientras vive (nunca se toca el kubeconfig real).
 */
final class LecturasDePrueba implements AutoCloseable {

    static final String NS = "synkra-pe";
    static final String CORE = ConstructorObjetosPais.NOMBRE_CORE;

    final KubernetesMockServer servidor;
    final KubernetesClient cliente;
    final AdaptadorFabric8 adaptador;
    private final String propiedadCasa;

    LecturasDePrueba(Path casaFalsa) {
        propiedadCasa = System.getProperty("user.home");
        System.setProperty("user.home", casaFalsa.toString());
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

    /**
     * Campos nulos = ausentes del {@code status} (cuentan como cumplidos). El simulado fija {@code metadata.generation}
     * en 1 al crear: {@link #generacion()} lo lee, para que las pruebas no lo supongan.
     */
    void deployment(int deseadas, Integer replicas, Integer listas, Integer actualizadas, Long observada) {
        var d = new DeploymentBuilder()
                .withNewMetadata().withName(CORE).withNamespace(NS).endMetadata()
                .withNewSpec().withReplicas(deseadas).endSpec()
                .withNewStatus().withReplicas(replicas).withReadyReplicas(listas).withUpdatedReplicas(actualizadas)
                .withObservedGeneration(observada).endStatus().build();
        cliente.apps().deployments().inNamespace(NS).resource(d).create();
    }

    long generacion() {
        return cliente.apps().deployments().inNamespace(NS).withName(CORE).get().getMetadata().getGeneration();
    }

    void podListo(String nombre) {
        pod(nombre, "Running", corriendo(), null);
    }

    void podFalla(String nombre, int reinicios) {
        pod(nombre, "Running", new ContainerStatusBuilder().withName(ConstructorObjetosPais.CONTENEDOR_CORE)
                .withReady(false).withRestartCount(reinicios).withState(new ContainerStateBuilder().withNewWaiting()
                        .withReason("CrashLoopBackOff").endWaiting().build()).build(), null);
    }

    void podPendiente(String nombre) {
        pod(nombre, "Pending", null, null);
    }

    private static ContainerStatus corriendo() {
        return new ContainerStatusBuilder().withName(ConstructorObjetosPais.CONTENEDOR_CORE).withReady(true)
                .withRestartCount(0).withState(new ContainerStateBuilder().withNewRunning().endRunning().build()).build();
    }

    private void pod(String nombre, String fase, ContainerStatus core, ContainerStatus init) {
        var estado = new PodStatusBuilder().withPhase(fase);
        if (core != null) {
            estado.withContainerStatuses(core);
        }
        if (init != null) {
            estado.withInitContainerStatuses(init);
        }
        var p = new PodBuilder().withNewMetadata().withName(nombre).withNamespace(NS)
                .addToLabels(ConstructorObjetosPais.ETIQUETA_NOMBRE, CORE).endMetadata().withStatus(estado.build()).build();
        cliente.pods().inNamespace(NS).resource(p).create();
    }

    /** Evento de un objeto de {@code kind}/{@code objeto}; {@code hora} va en {@code lastTimestamp}. */
    void evento(String nombre, String kind, String objeto, String tipo, String razon, String mensaje, String hora) {
        var e = new EventBuilder()
                .withNewMetadata().withName(nombre).withNamespace(NS).endMetadata()
                .withNewInvolvedObject().withKind(kind).withName(objeto).withNamespace(NS).endInvolvedObject()
                .withType(tipo).withReason(razon).withMessage(mensaje).withLastTimestamp(hora).build();
        cliente.v1().events().inNamespace(NS).resource(e).create();
    }

    @Override
    public void close() {
        cliente.close();
        servidor.destroy();
        if (propiedadCasa == null) {
            System.clearProperty("user.home");
        } else {
            System.setProperty("user.home", propiedadCasa);
        }
    }
}
