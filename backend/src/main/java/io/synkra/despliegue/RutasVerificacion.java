package io.synkra.despliegue;

import java.util.List;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** H-09 y H-08 (R-D2): verificar una selección sin crear nada. 200 también con {@code RECHAZADA} (RF-11). */
@RestController
@RequestMapping("/api/verificaciones")
class RutasVerificacion {

    /** Sin {@code @Valid}: {@code VerificacionVivo} valida y devuelve todos los detalles juntos. */
    record SolicitudVerificacion(String core, String versionCore, List<PluginPedido> plugins) {}

    private final VerificacionVivo verificacion;

    RutasVerificacion(VerificacionVivo verificacion) {
        this.verificacion = verificacion;
    }

    @PostMapping
    VeredictoDto verificar(@RequestBody SolicitudVerificacion s) {
        return VeredictoDto.de(verificacion.verificar(s.core(), s.versionCore(), s.plugins()).veredicto());
    }
}
