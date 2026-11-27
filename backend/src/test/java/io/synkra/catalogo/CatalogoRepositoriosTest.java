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
        assertThat(cores.findById("synkra-core")).contains(new Core("synkra-core"));
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
}
