package io.synkra.verificador;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Optional;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.Combinators;

/** Propiedades de {@link RangoVersion} (PBT-02, PBT-03, PBT-07, PBT-08). La semilla la elige jqwik y queda en el reporte. */
class RangoVersionPropiedades {

    @Provide
    Arbitrary<Integer> componente() {
        return Arbitraries.integers().between(0, 50);
    }

    @Provide
    Arbitrary<Version> versiones() {
        return Combinators.combine(componente(), componente(), componente()).as(Version::new);
    }

    private static Arbitrary<String> espacios() {
        return Arbitraries.integers().between(0, 3).map(" "::repeat);
    }

    /** Una versión estrictamente mayor que {@code a}, sin descartar nada. */
    private static Arbitrary<Version> mayorQue(Version a) {
        Arbitrary<Integer> delta = Arbitraries.integers().between(1, 50);
        Arbitrary<Integer> cualquiera = Arbitraries.integers().between(0, 50);
        return Arbitraries.oneOf(
                delta.map(d -> new Version(a.mayor(), a.menor(), a.parche() + d)),
                Combinators.combine(delta, cualquiera).as((d, p) -> new Version(a.mayor(), a.menor() + d, p)),
                Combinators.combine(delta, cualquiera, cualquiera)
                        .as((d, m, p) -> new Version(a.mayor() + d, m, p)));
    }

    @Provide
    Arbitrary<RangoVersion> rangos() {
        Arbitrary<RangoVersion> mayorX = componente().map(n -> new RangoVersion(
                new Version(n, 0, 0), Optional.of(new Version(n + 1, 0, 0)), n + ".x"));
        Arbitrary<RangoVersion> menorX = Combinators.combine(componente(), componente())
                .as((n, m) -> new RangoVersion(
                        new Version(n, m, 0), Optional.of(new Version(n, m + 1, 0)), n + "." + m + ".x"));
        Arbitrary<RangoVersion> exacta = versiones()
                .map(v -> new RangoVersion(
                        v, Optional.of(new Version(v.mayor(), v.menor(), v.parche() + 1)), v.toString()));
        Arbitrary<RangoVersion> abierto = Combinators.combine(versiones(), espacios())
                .as((a, s) -> new RangoVersion(a, Optional.empty(), ">=" + s + a));
        Arbitrary<RangoVersion> cerrado = versiones()
                .flatMap(a -> Combinators.combine(mayorQue(a), espacios(), espacios(), espacios())
                        .as((b, s1, s2, s3) ->
                                new RangoVersion(a, Optional.of(b), ">=" + s1 + a + s2 + "<" + s3 + b)));
        return Arbitraries.oneOf(mayorX, menorX, exacta, abierto, cerrado);
    }

    /** PBT-03: {@code N.x} incluye {@code v} si y solo si {@code v.mayor == N}. */
    @Property(tries = 1000)
    void nXIncluyeSoloSuMayor(
            @ForAll("componente") int n, @ForAll("componente") int mayorAzar, @ForAll("versiones") Version base,
            @ForAll boolean mismoMayor) {
        var v = new Version(mismoMayor ? n : mayorAzar, base.menor(), base.parche());
        var rango = switch (RangoVersion.parsear(n + ".x")) {
            case Resultado.Exito<RangoVersion> e -> e.valor();
            case Resultado.Fallo<RangoVersion> f -> throw new AssertionError("N.x debe parsear: " + f);
        };
        assertEquals(v.mayor() == n, rango.incluye(v));
    }

    /** PBT-02: ida y vuelta por el texto da el mismo intervalo. */
    @Property(tries = 1000)
    void parsearElTextoDaElMismoIntervalo(@ForAll("rangos") RangoVersion r) {
        switch (RangoVersion.parsear(r.texto())) {
            case Resultado.Exito<RangoVersion> e -> assertEquals(r, e.valor());
            case Resultado.Fallo<RangoVersion> f -> throw new AssertionError("'" + r.texto() + "' debe parsear: " + f);
        }
    }
}
