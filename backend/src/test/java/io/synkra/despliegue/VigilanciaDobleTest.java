package io.synkra.despliegue;

import static org.assertj.core.api.Assertions.assertThat;

import io.synkra.kubernetes.api.AdaptadorKubernetes;
import io.synkra.kubernetes.api.ContextoFalloK8s;
import io.synkra.kubernetes.api.EstadoPais;
import io.synkra.kubernetes.api.FaseDespliegue;
import io.synkra.kubernetes.api.LecturaFallidaException;
import io.synkra.kubernetes.api.LecturaPais;
import io.synkra.kubernetes.api.ObjetosPais;
import io.synkra.kubernetes.api.PlanPais;
import io.synkra.registro.api.ContextoFallo;
import io.synkra.registro.api.EstadoDespliegue;
import io.synkra.registro.api.NuevoDespliegue;
import io.synkra.registro.api.PluginDesplegado;
import io.synkra.registro.api.RegistroApi;
import io.synkra.registro.api.TipoDespliegue;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.convention.TestBean;

/**
 * U07-E ronda 2 (F-01, A, B, C, I y J): un adaptador de prueba (sin servidor simulado) que responde por país, para las
 * reglas de la decisión 2 que el simulador no puede provocar: una fila que lanza, un contexto que no se puede leer, un
 * contexto con datos al vencer el plazo y una fila que otra pasada ya cerró. Los despliegues se registran directo.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class VigilanciaDobleTest {

    static final Path DATOS = Path.of("build/sqlite-pruebas/VigilanciaDobleTest");
    static final Instant T = Instant.parse("2026-11-22T15:00:00.789Z");
    static final String SHA = "b".repeat(64);
    static final String PLAZO = "Venció el plazo de vigilancia sin que el despliegue quedara listo";
    static final String SIN_LECTURA = "No se pudo leer el estado del despliegue en el clúster";
    static final RelojMovible RELOJ = new RelojMovible(T);

    /** Antes de que se pida el contexto de {@code ww}, otra pasada (el gancho) ya cerró su fila. */
    static volatile Runnable alPedirContextoDeWw = () -> { };

    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry r) throws IOException {
        if (Files.exists(DATOS)) {
            try (var s = Files.walk(DATOS)) {
                s.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
            }
        }
        Files.createDirectories(DATOS);
        r.add("synkra.data", () -> DATOS.toAbsolutePath().toString());
        r.add("synkra.vigilancia.plazo", () -> "PT2M");
    }

    /** Todo país corre lo confirmado; lo demás se decide por país. */
    static final class Doble implements AdaptadorKubernetes {
        @Override public boolean disponible() { return true; }
        @Override public boolean existeNamespace(String namespace) { throw new UnsupportedOperationException(); }
        @Override public ObjetosPais construir(PlanPais plan) { throw new UnsupportedOperationException(); }
        @Override public void aplicar(ObjetosPais objetos) { throw new UnsupportedOperationException(); }

        @Override
        public Optional<LecturaPais> leerPais(String pais) {
            if (pais.equals("xx")) {
                throw new IllegalStateException("bug de prueba en la lectura de xx");
            }
            return Optional.of(new LecturaPais(pais, CatalogoDePrueba.IMAGEN_A,
                    List.of(new LecturaPais.PluginLeido("kyc-pe", "1.3.0", SHA))));
        }

        @Override
        public EstadoPais estado(String namespace) {
            return switch (namespace) {
                case "synkra-vv" -> new EstadoPais(1, 1, 0, FaseDespliegue.LISTO, null);
                case "synkra-yy" -> new EstadoPais(1, 0, 3, FaseDespliegue.FALLIDO, "CrashLoopBackOff");
                default -> new EstadoPais(1, 0, 0, FaseDespliegue.EN_CURSO, null);
            };
        }

        @Override
        public ContextoFalloK8s contextoFallo(String namespace) {
            return switch (namespace) {
                case "synkra-yy" -> throw new LecturaFallidaException("los pods de synkra-core", namespace,
                        new RuntimeException("ilegible"));
                case "synkra-zz" -> new ContextoFalloK8s("lc", "li", "Warning X: y");
                case "synkra-ww" -> {
                    alPedirContextoDeWw.run();
                    yield new ContextoFalloK8s("", "", "");
                }
                default -> new ContextoFalloK8s("", "", "");
            };
        }
    }

    @TestBean AdaptadorKubernetes adaptador;

    static AdaptadorKubernetes adaptador() {
        return new Doble();
    }

    @TestBean Clock reloj;

    static Clock reloj() {
        return RELOJ;
    }

    @Autowired RegistroApi registro;
    @Autowired VigilanciaDespliegues vigilancia;

    private void registra(long id, String pais, Instant base) {
        registro.registrar(new NuevoDespliegue(id, pais, TipoDespliegue.DESPLIEGUE, null, "synkra-core", "2.1.0",
                CatalogoDePrueba.IMAGEN_A, "ana", "luis", base,
                "{\"global\":\"APROBADA\",\"porPlugin\":[],\"motivosGlobales\":[]}",
                List.of(new PluginDesplegado("kyc-pe", "1.3.0", SHA, Map.of()))));
    }

    private EstadoDespliegue estado(long id) {
        return registro.obtener(id).orElseThrow().estado();
    }

    @Test
    void unaFilaQueLanzaNoDetieneLaPasada() {
        var base = T;
        registra(970, "xx", base);
        registra(971, "vv", base.plusSeconds(5));
        RELOJ.fija(base.plusSeconds(30));

        assertThat(vigilancia.revisar()).isEqualTo(1);

        assertThat(estado(971)).isEqualTo(EstadoDespliegue.LISTO);
        assertThat(estado(970)).isEqualTo(EstadoDespliegue.APLICANDO);
        registro.actualizarResultado(970, EstadoDespliegue.FALLIDO, new ContextoFallo("", "", "cierre de la prueba"));
    }

    @Test
    void unFallidoConContextoIlegibleSeCierraPorPlazoComoSinLectura() {
        var base = T.plus(Duration.ofHours(1));
        registra(972, "yy", base);

        RELOJ.fija(base.plusSeconds(30));
        assertThat(vigilancia.revisar()).isZero();
        assertThat(estado(972)).isEqualTo(EstadoDespliegue.APLICANDO);

        RELOJ.fija(base.plus(Duration.ofMinutes(2)));
        assertThat(vigilancia.revisar()).isEqualTo(1);
        var d = registro.obtener(972).orElseThrow();
        assertThat(d.estado()).isEqualTo(EstadoDespliegue.FALLIDO);
        assertThat(d.contexto()).isEqualTo(new ContextoFallo("", "", PLAZO + "\n" + SIN_LECTURA));
    }

    @Test
    void alVencerElPlazoElContextoDelClusterEntraEnElRegistro() {
        var base = T.plus(Duration.ofHours(2));
        registra(973, "zz", base);

        RELOJ.fija(base.plusSeconds(30));
        assertThat(vigilancia.revisar()).isZero();

        RELOJ.fija(base.plus(Duration.ofMinutes(2)));
        assertThat(vigilancia.revisar()).isEqualTo(1);
        assertThat(registro.obtener(973).orElseThrow().contexto())
                .isEqualTo(new ContextoFallo("lc", "li", PLAZO + "\nWarning X: y"));
    }

    @Test
    void unaFilaQueOtraPasadaYaCerroSeSaltaSinCortarLaPasada() {
        var base = T.plus(Duration.ofHours(3));
        registra(974, "ww", base);
        registra(975, "vv", base.plusSeconds(5));
        alPedirContextoDeWw = () -> registro.actualizarResultado(974, EstadoDespliegue.LISTO, null);
        try {
            RELOJ.fija(base.plus(Duration.ofMinutes(2)));
            // ww vence: antes de cerrarla otra pasada la dejó LISTO. vv ya vencida pero lista: se cierra LISTO.
            assertThat(vigilancia.revisar()).isEqualTo(1);
        } finally {
            alPedirContextoDeWw = () -> { };
        }
        assertThat(estado(974)).isEqualTo(EstadoDespliegue.LISTO);
        assertThat(estado(975)).isEqualTo(EstadoDespliegue.LISTO);
    }
}
