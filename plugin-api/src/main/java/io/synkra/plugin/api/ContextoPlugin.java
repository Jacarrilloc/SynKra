package io.synkra.plugin.api;

import java.util.Map;
import java.util.Objects;

/** Lo que un plugin necesita para iniciar sin conocer el core. */
public record ContextoPlugin(Map<String, String> configuracion, System.Logger logger) {
    public ContextoPlugin {
        Objects.requireNonNull(configuracion, "configuracion");
        Objects.requireNonNull(logger, "logger");
        configuracion = Map.copyOf(configuracion);
    }
}
