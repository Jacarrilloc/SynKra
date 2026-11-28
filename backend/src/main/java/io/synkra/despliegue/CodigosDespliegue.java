package io.synkra.despliegue;

/** Códigos de error propios del módulo (los de plataforma viven en {@code CodigosError}). */
final class CodigosDespliegue {

    static final String PAIS_NO_PERMITIDO = "PAIS_NO_PERMITIDO";
    static final String SELECCION_INVALIDA = "SELECCION_INVALIDA";
    static final String CORE_NO_REGISTRADO = "CORE_NO_REGISTRADO";
    static final String COMBINACION_RECHAZADA = "COMBINACION_RECHAZADA";
    static final String KUBERNETES_NO_DISPONIBLE = "KUBERNETES_NO_DISPONIBLE";
    static final String PAIS_INCONSISTENTE = "PAIS_INCONSISTENTE";
    static final String PLAN_VENCIDO = "PLAN_VENCIDO";
    static final String PLAN_NO_PENDIENTE = "PLAN_NO_PENDIENTE";
    static final String CONFIRMACION_NO_DISPONIBLE = "CONFIRMACION_NO_DISPONIBLE";

    private CodigosDespliegue() {
    }
}
