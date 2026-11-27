package io.synkra.catalogo;

import java.util.List;
import java.util.Optional;
import org.springframework.data.repository.ListCrudRepository;

interface VersionCoreRepositorio extends ListCrudRepository<VersionCore, String> {

    List<VersionCore> findByCore(String core);

    Optional<VersionCore> findByCoreAndVersion(String core, String version);
}
