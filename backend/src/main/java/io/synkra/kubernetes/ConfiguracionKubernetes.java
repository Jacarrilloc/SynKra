package io.synkra.kubernetes;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.synkra.kubernetes.api.AdaptadorKubernetes;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Condition;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Cliente de Kubernetes armado solo con la cuenta de servicio del pod. Parte de {@link Config#empty()}:
 * no lee ~/.kube/config, ni KUBECONFIG, ni propiedades o variables de autoconfiguración de Fabric8.
 * Sin token (o sin clúster) no hay bean {@link KubernetesClient}: el módulo queda deshabilitado.
 */
@Configuration(proxyBeanMethods = false)
class ConfiguracionKubernetes {

    static class HayCuentaDeServicio implements Condition {
        @Override
        public boolean matches(ConditionContext contexto, AnnotatedTypeMetadata metadatos) {
            return CuentaServicio.de(contexto.getEnvironment()).completa();
        }
    }

    @Bean(destroyMethod = "close")
    @Conditional(HayCuentaDeServicio.class)
    KubernetesClient clienteKubernetes(Environment env) {
        var cuenta = CuentaServicio.de(env);
        var config = new ConfigBuilder(Config.empty())
                .withAutoConfigure(false)
                .withMasterUrl(cuenta.url())
                .withOauthTokenProvider(cuenta::token)
                .withCaCertFile(java.nio.file.Files.isReadable(cuenta.archivoCa()) ? cuenta.archivoCa().toString() : null)
                .build();
        return new KubernetesClientBuilder().withConfig(config).build();
    }

    @Bean
    AdaptadorKubernetes adaptadorKubernetes(ObjectProvider<KubernetesClient> cliente) {
        return new AdaptadorFabric8(cliente.getIfAvailable());
    }
}
