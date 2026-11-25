package io.synkra.plataforma.prueba;

import java.util.List;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

public interface ElementoRepositorio
        extends ListCrudRepository<Elemento, Long>, PagingAndSortingRepository<Elemento, Long> {

    List<Elemento> findTop2ByOrderByXDesc();

    List<Elemento> findByTextoContaining(String parte);

    boolean existsByX(int x);
}
