package io.synkra.despliegue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;

import io.synkra.catalogo.api.CatalogoApi;
import io.synkra.kubernetes.api.AdaptadorKubernetes;
import io.synkra.verificador.Descriptor;
import io.synkra.verificador.LectorDescriptor;
import io.synkra.verificador.SuiteCombinaciones;
import io.synkra.verificador.SuiteCombinaciones.Entrada;
import io.synkra.verificador.Verificador;
import io.synkra.verificador.Veredicto;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * U07-T03, R-D2: {@code POST /api/verificaciones} por HTTP contra el catálogo real en SQLite (la suite S1 a S10 subida
 * por {@code CatalogoApi}); sin filas en {@code dep_plan} y sin hablar con Kubernetes.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class VerificacionVivoTest {

    static final Path DATOS = Path.of("build/sqlite-pruebas/VerificacionVivoTest");
    static final String IMAGEN_A = "localhost:5001/synkra-core@sha256:" + "a".repeat(64);
    static final String IMAGEN_B = "localhost:5001/synkra-core@sha256:" + "b".repeat(64);
    static final String IMAGEN_C = "localhost:5001/otro-core@sha256:" + "c".repeat(64);

    private static final String MENSAJE_SELECCION = "La selección no es válida";
    private static final String MOTIVO_CORE = "El core debe cumplir el patrón de nombres (minúsculas, dígitos y guiones)";
    private static final String MOTIVO_VERSION_CORE = "La versión del core debe tener la forma MAYOR.MENOR.PARCHE";
    private static final String MOTIVO_PLUGIN_NULO = "El plugin no puede ser nulo";
    private static final String MOTIVO_NOMBRE = "El nombre del plugin debe cumplir el patrón de nombres (minúsculas, dígitos y guiones)";
    private static final String MOTIVO_VERSION = "La versión del plugin debe tener la forma MAYOR.MENOR.PARCHE";

    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry r) throws IOException {
        if (Files.exists(DATOS)) {
            try (var s = Files.walk(DATOS)) {
                s.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
            }
        }
        Files.createDirectories(DATOS);
        r.add("synkra.data", () -> DATOS.toAbsolutePath().toString());
    }

    @MockitoBean AdaptadorKubernetes adaptador;
    @Autowired CatalogoApi catalogo;
    @Autowired TestRestTemplate http;
    @Autowired JdbcTemplate jdbc;

    private static boolean cargada;

    private synchronized void cargar() throws IOException {
        if (cargada) {
            return;
        }
        for (var c : SuiteCombinaciones.contenido()) {
            for (var v : c.versiones()) {
                var imagen = c.core().equals("otro-core") ? IMAGEN_C : v.toString().equals("2.1.0") ? IMAGEN_A : IMAGEN_B;
                catalogo.registrarCore(c.core(), v.toString(), imagen);
            }
            for (var d : c.plugins()) {
                catalogo.subirPlugin(c.core(), new ByteArrayInputStream(jar(d)), Map.of());
            }
        }
        cargada = true;
    }

    private static byte[] jar(Descriptor d) throws IOException {
        var salida = new ByteArrayOutputStream();
        try (var z = new ZipArchiveOutputStream(salida)) {
            z.putArchiveEntry(new ZipArchiveEntry(LectorDescriptor.ARCHIVO));
            z.write(LectorDescriptor.aYaml(d).getBytes(StandardCharsets.UTF_8));
            z.closeArchiveEntry();
        }
        return salida.toByteArray();
    }

    @SuppressWarnings("rawtypes")
    private org.springframework.http.ResponseEntity<Map> post(String json) {
        var h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        return http.exchange("/api/verificaciones", HttpMethod.POST, new HttpEntity<>(json, h), Map.class);
    }

    private static String cuerpo(io.synkra.verificador.Seleccion s) {
        var plugins = s.plugins().stream()
                .map(d -> "{\"nombre\":\"" + d.nombre() + "\",\"version\":\"" + d.version() + "\"}").toList();
        return "{\"core\":\"" + s.core() + "\",\"versionCore\":\"" + s.versionCore() + "\",\"plugins\":["
                + String.join(",", plugins) + "]}";
    }

    /** El JSON esperado, armado aquí a partir del veredicto en memoria (no del código de producción). */
    private static Map<String, Object> jsonEsperado(Veredicto v) {
        var plugins = new ArrayList<Object>();
        for (var p : v.porPlugin()) {
            var m = new HashMap<String, Object>();
            m.put("nombre", p.descriptor().nombre());
            m.put("version", p.descriptor().version().toString());
            m.put("core", p.descriptor().core());
            m.put("compatible", p.compatible());
            m.put("motivos", motivos(p.motivos()));
            m.put("alternativas", p.alternativas().stream().map(a -> {
                var x = new HashMap<String, Object>();
                x.put("tipo", a.tipo().name());
                x.put("nombre", a.nombre());
                x.put("version", a.version().toString());
                return (Object) x;
            }).toList());
            plugins.add(m);
        }
        var r = new HashMap<String, Object>();
        r.put("global", v.global().name());
        r.put("plugins", plugins);
        r.put("motivosGlobales", motivos(v.motivosGlobales()));
        return r;
    }

    private static List<Object> motivos(List<io.synkra.verificador.Motivo> motivos) {
        return motivos.stream().map(m -> {
            var x = new HashMap<String, Object>();
            x.put("codigo", m.codigo().name());
            x.put("plugin", m.plugin().orElse(null));
            x.put("mensaje", m.mensaje());
            x.put("campo", m.campo().orElse(null));
            return (Object) x;
        }).toList();
    }

    static Stream<SuiteCombinaciones.Caso> casos() {
        return SuiteCombinaciones.casos().stream()
                .filter(c -> List.of("S1", "S2", "S3", "S4", "S5", "S6", "S7", "S10").contains(c.id()));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("casos")
    @SuppressWarnings("unchecked")
    void laSeleccionDaElVeredictoDeLaSuiteConAlternativas(SuiteCombinaciones.Caso caso) throws IOException {
        cargar();
        var seleccion = ((Entrada.PorSeleccion) caso.entrada()).seleccion();
        var memoria = SuiteCombinaciones.catalogo();
        var esperadoVerificador = Verificador.alternativas(seleccion, Verificador.verificar(seleccion, memoria), memoria);

        var r = post(cuerpo(seleccion));

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(r.getBody()).isEqualTo(jsonEsperado(esperadoVerificador));
        // Y ese veredicto es el de la suite: global, motivos (código, plugin, campo, en orden) y alternativas.
        assertThat(r.getBody().get("global")).isEqualTo(caso.esperado().global().orElseThrow().name());
        var motivosJson = new ArrayList<List<Object>>();
        var alternativasJson = new ArrayList<Object>();
        for (var p : (List<Map<String, Object>>) r.getBody().get("plugins")) {
            for (var m : (List<Map<String, Object>>) p.get("motivos")) {
                motivosJson.add(List.of(m.get("codigo"), String.valueOf(m.get("plugin")), String.valueOf(m.get("campo"))));
            }
            alternativasJson.addAll((List<Object>) p.get("alternativas"));
        }
        for (var m : (List<Map<String, Object>>) r.getBody().get("motivosGlobales")) {
            motivosJson.add(List.of(m.get("codigo"), String.valueOf(m.get("plugin")), String.valueOf(m.get("campo"))));
        }
        var motivosSuite = caso.esperado().motivos().stream()
                .map(m -> List.<Object>of(m.codigo().name(), String.valueOf(m.plugin().orElse(null)),
                        String.valueOf(m.campo().orElse(null))))
                .toList();
        assertThat(motivosJson).isEqualTo(motivosSuite);
        assertThat(alternativasJson).isEqualTo(caso.esperado().alternativas().stream().map(a -> {
            var x = new HashMap<String, Object>();
            x.put("tipo", a.tipo().name());
            x.put("nombre", a.nombre());
            x.put("version", a.version().toString());
            return (Object) x;
        }).toList());
    }

    @Test
    @SuppressWarnings("unchecked")
    void s4DaLasDosAlternativasEnOrden() throws IOException {
        cargar();
        var r = post("{\"core\":\"synkra-core\",\"versionCore\":\"2.1.0\",\"plugins\":[{\"nombre\":\"kyc-pe\",\"version\":\"2.0.0\"}]}");
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        var p = ((List<Map<String, Object>>) r.getBody().get("plugins")).get(0);
        assertThat(p.get("alternativas")).isEqualTo(List.of(
                Map.of("tipo", "PLUGIN", "nombre", "kyc-pe", "version", "1.3.0"),
                Map.of("tipo", "CORE", "nombre", "synkra-core", "version", "3.0.0")));
    }

    @Test
    @SuppressWarnings("unchecked")
    void s7DaCoreEquivocadoYS6PluginAusente() throws IOException {
        cargar();
        var s7 = post("{\"core\":\"synkra-core\",\"versionCore\":\"2.1.0\",\"plugins\":[{\"nombre\":\"kyc-xx\",\"version\":\"1.0.0\"}]}");
        var s6 = post("{\"core\":\"synkra-core\",\"versionCore\":\"2.1.0\",\"plugins\":[{\"nombre\":\"kyc-mx\",\"version\":\"1.0.0\"}]}");
        assertThat(((List<Map<String, Object>>) ((Map<String, Object>) ((List<?>) s7.getBody().get("plugins")).get(0)).get("motivos"))
                .get(0).get("codigo")).isEqualTo("CORE_EQUIVOCADO");
        assertThat(((List<Map<String, Object>>) ((Map<String, Object>) ((List<?>) s6.getBody().get("plugins")).get(0)).get("motivos"))
                .get(0).get("codigo")).isEqualTo("PLUGIN_AUSENTE");
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, String>> detalles(Map<?, ?> cuerpo) {
        return (List<Map<String, String>>) cuerpo.get("detalles");
    }

    @Test
    void laSeleccionMalFormadaDa422ConTodosLosCamposYSinRepetirLoRecibido() throws IOException {
        cargar();
        var r = post("{\"core\":\"Synkra\",\"versionCore\":\"2.1\",\"plugins\":[{\"nombre\":\"KYC\",\"version\":\"x\"},null]}");

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(r.getBody().get("codigo")).isEqualTo("SELECCION_INVALIDA");
        assertThat(r.getBody().get("mensaje")).isEqualTo(MENSAJE_SELECCION);
        assertThat(detalles(r.getBody())).containsExactly(
                Map.of("campo", "core", "motivo", MOTIVO_CORE),
                Map.of("campo", "versionCore", "motivo", MOTIVO_VERSION_CORE),
                Map.of("campo", "plugins[0].nombre", "motivo", MOTIVO_NOMBRE),
                Map.of("campo", "plugins[0].version", "motivo", MOTIVO_VERSION),
                Map.of("campo", "plugins[1]", "motivo", MOTIVO_PLUGIN_NULO));
        var texto = r.getBody().toString();
        for (var recibido : List.of("Synkra", "KYC", "2.1,")) {
            assertThat(texto).doesNotContain(recibido);
        }
    }

    @Test
    void sinCamposDaCoreYVersionCoreInvalidosYUnCoreNuloNoRompe() throws IOException {
        cargar();
        var r = post("{}");
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(detalles(r.getBody())).containsExactly(
                Map.of("campo", "core", "motivo", MOTIVO_CORE),
                Map.of("campo", "versionCore", "motivo", MOTIVO_VERSION_CORE));
    }

    @Test
    void coreNoRegistradoOVersionNoRegistradaDa422() throws IOException {
        cargar();
        var sin = post("{\"core\":\"no-existe\",\"versionCore\":\"1.0.0\",\"plugins\":[]}");
        assertThat(sin.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(sin.getBody().get("codigo")).isEqualTo("CORE_NO_REGISTRADO");
        assertThat(sin.getBody().get("mensaje")).isEqualTo("El core no está registrado");
        assertThat(detalles(sin.getBody())).containsExactly(
                Map.of("campo", "core", "motivo", "No hay ningún core con ese nombre en el catálogo"));

        var version = post("{\"core\":\"synkra-core\",\"versionCore\":\"9.9.9\",\"plugins\":null}");
        assertThat(version.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(version.getBody().get("codigo")).isEqualTo("CORE_NO_REGISTRADO");
        assertThat(detalles(version.getBody())).containsExactly(
                Map.of("campo", "versionCore", "motivo", "El core no tiene registrada esa versión"));
        assertThat(sin.getBody().toString() + version.getBody()).doesNotContain("no-existe").doesNotContain("9.9.9");
    }

    @Test
    void pluginsNuloEsCoreSinPlugins() throws IOException {
        cargar();
        var r = post("{\"core\":\"synkra-core\",\"versionCore\":\"2.1.0\"}");
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(r.getBody().get("global")).isEqualTo("APROBADA");
        assertThat(r.getBody().get("plugins")).isEqualTo(List.of());
    }

    @Test
    void nadaTocaDepPlanNiKubernetes() throws IOException {
        cargar();
        post(cuerpo(((Entrada.PorSeleccion) SuiteCombinaciones.casos().get(3).entrada()).seleccion()));
        post("{}");
        post("{\"core\":\"no-existe\",\"versionCore\":\"1.0.0\"}");
        assertThat(jdbc.queryForObject("select count(*) from dep_plan", Long.class)).isZero();
        verifyNoInteractions(adaptador);
    }
}
