package io.synkra.despliegue;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/** Reloj de prueba: zona UTC y una hora que solo cambia cuando la prueba llama a {@link #fija(Instant)}. */
final class RelojMovible extends Clock {

    private volatile Instant ahora;

    RelojMovible(Instant inicio) {
        this.ahora = inicio;
    }

    void fija(Instant instante) {
        this.ahora = instante;
    }

    @Override
    public ZoneId getZone() {
        return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zona) {
        return this;
    }

    @Override
    public Instant instant() {
        return ahora;
    }
}
