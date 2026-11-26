package io.synkra.verificador;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class VerificadorTest {

    // ---- Catálogo de la suite (PRD 11.1): un solo lugar, para llevarlo a testFixtures en U01-T12.
    private static Version v(int a, int b, int c) {
        return new Version(a, b, c);
    }

    private static RangoVersion rango(String t) {
        return switch (RangoVersion.parsear(t)) {
            case Resultado.Exito<RangoVersion> e -> e.valor();
            case Resultado.Fallo<RangoVersion> f -> fail(f.motivos().toString());
        };
    }

    private static Descriptor d(String nombre, Version version, String core, String compatible) {
        return new Descriptor(nombre, version, core, rango(compatible), Map.of(), Optional.empty());
    }

    static final Descriptor KYC_CO = d("kyc-co", v(1, 0, 0), "synkra-core", "2.x");
    static final Descriptor KYC_PE_13 = d("kyc-pe", v(1, 3, 0), "synkra-core", "2.x");
    static final Descriptor KYC_PE_20 = d("kyc-pe", v(2, 0, 0), "synkra-core", "3.x");
    static final Descriptor KYC_XX = d("kyc-xx", v(1, 0, 0), "otro-core", "1.x");
    static final Descriptor KYC_MX = d("kyc-mx", v(1, 0, 0), "synkra-core", "2.x");

    static CatalogoEnMemoria catalogo() {
        return new CatalogoEnMemoria(
                Map.of(
                        "synkra-core", List.of(v(2, 1, 0), v(3, 0, 0)),
                        "otro-core", List.of(v(1, 0, 0))),
                List.of(KYC_CO, KYC_PE_13, KYC_PE_20, KYC_XX));
    }

    private static Seleccion sel(String core, Version vc, Descriptor... plugins) {
        return new Seleccion(core, vc, List.of(plugins));
    }

    private static Seleccion sel21(Descriptor... plugins) {
        return sel("synkra-core", v(2, 1, 0), plugins);
    }

    /** (codigo, plugin, campo) de todos los motivos, por plugin y luego globales. */
    private static List<String> motivos(Veredicto v) {
        var r = new java.util.ArrayList<String>();
        for (var p : v.porPlugin()) {
            for (var m : p.motivos()) r.add(clave(m));
        }
        for (var m : v.motivosGlobales()) r.add(clave(m));
        return r;
    }

    private static String clave(Motivo m) {
        return m.codigo() + "|" + m.plugin().orElse("") + "|" + m.campo().orElse("");
    }

    @Test
    void s1Aprobada() {
        var r = Verificador.verificar(sel21(KYC_CO), catalogo());
        assertEquals(Veredicto.Global.APROBADA, r.global());
        assertEquals(List.of(), motivos(r));
        assertEquals(1, r.porPlugin().size());
        assertTrue(r.porPlugin().get(0).compatible());
    }

    @Test
    void s2Aprobada() {
        var r = Verificador.verificar(sel21(KYC_PE_13), catalogo());
        assertEquals(Veredicto.Global.APROBADA, r.global());
        assertEquals(List.of(), motivos(r));
        assertEquals(1, r.porPlugin().size());
    }

    @Test
    void s3SinPluginsAprobada() {
        var r = Verificador.verificar(sel21(), catalogo());
        assertEquals(Veredicto.Global.APROBADA, r.global());
        assertEquals(List.of(), motivos(r));
        assertEquals(0, r.porPlugin().size());
    }

    @Test
    void s4VersionIncompatible() {
        var r = Verificador.verificar(sel21(KYC_PE_20), catalogo());
        assertEquals(Veredicto.Global.RECHAZADA, r.global());
        assertEquals(List.of("VERSION_INCOMPATIBLE|kyc-pe|coreCompatible"), motivos(r));
        assertEquals(List.of(), r.porPlugin().get(0).alternativas());
        assertFalse(r.porPlugin().get(0).compatible());
    }

    @Test
    void s6PluginAusente() {
        var r = Verificador.verificar(sel21(KYC_MX), catalogo());
        assertEquals(List.of("PLUGIN_AUSENTE|kyc-mx|"), motivos(r));
    }

    @Test
    void s7CoreEquivocadoSinOtroMotivo() {
        var r = Verificador.verificar(sel21(KYC_XX), catalogo());
        assertEquals(List.of("CORE_EQUIVOCADO|kyc-xx|core"), motivos(r));
    }

    @Test
    void s10RepetidoUnSoloMotivoGlobal() {
        var r = Verificador.verificar(sel21(KYC_PE_13, KYC_PE_20), catalogo());
        assertEquals(List.of("PLUGIN_REPETIDO|kyc-pe|"), motivos(r));
        assertEquals(1, r.motivosGlobales().size());
        assertEquals(1, r.porPlugin().size());
        assertEquals(v(1, 3, 0), r.porPlugin().get(0).descriptor().version());
        assertTrue(r.porPlugin().get(0).compatible());
        var msg = r.motivosGlobales().get(0).mensaje();
        assertTrue(msg.contains("1.3.0") && msg.contains("2.0.0"), msg);
    }

    @Test
    void tresAparicionesUnSoloRepetido() {
        var r = Verificador.verificar(sel21(KYC_PE_13, KYC_PE_20, KYC_PE_13), catalogo());
        assertEquals(List.of("PLUGIN_REPETIDO|kyc-pe|"), motivos(r));
        assertEquals(1, r.porPlugin().size());
    }

    @Test
    void seJuzgaConElDescriptorRecibido() {
        // Mismo (core, nombre, version) que está en el catálogo, pero otro coreCompatible.
        var otro = d("kyc-co", v(1, 0, 0), "synkra-core", "3.x");
        var r = Verificador.verificar(sel21(otro), catalogo());
        assertEquals(List.of("VERSION_INCOMPATIBLE|kyc-co|coreCompatible"), motivos(r));
        assertEquals(otro, r.porPlugin().get(0).descriptor());
    }

    @Test
    void dosPluginsConFallosDistintosEnOrdenRecibido() {
        var r = Verificador.verificar(sel21(KYC_MX, KYC_PE_20, KYC_CO), catalogo());
        assertEquals(
                List.of("kyc-mx", "kyc-pe", "kyc-co"),
                r.porPlugin().stream().map(p -> p.descriptor().nombre()).toList());
        assertEquals(
                List.of("PLUGIN_AUSENTE|kyc-mx|", "VERSION_INCOMPATIBLE|kyc-pe|coreCompatible"), motivos(r));
        assertTrue(r.porPlugin().get(2).compatible());
    }

    @Test
    void nulosLanzanNullPointer() {
        assertThrows(NullPointerException.class, () -> Verificador.verificar(null, catalogo()));
        assertThrows(NullPointerException.class, () -> Verificador.verificar(sel21(), null));
    }

    @Test
    void catalogoQueDevuelveNullEsErrorConMensaje() {
        Catalogo malo = new Catalogo() {
            public List<Version> versionesCore(String nombre) {
                return List.of();
            }

            public List<Version> versionesPlugin(String core, String plugin) {
                return List.of();
            }

            public Optional<Descriptor> descriptor(String core, String plugin, Version version) {
                return null;
            }
        };
        var e = assertThrows(NullPointerException.class, () -> Verificador.verificar(sel21(KYC_CO), malo));
        assertTrue(e.getMessage().contains("Optional"), e.getMessage());
    }

    @Test
    void deterministaEIgual() {
        var s = sel21(KYC_MX, KYC_PE_20, KYC_CO, KYC_PE_13);
        assertEquals(Verificador.verificar(s, catalogo()), Verificador.verificar(s, catalogo()));
    }
}
