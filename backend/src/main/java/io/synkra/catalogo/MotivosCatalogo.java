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

    private MotivosCatalogo() {
    }
}
