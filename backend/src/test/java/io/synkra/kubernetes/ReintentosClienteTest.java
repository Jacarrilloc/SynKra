package io.synkra.kubernetes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;
import io.synkra.kubernetes.api.AdaptadorKubernetes;
import io.synkra.kubernetes.api.AplicacionFallidaException;
import io.synkra.kubernetes.api.LecturaFallidaException;
import io.synkra.kubernetes.api.PlanPais;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * U07-E, C-76: el cliente que arma {@code ConfiguracionKubernetes} (cuenta de prueba, servidor simulado en TLS)
 * no reintenta solo ante un 5xx: una lectura o una escritura que falla es una sola petición.
 */
class ReintentosClienteTest {

    private static final String ERROR = "{\"kind\":\"Status\",\"code\":500}";
    private static final String RUTA_DEPLOYMENT = "/apis/apps/v1/namespaces/synkra-pe/deployments/synkra-core";
    private static final String RUTA_CONFIGMAP = "/api/v1/namespaces/synkra-pe/configmaps/synkra-core-config"
            + "?fieldManager=synkra&force=true";

    @TempDir
    Path cuenta;

    @TempDir
    Path casaFalsa;

    KubernetesMockServer servidor;
    private String propiedadCasa;

    @BeforeEach
    void arranca() throws Exception {
        propiedadCasa = System.getProperty("user.home");
        System.setProperty("user.home", casaFalsa.toString());
        servidor = new KubernetesMockServer(true);
        servidor.init();
        escribeCuenta();
    }

    @AfterEach
    void para() {
        servidor.destroy();
        if (propiedadCasa == null) {
            System.clearProperty("user.home");
        } else {
            System.setProperty("user.home", propiedadCasa);
        }
    }

    /** Token y la CA que presenta el servidor simulado, como en {@code ClienteKubernetesTest}. */
    private void escribeCuenta() throws Exception {
        Files.writeString(cuenta.resolve("token"), "t\n");
        var confiaEnTodo = new X509TrustManager() {
            public void checkClientTrusted(X509Certificate[] c, String t) {}
            public void checkServerTrusted(X509Certificate[] c, String t) {}
            public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
        };
        var ssl = SSLContext.getInstance("TLS");
        ssl.init(null, new TrustManager[] {confiaEnTodo}, null);
        var pem = new StringBuilder();
        try (var socket = (SSLSocket) ssl.getSocketFactory().createSocket(servidor.getHostName(), servidor.getPort())) {
            socket.startHandshake();
            for (var c : socket.getSession().getPeerCertificates()) {
                pem.append("-----BEGIN CERTIFICATE-----\n")
                        .append(Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(c.getEncoded()))
                        .append("\n-----END CERTIFICATE-----\n");
            }
        }
        Files.writeString(cuenta.resolve("ca.crt"), pem.toString());
    }

    private void conAdaptador(java.util.function.Consumer<AdaptadorKubernetes> prueba) {
        new ApplicationContextRunner().withUserConfiguration(ConfiguracionKubernetes.class)
                .withPropertyValues("synkra.kubernetes.cuenta=" + cuenta)
                .withBean(VariablesPod.class, () -> new VariablesPod(Map.of(
                        "KUBERNETES_SERVICE_HOST", servidor.getHostName(),
                        "KUBERNETES_SERVICE_PORT", String.valueOf(servidor.getPort()))))
                .run(ctx -> prueba.accept(ctx.getBean(AdaptadorKubernetes.class)));
    }

    @Test
    void elClienteNoTieneReintentosImplicitos() {
        conAdaptador(a -> assertThat(((AdaptadorFabric8) a).cliente().getConfiguration().getRequestRetryBackoffLimit())
                .isZero());
    }

    @Test
    void unGetQueDa500EsUnaSolaPeticion() throws Exception {
        servidor.expect().get().withPath(RUTA_DEPLOYMENT).andReturn(500, ERROR).always();
        conAdaptador(a -> assertThatThrownBy(() -> a.estado("synkra-pe")).isInstanceOf(LecturaFallidaException.class));
        assertThat(servidor.getRequestCount()).isEqualTo(1);
        var p = servidor.takeRequest();
        assertThat(p.getMethod() + " " + p.getPath()).isEqualTo("GET " + RUTA_DEPLOYMENT);
    }

    @Test
    void unPatchQueDa500EsUnaSolaPeticion() throws Exception {
        servidor.expect().patch().withPath(RUTA_CONFIGMAP).andReturn(500, ERROR).always();
        var plan = new PlanPais("pe", "localhost:5001/synkra-core@sha256:" + "a".repeat(64), List.of());
        conAdaptador(a -> assertThatThrownBy(() -> a.aplicar(a.construir(plan)))
                .isInstanceOf(AplicacionFallidaException.class));
        assertThat(servidor.getRequestCount()).isEqualTo(1);
        var p = servidor.takeRequest();
        assertThat(p.getMethod() + " " + p.getPath()).isEqualTo("PATCH " + RUTA_CONFIGMAP);
    }
}
