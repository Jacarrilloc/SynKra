package io.synkra.kubernetes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;
import io.fabric8.mockwebserver.http.RecordedRequest;
import io.synkra.kubernetes.api.AplicacionFallidaException;
import io.synkra.kubernetes.api.NamespaceNoPermitidoException;
import io.synkra.kubernetes.api.ObjetosPais;
import io.synkra.kubernetes.api.PlanPais;
import io.synkra.kubernetes.api.PlanPais.PluginPlan;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * U05-T04: aplicar con server-side apply contra el servidor simulado de Fabric8 en modo de expectativas (no CRUD: ver {@link #arranca()}). Nunca se toca el
 * kubeconfig real: {@code user.home} apunta a un directorio temporal y el cliente sale del servidor simulado.
 */
class AplicarTest {

    private static final String IMAGEN = "localhost:5001/core-pe@sha256:" + "a".repeat(64);

    private static final ObjectMapper YAML = new YAMLMapper();

    @TempDir
    Path casaFalsa;

    KubernetesMockServer servidor;
    KubernetesClient cliente;
    AdaptadorFabric8 adaptador;
    private int leidas;
    private String propiedadCasa;
    private String propiedadKubeconfig;

    @BeforeEach
    void arranca() {
        propiedadCasa = System.getProperty("user.home");
        propiedadKubeconfig = System.getProperty("kubeconfig");
        System.setProperty("user.home", casaFalsa.toString());
        System.setProperty("kubeconfig", casaFalsa.resolve("kubeconfig-falso").toString());
        // El modo CRUD del simulado responde 404 a un PATCH de apply sobre un objeto que no existe (un
        // clúster real lo crea) y no rellena managedFields; por eso se registran las peticiones y se
        // lee de vuelta el PATCH. Ver la bitácora.
        servidor = new KubernetesMockServer(false);
        servidor.init();
        servidor.expect().patch().withPath(ruta("/api/v1", "configmaps", ConstructorObjetosPais.NOMBRE_CONFIG))
                .andReturn(200, "{}").always();
        servidor.expect().patch().withPath(ruta("/api/v1", "services", ConstructorObjetosPais.NOMBRE_CORE))
                .andReturn(200, "{}").always();
        servidor.expect().patch().withPath(ruta("/apis/apps/v1", "deployments", ConstructorObjetosPais.NOMBRE_CORE))
                .andReturn(200, "{}").always();
        cliente = servidor.createClient();
        adaptador = new AdaptadorFabric8(cliente);
    }

    @AfterEach
    void para() {
        cliente.close();
        servidor.destroy();
        restaura("user.home", propiedadCasa);
        restaura("kubeconfig", propiedadKubeconfig);
    }

    private static void restaura(String clave, String valor) {
        if (valor == null) {
            System.clearProperty(clave);
        } else {
            System.setProperty(clave, valor);
        }
    }

    private static PlanPais plan(String plugin, String version, String imagen) {
        return new PlanPais("pe", imagen, List.of(new PluginPlan(plugin, version, "1".repeat(64))));
    }

    /** Las peticiones nuevas desde la última lectura: exactamente las que cuenta el servidor, sin cortar por tiempo. */
    private List<RecordedRequest> peticiones() throws Exception {
        List<RecordedRequest> r = new ArrayList<>();
        while (leidas < servidor.getRequestCount()) {
            r.add(servidor.takeRequest(5, TimeUnit.SECONDS));
            leidas++;
        }
        return r;
    }

    /** Cuerpo del PATCH, leído de vuelta como árbol. */
    private static JsonNode cuerpo(RecordedRequest r) throws Exception {
        return YAML.readTree(r.getUtf8Body());
    }

    private static List<RecordedRequest> patches(List<RecordedRequest> reqs) {
        return reqs.stream().filter(r -> r.getMethod().equals("PATCH")).toList();
    }

    @Test
    void losTresObjetosSeMandanConSuFieldManager() throws Exception {
        var objetos = adaptador.construir(plan("facturacion", "1.2.0", IMAGEN));
        adaptador.aplicar(objetos);
        var patches = patches(peticiones());
        assertThat(patches).hasSize(3);
        assertThat(patches).allSatisfy(r -> {
            assertThat(r.getHeader("Content-Type")).startsWith("application/apply-patch+yaml");
            assertThat(r.getPath()).contains("fieldManager=" + ConstructorObjetosPais.GESTOR).contains("force=true");
        });
        // El cuerpo enviado es exactamente el objeto construido, en synkra-pe.
        assertThat(cuerpo(patches.get(0))).isEqualTo(YAML.readTree(Serialization.asYaml(objetos.configMap())));
        assertThat(cuerpo(patches.get(1))).isEqualTo(YAML.readTree(Serialization.asYaml(objetos.service())));
        assertThat(cuerpo(patches.get(2))).isEqualTo(YAML.readTree(Serialization.asYaml(objetos.deployment())));
        // El cuerpo ya viene en synkra-pe (se compara contra el objeto construido, que lleva el namespace).
        assertThat(objetos.deployment().getMetadata().getNamespace()).isEqualTo("synkra-pe");
    }

    @Test
    void aplicarDosVecesElMismoPlanMandaLoMismo() throws Exception {
        var objetos = adaptador.construir(plan("facturacion", "1.2.0", IMAGEN));
        adaptador.aplicar(objetos);
        var primera = patches(peticiones());
        adaptador.aplicar(objetos);
        var segunda = patches(peticiones());
        assertThat(primera).hasSize(3);
        assertThat(segunda).hasSize(3);
        for (int i = 0; i < 3; i++) {
            assertThat(segunda.get(i).getPath()).isEqualTo(primera.get(i).getPath());
            assertThat(cuerpo(segunda.get(i))).isEqualTo(cuerpo(primera.get(i)));
        }
    }

    @Test
    void otroPlanCambiaElConfigMapYLaImagenDelDeployment() throws Exception {
        adaptador.aplicar(adaptador.construir(plan("facturacion", "1.2.0", IMAGEN)));
        var antes = patches(peticiones());
        String nueva = "localhost:5001/core-pe@sha256:" + "b".repeat(64);
        adaptador.aplicar(adaptador.construir(plan("impuestos", "2.0.1", nueva)));
        var despues = patches(peticiones());
        JsonNode cm = cuerpo(despues.get(0));
        assertThat(cm.at("/data/plugins-activos.yaml").asText()).contains("impuestos").doesNotContain("facturacion");
        assertThat(cm.at("/data/artefactos.txt").asText()).contains("impuestos").doesNotContain("facturacion");
        assertThat(cm).isNotEqualTo(cuerpo(antes.get(0)));
        assertThat(cuerpo(despues.get(2)).at("/spec/template/spec/containers/0/image").asText()).isEqualTo(nueva);
        assertThat(cuerpo(antes.get(2)).at("/spec/template/spec/containers/0/image").asText()).isEqualTo(IMAGEN);
    }

    @Test
    void objetosConNamespacesDistintosSeRechazanSinPeticiones() {
        var o = adaptador.construir(plan("facturacion", "1.2.0", IMAGEN));
        o.service().getMetadata().setNamespace("synkra-co");
        int antes = servidor.getRequestCount();
        assertThatThrownBy(() -> adaptador.aplicar(o)).isInstanceOf(IllegalArgumentException.class);
        assertThat(servidor.getRequestCount()).isEqualTo(antes);
    }

    @Test
    void unNamespaceFueraDeLaGuardiaSeRechazaSinPeticiones() {
        var o = adaptador.construir(plan("facturacion", "1.2.0", IMAGEN));
        o.configMap().getMetadata().setNamespace("kube-system");
        o.service().getMetadata().setNamespace("kube-system");
        o.deployment().getMetadata().setNamespace("kube-system");
        int antes = servidor.getRequestCount();
        assertThatThrownBy(() -> adaptador.aplicar(o)).isInstanceOf(NamespaceNoPermitidoException.class);
        assertThatThrownBy(() -> adaptador.aplicar(null)).isInstanceOf(NullPointerException.class);
        assertThat(servidor.getRequestCount()).isEqualTo(antes);
    }

    @Test
    void soloHayPatchEnElOrdenConfigMapServiceDeployment() throws Exception {
        adaptador.aplicar(adaptador.construir(plan("facturacion", "1.2.0", IMAGEN)));
        var reqs = peticiones();
        assertThat(reqs).isNotEmpty();
        assertThat(reqs).allSatisfy(r -> {
            assertThat(r.getMethod()).isIn("PATCH", "GET").isNotIn("DELETE", "POST");
            assertThat(r.getPath()).contains("/namespaces/synkra-pe/");
        });
        var patches = reqs.stream().filter(r -> r.getMethod().equals("PATCH")).toList();
        assertThat(patches).hasSize(3);
        assertThat(patches).allSatisfy(r -> {
            assertThat(r.getHeader("Content-Type")).startsWith("application/apply-patch+yaml");
            assertThat(r.getPath()).contains("fieldManager=" + ConstructorObjetosPais.GESTOR).contains("force=true");
        });
        assertThat(patches.get(0).getPath()).contains("/configmaps/");
        assertThat(patches.get(1).getPath()).contains("/services/");
        assertThat(patches.get(2).getPath()).contains("/deployments/");
    }

    @Test
    void siFallaElSegundoNombraElObjetoYElPrimeroSeQueda() throws Exception {
        servidor.destroy();
        servidor = new KubernetesMockServer(false);
        servidor.init();
        leidas = 0;
        cliente.close();
        cliente = servidor.createClient();
        var adaptadorSimple = new AdaptadorFabric8(cliente);
        var o = adaptadorSimple.construir(plan("facturacion", "1.2.0", IMAGEN));
        servidor.expect().patch().withPath(rutaDe("configmaps", o.configMap().getMetadata().getName()))
                .andReturn(200, o.configMap()).once();
        servidor.expect().patch().withPath(rutaDe("services", o.service().getMetadata().getName()))
                .andReturn(403, "{\"kind\":\"Status\",\"apiVersion\":\"v1\",\"code\":403}").once();
        assertThatThrownBy(() -> adaptadorSimple.aplicar(o))
                .isInstanceOf(AplicacionFallidaException.class)
                .hasMessageContaining("Service")
                .hasMessageContaining(o.service().getMetadata().getName());
        // Exactamente dos PATCH y nada más: ni DELETE que deshaga el ConfigMap, ni petición al Deployment.
        var reqs = peticiones();
        assertThat(reqs).extracting(RecordedRequest::getMethod).containsExactly("PATCH", "PATCH");
        assertThat(reqs.get(0).getPath()).contains("/configmaps/");
        assertThat(reqs.get(1).getPath()).contains("/services/");
    }

    @Test
    void sinClienteAplicarFallaConErrorPropioYSinAcusarALaApi() {
        var sinCliente = new AdaptadorFabric8(null);
        var o = sinCliente.construir(plan("facturacion", "1.2.0", IMAGEN));
        assertThat(sinCliente.disponible()).isFalse();
        assertThatThrownBy(() -> sinCliente.aplicar(o))
                .isInstanceOf(IllegalStateException.class)
                .isNotInstanceOf(AplicacionFallidaException.class)
                .hasMessageContaining("cuenta de servicio");
    }

    private static String rutaDe(String recurso, String nombre) {
        String base = recurso.equals("deployments") ? "/apis/apps/v1" : "/api/v1";
        return ruta(base, recurso, nombre);
    }

    private static String ruta(String base, String recurso, String nombre) {
        return base + "/namespaces/synkra-pe/" + recurso + "/" + nombre + "?fieldManager=" + ConstructorObjetosPais.GESTOR + "&force=true";
    }
}
