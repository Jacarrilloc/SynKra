plugins {
    java
    id("org.springframework.boot") version "3.5.16"
    id("io.spring.dependency-management") version "1.1.7"
}

// D4: la versión del core se compila; synkra-core.properties la lleva dentro del JAR.
version = "2.1.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation(project(":plugin-api"))
    // VersionCore devuelve io.synkra.verificador.Version (U02-T04 la usará para verificar).
    implementation(project(":verificador"))

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.processResources {
    val v = project.version.toString()
    inputs.property("version", v)
    doLast {
        val f = destinationDir.resolve("synkra-core.properties")
        f.writeText("version=$v\n")
    }
}

// D4: variable de entorno real en la JVM de prueba; VersionCoreTest comprueba que no cambia la versión.
tasks.test {
    environment("SYNKRA_CORE_VERSION", "9.9.9")
}
