package io.synkra.despliegue;

import java.util.List;

/** R-D3: lo que se va a desplegar, con la imagen por digest y los plugins con su sha256. */
record Resumen(String namespace, String core, String versionCore, String imagen, List<PluginResumen> plugins) {}
