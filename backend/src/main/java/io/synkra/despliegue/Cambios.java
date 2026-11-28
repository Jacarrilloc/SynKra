package io.synkra.despliegue;

import java.util.List;

/** R-D4: lo que cambia frente a lo que corre. Las listas van ordenadas por nombre y versión. */
record Cambios(CambioCore core, List<PluginResumen> anadidos, List<PluginResumen> quitados,
        List<PluginResumen> iguales, boolean sinCambios) {}
