package io.synkra.despliegue;

import static org.assertj.core.api.Assertions.assertThat;

import io.synkra.kubernetes.api.LecturaPais;
import io.synkra.kubernetes.api.LecturaPais.PluginLeido;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.Tuple;
import net.jqwik.api.statistics.Statistics;

/**
 * PBT-03 (R-D4): el cálculo de cambios entre lo que corre y la selección. Los nombres, versiones, hashes e imágenes
 * vienen de alfabetos pequeños para que haya coincidencias; los nombres son únicos dentro de lo que corre y dentro de
 * la selección. La función {@code aplicar} vive solo aquí. La semilla la elige jqwik en cada ejecución (PBT-08).
 */
class CambiosPropiedades {

    private static final List<String> NOMBRES = List.of("alfa", "beta", "gama", "delta");
    private static final List<String> VERSIONES = List.of("1.0.0", "2.0.0");
    private static final List<String> HASHES = List.of("1".repeat(64), "2".repeat(64));
    private static final List<String> IMAGENES = List.of("r/core@sha256:" + "a".repeat(64), "r/core@sha256:" + "b".repeat(64));
    private static final Comparator<PluginResumen> ORDEN =
            Comparator.comparing(PluginResumen::nombre).thenComparing(PluginResumen::version);

    record Escenario(Optional<LecturaPais> corre, String imagen, List<PluginResumen> seleccion) {

        Set<PluginResumen> queCorre() {
            return corre.map(c -> c.plugins().stream().map(p -> new PluginResumen(p.nombre(), p.version(), p.sha256()))
                    .collect(Collectors.toSet())).orElse(Set.of());
        }
    }

    // ---------------------------------------------------------------- generadores

    private static Arbitrary<List<PluginResumen>> plugins() {
        var porNombre = NOMBRES.stream().map(n -> Arbitraries.frequencyOf(
                Tuple.of(1, Arbitraries.<Optional<PluginResumen>>just(Optional.empty())),
                Tuple.of(3, Combinators.combine(Arbitraries.of(VERSIONES), Arbitraries.of(HASHES))
                        .as((v, h) -> Optional.of(new PluginResumen(n, v, h)))))).toList();
        return Combinators.combine(porNombre).as(l -> l.stream().flatMap(Optional::stream).toList());
    }

    private static Arbitrary<Escenario> escenarios(boolean paisVacio) {
        var corre = paisVacio
                ? Arbitraries.<Optional<List<PluginResumen>>>just(Optional.empty())
                : Arbitraries.frequencyOf(
                        Tuple.of(1, Arbitraries.<Optional<List<PluginResumen>>>just(Optional.empty())),
                        Tuple.of(4, plugins().map(Optional::of)));
        return Combinators.combine(corre, Arbitraries.of(IMAGENES), Arbitraries.of(IMAGENES), plugins(),
                Arbitraries.integers().between(0, 3)).as((c, imagenCorre, imagen, otra, modo) -> {
                    var lectura = c.map(l -> new LecturaPais("pe", imagenCorre,
                            l.stream().map(p -> new PluginLeido(p.nombre(), p.version(), p.sha256())).toList()));
                    // Una de cada cuatro veces la selección es exactamente lo que corre (para tener «sin cambios»).
                    var seleccion = new ArrayList<>(modo == 0 && c.isPresent() ? c.get() : otra);
                    Collections.reverse(seleccion);
                    return new Escenario(lectura, imagen, seleccion);
                });
    }

    @Provide
    Arbitrary<Escenario> cualquiera() {
        return escenarios(false);
    }

    @Provide
    Arbitrary<Escenario> paisVacio() {
        return escenarios(true);
    }

    // ---------------------------------------------------------------- oráculo (solo aquí)

    private static Set<PluginResumen> aplicar(Set<PluginResumen> corre, Cambios c) {
        var r = new HashSet<>(corre);
        r.removeAll(c.quitados());
        r.addAll(c.anadidos());
        return r;
    }

