package io.synkra.core;

/** El verificador (o la lectura de la configuración) rechazó el arranque; hace fallar el contexto de Spring. */
public class ArranqueRechazado extends RuntimeException {

    public ArranqueRechazado(String mensaje) {
        super(mensaje);
    }
}
