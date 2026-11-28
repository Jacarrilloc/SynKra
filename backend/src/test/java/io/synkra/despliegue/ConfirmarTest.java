package io.synkra.despliegue;

import static io.synkra.despliegue.HttpPlanes.detalle;
import static io.synkra.despliegue.HttpPlanes.esperaError;
import static io.synkra.despliegue.HttpPlanes.id;
import static io.synkra.despliegue.HttpPlanes.sinConsulta;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.synkra.catalogo.api.CatalogoApi;
import io.synkra.kubernetes.SimuladorKubernetes;
import io.synkra.kubernetes.api.AdaptadorKubernetes;
import io.synkra.plataforma.ExcepcionApi;
import io.synkra.registro.api.ContextoFallo;
import io.synkra.registro.api.EstadoDespliegue;
import io.synkra.registro.api.NuevoDespliegue;
import io.synkra.registro.api.RegistroApi;
import io.synkra.registro.api.TipoDespliegue;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.convention.TestBean;

/**
 * U07-T07, R-D5 a R-D9 (decisión 6): {@code POST /api/despliegues/{id}/confirmacion} contra el catálogo real y el
 * servidor simulado de Fabric8, con lectura de vuelta por la API y por la base. Cada caso usa su propio país y su
 * propia hora base (el reloj es un {@link RelojMovible}).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ConfirmarTest {

    static final Path DATOS = Path.of("build/sqlite-pruebas/ConfirmarTest");
    static final Instant T1 = Instant.parse("2026-11-20T15:00:00.456Z");

    static final SimuladorKubernetes SIM = new SimuladorKubernetes();
    static final RelojMovible RELOJ = new RelojMovible(T1);

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
    @Autowired ReservaConfirmacion reserva;
    @Autowired PlanRepositorio planes;
    @Autowired TestRestTemplate rest;
    @Autowired JdbcTemplate jdbc;

    private static boolean cargada;

    private synchronized void cargar() throws IOException {
        if (cargada) {
            return;
        }
        CatalogoDePrueba.cargar(catalogo);
        for (var pais : List.of("pe", "co", "cl", "ec")) {
            SIM.aceptaAplicar(pais);
        }
        for (var nombre : List.of("kyc-zz", "kyc-yy")) {
            catalogo.subirPlugin("synkra-core", new ByteArrayInputStream(jar(nombre)), Map.of());
        }
        cargada = true;
    }

    private static byte[] jar(String nombre) throws IOException {
        var salida = new ByteArrayOutputStream();
        try (var z = new ZipArchiveOutputStream(salida)) {
            z.putArchiveEntry(new ZipArchiveEntry("synkra-plugin.yaml"));
            z.write(("nombre: " + nombre + "\nversion: 1.0.0\ncore: synkra-core\ncoreCompatible: 2.x\n")
                    .getBytes(StandardCharsets.UTF_8));
            z.closeArchiveEntry();
        }
        return salida.toByteArray();
    }

    private HttpPlanes http() {
        return new HttpPlanes(rest);
    }

    private String sha(String plugin, String version) {
        return catalogo.obtenerPlugin("synkra-core", plugin, version).sha256();
    }

    /** Crea un plan S2 en {@code pais} a la hora {@code base}, con el usuario dado (nulo: sin cabecera). */
    private long crear(String pais, Instant base, String usuario, String plugin) {
        RELOJ.fija(base);
        var r = http().crear(pais, "synkra-core", "2.1.0", usuario, plugin);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return id(r);
    }

    private long crear(String pais, Instant base) {
        return crear(pais, base, "ana", "kyc-pe:1.3.0");
    }

    private String estadoPlan(long id) {
        return jdbc.queryForObject("select estado from dep_plan where id = ?", String.class, id);
    }

    private long filasRegistro(String pais) {
        return jdbc.queryForObject("select count(*) from reg_despliegue where pais = ?", Long.class, pais);
    }

    private static List<String> tresPatch(String pais) {
        return List.of("PATCH /api/v1/namespaces/synkra-" + pais + "/configmaps/synkra-core-config",
                "PATCH /api/v1/namespaces/synkra-" + pais + "/services/synkra-core",
                "PATCH /apis/apps/v1/namespaces/synkra-" + pais + "/deployments/synkra-core");
    }

    @Test
    void peConfirmaYSeRegistraAplicando() throws Exception {
        cargar();
        var id = crear("pe", T1);
        assertThat(http().obtener("/api/despliegues/" + id).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(http().obtener("/api/despliegues/" + id).getBody()).containsEntry("codigo", "NO_ENCONTRADO");
        assertThat(http().lista("/api/paises/pe/despliegues").getBody()).isEqualTo(List.of());
        var planViejo = planes.findById(id).orElseThrow();
        var confirmadoEn = T1.plus(Duration.ofMinutes(10));
        RELOJ.fija(confirmadoEn);
        SIM.nuevasPeticiones();

        var r = http().confirmar(id, "luis");

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(r.getHeaders().getLocation()).isNotNull();
        assertThat(r.getHeaders().getLocation().getPath()).isEqualTo("/api/despliegues/" + id);
        assertThat(r.getBody()).containsOnlyKeys("id", "pais", "estado", "creadoPor", "confirmadoPor", "confirmadoEn")
                .containsEntry("pais", "pe").containsEntry("estado", "aplicando").containsEntry("creadoPor", "ana")
                .containsEntry("confirmadoPor", "luis");
        assertThat(((Number) r.getBody().get("id")).longValue()).isEqualTo(id);
        assertThat(Instant.parse((String) r.getBody().get("confirmadoEn"))).isEqualTo(confirmadoEn);
        assertThat(sinConsulta(SIM.nuevasPeticiones())).containsExactlyElementsOf(tresPatch("pe"));

        var corre = SIM.adaptador().leerPais("pe").orElseThrow();
        assertThat(corre.imagenCore()).isEqualTo(CatalogoDePrueba.IMAGEN_A);
        assertThat(corre.plugins()).hasSize(1).first().satisfies(p -> {
            assertThat(p.nombre()).isEqualTo("kyc-pe");
            assertThat(p.version()).isEqualTo("1.3.0");
            assertThat(p.sha256()).isEqualTo(sha("kyc-pe", "1.3.0"));
        });
        SIM.nuevasPeticiones();

        var g = http().obtener("/api/despliegues/" + id);
        assertThat(g.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(g.getBody()).containsEntry("estado", "aplicando").containsEntry("creadoPor", "ana")
                .containsEntry("confirmadoPor", "luis").containsEntry("imagen", CatalogoDePrueba.IMAGEN_A);
        @SuppressWarnings("unchecked")
        var veredicto = (Map<String, Object>) g.getBody().get("veredicto");
        assertThat(veredicto).containsEntry("global", "APROBADA");
        assertThat(estadoPlan(id)).isEqualTo("confirmado");

        // Una segunda confirmación no vuelve a tocar nada.
        SIM.nuevasPeticiones();
        esperaError(http().confirmar(id, "luis"), HttpStatus.CONFLICT, "PLAN_NO_PENDIENTE",
                "El plan ya no está pendiente");
        assertThat(SIM.nuevasPeticiones()).isEmpty();

        // El UPDATE va primero: con el plan leído antes, reservar da PLAN_NO_PENDIENTE, no DESPLIEGUE_EN_CURSO.
        assertThatThrownBy(() -> reserva.reservar(planViejo, "luis", "{}", List.of()))
                .isInstanceOfSatisfying(ExcepcionApi.class, e -> {
                    assertThat(e.estado()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(e.error().codigo()).isEqualTo("PLAN_NO_PENDIENTE");
                });
        assertThat(filasRegistro("pe")).isEqualTo(1);
        assertThat(http().lista("/api/paises/pe/despliegues").getBody()).hasSize(1);
    }

    @Test
    void coUnSegundoPlanConUnoAplicandoDa409DespliegueEnCurso() throws Exception {
        cargar();
        var base = T1.plus(Duration.ofHours(1));
        var primero = crear("co", base);
        SIM.nuevasPeticiones();
        assertThat(http().confirmar(primero, "luis").getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        var segundo = crear("co", base.plus(Duration.ofMinutes(1)));
        SIM.nuevasPeticiones();

        esperaError(http().confirmar(segundo, "luis"), HttpStatus.CONFLICT, "DESPLIEGUE_EN_CURSO",
                "Ya hay un despliegue en curso en el país");

        assertThat(SIM.nuevasPeticiones()).isEmpty();
        assertThat(estadoPlan(segundo)).isEqualTo("pendiente");
        assertThat(filasRegistro("co")).isEqualTo(1);
    }

    @Test
    void clSoloBloqueaUnAplicandoYSinCabeceraElUsuarioEsAnonimo() throws Exception {
        cargar();
        var base = T1.plus(Duration.ofHours(2));
        registro.registrar(new NuevoDespliegue(900, "cl", TipoDespliegue.DESPLIEGUE, null, "synkra-core", "2.1.0",
                CatalogoDePrueba.IMAGEN_A, "ana", "ana", base.minusSeconds(60), "{}", List.of()));
        registro.actualizarResultado(900, EstadoDespliegue.LISTO, null);
        registro.registrar(new NuevoDespliegue(901, "cl", TipoDespliegue.DESPLIEGUE, null, "synkra-core", "2.1.0",
                CatalogoDePrueba.IMAGEN_A, "ana", "ana", base.minusSeconds(30), "{}", List.of()));
        registro.actualizarResultado(901, EstadoDespliegue.FALLIDO, new ContextoFallo("", "", "x"));
        var id = crear("cl", base, null, "kyc-pe:1.3.0");
        SIM.nuevasPeticiones();

        var r = http().confirmar(id, null);

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(r.getBody()).containsEntry("creadoPor", "anonimo").containsEntry("confirmadoPor", "anonimo")
                .containsEntry("estado", "aplicando");
        assertThat(sinConsulta(SIM.nuevasPeticiones())).containsExactlyElementsOf(tresPatch("cl"));
        var g = http().obtener("/api/despliegues/" + id).getBody();
        assertThat(g).containsEntry("creadoPor", "anonimo").containsEntry("confirmadoPor", "anonimo");
    }

    @Test
    void arElCatalogoCambioEntreMediasVuelveAVerificar() throws Exception {
        cargar();
        var id = crear("ar", T1.plus(Duration.ofHours(3)), "ana", "kyc-zz:1.0.0");
        jdbc.update("update cat_plugin set core_compatible = '3.x' where nombre = 'kyc-zz'");
        var mensaje = motivoDeVerificacion("kyc-zz:1.0.0");
        SIM.nuevasPeticiones();

        var r = http().confirmar(id, "luis");

        esperaError(r, HttpStatus.UNPROCESSABLE_ENTITY, "COMBINACION_RECHAZADA", "La combinación no es compatible",
                detalle("plugins.kyc-zz", mensaje));
        assertThat(SIM.nuevasPeticiones()).isEmpty();
        assertThat(estadoPlan(id)).isEqualTo("pendiente");
        assertThat(filasRegistro("ar")).isZero();
    }

    @SuppressWarnings("unchecked")
    private String motivoDeVerificacion(String plugin) {
        var v = http().verificar("synkra-core", "2.1.0", plugin);
        assertThat(v.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(v.getBody()).containsEntry("global", "RECHAZADA");
        var plugins = (List<Map<String, Object>>) v.getBody().get("plugins");
        var motivos = (List<Map<String, Object>>) plugins.get(0).get("motivos");
        assertThat(motivos).hasSize(1);
        return (String) motivos.get(0).get("mensaje");
    }

    @Test
    void uyElResumenDesactualizadoDa422() throws Exception {
        cargar();
        var id = crear("uy", T1.plus(Duration.ofHours(4)), "ana", "kyc-yy:1.0.0");
        jdbc.update("update cat_plugin set sha256 = ? where nombre = 'kyc-yy'", "7".repeat(64));
        SIM.nuevasPeticiones();

        var r = http().confirmar(id, "luis");

        esperaError(r, HttpStatus.UNPROCESSABLE_ENTITY, "PLAN_DESACTUALIZADO",
                "El catálogo cambió desde que se creó el plan");
        assertThat(SIM.nuevasPeticiones()).isEmpty();
        assertThat(estadoPlan(id)).isEqualTo("pendiente");
        assertThat(filasRegistro("uy")).isZero();
    }

    @Test
    void boSiAplicarFallaQuedaFallidoSinReintentos() throws Exception {
        cargar();
        var id = crear("bo", T1.plus(Duration.ofHours(5)));
        SIM.fallaAplicar("bo");
        SIM.nuevasPeticiones();

        var r = http().confirmar(id, "luis");

        esperaError(r, HttpStatus.BAD_GATEWAY, "APLICACION_FALLIDA", "No se pudo aplicar el plan en el clúster");
        // Uno por objeto: el del Deployment falla y ninguno se repite.
        assertThat(sinConsulta(SIM.nuevasPeticiones())).containsExactlyElementsOf(tresPatch("bo"));
        assertThat(estadoPlan(id)).isEqualTo("confirmado");
        var d = registro.obtener(id).orElseThrow();
        assertThat(d.estado()).isEqualTo(EstadoDespliegue.FALLIDO);
        assertThat(d.contexto()).isEqualTo(new ContextoFallo("", "", "No se pudo aplicar Deployment/synkra-core"));
        var g = http().obtener("/api/despliegues/" + id).getBody();
        assertThat(g).containsEntry("estado", "fallido");
    }

    @Test
    void ecDosConfirmacionesAlaVezSoloUnaGana() throws Exception {
        cargar();
        var base = T1.plus(Duration.ofHours(6));
        var uno = crear("ec", base);
        var dos = crear("ec", base.plusSeconds(1));
        RELOJ.fija(base.plusSeconds(1));
        SIM.nuevasPeticiones();
        var salida = new CountDownLatch(1);
        var pool = Executors.newFixedThreadPool(2);
        try {
            var f1 = pool.submit(() -> {
                salida.await();
                return http().confirmar(uno, "luis");
            });
            var f2 = pool.submit(() -> {
                salida.await();
                return http().confirmar(dos, "luis");
            });
            salida.countDown();
            var r1 = f1.get(20, TimeUnit.SECONDS);
            var r2 = f2.get(20, TimeUnit.SECONDS);

            assertThat(List.of(r1.getStatusCode(), r2.getStatusCode()))
                    .containsExactlyInAnyOrder(HttpStatus.ACCEPTED, HttpStatus.CONFLICT);
            var perdedora = r1.getStatusCode() == HttpStatus.CONFLICT ? r1 : r2;
            esperaError(perdedora, HttpStatus.CONFLICT, "DESPLIEGUE_EN_CURSO",
                    "Ya hay un despliegue en curso en el país");
        } finally {
            pool.shutdownNow();
        }
        assertThat(SIM.nuevasPeticiones()).hasSize(3);
        assertThat(filasRegistro("ec")).isEqualTo(1);
    }

    @Test
    void pyUnUsuarioInvalidoAlCrearNoDejaFilaNiPeticiones() throws Exception {
        cargar();
        RELOJ.fija(T1.plus(Duration.ofHours(7)));
        var antes = jdbc.queryForObject("select count(*) from dep_plan where pais = 'py'", Long.class);
        SIM.nuevasPeticiones();

        var r = http().crear("py", "synkra-core", "2.1.0", "Luis Pérez", "kyc-pe:1.3.0");

        esperaError(r, HttpStatus.UNPROCESSABLE_ENTITY, "USUARIO_INVALIDO", "El usuario no es válido",
                detalle("X-Synkra-Usuario", "El usuario debe tener de 1 a 64 caracteres: minúsculas, dígitos, punto,"
                        + " guion o guion bajo"));
        assertThat(SIM.nuevasPeticiones()).isEmpty();
        assertThat(jdbc.queryForObject("select count(*) from dep_plan where pais = 'py'", Long.class))
                .isEqualTo(antes);
    }

    @Test
    void pyUnUsuarioInvalidoAlConfirmarDejaElPlanPendiente() throws Exception {
        cargar();
        var id = crear("py", T1.plus(Duration.ofHours(7)));
        SIM.nuevasPeticiones();

        var r = http().confirmar(id, "LUIS");

        esperaError(r, HttpStatus.UNPROCESSABLE_ENTITY, "USUARIO_INVALIDO", "El usuario no es válido",
                detalle("X-Synkra-Usuario", "El usuario debe tener de 1 a 64 caracteres: minúsculas, dígitos, punto,"
                        + " guion o guion bajo"));
        assertThat(SIM.nuevasPeticiones()).isEmpty();
        assertThat(estadoPlan(id)).isEqualTo("pendiente");
        assertThat(filasRegistro("py")).isZero();
    }

    @Test
    void unPlanInexistenteDa404() throws Exception {
        cargar();
        SIM.nuevasPeticiones();
        esperaError(http().confirmar(999, "luis"), HttpStatus.NOT_FOUND, "NO_ENCONTRADO", "Recurso no encontrado");
        assertThat(SIM.nuevasPeticiones()).isEmpty();
    }
}
