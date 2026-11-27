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
import io.synkra.kubernetes.api.LecturaFallidaException;
import io.fabric8.kubernetes.client.KubernetesClientException;
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
    private static final String CORE = ConstructorObjetosPais.NOMBRE_CORE;
    private static final String CONT_CORE = ConstructorObjetosPais.CONTENEDOR_CORE;
    private static final String CONT_INIT = ConstructorObjetosPais.CONTENEDOR_INIT;
    private static final String TAIL = "&tailLines=" + LectorPais.LINEAS_LOG_CORE;

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
        // Sin reintentos: un 5xx sube de inmediato en lugar de esperar los backoffs del cliente.
        try (var base = servidor.createClient()) {
            cliente = new io.fabric8.kubernetes.client.KubernetesClientBuilder().withConfig(
                    new io.fabric8.kubernetes.client.ConfigBuilder(base.getConfiguration())
                            .withRequestRetryBackoffLimit(0).build()).build();
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
                .withNewMetadata().withName(CORE).withNamespace(NS).endMetadata()
                .withNewSpec().withReplicas(deseadas).endSpec()
                .withNewStatus().withReplicas(deseadas).withReadyReplicas(listas).endStatus()
                .build();
        cliente.apps().deployments().inNamespace(NS).resource(d).create();
    }

    /** Un Deployment sin {@code spec.replicas}: Kubernetes lo toma como una réplica. */
    private void siembraDeploymentSinReplicas(int listas) {
        Deployment d = new DeploymentBuilder()
                .withNewMetadata().withName(CORE).withNamespace(NS).endMetadata()
                .withNewSpec().endSpec()
                .withNewStatus().withReadyReplicas(listas).endStatus()
                .build();
        cliente.apps().deployments().inNamespace(NS).resource(d).create();
    }

    private Pod siembraPod(String nombre, ContainerStatus init, ContainerStatus core) {
        return siembraPod(nombre, "Running", init, core);
    }

    private Pod siembraPod(String nombre, String fase, ContainerStatus init, ContainerStatus core) {
        var estado = new io.fabric8.kubernetes.api.model.PodStatusBuilder().withPhase(fase);
        if (init != null) {
            estado.withInitContainerStatuses(init);
        }
        if (core != null) {
            estado.withContainerStatuses(core);
        }
        Pod p = new PodBuilder()
                .withNewMetadata().withName(nombre).withNamespace(NS)
                .addToLabels(ConstructorObjetosPais.ETIQUETA_NOMBRE, CORE).endMetadata()
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
        siembraEvento(nombre, pod, tipo, razon, mensaje, hora, null);
    }

    /** {@code hora} va en {@code lastTimestamp}; {@code horaEventTime}, si existe, en {@code eventTime} (microsegundos). */
    private void siembraEvento(String nombre, String pod, String tipo, String razon, String mensaje, String hora,
            String horaEventTime) {
        Event e = new EventBuilder()
                .withNewMetadata().withName(nombre).withNamespace(NS).endMetadata()
                .withNewInvolvedObject().withKind("Pod").withName(pod).withNamespace(NS).endInvolvedObject()
                .withType(tipo).withReason(razon).withMessage(mensaje).withLastTimestamp(hora)
                .build();
        if (horaEventTime != null) {
            e.setEventTime(new io.fabric8.kubernetes.api.model.MicroTimeBuilder().withTime(horaEventTime).build());
        }
        cliente.v1().events().inNamespace(NS).resource(e).create();
    }

    /** Sirve el log; el del core pide {@code tailLines=200}, el del init se lee entero. */
    private void sirveLog(String pod, String contenedor, boolean previo, String cuerpo) {
        boolean core = contenedor.equals(CONT_CORE);
        servidor.expect().get()
                .withPath(RUTA_POD + pod + "/log?pretty=false&container=" + contenedor + (previo ? "&previous=true" : "")
                        + (core ? TAIL : ""))
                .andReturn(200, cuerpo).always();
    }

    /** Un pod que aún no arrancó: la API responde 400 al pedir el log de un contenedor que no ha empezado. */
    private void logAunNoExiste(String pod) {
        for (String c : List.of(CONT_CORE, CONT_INIT)) {
            servidor.expect().get().withPath(RUTA_POD + pod + "/log?pretty=false&container=" + c
                    + (c.equals(CONT_CORE) ? TAIL : "")).andReturn(400, "container is waiting to start").always();
        }
        servidor.expect().get().withPath(RUTA_POD + pod + "/log?pretty=false&container=" + CONT_CORE + "&previous=true" + TAIL)
                .andReturn(400, "previous terminated container not found").always();
    }

    @Test
    void podListoEsListoConUnaReplicaYCeroReinicios() throws Exception {
        siembraDeployment(1, 1);
        siembraPod(POD, terminado(CONT_INIT, 0), corriendo(CONT_CORE, 0));
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
        siembraPod(POD, terminado(CONT_INIT, 0), esperando(CONT_CORE, "CrashLoopBackOff", 4));
        var log = IntStream.rangeClosed(801, 1000).mapToObj(i -> "linea " + i).collect(Collectors.joining("\n"));
        sirveLog(POD, CONT_CORE, false, log);
        sirveLog(POD, CONT_INIT, false, "descarga ok");
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
        assertThat(peticiones()).anySatisfy(r -> assertThat(r.getPath()).contains("/log").contains(TAIL.substring(1)));
    }

    @Test
    void sinLogActualSeTomaElDelIntentoAnterior() throws Exception {
        siembraDeployment(1, 0);
        siembraPod(POD, null, esperando(CONT_CORE, "CrashLoopBackOff", 2));
        sirveLog(POD, CONT_CORE, false, "");
        sirveLog(POD, CONT_CORE, true, "murio por X");
        terminaSiembra();
        assertThat(adaptador.contextoFallo(NS).logCore()).isEqualTo("murio por X");
    }

    @Test
    void initTerminadoConCodigoUnoEsInitErrorYSuLogEstaEnElContexto() throws Exception {
        siembraDeployment(1, 0);
        siembraPod(POD, terminado(CONT_INIT, 1), null);
        sirveLog(POD, CONT_INIT, false, "fallo la descarga: 404");
        terminaSiembra();
        var e = adaptador.estado(NS);
        assertThat(e.fase()).isEqualTo(FaseDespliegue.FALLIDO);
        assertThat(e.razon()).isEqualTo("Init:Error");
        assertThat(adaptador.contextoFallo(NS).logInit()).isEqualTo("fallo la descarga: 404");
    }

    @Test
    void initEnCrashLoopBackOffEsInitCrashLoopBackOff() throws Exception {
        siembraDeployment(1, 0);
        siembraPod(POD, esperando(CONT_INIT, "CrashLoopBackOff", 3), null);
        terminaSiembra();
        var e = adaptador.estado(NS);
        assertThat(e.fase()).isEqualTo(FaseDespliegue.FALLIDO);
        assertThat(e.razon()).isEqualTo("Init:CrashLoopBackOff");
    }

    @Test
    void imagePullBackOffEsFallido() throws Exception {
        siembraDeployment(1, 0);
        siembraPod(POD, terminado(CONT_INIT, 0), esperando(CONT_CORE, "ImagePullBackOff", 0));
        terminaSiembra();
        assertThat(adaptador.estado(NS).razon()).isEqualTo("ImagePullBackOff");
    }

    @Test
    void podPendienteEsEnCursoYElContextoNoFallaConLogsVacios() throws Exception {
        siembraDeployment(1, 0);
        // Un Pending real: aún sin programar, sin estados de contenedor.
        siembraPod(POD, "Pending", null, null);
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
                .isInstanceOf(DespliegueAusenteException.class).hasMessageContaining(NS).hasMessageContaining(CORE);
    }

    @Test
    void losEventosDeOtroPodDelMismoNamespaceNoAparecen() throws Exception {
        siembraDeployment(1, 0);
        siembraPod(POD, null, esperando(CONT_CORE, "CrashLoopBackOff", 1));
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
        siembraPod(POD, terminado(CONT_INIT, 1), esperando(CONT_CORE, "CrashLoopBackOff", 4));
        sirveLog(POD, CONT_CORE, false, "x");
        sirveLog(POD, CONT_INIT, false, "y");
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

    @Test
    void deploymentSinReplicasEspecificadasCuentaUnaReplicaPorDefecto() throws Exception {
        siembraDeploymentSinReplicas(1);
        terminaSiembra();
        var e = adaptador.estado(NS);
        assertThat(e.replicasDeseadas()).isEqualTo(LectorPais.REPLICAS_POR_DEFECTO_K8S).isEqualTo(1);
        assertThat(e.fase()).isEqualTo(FaseDespliegue.LISTO);
    }

    private void falla(String ruta, int codigo) {
        servidor.expect().get().withPath(ruta).andReturn(codigo, "{\"kind\":\"Status\",\"code\":" + codigo + "}").always();
    }

    private static final String RUTA_DEPLOYMENT = "/apis/apps/v1/namespaces/" + NS + "/deployments/" + CORE;
    private static final String RUTA_PODS = "/api/v1/namespaces/" + NS + "/pods?labelSelector="
            + "app.kubernetes.io%2Fname%3Dsynkra-core";
    private static final String RUTA_EVENTOS = "/api/v1/namespaces/" + NS + "/events";

    @Test
    void unaFalloDeLaApiAlLeerElDeploymentSaleComoLecturaFallidaConNamespaceYCausa() throws Exception {
        falla(RUTA_DEPLOYMENT, 500);
        terminaSiembra();
        assertThatThrownBy(() -> adaptador.estado(NS))
                .isInstanceOf(LecturaFallidaException.class)
                .hasMessageContaining("Deployment").hasMessageContaining(NS)
                .hasCauseInstanceOf(KubernetesClientException.class);
    }

    @Test
    void unFalloAlListarLosPodsNombraLosPodsYNoElDeployment() throws Exception {
        siembraDeployment(1, 0);
        falla(RUTA_PODS, 500);
        terminaSiembra();
        assertThatThrownBy(() -> adaptador.estado(NS))
                .isInstanceOf(LecturaFallidaException.class)
                .hasMessageContaining("pods").hasMessageContaining(NS).hasMessageNotContaining("Deployment")
                .hasCauseInstanceOf(KubernetesClientException.class);
        assertThatThrownBy(() -> adaptador.contextoFallo(NS))
                .isInstanceOf(LecturaFallidaException.class).hasMessageContaining("pods").hasMessageContaining(NS);
    }

    @Test
    void unFalloAlLeerLosEventosSaleComoLecturaFallida() throws Exception {
        siembraPod(POD, null, esperando(CONT_CORE, "CrashLoopBackOff", 1));
        sirveLog(POD, CONT_CORE, false, "x");
        sirveLog(POD, CONT_INIT, false, "y");
        falla(RUTA_EVENTOS, 500);
        terminaSiembra();
        assertThatThrownBy(() -> adaptador.contextoFallo(NS))
                .isInstanceOf(LecturaFallidaException.class).hasMessageContaining("eventos").hasMessageContaining(NS);
    }

    @Test
    void unLogConErrorDeServidorNoSeTragaComoVacio() throws Exception {
        logFallaYNoSeTraga(500);
    }

    @Test
    void unLogProhibidoNoSeTragaComoVacio() throws Exception {
        logFallaYNoSeTraga(403);
    }

    private void logFallaYNoSeTraga(int codigo) throws Exception {
        siembraPod(POD, null, esperando(CONT_CORE, "CrashLoopBackOff", 1));
        falla(RUTA_POD + POD + "/log?pretty=false&container=" + CONT_CORE + TAIL, codigo);
        terminaSiembra();
        assertThatThrownBy(() -> adaptador.contextoFallo(NS))
                .isInstanceOf(LecturaFallidaException.class)
                .hasMessageContaining(CONT_CORE).hasMessageContaining(POD)
                .cause().isInstanceOfSatisfying(KubernetesClientException.class,
                        k -> assertThat(k.getCode()).isEqualTo(codigo));
    }

    @Test
    void losEventosSeOrdenanPorInstanteAunqueMezclenFormatosYLosSinMarcaVanAlFinal() throws Exception {
        siembraDeployment(1, 0);
        siembraPod(POD, null, esperando(CONT_CORE, "CrashLoopBackOff", 1));
        // Como texto, "...:02.500000Z" < "...:02Z" (el '.' sale antes que la 'Z'): solo el Instant los ordena bien.
        siembraEvento("e3", POD, "Warning", "R3", "tercero", "2026-01-01T00:00:03Z");
        siembraEvento("e2", POD, "Warning", "R2", "segundo", null, "2026-01-01T00:00:02.500000Z");
        siembraEvento("e1", POD, "Warning", "R1", "primero", "2026-01-01T00:00:02Z");
        siembraEvento("e0", POD, "Warning", "R0", "sin marca", null);
        terminaSiembra();
        var lineas = adaptador.contextoFallo(NS).eventos().lines().toList();
        assertThat(lineas).hasSize(4);
        assertThat(lineas.get(0)).contains("primero");
        assertThat(lineas.get(1)).contains("segundo");
        assertThat(lineas.get(2)).contains("tercero");
        assertThat(lineas.get(3)).contains("sin marca");
    }

    @Test
    void unaExcepcionQueNoEsDeLaApiNoSeEnvuelveEnLecturaFallida() throws Exception {
        var roto = org.mockito.Mockito.mock(KubernetesClient.class);
        org.mockito.Mockito.when(roto.apps()).thenThrow(new IllegalStateException("bug propio"));
        org.mockito.Mockito.when(roto.pods()).thenThrow(new IllegalStateException("bug propio"));
        var a = new AdaptadorFabric8(roto);
        assertThatThrownBy(() -> a.estado(NS)).isExactlyInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> a.contextoFallo(NS)).isExactlyInstanceOf(IllegalStateException.class);
    }
}
