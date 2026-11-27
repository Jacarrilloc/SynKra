package io.synkra.core;

import static org.assertj.core.api.Assertions.assertThat;

import io.synkra.verificador.Version;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class VersionCoreTest {

    private static final Version ESPERADA = new Version(2, 1, 0);

    @Autowired
    VersionCore versionCore;

    @Autowired
    TestRestTemplate rest;

    @Test
    void daLaVersionCompilada() {
        assertThat(versionCore.version()).isEqualTo(ESPERADA);
    }

    @Test
    void ignoraLaPropiedadVersion() {
        assertThat(versionConPropiedad("version=9.9.9")).isEqualTo(ESPERADA);
    }

    @Test
    void ignoraLaVariableDeEntornoSynkraCoreVersion() {
        // Precondición: la variable es real (la fija tasks.test en core/build.gradle.kts), no una propiedad.
        assertThat(System.getenv("SYNKRA_CORE_VERSION")).isEqualTo("9.9.9");
        assertThat(versionConPropiedad()).isEqualTo(ESPERADA);
    }

    @Test
    void healthRespondeUp() {
        var cuerpo = rest.getForObject("/actuator/health", String.class);
        assertThat(cuerpo).contains("\"status\":\"UP\"");
    }

    private static Version versionConPropiedad(String... propiedades) {
        try (ConfigurableApplicationContext ctx = new SpringApplicationBuilder(SynkraCoreApplication.class)
                .web(WebApplicationType.NONE)
                .properties(propiedades)
                .run()) {
            return ctx.getBean(VersionCore.class).version();
        }
    }
}
