package io.synkra.kubernetes.api;

/** El Deployment {@code synkra-core} no existe en el namespace; no se inventa un estado. */
public class DespliegueAusenteException extends RuntimeException {

    public DespliegueAusenteException(String namespace) {
        super("No existe el Deployment synkra-core en el namespace " + namespace);
    }
}
