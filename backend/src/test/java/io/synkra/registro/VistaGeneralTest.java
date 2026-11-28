package io.synkra.registro;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.synkra.kubernetes.api.AdaptadorKubernetes;
import io.synkra.kubernetes.api.ClienteNoDisponibleException;
import io.synkra.kubernetes.api.LecturaFallidaException;
import io.synkra.kubernetes.api.LecturaPais;
import io.synkra.kubernetes.api.LecturaPais.PluginLeido;
import io.synkra.kubernetes.api.LecturaPaisInconsistenteException;
import io.synkra.registro.api.ContextoFallo;
import io.synkra.registro.api.DespliegueRegistrado;
import io.synkra.registro.api.EstadoDespliegue;
import io.synkra.registro.api.LecturaCluster;
import io.synkra.registro.api.NuevoDespliegue;
import io.synkra.registro.api.PluginCorriendo;
import io.synkra.registro.api.PluginDesplegado;
import io.synkra.registro.api.RegistroApi;
import io.synkra.registro.api.TipoDespliegue;
import io.synkra.registro.api.VistaGeneralApi;
import io.synkra.registro.api.VistaPais;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * H-19, R-R6 y R-R7: la vista general sobre un doble del AdaptadorKubernetes. Archivo en
 * build/sqlite-pruebas/VistaGeneralTest, ids 41 a 50.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class VistaGeneralTest {

    static final Path DATOS = Path.of("build/sqlite-pruebas/VistaGeneralTest");
    static final Instant T0 = Instant.parse("2026-11-18T11:06:43.456Z");
    static final String A = "localhost:5001/synkra-core@sha256:" + "a".repeat(64);
    static final String B = "localhost:5001/synkra-core@sha256:" + "b".repeat(64);
    static final List<String> ORDEN = List.of("pe", "co", "cl", "ar", "uy", "bo", "ec", "ve", "py", "sv");
    private static boolean sembrado;

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
        r.add("synkra.paises", () -> String.join(",", ORDEN));
    }

    @MockitoBean AdaptadorKubernetes adaptador;
    @Autowired RegistroApi registro;
    @Autowired VistaGeneralApi vista;
    @Autowired TestRestTemplate http;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;

    private static String sha(char c) {
        return String.valueOf(c).repeat(64);
    }

    private static PluginDesplegado plugin(String nombre, String version, char c) {
        return new PluginDesplegado(nombre, version, sha(c), Map.of());
    }

    private static PluginLeido leido(String nombre, String version, char c) {
        return new PluginLeido(nombre, version, sha(c));
    }

    private void alta(long id, String pais, int segundos, EstadoDespliegue fin, String imagen,
            PluginDesplegado... plugins) {
        registro.registrar(new NuevoDespliegue(id, pais, TipoDespliegue.DESPLIEGUE, null, "synkra-core", "2.1.0",
                imagen, "ana", "luis", T0.plusSeconds(segundos),
                "{\"global\":\"APROBADA\",\"porPlugin\":[],\"motivosGlobales\":[]}", List.of(plugins)));
        if (fin == EstadoDespliegue.LISTO) {
            registro.actualizarResultado(id, fin, null);
        } else if (fin == EstadoDespliegue.FALLIDO) {
            registro.actualizarResultado(id, fin, new ContextoFallo("linea-1\n", "", "Warning BackOff: x"));
        }
    }

    /** Siembra una sola vez por archivo (decisión 9); el orden de las altas no importa. */
    @BeforeEach
    void sembrarYProgramar() {
        synchronized (VistaGeneralTest.class) {
            if (!sembrado) {
                alta(41, "pe", 4, EstadoDespliegue.LISTO, A, plugin("kyc-pe", "1.3.0", 'b'), plugin("kyc-aml", "1.0.0", 'd'));
                alta(42, "pe", 1, EstadoDespliegue.LISTO, A, plugin("kyc-pe", "1.2.0", 'e'));
                alta(43, "pe", 6, EstadoDespliegue.FALLIDO, A, plugin("kyc-pe", "1.3.0", 'b'));
                alta(44, "co", 2, EstadoDespliegue.LISTO, A, plugin("kyc-co", "1.0.0", 'c'));
                alta(45, "cl", 3, EstadoDespliegue.LISTO, A, plugin("kyc-pe", "1.3.0", 'b'));
                alta(46, "uy", 5, EstadoDespliegue.LISTO, A, plugin("kyc-pe", "1.3.0", 'b'));
                alta(47, "pe", 7, EstadoDespliegue.APLICANDO, A, plugin("kyc-pe", "1.3.0", 'b'));
                alta(48, "cl", 3, EstadoDespliegue.LISTO, B, plugin("kyc-pe", "1.3.0", 'b'));
                alta(49, "mx", 5, EstadoDespliegue.LISTO, A, plugin("kyc-pe", "1.3.0", 'b'));
                alta(50, "sv", 5, EstadoDespliegue.LISTO, A, plugin("kyc-pe", "1.3.0", 'b'));
                sembrado = true;
            }
        }
        // El doble se reinicia tras cada prueba (@MockitoBean): se programa de nuevo en cada una.
        when(adaptador.leerPais("pe")).thenReturn(Optional.of(new LecturaPais("pe", A,
                List.of(leido("kyc-pe", "1.3.0", 'b'), leido("kyc-aml", "1.0.0", 'd')))));
        when(adaptador.leerPais("co")).thenReturn(Optional.of(new LecturaPais("co", A,
                List.of(leido("kyc-co", "1.0.0", 'f')))));
        when(adaptador.leerPais("cl")).thenReturn(Optional.of(new LecturaPais("cl", A,
                List.of(leido("kyc-pe", "1.3.0", 'b')))));
        when(adaptador.leerPais("ar")).thenReturn(Optional.empty());
        when(adaptador.leerPais("uy")).thenReturn(Optional.empty());
        when(adaptador.leerPais("bo")).thenReturn(Optional.of(new LecturaPais("bo", A,
                List.of(leido("kyc-pe", "1.3.0", 'b')))));
        when(adaptador.leerPais("ec")).thenThrow(new LecturaPaisInconsistenteException("ec", "falta el ConfigMap"));
        when(adaptador.leerPais("ve")).thenThrow(new LecturaFallidaException("el Deployment", "synkra-ve", null));
        when(adaptador.leerPais("py")).thenThrow(new ClienteNoDisponibleException());
        when(adaptador.leerPais("sv")).thenReturn(Optional.of(new LecturaPais("sv", A,
                List.of(leido("kyc-pe", "1.3.0", 'b'), leido("kyc-extra", "1.0.0", '9')))));
    }

    private static VistaPais de(List<VistaPais> v, String pais) {
        return v.stream().filter(x -> x.pais().equals(pais)).findFirst().orElseThrow();
    }

    @Test
    void daLosDiezPaisesEnElOrdenConfiguradoSinMxConLaLecturaLaConcordanciaYElUltimoListo() {
        var v = vista.vistaGeneral();
        assertThat(v.stream().map(VistaPais::pais)).containsExactlyElementsOf(ORDEN);
        assertThat(v.stream().map(VistaPais::lectura)).containsExactly(LecturaCluster.LEIDO, LecturaCluster.LEIDO,
                LecturaCluster.LEIDO, LecturaCluster.VACIO, LecturaCluster.VACIO, LecturaCluster.LEIDO,
                LecturaCluster.INCONSISTENTE, LecturaCluster.NO_DISPONIBLE, LecturaCluster.NO_DISPONIBLE,
                LecturaCluster.LEIDO);
        assertThat(v.stream().map(VistaPais::concuerda)).containsExactly(true, false, false, true, false, false,
                false, null, null, false);
        assertThat(v.stream().map(VistaPais::ultimoDespliegueId)).containsExactly(41L, 44L, 48L, null, 46L, null,
                null, null, null, 50L);
    }

    @Test
    void pePluginsOrdenadosImagenLeidaYCoreDelUltimoListo() {
        var pe = de(vista.vistaGeneral(), "pe");
        assertThat(pe.plugins()).containsExactly(new PluginCorriendo("kyc-aml", "1.0.0", sha('d')),
                new PluginCorriendo("kyc-pe", "1.3.0", sha('b')));
        assertThat(pe.imagen()).isEqualTo(A);
        assertThat(pe.core()).isEqualTo("synkra-core");
        assertThat(pe.versionCore()).isEqualTo("2.1.0");
        var ar = de(vista.vistaGeneral(), "ar");
        assertThat(ar.core()).isNull();
        assertThat(ar.versionCore()).isNull();
    }

    @Test
    void sinLecturaLaImagenEsNulaYLosPluginsVaciosNuncaNulos() {
        var v = vista.vistaGeneral();
        for (var pais : List.of("ar", "ec", "ve", "py", "uy")) {
            assertThat(de(v, pais).imagen()).as(pais).isNull();
            assertThat(de(v, pais).plugins()).as(pais).isNotNull().isEmpty();
        }
    }

    @Test
    void soloLeePaisesConfiguradosUnaVezCadaUnoEnOrdenYNadaMas() {
        vista.vistaGeneral();
        var orden = inOrder(adaptador);
        for (var pais : ORDEN) {
            orden.verify(adaptador).leerPais(pais);
        }
        verify(adaptador, never()).leerPais("mx");
        verifyNoMoreInteractions(adaptador);
    }


    /** H-19: imagen y plugins son lo que corre en el clúster, no lo registrado (cl, sv y co difieren). */
    @Test
    void imagenYPluginsSonLosDelClusterNoLosDelRegistro() {
        var v = vista.vistaGeneral();
        assertThat(de(v, "cl").imagen()).isEqualTo(A).isNotEqualTo(B);
        assertThat(de(v, "sv").plugins()).containsExactly(new PluginCorriendo("kyc-extra", "1.0.0", sha('9')),
                new PluginCorriendo("kyc-pe", "1.3.0", sha('b')));
        assertThat(de(v, "co").plugins()).containsExactly(new PluginCorriendo("kyc-co", "1.0.0", sha('f')));
        assertThat(de(v, "bo").plugins()).containsExactly(new PluginCorriendo("kyc-pe", "1.3.0", sha('b')));
    }

    @Test
    void elJsonTambienTraeLaImagenYLosPluginsDelCluster() throws IOException {
        var lista = json.readTree(http.getForEntity("/api/paises", String.class).getBody());
        assertThat(porPais(lista, "cl").get("imagen").asText()).isEqualTo(A);
        var sv = porPais(lista, "sv").get("plugins");
        assertThat(sv).hasSize(2);
        assertThat(sv.get(0).get("nombre").asText()).isEqualTo("kyc-extra");
        assertThat(sv.get(0).get("sha256").asText()).isEqualTo(sha('9'));
        var co = porPais(lista, "co").get("plugins");
        assertThat(co).hasSize(1);
        assertThat(co.get(0).get("sha256").asText()).isEqualTo(sha('f'));
        assertThat(porPais(lista, "cl").get("ultimoDespliegueId").asLong()).isEqualTo(48);
    }

    @Test
    void getApiPaisesDa200ConElMismoContenidoYLasClavesExactas() throws IOException {
        var r = http.getForEntity("/api/paises", String.class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        var lista = json.readTree(r.getBody());
        assertThat(lista.isArray()).isTrue();
        var paises = new ArrayList<String>();
        lista.forEach(d -> paises.add(d.get("pais").asText()));
        assertThat(paises).containsExactlyElementsOf(ORDEN);
        for (var d : lista) {
            var claves = new TreeSet<String>();
            d.fieldNames().forEachRemaining(claves::add);
            assertThat(claves).containsExactly("concuerda", "core", "imagen", "lectura", "pais", "plugins",
                    "ultimoDespliegueId", "versionCore");
        }
        var pe = lista.get(0);
        assertThat(pe.get("lectura").asText()).isEqualTo("leido");
        assertThat(pe.get("concuerda").isBoolean()).isTrue();
        assertThat(pe.get("concuerda").asBoolean()).isTrue();
        assertThat(pe.get("ultimoDespliegueId").asLong()).isEqualTo(41);
        assertThat(pe.get("imagen").asText()).isEqualTo(A);
        assertThat(pe.get("core").asText()).isEqualTo("synkra-core");
        assertThat(pe.get("versionCore").asText()).isEqualTo("2.1.0");
        assertThat(pe.get("plugins")).hasSize(2);
        assertThat(pe.get("plugins").get(0).get("nombre").asText()).isEqualTo("kyc-aml");
        assertThat(pe.get("plugins").get(0).get("sha256").asText()).isEqualTo(sha('d'));
        var minusculas = new ArrayList<String>();
        lista.forEach(d -> minusculas.add(d.get("lectura").asText()));
        assertThat(minusculas).containsExactly("leido", "leido", "leido", "vacio", "vacio", "leido",
                "inconsistente", "no_disponible", "no_disponible", "leido");
        assertThat(porPais(lista, "ve").get("concuerda").isNull()).isTrue();
        assertThat(porPais(lista, "py").get("concuerda").isNull()).isTrue();
        assertThat(porPais(lista, "ec").get("concuerda").asBoolean()).isFalse();
        assertThat(porPais(lista, "ar").get("imagen").isNull()).isTrue();
        assertThat(porPais(lista, "ar").get("plugins")).isEmpty();
        assertThat(porPais(lista, "ar").get("ultimoDespliegueId").isNull()).isTrue();
    }

    private static JsonNode porPais(JsonNode lista, String pais) {
        for (var d : lista) {
            if (d.get("pais").asText().equals(pais)) {
                return d;
            }
        }
        throw new AssertionError(pais);
    }

    @Test
    void ultimoListoTomaElMasRecienteListoDesempataPorIdYNoMiraSynkraPaises() {
        assertThat(registro.ultimoListo("pe")).isPresent();
        assertThat(registro.ultimoListo("pe").orElseThrow().id()).isEqualTo(41);
        assertThat(registro.ultimoListo("pe")).isEqualTo(registro.obtener(41));
        assertThat(registro.ultimoListo("cl").orElseThrow().id()).isEqualTo(48);
        assertThat(registro.ultimoListo("mx").orElseThrow().id()).isEqualTo(49);
        assertThat(registro.ultimoListo("ar")).isEmpty();
        assertThat(registro.ultimoListo("pe").orElseThrow().contexto()).isNull();
        assertThat(registro.ultimoListo("pe").orElseThrow().plugins().stream().map(PluginDesplegado::nombre))
                .containsExactly("kyc-aml", "kyc-pe");
    }

    @Test
    void ultimoListoRechazaPaisMalFormado() {
        assertThatThrownBy(() -> registro.ultimoListo("PE")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> registro.ultimoListo(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> registro.ultimoListo("per")).isInstanceOf(IllegalArgumentException.class);
    }

    /** Decisión 7: mientras se lee el clúster, la única conexión del pool queda libre para otra petición. */
    @Test
    void laVistaNoRetieneLaUnicaConexionMientrasLeeElCluster() throws Exception {
        var otro = Executors.newSingleThreadExecutor();
        var resultado = new AtomicReference<Object>();
        try {
            when(adaptador.leerPais("pe")).thenAnswer(inv -> {
                Future<Long> f = otro.submit(() -> jdbc.queryForObject("select count(*) from reg_despliegue", Long.class));
                try {
                    resultado.set(f.get(5, TimeUnit.SECONDS));
                } catch (Throwable t) {
                    resultado.set(t);
                }
                return Optional.of(new LecturaPais("pe", A,
                        List.of(leido("kyc-pe", "1.3.0", 'b'), leido("kyc-aml", "1.0.0", 'd'))));
            });
            vista.vistaGeneral();
        } finally {
            otro.shutdown();
        }
        assertThat(resultado.get()).as("el otro hilo terminó en menos de 5 s").isEqualTo(10L);
    }

    // ---- R-R6 directamente ----

    private static DespliegueRegistrado ultimo(String imagen, PluginDesplegado... plugins) {
        return new DespliegueRegistrado(1, "pe", TipoDespliegue.DESPLIEGUE, null, "synkra-core", "2.1.0", imagen,
                "ana", "luis", T0, EstadoDespliegue.LISTO, T0.plusSeconds(1), "{}", List.of(plugins), null);
    }

    private static Optional<LecturaPais> lectura(String imagen, PluginLeido... plugins) {
        return Optional.of(new LecturaPais("pe", imagen, List.of(plugins)));
    }

    @Test
    void concordanciaConVaciosYMixtos() {
        assertThat(Concordancia.concuerda(Optional.empty(), Optional.empty())).isTrue();
        assertThat(Concordancia.concuerda(Optional.of(ultimo(A)), Optional.empty())).isFalse();
        assertThat(Concordancia.concuerda(Optional.empty(), lectura(A))).isFalse();
    }

    @Test
    void concordanciaPorImagenConDigestYConjuntoDePlugins() {
        var kycPe = plugin("kyc-pe", "1.3.0", 'b');
        var kycAml = plugin("kyc-aml", "1.0.0", 'd');
        // iguales, aunque en otro orden
        assertThat(Concordancia.concuerda(Optional.of(ultimo(A, kycPe, kycAml)),
                lectura(A, leido("kyc-aml", "1.0.0", 'd'), leido("kyc-pe", "1.3.0", 'b')))).isTrue();
        // sin plugins en los dos lados
        assertThat(Concordancia.concuerda(Optional.of(ultimo(A)), lectura(A))).isTrue();
        // otro digest
        assertThat(Concordancia.concuerda(Optional.of(ultimo(B, kycPe)), lectura(A, leido("kyc-pe", "1.3.0", 'b'))))
                .isFalse();
        // otro sha256
        assertThat(Concordancia.concuerda(Optional.of(ultimo(A, kycPe)), lectura(A, leido("kyc-pe", "1.3.0", 'f'))))
                .isFalse();
        // otra versión
        assertThat(Concordancia.concuerda(Optional.of(ultimo(A, kycPe)), lectura(A, leido("kyc-pe", "1.2.0", 'b'))))
                .isFalse();
        // un plugin de más en el clúster
        assertThat(Concordancia.concuerda(Optional.of(ultimo(A, kycPe)),
                lectura(A, leido("kyc-pe", "1.3.0", 'b'), leido("kyc-extra", "1.0.0", '9')))).isFalse();
        // un plugin de menos en el clúster
        assertThat(Concordancia.concuerda(Optional.of(ultimo(A, kycPe, kycAml)),
                lectura(A, leido("kyc-pe", "1.3.0", 'b')))).isFalse();
    }
}
