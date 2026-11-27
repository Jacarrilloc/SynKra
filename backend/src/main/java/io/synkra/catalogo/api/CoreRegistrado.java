package io.synkra.catalogo.api;

import io.synkra.verificador.Version;
import java.util.List;
import java.util.Map;

/**
 * Un core y sus versiones registradas, ordenadas como {@link Version} (no como texto);
 * {@code imagenes} da la imagen de cada versión.
 */
public record CoreRegistrado(String nombre, List<Version> versiones, Map<Version, String> imagenes) {

    public CoreRegistrado {
        versiones = List.copyOf(versiones);
        imagenes = Map.copyOf(imagenes);
    }
}
