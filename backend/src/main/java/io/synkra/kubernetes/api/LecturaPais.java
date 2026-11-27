package io.synkra.kubernetes.api;

import java.util.List;

/** Lo que corre de verdad en un país, leído del clúster: imagen del core con su digest y plugins con su sha256. */
public record LecturaPais(String pais, String imagenCore, List<PluginLeido> plugins) {

    public LecturaPais {
        plugins = plugins == null ? List.of() : List.copyOf(plugins);
    }

    /** Un plugin del ConfigMap: nombre y versión de {@code plugins-activos.yaml}, sha256 de {@code artefactos.txt}. */
    public record PluginLeido(String nombre, String version, String sha256) {}
}
