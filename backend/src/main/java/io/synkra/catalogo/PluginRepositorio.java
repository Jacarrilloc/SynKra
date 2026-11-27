package io.synkra.catalogo;

import java.util.List;
import java.util.Optional;
import org.springframework.data.repository.ListCrudRepository;

interface PluginRepositorio extends ListCrudRepository<Plugin, String> {

    List<Plugin> findByCore(String core);

    Optional<Plugin> findByCoreAndNombreAndVersion(String core, String nombre, String version);

    List<Plugin> findBySha256(String sha256);
}
