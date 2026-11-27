package io.synkra.core;

import static org.junit.jupiter.api.Assertions.*;

import io.synkra.verificador.SuiteCombinaciones;
import io.synkra.verificador.SuiteCombinaciones.Caso;
import io.synkra.verificador.SuiteCombinaciones.Entrada;
import io.synkra.verificador.Descriptor;
import io.synkra.verificador.Veredicto.Global;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.stream.Stream;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * R1: la suite de combinaciones de U01 (S1 a S12) contra el arranque real. Cada caso ejecutable arma /config y
 * /plugins, arranca {@link ArranquePlugins} y compara con el veredicto esperado. Los que no se pueden ejecutar aquí
 * se listan con su razón (no se marcan como omitidos).
 */
@ExtendWith(OutputCaptureExtension.class)
class ConcordanciaSuiteTest {

    /** Versión del core compilada en el JAR (no la variable de entorno de la tarea test; C-71). */
    private static final io.synkra.verificador.Version COMPILADA = new VersionCore().version();

    /** Caso -> razón por la que no se ejecuta; vacío si se ejecuta. */
    static String razonNoEjecutable(Caso c) {
        return switch (c.entrada()) {
            case Entrada.FueraDeU01 f -> "fuera de U01: " + f.remiteA();
            case Entrada.PorSeleccion p -> {
                if (!p.seleccion().core().equals(ArranquePlugins.CORE)) {
                    yield "la selección es de otro core: " + p.seleccion().core();
                }
                yield p.seleccion().versionCore().equals(COMPILADA)
                        ? null
                        : "pide el core " + p.seleccion().versionCore() + " y el compilado es " + COMPILADA;
            }
            case Entrada.PorDescriptor d -> null;
        };
    }

    static Stream<Caso> ejecutables() {
        return SuiteCombinaciones.casos().stream().filter(c -> razonNoEjecutable(c) == null);
    }

    @Test
    void cadaCasoEstaEjecutadoOEnLaListaDeNoEjecutablesConSuRazon() {
        var ejecutados = ejecutables().map(Caso::id).toList();
        var no = new LinkedHashMap<String, String>();
        SuiteCombinaciones.casos().forEach(c -> {
            var r = razonNoEjecutable(c);
            if (r != null) {
                no.put(c.id(), r);
            }
        });
        System.out.println("CASOS EJECUTADOS: " + ejecutados);
        System.out.println("CASOS NO EJECUTABLES: " + no);
        assertTrue(ejecutados.size() > 0, "ningún caso se ejecuta; ¿la versión compilada del core cambió? (C-71)");
        no.forEach((id, r) -> assertFalse(r.isBlank(), id + " no se ejecuta y no dice por qué"));
        var todos = new ArrayList<>(ejecutados);
        todos.addAll(no.keySet());
        assertEquals(SuiteCombinaciones.casos().stream().map(Caso::id).sorted().toList(), todos.stream().sorted().toList(),
                "un caso desapareció sin razón");
        assertEquals(SuiteCombinaciones.casos().size(), ejecutados.size() + no.size());
        assertTrue(SuiteCombinaciones.contenido().get(0).versiones().contains(COMPILADA),
                "la versión compilada del core " + COMPILADA + " no está en el catálogo de la suite; ¿otra versión en la tarea test? (C-71)");
    }

    @TempDir
    Path tmp;

