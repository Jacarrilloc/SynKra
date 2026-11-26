package io.synkra.plataforma;

import java.nio.file.Files;
import java.nio.file.Path;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Componente {@code db} de /actuator/health. Sustituye al indicador por defecto: con la conexión
 * única del pool ya abierta, SQLite sigue respondiendo aunque el archivo se haya borrado, así que
 * además de la consulta se comprueba que {@code synkra.db} exista y se pueda escribir.
 */
@Component("dbHealthIndicator")
class SaludBaseDeDatos implements HealthIndicator {

    private final DataSource dataSource;
    private final Path archivo;

    SaludBaseDeDatos(DataSource dataSource, @Value("${synkra.data}") String directorio) {
        this.dataSource = dataSource;
        this.archivo = BaseDeDatos.archivo(directorio);
    }

    @Override
    public Health health() {
        if (!Files.isRegularFile(archivo) || !Files.isWritable(archivo)) {
            return Health.down().withDetail("error", "synkra.db no existe o no es escribible").build();
        }
        try (var c = dataSource.getConnection(); var s = c.createStatement()) {
            s.execute("SELECT 1");
            return Health.up().withDetail("database", "SQLite").build();
        } catch (Exception e) {
            return Health.down(e).build();
        }
    }
}
