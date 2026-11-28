package io.synkra.juguetek8s;

import io.fabric8.kubernetes.api.model.ConfigMap;

/** Clase de juguete (solo pruebas): un módulo ajeno a kubernetes que importa Fabric8. Debe hacer fallar la regla. */
public class FueraUsaFabric8 {

    ConfigMap cm = new ConfigMap();
}
