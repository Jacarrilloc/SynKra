# U01-T11: Propiedades del verificador (PBT-03)

**Unidad:** U01: `Verificador y suite`
**Historias que implementa:** H-08
**Depende de:** U01-T10

---

## Alcance

**Dentro** (una línea, concreta):

> `VerificadorPropiedades` con jqwik: `verificar` es determinista y no depende de otras llamadas, respeta el orden recibido, y toda alternativa sugerida aplicada deja ese plugin compatible (y si no sugiere ninguna, es porque no la hay), sobre catálogos generados y sobre el de la suite.

**Fuera** (lo que el codificador debe rechazar aunque lo vea roto):

- Cambios en `src/main`. Si una propiedad encuentra un contraejemplo real, se registra en la bitácora con la semilla y el contraejemplo reducido, y se para: el orquestador decide si es un defecto de U01-T09/T10 o de la propiedad.
- La suite S1–S12 en `testFixtures` (U01-T12).
- Nuevas dependencias (jqwik ya está).

---

## Archivos de contexto

- `aidlc-docs/construction/U01-verificador/functional-design/business-logic-model.md` (tabla de propiedades, PBT-03, PBT-07, PBT-08)
- `aidlc-docs/construction/U01-verificador/functional-design/business-rules.md` (R-V4 y R-V5 con sus decisiones)
- `verificador/src/main/java/io/synkra/verificador/Verificador.java`
- `verificador/src/testFixtures/java/io/synkra/verificador/CatalogoEnMemoria.java`
- `verificador/src/test/java/io/synkra/verificador/Generadores.java` (se reutiliza; sin `filter` ni `Assume`)

---

## Criterios de aceptación

- [ ] **CA-1**: las propiedades pasan, sin caché
  ```bash
  ./gradlew :verificador:test --rerun --tests '*VerificadorPropiedades'
  ```
  Esperado: `BUILD SUCCESSFUL`. Propiedades mínimas, cada una con `@Property(tries = 1000)`:
  - **Determinismo:** `verificar(s, c)` dos veces, y sobre una copia nueva de `c`, da veredictos `equals`;
    verificar otras selecciones en medio no lo cambia.
  - **Orden:** los descriptores de `porPlugin` son los de `s.plugins()` sin las apariciones repetidas, en
    el mismo orden; y hay un `PLUGIN_REPETIDO` global por cada nombre repetido.
  - **Un motivo por plugin** y `global = APROBADA` ⇔ ningún motivo (decisión 2 de U01-T09).
  - **Alternativa aplicada aprueba:** para cada plugin con `VERSION_INCOMPATIBLE`, cada alternativa
    sugerida, aplicada a la selección, deja ese plugin compatible al volver a llamar a `verificar`.
  - **Completitud:** si no se sugiere alternativa de plugin, ninguna versión del plugin en la carpeta del
    core sirve; si no se sugiere de core, ninguna versión registrada del core cabe en el rango. Se
    comprueba recorriendo el catálogo entero, sin llamar a `alternativas`.
  - **Mínimo y máximo:** la alternativa de plugin es la más alta que sirve y la de core la más baja,
    contra el mismo recorrido.

- [ ] **CA-2**: lectura de vuelta del reporte: 1000 intentos por propiedad, ninguno descartado, semilla registrada
  ```bash
  grep -o -E 'tries = [0-9]+|checks = [0-9]+|seed = [-0-9]+' verificador/build/test-results/test/TEST-*VerificadorPropiedades.xml
  grep -ho 'tests="[0-9]*" skipped="[0-9]*" failures="[0-9]*" errors="[0-9]*"' verificador/build/test-results/test/TEST-*VerificadorPropiedades.xml
  ```
  Esperado: una línea `tries = 1000`, una `checks = 1000` y una `seed = ` por propiedad; `tests` ≥ 6
  con `skipped="0" failures="0" errors="0"`.

- [ ] **CA-3**: el generador cubre los casos que importan. Una prueba de ejemplo
  `generadorCubreLosCodigos` toma 1000 pares `(Seleccion, Catalogo)` del mismo generador
  (`sampleStream()`) y afirma que aparecen, al menos una vez cada uno: `APROBADA`; cada uno de los
  cuatro códigos del verificador; una selección vacía; un nombre repetido tres veces; un
  `VERSION_INCOMPATIBLE` con alternativa de plugin y de core, uno con solo una de las dos y uno sin
  ninguna; y un plugin cuya versión compatible está solo en la carpeta de otro core.
  ```bash
  ./gradlew :verificador:test --rerun --tests '*VerificadorPropiedades.generadorCubreLosCodigos'
  ```
  Esperado: `BUILD SUCCESSFUL`.

- [ ] **CA-4**: las propiedades detectan regresiones. En una copia `git archive HEAD` fuera del worktree
  se aplica cada mutación por separado, se corre CA-1 y se revierte:
  - (a) la alternativa de plugin no comprueba `coreCompatible` → falla «alternativa aplicada aprueba»;
  - (b) la alternativa de core toma la más alta → falla «mínimo y máximo»;
  - (c) `verificar` ordena `porPlugin` por nombre → falla «orden».

  En la bitácora va, para cada una, el comando, la propiedad que falla con el contraejemplo reducido y la
  semilla, y la vuelta a verde.
  ```bash
  git diff --stat -- verificador/src/main
  ```
  Esperado: vacío.

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
  Esperado: `VerificadorPropiedades.java` y, si hace falta, `Generadores.java` en `src/test`; la
  bitácora y `revisiones/U01-T11/`. Nada más.

---

## Plan de pruebas

- Generador de catálogos: 1 a 2 cores, cada uno con 1 a 4 versiones; 1 a 4 plugins por core, cada uno
  con 1 a 3 versiones y un rango de `Generadores.rangos()`. Versiones con componentes pequeños (`0..5`)
  para que los rangos acierten a menudo. El catálogo de la suite entra como uno de los posibles.
- Generador de selecciones sobre un catálogo dado: core y `versionCore` tomados del catálogo o una
  versión cualquiera; 0 a 5 plugins, la mayoría del catálogo, algunos con un nombre que no está, otros de
  la carpeta de otro core, y repeticiones.
- Sin `filter` ni `Assume`, para que `checks = tries` (como en U01-T07).

**Rojo primero:** las propiedades se escriben antes de mirar casos concretos; CA-4 da los tres pares
falla-pasa. Si alguna propiedad sale roja contra el código actual, ver «Fuera».

---

## Notas

- Paquete: `io.synkra.verificador`.
- La semilla cambia en cada ejecución y queda en el reporte (PBT-08); no se fija.
- Si los 1000 intentos de alguna propiedad tardan más de 30 s, se anota en la bitácora con el tiempo
  del XML.
