package io.synkra.registro;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.jdbc.core.JdbcAggregateTemplate;
import org.springframework.data.repository.CrudRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** Repositorios del registro contra SQLite real, con el esquema de V3__registro.sql. */
@SpringBootTest
class RegistroRepositoriosTest {

    @TempDir
    static Path datos;

    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry r) {
        r.add("synkra.data", () -> datos.toString());
    }

    @Autowired JdbcAggregateTemplate plantilla;
    @Autowired JdbcTemplate jdbc;
    @Autowired DespliegueRepositorio despliegues;
    @Autowired PluginRegistradoRepositorio plugins;
    @Autowired ContextoRepositorio contextos;

    @BeforeEach
    void limpiar() {
        jdbc.execute("delete from reg_contexto");
        jdbc.execute("delete from reg_plugin");
        jdbc.execute("delete from reg_despliegue");
    }

    private Despliegue despliegue(long id, long en) {
        return new Despliegue(id, "pe", "despliegue", null, "synkra-core", "1.0.0", "reg/c@sha256:abc",
                "anonimo", "anonimo", en, "aplicando", null, "{}");
    }

    private void rechaza(String sqlTexto, String restriccion) {
        assertThatThrownBy(() -> jdbc.update(sqlTexto))
                .hasRootCauseInstanceOf(org.sqlite.SQLiteException.class)
                .rootCause().hasMessageContaining(restriccion);
    }

    private static final String INS = "insert into reg_despliegue(id,pais,tipo,origen_id,core,version_core,imagen,"
            + "creado_por,confirmado_por,confirmado_en,estado,terminado_en,veredicto) values ";

    @Test
    void altaYLecturaDeUnDespliegueConDosPluginsYSuContexto() {
        plantilla.insert(despliegue(7, 1L));
        plantilla.insert(new PluginRegistrado(7, "kyc-pe", "1.0.0", "a".repeat(64), "{\"pais\":\"pe\"}"));
        plantilla.insert(new PluginRegistrado(7, "otro", "2.0.0", "b".repeat(64), "{}"));
        plantilla.insert(new ContextoGuardado(7, "log", "init", "ev"));
        assertThat(despliegues.findOneById(7)).get().extracting(Despliegue::core).isEqualTo("synkra-core");
        assertThat(despliegues.findByPais("pe")).hasSize(1);
        assertThat(plugins.findByDespliegueId(7)).extracting(PluginRegistrado::nombre)
                .containsExactlyInAnyOrder("kyc-pe", "otro");
        assertThat(contextos.findByDespliegueId(7)).contains(new ContextoGuardado(7, "log", "init", "ev"));
    }

    @Test
    void confirmadoEnVuelveConElMismoInstanteEnMilisegundosComoInteger() {
        long ms = Instant.parse("2026-11-27T10:15:30.123Z").toEpochMilli();
        plantilla.insert(despliegue(1, ms));
        var leido = despliegues.findOneById(1).orElseThrow();
        assertThat(Instant.ofEpochMilli(leido.confirmadoEn())).isEqualTo(Instant.parse("2026-11-27T10:15:30.123Z"));
        assertThat(jdbc.queryForObject("select typeof(confirmado_en) from reg_despliegue", String.class))
                .isEqualTo("integer");
    }

    @Test
    void unPluginConDespliegueInexistenteFallaPorClaveForanea() {
        assertThatThrownBy(() -> plantilla.insert(new PluginRegistrado(99, "p", "1", "a".repeat(64), "{}")))
                .hasRootCauseInstanceOf(org.sqlite.SQLiteException.class)
                .rootCause().hasMessageContaining("SQLITE_CONSTRAINT_FOREIGNKEY");
        assertThat(plugins.findAll()).isEmpty();
    }

    @Test
    void unContextoConDespliegueInexistenteFallaPorClaveForanea() {
        assertThatThrownBy(() -> plantilla.insert(new ContextoGuardado(99, "", "", "")))
                .hasRootCauseInstanceOf(org.sqlite.SQLiteException.class)
                .rootCause().hasMessageContaining("SQLITE_CONSTRAINT_FOREIGNKEY");
    }

    @Test
    void elCheckDeEstadoRechazaValoresDesconocidos() {
        rechaza(INS + "(1,'pe','despliegue',null,'c','1','i','a','a',1,'otro',null,'{}')",
                "SQLITE_CONSTRAINT_CHECK");
    }

    @Test
    void elCheckDeTipoYOrigenRechazaReversionSinOrigenYDespliegueConOrigen() {
        plantilla.insert(despliegue(1, 1L));
        rechaza(INS + "(2,'pe','reversion',null,'c','1','i','a','a',1,'aplicando',null,'{}')",
                "SQLITE_CONSTRAINT_CHECK");
        rechaza(INS + "(3,'pe','despliegue',1,'c','1','i','a','a',1,'aplicando',null,'{}')",
                "SQLITE_CONSTRAINT_CHECK");
        rechaza(INS + "(4,'pe','otro',null,'c','1','i','a','a',1,'aplicando',null,'{}')",
                "SQLITE_CONSTRAINT_CHECK");
        jdbc.update(INS + "(5,'pe','reversion',1,'c','1','i','a','a',1,'aplicando',null,'{}')");
    }

    @Test
    void elCheckDeTerminadoEnExigeNuloSoloEnAplicando() {
        rechaza(INS + "(1,'pe','despliegue',null,'c','1','i','a','a',1,'aplicando',5,'{}')",
                "SQLITE_CONSTRAINT_CHECK");
        rechaza(INS + "(2,'pe','despliegue',null,'c','1','i','a','a',1,'listo',null,'{}')",
                "SQLITE_CONSTRAINT_CHECK");
        jdbc.update(INS + "(3,'pe','despliegue',null,'c','1','i','a','a',1,'listo',5,'{}')");
    }

    @Test
    void elCheckDePaisYJsonValidRechazan() {
        rechaza(INS + "(1,'PE','despliegue',null,'c','1','i','a','a',1,'aplicando',null,'{}')",
                "SQLITE_CONSTRAINT_CHECK");
        rechaza(INS + "(2,'per','despliegue',null,'c','1','i','a','a',1,'aplicando',null,'{}')",
                "SQLITE_CONSTRAINT_CHECK");
        rechaza(INS + "(3,'pe','despliegue',null,'c','1','i','a','a',1,'aplicando',null,'no es json')",
                "SQLITE_CONSTRAINT_CHECK");
        plantilla.insert(despliegue(4, 1L));
        rechaza("insert into reg_plugin values (4,'p','1','" + "a".repeat(64) + "','no json')",
                "SQLITE_CONSTRAINT_CHECK");
    }

    @Test
    void unIdRepetidoFallaPorClavePrimariaYLaFilaNoCambia() {
        plantilla.insert(despliegue(1, 1L));
        assertThatThrownBy(() -> plantilla.insert(despliegue(1, 2L)))
                .hasRootCauseInstanceOf(org.sqlite.SQLiteException.class)
                .rootCause().hasMessageContaining("SQLITE_CONSTRAINT_PRIMARYKEY");
        assertThat(despliegues.findOneById(1).orElseThrow().confirmadoEn()).isEqualTo(1L);
    }

    @Test
    void borrarUnDespliegueConPluginsSeRechazaPorRestrictYLasFilasSiguen() {
        plantilla.insert(despliegue(1, 1L));
        plantilla.insert(new PluginRegistrado(1, "p", "1", "a".repeat(64), "{}"));
        rechaza("delete from reg_despliegue where id = 1", "SQLITE_CONSTRAINT_TRIGGER");
        assertThat(jdbc.queryForObject("select count(*) from reg_despliegue", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from reg_plugin", Integer.class)).isEqualTo(1);
    }

    @Test
    void lasTresFkDeclaranOnDeleteRestrict() {
        for (var tabla : List.of("reg_despliegue", "reg_plugin", "reg_contexto")) {
            var fks = jdbc.queryForList("select on_delete from pragma_foreign_key_list('" + tabla + "')");
            assertThat(fks).as(tabla).isNotEmpty();
            assertThat(fks).as(tabla).allSatisfy(f -> assertThat(f.get("on_delete")).isEqualTo("RESTRICT"));
        }
    }

    @Test
    void elIndiceDePaisExisteYNoEsUnico() {
        var indices = jdbc.queryForList("select name, \"unique\" as u from pragma_index_list('reg_despliegue')");
        assertThat(indices).anySatisfy(f -> {
            assertThat(f.get("name")).isEqualTo("reg_despliegue_pais");
            assertThat(((Number) f.get("u")).intValue()).isZero();
        });
    }

    @Test
    void losRepositoriosNoOfreceOperacionesCrud() {
        for (var tipo : List.of(DespliegueRepositorio.class, PluginRegistradoRepositorio.class,
                ContextoRepositorio.class)) {
            assertThat(CrudRepository.class.isAssignableFrom(tipo)).as(tipo.getSimpleName()).isFalse();
            assertThat(Arrays.stream(tipo.getMethods()).map(java.lang.reflect.Method::getName))
                    .as(tipo.getSimpleName() + " metodos")
                    .noneMatch(n -> n.startsWith("save") || n.startsWith("delete")
                            || n.equals("findById") || n.equals("existsById"));
        }
    }
}
