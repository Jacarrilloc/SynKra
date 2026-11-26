package io.synkra.verificador;

import java.util.List;
import java.util.Optional;

/**
 * Vista de solo lectura de lo que hay subido: cores y, en la carpeta de cada core, sus plugins.
 * Las listas no tienen orden garantizado; quien necesite orden lo impone. Nunca devuelve {@code null}:
 * lo que no existe es una lista vacía o {@code Optional.empty()}.
 */
public interface Catalogo {

    /** Versiones subidas del core {@code nombre}; vacía si no se conoce. */
    List<Version> versionesCore(String nombre);

    /** Versiones del plugin {@code plugin} en la carpeta del core {@code core}; vacía si no hay. */
    List<Version> versionesPlugin(String core, String plugin);

    /** El descriptor de ese plugin y versión en la carpeta del core, si existe. */
    Optional<Descriptor> descriptor(String core, String plugin, Version version);
}
