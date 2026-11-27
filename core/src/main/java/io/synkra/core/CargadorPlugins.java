package io.synkra.core;

import io.synkra.plugin.api.SynkraPlugin;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

/**
 * Carga un plugin por JAR, cada uno en su propio {@link URLClassLoader}. El padre es el cargador de
 * {@code plugin-api}, así {@link SynkraPlugin} es el mismo tipo en el core y en el plugin.
 */
public class CargadorPlugins {

    public PluginCargado cargar(Path jar) {
        String nombre = jar.getFileName() == null ? jar.toString() : jar.getFileName().toString();
        if (!Files.isRegularFile(jar)) {
            throw new ErrorCargaPlugin("JAR de plugin no encontrado: " + nombre);
        }
        URL url;
        try {
            url = jar.toUri().toURL();
        } catch (MalformedURLException e) {
            throw new ErrorCargaPlugin("Ruta de JAR no válida: " + nombre, e);
        }
        URLClassLoader cargador = crearCargador(new URL[] {url}, SynkraPlugin.class.getClassLoader());
        try {
            List<SynkraPlugin> encontrados = new ArrayList<>();
            for (SynkraPlugin p : ServiceLoader.load(SynkraPlugin.class, cargador)) {
                encontrados.add(p);
            }
            if (encontrados.isEmpty()) {
                throw new ErrorCargaPlugin("El JAR " + nombre + " no contiene ninguna implementación de SynkraPlugin");
            }
            if (encontrados.size() > 1) {
                throw new ErrorCargaPlugin("El JAR " + nombre + " contiene " + encontrados.size()
                        + " implementaciones de SynkraPlugin; se espera una");
            }
            return new PluginCargado(encontrados.get(0), cargador);
        } catch (ServiceConfigurationError e) {
            cerrar(cargador, e);
            throw new ErrorCargaPlugin("El JAR " + nombre + " no se pudo cargar: " + e.getMessage(), e);
        } catch (RuntimeException e) {
            cerrar(cargador, e);
            throw e;
        }
    }

    /** Punto de extensión: crea el cargador propio del JAR. */
    protected URLClassLoader crearCargador(URL[] urls, ClassLoader padre) {
        return new URLClassLoader(urls, padre);
    }

    private static void cerrar(URLClassLoader cargador, Throwable causa) {
        try {
            cargador.close();
        } catch (IOException e) {
            causa.addSuppressed(e);
        }
    }
}
