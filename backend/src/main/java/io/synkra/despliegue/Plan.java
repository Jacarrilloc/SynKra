package io.synkra.despliegue;

import java.time.Duration;
import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Plan de despliegue o de reversión. {@code creadoEn} y {@code venceEn}: epoch en ms (C-05).
 * {@code origenId} apunta a un despliegue del registro (otro módulo) sin clave foránea.
 */
@Table("dep_plan")
record Plan(
        @Id Long id,
        String pais,
        String tipo,
        Long origenId,
        String core,
        String versionCore,
        String plugins,
        String resumen,
        String cambios,
        String estado,
        String creadoPor,
        long creadoEn,
        long venceEn) {

    /** Q6: un plan vive 30 minutos. */
    static final Duration VIGENCIA = Duration.ofMinutes(30);

    static final String PENDIENTE = "pendiente";
    static final String CONFIRMADO = "confirmado";
    static final String VENCIDO = "vencido";
    static final String DESPLIEGUE = "despliegue";
    static final String REVERSION = "reversion";

    static Plan nuevo(String pais, String tipo, Long origenId, String core, String versionCore, String plugins,
            String resumen, String cambios, String creadoPor, Instant creadoEn) {
        return new Plan(null, pais, tipo, origenId, core, versionCore, plugins, resumen, cambios, PENDIENTE,
                creadoPor, creadoEn.toEpochMilli(), creadoEn.plus(VIGENCIA).toEpochMilli());
    }
}
