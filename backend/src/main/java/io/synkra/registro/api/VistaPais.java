package io.synkra.registro.api;

import java.util.List;

/**
 * Una fila de la vista general (H-19). {@code imagen} y {@code plugins} son lo leído del clúster (con
 * {@code lectura != LEIDO}: imagen nula y plugins vacía, nunca nula). {@code ultimoDespliegueId}, {@code core} y
 * {@code versionCore} son del último despliegue {@code listo} del registro, o nulos si no hay. {@code concuerda} es
 * nulo cuando no se pudo leer el clúster (no se afirma nada sin haber leído).
 */
public record VistaPais(
        String pais,
        LecturaCluster lectura,
        String imagen,
        List<PluginCorriendo> plugins,
        Boolean concuerda,
        Long ultimoDespliegueId,
        String core,
        String versionCore) {}
