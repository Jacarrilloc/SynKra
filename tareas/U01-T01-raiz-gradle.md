# U01-T01: Raíz de Gradle

**Unidad:** U01: `Verificador y suite`
**Historias que implementa:** H-07
**Depende de:** ninguna (es la primera tarea del repositorio)

---

## Alcance

**Dentro** (una línea, concreta):

> La raíz del build Gradle multi-proyecto: `settings.gradle.kts`, `build.gradle.kts` con las convenciones comunes (Java 21, JUnit 5), el *wrapper* fijado con su checksum, `.gitignore` del build y un `verificador/build.gradle.kts` mínimo para que el proyecto exista.

**Fuera** (lo que el codificador debe rechazar aunque lo vea roto):

- Dependencias del verificador (SnakeYAML Engine, jqwik) y `java-test-fixtures`: son de U01-T02.
- Cualquier clase Java, en `verificador/` o en otro lado.
- El proyecto `backend/` y su `include`: son de U03-T01.
- Plugins de Spring Boot, Kotlin, calidad de código o publicación.
- Cambios en `Makefile`, `scripts/` o `aidlc-docs/`.

---

## Archivos de contexto

- `unidades-y-tareas.md` (sección U01)
- `aidlc-docs/construction/plans/U01-verificador-code-generation-plan.md`
- `aidlc-docs/inception/application-design/unit-of-work.md` (sección "Organización del código")

---

## Criterios de aceptación

- [ ] **CA-1**: el build reconoce el proyecto `:verificador`
  ```bash
  ./gradlew -q projects | grep -F "':verificador'"
  ```
  Esperado: una línea con `Project ':verificador'`.

- [ ] **CA-2**: Gradle corre sobre Java 21
  ```bash
  ./gradlew --version | grep -E 'JVM: +21\.'
  ```
  Esperado: al menos una línea con la JVM 21.

- [ ] **CA-3**: el *wrapper* está fijado a una versión y verifica su checksum (suelo Linux del módulo 1)
  ```bash
  grep -E '^distributionUrl=.*gradle-[0-9.]+-bin\.zip$' gradle/wrapper/gradle-wrapper.properties
  grep -E '^distributionSha256Sum=[0-9a-f]{64}$' gradle/wrapper/gradle-wrapper.properties
  ```
  Esperado: las dos líneas presentes.

- [ ] **CA-4**: las convenciones comunes fijan Java 21 y JUnit 5 para todo subproyecto Java
  ```bash
  grep -n 'JavaLanguageVersion.of(21)' build.gradle.kts
  grep -n 'useJUnitPlatform()' build.gradle.kts
  ./gradlew :verificador:build
  ```
  Esperado: las dos líneas encontradas y `BUILD SUCCESSFUL`.

- [ ] **CA-5**: los artefactos del build no se versionan
  ```bash
  git check-ignore build/ .gradle/ verificador/build/
  ```
  Esperado: imprime las tres rutas.

---

## Plan de pruebas

Esta tarea no tiene lógica: sus pruebas son los cinco criterios.

**Rojo primero:** antes de crear los archivos, corre CA-1 y CA-5 y registra que fallan (no hay
`gradlew` ni `.gitignore`). Después los mismos comandos, pasando.

---

## Notas

- La versión de Gradle es la estable más reciente compatible con Java 21 al momento de generar el
  *wrapper*. El checksum sale de la página oficial de la distribución, no se calcula a mano.
- Si la máquina no tiene Gradle ni Java 21 para generar el *wrapper*, eso es un **bloqueo** que se
  reporta; no se baja un binario sin checksum.
- `verificador/build.gradle.kts` queda con `plugins { `java-library` }` y nada más. U01-T02 lo
  modifica en su sitio.
- Paquete base del código Java del proyecto: `io.synkra` (decisión de la preparación del loop).
