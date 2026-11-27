package io.synkra.kubernetes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.synkra.kubernetes.api.NamespaceNoPermitidoException;
import io.synkra.kubernetes.api.ObjetosPais;
import io.synkra.kubernetes.api.PlanPais;
import io.synkra.kubernetes.api.PlanPais.PluginPlan;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** U05-T03: construir no habla con la API; el YAML esperado está escrito a mano a partir de U08. */
class ConstruirObjetosPaisTest {

    private static final String DIGEST = "a".repeat(64);
    private static final String IMAGEN = "localhost:5001/core-pe@sha256:" + DIGEST;
    private static final ObjectMapper YAML = new YAMLMapper();

    @TempDir
    Path casaFalsa;

    KubernetesMockServer servidor;
    KubernetesClient cliente;
    AdaptadorFabric8 adaptador;
    private String propiedadCasa;

    @BeforeEach
    void arranca() {
        propiedadCasa = System.getProperty("user.home");
        System.setProperty("user.home", casaFalsa.toString());
        servidor = new KubernetesMockServer(true);
        servidor.init();
        cliente = servidor.createClient();
        adaptador = new AdaptadorFabric8(cliente);
    }

    @AfterEach
    void para() {
        cliente.close();
        servidor.destroy();
        if (propiedadCasa == null) {
            System.clearProperty("user.home");
        } else {
            System.setProperty("user.home", propiedadCasa);
        }
    }

    private static PlanPais planPe() {
        // Fuera de orden a propósito: el ConfigMap debe salir ordenado por nombre.
        return new PlanPais("pe", IMAGEN, List.of(
                new PluginPlan("impuestos", "2.0.1", "2".repeat(64)),
                new PluginPlan("facturacion", "1.2.0", "1".repeat(64))));
    }

    private static List<JsonNode> comoArboles(ObjetosPais o) throws Exception {
        List<JsonNode> r = new ArrayList<>();
        for (Object obj : List.of(o.configMap(), o.service(), o.deployment())) {
            r.add(YAML.readTree(Serialization.asYaml(obj)));
        }
        return r;
    }

    private static List<JsonNode> esperados() throws Exception {
        try (InputStream in = ConstruirObjetosPaisTest.class.getResourceAsStream("/kubernetes/pais-pe-esperado.yaml")) {
            List<JsonNode> r = new ArrayList<>();
            YAML.readerFor(JsonNode.class).<JsonNode>readValues(in).forEachRemaining(r::add);
            return r;
        }
    }

    @Test
    void coincideConElYamlEsperadoDeSynkraPe() throws Exception {
        var objetos = adaptador.construir(planPe());
        assertThat(comoArboles(objetos)).containsExactlyElementsOf(esperados());
    }

    @Test
    void paisSinPluginsDejaArtefactosVacio() {
        var objetos = adaptador.construir(new PlanPais("co", IMAGEN, List.of()));
        assertThat(objetos.configMap().getMetadata().getNamespace()).isEqualTo("synkra-co");
        assertThat(objetos.configMap().getData()).containsEntry("artefactos.txt", "")
                .containsEntry("plugins-activos.yaml", "plugins: []\n");
    }

    @Test
    void planRechazaImagenSinDigest() {
        assertThatThrownBy(() -> new PlanPais("pe", "localhost:5001/core-pe:latest", List.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PlanPais("pe", "core@sha256:abc", List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void planRechazaSha256InvalidoYPaisInvalido() {
        assertThatThrownBy(() -> new PlanPais("pe", IMAGEN, List.of(new PluginPlan("x", "1", "A".repeat(64)))))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PlanPais("pe", IMAGEN, List.of(new PluginPlan("x", "1", "a".repeat(63)))))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PlanPais("PE", IMAGEN, List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void laGuardiaRechazaUnPaisInvalidoSinPeticiones() {
        // PlanPais ya garantiza [a-z]{2}, así que con un plan válido la guardia es inalcanzable desde construir:
        // por eso se prueba la guardia que construir invoca (namespaceDe), no un plan con país malo.
        int antes = servidor.getRequestCount();
        assertThatThrownBy(() -> AdaptadorFabric8.namespaceDe("pe/../x"))
                .isInstanceOf(NamespaceNoPermitidoException.class);
        assertThatThrownBy(() -> adaptador.construir(null)).isInstanceOf(NullPointerException.class);
        assertThat(servidor.getRequestCount()).isEqualTo(antes);
    }

    @Test
    void construirNoHaceNingunaPeticion() {
        int antes = servidor.getRequestCount();
        adaptador.construir(planPe());
        assertThat(servidor.getRequestCount()).isEqualTo(antes).isZero();
    }

    @Test
    void laImagenDelCoreEsExactamenteLaDelPlan() {
        var core = adaptador.construir(planPe()).deployment().getSpec().getTemplate().getSpec().getContainers().get(0);
        assertThat(core.getImage()).isEqualTo(IMAGEN);
    }

    @ParameterizedTest
    @ValueSource(strings = {"../x", "a/b", "a\nb", "a b", "a: b", ""})
    void planRechazaNombresDePluginPeligrosos(String nombre) {
        assertThatThrownBy(() -> new PluginPlan(nombre, "1.0.0", "1".repeat(64)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"1 0", "1/0", "../1", "1\n2", ""})
    void planRechazaVersionesPeligrosas(String version) {
        assertThatThrownBy(() -> new PluginPlan("x", version, "1".repeat(64)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void planAceptaSemverConPrereleaseYBuild() {
        assertThat(new PluginPlan("facturacion", "1.0.0-rc.1+b", "1".repeat(64)).version()).isEqualTo("1.0.0-rc.1+b");
    }

    @ParameterizedTest
    @ValueSource(strings = {"synkra-init-descarga", "synkra-init-descarga:latest", "localhost:5001/synkra-init-descarga",
            "localhost:5001/synkra-init-descarga:latest", "localhost:5001/x@sha256:abc", "init:1.0.0", " ", ""})
    void rechazaImagenDeInitSinRegistroOSinTagFijo(String imagen) {
        assertThatThrownBy(() -> new AdaptadorFabric8(cliente, imagen)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aceptaImagenDeInitConTagODigest() {
        assertThat(new AdaptadorFabric8(cliente, "localhost:5001/synkra-init-descarga:1.0.0")).isNotNull();
        assertThat(new AdaptadorFabric8(cliente, "registro.local/synkra/init@sha256:" + DIGEST)).isNotNull();
    }

    @Test
    void usaLaImagenDeInitConfigurada() {
        var a = new AdaptadorFabric8(cliente, "registro.local:5000/init:2.3.4");
        var init = a.construir(planPe()).deployment().getSpec().getTemplate().getSpec().getInitContainers().get(0);
        assertThat(init.getImage()).isEqualTo("registro.local:5000/init:2.3.4");
        assertThat(init.getImagePullPolicy()).isEqualTo("IfNotPresent");
    }
}
