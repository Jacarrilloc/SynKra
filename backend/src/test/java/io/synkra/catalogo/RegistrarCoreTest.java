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

    private static final String H = "@sha256:" + HEX;

    @Test
    void lasImagenesHostilesDan422() {
        var hostiles = new String[] {
                "registro/repo@sha256:" + "a".repeat(65),
                "basura registro/repo" + H,
                "registro/repo" + H + "basura",
                "registro/repo" + H + "\n",
                "repo" + H,
                "registro/repo:tag:otro" + H,
                "registro:1:2/repo" + H,
                "registro/repo:tag/sub" + H,
                "registro/repo:" + H,
                "../x" + H,
                "../../etc/passwd" + H,
                "registro/REPO" + H,
                "REGISTRO/repo" + H,
                "registro/rëpo" + H,
                "registro/re\u00a0po" + H,
                "registro/re\u2028po" + H,
                "registro/re\u0085po" + H,
                "registro/re\0po" + H,
                "registro/" + "a".repeat(5000) + H,
                "registro/" + "a".repeat(250) + H};
        for (var imagen : hostiles) {
            assertThat(camposDe(invalido("synkra-core", "1.0.0", imagen)))
                    .as("imagen rechazada: %s", imagen.length() > 80 ? imagen.length() + " caracteres" : imagen)
                    .containsExactly("imagen");
        }
    }

    @Test
    void lasImagenesValidasSeAceptan() {
        var validas = new String[] {
                "registro.local:5000/synkra/core:2.0.0" + H,
                "localhost:5001/synkra-core" + H,
                "registro/repo" + H,
                "ghcr.io/org/sub.repo_x/core:v1.2-rc_1" + H};
        for (int i = 0; i < validas.length; i++) {
            var r = api.registrarCore("core-" + i, "1.0.0", validas[i]);
            assertThat(r.versiones()).hasSize(1);
        }
    }

    /** C-33: bordes de la longitud de la imagen (255 caracteres) y del tag (128). */
    private static String imagenDeLongitud(int total) {
        return "r.io/" + "a".repeat(total - 5 - H.length()) + H;
    }

    @Test
    void laImagenDe255CaracteresSeAcepta() {
        var imagen = imagenDeLongitud(255);
        assertThat(imagen).hasSize(255);
        assertThat(api.registrarCore("synkra-core", "1.0.0", imagen).versiones()).hasSize(1);
    }

    @Test
    void laImagenDe256CaracteresDa422() {
        var imagen = imagenDeLongitud(256);
        assertThat(imagen).hasSize(256);
        assertThat(camposDe(invalido("synkra-core", "1.0.0", imagen))).containsExactly("imagen");
    }

    @Test
    void elTagDe128CaracteresSeAcepta() {
        var imagen = "r.io/x:" + "t".repeat(128) + H;
        assertThat(api.registrarCore("synkra-core", "1.0.0", imagen).versiones()).hasSize(1);
    }

    @Test
    void elTagDe129CaracteresDa422() {
        var imagen = "r.io/x:" + "t".repeat(129) + H;
        assertThat(camposDe(invalido("synkra-core", "1.0.0", imagen))).containsExactly("imagen");
    }

    @Test
    void nombreYVersionNulosDan422() {
        assertThat(camposDe(invalido(null, "1.0.0", IMAGEN))).containsExactly("nombre");
        assertThat(camposDe(invalido("synkra-core", null, IMAGEN))).containsExactly("version");
        assertThat(camposDe(invalido(null, null, null))).containsExactlyInAnyOrder("nombre", "version", "imagen");
    }

    @Test
    void siFallaLaVersionElCoreNuevoNoQueda() {
        jdbc.execute("create trigger falla_version before insert on cat_version_core "
                + "begin select raise(abort, 'falla simulada'); end");
        try {
            org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class,
                    () -> api.registrarCore("synkra-core", "1.0.0", IMAGEN));
        } finally {
            jdbc.execute("drop trigger falla_version");
        }
        assertThat(jdbc.queryForObject("select count(*) from cat_core", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("select count(*) from cat_version_core", Integer.class)).isZero();
    }

    @Test
    void losMotivosSonLosTextosFijados() {
        var e = invalido("Mal_Nombre", "2.1", "sin-digest");
        var motivos = e.error().detalles().stream()
                .collect(java.util.stream.Collectors.toMap(DetalleError::campo, DetalleError::motivo));
        assertThat(motivos.get("nombre")).isEqualTo(MotivosCatalogo.NOMBRE_INVALIDO)
                .isEqualTo("El nombre debe llevar solo minúsculas, dígitos y guiones medios, "
                        + "sin guion al principio ni al final");
        assertThat(motivos.get("imagen")).isEqualTo(MotivosCatalogo.IMAGEN_INVALIDA)
                .isEqualTo("La imagen debe tener la forma registro/repo[:tag]@sha256:<64 hex en minúsculas>, "
                        + "de hasta 255 caracteres");
        assertThat(motivos.get("version")).isEqualTo(
                "La versión '2.1' no tiene la forma MAYOR.MENOR.PARCHE (por ejemplo 2.1.0)");

        api.registrarCore("synkra-core", "1.0.0", IMAGEN);
        var conflicto = falla(() -> api.registrarCore("synkra-core", "1.0.0", IMAGEN));
        assertThat(conflicto.error().detalles()).containsExactly(
                new DetalleError("version", "La versión 1.0.0 del core ya está registrada"));
    }
}
