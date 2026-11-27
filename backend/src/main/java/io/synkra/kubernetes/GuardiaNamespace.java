package io.synkra.kubernetes;

import io.synkra.kubernetes.api.NamespaceNoPermitidoException;
import java.util.regex.Pattern;

/**
 * Segunda capa (D13): solo deja pasar {@code synkra-<país>} con país de dos letras minúsculas ASCII.
 * Comprueba la forma, no la lista de países (R-D1, U07-T02).
 */
final class GuardiaNamespace {

    private static final Pattern FORMA = Pattern.compile("synkra-[a-z]{2}");

    private GuardiaNamespace() {}

    /** Devuelve el namespace si es válido; si no, lanza antes de que nadie hable con la API. */
    static String exigir(String namespace) {
        if (namespace == null || !FORMA.matcher(namespace).matches()) {
            throw new NamespaceNoPermitidoException(namespace);
        }
        return namespace;
    }
}
