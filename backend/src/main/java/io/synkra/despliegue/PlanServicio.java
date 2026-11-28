package io.synkra.despliegue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.synkra.catalogo.api.CatalogoApi;
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
import java.util.Comparator;
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

    static final String CREADO_POR = "anonimo";

    private final PaisPermitido paises;
    private final VerificacionVivo verificacion;
    private final CatalogoApi catalogo;
    private final AdaptadorKubernetes kubernetes;
    private final JdbcAggregateTemplate plantilla;
    private final ObjectMapper json;
    private final TransactionTemplate transaccion;
    private final Clock reloj = Clock.systemUTC();

    PlanServicio(PaisPermitido paises, VerificacionVivo verificacion, CatalogoApi catalogo,
            AdaptadorKubernetes kubernetes, JdbcAggregateTemplate plantilla, ObjectMapper json,
            PlatformTransactionManager gestor) {
        this.paises = paises;
        this.verificacion = verificacion;
        this.catalogo = catalogo;
        this.kubernetes = kubernetes;
        this.plantilla = plantilla;
        this.json = json;
        this.transaccion = new TransactionTemplate(gestor);
    }

    PlanPreparado prepararPlan(String pais, String core, String versionCore, List<PluginPedido> plugins) {
        var namespace = paises.namespace(pais);

        var verificado = verificacion.verificar(core, versionCore, plugins);
        rechazaSiNoCompatible(verificado.veredicto());

        var registrado = catalogo.obtenerCore(core);
        var imagen = registrado.imagenes().get(verificado.seleccion().versionCore());
        var elegidos = verificado.seleccion().plugins().stream()
                .map(d -> new PluginResumen(d.nombre(), d.version().toString(),
                        catalogo.obtenerPlugin(core, d.nombre(), d.version().toString()).sha256()))
                .sorted(Comparator.comparing(PluginResumen::nombre).thenComparing(PluginResumen::version))
                .toList();
        var resumen = new Resumen(namespace, core, verificado.seleccion().versionCore().toString(), imagen, elegidos);

        var corre = leer(pais);
        var cambios = CalculoCambios.calcular(corre, imagen, elegidos);

        var pedidos = elegidos.stream().map(p -> new PluginPedido(p.nombre(), p.version())).toList();
        var nuevo = Plan.nuevo(pais, Plan.DESPLIEGUE, null, core, resumen.versionCore(), texto(pedidos),
                texto(resumen), texto(cambios), CREADO_POR, reloj.instant());
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

    private static void rechazaSiNoCompatible(Veredicto veredicto) {
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
