package io.synkra.plataforma;

/** Codigos y mensajes de plataforma de {@link ErrorApi}: contrato de la API, un solo sitio. */
public final class CodigosError {

    public static final String SOLICITUD_INVALIDA = "SOLICITUD_INVALIDA";
    public static final String NO_ENCONTRADO = "NO_ENCONTRADO";
    public static final String METODO_NO_PERMITIDO = "METODO_NO_PERMITIDO";
    public static final String CONFLICTO = "CONFLICTO";
    public static final String DEMASIADO_GRANDE = "DEMASIADO_GRANDE";
    public static final String TIPO_NO_SOPORTADO = "TIPO_NO_SOPORTADO";
    public static final String ENTIDAD_NO_PROCESABLE = "ENTIDAD_NO_PROCESABLE";
    public static final String ERROR_INTERNO = "ERROR_INTERNO";

    public static final String MSG_SOLICITUD_INVALIDA = "La solicitud es inválida o su cuerpo no se puede leer";
    public static final String MSG_NO_ENCONTRADO = "Recurso no encontrado";
    public static final String MSG_METODO_NO_PERMITIDO = "Método no permitido";
    public static final String MSG_CONFLICTO = "La solicitud entra en conflicto con el estado actual";
    public static final String MSG_DEMASIADO_GRANDE = "El archivo supera el tamaño permitido";
    /** C-38: el 413 de un cuerpo JSON (no es un archivo). */
    public static final String MSG_CUERPO_DEMASIADO_GRANDE = "El cuerpo de la solicitud supera el tamaño permitido";
    public static final String MSG_TIPO_NO_SOPORTADO = "Tipo de contenido no soportado";
    public static final String MSG_ENTIDAD_NO_PROCESABLE = "La solicitud no se puede procesar";
    public static final String MSG_ERROR_INTERNO = "Error interno del servidor";

    /** Motivos de respaldo de las validaciones (C-07): se usan. */
    public static final String MOTIVO_OBLIGATORIO = "El campo es obligatorio";
    public static final String MOTIVO_TIPO_INVALIDO = "El valor no tiene el tipo esperado";
    /** Campo cuando un parámetro validado no tiene nombre. */
    public static final String PARAMETRO_SIN_NOMBRE = "parametro";

    /** C-38: campo y motivo cuando el cuerpo JSON pasa el tope; se completa con el tope en bytes. */
    public static final String CAMPO_CUERPO = "cuerpo";
    public static final String MOTIVO_JSON_GRANDE = "El cuerpo JSON supera el límite de %d bytes";

    private CodigosError() {
    }
}
