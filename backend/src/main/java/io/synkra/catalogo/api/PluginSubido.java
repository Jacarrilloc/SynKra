package io.synkra.catalogo.api;

import io.synkra.verificador.RangoVersion;
import io.synkra.verificador.Version;
import java.util.Map;

/** Un plugin del catálogo: lo que declara su descriptor, las etiquetas unidas (R-C10) y el SHA-256 de su JAR. */
public record PluginSubido(
        String core,
        String nombre,
        Version version,
        RangoVersion coreCompatible,
        Map<String, String> etiquetas,
        String sha256,
        long tamano) {

    public PluginSubido {
        etiquetas = Map.copyOf(etiquetas);
    }
}
