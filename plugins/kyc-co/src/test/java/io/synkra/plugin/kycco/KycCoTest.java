package io.synkra.plugin.kycco;

import static org.junit.jupiter.api.Assertions.*;

import io.synkra.plugin.api.PuntoKyc;
import io.synkra.plugin.api.SolicitudKyc;
import io.synkra.plugin.api.SynkraPlugin;
import io.synkra.verificador.LectorDescriptor;
import io.synkra.verificador.Resultado;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.zip.ZipFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class KycCoTest {
    private final PuntoKyc punto = (PuntoKyc) new PluginKycCo();

    private static SolicitudKyc doc(String d) {
        return new SolicitudKyc(d, Map.of());
    }

    @ParameterizedTest
    @ValueSource(strings = {"123456", "1234567890", "1234567"})
    void aprueba6a10Digitos(String d) {
        var r = punto.validar(doc(d));
        assertTrue(r.aprobado(), r.motivo());
    }

    @ParameterizedTest
    @ValueSource(strings = {"12345", "12345678901", "12ab", "١٢٣٤٥٦", "", "123 456", "-123456"})
    void rechazaConMotivo(String d) {
        var r = punto.validar(doc(d));
        assertFalse(r.aprobado());
        assertFalse(r.motivo().isBlank());
    }

    /** SolicitudKyc no admite documento nulo (lo impide el record); queda la solicitud nula. */
    @Test
    void rechazaSolicitudNulaSinLanzar() {
        var r = punto.validar(null);
        assertFalse(r.aprobado());
        assertFalse(r.motivo().isBlank());
    }

    @Test
    void nombreIgualAlDescriptor() {
        assertEquals("kyc-co", new PluginKycCo().nombre());
    }

    @Test
    void serviceLoaderLoEncuentra() {
        var encontrados = ServiceLoader.load(SynkraPlugin.class).stream().map(ServiceLoader.Provider::type).toList();
        assertTrue(encontrados.contains(PluginKycCo.class));
    }

    @Test
    void descriptorDelJarEsValidoParaU01() throws IOException {
        var jar = Path.of(System.getProperty("kyc.jar"));
        assertTrue(Files.isRegularFile(jar), "falta el JAR " + jar);
        assertEquals("kyc-co-1.0.0.jar", jar.getFileName().toString());
        var r = LectorDescriptor.leer(jar);
        var ok = assertInstanceOf(Resultado.Exito.class, r, r.toString());
        var d = (io.synkra.verificador.Descriptor) ok.valor();
        assertEquals("kyc-co", d.nombre());
        assertEquals("1.0.0", d.version().toString());
        assertEquals("synkra-core", d.core());
        assertEquals(Map.of("pais", "co"), d.etiquetas());
        try (var zip = new ZipFile(jar.toFile())) {
            assertTrue(zip.stream().noneMatch(e -> e.getName().startsWith("io/synkra/plugin/api/")));
            assertNotNull(zip.getEntry("META-INF/services/io.synkra.plugin.api.SynkraPlugin"));
        }
    }
}
