package io.synkra.catalogo;

import static org.assertj.core.api.Assertions.assertThat;

import io.synkra.catalogo.api.CatalogoApi;
import io.synkra.plataforma.CodigosError;
import io.synkra.plataforma.DetalleError;
import io.synkra.plataforma.ExcepcionApi;
import io.synkra.verificador.Version;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** R-C1 y R-C2 contra la API pública del catálogo, con SQLite real. */
@SpringBootTest
class RegistrarCoreTest {

    private static final String HEX = "a".repeat(64);
    private static final String IMAGEN = "registro.local/synkra/core:2.0.0@sha256:" + HEX;

    @TempDir
    static Path datos;

    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry r) {
        r.add("synkra.data", () -> datos.toString());
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

    private ExcepcionApi falla(Runnable accion) {
        var e = org.junit.jupiter.api.Assertions.assertThrows(ExcepcionApi.class, accion::run);
        return e;
    }

    private List<String> camposDe(ExcepcionApi e) {
        return e.error().detalles().stream().map(DetalleError::campo).toList();
    }

    private ExcepcionApi invalido(String nombre, String version, String imagen) {
        var e = falla(() -> api.registrarCore(nombre, version, imagen));
        assertThat(e.estado()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(e.error().codigo()).isEqualTo(CodigosError.ENTIDAD_NO_PROCESABLE);
        assertThat(e.error().mensaje()).isEqualTo(CodigosError.MSG_ENTIDAD_NO_PROCESABLE);
        return e;
    }

    @Test
    void laPrimeraVersionCreaElCoreConUnaVersion() {
        var r = api.registrarCore("synkra-core", "1.0.0", IMAGEN);
        assertThat(r.nombre()).isEqualTo("synkra-core");
        assertThat(r.versiones()).containsExactly(new Version(1, 0, 0));
        assertThat(api.obtenerCore("synkra-core")).isEqualTo(r);
    }

    @Test
    void laSegundaVersionNoDuplicaElCore() {
        api.registrarCore("synkra-core", "1.0.0", IMAGEN);
        var r = api.registrarCore("synkra-core", "1.1.0", IMAGEN);
        assertThat(r.versiones()).containsExactly(new Version(1, 0, 0), new Version(1, 1, 0));
        assertThat(jdbc.queryForObject("select count(*) from cat_core", Integer.class)).isEqualTo(1);
    }

    @Test
    void lasVersionesSeOrdenanComoVersionNoComoTexto() {
        api.registrarCore("synkra-core", "2.10.0", IMAGEN);
        var r = api.registrarCore("synkra-core", "2.9.0", IMAGEN);
        assertThat(r.versiones()).containsExactly(new Version(2, 9, 0), new Version(2, 10, 0));
    }

    @Test
    void laImagenSinDigestOConDigestMalFormadoDa422() {
        for (var imagen : new String[] {
                "registro.local/synkra/core:2.0.0",
                "registro.local/synkra/core@sha256:" + "a".repeat(63),
                "registro.local/synkra/core@sha256:" + "A".repeat(64),
                null}) {
            var e = invalido("synkra-core", "1.0.0", imagen);
            assertThat(camposDe(e)).containsExactly("imagen");
        }
        assertThat(jdbc.queryForObject("select count(*) from cat_core", Integer.class)).isZero();
    }

    @Test
    void elNombreConMayusculasOGuionBajoDa422() {
        for (var nombre : new String[] {"Synkra-Core", "synkra_core"}) {
            assertThat(camposDe(invalido(nombre, "1.0.0", IMAGEN))).containsExactly("nombre");
        }
    }

    @Test
    void laVersionSinParcheDa422() {
        assertThat(camposDe(invalido("synkra-core", "2.1", IMAGEN))).containsExactly("version");
    }

    @Test
    void tresCamposInvalidosDanUnSoloErrorConTresDetalles() {
        var e = invalido("Mal_Nombre", "2.1", "sin-digest");
        assertThat(camposDe(e)).containsExactlyInAnyOrder("nombre", "version", "imagen");
        assertThat(e.error().detalles()).allSatisfy(d -> assertThat(d.motivo()).isNotBlank());
    }

    @Test
    void laVersionRepetidaDa409YNoCambiaLaBase() {
        api.registrarCore("synkra-core", "1.0.0", IMAGEN);
        var otra = "registro.local/otra/core@sha256:" + "b".repeat(64);
        var e = falla(() -> api.registrarCore("synkra-core", "1.0.0", otra));
        assertThat(e.estado()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(e.error().codigo()).isEqualTo(CodigosError.CONFLICTO);
        assertThat(e.error().mensaje()).isEqualTo(CodigosError.MSG_CONFLICTO);
        assertThat(camposDe(e)).containsExactly("version");
        assertThat(jdbc.queryForObject(
                "select imagen from cat_version_core where core = 'synkra-core' and version = '1.0.0'",
                String.class)).isEqualTo(IMAGEN);
        assertThat(jdbc.queryForObject("select count(*) from cat_version_core", Integer.class)).isEqualTo(1);
    }

    @Test
    void registradoPorEsAnonimo() {
        api.registrarCore("synkra-core", "1.0.0", IMAGEN);
        assertThat(jdbc.queryForObject("select registrado_por from cat_version_core", String.class))
                .isEqualTo("anonimo");
    }

    @Test
    void unCoreInexistenteDa404() {
        var e = falla(() -> api.obtenerCore("no-existe"));
        assertThat(e.estado()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(e.error().codigo()).isEqualTo(CodigosError.NO_ENCONTRADO);
        assertThat(e.error().mensaje()).isEqualTo(CodigosError.MSG_NO_ENCONTRADO);
    }
}
