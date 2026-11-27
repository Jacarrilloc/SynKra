package io.synkra.catalogo;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.synkra.catalogo.api.CatalogoApi;
import io.synkra.catalogo.api.PluginSubido;
import io.synkra.plataforma.CodigosError;
import io.synkra.plataforma.ExcepcionApi;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** R-C12 (H-05, H-06): el listado y el filtro por etiqueta; los datos entran por {@code subirPlugin}, no por SQL. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ListarPluginsTest {

    private static final String IMAGEN = "localhost:5001/synkra-core@sha256:" + "0".repeat(64);

    @TempDir
    static Path datos;

    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry r) {
        r.add("synkra.data", () -> datos.toString());
    }

    @LocalServerPort int puerto;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper json;
    @Autowired CatalogoApi api;

    private final HttpClient http = HttpClient.newHttpClient();

    @BeforeEach
    void limpiar() throws IOException {
        jdbc.execute("delete from cat_etiqueta");
        jdbc.execute("delete from cat_plugin");
        jdbc.execute("delete from cat_version_core");
        jdbc.execute("delete from cat_core");
        var artefactos = datos.resolve("artefactos");
        if (Files.exists(artefactos)) {
            try (Stream<Path> s = Files.walk(artefactos)) {
                for (var p : s.sorted(Comparator.reverseOrder()).toList()) {
                    Files.delete(p);
                }
            }
        }
        api.registrarCore("synkra-core", "2.1.0", IMAGEN);
        api.registrarCore("otro-core", "2.1.0", IMAGEN);
    }

    private static byte[] jar(String core, String nombre, String version, Map<String, String> etiquetas)
            throws IOException {
        var yaml = new StringBuilder("nombre: " + nombre + "\nversion: " + version + "\ncore: " + core
                + "\ncoreCompatible: 2.x\n");
        if (!etiquetas.isEmpty()) {
            yaml.append("etiquetas:\n");
            etiquetas.forEach((k, v) -> yaml.append("  ").append(k).append(": \"").append(v).append("\"\n"));
        }
        var salida = new ByteArrayOutputStream();
        try (var z = new ZipOutputStream(salida)) {
            z.putNextEntry(new ZipEntry("synkra-plugin.yaml"));
            z.write(yaml.toString().getBytes(StandardCharsets.UTF_8));
            z.closeEntry();
        }
        return salida.toByteArray();
    }

    private void subir(String core, String nombre, String version, Map<String, String> deDescriptor,
            Map<String, String> deSubida) throws IOException {
        api.subirPlugin(core, new ByteArrayInputStream(jar(core, nombre, version, deDescriptor)), deSubida);
    }

    private static Map<String, String> m(String... kv) {
        var r = new LinkedHashMap<String, String>();
        for (int i = 0; i < kv.length; i += 2) {
            r.put(kv[i], kv[i + 1]);
        }
        return r;
    }

    private List<String> ids(List<PluginSubido> l) {
        return l.stream().map(p -> p.nombre() + "@" + p.version()).toList();
    }

    private void datosBase() throws IOException {
        subir("synkra-core", "kyc-pe", "1.10.0", m("pais", "pe"), m());
        subir("synkra-core", "kyc-pe", "1.9.0", m("pais", "pe", "cliente", "x"), m());
        subir("synkra-core", "kyc-co", "1.0.0", m("pais", "co"), m("region", "andina"));
        subir("synkra-core", "aaa", "0.1.0", m(), m("pais", "pe", "cliente", "y"));
    }

    private HttpResponse<byte[]> get(String ruta) throws IOException {
        try {
            return http.send(HttpRequest.newBuilder(URI.create("http://localhost:" + puerto + ruta)).GET().build(),
                    HttpResponse.BodyHandlers.ofByteArray());
        } catch (InterruptedException e) {
            throw new IOException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> lista(HttpResponse<byte[]> r) throws IOException {
        assertThat(r.statusCode()).isEqualTo(200);
        return json.readValue(r.body(), List.class);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> error(HttpResponse<byte[]> r, int estado) throws IOException {
        assertThat(r.statusCode()).isEqualTo(estado);
        return json.readValue(r.body(), Map.class);
    }

    private List<String> camposDe(Map<String, Object> e) {
        var out = new ArrayList<String>();
        for (var d : (List<?>) e.get("detalles")) {
            out.add((String) ((Map<?, ?>) d).get("campo"));
        }
        return out;
    }

    @Test
    void sinFiltroDevuelveTodosEnOrdenPorNombreYVersionSemantica() throws Exception {
        datosBase();
        assertThat(ids(api.listarPlugins("synkra-core", Map.of())))
                .containsExactly("aaa@0.1.0", "kyc-co@1.0.0", "kyc-pe@1.9.0", "kyc-pe@1.10.0");
        assertThat(ids(api.listarPlugins("synkra-core", null))).hasSize(4);
        var web = lista(get("/api/cores/synkra-core/plugins"));
        assertThat(web).extracting(p -> p.get("nombre") + "@" + p.get("version"))
                .containsExactly("aaa@0.1.0", "kyc-co@1.0.0", "kyc-pe@1.9.0", "kyc-pe@1.10.0");
        assertThat(web.get(2).get("etiquetas")).isEqualTo(Map.of("pais", "pe", "cliente", "x"));
    }

    @Test
    void unaEtiquetaFiltraYIncluyeLasQueVinieronDeLaSubida() throws Exception {
        datosBase();
        assertThat(ids(api.listarPlugins("synkra-core", Map.of("pais", "pe"))))
                .containsExactly("aaa@0.1.0", "kyc-pe@1.9.0", "kyc-pe@1.10.0");
        assertThat(lista(get("/api/cores/synkra-core/plugins?pais=pe"))).hasSize(3);
    }

    @Test
    void variasParejasSeAplicanATodasAVez() throws Exception {
        datosBase();
        assertThat(ids(api.listarPlugins("synkra-core", Map.of("pais", "pe", "cliente", "x"))))
                .containsExactly("kyc-pe@1.9.0");
        var web = lista(get("/api/cores/synkra-core/plugins?pais=pe&cliente=x"));
        assertThat(web).hasSize(1);
        assertThat(web.get(0).get("nombre")).isEqualTo("kyc-pe");
    }

    @Test
    void unaEtiquetaDeLaSubidaTambienFiltra() throws Exception {
        datosBase();
        assertThat(ids(api.listarPlugins("synkra-core", Map.of("region", "andina")))).containsExactly("kyc-co@1.0.0");
        assertThat(lista(get("/api/cores/synkra-core/plugins?region=andina"))).hasSize(1);
    }

    @Test
    void sinCoincidenciasDaListaVaciaYLaComparacionDistingueMayusculas() throws Exception {
        datosBase();
        assertThat(api.listarPlugins("synkra-core", Map.of("pais", "mx"))).isEmpty();
        assertThat(api.listarPlugins("synkra-core", Map.of("pais", "PE"))).isEmpty();
        assertThat(api.listarPlugins("synkra-core", Map.of("noexiste", "pe"))).isEmpty();
        assertThat(lista(get("/api/cores/synkra-core/plugins?pais=mx"))).isEmpty();
        assertThat(api.listarPlugins("otro-core", Map.of())).isEmpty();
    }

    @Test
    void unaCarpetaNuncaMuestraLosPluginsDeOtra() throws Exception {
        subir("synkra-core", "kyc-pe", "1.0.0", m("pais", "pe"), m());
        subir("otro-core", "kyc-xx", "1.0.0", m("pais", "pe"), m());
        assertThat(ids(api.listarPlugins("synkra-core", Map.of("pais", "pe")))).containsExactly("kyc-pe@1.0.0");
        assertThat(ids(api.listarPlugins("otro-core", Map.of("pais", "pe")))).containsExactly("kyc-xx@1.0.0");
        assertThat(ids(api.listarPlugins("synkra-core", Map.of()))).containsExactly("kyc-pe@1.0.0");
        assertThat(lista(get("/api/cores/otro-core/plugins")))
                .extracting(p -> p.get("nombre")).containsExactly("kyc-xx");
    }

    @Test
    void unaClaveRepetidaDa422ConElCampoYUnaClaveInvalidaDa422() throws Exception {
        datosBase();
        var rep = error(get("/api/cores/synkra-core/plugins?pais=pe&pais=co"), 422);
        assertThat(rep.get("codigo")).isEqualTo(CodigosError.ENTIDAD_NO_PROCESABLE);
        assertThat(camposDe(rep)).containsExactly("pais");
        var mala = error(get("/api/cores/synkra-core/plugins?Pais=pe"), 422);
        assertThat(mala.get("codigo")).isEqualTo(CodigosError.ENTIDAD_NO_PROCESABLE);
        assertThat(camposDe(mala)).containsExactly("Pais");
        try {
            api.listarPlugins("synkra-core", Map.of("-mala-", "x"));
            throw new AssertionError("debía dar 422");
        } catch (ExcepcionApi e) {
            assertThat(e.estado().value()).isEqualTo(422);
        }
    }

    @Test
    void unCoreInexistenteDa404() throws Exception {
        var r = error(get("/api/cores/no-existe/plugins"), 404);
        assertThat(r.get("codigo")).isEqualTo(CodigosError.NO_ENCONTRADO);
        assertThat(error(get("/api/cores/no-existe/plugins?pais=pe"), 404).get("codigo"))
                .isEqualTo(CodigosError.NO_ENCONTRADO);
    }

    @Test
    void unValorConComillaOPorcentajeNoRompeLaConsultaYSeComparaLiteral() throws Exception {
        subir("synkra-core", "kyc-pe", "1.0.0", m("pais", "pe"), m("nota", "o'brien%"));
        subir("synkra-core", "kyc-co", "1.0.0", m("pais", "co"), m("nota", "otro"));
        assertThat(ids(api.listarPlugins("synkra-core", Map.of("nota", "o'brien%")))).containsExactly("kyc-pe@1.0.0");
        assertThat(lista(get("/api/cores/synkra-core/plugins?nota=o%27brien%25"))).hasSize(1);
        assertThat(api.listarPlugins("synkra-core", Map.of("nota", "%"))).isEmpty();
        assertThat(api.listarPlugins("synkra-core", Map.of("nota", "o%"))).isEmpty();
        assertThat(api.listarPlugins("synkra-core", Map.of("nota", "' OR '1'='1"))).isEmpty();
        assertThat(lista(get("/api/cores/synkra-core/plugins?nota=%27%20OR%20%271%27%3D%271"))).isEmpty();
        assertThat(api.listarPlugins("synkra-core", Map.of())).hasSize(2);
    }
}
