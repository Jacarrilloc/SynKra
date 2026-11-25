# U01-T02: Proyecto `verificador/` sin dependencias de framework

**Unidad:** U01: `Verificador y suite`
**Historias que implementa:** H-07 (base)
**Depende de:** U01-T01

---

## Alcance

**Dentro** (una línea, concreta):

> `verificador/build.gradle.kts` con `java-library` y `java-test-fixtures`, SnakeYAML Engine como única dependencia de ejecución, y JUnit Jupiter, el *launcher* de JUnit Platform y jqwik solo para pruebas.

**Fuera** (lo que el codificador debe rechazar aunque lo vea roto):

- Cualquier clase Java de producción: empiezan en U01-T03. La única clase permitida es la prueba de humo de abajo.
- Cambios en `settings.gradle.kts` o en el `build.gradle.kts` de la raíz. Si un criterio falla por ellos, se reporta como bloqueo; no se arregla aquí.
- Spring, Fabric8, Lombok, AssertJ o cualquier otra librería.
- El proyecto `backend/` (es de U03-T01, que va en la misma ola).
- Mover la suite a `testFixtures` o darle dependencias a `testFixtures`: es de U01-T12.

---

## Archivos de contexto

- `unidades-y-tareas.md` (sección U01)
- `aidlc-docs/construction/plans/U01-verificador-code-generation-plan.md`
- `aidlc-docs/construction/U01-verificador/functional-design/business-logic-model.md` (por qué jqwik y por qué un YAML seguro)

---

## Criterios de aceptación

- [ ] **CA-1**: el verificador no arrastra ningún framework en ejecución
  ```bash
  ./gradlew -q :verificador:dependencies --configuration runtimeClasspath | grep -ciE "spring|fabric8"
  ```
  Esperado: `0`.

- [ ] **CA-2**: SnakeYAML Engine está en ejecución, y nada de pruebas se coló ahí
  ```bash
  ./gradlew -q :verificador:dependencies --configuration runtimeClasspath | grep -c 'org.snakeyaml:snakeyaml-engine'
  ./gradlew -q :verificador:dependencies --configuration runtimeClasspath | grep -ciE 'jqwik|junit'
  ```
  Esperado: `1` o más en la primera; `0` en la segunda.

- [ ] **CA-3**: JUnit Jupiter, el *launcher* y jqwik están en las pruebas
  ```bash
  ./gradlew -q :verificador:dependencies --configuration testRuntimeClasspath | grep -oE 'net\.jqwik:jqwik-engine|org\.junit\.jupiter:junit-jupiter-engine|org\.junit\.platform:junit-platform-launcher' | sort -u
  ```
  Esperado: las tres líneas.

- [ ] **CA-4**: las pruebas de verdad corren con los dos motores. Lectura de vuelta del reporte, no del código de salida
  ```bash
  ./gradlew :verificador:test --rerun
  grep -ho 'tests="[0-9]*" skipped="[0-9]*" failures="[0-9]*" errors="[0-9]*"' verificador/build/test-results/test/TEST-*.xml
  ```
  Esperado: `BUILD SUCCESSFUL` y un reporte con `tests="2" skipped="0" failures="0" errors="0"`: una
  prueba de JUnit (`@Test`) y una propiedad de jqwik (`@Property`) en `HumoMotoresTest`.

- [ ] **CA-5**: existen los *test fixtures* donde vivirá la suite (U01-T12)
  ```bash
  ./gradlew -q :verificador:tasks --all | grep -E '^testFixturesJar'
  ```
  Esperado: una línea con `testFixturesJar`.

---

## Plan de pruebas

- `verificador/src/test/java/io/synkra/verificador/HumoMotoresTest.java`: un `@Test` trivial y una
  `@Property` trivial. Su único fin es demostrar que los dos motores cargan. U01-T03 puede borrarla.

**Rojo primero:** escribe `HumoMotoresTest` antes de declarar las dependencias, corre CA-4 y registra
que no compila. Después, el mismo comando pasando.

---

## Notas

- Las versiones son las estables más recientes al generar la tarea. Escríbelas en el archivo de
  build (o en `gradle/libs.versions.toml`), nunca como rango ni `latest.release`.
- Con Gradle 9, `useJUnitPlatform()` falla con «Failed to load JUnit Platform» si no se declara el
  *launcher* en `testRuntimeOnly`.
- Se modifica en su sitio: `verificador/build.gradle.kts`.
