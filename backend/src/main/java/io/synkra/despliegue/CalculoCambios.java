package io.synkra.despliegue;

import io.synkra.kubernetes.api.LecturaPais;
import java.util.List;
import java.util.Optional;

/** R-D4: cambios entre lo que corre y la selección. */
final class CalculoCambios {

    private CalculoCambios() {
    }

    static Cambios calcular(Optional<LecturaPais> corre, String imagen, List<PluginResumen> seleccion) {
        throw new UnsupportedOperationException("pendiente");
    }
}
