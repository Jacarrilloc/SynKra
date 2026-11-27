package io.synkra.core;

import static org.assertj.core.api.Assertions.assertThat;

import io.synkra.verificador.Version;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class VersionCoreTest {

    private static final Version ESPERADA = new Version(2, 1, 0);

    @Autowired
    VersionCore versionCore;

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
        // Una variable de entorno entra al contexto como propiedad (systemEnvironment); se simula igual.
        assertThat(versionConPropiedad("SYNKRA_CORE_VERSION=9.9.9", "synkra.core.version=9.9.9"))
                .isEqualTo(ESPERADA);
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
