package io.synkra.kycpe;

import static org.junit.jupiter.api.Assertions.*;

import io.synkra.plugin.api.PuntoKyc;
import io.synkra.plugin.api.SolicitudKyc;
import io.synkra.plugin.api.SynkraPlugin;
import io.synkra.verificador.Descriptor;
import io.synkra.verificador.LectorDescriptor;
import io.synkra.verificador.Resultado;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.zip.ZipFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class KycPeTest {
    private final PuntoKyc punto = (PuntoKyc) new PluginKycPe();

    private static SolicitudKyc doc(String d) {
        return new SolicitudKyc(d, Map.of());
    }

    @ParameterizedTest
    @ValueSource(strings = {"12345678", "00000000", "98765432"})
    void apruebaOchoDigitos(String d) {
        var r = punto.validar(doc(d));
        assertTrue(r.aprobado(), r.motivo());
        assertEquals(PluginKycPe.MOTIVO_VALIDO, r.motivo());
    }

    @ParameterizedTest
    @ValueSource(strings = {"1234567", "123456789", "", "1"})
    void rechazaPorLongitud(String d) {
        var r = punto.validar(doc(d));
        assertFalse(r.aprobado());
        assertEquals(PluginKycPe.MOTIVO_LONGITUD, r.motivo());
    }

    @ParameterizedTest
    @ValueSource(strings = {"1234567a", "abcdefgh", "١٢٣٤٥٦٧٨", "1234 678", "-1234567"})
    void rechazaPorNoAscii(String d) {
        var r = punto.validar(doc(d));
        assertFalse(r.aprobado());
        assertEquals(PluginKycPe.MOTIVO_NO_DIGITOS, r.motivo());
    }

    @Test
    void rechazaSolicitudNulaSinLanzar() {
        var r = punto.validar(null);
        assertFalse(r.aprobado());
        assertEquals(PluginKycPe.MOTIVO_SOLICITUD_NULA, r.motivo());
    }

    @Test
    void losMotivosSonDistintos() {
        assertEquals(
                4,
                Set.of(
                                PluginKycPe.MOTIVO_LONGITUD,
                                PluginKycPe.MOTIVO_NO_DIGITOS,
                                PluginKycPe.MOTIVO_SOLICITUD_NULA,
                                PluginKycPe.MOTIVO_VALIDO)
                        .size());
    }

    @Test
    void nombreIgualAlDescriptor() {
        assertEquals("kyc-pe", new PluginKycPe().nombre());
    }

    @Test
    void serviceLoaderLoEncuentra() {
        var encontrados = ServiceLoader.load(SynkraPlugin.class).stream().map(ServiceLoader.Provider::type).toList();
        assertTrue(encontrados.contains(PluginKycPe.class));
    }

    @ParameterizedTest
    @CsvSource({"kyc.jar130,kyc-pe-1.3.0.jar,1.3.0,2.x", "kyc.jar200,kyc-pe-2.0.0.jar,2.0.0,3.x"})
    void descriptorDeCadaJarEsValidoParaU01(String prop, String archivo, String version, String rango)
            throws IOException {
        var jar = Path.of(System.getProperty(prop));
        assertTrue(Files.isRegularFile(jar), "falta el JAR " + jar);
        assertEquals(archivo, jar.getFileName().toString());
        var r = LectorDescriptor.leer(jar);
        var ok = assertInstanceOf(Resultado.Exito.class, r, r.toString());
        var d = (Descriptor) ok.valor();
        assertEquals("kyc-pe", d.nombre());
        assertEquals(version, d.version().toString());
        assertEquals("synkra-core", d.core());
        assertEquals(rango, d.coreCompatible().texto());
        assertEquals(Map.of("pais", "pe"), d.etiquetas());
        try (var zip = new ZipFile(jar.toFile())) {
            assertTrue(zip.stream().noneMatch(e -> e.getName().startsWith("io/synkra/plugin/api/")));
            assertNotNull(zip.getEntry("META-INF/services/io.synkra.plugin.api.SynkraPlugin"));
            assertNotNull(zip.getEntry("io/synkra/kycpe/PluginKycPe.class"));
        }
    }
}
