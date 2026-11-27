plugins {
    `java-library`
    `java-test-fixtures`
}

repositories {
    mavenCentral()
}

dependencies {
    // Única dependencia de ejecución: YAML seguro, sin etiquetas que instancien clases.
    implementation("org.snakeyaml:snakeyaml-engine:3.2")

    // Solo pruebas.
    testImplementation(platform("org.junit:junit-bom:6.1.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("net.jqwik:jqwik:1.10.2")
    // Gradle 9 exige el launcher declarado explícitamente.
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

// H-10: la suite S1-S12 bloquea el build. `check` depende de verificarSuite, que lee el XML de
// SuiteCombinacionesTest y falla en cuatro casos, cada uno con un mensaje que nombra la causa:
//   1. el XML no existe;
//   2. tests < PRUEBAS_MINIMAS_SUITE;
//   3. skipped != 0;
//   4. el XML es más viejo que las clases compiladas de test o de testFixtures (no se compara con la
//      hora del build: un `test` UP-TO-DATE deja un XML válido y más viejo que el build).
//
// PRUEBAS_MINIMAS_SUITE = 17: son los 10 casos ejecutables de la suite (S1 a S10, el @ParameterizedTest;
// S11 y S12 son FueraDeU01 y no se ejecutan aquí) más las 7 pruebas de forma de SuiteCombinacionesTest
// (@Test). Si se añade o quita una prueba de SuiteCombinacionesTest, este número se cambia a propósito
// en el mismo cambio.
val pruebasMinimasSuite = 17
val xmlSuite = layout.buildDirectory.file("test-results/test/TEST-io.synkra.verificador.SuiteCombinacionesTest.xml")
val clasesCompiladas = files(sourceSets["test"].output.classesDirs, sourceSets["testFixtures"].output.classesDirs)

val verificarSuite by tasks.registering {
    group = "verification"
    description = "Falla si SuiteCombinacionesTest no corrió completa, o si su XML es de antes de un cambio."
    dependsOn(tasks.named("test"))
    inputs.files(clasesCompiladas)
    // Siempre se evalúa: depende de un archivo que puede borrarse o envejecer sin que cambien las entradas.
    outputs.upToDateWhen { false }
    val xml = xmlSuite.map { it.asFile }
    val minimo = pruebasMinimasSuite
    val clases = clasesCompiladas
    doLast {
        val f = xml.get()
        if (!f.isFile) {
            throw GradleException("verificarSuite: el XML de SuiteCombinacionesTest no existe: $f (la suite no corrió)")
        }
        val raiz = javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(f).documentElement
        val tests = raiz.getAttribute("tests").toIntOrNull()
            ?: throw GradleException("verificarSuite: el XML no trae el atributo tests: $f")
        val skipped = raiz.getAttribute("skipped").toIntOrNull()
            ?: throw GradleException("verificarSuite: el XML no trae el atributo skipped: $f")
        if (tests < minimo) {
            throw GradleException("verificarSuite: corrieron $tests pruebas de SuiteCombinacionesTest y se exigen al menos $minimo (10 casos S1-S10 más 7 de forma)")
        }
        if (skipped != 0) {
            throw GradleException("verificarSuite: $skipped pruebas de SuiteCombinacionesTest se saltaron (skipped debe ser 0)")
        }
        val ultimaClase = clases.asFileTree.files.maxOfOrNull { it.lastModified() } ?: 0L
        if (f.lastModified() < ultimaClase) {
            throw GradleException("verificarSuite: el XML de SuiteCombinacionesTest es más viejo que las clases compiladas de test o testFixtures (resultado de una corrida anterior a un cambio)")
        }
    }
}

tasks.named("check") { dependsOn(verificarSuite) }
