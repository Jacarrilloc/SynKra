package io.synkra.verificador;

/** Códigos de motivo de rechazo (domain-entities.md, «Códigos de motivo»). */
public enum CodigoMotivo {
    DESCRIPTOR_FALTA_CAMPO,
    DESCRIPTOR_CAMPO_DESCONOCIDO,
    DESCRIPTOR_SIN_ARCHIVO,
    VERSION_INVALIDA,
    RANGO_INVALIDO,
    CORE_EQUIVOCADO,
    VERSION_INCOMPATIBLE,
    PLUGIN_REPETIDO,
    PLUGIN_AUSENTE
}
