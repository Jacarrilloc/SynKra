package io.synkra.catalogo.api;

import io.synkra.verificador.Version;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Un core y sus versiones registradas, ordenadas como {@link Version} (no como texto);
 * {@code imagenes} da la imagen de cada versión y tiene exactamente las mismas claves.
 */
public record CoreRegistrado(String nombre, List<Version> versiones, Map<Version, String> imagenes) {

    public CoreRegistrado {
        versiones = List.copyOf(versiones);
        imagenes = Map.copyOf(imagenes);
        if (!imagenes.keySet().equals(Set.copyOf(versiones))) {
            throw new IllegalArgumentException("versiones e imagenes deben tener las mismas versiones");
        }
    }
}
