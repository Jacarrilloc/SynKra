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
import java.util.Map;
import java.util.Set;
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
    private Path armarJar(String nombreJar, Map<String, String> fuentes, List<String> servicios)
            throws IOException {
        return armarJar(nombreJar, fuentes, servicios, Set.of());
    }

    /** Como el anterior, pero las clases de {@code excluir} se compilan y no se empaquetan. */
    private Path armarJar(String nombreJar, Map<String, String> fuentes, List<String> servicios,
            Set<String> excluir) throws IOException {
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
                if (excluir.contains(clase)) {
                    continue;
                }
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

    private static String plugin(String clase) {
        return "public class " + clase + " implements io.synkra.plugin.api.SynkraPlugin {\n"
                + "  public String nombre() { return " + clase + ".class.getName() + \":\" + Comun.valor(); }\n"
                + "  public void iniciar(io.synkra.plugin.api.ContextoPlugin c) {}\n"
                + "  public void detener() {}\n}\n";
    }

    private static String comun(String valor) {
        return "public class Comun { public static String valor() { return \"" + valor + "\"; } }\n";
    }

    private Path jarA() throws IOException {
        return armarJar("a", Map.of("PluginA", plugin("PluginA"), "Comun", comun("A")),
                List.of("PluginA"));
    }

    private Path jarB() throws IOException {
        return armarJar("b", Map.of("PluginB", plugin("PluginB"), "Comun", comun("B")),
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
        Path jar = armarJar("vacio", Map.of("Comun", comun("V")), List.of());
        ErrorCargaPlugin e = assertThrows(ErrorCargaPlugin.class, () -> new CargadorPlugins().cargar(jar));
        assertTrue(e.getMessage().contains("vacio.jar"), e.getMessage());
    }

    @Test
    void jarConDosImplementacionesDaErrorQueNombraElJar() throws Exception {
        Path jar = armarJar("doble",
                Map.of("PluginA", plugin("PluginA"), "PluginB", plugin("PluginB"),
                        "Comun", comun("D")),
                List.of("PluginA", "PluginB"));
        ErrorCargaPlugin e = assertThrows(ErrorCargaPlugin.class, () -> new CargadorPlugins().cargar(jar));
        assertTrue(e.getMessage().contains("doble.jar"), e.getMessage());
    }

    /** Cargador de pruebas que registra si cada URLClassLoader que crea fue cerrado. */
    private static final class CargadorEspia extends CargadorPlugins {
        final List<boolean[]> cerrados = new ArrayList<>();

        @Override
        URLClassLoader crearCargador(URL[] urls, ClassLoader padre) {
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

        void afirmarCerrado() {
            assertEquals(1, cerrados.size());
            assertTrue(cerrados.get(0)[0], "el cargador debe cerrarse si la carga falla");
        }
    }

    private void afirmarFalla(Path jar, String nombreJar) {
        CargadorEspia c = new CargadorEspia();
        ErrorCargaPlugin e = assertThrows(ErrorCargaPlugin.class, () -> c.cargar(jar));
        assertTrue(e.getMessage().contains(nombreJar), e.getMessage());
        c.afirmarCerrado();
    }

    @Test
    void elCargadorQuedaCerradoTrasUnFallo() throws Exception {
        afirmarFalla(armarJar("vacio2", Map.of("Comun", comun("V")), List.of()), "vacio2.jar");
    }

    @Test
    void proveedorInexistenteDaErrorQueNombraElJarYCierra() throws Exception {
        afirmarFalla(armarJar("fantasma", Map.of("Comun", comun("F")), List.of("NoExiste")), "fantasma.jar");
    }

    @Test
    void claseFaltanteDaErrorQueNombraElJarYCierra() throws Exception {
        String base = "public class Base {}\n";
        String hijo = "public class PluginL extends Base implements io.synkra.plugin.api.SynkraPlugin {\n"
                + "  public String nombre() { return \"L\"; }\n"
                + "  public void iniciar(io.synkra.plugin.api.ContextoPlugin c) {}\n"
                + "  public void detener() {}\n}\n";
        Path jar = armarJar("sinbase", Map.of("Base", base, "PluginL", hijo), List.of("PluginL"), Set.of("Base"));
        afirmarFalla(jar, "sinbase.jar");
    }

    @Test
    void ignoraLosProveedoresDelCargadorPadre() throws Exception {
        // Un proveedor visible solo desde el padre no cuenta ni se devuelve.
        Path jarPadre = armarJar("padre", Map.of("PluginP", plugin("PluginP").replace(" + \":\" + Comun.valor()", "")),
                List.of("PluginP"));
        try (URLClassLoader padre = new URLClassLoader(new URL[] {jarPadre.toUri().toURL()},
                SynkraPlugin.class.getClassLoader())) {
            Path vacio = armarJar("vacio3", Map.of("Comun", comun("V")), List.of());
            ErrorCargaPlugin e = assertThrows(ErrorCargaPlugin.class, () -> new CargadorPlugins().cargar(vacio, padre));
            assertTrue(e.getMessage().contains("vacio3.jar"), e.getMessage());

            try (PluginCargado b = new CargadorPlugins().cargar(jarB(), padre)) {
                assertEquals("PluginB:B", b.plugin().nombre());
            }
        }
    }

    @Test
    void jarInexistenteDaErrorQueNombraElJar() {
        Path jar = tmp.resolve("no-existe.jar");
        ErrorCargaPlugin e = assertThrows(ErrorCargaPlugin.class, () -> new CargadorPlugins().cargar(jar));
        assertTrue(e.getMessage().contains("no-existe.jar"), e.getMessage());
    }
}
