package io.synkra.registro.api;

/** Cómo salió la lectura del clúster de un país (solo la lectura, no el estado de los pods). */
public enum LecturaCluster {
    /** No hay Deployment ni ConfigMap de SynKra en el país. */
    VACIO,
    /** Se leyó un despliegue válido. */
    LEIDO,
    /** Lo que hay en el clúster no es un despliegue válido de SynKra. */
    INCONSISTENTE,
    /** No se pudo leer (sin cliente o la API falló): no se sabe qué corre. */
    NO_DISPONIBLE
}
