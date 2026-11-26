# U01-T13: La suite bloquea el build

**Unidad:** U01: `Verificador y suite`
**Historias que implementa:** H-10
**Depende de:** U01-T12

---

## Alcance

**Dentro** (una línea, concreta):

> Una tarea de Gradle `verificarSuite` en `verificador/build.gradle.kts`, de la que depende `check`, que lee el XML de `SuiteCombinacionesTest` y falla si falta, si se saltó algún caso o si no corrieron los 10, y un script `scripts/control-suite.sh` que demuestra en una copia aparte que alterar S4 o desactivar la suite hace fallar `check`.

**Fuera** (lo que el codificador debe rechazar aunque lo vea roto):

- Cambios en `src/main`, en `SuiteCombinaciones` o en las pruebas existentes.
- Que `./gradlew :verificador:check -x test` falle: saltarse las pruebas a mano a propósito no entra.
- CI en GitHub Actions (U11) y la suite del lado del core (U02).
- C-25 (semilla de jqwik): se decide en el resumen, U01-T14.
- Nuevas dependencias o plugins de Gradle.

---

## Archivos de contexto

- `verificador/build.gradle.kts`
- `verificador/src/testFixtures/java/io/synkra/verificador/SuiteCombinaciones.java`
- `verificador/src/test/java/io/synkra/verificador/SuiteCombinacionesTest.java`
- `aidlc-docs/inception/user-stories/stories.md` (H-10: `check` = 0, y ≠ 0 con un caso alterado)
- `scripts/` (para seguir el estilo de `doctor.sh`)

---

## Decisiones del orquestador

1. **`verificarSuite`** depende de `test`, y `check` depende de ella. Lee
   `build/test-results/test/TEST-io.synkra.verificador.SuiteCombinacionesTest.xml` y falla con un
   mensaje que nombra la causa en cuatro casos:
   - el archivo no existe;
   - `tests` es menor que 10 más las pruebas de forma (el número exacto se fija en la tarea y se
     documenta en el script);
   - `skipped` es distinto de 0;
   - el XML es más viejo que las clases compiladas de `test` y de `testFixtures`. Así no cuenta un XML
     de una corrida anterior a un cambio de las pruebas o de la suite. No se compara con la hora del
     build, porque un `test` UP-TO-DATE deja un XML válido y más viejo que el build.

   Se escribe en Kotlin DSL, sin plugins nuevos, y se puede configurar con la caché de configuración.
2. **`scripts/control-suite.sh`** hace `git archive HEAD` a un directorio temporal y corre ahí tres
   escenarios, imprimiendo el código de salida de cada uno:
   - (1) sin cambios → `check` = 0;
   - (2) en `SuiteCombinaciones`, el esperado de S4 pasa a `kyc-pe 1.2.0` → `check` ≠ 0;
   - (3) `@Disabled` sobre `SuiteCombinacionesTest` → `check` ≠ 0 por `verificarSuite`.

   Termina con 0 solo si los tres dan lo esperado. Nunca toca el worktree y al final borra el directorio
   temporal.

---

## Criterios de aceptación

- [ ] **CA-1**: `check` pasa y corre `verificarSuite`
  ```bash
  ./gradlew :verificador:test --rerun :verificador:check --console=plain 2>&1 | grep -E 'verificarSuite|BUILD'
  ```
  Esperado: aparece `> Task :verificador:verificarSuite` y `BUILD SUCCESSFUL`. Además, un segundo
  `./gradlew :verificador:check` sin `--rerun` (con `test` UP-TO-DATE) también da `BUILD SUCCESSFUL`.

- [ ] **CA-2**: el control demuestra que la suite bloquea el build
  ```bash
  bash scripts/control-suite.sh; echo "rc=$?"
  ```
  Esperado: tres líneas, una por escenario, con el código de salida de `check` (0, ≠ 0, ≠ 0), y
  `rc=0`. La bitácora pega la salida completa. En el escenario 2, la causa del fallo es el caso S4 de
  `SuiteCombinacionesTest`; en el 3, el mensaje de `verificarSuite`.

- [ ] **CA-3**: `verificarSuite` rechaza las cuatro formas de saltarse la suite. En una copia
  `git archive HEAD` fuera del worktree, se prueba cada caso por separado:
  - (a) se borra el XML antes de `verificarSuite`, corriendo con `-x test` → falla con «no existe»;
  - (b) con el XML de una corrida verde en su lugar, se cambia S4 en `SuiteCombinaciones` y se corre
    `-x test` → falla con «más viejo que las clases» o similar;
  - (c) `@Disabled` en un solo caso parametrizado (S7) → falla con `skipped`;
  - (d) se quita S10 de `casos()` y la prueba de forma se ajusta a 11 → falla por número de pruebas.

  La bitácora pega cada comando y su mensaje. Ver la nota sobre `-x test` en «Fuera»: aquí se usa solo
  para fabricar el estado, y lo que se prueba es que `verificarSuite` lo detecta.

- [ ] **CA-4**: el script no deja rastro
  ```bash
  git status --short
  ```
  Esperado: vacío después de correr CA-2. El directorio temporal ya no existe; el script imprime su
  ruta, y la bitácora pega un `ls` que lo confirma.

- [ ] **CA-5**: el verificador sigue sin Spring ni Fabric8 en ejecución
  ```bash
  ./gradlew :verificador:dependencies --configuration runtimeClasspath | grep -ciE "spring|fabric8"
  ```
  Esperado: `0`.

- [ ] **CA-6**: todo el proyecto sigue pasando, sin caché
  ```bash
  ./gradlew :verificador:test --rerun :backend:test --rerun :verificador:check :backend:check
  ```
  Esperado: `BUILD SUCCESSFUL`, sin pruebas saltadas en ningún XML de los dos proyectos.

- [ ] **CA-7**: el diff queda acotado
  ```bash
  git diff --stat origin/main...HEAD
  ```
  Esperado: `verificador/build.gradle.kts`, `scripts/control-suite.sh`, la bitácora y
  `revisiones/U01-T13/`. Nada más.

---

## Plan de pruebas

- No hay pruebas JUnit nuevas. La prueba de esta tarea es el script de control (CA-2) más las cuatro
  formas de CA-3.
- **Rojo primero:** antes de añadir `verificarSuite`, se corre el escenario 3 del script y se registra
  que `check` da **0** con la suite desactivada. Ese es el hueco que la tarea cierra. Después da ≠ 0.

---

## Notas

- `scripts/control-suite.sh` empieza con `set -euo pipefail`, pero captura el código de salida de cada
  `check` sin abortar. Usa `mktemp -d` y `trap` para borrar el temporal.
- El script corre Gradle con `--no-daemon` o con su propio `--project-cache-dir` dentro del temporal,
  para no compartir estado con el worktree.
