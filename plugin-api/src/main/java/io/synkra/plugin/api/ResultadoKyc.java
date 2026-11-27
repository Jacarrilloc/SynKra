package io.synkra.plugin.api;

import java.util.Objects;

public record ResultadoKyc(boolean aprobado, String motivo) {
    public ResultadoKyc {
        Objects.requireNonNull(motivo, "motivo");
    }
}
