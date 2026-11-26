package io.synkra.fronteras;

import org.springframework.modulith.Modulithic;

/**
 * Raíz de la aplicación de prueba: dos módulos, uno y dos. Solo para FronterasModulosTest.
 * Es @Modulithic y no @SpringBootApplication para no entrar como configuración en el
 * contexto de los @SpringBootTest del backend real.
 */
@Modulithic
public class AppFronteras {
}
