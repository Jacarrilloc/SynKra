package io.synkra.despliegue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Comparator;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.data.jdbc.core.JdbcAggregateTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** U07-T01: repositorio de planes contra SQLite real (carpeta fija para leer el archivo de vuelta). */
@SpringBootTest
class PlanRepositorioTest {

    static final Path DATOS = Path.of("build/sqlite-pruebas/PlanRepositorioTest");
    static final String JSON = "{\"a\":1}";

    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry r) throws IOException {
        if (Files.exists(DATOS)) {
            try (var s = Files.walk(DATOS)) {
                s.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
            }
        }
        Files.createDirectories(DATOS);
        r.add("synkra.data", () -> DATOS.toAbsolutePath().toString());
    }

    @Autowired JdbcAggregateTemplate plantilla;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlanRepositorio planes;

    private Plan nuevo(String pais, String tipo, Long origen, Instant en) {
        return Plan.nuevo(pais, tipo, origen, "synkra-core", "1.0.0", "[{\"nombre\":\"p\",\"version\":\"1.0.0\"}]",
                JSON, JSON, "anonimo", en);
    }

    private void sql(String pais, String tipo, Long origen, String estado, long creado, long vence, String json) {
        jdbc.update("insert into dep_plan (pais, tipo, origen_id, core, version_core, plugins, resumen, cambios, "
                + "estado, creado_por, creado_en, vence_en) values (?,?,?,?,?,?,?,?,?,?,?,?)",
                pais, tipo, origen, "c", "1", json, JSON, JSON, estado, "x", creado, vence);
    }

    @Test
    void altaLecturaPorIdPorPaisYEstadoEInstanteDeIdaYVuelta() {
        var en = Instant.ofEpochMilli(1_700_000_000_123L);
        var guardado = plantilla.insert(nuevo("aa", Plan.DESPLIEGUE, null, en));
        assertThat(guardado.id()).isNotNull();
        var leido = planes.findById(guardado.id()).orElseThrow();
        assertThat(leido).isEqualTo(guardado);
        assertThat(leido.estado()).isEqualTo(Plan.PENDIENTE);
        assertThat(leido.origenId()).isNull();
        assertThat(Instant.ofEpochMilli(leido.creadoEn())).isEqualTo(en);
        assertThat(jdbc.queryForObject("select typeof(creado_en) from dep_plan where id = ?", String.class, guardado.id()))
                .isEqualTo("integer");
        assertThat(planes.findByPaisAndEstado("aa", Plan.PENDIENTE)).containsExactly(guardado);
        assertThat(planes.findByPaisAndEstado("aa", Plan.CONFIRMADO)).isEmpty();
    }

    @Test
    void laVigenciaSonTreintaMinutosEnMilisegundos() {
        var en = Instant.ofEpochMilli(1_700_000_000_123L);
        var p = nuevo("aa", Plan.DESPLIEGUE, null, en);
        assertThat(Plan.VIGENCIA.toMillis()).isEqualTo(1_800_000L);
        assertThat(p.creadoEn()).isEqualTo(1_700_000_000_123L);
        assertThat(p.venceEn() - p.creadoEn()).isEqualTo(1_800_000L);
        assertThat(p.id()).isNull();
        assertThat(p.estado()).isEqualTo("pendiente");
    }

    @Test
    void reversionAceptaUnOrigenSinClaveForanea() {
        var g = plantilla.insert(nuevo("ab", Plan.REVERSION, 41L, Instant.ofEpochMilli(1_000L)));
        assertThat(planes.findById(g.id()).orElseThrow().origenId()).isEqualTo(41L);
    }

    @Test
    void losIdsNoSeReutilizan() {
        var ac = plantilla.insert(nuevo("ac", Plan.DESPLIEGUE, null, Instant.ofEpochMilli(1_000L)));
        jdbc.update("delete from dep_plan where id = ?", ac.id());
        var ad = plantilla.insert(nuevo("ad", Plan.DESPLIEGUE, null, Instant.ofEpochMilli(1_000L)));
        assertThat(ad.id()).isGreaterThan(ac.id());
    }

    @Test
    void aceptaLosEstadosConfirmadoYVencido() {
        sql("ae", "despliegue", null, "confirmado", 1000, 1_801_000, JSON);
        sql("af", "despliegue", null, "vencido", 1000, 1_801_000, JSON);
        assertThat(planes.findByPaisAndEstado("ae", Plan.CONFIRMADO)).hasSize(1);
        assertThat(planes.findByPaisAndEstado("af", Plan.VENCIDO)).hasSize(1);
    }

    private void rechaza(Runnable r, String pais) {
        assertThatThrownBy(r::run).isInstanceOf(DataAccessException.class)
                .hasMessageContaining("SQLITE_CONSTRAINT_CHECK");
        assertThat(jdbc.queryForObject("select count(*) from dep_plan where pais = ?", Integer.class, pais))
                .isZero();
    }

    @Test
    void rechazaEstadoDesconocido() {
        rechaza(() -> sql("ba", "despliegue", null, "otro", 1000, 2000, JSON), "ba");
    }

    @Test
    void rechazaReversionSinOrigen() {
        rechaza(() -> sql("bb", "reversion", null, "pendiente", 1000, 2000, JSON), "bb");
    }

    @Test
    void rechazaDespliegueConOrigen() {
        rechaza(() -> sql("bc", "despliegue", 7L, "pendiente", 1000, 2000, JSON), "bc");
    }

    @Test
    void rechazaJsonInvalido() {
        rechaza(() -> sql("bd", "despliegue", null, "pendiente", 1000, 2000, "no es json"), "bd");
    }

    @Test
    void rechazaVencimientoNoPosterior() {
        rechaza(() -> sql("be", "despliegue", null, "pendiente", 2000, 2000, JSON), "be");
    }

    @Test
    void rechazaPaisConMayusculas() {
        rechaza(() -> sql("PE", "despliegue", null, "pendiente", 1000, 2000, JSON), "PE");
    }

    @Test
    void elRepositorioNoOfreceSaveDeleteNiExistsById() {
        var nombres = Arrays.stream(PlanRepositorio.class.getMethods()).map(m -> m.getName()).toList();
        assertThat(nombres).containsExactlyInAnyOrder("findById", "findByPaisAndEstado");
        assertThat(nombres).doesNotContain("save", "delete", "existsById");
    }
}
