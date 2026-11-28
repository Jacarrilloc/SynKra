package io.synkra.registro;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

/** Fila de {@code reg_contexto}: los tres textos ya truncados (R-R4). */
@Table("reg_contexto")
record ContextoGuardado(@Id long despliegueId, String logCore, String logInit, String eventos) {}
