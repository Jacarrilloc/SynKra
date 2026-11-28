package io.synkra.registro;

import static org.assertj.core.api.Assertions.assertThat;

import io.synkra.registro.api.ContextoFallo;
import io.synkra.registro.api.EstadoDespliegue;
import io.synkra.registro.api.NuevoDespliegue;
import io.synkra.registro.api.RegistroApi;
import io.synkra.registro.api.TipoDespliegue;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** R-R4: contexto truncado. El archivo SQLite queda en build/sqlite-pruebas para leerlo de vuelta. */
@SpringBootTest
class ContextoFalloTest {

    static final Path DATOS = Path.of("build/sqlite-pruebas/ContextoFalloTest");
    static final int MAX = 65_536;

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

    private static String lineas(int n) {
        return IntStream.rangeClosed(1, n).mapToObj(i -> String.format("linea-%04d\n", i))
                .collect(Collectors.joining());
    }

    private static int bytes(String s) {
        return s.getBytes(StandardCharsets.UTF_8).length;
    }

    @Test
    void milLineasGuardanLasUltimas200() {
        var r = TruncadoContexto.logCore(lineas(1000));
        assertThat(r.split("\n")).hasSize(200).startsWith("linea-0801").endsWith("linea-1000");
        assertThat(r).isEqualTo(lineas(1000).substring(800 * 11));
        assertThat(r).doesNotContain(TruncadoContexto.MARCA);
    }

    @Test
    void doscientasLineasOMenosQuedanIguales() {
        assertThat(TruncadoContexto.logCore(lineas(200))).isEqualTo(lineas(200));
        assertThat(TruncadoContexto.logCore(lineas(3))).isEqualTo(lineas(3));
        assertThat(TruncadoContexto.logCore("sin salto final")).isEqualTo("sin salto final");
    }

    @Test
    void sinSaltoFinalLaUltimaLineaNoGanaUnSalto() {
        var sin = lineas(300);
        sin = sin.substring(0, sin.length() - 1);
        var r = TruncadoContexto.logCore(sin);
        assertThat(r).endsWith("linea-0300").doesNotEndWith("\n");
        assertThat(r.split("\n")).hasSize(200).startsWith("linea-0101");
    }

    @Test
    void unTextoDe100KbQuedaEn65536BytesConMarcaYElFinalOriginal() {
        var original = IntStream.range(0, 102_400).mapToObj(i -> String.valueOf((char) ('a' + i % 26)))
                .collect(Collectors.joining());
        var r = TruncadoContexto.texto(original);
        assertThat(bytes(r)).isEqualTo(MAX);
        assertThat(r).startsWith("[truncado]\n");
        assertThat(original).endsWith(r.substring("[truncado]\n".length()));
    }

    @Test
    void unTextoDeExactamente65536BytesQuedaIgualYSinMarca() {
        var exacto = "x".repeat(MAX);
        assertThat(TruncadoContexto.texto(exacto)).isEqualTo(exacto);
        var uno = "x".repeat(MAX + 1);
        assertThat(bytes(TruncadoContexto.texto(uno))).isEqualTo(MAX);
        assertThat(TruncadoContexto.texto(uno)).startsWith("[truncado]\n");
    }

    @Test
    void cienKbDeEnieDanUtf8ValidoSinCaracterPartido() {
        var original = "ñ".repeat(51_200);
        var r = TruncadoContexto.texto(original);
        assertThat(bytes(r)).isLessThanOrEqualTo(MAX);
        assertThat(r).startsWith("[truncado]\n").doesNotContain("�");
        var cuerpo = r.substring("[truncado]\n".length());
        assertThat(cuerpo).matches("ñ+");
        assertThat(original).endsWith(cuerpo);
        // Ida y vuelta: los bytes decodifican sin sustituciones.
        assertThat(new String(r.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8)).isEqualTo(r);
    }

    @Test
    void milLineasLargasRecibenLosDosCortes() {
        var linea = "x".repeat(499) + "\n"; // 500 bytes: 200 lineas = 100000 bytes
        var log = IntStream.range(0, 1000).mapToObj(i -> String.format("%04d", i) + linea.substring(4))
                .collect(Collectors.joining());
        var r = TruncadoContexto.logCore(log);
        assertThat(bytes(r)).isEqualTo(MAX);
        assertThat(r).startsWith("[truncado]\n");
        assertThat(r).endsWith("0999" + linea.substring(4));
        assertThat(r).doesNotContain("0799");
    }

    @Test
    void nulosSeGuardanComoVacio() {
        assertThat(TruncadoContexto.logCore(null)).isEmpty();
        assertThat(TruncadoContexto.texto(null)).isEmpty();
        var c = new ContextoFallo(null, null, null);
        assertThat(c.logCore()).isEmpty();
        assertThat(c.logInit()).isEmpty();
        assertThat(c.eventos()).isEmpty();
    }

    @Test
    void integracionElId1FallidoGuardaElContextoTruncado() {
        api.registrar(new NuevoDespliegue(1, "pe", TipoDespliegue.DESPLIEGUE, null, "synkra-core", "1.0.0",
                "reg/c@sha256:" + "a".repeat(64), "anonimo", "anonimo", Instant.parse("2026-11-27T10:15:30Z"), "{}",
                List.of()));
        api.actualizarResultado(1, EstadoDespliegue.FALLIDO,
                new ContextoFallo(lineas(1000), "", "e".repeat(102_400)));
        var fila = jdbc.queryForMap("select log_core, log_init, eventos from reg_contexto where despliegue_id = 1");
        assertThat(((String) fila.get("log_core")).split("\n")).hasSize(200).startsWith("linea-0801");
        assertThat(fila.get("log_init")).isEqualTo("");
        assertThat(bytes((String) fila.get("eventos"))).isEqualTo(MAX);
        assertThat((String) fila.get("eventos")).startsWith("[truncado]");
    }
}
