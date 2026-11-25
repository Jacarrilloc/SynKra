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
