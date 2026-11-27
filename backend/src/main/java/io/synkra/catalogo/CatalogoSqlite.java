package io.synkra.catalogo;

import io.synkra.verificador.Catalogo;
import io.synkra.verificador.Descriptor;
import io.synkra.verificador.RangoVersion;
import io.synkra.verificador.Resultado;
import io.synkra.verificador.Version;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * {@link Catalogo} de U01 leído de SQLite en cada llamada (sin caché). Una carpeta solo ve sus plugins porque toda
 * consulta lleva el core. Una fila que no se puede parsear es un error interno ({@link IllegalStateException}, 500):
 * no se oculta. El descriptor lleva solo las etiquetas de origen {@code descriptor}, como el del JAR.
 */
@Component
class CatalogoSqlite implements Catalogo {

    static final String ORIGEN_DESCRIPTOR = "descriptor";

    private final VersionCoreRepositorio versionesCore;
    private final PluginRepositorio plugins;
    private final EtiquetaRepositorio etiquetas;

    CatalogoSqlite(VersionCoreRepositorio versionesCore, PluginRepositorio plugins, EtiquetaRepositorio etiquetas) {
        this.versionesCore = versionesCore;
        this.plugins = plugins;
        this.etiquetas = etiquetas;
    }

    @Override
    public List<Version> versionesCore(String nombre) {
        return versionesCore.findByCore(nombre).stream()
                .map(v -> version(v.version(), "cat_version_core " + v.core()))
                .toList();
    }

    @Override
    public List<Version> versionesPlugin(String core, String plugin) {
        return plugins.findByCore(core).stream()
                .filter(p -> p.nombre().equals(plugin))
                .map(p -> version(p.version(), "cat_plugin " + core + "/" + plugin))
                .toList();
    }

    @Override
    public Optional<Descriptor> descriptor(String core, String plugin, Version version) {
        return plugins.findByCoreAndNombreAndVersion(core, plugin, version.toString()).map(p -> {
            var delDescriptor = etiquetas.findByCoreAndPluginAndVersion(core, plugin, p.version()).stream()
                    .filter(e -> ORIGEN_DESCRIPTOR.equals(e.origen()))
                    .collect(Collectors.toMap(Etiqueta::clave, Etiqueta::valor));
            try {
                return new Descriptor(p.nombre(), version(p.version(), "cat_plugin"), p.core(),
                        rango(p.coreCompatible(), p), delDescriptor, Optional.ofNullable(p.descripcion()));
            } catch (IllegalArgumentException e) {
                throw new IllegalStateException("Plugin no válido en la base: " + core + "/" + plugin + "/" + p.version(), e);
            }
        });
    }

    private static Version version(String texto, String donde) {
        return switch (Version.parsear(texto)) {
            case Resultado.Exito<Version> ok -> ok.valor();
            case Resultado.Fallo<Version> f ->
                throw new IllegalStateException("Versión no válida en la base (" + donde + "): " + texto);
        };
    }

    private static RangoVersion rango(String texto, Plugin p) {
        return switch (RangoVersion.parsear(texto)) {
            case Resultado.Exito<RangoVersion> ok -> ok.valor();
            case Resultado.Fallo<RangoVersion> f -> throw new IllegalStateException(
                    "coreCompatible no válido en la base: " + p.core() + "/" + p.nombre() + "/" + p.version());
        };
    }
}
