package io.synkra.verificador;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

/** Propiedades de {@link RangoVersion} (PBT-02, PBT-03, PBT-07, PBT-08). La semilla la elige jqwik y queda en el reporte. */
class RangoVersionPropiedades {

    @Provide
    Arbitrary<Integer> componente() {
        return Generadores.componente();
    }

    @Provide
    Arbitrary<Version> versiones() {
        return Generadores.versiones();
    }

    @Provide
    Arbitrary<RangoVersion> rangos() {
        return Generadores.rangos();
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
