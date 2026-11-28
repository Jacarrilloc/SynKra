package io.synkra.despliegue;

import java.util.List;
import java.util.Optional;
import org.springframework.data.repository.Repository;

/** Solo lectura: las altas van por {@code JdbcAggregateTemplate.insert} (sin save ni delete). */
interface PlanRepositorio extends Repository<Plan, Long> {

    Optional<Plan> findById(Long id);

    List<Plan> findByPaisAndEstado(String pais, String estado);
}
