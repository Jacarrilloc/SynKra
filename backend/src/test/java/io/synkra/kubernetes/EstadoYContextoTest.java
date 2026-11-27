package io.synkra.kubernetes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.fabric8.kubernetes.api.model.ContainerStateBuilder;
import io.fabric8.kubernetes.api.model.ContainerStatus;
import io.fabric8.kubernetes.api.model.ContainerStatusBuilder;
import io.fabric8.kubernetes.api.model.Event;
import io.fabric8.kubernetes.api.model.EventBuilder;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;
import io.fabric8.mockwebserver.http.RecordedRequest;
import io.synkra.kubernetes.api.ClienteNoDisponibleException;
import io.synkra.kubernetes.api.DespliegueAusenteException;
import io.synkra.kubernetes.api.FaseDespliegue;
import io.synkra.kubernetes.api.NamespaceNoPermitidoException;
import io.fabric8.mockwebserver.Context;
import io.fabric8.mockwebserver.MockWebServer;
import io.fabric8.kubernetes.client.server.mock.KubernetesMixedDispatcher;
import io.fabric8.mockwebserver.ServerRequest;
import io.fabric8.mockwebserver.ServerResponse;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Queue;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * U05-T05: {@code estado} y {@code contextoFallo} contra el servidor simulado de Fabric8 en modo CRUD, con los objetos
 * sembrados con su {@code status} (no hay kubelet). Los logs se sirven con expectativas sobre la ruta exacta, que
 * incluye {@code tailLines=200}. Nunca se toca el kubeconfig real.
 */
class EstadoYContextoTest {

    private static final String NS = "synkra-pe";
    private static final String POD = "synkra-core-abc";
    private static final String RUTA_POD = "/api/v1/namespaces/" + NS + "/pods/";

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
        // Modo CRUD mixto: las expectativas (logs) tienen prioridad y el resto se guarda y se lee como en un clúster.
        var respuestas = new HashMap<ServerRequest, Queue<ServerResponse>>();
        servidor = new KubernetesMockServer(new Context(), new MockWebServer(), respuestas,
                new KubernetesMixedDispatcher(respuestas), false);
        servidor.init();
        cliente = servidor.createClient();
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

