# U04-T02: Registrar un core (R-C1, R-C2)

**Unidad:** U04: `Catálogo`
**Historias que implementa:** H-01
**Depende de:** U04-T01

---

## Alcance

**Dentro** (una línea, concreta):

> `CatalogoApi` en `io.synkra.catalogo.api` con `registrarCore` y `obtenerCore`, y su implementación en el módulo, con R-C1 y R-C2 y errores `ExcepcionApi`. `backend` pasa a depender de `:verificador`.

**Fuera** (lo que el codificador debe rechazar aunque lo vea roto):

- Controladores y rutas HTTP (U04-T03).
- Plugins, JAR y etiquetas (U04-T04 a U04-T08).
- R-C3, el registro de imágenes permitido (U04-T13, *Should*).
- Autenticación: `registrado_por` sigue en `'anonimo'` hasta U10.
- Cambios en `verificador/` o en la migración `V2` (si algo falta, se anota y se para).

---

## Archivos de contexto

- `aidlc-docs/construction/U04-catalogo/functional-design/business-rules.md` (R-C1, R-C2)
- `aidlc-docs/construction/U04-catalogo/functional-design/business-logic-model.md` (interfaz pública)
- `aidlc-docs/inception/user-stories/stories.md` (H-01)
- `verificador/src/main/java/io/synkra/verificador/Version.java` y `Descriptor.java` (`NOMBRE`)
- `backend/src/main/java/io/synkra/plataforma/` (`ExcepcionApi`, `ErrorApi`, `DetalleError`, `CodigosError`)
- `tareas/U04-T01-migracion-repositorios.md`

---

## Decisiones del orquestador

1. **Validación (R-C1).** Cada campo inválido da un `DetalleError(campo, motivo)`, y se devuelven **todos** en un solo 422 `ENTIDAD_NO_PROCESABLE`:
   - `nombre`: tiene que cumplir `Descriptor.NOMBRE` de U01. No se copia la expresión.
   - `version`: tiene que pasar `Version.parsear` de U01.
   - `imagen`: forma `registro/repo[:tag]@sha256:<64 hex en minúsculas>`. Se compila una sola expresión en el módulo.
2. **Repetido (R-C2).** Una versión ya registrada da 409 `CONFLICTO` con el detalle `version`. No cambia nada en la base, ni siquiera la imagen.
3. **Carpeta.** La «carpeta del core» es la fila de `cat_core`. Se crea con la primera versión, en la misma transacción.
4. **Lo que devuelve.** `CoreRegistrado(nombre, versiones)`, con las versiones ordenadas como `Version`, no como texto (`2.10.0` va después de `2.9.0`). `obtenerCore` de un core inexistente da 404 `NO_ENCONTRADO`.
5. **Módulo.** `catalogo.api` se declara como `@NamedInterface`. Los demás módulos solo ven `CatalogoApi` y sus records.

---

## Criterios de aceptación

- [ ] **CA-1**: R-C1 y R-C2, sin caché y sin saltos
  ```bash
  ./gradlew :backend:test --rerun --tests '*RegistrarCoreTest'
  grep -ho 'tests="[0-9]*" skipped="[0-9]*" failures="[0-9]*" errors="[0-9]*"' backend/build/test-results/test/TEST-*RegistrarCoreTest.xml
  ```
  Esperado: `BUILD SUCCESSFUL`, `tests` ≥ 8 y `skipped="0" failures="0" errors="0"`. Las pruebas cubren:
  - la primera versión: el core queda con una versión;
  - la segunda versión del mismo core: dos versiones y una sola fila en `cat_core`;
  - el orden por versión: `2.9.0` antes que `2.10.0`;
  - la imagen sin digest y con un digest de 63 caracteres o en mayúsculas: 422 con `detalles[].campo = "imagen"`;
  - el nombre con mayúsculas o con `_`: 422 `nombre`;
  - la versión `2.1`: 422 `version`;
  - tres campos inválidos a la vez: un solo 422 con los tres detalles;
  - la versión repetida: 409, y la base queda igual (se lee de vuelta la imagen original);
  - el core inexistente: 404.

- [ ] **CA-2**: la validación usa U01, no una copia
  ```bash
  grep -rn 'Descriptor.NOMBRE\|Version.parsear' backend/src/main/java/io/synkra/catalogo/
  grep -rn '\[a-z0-9\]' backend/src/main/java/io/synkra/catalogo/ || echo "sin copia"
  ```
  Esperado: aparecen las dos referencias y la segunda línea imprime `sin copia`.

- [ ] **CA-3**: las fronteras y todo el backend siguen pasando
  ```bash
  ./gradlew :backend:test --rerun :backend:check :verificador:check
  for f in backend/build/test-results/test/TEST-*.xml; do grep -o 'tests="[0-9]*" skipped="[0-9]*" failures="[0-9]*" errors="[0-9]*"' "$f"; done
  ```
  Esperado: `BUILD SUCCESSFUL`, sin saltos ni fallos. `FronterasModulosTest` pasa, y `catalogo.api` aparece como interfaz con nombre.

- [ ] **CA-4**: el diff queda acotado
  ```bash
  git diff --stat origin/main...HEAD
  ```
  Esperado: `backend/build.gradle.kts` (solo `implementation(project(":verificador"))`), `backend/src/main/java/io/synkra/catalogo/**`, las pruebas nuevas, la bitácora y `revisiones/U04-T02/`.

---

## Plan de pruebas

- `RegistrarCoreTest` (`@SpringBootTest` con `@TempDir`) llama a `CatalogoApi`, afirma el `ExcepcionApi` (estado y `ErrorApi` con sus detalles) y lee de vuelta con `JdbcTemplate`.

**Rojo primero:** primero se escribe la prueba con la interfaz vacía. Hay que registrar el fallo de CA-1 y después ver pasar el mismo comando.

---

## Notas

- Paquete: `io.synkra.catalogo` (implementación) e `io.synkra.catalogo.api` (pública).
- El mensaje de 422 y de 409 usa las constantes de `CodigosError`. Los `motivo` van en español.
