# U01-T01: Raíz de Gradle

**Unidad:** U01: `Verificador y suite`
**Historias que implementa:** H-07 (base)
**Depende de:** ninguna. Es la primera tarea del repositorio y crea la raíz que usan U01 y U03.

---

## Alcance

**Dentro** (una línea, concreta):

> La raíz del build multi-proyecto: `settings.gradle.kts` que incluye `verificador`, `build.gradle.kts` con las convenciones comunes (toolchain Java 21, JUnit Platform), el *wrapper* de Gradle 9.9.2 con su checksum, la JVM del daemon fijada en 21, `.gitignore` del build y un `verificador/build.gradle.kts` mínimo.

**Fuera** (lo que el codificador debe rechazar aunque lo vea roto):

- Dependencias del verificador (SnakeYAML Engine, JUnit, jqwik) y `java-test-fixtures`: son de U01-T02.
- Cualquier clase Java, de producción o de prueba.
- El proyecto `backend/` y su `include`: son de U03-T01.
- Plugins de Spring Boot, Kotlin, calidad de código, publicación o un plugin de convención en `buildSrc`.
- Cambios en `Makefile`, `scripts/`, `aidlc-docs/` o `tareas/`.
- Instalar paquetes del sistema (un JDK 21, Gradle): la máquina solo tiene JDK 25 y Java 21 se provisiona desde Gradle.

---

## Archivos de contexto

- `unidades-y-tareas.md` (sección U01)
- `aidlc-docs/construction/plans/U01-verificador-code-generation-plan.md`
- `aidlc-docs/inception/application-design/unit-of-work.md` (sección «Organización del código»)

---

## Criterios de aceptación

- [ ] **CA-1**: el build reconoce el proyecto `:verificador`
  ```bash
  ./gradlew -q projects | grep -F "':verificador'"
  ```
  Esperado: una línea con `Project ':verificador'`.

- [ ] **CA-2**: el *wrapper* es el de Gradle 9.9.2 y verifica la distribución
  ```bash
  grep -E '^distributionUrl=.*gradle-9\.9\.2-bin\.zip$' gradle/wrapper/gradle-wrapper.properties
  grep -E '^distributionSha256Sum=[0-9a-f]{64}$' gradle/wrapper/gradle-wrapper.properties
  sha256sum gradle/wrapper/gradle-wrapper.jar
  ```
  Esperado: las dos líneas presentes. El `distributionSha256Sum` es igual al de
  `https://services.gradle.org/distributions/gradle-9.9.2-bin.zip.sha256`, y el sha256 del jar es igual
  al `wrapperChecksum` de 9.9.2 en `https://services.gradle.org/versions/all`. Lectura de vuelta
  contra la fuente oficial: no basta con que el archivo diga 9.9.2.

- [ ] **CA-3**: el daemon de Gradle corre sobre Java 21, aunque el JDK de la máquina sea 25
  ```bash
  ./gradlew --version | grep -E '^Daemon JVM:'
  for p in $(pgrep -f GradleDaemon); do "$(readlink -f /proc/$p/exe)" -version 2>&1 | head -1; done
  ```
  Esperado: la primera línea dice que el daemon es compatible con Java 21 y que el criterio sale de
  `gradle/gradle-daemon-jvm.properties`. El segundo comando (lectura de vuelta del proceso real)
  imprime al menos una versión `21.`.

- [ ] **CA-4**: las convenciones fijan Java 21 y JUnit Platform para todo subproyecto Java
  ```bash
  grep -n 'JavaLanguageVersion.of(21)' build.gradle.kts
  grep -n 'useJUnitPlatform()' build.gradle.kts
  ./gradlew :verificador:build --rerun-tasks
  ```
  Esperado: las dos líneas encontradas y `BUILD SUCCESSFUL`.

- [ ] **CA-5**: los artefactos del build no se versionan
  ```bash
  git check-ignore build/ .gradle/ verificador/build/ .kotlin/
  ```
  Esperado: imprime las cuatro rutas.

---

## Plan de pruebas

Esta tarea no tiene lógica: sus pruebas son los cinco criterios.

**Rojo primero:** antes de crear los archivos corre CA-1, CA-3 y CA-5 y registra que fallan (no hay
`gradlew`, ni daemon, ni `.gitignore`). Después, los mismos comandos pasando.

---

## Notas

- Para generar el *wrapper* no hay Gradle instalado. Hay distribuciones en `~/.gradle/wrapper/dists/`
  (9.4.1 a 9.6.1) que sirven para arrancar. **Después de generarlo, vuelve a correr
  `./gradlew wrapper --gradle-version 9.9.2 ...` con el propio wrapper**: es lo que reemplaza el jar y
  los scripts por los de 9.9.2. Si te saltas ese segundo paso, el jar queda de otra versión.
- La JVM del daemon se fija con la tarea `updateDaemonJvm` de Gradle (`--jvm-version=21`). El
  toolchain de compilación necesita el plugin `org.gradle.toolchains.foojay-resolver-convention` en
  `settings.gradle.kts` para poder descargar Java 21.
- `verificador/build.gradle.kts` queda con `plugins { `java-library` }` y nada más. U01-T02 lo
  modifica en su sitio.
- Paquete base del código Java del proyecto: `io.synkra`.
