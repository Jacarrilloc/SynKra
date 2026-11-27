package io.synkra.core;

import io.synkra.plugin.api.SynkraPlugin;
import java.io.IOException;
import java.net.URLClassLoader;

/** Un plugin cargado junto con su cargador, para que quien lo use pueda cerrarlo. */
public record PluginCargado(SynkraPlugin plugin, URLClassLoader cargador) implements AutoCloseable {

    @Override
    public void close() throws IOException {
        cargador.close();
    }
}
