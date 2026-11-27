package io.synkra.catalogo;

import static org.assertj.core.api.Assertions.assertThat;

import io.synkra.SynkraApp;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

/** {@code catalogo.api} es una interfaz con nombre: lo único visible desde otros módulos. */
class InterfazCatalogoTest {

    @Test
    void catalogoApiEsInterfazConNombre() {
        var modulo = ApplicationModules.of(SynkraApp.class).getModuleByName("catalogo").orElseThrow();
        var nombres = modulo.getNamedInterfaces().stream().map(i -> i.getName()).toList();
        assertThat(nombres).contains("api");
    }
}
