package io.synkra.core;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/** S1, S4, S6 y S12 de la suite de U01 contra el arranque real del contexto de Spring. */
@ExtendWith(OutputCaptureExtension.class)
class ArranquePluginsTest {

    @TempDir
    Path tmp;

    Path config;
    Path plugins;

    @BeforeEach
    void carpetas() throws IOException {
        config = Files.createDirectories(tmp.resolve("config"));
        plugins = Files.createDirectories(tmp.resolve("plugins"));
    }

    /** Cuenta qué JAR se cargaron: un JAR que no pasa por aquí nunca tuvo cargador. */
    @TestConfiguration
    static class Contador {
        @Bean
        @Primary
        CargadorPluginsContador cargadorContador() {
            return new CargadorPluginsContador();
        }
    }

    static class CargadorPluginsContador extends CargadorPlugins {
        final List<String> cargados = new ArrayList<>();

        @Override
        public PluginCargado cargar(Path jar) {
            cargados.add(jar.getFileName().toString());
            return super.cargar(jar);
        }
    }

    private ConfigurableApplicationContext arrancar() {
        return new SpringApplicationBuilder(SynkraCoreApplication.class, Contador.class)
                .web(WebApplicationType.NONE)
                .properties(
                        "synkra.core.config=" + config,
                        "synkra.core.plugins=" + plugins,
                        "spring.main.banner-mode=off")
                .run();
    }

    private void jar(String archivo, String nombre, String version, String core, String compatible)
            throws IOException {
        String yaml = "nombre: " + nombre + "\nversion: " + version + "\ncore: " + core
                + "\ncoreCompatible: " + compatible + "\n";
        String clase = "Plugin" + nombre.replace("-", "");
        String fuente = "public class " + clase + " implements io.synkra.plugin.api.SynkraPlugin {\n"
                + "  public String nombre() { return \"" + nombre + "\"; }\n"
                + "  public void iniciar(io.synkra.plugin.api.ContextoPlugin c) {}\n"
                + "  public void detener() {}\n}\n";
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
    void s1ValidoArrancaYCargaElPluginActivo() throws IOException {
        jar("kyc-co-1.0.0.jar", "kyc-co", "1.0.0", "synkra-core", "2.x");
        activos("- nombre: kyc-co\n  version: 1.0.0\n");
        try (var ctx = arrancar()) {
            var arranque = ctx.getBean(ArranquePlugins.class);
            assertEquals(1, arranque.cargados().size());
            assertEquals("kyc-co", arranque.cargados().get(0).plugin().nombre());
            assertEquals(List.of("kyc-co-1.0.0.jar"), ctx.getBean(CargadorPluginsContador.class).cargados);
        }
    }

    @Test
    void s4PluginTres_x_ConCore210NoArrancaYElLogNombraElMotivo(CapturedOutput salida) throws IOException {
        jar("kyc-pe-2.0.0.jar", "kyc-pe", "2.0.0", "synkra-core", "3.x");
        activos("- nombre: kyc-pe\n  version: 2.0.0\n");
        assertThrows(RuntimeException.class, this::arrancar);
        assertTrue(salida.getAll().contains("VERSION_INCOMPATIBLE plugin=kyc-pe"), salida.getAll());
    }

    @Test
    void s6PluginActivoAusenteNoArranca(CapturedOutput salida) throws IOException {
        activos("- nombre: kyc-mx\n  version: 1.0.0\n");
        assertThrows(RuntimeException.class, this::arrancar);
        assertTrue(salida.getAll().contains("PLUGIN_AUSENTE plugin=kyc-mx"), salida.getAll());
    }

    @Test
    void s12PresenteYNoActivoArrancaSinCargarlo() throws IOException {
        jar("kyc-co-1.0.0.jar", "kyc-co", "1.0.0", "synkra-core", "2.x");
        jar("kyc-pe-1.3.0.jar", "kyc-pe", "1.3.0", "synkra-core", "2.x");
        activos("- nombre: kyc-co\n  version: 1.0.0\n");
        try (var ctx = arrancar()) {
            assertEquals(List.of("kyc-co-1.0.0.jar"), ctx.getBean(CargadorPluginsContador.class).cargados);
            assertEquals(1, ctx.getBean(ArranquePlugins.class).cargados().size());
        }
    }

    @Test
    void sinPluginsActivosYamlArrancaSinPlugins() throws IOException {
        jar("kyc-co-1.0.0.jar", "kyc-co", "1.0.0", "synkra-core", "2.x");
        try (var ctx = arrancar()) {
            assertTrue(ctx.getBean(ArranquePlugins.class).cargados().isEmpty());
            assertTrue(ctx.getBean(CargadorPluginsContador.class).cargados.isEmpty());
        }
    }

    private static Throwable raiz(Throwable t) {
        while (t.getCause() != null) {
            t = t.getCause();
        }
        return t;
    }

    private String motivoDeRechazo() {
        var e = assertThrows(RuntimeException.class, this::arrancar);
        var r = raiz(e);
        assertInstanceOf(ArranqueRechazado.class, r, e.toString());
        return r.getMessage();
    }

    @Test
    void yamlConEtiquetaDeTipoSeRechazaPorLaEtiqueta() throws IOException {
        jar("kyc-co-1.0.0.jar", "kyc-co", "1.0.0", "synkra-core", "2.x");
        // Control: sin la etiqueta el mismo documento es válido y arranca.
        activos("- {nombre: kyc-co, version: 1.0.0}\n");
        try (var ctx = arrancar()) {
            assertEquals(1, ctx.getBean(ArranquePlugins.class).cargados().size());
        }
        activos("- !!java.util.HashMap {nombre: kyc-co, version: 1.0.0}\n");
        var motivo = motivoDeRechazo();
        assertTrue(motivo.contains("etiqueta de tipo 'tag:yaml.org,2002:java.util.HashMap' no permitida"), motivo);
    }

    @Test
    void etiquetaExplicitaQueNoEsStrSeRechaza() throws IOException {
        activos("- nombre: kyc-co\n  version: !!int 1.0.0\n");
        var motivo = motivoDeRechazo();
        assertTrue(motivo.contains("etiqueta de tipo 'tag:yaml.org,2002:int' no permitida"), motivo);
    }

    @Test
    void yamlDemasiadoAnidadoSeRechaza() throws IOException {
        activos("- " + "[".repeat(40) + "]".repeat(40) + "\n");
        var motivo = motivoDeRechazo();
        assertTrue(motivo.contains("anidación máxima de 32 niveles"), motivo);
    }

    @Test
    void yamlDemasiadoGrandeSeRechaza() throws IOException {
        activos("# " + "x".repeat(ArranquePlugins.MAX_BYTES) + "\n");
        var motivo = motivoDeRechazo();
        assertTrue(motivo.contains("tamaño máximo de " + ArranquePlugins.MAX_BYTES + " bytes"), motivo);
    }

    @Test
    void jarConDescriptorInvalidoNoEntraAlCatalogoYSeRegistraPorNombre(CapturedOutput salida) throws IOException {
        Files.writeString(plugins.resolve("roto.jar"), "no es un zip");
        try (var ctx = arrancar()) {
            assertTrue(ctx.getBean(ArranquePlugins.class).cargados().isEmpty());
        }
        assertTrue(salida.getAll().contains("roto.jar"), salida.getAll());
    }
}