    private static boolean unicos(List<PluginResumen> l) {
        return new HashSet<>(l).size() == l.size();
    }

    // ---------------------------------------------------------------- propiedades

    @Property(tries = 1000)
    void aplicarLosCambiosAlEstadoQueCorreDaLaSeleccion(@ForAll("cualquiera") Escenario e) {
        var c = CalculoCambios.calcular(e.corre(), e.imagen(), e.seleccion());
        var corre = e.queCorre();

        assertThat(aplicar(corre, c)).isEqualTo(Set.copyOf(e.seleccion()));

        var cambioDeVersionOHash = e.seleccion().stream().anyMatch(s -> corre.stream()
                .anyMatch(k -> k.nombre().equals(s.nombre()) && !k.equals(s)));
        Statistics.label("mismo nombre, otra versión o hash").collect(cambioDeVersionOHash);
        Statistics.label("país vacío").collect(e.corre().isEmpty());
        Statistics.label("sin cambios").collect(c.sinCambios());
        Statistics.label("mismo nombre, otra versión o hash").coverage(k -> k.check(true).percentage(p -> p >= 5));
        Statistics.label("país vacío").coverage(k -> k.check(true).percentage(p -> p >= 5));
        Statistics.label("sin cambios").coverage(k -> k.check(true).percentage(p -> p >= 5));
    }

    @Property(tries = 1000)
    void lasTresListasParticionanLaSeleccionYLoQueCorre(@ForAll("cualquiera") Escenario e) {
        var c = CalculoCambios.calcular(e.corre(), e.imagen(), e.seleccion());
        var corre = e.queCorre();
        var seleccion = Set.copyOf(e.seleccion());

        var interseccion = new HashSet<>(corre);
        interseccion.retainAll(seleccion);
        assertThat(Set.copyOf(c.iguales())).isEqualTo(interseccion);
        assertThat(seleccion).containsAll(c.anadidos());
        assertThat(corre).containsAll(c.quitados());
        for (var lista : List.of(c.anadidos(), c.quitados(), c.iguales())) {
            assertThat(unicos(lista)).as("sin repetidos").isTrue();
            assertThat(lista).as("ordenada por nombre y versión").isSortedAccordingTo(ORDEN);
        }
        assertThat(Collections.disjoint(c.anadidos(), c.quitados())).isTrue();
        assertThat(Collections.disjoint(c.anadidos(), c.iguales())).isTrue();
        assertThat(Collections.disjoint(c.quitados(), c.iguales())).isTrue();
    }

    @Property(tries = 1000)
    void sinCambiosEsCoreIgualYMismosPluginsYElCoreDicePorLaImagen(@ForAll("cualquiera") Escenario e) {
        var c = CalculoCambios.calcular(e.corre(), e.imagen(), e.seleccion());
        var antes = e.corre().map(LecturaPais::imagenCore).orElse(null);

        assertThat(c.core().antes()).isEqualTo(antes);
        assertThat(c.core().despues()).isEqualTo(e.imagen());
        assertThat(c.core().igual()).isEqualTo(e.imagen().equals(antes));
        assertThat(c.sinCambios()).isEqualTo(c.core().igual() && e.queCorre().equals(Set.copyOf(e.seleccion())));
    }

    @Property(tries = 1000)
    void unPaisVacioLoAnadeTodoYNoQuitaNiConserva(@ForAll("paisVacio") Escenario e) {
        var c = CalculoCambios.calcular(e.corre(), e.imagen(), e.seleccion());

        assertThat(c.quitados()).isEmpty();
        assertThat(c.iguales()).isEmpty();
        assertThat(c.anadidos()).containsExactlyInAnyOrderElementsOf(e.seleccion());
        assertThat(c.anadidos()).isSortedAccordingTo(ORDEN);
        assertThat(c.core().antes()).isNull();
        assertThat(c.core().igual()).isFalse();
        assertThat(c.sinCambios()).isFalse();
    }
}
