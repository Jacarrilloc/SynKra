# U01-T07: Propiedad de ida y vuelta del descriptor (PBT-02)

**Unidad:** U01: `Verificador y suite`
**Historias que implementa:** H-03
**Depende de:** U01-T05, U01-T06

---

## Alcance

**Dentro** (una línea, concreta):

> La clase de pruebas `DescriptorPropiedades` (jqwik) con un generador de `Descriptor` válidos y dos propiedades de 1000 intentos: «`desdeYaml(aYaml(d))` es `Exito(d)`» (PBT-02) y «`desdeYaml` nunca lanza, para cualquier texto» (entrada hostil); los generadores de `Version` y `RangoVersion` pasan a una clase `Generadores` compartida con `RangoVersionPropiedades`, con los ajustes de C-10, y se cierran C-09 y C-16 en el diseño.

**Fuera** (lo que el codificador debe rechazar aunque lo vea roto):

- Cambios en `src/main` (`Descriptor`, `LectorDescriptor`, `Version`, `RangoVersion`, `Resultado`, `Motivo`, `CodigoMotivo`). Si una propiedad encuentra un defecto, se registra en la bitácora con el contraejemplo reducido y se para la tarea: el orquestador decide si se corrige aquí.
- Fijar una semilla constante (`@Property(seed = ...)`): la semilla cambia en cada ejecución y solo se **registra** (PBT-08).
- Propiedades del verificador (U01-T11) y la suite de casos (U01-T12).
- Generadores en `testFixtures`: viven en `src/test` hasta que otra tarea los necesite fuera de `:verificador`.
- C-13 (ruta absoluta en los mensajes de `leer`): es de U04.
- Cambios en `build.gradle.kts`: jqwik 1.10.2 ya está declarado.

---

## Archivos de contexto

- `aidlc-docs/construction/U01-verificador/functional-design/business-logic-model.md` (tabla «Propiedades (PBT parcial)»)
- `aidlc-docs/construction/U01-verificador/functional-design/business-rules.md` (R-V2 y R-V3)
- `verificador/src/main/java/io/synkra/verificador/Descriptor.java` y `LectorDescriptor.java`
- `verificador/src/test/java/io/synkra/verificador/RangoVersionPropiedades.java` (generadores actuales)
- `verificador/src/test/java/io/synkra/verificador/LectorDescriptorTest.java` (casos de ejemplo ya cubiertos)
- `CANDIDATAS.md` (C-09, C-10, C-15 y C-16)
- `revisiones/U01-T06/ronda-2.md` (F-07: dónde se rompió la ida y vuelta)

---

## Criterios de aceptación

- [ ] **CA-1**: las propiedades del descriptor pasan, sin caché
  ```bash
  ./gradlew :verificador:test --rerun --tests '*DescriptorPropiedades'
  ```
  Esperado: `BUILD SUCCESSFUL`.

- [ ] **CA-2**: lectura de vuelta del reporte: 1000 intentos por propiedad, ninguno descartado, semilla registrada
  ```bash
  grep -o -E 'tries = [0-9]+|checks = [0-9]+|seed = [-0-9]+' verificador/build/test-results/test/TEST-*DescriptorPropiedades.xml
  grep -ho 'tests="[0-9]*" skipped="[0-9]*" failures="[0-9]*" errors="[0-9]*"' verificador/build/test-results/test/TEST-*DescriptorPropiedades.xml
  ```
  Esperado: al menos 2 líneas `tries = 1000`, al menos 2 líneas `checks = 1000`, una línea `seed = `
  por propiedad, y `tests` ≥ 2 con `skipped="0" failures="0" errors="0"`.

- [ ] **CA-3**: el generador cubre los textos ambiguos de YAML 1.2 Core (C-15). Una prueba de ejemplo
  `generadorCubreTextosAmbiguos` toma 1000 descriptores del mismo generador (`Arbitraries...sampleStream()`)
  y afirma que aparecen, al menos una vez cada uno:
  - en `descripcion` y en un valor de etiqueta: un nulo (`~` o `null`), un bool (`True` o `false`),
    un entero con cero inicial u otra base (`007`, `0x1F` o `0o17`) y un float especial (`.NaN` o `-.Inf`);
  - como clave de etiqueta: `007`, `true` y `null` (cumplen el patrón de `nombre`);
  - un texto con `: `, uno con `# `, uno multilínea y uno con caracteres fuera de ASCII;
  - `descripcion` ausente, y 0 y 5 etiquetas.
  ```bash
  ./gradlew :verificador:test --rerun --tests '*DescriptorPropiedades.generadorCubreTextosAmbiguos'
  ```
  Esperado: `BUILD SUCCESSFUL`.

