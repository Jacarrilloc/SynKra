import java.security.MessageDigest

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
// SuiteCombinacionesTest y falla, con un mensaje que nombra la causa, si:
//   1. el XML no existe;
//   2. tests < pruebasMinimasSuite;
//   3. skipped != 0;
//   4. failures o errors != 0 (por ejemplo con ignoreFailures = true);
//   5. el XML no corresponde a las clases actuales (o falta la huella).
//
// Frescura (desviación de la decisión 1, mtime -> contenido): al terminar, `test` escribe una huella
// (SHA-256 de las rutas relativas ordenadas y los bytes de las clases compiladas de main, testFixtures y
// test) en test-results/test/huella-suite.txt, salida declarada de `test`. verificarSuite recalcula la
// huella. Un `test` UP-TO-DATE conserva XML y huella; recompilar con el mismo bytecode (un comentario) no
// los invalida; cambiar cualquier clase sin volver a correr `test` sí.
//
// pruebasMinimasSuite = 17: son los 10 casos ejecutables de la suite (S1 a S10, el @ParameterizedTest;
// S11 y S12 son FueraDeU01 y no se ejecutan aquí) más las 7 pruebas de forma de SuiteCombinacionesTest
// (@Test). Si se añade o quita una prueba de SuiteCombinacionesTest, este número se cambia a propósito
// en el mismo cambio.
val pruebasMinimasSuite = 17
val xmlSuite = layout.buildDirectory.file("test-results/test/TEST-io.synkra.verificador.SuiteCombinacionesTest.xml")
val huellaSuite = layout.buildDirectory.file("test-results/test/huella-suite.txt")
val clasesDeLaSuite = listOf(
    files(sourceSets["main"].output.classesDirs),
    files(sourceSets["testFixtures"].output.classesDirs),
    files(sourceSets["test"].output.classesDirs),
)

// Sin referencias al proyecto: solo recibe colecciones de archivos (compatible con la caché de configuración).
val calcularHuella: (List<FileCollection>) -> String = { grupos ->
    val md = MessageDigest.getInstance("SHA-256")
    grupos.forEachIndexed { n, grupo ->
        val entradas = grupo.files.filter { it.isDirectory }.flatMap { raiz ->
            raiz.walkTopDown().filter { it.isFile }.map { it.relativeTo(raiz).invariantSeparatorsPath to it }.toList()
        }.sortedBy { it.first }
        for ((ruta, archivo) in entradas) {
            md.update("$n:$ruta\n".toByteArray())
            md.update(archivo.readBytes())
        }
    }
    md.digest().joinToString("") { "%02x".format(it) }
}

tasks.named<Test>("test") {
    val salida = huellaSuite.map { it.asFile }
    val grupos = clasesDeLaSuite
    val huella = calcularHuella
    outputs.file(huellaSuite)
    doLast {
        salida.get().writeText(huella(grupos) + "\n")
    }
}

tasks.register("verificarSuite") {
    group = "verification"
    description = "Falla si SuiteCombinacionesTest no corrió completa, con fallos, o si su XML no corresponde a las clases actuales."
    dependsOn(tasks.named("test"))
    inputs.files(clasesDeLaSuite)
    // Siempre se evalúa: depende de un archivo que puede borrarse sin que cambien las entradas.
    outputs.upToDateWhen { false }
    val xml = xmlSuite.map { it.asFile }
    val archivoHuella = huellaSuite.map { it.asFile }
    val minimo = pruebasMinimasSuite
    val grupos = clasesDeLaSuite
    val huella = calcularHuella
    doLast {
        val f = xml.get()
        if (!f.isFile) {
            throw GradleException("verificarSuite: el XML de SuiteCombinacionesTest no existe: $f (la suite no corrió)")
        }
        val raiz = javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(f).documentElement
        fun atributo(nombre: String) = raiz.getAttribute(nombre).toIntOrNull()
            ?: throw GradleException("verificarSuite: el XML no trae el atributo $nombre: $f")
        val tests = atributo("tests")
        val skipped = atributo("skipped")
        val failures = atributo("failures")
        val errors = atributo("errors")
        if (tests < minimo) {
            throw GradleException("verificarSuite: corrieron $tests pruebas de SuiteCombinacionesTest y se exigen al menos $minimo (los 10 casos S1-S10 más las pruebas de forma)")
        }
        if (skipped != 0) {
            throw GradleException("verificarSuite: $skipped pruebas de SuiteCombinacionesTest se saltaron (skipped debe ser 0)")
        }
        if (failures != 0 || errors != 0) {
            throw GradleException("verificarSuite: SuiteCombinacionesTest tiene failures=$failures y errors=$errors (deben ser 0)")
        }
        val h = archivoHuella.get()
        if (!h.isFile) {
            throw GradleException("verificarSuite: el XML no corresponde a las clases actuales: falta la huella $h")
        }
        if (h.readText().trim() != huella(grupos)) {
            throw GradleException("verificarSuite: el XML no corresponde a las clases actuales (la huella de main, testFixtures y test cambió desde que corrió test)")
        }
    }
}

tasks.named("check") { dependsOn("verificarSuite") }
