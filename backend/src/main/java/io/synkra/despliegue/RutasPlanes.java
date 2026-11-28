package io.synkra.despliegue;

import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * H-11 (R-D3): crear un plan de despliegue. Solo {@code POST}: el {@code GET} de {@code /api/despliegues/**} es de
 * {@code registro}, por eso la respuesta no lleva {@code Location}.
 */
@RestController
@RequestMapping("/api/despliegues")
class RutasPlanes {

    /** Sin {@code @Valid}: {@code PlanServicio} valida y devuelve todos los detalles juntos. */
    record SolicitudPlan(String pais, String core, String versionCore, List<PluginPedido> plugins) {}

    record PlanDto(Long id, String pais, String tipo, String estado, String core, String versionCore, String creadoPor,
            Instant creadoEn, Instant venceEn, Resumen resumen, Cambios cambios) {

        static PlanDto de(PlanPreparado p) {
            var plan = p.plan();
            return new PlanDto(plan.id(), plan.pais(), plan.tipo(), plan.estado(), plan.core(), plan.versionCore(),
                    plan.creadoPor(), Instant.ofEpochMilli(plan.creadoEn()), Instant.ofEpochMilli(plan.venceEn()),
                    p.resumen(), p.cambios());
        }
    }

    private final PlanServicio planes;

    RutasPlanes(PlanServicio planes) {
        this.planes = planes;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    PlanDto crear(@RequestBody SolicitudPlan s) {
        return PlanDto.de(planes.prepararPlan(s.pais(), s.core(), s.versionCore(), s.plugins()));
    }
}
