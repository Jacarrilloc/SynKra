package io.synkra.despliegue;

/** Textos de {@code motivo} de los errores del módulo: constantes, sin repetir lo recibido (C-34). */
final class MotivosDespliegue {

    static final String PAIS_MENSAJE = "El país no está permitido";
    static final String PAIS_FORMA = "El país debe ser un código de dos letras en minúscula (S4)";
    static final String PAIS_FUERA_DE_LISTA = "El país no está entre los configurados (SYNKRA_PAISES)";

    static final String SELECCION_MENSAJE = "La selección no es válida";
    static final String SELECCION_CORE = "El core debe cumplir el patrón de nombres (minúsculas, dígitos y guiones)";
    static final String SELECCION_VERSION_CORE = "La versión del core debe tener la forma MAYOR.MENOR.PARCHE";
    static final String SELECCION_PLUGIN_NULO = "El plugin no puede ser nulo";
    static final String SELECCION_NOMBRE = "El nombre del plugin debe cumplir el patrón de nombres (minúsculas, dígitos y guiones)";
    static final String SELECCION_VERSION = "La versión del plugin debe tener la forma MAYOR.MENOR.PARCHE";

    static final String CORE_MENSAJE = "El core no está registrado";
    static final String CORE_SIN_NOMBRE = "No hay ningún core con ese nombre en el catálogo";
    static final String CORE_SIN_VERSION = "El core no tiene registrada esa versión";

    static final String COMBINACION_MENSAJE = "La combinación no es compatible";
    static final String KUBERNETES_MENSAJE = "No se puede leer lo que corre en el país";
    static final String PAIS_INCONSISTENTE_MENSAJE = "Lo que corre en el país no es consistente";
    static final String PLAN_VENCIDO_MENSAJE = "El plan venció sin confirmarse";
    static final String PLAN_NO_PENDIENTE_MENSAJE = "El plan ya no está pendiente";
    static final String DESPLIEGUE_EN_CURSO_MENSAJE = "Ya hay un despliegue en curso en el país";
    static final String PLAN_DESACTUALIZADO_MENSAJE = "El catálogo cambió desde que se creó el plan";
    static final String APLICACION_FALLIDA_MENSAJE = "No se pudo aplicar el plan en el clúster";
    static final String KUBERNETES_APLICAR_MENSAJE = "No hay clúster al que aplicar el plan";
    static final String USUARIO_MENSAJE = "El usuario no es válido";
    static final String USUARIO_FORMA = "El usuario debe tener de 1 a 64 caracteres: minúsculas, dígitos, punto, guion o guion bajo";

    private MotivosDespliegue() {
    }
}
