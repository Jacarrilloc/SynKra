package io.synkra.despliegue;

import static io.synkra.despliegue.HttpPlanes.detalle;
import static io.synkra.despliegue.HttpPlanes.esperaError;
import static org.assertj.core.api.Assertions.assertThat;

import io.synkra.catalogo.api.CatalogoApi;
import io.synkra.kubernetes.SimuladorKubernetes;
import io.synkra.kubernetes.api.AdaptadorKubernetes;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.convention.TestBean;

/**
 * U07-T11, P4: un plugin de la carpeta de otro core no llega al clúster, ni al crear el plan ni al confirmarlo.
 * Contra el catálogo real y el servidor simulado de Fabric8.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CarpetaEquivocadaPlanTest {

    static final Path DATOS = Path.of("build/sqlite-pruebas/CarpetaEquivocadaPlanTest");
    static final SimuladorKubernetes SIM = new SimuladorKubernetes();

    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry r) throws IOException {
        if (Files.exists(DATOS)) {
            try (var s = Files.walk(DATOS)) {
                s.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
            }
        }
        Files.createDirectories(DATOS);
        r.add("synkra.data", () -> DATOS.toAbsolutePath().toString());
        r.add("synkra.paises", () -> "pe,co");
    }

    @TestBean AdaptadorKubernetes adaptador;

    static AdaptadorKubernetes adaptador() {
        return SIM.adaptador();
    }

    @AfterAll
    static void cierra() {
        SIM.close();
    }

    @Autowired CatalogoApi catalogo;
    @Autowired TestRestTemplate rest;
    @Autowired JdbcTemplate jdbc;

    private static boolean cargada;

    private synchronized void cargar() throws IOException {
        if (!cargada) {
            CatalogoDePrueba.cargar(catalogo);
            cargada = true;
        }
    }

    private long planes() {
        return jdbc.queryForObject("select count(*) from dep_plan", Long.class);
    }

    @SuppressWarnings("unchecked")
    private String mensajeDeVerificacion(String core, String versionCore, String plugin, String codigo) {
        var v = new HttpPlanes(rest).verificar(core, versionCore, plugin);
        assertThat(v.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(v.getBody()).containsEntry("global", "RECHAZADA");
        var plugins = (List<Map<String, Object>>) v.getBody().get("plugins");
        var motivos = (List<Map<String, Object>>) plugins.get(0).get("motivos");
        assertThat(motivos).hasSize(1);
        assertThat(motivos.get(0)).containsEntry("codigo", codigo);
        return (String) motivos.get(0).get("mensaje");
    }

    @Test
    void crearConUnPluginDeOtraCarpetaDa422SinFilaNiPeticiones() throws Exception {
        cargar();
        SIM.nuevasPeticiones();
        var antes = planes();

        var r = new HttpPlanes(rest).crear("pe", "otro-core", "1.0.0", null, "kyc-pe:1.3.0");

        var motivo = mensajeDeVerificacion("otro-core", "1.0.0", "kyc-pe:1.3.0", "CORE_EQUIVOCADO");
        esperaError(r, HttpStatus.UNPROCESSABLE_ENTITY, "COMBINACION_RECHAZADA", "La combinación no es compatible",
                detalle("plugins.kyc-pe", motivo));
        assertThat(SIM.nuevasPeticiones()).isEmpty();
        assertThat(planes()).isEqualTo(antes);
    }

    @Test
    void crearAlReves() throws Exception {
        cargar();
        SIM.nuevasPeticiones();
        var antes = planes();

        var r = new HttpPlanes(rest).crear("pe", "synkra-core", "2.1.0", null, "kyc-xx:1.0.0");

        var motivo = mensajeDeVerificacion("synkra-core", "2.1.0", "kyc-xx:1.0.0", "CORE_EQUIVOCADO");
        esperaError(r, HttpStatus.UNPROCESSABLE_ENTITY, "COMBINACION_RECHAZADA", "La combinación no es compatible",
                detalle("plugins.kyc-xx", motivo));
        assertThat(SIM.nuevasPeticiones()).isEmpty();
        assertThat(planes()).isEqualTo(antes);
    }

    @Test
    void confirmarUnPlanManipuladoConUnPluginDeOtraCarpetaDa422() throws Exception {
        cargar();
        var sha = catalogo.obtenerPlugin("synkra-core", "kyc-pe", "1.3.0").sha256();
        var ahora = Instant.now().toEpochMilli();
        var resumen = "{\"namespace\":\"synkra-co\",\"core\":\"otro-core\",\"versionCore\":\"1.0.0\",\"imagen\":\""
                + CatalogoDePrueba.IMAGEN_C + "\",\"plugins\":[{\"nombre\":\"kyc-pe\",\"version\":\"1.3.0\","
                + "\"sha256\":\"" + sha + "\"}]}";
        jdbc.update("insert into dep_plan (pais, tipo, origen_id, core, version_core, plugins, resumen, cambios,"
                + " estado, creado_por, creado_en, vence_en) values ('co', 'despliegue', null, 'otro-core', '1.0.0', ?,"
                + " ?, '{}', 'pendiente', 'ana', ?, ?)", "[{\"nombre\":\"kyc-pe\",\"version\":\"1.3.0\"}]", resumen,
                ahora, ahora + 30 * 60_000L);
        var id = jdbc.queryForObject("select max(id) from dep_plan", Long.class);
        var motivo = mensajeDeVerificacion("otro-core", "1.0.0", "kyc-pe:1.3.0", "CORE_EQUIVOCADO");
        SIM.nuevasPeticiones();

        var r = new HttpPlanes(rest).confirmar(id, "luis");

        esperaError(r, HttpStatus.UNPROCESSABLE_ENTITY, "COMBINACION_RECHAZADA", "La combinación no es compatible",
                detalle("plugins.kyc-pe", motivo));
        assertThat(SIM.nuevasPeticiones()).isEmpty();
        assertThat(jdbc.queryForObject("select estado from dep_plan where id = ?", String.class, id))
                .isEqualTo("pendiente");
        assertThat(jdbc.queryForObject("select count(*) from reg_despliegue", Long.class)).isZero();
    }
}
