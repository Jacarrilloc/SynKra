package io.synkra.kubernetes;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.synkra.kubernetes.api.AdaptadorKubernetes;
import java.nio.file.Files;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * Cliente de Kubernetes armado solo con la cuenta de servicio del pod. Parte de {@link Config#empty()}:
 * no lee ~/.kube/config, ni KUBECONFIG, ni propiedades o variables de autoconfiguración de Fabric8.
 * El cliente no es un bean: sin token (o sin clúster) el adaptador queda sin cliente y el módulo deshabilitado.
 */
@Configuration(proxyBeanMethods = false)
class ConfiguracionKubernetes {

    /**
     * El único bean que sale de aquí es el adaptador: el cliente de Fabric8 nace dentro de él y se cierra con él,
     * así que ningún otro módulo puede inyectarlo y saltarse la guardia de namespace. Sin cuenta completa
     * (token y clúster) el adaptador no tiene cliente y queda no disponible.
     */
    @Bean
    AdaptadorKubernetes adaptadorKubernetes(Environment env, ObjectProvider<VariablesPod> variables,
            @Value("${synkra.kubernetes.imagen-init:" + ConstructorObjetosPais.IMAGEN_INIT_POR_DEFECTO + "}") String imagenInit) {
        var cuenta = CuentaServicio.de(env, variables.getIfAvailable(VariablesPod::delProceso));
        return new AdaptadorFabric8(cuenta.completa() ? cliente(cuenta) : null, imagenInit);
    }

    private static KubernetesClient cliente(CuentaServicio cuenta) {
        var config = new ConfigBuilder(Config.empty())
                .withAutoConfigure(false)
                .withMasterUrl(cuenta.url())
                .withOauthTokenProvider(cuenta::token)
                .withRequestRetryBackoffLimit(0)
                .withCaCertFile(Files.isReadable(cuenta.archivoCa()) ? cuenta.archivoCa().toString() : null)
                .build();
        return new KubernetesClientBuilder().withConfig(config).build();
    }
}
