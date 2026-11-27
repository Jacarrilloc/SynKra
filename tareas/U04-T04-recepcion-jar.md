# U04-T04: Recepción segura del JAR (R-C5, R-C6, R-C11)

**Unidad:** U04: `Catálogo`
**Historias que implementa:** H-04, H-02 (base)
**Depende de:** U04-T02

---

## Alcance

**Dentro** (una línea, concreta):

> Un componente no público `RecepcionArtefacto` en `io.synkra.catalogo` que recibe un `InputStream`, lo copia a un archivo temporal bajo `$SYNKRA_DATA/artefactos/.tmp/` cortando al pasar el límite, calcula el SHA-256 mientras escribe y revisa las entradas del JAR antes de devolver un `ArtefactoRecibido(temporal, sha256, tamano)`. Si falla, no deja nada en disco.

**Fuera** (lo que el codificador debe rechazar aunque lo vea roto):

- Leer el descriptor, comprobar el core y guardar metadatos: es U04-T06.
- Mover el archivo a su ruta final: es U04-T06. Aquí solo se entrega el temporal.
- La ruta HTTP y el límite de multipart de Spring: es U04-T07.
- La propiedad PBT-03: es U04-T05. Aquí van las pruebas de ejemplo.
- Cambios en `verificador/` (C-13 y C-20 siguen abiertas) y en la migración `V2`.

---

## Archivos de contexto

- `aidlc-docs/construction/U04-catalogo/functional-design/business-rules.md` (R-C5, R-C6, R-C11)
- `aidlc-docs/construction/U04-catalogo/functional-design/business-logic-model.md` (flujo de subida)
- `aidlc-docs/inception/user-stories/stories.md` (H-04)
- `backend/src/main/java/io/synkra/plataforma/` (`ExcepcionApi`, `ErrorApi`, `CodigosError`, `RutaDatos`)
- `backend/src/main/java/io/synkra/catalogo/` (`MotivosCatalogo`, `CatalogoServicio`)
- `CANDIDATAS.md` (C-08, C-13, C-20)

---

## Decisiones del orquestador

1. **Límite (R-C5).** La propiedad es `synkra.max-artefacto`, con `SYNKRA_MAX_ARTEFACTO` y un valor por defecto de `20MB` (tipo `DataSize`).
   - La lectura se corta al leer el byte `límite + 1`. No se confía en `Content-Length`.
   - Si se pasa, se borra el temporal y se lanza `ExcepcionApi` 413 `DEMASIADO_GRANDE`, con un detalle `campo = "archivo"` cuyo `motivo` nombra el límite configurado (por ejemplo «El artefacto supera el límite de 20 MB»).
2. **Temporal (R-C11).**
   - El archivo temporal se crea con `Files.createTempFile` dentro de `$SYNKRA_DATA/artefactos/.tmp/`, y el directorio se crea si no existe.
   - El SHA-256 sale de un `DigestInputStream` (o equivalente) durante la misma copia, nunca de una segunda lectura. Se devuelve como 64 hex en minúsculas.
   - En cualquier fallo, incluida una excepción inesperada, el temporal se borra (`finally`).
