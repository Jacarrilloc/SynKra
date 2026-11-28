package io.synkra.plataforma;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** U07-T06, decisión 2: la aplicación tiene un solo bean {@link Clock}, en UTC y con la hora real. */
@SpringBootTest
class RelojTest {

    @TempDir
    static Path datos;

    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry r) {
        r.add("synkra.data", () -> datos.toString());
    }

    @Autowired ApplicationContext contexto;

    @Test
    void hayExactamenteUnBeanClockEnUtcConLaHoraReal() {
        var relojes = contexto.getBeansOfType(Clock.class);

        assertThat(relojes).hasSize(1);
        var reloj = relojes.values().iterator().next();
        assertThat(reloj.getZone()).isEqualTo(ZoneOffset.UTC);
        assertThat(Duration.between(reloj.instant(), Instant.now()).abs()).isLessThan(Duration.ofSeconds(5));
    }
}