    /** Marca el final de la siembra: lo que se cuente después son solo las peticiones del adaptador. */
    private void terminaSiembra() throws Exception {
        leidas = 0;
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

    private void siembraDeployment(int deseadas, int listas) {
        Deployment d = new DeploymentBuilder()
                .withNewMetadata().withName("synkra-core").withNamespace(NS).endMetadata()
                .withNewSpec().withReplicas(deseadas).endSpec()
                .withNewStatus().withReplicas(deseadas).withReadyReplicas(listas).endStatus()
                .build();
        cliente.apps().deployments().inNamespace(NS).resource(d).create();
    }

    private Pod siembraPod(String nombre, ContainerStatus init, ContainerStatus core) {
        var estado = new io.fabric8.kubernetes.api.model.PodStatusBuilder().withPhase("Running");
        if (init != null) {
            estado.withInitContainerStatuses(init);
        }
        if (core != null) {
            estado.withContainerStatuses(core);
        }
        Pod p = new PodBuilder()
                .withNewMetadata().withName(nombre).withNamespace(NS)
                .addToLabels("app.kubernetes.io/name", "synkra-core").endMetadata()
                .withStatus(estado.build())
                .build();
        return cliente.pods().inNamespace(NS).resource(p).create();
    }

    private static ContainerStatus corriendo(String nombre, int reinicios) {
        return new ContainerStatusBuilder().withName(nombre).withReady(true).withRestartCount(reinicios)
                .withState(new ContainerStateBuilder().withNewRunning().endRunning().build()).build();
    }

    private static ContainerStatus esperando(String nombre, String razon, int reinicios) {
        return new ContainerStatusBuilder().withName(nombre).withReady(false).withRestartCount(reinicios)
                .withState(new ContainerStateBuilder().withNewWaiting().withReason(razon).endWaiting().build()).build();
    }

    private static ContainerStatus terminado(String nombre, int codigo) {
        return new ContainerStatusBuilder().withName(nombre).withReady(false).withRestartCount(0)
                .withState(new ContainerStateBuilder().withNewTerminated().withExitCode(codigo).withReason("Error")
                        .endTerminated().build()).build();
    }

    private void siembraEvento(String nombre, String pod, String tipo, String razon, String mensaje, String hora) {
        Event e = new EventBuilder()
                .withNewMetadata().withName(nombre).withNamespace(NS).endMetadata()
                .withNewInvolvedObject().withKind("Pod").withName(pod).withNamespace(NS).endInvolvedObject()
                .withType(tipo).withReason(razon).withMessage(mensaje).withLastTimestamp(hora)
                .build();
        cliente.v1().events().inNamespace(NS).resource(e).create();
    }

    /** Sirve el log; el del core pide {@code tailLines=200}, el del init se lee entero. */
    private void sirveLog(String pod, String contenedor, boolean previo, String cuerpo) {
        boolean core = contenedor.equals("core");
        servidor.expect().get()
                .withPath(RUTA_POD + pod + "/log?pretty=false&container=" + contenedor + (previo ? "&previous=true" : "")
                        + (core ? "&tailLines=200" : ""))
                .andReturn(200, cuerpo).always();
    }

    /** Un pod que aún no arrancó: la API responde 400 al pedir el log de un contenedor que no ha empezado. */
    private void logAunNoExiste(String pod) {
        for (String c : List.of("core", "descarga")) {
            servidor.expect().get().withPath(RUTA_POD + pod + "/log?pretty=false&container=" + c
                    + (c.equals("core") ? "&tailLines=200" : "")).andReturn(400, "container is waiting to start").always();
        }
        servidor.expect().get().withPath(RUTA_POD + pod + "/log?pretty=false&container=core&previous=true&tailLines=200")
                .andReturn(400, "previous terminated container not found").always();
    }

    @Test
    void podListoEsListoConUnaReplicaYCeroReinicios() throws Exception {
        siembraDeployment(1, 1);
        siembraPod(POD, terminado("descarga", 0), corriendo("core", 0));
        terminaSiembra();
        var e = adaptador.estado(NS);
        assertThat(e.fase()).isEqualTo(FaseDespliegue.LISTO);
        assertThat(e.replicasDeseadas()).isEqualTo(1);
        assertThat(e.replicasListas()).isEqualTo(1);
        assertThat(e.reinicios()).isZero();
        assertThat(e.razon()).isNull();
    }

    @Test
    void crashLoopBackOffEsFalladoConSusReiniciosYContextoConLasUltimas200Lineas() throws Exception {
        siembraDeployment(1, 0);
        siembraPod(POD, terminado("descarga", 0), esperando("core", "CrashLoopBackOff", 4));
        var log = IntStream.rangeClosed(801, 1000).mapToObj(i -> "linea " + i).collect(Collectors.joining("\n"));
        sirveLog(POD, "core", false, log);
        sirveLog(POD, "descarga", false, "descarga ok");
        siembraEvento("ev1", POD, "Warning", "BackOff", "Back-off restarting failed container", "2026-01-01T00:00:02Z");
        terminaSiembra();

        var e = adaptador.estado(NS);
        assertThat(e.fase()).isEqualTo(FaseDespliegue.FALLIDO);
        assertThat(e.razon()).isEqualTo("CrashLoopBackOff");
        assertThat(e.reinicios()).isEqualTo(4);
        assertThat(e.replicasListas()).isZero();

        var c = adaptador.contextoFallo(NS);
        var lineas = c.logCore().lines().toList();
        assertThat(lineas).hasSize(200);
        assertThat(lineas.get(0)).isEqualTo("linea 801");
        assertThat(lineas.get(199)).isEqualTo("linea 1000");
        assertThat(c.logInit()).isEqualTo("descarga ok");
        assertThat(c.eventos()).contains("Warning BackOff: Back-off restarting failed container");
        assertThat(peticiones()).anySatisfy(r -> assertThat(r.getPath()).contains("/log").contains("tailLines=200"));
    }

    @Test
    void sinLogActualSeTomaElDelIntentoAnterior() throws Exception {
        siembraDeployment(1, 0);
        siembraPod(POD, null, esperando("core", "CrashLoopBackOff", 2));
        sirveLog(POD, "core", false, "");
        sirveLog(POD, "core", true, "murio por X");
        terminaSiembra();
        assertThat(adaptador.contextoFallo(NS).logCore()).isEqualTo("murio por X");
    }

    @Test
    void initTerminadoConCodigoUnoEsInitErrorYSuLogEstaEnElContexto() throws Exception {
        siembraDeployment(1, 0);
        siembraPod(POD, terminado("descarga", 1), null);
        sirveLog(POD, "descarga", false, "fallo la descarga: 404");
        terminaSiembra();
        var e = adaptador.estado(NS);
        assertThat(e.fase()).isEqualTo(FaseDespliegue.FALLIDO);
        assertThat(e.razon()).isEqualTo("Init:Error");
        assertThat(adaptador.contextoFallo(NS).logInit()).isEqualTo("fallo la descarga: 404");
    }

    @Test
    void initEnCrashLoopBackOffEsInitCrashLoopBackOff() throws Exception {
        siembraDeployment(1, 0);
        siembraPod(POD, esperando("descarga", "CrashLoopBackOff", 3), null);
        terminaSiembra();
        var e = adaptador.estado(NS);
        assertThat(e.fase()).isEqualTo(FaseDespliegue.FALLIDO);
        assertThat(e.razon()).isEqualTo("Init:CrashLoopBackOff");
    }

    @Test
    void imagePullBackOffEsFallido() throws Exception {
        siembraDeployment(1, 0);
        siembraPod(POD, terminado("descarga", 0), esperando("core", "ImagePullBackOff", 0));
        terminaSiembra();
        assertThat(adaptador.estado(NS).razon()).isEqualTo("ImagePullBackOff");
    }

    @Test
    void podPendienteEsEnCursoYElContextoNoFallaConLogsVacios() throws Exception {
        siembraDeployment(1, 0);
        siembraPod(POD, esperando("descarga", "PodInitializing", 0), null);
        logAunNoExiste(POD);
        terminaSiembra();
        var e = adaptador.estado(NS);
        assertThat(e.fase()).isEqualTo(FaseDespliegue.EN_CURSO);
        assertThat(e.razon()).isNull();
        var c = adaptador.contextoFallo(NS);
        assertThat(c.logCore()).isEmpty();
        assertThat(c.logInit()).isEmpty();
        assertThat(c.eventos()).isEmpty();
    }

    @Test
    void deploymentAusenteLanzaExcepcionPropiaQueNombraElNamespace() throws Exception {
        terminaSiembra();
        assertThatThrownBy(() -> adaptador.estado(NS))
                .isInstanceOf(DespliegueAusenteException.class).hasMessageContaining(NS);
    }

    @Test
    void losEventosDeOtroPodDelMismoNamespaceNoAparecen() throws Exception {
        siembraDeployment(1, 0);
        siembraPod(POD, null, esperando("core", "CrashLoopBackOff", 1));
        siembraEvento("ev1", POD, "Warning", "BackOff", "mio", "2026-01-01T00:00:02Z");
        siembraEvento("ev0", POD, "Normal", "Pulled", "antes", "2026-01-01T00:00:01Z");
        siembraEvento("ev2", "otro-pod", "Warning", "Failed", "ajeno", "2026-01-01T00:00:03Z");
        terminaSiembra();
        var eventos = adaptador.contextoFallo(NS).eventos();
        assertThat(eventos).contains("mio").doesNotContain("ajeno");
        assertThat(eventos.indexOf("antes")).isLessThan(eventos.indexOf("mio"));
    }

    @Test
    void namespacesProhibidosSeRechazanSinPeticiones() throws Exception {
        terminaSiembra();
        for (String ns : List.of("default", "kube-system", "synkra")) {
            assertThatThrownBy(() -> adaptador.estado(ns)).isInstanceOf(NamespaceNoPermitidoException.class);
            assertThatThrownBy(() -> adaptador.contextoFallo(ns)).isInstanceOf(NamespaceNoPermitidoException.class);
        }
        assertThat(peticiones()).isEmpty();
    }

    @Test
    void sinClienteHayErrorPropioYCeroPeticiones() throws Exception {
        terminaSiembra();
        var sin = new AdaptadorFabric8(null);
        assertThatThrownBy(() -> sin.estado(NS)).isInstanceOf(ClienteNoDisponibleException.class);
        assertThatThrownBy(() -> sin.contextoFallo(NS)).isInstanceOf(ClienteNoDisponibleException.class);
        assertThat(peticiones()).isEmpty();
    }

    @Test
    void todasLasPeticionesSonGetYVanAlNamespaceDelPais() throws Exception {
        siembraDeployment(1, 0);
        siembraPod(POD, terminado("descarga", 1), esperando("core", "CrashLoopBackOff", 4));
        sirveLog(POD, "core", false, "x");
        sirveLog(POD, "descarga", false, "y");
        siembraEvento("ev1", POD, "Warning", "BackOff", "m", "2026-01-01T00:00:02Z");
        terminaSiembra();
        adaptador.estado(NS);
        adaptador.contextoFallo(NS);
        var reqs = peticiones();
        assertThat(reqs).isNotEmpty();
        assertThat(reqs).allSatisfy(r -> {
            assertThat(r.getMethod()).isEqualTo("GET");
            assertThat(r.getPath()).contains("/namespaces/" + NS + "/");
        });
    }
}
