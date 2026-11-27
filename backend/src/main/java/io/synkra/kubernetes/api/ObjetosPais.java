package io.synkra.kubernetes.api;

/**
 * Los tres objetos de un país, armados en memoria; aún no están en el clúster. Solo los entrega
 * {@link AdaptadorKubernetes#construir(PlanPais)}: la API no expone tipos de Fabric8, así que quien necesite
 * ver el contenido lo lee como texto YAML.
 */
public interface ObjetosPais {

    /** El Deployment del core, como YAML. */
    String yamlDeployment();

    /** El ConfigMap del país, como YAML. */
    String yamlConfigMap();

    /** El Service del core, como YAML. */
    String yamlService();
}
