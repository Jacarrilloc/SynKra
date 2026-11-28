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

    private MotivosDespliegue() {
    }
}
