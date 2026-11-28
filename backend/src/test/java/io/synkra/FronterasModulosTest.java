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
import org.springframework.modulith.core.DependencyType;
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
                // getAllInvolvedRawTypes incluye los argumentos de tipo (Algo<ConfigMap>) y los límites de comodines.
                cls.getSuperclass().ifPresent(t -> revisa(m, cls.getName() + " (superclase)", t.getAllInvolvedRawTypes()));
                cls.getInterfaces().forEach(t -> revisa(m, cls.getName() + " (interfaz)", t.getAllInvolvedRawTypes()));
                cls.getTypeParameters().forEach(v -> v.getUpperBounds()
                        .forEach(b -> revisa(m, cls.getName() + " (límite de tipo)", b.getAllInvolvedRawTypes())));
                cls.getAnnotations().forEach(a -> revisa(m, cls.getName() + " (anotación)", List.of(a.getRawType())));
                for (JavaCodeUnit u : cls.getCodeUnits()) {
                    if (!u.getModifiers().contains(JavaModifier.PUBLIC) && !u.getModifiers().contains(JavaModifier.PROTECTED)) {
                        continue;
                    }
                    revisa(m, u.getFullName(), u.getReturnType().getAllInvolvedRawTypes());
                    for (var p : u.getParameterTypes()) {
                        revisa(m, u.getFullName(), p.getAllInvolvedRawTypes());
                    }
                    u.getThrowsClause().getTypes().forEach(t -> revisa(m, u.getFullName() + " (throws)", t.getAllInvolvedRawTypes()));
                    u.getTypeParameters().forEach(v -> v.getUpperBounds()
                            .forEach(b -> revisa(m, u.getFullName() + " (límite de tipo)", b.getAllInvolvedRawTypes())));
                    u.getAnnotations().forEach(a -> revisa(m, u.getFullName() + " (anotación)", List.of(a.getRawType())));
                }
                for (JavaField f : cls.getFields()) {
                    if (f.getModifiers().contains(JavaModifier.PUBLIC) || f.getModifiers().contains(JavaModifier.PROTECTED)) {
                        revisa(m, f.getFullName(), f.getType().getAllInvolvedRawTypes());
                        f.getAnnotations().forEach(a -> revisa(m, f.getFullName() + " (anotación)", List.of(a.getRawType())));
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
                .hasMessageContaining("ApiSuperGenerica (superclase)")
                .hasMessageContaining("ApiInterfazGenerica (interfaz)")
                .hasMessageContaining("ApiThrows.lanza")
                .hasMessageContaining("(throws)")
                .hasMessageContaining("ApiLimite (límite de tipo)")
                .hasMessageContaining("ApiAnotada (anotación)")
                .hasMessageNotContaining("ApiLimpia");
    }

    // ---- U06-T07: las fronteras del módulo registro ----

    private static final String API_REGISTRO = "io.synkra.registro.api..";

    /** Un tipo de io.synkra fuera de la API de registro, o de io.fabric8. */
    private static boolean ajenoALaApiDeRegistro(JavaClass t) {
        String paquete = t.getPackageName();
        boolean propio = paquete.equals("io.synkra.registro.api") || paquete.startsWith("io.synkra.registro.api.");
        return (paquete.startsWith("io.synkra") && !propio) || paquete.startsWith("io.fabric8");
    }

    private static void revisaAjenos(List<String> mencionados, String donde, Iterable<JavaClass> tipos) {
        for (JavaClass t : tipos) {
            if (ajenoALaApiDeRegistro(t)) {
                mencionados.add(donde + " menciona " + t.getName());
            }
        }
    }

    /** Las firmas públicas de la API de registro solo mencionan tipos de su propio paquete (y del JDK). */
    private static ArchRule laApiDeRegistroSoloMencionaTipos(String paqueteApi) {
        ArchCondition<JavaClass> condicion = new ArchCondition<>("mencionar solo tipos propios en su API pública") {
            @Override
            public void check(JavaClass cls, ConditionEvents eventos) {
                if (!cls.getModifiers().contains(JavaModifier.PUBLIC)) {
                    return;
                }
                List<String> m = new ArrayList<>();
                cls.getSuperclass().ifPresent(t -> revisaAjenos(m, cls.getName() + " (superclase)", t.getAllInvolvedRawTypes()));
                cls.getInterfaces().forEach(t -> revisaAjenos(m, cls.getName() + " (interfaz)", t.getAllInvolvedRawTypes()));
                for (JavaCodeUnit u : cls.getCodeUnits()) {
                    if (!u.getModifiers().contains(JavaModifier.PUBLIC) && !u.getModifiers().contains(JavaModifier.PROTECTED)) {
                        continue;
                    }
                    revisaAjenos(m, u.getFullName(), u.getReturnType().getAllInvolvedRawTypes());
                    for (var p : u.getParameterTypes()) {
                        revisaAjenos(m, u.getFullName(), p.getAllInvolvedRawTypes());
                    }
                    u.getThrowsClause().getTypes().forEach(t -> revisaAjenos(m, u.getFullName() + " (throws)", t.getAllInvolvedRawTypes()));
                }
                for (JavaField f : cls.getFields()) {
                    // En un record, cada componente es un campo privado: también cuenta.
                    if (cls.isRecord() || f.getModifiers().contains(JavaModifier.PUBLIC)
                            || f.getModifiers().contains(JavaModifier.PROTECTED)) {
                        revisaAjenos(m, f.getFullName(), f.getType().getAllInvolvedRawTypes());
                    }
                }
                m.forEach(texto -> eventos.add(SimpleConditionEvent.violated(cls, texto)));
            }
        };
        return ArchRuleDefinition.classes().that().resideInAPackage(paqueteApi).should(condicion);
    }

    @Test
    void laApiDeRegistroSoloMencionaTiposPropios() {
        laApiDeRegistroSoloMencionaTipos(API_REGISTRO).check(produccion());
    }

    private static final List<String> ANOTACIONES_DE_ESCRITURA = List.of(
            "org.springframework.web.bind.annotation.PostMapping",
            "org.springframework.web.bind.annotation.PutMapping",
            "org.springframework.web.bind.annotation.PatchMapping",
            "org.springframework.web.bind.annotation.DeleteMapping");
    private static final List<String> VERBOS_DE_ESCRITURA = List.of("POST", "PUT", "PATCH", "DELETE");

    private static boolean escribe(com.tngtech.archunit.core.domain.JavaAnnotation<?> a) {
        String tipo = a.getRawType().getName();
        if (ANOTACIONES_DE_ESCRITURA.contains(tipo)) {
            return true;
        }
        if (!tipo.equals("org.springframework.web.bind.annotation.RequestMapping")) {
            return false;
        }
        for (Object verbo : a.get("method").map(v -> v instanceof Object[] arr ? arr : new Object[] {v}).orElse(new Object[0])) {
            String nombre = verbo instanceof com.tngtech.archunit.core.domain.JavaEnumConstant e ? e.name() : String.valueOf(verbo);
            if (VERBOS_DE_ESCRITURA.contains(nombre)) {
                return true;
            }
        }
        return false;
    }

    /** Ningún método de las clases del paquete lleva una anotación de ruta de escritura. */
    private static ArchRule sinRutasDeEscritura(String paquete) {
        ArchCondition<JavaClass> condicion = new ArchCondition<>("no tener métodos con rutas de escritura") {
            @Override
            public void check(JavaClass cls, ConditionEvents eventos) {
                for (var metodo : cls.getMethods()) {
                    for (var a : metodo.getAnnotations()) {
                        if (escribe(a)) {
                            eventos.add(SimpleConditionEvent.violated(cls,
                                    metodo.getFullName() + " lleva @" + a.getRawType().getSimpleName()));
                        }
                    }
                }
            }
        };
        return ArchRuleDefinition.classes().that().resideInAPackage(paquete).should(condicion);
    }

    @Test
    void registroNoTieneRutasDeEscritura() {
        sinRutasDeEscritura("io.synkra.registro..").check(produccion());
    }

    @Test
    void registroSoloDependeDeKubernetesApiYPlataforma() {
        var modulos = backendReal();
        var registro = modulos.getModuleByName("registro").orElseThrow();
        var destinos = registro.getDependencies(modulos, DependencyType.values()).uniqueModules()
                .map(m -> m.getIdentifier().toString()).sorted().toList();
        assertThat(destinos).containsExactly("kubernetes", "plataforma");
    }
}
