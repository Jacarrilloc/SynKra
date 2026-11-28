package io.synkra.registro.api;

/** El cambio de estado pedido no es {@code aplicando → listo} ni {@code aplicando → fallido} (R-R2). */
public class TransicionNoPermitidaException extends RuntimeException {

    public TransicionNoPermitidaException(String mensaje) {
        super(mensaje);
    }
}
