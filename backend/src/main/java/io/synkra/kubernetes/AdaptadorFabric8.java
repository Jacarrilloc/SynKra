package io.synkra.kubernetes;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.synkra.kubernetes.api.AdaptadorKubernetes;
import io.synkra.kubernetes.api.ObjetosPais;
import io.synkra.kubernetes.api.PlanPais;

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

    @Override
    public boolean existeNamespace(String namespace) {
        String ns = GuardiaNamespace.exigir(namespace);
        return cliente != null && cliente.namespaces().withName(ns).get() != null;
    }

    @Override
    public ObjetosPais construir(PlanPais plan) {
        return ConstructorObjetosPais.construir(plan, namespaceDe(plan.pais()));
    }

    /** Namespace del país, pasando por la guardia antes de armar nada. */
    static String namespaceDe(String pais) {
        return GuardiaNamespace.exigir("synkra-" + pais);
    }
}
