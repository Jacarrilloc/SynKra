package io.synkra.plataforma;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.core.env.StandardEnvironment;

/** Resuelve la propiedad sin arrancar el DataSource: no toca /data. */
class RutaDatosPorDefectoTest {

    @Test
    void sinSynkraDataLaUrlEsDataSynkraDb() {
        var env = new StandardEnvironment();
        env.getPropertySources().remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
        env.getPropertySources().remove(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);
        // Carga la configuracion como lo hace Spring Boot (properties, yml o perfiles).
        ConfigDataEnvironmentPostProcessor.applyTo(env);
        String ruta = env.getProperty("synkra.data");
        assertThat(ruta).isEqualTo("/data");
        assertThat(BaseDeDatos.urlJdbc(ruta)).isEqualTo("jdbc:sqlite:/data/synkra.db");
    }
}
