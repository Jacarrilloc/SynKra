package io.synkra;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
class SynkraAppTest {

    @TempDir
    static Path datos;

    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry r) {
        r.add("synkra.data", () -> datos.toString());
    }

    @Autowired
    ApplicationContext contexto;

    @Test
    void elContextoCarga() {
    }

    @Test
    void laAppDePruebaDeFronterasNoEntraAlContextoReal() {
        assertThat(contexto.getBeanDefinitionNames()).noneMatch(n -> n.toLowerCase().contains("fronteras"));
    }
}
