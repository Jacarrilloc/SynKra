package io.synkra.kubernetes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * U05-T09 (C-73): caracterización, no unificación. {@code exigirImagenInit} (kubernetes) y {@code RegistroPermitido}
 * (catálogo) analizan referencias de imagen con reglas distintas; esta prueba deja escrito qué acepta cada uno.
 * Unificarlos cambiaría el comportamiento de U04 y queda como candidata.
 */
class ImagenInitCaracterizacionTest {

    @Test
    void exigirImagenInitPideRegistroYTagODigestYRechazaLatest() {
        for (var buena : List.of("localhost:5001/init:1.0.0", "registro.local:5000/synkra/init:2.3.4",
                "ghcr.io/synkra/init@sha256:" + "a".repeat(64))) {
            assertThat(ConstructorObjetosPais.exigirImagenInit(buena)).isEqualTo(buena);
        }
        for (var mala : List.of("init:1.0.0", "synkra/init:1.0.0", "localhost:5001/init", "localhost:5001/init:latest")) {
            assertThatThrownBy(() -> ConstructorObjetosPais.exigirImagenInit(mala))
                    .as(mala).isInstanceOf(IllegalArgumentException.class);
        }
    }

    /** Lo que {@code exigirImagenInit} acepta y {@code RegistroPermitido} no mira (ver la prueba gemela del catálogo). */
    @Test
    void exigirImagenInitNoMiraQueElRegistroSeaUnoConcreto() {
        // Acepta cualquier registro con punto, puerto o localhost: no compara contra un registro permitido.
        assertThat(ConstructorObjetosPais.exigirImagenInit("docker.io/x/init:1")).isEqualTo("docker.io/x/init:1");
        assertThat(ConstructorObjetosPais.exigirImagenInit("localhost:5002/init:1")).isEqualTo("localhost:5002/init:1");
    }
}
