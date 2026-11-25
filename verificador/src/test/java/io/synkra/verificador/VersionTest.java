package io.synkra.verificador;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class VersionTest {

    @Test
    void versionValidaSeParsea() {
        var r = Version.parsear("2.1.0");
        var exito = assertInstanceOf(Resultado.Exito.class, r);
        assertEquals(new Version(2, 1, 0), exito.valor());
    }

    @ParameterizedTest
    @ValueSource(strings = {"2.01.0", "2.1.0-rc1", "2.1", "2.1.0+build", "", "a.b.c", "-1.0.0", " 2.1.0"})
    @NullSource
    @SuppressWarnings("unchecked")
    void textoInvalidoDevuelveVersionInvalidaSinExcepcion(String texto) {
        var r = assertDoesNotThrow(() -> Version.parsear(texto));
        var fallo = (Resultado.Fallo<Version>) assertInstanceOf(Resultado.Fallo.class, r);
        assertEquals(1, fallo.motivos().size());
        assertEquals(CodigoMotivo.VERSION_INVALIDA, fallo.motivos().get(0).codigo());
        assertFalse(fallo.motivos().get(0).mensaje().isBlank());
    }

    @Test
    void diezEsMayorQueNueve() {
        assertTrue(new Version(2, 10, 0).compareTo(new Version(2, 9, 0)) > 0);
        assertTrue(new Version(3, 0, 0).compareTo(new Version(2, 99, 99)) > 0);
        assertTrue(new Version(2, 1, 1).compareTo(new Version(2, 1, 0)) > 0);
        assertEquals(0, new Version(2, 1, 0).compareTo(new Version(2, 1, 0)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0.0.0", "2.1.0", "10.20.30"})
    void toStringDevuelveElTextoDeEntrada(String texto) {
        var exito = assertInstanceOf(Resultado.Exito.class, Version.parsear(texto));
        assertEquals(texto, exito.valor().toString());
    }

    @Test
    void numeroFueraDeRangoEsInvalidoSinExcepcion() {
        var r = assertDoesNotThrow(() -> Version.parsear("99999999999.0.0"));
        assertInstanceOf(Resultado.Fallo.class, r);
    }
}
