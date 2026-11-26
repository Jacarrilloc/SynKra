package io.synkra.verificador;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** {@link Catalogo} inmutable para pruebas: copia sus entradas y cada descriptor cae en la carpeta de su core. */
public final class CatalogoEnMemoria implements Catalogo {

    private record Clave(String core, String plugin, Version version) {}

    private final Map<String, List<Version>> versionesCore = new HashMap<>();
    private final Map<String, List<Version>> versionesPlugin = new HashMap<>();
    private final Map<Clave, Descriptor> descriptores = new HashMap<>();

    public CatalogoEnMemoria(Map<String, List<Version>> versionesCore, List<Descriptor> descriptores) {
        Objects.requireNonNull(versionesCore, "versionesCore");
        Objects.requireNonNull(descriptores, "descriptores");
        versionesCore.forEach((nombre, versiones) -> this.versionesCore.put(nombre, List.copyOf(versiones)));
        var plugins = new HashMap<String, java.util.ArrayList<Version>>();
        for (var d : descriptores) {
            var clave = new Clave(d.core(), d.nombre(), d.version());
            if (this.descriptores.putIfAbsent(clave, d) != null) {
                throw new IllegalArgumentException(
                        "Descriptor repetido: " + d.core() + "/" + d.nombre() + " " + d.version());
            }
            plugins.computeIfAbsent(d.core() + "/" + d.nombre(), k -> new java.util.ArrayList<>())
                    .add(d.version());
        }
        plugins.forEach((k, v) -> versionesPlugin.put(k, List.copyOf(v)));
    }

    @Override
    public List<Version> versionesCore(String nombre) {
        return versionesCore.getOrDefault(nombre, List.of());
    }

    @Override
    public List<Version> versionesPlugin(String core, String plugin) {
        return versionesPlugin.getOrDefault(core + "/" + plugin, List.of());
    }

    @Override
    public Optional<Descriptor> descriptor(String core, String plugin, Version version) {
        return Optional.ofNullable(descriptores.get(new Clave(core, plugin, version)));
    }
}
