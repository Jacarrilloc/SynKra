package io.synkra.kubernetes.api;

/** La API de Kubernetes falló al leer; nombra qué se leía y en qué namespace. */
public class LecturaFallidaException extends RuntimeException {

    public LecturaFallidaException(String que, String namespace, Throwable causa) {
        super("No se pudo leer " + que + " en " + namespace, causa);
    }
}
