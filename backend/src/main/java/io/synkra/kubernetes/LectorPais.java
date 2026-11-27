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
    private static final int HTTP_BAD_REQUEST = 400;
    private static final int HTTP_NOT_FOUND = 404;

    private final KubernetesClient cliente;

    LectorPais(KubernetesClient cliente) {
        this.cliente = cliente;
    }

    EstadoPais estado(String ns) {
        try {
            Deployment d = cliente.apps().deployments().inNamespace(ns).withName(ConstructorObjetosPais.NOMBRE_CORE).get();
            if (d == null) {
                throw new DespliegueAusenteException(ns);
            }
            int deseadas = d.getSpec() != null && d.getSpec().getReplicas() != null ? d.getSpec().getReplicas() : 1;
            int listas = d.getStatus() != null && d.getStatus().getReadyReplicas() != null
                    ? d.getStatus().getReadyReplicas() : 0;
            List<Pod> pods = pods(ns);
            int reinicios = pods.stream().flatMap(LectorPais::estados).mapToInt(LectorPais::reinicios).sum();
            if (listas == deseadas && deseadas > 0) {
                return new EstadoPais(deseadas, listas, reinicios, FaseDespliegue.LISTO, null);
            }
            String razon = pods.stream().map(LectorPais::razonDeFallo).filter(Objects::nonNull).findFirst().orElse(null);
            return new EstadoPais(deseadas, listas, reinicios,
                    razon != null ? FaseDespliegue.FALLIDO : FaseDespliegue.EN_CURSO, razon);
        } catch (KubernetesClientException e) {
            throw new LecturaFallidaException("el Deployment " + ConstructorObjetosPais.NOMBRE_CORE, ns, e);
        }
    }

    ContextoFalloK8s contexto(String ns) {
        try {
            List<Pod> pods = pods(ns);
            // Un solo pod cuenta el fallo: el de más reinicios y, a igualdad, el último en nombre.
            Pod pod = pods.stream().max(Comparator.comparingInt((Pod p) -> estados(p).mapToInt(LectorPais::reinicios).sum())
                    .thenComparing(p -> p.getMetadata().getName())).orElse(null);
            String core = "";
            String init = "";
            if (pod != null) {
                String nombre = pod.getMetadata().getName();
                core = logCore(ns, nombre);
                init = leer(() -> cliente.pods().inNamespace(ns).withName(nombre)
                        .inContainer(ConstructorObjetosPais.CONTENEDOR_INIT).getLog());
            }
            return new ContextoFalloK8s(core, init, eventos(ns, pods));
        } catch (KubernetesClientException e) {
            throw new LecturaFallidaException("el contexto de fallo", ns, e);
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
                .sorted(Comparator.comparing(LectorPais::hora))
                .map(e -> e.getType() + " " + e.getReason() + ": " + e.getMessage())
                .collect(Collectors.joining("\n"));
    }

    /** Marca de tiempo ISO-8601 del evento; se ordena como texto. */
    private static String hora(Event e) {
        if (e.getLastTimestamp() != null) {
            return e.getLastTimestamp();
        }
        if (e.getEventTime() != null && e.getEventTime().getTime() != null) {
            return e.getEventTime().getTime();
        }
        if (e.getFirstTimestamp() != null) {
            return e.getFirstTimestamp();
        }
        String creado = e.getMetadata() != null ? e.getMetadata().getCreationTimestamp() : null;
        return creado != null ? creado : "";
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
