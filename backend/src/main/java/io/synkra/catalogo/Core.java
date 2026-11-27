package io.synkra.catalogo;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("cat_core")
record Core(@Id String nombre) {}
