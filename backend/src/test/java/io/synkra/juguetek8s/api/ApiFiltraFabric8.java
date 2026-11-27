package io.synkra.juguetek8s.api;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.Service;
import java.util.List;

/** API de juguete (solo pruebas): firmas públicas con tipos de Fabric8, directos y como argumento genérico. */
public class ApiFiltraFabric8 {

    public ConfigMap directo() {
        return null;
    }

    public void genericoEnParametro(List<Service> servicios) {}
}
