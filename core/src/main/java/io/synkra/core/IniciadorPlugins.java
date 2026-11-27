package io.synkra.core;

import io.synkra.plugin.api.ContextoPlugin;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Inicia los plugins ya cargados (S8, H-22). Si uno lanza una excepción detiene en orden inverso los que ya se
 * habían iniciado y deja la readiness en 503: el proceso sigue vivo (liveness intacta) para que Kubernetes no le
 * mande tráfico y el SRE pueda leer el log. No reintenta ni sigue con los demás plugins.
 *
 * <p>La readiness no depende del orden de los listeners de eventos: {@link PluginsHealthIndicator} está incluido en
 * el grupo {@code readiness} (application.properties) y reporta DOWN mientras {@link #hayFallo()} sea cierto.
 */
@Component
public class IniciadorPlugins {

    /** Prefijo de la línea ERROR; se completa con {@code <nombre>: <causa>}. */
    static final String PREFIJO_FALLO = "Plugin fallo al iniciar plugin=";

    /** Prefijo de la línea ERROR cuando un {@code detener()} lanza; se completa con {@code <nombre>: <causa>}. */
    static final String PREFIJO_FALLO_DETENER = "Plugin fallo al detener plugin=";

    private static final Logger LOG = LoggerFactory.getLogger(IniciadorPlugins.class);

    private final ArranquePlugins arranque;
    private volatile String fallido;

    public IniciadorPlugins(ArranquePlugins arranque) {
        this.arranque = arranque;
    }

    /** True si algún plugin falló al iniciar; lo lee el indicador de salud de readiness. */
    boolean hayFallo() {
        return fallido != null;
    }

    /** Nombre del plugin que falló al iniciar, o null si ninguno falló. */
    String pluginFallido() {
        return fallido;
    }

    /** Corre al terminar el arranque. */
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
                fallido = plugin.nombre();
                detener(iniciados);
                return;
            }
        }
    }

    private void detener(List<PluginCargado> iniciados) {
        for (int i = iniciados.size() - 1; i >= 0; i--) {
            var plugin = iniciados.get(i).plugin();
            try {
                plugin.detener();
            } catch (Exception | LinkageError e) {
                LOG.error(PREFIJO_FALLO_DETENER + plugin.nombre() + ": " + e, e);
            }
        }
    }
}
