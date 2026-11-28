package io.synkra.despliegue;

import static org.assertj.core.api.Assertions.assertThat;

import io.synkra.catalogo.api.CatalogoApi;
import io.synkra.kubernetes.SimuladorKubernetes;
import io.synkra.kubernetes.api.AdaptadorKubernetes;
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
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.convention.TestBean;

/**
 * U07-E ronda 2 (F-01, F y G): «lo que corre es lo confirmado» compara la imagen Y el conjunto de plugins con su
 * sha256. Cada caso cambia una sola cosa en el clúster (que está {@code listo}) y el despliegue no se da por listo:
 * se cierra por plazo como no aplicado.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class VigilanciaCoincideTest {

    static final Path DATOS = Path.of("build/sqlite-pruebas/VigilanciaCoincideTest");
    static final Instant T = Instant.parse("2026-11-22T15:00:00.789Z");
    static final String PLAZO = "Venció el plazo de vigilancia sin que el despliegue quedara listo";
    static final String NO_APLICADO = "Lo que corre en el clúster no es lo que se confirmó";

    static final SimuladorKubernetes SIM = new SimuladorKubernetes();
    static final RelojMovible RELOJ = new RelojMovible(T);

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

    @TestBean AdaptadorKubernetes adaptador;

    static AdaptadorKubernetes adaptador() {
        return SIM.adaptador();
    }

    @TestBean Clock reloj;

    static Clock reloj() {
        return RELOJ;
    }

    @AfterAll
    static void cierra() {
        SIM.close();
    }

    @Autowired CatalogoApi catalogo;
    @Autowired RegistroApi registro;
    @Autowired VigilanciaDespliegues vigilancia;

    private static boolean cargada;

    private synchronized void cargar() throws IOException {
        if (!cargada) {
            CatalogoDePrueba.cargar(catalogo);
            cargada = true;
        }
    }

    /** El clúster corre {@code imagen} con {@code plugins} y está listo; se confirmó imagen A con kyc-pe 1.3.0. */
    private void caso(long id, String pais, Instant base, String imagenEnCluster, List<PlanPais.PluginPlan> enCluster)
            throws IOException {
        cargar();
        var sha = catalogo.obtenerPlugin("synkra-core", "kyc-pe", "1.3.0").sha256();
        SIM.siembra(new PlanPais(pais, imagenEnCluster, enCluster));
        SIM.marcaListo(pais);
        registro.registrar(new NuevoDespliegue(id, pais, TipoDespliegue.DESPLIEGUE, null, "synkra-core", "2.1.0",
                CatalogoDePrueba.IMAGEN_A, "ana", "luis", base,
                "{\"global\":\"APROBADA\",\"porPlugin\":[],\"motivosGlobales\":[]}",
                List.of(new PluginDesplegado("kyc-pe", "1.3.0", sha, Map.of()))));

        RELOJ.fija(base.plusSeconds(60));
        assertThat(vigilancia.revisar()).isZero();
        assertThat(registro.obtener(id).orElseThrow().estado()).isEqualTo(EstadoDespliegue.APLICANDO);

        RELOJ.fija(base.plus(Duration.ofMinutes(2)));
        assertThat(vigilancia.revisar()).isEqualTo(1);
        var d = registro.obtener(id).orElseThrow();
        assertThat(d.estado()).isEqualTo(EstadoDespliegue.FALLIDO);
        assertThat(d.contexto()).isEqualTo(new ContextoFallo("", "", PLAZO + "\n" + NO_APLICADO));
    }

    @Test
    void soloLosPluginsDifierenNoEsListo() throws Exception {
        cargar();
        var kycCo = catalogo.obtenerPlugin("synkra-core", "kyc-co", "1.0.0").sha256();
        caso(960, "pe", T, CatalogoDePrueba.IMAGEN_A, List.of(new PlanPais.PluginPlan("kyc-co", "1.0.0", kycCo)));
    }

    @Test
    void soloLaImagenDifiereNoEsListo() throws Exception {
        cargar();
        var kycPe = catalogo.obtenerPlugin("synkra-core", "kyc-pe", "1.3.0").sha256();
        caso(961, "co", T.plus(Duration.ofHours(1)), CatalogoDePrueba.IMAGEN_B,
                List.of(new PlanPais.PluginPlan("kyc-pe", "1.3.0", kycPe)));
    }

    @Test
    void soloElShaDeUnPluginDifiereNoEsListo() throws Exception {
        caso(962, "cl", T.plus(Duration.ofHours(2)), CatalogoDePrueba.IMAGEN_A,
                List.of(new PlanPais.PluginPlan("kyc-pe", "1.3.0", "9".repeat(64))));
    }
}