- [ ] **CA-4**: la propiedad detecta la regresión de F-07. En una copia de trabajo, `aYaml` vuelve a usar
  `DumpSettings` sin `CoreSchema`; CA-1 falla y el reporte muestra un contraejemplo reducido (se espera
  algo como `descripcion = "~"` o un valor `"007"`). El cambio se revierte y se pega en la bitácora el
  par falla-pasa, con el comando y la salida.
  ```bash
  git diff --stat -- verificador/src/main
  ```
  Esperado: vacío al terminar (la mutación no queda en el commit).

- [ ] **CA-5**: `RangoVersionPropiedades` sigue cumpliendo con los generadores compartidos
  ```bash
  ./gradlew :verificador:test --rerun --tests '*RangoVersionPropiedades'
  grep -o -E 'tries = [0-9]+|checks = [0-9]+' verificador/build/test-results/test/TEST-*RangoVersionPropiedades.xml
  ```
  Esperado: `BUILD SUCCESSFUL`, al menos 2 líneas `tries = 1000` y 2 líneas `checks = 1000`.

- [ ] **CA-6**: el diseño queda alineado (C-09 y C-16)
  ```bash
  grep -n 'PBT-08' aidlc-docs/construction/U01-verificador/functional-design/business-logic-model.md
  grep -n 'etiquetas' aidlc-docs/construction/U01-verificador/functional-design/business-rules.md
  ```
  Esperado: la fila PBT-08 ya no dice «Semilla fija»: dice que la semilla cambia en cada ejecución y se
  registra en el reporte. R-V3 dice en una línea que una etiqueta explícita en una clave o un valor de
  `etiquetas` que no sea `str` (`{!!int pais: pe}`, `{pais: !!int pe}`) es `DESCRIPTOR_INVALIDO`,
  a diferencia de los campos (`version: !!int 1` da `VERSION_INVALIDA`).

- [ ] **CA-7**: todo el verificador sigue pasando, sin caché
  ```bash
  ./gradlew :verificador:check --rerun
  ```
  Esperado: `BUILD SUCCESSFUL`, sin pruebas saltadas en ningún XML.

---

## Plan de pruebas

- `Generadores` (PBT-07), en `src/test`, con lo que hoy está en `RangoVersionPropiedades` más C-10:
  - `Version` con `mayor`, `menor` y `parche` en `0..50`;
  - `RangoVersion` de las cuatro formas de R-V2, con el máximo de `>=A <B` acotado a `0..50` (C-10, F-01)
    y el mayor generado como `n + desplazamiento` para que la reducción llegue a `0, 1` (C-10, F-02);
  - nombres que cumplen `^[a-z0-9]+(-[a-z0-9]+)*$`, incluidos los que parecen otro tipo (`007`, `true`, `null`);
  - textos libres que mezclan los ambiguos de C-15 con texto común.
- Generador de `Descriptor`: `nombre`, `core`, `version`, `coreCompatible`, 0 a 5 etiquetas y `descripcion`
  presente o ausente. No usa `filter` ni `Assume`, así que `checks = tries`.
- `idaYVuelta` (`@Property(tries = 1000)`): `desdeYaml(aYaml(d))` es `Exito` y su valor es igual a `d`
  (incluido el `texto` del rango).
- `desdeYamlNuncaLanza` (`@Property(tries = 1000)`): para cualquier `String` (jqwik por defecto, más
  mutaciones de un `aYaml(d)` válido: cortar, duplicar líneas, insertar `!!`, `&`, `*`, `{`, `[`),
  `desdeYaml` devuelve `Exito` o `Fallo` y no lanza. Si devuelve `Fallo`, todos sus motivos tienen un
  `mensaje` no vacío.
- `generadorCubreTextosAmbiguos`: prueba de ejemplo de CA-3, sobre el mismo generador que usa `idaYVuelta`.

**Rojo primero:** CA-3 y CA-4 son los pares falla-pasa de esta tarea, porque las propiedades se escriben
sobre código que ya existe. Para CA-3, se registra primero que el generador sin los textos ambiguos
no la cumple.

---

## Notas

- Paquete: `io.synkra.verificador`.
- jqwik escribe `tries`, `checks` y `seed` en el `system-out` del XML de Gradle (comprobado en U01-T05).
- Si `Descriptor.equals` no basta para comparar (por ejemplo, por el orden de las etiquetas), se
  registra en la bitácora y se para: no se cambia `Descriptor`.
- La lección de las olas 4 y 5: las rondas extra vinieron de entradas hostiles y regresiones que los
  criterios no cubrían. Por eso esta tarea trae la propiedad de «nunca lanza» y la mutación de F-07.
