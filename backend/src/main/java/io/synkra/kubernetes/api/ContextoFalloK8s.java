package io.synkra.kubernetes.api;

/**
 * Texto para diagnosticar un fallo, sin truncar a 64 KB (eso es de U06). Un campo sin dato queda vacío, nunca nulo.
 *
 * @param logCore últimas 200 líneas del contenedor {@code core}
 * @param logInit log completo del init {@code descarga}
 * @param eventos eventos de los pods del país, uno por línea, {@code tipo razón: mensaje}, del más antiguo al más nuevo
 */
public record ContextoFalloK8s(String logCore, String logInit, String eventos) {}
