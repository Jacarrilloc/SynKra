package io.synkra.verificador;

import java.util.Objects;

/** Versión sugerida de un plugin o de un core. Es solo el valor; otra tarea decide si es compatible. */
public record Alternativa(TipoAlternativa tipo, String nombre, Version version) {

    /** Qué se sugiere cambiar. */
    public enum TipoAlternativa { PLUGIN, CORE }

    public Alternativa {
        Objects.requireNonNull(tipo, "tipo");
        Objects.requireNonNull(nombre, "nombre");
        Objects.requireNonNull(version, "version");
        if (!Descriptor.NOMBRE.matcher(nombre).matches()) {
            throw new IllegalArgumentException("nombre debe cumplir " + Descriptor.NOMBRE.pattern());
        }
    }
}
