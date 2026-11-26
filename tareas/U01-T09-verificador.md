# U01-T09: `Verificador.verificar` (R-V4) y la interfaz `Catalogo`

**Unidad:** U01: `Verificador y suite`
**Historias que implementa:** H-07
**Depende de:** U01-T08

---

## Alcance

**Dentro** (una línea, concreta):

> La interfaz de solo lectura `Catalogo`, la función pura `Verificador.verificar(Seleccion, Catalogo) → Veredicto` con el orden de R-V4, un `CatalogoEnMemoria` en `src/testFixtures` para las pruebas (y para U02 y U07 después), y dos líneas en R-V4 que fijan las dos lecturas del diseño que esta tarea decide.

**Fuera** (lo que el codificador debe rechazar aunque lo vea roto):

- Calcular alternativas (U01-T10): aquí toda `VeredictoPlugin.alternativas` es vacía.
- Propiedades de jqwik sobre el verificador (U01-T11) y la suite S1–S12 completa en `testFixtures` (U01-T12).
- Comprobar que `seleccion.core` exista en el catálogo o que `versionCore` esté registrada: lo hace U07 antes de llamar.
- Comprobar que el descriptor recibido sea idéntico byte a byte al del catálogo (integridad: U04 y U05).
- Cambios en `CodigoMotivo`, `Motivo`, `Descriptor`, `Version`, `RangoVersion`, `Resultado`, `Seleccion`, `Veredicto`, `VeredictoPlugin` o `Alternativa`. Si algo de eso impide cumplir un criterio, se registra en la bitácora y se para.
- Nuevas dependencias.

---

## Archivos de contexto

- `aidlc-docs/construction/U01-verificador/functional-design/business-rules.md` (R-V4, R-V6)
- `aidlc-docs/construction/U01-verificador/functional-design/domain-entities.md` (`Catalogo`, tabla de códigos)
- `aidlc-docs/construction/U01-verificador/functional-design/business-logic-model.md` (catálogo de la suite, S1–S10)
- `verificador/src/main/java/io/synkra/verificador/` (todos los records)
- `CANDIDATAS.md` (C-06)

---

## Decisiones del orquestador (lecturas del diseño que esta tarea fija)

1. **Dónde se busca el plugin (paso 2 de R-V4).** En la carpeta que declara el propio descriptor:
   `catalogo.descriptor(d.core(), d.nombre(), d.version())`. Así S7 (`kyc-xx` de `otro-core` pedido con
   `synkra-core`) da `CORE_EQUIVOCADO` y no `PLUGIN_AUSENTE`, que es lo que dice la suite.
2. **Un motivo por plugin como máximo.** Los pasos 2, 3 y 4 se evalúan en orden y el primero que falla
   corta los siguientes para ese plugin. Comparar la versión del core contra el rango de un plugin de
   otro core no tiene sentido (S7 daría además `VERSION_INCOMPATIBLE`).
3. **Repetidos.** La primera aparición de un `nombre` se verifica normal. Cada aparición siguiente no
   entra en `porPlugin` y produce un único `PLUGIN_REPETIDO` global por nombre, con `plugin` = ese nombre
   y un mensaje que lista las versiones pedidas. S10 da solo `PLUGIN_REPETIDO`.
4. **Campos del `Motivo`** que pone el verificador: `plugin` = `descriptor.nombre()` en todos; `campo` =
   `"core"` en `CORE_EQUIVOCADO`, `"coreCompatible"` en `VERSION_INCOMPATIBLE`, vacío en `PLUGIN_AUSENTE` y
   `PLUGIN_REPETIDO`. Los mensajes, en español, nombran el plugin y las versiones o cores en juego.
5. **C-06** se cierra sin cambiar `LectorDescriptor`: `Version` y `RangoVersion` fijan `"version"` y
   `"coreCompatible"`, que son justo los nombres de los campos del descriptor. Se deja una prueba que lo
   ata (CA-5).

---

## Criterios de aceptación

- [ ] **CA-1**: las pruebas del verificador pasan, sin caché
  ```bash
  ./gradlew :verificador:test --rerun --tests '*VerificadorTest'
  grep -ho 'tests="[0-9]*" skipped="[0-9]*" failures="[0-9]*" errors="[0-9]*"' verificador/build/test-results/test/TEST-*VerificadorTest.xml
  ```
  Esperado: `BUILD SUCCESSFUL`, `tests` ≥ 12, `skipped="0" failures="0" errors="0"`. Sobre el catálogo
  de la suite (construido con `CatalogoEnMemoria`), cada caso afirma la lista **exacta** de motivos
  (código, `plugin`, `campo`), no solo el código:
  - S1, S2 y S3 → `APROBADA`, sin motivos, `porPlugin` con 1, 1 y 0 elementos.
  - S6 → `PLUGIN_AUSENTE` en `kyc-mx`.
  - S7 → solo `CORE_EQUIVOCADO` en `kyc-xx` (decisiones 1 y 2).
  - S4 → solo `VERSION_INCOMPATIBLE` en `kyc-pe`, sin alternativas.
  - S10 → un `PLUGIN_REPETIDO` global, `porPlugin` con un solo `kyc-pe` (1.3.0) compatible (decisión 3).
  - Tres apariciones del mismo nombre → un solo `PLUGIN_REPETIDO`.
  - Un plugin presente en el catálogo con la misma versión pero otro `coreCompatible` en el descriptor
    recibido: se juzga con el descriptor recibido (el catálogo solo dice si existe).
  - Dos plugins con fallos distintos en la misma selección → cada uno con su motivo, en el orden recibido.
  - `verificar(null, c)` y `verificar(s, null)` → `NullPointerException`; un `Catalogo` que devuelve `null`
    en vez de `Optional.empty()` → `NullPointerException` con un mensaje que lo dice, no un veredicto.
  - Llamar dos veces con los mismos datos da veredictos `equals`.

