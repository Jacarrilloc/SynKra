package io.synkra.despliegue;

import static io.synkra.despliegue.HttpPlanes.detalle;
import static io.synkra.despliegue.HttpPlanes.esperaError;
import static io.synkra.despliegue.HttpPlanes.id;
import static io.synkra.despliegue.HttpPlanes.sinConsulta;
import static org.assertj.core.api.Assertions.assertThat;

import io.synkra.catalogo.api.CatalogoApi;
import io.synkra.kubernetes.SimuladorKubernetes;
import io.synkra.kubernetes.api.AdaptadorKubernetes;
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
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.convention.TestBean;

/**
 * U07-T10, R-D11 (H-20): {@code POST /api/despliegues/{id}/reversion} contra el catálogo real y el servidor simulado
 * de Fabric8, con la programación apagada y un {@link RelojMovible}. Cada caso usa su país y su hora base y termina
 * sin filas {@code aplicando}. Los orígenes manipulados se escriben con {@link RegistroApi}, sin plan.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ReversionTest {

    static final Path DATOS = Path.of("build/sqlite-pruebas/ReversionTest");
    static final Instant T3 = Instant.parse("2026-11-23T15:00:00.321Z");

    static final String ORIGEN_NO_LISTO = "Solo se puede revertir a un despliegue listo";
    static final String ORIGEN_DESACTUALIZADO = "El catálogo ya no tiene lo que se desplegó en el origen";
    static final String VEREDICTO = "{\"global\":\"APROBADA\",\"porPlugin\":[],\"motivosGlobales\":[]}";

    static final SimuladorKubernetes SIM = new SimuladorKubernetes();
    static final RelojMovible RELOJ = new RelojMovible(T3);

    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry r) throws IOException {
        if (Files.exists(DATOS)) {
            try (var s = Files.walk(DATOS)) {
                s.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
            }
        }
        Files.createDirectories(DATOS);
        r.add("synkra.data", () -> DATOS.toAbsolutePath().toString());
        r.add("synkra.paises", () -> "pe,py,co,cl,ar,ec,bo");
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
    @Autowired JdbcTemplate jdbc;

    private static boolean cargada;

    private synchronized void cargar() throws IOException {
        if (cargada) {
            return;
        }
        CatalogoDePrueba.cargar(catalogo);
        for (var pais : List.of("pe", "py", "ec")) {
            SIM.aceptaAplicar(pais);
        }
        cargada = true;
    }

    private HttpPlanes http() {
        return new HttpPlanes(rest);
    }

    /** Crea y confirma un plan en {@code pais} a la hora {@code base}; deja drenadas las peticiones. */
    private long confirma(String pais, Instant base, String plugin) {
        RELOJ.fija(base);
        var creado = http().crear(pais, "synkra-core", "2.1.0", "ana", plugin);
        assertThat(creado.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        var id = id(creado);
        assertThat(http().confirmar(id, "luis").getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        SIM.nuevasPeticiones();
        return id;
    }

    /** El despliegue {@code id} del país queda {@code listo} a la hora {@code hora}. */
    private void cierraListo(String pais, long id, Instant hora) {
        SIM.marcaListo(pais);
        RELOJ.fija(hora);
        assertThat(vigilancia.revisar()).isEqualTo(1);
        assertThat(estado(id)).isEqualTo(EstadoDespliegue.LISTO);
        SIM.nuevasPeticiones();
    }

    private EstadoDespliegue estado(long id) {
        return registro.obtener(id).orElseThrow().estado();
    }

    @SuppressWarnings("rawtypes")
    private ResponseEntity<Map> revertir(String id, String usuario, String cuerpo) {
        var h = new HttpHeaders();
        if (usuario != null) {
            h.set(HttpPlanes.CABECERA, usuario);
        }
        if (cuerpo != null) {
            h.setContentType(MediaType.APPLICATION_JSON);
        }
        return rest.exchange("/api/despliegues/" + id + "/reversion", HttpMethod.POST, new HttpEntity<>(cuerpo, h),
                Map.class);
    }

    @SuppressWarnings("rawtypes")
    private ResponseEntity<Map> revertir(long id, String usuario) {
        return revertir(Long.toString(id), usuario, null);
    }

    private long filasPlan(String pais) {
        return jdbc.queryForObject("select count(*) from dep_plan where pais = ?", Long.class, pais);
    }

    private Map<String, Object> filaPlan(long id) {
        return jdbc.queryForMap("select pais, tipo, origen_id, estado, creado_por, creado_en, vence_en from dep_plan"
                + " where id = ?", id);
    }

    private static List<String> tresPatch(String pais) {
        return List.of("PATCH /api/v1/namespaces/synkra-" + pais + "/configmaps/synkra-core-config",
                "PATCH /api/v1/namespaces/synkra-" + pais + "/services/synkra-core",
                "PATCH /apis/apps/v1/namespaces/synkra-" + pais + "/deployments/synkra-core");
    }

    private String sha(String plugin, String version) {
        return catalogo.obtenerPlugin("synkra-core", plugin, version).sha256();
    }

    /** Un origen escrito directo en el registro, sin plan; {@code listo} o {@code aplicando}. */
    private void origen(long id, String pais, Instant cuando, String imagen, String plugin, String version,
            String sha256, boolean listo) {
        registro.registrar(new NuevoDespliegue(id, pais, TipoDespliegue.DESPLIEGUE, null, "synkra-core", "2.1.0",
                imagen, "ana", "luis", cuando, VEREDICTO,
                List.of(new PluginDesplegado(plugin, version, sha256, Map.of()))));
        if (listo) {
            registro.actualizarResultado(id, EstadoDespliegue.LISTO, null);
        }
    }

    private void sinFilasNiPeticiones(String pais, long filasAntes) {
        assertThat(filasPlan(pais)).isEqualTo(filasAntes);
        assertThat(SIM.nuevasPeticiones()).isEmpty();
    }

    @Test
    void peRevertirAUnListoCreaUnPlanNuevoQueSeConfirmaYSeVigila() throws Exception {
        cargar();
        var a = confirma("pe", T3, "kyc-pe:1.3.0");
        cierraListo("pe", a, T3.plusSeconds(60));
        var b = confirma("pe", T3.plus(Duration.ofMinutes(5)), "kyc-co:1.0.0");
        SIM.marcaFallo("pe", "CrashLoopBackOff");
        RELOJ.fija(T3.plus(Duration.ofMinutes(6)));
        assertThat(vigilancia.revisar()).isEqualTo(1);
        assertThat(estado(b)).isEqualTo(EstadoDespliegue.FALLIDO);
        SIM.nuevasPeticiones();

        var diezMin = T3.plus(Duration.ofMinutes(10));
        RELOJ.fija(diezMin);
        var original = http().obtener("/api/despliegues/" + a).getBody();
        var antes = http().lista("/api/paises/pe/despliegues").getBody();
        assertThat(antes).hasSize(2);
        SIM.nuevasPeticiones();

        var r = revertir(a, "marta");

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(r.getHeaders().getLocation()).isNull();
        assertThat(SIM.nuevasPeticiones()).isNotEmpty().allSatisfy(p -> assertThat(p).startsWith("GET "));
        var plan = id(r);
        var cuerpo = r.getBody();
        assertThat(cuerpo).containsEntry("tipo", "reversion").containsEntry("estado", "pendiente")
                .containsEntry("pais", "pe").containsEntry("core", "synkra-core")
                .containsEntry("versionCore", "2.1.0").containsEntry("creadoPor", "marta");
        assertThat(((Number) cuerpo.get("origenId")).longValue()).isEqualTo(a);
        assertThat(Instant.parse((String) cuerpo.get("creadoEn"))).isEqualTo(diezMin);
        assertThat(Instant.parse((String) cuerpo.get("venceEn"))).isEqualTo(diezMin.plus(Duration.ofMinutes(30)));
        var kycPe = Map.of("nombre", "kyc-pe", "version", "1.3.0", "sha256", sha("kyc-pe", "1.3.0"));
        var kycCo = Map.of("nombre", "kyc-co", "version", "1.0.0", "sha256", sha("kyc-co", "1.0.0"));
        assertThat(cuerpo.get("resumen")).isEqualTo(Map.of("namespace", "synkra-pe", "core", "synkra-core",
                "versionCore", "2.1.0", "imagen", CatalogoDePrueba.IMAGEN_A, "plugins", List.of(kycPe)));
        assertThat(cuerpo.get("resumen")).isEqualTo(resumenDelPlan(a));
        assertThat(cuerpo.get("cambios")).isEqualTo(Map.of(
                "core", Map.of("antes", CatalogoDePrueba.IMAGEN_A, "despues", CatalogoDePrueba.IMAGEN_A, "igual", true),
                "anadidos", List.of(kycPe), "quitados", List.of(kycCo), "iguales", List.of(), "sinCambios", false));
        assertThat(original).containsEntry("estado", "listo");

        // H-17: un plan sin confirmar no está en el registro.
        assertThat(http().obtener("/api/despliegues/" + plan).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(http().lista("/api/paises/pe/despliegues").getBody()).hasSize(2);
        assertThat(registro.obtener(plan)).isEmpty();
        var fila = filaPlan(plan);
        assertThat(fila).containsEntry("tipo", "reversion").containsEntry("estado", "pendiente")
                .containsEntry("creado_por", "marta").containsEntry("pais", "pe");
        assertThat(((Number) fila.get("origen_id")).longValue()).isEqualTo(a);

        RELOJ.fija(diezMin.plus(Duration.ofMinutes(1)));
        var c = http().confirmar(plan, "luis");

        assertThat(c.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(sinConsulta(SIM.nuevasPeticiones())).containsExactlyElementsOf(tresPatch("pe"));
        var d = registro.obtener(plan).orElseThrow();
        assertThat(d.tipo()).isEqualTo(TipoDespliegue.REVERSION);
        assertThat(d.origenId()).isEqualTo(a);
        assertThat(d.creadoPor()).isEqualTo("marta");
        assertThat(d.confirmadoPor()).isEqualTo("luis");
        assertThat(d.estado()).isEqualTo(EstadoDespliegue.APLICANDO);
        assertThat(d.imagen()).isEqualTo(CatalogoDePrueba.IMAGEN_A);
        assertThat(d.plugins()).extracting(PluginDesplegado::nombre, PluginDesplegado::version,
                PluginDesplegado::sha256).containsExactly(
                        org.assertj.core.groups.Tuple.tuple("kyc-pe", "1.3.0", sha("kyc-pe", "1.3.0")));
        assertThat(filaPlan(plan)).containsEntry("estado", "confirmado");

        cierraListo("pe", plan, diezMin.plus(Duration.ofMinutes(2)));
        var corre = SIM.adaptador().leerPais("pe").orElseThrow();
        assertThat(corre.imagenCore()).isEqualTo(CatalogoDePrueba.IMAGEN_A);
        assertThat(corre.plugins()).extracting(p -> p.nombre() + " " + p.version()).containsExactly("kyc-pe 1.3.0");
        var api = http().obtener("/api/despliegues/" + plan).getBody();
        assertThat(api).containsEntry("tipo", "reversion").containsEntry("estado", "listo");
        assertThat(((Number) api.get("origenId")).longValue()).isEqualTo(a);
        assertThat(estado(plan)).isEqualTo(EstadoDespliegue.LISTO);
        assertThat(registro.obtener(a).orElseThrow().tipo()).isEqualTo(TipoDespliegue.DESPLIEGUE);
        assertThat(registro.obtener(a).orElseThrow().origenId()).isNull();
    }

    /** El resumen guardado en el plan del origen, leído de la base como mapa (lo que se mostró al crear el origen). */
    private Map<?, ?> resumenDelPlan(long idOrigen) throws Exception {
        var texto = jdbc.queryForObject("select resumen from dep_plan where id = ?", String.class, idOrigen);
        return new com.fasterxml.jackson.databind.ObjectMapper().readValue(texto, Map.class);
    }

    @Test
    void pyRevertirAlQueCorreEsUnPlanSinCambiosQueTambienVence() throws Exception {
        cargar();
        var base = T3.plus(Duration.ofHours(1));
        var a = confirma("py", base, "kyc-pe:1.3.0");
        cierraListo("py", a, base.plusSeconds(60));
        RELOJ.fija(base.plus(Duration.ofMinutes(10)));

        // La ruta no tiene cuerpo: si llega uno, se ignora (el país sale siempre del origen).
        var r = revertir(Long.toString(a), "marta", "{\"pais\":\"co\",\"core\":\"otro-core\",\"versionCore\":\"1.0.0\"}");

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        SIM.nuevasPeticiones();
        var cuerpo = r.getBody();
        assertThat(cuerpo).containsEntry("pais", "py").containsEntry("tipo", "reversion")
                .containsEntry("core", "synkra-core");
        @SuppressWarnings("unchecked")
        var cambios = (Map<String, Object>) cuerpo.get("cambios");
        assertThat(cambios).containsEntry("sinCambios", true).containsEntry("anadidos", List.of())
                .containsEntry("quitados", List.of());
        assertThat(cambios.get("core")).isEqualTo(
                Map.of("antes", CatalogoDePrueba.IMAGEN_A, "despues", CatalogoDePrueba.IMAGEN_A, "igual", true));
        assertThat(cambios.get("iguales")).isEqualTo(
                List.of(Map.of("nombre", "kyc-pe", "version", "1.3.0", "sha256", sha("kyc-pe", "1.3.0"))));
        var plan = id(r);

        RELOJ.fija(Instant.parse((String) cuerpo.get("venceEn")));
        var c = http().confirmar(plan, "luis");

        esperaError(c, HttpStatus.GONE, "PLAN_VENCIDO", "El plan venció sin confirmarse");
        assertThat(SIM.nuevasPeticiones()).isEmpty();
        assertThat(filaPlan(plan)).containsEntry("estado", "vencido");
        assertThat(registro.obtener(plan)).isEmpty();
        assertThat(http().lista("/api/paises/py/despliegues").getBody()).hasSize(1);
    }

    @Test
    void coElOrigenQueNoEstaListoNoSeRevierte() throws Exception {
        cargar();
        var base = T3.plus(Duration.ofHours(2));
        var imagen = CatalogoDePrueba.IMAGEN_A;
        origen(970, "co", base, imagen, "kyc-pe", "1.3.0", sha("kyc-pe", "1.3.0"), false);
        registro.actualizarResultado(970, EstadoDespliegue.FALLIDO, new ContextoFallo("", "", "falló en la prueba"));
        origen(971, "co", base.plusSeconds(1), imagen, "kyc-pe", "1.3.0", sha("kyc-pe", "1.3.0"), false);
        assertThat(estado(970)).isEqualTo(EstadoDespliegue.FALLIDO);
        assertThat(estado(971)).isEqualTo(EstadoDespliegue.APLICANDO);
        RELOJ.fija(base.plusSeconds(10));
        SIM.nuevasPeticiones();

        for (long origen : new long[] {970, 971}) {
            var r = revertir(origen, "marta");
            esperaError(r, HttpStatus.UNPROCESSABLE_ENTITY, "ORIGEN_NO_LISTO", ORIGEN_NO_LISTO);
            sinFilasNiPeticiones("co", 0);
        }

        registro.actualizarResultado(971, EstadoDespliegue.FALLIDO, new ContextoFallo("", "", "cerrado por la prueba"));
    }

    @Test
    void clUnPluginQueYaNoEsCompatibleODesaparecioDaElMismoRechazoDeLaCreacion() throws Exception {
        cargar();
        var base = T3.plus(Duration.ofHours(3));
        origen(972, "cl", base, CatalogoDePrueba.IMAGEN_A, "kyc-pe", "2.0.0", sha("kyc-pe", "2.0.0"), true);
        origen(977, "cl", base.plusSeconds(1), CatalogoDePrueba.IMAGEN_A, "kyc-qq", "1.0.0", "5".repeat(64), true);
        RELOJ.fija(base.plusSeconds(10));
        SIM.nuevasPeticiones();

        var r = revertir(972, "marta");
        var v = http().verificar("synkra-core", "2.1.0", "kyc-pe:2.0.0");
        @SuppressWarnings("unchecked")
        var motivos = (List<Map<String, Object>>) ((Map<String, Object>) ((List<?>) v.getBody().get("plugins")).get(0))
                .get("motivos");
        assertThat(motivos).hasSize(1);
        esperaError(r, HttpStatus.UNPROCESSABLE_ENTITY, "COMBINACION_RECHAZADA", "La combinación no es compatible",
                detalle("plugins.kyc-pe", (String) motivos.get(0).get("mensaje")));
        SIM.nuevasPeticiones();
        sinFilasNiPeticiones("cl", 0);

        var q = revertir(977, "marta");
        assertThat(q.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(q.getBody()).containsEntry("codigo", "COMBINACION_RECHAZADA")
                .containsEntry("mensaje", "La combinación no es compatible");
        @SuppressWarnings("unchecked")
        var detalles = (List<Map<String, String>>) q.getBody().get("detalles");
        assertThat(detalles).hasSize(1);
        assertThat(detalles.get(0)).containsEntry("campo", "plugins.kyc-qq");
        sinFilasNiPeticiones("cl", 0);
    }

    @Test
    void arElCatalogoYaNoTieneLoQueCorrioDaOrigenDesactualizado() throws Exception {
        cargar();
        var base = T3.plus(Duration.ofHours(4));
        origen(973, "ar", base, CatalogoDePrueba.IMAGEN_A, "kyc-pe", "1.3.0", "7".repeat(64), true);
        origen(975, "ar", base.plusSeconds(1), CatalogoDePrueba.IMAGEN_B, "kyc-pe", "1.3.0", sha("kyc-pe", "1.3.0"), true);
        RELOJ.fija(base.plusSeconds(10));
        SIM.nuevasPeticiones();

        for (long origen : new long[] {973, 975}) {
            esperaError(revertir(origen, "marta"), HttpStatus.UNPROCESSABLE_ENTITY, "ORIGEN_DESACTUALIZADO",
                    ORIGEN_DESACTUALIZADO);
            sinFilasNiPeticiones("ar", 0);
        }
    }

    @Test
    void mxUnPaisQueYaNoEstaPermitidoNoSeRevierte() throws Exception {
        cargar();
        var base = T3.plus(Duration.ofHours(4)).plusSeconds(30);
        origen(974, "mx", base, CatalogoDePrueba.IMAGEN_A, "kyc-pe", "1.3.0", sha("kyc-pe", "1.3.0"), true);
        RELOJ.fija(base.plusSeconds(10));
        SIM.nuevasPeticiones();

        esperaError(revertir(974, "marta"), HttpStatus.UNPROCESSABLE_ENTITY, "PAIS_NO_PERMITIDO",
                "El país no está permitido",
                detalle("pais", "El país no está entre los configurados (SYNKRA_PAISES)"));
        sinFilasNiPeticiones("mx", 0);
    }

    @Test
    void ecConfirmarLaReversionConOtroAplicandoEnElPaisDaDespliegueEnCurso() throws Exception {
        cargar();
        var base = T3.plus(Duration.ofHours(5));
        var a = confirma("ec", base, "kyc-pe:1.3.0");
        cierraListo("ec", a, base.plusSeconds(60));
        var c = confirma("ec", base.plus(Duration.ofMinutes(5)), "kyc-co:1.0.0");
        RELOJ.fija(base.plus(Duration.ofMinutes(6)));

        var r = revertir(a, "marta");

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        SIM.nuevasPeticiones();
        var plan = id(r);
        assertThat(estado(c)).isEqualTo(EstadoDespliegue.APLICANDO);

        esperaError(http().confirmar(plan, "luis"), HttpStatus.CONFLICT, "DESPLIEGUE_EN_CURSO",
                "Ya hay un despliegue en curso en el país");
        assertThat(SIM.nuevasPeticiones()).isEmpty();
        assertThat(filaPlan(plan)).containsEntry("estado", "pendiente");
        assertThat(registro.obtener(plan)).isEmpty();

        SIM.marcaFallo("ec", "CrashLoopBackOff");
        RELOJ.fija(base.plus(Duration.ofMinutes(7)));
        assertThat(vigilancia.revisar()).isEqualTo(1);
        assertThat(estado(c)).isEqualTo(EstadoDespliegue.FALLIDO);
        SIM.nuevasPeticiones();
    }

    @Test
    void boUnUsuarioInvalidoSeRechazaAntesDeTodo() throws Exception {
        cargar();
        var base = T3.plus(Duration.ofHours(6));
        origen(976, "bo", base, CatalogoDePrueba.IMAGEN_A, "kyc-pe", "1.3.0", sha("kyc-pe", "1.3.0"), true);
        RELOJ.fija(base.plusSeconds(10));
        SIM.nuevasPeticiones();

        esperaError(revertir(976, "Marta"), HttpStatus.UNPROCESSABLE_ENTITY, "USUARIO_INVALIDO",
                "El usuario no es válido", detalle("X-Synkra-Usuario",
                        "El usuario debe tener de 1 a 64 caracteres: minúsculas, dígitos, punto, guion o guion bajo"));
        sinFilasNiPeticiones("bo", 0);
    }

    @Test
    void unOrigenQueNoExisteODeIdInvalidoDa404Y400() throws Exception {
        cargar();
        SIM.nuevasPeticiones();
        for (var id : List.of("999", "0", "-1")) {
            esperaError(revertir(id, "marta", null), HttpStatus.NOT_FOUND, "NO_ENCONTRADO", "Recurso no encontrado");
        }
        var r = revertir("abc", "marta", null);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(r.getBody()).containsEntry("codigo", "SOLICITUD_INVALIDA");
        assertThat(SIM.nuevasPeticiones()).isEmpty();
        assertThat(jdbc.queryForObject("select count(*) from dep_plan where tipo = 'reversion' and pais in"
                + " ('co', 'cl', 'ar', 'mx', 'bo')", Long.class)).isZero();
    }
}
