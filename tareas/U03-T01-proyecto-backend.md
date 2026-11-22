# U03-T01: Proyecto `backend/` (Spring Boot 3, Java 21)

**Unidad:** U03: `Plataforma del backend`
**Historias que implementa:** H-24 (contribuye)
**Depende de:** U01-T01 (la raíz de Gradle). U03 no depende de U01 como unidad, pero necesita el `settings.gradle.kts` que crea esa tarea.

---

## Alcance

**Dentro** (una línea, concreta):

> El proyecto `backend/` incluido en `settings.gradle.kts`, con Spring Boot 3, las dependencias de la plataforma y una clase `io.synkra.SynkraApp` vacía que arranca, para que `bootJar` produzca un JAR ejecutable.

**Fuera** (lo que el codificador debe rechazar aunque lo vea roto):

- Configuración de SQLite, `SYNKRA_DATA` y migraciones de Flyway: son de U03-T02.
- `ErrorApi` y el manejador de errores: U03-T03.
- Configuración de salud más allá de lo que trae actuator: U03-T04.
- La prueba de Spring Modulith: U03-T05. Aquí solo se declara la dependencia de pruebas.
- Dockerfile: U03-T06.
- Cualquier módulo de negocio (`catalogo`, `despliegue`…) y cualquier dependencia de `:verificador`.
- Cambios en el `build.gradle.kts` de la raíz o en `verificador/`.

---

## Archivos de contexto

- `unidades-y-tareas.md` (sección U03)
- `aidlc-docs/construction/plans/U03-plataforma-backend-code-generation-plan.md`
- `aidlc-docs/inception/application-design/application-design.md` (D1, D7 y D14)
- `aidlc-docs/inception/application-design/unit-of-work.md` (sección "Organización del código")

---

## Criterios de aceptación

- [ ] **CA-1**: el build reconoce `:backend`
  ```bash
  ./gradlew -q projects | grep -F "':backend'"
  ```
  Esperado: una línea con `Project ':backend'`.

- [ ] **CA-2**: `bootJar` produce un JAR con `SynkraApp` como clase de arranque
  ```bash
  ./gradlew :backend:bootJar
  unzip -p backend/build/libs/backend-*.jar META-INF/MANIFEST.MF | grep -F 'Start-Class: io.synkra.SynkraApp'
  ```
  Esperado: `BUILD SUCCESSFUL` y la línea `Start-Class`.

- [ ] **CA-3**: están las dependencias de la plataforma
  ```bash
  ./gradlew -q :backend:dependencies --configuration runtimeClasspath | grep -oE 'spring-boot-starter-(web|actuator|data-jdbc)|org\.xerial:sqlite-jdbc|org\.flywaydb:flyway-core' | sort -u
  ./gradlew -q :backend:dependencies --configuration testRuntimeClasspath | grep -c 'org.springframework.modulith'
  ```
  Esperado: las cinco líneas en la primera; `1` o más en la segunda.

- [ ] **CA-4**: el verificador no se rompió
  ```bash
  ./gradlew :verificador:check
  ```
  Esperado: `BUILD SUCCESSFUL`.

---

## Plan de pruebas

No hay lógica que probar todavía; la prueba de arranque con base de datos llega en U03-T02.

**Rojo primero:** antes de crear `backend/`, corre CA-1 y registra que no encuentra el proyecto.
Después, pasando.

---

## Notas

- Archivo que se **modifica en su sitio**: `settings.gradle.kts` (se añade `include("backend")`).
- El plugin de Spring Boot y su gestión de dependencias se declaran en `backend/build.gradle.kts`,
  con versión fija (la 3.x estable al escribir la tarea).
- Spring Boot autoconfigura un `DataSource` al ver `sqlite-jdbc`; esta tarea no arranca la
  aplicación, así que no lo configura (es U03-T02).
