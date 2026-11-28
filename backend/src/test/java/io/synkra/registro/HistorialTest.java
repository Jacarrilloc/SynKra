package io.synkra.registro;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** R-R5 (H-18): historial y detalle por HTTP y por RegistroApi. Archivo en build/sqlite-pruebas, ids 21 a 25. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class HistorialTest {

    static final Path DATOS = Path.of("build/sqlite-pruebas/HistorialTest");
    static final Instant T0 = Instant.parse("2026-11-18T11:06:43.456Z");
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
    }

    @Autowired RegistroApi api;
    @Autowired TestRestTemplate http;
    @Autowired ObjectMapper json;

    private static NuevoDespliegue nuevo(long id, String pais, TipoDespliegue tipo, Long origen, Instant en) {
        var plugin = new PluginDesplegado("kyc-pe", "1.3.0", "b".repeat(64), Map.of("pais", "pe", "cliente", "acme"));
        return new NuevoDespliegue(id, pais, tipo, origen, "synkra-core", "2.1.0",
                "localhost:5001/synkra-core@sha256:" + "a".repeat(64), "ana", "luis", en,
                "{\"global\":\"APROBADA\",\"porPlugin\":[],\"motivosGlobales\":[]}", List.of(plugin));
    }

    /** Siembra una sola vez por archivo, sin depender del orden de las pruebas. */
    @BeforeEach
    void sembrar() {
        synchronized (HistorialTest.class) {
            if (sembrado) {
                return;
            }
            api.registrar(nuevo(21, "pe", TipoDespliegue.DESPLIEGUE, null, T0.plusSeconds(1)));
            api.actualizarResultado(21, EstadoDespliegue.LISTO, null);
            api.registrar(nuevo(22, "pe", TipoDespliegue.DESPLIEGUE, null, T0.plusSeconds(3)));
            api.actualizarResultado(22, EstadoDespliegue.FALLIDO, new ContextoFallo("linea-1\n", "", "Warning BackOff: x"));
            api.registrar(nuevo(23, "pe", TipoDespliegue.DESPLIEGUE, null, T0.plusSeconds(2)));
            api.registrar(nuevo(24, "co", TipoDespliegue.DESPLIEGUE, null, T0.plusSeconds(5)));
            api.actualizarResultado(24, EstadoDespliegue.LISTO, null);
            api.registrar(nuevo(25, "pe", TipoDespliegue.REVERSION, 21L, T0.plusSeconds(3)));
            sembrado = true;
        }
    }

    private JsonNode get(String ruta, HttpStatus esperado) throws IOException {
        var r = http.getForEntity(ruta, String.class);
        assertThat(r.getStatusCode()).as(ruta).isEqualTo(esperado);
        return json.readTree(r.getBody());
    }

    private static List<Long> ids(JsonNode lista) {
        var ids = new ArrayList<Long>();
        lista.forEach(n -> ids.add(n.get("id").asLong()));
        return ids;
    }

    @Test
    void historialDePeVaDelMasRecienteAlMasAntiguoYDesempataPorId() throws IOException {
        var lista = get("/api/paises/pe/despliegues", HttpStatus.OK);
        assertThat(ids(lista)).containsExactly(25L, 22L, 23L, 21L);
        assertThat(lista.get(0).get("confirmadoEn").asText()).isEqualTo("2026-11-18T11:06:46.456Z");
        assertThat(lista.get(1).get("confirmadoEn").asText()).isEqualTo(lista.get(0).get("confirmadoEn").asText());
    }

    @Test
    void cadaElementoTraeLasClavesDelResumenYNoVeredictoNiContexto() throws IOException {
        var lista = get("/api/paises/pe/despliegues", HttpStatus.OK);
        for (var d : lista) {
            var claves = new TreeSet<String>();
            d.fieldNames().forEachRemaining(claves::add);
            assertThat(claves).containsExactly("confirmadoEn", "confirmadoPor", "core", "creadoPor", "estado", "id",
                    "imagen", "origenId", "pais", "plugins", "terminadoEn", "tipo", "versionCore");
            assertThat(d.get("plugins").get(0).get("nombre").asText()).isEqualTo("kyc-pe");
            assertThat(d.get("plugins").get(0).get("etiquetas").get("cliente").asText()).isEqualTo("acme");
        }
        var rev = lista.get(0);
        assertThat(rev.get("tipo").asText()).isEqualTo("reversion");
        assertThat(rev.get("origenId").asLong()).isEqualTo(21);
        assertThat(lista.get(3).get("origenId").isNull()).isTrue();
    }

    @Test
    void otrosPaisesYPaisSinFilas() throws IOException {
        assertThat(ids(get("/api/paises/co/despliegues", HttpStatus.OK))).containsExactly(24L);
        var vacia = get("/api/paises/cl/despliegues", HttpStatus.OK);
        assertThat(vacia.isArray()).isTrue();
        assertThat(vacia).isEmpty();
    }

    @Test
    void paisMalFormadoDa422SinRepetirLoRecibido() throws IOException {
        for (var malo : List.of("PE", "p", "per", "p1")) {
            var cuerpo = get("/api/paises/" + malo + "/despliegues", HttpStatus.UNPROCESSABLE_ENTITY);
            assertThat(cuerpo.get("codigo").asText()).as(malo).isEqualTo("PAIS_INVALIDO");
            assertThat(cuerpo.get("mensaje").asText()).isEqualTo("El país no es válido");
            assertThat(cuerpo.get("detalles")).hasSize(1);
            assertThat(cuerpo.get("detalles").get(0).get("campo").asText()).isEqualTo("pais");
            assertThat(cuerpo.get("detalles").get(0).get("motivo").asText())
                    .isEqualTo("El país debe ser un código de dos letras en minúscula (S4)");
            assertThat(cuerpo.toString()).as("no repite lo recibido: " + malo).doesNotContain("\"" + malo + "\"")
                    .doesNotContain(" " + malo + " ");
        }
    }

    @Test
    void detalleDeUnFallidoTraeContextoYVeredictoComoObjeto() throws IOException {
        var d = get("/api/despliegues/22", HttpStatus.OK);
        assertThat(d.get("estado").asText()).isEqualTo("fallido");
        var claves = new TreeSet<String>();
        d.get("contexto").fieldNames().forEachRemaining(claves::add);
        assertThat(claves).containsExactly("eventos", "logCore", "logInit");
        assertThat(d.get("contexto").get("logCore").asText()).isEqualTo("linea-1\n");
        assertThat(d.get("contexto").get("eventos").asText()).isEqualTo("Warning BackOff: x");
        assertThat(d.get("veredicto").isObject()).isTrue();
        assertThat(d.get("veredicto").get("global").asText()).isEqualTo("APROBADA");
        assertThat(d.get("terminadoEn").isNull()).isFalse();
        assertThat(d.get("plugins").get(0).get("etiquetas").get("pais").asText()).isEqualTo("pe");
    }

    @Test
    void detalleDeListoYDeAplicando() throws IOException {
        var listo = get("/api/despliegues/21", HttpStatus.OK);
        assertThat(listo.get("estado").asText()).isEqualTo("listo");
        assertThat(listo.get("terminadoEn").isNull()).isFalse();
        assertThat(listo.has("contexto")).isTrue();
        assertThat(listo.get("contexto").isNull()).isTrue();
        var aplicando = get("/api/despliegues/23", HttpStatus.OK);
        assertThat(aplicando.get("estado").asText()).isEqualTo("aplicando");
        assertThat(aplicando.get("terminadoEn").isNull()).isTrue();
        assertThat(aplicando.get("contexto").isNull()).isTrue();
    }

    @Test
    void idInexistenteDa404YIdNoNumericoLoResuelveLaPlataforma() throws IOException {
        var nf = get("/api/despliegues/999", HttpStatus.NOT_FOUND);
        assertThat(nf.get("codigo").asText()).isEqualTo("NO_ENCONTRADO");
        assertThat(nf.get("mensaje").asText()).isEqualTo("Recurso no encontrado");
        assertThat(nf.get("detalles")).isEmpty();
        var abc = get("/api/despliegues/abc", HttpStatus.BAD_REQUEST);
        assertThat(abc.get("codigo").asText()).isEqualTo("SOLICITUD_INVALIDA");
    }

    @Test
    void laApiDeRegistroObtieneYValida() {
        assertThat(api.obtener(999)).isEmpty();
        assertThat(api.obtener(0)).isEmpty();
        assertThat(api.obtener(-5)).isEmpty();
        assertThat(api.historial("cl")).isEmpty();
        assertThatThrownBy(() -> api.historial("PE")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> api.historial(null)).isInstanceOf(IllegalArgumentException.class);
        var fallido = api.obtener(22).orElseThrow();
        assertThat(fallido.contexto()).isEqualTo(new ContextoFallo("linea-1\n", "", "Warning BackOff: x"));
        assertThat(fallido.plugins()).containsExactly(
                new PluginDesplegado("kyc-pe", "1.3.0", "b".repeat(64), Map.of("pais", "pe", "cliente", "acme")));
        assertThat(api.obtener(21).orElseThrow().contexto()).isNull();
        assertThat(api.obtener(25).orElseThrow().origenId()).isEqualTo(21L);
        assertThat(api.historial("pe")).extracting(d -> d.contexto()).containsOnlyNulls();
        assertThat(api.historial("pe")).extracting(d -> d.id()).containsExactly(25L, 22L, 23L, 21L);
    }
}
