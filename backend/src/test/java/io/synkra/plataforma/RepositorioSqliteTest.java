package io.synkra.plataforma;

import static org.assertj.core.api.Assertions.assertThat;

import io.synkra.plataforma.prueba.Elemento;
import io.synkra.plataforma.prueba.ElementoRepositorio;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** Spring Data JDBC de punta a punta contra SQLite con el dialecto propio. Esquema solo de pruebas. */
@SpringBootTest
class RepositorioSqliteTest {

    @TempDir
    static Path datos;

    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry r) {
        r.add("synkra.data", () -> datos.toString());
    }

    @Autowired ElementoRepositorio repo;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void esquema() {
        jdbc.execute("drop table if exists elemento");
        jdbc.execute("create table elemento (id integer primary key autoincrement, x integer not null,"
                + " texto text, activo integer, monto text)");
    }

    @Test
    void saveAllDevuelveLosIdsGenerados() {
        var guardados = repo.saveAll(List.of(Elemento.con(1), Elemento.con(2), Elemento.con(3)));
        assertThat(guardados).extracting(Elemento::id).doesNotContainNull().doesNotHaveDuplicates();
        assertThat(repo.findAll()).hasSize(3);
    }

    @Test
    void tiposComunesHacenViajeDeIdaYVuelta() {
        var g = repo.save(Elemento.con(2));
        var l = repo.findById(g.id()).orElseThrow();
        assertThat(l).usingRecursiveComparison().isEqualTo(g);
        assertThat(l.monto()).isEqualByComparingTo("12.50");
    }

    @Test
    void paginaYOrdena() {
        repo.saveAll(List.of(Elemento.con(1), Elemento.con(2), Elemento.con(3), Elemento.con(4), Elemento.con(5)));
        var pagina = repo.findAll(PageRequest.of(1, 2, Sort.by("x")));
        assertThat(pagina.getContent()).extracting(Elemento::x).containsExactly(3, 4);
        assertThat(pagina.getTotalElements()).isEqualTo(5);
        assertThat(repo.findTop2ByOrderByXDesc()).extracting(Elemento::x).containsExactly(5, 4);
    }

    @Test
    void consultasDerivadas() {
        repo.saveAll(List.of(Elemento.con(1), Elemento.con(2)));
        assertThat(repo.existsByX(2)).isTrue();
        assertThat(repo.existsByX(9)).isFalse();
        assertThat(repo.findByTextoContaining("t1")).hasSize(1);
    }
}
