package io.synkra.kubernetes.api;

/** Fase del despliegue de un país, según lo que muestran el Deployment y sus pods en una sola lectura. */
public enum FaseDespliegue {
    LISTO,
    EN_CURSO,
    FALLIDO
}
