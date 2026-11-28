package io.synkra.despliegue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.synkra.kubernetes.api.AdaptadorKubernetes;
import io.synkra.kubernetes.api.ClienteNoDisponibleException;
import io.synkra.kubernetes.api.LecturaFallidaException;
import io.synkra.kubernetes.api.LecturaPaisInconsistenteException;
import io.synkra.plataforma.CodigosError;
import io.synkra.plataforma.DetalleError;
import io.synkra.plataforma.ErrorApi;
import io.synkra.plataforma.ExcepcionApi;
import io.synkra.registro.api.DespliegueRegistrado;
import io.synkra.registro.api.EstadoDespliegue;
import io.synkra.registro.api.PluginDesplegado;
import io.synkra.registro.api.RegistroApi;
import io.synkra.verificador.Veredicto;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.jdbc.core.JdbcAggregateTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * R-D3 y R-D4: crea un plan pendiente. El orden es contrato: país, selección y veredicto, resumen del catálogo,
 * lectura de lo que corre y, solo entonces, el alta (en su propia transacción corta, sin la lectura del clúster).
 * R-D11: la reversión es el mismo plan, con la selección del despliegue {@code listo} de origen.
 */
@Service
class PlanServicio {

    private final PaisPermitido paises;
    private final VerificacionVivo verificacion;
    private final ResumenCatalogo resumenCatalogo;
    private final AdaptadorKubernetes kubernetes;
    private final JdbcAggregateTemplate plantilla;
    private final ObjectMapper json;
    private final TransactionTemplate transaccion;
    private final Clock reloj;
    private final RegistroApi registro;

    PlanServicio(PaisPermitido paises, VerificacionVivo verificacion, ResumenCatalogo resumenCatalogo,
            AdaptadorKubernetes kubernetes, JdbcAggregateTemplate plantilla, ObjectMapper json,
            PlatformTransactionManager gestor, Clock reloj, RegistroApi registro) {
        this.reloj = reloj;
        this.registro = registro;
        this.paises = paises;
        this.verificacion = verificacion;
        this.resumenCatalogo = resumenCatalogo;
        this.kubernetes = kubernetes;
        this.plantilla = plantilla;
        this.json = json;
        this.transaccion = new TransactionTemplate(gestor);
    }

    PlanPreparado prepararPlan(String pais, String core, String versionCore, List<PluginPedido> plugins,
            String creadoPor) {
        paises.exigir(pais);

        var verificado = verificacion.verificar(core, versionCore, plugins);
        rechazaSiNoCompatible(verificado.veredicto());

        var resumen = resumenCatalogo.armar(pais, verificado.seleccion());
        return guardar(pais, Plan.DESPLIEGUE, null, core, resumen, creadoPor);
    }

    /** R-D11: el origen debe ser un despliegue {@code listo}; el país, el core y los plugins salen de él. */
    PlanPreparado prepararReversion(long origenId, String creadoPor) {
        var origen = origenId <= 0 ? Optional.<DespliegueRegistrado>empty() : registro.obtener(origenId);
        if (origen.isEmpty()) {
            throw new ExcepcionApi(HttpStatus.NOT_FOUND,
                    new ErrorApi(CodigosError.NO_ENCONTRADO, CodigosError.MSG_NO_ENCONTRADO, List.of()));
        }
        var d = origen.get();
        if (d.estado() != EstadoDespliegue.LISTO) {
            throw new ExcepcionApi(HttpStatus.UNPROCESSABLE_ENTITY, new ErrorApi(CodigosDespliegue.ORIGEN_NO_LISTO,
                    MotivosDespliegue.ORIGEN_NO_LISTO_MENSAJE, List.of()));
        }
        paises.namespace(d.pais());

        var pedidos = d.plugins().stream().map(p -> new PluginPedido(p.nombre(), p.version())).toList();
        var verificado = verificacion.verificar(d.core(), d.versionCore(), pedidos);
        rechazaSiNoCompatible(verificado.veredicto());

        var resumen = resumenCatalogo.armar(d.pais(), verificado.seleccion());
        if (!resumen.imagen().equals(d.imagen()) || !ternas(d.plugins()).equals(Set.copyOf(resumen.plugins()))) {
            throw new ExcepcionApi(HttpStatus.UNPROCESSABLE_ENTITY, new ErrorApi(
                    CodigosDespliegue.ORIGEN_DESACTUALIZADO, MotivosDespliegue.ORIGEN_DESACTUALIZADO_MENSAJE, List.of()));
        }
        return guardar(d.pais(), Plan.REVERSION, origenId, d.core(), resumen, creadoPor);
    }

    private static Set<PluginResumen> ternas(List<PluginDesplegado> plugins) {
        return plugins.stream().map(p -> new PluginResumen(p.nombre(), p.version(), p.sha256()))
                .collect(Collectors.toSet());
    }

    /** Lo que corre, los cambios y el alta: los pasos finales de crear un plan y de crear una reversión. */
    private PlanPreparado guardar(String pais, String tipo, Long origenId, String core, Resumen resumen,
            String creadoPor) {
        var elegidos = resumen.plugins();
        var corre = leer(pais);
        var cambios = CalculoCambios.calcular(corre, resumen.imagen(), elegidos);

        var pedidos = elegidos.stream().map(p -> new PluginPedido(p.nombre(), p.version())).toList();
        var nuevo = Plan.nuevo(pais, tipo, origenId, core, resumen.versionCore(), texto(pedidos),
                texto(resumen), texto(cambios), creadoPor, reloj.instant());
        var guardado = transaccion.execute(estado -> plantilla.insert(nuevo));
        return new PlanPreparado(guardado, resumen, cambios);
    }

    private Optional<io.synkra.kubernetes.api.LecturaPais> leer(String pais) {
        try {
            return kubernetes.leerPais(pais);
        } catch (ClienteNoDisponibleException | LecturaFallidaException e) {
            throw new ExcepcionApi(HttpStatus.SERVICE_UNAVAILABLE, new ErrorApi(
                    CodigosDespliegue.KUBERNETES_NO_DISPONIBLE, MotivosDespliegue.KUBERNETES_MENSAJE, List.of()));
        } catch (LecturaPaisInconsistenteException e) {
            throw new ExcepcionApi(HttpStatus.CONFLICT, new ErrorApi(
                    CodigosDespliegue.PAIS_INCONSISTENTE, MotivosDespliegue.PAIS_INCONSISTENTE_MENSAJE, List.of()));
        }
    }

    /** R-D3 y R-D6: 422 {@code COMBINACION_RECHAZADA} con un detalle por motivo; lo usan crear y confirmar. */
    static void rechazaSiNoCompatible(Veredicto veredicto) {
        if (veredicto.global() == Veredicto.Global.APROBADA) {
            return;
        }
        var detalles = new ArrayList<DetalleError>();
        for (var p : veredicto.porPlugin()) {
            p.motivos().forEach(m -> detalles.add(detalle(m)));
        }
        veredicto.motivosGlobales().forEach(m -> detalles.add(detalle(m)));
        throw new ExcepcionApi(HttpStatus.UNPROCESSABLE_ENTITY, new ErrorApi(
                CodigosDespliegue.COMBINACION_RECHAZADA, MotivosDespliegue.COMBINACION_MENSAJE, detalles));
    }

    private static DetalleError detalle(io.synkra.verificador.Motivo m) {
        return new DetalleError(m.plugin().map(n -> "plugins." + n).orElse("plugins"), m.mensaje());
    }

    private String texto(Object valor) {
        try {
            return json.writeValueAsString(valor);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudo escribir el JSON del plan", e);
        }
    }
}
