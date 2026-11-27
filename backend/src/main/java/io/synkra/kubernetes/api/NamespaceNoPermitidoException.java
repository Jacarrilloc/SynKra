package io.synkra.kubernetes.api;

/** El namespace no tiene la forma {@code synkra-<país>}; se lanza antes de cualquier petición a la API. */
public class NamespaceNoPermitidoException extends RuntimeException {

    private static final int MAXIMO = 63;

    public NamespaceNoPermitidoException(String namespace) {
        super("Namespace no permitido: " + recorta(namespace));
    }

    private static String recorta(String namespace) {
        if (namespace == null) {
            return "null";
        }
        return namespace.length() <= MAXIMO ? namespace : namespace.substring(0, MAXIMO);
    }
}
