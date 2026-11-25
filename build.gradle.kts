// Convenciones comunes de todo subproyecto Java de SynKra.
subprojects {
    plugins.withId("java") {
        extensions.configure<JavaPluginExtension> {
            toolchain {
                languageVersion.set(JavaLanguageVersion.of(21))
            }
        }
        tasks.withType<Test>().configureEach {
            useJUnitPlatform()
        }
    }
}
