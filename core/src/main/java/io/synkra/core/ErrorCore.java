package io.synkra.core;

/** Cuerpo JSON de error de los endpoints del core: {@code {"codigo": ..., "mensaje": ...}}. */
public record ErrorCore(String codigo, String mensaje) {

    /** El plugin pedido no está activo y iniciado. */
    public static final String PLUGIN_NO_ACTIVO = "PLUGIN_NO_ACTIVO";
    /** El plugin está activo pero no implementa {@code PuntoKyc}. */
    public static final String PLUGIN_SIN_KYC = "PLUGIN_SIN_KYC";
    /** El cuerpo no es una {@code SolicitudKyc} válida. */
    public static final String SOLICITUD_INVALIDA = "SOLICITUD_INVALIDA";
    /** El plugin lanzó una excepción al validar. */
    public static final String PLUGIN_FALLO = "PLUGIN_FALLO";

    static final String MENSAJE_NO_ACTIVO = "El plugin no está activo: ";
    static final String MENSAJE_SIN_KYC = "El plugin no ofrece validación KYC: ";
    static final String MENSAJE_SOLICITUD_INVALIDA =
            "El cuerpo debe ser una solicitud KYC con documento (texto) y datos (mapa de texto)";
    static final String MENSAJE_PLUGIN_FALLO = "El plugin falló al procesar la solicitud: ";
}
