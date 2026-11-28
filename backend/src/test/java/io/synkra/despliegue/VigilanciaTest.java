package io.synkra.despliegue;

import static io.synkra.despliegue.HttpPlanes.id;
import static org.assertj.core.api.Assertions.assertThat;

import io.synkra.catalogo.api.CatalogoApi;
import io.synkra.kubernetes.SimuladorKubernetes;
import io.synkra.kubernetes.api.AdaptadorKubernetes;
import io.synkra.kubernetes.api.PlanPais;
import io.synkra.registro.api.ContextoFallo;
import io.synkra.registro.api.EstadoDespliegue;
import io.synkra.registro.api.NuevoDespliegue;
import io.synkra.registro.api.PluginDesplegado;
import io.synkra.registro.api.RegistroApi;
import io.synkra.registro.api.TipoDespliegue;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.convention.TestBean;

/**
 * U07-T08, R-D10 (decisión 11): {@code revisar()} a mano contra el servidor simulado de Fabric8, con la programación
 * apagada y un {@link RelojMovible}. Cada caso usa su país y su hora base y termina con sus filas cerradas. Después de
 * cada confirmación se drenan sus tres {@code PATCH}: desde ahí toda petición tiene que ser un {@code GET}.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class VigilanciaTest {

    static final Path DATOS = Path.of("build/sqlite-pruebas/VigilanciaTest");
    static final Instant T2 = Instant.parse("2026-11-22T15:00:00.789Z");

    static final String PLAZO = "Venció el plazo de vigilancia sin que el despliegue quedara listo";
    static final String NO_APLICADO = "Lo que corre en el clúster no es lo que se confirmó";
    static final String SIN_LECTURA = "No se pudo leer el estado del despliegue en el clúster";

    static final SimuladorKubernetes SIM = new SimuladorKubernetes();
    static final RelojMovible RELOJ = new RelojMovible(T2);

    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry r) throws IOException {
        if (Files.exists(DATOS)) {
            try (var s = Files.walk(DATOS)) {
                s.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
            }
        }
        Files.createDirectories(DATOS);
        r.add("synkra.data", () -> DATOS.toAbsolutePath().toString());
        r.add("synkra.paises", () -> "pe,co,cl,ar,uy,bo,ec,py");
        r.add("synkra.vigilancia.plazo", () -> "PT2M");
    }

    @TestBean AdaptadorKubernetes adaptador;

    static AdaptadorKubernetes adaptador() {
        return SIM.adaptador();
    }

    @TestBean Clock reloj;

    static Clock reloj() {
        return RELOJ;
    }

    @AfterAll
    static void cierra() {
        SIM.close();
    }

    @Autowired CatalogoApi catalogo;
    @Autowired RegistroApi registro;
    @Autowired VigilanciaDespliegues vigilancia;
    @Autowired TestRestTemplate rest;

    private static boolean cargada;

    private synchronized void cargar() throws IOException {
        if (cargada) {
            return;
        }
        CatalogoDePrueba.cargar(catalogo);
        for (var pais : List.of("pe", "co", "cl", "ar", "uy", "ec")) {
            SIM.aceptaAplicar(pais);
        }
        cargada = true;
    }

    private HttpPlanes http() {
        return new HttpPlanes(rest);
    }

    /** Crea y confirma un plan S2 en {@code pais} a la hora {@code base}; drena las peticiones de la confirmación. */
    private long confirma(String pais, Instant base) {
        RELOJ.fija(base);
        var creado = http().crear(pais, "synkra-core", "2.1.0", "ana", "kyc-pe:1.3.0");
        assertThat(creado.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        var id = id(creado);
        assertThat(http().confirmar(id, "luis").getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(SIM.nuevasPeticiones()).as("la confirmación").contains(
                "PATCH /apis/apps/v1/namespaces/synkra-" + pais + "/deployments/synkra-core?fieldManager=synkra&force=true");
        return id;
    }

    private void soloLecturas() {
        assertThat(SIM.nuevasPeticiones()).allSatisfy(p -> assertThat(p).startsWith("GET "));
    }

    private int revisaA(Instant hora) {
        RELOJ.fija(hora);
        return vigilancia.revisar();
    }

    private EstadoDespliegue estado(long id) {
        return registro.obtener(id).orElseThrow().estado();
    }

    private void cerradoComoFallido(long id, ContextoFallo esperado) {
        var d = registro.obtener(id).orElseThrow();
        assertThat(d.estado()).isEqualTo(EstadoDespliegue.FALLIDO);
        assertThat(d.contexto()).isEqualTo(esperado);
        assertThat(d.terminadoEn()).isNotNull();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> api(long id) {
        var g = http().obtener("/api/despliegues/" + id);
        assertThat(g.getStatusCode()).isEqualTo(HttpStatus.OK);
        return g.getBody();
    }

    @Test
    void peListoCuandoLoQueCorreEsLoConfirmado() throws Exception {
        cargar();
        var id = confirma("pe", T2);

        assertThat(revisaA(T2.plusSeconds(10))).isZero();
        assertThat(estado(id)).isEqualTo(EstadoDespliegue.APLICANDO);
        soloLecturas();

        SIM.marcaListo("pe");
        assertThat(revisaA(T2.plusSeconds(20))).isEqualTo(1);
        var d = registro.obtener(id).orElseThrow();
        assertThat(d.estado()).isEqualTo(EstadoDespliegue.LISTO);
        assertThat(d.contexto()).isNull();
        assertThat(d.terminadoEn()).isNotNull();
        var lecturas = SIM.nuevasPeticiones();
        assertThat(lecturas).isNotEmpty().allSatisfy(p -> assertThat(p).startsWith("GET ").doesNotContain("/log"));
        assertThat(api(id)).containsEntry("estado", "listo");

        assertThat(vigilancia.revisar()).isZero();
        assertThat(SIM.nuevasPeticiones()).isEmpty();
    }

    @Test
    void coVenceElPlazoExactoConPropiedadDeDosMinutos() throws Exception {
        cargar();
        var base = T2.plus(Duration.ofHours(1));
        var id = confirma("co", base);

        assertThat(revisaA(base.plus(Duration.ofMinutes(2)).minusMillis(1))).isZero();
        assertThat(estado(id)).isEqualTo(EstadoDespliegue.APLICANDO);
        soloLecturas();

        assertThat(revisaA(base.plus(Duration.ofMinutes(2)))).isEqualTo(1);
        cerradoComoFallido(id, new ContextoFallo("", "", PLAZO));
        soloLecturas();
        assertThat(http().lista("/api/paises/co/despliegues").getBody()).hasSize(1);

        assertThat(vigilancia.revisar()).isZero();
        assertThat(SIM.nuevasPeticiones()).isEmpty();
        assertThat(http().lista("/api/paises/co/despliegues").getBody()).hasSize(1);
        assertThat(api(id)).containsEntry("estado", "fallido");
    }

    @Test
    void clCrashLoopBackOffCierraEnLaMismaPasadaConElContextoDeVerdad() throws Exception {
        cargar();
        var base = T2.plus(Duration.ofHours(2));
        var id = confirma("cl", base);
        SIM.marcaFallo("cl", "CrashLoopBackOff");

        assertThat(revisaA(base.plusSeconds(30))).isEqualTo(1);
        cerradoComoFallido(id, new ContextoFallo(SimuladorKubernetes.LOG_CORE, SimuladorKubernetes.LOG_INIT,
                SimuladorKubernetes.EVENTO));
        soloLecturas();
        assertThat(api(id).get("contexto")).isEqualTo(Map.of("logCore", SimuladorKubernetes.LOG_CORE,
                "logInit", SimuladorKubernetes.LOG_INIT, "eventos", SimuladorKubernetes.EVENTO));
        assertThat(vigilancia.revisar()).isZero();
        assertThat(SIM.nuevasPeticiones()).isEmpty();
    }

    @Test
    void arInitErrorCierraConElLogDelInit() throws Exception {
        cargar();
        var base = T2.plus(Duration.ofHours(3));
        var id = confirma("ar", base);
        SIM.marcaFallo("ar", "Init:Error");

        assertThat(revisaA(base.plusSeconds(30))).isEqualTo(1);
        cerradoComoFallido(id, new ContextoFallo("", SimuladorKubernetes.LOG_INIT, SimuladorKubernetes.EVENTO));
        soloLecturas();
    }

    @Test
    void uyUnPodViejoListoNoTapaAlNuevoQueFalla() throws Exception {
        cargar();
        var base = T2.plus(Duration.ofHours(4));
        var id = confirma("uy", base);
        SIM.marcaRolloutConFallo("uy");

        assertThat(revisaA(base.plusSeconds(30))).isEqualTo(1);
        cerradoComoFallido(id, new ContextoFallo(SimuladorKubernetes.LOG_CORE, SimuladorKubernetes.LOG_INIT,
                SimuladorKubernetes.EVENTO));
        assertThat(estado(id)).isNotEqualTo(EstadoDespliegue.LISTO);
        soloLecturas();
    }

    @Test
    void boLoQueCorreNoEsLoConfirmadoNuncaEsListo() throws Exception {
        cargar();
        var base = T2.plus(Duration.ofHours(5));
        var kycPe = catalogo.obtenerPlugin("synkra-core", "kyc-pe", "1.3.0").sha256();
        var kycCo = catalogo.obtenerPlugin("synkra-core", "kyc-co", "1.0.0").sha256();
        SIM.siembra(new PlanPais("bo", CatalogoDePrueba.IMAGEN_B,
                List.of(new PlanPais.PluginPlan("kyc-co", "1.0.0", kycCo))));
        SIM.marcaListo("bo");
        registro.registrar(sinConfirmar(950, "bo", base, kycPe));

        assertThat(revisaA(base.plusSeconds(60))).isZero();
        assertThat(estado(950)).isEqualTo(EstadoDespliegue.APLICANDO);
        soloLecturas();

        assertThat(revisaA(base.plus(Duration.ofMinutes(2)))).isEqualTo(1);
        cerradoComoFallido(950, new ContextoFallo("", "", PLAZO + "\n" + NO_APLICADO));
        soloLecturas();
    }

    @Test
    void pyUnAplicandoSinNadaEnElClusterSeCierraPorPlazo() throws Exception {
        cargar();
        var base = T2.plus(Duration.ofHours(7));
        registro.registrar(sinConfirmar(951, "py", base, catalogo.obtenerPlugin("synkra-core", "kyc-pe", "1.3.0").sha256()));

        assertThat(revisaA(base.plusSeconds(60))).isZero();
        assertThat(revisaA(base.plus(Duration.ofMinutes(2)))).isEqualTo(1);
        cerradoComoFallido(951, new ContextoFallo("", "", PLAZO + "\n" + NO_APLICADO));
        soloLecturas();
    }

    @Test
    void ecSinLecturaNoCierraNadaHastaElPlazo() throws Exception {
        cargar();
        var base = T2.plus(Duration.ofHours(6));
        var id = confirma("ec", base);
        SIM.fallaLecturas("ec");

        assertThat(revisaA(base.plusSeconds(60))).isZero();
        assertThat(estado(id)).isEqualTo(EstadoDespliegue.APLICANDO);
        soloLecturas();

        assertThat(revisaA(base.plus(Duration.ofMinutes(2)))).isEqualTo(1);
        cerradoComoFallido(id, new ContextoFallo("", "", PLAZO + "\n" + SIN_LECTURA));
        soloLecturas();
    }

    private static NuevoDespliegue sinConfirmar(long id, String pais, Instant confirmadoEn, String shaKycPe) {
        return new NuevoDespliegue(id, pais, TipoDespliegue.DESPLIEGUE, null, "synkra-core", "2.1.0",
                CatalogoDePrueba.IMAGEN_A, "ana", "luis", confirmadoEn,
                "{\"global\":\"APROBADA\",\"porPlugin\":[],\"motivosGlobales\":[]}",
                List.of(new PluginDesplegado("kyc-pe", "1.3.0", shaKycPe, Map.of())));
    }
}
