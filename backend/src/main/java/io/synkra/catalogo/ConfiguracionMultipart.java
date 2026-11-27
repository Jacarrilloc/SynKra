package io.synkra.catalogo;

import jakarta.servlet.MultipartConfigElement;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.unit.DataSize;

/**
 * Límites de multipart por encima del tope del artefacto (R-C5), para que un JAR demasiado grande lo rechace
 * {@link RecepcionArtefacto} con su 413 y no Tomcat. Un cuerpo aún mayor lo corta Tomcat y {@code ManejadorErrores}
 * lo traduce a un 413 con {@code ErrorApi}. Sustituye al bean de Spring Boot, que lee {@code spring.servlet.multipart.*}.
 */
@Configuration
class ConfiguracionMultipart {

    /** Margen de la parte del JAR sobre el tope del artefacto. */
    static final DataSize MARGEN_ARCHIVO = DataSize.ofMegabytes(1);
    /** Margen de las cabeceras y los delimitadores de las partes, sobre la parte del JAR y la de etiquetas. */
    static final DataSize MARGEN_CABECERAS = DataSize.ofKilobytes(64);

    @Bean
    MultipartConfigElement limitesMultipart(@Value("${synkra.max-artefacto}") DataSize maxArtefacto,
            @Value("${synkra.max-cuerpo-json}") DataSize maxCuerpoJson) {
        long archivo = maxArtefacto.toBytes() + MARGEN_ARCHIVO.toBytes();
        long peticion = archivo + maxCuerpoJson.toBytes() + MARGEN_CABECERAS.toBytes();
        return new MultipartConfigElement("", archivo, peticion, 0);
    }
}
