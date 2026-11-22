plugins {
    `java-library`
    `java-test-fixtures`
}

repositories {
    mavenCentral()
}

dependencies {
    // Única dependencia de ejecución: lector YAML seguro.
    implementation("org.snakeyaml:snakeyaml-engine:2.10")

    // Solo pruebas.
    testImplementation(platform("org.junit:junit-bom:5.13.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("net.jqwik:jqwik:1.9.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
