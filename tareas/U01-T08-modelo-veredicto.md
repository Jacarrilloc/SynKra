# U01-T08: Modelo del veredicto y códigos de motivo

**Unidad:** U01: `Verificador y suite`
**Historias que implementa:** H-07
**Depende de:** U01-T06

---

## Alcance

**Dentro** (una línea, concreta):

> Los records inmutables `Seleccion`, `Alternativa` (con el enum `TipoAlternativa`: `PLUGIN`, `CORE`), `VeredictoPlugin` y `Veredicto` (con el enum `Global`: `APROBADA`, `RECHAZADA`) de `domain-entities.md`, con sus invariantes comprobadas en el constructor, y la prueba `CodigoMotivoTest` que compara `CodigoMotivo` con la tabla «Códigos de motivo» de `domain-entities.md`, leída del archivo.

**Fuera** (lo que el codificador debe rechazar aunque lo vea roto):

- `Verificador.verificar`, el orden de R-V4 y la interfaz `Catalogo` (U01-T09).
- Calcular alternativas o decidir cuándo una es «compatible de verdad» (U01-T10): aquí `Alternativa` es solo el valor.
- Serializar el veredicto a JSON o a la API (U06).
- Cambios en `CodigoMotivo`, `Motivo`, `Descriptor`, `Version`, `RangoVersion`, `Resultado` o `LectorDescriptor`. Si la prueba de CA-2 encuentra una diferencia con la tabla, se registra en la bitácora y se para: el orquestador decide qué lado está mal.
- C-06 (el `campo` del `Motivo` que pone el lector): sigue para U01-T09.
- Nuevas dependencias.

---

## Archivos de contexto

- `aidlc-docs/construction/U01-verificador/functional-design/domain-entities.md` (entidades y tabla de códigos)
- `aidlc-docs/construction/U01-verificador/functional-design/business-rules.md` (R-V4: cuándo es `APROBADA`)
- `verificador/src/main/java/io/synkra/verificador/` (`Motivo`, `CodigoMotivo`, `Descriptor`, `Version`)
- `CANDIDATAS.md` (C-06)

---

## Criterios de aceptación

- [ ] **CA-1**: las pruebas del modelo pasan, sin caché
  ```bash
  ./gradlew :verificador:test --rerun --tests '*CodigoMotivoTest' --tests '*VeredictoTest'
  ```
  Esperado: `BUILD SUCCESSFUL`.

- [ ] **CA-2**: `CodigoMotivo` tiene exactamente los 10 códigos de la tabla del diseño
  ```bash
  grep -ho 'tests="[0-9]*" skipped="[0-9]*" failures="[0-9]*" errors="[0-9]*"' verificador/build/test-results/test/TEST-*CodigoMotivoTest.xml
  ```
  Esperado: `skipped="0" failures="0" errors="0"`. `CodigoMotivoTest` lee
  `aidlc-docs/construction/U01-verificador/functional-design/domain-entities.md` (ruta relativa a la
  raíz del repositorio, resuelta desde el directorio del proyecto), toma la primera columna de la tabla
  que sigue a `## Códigos de motivo` y afirma:
  - que hay 10 filas;
  - que el conjunto de nombres es igual al de `CodigoMotivo.values()` (ni uno de más ni uno de menos);
  - que no hay nombres repetidos ni en la tabla ni en el enum.
  Si el archivo no existe o la tabla no se encuentra, la prueba **falla** con un mensaje que lo dice;
  no se salta.

- [ ] **CA-3**: la prueba de CA-2 detecta una diferencia. En una copia de trabajo: (a) se agrega una
  fila `| X_INVENTADO | ... |` a la tabla, y (b) aparte, se borra `PLUGIN_AUSENTE` de la tabla. En los
  dos casos CA-1 falla y el mensaje nombra el código que sobra o falta. Se revierte y se pega en la
  bitácora cada par falla-pasa.
  ```bash
  git diff --stat -- aidlc-docs verificador/src/main
  ```
  Esperado: vacío al terminar.

- [ ] **CA-4**: las invariantes del modelo se cumplen y las entradas malas se rechazan
  ```bash
  grep -ho 'tests="[0-9]*" skipped="[0-9]*" failures="[0-9]*" errors="[0-9]*"' verificador/build/test-results/test/TEST-*VeredictoTest.xml
  ```
  Esperado: `tests` ≥ 10, `skipped="0" failures="0" errors="0"`. Casos mínimos:
  - `Seleccion` sin plugins es válida (PRD §11.1); con `core` que no cumple el patrón de `nombre`, o con
    `null` en cualquier campo o dentro de la lista → `IllegalArgumentException` o `NullPointerException`.
  - `VeredictoPlugin` con `compatible = true` y algún motivo → excepción; con `compatible = false` y sin
    motivos → excepción.
  - `Veredicto` con `APROBADA` y algún motivo (global o de un plugin) → excepción; con `RECHAZADA` y
    ningún motivo → excepción. Una fábrica `Veredicto.de(porPlugin, motivosGlobales)` calcula `global`
    y da `APROBADA` solo cuando no hay ningún motivo.
  - `Veredicto` sin plugins y sin motivos globales → `APROBADA`.
  - **Inmutabilidad:** cambiar la lista original después de construir `Seleccion`, `VeredictoPlugin` o
    `Veredicto` no cambia el objeto, y las listas que devuelven lanzan
    `UnsupportedOperationException` al intentar modificarlas.
  - `Alternativa` con `nombre` vacío o que no cumple el patrón → excepción.
  - Dos veredictos construidos con los mismos datos son `equals` y tienen el mismo `hashCode`.

- [ ] **CA-5**: el verificador sigue sin Spring ni Fabric8 en ejecución
  ```bash
  ./gradlew :verificador:dependencies --configuration runtimeClasspath | grep -ciE "spring|fabric8"
  ```
  Esperado: `0`.

- [ ] **CA-6**: todo el verificador sigue pasando, sin caché
  ```bash
  ./gradlew :verificador:check --rerun
  ```
  Esperado: `BUILD SUCCESSFUL`, sin pruebas saltadas en ningún XML.

---

## Plan de pruebas

- `CodigoMotivoTest`: lee el Markdown con `Files.readAllLines`, sin dependencias nuevas. Ubica la raíz
  del repositorio subiendo desde `Path.of("").toAbsolutePath()` hasta encontrar `settings.gradle.kts`,
  para no depender del directorio desde el que se lanza Gradle.
- `VeredictoTest`: casos de ejemplo de JUnit, uno por invariante de CA-4.

**Rojo primero:** prueba antes que código. Se registra CA-1 fallando por compilación (los records no
existen) y luego pasando; CA-3 da además el par falla-pasa de `CodigoMotivoTest`.

---

## Notas

- Paquete: `io.synkra.verificador`.
- Los records siguen la forma de `Descriptor`: `Objects.requireNonNull` y `List.copyOf` en el
  constructor compacto.
- `Alternativa.version` es una `Version`, no un texto.
- `VeredictoPlugin.descriptor` es el `Descriptor` leído; el verificador de U01-T09 es el que llena los
  motivos.
- Los mensajes de las excepciones nombran la invariante rota (por ejemplo, «compatible sin motivos»);
  no hacen falta códigos nuevos.
