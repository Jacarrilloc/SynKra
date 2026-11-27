package io.synkra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tngtech.archunit.core.importer.ImportOption;
import io.synkra.fronteras.AppFronteras;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.core.Violations;
import org.springframework.modulith.docs.Documenter;

class FronterasModulosTest {

    private static ApplicationModules backendReal() {
        return ApplicationModules.of(SynkraApp.class);
    }

    @Test
    void elBackendRealRespetaSusFronteras() {
        backendReal().verify();
    }

    @Test
    void generaLaDocumentacionDeModulos() throws Exception {
        var documenter = new Documenter(backendReal());
        documenter.writeModulesAsPlantUml();
        documenter.writeModuleCanvases();
        // Documenter escribe en build/spring-modulith-docs relativo al directorio de trabajo de la
        // prueba, que Gradle fija en backend/ (carpeta por defecto de Documenter).
        assertThat(Files.readString(Path.of("build/spring-modulith-docs/module-plataforma.adoc")))
                .contains("plataforma");
    }

    @Test
    void verifyFallaCuandoUnModuloImportaUnPaqueteInternoDeOtro() {
        // Modulith, por defecto, no importa clases de prueba (DoNotIncludeTests): la
        // aplicación de prueba solo es visible con una opción que sí las incluya.
        var modulos = ApplicationModules.of(AppFronteras.class, (ImportOption) location -> true);
        assertThatThrownBy(modulos::verify)
                .isInstanceOf(Violations.class)
                .hasMessageContaining("dos.interno");
    }

    /** C-36: verificador es una biblioteca compartida, no un módulo con fronteras propias. */
    @Test
    void verificadorEsModuloCompartido() {
        var compartidos = backendReal().getSharedModules().stream().map(m -> m.getIdentifier().toString()).toList();
        assertThat(compartidos).containsExactly("verificador");
    }

    @Test
    void laDocumentacionGeneradaNoSeVersiona() throws Exception {
        var p = new ProcessBuilder("git", "check-ignore", "-q", "backend/build/spring-modulith-docs/module-plataforma.adoc")
                .directory(Path.of("..").toFile()).start();
        assertThat(p.waitFor()).as("git check-ignore (0 = ignorado)").isZero();
    }
}
