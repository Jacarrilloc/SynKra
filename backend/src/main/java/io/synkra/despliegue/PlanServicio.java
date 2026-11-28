package io.synkra.despliegue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.synkra.kubernetes.api.AdaptadorKubernetes;
import io.synkra.kubernetes.api.ClienteNoDisponibleException;
import io.synkra.kubernetes.api.LecturaFallidaException;
import io.synkra.kubernetes.api.LecturaPaisInconsistenteException;
import io.synkra.plataforma.DetalleError;
import io.synkra.plataforma.ErrorApi;
import io.synkra.plataforma.ExcepcionApi;
import io.synkra.verificador.Veredicto;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jdbc.core.JdbcAggregateTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * R-D3 y R-D4: crea un plan pendiente. El orden es contrato: país, selección y veredicto, resumen del catálogo,
 * lectura de lo que corre y, solo entonces, el alta (en su propia transacción corta, sin la lectura del clúster).
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

    PlanServicio(PaisPermitido paises, VerificacionVivo verificacion, ResumenCatalogo resumenCatalogo,
            AdaptadorKubernetes kubernetes, JdbcAggregateTemplate plantilla, ObjectMapper json,
            PlatformTransactionManager gestor, Clock reloj) {
        this.reloj = reloj;
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
        var imagen = resumen.imagen();
        var elegidos = resumen.plugins();

        var corre = leer(pais);
        var cambios = CalculoCambios.calcular(corre, imagen, elegidos);

        var pedidos = elegidos.stream().map(p -> new PluginPedido(p.nombre(), p.version())).toList();
        var nuevo = Plan.nuevo(pais, Plan.DESPLIEGUE, null, core, resumen.versionCore(), texto(pedidos),
                texto(resumen), texto(cambios), creadoPor, reloj.instant());
        var guardado = transaccion.execute(estado -> plantilla.insert(nuevo));
        return new PlanPreparado(guardado, resumen, cambios);
    }

    private java.util.Optional<io.synkra.kubernetes.api.LecturaPais> leer(String pais) {
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
