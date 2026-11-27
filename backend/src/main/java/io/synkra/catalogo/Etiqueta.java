package io.synkra.catalogo;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

/** Clave real (core, plugin, version, clave); ver {@link VersionCore}. */
@Table("cat_etiqueta")
record Etiqueta(@Id String core, String plugin, String version, String clave, String valor, String origen) {}
