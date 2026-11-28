package io.synkra.despliegue;

/** Un plugin del resumen o de los cambios: nombre, versión y sha256 de su JAR. Se compara entero (R-D4). */
record PluginResumen(String nombre, String version, String sha256) {}
