package io.synkra.despliegue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.synkra.catalogo.api.CatalogoApi;
import io.synkra.kubernetes.api.AdaptadorKubernetes;
import io.synkra.kubernetes.api.AplicacionFallidaException;
import io.synkra.kubernetes.api.PlanPais;
import io.synkra.plataforma.ErrorApi;
import io.synkra.plataforma.ExcepcionApi;
import io.synkra.registro.api.ContextoFallo;
import io.synkra.registro.api.EstadoDespliegue;
import io.synkra.registro.api.PluginDesplegado;
import io.synkra.registro.api.RegistroApi;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * R-D5 a R-D9. El orden es contrato: usuario, guarda, verificar de nuevo, comparar el resumen con el catálogo de
 * hoy, clúster disponible, armar los objetos, reservar (una transacción) y, fuera de ella, aplicar una sola vez.
 * Desviación de R-D9 (decisión 6 de la tarea): se registra {@code aplicando} antes de aplicar.
 */
@Service
class ConfirmacionServicio {

    record Confirmado(Plan plan, String confirmadoPor, Instant confirmadoEn) {}

    private final GuardaConfirmacion guarda;
    private final VerificacionVivo verificacion;
    private final ResumenCatalogo resumenCatalogo;
    private final CatalogoApi catalogo;
    private final AdaptadorKubernetes kubernetes;
    private final ReservaConfirmacion reserva;
    private final RegistroApi registro;
    private final ObjectMapper json;

    ConfirmacionServicio(GuardaConfirmacion guarda, VerificacionVivo verificacion, ResumenCatalogo resumenCatalogo,
            CatalogoApi catalogo, AdaptadorKubernetes kubernetes, ReservaConfirmacion reserva, RegistroApi registro,
            ObjectMapper json) {
        this.guarda = guarda;
        this.verificacion = verificacion;
        this.resumenCatalogo = resumenCatalogo;
        this.catalogo = catalogo;
        this.kubernetes = kubernetes;
        this.reserva = reserva;
        this.registro = registro;
        this.json = json;
    }

    Confirmado confirmar(long id, String cabeceraUsuario) {
        var confirmadoPor = Usuario.de(cabeceraUsuario);
        var plan = guarda.exigirConfirmable(id);

        var pedidos = leer(plan.plugins(), new TypeReference<List<PluginPedido>>() {});
        var verificado = verificacion.verificar(plan.core(), plan.versionCore(), pedidos);
        PlanServicio.rechazaSiNoCompatible(verificado.veredicto());

        var resumen = leer(plan.resumen(), new TypeReference<Resumen>() {});
        if (!resumenCatalogo.armar(plan.pais(), verificado.seleccion()).equals(resumen)) {
            throw new ExcepcionApi(HttpStatus.UNPROCESSABLE_ENTITY, new ErrorApi(CodigosDespliegue.PLAN_DESACTUALIZADO,
                    MotivosDespliegue.PLAN_DESACTUALIZADO_MENSAJE, List.of()));
        }

        if (!kubernetes.disponible()) {
            throw new ExcepcionApi(HttpStatus.SERVICE_UNAVAILABLE, new ErrorApi(
                    CodigosDespliegue.KUBERNETES_NO_DISPONIBLE, MotivosDespliegue.KUBERNETES_APLICAR_MENSAJE, List.of()));
        }
        var objetos = kubernetes.construir(new PlanPais(plan.pais(), resumen.imagen(), resumen.plugins().stream()
                .map(p -> new PlanPais.PluginPlan(p.nombre(), p.version(), p.sha256())).toList()));

        var plugins = resumen.plugins().stream().map(p -> new PluginDesplegado(p.nombre(), p.version(), p.sha256(),
                catalogo.obtenerPlugin(plan.core(), p.nombre(), p.version()).etiquetas())).toList();
        var confirmadoEn = reserva.reservar(plan, confirmadoPor, texto(VeredictoDto.de(verificado.veredicto())), plugins);

        try {
            kubernetes.aplicar(objetos);
        } catch (AplicacionFallidaException e) {
            fallido(plan.id(), e.getMessage());
            throw new ExcepcionApi(HttpStatus.BAD_GATEWAY, new ErrorApi(CodigosDespliegue.APLICACION_FALLIDA,
                    MotivosDespliegue.APLICACION_FALLIDA_MENSAJE, List.of()));
        } catch (RuntimeException e) {
            fallido(plan.id(), e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
            throw e;
        }
        return new Confirmado(plan, confirmadoPor, confirmadoEn);
    }

    private void fallido(long id, String eventos) {
        registro.actualizarResultado(id, EstadoDespliegue.FALLIDO, new ContextoFallo("", "", eventos));
    }

    private <T> T leer(String texto, TypeReference<T> tipo) {
        try {
            return json.readValue(texto, tipo);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("El plan guardado no se puede leer", e);
        }
    }

    private String texto(Object valor) {
        try {
            return json.writeValueAsString(valor);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudo escribir el JSON del veredicto", e);
        }
    }
}
