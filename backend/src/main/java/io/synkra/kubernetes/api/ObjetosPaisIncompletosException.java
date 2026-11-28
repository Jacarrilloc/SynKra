package io.synkra.kubernetes.api;

/** Los objetos que se quieren aplicar no son los que armó {@code construir}: falta uno o no trae metadatos. */
public class ObjetosPaisIncompletosException extends IllegalArgumentException {

    public ObjetosPaisIncompletosException(String detalle) {
        super("Los objetos del país están incompletos: " + detalle);
    }
}
