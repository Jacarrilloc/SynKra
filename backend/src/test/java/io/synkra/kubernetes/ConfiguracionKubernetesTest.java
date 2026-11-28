package io.synkra.kubernetes;

import static org.assertj.core.api.Assertions.assertThat;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.synkra.kubernetes.api.AdaptadorKubernetes;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/** U05-T09 (C-70): el cliente de Fabric8 no es un bean; solo se publica el adaptador. */
class ConfiguracionKubernetesTest {

    @TempDir
    Path cuenta;

    @Test
    void conCuentaDeServicioElClienteNoEsUnBeanYElAdaptadorSi() throws Exception {
        Files.writeString(cuenta.resolve("token"), "t\n");
        new ApplicationContextRunner().withUserConfiguration(ConfiguracionKubernetes.class)
                .withPropertyValues("synkra.kubernetes.cuenta=" + cuenta)
                .withBean(VariablesPod.class, () -> new VariablesPod(Map.of("KUBERNETES_SERVICE_HOST", "127.0.0.1")))
                .run(ctx -> {
                    assertThat(ctx).hasNotFailed();
                    assertThat(ctx.getBeanNamesForType(KubernetesClient.class)).isEmpty();
                    assertThat(ctx.getBeanNamesForType(AdaptadorKubernetes.class)).hasSize(1);
                    assertThat(ctx.getBean(AdaptadorKubernetes.class).disponible()).isTrue();
                });
    }

    @Test
    void sinCuentaTampocoHayClienteComoBean() {
        new ApplicationContextRunner().withUserConfiguration(ConfiguracionKubernetes.class)
                .withPropertyValues("synkra.kubernetes.cuenta=" + cuenta)
                .withBean(VariablesPod.class, () -> new VariablesPod(Map.of()))
                .run(ctx -> {
                    assertThat(ctx.getBeanNamesForType(KubernetesClient.class)).isEmpty();
                    assertThat(ctx.getBean(AdaptadorKubernetes.class).disponible()).isFalse();
                });
    }
}
