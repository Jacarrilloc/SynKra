package io.synkra.catalogo.api;

import io.synkra.verificador.Version;
import java.util.List;

/** Un core y sus versiones registradas, ordenadas como {@link Version} (no como texto). */
public record CoreRegistrado(String nombre, List<Version> versiones) {

    public CoreRegistrado {
        versiones = List.copyOf(versiones);
    }
}
