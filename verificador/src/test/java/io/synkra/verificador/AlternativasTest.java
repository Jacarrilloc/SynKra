package io.synkra.verificador;

import static io.synkra.verificador.Alternativa.TipoAlternativa.CORE;
import static io.synkra.verificador.Alternativa.TipoAlternativa.PLUGIN;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class AlternativasTest {

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

    private static Veredicto alt(Seleccion s, Catalogo c) {
        return Verificador.alternativas(s, Verificador.verificar(s, c), c);
    }

    private static List<String> claves(VeredictoPlugin p) {
        return p.alternativas().stream()
                .map(a -> a.tipo() + "|" + a.nombre() + "|" + a.version())
                .toList();
    }

    @Test
    void s4PluginYCore() {
        var r = alt(sel21(KYC_PE_20), catalogo());
        assertEquals(List.of("PLUGIN|kyc-pe|1.3.0", "CORE|synkra-core|3.0.0"), claves(r.porPlugin().get(0)));
        assertEquals(Veredicto.Global.RECHAZADA, r.global());
        assertEquals(1, r.porPlugin().get(0).motivos().size());
        assertEquals(CodigoMotivo.VERSION_INCOMPATIBLE, r.porPlugin().get(0).motivos().get(0).codigo());
    }

    @Test
    void s5SoloCore() {
        var r = alt(sel("synkra-core", v(3, 0, 0), KYC_CO), catalogo());
        assertEquals(List.of("CORE|synkra-core|2.1.0"), claves(r.porPlugin().get(0)));
    }

    @Test
    void masAltaYMasBajaDeVerdadSinImportarElOrden() {
        // El Catalogo no garantiza orden: la respuesta esperada no puede ser la primera de la lista en ningún orden.
        var kyc12 = d("kyc-pe", v(1, 2, 0), "synkra-core", "2.x");
        var plugins = List.of(kyc12, KYC_PE_13, KYC_PE_20, KYC_CO, KYC_XX);
        var cores = List.of(v(2, 1, 0), v(3, 0, 0), v(3, 1, 0));
        for (var invertir : List.of(false, true)) {
            var ps = new java.util.ArrayList<>(plugins);
            var cs = new java.util.ArrayList<>(cores);
            if (invertir) {
                java.util.Collections.reverse(ps);
                java.util.Collections.reverse(cs);
            }
            var c = new CatalogoEnMemoria(Map.of("synkra-core", cs, "otro-core", List.of(v(1, 0, 0))), ps);
            var r = alt(sel21(KYC_PE_20), c);
            assertEquals(
                    List.of("PLUGIN|kyc-pe|1.3.0", "CORE|synkra-core|3.0.0"),
                    claves(r.porPlugin().get(0)),
                    "invertido=" + invertir);
        }
    }

    @Test
    void soloLaCarpetaDelCoreSeleccionado() {
        var enOtroCore = d("kyc-pe", v(1, 5, 0), "otro-core", "2.x");
        var c = new CatalogoEnMemoria(
                Map.of("synkra-core", List.of(v(2, 1, 0), v(3, 0, 0)), "otro-core", List.of(v(1, 0, 0))),
                List.of(KYC_PE_20, enOtroCore));
        var r = alt(sel21(KYC_PE_20), c);
        assertEquals(List.of("CORE|synkra-core|3.0.0"), claves(r.porPlugin().get(0)));
    }

    @Test
    void versionListadaSinDescriptorONoDelCoreNoSeSugiere() {
        // Catálogo que lista versiones pero no tiene el descriptor, o lo devuelve de otro core.
        var fantasma = new Version(1, 9, 0);
        var ajeno = d("kyc-pe", v(1, 8, 0), "otro-core", "2.x");
        Catalogo c = new Catalogo() {
            public List<Version> versionesCore(String nombre) {
                return List.of();
            }

            public List<Version> versionesPlugin(String core, String plugin) {
                return List.of(v(1, 8, 0), fantasma, v(2, 0, 0));
            }

            public Optional<Descriptor> descriptor(String core, String plugin, Version version) {
                if (version.equals(v(2, 0, 0))) return Optional.of(KYC_PE_20);
                if (version.equals(v(1, 8, 0))) return Optional.of(ajeno);
                return Optional.empty();
            }
        };
        var s = sel21(KYC_PE_20);
        var r = Verificador.alternativas(s, Verificador.verificar(s, c), c);
        assertEquals(List.of(), claves(r.porPlugin().get(0)));
    }

    @Test
    void ningunaReemplazaElMotivo() {
        var c = new CatalogoEnMemoria(Map.of("synkra-core", List.of(v(2, 1, 0))), List.of(KYC_PE_20));
        var s = sel21(KYC_PE_20);
        var antes = Verificador.verificar(s, c);
        var r = Verificador.alternativas(s, antes, c);
        var p = r.porPlugin().get(0);
        assertEquals(List.of(), p.alternativas());
        assertEquals(1, p.motivos().size());
        var m = p.motivos().get(0);
        var m0 = antes.porPlugin().get(0).motivos().get(0);
        assertEquals(CodigoMotivo.VERSION_INCOMPATIBLE, m.codigo());
        assertEquals(m0.plugin(), m.plugin());
        assertEquals(m0.campo(), m.campo());
        assertNotEquals(m0.mensaje(), m.mensaje());
        assertTrue(m.mensaje().endsWith("no hay una versión compatible registrada"), m.mensaje());
        assertTrue(m.mensaje().contains("kyc-pe") && m.mensaje().contains("2.0.0") && m.mensaje().contains("2.1.0"),
                m.mensaje());
        assertFalse(p.compatible());
        assertEquals(Veredicto.Global.RECHAZADA, r.global());
    }

    @Test
    void soloPluginSinCoreTampocoCambiaElMotivo() {
        var c = new CatalogoEnMemoria(Map.of("synkra-core", List.of(v(2, 1, 0))), List.of(KYC_PE_13, KYC_PE_20));
        var s = sel21(KYC_PE_20);
        var r = Verificador.alternativas(s, Verificador.verificar(s, c), c);
        assertEquals(List.of("PLUGIN|kyc-pe|1.3.0"), claves(r.porPlugin().get(0)));
        assertFalse(r.porPlugin().get(0).motivos().get(0).mensaje().contains("no hay una versión compatible"));
    }

    @Test
    void s4AplicadaApruebaConPlugin() {
        var s = sel21(KYC_PE_20);
        var a = alt(s, catalogo()).porPlugin().get(0).alternativas().get(0);
        assertEquals(PLUGIN, a.tipo());
        var nuevo = catalogo().descriptor("synkra-core", a.nombre(), a.version()).orElseThrow();
        var r = Verificador.verificar(sel21(nuevo), catalogo());
        assertEquals(Veredicto.Global.APROBADA, r.global());
        assertTrue(r.porPlugin().get(0).compatible());
    }

    @Test
    void s4AplicadaApruebaConCore() {
        var a = alt(sel21(KYC_PE_20), catalogo()).porPlugin().get(0).alternativas().get(1);
        assertEquals(CORE, a.tipo());
        var r = Verificador.verificar(sel("synkra-core", a.version(), KYC_PE_20), catalogo());
        assertTrue(r.porPlugin().get(0).compatible());
        assertEquals(Veredicto.Global.APROBADA, r.global());
    }

    @Test
    void s5AplicadaApruebaConCore() {
        var a = alt(sel("synkra-core", v(3, 0, 0), KYC_CO), catalogo()).porPlugin().get(0).alternativas().get(0);
        var r = Verificador.verificar(sel("synkra-core", a.version(), KYC_CO), catalogo());
        assertTrue(r.porPlugin().get(0).compatible());
    }

    @Test
    void sinIncompatiblesSaleIgual() {
        var cat = catalogo();
        var casos = List.of(
                sel21(KYC_CO), // S1
                sel21(KYC_MX), // S6 ausente
                sel21(KYC_XX), // S7 core equivocado
                sel21(KYC_PE_13, KYC_PE_20), // S10 repetido
                sel21()); // sin plugins
        for (var s : casos) {
            var v = Verificador.verificar(s, cat);
            assertEquals(v, Verificador.alternativas(s, v, cat));
        }
    }

    @Test
    void dosIncompatiblesCadaUnoConLasSuyas() {
        var kycCo20 = d("kyc-co", v(2, 0, 0), "synkra-core", "3.x");
        var c = new CatalogoEnMemoria(
                Map.of("synkra-core", List.of(v(2, 1, 0), v(3, 0, 0))),
                List.of(KYC_CO, kycCo20, KYC_PE_13, KYC_PE_20));
        var s = sel21(KYC_PE_20, KYC_MX, kycCo20);
        var r = alt(s, c);
        assertEquals(3, r.porPlugin().size());
        assertEquals(List.of("PLUGIN|kyc-pe|1.3.0", "CORE|synkra-core|3.0.0"), claves(r.porPlugin().get(0)));
        assertEquals(List.of(), claves(r.porPlugin().get(1)));
        assertEquals(CodigoMotivo.PLUGIN_AUSENTE, r.porPlugin().get(1).motivos().get(0).codigo());
        assertEquals(List.of("PLUGIN|kyc-co|1.0.0", "CORE|synkra-core|3.0.0"), claves(r.porPlugin().get(2)));
    }

    @Test
    void nulosLanzanNullPointer() {
        var s = sel21(KYC_PE_20);
        var v = Verificador.verificar(s, catalogo());
        assertThrows(NullPointerException.class, () -> Verificador.alternativas(null, v, catalogo()));
        assertThrows(NullPointerException.class, () -> Verificador.alternativas(s, null, catalogo()));
        assertThrows(NullPointerException.class, () -> Verificador.alternativas(s, v, null));
    }

    @Test
    void veredictoAjenoOConAlternativasSeRechaza() {
        var s = sel21(KYC_PE_20);
        var otra = sel21(KYC_CO);
        var deOtra = Verificador.verificar(otra, catalogo());
        var e = assertThrows(
                IllegalArgumentException.class, () -> Verificador.alternativas(s, deOtra, catalogo()));
        assertTrue(e.getMessage().contains("verificar"), e.getMessage());
        var yaConAlternativas = alt(s, catalogo());
        assertThrows(IllegalArgumentException.class, () -> Verificador.alternativas(s, yaConAlternativas, catalogo()));
        // Mismo veredicto pero de un catálogo distinto al que se pasa.
        var vistoEnOtroCatalogo = Verificador.verificar(s, new CatalogoEnMemoria(Map.of(), List.of()));
        assertThrows(
                IllegalArgumentException.class, () -> Verificador.alternativas(s, vistoEnOtroCatalogo, catalogo()));
    }

    @Test
    void catalogoQueDevuelveNullEsErrorConMensaje() {
        var s = sel21(KYC_PE_20);
        var v = Verificador.verificar(s, catalogo());
        Catalogo sinCore = new Catalogo() {
            public List<Version> versionesCore(String nombre) {
                return null;
            }

            public List<Version> versionesPlugin(String core, String plugin) {
                return catalogo().versionesPlugin(core, plugin);
            }

            public Optional<Descriptor> descriptor(String core, String plugin, Version version) {
                return catalogo().descriptor(core, plugin, version);
            }
        };
        var e = assertThrows(NullPointerException.class, () -> Verificador.alternativas(s, v, sinCore));
        assertTrue(e.getMessage().contains("versionesCore"), e.getMessage());
        Catalogo sinDescriptor = new Catalogo() {
            public List<Version> versionesCore(String nombre) {
                return catalogo().versionesCore(nombre);
            }

            public List<Version> versionesPlugin(String core, String plugin) {
                return catalogo().versionesPlugin(core, plugin);
            }

            public Optional<Descriptor> descriptor(String core, String plugin, Version version) {
                return version.equals(KYC_PE_20.version()) ? Optional.of(KYC_PE_20) : null;
            }
        };
        var e2 = assertThrows(NullPointerException.class, () -> Verificador.alternativas(s, v, sinDescriptor));
        assertTrue(e2.getMessage().contains("Optional"), e2.getMessage());
    }
}
