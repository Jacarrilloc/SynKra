package io.synkra.despliegue;

import io.synkra.kubernetes.api.LecturaPais;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

/**
 * R-D4: cambios entre lo que corre y la selección. Un plugin es la terna (nombre, versión, sha256) y se compara
 * entera: si cambia la versión o el hash, sale en {@code quitados} (lo viejo) y en {@code anadidos} (lo nuevo).
 */
final class CalculoCambios {

    private static final Comparator<PluginResumen> ORDEN = Comparator.comparing(PluginResumen::nombre)
            .thenComparing(PluginResumen::version).thenComparing(PluginResumen::sha256);

    private CalculoCambios() {
    }

    static Cambios calcular(Optional<LecturaPais> corre, String imagen, List<PluginResumen> seleccion) {
        var actuales = new LinkedHashSet<PluginResumen>();
        corre.ifPresent(c -> c.plugins().forEach(p -> actuales.add(new PluginResumen(p.nombre(), p.version(), p.sha256()))));
        var elegidos = new LinkedHashSet<>(seleccion);

        var anadidos = new LinkedHashSet<>(elegidos);
        anadidos.removeAll(actuales);
        var quitados = new LinkedHashSet<>(actuales);
        quitados.removeAll(elegidos);
        var iguales = new LinkedHashSet<>(elegidos);
        iguales.retainAll(actuales);

        var antes = corre.map(LecturaPais::imagenCore).orElse(null);
        var core = new CambioCore(antes, imagen, imagen.equals(antes));
        return new Cambios(core, ordenada(anadidos), ordenada(quitados), ordenada(iguales),
                core.igual() && anadidos.isEmpty() && quitados.isEmpty());
    }

    private static List<PluginResumen> ordenada(java.util.Collection<PluginResumen> plugins) {
        return plugins.stream().sorted(ORDEN).toList();
    }
}
