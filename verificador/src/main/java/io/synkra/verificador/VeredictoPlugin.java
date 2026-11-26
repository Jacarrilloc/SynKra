package io.synkra.verificador;

import java.util.List;
import java.util.Objects;

/** Resultado de verificar un plugin de la selección. */
public record VeredictoPlugin(
        Descriptor descriptor, boolean compatible, List<Motivo> motivos, List<Alternativa> alternativas) {

    public VeredictoPlugin {
        Objects.requireNonNull(descriptor, "descriptor");
        Objects.requireNonNull(motivos, "motivos");
        Objects.requireNonNull(alternativas, "alternativas");
        motivos = List.copyOf(motivos);
        alternativas = List.copyOf(alternativas);
        if (compatible && !motivos.isEmpty()) {
            throw new IllegalArgumentException("compatible con motivos");
        }
        if (!compatible && motivos.isEmpty()) {
            throw new IllegalArgumentException("incompatible sin motivos");
        }
        if (!alternativas.isEmpty()
                && motivos.stream().noneMatch(m -> m.codigo() == CodigoMotivo.VERSION_INCOMPATIBLE)) {
            throw new IllegalArgumentException(
                    "alternativas solo existen para un plugin con motivo VERSION_INCOMPATIBLE (R-V5)");
        }
    }
}
