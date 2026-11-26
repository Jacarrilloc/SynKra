package io.synkra.verificador;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class VeredictoTest {

    private static final Version V1 = new Version(1, 0, 0);

    private static Descriptor descriptor(String nombre) {
        return new Descriptor(nombre, V1, "core-x", new RangoVersion(V1, Optional.empty(), "1.0.0"),
                Map.of(), Optional.empty());
    }

    private static Motivo motivo() {
        return Motivo.de(CodigoMotivo.PLUGIN_AUSENTE, "falta");
    }

    private static VeredictoPlugin ok(String nombre) {
        return new VeredictoPlugin(descriptor(nombre), true, List.of(), List.of());
    }

    private static VeredictoPlugin mal(String nombre) {
        return new VeredictoPlugin(descriptor(nombre), false, List.of(motivo()), List.of());
    }

    @Test
    void seleccionSinPluginsEsValida() {
        var s = new Seleccion("core-x", V1, List.of());
        assertTrue(s.plugins().isEmpty());
    }

    @Test
    void seleccionRechazaCoreInvalidoYNulos() {
        assertThrows(IllegalArgumentException.class, () -> new Seleccion("Core_X", V1, List.of()));
        assertThrows(IllegalArgumentException.class, () -> new Seleccion("", V1, List.of()));
        assertThrows(NullPointerException.class, () -> new Seleccion(null, V1, List.of()));
        assertThrows(NullPointerException.class, () -> new Seleccion("core-x", null, List.of()));
        assertThrows(NullPointerException.class, () -> new Seleccion("core-x", V1, null));
        var conNulo = new ArrayList<Descriptor>();
        conNulo.add(null);
        assertThrows(NullPointerException.class, () -> new Seleccion("core-x", V1, conNulo));
    }

    @Test
    void veredictoPluginCompatibleNoLlevaMotivos() {
        assertThrows(IllegalArgumentException.class,
                () -> new VeredictoPlugin(descriptor("a"), true, List.of(motivo()), List.of()));
    }

    @Test
    void veredictoPluginIncompatibleNecesitaMotivos() {
        assertThrows(IllegalArgumentException.class,
                () -> new VeredictoPlugin(descriptor("a"), false, List.of(), List.of()));
        assertThrows(NullPointerException.class,
                () -> new VeredictoPlugin(null, true, List.of(), List.of()));
        assertThrows(NullPointerException.class,
                () -> new VeredictoPlugin(descriptor("a"), true, null, List.of()));
    }

    private static Motivo incompatible() {
        return new Motivo(CodigoMotivo.VERSION_INCOMPATIBLE, Optional.of("a"), "no sirve", Optional.of("coreCompatible"));
    }

    private static Alternativa alternativa() {
        return new Alternativa(Alternativa.TipoAlternativa.PLUGIN, "a", V1);
    }

    @Test
    void compatibleConAlternativasSeRechaza() {
        var e = assertThrows(IllegalArgumentException.class,
                () -> new VeredictoPlugin(descriptor("a"), true, List.of(), List.of(alternativa())));
        assertTrue(e.getMessage().contains("VERSION_INCOMPATIBLE"), e.getMessage());
    }

    @Test
    void incompatibleSinVersionIncompatibleConAlternativasSeRechaza() {
        var e = assertThrows(IllegalArgumentException.class,
                () -> new VeredictoPlugin(descriptor("a"), false, List.of(motivo()), List.of(alternativa())));
        assertTrue(e.getMessage().contains("VERSION_INCOMPATIBLE"), e.getMessage());
    }

    @Test
    void versionIncompatibleConAlternativasEsValido() {
        var vp = new VeredictoPlugin(descriptor("a"), false, List.of(incompatible()), List.of(alternativa()));
        assertEquals(1, vp.alternativas().size());
    }

    @Test
    void aprobadaConMotivosSeRechaza() {
        assertThrows(IllegalArgumentException.class,
                () -> new Veredicto(Veredicto.Global.APROBADA, List.of(ok("a")), List.of(motivo())));
        assertThrows(IllegalArgumentException.class,
                () -> new Veredicto(Veredicto.Global.APROBADA, List.of(mal("a")), List.of()));
    }

    @Test
    void rechazadaSinMotivosSeRechaza() {
        assertThrows(IllegalArgumentException.class,
                () -> new Veredicto(Veredicto.Global.RECHAZADA, List.of(ok("a")), List.of()));
        assertThrows(IllegalArgumentException.class,
                () -> new Veredicto(Veredicto.Global.RECHAZADA, List.of(), List.of()));
    }

    @Test
    void fabricaCalculaGlobal() {
        assertEquals(Veredicto.Global.APROBADA, Veredicto.de(List.of(ok("a")), List.of()).global());
        assertEquals(Veredicto.Global.RECHAZADA, Veredicto.de(List.of(mal("a")), List.of()).global());
        assertEquals(Veredicto.Global.RECHAZADA, Veredicto.de(List.of(ok("a")), List.of(motivo())).global());
    }

    @Test
    void sinPluginsNiMotivosEsAprobada() {
        assertEquals(Veredicto.Global.APROBADA, Veredicto.de(List.of(), List.of()).global());
    }

    @Test
    void inmutabilidad() {
        var descriptores = new ArrayList<>(List.of(descriptor("a")));
        var s = new Seleccion("core-x", V1, descriptores);
        descriptores.add(descriptor("b"));
        assertEquals(1, s.plugins().size());
        assertThrows(UnsupportedOperationException.class, () -> s.plugins().add(descriptor("c")));

        var motivos = new ArrayList<>(List.of(motivo()));
        var alternativas = new ArrayList<Alternativa>();
        var vp = new VeredictoPlugin(descriptor("a"), false, motivos, alternativas);
        motivos.clear();
        alternativas.add(new Alternativa(Alternativa.TipoAlternativa.PLUGIN, "a", V1));
        assertEquals(1, vp.motivos().size());
        assertTrue(vp.alternativas().isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> vp.motivos().clear());
        assertThrows(UnsupportedOperationException.class, () -> vp.alternativas().add(null));

        var porPlugin = new ArrayList<>(List.of(mal("a")));
        var globales = new ArrayList<Motivo>();
        var v = Veredicto.de(porPlugin, globales);
        porPlugin.clear();
        globales.add(motivo());
        assertEquals(1, v.porPlugin().size());
        assertTrue(v.motivosGlobales().isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> v.porPlugin().clear());
        assertThrows(UnsupportedOperationException.class, () -> v.motivosGlobales().add(motivo()));
    }

    @Test
    void alternativaValida() {
        var a = new Alternativa(Alternativa.TipoAlternativa.CORE, "core-x", new Version(2, 0, 0));
        assertEquals(Alternativa.TipoAlternativa.CORE, a.tipo());
    }

    @Test
    void alternativaRechazaNombreMalo() {
        assertThrows(IllegalArgumentException.class,
                () -> new Alternativa(Alternativa.TipoAlternativa.PLUGIN, "", V1));
        assertThrows(IllegalArgumentException.class,
                () -> new Alternativa(Alternativa.TipoAlternativa.PLUGIN, "Mal_Nombre", V1));
        assertThrows(NullPointerException.class,
                () -> new Alternativa(null, "a", V1));
        assertThrows(NullPointerException.class,
                () -> new Alternativa(Alternativa.TipoAlternativa.PLUGIN, null, V1));
        assertThrows(NullPointerException.class,
                () -> new Alternativa(Alternativa.TipoAlternativa.PLUGIN, "a", null));
    }

    @Test
    void igualdadYHash() {
        var v1 = Veredicto.de(List.of(mal("a"), ok("b")), List.of(motivo()));
        var v2 = Veredicto.de(List.of(mal("a"), ok("b")), List.of(motivo()));
        assertEquals(v1, v2);
        assertEquals(v1.hashCode(), v2.hashCode());
    }

    @Test
    void distintosNoSonEquals() {
        var v1 = Veredicto.de(List.of(mal("a")), List.of());
        var v2 = Veredicto.de(List.of(mal("b")), List.of());
        var v3 = Veredicto.de(List.of(ok("a")), List.of());
        assertNotEquals(v1, v2);
        assertNotEquals(v1, v3);
    }

    @Test
    void nulosDentroDeLasListasYAlternativasNulas() {
        var motivosConNulo = new ArrayList<Motivo>();
        motivosConNulo.add(null);
        var alternativasConNulo = new ArrayList<Alternativa>();
        alternativasConNulo.add(null);
        var pluginsConNulo = new ArrayList<VeredictoPlugin>();
        pluginsConNulo.add(null);
        assertThrows(NullPointerException.class,
                () -> new VeredictoPlugin(descriptor("a"), false, motivosConNulo, List.of()));
        assertThrows(NullPointerException.class,
                () -> new VeredictoPlugin(descriptor("a"), false, List.of(motivo()), alternativasConNulo));
        assertThrows(NullPointerException.class,
                () -> new VeredictoPlugin(descriptor("a"), false, List.of(motivo()), null));
        assertThrows(NullPointerException.class,
                () -> new Veredicto(Veredicto.Global.RECHAZADA, pluginsConNulo, List.of(motivo())));
        assertThrows(NullPointerException.class,
                () -> new Veredicto(Veredicto.Global.RECHAZADA, List.of(), motivosConNulo));
        assertThrows(NullPointerException.class, () -> Veredicto.de(pluginsConNulo, List.of()));
        assertThrows(NullPointerException.class, () -> Veredicto.de(List.of(), motivosConNulo));
    }
}
