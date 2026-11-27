package io.synkra.kubernetes;

import static org.assertj.core.api.Assertions.assertThat;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;
import io.fabric8.mockwebserver.http.RecordedRequest;
import io.synkra.kubernetes.api.AdaptadorKubernetes;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.security.cert.X509Certificate;
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
 * U05-T01: el cliente sale solo de la cuenta del pod; solo se usa el servidor simulado de Fabric8,
 * en TLS, con su certificado como {@code ca.crt}. Nunca se toca el kubeconfig real: los kubeconfig
 * de estas pruebas son archivos falsos en un {@code @TempDir}.
 */
class ClienteKubernetesTest {

    private static final String VERSION = "{\"major\":\"1\",\"minor\":\"30\",\"gitVersion\":\"v1.30.0\"}";

    @TempDir
    Path cuenta;

    @TempDir
    Path casaFalsa;

    KubernetesMockServer servidor;

    private String propiedadKubeconfig;
    private String propiedadCasa;

    final ApplicationContextRunner runner =
            new ApplicationContextRunner().withUserConfiguration(ConfiguracionKubernetes.class);

    @BeforeEach
    void arrancaServidor() {
        propiedadKubeconfig = System.getProperty("kubeconfig");
        propiedadCasa = System.getProperty("user.home");
        System.setProperty("user.home", casaFalsa.toString());
        servidor = new KubernetesMockServer(true);
        servidor.init();
        servidor.expect().get().withPath("/version").andReturn(200, VERSION).always();
    }

    @AfterEach
    void paraServidor() {
        servidor.destroy();
        restaura("kubeconfig", propiedadKubeconfig);
        restaura("user.home", propiedadCasa);
    }

    private static void restaura(String clave, String valor) {
        if (valor == null) {
            System.clearProperty(clave);
        } else {
            System.setProperty(clave, valor);
        }
    }

    /** Variables del pod simuladas (solo el código de prueba puede registrar este bean). */
    private VariablesPod variablesDelServidor() {
        return new VariablesPod(Map.of(
                "KUBERNETES_SERVICE_HOST", servidor.getHostName(),
                "KUBERNETES_SERVICE_PORT", String.valueOf(servidor.getPort())));
    }

