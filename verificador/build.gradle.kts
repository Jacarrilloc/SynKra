plugins {
    `java-library`
    `java-test-fixtures`
}

repositories {
    mavenCentral()
}

dependencies {
    // Única dependencia de ejecución: lector YAML seguro.
    implementation("org.snakeyaml:snakeyaml-engine:3.2")

    // Solo pruebas.
    testImplementation(platform("org.junit:junit-bom:5.14.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("net.jqwik:jqwik:1.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
