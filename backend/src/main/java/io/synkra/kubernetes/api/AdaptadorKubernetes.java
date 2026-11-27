package io.synkra.kubernetes.api;

/** Puerta del módulo Kubernetes. Los métodos de negocio llegan en U05-T02 a T06. */
public interface AdaptadorKubernetes {

    /** Verdadero solo si el pod tiene cuenta de servicio (token) y clúster al que hablar. */
    boolean disponible();
}
