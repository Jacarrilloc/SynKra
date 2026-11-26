package io.synkra.verificador;

import java.util.List;
import java.util.Objects;

/** Core y plugins que se quieren desplegar juntos. Puede no tener plugins (PRD §11.1). */
public record Seleccion(String core, Version versionCore, List<Descriptor> plugins) {

    public Seleccion {
        Objects.requireNonNull(core, "core");
        Objects.requireNonNull(versionCore, "versionCore");
        Objects.requireNonNull(plugins, "plugins");
        if (!Descriptor.NOMBRE.matcher(core).matches()) {
            throw new IllegalArgumentException("core debe cumplir " + Descriptor.NOMBRE.pattern());
        }
        plugins = List.copyOf(plugins);
    }
}
