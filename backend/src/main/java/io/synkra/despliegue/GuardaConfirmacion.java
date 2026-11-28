package io.synkra.despliegue;

import io.synkra.plataforma.CodigosError;
import io.synkra.plataforma.ErrorApi;
import io.synkra.plataforma.ExcepcionApi;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * R-D5: un plan solo se confirma si sigue pendiente y vigente. El orden es contrato. No es {@code @Transactional}: el
 * {@code UPDATE} del vencimiento queda confirmado aunque después se lance el 410. No habla con el clúster.
 */
@Component
class GuardaConfirmacion {

    private final VencimientoPlanes vencimiento;
    private final PlanRepositorio planes;

    GuardaConfirmacion(VencimientoPlanes vencimiento, PlanRepositorio planes) {
        this.vencimiento = vencimiento;
        this.planes = planes;
    }

    Plan exigirConfirmable(long id) {
        if (id <= 0) {
            throw noEncontrado();
        }
        vencimiento.vencerSiToca(id);
        var plan = planes.findById(id).orElseThrow(GuardaConfirmacion::noEncontrado);
        return switch (plan.estado()) {
            case Plan.VENCIDO -> throw new ExcepcionApi(HttpStatus.GONE, new ErrorApi(
                    CodigosDespliegue.PLAN_VENCIDO, MotivosDespliegue.PLAN_VENCIDO_MENSAJE, List.of()));
            case Plan.CONFIRMADO -> throw new ExcepcionApi(HttpStatus.CONFLICT, new ErrorApi(
                    CodigosDespliegue.PLAN_NO_PENDIENTE, MotivosDespliegue.PLAN_NO_PENDIENTE_MENSAJE, List.of()));
            case Plan.PENDIENTE -> plan;
            default -> throw new IllegalStateException("Estado de plan desconocido: " + plan.estado());
        };
    }

    private static ExcepcionApi noEncontrado() {
        return new ExcepcionApi(HttpStatus.NOT_FOUND,
                new ErrorApi(CodigosError.NO_ENCONTRADO, CodigosError.MSG_NO_ENCONTRADO, List.of()));
    }
}
