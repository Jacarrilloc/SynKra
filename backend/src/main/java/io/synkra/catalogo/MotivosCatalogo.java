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
    static final String ENTRADAS_NO_COINCIDEN =
            "El JAR tiene entradas que no coinciden entre su índice y su contenido";
    /** Se completa con el tope de bytes descomprimidos configurado. */
    static final String DESCOMPRIMIDO_EXCEDIDO = "El contenido descomprimido supera el límite de %s";

    static final String ENTRADAS_DUPLICADAS = "El JAR tiene entradas con el mismo nombre";

    /** Se completa con el core del descriptor y el de la carpeta (R-C8). */
    static final String CORE_EQUIVOCADO = "El descriptor es del core '%s' y la carpeta es del core '%s'";
    /** Se completa con el nombre y la versión del plugin (R-C9). */
    static final String PLUGIN_REPETIDO = "El plugin %s ya tiene la versión %s en este core";
    /** Se completa con la clave, el valor del descriptor y el de la petición, recortados (R-C10). */
    static final String ETIQUETA_EN_CONFLICTO = "La etiqueta '%s' vale '%s' en el descriptor y '%s' en la petición";
    /** Se completa con la clave recortada; el patrón es el de U01 ({@code Descriptor.NOMBRE}). */
    static final String ETIQUETA_CLAVE_INVALIDA =
            "La clave de etiqueta '%s' debe llevar solo minúsculas, dígitos y guiones medios, sin guion al "
                    + "principio ni al final";
    /** Se completa con la clave recortada (C-50); el tope es {@code RutasPlugins.MAX_VALOR_ETIQUETA}. */
    static final String ETIQUETA_VALOR_LARGO = "El valor de la etiqueta '%s' supera los " + RutasPlugins.MAX_VALOR_ETIQUETA
            + " caracteres";
    static final String ARCHIVO_OBLIGATORIO = "Falta la parte 'archivo' con el JAR del plugin";
    static final String ETIQUETAS_NO_ES_OBJETO = "La parte 'etiquetas' debe ser un objeto JSON de texto a texto";
    /** Se completa con la clave recortada; la clave aparece más de una vez en la consulta del listado. */
    static final String ETIQUETA_REPETIDA = "La clave '%s' aparece más de una vez en el filtro";
    /** Se completa con el tope de parejas ({@code ListadoPlugins.MAX_PAREJAS}). */
    static final String FILTRO_LARGO = "El filtro tiene más de %d parejas clave=valor";
    static final String CONSULTA_INVALIDA = "La consulta tiene una codificación de porcentaje no válida";
    static final String ETIQUETA_SIN_VALOR = "La etiqueta '%s' no tiene valor";
    /** Último detalle cuando hay más de {@code SubidaPlugin.MAX_DETALLES} (C-19); se completa con los omitidos. */
    static final String MOTIVOS_OMITIDOS = "Se omitieron %d motivos más";

    private MotivosCatalogo() {
    }
}