    /** Token y CA (la cadena que presenta el servidor simulado, que no la publica) en la cuenta de prueba. */
    private void escribeCuenta(String token) throws Exception {
        Files.writeString(cuenta.resolve("token"), token + "\n");
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

    private ApplicationContextRunner conCuenta() {
        return runner.withPropertyValues("synkra.kubernetes.cuenta=" + cuenta)
                .withBean(VariablesPod.class, this::variablesDelServidor);
    }

    @Test
    void conTokenYCaDeLaCuentaLaPeticionLlevaElBearerPorTls() throws Exception {
        escribeCuenta("token-del-pod-123");
        conCuenta().run(ctx -> {
            assertThat(ctx).hasSingleBean(KubernetesClient.class);
            assertThat(ctx.getBean(AdaptadorKubernetes.class).disponible()).isTrue();
            var cliente = ctx.getBean(KubernetesClient.class);
            assertThat(cliente.getMasterUrl().toString()).startsWith("https://");
            cliente.getKubernetesVersion();
            RecordedRequest peticion = servidor.getLastRequest();
            assertThat(peticion.getPath()).isEqualTo("/version");
            assertThat(peticion.getHeader("Authorization")).isEqualTo("Bearer token-del-pod-123");
        });
    }

    @Test
    void sinLaCaDeLaCuentaLaConexionTlsFalla() throws Exception {
        escribeCuenta("t");
        Files.delete(cuenta.resolve("ca.crt"));
        conCuenta().run(ctx -> {
            var cliente = ctx.getBean(KubernetesClient.class);
            org.assertj.core.api.Assertions.assertThatThrownBy(cliente::getKubernetesVersion).isNotNull();
        });
    }

    @Test
    void elKubeconfigDeLaMaquinaNoSeUsa() throws Exception {
        escribeCuenta("token-del-pod");
        Path kubeconfig = cuenta.resolve("kubeconfig-ajeno");
        Files.writeString(kubeconfig, """
                apiVersion: v1
                kind: Config
                current-context: ajeno
                clusters:
                - name: ajeno
                  cluster:
                    server: https://127.0.0.1:1
                contexts:
                - name: ajeno
                  context: {cluster: ajeno, user: ajeno, namespace: ns-ajeno}
                users:
                - name: ajeno
                  user: {token: token-ajeno}
                """);
        Files.createDirectories(casaFalsa.resolve(".kube"));
        Files.copy(kubeconfig, casaFalsa.resolve(".kube/config"));
        System.setProperty("kubeconfig", kubeconfig.toString());
        conCuenta().run(ctx -> {
            var cliente = ctx.getBean(KubernetesClient.class);
            var config = cliente.getConfiguration();
            assertThat(config.getFile()).as("archivo kubeconfig").isNull();
            assertThat(config.getCurrentContext()).as("contexto").isNull();
            assertThat(config.getNamespace()).as("namespace").isNull();
            assertThat(config.getMasterUrl()).doesNotContain("127.0.0.1:1");
            cliente.getKubernetesVersion();
            assertThat(servidor.getLastRequest().getHeader("Authorization")).isEqualTo("Bearer token-del-pod");
        });
    }

    @Test
    void laUrlNoSaleDeLasPropiedadesDeSpring() throws Exception {
        escribeCuenta("token-del-pod");
        // Sin variables del pod en el proceso, ni la propiedad ni las claves de Spring dan clúster.
        runner.withPropertyValues(
                        "synkra.kubernetes.cuenta=" + cuenta,
                        "synkra.kubernetes.url=http://127.0.0.1:1",
                        "SYNKRA_KUBERNETES_URL=http://127.0.0.1:1",
                        "KUBERNETES_SERVICE_HOST=127.0.0.1",
                        "KUBERNETES_SERVICE_PORT=1")
                .run(ctx -> {
                    assertThat(ctx).hasNotFailed();
                    assertThat(ctx).doesNotHaveBean(KubernetesClient.class);
                    assertThat(ctx.getBean(AdaptadorKubernetes.class).disponible()).isFalse();
                });
    }

    @Test
    void conVariablesDelPodLaPropiedadDeUrlNoCambiaElServidor() throws Exception {
        escribeCuenta("token-del-pod");
        conCuenta().withPropertyValues("synkra.kubernetes.url=http://127.0.0.1:1", "SYNKRA_KUBERNETES_URL=http://127.0.0.1:1")
                .run(ctx -> {
                    var cliente = ctx.getBean(KubernetesClient.class);
                    assertThat(cliente.getConfiguration().getMasterUrl())
                            .startsWith("https://" + servidor.getHostName() + ":" + servidor.getPort());
                });
    }

    @Test
    void sinTokenElBackendArrancaYNoHayCliente() {
        conCuenta().run(ctx -> {
            assertThat(ctx).hasNotFailed();
            assertThat(ctx).doesNotHaveBean(KubernetesClient.class);
            assertThat(ctx.getBean(AdaptadorKubernetes.class).disponible()).isFalse();
            assertThat(servidor.getRequestCount()).isZero();
        });
    }

    @Test
    void conTokenPeroSinKubernetesServiceHostNoHayCliente() throws Exception {
        escribeCuenta("token-del-pod");
        runner.withPropertyValues("synkra.kubernetes.cuenta=" + cuenta)
                .withBean(VariablesPod.class, () -> new VariablesPod(Map.of()))
                .run(ctx -> {
                    assertThat(ctx).hasNotFailed();
                    assertThat(ctx).doesNotHaveBean(KubernetesClient.class);
                    assertThat(ctx.getBean(AdaptadorKubernetes.class).disponible()).isFalse();
                });
    }

    @Test
    void unTokenIlegibleCuentaComoSinToken() throws Exception {
        // «token» es un directorio: leerlo da IOException; el arranque no debe fallar.
        Files.createDirectory(cuenta.resolve("token"));
        conCuenta().run(ctx -> {
            assertThat(ctx).hasNotFailed();
            assertThat(ctx).doesNotHaveBean(KubernetesClient.class);
            assertThat(ctx.getBean(AdaptadorKubernetes.class).disponible()).isFalse();
        });
    }

    @Test
    void elClienteNoHacePeticionesAlArrancar() throws Exception {
        escribeCuenta("t");
        conCuenta().run(ctx -> {
            assertThat(ctx).hasSingleBean(KubernetesClient.class);
            assertThat(servidor.getRequestCount()).isZero();
        });
    }
}