    @ParameterizedTest(name = "{0}")
    @MethodSource("ejecutablesConId")
    void concuerdaConElArranque(String id, Caso caso, CapturedOutput salida) throws IOException {
        var config = Files.createDirectories(tmp.resolve("config"));
        var plugins = Files.createDirectories(tmp.resolve("plugins"));
        var activos = new StringBuilder();
        var nombresEsperados = new ArrayList<String>();
        int n = 0;
        switch (caso.entrada()) {
            case Entrada.PorSeleccion p -> {
                for (Descriptor d : p.seleccion().plugins()) {
                    activos.append("- nombre: ").append(d.nombre()).append("\n  version: ")
                            .append(d.version()).append("\n");
                    nombresEsperados.add(d.nombre());
                    if (enCatalogo(d)) {
                        jar(plugins, "j" + (n++) + ".jar", d.nombre(), d.version().toString(), d.core(),
                                d.coreCompatible().texto(), null);
                    }
                }
                if (p.seleccion().plugins().isEmpty()) {
                    activos.append("[]\n");
                }
            }
            case Entrada.PorDescriptor d -> {
                jar(plugins, "descriptor.jar", "kyc-co", "1.0.0", null, null, d.yaml());
                activos.append("- nombre: kyc-co\n  version: 1.0.0\n");
            }
            default -> fail("no ejecutable: " + id);
        }
        Files.writeString(config.resolve("plugins-activos.yaml"), activos.toString());

        var esperado = caso.esperado();
        boolean aprueba = esperado.global().equals(java.util.Optional.of(Global.APROBADA));
        if (aprueba) {
            try (var ctx = arrancar(config, plugins)) {
                var cargados = ctx.getBean(ArranquePlugins.class).cargados().stream()
                        .map(c -> c.plugin().nombre()).toList();
                assertEquals(nombresEsperados, cargados, id);
            }
        } else {
            var caida = assertThrows(RuntimeException.class, () -> arrancar(config, plugins).close(),
                    id + ": el contexto debía no arrancar");
            assertTrue(conCausa(caida, ArranqueRechazado.class),
                    id + ": el arranque debía terminar en ArranqueRechazado y terminó en " + caida);
            var log = salida.getAll();
            for (var m : esperado.motivos()) {
                var codigo = m.codigo().name();
                if (m.plugin().isPresent()) {
                    assertTrue(log.contains(codigo + " plugin=" + m.plugin().get()),
                            id + ": falta " + codigo + " plugin=" + m.plugin().get() + " en\n" + log);
                } else {
                    assertTrue(log.contains(codigo), id + ": falta " + codigo + " en\n" + log);
                    assertTrue(log.contains(m.enMensaje()), id + ": el log no nombra '" + m.enMensaje() + "'\n" + log);
                }
            }
            if (esperado.rechazaElLector()) {
                // el código del lector y el JAR salen en la misma línea
                for (var m : esperado.motivos()) {
                    assertTrue(log.lines().anyMatch(l -> l.contains("descriptor.jar") && l.contains(m.codigo().name())
                                    && l.contains(m.enMensaje())),
                            id + ": ninguna línea liga descriptor.jar con " + m.codigo() + "\n" + log);
                }
                // el rechazo real: el plugin activo se queda sin JAR en el catálogo
                assertTrue(log.contains("PLUGIN_AUSENTE plugin=kyc-co"),
                        id + ": falta el rechazo PLUGIN_AUSENTE plugin=kyc-co\n" + log);
            }
        }
    }

    private static boolean conCausa(Throwable t, Class<? extends Throwable> tipo) {
        for (Throwable x = t; x != null; x = x.getCause() == x ? null : x.getCause()) {
            if (tipo.isInstance(x)) {
                return true;
            }
        }
        return false;
    }

    static Stream<org.junit.jupiter.params.provider.Arguments> ejecutablesConId() {
        return ejecutables().map(c -> org.junit.jupiter.params.provider.Arguments.of(c.id(), c));
    }

    private static boolean enCatalogo(Descriptor d) {
        return SuiteCombinaciones.contenido().stream().flatMap(c -> c.plugins().stream()).anyMatch(d::equals);
    }

    private static ConfigurableApplicationContext arrancar(Path config, Path plugins) {
        return new SpringApplicationBuilder(SynkraCoreApplication.class)
                .web(WebApplicationType.NONE)
                .properties("synkra.core.config=" + config, "synkra.core.plugins=" + plugins,
                        "spring.main.banner-mode=off")
                .run();
    }

    /** JAR con un plugin real; si {@code yamlCrudo} no es nulo se usa tal cual como synkra-plugin.yaml. */
    private void jar(Path plugins, String archivo, String nombre, String version, String core, String compatible,
            String yamlCrudo) throws IOException {
        String yaml = yamlCrudo != null ? yamlCrudo
                : "nombre: " + nombre + "\nversion: " + version + "\ncore: " + core
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
}
