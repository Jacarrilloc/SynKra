package io.synkra.catalogo.api;

import java.io.InputStream;

/**
 * Un JAR del catálogo listo para servir (R-C13): su flujo, su tamaño verificado contra el disco y el nombre de
 * descarga. Quien lo recibe debe cerrar {@code flujo}.
 */
public record ArtefactoAbierto(String sha256, long tamano, String nombreDescarga, InputStream flujo) {}
