package io.synkra.registro;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.synkra.SynkraApp;
import io.synkra.registro.api.ContextoFallo;
import io.synkra.registro.api.EstadoDespliegue;
import io.synkra.registro.api.NuevoDespliegue;
import io.synkra.registro.api.PluginDesplegado;
import io.synkra.registro.api.RegistroApi;
import io.synkra.registro.api.TipoDespliegue;
import io.synkra.registro.api.TransicionNoPermitidaException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

/** R-R8: el registro sobrevive a cerrar el contexto y abrir otro sobre el mismo archivo (ids 31 a 33). */
class RegistroReinicioTest {

    static final Path DATOS = Path.of("build/sqlite-pruebas/RegistroReinicioTest");

    private static ConfigurableApplicationContext arrancar() {
        return new SpringApplicationBuilder(SynkraApp.class)
                .properties("synkra.data=" + DATOS.toAbsolutePath(), "server.port=0").run();
    }

    private static NuevoDespliegue nuevo(long id, Instant en) {
        var plugin = new PluginDesplegado("kyc-pe", "1.3.0", "b".repeat(64), Map.of("pais", "pe"));
        return new NuevoDespliegue(id, "pe", TipoDespliegue.DESPLIEGUE, null, "synkra-core", "2.1.0",
                "localhost:5001/synkra-core@sha256:" + "a".repeat(64), "ana", "luis", en,
                "{\"global\":\"APROBADA\",\"porPlugin\":[],\"motivosGlobales\":[]}", List.of(plugin));
    }

    private static void borrarCarpeta() throws IOException {
        if (Files.exists(DATOS)) {
            try (var s = Files.walk(DATOS)) {
                for (var p : s.sorted(java.util.Comparator.reverseOrder()).toList()) {
                    Files.delete(p);
                }
            }
        }
        Files.createDirectories(DATOS);
    }

    @Test
    void elHistorialYElDetalleSobrevivenAlReinicio() throws IOException {
        borrarCarpeta();
        var t0 = Instant.parse("2026-11-18T11:06:43.456Z");
        var historialAntes = List.<io.synkra.registro.api.DespliegueRegistrado>of();
        io.synkra.registro.api.DespliegueRegistrado detalleAntes;
        ConfigurableApplicationContext uno = arrancar();
        try {
            var api = uno.getBean(RegistroApi.class);
            api.registrar(nuevo(31, t0.plusSeconds(1)));
            api.actualizarResultado(31, EstadoDespliegue.LISTO, null);
            api.registrar(nuevo(32, t0.plusSeconds(2)));
            api.actualizarResultado(32, EstadoDespliegue.FALLIDO, new ContextoFallo("log-core\n", "log-init", "Warning BackOff: x"));
            api.registrar(nuevo(33, t0.plusSeconds(3)));
            historialAntes = api.historial("pe");
            detalleAntes = api.obtener(32).orElseThrow();
            assertThat(historialAntes).extracting(d -> d.id()).containsExactly(33L, 32L, 31L);
            assertThat(detalleAntes.contexto()).isNotNull();
        } finally {
            uno.close();
        }
        assertThat(uno.isActive()).isFalse();

        ConfigurableApplicationContext dos = arrancar();
        try {
            var api = dos.getBean(RegistroApi.class);
            assertThat(api.historial("pe")).isEqualTo(historialAntes);
            assertThat(api.obtener(32)).contains(detalleAntes);
            api.actualizarResultado(33, EstadoDespliegue.LISTO, null);
            assertThatThrownBy(() -> api.actualizarResultado(31, EstadoDespliegue.FALLIDO, new ContextoFallo("a", "b", "c")))
                    .isInstanceOf(TransicionNoPermitidaException.class);
        } finally {
            dos.close();
        }
        assertThat(dos.isActive()).isFalse();
    }
}
