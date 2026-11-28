package io.synkra.registro;

import io.synkra.kubernetes.api.LecturaPais;
import io.synkra.registro.api.DespliegueRegistrado;
import java.util.Optional;

/** R-R6. Firma vacía del rojo. */
final class Concordancia {

    private Concordancia() {}

    static boolean concuerda(Optional<DespliegueRegistrado> ultimo, Optional<LecturaPais> leido) {
        throw new UnsupportedOperationException("pendiente");
    }
}
