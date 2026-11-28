package io.synkra.despliegue;

import java.net.URI;
import io.synkra.registro.api.EstadoDespliegue;
import java.time.Instant;
import java.util.Locale;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/** H-12 (R-D5 a R-D9): confirma un plan pendiente. 202 con el despliegue registrado en {@code aplicando}. */
@RestController
class RutasConfirmacion {

    private static final String RUTA_DESPLIEGUES = "/api/despliegues";

    record ConfirmacionDto(long id, String pais, String estado, String creadoPor, String confirmadoPor,
            Instant confirmadoEn) {}

    private final ConfirmacionServicio confirmacion;

    RutasConfirmacion(ConfirmacionServicio confirmacion) {
        this.confirmacion = confirmacion;
    }

    @PostMapping(RUTA_DESPLIEGUES + "/{id}/confirmacion")
    ResponseEntity<ConfirmacionDto> confirmar(@PathVariable long id,
            @RequestHeader(name = Usuario.CABECERA, required = false) String usuario) {
        var c = confirmacion.confirmar(id, usuario);
        var plan = c.plan();
        return ResponseEntity.accepted().location(URI.create(RUTA_DESPLIEGUES + "/" + plan.id()))
                .body(new ConfirmacionDto(plan.id(), plan.pais(),
                        EstadoDespliegue.APLICANDO.name().toLowerCase(Locale.ROOT), plan.creadoPor(), c.confirmadoPor(),
                        c.confirmadoEn()));
    }
}
