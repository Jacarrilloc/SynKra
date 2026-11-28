package io.synkra.registro;

import static org.assertj.core.api.Assertions.assertThat;

import io.synkra.registro.api.ContextoFallo;
import io.synkra.registro.api.EstadoDespliegue;
import io.synkra.registro.api.NuevoDespliegue;
import io.synkra.registro.api.PluginDesplegado;
import io.synkra.registro.api.RegistroApi;
import io.synkra.registro.api.TipoDespliegue;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** U07-E, C-116: {@code RegistroApi.aplicando()} (ids 41 a 45): solo los aplicando, de todos los países, el más antiguo primero. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AplicandoTest {

    static final Path DATOS = Path.of("build/sqlite-pruebas/AplicandoTest");
    static final Instant T = Instant.parse("2026-11-22T10:00:00.123Z");

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

    @Autowired RegistroApi registro;
    @Autowired JdbcTemplate jdbc;

    private static NuevoDespliegue nuevo(long id, String pais, Instant en, PluginDesplegado... plugins) {
        return new NuevoDespliegue(id, pais, TipoDespliegue.DESPLIEGUE, null, "synkra-core", "2.1.0",
                "localhost:5001/synkra-core@sha256:" + "a".repeat(64), "ana", "luis", en,
                "{\"global\":\"APROBADA\",\"porPlugin\":[],\"motivosGlobales\":[]}", List.of(plugins));
    }

    private static PluginDesplegado plugin(String nombre, String sha) {
        return new PluginDesplegado(nombre, "1.3.0", sha.repeat(64), Map.of("pais", "pe"));
    }

    @Test
    void soloLosAplicandoDeTodosLosPaisesElMasAntiguoPrimero() {
        // «cl» no está en synkra.paises (co,pe): aplicando() no mira la lista de países.
        registro.registrar(nuevo(41, "pe", T.plusSeconds(2), plugin("kyc-pe", "b"), plugin("kyc-aa", "c")));
        registro.registrar(nuevo(42, "co", T.plusSeconds(1), plugin("kyc-co", "d")));
        registro.registrar(nuevo(43, "pe", T));
        registro.actualizarResultado(43, EstadoDespliegue.LISTO, null);
        registro.registrar(nuevo(44, "co", T, plugin("kyc-co", "e")));
        registro.actualizarResultado(44, EstadoDespliegue.FALLIDO, new ContextoFallo("l", "i", "e"));
        registro.registrar(nuevo(45, "cl", T.plusSeconds(2), plugin("kyc-cl", "f")));

        var filas = registro.aplicando();

        assertThat(filas).extracting(d -> d.id()).containsExactly(42L, 41L, 45L);
        assertThat(filas).allSatisfy(d -> {
            assertThat(d.estado()).isEqualTo(EstadoDespliegue.APLICANDO);
            assertThat(d.contexto()).isNull();
            assertThat(d.terminadoEn()).isNull();
        });
        assertThat(filas.get(0).plugins()).containsExactly(plugin("kyc-co", "d"));
        assertThat(filas.get(1).plugins()).containsExactly(plugin("kyc-aa", "c"), plugin("kyc-pe", "b"));
        assertThat(filas.get(2).plugins()).containsExactly(plugin("kyc-cl", "f"));
        assertThat(filas.get(0)).isEqualTo(registro.obtener(42).orElseThrow());
        assertThat(jdbc.queryForList("select id from reg_despliegue where estado = 'aplicando' order by id", Long.class))
                .containsExactly(41L, 42L, 45L);
    }
}
