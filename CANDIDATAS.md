# Tareas candidatas

Trabajo real que apareció durante el loop y quedó **fuera del alcance** de la tarea en curso. No se
hizo. Cada candidata dice de dónde salió y quién la propuso.

| # | Candidata | Salió de | Propuesta por | Decisión |
|---|---|---|---|---|
| C-01 | Declarar `testRuntimeOnly("org.junit.platform:junit-platform-launcher")`, que Gradle 9 exige en cuanto haya pruebas | Revisión de U01-T01, ronda 1 | Revisor | Va con U01-T02, que añade las dependencias de prueba |
| C-02 | Pasar las convenciones de `subprojects {}` a un plugin de convención (`buildSrc` o build incluido) | Revisión de U01-T01, F-02 (AMARILLO) | Revisor; el codificador lo empujó fuera de alcance | Aceptado fuera de alcance por el orquestador; se reconsidera en U03-T01 |
| C-03 | Fijar la JVM del daemon con `gradle/gradle-daemon-jvm.properties` | Revisión de U01-T01, F-03 (AMARILLO) | Revisor; el codificador lo empujó fuera de alcance | Aceptado fuera de alcance por el orquestador: CA-2 no lo pide y el toolchain ya fija Java 21 |
| C-05 | Revisar Spring Boot 3.5.x y dependency-management frente a Gradle 10/11 (deprecación de `Configuration.setVisible`) | Revisión de U03-T01, F-01 (AMARILLO) | Revisor | Pendiente |
| C-06 | Mover `repositories { mavenCentral() }` a la raíz en vez de repetirlo por proyecto | Bitácoras de U01-T02 y U03-T01 | Codificadores | Pendiente |
