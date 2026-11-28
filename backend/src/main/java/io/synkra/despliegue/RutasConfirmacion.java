package io.synkra.despliegue;

import java.net.URI;
import java.time.Instant;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/** H-12 (R-D5 a R-D9): confirma un plan pendiente. 202 con el despliegue registrado en {@code aplicando}. */
@RestController
class RutasConfirmacion {

    record ConfirmacionDto(long id, String pais, String estado, String creadoPor, String confirmadoPor,
            Instant confirmadoEn) {}

    private final ConfirmacionServicio confirmacion;

    RutasConfirmacion(ConfirmacionServicio confirmacion) {
        this.confirmacion = confirmacion;
    }

    @PostMapping("/api/despliegues/{id}/confirmacion")
    ResponseEntity<ConfirmacionDto> confirmar(@PathVariable long id,
            @RequestHeader(name = Usuario.CABECERA, required = false) String usuario) {
        var c = confirmacion.confirmar(id, usuario);
        var plan = c.plan();
        return ResponseEntity.accepted().location(URI.create("/api/despliegues/" + plan.id()))
                .body(new ConfirmacionDto(plan.id(), plan.pais(), "aplicando", plan.creadoPor(), c.confirmadoPor(),
                        c.confirmadoEn()));
    }
}
