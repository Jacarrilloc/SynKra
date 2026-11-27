package io.synkra.kubernetes.api;

/**
 * Lectura puntual del Deployment {@code synkra-core} y sus pods.
 *
 * @param replicasDeseadas réplicas pedidas por el Deployment
 * @param replicasListas réplicas que Kubernetes da por listas
 * @param reinicios suma de reinicios de los contenedores (y del init) de los pods del país
 * @param fase {@code LISTO}, {@code EN_CURSO} o {@code FALLIDO}
 * @param razon con {@code FALLIDO}, la razón tal como la da Kubernetes (por ejemplo {@code CrashLoopBackOff},
 *     {@code Init:Error}); en otro caso es nula
 */
public record EstadoPais(int replicasDeseadas, int replicasListas, int reinicios, FaseDespliegue fase, String razon) {}
