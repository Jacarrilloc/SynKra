package io.synkra.registro;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

/** Fila de {@code reg_plugin}; clave real (despliegue_id, nombre). {@code etiquetas}: JSON como texto. */
@Table("reg_plugin")
record PluginRegistrado(@Id long despliegueId, String nombre, String version, String sha256, String etiquetas) {}
