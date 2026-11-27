# U02-T05: la suite de U01 aplicada al arranque

**Unidad:** U02: `Core, contrato y plugins de país`
**Historias que implementa:** H-21, H-10 (contribuye)
**Depende de:** U02-T04

---

## Alcance

**Dentro** (una línea, concreta):

> Una prueba parametrizada `ConcordanciaSuiteTest` en `core/src/test` que recorre `SuiteCombinaciones.casos()` de U01 y, para cada caso que se puede ejecutar contra el arranque, arma `/config` y `/plugins` en un `@TempDir`, arranca `ArranquePlugins` y comprueba que el resultado (arranca o no, y los códigos de motivo del log) coincide con el veredicto esperado (concordancia motor-core, R1).

**Fuera** (lo que el codificador debe rechazar aunque lo vea roto):

- Cambiar `ArranquePlugins` para que un caso pase. Si un caso no concuerda, la prueba falla y la bitácora lo explica; el arreglo es otra tarea (se reporta al orquestador).
- Cambios en `verificador/` (incluida la suite), `plugin-api/` o `backend/`.
- `iniciar`, readiness y endpoints (U02-T06 y T07).

---

## Archivos de contexto

- `aidlc-docs/construction/plans/U02-core-plugins-code-generation-plan.md` (U02-T05)
- `aidlc-docs/construction/U01-verificador/functional-design/` (R1 y la suite S1 a S12)
- `verificador/src/testFixtures/java/io/synkra/verificador/SuiteCombinaciones.java` (`Caso`, `Entrada`, `Esperado`, `Origen`)
- `core/src/main/java/io/synkra/core/` (`ArranquePlugins`, `CargadorPlugins`, `VersionCore`)
- `core/src/test/java/io/synkra/core/ArranquePluginsTest.java` (cómo se arman los JAR en `@TempDir`)
- `CANDIDATAS.md` (C-71: la versión del core en la tarea `test`; C-77)

---

## Decisiones del orquestador

1. **Qué casos se ejecutan.** Se ejecutan los `PorSeleccion` y los `PorDescriptor` cuyo core del catálogo es `synkra-core` en la versión compilada. Los `FueraDeU01` y los que piden otra versión del core se **saltan con nombre**: la prueba los cuenta y los lista en su salida, y una segunda prueba afirma que el número de casos ejecutados es mayor que 0 y que cada caso de la suite está ejecutado o en la lista de no ejecutables con su razón. Un caso que desaparece sin razón es un fallo.
2. **Cómo se juzga.** `APROBADA` esperada: el contexto arranca y los plugins cargados son exactamente los de la selección. `RECHAZADA` esperada: el contexto no arranca y el log contiene cada `CodigoMotivo` esperado, ligado al plugin cuando el caso lo nombra (F-06 de U02-T04). Rechazo del lector (`rechazaElLector()`): el contexto no arranca y el motivo nombra el descriptor.
3. **Versión del core.** Se usa la versión compilada (`VersionCore`), no una variable de entorno. Si C-71 hace que la tarea `test` herede otra versión, la prueba lo detecta y falla con un mensaje claro; si hace falta, se registra una tarea `Test` propia en `core/build.gradle.kts` y lo dice la bitácora.
4. **Dependencia.** `core` usa `testImplementation(testFixtures(project(":verificador")))`. Es el único cambio de build permitido.

---

## Criterios de aceptación

- [ ] **CA-1**: la suite concuerda con el arranque
  ```bash
  ./gradlew :core:test --rerun --tests '*ConcordanciaSuiteTest'
  grep -ho 'tests="[0-9]*" skipped="[0-9]*" failures="[0-9]*" errors="[0-9]*"' core/build/test-results/test/TEST-*ConcordanciaSuiteTest.xml
  ```
  Esperado: `BUILD SUCCESSFUL`, `skipped="0"` (los no ejecutables no se marcan como omitidos: se listan), sin fallos, y `tests` igual al número de casos ejecutables más las pruebas de conteo. La bitácora pega la lista de casos ejecutados y la de no ejecutables con su razón.

- [ ] **CA-2**: la prueba muerde (lectura de vuelta del rojo)
  En una copia (sin `git stash`), se cambia `ArranquePlugins` para que acepte siempre la selección, y se corre CA-1. Esperado: fallan al menos los casos con `RECHAZADA` esperada, por aserción y no por compilación. Se pega la salida y el número de fallos.

- [ ] **CA-3**: el resto del build sigue pasando
  ```bash
  ./gradlew :verificador:test --rerun :verificador:check :plugin-api:test --rerun :plugin-api:check :core:test --rerun :core:check
  for f in core/build/test-results/test/TEST-*.xml; do grep -Ho 'tests="[0-9]*" skipped="[0-9]*" failures="[0-9]*" errors="[0-9]*"' "$f"; done
  ```
  Esperado: `BUILD SUCCESSFUL`, sin saltos ni fallos. Se pega la salida tal cual.

- [ ] **CA-4**: el diff queda acotado
  ```bash
  git rev-parse --short HEAD; git diff --stat <base-del-despacho>...HEAD
  ```
  Esperado: `core/src/test/**`, `core/build.gradle.kts` (solo la dependencia de la decisión 4 y, si hace falta, la tarea de C-71), la bitácora y `revisiones/U02-T05/`. Se anota el sha. La base es la que da el despacho, no `origin/main`.

---

## Plan de pruebas

- `@ParameterizedTest` con `@MethodSource` sobre los casos ejecutables, con el id del caso en el nombre de la prueba. Cada caso arranca un `SpringApplicationBuilder` apuntando `synkra.core.config` y `synkra.core.plugins` a su `@TempDir`, como `ArranquePluginsTest`.

**Rojo primero:** CA-2 es el rojo.
