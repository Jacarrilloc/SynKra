package io.synkra.despliegue;

import io.synkra.registro.api.PluginDesplegado;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Component;

/** Firma vacía (commit rojo de U07-D): la lógica llega después de las pruebas. */
@Component
class ReservaConfirmacion {

    Instant reservar(Plan plan, String confirmadoPor, String veredicto, List<PluginDesplegado> plugins) {
        throw new UnsupportedOperationException();
    }
}
