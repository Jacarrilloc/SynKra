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
        return cargar(jar, SynkraPlugin.class.getClassLoader());
    }

    /** Costura de prueba: permite fijar el cargador padre. */
    PluginCargado cargar(Path jar, ClassLoader padre) {
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
        URLClassLoader cargador = crearCargador(new URL[] {url}, padre);
        boolean exito = false;
        try {
            // Solo cuentan los proveedores definidos por el cargador del JAR, no los del padre.
            List<SynkraPlugin> encontrados = new ArrayList<>();
            ServiceLoader.load(SynkraPlugin.class, cargador).stream()
                    .filter(p -> p.type().getClassLoader() == cargador)
                    .forEach(p -> encontrados.add(p.get()));
            if (encontrados.isEmpty()) {
                throw new ErrorCargaPlugin("El JAR " + nombre + " no contiene ninguna implementación de SynkraPlugin");
            }
            if (encontrados.size() > 1) {
                throw new ErrorCargaPlugin("El JAR " + nombre + " contiene " + encontrados.size()
                        + " implementaciones de SynkraPlugin; se espera una");
            }
            PluginCargado cargado = new PluginCargado(encontrados.get(0), cargador);
            exito = true;
            return cargado;
        } catch (ServiceConfigurationError | LinkageError e) {
            throw new ErrorCargaPlugin("El JAR " + nombre + " no se pudo cargar: " + e, e);
        } finally {
            if (!exito) {
                cerrar(cargador);
            }
        }
    }

    /** Crea el cargador propio del JAR (visible en el paquete para las pruebas). */
    URLClassLoader crearCargador(URL[] urls, ClassLoader padre) {
        return new URLClassLoader(urls, padre);
    }

    private static void cerrar(URLClassLoader cargador) {
        try {
            cargador.close();
        } catch (IOException e) {
            // el fallo original es el que importa; no se oculta con uno de cierre
        }
    }
}
