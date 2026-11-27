package io.synkra.core;

import io.synkra.plugin.api.ContextoPlugin;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.availability.AvailabilityChangeEvent;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Inicia los plugins ya cargados (S8, H-22). Si uno lanza una excepción detiene en orden inverso los que ya se
 * habían iniciado y deja la readiness en {@code REFUSING_TRAFFIC}: el proceso sigue vivo (liveness intacta) para
 * que Kubernetes no le mande tráfico y el SRE pueda leer el log. No reintenta ni sigue con los demás plugins.
 */
@Component
public class IniciadorPlugins {

    /** Prefijo de la línea ERROR; se completa con {@code <nombre>: <causa>}. */
    static final String PREFIJO_FALLO = "Plugin fallo al iniciar plugin=";

    private static final Logger LOG = LoggerFactory.getLogger(IniciadorPlugins.class);

    private final ArranquePlugins arranque;
    private final ApplicationEventPublisher publicador;
    private volatile boolean fallo;

    public IniciadorPlugins(ArranquePlugins arranque, ApplicationEventPublisher publicador) {
        this.arranque = arranque;
        this.publicador = publicador;
    }

    /** Corre al terminar el arranque; Spring Boot publica ACCEPTING_TRAFFIC justo después (ver {@link #vigilar}). */
    @EventListener
    void iniciar(ApplicationReadyEvent evento) {
        var iniciados = new ArrayList<PluginCargado>();
        for (var c : arranque.cargados()) {
            var plugin = c.plugin();
            try {
                var contexto = new ContextoPlugin(Map.of(), System.getLogger(plugin.nombre()));
                plugin.iniciar(contexto);
                iniciados.add(c);
            } catch (Exception | LinkageError e) {
                LOG.error(PREFIJO_FALLO + plugin.nombre() + ": " + e, e);
                fallo = true;
                detener(iniciados);
                publicador.publishEvent(new AvailabilityChangeEvent<>(this, ReadinessState.REFUSING_TRAFFIC));
                return;
            }
        }
    }

    /** Boot publica ACCEPTING_TRAFFIC después de ApplicationReadyEvent; si un plugin falló, se vuelve a rechazar. */
    @EventListener
    void vigilar(AvailabilityChangeEvent<?> evento) {
        if (fallo && evento.getState() == ReadinessState.ACCEPTING_TRAFFIC) {
            publicador.publishEvent(new AvailabilityChangeEvent<>(this, ReadinessState.REFUSING_TRAFFIC));
        }
    }

    private void detener(List<PluginCargado> iniciados) {
        for (int i = iniciados.size() - 1; i >= 0; i--) {
            var plugin = iniciados.get(i).plugin();
            try {
                plugin.detener();
            } catch (Exception | LinkageError e) {
                LOG.error("Plugin fallo al detener plugin={}: {}", plugin.nombre(), e.toString(), e);
            }
        }
    }
}
