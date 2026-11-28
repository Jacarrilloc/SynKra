package io.synkra.registro;

import java.util.List;
import java.util.Optional;
import org.springframework.data.repository.Repository;

/** Solo lectura: las altas van por {@code JdbcAggregateTemplate.insert} (sin save ni delete). */
interface ContextoRepositorio extends Repository<ContextoGuardado, Long> {

    Optional<ContextoGuardado> findByDespliegueId(long despliegueId);

    List<ContextoGuardado> findAll();
}
