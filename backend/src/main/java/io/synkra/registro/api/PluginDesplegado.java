package io.synkra.registro.api;

import java.util.Map;

/** Un plugin del despliegue: nombre, versión, sha256 (64 hexadecimales en minúsculas) y etiquetas. */
public record PluginDesplegado(String nombre, String version, String sha256, Map<String, String> etiquetas) {}
