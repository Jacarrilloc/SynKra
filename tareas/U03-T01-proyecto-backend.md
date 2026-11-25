# U03-T01: Proyecto `backend/` (Spring Boot 3, Java 21)

**Unidad:** U03: `Plataforma del backend`
**Historias que implementa:** H-24 (base)
**Depende de:** U01-T01 (la raíz de Gradle)

---

## Alcance

**Dentro** (una línea, concreta):

> El proyecto `backend/` incluido en `settings.gradle.kts`, con Spring Boot 3, las dependencias de la plataforma y una clase `io.synkra.SynkraApp` que arranca, para que `bootJar` produzca un JAR ejecutable.

**Fuera** (lo que el codificador debe rechazar aunque lo vea roto):

- Configurar SQLite, `SYNKRA_DATA` o migraciones de Flyway: es de U03-T02. Aquí solo se declaran las dependencias, y la aplicación arranca sin base de datos (se excluye la autoconfiguración del `DataSource` o se arranca en una prueba sin ella).
- `ErrorApi` y el manejador de errores: U03-T03.
- Configuración de salud más allá de lo que trae actuator: U03-T04.
- La prueba de Spring Modulith: U03-T05. Aquí solo se declara la dependencia de pruebas.
- Dockerfile: U03-T06.
- Cualquier módulo de negocio (`catalogo`, `despliegue`…) y cualquier dependencia de `:verificador`.
- Cambios en el `build.gradle.kts` de la raíz o en `verificador/` (es de U01-T02, que va en la misma ola). La única línea que se toca fuera de `backend/` es el `include` de `settings.gradle.kts`.

---

## Archivos de contexto

- `unidades-y-tareas.md` (sección U03)
- `aidlc-docs/construction/plans/U03-plataforma-backend-code-generation-plan.md`
- `aidlc-docs/inception/application-design/application-design.md` (D1, D7 y D14)
- `aidlc-docs/inception/application-design/unit-of-work.md` (sección «Organización del código»)

---

## Criterios de aceptación

- [ ] **CA-1**: el build reconoce `:backend`
  ```bash
  ./gradlew -q projects | grep -F "':backend'"
  ```
  Esperado: una línea con `Project ':backend'`.

- [ ] **CA-2**: `bootJar` produce un JAR con `SynkraApp` como clase de arranque (lectura de vuelta del JAR)
  ```bash
  ./gradlew :backend:bootJar --rerun-tasks
  unzip -p backend/build/libs/backend-*.jar META-INF/MANIFEST.MF | grep -E '^(Start-Class|Spring-Boot-Version):'
  ```
  Esperado: `BUILD SUCCESSFUL`, `Start-Class: io.synkra.SynkraApp` y una `Spring-Boot-Version` que empieza por `3.`.

- [ ] **CA-3**: están las dependencias de la plataforma
  ```bash
  ./gradlew -q :backend:dependencies --configuration runtimeClasspath | grep -oE 'spring-boot-starter-(web|actuator|data-jdbc)|org\.xerial:sqlite-jdbc|org\.flywaydb:flyway-core' | sort -u
  ./gradlew -q :backend:dependencies --configuration testRuntimeClasspath | grep -c 'org.springframework.modulith'
  ```
  Esperado: las cinco líneas en la primera; `1` o más en la segunda.

- [ ] **CA-4**: el contexto de Spring arranca de verdad, en una prueba que no se saltó
  ```bash
  ./gradlew :backend:test --rerun --tests '*SynkraAppTest'
  grep -ho 'tests="[0-9]*" skipped="[0-9]*" failures="[0-9]*" errors="[0-9]*"' backend/build/test-results/test/TEST-*SynkraAppTest.xml
  ```
  Esperado: `BUILD SUCCESSFUL` y `tests="1" skipped="0" failures="0" errors="0"`.

- [ ] **CA-5**: el bytecode del backend es de Java 21
  ```bash
  javap -v backend/build/classes/java/main/io/synkra/SynkraApp.class | grep 'major version'
  ```
  Esperado: `major version: 65`.

---

## Plan de pruebas

- `SynkraAppTest` con `@SpringBootTest` que solo comprueba que el contexto carga.

**Rojo primero:** escribe `SynkraAppTest` antes que `SynkraApp`, corre CA-4 y registra que falla.
Después, el mismo comando pasando.

---

## Notas

- Spring Boot 3, la última versión 3.x publicada, con el plugin `io.spring.dependency-management` o
  la plataforma de Boot. Spring Modulith en la línea compatible con esa versión de Boot.
- Java 21 sale del toolchain de la raíz. No se vuelve a declarar en `backend/`.
- Paquete base: `io.synkra`; la plataforma vive en `io.synkra.plataforma`.
