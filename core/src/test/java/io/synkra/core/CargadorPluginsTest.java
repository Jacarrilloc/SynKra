package io.synkra.core;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.synkra.plugin.api.SynkraPlugin;

class CargadorPluginsTest {

    @TempDir
    Path tmp;

    /** Compila las fuentes (nombre de clase -> código) y empaqueta un JAR con los servicios dados. */
    private Path armarJar(String nombreJar, java.util.Map<String, String> fuentes, List<String> servicios)
            throws IOException {
        Path src = Files.createDirectories(tmp.resolve(nombreJar + "-src"));
        Path out = Files.createDirectories(tmp.resolve(nombreJar + "-out"));
        List<String> archivos = new ArrayList<>();
        for (var e : fuentes.entrySet()) {
            Path f = src.resolve(e.getKey() + ".java");
            Files.writeString(f, e.getValue());
            archivos.add(f.toString());
        }
        JavaCompiler jc = ToolProvider.getSystemJavaCompiler();
        assertNotNull(jc, "se necesita un JDK para compilar los plugins de prueba");
        List<String> args = new ArrayList<>(List.of("-d", out.toString(), "-cp", System.getProperty("java.class.path")));
        args.addAll(archivos);
        assertEquals(0, jc.run(null, null, null, args.toArray(String[]::new)));
        Path jar = tmp.resolve(nombreJar + ".jar");
        try (JarOutputStream jos = new JarOutputStream(Files.newOutputStream(jar))) {
            for (String clase : fuentes.keySet()) {
                jos.putNextEntry(new JarEntry(clase + ".class"));
                jos.write(Files.readAllBytes(out.resolve(clase + ".class")));
                jos.closeEntry();
            }
            if (!servicios.isEmpty()) {
                jos.putNextEntry(new JarEntry("META-INF/services/io.synkra.plugin.api.SynkraPlugin"));
                jos.write((String.join("\n", servicios) + "\n").getBytes(StandardCharsets.UTF_8));
                jos.closeEntry();
            }
        }
        return jar;
    }

    private static String plugin(String clase, String valorComun) {
        return "public class " + clase + " implements io.synkra.plugin.api.SynkraPlugin {\n"
                + "  public String nombre() { return " + clase + ".class.getName() + \":\" + Comun.valor(); }\n"
                + "  public void iniciar(io.synkra.plugin.api.ContextoPlugin c) {}\n"
                + "  public void detener() {}\n}\n";
    }

    private static String comun(String valor) {
        return "public class Comun { public static String valor() { return \"" + valor + "\"; } }\n";
    }

    private Path jarA() throws IOException {
        return armarJar("a", java.util.Map.of("PluginA", plugin("PluginA", "A"), "Comun", comun("A")),
                List.of("PluginA"));
    }

    private Path jarB() throws IOException {
        return armarJar("b", java.util.Map.of("PluginB", plugin("PluginB", "B"), "Comun", comun("B")),
                List.of("PluginB"));
    }

    @Test
    void dosPluginsTienenCargadoresDistintos() throws Exception {
        CargadorPlugins c = new CargadorPlugins();
        try (PluginCargado a = c.cargar(jarA()); PluginCargado b = c.cargar(jarB())) {
            assertNotSame(a.cargador(), b.cargador());
            assertNotSame(a.plugin().getClass().getClassLoader(), b.plugin().getClass().getClassLoader());
            assertSame(a.cargador(), a.plugin().getClass().getClassLoader());
        }
    }

    @Test
    void claseComunConElMismoNombreNoSePisa() throws Exception {
        CargadorPlugins c = new CargadorPlugins();
        try (PluginCargado a = c.cargar(jarA()); PluginCargado b = c.cargar(jarB())) {
            assertEquals("PluginA:A", a.plugin().nombre());
            assertEquals("PluginB:B", b.plugin().nombre());
        }
    }

    @Test
    void synkraPluginEsElMismoClassEnCoreYPlugin() throws Exception {
        CargadorPlugins c = new CargadorPlugins();
        try (PluginCargado a = c.cargar(jarA())) {
            assertInstanceOf(SynkraPlugin.class, a.plugin());
            Class<?> delPlugin = Class.forName(SynkraPlugin.class.getName(), false, a.cargador());
            assertSame(SynkraPlugin.class, delPlugin);
        }
    }

    @Test
    void jarSinImplementacionDaErrorQueNombraElJar() throws Exception {
        Path jar = armarJar("vacio", java.util.Map.of("Comun", comun("V")), List.of());
        ErrorCargaPlugin e = assertThrows(ErrorCargaPlugin.class, () -> new CargadorPlugins().cargar(jar));
        assertTrue(e.getMessage().contains("vacio.jar"), e.getMessage());
    }

    @Test
    void jarConDosImplementacionesDaErrorQueNombraElJar() throws Exception {
        Path jar = armarJar("doble",
                java.util.Map.of("PluginA", plugin("PluginA", "A"), "PluginB", plugin("PluginB", "B"),
                        "Comun", comun("D")),
                List.of("PluginA", "PluginB"));
        ErrorCargaPlugin e = assertThrows(ErrorCargaPlugin.class, () -> new CargadorPlugins().cargar(jar));
        assertTrue(e.getMessage().contains("doble.jar"), e.getMessage());
    }

    @Test
    void elCargadorQuedaCerradoTrasUnFallo() throws Exception {
        Path jar = armarJar("vacio2", java.util.Map.of("Comun", comun("V")), List.of());
        List<boolean[]> cerrados = new ArrayList<>();
        CargadorPlugins c = new CargadorPlugins() {
            @Override
            protected URLClassLoader crearCargador(URL[] urls, ClassLoader padre) {
                boolean[] cerrado = new boolean[1];
                cerrados.add(cerrado);
                return new URLClassLoader(urls, padre) {
                    @Override
                    public void close() throws IOException {
                        cerrado[0] = true;
                        super.close();
                    }
                };
            }
        };
        assertThrows(ErrorCargaPlugin.class, () -> c.cargar(jar));
        assertEquals(1, cerrados.size());
        assertTrue(cerrados.get(0)[0], "el cargador debe cerrarse si la carga falla");
    }

    @Test
    void jarInexistenteDaErrorQueNombraElJar() {
        Path jar = tmp.resolve("no-existe.jar");
        ErrorCargaPlugin e = assertThrows(ErrorCargaPlugin.class, () -> new CargadorPlugins().cargar(jar));
        assertTrue(e.getMessage().contains("no-existe.jar"), e.getMessage());
    }
}
