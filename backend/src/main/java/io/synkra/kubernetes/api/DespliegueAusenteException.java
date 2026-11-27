package io.synkra.kubernetes.api;

/** El Deployment del core no existe en el namespace; no se inventa un estado. */
public class DespliegueAusenteException extends RuntimeException {

    public DespliegueAusenteException(String deployment, String namespace) {
        super("No existe el Deployment " + deployment + " en el namespace " + namespace);
    }
}
