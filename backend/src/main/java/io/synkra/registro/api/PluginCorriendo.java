package io.synkra.registro.api;

/** Un plugin que corre de verdad en un país, leído del ConfigMap del clúster. */
public record PluginCorriendo(String nombre, String version, String sha256) {}
