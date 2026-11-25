package io.synkra.plataforma;

import static org.assertj.core.api.Assertions.assertThat;

import com.zaxxer.hikari.HikariDataSource;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.DynamicPropertyRegistry;

@SpringBootTest
class BaseDeDatosTest {

    @TempDir
    static Path datos;

    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry r) {
        r.add("synkra.data", () -> datos.resolve("sub").toString());
    }

    @Autowired DataSource dataSource;
    @Autowired JdbcTemplate jdbc;

    @Test
    void creaSynkraDbEnElDirectorioConfigurado() {
        assertThat(Files.isRegularFile(datos.resolve("sub").resolve("synkra.db"))).isTrue();
    }

    @Test
    void flywayAplicoLaVersion1() {
        String version = jdbc.queryForObject(
                "select version from flyway_schema_history where success = 1", String.class);
        assertThat(version).isEqualTo("1");
    }

    @Test
    void elPoolTieneUnaSolaConexion() {
        assertThat(dataSource).isInstanceOf(HikariDataSource.class);
        assertThat(((HikariDataSource) dataSource).getMaximumPoolSize()).isEqualTo(1);
    }
}
