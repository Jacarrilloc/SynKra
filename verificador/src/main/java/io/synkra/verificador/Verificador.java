package io.synkra.verificador;

import java.util.ArrayList;
import java.util.Comparator;
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
                                + versiones.stream().map(Version::toString).toList() + "); solo se admite una",
                        Optional.empty()));
            }
        });
        return Veredicto.de(porPlugin, globales);
    }

    /**
     * Añade a cada plugin con {@code VERSION_INCOMPATIBLE} la versión más alta del plugin y la más baja del
     * core que sí sirven (R-V5), o reemplaza el motivo por uno que dice que no hay ninguna. {@code veredicto}
     * debe ser exactamente {@code verificar(seleccion, catalogo)}. El resto del veredicto no cambia.
     */
    public static Veredicto alternativas(Seleccion seleccion, Veredicto veredicto, Catalogo catalogo) {
        Objects.requireNonNull(seleccion, "seleccion");
        Objects.requireNonNull(veredicto, "veredicto");
        Objects.requireNonNull(catalogo, "catalogo");
        if (!veredicto.equals(verificar(seleccion, catalogo))) {
            throw new IllegalArgumentException(
                    "El veredicto debe ser exactamente verificar(seleccion, catalogo) para esta selección");
        }
        var porPlugin = new ArrayList<VeredictoPlugin>();
        for (var p : veredicto.porPlugin()) {
            var incompatible = p.motivos().stream()
                    .filter(m -> m.codigo() == CodigoMotivo.VERSION_INCOMPATIBLE)
                    .findFirst();
            if (incompatible.isEmpty()) {
                porPlugin.add(p);
                continue;
            }
            var d = p.descriptor();
            var alternativas = new ArrayList<Alternativa>();
            altaDelPlugin(seleccion, catalogo, d).ifPresent(version -> alternativas.add(
                    new Alternativa(Alternativa.TipoAlternativa.PLUGIN, d.nombre(), version)));
            bajaDelCore(seleccion, catalogo, d).ifPresent(version -> alternativas.add(
                    new Alternativa(Alternativa.TipoAlternativa.CORE, seleccion.core(), version)));
            if (alternativas.isEmpty()) {
                var m = incompatible.get();
                var sinNada = new Motivo(
                        m.codigo(), m.plugin(), m.mensaje() + "; no hay una versión compatible registrada", m.campo());
                porPlugin.add(new VeredictoPlugin(d, false, List.of(sinNada), List.of()));
            } else {
                porPlugin.add(new VeredictoPlugin(d, false, p.motivos(), alternativas));
            }
        }
        return Veredicto.de(porPlugin, veredicto.motivosGlobales());
    }

    private static Optional<Version> altaDelPlugin(Seleccion s, Catalogo catalogo, Descriptor pedido) {
        return catalogo.versionesPlugin(s.core(), pedido.nombre()).stream()
                .filter(version -> catalogo.descriptor(s.core(), pedido.nombre(), version)
                        .filter(c -> c.core().equals(s.core()) && c.coreCompatible().incluye(s.versionCore()))
                        .isPresent())
                .max(Comparator.naturalOrder());
    }

    private static Optional<Version> bajaDelCore(Seleccion s, Catalogo catalogo, Descriptor pedido) {
        return catalogo.versionesCore(s.core()).stream()
                .filter(pedido.coreCompatible()::incluye)
                .min(Comparator.naturalOrder());
    }

    private static Optional<Motivo> motivoDe(Seleccion s, Catalogo catalogo, Descriptor d) {
        var enCatalogo = catalogo.descriptor(d.core(), d.nombre(), d.version());
        Objects.requireNonNull(enCatalogo, "Catalogo.descriptor devolvió null en vez de Optional.empty()");
        if (enCatalogo.isEmpty()) {
            return Optional.of(motivo(
                    CodigoMotivo.PLUGIN_AUSENTE,
                    d,
                    "El plugin '" + d.nombre() + "' " + d.version() + " no está en la carpeta del core '"
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
                    "El plugin '" + d.nombre() + "' " + d.version() + " requiere el core '" + d.core()
                            + "' en " + d.coreCompatible().texto() + " y se pidió " + s.versionCore(),
                    Optional.of("coreCompatible")));
        }
        return Optional.empty();
    }

    private static Motivo motivo(CodigoMotivo codigo, Descriptor d, String mensaje, Optional<String> campo) {
        return new Motivo(codigo, Optional.of(d.nombre()), mensaje, campo);
    }
}
