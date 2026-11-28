package io.synkra.despliegue;

import static io.synkra.despliegue.HttpPlanes.esperaError;
import static io.synkra.despliegue.HttpPlanes.id;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.synkra.catalogo.api.CatalogoApi;
import io.synkra.kubernetes.api.AdaptadorKubernetes;
import io.synkra.registro.api.RegistroApi;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/** U07-T07, R-D9 sin clúster: la confirmación no construye, no aplica, no registra y deja el plan pendiente. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ConfirmarSinClusterTest {

    static final Path DATOS = Path.of("build/sqlite-pruebas/ConfirmarSinClusterTest");

    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry r) throws IOException {
        if (Files.exists(DATOS)) {
            try (var s = Files.walk(DATOS)) {
                s.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
            }
        }
        Files.createDirectories(DATOS);
        r.add("synkra.data", () -> DATOS.toAbsolutePath().toString());
        r.add("synkra.paises", () -> "pe");
    }

    @MockitoBean AdaptadorKubernetes adaptador;

    @Autowired CatalogoApi catalogo;
    @Autowired RegistroApi registro;
    @Autowired TestRestTemplate rest;
    @Autowired JdbcTemplate jdbc;

    @Test
    void sinClusterDa503YNoTocaNada() throws Exception {
        when(adaptador.disponible()).thenReturn(false);
        when(adaptador.leerPais("pe")).thenReturn(Optional.empty());
        CatalogoDePrueba.cargar(catalogo);
        var http = new HttpPlanes(rest);
        var creado = http.crear("pe", "synkra-core", "2.1.0", "ana", "kyc-pe:1.3.0");
        assertThat(creado.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        var id = id(creado);

        var r = http.confirmar(id, "luis");

        esperaError(r, HttpStatus.SERVICE_UNAVAILABLE, "KUBERNETES_NO_DISPONIBLE", "No hay clúster al que aplicar el plan");
        assertThat(jdbc.queryForObject("select estado from dep_plan where id = ?", String.class, id))
                .isEqualTo("pendiente");
        assertThat(registro.obtener(id)).isEmpty();
        verify(adaptador, never()).construir(any());
        verify(adaptador, never()).aplicar(any());
    }
}
