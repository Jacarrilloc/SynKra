plugins {
    java
    id("org.springframework.boot") version "3.5.16"
    id("io.spring.dependency-management") version "1.1.7"
}

version = "0.1.0"

repositories {
    mavenCentral()
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.modulith:spring-modulith-bom:1.4.14")
    }
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-data-jdbc")
    implementation(project(":verificador"))
    implementation("org.springframework.modulith:spring-modulith-api")
    implementation("org.xerial:sqlite-jdbc")
    implementation("org.flywaydb:flyway-core")
    implementation("org.apache.commons:commons-compress:1.27.1")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation(testFixtures(project(":verificador")))
    // Solo los fixtures de C-07 usan @Valid y @Min; main no valida con Bean Validation.
    testImplementation("org.springframework.boot:spring-boot-starter-validation")
    testImplementation("org.springframework.modulith:spring-modulith-starter-test")
    // PBT-03 (U04-T05): la misma versión que verificador.
    testImplementation("net.jqwik:jqwik:1.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
