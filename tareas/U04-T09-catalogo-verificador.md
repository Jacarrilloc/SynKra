# U04-T09: `Catalogo` de U01 implementado por el módulo

**Unidad:** U04: `Catálogo`
**Historias que implementa:** H-06
**Depende de:** U04-T08

---

## Alcance

**Dentro** (una línea, concreta):

> Un bean del módulo `catalogo` que implementa `io.synkra.verificador.Catalogo` (`versionesCore`, `versionesPlugin`, `descriptor`) leyendo de SQLite, expuesto en `catalogo.api` para que U07 lo use. Se prueba con los casos S1 a S7 de `SuiteCombinaciones` de U01, cargados por `CatalogoApi`.

**Fuera** (lo que el codificador debe rechazar aunque lo vea roto):

- Cambios en `verificador/`. Si la interfaz `Catalogo` no alcanza, se anota y se para.
- Los casos S8 a S12: son del descriptor (S8 y S9, que ya cubre U04-T06), de la selección (S10) o de U07 (S11 y S12).
- Caché del catálogo.

---

## Archivos de contexto

- `verificador/src/main/java/io/synkra/verificador/Catalogo.java`, `Verificador.java` y `Descriptor.java`
- `verificador/src/testFixtures/java/io/synkra/verificador/SuiteCombinaciones.java` y `CatalogoEnMemoria.java`
- `backend/src/main/java/io/synkra/catalogo/` (repositorios, `SubidaPlugin`, `api/`)
- `backend/build.gradle.kts`

---

## Decisiones del orquestador

1. **Ubicación.** La clase de implementación es package-private en `catalogo`. Lo que se expone en `catalogo.api` es un tipo o un método de fábrica que devuelve `Catalogo`, para que `InterfazCatalogoTest` siga pasando. Se describe la elección en la bitácora.
2. **Descriptor.** `descriptor(core, plugin, version)` se rearma desde `cat_plugin` (`nombre`, `version`, `core`, `core_compatible` parseado con `RangoVersion` de U01, `descripcion`) y solo las etiquetas con `origen = 'descriptor'`. Así el resultado es igual al descriptor que había en el JAR. Una fila que no se puede parsear es un error interno (500) y no se oculta.
3. **Contrato de U01.** Nunca devuelve `null`. Lo que no existe es una lista vacía u `Optional.empty()`. Una carpeta solo ve sus plugins.
4. **Prueba de equivalencia.** Se sube a SQLite el contenido de `SuiteCombinaciones.contenido()` con `registrarCore` y `subirPlugin` (los JAR se arman con `LectorDescriptor.aYaml`). Para cada caso S1 a S7, el `Verificador` de U01 da **el mismo veredicto** con el catálogo de SQLite que con `SuiteCombinaciones.catalogo()`.
5. **Dependencia.** `backend` agrega `testImplementation(testFixtures(project(":verificador")))`. No hay otra dependencia nueva.

---

## Criterios de aceptación

- [ ] **CA-1**: S1 a S7 con datos en SQLite, sin caché y sin saltos
  ```bash
  ./gradlew :backend:test --rerun --tests '*CatalogoVerificadorTest'
  grep -ho 'tests="[0-9]*" skipped="[0-9]*" failures="[0-9]*" errors="[0-9]*"' backend/build/test-results/test/TEST-*CatalogoVerificadorTest.xml
  ```
  Esperado: `BUILD SUCCESSFUL`, `tests` ≥ 10, `skipped="0" failures="0" errors="0"`. Las pruebas cubren:
  - S1 a S7, uno por caso, comparando con el veredicto en memoria;
  - `versionesCore` y `versionesPlugin` de algo inexistente: lista vacía;
  - `descriptor` de una versión inexistente: `Optional.empty()`;
  - `descriptor` de un plugin con etiquetas de subida: no las trae;
  - una carpeta no ve los plugins de otra.

- [ ] **CA-2**: la interfaz pública sigue cerrada
  ```bash
  ./gradlew :backend:test --rerun --tests '*InterfazCatalogoTest' --tests '*FronterasModulosTest'
  ```
  Esperado: `BUILD SUCCESSFUL`.

- [ ] **CA-3**: todo el backend y el verificador siguen pasando
  ```bash
  ./gradlew :backend:test --rerun :backend:check :verificador:check
  for f in backend/build/test-results/test/TEST-*.xml; do grep -o 'tests="[0-9]*" skipped="[0-9]*" failures="[0-9]*" errors="[0-9]*"' "$f"; done
  ```
  Esperado: `BUILD SUCCESSFUL`, sin saltos ni fallos.

- [ ] **CA-4**: el diff queda acotado
  ```bash
  git diff --stat origin/main...HEAD
  ```
  Esperado: `backend/build.gradle.kts` (solo testFixtures), `catalogo/**`, las pruebas nuevas, la bitácora y `revisiones/U04-T09/`.

---

## Plan de pruebas

- `CatalogoVerificadorTest` (`@SpringBootTest`, `@TempDir`) carga la suite por la API y compara veredictos.

**Rojo primero:** la prueba con la implementación vacía, registrando el fallo.
