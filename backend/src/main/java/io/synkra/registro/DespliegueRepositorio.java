package io.synkra.registro;

import java.util.List;
import java.util.Optional;
import org.springframework.data.repository.Repository;

/** Solo lectura: las altas van por {@code JdbcAggregateTemplate.insert} (sin save ni delete). */
interface DespliegueRepositorio extends Repository<Despliegue, Long> {

    Optional<Despliegue> findOneById(long id);

    List<Despliegue> findAll();

    List<Despliegue> findByPais(String pais);
}
