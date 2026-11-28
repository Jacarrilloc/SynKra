package io.synkra.kubernetes;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import io.synkra.kubernetes.api.AdaptadorKubernetes;
import io.synkra.kubernetes.api.ObjetosPais;
import io.synkra.kubernetes.api.PlanPais;
import io.synkra.kubernetes.api.PlanPais.PluginPlan;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * U05-T09 (F-01): lo que U07 verá de {@link ObjetosPais} es solo texto; se lee de vuelta desde la interfaz, sin
 * pasar por los tipos de Fabric8, y se compara con el YAML esperado de {@code ConstruirObjetosPaisTest}.
 */
class YamlObjetosPaisTest {

    private static final String IMAGEN = "localhost:5001/core-pe@sha256:" + "a".repeat(64);
    private static final ObjectMapper YAML = new YAMLMapper();

    private static ObjetosPais construye() {
        AdaptadorKubernetes adaptador = new AdaptadorFabric8(null);
        return adaptador.construir(new PlanPais("pe", IMAGEN, List.of(
                new PluginPlan("impuestos", "2.0.1", "2".repeat(64)),
                new PluginPlan("facturacion", "1.2.0", "1".repeat(64)))));
    }

    private static List<JsonNode> esperados() throws Exception {
        try (InputStream in = YamlObjetosPaisTest.class.getResourceAsStream("/kubernetes/pais-pe-esperado.yaml")) {
            List<JsonNode> r = new ArrayList<>();
            YAML.readerFor(JsonNode.class).<JsonNode>readValues(in).forEachRemaining(r::add);
            return r;
        }
    }

    @Test
    void cadaYamlTraeSuKindNombreYNamespace() throws Exception {
        ObjetosPais o = construye();
        JsonNode cm = YAML.readTree(o.yamlConfigMap());
        JsonNode svc = YAML.readTree(o.yamlService());
        JsonNode dep = YAML.readTree(o.yamlDeployment());
        assertThat(cm.at("/kind").asText()).isEqualTo("ConfigMap");
        assertThat(svc.at("/kind").asText()).isEqualTo("Service");
        assertThat(dep.at("/kind").asText()).isEqualTo("Deployment");
        assertThat(cm.at("/metadata/name").asText()).isEqualTo(ConstructorObjetosPais.NOMBRE_CONFIG);
        assertThat(svc.at("/metadata/name").asText()).isEqualTo(ConstructorObjetosPais.NOMBRE_CORE);
        assertThat(dep.at("/metadata/name").asText()).isEqualTo(ConstructorObjetosPais.NOMBRE_CORE);
        for (JsonNode n : List.of(cm, svc, dep)) {
            assertThat(n.at("/metadata/namespace").asText()).isEqualTo("synkra-pe");
        }
        assertThat(dep.at("/spec/template/spec/containers/0/image").asText()).isEqualTo(IMAGEN);
        assertThat(dep.at("/spec/template/spec/initContainers/0/image").asText())
                .isEqualTo(ConstructorObjetosPais.IMAGEN_INIT_POR_DEFECTO);
    }

    @Test
    void losTresYamlCoincidenConElArbolEsperado() throws Exception {
        ObjetosPais o = construye();
        assertThat(List.of(YAML.readTree(o.yamlConfigMap()), YAML.readTree(o.yamlService()), YAML.readTree(o.yamlDeployment())))
                .containsExactlyElementsOf(esperados());
    }
}
