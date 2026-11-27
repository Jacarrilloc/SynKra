package io.synkra.kubernetes;

import io.fabric8.kubernetes.api.model.ContainerStatus;
import io.fabric8.kubernetes.api.model.Event;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.synkra.kubernetes.api.ContextoFalloK8s;
import io.synkra.kubernetes.api.DespliegueAusenteException;
import io.synkra.kubernetes.api.EstadoPais;
import io.synkra.kubernetes.api.FaseDespliegue;
import io.synkra.kubernetes.api.LecturaFallidaException;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Lecturas de U05-T05: solo {@code get}, {@code list} y {@code pods/log}; nunca escribe. El namespace ya pasó la guardia. */
final class LectorPais {

    static final int LINEAS_LOG_CORE = 200;
    static final String PREFIJO_INIT = "Init:";
    static final String RAZON_INIT_ERROR = PREFIJO_INIT + "Error";
    private static final Set<String> RAZONES_DE_FALLO =
            Set.of("CrashLoopBackOff", "ImagePullBackOff", "ErrImagePull");
    /** Valor por defecto de {@code spec.replicas} en Kubernetes cuando el Deployment no lo fija. */
    static final int REPLICAS_POR_DEFECTO_K8S = 1;
    private static final int HTTP_BAD_REQUEST = 400;
    private static final int HTTP_NOT_FOUND = 404;

    private final KubernetesClient cliente;

    LectorPais(KubernetesClient cliente) {
        this.cliente = cliente;
    }

    EstadoPais estado(String ns) {
        String deployment = "el Deployment " + ConstructorObjetosPais.NOMBRE_CORE;
        Deployment d = envuelve(deployment, ns, () -> cliente.apps().deployments().inNamespace(ns)
                .withName(ConstructorObjetosPais.NOMBRE_CORE).get());
        if (d == null) {
            throw new DespliegueAusenteException(ConstructorObjetosPais.NOMBRE_CORE, ns);
        }
        int deseadas = d.getSpec() != null && d.getSpec().getReplicas() != null
                ? d.getSpec().getReplicas() : REPLICAS_POR_DEFECTO_K8S;
        int listas = d.getStatus() != null && d.getStatus().getReadyReplicas() != null
                ? d.getStatus().getReadyReplicas() : 0;
        List<Pod> pods = envuelve("los pods de " + ConstructorObjetosPais.NOMBRE_CORE, ns, () -> pods(ns));
        int reinicios = pods.stream().flatMap(LectorPais::estados).mapToInt(LectorPais::reinicios).sum();
        if (listas == deseadas && deseadas > 0) {
            return new EstadoPais(deseadas, listas, reinicios, FaseDespliegue.LISTO, null);
        }
        String razon = pods.stream().map(LectorPais::razonDeFallo).filter(Objects::nonNull).findFirst().orElse(null);
        return new EstadoPais(deseadas, listas, reinicios,
                razon != null ? FaseDespliegue.FALLIDO : FaseDespliegue.EN_CURSO, razon);
    }

    ContextoFalloK8s contexto(String ns) {
        List<Pod> pods = envuelve("los pods de " + ConstructorObjetosPais.NOMBRE_CORE, ns, () -> pods(ns));
        // Un solo pod cuenta el fallo: el de más reinicios y, a igualdad, el último en nombre.
        Pod pod = pods.stream().max(Comparator.comparingInt((Pod p) -> estados(p).mapToInt(LectorPais::reinicios).sum())
                .thenComparing(p -> p.getMetadata().getName())).orElse(null);
        String core = "";
        String init = "";
        if (pod != null) {
            String nombre = pod.getMetadata().getName();
            core = envuelve("el log del contenedor " + ConstructorObjetosPais.CONTENEDOR_CORE + " del pod " + nombre,
                    ns, () -> logCore(ns, nombre));
            init = envuelve("el log del contenedor " + ConstructorObjetosPais.CONTENEDOR_INIT + " del pod " + nombre,
                    ns, () -> leer(() -> cliente.pods().inNamespace(ns).withName(nombre)
                            .inContainer(ConstructorObjetosPais.CONTENEDOR_INIT).getLog()));
        }
        String eventos = envuelve("los eventos de los pods", ns, () -> eventos(ns, pods));
        return new ContextoFalloK8s(core, init, eventos);
    }

    /** Solo las {@link KubernetesClientException} se envuelven, nombrando lo que se leía; el resto sube tal cual. */
    private static <T> T envuelve(String que, String ns, java.util.function.Supplier<T> lectura) {
        try {
            return lectura.get();
        } catch (KubernetesClientException e) {
            throw new LecturaFallidaException(que, ns, e);
        }
    }

