package io.synkra.kubernetes.api;

/** Lo que hay en el clúster no cuadra (falta un objeto, un plugin en un solo archivo, un valor inválido); nombra el país. */
public class LecturaPaisInconsistenteException extends RuntimeException {

    public LecturaPaisInconsistenteException(String pais, String detalle) {
        super("Lectura inconsistente del país " + pais + ": " + detalle);
    }

    public LecturaPaisInconsistenteException(String pais, String detalle, Throwable causa) {
        super("Lectura inconsistente del país " + pais + ": " + detalle, causa);
    }
}
