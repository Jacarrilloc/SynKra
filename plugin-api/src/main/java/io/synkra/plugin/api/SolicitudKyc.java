package io.synkra.plugin.api;

import java.util.Map;
import java.util.Objects;

public record SolicitudKyc(String documento, Map<String, String> datos) {
    public SolicitudKyc {
        Objects.requireNonNull(documento, "documento");
        Objects.requireNonNull(datos, "datos");
        datos = Map.copyOf(datos);
    }
}
