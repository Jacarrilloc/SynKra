package io.synkra.catalogo;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.synkra.plataforma.CodigosError;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
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

/** H-02: las rutas de subida y consulta de plugins contra el contenedor real, SQLite y disco reales. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RutasPluginsTest {

    private static final String IMAGEN = "localhost:5001/synkra-core@sha256:" + "0".repeat(64);
    private static final int MAX_ARTEFACTO = 16 * 1024;
    private static final int MAX_JSON = 64 * 1024;
    private static final String BASE = "/api/cores/synkra-core/plugins";

    @TempDir
    static Path datos;

    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry r) {
        r.add("synkra.data", () -> datos.toString());
        r.add("synkra.max-artefacto", () -> MAX_ARTEFACTO + "B");
    }

    @LocalServerPort int puerto;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper json;

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
        var r = enviar("POST", "/api/cores", "application/json", (
                "{\"nombre\":\"synkra-core\",\"version\":\"2.1.0\",\"imagen\":\"" + IMAGEN + "\"}")
                        .getBytes(StandardCharsets.UTF_8));
        assertThat(r.statusCode()).isEqualTo(201);
    }

    // ---------- ayudas ----------

    private HttpResponse<byte[]> enviar(String metodo, String ruta, String tipo, byte[] cuerpo) throws IOException {
        return enviar(metodo, ruta, tipo, HttpRequest.BodyPublishers.ofByteArray(cuerpo));
    }

    private HttpResponse<byte[]> enviar(String metodo, String ruta, String tipo, HttpRequest.BodyPublisher cuerpo)
            throws IOException {
        var b = HttpRequest.newBuilder(URI.create("http://localhost:" + puerto + ruta)).method(metodo, cuerpo);
        if (tipo != null) {
            b.header("Content-Type", tipo);
        }
        try {
            return http.send(b.build(), HttpResponse.BodyHandlers.ofByteArray());
        } catch (InterruptedException e) {
            throw new IOException(e);
        }
    }

    private HttpResponse<byte[]> obtener(String ruta) throws IOException {
        return enviar("GET", ruta, null, HttpRequest.BodyPublishers.noBody());
    }

    /** Una parte multipart: {@code archivo} no nulo la hace parte de archivo. */
    private record Parte(String nombre, String archivo, byte[] contenido) {}

    private HttpResponse<byte[]> subir(String ruta, Parte... partes) throws IOException {
        var limite = "----synkra" + System.nanoTime();
        var o = new ByteArrayOutputStream();
        for (var p : partes) {
            var cab = "--" + limite + "\r\nContent-Disposition: form-data; name=\"" + p.nombre() + "\""
                    + (p.archivo() == null ? "" : "; filename=\"" + p.archivo() + "\"") + "\r\n\r\n";
            o.writeBytes(cab.getBytes(StandardCharsets.UTF_8));
            o.writeBytes(p.contenido());
            o.writeBytes("\r\n".getBytes(StandardCharsets.UTF_8));
        }
        o.writeBytes(("--" + limite + "--\r\n").getBytes(StandardCharsets.UTF_8));
        return enviar("POST", ruta, "multipart/form-data; boundary=" + limite, o.toByteArray());
    }

    private static Parte archivo(byte[] jar) {
        return new Parte("archivo", "kyc-pe-1.3.0.jar", jar);
    }

    private static Parte etiquetas(String texto) {
        return new Parte("etiquetas", null, texto.getBytes(StandardCharsets.UTF_8));
    }

    private static byte[] jar(String version) throws IOException {
        var yaml = "nombre: kyc-pe\nversion: " + version + "\ncore: synkra-core\ncoreCompatible: 2.x\n"
                + "etiquetas:\n  pais: pe\n";
        var salida = new ByteArrayOutputStream();
        try (var z = new ZipOutputStream(salida)) {
            z.putNextEntry(new ZipEntry("synkra-plugin.yaml"));
            z.write(yaml.getBytes(StandardCharsets.UTF_8));
            z.closeEntry();
        }
        return salida.toByteArray();
    }

    private static String sha(byte[] b) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(b));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> mapa(HttpResponse<byte[]> r) throws IOException {
        return json.readValue(r.body(), Map.class);
    }

    @SuppressWarnings("unchecked")
    private List<String> campos(HttpResponse<byte[]> r) throws IOException {
        var out = new ArrayList<String>();
        for (var d : (List<Map<String, String>>) mapa(r).get("detalles")) {
            out.add(d.get("campo"));
        }
        return out;
    }

    private void assertErrorApi(HttpResponse<byte[]> r, int estado, String codigo) throws IOException {
        assertThat(r.statusCode()).isEqualTo(estado);
        assertThat(r.headers().firstValue("Content-Type").orElse("")).startsWith("application/json");
        assertThat(mapa(r).get("codigo")).isEqualTo(codigo);
    }

    private long archivosEnDisco() throws IOException {
        var raiz = datos.resolve("artefactos");
        if (!Files.exists(raiz)) {
            return 0;
        }
        try (Stream<Path> s = Files.walk(raiz)) {
            return s.filter(Files::isRegularFile).count();
        }
    }

    // ---------- pruebas ----------

    @Test
    void laSubidaDa201ConLocationYSha256YElGetDevuelveLoMismo() throws Exception {
        var bytes = jar("1.3.0");
        var r = subir(BASE, archivo(bytes), etiquetas("{\"region\":\"andina\"}"));
        assertThat(r.statusCode()).isEqualTo(201);
        assertThat(r.headers().firstValue("Location")).contains(BASE + "/kyc-pe/1.3.0");
        var cuerpo = mapa(r);
        assertThat(cuerpo.get("sha256")).isEqualTo(sha(bytes));
        assertThat(cuerpo.get("sha256").toString()).matches("[0-9a-f]{64}");
        assertThat(cuerpo.get("nombre")).isEqualTo("kyc-pe");
        assertThat(cuerpo.get("version")).isEqualTo("1.3.0");
        assertThat(cuerpo.get("etiquetas")).isEqualTo(Map.of("pais", "pe", "region", "andina"));

        var g = obtener(BASE + "/kyc-pe/1.3.0");
        assertThat(g.statusCode()).isEqualTo(200);
        assertThat(mapa(g)).isEqualTo(cuerpo);
    }

    @Test
    void sinLaParteEtiquetasEsUnMapaVacio() throws Exception {
        var r = subir(BASE, archivo(jar("1.3.0")));
        assertThat(r.statusCode()).isEqualTo(201);
        assertThat(mapa(r).get("etiquetas")).isEqualTo(Map.of("pais", "pe"));
    }

    @Test
    void sinArchivoDa422ConCampoArchivo() throws Exception {
        var r = subir(BASE, etiquetas("{}"));
        assertErrorApi(r, 422, CodigosError.ENTIDAD_NO_PROCESABLE);
        assertThat(campos(r)).containsExactly("archivo");
    }

    @Test
    void etiquetasQueNoSonJsonDan422() throws Exception {
        var r = subir(BASE, archivo(jar("1.3.0")), etiquetas("esto no es json"));
        assertErrorApi(r, 422, CodigosError.ENTIDAD_NO_PROCESABLE);
        assertThat(campos(r)).containsExactly("etiquetas");
        assertThat(archivosEnDisco()).isZero();
    }

    @Test
    void etiquetasConValorNumericoDan422() throws Exception {
        var r = subir(BASE, archivo(jar("1.3.0")), etiquetas("{\"region\":5}"));
        assertErrorApi(r, 422, CodigosError.ENTIDAD_NO_PROCESABLE);
        assertThat(campos(r)).containsExactly("etiquetas");
    }

    @Test
    void unValorDe64CaracteresDa422ConLaClaveYUnoDe63SeAcepta() throws Exception {
        var largo = subir(BASE, archivo(jar("1.3.0")), etiquetas("{\"region\":\"" + "a".repeat(64) + "\"}"));
        assertErrorApi(largo, 422, CodigosError.ENTIDAD_NO_PROCESABLE);
        assertThat(campos(largo)).containsExactly("etiquetas.region");
        assertThat(archivosEnDisco()).isZero();

        var justo = subir(BASE, archivo(jar("1.3.0")), etiquetas("{\"region\":\"" + "a".repeat(63) + "\"}"));
        assertThat(justo.statusCode()).isEqualTo(201);
    }

    @Test
    void elTextoDelMotivoDeValorLargoEstaFijado() {
        assertThat(MotivosCatalogo.ETIQUETA_VALOR_LARGO)
                .isEqualTo("El valor de la etiqueta '%s' supera los 63 caracteres");
    }

    @Test
    void unJarDeLimiteMasUnoDa413ConErrorApiYNadaEnDisco() throws Exception {
        var r = subir(BASE, archivo(new byte[MAX_ARTEFACTO + 1]));
        assertErrorApi(r, 413, CodigosError.DEMASIADO_GRANDE);
        assertThat(archivosEnDisco()).isZero();
        var tmp = datos.resolve("artefactos").resolve(".tmp");
        if (Files.exists(tmp)) {
            try (Stream<Path> s = Files.list(tmp)) {
                assertThat(s.toList()).isEmpty();
            }
        }
    }

    @Test
    void unCuerpoMayorQueElMaximoDeLaPeticionDa413ConErrorApiNoHtml() throws Exception {
        var r = subir(BASE, archivo(new byte[1_300_000]));
        assertErrorApi(r, 413, CodigosError.DEMASIADO_GRANDE);
        assertThat(archivosEnDisco()).isZero();
    }

    @Test
    void unaParteEtiquetasDeMasDe64KbDa413() throws Exception {
        var grande = "{\"region\":\"" + "a".repeat(MAX_JSON) + "\"}";
        var r = subir(BASE, archivo(jar("1.3.0")), etiquetas(grande));
        assertErrorApi(r, 413, CodigosError.DEMASIADO_GRANDE);
        assertThat(archivosEnDisco()).isZero();
    }

    /** Un JSON de {@code bytes} bytes con el relleno dentro de la imagen: no se puede dejar sin leer. */
    private static byte[] jsonLleno(int bytes) {
        var base = "{\"nombre\":\"synkra-core\",\"version\":\"2.2.0\",\"imagen\":\"\"}";
        return base.replace("\"imagen\":\"\"", "\"imagen\":\"" + "a".repeat(bytes - base.length()) + "\"")
                .getBytes(StandardCharsets.UTF_8);
    }

    private static byte[] jsonDeTamano(int bytes) {
        var base = "{\"nombre\":\"synkra-core\",\"version\":\"2.2.0\",\"imagen\":\"" + IMAGEN + "\"}";
        return (base + " ".repeat(bytes - base.length())).getBytes(StandardCharsets.UTF_8);
    }

    @Test
    void unJsonDe65KbEnPostCoresDa413ConErrorApi() throws Exception {
        var r = enviar("POST", "/api/cores", "application/json", jsonDeTamano(65 * 1024));
        assertErrorApi(r, 413, CodigosError.DEMASIADO_GRANDE);
        assertThat(obtener("/api/cores/synkra-core").statusCode()).isEqualTo(200);
        assertThat(mapa(obtener("/api/cores/synkra-core")).get("versiones").toString()).doesNotContain("2.2.0");
    }

    @Test
    void unJsonLlenoDe65KbDa413() throws Exception {
        var r = enviar("POST", "/api/cores", "application/json", jsonLleno(65 * 1024));
        assertErrorApi(r, 413, CodigosError.DEMASIADO_GRANDE);
    }

    @Test
    void unJsonLlenoDeExactamente64KbLlegaAlServicio() throws Exception {
        var r = enviar("POST", "/api/cores", "application/json", jsonLleno(MAX_JSON));
        assertErrorApi(r, 422, CodigosError.ENTIDAD_NO_PROCESABLE);
    }

    @Test
    void unJsonDeExactamente64KbSePasa() throws Exception {
        var r = enviar("POST", "/api/cores", "application/json", jsonDeTamano(MAX_JSON));
        assertThat(r.statusCode()).isEqualTo(201);
    }

    @Test
    void unJsonSinContentLengthDe65KbDa413() throws Exception {
        InputStream flujo = new ByteArrayInputStream(jsonLleno(65 * 1024));
        var r = enviar("POST", "/api/cores", "application/json", HttpRequest.BodyPublishers.ofInputStream(() -> flujo));
        assertErrorApi(r, 413, CodigosError.DEMASIADO_GRANDE);
    }

    /** F-01: el tope no depende de cómo se escriba la ruta. */
    @Test
    void elTopeDeJsonValeConParametrosDeRutaYPercentEncoding() throws Exception {
        for (var ruta : List.of("/api/cores;x=1", "/api/%63ores", "/api/cores/")) {
            var r = enviar("POST", ruta, "application/json", jsonDeTamano(65 * 1024));
            assertThat(r.statusCode()).as(ruta).isEqualTo(413);
            var llena = enviar("POST", ruta, "application/json", jsonLleno(65 * 1024));
            assertThat(llena.statusCode()).as(ruta).isEqualTo(413);
        }
    }

    /** F-02: el relleno después del objeto también cuenta, aunque no haya Content-Length. */
    @Test
    void unJsonChunkedDe65KbConRellenoFinalDa413YNoSeRegistra() throws Exception {
        InputStream flujo = new ByteArrayInputStream(jsonDeTamano(65 * 1024));
        var r = enviar("POST", "/api/cores", "application/json", HttpRequest.BodyPublishers.ofInputStream(() -> flujo));
        assertErrorApi(r, 413, CodigosError.DEMASIADO_GRANDE);
        assertThat(mapa(obtener("/api/cores/synkra-core")).get("versiones").toString()).doesNotContain("2.2.0");
    }

    @Test
    void unJsonChunkedDeExactamente64KbConRellenoSePasa() throws Exception {
        InputStream flujo = new ByteArrayInputStream(jsonDeTamano(MAX_JSON));
        var r = enviar("POST", "/api/cores", "application/json", HttpRequest.BodyPublishers.ofInputStream(() -> flujo));
        assertThat(r.statusCode()).isEqualTo(201);
    }

    /** F-07: el 413 de un JSON habla de cuerpo, no de archivo. */
    @Test
    void elMensajeDelJsonGrandeHablaDelCuerpo() throws Exception {
        var r = enviar("POST", "/api/cores", "application/json", jsonDeTamano(65 * 1024));
        assertThat(mapa(r).get("mensaje").toString()).doesNotContain("archivo").contains("cuerpo");
    }

    /** F-04: los límites de multipart del contexto salen de synkra.max-artefacto. */
    @Autowired jakarta.servlet.MultipartConfigElement multipart;

    @Test
    void losLimitesDeMultipartSalenDeMaxArtefacto() {
        assertThat(multipart.getMaxFileSize()).isEqualTo(MAX_ARTEFACTO + 1024 * 1024L);
        assertThat(multipart.getMaxRequestSize()).isGreaterThan(multipart.getMaxFileSize());
        assertThat(multipart.getFileSizeThreshold()).isZero();
    }

    /** F-04: un archivo entre el límite del artefacto y el de Tomcat da el 413 de R-C5, con su motivo. */
    @Test
    void unArchivoPorEncimaDelArtefactoYDebajoDeTomcatDa413DeRC5() throws Exception {
        var r = subir(BASE, archivo(new byte[500_000]));
        assertErrorApi(r, 413, CodigosError.DEMASIADO_GRANDE);
        assertThat(mapa(r).get("detalles").toString()).contains("16 KB");
    }

    /** F-05: el tope cuenta caracteres, no unidades UTF-16. */
    @Test
    void sesentaYTresEmojisSeAceptanYSesentaYCuatroNo() throws Exception {
        var e = "\uD83D\uDE00";
        assertThat(subir(BASE, archivo(jar("1.3.0")), etiquetas("{\"region\":\"" + e.repeat(63) + "\"}")).statusCode())
                .isEqualTo(201);
        var r = subir(BASE, archivo(jar("1.4.0")), etiquetas("{\"region\":\"" + e.repeat(64) + "\"}"));
        assertThat(r.statusCode()).isEqualTo(422);
    }

    /** F-06: basura después del objeto de etiquetas. */
    @Test
    void etiquetasConBasuraAlFinalDan422() throws Exception {
        var r = subir(BASE, archivo(jar("1.3.0")), etiquetas("{\"region\":\"x\"} basura"));
        assertErrorApi(r, 422, CodigosError.ENTIDAD_NO_PROCESABLE);
        assertThat(campos(r)).containsExactly("etiquetas");
    }

    @Test
    void unCoreInexistenteDa404() throws Exception {
        var r = subir("/api/cores/no-existe/plugins", archivo(jar("1.3.0")));
        assertErrorApi(r, 404, CodigosError.NO_ENCONTRADO);
    }

    @Test
    void unaVersionRepetidaDa409() throws Exception {
        assertThat(subir(BASE, archivo(jar("1.3.0"))).statusCode()).isEqualTo(201);
        var r = subir(BASE, archivo(jar("1.3.0")));
        assertErrorApi(r, 409, CodigosError.CONFLICTO);
    }

    @Test
    void elGetDeUnPluginInexistenteDa404() throws Exception {
        var r = obtener(BASE + "/kyc-pe/9.9.9");
        assertErrorApi(r, 404, CodigosError.NO_ENCONTRADO);
    }

    @Test
    void unCuerpoQueNoEsMultipartDa415ConErrorApi() throws Exception {
        var r = enviar("POST", BASE, "application/json", "{}".getBytes(StandardCharsets.UTF_8));
        assertErrorApi(r, 415, CodigosError.TIPO_NO_SOPORTADO);
    }
}
