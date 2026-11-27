package io.synkra.kubernetes.api;

/** No hay cliente de Kubernetes (el pod no tiene cuenta de servicio); se lanza sin hacer ninguna petición. */
public class ClienteNoDisponibleException extends IllegalStateException {

    public ClienteNoDisponibleException() {
        super("No hay cliente de Kubernetes: el pod no tiene cuenta de servicio");
    }
}
