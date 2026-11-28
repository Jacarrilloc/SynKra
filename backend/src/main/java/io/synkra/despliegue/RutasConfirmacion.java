package io.synkra.despliegue;

import io.synkra.plataforma.ErrorApi;
import io.synkra.plataforma.ExcepcionApi;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * R-D5 a medias a propósito: da los 404, 409 y 410 de la guarda y, si el plan es confirmable, 501. U07-D reemplaza
 * ese 501 por la confirmación de verdad. Nunca aplica nada ni habla con el clúster (R-D13).
 */
@RestController
class RutasConfirmacion {

    private final GuardaConfirmacion guarda;

    RutasConfirmacion(GuardaConfirmacion guarda) {
        this.guarda = guarda;
    }

    @PostMapping("/api/despliegues/{id}/confirmacion")
    void confirmar(@PathVariable long id) {
        guarda.exigirConfirmable(id);
        throw new ExcepcionApi(HttpStatus.NOT_IMPLEMENTED, new ErrorApi(CodigosDespliegue.CONFIRMACION_NO_DISPONIBLE,
                MotivosDespliegue.CONFIRMACION_NO_DISPONIBLE_MENSAJE, List.of()));
    }
}