- [ ] **CA-2**: `CatalogoEnMemoria` cumple el contrato de solo lectura
  ```bash
  ./gradlew :verificador:test --rerun --tests '*CatalogoEnMemoriaTest'
  ```
  Esperado: `BUILD SUCCESSFUL`. Vive en `verificador/src/testFixtures/java/io/synkra/verificador/`. Se
  construye con una lista de versiones de core por nombre y una lista de `Descriptor`; cada descriptor
  cae en la carpeta de su `core()`. Casos: nombre desconocido → listas vacías y `Optional.empty()`;
  las listas devueltas no se pueden modificar; cambiar la lista original después de construirlo no lo
  cambia; dos descriptores con el mismo `(core, nombre, version)` → `IllegalArgumentException`.

- [ ] **CA-3**: las pruebas detectan regresiones. En una copia `git archive HEAD` fuera del worktree se
  aplica cada mutación por separado, se corre CA-1 y se revierte:
  - (a) los pasos 3 y 4 cambiados de orden → falla S7;
  - (b) sin el corte de la decisión 2 → falla S7;
  - (c) el paso 2 busca en la carpeta de `seleccion.core()` → falla S7;
  - (d) los repetidos entran en `porPlugin` → falla S10.

  En la bitácora va, para cada una, el comando, la prueba que falla con su mensaje y la vuelta a verde.
  ```bash
  git diff --stat -- verificador/src/main
  ```
  Esperado: en el worktree, solo `Catalogo.java` y `Verificador.java` nuevos (sin mutaciones).

- [ ] **CA-4**: R-V4 deja escritas las decisiones 1, 2 y 3
  ```bash
  sed -n '/## R-V4/,/## R-V5/p' aidlc-docs/construction/U01-verificador/functional-design/business-rules.md
  ```
  Esperado: dice en qué carpeta se busca el plugin, que el primer fallo corta los siguientes para ese
  plugin, y que las apariciones repetidas no entran en `porPlugin`. El resto del archivo no cambia.

- [ ] **CA-5**: C-06 queda atado por una prueba
  ```bash
  ./gradlew :verificador:test --rerun --tests '*LectorDescriptorTest'
  ```
  Esperado: `BUILD SUCCESSFUL`, con un caso nuevo que lee un descriptor con `version` y `coreCompatible`
  inválidos y afirma `campo = "version"` y `campo = "coreCompatible"`. En `CANDIDATAS.md`, la fila C-06
  dice que se cerró en U01-T09 sin cambiar el lector, con la razón.

- [ ] **CA-6**: el verificador sigue sin Spring ni Fabric8 en ejecución
  ```bash
  ./gradlew :verificador:dependencies --configuration runtimeClasspath | grep -ciE "spring|fabric8"
  ```
  Esperado: `0`.

- [ ] **CA-7**: todo el verificador sigue pasando, sin caché
  ```bash
  ./gradlew :verificador:test --rerun :verificador:check
  ```
  Esperado: `BUILD SUCCESSFUL`, sin pruebas saltadas en ningún XML.

- [ ] **CA-8**: el diff queda acotado
  ```bash
  git diff --stat origin/main...HEAD
  ```
  Esperado: `Catalogo.java` y `Verificador.java` en `src/main`; `CatalogoEnMemoria.java` en
  `src/testFixtures`; `VerificadorTest`, `CatalogoEnMemoriaTest` y `LectorDescriptorTest` en `src/test`;
  `business-rules.md`, `CANDIDATAS.md`, la bitácora y `revisiones/U01-T09/`. Nada más.

---

## Plan de pruebas

- `VerificadorTest`: casos de ejemplo de JUnit sobre el catálogo de la suite. Un ayudante construye los
  `Descriptor` de la suite en una sola parte del archivo, para que U01-T12 lo pueda llevar a
  `testFixtures` sin reescribirlo.
- `CatalogoEnMemoriaTest`: un caso por punto de CA-2.

**Rojo primero:** prueba antes que código. Se registra CA-1 fallando por compilación (`Verificador` no
existe) y luego pasando; CA-3 da los cuatro pares falla-pasa.

---

## Notas

- Paquete: `io.synkra.verificador`. `Verificador` es una clase `final` con constructor privado y métodos
  estáticos, sin estado: el core (R-V6) y U07 llaman a la misma función.
- `Catalogo`: `List<Version> versionesCore(String nombre)`, `List<Version> versionesPlugin(String core,
  String plugin)`, `Optional<Descriptor> descriptor(String core, String plugin, Version version)`. Las
  listas no tienen orden garantizado; quien necesite orden lo impone.
- `java-test-fixtures` ya está en `verificador/build.gradle.kts`; no hace falta tocarlo.
- El veredicto se arma con `Veredicto.de(porPlugin, motivosGlobales)`.
