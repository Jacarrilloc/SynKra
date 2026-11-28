package io.synkra.despliegue;

import java.time.Clock;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Q6: paso de {@code pendiente} a {@code vencido} cuando llega {@code vence_en} (a {@code vence_en} ya no está
 * vigente). La guarda vive en el {@code WHERE}: nunca se lee y luego se escribe. Solo toca planes pendientes.
 */
@Component
class VencimientoPlanes {

    private static final String SQL = "UPDATE dep_plan SET estado = '" + Plan.VENCIDO + "' WHERE estado = '" + Plan.PENDIENTE + "' AND vence_en <= ?";

    private final JdbcTemplate jdbc;
    private final Clock reloj;

    VencimientoPlanes(JdbcTemplate jdbc, Clock reloj) {
        this.jdbc = jdbc;
        this.reloj = reloj;
    }

    /** Vence todos los pendientes cuyo {@code vence_en} ya llegó; devuelve cuántos. Nadie lo llama aún (decisión 6). */
    @Transactional
    int vencerPendientes() {
        return jdbc.update(SQL, reloj.instant().toEpochMilli());
    }

    /** Vence el plan {@code id} si está pendiente y ya llegó su {@code vence_en}; devuelve si cambió la fila. */
    @Transactional
    boolean vencerSiToca(long id) {
        return jdbc.update(SQL + " AND id = ?", reloj.instant().toEpochMilli(), id) == 1;
    }
}
