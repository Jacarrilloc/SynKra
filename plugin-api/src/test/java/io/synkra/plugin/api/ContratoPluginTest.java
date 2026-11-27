package io.synkra.plugin.api;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ContratoPluginTest {

    static class PluginDePrueba implements SynkraPlugin, PuntoKyc {
        boolean iniciado;
        boolean detenido;

        @Override public String nombre() { return "prueba"; }
        @Override public void iniciar(ContextoPlugin contexto) { iniciado = true; }
        @Override public void detener() { detenido = true; }
        @Override public ResultadoKyc validar(SolicitudKyc s) {
            return new ResultadoKyc(!s.documento().isBlank(), "ok");
        }
    }

    @Test
    void solicitudKycRechazaNull() {
        assertThrows(NullPointerException.class, () -> new SolicitudKyc(null, Map.of()));
        assertThrows(NullPointerException.class, () -> new SolicitudKyc("1", null));
    }

    @Test
    void resultadoKycRechazaMotivoNull() {
        assertThrows(NullPointerException.class, () -> new ResultadoKyc(true, null));
    }

    @Test
    void configuracionDelContextoEsInmodificable() {
        Map<String, String> origen = new HashMap<>(Map.of("a", "1"));
        ContextoPlugin ctx = new ContextoPlugin(origen, System.getLogger("t"));
        assertThrows(UnsupportedOperationException.class, () -> ctx.configuracion().put("b", "2"));
        origen.put("c", "3");
        assertFalse(ctx.configuracion().containsKey("c"));
        assertThrows(NullPointerException.class, () -> new ContextoPlugin(null, System.getLogger("t")));
        assertThrows(NullPointerException.class, () -> new ContextoPlugin(Map.of(), null));
    }

    @Test
    void pluginDePruebaImplementaYResponde() {
        PluginDePrueba p = new PluginDePrueba();
        p.iniciar(new ContextoPlugin(Map.of(), System.getLogger("t")));
        assertTrue(p.iniciado);
        assertEquals("prueba", p.nombre());
        ResultadoKyc r = p.validar(new SolicitudKyc("123", Map.of()));
        assertTrue(r.aprobado());
        assertFalse(p.validar(new SolicitudKyc(" ", Map.of())).aprobado());
        p.detener();
        assertTrue(p.detenido);
    }
}
