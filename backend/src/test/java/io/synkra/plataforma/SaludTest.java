package io.synkra.plataforma;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class SaludTest {

    @TempDir
    static Path raiz;

    /** Cada contexto (uno por prueba) usa su propio directorio de datos. */
    static Path datos;

    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry r) {
        datos = raiz.resolve("bd-" + System.nanoTime());
        r.add("synkra.data", () -> datos.toString());
    }

    @Autowired
    TestRestTemplate http;

    @Test
    @SuppressWarnings("unchecked")
    void saludUpConComponenteDb() {
        var r = http.getForEntity("/actuator/health", Map.class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(r.getBody()).containsEntry("status", "UP");
        var componentes = (Map<String, Object>) r.getBody().get("components");
        assertThat(componentes).containsKey("db");
        assertThat(((Map<String, Object>) componentes.get("db"))).containsEntry("status", "UP");
    }

    @Test
    void otrosEndpointsNoExpuestos() {
        assertThat(http.getForEntity("/actuator/env", String.class).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(http.getForEntity("/actuator/beans", String.class).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @SuppressWarnings("unchecked")
    void conBaseInutilizableLaSaludCae() throws IOException {
        try (var arbol = Files.walk(datos)) {
            arbol.sorted(Comparator.reverseOrder())
                    .filter(p -> !p.equals(datos))
                    .forEach(p -> p.toFile().delete());
        }
        Files.deleteIfExists(datos);
        var r = http.getForEntity("/actuator/health", Map.class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(r.getBody()).containsEntry("status", "DOWN");
        var componentes = (Map<String, Object>) r.getBody().get("components");
        assertThat((Map<String, Object>) componentes.get("db")).containsEntry("status", "DOWN");
    }
}
