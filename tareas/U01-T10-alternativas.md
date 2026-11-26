# U01-T10: `Verificador.alternativas` (R-V5)

**Unidad:** U01: `Verificador y suite`
**Historias que implementa:** H-08
**Depende de:** U01-T09

---

## Alcance

**Dentro** (una línea, concreta):

> `Verificador.alternativas(Seleccion, Veredicto, Catalogo) → Veredicto`, que añade a cada plugin con `VERSION_INCOMPATIBLE` la versión más alta del plugin y la versión más baja del core que sí sirven (R-V5), o deja dicho que no hay ninguna, y la invariante de C-22 en `VeredictoPlugin`.

**Fuera** (lo que el codificador debe rechazar aunque lo vea roto):

- Cambiar `Verificador.verificar` o el orden de R-V4 (U01-T09).
- Alternativas para otros códigos (`PLUGIN_AUSENTE`, `CORE_EQUIVOCADO`, `PLUGIN_REPETIDO`): R-V5 dice que solo existen para `VERSION_INCOMPATIBLE`.
- Combinar alternativas de varios plugins en una sola selección que apruebe entera: cada alternativa se garantiza para su plugin.
- Propiedades de jqwik (U01-T11) y la suite en `testFixtures` (U01-T12).
- Cambios en `CodigoMotivo`, `Motivo`, `Descriptor`, `Version`, `RangoVersion`, `Seleccion`, `Veredicto` o `Alternativa`. El único record de `src/main` que cambia es `VeredictoPlugin`, y solo por la invariante de CA-4.
- Nuevas dependencias.

---

## Archivos de contexto

- `aidlc-docs/construction/U01-verificador/functional-design/business-rules.md` (R-V4 con las decisiones de U01-T09, R-V5)
- `aidlc-docs/construction/U01-verificador/functional-design/business-logic-model.md` (S4, S5)
- `verificador/src/main/java/io/synkra/verificador/Verificador.java`, `Catalogo.java`, `VeredictoPlugin.java`, `Alternativa.java`
- `verificador/src/testFixtures/java/io/synkra/verificador/CatalogoEnMemoria.java`
- `CANDIDATAS.md` (C-22)

---

## Decisiones del orquestador

1. **Entrada.** `veredicto` tiene que ser exactamente `verificar(seleccion, catalogo)`; si no lo es,
   `IllegalArgumentException`. Así no se puede pedir alternativas para un veredicto de otra selección ni
   aplicarlas dos veces.
2. **Alternativa de plugin.** Entre `catalogo.versionesPlugin(seleccion.core(), nombre)`, la versión más
   alta cuyo descriptor del catálogo existe, tiene `core = seleccion.core()` e incluye `versionCore` en su
   `coreCompatible`.
3. **Alternativa de core.** Entre `catalogo.versionesCore(seleccion.core())`, la versión más baja que cabe
   en el `coreCompatible` del plugin **pedido** (el que dio `VERSION_INCOMPATIBLE`).
4. **Ninguna.** Si no hay ni de plugin ni de core, el motivo `VERSION_INCOMPATIBLE` se reemplaza por uno
   con el mismo código, `plugin` y `campo`, y un mensaje que termina en «no hay una versión compatible
   registrada».
5. **C-22.** `VeredictoPlugin` rechaza alternativas si ninguno de sus motivos es `VERSION_INCOMPATIBLE`
   (`IllegalArgumentException`, mensaje que lo nombra). Con eso queda fijado que `compatible = true` no
   trae alternativas.
6. El `global` y el resto del veredicto no cambian: los motivos globales, el orden de `porPlugin` y los
   plugins sin `VERSION_INCOMPATIBLE` salen iguales.

---

## Criterios de aceptación

