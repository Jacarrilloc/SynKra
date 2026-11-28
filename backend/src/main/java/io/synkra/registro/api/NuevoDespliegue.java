package io.synkra.registro.api;

import java.time.Instant;
import java.util.List;

/**
 * Lo que U07 entrega al confirmar. {@code id} es el id del plan (mayor que 0). {@code veredicto} es JSON.
 * {@code plugins} nulo cuenta como lista vacía.
 */
public record NuevoDespliegue(
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
        String veredicto,
        List<PluginDesplegado> plugins) {}
