package io.synkra.despliegue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.synkra.plataforma.ErrorApi;
import io.synkra.plataforma.ExcepcionApi;
import io.synkra.registro.api.EstadoDespliegue;
import io.synkra.registro.api.NuevoDespliegue;
import io.synkra.registro.api.PluginDesplegado;
import io.synkra.registro.api.RegistroApi;
import io.synkra.registro.api.TipoDespliegue;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * R-D5 y R-D7 en una sola transacción: pasa el plan a {@code confirmado} (la guarda vive en el {@code WHERE}),
 * comprueba que el país no tenga un despliegue aplicándose y lo registra en {@code aplicando}. Cualquier excepción
 * revierte los tres pasos. Con el pool de una conexión, dos confirmaciones del mismo país se serializan aquí.
 */
@Component
class ReservaConfirmacion {

    private static final String CONFIRMAR = "UPDATE dep_plan SET estado = 'confirmado' WHERE id = ? AND estado = 'pendiente' AND vence_en > ?";

    private final JdbcTemplate jdbc;
    private final PlanRepositorio planes;
    private final RegistroApi registro;
    private final ObjectMapper json;
    private final Clock reloj;

    ReservaConfirmacion(JdbcTemplate jdbc, PlanRepositorio planes, RegistroApi registro, ObjectMapper json,
            Clock reloj) {
        this.jdbc = jdbc;
        this.planes = planes;
        this.registro = registro;
        this.json = json;
        this.reloj = reloj;
    }

    /** Devuelve el {@code confirmadoEn} que registró. */
    @Transactional
    Instant reservar(Plan plan, String confirmadoPor, String veredicto, List<PluginDesplegado> plugins) {
        var ahora = reloj.instant();
        int cambiadas = jdbc.update(CONFIRMAR, plan.id(), ahora.toEpochMilli());
        if (cambiadas == 0) {
            var actual = planes.findById(plan.id());
            if (actual.isPresent() && Plan.PENDIENTE.equals(actual.get().estado())) {
                throw error(HttpStatus.GONE, CodigosDespliegue.PLAN_VENCIDO, MotivosDespliegue.PLAN_VENCIDO_MENSAJE);
            }
            throw error(HttpStatus.CONFLICT, CodigosDespliegue.PLAN_NO_PENDIENTE,
                    MotivosDespliegue.PLAN_NO_PENDIENTE_MENSAJE);
        }
        if (registro.historial(plan.pais()).stream().anyMatch(d -> d.estado() == EstadoDespliegue.APLICANDO)) {
            throw error(HttpStatus.CONFLICT, CodigosDespliegue.DESPLIEGUE_EN_CURSO,
                    MotivosDespliegue.DESPLIEGUE_EN_CURSO_MENSAJE);
        }
        registro.registrar(new NuevoDespliegue(plan.id(), plan.pais(),
                TipoDespliegue.valueOf(plan.tipo().toUpperCase(Locale.ROOT)), plan.origenId(), plan.core(),
                plan.versionCore(), imagenDe(plan), plan.creadoPor(), confirmadoPor, ahora, veredicto, plugins));
        return ahora;
    }

    private String imagenDe(Plan plan) {
        try {
            return json.readValue(plan.resumen(), Resumen.class).imagen();
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("El resumen guardado del plan no se puede leer", e);
        }
    }

    private static ExcepcionApi error(HttpStatus estado, String codigo, String mensaje) {
        return new ExcepcionApi(estado, new ErrorApi(codigo, mensaje, List.of()));
    }
}