- [ ] **CA-1**: las pruebas de alternativas pasan, sin caché
  ```bash
  ./gradlew :verificador:test --rerun --tests '*AlternativasTest'
  grep -ho 'tests="[0-9]*" skipped="[0-9]*" failures="[0-9]*" errors="[0-9]*"' verificador/build/test-results/test/TEST-*AlternativasTest.xml
  ```
  Esperado: `BUILD SUCCESSFUL`, `tests` ≥ 10, `skipped="0" failures="0" errors="0"`. Casos mínimos, con
  la lista exacta de alternativas (tipo, nombre, versión) en el orden plugin y luego core:
  - **S4** (catálogo de la suite): `kyc-pe 1.3.0` (PLUGIN) y `synkra-core 3.0.0` (CORE).
  - **S5**: solo `synkra-core 2.1.0` (CORE).
  - **Más alta y más baja de verdad**: con un catálogo ampliado que agrega `kyc-pe 1.2.0` (`2.x`) y
    `synkra-core 3.1.0`, S4 sigue dando `kyc-pe 1.3.0` y `synkra-core 3.0.0`.
  - **Carpeta:** una versión compatible de `kyc-pe` que solo existe en la carpeta de `otro-core` no se
    sugiere.
  - **Ninguna:** catálogo con solo `synkra-core 2.1.0` y `kyc-pe 2.0.0` → sin alternativas y el mensaje
    de la decisión 4.
  - **Aplicada aprueba:** para S4 y S5, cambiar el descriptor por el de la alternativa de plugin, o
    `versionCore` por la de core, y volver a llamar a `verificar` deja ese plugin compatible.
  - **Sin incompatibles:** S1, S6, S7 y S10 salen `equals` a su `verificar` (decisión 6).
  - **Dos incompatibles** en la misma selección: cada uno con sus propias alternativas.
  - **Entrada mala:** `null` en cualquier argumento → `NullPointerException`; un veredicto de otra
    selección, o uno que ya trae alternativas → `IllegalArgumentException`.

- [ ] **CA-2**: la invariante de C-22
  ```bash
  ./gradlew :verificador:test --rerun --tests '*VeredictoTest'
  ```
  Esperado: `BUILD SUCCESSFUL`, con casos nuevos: `VeredictoPlugin` compatible con alternativas →
  excepción; incompatible con `PLUGIN_AUSENTE` y alternativas → excepción; incompatible con
  `VERSION_INCOMPATIBLE` y alternativas → válido. La fila C-22 de `CANDIDATAS.md` dice que se cerró en
  U01-T10 y cómo.

- [ ] **CA-3**: las pruebas detectan regresiones. En una copia `git archive HEAD` fuera del worktree se
  aplica cada mutación por separado, se corre CA-1 y se revierte:
  - (a) la alternativa de plugin toma la versión más baja → falla el caso «más alta y más baja»;
  - (b) la alternativa de core toma la más alta → falla el mismo caso;
  - (c) la alternativa de plugin no comprueba `coreCompatible` → falla «aplicada aprueba» o S4;
  - (d) sin la comprobación de la decisión 1 → falla «entrada mala».

  En la bitácora va, para cada una, el comando, la prueba que falla con su mensaje y la vuelta a verde.
  ```bash
  git diff --stat -- verificador/src/main
  ```
  Esperado: en el worktree, solo `Verificador.java` y `VeredictoPlugin.java` (sin mutaciones).

- [ ] **CA-4**: R-V5 deja escritas las decisiones 1, 3 y 5
  ```bash
  sed -n '/## R-V5/,/## R-V6/p' aidlc-docs/construction/U01-verificador/functional-design/business-rules.md
  ```
  Esperado: dice que la alternativa de core se calcula con el rango del plugin pedido, que el veredicto
  de entrada debe ser el de `verificar` para esa selección, y que un plugin sin `VERSION_INCOMPATIBLE`
  nunca trae alternativas.

- [ ] **CA-5**: el verificador sigue sin Spring ni Fabric8 en ejecución
  ```bash
  ./gradlew :verificador:dependencies --configuration runtimeClasspath | grep -ciE "spring|fabric8"
  ```
  Esperado: `0`.

- [ ] **CA-6**: todo el verificador sigue pasando, sin caché
  ```bash
  ./gradlew :verificador:test --rerun :verificador:check
  ```
  Esperado: `BUILD SUCCESSFUL`, sin pruebas saltadas en ningún XML.

- [ ] **CA-7**: el diff queda acotado
  ```bash
  git diff --stat origin/main...HEAD
  ```
  Esperado: `Verificador.java` y `VeredictoPlugin.java` en `src/main`; `AlternativasTest` y
  `VeredictoTest` en `src/test`; `business-rules.md`, `CANDIDATAS.md`, la bitácora y
  `revisiones/U01-T10/`. Nada más.

---

## Plan de pruebas

- `AlternativasTest`: casos de ejemplo de JUnit sobre `CatalogoEnMemoria`, con el catálogo de la suite
  y el ampliado de CA-1.
- `VeredictoTest`: los tres casos nuevos de CA-2.

**Rojo primero:** prueba antes que código. Se registra CA-1 fallando por compilación (`alternativas` no
existe) y luego pasando; CA-3 da los cuatro pares falla-pasa.

---

## Notas

- Paquete: `io.synkra.verificador`. `alternativas` es estático, como `verificar`.
- `Version` ya es `Comparable`; no hace falta otro orden.
- El core no llama a `alternativas` (R-V6). Lo usa U07.
