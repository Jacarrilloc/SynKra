package io.synkra.kubernetes;

import static org.assertj.core.api.Assertions.assertThat;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;
import io.synkra.kubernetes.api.AdaptadorKubernetes;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import io.fabric8.mockwebserver.http.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/** U05-T01: el cliente sale solo de la cuenta del pod; solo se usa el servidor simulado de Fabric8. */
class ClienteKubernetesTest {

    private static final String VERSION = "{\"major\":\"1\",\"minor\":\"30\",\"gitVersion\":\"v1.30.0\"}";

    @TempDir
    Path cuenta;

    KubernetesMockServer servidor;

    final ApplicationContextRunner runner =
            new ApplicationContextRunner().withUserConfiguration(ConfiguracionKubernetes.class);

    @BeforeEach
    void arrancaServidor() {
        servidor = new KubernetesMockServer(false);
        servidor.init();
        servidor.expect().get().withPath("/version").andReturn(200, VERSION).always();
    }

    @AfterEach
    void paraServidor() {
        servidor.destroy();
        System.clearProperty("kubeconfig");
    }

    private String urlServidor() {
        return "http://" + servidor.getHostName() + ":" + servidor.getPort();
    }

    private void escribeCuenta(String token) throws Exception {
        Files.writeString(cuenta.resolve("token"), token + "\n");
        try (var ca = getClass().getResourceAsStream("/kubernetes/ca.crt")) {
            Files.copy(ca, cuenta.resolve("ca.crt"), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private ApplicationContextRunner conCuenta() {
        return runner.withPropertyValues(
                "synkra.kubernetes.cuenta=" + cuenta, "synkra.kubernetes.url=" + urlServidor());
    }

    @Test
    void conTokenYCaDeLaCuentaLaPeticionLlevaElBearer() throws Exception {
        escribeCuenta("token-del-pod-123");
        conCuenta().run(ctx -> {
            assertThat(ctx).hasSingleBean(KubernetesClient.class);
            assertThat(ctx.getBean(AdaptadorKubernetes.class).disponible()).isTrue();
            ctx.getBean(KubernetesClient.class).getKubernetesVersion();
            RecordedRequest peticion = servidor.getLastRequest();
            assertThat(peticion.getPath()).isEqualTo("/version");
            assertThat(peticion.getHeader("Authorization")).isEqualTo("Bearer token-del-pod-123");
        });
    }

    @Test
    void elKubeconfigDeLaMaquinaNoSeUsa() throws Exception {
        escribeCuenta("token-del-pod");
        var otro = new KubernetesMockServer(false);
        otro.init();
        try {
            Path kubeconfig = cuenta.resolve("kubeconfig-ajeno");
            Files.writeString(kubeconfig, """
                    apiVersion: v1
                    kind: Config
                    current-context: ajeno
                    clusters:
                    - name: ajeno
                      cluster:
                        server: http://%s:%d
                    contexts:
                    - name: ajeno
                      context: {cluster: ajeno, user: ajeno}
                    users:
                    - name: ajeno
                      user: {token: token-ajeno}
                    """.formatted(otro.getHostName(), otro.getPort()));
            System.setProperty("kubeconfig", kubeconfig.toString());
            conCuenta().run(ctx -> {
                ctx.getBean(KubernetesClient.class).getKubernetesVersion();
                assertThat(servidor.getLastRequest().getHeader("Authorization")).isEqualTo("Bearer token-del-pod");
                assertThat(otro.getRequestCount()).isZero();
            });
        } finally {
            otro.destroy();
        }
    }

    @Test
    void sinTokenElBackendArrancaYNoHayCliente() {
        // la cuenta existe pero vacia de token
        conCuenta().run(ctx -> {
            assertThat(ctx).hasNotFailed();
            assertThat(ctx).doesNotHaveBean(KubernetesClient.class);
            assertThat(ctx.getBean(AdaptadorKubernetes.class).disponible()).isFalse();
            assertThat(servidor.getRequestCount()).isZero();
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
