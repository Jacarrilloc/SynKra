package io.synkra.plataforma;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.sql.DataSource;
import org.springframework.data.relational.core.dialect.Dialect;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** SQLite en {@code $SYNKRA_DATA/synkra.db} con un solo escritor (RNF-07). */
@Configuration
public class BaseDeDatos {

    /** Única fuente de la ruta del archivo de la base. */
    public static Path archivo(String directorio) {
        return Path.of(directorio, "synkra.db");
    }

    public static String urlJdbc(String directorio) {
        return "jdbc:sqlite:" + archivo(directorio);
    }

    @Bean
    DataSource dataSource(@Value("${synkra.data}") String directorio) {
        try {
            Files.createDirectories(Path.of(directorio));
        } catch (IOException | RuntimeException e) {
            throw new IllegalStateException(
                    "No se pudo crear el directorio de datos SYNKRA_DATA: " + directorio, e);
        }
        var config = new HikariConfig();
        config.setJdbcUrl(urlJdbc(directorio));
        config.setMaximumPoolSize(1);
        config.setPoolName("synkra-sqlite");
        return new HikariDataSource(config);
    }

    /** Spring Data JDBC no trae dialecto para SQLite; se usa el propio. */
    @Bean
    Dialect jdbcDialect() {
        return DialectoSqlite.INSTANCE;
    }
}
