package io.synkra.core;

/** Error propio del core al cargar un JAR de plugin. El mensaje nombra el JAR. */
public class ErrorCargaPlugin extends RuntimeException {

    public ErrorCargaPlugin(String mensaje) {
        super(mensaje);
    }

    public ErrorCargaPlugin(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
