plugins {
    `java-library`
}

// Versión del contrato; el JAR la lleva en Implementation-Version.
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    // Sin dependencias de ejecución: el contrato solo usa la JDK.
    testImplementation(platform("org.junit:junit-bom:6.1.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.jar {
    manifest {
        attributes("Implementation-Version" to project.version)
    }
}
