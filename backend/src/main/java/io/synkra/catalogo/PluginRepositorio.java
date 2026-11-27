package io.synkra.catalogo;

import java.util.List;
import java.util.Optional;
import org.springframework.data.repository.Repository;

/** Solo lectura: las altas van por {@code JdbcAggregateTemplate.insert} (sin save ni delete). */
interface PluginRepositorio extends Repository<Plugin, String> {

    List<Plugin> findAll();

    List<Plugin> findByCore(String core);

    Optional<Plugin> findByCoreAndNombreAndVersion(String core, String nombre, String version);

    List<Plugin> findBySha256(String sha256);
}
