package io.synkra.registro;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.jdbc.core.JdbcAggregateTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.support.TransactionTemplate;

/** R-R1 a R-R3. El archivo SQLite queda en build/sqlite-pruebas para leerlo de vuelta (ids fijos 1 a 10). */
@SpringBootTest
class TransicionesRegistroTest {

    static final Path DATOS = Path.of("build/sqlite-pruebas/TransicionesRegistroTest");

    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry r) throws IOException {
        if (Files.exists(DATOS)) {
            try (var s = Files.walk(DATOS)) {
                for (var p : s.sorted(java.util.Comparator.reverseOrder()).toList()) {
                    Files.delete(p);
                }
            }
        }
        Files.createDirectories(DATOS);
        r.add("synkra.data", () -> DATOS.toAbsolutePath().toString());
    }

    @Autowired RegistroApi api;
    @Autowired JdbcTemplate jdbc;
    @Autowired TransactionTemplate transacciones;
    @Autowired JdbcAggregateTemplate plantilla;
    @Autowired ObjectMapper json;

    private static NuevoDespliegue nuevo(long id, TipoDespliegue tipo, Long origen, List<PluginDesplegado> plugins) {
        return new NuevoDespliegue(id, "pe", tipo, origen, "synkra-core", "1.0.0", "reg/c@sha256:" + "a".repeat(64),
                "anonimo", "anonimo", Instant.parse("2026-11-27T10:15:30.123Z"), "{\"ok\":true}", plugins);
    }

    private static PluginDesplegado kyc() {
        return new PluginDesplegado("kyc-pe", "1.0.0", "b".repeat(64), Map.of("pais", "pe"));
    }

    private static NuevoDespliegue valido(long id) {
        return nuevo(id, TipoDespliegue.DESPLIEGUE, null, List.of(kyc()));
    }

    private String estado(long id) {
        return jdbc.queryForObject("select estado from reg_despliegue where id = ?", String.class, id);
    }

    private int filas(String tabla, long id) {
        var col = tabla.equals("reg_despliegue") ? "id" : "despliegue_id";
        return jdbc.queryForObject("select count(*) from " + tabla + " where " + col + " = ?", Integer.class, id);
    }

    /** id1 es el origen de id8: quien corra primero lo registra, sin depender del orden de las pruebas. */
    private void asegurarRegistrado1() {
        if (filas("reg_despliegue", 1) == 0) {
            api.registrar(valido(1));
        }
    }

    @Test
    void id1AplicandoAListo() {
        asegurarRegistrado1();
        api.actualizarResultado(1, EstadoDespliegue.LISTO, null);
        assertThat(estado(1)).isEqualTo("listo");
        assertThat(filas("reg_contexto", 1)).isZero();
        assertThat(jdbc.queryForObject("select terminado_en is not null from reg_despliegue where id = 1",
                Boolean.class)).isTrue();
    }

    @Test
    void id2AplicandoAFallidoGuardaElContexto() {
        api.registrar(valido(2));
        api.actualizarResultado(2, EstadoDespliegue.FALLIDO, new ContextoFallo("log", "init", "ev"));
        assertThat(estado(2)).isEqualTo("fallido");
        assertThat(jdbc.queryForMap("select log_core, log_init, eventos from reg_contexto where despliegue_id = 2"))
                .containsEntry("log_core", "log").containsEntry("log_init", "init").containsEntry("eventos", "ev");
    }

    @Test
    void id3UnListoNoPasaAFallido() {
        api.registrar(valido(3));
        api.actualizarResultado(3, EstadoDespliegue.LISTO, null);
        assertThatThrownBy(() -> api.actualizarResultado(3, EstadoDespliegue.FALLIDO, new ContextoFallo("a", "b", "c")))
                .isInstanceOf(TransicionNoPermitidaException.class).hasMessageContaining("listo");
        assertThat(estado(3)).isEqualTo("listo");
        assertThat(filas("reg_contexto", 3)).isZero();
    }

    @Test
    void id4UnFallidoNoPasaAListo() {
        api.registrar(valido(4));
        api.actualizarResultado(4, EstadoDespliegue.FALLIDO, new ContextoFallo("a", "b", "c"));
        assertThatThrownBy(() -> api.actualizarResultado(4, EstadoDespliegue.LISTO, null))
                .isInstanceOf(TransicionNoPermitidaException.class).hasMessageContaining("fallido");
        assertThat(estado(4)).isEqualTo("fallido");
        assertThat(jdbc.queryForObject("select log_core from reg_contexto where despliegue_id = 4", String.class))
                .isEqualTo("a");
    }

    @Test
    void id5FallidoSinContextoSeRechazaYNadaCambia() {
        api.registrar(valido(5));
        assertThatThrownBy(() -> api.actualizarResultado(5, EstadoDespliegue.FALLIDO, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(estado(5)).isEqualTo("aplicando");
        assertThat(filas("reg_contexto", 5)).isZero();
    }

    @Test
    void id6ListoConContextoSeRechazaYNadaCambia() {
        api.registrar(valido(6));
        assertThatThrownBy(() -> api.actualizarResultado(6, EstadoDespliegue.LISTO, new ContextoFallo("a", "b", "c")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(estado(6)).isEqualTo("aplicando");
        assertThat(filas("reg_contexto", 6)).isZero();
    }

    @Test
    void id7RegistradoDentroDeUnaTransaccionRevertidaNoQuedaNada() {
        transacciones.executeWithoutResult(t -> {
            api.registrar(valido(7));
            assertThat(filas("reg_despliegue", 7)).isEqualTo(1);
            t.setRollbackOnly();
        });
        assertThat(filas("reg_despliegue", 7)).isZero();
        assertThat(filas("reg_plugin", 7)).isZero();
    }

    @Test
    void id8UnaReversionGuardaSuOrigen() {
        asegurarRegistrado1();
        api.registrar(nuevo(8, TipoDespliegue.REVERSION, 1L, List.of(kyc())));
        assertThat(jdbc.queryForMap("select tipo, origen_id, estado from reg_despliegue where id = 8"))
                .containsEntry("tipo", "reversion").containsEntry("origen_id", 1)
                .containsEntry("estado", "aplicando");
    }

    @Test
    void id9DosPluginsConElMismoNombreRechazanElAltaEntera() {
        var repetido = List.of(kyc(), new PluginDesplegado("kyc-pe", "2.0.0", "c".repeat(64), Map.of()));
        assertThatThrownBy(() -> api.registrar(nuevo(9, TipoDespliegue.DESPLIEGUE, null, repetido)))
                .hasRootCauseInstanceOf(org.sqlite.SQLiteException.class)
                .rootCause().hasMessageContaining("SQLITE_CONSTRAINT_PRIMARYKEY");
        assertThat(filas("reg_despliegue", 9)).isZero();
        assertThat(filas("reg_plugin", 9)).isZero();
    }

    @Test
    void id10RegistrarDosVecesRechazaLaSegundaYLaFilaNoCambia() {
        api.registrar(valido(10));
        // valida en todo salvo el id repetido: solo puede caer por la clave primaria
        var otro = new NuevoDespliegue(10, "pe", TipoDespliegue.DESPLIEGUE, null, "otro", "2.0.0", "x@sha256:" + "a".repeat(64),
                "x", "y", Instant.parse("2027-01-01T00:00:00Z"), "{}", List.of(kyc()));
        assertThatThrownBy(() -> api.registrar(otro)).hasRootCauseInstanceOf(org.sqlite.SQLiteException.class)
                .rootCause().hasMessageContaining("SQLITE_CONSTRAINT_PRIMARYKEY");
        assertThat(jdbc.queryForMap("select pais, core, estado from reg_despliegue where id = 10"))
                .containsEntry("pais", "pe").containsEntry("core", "synkra-core").containsEntry("estado", "aplicando");
        assertThat(filas("reg_plugin", 10)).isEqualTo(1);
    }

    private void revertida(Runnable r) {
        transacciones.executeWithoutResult(t -> {
            r.run();
            t.setRollbackOnly();
        });
    }

    // --- Casos extra: corren en una transaccion que se revierte, para no dejar filas en el archivo de CA-2 ---

    @Test
    void registrarDejaElDespliegueEnAplicandoSinTerminadoEnYConSuPlugin() {
        revertida(() -> {
            api.registrar(valido(11));
            assertThat(estado(11)).isEqualTo("aplicando");
            assertThat(jdbc.queryForObject("select terminado_en is null from reg_despliegue where id = 11", Boolean.class))
                    .isTrue();
            assertThat(jdbc.queryForObject("select etiquetas from reg_plugin where despliegue_id = 11", String.class))
                    .isEqualTo("{\"pais\":\"pe\"}");
            assertThat(jdbc.queryForObject("select confirmado_en from reg_despliegue where id = 11", Long.class))
                    .isEqualTo(Instant.parse("2026-11-27T10:15:30.123Z").toEpochMilli());
        });
    }

    @Test
    void pedirAplicandoComoResultadoSeRechaza() {
        revertida(() -> {
            api.registrar(valido(21));
            assertThatThrownBy(() -> api.actualizarResultado(21, EstadoDespliegue.APLICANDO, null))
                    .isInstanceOf(TransicionNoPermitidaException.class);
            assertThat(estado(21)).isEqualTo("aplicando");
        });
    }

    @Test
    void unIdInexistenteSeRechazaConNoSuchElement() {
        revertida(() -> {
            assertThatThrownBy(() -> api.actualizarResultado(999, EstadoDespliegue.LISTO, null))
                    .isInstanceOf(NoSuchElementException.class);
        });
    }

    @Test
    void registrarSinPluginsEsValidoYPluginsNuloCuentaComoVacio() {
        revertida(() -> {
            api.registrar(nuevo(22, TipoDespliegue.DESPLIEGUE, null, null));
            assertThat(filas("reg_despliegue", 22)).isEqualTo(1);
            assertThat(filas("reg_plugin", 22)).isZero();
        });
    }

    @Test
    void losArgumentosInvalidosSeRechazanSinFilas() {
        var ok = valido(30);
        var casos = List.of(
                new NuevoDespliegue(0, "pe", TipoDespliegue.DESPLIEGUE, null, "c", "1", "i", "a", "a", ok.confirmadoEn(), "{}", List.of()),
                new NuevoDespliegue(30, "PE", TipoDespliegue.DESPLIEGUE, null, "c", "1", "i", "a", "a", ok.confirmadoEn(), "{}", List.of()),
                new NuevoDespliegue(30, null, TipoDespliegue.DESPLIEGUE, null, "c", "1", "i", "a", "a", ok.confirmadoEn(), "{}", List.of()),
                new NuevoDespliegue(30, "pe", TipoDespliegue.REVERSION, null, "c", "1", "i", "a", "a", ok.confirmadoEn(), "{}", List.of()),
                new NuevoDespliegue(30, "pe", TipoDespliegue.DESPLIEGUE, 1L, "c", "1", "i", "a", "a", ok.confirmadoEn(), "{}", List.of()),
                new NuevoDespliegue(30, "pe", TipoDespliegue.DESPLIEGUE, null, " ", "1", "i", "a", "a", ok.confirmadoEn(), "{}", List.of()),
                new NuevoDespliegue(30, "pe", TipoDespliegue.DESPLIEGUE, null, "c", "1", "i", "a", "a", null, "{}", List.of()),
                new NuevoDespliegue(30, "pe", TipoDespliegue.DESPLIEGUE, null, "c", "1", "i", "a", "a", ok.confirmadoEn(), "no json", List.of()),
                new NuevoDespliegue(30, "pe", TipoDespliegue.DESPLIEGUE, null, "c", "1", "i", "a", "a", ok.confirmadoEn(), "{}",
                        List.of(new PluginDesplegado("p", "1", "ABC", Map.of()))));
        for (var c : casos) {
            assertThatThrownBy(() -> api.registrar(c)).isInstanceOf(IllegalArgumentException.class);
        }
        assertThat(filas("reg_despliegue", 30)).isZero();
        assertThat(filas("reg_plugin", 30)).isZero();
    }

    @Test
    void cualquierPaisDeDosLetrasMinusculasEsValido() {
        revertida(() -> {
            api.registrar(new NuevoDespliegue(24, "co", TipoDespliegue.DESPLIEGUE, null, "c", "1", "i", "a", "a",
                    Instant.parse("2026-11-27T10:15:30.123Z"), "{}", List.of()));
            api.registrar(new NuevoDespliegue(25, "mx", TipoDespliegue.DESPLIEGUE, null, "c", "1", "i", "a", "a",
                    Instant.parse("2026-11-27T10:15:30.123Z"), "{}", List.of()));
            assertThat(jdbc.queryForList("select pais from reg_despliegue where id in (24, 25) order by id",
                    String.class)).containsExactly("co", "mx");
        });
    }

    @Test
    void terminadoEnUsaElRelojDelConstructorDePaquete() {
        var fijo = Clock.fixed(Instant.parse("2030-01-02T03:04:05.678Z"), ZoneOffset.UTC);
        var servicio = new RegistroServicio(plantilla, jdbc, json, fijo);
        revertida(() -> {
            servicio.registrar(valido(23));
            servicio.actualizarResultado(23, EstadoDespliegue.LISTO, null);
            assertThat(jdbc.queryForObject("select terminado_en from reg_despliegue where id = 23", Long.class))
                    .isEqualTo(fijo.millis());
        });
    }

    /** Si el insert de reg_contexto falla, el UPDATE del estado debe revertirse con el: una sola transaccion. */
    @Test
    void siFallaElContextoElEstadoNoQuedaEnFallido() {
        api.registrar(valido(40));
        try {
            jdbc.update("insert into reg_contexto (despliegue_id, log_core, log_init, eventos) values (40, '', '', '')");
            assertThatThrownBy(() -> api.actualizarResultado(40, EstadoDespliegue.FALLIDO, new ContextoFallo("a", "b", "c")))
                    .hasRootCauseInstanceOf(org.sqlite.SQLiteException.class)
                    .rootCause().hasMessageContaining("SQLITE_CONSTRAINT_PRIMARYKEY");
            assertThat(estado(40)).isEqualTo("aplicando");
            assertThat(jdbc.queryForObject("select terminado_en is null from reg_despliegue where id = 40",
                    Boolean.class)).isTrue();
        } finally {
            jdbc.update("delete from reg_contexto where despliegue_id = 40");
            jdbc.update("delete from reg_plugin where despliegue_id = 40");
            jdbc.update("delete from reg_despliegue where id = 40");
        }
    }
}
