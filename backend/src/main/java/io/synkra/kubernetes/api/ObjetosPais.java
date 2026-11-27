package io.synkra.kubernetes.api;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.apps.Deployment;

/** Los tres objetos de un país, armados en memoria; aún no están en el clúster. */
public record ObjetosPais(Deployment deployment, ConfigMap configMap, Service service) {}
