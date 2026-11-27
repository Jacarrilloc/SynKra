package io.synkra.catalogo;

/** Textos de {@code motivo} de los errores del catálogo: un solo sitio dentro del módulo. */
final class MotivosCatalogo {

    static final String NOMBRE_INVALIDO =
            "El nombre debe llevar solo minúsculas, dígitos y guiones medios, sin guion al principio ni al final";
    static final String IMAGEN_INVALIDA =
            "La imagen debe tener la forma registro/repo[:tag]@sha256:<64 hex en minúsculas>, de hasta "
                    + CatalogoServicio.IMAGEN_MAX + " caracteres";
    /** Se completa con la versión repetida. */
    static final String VERSION_REPETIDA = "La versión %s del core ya está registrada";

    /** Se completa con el límite configurado, por ejemplo «20 MB». */
    static final String ARTEFACTO_GRANDE = "El artefacto supera el límite de %s";
    static final String ARTEFACTO_NO_ES_ZIP = "El archivo no es un JAR (ZIP) legible";
    /** Se completa con el nombre de la entrada, recortado a 200 caracteres. */
    static final String ENTRADA_NO_PERMITIDA = "La entrada '%s' tiene una ruta no permitida";
    static final String ENTRADA_ENLACE = "La entrada '%s' es un enlace simbólico";

    /** Unidades del límite en {@link #ARTEFACTO_GRANDE}. */
    static final String UNIDAD_MB = "MB";
    static final String UNIDAD_KB = "KB";
    static final String UNIDAD_BYTES = "bytes";
    /** Se completa con el tope de entradas configurado. */
    static final String DEMASIADAS_ENTRADAS = "El artefacto tiene más de %d entradas";
    static final String ENTRADA_UNICODE_DISTINTA =
            "La entrada '%s' lleva un nombre Unicode distinto del nombre crudo";
    static final String ENTRADA_LOCAL_DISTINTA =
            "La entrada '%s' tiene una cabecera local distinta de la del directorio central";
    static final String ESTRUCTURA_AMBIGUA =
            "El JAR tiene datos o entradas locales fuera del directorio central";

    private MotivosCatalogo() {
    }
}
