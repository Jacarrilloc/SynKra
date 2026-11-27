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

    private MotivosCatalogo() {
    }
}
