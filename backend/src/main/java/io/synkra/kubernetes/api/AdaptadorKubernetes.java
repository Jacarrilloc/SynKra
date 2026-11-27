package io.synkra.kubernetes.api;

/** Puerta del módulo Kubernetes. Los métodos de negocio llegan en U05-T02 a T06. */
public interface AdaptadorKubernetes {

    /** Verdadero solo si el pod tiene cuenta de servicio (token) y clúster al que hablar. */
    boolean disponible();

    /**
     * Verdadero si el namespace existe en el clúster. Método mínimo de U05-T02 para ejercer la guardia.
     *
     * @throws NamespaceNoPermitidoException si no es {@code synkra-<país>} (sin tocar la red)
     */
    boolean existeNamespace(String namespace);
}
