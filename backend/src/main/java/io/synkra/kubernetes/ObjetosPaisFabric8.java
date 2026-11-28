package io.synkra.kubernetes;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.synkra.kubernetes.api.ObjetosPais;
import io.synkra.kubernetes.api.ObjetosPaisIncompletosException;

/** Los objetos de Fabric8 detrás de {@link ObjetosPais}; solo este paquete los ve. */
record ObjetosPaisFabric8(Deployment deployment, ConfigMap configMap, Service service) implements ObjetosPais {

    /** @throws IllegalArgumentException si {@code objetos} no lo armó este módulo */
    static ObjetosPaisFabric8 de(ObjetosPais objetos) {
        if (objetos instanceof ObjetosPaisFabric8 f) {
            return f;
        }
        throw new IllegalArgumentException("Solo se aplican los objetos que entrega construir");
    }

    /** El namespace único de los tres; falla con el nombre de lo que falta o de la discrepancia. */
    String namespaceComun() {
        String ns = namespaceDe("ConfigMap", configMap == null ? null : configMap.getMetadata());
        if (!ns.equals(namespaceDe("Service", service == null ? null : service.getMetadata()))
                || !ns.equals(namespaceDe("Deployment", deployment == null ? null : deployment.getMetadata()))) {
            throw new IllegalArgumentException("Los tres objetos deben estar en el mismo namespace");
        }
        return ns;
    }

    private static String namespaceDe(String tipo, io.fabric8.kubernetes.api.model.ObjectMeta meta) {
        if (meta == null) {
            throw new ObjetosPaisIncompletosException(tipo + " ausente o sin metadatos");
        }
        if (meta.getNamespace() == null) {
            throw new ObjetosPaisIncompletosException(tipo + " sin namespace");
        }
        return meta.getNamespace();
    }

    @Override
    public String yamlDeployment() {
        return Serialization.asYaml(deployment);
    }

    @Override
    public String yamlConfigMap() {
        return Serialization.asYaml(configMap);
    }

    @Override
    public String yamlService() {
        return Serialization.asYaml(service);
    }
}
