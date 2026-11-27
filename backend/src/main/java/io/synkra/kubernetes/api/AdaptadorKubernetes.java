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

    /**
     * Arma Deployment, ConfigMap y Service del país en {@code synkra-<país>}, sin hablar con la API.
     *
     * @throws NamespaceNoPermitidoException si el namespace derivado no pasa la guardia
     */
    ObjetosPais construir(PlanPais plan);

    /**
     * Aplica los tres objetos con server-side apply (field manager {@code synkra}), en el orden ConfigMap,
     * Service y Deployment. Si uno falla, los anteriores no se deshacen.
     *
     * @throws IllegalArgumentException si los objetos no comparten namespace (sin peticiones)
     * @throws NamespaceNoPermitidoException si el namespace no pasa la guardia (sin peticiones)
     * @throws IllegalStateException si no hay cliente (el pod no tiene cuenta de servicio); sin peticiones
     * @throws AplicacionFallidaException si la API falla; nombra el objeto
     */
    void aplicar(ObjetosPais objetos);
}
