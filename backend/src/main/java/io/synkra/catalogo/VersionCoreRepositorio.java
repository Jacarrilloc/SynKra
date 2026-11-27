package io.synkra.catalogo;

import java.util.List;
import java.util.Optional;
import org.springframework.data.repository.Repository;

/** Solo lectura: las altas van por {@code JdbcAggregateTemplate.insert} (sin save ni delete). */
interface VersionCoreRepositorio extends Repository<VersionCore, String> {

    List<VersionCore> findAll();

    List<VersionCore> findByCore(String core);

    Optional<VersionCore> findByCoreAndVersion(String core, String version);
}
