package io.synkra.catalogo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.synkra.catalogo.api.CatalogoApi;
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
import java.security.MessageDigest;
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

/** R-C13 (H-15): servir artefactos por hash; los datos entran por {@code subirPlugin}. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ArtefactosPorHashTest {

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
    }

    private static byte[] jar(String nombre, String version, String relleno) throws IOException {
        var yaml = "nombre: " + nombre + "\nversion: " + version + "\ncore: synkra-core\ncoreCompatible: 2.x\n";
        var salida = new ByteArrayOutputStream();
        try (var z = new ZipOutputStream(salida)) {
            z.putNextEntry(new ZipEntry("synkra-plugin.yaml"));
            z.write(yaml.getBytes(StandardCharsets.UTF_8));
            z.closeEntry();
            z.putNextEntry(new ZipEntry("relleno.txt"));
            z.write(relleno.getBytes(StandardCharsets.UTF_8));
            z.closeEntry();
        }
        return salida.toByteArray();
    }

    private String subir(byte[] bytes) throws IOException {
        return api.subirPlugin("synkra-core", new ByteArrayInputStream(bytes), Map.of()).sha256();
    }

    private static String sha(byte[] b) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(b));
    }

    private HttpResponse<byte[]> get(String ruta) throws IOException {
        try {
            return http.send(HttpRequest.newBuilder(URI.create("http://localhost:" + puerto + ruta)).GET().build(),
                    HttpResponse.BodyHandlers.ofByteArray());
        } catch (InterruptedException e) {
            throw new IOException(e);
        }
    }

    private Path archivoDe(String nombre) {
        return datos.resolve(jdbc.queryForObject("select ruta from cat_plugin where nombre = ?", String.class, nombre));
    }

    @SuppressWarnings("unchecked")
    private void esError(HttpResponse<byte[]> r, int estado, String codigo) throws IOException {
        assertThat(r.statusCode()).isEqualTo(estado);
        assertThat(((Map<String, Object>) json.readValue(r.body(), Map.class)).get("codigo")).isEqualTo(codigo);
    }

    @Test
    void sirveLosMismosBytesConLasTresCabeceras() throws Exception {
        var bytes = jar("kyc-pe", "1.3.0", "x".repeat(5000));
        var hash = subir(bytes);
        var r = get("/api/artefactos/" + hash);
        assertThat(r.statusCode()).isEqualTo(200);
        assertThat(sha(r.body())).isEqualTo(hash).isEqualTo(sha(bytes));
        assertThat(r.headers().firstValue("Content-Type")).contains("application/java-archive");
        assertThat(r.headers().firstValueAsLong("Content-Length")).hasValue(bytes.length);
        assertThat(r.headers().firstValue("X-Synkra-Sha256")).contains(hash);
        assertThat(r.headers().firstValue("Content-Disposition")).get().asString()
                .contains("attachment").contains("kyc-pe-1.3.0.jar");
    }

    @Test
    void hashDesconocidoEs404() throws IOException {
        esError(get("/api/artefactos/" + "0".repeat(64)), 404, "NO_ENCONTRADO");
    }

    @Test
    void hashMalFormadoEs404() throws Exception {
        var hash = subir(jar("kyc-pe", "1.3.0", "a"));
        esError(get("/api/artefactos/" + hash.toUpperCase()), 404, "NO_ENCONTRADO");
        esError(get("/api/artefactos/" + hash.substring(1)), 404, "NO_ENCONTRADO");
        esError(get("/api/artefactos/%2e%2e"), 404, "NO_ENCONTRADO");
        for (var malo : List.of("../" + hash.substring(3), hash.toUpperCase(), hash.substring(1), "", "..")) {
            assertThatThrownBy(() -> api.abrirArtefacto(malo)).isInstanceOfSatisfying(ExcepcionApi.class,
                    e -> assertThat(e.estado().value()).isEqualTo(404));
        }
        assertThatThrownBy(() -> api.abrirArtefacto(null)).isInstanceOf(ExcepcionApi.class);
    }

    @Test
    void dosPluginsConLosMismosBytesSeSirvenPorElMismoHash() throws Exception {
        var bytes = jar("kyc-pe", "1.3.0", "igual");
        var hash = subir(bytes);
        // Mismo contenido bajo otro plugin: se clona la fila apuntando a una copia del mismo archivo.
        var copia = datos.resolve("artefactos/synkra-core/kyc-co/1.0.0.jar");
        Files.createDirectories(copia.getParent());
        Files.copy(archivoDe("kyc-pe"), copia);
        jdbc.update("insert into cat_plugin select core, 'kyc-co', '1.0.0', core_compatible, sha256, tamano, ?, "
                + "descripcion, subido_por, subido_en from cat_plugin where nombre = 'kyc-pe'",
                datos.relativize(copia).toString());
        var a = get("/api/artefactos/" + hash);
        assertThat(a.statusCode()).isEqualTo(200);
        assertThat(sha(a.body())).isEqualTo(hash);
        // Determinista: se elige la primera por (core, nombre, version), o sea kyc-co.
        assertThat(a.headers().firstValue("Content-Disposition")).get().asString().contains("kyc-co-1.0.0.jar");
        Files.delete(copia);
        // Si falla la elegida, falla: no se salta a otra fila.
        assertThat(get("/api/artefactos/" + hash).statusCode()).isEqualTo(500);
    }

    @Test
    void archivoBorradoEs500NoUn200Vacio() throws Exception {
        var hash = subir(jar("kyc-pe", "1.3.0", "a"));
        Files.delete(archivoDe("kyc-pe"));
        var r = get("/api/artefactos/" + hash);
        esError(r, 500, "ERROR_INTERNO");
        assertThat(new String(r.body(), StandardCharsets.UTF_8)).doesNotContain(datos.toString())
                .contains(MotivosCatalogo.ARTEFACTO_AUSENTE);
    }

    @Test
    void archivoTruncadoEs500AntesDelCuerpo() throws Exception {
        var hash = subir(jar("kyc-pe", "1.3.0", "a".repeat(3000)));
        var archivo = archivoDe("kyc-pe");
        var bytes = Files.readAllBytes(archivo);
        Files.delete(archivo);
        Files.write(archivo, java.util.Arrays.copyOf(bytes, bytes.length / 2));
        var r = get("/api/artefactos/" + hash);
        esError(r, 500, "ERROR_INTERNO");
        assertThat(r.headers().firstValue("X-Synkra-Sha256")).isEmpty();
        assertThat(new String(r.body(), StandardCharsets.UTF_8)).contains(MotivosCatalogo.ARTEFACTO_TAMANO);
    }

    @Test
    void rutaFueraDeLaRaizEs500YNoSirveNada() throws Exception {
        var hash = subir(jar("kyc-pe", "1.3.0", "a"));
        var fuera = Files.createTempFile("fuera", ".jar");
        try {
            Files.write(fuera, jar("kyc-pe", "1.3.0", "a"));
            for (var ruta : List.of("../../../../../.." + fuera, "/" + fuera.toString().substring(1), "../fuera.jar")) {
                jdbc.update("update cat_plugin set ruta = ?", ruta);
                var r = get("/api/artefactos/" + hash);
                esError(r, 500, "ERROR_INTERNO");
                assertThat(r.headers().firstValue("X-Synkra-Sha256")).isEmpty();
                assertThat(new String(r.body(), StandardCharsets.UTF_8)).contains(MotivosCatalogo.ARTEFACTO_RUTA_FUERA);
            }
        } finally {
            Files.deleteIfExists(fuera);
        }
    }

    @Test
    void enlaceSimbolicoEs500() throws Exception {
        var hash = subir(jar("kyc-pe", "1.3.0", "a"));
        var archivo = archivoDe("kyc-pe");
        var original = Files.readAllBytes(archivo);
        var otro = Files.createTempFile("otro", ".jar");
        try {
            Files.write(otro, original);
            Files.delete(archivo);
            Files.createSymbolicLink(archivo, otro);
            esError(get("/api/artefactos/" + hash), 500, "ERROR_INTERNO");
        } finally {
            Files.deleteIfExists(archivo);
            Files.deleteIfExists(otro);
        }
    }

    @Test
    void laBaseSqliteNoSeSirveAunqueLaFilaApuntaAEllaConSuTamano() throws Exception {
        var hash = subir(jar("kyc-pe", "1.3.0", "a"));
        for (var nombre : List.of("synkra.db", "artefactos/../synkra.db")) {
            var db = datos.resolve("synkra.db");
            assertThat(db).exists();
            jdbc.update("update cat_plugin set ruta = ?, tamano = ?", nombre, Files.size(db));
            var r = get("/api/artefactos/" + hash);
            esError(r, 500, "ERROR_INTERNO");
            assertThat(r.headers().firstValue("X-Synkra-Sha256")).isEmpty();
            assertThat(new String(r.body(), StandardCharsets.UTF_8)).contains(MotivosCatalogo.ARTEFACTO_RUTA_FUERA);
        }
    }

    @Test
    void directorioIntermedioEnlazadoEs500() throws Exception {
        var hash = subir(jar("kyc-pe", "1.3.0", "a"));
        var carpeta = archivoDe("kyc-pe").getParent();
        var movida = Files.createTempDirectory("movida");
        try {
            Files.move(carpeta, movida, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            Files.createSymbolicLink(carpeta, movida);
            esError(get("/api/artefactos/" + hash), 500, "ERROR_INTERNO");
        } finally {
            Files.deleteIfExists(carpeta);
            try (Stream<Path> s = Files.walk(movida)) {
                for (var p : s.sorted(Comparator.reverseOrder()).toList()) {
                    Files.delete(p);
                }
            }
        }
    }
}
