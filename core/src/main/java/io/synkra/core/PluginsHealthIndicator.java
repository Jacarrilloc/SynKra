package io.synkra.core;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Indicador {@code plugins} del grupo {@code readiness}: OUT_OF_SERVICE (HTTP 503) mientras un plugin haya fallado al
 * iniciar. No depende del orden de los listeners de eventos ni deja ventana con 200.
 *
 * <p>Decisión del orquestador: el indicador también participa en el agregado {@code /actuator/health}, que por tanto
 * refleja el fallo (503 OUT_OF_SERVICE). Es deliberado; la liveness no cambia (su grupo solo incluye
 * {@code livenessState}) y sigue en 200 UP. El detalle (visible solo si se activa {@code show-details}) nombra el
 * plugin que falló.
 */
@Component
public class PluginsHealthIndicator implements HealthIndicator {

    /** Prefijo del detalle {@code plugins}; se completa con el nombre del plugin que falló. */
    static final String DETALLE_FALLO = "fallo al iniciar plugin=";

    private final IniciadorPlugins iniciador;

    public PluginsHealthIndicator(IniciadorPlugins iniciador) {
        this.iniciador = iniciador;
    }

    @Override
    public Health health() {
        var fallido = iniciador.pluginFallido();
        return fallido != null
                ? Health.outOfService().withDetail("plugins", DETALLE_FALLO + fallido).build()
                : Health.up().build();
    }
}
