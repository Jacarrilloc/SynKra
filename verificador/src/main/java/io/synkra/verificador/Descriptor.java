package io.synkra.verificador;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

/** Descriptor de un plugin (R-V3): lo que declara su {@code synkra-plugin.yaml}. */
public record Descriptor(
        String nombre,
        Version version,
        String core,
        RangoVersion coreCompatible,
        Map<String, String> etiquetas,
        Optional<String> descripcion) {

    /** Patrón de {@code nombre}, {@code core} y claves de etiqueta. */
    public static final Pattern NOMBRE = Pattern.compile("[a-z0-9]+(-[a-z0-9]+)*");

    public Descriptor {
        Objects.requireNonNull(nombre, "nombre");
        Objects.requireNonNull(version, "version");
        Objects.requireNonNull(core, "core");
        Objects.requireNonNull(coreCompatible, "coreCompatible");
        Objects.requireNonNull(descripcion, "descripcion");
        if (!NOMBRE.matcher(nombre).matches() || !NOMBRE.matcher(core).matches()) {
            throw new IllegalArgumentException("nombre y core deben cumplir " + NOMBRE.pattern());
        }
        etiquetas = Map.copyOf(etiquetas);
        for (var clave : etiquetas.keySet()) {
            if (!NOMBRE.matcher(clave).matches()) {
                throw new IllegalArgumentException("Clave de etiqueta inválida: " + clave);
            }
        }
    }
}
