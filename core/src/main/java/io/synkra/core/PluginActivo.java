package io.synkra.core;

/** Un plugin activo: nombre y versión del descriptor del JAR (no del plugin) y el plugin cargado. */
public record PluginActivo(String nombre, String version, PluginCargado cargado) {}
