package io.synkra.registro.api;

import java.time.Instant;
import java.util.List;

/**
 * Un despliegue del registro, tal como se confirmó y terminó. {@code plugins} va ordenado por nombre y nunca es
 * nulo. {@code veredicto} es el JSON guardado, como texto. {@code contexto} solo es no nulo en {@code obtener} de
 * un {@code fallido}; en {@code historial} siempre es nulo. {@code terminadoEn} es nulo mientras está aplicando.
 */
public record DespliegueRegistrado(
        long id,
        String pais,
        TipoDespliegue tipo,
        Long origenId,
        String core,
        String versionCore,
        String imagen,
        String creadoPor,
        String confirmadoPor,
        Instant confirmadoEn,
        EstadoDespliegue estado,
        Instant terminadoEn,
        String veredicto,
        List<PluginDesplegado> plugins,
        ContextoFallo contexto) {}
