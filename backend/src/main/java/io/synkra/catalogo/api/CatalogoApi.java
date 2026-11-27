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

    /**
     * R-C12: los plugins de la carpeta de {@code core} que cumplen todas las parejas {@code clave=valor} del filtro
     * (cualquier origen, comparación exacta). Un filtro nulo o vacío devuelve todos. Orden: nombre y versión
     * semántica. 404 si el core no existe; 422 si una clave no cumple el patrón de U01 o un valor es nulo.
     */
    List<PluginSubido> listarPlugins(String core, Map<String, String> filtro);

    /**
     * La vista de solo lectura del catálogo que usa el {@code Verificador} de U01, leída de SQLite en cada llamada
     * (sin caché). Nunca devuelve {@code null}; una fila corrupta es un error interno.
     */
    io.synkra.verificador.Catalogo catalogoParaVerificar();
}
