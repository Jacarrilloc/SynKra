package io.synkra.verificador;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Verificación de una selección (R-V4). Función pura y sin estado: el core y U07 llaman a la misma. */
public final class Verificador {

    private Verificador() {}

    /**
     * Juzga cada plugin con el descriptor recibido; el catálogo solo dice si existe. Por plugin hay a lo
     * sumo un motivo: el primer fallo entre ausente, core equivocado e incompatible corta los demás.
     * Las apariciones repetidas de un nombre no entran en {@code porPlugin}: dan un
     * {@code PLUGIN_REPETIDO} global por nombre.
     */
    public static Veredicto verificar(Seleccion seleccion, Catalogo catalogo) {
        Objects.requireNonNull(seleccion, "seleccion");
        Objects.requireNonNull(catalogo, "catalogo");
        var porPlugin = new ArrayList<VeredictoPlugin>();
        var vistos = new HashSet<String>();
        var repetidos = new LinkedHashMap<String, List<Version>>();
        for (var d : seleccion.plugins()) {
            if (!vistos.add(d.nombre())) {
                repetidos.get(d.nombre()).add(d.version());
                continue;
            }
            repetidos.put(d.nombre(), new ArrayList<>(List.of(d.version())));
            var motivo = motivoDe(seleccion, catalogo, d);
            porPlugin.add(new VeredictoPlugin(
                    d, motivo.isEmpty(), motivo.map(List::of).orElse(List.of()), List.of()));
        }
        var globales = new ArrayList<Motivo>();
        repetidos.forEach((nombre, versiones) -> {
            if (versiones.size() > 1) {
                globales.add(new Motivo(
                        CodigoMotivo.PLUGIN_REPETIDO,
                        Optional.of(nombre),
                        "El plugin '" + nombre + "' aparece " + versiones.size() + " veces en la selección (versiones "
                                + versiones.stream().map(Verificador::texto).toList() + "); solo se admite una",
                        Optional.empty()));
            }
        });
        return Veredicto.de(porPlugin, globales);
    }

    private static Optional<Motivo> motivoDe(Seleccion s, Catalogo catalogo, Descriptor d) {
        var enCatalogo = catalogo.descriptor(d.core(), d.nombre(), d.version());
        Objects.requireNonNull(enCatalogo, "Catalogo.descriptor devolvió null en vez de Optional.empty()");
        if (enCatalogo.isEmpty()) {
            return Optional.of(motivo(
                    CodigoMotivo.PLUGIN_AUSENTE,
                    d,
                    "El plugin '" + d.nombre() + "' " + texto(d.version()) + " no está en la carpeta del core '"
                            + d.core() + "'",
                    Optional.empty()));
        }
        if (!d.core().equals(s.core())) {
            return Optional.of(motivo(
                    CodigoMotivo.CORE_EQUIVOCADO,
                    d,
                    "El plugin '" + d.nombre() + "' es del core '" + d.core() + "', no de '" + s.core() + "'",
                    Optional.of("core")));
        }
        if (!d.coreCompatible().incluye(s.versionCore())) {
            return Optional.of(motivo(
                    CodigoMotivo.VERSION_INCOMPATIBLE,
                    d,
                    "El plugin '" + d.nombre() + "' " + texto(d.version()) + " requiere el core '" + d.core()
                            + "' en " + d.coreCompatible().texto() + " y se pidió " + texto(s.versionCore()),
                    Optional.of("coreCompatible")));
        }
        return Optional.empty();
    }

    private static Motivo motivo(CodigoMotivo codigo, Descriptor d, String mensaje, Optional<String> campo) {
        return new Motivo(codigo, Optional.of(d.nombre()), mensaje, campo);
    }

    private static String texto(Version v) {
        return v.mayor() + "." + v.menor() + "." + v.parche();
    }
}
