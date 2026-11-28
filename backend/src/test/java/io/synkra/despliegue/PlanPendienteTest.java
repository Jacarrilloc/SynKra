package io.synkra.despliegue;

import static org.assertj.core.api.Assertions.assertThat;

import io.synkra.catalogo.api.CatalogoApi;
import io.synkra.kubernetes.SimuladorKubernetes;
import io.synkra.kubernetes.api.AdaptadorKubernetes;
import io.synkra.kubernetes.api.PlanPais;
import io.synkra.kubernetes.api.PlanPais.PluginPlan;
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
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.convention.TestBean;

/**
 * U07-T06, R-D5 y R-D13 (Q6): un plan sin confirmar nunca aplica y vence. Contra el catálogo real y el servidor
 * simulado de Fabric8; el reloj es un {@link RelojMovible}. Cada caso usa su propio país y su propia hora base.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PlanPendienteTest {

    static final Path DATOS = Path.of("build/sqlite-pruebas/PlanPendienteTest");
    static final Instant T0 = Instant.parse("2026-11-18T11:00:00.123Z");
    static final Duration VIGENCIA = Duration.ofMinutes(30);

    static final SimuladorKubernetes SIM = new SimuladorKubernetes();
    static final RelojMovible RELOJ = new RelojMovible(T0);

    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry r) throws IOException {
        if (Files.exists(DATOS)) {
            try (var s = Files.walk(DATOS)) {
                s.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
            }
        }
        Files.createDirectories(DATOS);
        r.add("synkra.data", () -> DATOS.toAbsolutePath().toString());
        r.add("synkra.paises", () -> "pe,co,cl,py,ar,uy,bo,ec");
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
    @Autowired TestRestTemplate http;
    @Autowired JdbcTemplate jdbc;
    @Autowired VencimientoPlanes vencimiento;
    @Autowired GuardaConfirmacion guarda;

    private static boolean cargada;

    private synchronized void cargar() throws IOException {
        if (!cargada) {
            CatalogoDePrueba.cargar(catalogo);
            cargada = true;
        }
    }

    private String sha(String plugin, String version) {
        return catalogo.obtenerPlugin("synkra-core", plugin, version).sha256();
    }

    @SuppressWarnings("rawtypes")
    private org.springframework.http.ResponseEntity<Map> crear(String pais) {
        var h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        var json = "{\"pais\":\"" + pais + "\",\"core\":\"synkra-core\",\"versionCore\":\"2.1.0\","
                + "\"plugins\":[{\"nombre\":\"kyc-pe\",\"version\":\"1.3.0\"}]}";
        return http.exchange("/api/despliegues", HttpMethod.POST, new HttpEntity<>(json, h), Map.class);
    }

    /** Crea un plan S2 en {@code pais} a la hora {@code base}; verifica el 201, el reloj y que solo hubo lecturas. */
    private long crearA(String pais, Instant base) {
        RELOJ.fija(base);
        SIM.nuevasPeticiones();
        var r = crear(pais);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(Instant.parse((String) r.getBody().get("creadoEn"))).isEqualTo(base);
        assertThat(Instant.parse((String) r.getBody().get("venceEn"))).isEqualTo(base.plus(VIGENCIA));
        assertThat(SIM.nuevasPeticiones()).isNotEmpty().allSatisfy(p -> assertThat(p).startsWith("GET "));
        return ((Number) r.getBody().get("id")).longValue();
    }

    @SuppressWarnings("rawtypes")
    private org.springframework.http.ResponseEntity<Map> confirmar(String id) {
        SIM.nuevasPeticiones();
        var r = http.exchange("/api/despliegues/" + id + "/confirmacion", HttpMethod.POST, HttpEntity.EMPTY, Map.class);
        assertThat(SIM.nuevasPeticiones()).isEmpty();
        return r;
    }

    @SuppressWarnings("rawtypes")
    private static void esperaError(org.springframework.http.ResponseEntity<Map> r, HttpStatus estado, String codigo,
            String mensaje) {
        assertThat(r.getStatusCode()).isEqualTo(estado);
        assertThat(r.getBody()).containsEntry("codigo", codigo).containsEntry("mensaje", mensaje)
                .containsEntry("detalles", List.of());
    }

    private String estado(long id) {
        return jdbc.queryForObject("select estado from dep_plan where id = ?", String.class, id);
    }

    private long vencidos() {
        return jdbc.queryForObject("select count(*) from dep_plan where estado = 'vencido'", Long.class);
    }

    private static Instant venceEn(Instant base) {
        return base.plus(VIGENCIA);
    }

    @Test
    void elBarridoVenceSoloLosPlanesCuyoVenceEnLlego() throws Exception {
        cargar();
        var primero = crearA("ec", T0);
        var segundo = crearA("ec", T0.plus(Duration.ofMinutes(10)));

        RELOJ.fija(T0.plus(Duration.ofMinutes(35)));
        SIM.nuevasPeticiones();
        var vencidos = vencimiento.vencerPendientes();

        assertThat(vencidos).isEqualTo(1);
        assertThat(estado(primero)).isEqualTo("vencido");
        assertThat(estado(segundo)).isEqualTo("pendiente");
        assertThat(SIM.nuevasPeticiones()).isEmpty();
    }

    @Test
    void crearEnPaisVacioNoEscribeEnElCluster() throws Exception {
        cargar();
        var id = crearA("pe", T0.plus(Duration.ofHours(1)));
        assertThat(estado(id)).isEqualTo("pendiente");
    }

    @Test
    void crearEnPaisConOtroPluginNoEscribeEnElCluster() throws Exception {
        cargar();
        SIM.siembra(new PlanPais("co", CatalogoDePrueba.IMAGEN_A,
                List.of(new PluginPlan("kyc-co", "1.0.0", sha("kyc-co", "1.0.0")))));
        var id = crearA("co", T0.plus(Duration.ofHours(1)));
        assertThat(estado(id)).isEqualTo("pendiente");
    }

    @Test
    void crearEnPaisSinCambiosNoEscribeEnElCluster() throws Exception {
        cargar();
        SIM.siembra(new PlanPais("cl", CatalogoDePrueba.IMAGEN_A,
                List.of(new PluginPlan("kyc-pe", "1.3.0", sha("kyc-pe", "1.3.0")))));
        var id = crearA("cl", T0.plus(Duration.ofHours(1)));
        assertThat(estado(id)).isEqualTo("pendiente");
    }

    @Test
    void confirmarUnPlanVigenteDa501SinTocarElClusterNiLaFila() throws Exception {
        cargar();
        var id = crearA("py", T0.plus(Duration.ofHours(2)));

        SIM.nuevasPeticiones();
        assertThat(guarda.exigirConfirmable(id).estado()).isEqualTo("pendiente");

        assertThat(SIM.nuevasPeticiones()).isEmpty();
        assertThat(estado(id)).isEqualTo("pendiente");
    }

    @Test
    void confirmarUnPlanVencidoDa410YLoDejaVencidoSinTocarOtros() throws Exception {
        cargar();
        var base = T0.plus(Duration.ofHours(3));
        var primero = crearA("ar", base);
        var segundo = crearA("ar", base.plusSeconds(30));
        RELOJ.fija(base.plus(Duration.ofMinutes(31)));
        var antes = vencidos();

        esperaError(confirmar(String.valueOf(primero)), HttpStatus.GONE, "PLAN_VENCIDO",
                "El plan venció sin confirmarse");

        assertThat(estado(primero)).isEqualTo("vencido");
        assertThat(estado(segundo)).isEqualTo("pendiente");
        assertThat(vencidos()).isEqualTo(antes + 1);
        esperaError(confirmar(String.valueOf(primero)), HttpStatus.GONE, "PLAN_VENCIDO",
                "El plan venció sin confirmarse");
        assertThat(estado(primero)).isEqualTo("vencido");
    }

    @Test
    void laFronteraEsVenceEnExacto() throws Exception {
        cargar();
        var base = T0.plus(Duration.ofHours(4));
        var id = crearA("uy", base);

        RELOJ.fija(venceEn(base).minusMillis(1));
        SIM.nuevasPeticiones();
        assertThat(guarda.exigirConfirmable(id).estado()).isEqualTo("pendiente");
        assertThat(SIM.nuevasPeticiones()).isEmpty();
        assertThat(estado(id)).isEqualTo("pendiente");

        RELOJ.fija(venceEn(base));
        esperaError(confirmar(String.valueOf(id)), HttpStatus.GONE, "PLAN_VENCIDO", "El plan venció sin confirmarse");
        assertThat(estado(id)).isEqualTo("vencido");
    }

    @Test
    void unPlanConfirmadoDa409YNoCambia() throws Exception {
        cargar();
        var base = T0.plus(Duration.ofHours(5));
        var id = crearA("bo", base);
        jdbc.update("update dep_plan set estado = 'confirmado' where id = ?", id);
        RELOJ.fija(base.plus(Duration.ofMinutes(31)));

        esperaError(confirmar(String.valueOf(id)), HttpStatus.CONFLICT, "PLAN_NO_PENDIENTE",
                "El plan ya no está pendiente");

        assertThat(estado(id)).isEqualTo("confirmado");
    }

    @Test
    void unIdInexistenteODeFormaInvalidaNoEsUnPlan() throws Exception {
        cargar();
        for (var id : List.of("999", "0", "-1")) {
            esperaError(confirmar(id), HttpStatus.NOT_FOUND, "NO_ENCONTRADO", "Recurso no encontrado");
        }
        var r = confirmar("abc");
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(r.getBody()).containsEntry("codigo", "SOLICITUD_INVALIDA").containsEntry("mensaje", "La solicitud es inválida o su cuerpo no se puede leer")
                .containsEntry("detalles", List.of());
    }
}
