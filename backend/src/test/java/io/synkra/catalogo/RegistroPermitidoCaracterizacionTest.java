package io.synkra.catalogo;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * U05-T09 (C-73): caracterización de {@code RegistroPermitido}, gemela de
 * {@code ImagenInitCaracterizacionTest} (kubernetes). Solo compara el host de la referencia con un registro
 * configurado; no exige tag ni digest ni rechaza {@code :latest}. Unificarlo con {@code exigirImagenInit} cambiaría
 * comportamiento de U04 y queda como candidata.
 */
class RegistroPermitidoCaracterizacionTest {

    private final RegistroPermitido registro = new RegistroPermitido("localhost:5001");

    @Test
    void soloMiraQueElHostCoincidaConElConfigurado() {
        assertThat(registro.permite("localhost:5001/init")).as("sin tag: lo acepta").isTrue();
        assertThat(registro.permite("localhost:5001/init:latest")).as(":latest: lo acepta").isTrue();
        assertThat(registro.permite("LOCALHOST:5001/init:1")).as("mayúsculas: las ignora").isTrue();
        assertThat(registro.permite("localhost:5002/init:1")).as("otro puerto: lo rechaza").isFalse();
        assertThat(registro.permite("docker.io/init:1")).as("otro registro: lo rechaza").isFalse();
    }

    @Test
    void sinValorConfiguradoNoAplica() {
        assertThat(new RegistroPermitido("").permite("docker.io/init")).isTrue();
    }
}
