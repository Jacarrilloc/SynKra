package io.synkra.despliegue;

/** Un plugin de la petición: nombre y versión, tal como llegan (se validan en {@code VerificacionVivo}). */
record PluginPedido(String nombre, String version) {}
