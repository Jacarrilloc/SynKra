package io.synkra.kubernetes.api;

/** La API de Kubernetes rechazó o no pudo aplicar un objeto; nombra el tipo y el nombre del objeto que falló. */
public class AplicacionFallidaException extends RuntimeException {

    private final String objeto;

    public AplicacionFallidaException(String objeto, Throwable causa) {
        super("No se pudo aplicar " + objeto, causa);
        this.objeto = objeto;
    }

    /** Tipo y nombre del objeto que falló, por ejemplo {@code Service/core-pe}. */
    public String objeto() {
        return objeto;
    }
}
