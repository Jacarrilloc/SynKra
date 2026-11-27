package io.synkra.catalogo;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Clave real (core, version). Spring Data JDBC no maneja claves compuestas: {@code core} lleva
 * {@code @Id} solo para el mapeo; las altas usan {@code JdbcAggregateTemplate.insert} y las
 * lecturas, consultas derivadas por ambos campos. {@code registradoEn}: epoch en ms (C-05).
 */
@Table("cat_version_core")
record VersionCore(@Id String core, String version, String imagen, String registradoPor, long registradoEn) {}