3. **Entradas del JAR (R-C6).** Se recorren todas las entradas del directorio central. Se rechaza el JAR si alguna entrada:
   - tiene un nombre absoluto (`/…`, `\…` o una letra de unidad `C:`);
   - contiene un segmento `..` tras normalizar `\` a `/`;
   - contiene NUL;
   - es un enlace simbólico (modo Unix `S_IFLNK` en los atributos externos).

   Para ver los enlaces, java.util.zip no basta. Se usa `org.apache.commons:commons-compress` (`ZipFile.builder()` y `ZipArchiveEntry.isUnixSymlink()`). Si Spring Boot ya gestiona su versión, se usa sin número; si no, se fija `1.27.1`. Es la única dependencia nueva.

   Un archivo que no es ZIP también se rechaza: 422 `ARTEFACTO_INVALIDO`, con `campo = "archivo"`.
4. **Errores.**
   - El rechazo por ruta da 422 con `codigo = "RUTA_NO_PERMITIDA"`. Hay un detalle por entrada culpable, hasta 10, con `campo = "archivo"` y un `motivo` que nombra la entrada recortada a 200 caracteres.
   - Los códigos del módulo (`RUTA_NO_PERMITIDA`, `ARTEFACTO_INVALIDO`) van como constantes en `io.synkra.catalogo` (junto a `MotivosCatalogo` o en un `CodigosCatalogo` no público), no en `plataforma`.
5. **Nada en disco.** En todo rechazo, `$SYNKRA_DATA/artefactos/` queda sin archivos (el directorio `.tmp/` vacío se permite).

---

## Criterios de aceptación

- [ ] **CA-1**: la recepción, sin caché y sin saltos
  ```bash
  ./gradlew :backend:test --rerun --tests '*RecepcionArtefactoTest'
  grep -ho 'tests="[0-9]*" skipped="[0-9]*" failures="[0-9]*" errors="[0-9]*"' backend/build/test-results/test/TEST-*RecepcionArtefactoTest.xml
  ```
  Esperado: `BUILD SUCCESSFUL`, `tests` ≥ 10, `skipped="0" failures="0" errors="0"`. Con `@TempDir` como `synkra.data`, las pruebas comprueban:
  - un JAR válido: el `sha256` coincide con `MessageDigest` sobre los mismos bytes y el temporal existe con ese tamaño;
  - un flujo de 25 MB con el límite en 20 MB: 413, el `motivo` nombra 20 MB y no queda ningún archivo;
  - exactamente el límite: se acepta. Límite + 1: 413;
  - un límite bajo (`synkra.max-artefacto=1KB`) para probar el corte sin generar 20 MB;
  - una entrada `../../etc/x`: 422 `RUTA_NO_PERMITIDA` y nada en disco;
  - una entrada absoluta `/etc/x`, otra `C:\x` y otra `a\..\..\x`: 422;
  - un enlace simbólico (escrito con commons-compress y modo `0120777`): 422;
  - un archivo que no es ZIP: 422 `ARTEFACTO_INVALIDO`;
  - un `InputStream` que lanza `IOException` a mitad: la excepción sube y no queda temporal.

- [ ] **CA-2**: el hash se calcula en la misma pasada
  ```bash
  grep -n 'DigestInputStream\|DigestOutputStream\|MessageDigest' backend/src/main/java/io/synkra/catalogo/*.java
  ```
  Esperado: una sola clase de recepción con el digest sobre el flujo de copia. La bitácora explica en una línea por qué no hay segunda lectura.

- [ ] **CA-3**: todo el backend y las fronteras siguen pasando
  ```bash
  ./gradlew :backend:test --rerun :backend:check
  for f in backend/build/test-results/test/TEST-*.xml; do grep -o 'tests="[0-9]*" skipped="[0-9]*" failures="[0-9]*" errors="[0-9]*"' "$f"; done
  ```
  Esperado: `BUILD SUCCESSFUL`, sin saltos ni fallos. `InterfazCatalogoTest` sigue pasando: ningún tipo nuevo es público fuera de `catalogo.api`.

- [ ] **CA-4**: el diff queda acotado
  ```bash
  git diff --stat origin/main...HEAD
  ```
  Esperado:
  - `backend/build.gradle.kts`, solo con commons-compress;
  - `backend/src/main/java/io/synkra/catalogo/**`;
  - `application.properties`, solo con `synkra.max-artefacto`;
  - las pruebas nuevas;
  - la bitácora y `revisiones/U04-T04/`.

---

## Plan de pruebas

- `RecepcionArtefactoTest` (`@SpringBootTest` con `@TempDir`) crea los JAR en memoria con `ZipArchiveOutputStream` y llama al componente directamente. Después de cada rechazo recorre `$SYNKRA_DATA/artefactos/` con `Files.walk` para afirmar que no quedan archivos.

**Rojo primero:** se escribe la prueba con el componente vacío y se registra el fallo de CA-1. Para el enlace simbólico también se registra el rojo: con solo java.util.zip, la prueba no falla.

---

## Notas

- Para no tardar, el JAR de 25 MB se genera como flujo y no como archivo en el repo.
- Los `motivo` van en español y en constantes. Una prueba fija sus textos (lección de U04-T02, F-04).
