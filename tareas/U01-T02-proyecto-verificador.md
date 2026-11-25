# U01-T02: Proyecto `verificador/` sin dependencias de framework

**Unidad:** U01: `Verificador y suite`
**Historias que implementa:** H-07
**Depende de:** U01-T01

---

## Alcance

**Dentro** (una línea, concreta):

> `verificador/build.gradle.kts` con `java-library` y `java-test-fixtures`, SnakeYAML Engine como única dependencia de ejecución, y JUnit 5 y jqwik solo para pruebas.

**Fuera** (lo que el codificador debe rechazar aunque lo vea roto):

- Cualquier clase Java de producción o de prueba: empiezan en U01-T03.
- Cambios en `settings.gradle.kts` o en el `build.gradle.kts` de la raíz (salvo que un criterio falle por ellos: entonces se reporta, no se arregla aquí).
- Spring, Fabric8, Lombok o cualquier otra librería.
- El proyecto `backend/`.

---

## Archivos de contexto

- `unidades-y-tareas.md` (sección U01)
- `aidlc-docs/construction/plans/U01-verificador-code-generation-plan.md`
- `aidlc-docs/construction/U01-verificador/functional-design/business-logic-model.md` (por qué jqwik)

---

## Criterios de aceptación

- [ ] **CA-1**: el verificador no arrastra ningún framework en ejecución
  ```bash
  ./gradlew -q :verificador:dependencies --configuration runtimeClasspath | grep -ciE "spring|fabric8"
  ```
  Esperado: `0`.

- [ ] **CA-2**: SnakeYAML Engine está en ejecución, y jqwik no
  ```bash
  ./gradlew -q :verificador:dependencies --configuration runtimeClasspath | grep -c 'org.snakeyaml:snakeyaml-engine'
  ./gradlew -q :verificador:dependencies --configuration runtimeClasspath | grep -c 'net.jqwik'
  ```
  Esperado: `1` o más en la primera; `0` en la segunda.

- [ ] **CA-3**: JUnit 5 y jqwik están en las pruebas
  ```bash
  ./gradlew -q :verificador:dependencies --configuration testRuntimeClasspath | grep -cE 'net\.jqwik:jqwik|org\.junit\.jupiter'
  ```
  Esperado: `2` o más.

- [ ] **CA-4**: existen los *test fixtures* donde vivirá la suite (U01-T12)
  ```bash
  ./gradlew -q :verificador:tasks --all | grep -E '^testFixturesJar'
  ```
  Esperado: una línea con `testFixturesJar`.

- [ ] **CA-5**: el proyecto construye
  ```bash
  ./gradlew :verificador:check
  ```
  Esperado: `BUILD SUCCESSFUL`.

---

## Plan de pruebas

Sin código, las pruebas son los criterios.

**Rojo primero:** con el `verificador/build.gradle.kts` mínimo de U01-T01, corre CA-2 (primera
línea) y CA-4 y registra que fallan. Después, pasando.

---

## Notas

- Archivo que se **modifica en su sitio**: `verificador/build.gradle.kts`.
- Versiones fijas (sin `+` ni `latest`), las estables al momento de escribir la tarea.
- La raíz usa Gradle 9.9.2: `useJUnitPlatform()` exige declarar también
  `testRuntimeOnly("org.junit.platform:junit-platform-launcher")` en cuanto haya pruebas (candidata C-01 de U01-T01).
