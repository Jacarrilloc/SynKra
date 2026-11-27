package io.synkra.catalogo;

import java.util.List;
import java.util.Optional;
import org.springframework.data.repository.Repository;

/** Solo lectura: las altas van por {@code JdbcAggregateTemplate.insert} (sin save ni delete). */
interface CoreRepositorio extends Repository<Core, String> {

    Optional<Core> findByNombre(String nombre);

    List<Core> findAll();
}
