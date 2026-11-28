package io.synkra.plataforma;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** El único reloj de la aplicación: cualquier módulo lo inyecta como {@link Clock}; las pruebas lo reemplazan. */
@Configuration("configuracionReloj")
class Reloj {

    @Bean
    Clock reloj() {
        return Clock.systemUTC();
    }
}
