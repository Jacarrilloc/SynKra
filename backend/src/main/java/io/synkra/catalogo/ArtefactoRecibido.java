package io.synkra.catalogo;

import java.nio.file.Path;

/** Un JAR ya copiado al temporal: ruta, SHA-256 (64 hex en minúsculas) y bytes. U04-T06 lo mueve a su sitio. */
record ArtefactoRecibido(Path temporal, String sha256, long tamano) {
}
