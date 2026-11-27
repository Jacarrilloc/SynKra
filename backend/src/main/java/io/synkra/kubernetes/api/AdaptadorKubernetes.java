package io.synkra.kubernetes.api;

import java.util.Optional;

/** Puerta del módulo Kubernetes. Los métodos de negocio llegan en U05-T02 a T06. */
public interface AdaptadorKubernetes {

    /**
     * Verdadero solo si el pod tiene cuenta de servicio (token) y clúster al que hablar. Se decide una vez al
     * arrancar y no cambia durante la vida del proceso (C-68: un token que aparece después no se recoge).
     */
    boolean disponible();

    /**
     * Verdadero si el namespace existe en el clúster. Método mínimo de U05-T02 para ejercer la guardia.
     *
     * @throws NamespaceNoPermitidoException si no es {@code synkra-<país>} (sin tocar la red)
     */
    boolean existeNamespace(String namespace);

    /**
     * Arma Deployment, ConfigMap y Service del país en {@code synkra-<país>}, sin hablar con la API.
     *
     * @throws NamespaceNoPermitidoException si el namespace derivado no pasa la guardia
     */
    ObjetosPais construir(PlanPais plan);

    /**
     * Aplica los tres objetos con server-side apply (field manager {@code synkra}), en el orden ConfigMap,
     * Service y Deployment. Si uno falla, los anteriores no se deshacen.
     *
     * @throws ObjetosPaisIncompletosException si falta un objeto, sus metadatos o su namespace (sin peticiones)
     * @throws IllegalArgumentException si los objetos no comparten namespace o no los armó {@link #construir} (sin peticiones)
     * @throws NamespaceNoPermitidoException si el namespace no pasa la guardia (sin peticiones)
     * @throws IllegalStateException si no hay cliente (el pod no tiene cuenta de servicio); sin peticiones
     * @throws AplicacionFallidaException si la API falla; nombra el objeto
     */
    void aplicar(ObjetosPais objetos);

    /**
     * Una sola lectura (sin esperar ni sondear) de réplicas, reinicios y fase del Deployment {@code synkra-core}.
     *
     * @throws NamespaceNoPermitidoException si el namespace no pasa la guardia (sin peticiones)
     * @throws ClienteNoDisponibleException si no hay cliente (sin peticiones)
     * @throws DespliegueAusenteException si el Deployment no existe
     * @throws LecturaFallidaException si la API falla
     */
    EstadoPais estado(String namespace);

    /**
     * Últimas 200 líneas del log de {@code core}, log del init {@code descarga} y eventos de los pods del país.
     * Un log que aún no existe (pod {@code Pending}) queda vacío y no es un error.
     *
     * @throws NamespaceNoPermitidoException si el namespace no pasa la guardia (sin peticiones)
     * @throws ClienteNoDisponibleException si no hay cliente (sin peticiones)
     * @throws LecturaFallidaException si la API falla
     */
    ContextoFalloK8s contextoFallo(String namespace);

    /**
     * Lee del clúster, solo con {@code GET}, el Deployment {@code synkra-core} y el ConfigMap {@code synkra-core-config}
     * de {@code synkra-<país>}: la imagen del core y los plugins con nombre, versión y sha256. Vacío si no existe
     * ninguno de los dos objetos.
     *
     * @throws NamespaceNoPermitidoException si el país no son dos letras minúsculas (sin peticiones)
     * @throws ClienteNoDisponibleException si no hay cliente (sin peticiones)
     * @throws LecturaPaisInconsistenteException si falta uno de los dos objetos, un plugin está en un solo archivo
     *     del ConfigMap, el YAML no es seguro o un valor no cumple el formato de {@link PlanPais}
     * @throws LecturaFallidaException si la API falla
     */
    Optional<LecturaPais> leerPais(String pais);
}
