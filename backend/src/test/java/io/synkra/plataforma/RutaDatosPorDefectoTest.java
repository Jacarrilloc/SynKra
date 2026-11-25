package io.synkra.plataforma;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.env.PropertiesPropertySourceLoader;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;

/** Resuelve la propiedad sin arrancar el DataSource: no toca /data. */
class RutaDatosPorDefectoTest {

    @Test
    void sinSynkraDataLaUrlEsDataSynkraDb() throws Exception {
        var env = new StandardEnvironment();
        env.getPropertySources().remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
        env.getPropertySources().remove(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);
        env.getPropertySources().addLast(new MapPropertySource("vacio", java.util.Map.of()));
        for (var ps : new PropertiesPropertySourceLoader()
                .load("application", new ClassPathResource("application.properties"))) {
            env.getPropertySources().addLast(ps);
        }
        String ruta = env.getProperty("synkra.data");
        assertThat(ruta).isEqualTo("/data");
        assertThat(BaseDeDatos.urlJdbc(ruta)).isEqualTo("jdbc:sqlite:/data/synkra.db");
    }
}