    private String logCore(String ns, String pod) {
        var contenedor = cliente.pods().inNamespace(ns).withName(pod).inContainer(ConstructorObjetosPais.CONTENEDOR_CORE);
        String actual = leer(() -> contenedor.tailingLines(LINEAS_LOG_CORE).getLog());
        if (!actual.isEmpty()) {
            return actual;
        }
        return leer(() -> contenedor.terminated().tailingLines(LINEAS_LOG_CORE).getLog());
    }

    /** Un log que aún no existe (400 o 404 de la API) es vacío; cualquier otro fallo sube. */
    private static String leer(java.util.function.Supplier<String> lectura) {
        try {
            String log = lectura.get();
            return log == null ? "" : log;
        } catch (KubernetesClientException e) {
            if (e.getCode() == HTTP_BAD_REQUEST || e.getCode() == HTTP_NOT_FOUND) {
                return "";
            }
            throw e;
        }
    }

    private List<Pod> pods(String ns) {
        return cliente.pods().inNamespace(ns)
                .withLabel(ConstructorObjetosPais.ETIQUETA_NOMBRE, ConstructorObjetosPais.NOMBRE_CORE).list().getItems();
    }

    private String eventos(String ns, List<Pod> pods) {
        Set<String> nombres = pods.stream().map(p -> p.getMetadata().getName()).collect(Collectors.toSet());
        List<Event> eventos = new ArrayList<>(cliente.v1().events().inNamespace(ns).list().getItems());
        return eventos.stream()
                .filter(e -> e.getInvolvedObject() != null && nombres.contains(e.getInvolvedObject().getName()))
                // Los eventos sin marca de tiempo válida van al final.
                .sorted(Comparator.comparing(LectorPais::hora, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(e -> e.getType() + " " + e.getReason() + ": " + e.getMessage())
                .collect(Collectors.joining("\n"));
    }

    /** Instante del evento (se compara como {@link Instant}, no como texto); {@code null} si no tiene marca válida. */
    private static Instant hora(Event e) {
        String texto = null;
        if (e.getLastTimestamp() != null) {
            texto = e.getLastTimestamp();
        } else if (e.getEventTime() != null && e.getEventTime().getTime() != null) {
            texto = e.getEventTime().getTime();
        } else if (e.getFirstTimestamp() != null) {
            texto = e.getFirstTimestamp();
        } else if (e.getMetadata() != null) {
            texto = e.getMetadata().getCreationTimestamp();
        }
        if (texto == null) {
            return null;
        }
        try {
            return Instant.parse(texto);
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    private static Stream<ContainerStatus> estados(Pod p) {
        if (p.getStatus() == null) {
            return Stream.empty();
        }
        return Stream.concat(orVacio(p.getStatus().getInitContainerStatuses()), orVacio(p.getStatus().getContainerStatuses()));
    }

    private static Stream<ContainerStatus> orVacio(List<ContainerStatus> l) {
        return l == null ? Stream.empty() : l.stream();
    }

    private static int reinicios(ContainerStatus c) {
        return c.getRestartCount() == null ? 0 : c.getRestartCount();
    }

    /** Razón de fallo del pod, con el prefijo {@code Init:} si viene de un init, o nula si no falla. */
    private static String razonDeFallo(Pod p) {
        if (p.getStatus() == null) {
            return null;
        }
        for (ContainerStatus c : (Iterable<ContainerStatus>) orVacio(p.getStatus().getInitContainerStatuses())::iterator) {
            var terminado = c.getState() != null ? c.getState().getTerminated() : null;
            if (terminado != null && terminado.getExitCode() != null && terminado.getExitCode() != 0) {
                return RAZON_INIT_ERROR;
            }
            String espera = razonEnEspera(c);
            if (espera != null) {
                return PREFIJO_INIT + espera;
            }
        }
        for (ContainerStatus c : (Iterable<ContainerStatus>) orVacio(p.getStatus().getContainerStatuses())::iterator) {
            String espera = razonEnEspera(c);
            if (espera != null) {
                return espera;
            }
        }
        return null;
    }

    private static String razonEnEspera(ContainerStatus c) {
        var espera = c.getState() != null ? c.getState().getWaiting() : null;
        return espera != null && RAZONES_DE_FALLO.contains(espera.getReason()) ? espera.getReason() : null;
    }
}
