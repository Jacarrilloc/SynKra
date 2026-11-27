package io.synkra.core;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Indicador {@code plugins} del grupo {@code readiness}: DOWN (HTTP 503) mientras un plugin haya fallado al iniciar.
 * No depende del orden de los listeners de eventos ni deja ventana con 200.
 */
@Component
public class PluginsHealthIndicator implements HealthIndicator {

    private final IniciadorPlugins iniciador;

    public PluginsHealthIndicator(IniciadorPlugins iniciador) {
        this.iniciador = iniciador;
    }

    @Override
    public Health health() {
        return iniciador.hayFallo() ? Health.down().withDetail("plugins", "fallo al iniciar").build() : Health.up().build();
    }
}
