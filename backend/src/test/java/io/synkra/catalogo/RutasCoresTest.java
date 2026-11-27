package io.synkra.catalogo;

import static org.assertj.core.api.Assertions.assertThat;

import io.synkra.plataforma.CodigosError;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** H-01: las rutas de cores contra el contenedor real y SQLite real. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RutasCoresTest {

    private static final String IMAGEN = "localhost:5001/synkra-core@sha256:" + "0".repeat(64);

    @TempDir
    static Path datos;

    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry r) {
        r.add("synkra.data", () -> datos.toString());
    }

    @Autowired TestRestTemplate http;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void limpiar() {
        jdbc.execute("delete from cat_etiqueta");
        jdbc.execute("delete from cat_plugin");
        jdbc.execute("delete from cat_version_core");
        jdbc.execute("delete from cat_core");
    }

    private org.springframework.http.ResponseEntity<Map> post(String json) {
        var h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        return http.exchange("/api/cores", HttpMethod.POST, new HttpEntity<>(json, h), Map.class);
    }

    private static String cuerpo(String nombre, String version) {
        return "{\"nombre\":\"" + nombre + "\",\"version\":\"" + version + "\",\"imagen\":\"" + IMAGEN + "\"}";
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, String>> detalles(Map<?, ?> cuerpo) {
        return (List<Map<String, String>>) cuerpo.get("detalles");
    }

    @Test
    @SuppressWarnings("unchecked")
    void registrarDevuelve201ConLocationYCuerpo() {
        var r = post(cuerpo("synkra-core", "2.1.0"));
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(r.getHeaders().getLocation().getPath()).isEqualTo("/api/cores/synkra-core");
        assertThat(r.getBody().get("nombre")).isEqualTo("synkra-core");
        assertThat((List<Object>) r.getBody().get("versiones"))
                .containsExactly(Map.of("version", "2.1.0", "imagen", IMAGEN));
    }

    @Test
    void unaSegundaVersionApareceEnElGetJuntoALaPrimera() {
        post(cuerpo("synkra-core", "2.1.0"));
        assertThat(post(cuerpo("synkra-core", "3.0.0")).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        var r = http.getForEntity("/api/cores/synkra-core", Map.class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        var vs = (List<Map<String, String>>) r.getBody().get("versiones");
        assertThat(vs).extracting(v -> v.get("version")).containsExactly("2.1.0", "3.0.0");
    }

    @Test
    void laListaVaOrdenadaPorNombre() {
        post(cuerpo("zeta", "1.0.0"));
        post(cuerpo("alfa", "1.0.0"));
        var r = http.getForEntity("/api/cores", List.class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(((List<Map<String, Object>>) r.getBody())).extracting(c -> c.get("nombre"))
                .containsExactly("alfa", "zeta");
    }

    @Test
    void unaVersionRepetidaDa409ConErrorApi() {
        post(cuerpo("synkra-core", "3.0.0"));
        var r = post(cuerpo("synkra-core", "3.0.0"));
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(r.getBody().get("codigo")).isEqualTo(CodigosError.CONFLICTO);
        assertThat(detalles(r.getBody())).extracting(d -> d.get("campo")).containsExactly("version");
    }

    @Test
    void tresCamposInvalidosDan422ConTresDetalles() {
        var r = post("{\"nombre\":\"Mal Nombre\",\"version\":\"x\",\"imagen\":\"y\"}");
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(r.getBody().get("codigo")).isEqualTo(CodigosError.ENTIDAD_NO_PROCESABLE);
        assertThat(detalles(r.getBody())).extracting(d -> d.get("campo"))
                .containsExactlyInAnyOrder("nombre", "version", "imagen");
    }

    @Test
    void unJsonSinImagenDa422ConLosTresDetalles() {
        var r = post("{\"nombre\":\"Mal Nombre\",\"version\":\"x\"}");
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(r.getBody().get("codigo")).isEqualTo(CodigosError.ENTIDAD_NO_PROCESABLE);
        assertThat(detalles(r.getBody())).extracting(d -> d.get("campo"))
                .containsExactlyInAnyOrder("nombre", "version", "imagen");
    }

    @Test
    void unJsonSinImagenDa422ConCampoImagen() {
        var r = post("{\"nombre\":\"synkra-core\",\"version\":\"1.0.0\"}");
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(detalles(r.getBody())).hasSize(1);
        assertThat(detalles(r.getBody()).get(0).get("campo")).isEqualTo("imagen");
        assertThat(detalles(r.getBody()).get(0).get("motivo")).isEqualTo(MotivosCatalogo.IMAGEN_INVALIDA);
    }

    @Test
    void unTipoEquivocadoNoSeCombinaConLasReglasDeR_C1() {
        var r = post("{\"nombre\":\"Mal Nombre\",\"version\":\"x\",\"imagen\":5}");
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(r.getBody().get("codigo")).isEqualTo(CodigosError.ENTIDAD_NO_PROCESABLE);
        assertThat(detalles(r.getBody())).hasSize(1);
        assertThat(detalles(r.getBody()).get(0))
                .isEqualTo(Map.of("campo", "imagen", "motivo", CodigosError.MOTIVO_TIPO_INVALIDO));
    }

    @Test
    void sinNombreOSinVersionDa422ConEsteCampo() {
        var sinNombre = post("{\"version\":\"1.0.0\",\"imagen\":\"" + IMAGEN + "\"}");
        assertThat(sinNombre.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(detalles(sinNombre.getBody())).extracting(d -> d.get("campo")).containsExactly("nombre");
        var sinVersion = post("{\"nombre\":\"a\",\"imagen\":\"" + IMAGEN + "\"}");
        assertThat(sinVersion.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(detalles(sinVersion.getBody())).extracting(d -> d.get("campo")).containsExactly("version");
    }

    @Test
    void unCuerpoVacioDa422ConLosTresDetalles() {
        var r = post("{}");
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(detalles(r.getBody())).extracting(d -> d.get("campo"))
                .containsExactlyInAnyOrder("nombre", "version", "imagen");
    }

    @Test
    void unCuerpoQueNoEsObjetoDa400() {
        var r = post("[]");
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(r.getBody().get("codigo")).isEqualTo(CodigosError.SOLICITUD_INVALIDA);
    }

    @Test
    void unTipoEquivocadoDa422ConElCampoReal() {
        for (var c : List.of(
                new String[] {"imagen", "{\"nombre\":\"a\",\"version\":\"1.0.0\",\"imagen\":[\"x\"]}"},
                new String[] {"imagen", "{\"nombre\":\"a\",\"version\":\"1.0.0\",\"imagen\":{\"a\":1}}"},
                new String[] {"nombre", "{\"nombre\":123,\"version\":\"1.0.0\",\"imagen\":\"" + IMAGEN + "\"}"},
                new String[] {"nombre", "{\"nombre\":true,\"version\":\"1.0.0\",\"imagen\":\"" + IMAGEN + "\"}"})) {
            var r = post(c[1]);
            assertThat(r.getStatusCode()).as(c[1]).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
            assertThat(r.getBody().get("codigo")).isEqualTo(CodigosError.ENTIDAD_NO_PROCESABLE);
            assertThat(detalles(r.getBody())).extracting(d -> d.get("campo")).as(c[1]).containsExactly(c[0]);
        }
        assertThat(http.getForEntity("/api/cores", List.class).getBody()).isEmpty();
    }

    @Test
    void cadaVersionConservaSuPropiaImagenEnElPostYEnElGet() {
        var img1 = "localhost:5001/synkra-core@sha256:" + "1".repeat(64);
        var img2 = "localhost:5001/synkra-core@sha256:" + "2".repeat(64);
        var p1 = post("{\"nombre\":\"synkra-core\",\"version\":\"1.0.0\",\"imagen\":\"" + img1 + "\"}");
        assertThat(p1.getBody().get("versiones")).isEqualTo(List.of(Map.of("version", "1.0.0", "imagen", img1)));
        var p2 = post("{\"nombre\":\"synkra-core\",\"version\":\"2.0.0\",\"imagen\":\"" + img2 + "\"}");
        var esperado = List.of(Map.of("version", "1.0.0", "imagen", img1), Map.of("version", "2.0.0", "imagen", img2));
        assertThat(p2.getBody().get("versiones")).isEqualTo(esperado);
        assertThat(http.getForEntity("/api/cores/synkra-core", Map.class).getBody().get("versiones"))
                .isEqualTo(esperado);
    }

    @Test
    void unJsonMalformadoDa400SolicitudInvalida() {
        var r = post("{\"nombre\":");
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(r.getBody().get("codigo")).isEqualTo(CodigosError.SOLICITUD_INVALIDA);
    }

    @Test
    void pluginsDeUnCoreSinPluginsEsVacioYDeUnoInexistenteDa404() {
        post(cuerpo("synkra-core", "2.1.0"));
        var ok = http.getForEntity("/api/cores/synkra-core/plugins", List.class);
        assertThat(ok.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(ok.getBody()).isEmpty();
        var no = http.getForEntity("/api/cores/no-existe/plugins", Map.class);
        assertThat(no.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(no.getBody().get("codigo")).isEqualTo(CodigosError.NO_ENCONTRADO);
        assertThat(http.getForEntity("/api/cores/no-existe", Map.class).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }
}
