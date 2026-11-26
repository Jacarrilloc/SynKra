package io.synkra.verificador;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class CatalogoEnMemoriaTest {

    private static final Version V100 = new Version(1, 0, 0);

    private static Descriptor d(String nombre, Version v, String core) {
        var rango = RangoVersion.parsear("1.x");
        return new Descriptor(
                nombre, v, core, ((Resultado.Exito<RangoVersion>) rango).valor(), Map.of(), Optional.empty());
    }

    @Test
    void cadaDescriptorCaeEnLaCarpetaDeSuCore() {
        var a = d("kyc-a", V100, "core-a");
        var b = d("kyc-a", V100, "core-b");
        var c = new CatalogoEnMemoria(Map.of("core-a", List.of(V100)), List.of(a, b));
        assertEquals(Optional.of(a), c.descriptor("core-a", "kyc-a", V100));
        assertEquals(Optional.of(b), c.descriptor("core-b", "kyc-a", V100));
        assertEquals(List.of(V100), c.versionesPlugin("core-a", "kyc-a"));
        assertEquals(List.of(V100), c.versionesCore("core-a"));
    }

    @Test
    void nombreDesconocidoDaVacio() {
        var c = new CatalogoEnMemoria(Map.of(), List.of());
        assertEquals(List.of(), c.versionesCore("x"));
        assertEquals(List.of(), c.versionesPlugin("x", "y"));
        assertEquals(Optional.empty(), c.descriptor("x", "y", V100));
    }

    @Test
    void laslistasDevueltasNoSeModifican() {
        var c = new CatalogoEnMemoria(Map.of("core-a", List.of(V100)), List.of(d("kyc-a", V100, "core-a")));
        assertThrows(UnsupportedOperationException.class, () -> c.versionesCore("core-a").add(V100));
        assertThrows(UnsupportedOperationException.class, () -> c.versionesPlugin("core-a", "kyc-a").add(V100));
    }

    @Test
    void cambiarLosOriginalesNoLoCambia() {
        var versiones = new ArrayList<>(List.of(V100));
        var mapa = new HashMap<String, List<Version>>();
        mapa.put("core-a", versiones);
        var descriptores = new ArrayList<>(List.of(d("kyc-a", V100, "core-a")));
        var c = new CatalogoEnMemoria(mapa, descriptores);
        versiones.add(new Version(2, 0, 0));
        mapa.put("core-b", List.of(V100));
        descriptores.add(d("kyc-b", V100, "core-a"));
        assertEquals(List.of(V100), c.versionesCore("core-a"));
        assertEquals(List.of(), c.versionesCore("core-b"));
        assertEquals(List.of(), c.versionesPlugin("core-a", "kyc-b"));
    }

    @Test
    void duplicadoEsIllegalArgument() {
        var a = d("kyc-a", V100, "core-a");
        assertThrows(IllegalArgumentException.class, () -> new CatalogoEnMemoria(Map.of(), List.of(a, a)));
    }
}
