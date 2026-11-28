package io.synkra.despliegue;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.synkra.catalogo.api.CatalogoApi;
import io.synkra.kubernetes.SimuladorKubernetes;
import io.synkra.kubernetes.api.AdaptadorKubernetes;
import io.synkra.kubernetes.api.PlanPais;
import io.synkra.kubernetes.api.PlanPais.PluginPlan;
import io.synkra.verificador.Descriptor;
import io.synkra.verificador.LectorDescriptor;
import io.synkra.verificador.SuiteCombinaciones;
import io.synkra.verificador.Verificador;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
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
 * U07-T04, R-D3 y R-D4: {@code POST /api/despliegues} contra el catálogo real y contra el servidor simulado de Fabric8
 * (lo que "corre" se lee del clúster con {@code leerPais}). Cada caso usa su propio país; la base se lee de vuelta
 * desde el archivo.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CrearPlanTest {

    static final Path DATOS = Path.of("build/sqlite-pruebas/CrearPlanTest");
    static final String IMAGEN_A = "localhost:5001/synkra-core@sha256:" + "a".repeat(64);
    static final String IMAGEN_B = "localhost:5001/synkra-core@sha256:" + "b".repeat(64);
    static final String IMAGEN_C = "localhost:5001/otro-core@sha256:" + "c".repeat(64);
    static final String SHA_VIEJO = "9".repeat(64);
    static final ObjectMapper JSON = new ObjectMapper();

    static final SimuladorKubernetes SIM = new SimuladorKubernetes();

    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry r) throws IOException {
        if (Files.exists(DATOS)) {
            try (var s = Files.walk(DATOS)) {
                s.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
            }
        }
        Files.createDirectories(DATOS);
        r.add("synkra.data", () -> DATOS.toAbsolutePath().toString());
        r.add("synkra.paises", () -> "pe,co,cl,ar,uy,bo");
    }

    @TestBean AdaptadorKubernetes adaptador;

    static AdaptadorKubernetes adaptador() {
        return SIM.adaptador();
    }

    @AfterAll
    static void cierra() {
        SIM.close();
    }

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

    private String sha(String plugin, String version) {
        return catalogo.obtenerPlugin("synkra-core", plugin, version).sha256();
    }

    @SuppressWarnings("rawtypes")
    private org.springframework.http.ResponseEntity<Map> post(String json) {
        var h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        return http.exchange("/api/despliegues", HttpMethod.POST, new HttpEntity<>(json, h), Map.class);
    }

    private static String cuerpo(String pais, String version, String... plugins) {
        var lista = new java.util.ArrayList<String>();
        for (var p : plugins) {
            var partes = p.split(":");
            lista.add("{\"nombre\":\"" + partes[0] + "\",\"version\":\"" + partes[1] + "\"}");
        }
        return "{\"pais\":\"" + pais + "\",\"core\":\"synkra-core\",\"versionCore\":\"" + version + "\",\"plugins\":["
                + String.join(",", lista) + "]}";
    }

    private long filas() {
        return jdbc.queryForObject("select count(*) from dep_plan", Long.class);
    }

    private static void soloGet(List<String> peticiones) {
        assertThat(peticiones).isNotEmpty().allSatisfy(p -> assertThat(p).startsWith("GET "));
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, String>> detalles(Map<?, ?> cuerpo) {
        return (List<Map<String, String>>) cuerpo.get("detalles");
    }

    private JsonNode fila(String pais, String columna) throws Exception {
        return JSON.readTree(jdbc.queryForObject("select " + columna + " from dep_plan where pais = ?", String.class, pais));
    }

    @Test
    void paisVacioCreaUnPlanPendienteConTodoAnadido() throws Exception {
        cargar();
        SIM.nuevasPeticiones();
        var antes = filas();

        var r = post(cuerpo("pe", "2.1.0", "kyc-pe:1.3.0"));

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(r.getHeaders().getLocation()).isNull();
        soloGet(SIM.nuevasPeticiones());
        assertThat(filas()).isEqualTo(antes + 1);
        var b = r.getBody();
        assertThat(b.get("estado")).isEqualTo("pendiente");
        assertThat(b.get("tipo")).isEqualTo("despliegue");
        assertThat(b.get("pais")).isEqualTo("pe");
        assertThat(b.get("core")).isEqualTo("synkra-core");
        assertThat(b.get("versionCore")).isEqualTo("2.1.0");
        assertThat(b.get("creadoPor")).isEqualTo("anonimo");
        assertThat(b.get("id")).isNotNull();
        assertThat(Duration.between(Instant.parse((String) b.get("creadoEn")), Instant.parse((String) b.get("venceEn"))))
                .isEqualTo(Duration.ofMinutes(30));
        var esperadoPlugin = Map.of("nombre", "kyc-pe", "version", "1.3.0", "sha256", sha("kyc-pe", "1.3.0"));
        assertThat(b.get("resumen")).isEqualTo(Map.of("namespace", "synkra-pe", "core", "synkra-core",
                "versionCore", "2.1.0", "imagen", IMAGEN_A, "plugins", List.of(esperadoPlugin)));
        var core = new java.util.HashMap<String, Object>();
        core.put("antes", null);
        core.put("despues", IMAGEN_A);
        core.put("igual", false);
        assertThat(b.get("cambios")).isEqualTo(Map.of("core", core, "anadidos", List.of(esperadoPlugin),
                "quitados", List.of(), "iguales", List.of(), "sinCambios", false));
        // Lo que se guardó es lo que se respondió.
        assertThat(fila("pe", "resumen")).isEqualTo(JSON.valueToTree(b.get("resumen")));
        assertThat(fila("pe", "cambios")).isEqualTo(JSON.valueToTree(b.get("cambios")));
        assertThat(fila("pe", "plugins")).isEqualTo(JSON.readTree("[{\"nombre\":\"kyc-pe\",\"version\":\"1.3.0\"}]"));
    }

    @Test
    void cambiaUnPluginYSeQuedaOtroDistingueAnadidosQuitadosEIguales() throws Exception {
        cargar();
        SIM.siembra(new PlanPais("co", IMAGEN_A, List.of(new PluginPlan("kyc-co", "1.0.0", sha("kyc-co", "1.0.0")),
                new PluginPlan("kyc-viejo", "0.9.0", SHA_VIEJO))));

        var r = post(cuerpo("co", "2.1.0", "kyc-pe:1.3.0", "kyc-co:1.0.0"));

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        soloGet(SIM.nuevasPeticiones());
        // F-01: se pidieron al revés (kyc-pe antes que kyc-co); resumen y columna van ordenados por nombre.
        @SuppressWarnings("unchecked")
        var resumenPlugins = (List<Map<String, String>>) ((Map<String, Object>) r.getBody().get("resumen")).get("plugins");
        assertThat(resumenPlugins.stream().map(p -> p.get("nombre")).toList()).containsExactly("kyc-co", "kyc-pe");
        assertThat(fila("co", "resumen").get("plugins")).isEqualTo(JSON.valueToTree(resumenPlugins));
        assertThat(fila("co", "plugins")).isEqualTo(JSON.readTree(
                "[{\"nombre\":\"kyc-co\",\"version\":\"1.0.0\"},{\"nombre\":\"kyc-pe\",\"version\":\"1.3.0\"}]"));
        var cambios = fila("co", "cambios");
        assertThat(cambios.get("anadidos")).isEqualTo(JSON.readTree(
                "[{\"nombre\":\"kyc-pe\",\"version\":\"1.3.0\",\"sha256\":\"" + sha("kyc-pe", "1.3.0") + "\"}]"));
        assertThat(cambios.get("quitados")).isEqualTo(JSON.readTree(
                "[{\"nombre\":\"kyc-viejo\",\"version\":\"0.9.0\",\"sha256\":\"" + SHA_VIEJO + "\"}]"));
        assertThat(cambios.get("iguales")).isEqualTo(JSON.readTree(
                "[{\"nombre\":\"kyc-co\",\"version\":\"1.0.0\",\"sha256\":\"" + sha("kyc-co", "1.0.0") + "\"}]"));
        assertThat(cambios.get("core").get("antes").asText()).isEqualTo(IMAGEN_A);
        assertThat(cambios.get("core").get("igual").asBoolean()).isTrue();
        assertThat(cambios.get("sinCambios").asBoolean()).isFalse();
    }

    @Test
    void unPlanIdenticoALoQueCorreSeCreaIgualConSinCambios() throws Exception {
        cargar();
        SIM.siembra(new PlanPais("cl", IMAGEN_A, List.of(new PluginPlan("kyc-pe", "1.3.0", sha("kyc-pe", "1.3.0")))));

        var r = post(cuerpo("cl", "2.1.0", "kyc-pe:1.3.0"));

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        soloGet(SIM.nuevasPeticiones());
        @SuppressWarnings("unchecked")
        var cambios = (Map<String, Object>) r.getBody().get("cambios");
        assertThat(cambios.get("sinCambios")).isEqualTo(true);
        assertThat(cambios.get("anadidos")).isEqualTo(List.of());
        assertThat(cambios.get("quitados")).isEqualTo(List.of());
    }

    @Test
    void unaCombinacionRechazadaDa422SinFilaNiPeticionesAKubernetes() throws Exception {
        cargar();
        SIM.nuevasPeticiones();
        var antes = filas();
        var seleccion = ((SuiteCombinaciones.Entrada.PorSeleccion) SuiteCombinaciones.casos().get(3).entrada()).seleccion();
        var memoria = SuiteCombinaciones.catalogo();
        var mensaje = Verificador.alternativas(seleccion, Verificador.verificar(seleccion, memoria), memoria)
                .porPlugin().get(0).motivos().get(0).mensaje();

        var r = post(cuerpo("ar", "2.1.0", "kyc-pe:2.0.0"));

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(r.getBody().get("codigo")).isEqualTo("COMBINACION_RECHAZADA");
        assertThat(r.getBody().get("mensaje")).isEqualTo("La combinación no es compatible");
        assertThat(detalles(r.getBody())).containsExactly(Map.of("campo", "plugins.kyc-pe", "motivo", mensaje));
        assertThat(SIM.nuevasPeticiones()).isEmpty();
        assertThat(filas()).isEqualTo(antes);
    }

    @Test
    void unPaisConSoloElDeploymentDa409SinFila() {
        try {
            cargar();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        SIM.siembraSoloDeployment(new PlanPais("uy", IMAGEN_A, List.of()));
        var antes = filas();

        var r = post(cuerpo("uy", "2.1.0", "kyc-pe:1.3.0"));

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(r.getBody().get("codigo")).isEqualTo("PAIS_INCONSISTENTE");
        assertThat(r.getBody().get("mensaje")).isEqualTo("Lo que corre en el país no es consistente");
        assertThat(detalles(r.getBody())).isEmpty();
        assertThat(r.getBody().toString()).doesNotContain("synkra-core-config").doesNotContain("uy");
        soloGet(SIM.nuevasPeticiones());
        assertThat(filas()).isEqualTo(antes);
    }

    @Test
    void unaLecturaQueFallaDa503SinFila() throws Exception {
        cargar();
        SIM.fallaLecturas("bo");
        SIM.nuevasPeticiones();
        var antes = filas();

        var r = post(cuerpo("bo", "2.1.0", "kyc-pe:1.3.0"));

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(r.getBody().get("codigo")).isEqualTo("KUBERNETES_NO_DISPONIBLE");
        assertThat(r.getBody().get("mensaje")).isEqualTo("No se puede leer lo que corre en el país");
        assertThat(detalles(r.getBody())).isEmpty();
        assertThat(r.getBody().toString()).doesNotContain("bo/").doesNotContain("synkra-bo");
        soloGet(SIM.nuevasPeticiones());
        assertThat(filas()).isEqualTo(antes);
    }

    @Test
    void unPaisNoPermitidoDa422AunqueLaSeleccionTambienSeaInvalidaSinPeticiones() throws Exception {
        cargar();
        SIM.nuevasPeticiones();
        var antes = filas();

        var r = post(cuerpo("mx", "2.1.0", "kyc-pe:2.0.0"));
        var mal = post("{\"pais\":\"mx\",\"core\":\"Synkra\"}");

        for (var x : List.of(r, mal)) {
            assertThat(x.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
            assertThat(x.getBody().get("codigo")).isEqualTo("PAIS_NO_PERMITIDO");
            assertThat(detalles(x.getBody())).containsExactly(
                    Map.of("campo", "pais", "motivo", "El país no está entre los configurados (SYNKRA_PAISES)"));
        }
        assertThat(SIM.nuevasPeticiones()).isEmpty();
        assertThat(filas()).isEqualTo(antes);
    }

    @Test
    void unaSeleccionMalFormadaDa422SinFilaNiPeticiones() throws Exception {
        cargar();
        SIM.nuevasPeticiones();
        var antes = filas();

        var r = post("{\"pais\":\"pe\",\"core\":\"Synkra\",\"versionCore\":\"2.1\",\"plugins\":[{\"nombre\":\"KYC\",\"version\":\"x\"},null]}");

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(r.getBody().get("codigo")).isEqualTo("SELECCION_INVALIDA");
        assertThat(r.getBody().get("mensaje")).isEqualTo("La selección no es válida");
        assertThat(detalles(r.getBody()).stream().map(d -> d.get("campo")).toList())
                .containsExactly("core", "versionCore", "plugins[0].nombre", "plugins[0].version", "plugins[1]");
        assertThat(SIM.nuevasPeticiones()).isEmpty();
        assertThat(filas()).isEqualTo(antes);
    }

    @Test
    void unCoreNoRegistradoDa422SinFilaNiPeticiones() throws Exception {
        cargar();
        SIM.nuevasPeticiones();
        var antes = filas();

        var r = post("{\"pais\":\"pe\",\"core\":\"synkra-core\",\"versionCore\":\"9.9.9\"}");

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(r.getBody().get("codigo")).isEqualTo("CORE_NO_REGISTRADO");
        assertThat(SIM.nuevasPeticiones()).isEmpty();
        assertThat(filas()).isEqualTo(antes);
    }
}
