package io.synkra.despliegue;

import io.synkra.kubernetes.api.AdaptadorKubernetes;
import io.synkra.kubernetes.api.ClienteNoDisponibleException;
import io.synkra.kubernetes.api.ContextoFalloK8s;
import io.synkra.kubernetes.api.DespliegueAusenteException;
import io.synkra.kubernetes.api.LecturaFallidaException;
import io.synkra.kubernetes.api.LecturaPais;
import io.synkra.kubernetes.api.LecturaPaisInconsistenteException;
import io.synkra.registro.api.ContextoFallo;
import io.synkra.registro.api.DespliegueRegistrado;
import io.synkra.registro.api.EstadoDespliegue;
import io.synkra.registro.api.RegistroApi;
import io.synkra.registro.api.TransicionNoPermitidaException;
import java.time.Clock;
import java.time.Duration;
import java.util.HashSet;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * R-D10: una pasada ({@link #revisar()}) sobre los despliegues {@code aplicando} del registro. Solo lee del clúster:
 * nunca construye, aplica ni revierte (P2). Sin estado en memoria: lo que vigila sale cada vez del registro, así que
 * tras un reinicio la primera pasada retoma lo que había (R-D12). No es transaccional: no retiene la única conexión
 * mientras habla con el clúster.
 */
@Component
class VigilanciaDespliegues {

    private static final Logger LOG = LoggerFactory.getLogger(VigilanciaDespliegues.class);

    /** Lo que se vio en el clúster para un despliegue. */
    private enum Situacion { LISTO, FALLIDO, EN_CURSO, NO_APLICADO, SIN_LECTURA }

    private final RegistroApi registro;
    private final AdaptadorKubernetes kubernetes;
    private final Clock reloj;
    private final Duration plazo;

    VigilanciaDespliegues(RegistroApi registro, AdaptadorKubernetes kubernetes, Clock reloj,
            @Value("${synkra.vigilancia.plazo}") Duration plazo) {
        if (plazo.isZero() || plazo.isNegative()) {
            throw new IllegalStateException("synkra.vigilancia.plazo debe ser positivo");
        }
        this.registro = registro;
        this.kubernetes = kubernetes;
        this.reloj = reloj;
        this.plazo = plazo;
    }

    /** Una pasada, del {@code aplicando} más antiguo al más nuevo; devuelve cuántos despliegues cerró. */
    int revisar() {
        int cerrados = 0;
        for (var d : registro.aplicando()) {
            try {
                if (revisa(d)) {
                    cerrados++;
                }
            } catch (TransicionNoPermitidaException e) {
                // Otra pasada ya la cerró: no es un error.
            } catch (RuntimeException e) {
                LOG.warn("No se pudo revisar el despliegue {} de {}; sigue con el siguiente", d.id(), d.pais(), e);
            }
        }
        return cerrados;
    }

    private boolean revisa(DespliegueRegistrado d) {
        boolean vencido = !reloj.instant().isBefore(d.confirmadoEn().plus(plazo));
        var situacion = leer(d);
        if (situacion == Situacion.LISTO) {
            registro.actualizarResultado(d.id(), EstadoDespliegue.LISTO, null);
            return true;
        }
        Optional<ContextoFalloK8s> leido = Optional.empty();
        boolean intentado = false;
        if (situacion == Situacion.FALLIDO) {
            leido = contexto(d);
            intentado = true;
            if (leido.isPresent()) {
                var k = leido.get();
                registro.actualizarResultado(d.id(), EstadoDespliegue.FALLIDO,
                        new ContextoFallo(k.logCore(), k.logInit(), k.eventos()));
                return true;
            }
            // Sin poder leer el contexto de un fallo no se cierra aquí: el plazo lo cerrará.
            situacion = Situacion.SIN_LECTURA;
        }
        if (!vencido) {
            return false;
        }
        var k = intentado ? leido : contexto(d);
        var eventos = new StringBuilder(MotivosDespliegue.VIGILANCIA_PLAZO);
        if (situacion == Situacion.NO_APLICADO) {
            eventos.append('\n').append(MotivosDespliegue.VIGILANCIA_NO_APLICADO);
        } else if (situacion == Situacion.SIN_LECTURA) {
            eventos.append('\n').append(MotivosDespliegue.VIGILANCIA_SIN_LECTURA);
        }
        if (k.isPresent() && !k.get().eventos().isEmpty()) {
            eventos.append('\n').append(k.get().eventos());
        }
        registro.actualizarResultado(d.id(), EstadoDespliegue.FALLIDO, new ContextoFallo(
                k.map(ContextoFalloK8s::logCore).orElse(""), k.map(ContextoFalloK8s::logInit).orElse(""),
                eventos.toString()));
        return true;
    }

    /** Solo lecturas del adaptador. */
    private Situacion leer(DespliegueRegistrado d) {
        if (!kubernetes.disponible()) {
            return Situacion.SIN_LECTURA;
        }
        try {
            var lectura = kubernetes.leerPais(d.pais());
            if (lectura.isEmpty() || !coincide(d, lectura.get())) {
                return Situacion.NO_APLICADO;
            }
            return switch (kubernetes.estado(namespace(d)).fase()) {
                case LISTO -> Situacion.LISTO;
                case FALLIDO -> Situacion.FALLIDO;
                case EN_CURSO -> Situacion.EN_CURSO;
            };
        } catch (DespliegueAusenteException e) {
            return Situacion.NO_APLICADO;
        } catch (ClienteNoDisponibleException | LecturaFallidaException | LecturaPaisInconsistenteException e) {
            return Situacion.SIN_LECTURA;
        }
    }

    /** Vacío si no hay cliente o la lectura falla. */
    private Optional<ContextoFalloK8s> contexto(DespliegueRegistrado d) {
        if (!kubernetes.disponible()) {
            return Optional.empty();
        }
        try {
            return Optional.of(kubernetes.contextoFallo(namespace(d)));
        } catch (ClienteNoDisponibleException | LecturaFallidaException e) {
            return Optional.empty();
        }
    }

    /** Lo que corre es lo confirmado: la imagen y el conjunto de (nombre, versión, sha256) de los plugins. */
    private static boolean coincide(DespliegueRegistrado d, LecturaPais corre) {
        if (!d.imagen().equals(corre.imagenCore())) {
            return false;
        }
        var confirmados = new HashSet<LecturaPais.PluginLeido>();
        d.plugins().forEach(p -> confirmados.add(new LecturaPais.PluginLeido(p.nombre(), p.version(), p.sha256())));
        return confirmados.equals(new HashSet<>(corre.plugins()));
    }

    private static String namespace(DespliegueRegistrado d) {
        return "synkra-" + d.pais();
    }
}
