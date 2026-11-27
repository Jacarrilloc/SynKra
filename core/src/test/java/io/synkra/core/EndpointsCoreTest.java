package io.synkra.core;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import io.synkra.plugin.api.ResultadoKyc;
import io.synkra.plugin.api.SolicitudKyc;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
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
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * H-21: GET /plugins y POST /kyc/{plugin}. Servidor real en puerto aleatorio y cliente HTTP real; se lee el cuerpo,
 * no solo el código. Los plugins se compilan en un {@code @TempDir} y dejan constancia de sus llamadas en
 * {@link Eventos}, que vive en el classpath padre.
 */
public class EndpointsCoreTest {

    /** Visible desde los plugins de prueba (los carga un URLClassLoader hijo con el classpath de la prueba). */
    public static final class Eventos {
        public static final List<String> LLAMADAS = new CopyOnWriteArrayList<>();
        public static volatile ClassLoader TCCL_EN_PLUGIN;
        public static volatile ClassLoader CARGADOR_DEL_PLUGIN;
        public static volatile Thread HILO;

        private Eventos() {}

        public static void registrar(String e) {
            LLAMADAS.add(e);
        }

        public static void observar(Object plugin) {
            HILO = Thread.currentThread();
            TCCL_EN_PLUGIN = Thread.currentThread().getContextClassLoader();
            CARGADOR_DEL_PLUGIN = plugin.getClass().getClassLoader();
        }

        static void limpiar() {
            LLAMADAS.clear();
            TCCL_EN_PLUGIN = null;
            CARGADOR_DEL_PLUGIN = null;
            HILO = null;
        }
    }

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String SOLICITUD = "{\"documento\":\"OK-123\",\"datos\":{\"clave\":\"valor\"}}";

    @TempDir
    Path tmp;

    Path config;
    Path plugins;
    final HttpClient http = HttpClient.newHttpClient();

