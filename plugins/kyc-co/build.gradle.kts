plugins {
    `java-library`
}

// La versión del plugin: el descriptor la toma de aquí para que no se desalineen.
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    // El core provee plugin-api en tiempo de ejecución; el JAR no la lleva dentro.
    compileOnly(project(":plugin-api"))

    testImplementation(project(":plugin-api"))
    // LectorDescriptor de U01, para dar por válido el descriptor del JAR construido.
    testImplementation(project(":verificador"))
    testImplementation(platform("org.junit:junit-bom:6.1.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.processResources {
    val v = project.version.toString()
    inputs.property("version", v)
    filesMatching("synkra-plugin.yaml") {
        expand("version" to v)
    }
}

tasks.jar {
    archiveBaseName.set("kyc-co")
}

// La prueba del descriptor lee el JAR construido.
tasks.test {
    dependsOn(tasks.jar)
    val jar = tasks.jar.flatMap { it.archiveFile }
    doFirst { systemProperty("kyc.jar", jar.get().asFile.absolutePath) }
}
