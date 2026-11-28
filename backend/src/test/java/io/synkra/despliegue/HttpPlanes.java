package io.synkra.despliegue;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/** Ayudante de pruebas de U07-D: crear y confirmar planes por HTTP, con la cabecera de usuario opcional. */
final class HttpPlanes {

    static final String CABECERA = "X-Synkra-Usuario";

    private final TestRestTemplate http;

    HttpPlanes(TestRestTemplate http) {
        this.http = http;
    }

    /** Cada plugin como {@code nombre:version}. {@code usuario} nulo: sin cabecera. */
    @SuppressWarnings("rawtypes")
    ResponseEntity<Map> crear(String pais, String core, String versionCore, String usuario, String... plugins) {
        var lista = new ArrayList<String>();
        for (var p : plugins) {
            var partes = p.split(":");
            lista.add("{\"nombre\":\"" + partes[0] + "\",\"version\":\"" + partes[1] + "\"}");
        }
        var json = "{\"pais\":\"" + pais + "\",\"core\":\"" + core + "\",\"versionCore\":\"" + versionCore
                + "\",\"plugins\":[" + String.join(",", lista) + "]}";
        var h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        if (usuario != null) {
            h.set(CABECERA, usuario);
        }
        return http.exchange("/api/despliegues", HttpMethod.POST, new HttpEntity<>(json, h), Map.class);
    }

    @SuppressWarnings("rawtypes")
    ResponseEntity<Map> confirmar(long id, String usuario) {
        var h = new HttpHeaders();
        if (usuario != null) {
            h.set(CABECERA, usuario);
        }
        return http.exchange("/api/despliegues/" + id + "/confirmacion", HttpMethod.POST, new HttpEntity<>(h),
                Map.class);
    }

    @SuppressWarnings("rawtypes")
    ResponseEntity<Map> obtener(String ruta) {
        return http.exchange(ruta, HttpMethod.GET, HttpEntity.EMPTY, Map.class);
    }

    @SuppressWarnings("rawtypes")
    ResponseEntity<List> lista(String ruta) {
        return http.exchange(ruta, HttpMethod.GET, HttpEntity.EMPTY, List.class);
    }

    @SuppressWarnings("rawtypes")
    ResponseEntity<Map> verificar(String core, String versionCore, String... plugins) {
        var lista = new ArrayList<String>();
        for (var p : plugins) {
            var partes = p.split(":");
            lista.add("{\"nombre\":\"" + partes[0] + "\",\"version\":\"" + partes[1] + "\"}");
        }
        var json = "{\"core\":\"" + core + "\",\"versionCore\":\"" + versionCore + "\",\"plugins\":["
                + String.join(",", lista) + "]}";
        var h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        return http.exchange("/api/verificaciones", HttpMethod.POST, new HttpEntity<>(json, h), Map.class);
    }

    static long id(ResponseEntity<Map> r) {
        return ((Number) r.getBody().get("id")).longValue();
    }

    /** Código, mensaje y detalles (campo y motivo) exactos. */
    @SafeVarargs
    @SuppressWarnings("rawtypes")
    static void esperaError(ResponseEntity<Map> r, HttpStatus estado, String codigo, String mensaje,
            Map<String, String>... detalles) {
        assertThat(r.getStatusCode()).isEqualTo(estado);
        assertThat(r.getBody()).containsEntry("codigo", codigo).containsEntry("mensaje", mensaje)
                .containsEntry("detalles", List.of(detalles));
    }

    /** {@code "MÉTODO ruta"} sin la consulta. */
    static List<String> sinConsulta(List<String> peticiones) {
        return peticiones.stream().map(p -> p.contains("?") ? p.substring(0, p.indexOf('?')) : p).toList();
    }

    static Map<String, String> detalle(String campo, String motivo) {
        return Map.of("campo", campo, "motivo", motivo);
    }
}
