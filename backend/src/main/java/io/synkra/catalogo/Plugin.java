package io.synkra.catalogo;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

/** Clave real (core, nombre, version); ver {@link VersionCore}. {@code subidoEn}: epoch en ms (C-05). */
@Table("cat_plugin")
record Plugin(
        @Id String core,
        String nombre,
        String version,
        String coreCompatible,
        String sha256,
        long tamano,
        String ruta,
        String descripcion,
        String subidoPor,
        long subidoEn) {}
