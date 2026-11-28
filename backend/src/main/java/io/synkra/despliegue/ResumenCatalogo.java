package io.synkra.despliegue;

import io.synkra.catalogo.api.CatalogoApi;
import io.synkra.verificador.Seleccion;
import java.util.Comparator;
import org.springframework.stereotype.Component;

/**
 * El resumen de lo que se va a desplegar (R-D3), armado con el catálogo de hoy: imagen por digest y sha256 de cada
 * plugin. Lo usan crear (para mostrarlo) y confirmar (para compararlo con lo que se mostró).
 */
@Component
class ResumenCatalogo {

    private final PaisPermitido paises;
    private final CatalogoApi catalogo;

    ResumenCatalogo(PaisPermitido paises, CatalogoApi catalogo) {
        this.paises = paises;
        this.catalogo = catalogo;
    }

    Resumen armar(String pais, Seleccion seleccion) {
        var namespace = paises.namespace(pais);
        var core = seleccion.core();
        var registrado = catalogo.obtenerCore(core);
        var imagen = registrado.imagenes().get(seleccion.versionCore());
        var elegidos = seleccion.plugins().stream()
                .map(d -> new PluginResumen(d.nombre(), d.version().toString(),
                        catalogo.obtenerPlugin(core, d.nombre(), d.version().toString()).sha256()))
                .sorted(Comparator.comparing(PluginResumen::nombre).thenComparing(PluginResumen::version))
                .toList();
        return new Resumen(namespace, core, seleccion.versionCore().toString(), imagen, elegidos);
    }
}
