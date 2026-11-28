package io.synkra.registro;

import io.synkra.kubernetes.api.LecturaPais;
import io.synkra.registro.api.DespliegueRegistrado;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * R-R6: lo que corre en el clúster es lo que el registro dice que se desplegó por última vez. Los dos vacíos
 * concuerdan; uno sin el otro, no. Con los dos presentes: la imagen es igual como texto (incluye el digest) y el
 * conjunto de ternas (nombre, versión, sha256) de los plugins es el mismo, sin importar el orden.
 * Desviación anotada: la versión del core no se compara aparte (el digest ya identifica el build).
 */
final class Concordancia {

    private record Terna(String nombre, String version, String sha256) {}

    private Concordancia() {}

    static boolean concuerda(Optional<DespliegueRegistrado> ultimo, Optional<LecturaPais> leido) {
        if (ultimo.isEmpty() || leido.isEmpty()) {
            return ultimo.isEmpty() && leido.isEmpty();
        }
        var registrado = ultimo.get();
        var enClaster = leido.get();
        if (!registrado.imagen().equals(enClaster.imagenCore())) {
            return false;
        }
        Set<Terna> esperados = new HashSet<>();
        registrado.plugins().forEach(p -> esperados.add(new Terna(p.nombre(), p.version(), p.sha256())));
        Set<Terna> corriendo = new HashSet<>();
        enClaster.plugins().forEach(p -> corriendo.add(new Terna(p.nombre(), p.version(), p.sha256())));
        return esperados.equals(corriendo);
    }
}
