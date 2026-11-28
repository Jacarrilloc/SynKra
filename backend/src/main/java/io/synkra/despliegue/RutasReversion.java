package io.synkra.despliegue;

import io.synkra.despliegue.RutasPlanes.PlanDto;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * H-20 (R-D11): revertir a un despliegue {@code listo} es crear un plan nuevo de tipo {@code reversion}. Sin cuerpo
 * (si llega uno, se ignora) y sin {@code Location}: el plan no está en el registro hasta que se confirme.
 */
@RestController
class RutasReversion {

    private final PlanServicio planes;

    RutasReversion(PlanServicio planes) {
        this.planes = planes;
    }

    @PostMapping("/api/despliegues/{id}/reversion")
    @ResponseStatus(HttpStatus.CREATED)
    PlanDto revertir(@PathVariable long id,
            @RequestHeader(name = Usuario.CABECERA, required = false) String usuario) {
        var creadoPor = Usuario.de(usuario);
        return PlanDto.de(planes.prepararReversion(id, creadoPor));
    }
}
