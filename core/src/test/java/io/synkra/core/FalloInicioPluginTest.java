package io.synkra.core;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * H-22: un plugin que falla en {@code iniciar} deja el core vivo pero sin readiness. Servidor real en puerto
 * aleatorio y peticiones HTTP reales a las sondas. Los plugins se compilan en un {@code @TempDir} (no hay
 * subproyecto fixture) y registran sus llamadas en {@link Eventos}, que está en el classpath padre.
 */
@ExtendWith(OutputCaptureExtension.class)
public class FalloInicioPluginTest {

    /** Visible desde los plugins de prueba (los carga un URLClassLoader hijo con el classpath de la prueba). */
    public static final class Eventos {
        public static final List<String> LLAMADAS = new CopyOnWriteArrayList<>();

        private Eventos() {}

        public static void registrar(String e) {
            LLAMADAS.add(e);
        }
    }

    @TempDir
    Path tmp;

    Path config;
    Path plugins;
    final HttpClient http = HttpClient.newHttpClient();

    @BeforeEach
    void carpetas() throws IOException {
        config = Files.createDirectories(tmp.resolve("config"));
        plugins = Files.createDirectories(tmp.resolve("plugins"));
        Eventos.LLAMADAS.clear();
    }

    private ConfigurableApplicationContext arrancar() {
        return new SpringApplicationBuilder(SynkraCoreApplication.class)
                .properties(
                        "server.port=0",
                        "synkra.core.config=" + config,
                        "synkra.core.plugins=" + plugins,
                        "spring.main.banner-mode=off")
                .run();
    }

    private int get(ConfigurableApplicationContext ctx, String ruta) throws Exception {
        String puerto = ctx.getEnvironment().getProperty("local.server.port");
        assertNotNull(puerto, "el servidor debe estar en un puerto aleatorio");
        var peticion = HttpRequest.newBuilder(URI.create("http://localhost:" + puerto + ruta)).GET().build();
        return http.send(peticion, HttpResponse.BodyHandlers.ofString()).statusCode();
    }

    /** JAR con un plugin que registra iniciar/detener y, si {@code falla}, lanza IllegalStateException al iniciar. */
    private void jar(String nombre, boolean falla) throws IOException {
        String archivo = nombre + "-1.0.0.jar";
        String yaml = "nombre: " + nombre + "\nversion: 1.0.0\ncore: synkra-core\ncoreCompatible: 2.x\n";
        String clase = "Plugin" + nombre.replace("-", "");
        String ev = Eventos.class.getCanonicalName();
        String fuente = "public class " + clase + " implements io.synkra.plugin.api.SynkraPlugin {\n"
                + "  public String nombre() { return \"" + nombre + "\"; }\n"
                + "  public void iniciar(io.synkra.plugin.api.ContextoPlugin c) {\n"
                + "    " + ev + ".registrar(\"iniciar:" + nombre + "\");\n"
                + (falla ? "    throw new IllegalStateException(\"boom-" + nombre + "\");\n" : "")
                + "  }\n"
                + "  public void detener() { " + ev + ".registrar(\"detener:" + nombre + "\"); }\n}\n";
        Path src = Files.createDirectories(tmp.resolve(archivo + "-src"));
        Path out = Files.createDirectories(tmp.resolve(archivo + "-out"));
        Files.writeString(src.resolve(clase + ".java"), fuente);
        JavaCompiler jc = ToolProvider.getSystemJavaCompiler();
        assertNotNull(jc, "se necesita un JDK para compilar los plugins de prueba");
        assertEquals(0, jc.run(null, null, null, "-d", out.toString(), "-cp", System.getProperty("java.class.path"),
                src.resolve(clase + ".java").toString()));
        try (JarOutputStream jos = new JarOutputStream(Files.newOutputStream(plugins.resolve(archivo)))) {
            jos.putNextEntry(new JarEntry(clase + ".class"));
            jos.write(Files.readAllBytes(out.resolve(clase + ".class")));
            jos.closeEntry();
            jos.putNextEntry(new JarEntry("META-INF/services/io.synkra.plugin.api.SynkraPlugin"));
            jos.write((clase + "\n").getBytes(StandardCharsets.UTF_8));
            jos.closeEntry();
            jos.putNextEntry(new JarEntry("synkra-plugin.yaml"));
            jos.write(yaml.getBytes(StandardCharsets.UTF_8));
            jos.closeEntry();
        }
    }

    private void activos(String yaml) throws IOException {
        Files.writeString(config.resolve("plugins-activos.yaml"), yaml);
    }

    @Test
    void unPluginQueFallaDejaReadiness503LivenessEn200YElLogNombraPluginYCausa(CapturedOutput salida)
            throws Exception {
        jar("kyc-malo", true);
        activos("- nombre: kyc-malo\n  version: 1.0.0\n");
        try (var ctx = arrancar()) {
            assertEquals(503, get(ctx, "/actuator/health/readiness"));
            assertEquals(200, get(ctx, "/actuator/health/liveness"));
        }
        assertTrue(salida.getAll().contains("Plugin fallo al iniciar plugin=kyc-malo: "), salida.getAll());
        assertTrue(salida.getAll().contains("IllegalStateException: boom-kyc-malo"), salida.getAll());
    }

    @Test
    void siElSegundoFallaElPrimeroSeDetiene() throws Exception {
        jar("kyc-a", false);
        jar("kyc-b", true);
        activos("- nombre: kyc-a\n  version: 1.0.0\n- nombre: kyc-b\n  version: 1.0.0\n");
        try (var ctx = arrancar()) {
            assertEquals(503, get(ctx, "/actuator/health/readiness"));
            assertEquals(List.of("iniciar:kyc-a", "iniciar:kyc-b", "detener:kyc-a"), Eventos.LLAMADAS);
        }
    }

    @Test
    void siTodosIniciaReadinessEs200() throws Exception {
        jar("kyc-a", false);
        jar("kyc-b", false);
        activos("- nombre: kyc-a\n  version: 1.0.0\n- nombre: kyc-b\n  version: 1.0.0\n");
        try (var ctx = arrancar()) {
            assertEquals(200, get(ctx, "/actuator/health/readiness"));
            assertEquals(200, get(ctx, "/actuator/health/liveness"));
            assertEquals(List.of("iniciar:kyc-a", "iniciar:kyc-b"), Eventos.LLAMADAS);
        }
    }

    @Test
    void sinPluginsActivosReadinessEs200() throws Exception {
        activos("[]\n");
        try (var ctx = arrancar()) {
            assertEquals(200, get(ctx, "/actuator/health/readiness"));
        }
        assertTrue(Eventos.LLAMADAS.isEmpty());
    }
}
