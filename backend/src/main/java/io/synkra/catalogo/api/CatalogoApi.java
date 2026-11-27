package io.synkra.catalogo.api;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

/** Interfaz pública del módulo Catálogo: lo único que ven los demás módulos. */
public interface CatalogoApi {

    /** R-C1, R-C2: registra una versión de un core; crea el core con su primera versión. */
    CoreRegistrado registrarCore(String nombre, String version, String imagen);

    /** Devuelve el core con sus versiones; 404 si no existe. */
    CoreRegistrado obtenerCore(String nombre);

    /** Todos los cores con sus versiones, ordenados por nombre. */
    List<CoreRegistrado> listarCores();

    /**
     * R-C4 a R-C11: sube el JAR de un plugin a la carpeta del core, con las etiquetas de la petición. Un mapa nulo
     * es un mapa vacío; una clave nula o fuera del patrón de U01, o un valor nulo, dan 422. {@link IOException} solo
     * por fallos del servidor (disco); los rechazos son {@code ExcepcionApi}.
     */
    PluginSubido subirPlugin(String core, InputStream jar, Map<String, String> etiquetas) throws IOException;

    /** El plugin {@code nombre} en la versión {@code version} del core; 404 si no existe. */
    PluginSubido obtenerPlugin(String core, String nombre, String version);
}
