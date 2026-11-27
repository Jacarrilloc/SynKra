package io.synkra.catalogo;

import java.util.List;
import org.springframework.data.repository.Repository;

/** Solo lectura: las altas van por {@code JdbcAggregateTemplate.insert} (sin save ni delete). */
interface EtiquetaRepositorio extends Repository<Etiqueta, String> {

    List<Etiqueta> findAll();

    List<Etiqueta> findByCoreAndPluginAndVersion(String core, String plugin, String version);
}
