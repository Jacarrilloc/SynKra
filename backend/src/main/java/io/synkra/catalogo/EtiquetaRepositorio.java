package io.synkra.catalogo;

import java.util.List;
import org.springframework.data.repository.ListCrudRepository;

interface EtiquetaRepositorio extends ListCrudRepository<Etiqueta, String> {

    List<Etiqueta> findByCoreAndPluginAndVersion(String core, String plugin, String version);
}
