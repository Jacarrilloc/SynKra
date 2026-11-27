package io.synkra.kubernetes;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.synkra.kubernetes.api.AdaptadorKubernetes;

/** Implementación sobre Fabric8; {@code cliente} es nulo si el pod no tiene cuenta de servicio. */
class AdaptadorFabric8 implements AdaptadorKubernetes {

    private final KubernetesClient cliente;

    AdaptadorFabric8(KubernetesClient cliente) {
        this.cliente = cliente;
    }

    @Override
    public boolean disponible() {
        return cliente != null;
    }
}
