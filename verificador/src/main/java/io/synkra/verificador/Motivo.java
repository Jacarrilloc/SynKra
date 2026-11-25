package io.synkra.verificador;

import java.util.Objects;
import java.util.Optional;

/** Razón legible por una persona por la que algo no es válido o compatible. */
public record Motivo(CodigoMotivo codigo, Optional<String> plugin, String mensaje, Optional<String> campo) {

    public Motivo {
        Objects.requireNonNull(codigo, "codigo");
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(mensaje, "mensaje");
        Objects.requireNonNull(campo, "campo");
        if (mensaje.isBlank()) {
            throw new IllegalArgumentException("El mensaje del motivo no puede estar vacío");
        }
    }

    public static Motivo de(CodigoMotivo codigo, String mensaje) {
        return new Motivo(codigo, Optional.empty(), mensaje, Optional.empty());
    }

    public static Motivo de(CodigoMotivo codigo, String mensaje, String campo) {
        return new Motivo(codigo, Optional.empty(), mensaje, Optional.of(campo));
    }
}
