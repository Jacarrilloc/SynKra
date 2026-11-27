package io.synkra.catalogo.api;

/** Interfaz pública del módulo Catálogo: lo único que ven los demás módulos. */
public interface CatalogoApi {

    /** R-C1, R-C2: registra una versión de un core; crea el core con su primera versión. */
    CoreRegistrado registrarCore(String nombre, String version, String imagen);

    /** Devuelve el core con sus versiones; 404 si no existe. */
    CoreRegistrado obtenerCore(String nombre);
}
