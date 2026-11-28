package io.synkra.despliegue;

import static io.synkra.despliegue.HttpPlanes.id;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

import io.synkra.SynkraApp;
import io.synkra.catalogo.api.CatalogoApi;
import io.synkra.kubernetes.SimuladorKubernetes;
import io.synkra.kubernetes.api.AdaptadorKubernetes;
import io.synkra.registro.api.ContextoFallo;
import io.synkra.registro.api.EstadoDespliegue;
import io.synkra.registro.api.RegistroApi;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * U07-T09, R-D12 y C-109 (decisión 12): dos arranques sobre la misma carpeta. El segundo, con la programación real a
 * 100 ms, retoma los {@code aplicando} del primero sin reaplicar, y vence el plan que nadie toca. Las esperas son de
 * Awaitility; el tiempo lo mueve el {@link RelojMovible}.
 */
class RetomarVigilanciaTest {

    static final Path DATOS = Path.of("build/sqlite-pruebas/RetomarVigilanciaTest");
    static final Instant T4 = Instant.parse("2026-11-22T18:00:00.789Z");
    static final String PLAZO = "Venció el plazo de vigilancia sin que el despliegue quedara listo";

    static final SimuladorKubernetes SIM = new SimuladorKubernetes();
    static final RelojMovible RELOJ = new RelojMovible(T4);

    @AfterAll
    static void cierra() {
        SIM.close();
    }

    /** Propiedades por {@code .run(...)} (C-111); el adaptador y el reloj de prueba entran como beans primarios. */
    private static ConfigurableApplicationContext arranca(Path datos, String plazo, String... propiedades) {
        var args = new java.util.ArrayList<String>(java.util.List.of("--synkra.data=" + datos.toAbsolutePath(),
                "--server.port=0", "--synkra.paises=pe,co", "--synkra.vigilancia.plazo=" + plazo));
        args.addAll(java.util.List.of(propiedades));
        return new SpringApplicationBuilder(SynkraApp.class)
                .initializers((GenericApplicationContext ctx) -> {
                    ctx.registerBean("adaptadorDePrueba", AdaptadorKubernetes.class, SIM::adaptador, d -> {
                        d.setPrimary(true);
                        d.setDestroyMethodName("");
                    });
                    ctx.registerBean("relojDePrueba", Clock.class, () -> RELOJ, d -> d.setPrimary(true));
                })
                .run(args.toArray(String[]::new));
    }

    private static void borra(Path carpeta) throws IOException {
        if (Files.exists(carpeta)) {
            try (var s = Files.walk(carpeta)) {
                s.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
            }
        }
        Files.createDirectories(carpeta);
    }

    private static HttpPlanes http(ConfigurableApplicationContext ctx) {
        var puerto = ctx.getEnvironment().getProperty("local.server.port");
        return new HttpPlanes(new TestRestTemplate(new RestTemplateBuilder().rootUri("http://localhost:" + puerto)));
    }

    @Test
    void trasUnReinicioSeRetomaLaVigilanciaSinReaplicarYLosPlanesVencenSolos() throws Exception {
        borra(DATOS);
        SIM.aceptaAplicar("pe");
        SIM.aceptaAplicar("co");

        var uno = arranca(DATOS, "PT3M", "--synkra.programacion.activa=false");
        try {
            CatalogoDePrueba.cargar(uno.getBean(CatalogoApi.class));
            var http = http(uno);
            RELOJ.fija(T4);
            var pe = http.crear("pe", "synkra-core", "2.1.0", "ana", "kyc-pe:1.3.0");
            var co = http.crear("co", "synkra-core", "2.1.0", "ana", "kyc-pe:1.3.0");
            assertThat(pe.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(id(pe)).isEqualTo(1);
            assertThat(id(co)).isEqualTo(2);
            RELOJ.fija(T4.plus(Duration.ofMinutes(1)));
            assertThat(http.confirmar(1, "luis").getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
            assertThat(http.confirmar(2, "luis").getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
            assertThat(SIM.nuevasPeticiones()).filteredOn(p -> p.startsWith("PATCH ")).hasSize(6);
            assertThat(id(http.crear("pe", "synkra-core", "2.1.0", "ana", "kyc-pe:1.3.0"))).isEqualTo(3);
            var registro = uno.getBean(RegistroApi.class);
            assertThat(registro.obtener(1).orElseThrow().estado()).isEqualTo(EstadoDespliegue.APLICANDO);
            assertThat(registro.obtener(2).orElseThrow().estado()).isEqualTo(EstadoDespliegue.APLICANDO);
        } finally {
            uno.close();
        }

        SIM.marcaListo("pe");
        RELOJ.fija(T4.plus(Duration.ofMinutes(2)));
        SIM.nuevasPeticiones();

        var dos = arranca(DATOS, "PT3M", "--synkra.programacion.activa=true", "--synkra.vigilancia.intervalo=PT0.1S",
                "--synkra.planes.barrido=PT0.1S");
        try {
            var registro = dos.getBean(RegistroApi.class);
            var jdbc = dos.getBean(JdbcTemplate.class);
            await().atMost(Duration.ofSeconds(20)).pollInterval(Duration.ofMillis(50))
                    .until(() -> registro.obtener(1).orElseThrow().estado() == EstadoDespliegue.LISTO);
            assertThat(registro.obtener(2).orElseThrow().estado()).isEqualTo(EstadoDespliegue.APLICANDO);

            RELOJ.fija(T4.plus(Duration.ofMinutes(4)));
            await().atMost(Duration.ofSeconds(20)).pollInterval(Duration.ofMillis(50))
                    .until(() -> registro.obtener(2).orElseThrow().estado() == EstadoDespliegue.FALLIDO);
            assertThat(registro.obtener(2).orElseThrow().contexto()).isEqualTo(new ContextoFallo("", "", PLAZO));

            RELOJ.fija(T4.plus(Duration.ofMinutes(31)));
            await().atMost(Duration.ofSeconds(20)).pollInterval(Duration.ofMillis(50))
                    .until(() -> "vencido".equals(jdbc.queryForObject("select estado from dep_plan where id = 3", String.class)));

            assertThat(SIM.nuevasPeticiones()).isNotEmpty().allSatisfy(p -> assertThat(p).startsWith("GET "));
        } finally {
            dos.close();
        }
    }

    @Test
    void unPlazoNoPositivoNoArranca() throws Exception {
        var otra = Files.createTempDirectory("retomar-plazo");
        assertThatThrownBy(() -> arranca(otra, "PT0S", "--synkra.programacion.activa=false")
                .close())
                .hasRootCauseInstanceOf(IllegalStateException.class)
                .hasRootCauseMessage("synkra.vigilancia.plazo debe ser positivo");
    }
}
