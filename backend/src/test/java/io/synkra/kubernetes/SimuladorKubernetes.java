package io.synkra.kubernetes;

import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.kubernetes.client.server.mock.KubernetesMixedDispatcher;
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;
import io.fabric8.mockwebserver.Context;
import io.fabric8.mockwebserver.MockWebServer;
import io.fabric8.mockwebserver.ServerRequest;
import io.fabric8.mockwebserver.ServerResponse;
import io.synkra.kubernetes.api.AdaptadorKubernetes;
import io.synkra.kubernetes.api.PlanPais;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.TimeUnit;

/**
 * U07-B: el servidor simulado de Fabric8 (modo CRUD) con un {@link AdaptadorKubernetes} real encima, para que las
 * pruebas de otros módulos lean lo que "corre" sin un clúster. Es de este paquete porque {@code AdaptadorFabric8} es
 * privado de paquete; su API pública no menciona ningún tipo de Fabric8. Fija una casa y un kubeconfig falsos mientras
 * vive (nunca se toca el kubeconfig real) y los restaura al cerrar.
 */
public final class SimuladorKubernetes implements AutoCloseable {

    private final KubernetesMockServer servidor;
    private final KubernetesClient cliente;
    private final AdaptadorFabric8 adaptador;
    private final Path casa;
    private final String propiedadCasa;
    private final String propiedadKubeconfig;
    private int leidas;

    public SimuladorKubernetes() {
        try {
            casa = Files.createTempDirectory("simulador-k8s");
        } catch (java.io.IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
        propiedadCasa = System.getProperty("user.home");
        propiedadKubeconfig = System.getProperty("kubeconfig");
        System.setProperty("user.home", casa.toString());
        System.setProperty("kubeconfig", casa.resolve("kubeconfig-falso").toString());
        var respuestas = new HashMap<ServerRequest, Queue<ServerResponse>>();
        servidor = new KubernetesMockServer(new Context(), new MockWebServer(), respuestas,
                new KubernetesMixedDispatcher(respuestas), false);
        servidor.init();
        try (var base = servidor.createClient()) {
            cliente = new KubernetesClientBuilder().withConfig(
                    new ConfigBuilder(base.getConfiguration()).withRequestRetryBackoffLimit(0).build()).build();
        }
        adaptador = new AdaptadorFabric8(cliente);
    }

    /** Un adaptador real, hablando con el servidor simulado. */
    public AdaptadorKubernetes adaptador() {
        return adaptador;
    }

    /** Deployment y ConfigMap del país, tal como los arma {@code construir}. */
    public void siembra(PlanPais plan) {
        var ns = "synkra-" + plan.pais();
        var o = adaptador.construir(plan);
        cliente.apps().deployments().inNamespace(ns).resource(o.deployment()).create();
        cliente.configMaps().inNamespace(ns).resource(o.configMap()).create();
        drena();
    }

    /** Solo el Deployment (el país queda inconsistente para {@code leerPais}). */
    public void siembraSoloDeployment(PlanPais plan) {
        var o = adaptador.construir(plan);
        cliente.apps().deployments().inNamespace("synkra-" + plan.pais()).resource(o.deployment()).create();
        drena();
    }

    /** El {@code GET} del Deployment de ese país responde 500. */
    public void fallaLecturas(String pais) {
        servidor.expect().get()
                .withPath("/apis/apps/v1/namespaces/synkra-" + pais + "/deployments/" + ConstructorObjetosPais.NOMBRE_CORE)
                .andReturn(500, "{\"kind\":\"Status\",\"code\":500}").always();
    }

    /** El {@code PATCH} del Deployment {@code synkra-core} de ese país (el de aplicar) responde 500 siempre. */
    public void fallaAplicar(String pais) {
        servidor.expect().patch()
                .withPath("/apis/apps/v1/namespaces/synkra-" + pais + "/deployments/" + ConstructorObjetosPais.NOMBRE_CORE
                        + "?fieldManager=" + ConstructorObjetosPais.GESTOR + "&force=true")
                .andReturn(500, "{\"kind\":\"Status\",\"code\":500}").always();
    }

    /** {@code "MÉTODO ruta"} de lo recibido desde la llamada anterior (la siembra no cuenta). */
    public List<String> nuevasPeticiones() {
        var r = new ArrayList<String>();
        try {
            while (leidas < servidor.getRequestCount()) {
                var p = servidor.takeRequest(5, TimeUnit.SECONDS);
                leidas++;
                r.add(p.getMethod() + " " + p.getPath());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
        return r;
    }

    private void drena() {
        nuevasPeticiones();
    }

    @Override
    public void close() {
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
}
