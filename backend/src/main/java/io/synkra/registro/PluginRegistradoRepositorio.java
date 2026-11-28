package io.synkra.registro;

import java.util.List;
import org.springframework.data.repository.Repository;

/** Solo lectura: las altas van por {@code JdbcAggregateTemplate.insert} (sin save ni delete). */
interface PluginRegistradoRepositorio extends Repository<PluginRegistrado, Long> {

    List<PluginRegistrado> findAll();

    List<PluginRegistrado> findByDespliegueId(long despliegueId);
}
