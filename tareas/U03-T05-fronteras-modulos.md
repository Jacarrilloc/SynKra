# U03-T05: Prueba de fronteras del monolito modular (D14)

**Unidad:** U03: `Plataforma del backend`
**Historias que implementa:** H-24
**Depende de:** U03-T01

---

## Alcance

**Dentro** (una línea, concreta):

> La prueba `FronterasModulosTest`, que corre `ApplicationModules.of(SynkraApp.class).verify()` sobre el backend real, genera la documentación de módulos con `Documenter` y demuestra con una aplicación de prueba propia que `verify()` falla cuando un módulo importa un paquete interno de otro.

**Fuera** (lo que el codificador debe rechazar aunque lo vea roto):

- Spring Modulith en ejecución (`spring-modulith-starter-core`, `spring-modulith-api` o anotaciones `@ApplicationModule` en `src/main`): en U03-T01 eso fue un hallazgo ROJO. Modulith se queda solo en `testImplementation`.
- Crear módulos de negocio (`catalogo`, `despliegue`...) en `src/main`: llegan con U04 en adelante.
- Declarar `plataforma` como módulo abierto o compartido: se decide cuando exista el primer módulo que la use.
- Centralizar versiones en la raíz de Gradle (C-03).

---

## Archivos de contexto

- `aidlc-docs/construction/plans/U03-plataforma-backend-code-generation-plan.md` (U03-T05 y D14)
- `aidlc-docs/inception/application-design/application-design.md` (D14, monolito modular con fronteras comprobadas)
- `backend/build.gradle.kts`
- `backend/src/main/java/io/synkra/SynkraApp.java` y `backend/src/main/java/io/synkra/plataforma/package-info.java`

---

## Criterios de aceptación

- [ ] **CA-1**: la prueba de fronteras pasa, sin caché
  ```bash
  ./gradlew :backend:test --rerun --tests '*FronterasModulosTest'
  ```
  Esperado: `BUILD SUCCESSFUL`.

- [ ] **CA-2**: lectura de vuelta del reporte: el caso real y el caso que debe fallar
  ```bash
  grep -ho 'tests="[0-9]*" skipped="[0-9]*" failures="[0-9]*" errors="[0-9]*"' backend/build/test-results/test/TEST-*FronterasModulosTest.xml
  ```
  Esperado: `tests` ≥ 2, `skipped="0" failures="0" errors="0"`. Un caso verifica `SynkraApp` y pasa.
  El otro arma `ApplicationModules.of(...)` sobre una aplicación de prueba en
  `src/test/java/io/synkra/fronteras/` (dos módulos, `uno` y `dos`, donde `uno` importa
  `dos.interno.Algo`) y afirma que `verify()` lanza `Violations` y que el mensaje nombra `dos.interno`.

- [ ] **CA-3**: la documentación de módulos se genera
  ```bash
  ls backend/build/spring-modulith-docs/
  ```
  Esperado: al menos un archivo `.puml` o `.adoc` que nombra el módulo `plataforma`.

- [ ] **CA-4**: Modulith no llega al JAR de ejecución
  ```bash
  ./gradlew :backend:dependencies --configuration runtimeClasspath | grep -ci modulith
  ```
  Esperado: `0`.

- [ ] **CA-5**: todo el backend sigue pasando, sin caché
  ```bash
  ./gradlew :backend:check --rerun
  ```
  Esperado: `BUILD SUCCESSFUL`.

---

## Plan de pruebas

- `FronterasModulosTest` en `io.synkra`, sin contexto de Spring: `ApplicationModules` analiza el
  bytecode y no necesita arrancar la aplicación.
- La aplicación de prueba de CA-2 vive en un paquete raíz propio (`io.synkra.fronteras`) para que no
  se mezcle con los módulos reales.

**Rojo primero:** el caso de CA-2 es el par falla-pasa: primero se escribe la importación prohibida y
se ve `verify()` fallar, y se pega esa salida en la bitácora.

---

## Notas

- Si `ApplicationModules.of` sobre `io.synkra` recoge también las clases de `io.synkra.fronteras`
  (porque están en el classpath de pruebas), hay que excluirlas con el filtro de `ApplicationModules`
  y explicarlo en la bitácora. Es el punto más probable de un hallazgo.