    @BeforeEach
    void carpetas() throws IOException {
        config = Files.createDirectories(tmp.resolve("config"));
        plugins = Files.createDirectories(tmp.resolve("plugins"));
        Eventos.limpiar();
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

    private HttpResponse<String> get(ConfigurableApplicationContext ctx, String ruta) throws Exception {
        return http.send(base(ctx, ruta).GET().build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> post(ConfigurableApplicationContext ctx, String ruta, String cuerpo) throws Exception {
        var b = base(ctx, ruta).header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(cuerpo)).build();
        return http.send(b, HttpResponse.BodyHandlers.ofString());
    }

    private HttpRequest.Builder base(ConfigurableApplicationContext ctx, String ruta) {
        String puerto = ctx.getEnvironment().getProperty("local.server.port");
        assertNotNull(puerto, "el servidor debe estar en un puerto aleatorio");
        return HttpRequest.newBuilder(URI.create("http://localhost:" + puerto + ruta));
    }

    private enum Tipo { SOLO_CICLO, CON_KYC, KYC_QUE_LANZA, KYC_NULO, KYC_LINKAGE, FALLA_AL_INICIAR }

    private void jar(String nombre, String version, Tipo tipo) throws IOException {
        String archivo = nombre + "-" + version + ".jar";
        String yaml = "nombre: " + nombre + "\nversion: " + version + "\ncore: synkra-core\ncoreCompatible: 2.x\n";
        String clase = "Plugin" + nombre.replace("-", "");
        String ev = Eventos.class.getCanonicalName();
        boolean kyc = tipo == Tipo.CON_KYC || tipo == Tipo.KYC_QUE_LANZA || tipo == Tipo.KYC_NULO || tipo == Tipo.KYC_LINKAGE;
        String fuente = "public class " + clase + " implements io.synkra.plugin.api.SynkraPlugin"
                + (kyc ? ", io.synkra.plugin.api.PuntoKyc" : "") + " {\n"
                + "  public String nombre() { return \"" + nombre + "\"; }\n"
                + "  public void iniciar(io.synkra.plugin.api.ContextoPlugin c) {"
                + (tipo == Tipo.FALLA_AL_INICIAR ? " throw new IllegalStateException(\"boom-inicio\");" : "")
                + " }\n"
                + "  public void detener() { }\n"
                + (kyc ? "  public io.synkra.plugin.api.ResultadoKyc validar(io.synkra.plugin.api.SolicitudKyc s) {\n"
                        + "    " + ev + ".observar(this);\n"
                        + "    " + ev + ".registrar(\"validar:" + nombre + ":\" + s.documento() + \":\" + s.datos().get(\"clave\"));\n"
                        + (tipo == Tipo.KYC_QUE_LANZA
                                ? "    throw new IllegalStateException(\"boom-secreto\");\n"
                                : tipo == Tipo.KYC_NULO
                                ? "    return null;\n"
                                : tipo == Tipo.KYC_LINKAGE
                                ? "    throw new NoClassDefFoundError(\"boom-enlace\");\n"
                                : "    return new io.synkra.plugin.api.ResultadoKyc(s.documento().startsWith(\"OK\"), \"motivo-" + nombre + "\");\n")
                        + "  }\n"
                : "")
                + "}\n";
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

    private static void esperarError(HttpResponse<String> r, int estado, String codigo, String nombre) throws Exception {
        esperarError(r, estado, codigo);
        assertTrue(JSON.readTree(r.body()).path("mensaje").asText().contains(nombre), r.body());
    }

    private static void esperarError(HttpResponse<String> r, int estado, String codigo) throws Exception {
        assertEquals(estado, r.statusCode(), r.body());
        JsonNode j = JSON.readTree(r.body());
        assertEquals(codigo, j.path("codigo").asText(), r.body());
        assertFalse(j.path("mensaje").asText().isBlank(), r.body());
    }

    @Test
    void getPluginsListaExactamenteLosActivosConSuVersionEnOrdenDeCarga() throws Exception {
        jar("kyc-a", "1.0.0", Tipo.CON_KYC);
        jar("kyc-b", "2.1.0", Tipo.SOLO_CICLO);
        jar("kyc-c", "3.0.0", Tipo.CON_KYC); // presente pero no activo
        activos("- nombre: kyc-b\n  version: 2.1.0\n- nombre: kyc-a\n  version: 1.0.0\n");
        try (var ctx = arrancar()) {
            var r = get(ctx, "/plugins");
            assertEquals(200, r.statusCode(), r.body());
            assertEquals(JSON.readTree(
                    "[{\"nombre\":\"kyc-b\",\"version\":\"2.1.0\"},{\"nombre\":\"kyc-a\",\"version\":\"1.0.0\"}]"),
                    JSON.readTree(r.body()), r.body());
        }
    }

    @Test
    void getPluginsSoloListaLosIniciados() throws Exception {
        jar("kyc-a", "1.0.0", Tipo.CON_KYC);
        jar("kyc-b", "1.0.0", Tipo.FALLA_AL_INICIAR);
        activos("- nombre: kyc-a\n  version: 1.0.0\n- nombre: kyc-b\n  version: 1.0.0\n");
        try (var ctx = arrancar()) {
            assertEquals(503, get(ctx, "/actuator/health/readiness").statusCode());
            var r = get(ctx, "/plugins");
            assertEquals(200, r.statusCode(), r.body());
            assertEquals(JSON.readTree("[]"), JSON.readTree(r.body()), r.body());
            // el que se inició y luego se detuvo por el fallo del vecino tampoco atiende KYC
            esperarError(post(ctx, "/kyc/kyc-a", SOLICITUD), 404, ErrorCore.PLUGIN_NO_ACTIVO, "kyc-a");
            assertTrue(Eventos.LLAMADAS.isEmpty(), Eventos.LLAMADAS.toString());
        }
    }

    @Test
    void postKycDelegaEnElPluginYDevuelveSuResultado() throws Exception {
        jar("kyc-a", "1.0.0", Tipo.CON_KYC);
        activos("- nombre: kyc-a\n  version: 1.0.0\n");
        try (var ctx = arrancar()) {
            var r = post(ctx, "/kyc/kyc-a", SOLICITUD);
            assertEquals(200, r.statusCode(), r.body());
            assertEquals(JSON.readTree("{\"aprobado\":true,\"motivo\":\"motivo-kyc-a\"}"), JSON.readTree(r.body()),
                    r.body());
            assertEquals(List.of("validar:kyc-a:OK-123:valor"), Eventos.LLAMADAS);
            var rechazo = post(ctx, "/kyc/kyc-a", "{\"documento\":\"MAL-1\",\"datos\":{}}");
            assertEquals(JSON.readTree("{\"aprobado\":false,\"motivo\":\"motivo-kyc-a\"}"),
                    JSON.readTree(rechazo.body()), rechazo.body());
        }
    }

    @Test
    void pluginNoActivoDa404PluginNoActivo() throws Exception {
        jar("kyc-a", "1.0.0", Tipo.CON_KYC);
        jar("kyc-c", "3.0.0", Tipo.CON_KYC); // presente, no activo
        activos("- nombre: kyc-a\n  version: 1.0.0\n");
        try (var ctx = arrancar()) {
            esperarError(post(ctx, "/kyc/kyc-c", SOLICITUD), 404, ErrorCore.PLUGIN_NO_ACTIVO, "kyc-c");
            esperarError(post(ctx, "/kyc/no-existe", SOLICITUD), 404, ErrorCore.PLUGIN_NO_ACTIVO, "no-existe");
            // el nombre se busca exacto, sin distinguir nada de mayusculas
            esperarError(post(ctx, "/kyc/KYC-A", SOLICITUD), 404, ErrorCore.PLUGIN_NO_ACTIVO, "KYC-A");
            assertTrue(Eventos.LLAMADAS.isEmpty(), Eventos.LLAMADAS.toString());
        }
    }

    @Test
    void pluginActivoSinPuntoKycDa404PluginSinKyc() throws Exception {
        jar("kyc-b", "2.1.0", Tipo.SOLO_CICLO);
        activos("- nombre: kyc-b\n  version: 2.1.0\n");
        try (var ctx = arrancar()) {
            esperarError(post(ctx, "/kyc/kyc-b", SOLICITUD), 404, ErrorCore.PLUGIN_SIN_KYC, "kyc-b");
        }
    }

    @Test
    void cuerpoInvalidoDa400SolicitudInvalidaSinLlamarAlPlugin() throws Exception {
        jar("kyc-a", "1.0.0", Tipo.CON_KYC);
        activos("- nombre: kyc-a\n  version: 1.0.0\n");
        try (var ctx = arrancar()) {
            for (String malo : List.of("no es json", "", "null", "{}", "{\"documento\":\"x\"}",
                    "{\"datos\":{}}", "{\"documento\":5,\"datos\":{}}", "{\"documento\":\"x\",\"datos\":[1]}", "{\"documento\":true,\"datos\":{}}",
                    "{\"documento\":\"NO\",\"documento\":\"OK-dup\",\"datos\":{}}",
                    "{\"documento\":\"OK\",\"datos\":{}} basura",
                    "{\"documento\":\"OK\",\"datos\":{}}{\"x\":1}",
                    "{\"documento\":\"OK\",\"datos\":{},\"extra\":1}")) {
                var r = post(ctx, "/kyc/kyc-a", malo);
                assertEquals(400, r.statusCode(), "cuerpo enviado: " + malo + " -> " + r.body());
                esperarError(r, 400, ErrorCore.SOLICITUD_INVALIDA);
            }
            assertTrue(Eventos.LLAMADAS.isEmpty(), Eventos.LLAMADAS.toString());
        }
    }

    @Test
    void pluginQueLanzaDa500PluginFalloQueNombraElPluginSinFiltrarLaTraza() throws Exception {
        jar("kyc-x", "1.0.0", Tipo.KYC_QUE_LANZA);
        activos("- nombre: kyc-x\n  version: 1.0.0\n");
        try (var ctx = arrancar()) {
            var r = post(ctx, "/kyc/kyc-x", SOLICITUD);
            esperarError(r, 500, ErrorCore.PLUGIN_FALLO);
            String mensaje = JSON.readTree(r.body()).path("mensaje").asText();
            assertTrue(mensaje.contains("kyc-x"), r.body());
            assertFalse(r.body().contains("at io."), r.body());
            assertFalse(r.body().contains("Exception"), r.body());
            assertFalse(r.body().contains("boom-secreto"), r.body());
            assertEquals(List.of("validar:kyc-x:OK-123:valor"), Eventos.LLAMADAS);
        }
    }

    @Test
    void pluginQueDevuelveNullDa500PluginFalloQueNombraElPlugin() throws Exception {
        jar("kyc-n", "1.0.0", Tipo.KYC_NULO);
        activos("- nombre: kyc-n\n  version: 1.0.0\n");
        try (var ctx = arrancar()) {
            var r = post(ctx, "/kyc/kyc-n", SOLICITUD);
            esperarError(r, 500, ErrorCore.PLUGIN_FALLO, "kyc-n");
        }
    }

    @Test
    void pluginQueLanzaUnErrorDeEnlaceDa500PluginFallo() throws Exception {
        jar("kyc-l", "1.0.0", Tipo.KYC_LINKAGE);
        activos("- nombre: kyc-l\n  version: 1.0.0\n");
        try (var ctx = arrancar()) {
            var r = post(ctx, "/kyc/kyc-l", SOLICITUD);
            esperarError(r, 500, ErrorCore.PLUGIN_FALLO, "kyc-l");
            assertFalse(r.body().contains("boom-enlace"), r.body());
            assertFalse(r.body().contains("NoClassDefFoundError"), r.body());
        }
    }

    @Test
    void dentroDelPluginElContextClassLoaderEsElDelPlugin() throws Exception {
        jar("kyc-a", "1.0.0", Tipo.CON_KYC);
        activos("- nombre: kyc-a\n  version: 1.0.0\n");
        try (var ctx = arrancar()) {
            assertEquals(200, post(ctx, "/kyc/kyc-a", SOLICITUD).statusCode());
            assertNotNull(Eventos.CARGADOR_DEL_PLUGIN);
            assertNotSame(EndpointsCoreTest.class.getClassLoader(), Eventos.CARGADOR_DEL_PLUGIN);
            assertSame(Eventos.CARGADOR_DEL_PLUGIN, Eventos.TCCL_EN_PLUGIN, "dentro del plugin");
        }
    }

    // Tomcat restaura por su cuenta el cargador del hilo al terminar la petición, así que la restauración del
    // core se prueba llamando a invocar() directamente en el hilo de la prueba.
    @Test
    void invocarPoneElCargadorDelPluginYLoRestauraAlTerminar() throws Exception {
        var anterior = new URLClassLoader(new URL[0]);
        var delPlugin = new URLClassLoader(new URL[0]);
        var visto = new AtomicReference<ClassLoader>();
        var hilo = Thread.currentThread();
        var original = hilo.getContextClassLoader();
        try {
            hilo.setContextClassLoader(anterior);
            var r = new EndpointsCore(null, JSON).invocar("p", new PluginCargado(null, delPlugin), s -> {
                visto.set(Thread.currentThread().getContextClassLoader());
                return new ResultadoKyc(true, "m");
            }, new SolicitudKyc("d", Map.of()));
            assertEquals(200, r.getStatusCode().value());
            assertSame(delPlugin, visto.get(), "dentro del plugin");
            assertSame(anterior, hilo.getContextClassLoader(), "restaurado tras volver");
        } finally {
            hilo.setContextClassLoader(original);
        }
    }

    @Test
    void invocarRestauraElCargadorTambienSiElPluginLanza() throws Exception {
        var anterior = new URLClassLoader(new URL[0]);
        var delPlugin = new URLClassLoader(new URL[0]);
        var visto = new AtomicReference<ClassLoader>();
        var hilo = Thread.currentThread();
        var original = hilo.getContextClassLoader();
        try {
            hilo.setContextClassLoader(anterior);
            var r = new EndpointsCore(null, JSON).invocar("p", new PluginCargado(null, delPlugin), s -> {
                visto.set(Thread.currentThread().getContextClassLoader());
                throw new IllegalStateException("boom");
            }, new SolicitudKyc("d", Map.of()));
            assertEquals(500, r.getStatusCode().value());
            assertSame(delPlugin, visto.get(), "dentro del plugin");
            assertSame(anterior, hilo.getContextClassLoader(), "restaurado tras lanzar");
        } finally {
            hilo.setContextClassLoader(original);
        }
    }
}
