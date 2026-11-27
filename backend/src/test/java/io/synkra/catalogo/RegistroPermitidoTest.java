package io.synkra.catalogo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.synkra.SynkraApp;
import io.synkra.catalogo.api.CatalogoApi;
import io.synkra.plataforma.CodigosError;
import io.synkra.plataforma.DetalleError;
import io.synkra.plataforma.ExcepcionApi;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** R-C3 (H-31) con {@code synkra.registro-permitido=localhost:5001}. */
@SpringBootTest
class RegistroPermitidoTest {

    private static final String H = "@sha256:" + "a".repeat(64);

    @TempDir
    static Path datos;

    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry r) {
        r.add("synkra.data", () -> datos.toString());
        r.add("synkra.registro-permitido", () -> "localhost:5001");
    }

    @Autowired CatalogoApi api;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void limpiar() {
        jdbc.execute("delete from cat_etiqueta");
        jdbc.execute("delete from cat_plugin");
        jdbc.execute("delete from cat_version_core");
        jdbc.execute("delete from cat_core");
    }

    private ExcepcionApi rechazada(String imagen) {
        var e = org.junit.jupiter.api.Assertions.assertThrows(ExcepcionApi.class,
                () -> api.registrarCore("synkra-core", "1.0.0", imagen));
        assertThat(e.estado()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(e.error().codigo()).isEqualTo(CodigosError.ENTIDAD_NO_PROCESABLE);
        assertThat(e.error().detalles()).extracting(DetalleError::campo).containsExactly("imagen");
        return e;
    }

    @Test
    void otroRegistroDa422ConElMotivoDeR_C3() {
        var e = rechazada("docker.io/x" + H);
        assertThat(e.error().detalles().get(0).motivo()).isEqualTo(MotivosCatalogo.REGISTRO_NO_PERMITIDO.formatted("localhost:5001"));
        assertThat(jdbc.queryForObject("select count(*) from cat_core", Integer.class)).isZero();
    }

    @Test
    void elRegistroPermitidoSeAcepta() {
        assertThat(api.registrarCore("synkra-core", "1.0.0", "localhost:5001/x" + H).versiones()).hasSize(1);
        assertThat(api.registrarCore("synkra-core", "1.1.0", "localhost:5001/a/b:t" + H).versiones()).hasSize(2);
    }

    @Test
    void otroPuertoOSinPuertoDan422() {
        rechazada("localhost:5002/x" + H);
        rechazada("localhost/x" + H);
        rechazada("localhost:50010/x" + H);
    }

    @Test
    void unaImagenSinRegistroDa422() {
        rechazada("synkra-core" + H);
        // primer segmento que parece repo pero es el registro: no coincide
        rechazada("synkra/core" + H);
    }

    @Test
    void laFormaMalaNoAgregaElMotivoDeRegistro() {
        var e = rechazada("docker.io/X" + H);
        assertThat(e.error().detalles().get(0).motivo()).isEqualTo(MotivosCatalogo.IMAGEN_INVALIDA);
        // R-C1 sigue mandando: la gramática no admite mayúsculas en la imagen
        assertThat(rechazada("LOCALHOST:5001/x" + H).error().detalles().get(0).motivo())
                .isEqualTo(MotivosCatalogo.IMAGEN_INVALIDA);
    }

    @Test
    void sinValorNoSeAplicaYElHostNoDistingueMayusculas() {
        for (var vacio : new String[] {"", "   "}) {
            var r = new RegistroPermitido(vacio);
            assertThat(r.activo()).isFalse();
            assertThat(r.permite("docker.io/x" + H)).isTrue();
            assertThat(r.permite("synkra-core" + H)).isTrue();
        }
        var mayus = new RegistroPermitido("LOCALHOST:5001");
        assertThat(mayus.permite("localhost:5001/x" + H)).isTrue();
        assertThat(mayus.permite("LOCALHOST:5001/x" + H)).isTrue();
        assertThat(mayus.permite("localhost:5002/x" + H)).isFalse();
    }

    @Test
    void unValorInvalidoImpideArrancarElContexto() {
        for (var malo : List.of("http://localhost:5001", "localhost:5001/x", "local host", "a:b", "-x", "a".repeat(300) + "/")) {
            assertThatThrownBy(() -> new SpringApplicationBuilder(SynkraApp.class)
                    .web(WebApplicationType.NONE)
                    .run("--synkra.data=" + datos, "--synkra.registro-permitido=" + malo).close())
                    .as("valor %s", malo.length() > 40 ? malo.length() + " caracteres" : malo)
                    .hasRootCauseInstanceOf(IllegalStateException.class)
                    .rootCause().hasMessageContaining("synkra.registro-permitido");
        }
    }
}
