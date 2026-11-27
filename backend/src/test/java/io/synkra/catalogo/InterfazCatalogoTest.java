package io.synkra.catalogo;

import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import io.synkra.SynkraApp;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

/** {@code catalogo.api} es la única interfaz visible desde otros módulos. */
class InterfazCatalogoTest {

    private static com.tngtech.archunit.core.domain.JavaClasses clasesDelModulo() {
        return new ClassFileImporter().withImportOption(new ImportOption.DoNotIncludeTests())
                .importPackages("io.synkra.catalogo");
    }

    @Test
    void catalogoApiEsInterfazConNombre() {
        var modulo = ApplicationModules.of(SynkraApp.class).getModuleByName("catalogo").orElseThrow();
        var nombres = modulo.getNamedInterfaces().stream().map(i -> i.getName()).toList();
        assertThat(nombres).contains("api");
    }

    @Test
    void ningunTipoPublicoFueraDeApi() {
        var publicos = clasesDelModulo().stream()
                .filter(c -> c.getPackageName().equals("io.synkra.catalogo"))
                .filter(c -> c.getModifiers().contains(JavaModifier.PUBLIC))
                .map(c -> c.getName()).toList();
        assertThat(publicos).as("tipos públicos en el paquete base de catalogo").isEmpty();
    }

    @Test
    void laInterfazSinNombreNoExponeTipos() {
        var modulo = ApplicationModules.of(SynkraApp.class).getModuleByName("catalogo").orElseThrow();
        var sinNombre = modulo.getNamedInterfaces().stream().filter(i -> i.isUnnamed()).toList();
        assertThat(sinNombre).allSatisfy(i -> assertThat(i.asJavaClasses()
                .filter(c -> c.getModifiers().contains(JavaModifier.PUBLIC)).toList()).isEmpty());
    }
}
