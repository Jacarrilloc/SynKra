package io.synkra.verificador;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class RangoVersionTest {

    private static RangoVersion rango(String texto) {
        return (RangoVersion) assertInstanceOf(Resultado.Exito.class, RangoVersion.parsear(texto)).valor();
    }

    private static Version v(String texto) {
        return (Version) assertInstanceOf(Resultado.Exito.class, Version.parsear(texto)).valor();
    }

    @ParameterizedTest
    @CsvSource(
            delimiter = ';',
            value = {
                "2.x;2.0.0;3.0.0",
                "2.1.x;2.1.0;2.2.0",
                "2.1.0;2.1.0;2.1.1",
                ">=2.1.0 <3.0.0;2.1.0;3.0.0",
                ">= 2.1.0  <  3.0.0;2.1.0;3.0.0",
                ">=2.1.0<3.0.0;2.1.0;3.0.0",
                ">=2.1.0;2.1.0;",
            })
    void lasCuatroFormasDanSuIntervalo(String texto, String minimo, String maximo) {
        var r = rango(texto);
        assertEquals(v(minimo), r.minimo());
        assertEquals(maximo == null ? Optional.empty() : Optional.of(v(maximo)), r.maximo());
        assertEquals(texto, r.texto());
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                ">=3.0.0 <2.0.0",
                ">=2.0.0 <2.0.0",
                "2.x.x",
                "^2.1",
                "~2.1",
                "2.*",
                ">=2.1.0 || >=3.0.0",
                "",
                " 2.x",
                "2.x ",
                ">=",
                ">=2.1.0 <",
                "<3.0.0",
                "x.x",
                "2147483647.x",
                "2147483647.2147483647.x",
                "1.1.2147483647",
                ">=2.1.0\t<3.0.0",
                ">=\n2.1.0",
                ">=2.1.0\r\n<3.0.0",
                " >=2.1.0",
                ">=2.1.0 ",
                ">=2.1.0 <3.0.0 ",
            })
    @NullSource
    @SuppressWarnings("unchecked")
    void formaInvalidaDevuelveRangoInvalidoSinExcepcion(String texto) {
        var r = assertDoesNotThrow(() -> RangoVersion.parsear(texto));
        var fallo = (Resultado.Fallo<RangoVersion>) assertInstanceOf(Resultado.Fallo.class, r);
        assertEquals(1, fallo.motivos().size());
        assertEquals(CodigoMotivo.RANGO_INVALIDO, fallo.motivos().get(0).codigo());
        assertFalse(fallo.motivos().get(0).mensaje().isBlank());
    }

    @Test
    void dosXIncluyeSoloLaMayor2() {
        var r = rango("2.x");
        assertTrue(r.incluye(v("2.0.0")));
        assertTrue(r.incluye(v("2.99.0")));
        assertFalse(r.incluye(v("3.0.0")));
        assertFalse(r.incluye(v("1.9.9")));
    }

    @Test
    void elMinimoEntraYElMaximoNo() {
        var r = rango(">=2.1.0 <3.0.0");
        assertTrue(r.incluye(v("2.1.0")));
        assertTrue(r.incluye(v("2.99.99")));
        assertFalse(r.incluye(v("3.0.0")));
        assertFalse(r.incluye(v("2.0.99")));
    }

    @Test
    void sinMaximoIncluyeTodoLoSuperior() {
        var r = rango(">=2.1.0");
        assertTrue(r.incluye(v("2.1.0")));
        assertTrue(r.incluye(v("999.0.0")));
        assertFalse(r.incluye(v("2.0.9")));
    }

    @Test
    void exactaIncluyeSoloEsaVersion() {
        var r = rango("2.1.0");
        assertTrue(r.incluye(v("2.1.0")));
        assertFalse(r.incluye(v("2.1.1")));
        assertFalse(r.incluye(v("2.0.9")));
    }

    @Test
    void elConstructorRechazaIntervaloVacio() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new RangoVersion(v("2.0.0"), Optional.of(v("2.0.0")), "x"));
    }
}
