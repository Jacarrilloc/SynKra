package io.synkra.kubernetes;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.synkra.kubernetes.api.AdaptadorKubernetes;
import io.synkra.kubernetes.api.AplicacionFallidaException;
import io.synkra.kubernetes.api.ClienteNoDisponibleException;
import io.synkra.kubernetes.api.ContextoFalloK8s;
import io.synkra.kubernetes.api.EstadoPais;
import io.synkra.kubernetes.api.LecturaPais;
import io.synkra.kubernetes.api.ObjetosPais;
import io.synkra.kubernetes.api.PlanPais;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Implementación sobre Fabric8; {@code cliente} es nulo si el pod no tiene cuenta de servicio. */
class AdaptadorFabric8 implements AdaptadorKubernetes {

    private final KubernetesClient cliente;

    private final String imagenInit;

    AdaptadorFabric8(KubernetesClient cliente) {
        this(cliente, ConstructorObjetosPais.IMAGEN_INIT_POR_DEFECTO);
    }

    /** @throws IllegalArgumentException si la imagen del init no lleva registro y tag o digest */
    AdaptadorFabric8(KubernetesClient cliente, String imagenInit) {
        this.cliente = cliente;
        this.imagenInit = ConstructorObjetosPais.exigirImagenInit(imagenInit);
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
        return ConstructorObjetosPais.construir(plan, namespaceDe(plan.pais()), imagenInit);
    }

    @Override
    public void aplicar(ObjetosPais objetos) {
        Objects.requireNonNull(objetos, "objetos");
        String ns = namespaceComun(objetos);
        if (cliente == null) {
            throw new IllegalStateException("No hay cliente de Kubernetes: el pod no tiene cuenta de servicio");
        }
        // ConfigMap primero, luego Service, luego Deployment; el que falla corta, los anteriores se quedan.
        for (HasMetadata objeto : List.of(objetos.configMap(), objetos.service(), objetos.deployment())) {
            try {
                cliente.resource(objeto).inNamespace(ns).fieldManager(ConstructorObjetosPais.GESTOR).forceConflicts().serverSideApply();
            } catch (KubernetesClientException e) {
                throw new AplicacionFallidaException(objeto.getKind() + "/" + objeto.getMetadata().getName(), e);
            }
        }
    }

    @Override
    public EstadoPais estado(String namespace) {
        String ns = GuardiaNamespace.exigir(namespace);
        return lector().estado(ns);
    }

    @Override
    public ContextoFalloK8s contextoFallo(String namespace) {
        String ns = GuardiaNamespace.exigir(namespace);
        return lector().contexto(ns);
    }

    @Override
    public Optional<LecturaPais> leerPais(String pais) {
        String ns = namespaceDe(pais);
        return lector().leerPais(pais, ns);
    }

    private LectorPais lector() {
        if (cliente == null) {
            throw new ClienteNoDisponibleException();
        }
        return new LectorPais(cliente);
    }

    /** El namespace único de los tres objetos, ya pasado por la guardia; sin ninguna petición. */
    private static String namespaceComun(ObjetosPais objetos) {
        String ns = objetos.configMap().getMetadata().getNamespace();
        if (!Objects.equals(ns, objetos.service().getMetadata().getNamespace())
                || !Objects.equals(ns, objetos.deployment().getMetadata().getNamespace())) {
            throw new IllegalArgumentException("Los tres objetos deben estar en el mismo namespace");
        }
        return GuardiaNamespace.exigir(ns);
    }

    /** Namespace del país, pasando por la guardia antes de armar nada. */
    static String namespaceDe(String pais) {
        return GuardiaNamespace.exigir("synkra-" + pais);
    }
}
