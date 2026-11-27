package io.synkra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaCodeUnit;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;
import java.util.ArrayList;
import java.util.List;
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

    // ---- U05-T09: la frontera del módulo kubernetes (C-70, C-73) ----

    private static final String FABRIC8 = "io.fabric8..";

    /** Solo el código de producción del backend (sin clases de prueba ni juguetes). */
    private static JavaClasses produccion() {
        return new ClassFileImporter().withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("io.synkra");
    }

    private static JavaClasses juguetes(String paquete) {
        return new ClassFileImporter().withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_JARS)
                .importPackages(paquete);
    }

    /** Ninguna clase fuera de {@code io.synkra.kubernetes..} depende de {@code io.fabric8..}. */
    private static ArchRule soloKubernetesHablaConFabric8() {
        return ArchRuleDefinition.noClasses().that().resideOutsideOfPackage("io.synkra.kubernetes..")
                .should().dependOnClassesThat().resideInAPackage(FABRIC8)
                .as("ninguna clase fuera de io.synkra.kubernetes depende de io.fabric8");
    }

    private static void revisa(List<String> mencionados, String donde, Iterable<JavaClass> tipos) {
        for (JavaClass t : tipos) {
            if (t.getPackageName().startsWith("io.fabric8")) {
                mencionados.add(donde + " menciona " + t.getName());
            }
        }
    }

    /** Ninguna firma pública (métodos, constructores, campos y supertipos) de las clases del paquete menciona Fabric8. */
    private static ArchRule laApiNoExponeFabric8(String paqueteApi) {
        ArchCondition<JavaClass> condicion = new ArchCondition<>("no mencionar io.fabric8 en su API pública") {
            @Override
            public void check(JavaClass cls, ConditionEvents eventos) {
                if (!cls.getModifiers().contains(JavaModifier.PUBLIC)) {
                    return;
                }
                List<String> m = new ArrayList<>();
                revisa(m, cls.getName() + " (superclase)", cls.getRawSuperclass().stream()
                        .flatMap(t -> t.getAllInvolvedRawTypes().stream()).toList());
                revisa(m, cls.getName() + " (interfaces)", cls.getRawInterfaces().stream()
                        .flatMap(t -> t.getAllInvolvedRawTypes().stream()).toList());
                for (JavaCodeUnit u : cls.getCodeUnits()) {
                    if (!u.getModifiers().contains(JavaModifier.PUBLIC) && !u.getModifiers().contains(JavaModifier.PROTECTED)) {
                        continue;
                    }
                    revisa(m, u.getFullName(), u.getReturnType().getAllInvolvedRawTypes());
                    for (var p : u.getParameterTypes()) {
                        revisa(m, u.getFullName(), p.getAllInvolvedRawTypes());
                    }
                }
                for (JavaField f : cls.getFields()) {
                    if (f.getModifiers().contains(JavaModifier.PUBLIC) || f.getModifiers().contains(JavaModifier.PROTECTED)) {
                        revisa(m, f.getFullName(), f.getType().getAllInvolvedRawTypes());
                    }
                }
                m.forEach(texto -> eventos.add(SimpleConditionEvent.violated(cls, texto)));
            }
        };
        return ArchRuleDefinition.classes().that().resideInAPackage(paqueteApi).should(condicion);
    }

    @Test
    void soloElModuloKubernetesImportaFabric8() {
        soloKubernetesHablaConFabric8().check(produccion());
    }

    @Test
    void laReglaDeFabric8FueraDeKubernetesMuerdeConUnaClaseDeJuguete() {
        assertThatThrownBy(() -> soloKubernetesHablaConFabric8().check(juguetes("io.synkra.juguetek8s")))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("FueraUsaFabric8")
                .hasMessageContaining("io.fabric8");
    }

    @Test
    void laApiPublicaDeKubernetesNoMencionaFabric8() {
        laApiNoExponeFabric8("io.synkra.kubernetes.api..").check(produccion());
    }

    @Test
    void laReglaDeApiMuerdeConUnaFirmaPublicaConFabric8() {
        assertThatThrownBy(() -> laApiNoExponeFabric8("io.synkra.juguetek8s.api..").check(juguetes("io.synkra.juguetek8s")))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("ApiFiltraFabric8.directo")
                .hasMessageContaining("ApiFiltraFabric8.genericoEnParametro")
                .hasMessageNotContaining("ApiLimpia");
    }
}
