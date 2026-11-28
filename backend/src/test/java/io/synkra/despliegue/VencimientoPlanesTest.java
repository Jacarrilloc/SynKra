package io.synkra.despliegue;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.convention.TestBean;

/**
 * U07-T06, Q6 (F-02 de la ronda 1): lo que devuelve {@code vencerSiToca} y la frontera de {@code vencerPendientes}.
 * Filas sembradas con SQL, sin clúster; la tabla se vacía antes de cada caso.
 */
@SpringBootTest
class VencimientoPlanesTest {

    static final Path DATOS = Path.of("build/sqlite-pruebas/VencimientoPlanesTest");
    static final Instant AHORA = Instant.parse("2026-11-18T11:00:00.123Z");
    static final long MS = AHORA.toEpochMilli();
    static final RelojMovible RELOJ = new RelojMovible(AHORA);

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

    @TestBean Clock reloj;

    static Clock reloj() {
        return RELOJ;
    }

    @Autowired VencimientoPlanes vencimiento;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void limpia() {
        RELOJ.fija(AHORA);
        jdbc.update("delete from dep_plan");
    }

    private void siembra(long id, String estado, long venceEn) {
        jdbc.update("insert into dep_plan (id, pais, tipo, origen_id, core, version_core, plugins, resumen, cambios, estado,"
                + " creado_por, creado_en, vence_en) values (?, 'pe', 'despliegue', null, 'synkra-core', '2.1.0', '[]', '{}',"
                + " '{}', ?, 'anonimo', ?, ?)", id, estado, venceEn - 1_800_000, venceEn);
    }

    private String estado(long id) {
        return jdbc.queryForObject("select estado from dep_plan where id = ?", String.class, id);
    }

    @Test
    void vencerSiTocaDiceSiCambioUnaFila() {
        siembra(1, "pendiente", MS + 1);
        siembra(2, "pendiente", MS);
        siembra(3, "confirmado", MS - 100);
        siembra(4, "vencido", MS - 100);

        assertThat(vencimiento.vencerSiToca(1)).isFalse();
        assertThat(estado(1)).isEqualTo("pendiente");
        assertThat(vencimiento.vencerSiToca(2)).isTrue();
        assertThat(estado(2)).isEqualTo("vencido");
        assertThat(vencimiento.vencerSiToca(2)).isFalse();
        assertThat(vencimiento.vencerSiToca(3)).isFalse();
        assertThat(estado(3)).isEqualTo("confirmado");
        assertThat(vencimiento.vencerSiToca(4)).isFalse();
        assertThat(vencimiento.vencerSiToca(999)).isFalse();
    }

    @Test
    void elBarridoVenceDesdeVenceEnExactoYSoloLosPendientes() {
        siembra(1, "pendiente", MS - 1);
        siembra(2, "pendiente", MS);
        siembra(3, "pendiente", MS + 1);
        siembra(4, "confirmado", MS - 100);

        assertThat(vencimiento.vencerPendientes()).isEqualTo(2);

        assertThat(List.of(estado(1), estado(2), estado(3), estado(4)))
                .containsExactly("vencido", "vencido", "pendiente", "confirmado");
        assertThat(vencimiento.vencerPendientes()).isZero();
    }
}
