plugins {
    `java-library`
}

repositories {
    mavenCentral()
}

dependencies {
    // El core provee plugin-api en tiempo de ejecución; los JAR no la llevan dentro.
    compileOnly(project(":plugin-api"))

    testImplementation(project(":plugin-api"))
    // LectorDescriptor de U01, para dar por válido el descriptor de cada JAR construido.
    testImplementation(project(":verificador"))
    testImplementation(platform("org.junit:junit-bom:6.1.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

// Dos JAR desde el mismo código: (versión del plugin, rango de core compatible).
val variantes = mapOf("130" to ("1.3.0" to "2.x"), "200" to ("2.0.0" to "3.x"))

val jars = variantes.map { (id, par) ->
    val (version, rango) = par
    val descriptor = tasks.register<Copy>("descriptor$id") {
        from("src/main/descriptor/synkra-plugin.yaml")
        into(layout.buildDirectory.dir("descriptores/$version"))
        inputs.property("version", version)
        inputs.property("rango", rango)
        expand("version" to version, "coreCompatible" to rango)
    }
    tasks.register<Jar>("jar$id") {
        archiveBaseName.set("kyc-pe")
        archiveVersion.set(version)
        from(sourceSets.main.get().output.classesDirs)
        from(sourceSets.main.get().output.resourcesDir ?: layout.buildDirectory.dir("resources/main")) {
            include("META-INF/**")
        }
        from(descriptor)
        dependsOn(tasks.classes)
    }
}

// El jar por defecto de java-library no se publica: assemble produce solo los dos JAR de variante.
tasks.jar { enabled = false }
tasks.assemble { dependsOn(jars) }

tasks.test {
    dependsOn(jars)
    doFirst {
        systemProperty("kyc.jar130", tasks.named<Jar>("jar130").get().archiveFile.get().asFile.absolutePath)
        systemProperty("kyc.jar200", tasks.named<Jar>("jar200").get().archiveFile.get().asFile.absolutePath)
    }
}
