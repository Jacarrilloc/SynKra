package io.synkra.registro;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

/** Fila de {@code reg_despliegue}. El id lo da quien registra. Fechas: epoch en ms (C-05). */
@Table("reg_despliegue")
record Despliegue(
        @Id Long id,
        String pais,
        String tipo,
        Long origenId,
        String core,
        String versionCore,
        String imagen,
        String creadoPor,
        String confirmadoPor,
        long confirmadoEn,
        String estado,
        Long terminadoEn,
        String veredicto) {}
