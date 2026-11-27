package io.synkra.catalogo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Path;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.jdbc.core.JdbcAggregateTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** Repositorios del catálogo contra SQLite real, con el esquema de V2__catalogo.sql. */
@SpringBootTest
class CatalogoRepositoriosTest {

    @TempDir
    static Path datos;

    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry r) {
        r.add("synkra.data", () -> datos.toString());
    }

    @Autowired JdbcAggregateTemplate plantilla;
    @Autowired JdbcTemplate jdbc;
    @Autowired CoreRepositorio cores;
    @Autowired VersionCoreRepositorio versiones;
    @Autowired PluginRepositorio plugins;
    @Autowired EtiquetaRepositorio etiquetas;

    @BeforeEach
    void limpiar() {
        jdbc.execute("delete from cat_etiqueta");
        jdbc.execute("delete from cat_plugin");
        jdbc.execute("delete from cat_version_core");
        jdbc.execute("delete from cat_core");
    }

    private VersionCore version(String core, String v, long en) {
        return new VersionCore(core, v, "reg/" + core + "@sha256:abc", "anonimo", en);
    }

    private Plugin plugin(String core, String nombre, String v, String sha) {
        return new Plugin(core, nombre, v, ">=1.0.0 <2.0.0", sha, 10L,
                "/data/artefactos/" + core + "/" + nombre + "/" + v + ".jar", "desc", "anonimo", 1L);
    }

    @Test
    void altaYLecturaDeUnCoreConDosVersiones() {
        plantilla.insert(new Core("synkra-core"));
        plantilla.insert(version("synkra-core", "1.0.0", 1L));
        plantilla.insert(version("synkra-core", "1.1.0", 2L));
        assertThat(cores.findByNombre("synkra-core")).contains(new Core("synkra-core"));
        assertThat(versiones.findByCore("synkra-core")).extracting(VersionCore::version)
                .containsExactlyInAnyOrder("1.0.0", "1.1.0");
        assertThat(versiones.findByCoreAndVersion("synkra-core", "1.1.0")).isPresent();
    }

    @Test
    void pluginConDosEtiquetasSeLeePorSuClave() {
        plantilla.insert(new Core("c"));
        plantilla.insert(plugin("c", "p", "1.0.0", "aa"));
        plantilla.insert(new Etiqueta("c", "p", "1.0.0", "pais", "co", "descriptor"));
        plantilla.insert(new Etiqueta("c", "p", "1.0.0", "cliente", "x", "subida"));
        var leido = plugins.findByCoreAndNombreAndVersion("c", "p", "1.0.0").orElseThrow();
        assertThat(leido.coreCompatible()).isEqualTo(">=1.0.0 <2.0.0");
        assertThat(leido.ruta()).isEqualTo("/data/artefactos/c/p/1.0.0.jar");
        assertThat(etiquetas.findByCoreAndPluginAndVersion("c", "p", "1.0.0"))
                .extracting(e -> e.clave() + "=" + e.valor())
                .containsExactlyInAnyOrder("pais=co", "cliente=x");
    }

    @Test
    void buscaPluginsPorSha256() {
        plantilla.insert(new Core("c"));
        plantilla.insert(plugin("c", "p", "1.0.0", "h1"));
        plantilla.insert(plugin("c", "p", "1.1.0", "h2"));
        assertThat(plugins.findBySha256("h2")).extracting(Plugin::version).containsExactly("1.1.0");
        assertThat(plugins.findBySha256("nada")).isEmpty();
    }

    @Test
    void registradoEnVuelveConElMismoInstanteEnMilisegundos() {
        long ms = Instant.parse("2026-11-27T10:15:30.123Z").toEpochMilli();
        plantilla.insert(new Core("c"));
        plantilla.insert(version("c", "1.0.0", ms));
        var leido = versiones.findByCoreAndVersion("c", "1.0.0").orElseThrow();
        assertThat(Instant.ofEpochMilli(leido.registradoEn())).isEqualTo(Instant.parse("2026-11-27T10:15:30.123Z"));
        assertThat(jdbc.queryForObject("select typeof(registrado_en) from cat_version_core", String.class))
                .isEqualTo("integer");
    }

    @Test
    void unaVersionRepetidaFallaPorClaveDuplicadaYNoSeSobrescribe() {
        plantilla.insert(new Core("c"));
        plantilla.insert(version("c", "1.0.0", 1L));
        assertThatThrownBy(() -> plantilla.insert(version("c", "1.0.0", 2L)))
                .hasRootCauseInstanceOf(org.sqlite.SQLiteException.class)
                .rootCause().hasMessageContaining("SQLITE_CONSTRAINT_PRIMARYKEY");
        assertThat(versiones.findByCoreAndVersion("c", "1.0.0").orElseThrow().registradoEn())
                .isEqualTo(1L);
    }

    @Test
    void unPluginConCoreInexistenteFallaPorClaveForanea() {
        assertThatThrownBy(() -> plantilla.insert(plugin("no-existe", "p", "1.0.0", "h")))
                .hasRootCauseInstanceOf(org.sqlite.SQLiteException.class)
                .rootCause().hasMessageContaining("SQLITE_CONSTRAINT_FOREIGNKEY");
        assertThat(plugins.findAll()).isEmpty();
    }

    @Test
    void losRepositoriosNoOfreceOperacionesCrud() {
        for (var tipo : java.util.List.of(CoreRepositorio.class, VersionCoreRepositorio.class,
                PluginRepositorio.class, EtiquetaRepositorio.class)) {
            assertThat(org.springframework.data.repository.CrudRepository.class.isAssignableFrom(tipo))
                    .as(tipo.getSimpleName()).isFalse();
            assertThat(java.util.Arrays.stream(tipo.getMethods()).map(java.lang.reflect.Method::getName))
                    .as(tipo.getSimpleName() + " metodos")
                    .noneMatch(n -> n.startsWith("save") || n.startsWith("delete")
                            || n.equals("findById") || n.equals("existsById"));
        }
    }

    // --- Decisiones del esquema, con SQL directo y foreign_keys activo ---

    private void sql(String s, Object... args) {
        jdbc.update(s, args);
    }

    private void core(String n) {
        sql("insert into cat_core(nombre) values (?)", n);
    }

    private void pluginSql(String core, String nombre, String v, String sha) {
        sql("insert into cat_plugin(core,nombre,version,core_compatible,sha256,tamano,ruta,subido_por,subido_en)"
                + " values (?,?,?,'>=1.0.0 <2.0.0',?,1,'/r','anonimo',1)", core, nombre, v, sha);
    }

    private void rechaza(String sqlTexto, String restriccion) {
        assertThatThrownBy(() -> sql(sqlTexto))
                .hasRootCauseInstanceOf(org.sqlite.SQLiteException.class)
                .rootCause().hasMessageContaining(restriccion);
    }

    @Test
    void laFkDeVersionCoreRechazaUnCoreInexistente() {
        rechaza("insert into cat_version_core values ('no','1.0.0','i','anonimo',1)",
                "SQLITE_CONSTRAINT_FOREIGNKEY");
    }

    @Test
    void laFkCompuestaDeEtiquetaRechazaUnPluginInexistente() {
        core("c");
        pluginSql("c", "p", "1.0.0", "h");
        rechaza("insert into cat_etiqueta values ('c','p','9.9.9','pais','co','descriptor')",
                "SQLITE_CONSTRAINT_FOREIGNKEY");
        sql("insert into cat_etiqueta values ('c','p','1.0.0','pais','co','descriptor')");
    }

    @Test
    void elCheckDeOrigenRechazaValoresDistintosDeDescriptorYSubida() {
        core("c");
        pluginSql("c", "p", "1.0.0", "h");
        rechaza("insert into cat_etiqueta values ('c','p','1.0.0','pais','co','otro')",
                "SQLITE_CONSTRAINT_CHECK");
        sql("insert into cat_etiqueta values ('c','p','1.0.0','pais','co','subida')");
    }

    @Test
    void borrarUnCoreConHijosSeRechazaPorRestrictYLaFilaSigue() {
        core("c");
        sql("insert into cat_version_core values ('c','1.0.0','i','anonimo',1)");
        rechaza("delete from cat_core where nombre = 'c'", "SQLITE_CONSTRAINT_TRIGGER");
        assertThat(jdbc.queryForObject("select count(*) from cat_core", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from cat_version_core", Integer.class)).isEqualTo(1);
    }

    @Test
    void dosPluginsConElMismoSha256SeAceptanYElIndiceNoEsUnico() {
        core("c");
        pluginSql("c", "p", "1.0.0", "igual");
        pluginSql("c", "p", "1.1.0", "igual");
        assertThat(jdbc.queryForObject("select count(*) from cat_plugin where sha256='igual'", Integer.class))
                .isEqualTo(2);
        var indices = jdbc.queryForList("select name, \"unique\" as u from pragma_index_list('cat_plugin')");
        assertThat(indices).anySatisfy(f -> {
            assertThat(f.get("name")).isEqualTo("cat_plugin_sha256");
            assertThat(((Number) f.get("u")).intValue()).isZero();
        });
    }

    @Test
    void elMismoPluginYVersionEntranEnDosCores() {
        core("a");
        core("b");
        pluginSql("a", "p", "1.0.0", "h1");
        pluginSql("b", "p", "1.0.0", "h2");
        assertThat(jdbc.queryForObject("select count(*) from cat_plugin", Integer.class)).isEqualTo(2);
    }

    @Test
    void subidoEnSeGuardaComoInteger() {
        core("c");
        pluginSql("c", "p", "1.0.0", "h");
        assertThat(jdbc.queryForObject("select typeof(subido_en) from cat_plugin", String.class))
                .isEqualTo("integer");
    }

    @Test
    void imagenNulaSeRechazaPorNotNull() {
        core("c");
        rechaza("insert into cat_version_core values ('c','1.0.0',null,'anonimo',1)",
                "SQLITE_CONSTRAINT_NOTNULL");
    }

    @Test
    void lasTresFkDeclaranOnDeleteRestrict() {
        for (var tabla : java.util.List.of("cat_version_core", "cat_plugin", "cat_etiqueta")) {
            var fks = jdbc.queryForList("select on_delete from pragma_foreign_key_list('" + tabla + "')");
            assertThat(fks).as(tabla).isNotEmpty();
            assertThat(fks).as(tabla).allSatisfy(f -> assertThat(f.get("on_delete")).isEqualTo("RESTRICT"));
        }
    }

    @Test
    void borrarUnPluginConEtiquetasYUnCoreConPluginsSeRechaza() {
        core("c");
        pluginSql("c", "p", "1.0.0", "h");
        sql("insert into cat_etiqueta values ('c','p','1.0.0','pais','co','descriptor')");
        rechaza("delete from cat_plugin where nombre = 'p'", "SQLITE_CONSTRAINT_TRIGGER");
        rechaza("delete from cat_core where nombre = 'c'", "SQLITE_CONSTRAINT_TRIGGER");
        assertThat(jdbc.queryForObject("select count(*) from cat_etiqueta", Integer.class)).isEqualTo(1);
    }

    @Test
    void lasColumnasDeAuditoriaSonNotNull() {
        for (var par : java.util.List.of("cat_version_core:registrado_en", "cat_version_core:registrado_por",
                "cat_version_core:imagen", "cat_plugin:subido_en", "cat_plugin:subido_por")) {
            var t = par.split(":");
            assertThat(jdbc.queryForObject("select \"notnull\" from pragma_table_info('" + t[0]
                    + "') where name = ?", Integer.class, t[1])).as(par).isEqualTo(1);
        }
    }

    @Test
    void findAllDeLosRepositoriosDevuelveLoGuardado() {
        plantilla.insert(new Core("c"));
        plantilla.insert(version("c", "1.0.0", 1L));
        plantilla.insert(plugin("c", "p", "1.0.0", "h"));
        plantilla.insert(new Etiqueta("c", "p", "1.0.0", "pais", "co", "descriptor"));
        assertThat(cores.findAll()).containsExactly(new Core("c"));
        assertThat(versiones.findAll()).hasSize(1);
        assertThat(etiquetas.findAll()).hasSize(1);
    }
}
