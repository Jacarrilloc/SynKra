# Ronda 2 — U01-T07

VEREDICTO: VERDE

Corrí los 7 criterios en el worktree y todos pasan. F-01, F-02 y F-03 quedan cerrados, y lo comprobé con mutaciones en una copia fuera del worktree. Los cambios de F-05 y F-06 no rompieron nada ni bajaron la detección. Lo único que queda son dos observaciones AMARILLO, que no bloquean.

Comprobaciones previas: el hash de la tarea coincide (d7baf898…) y HEAD está en 0cc0ff6. El diff `41596ac...HEAD` toca solo `src/test` (3 archivos), los dos documentos de diseño, la bitácora y las revisiones. Nada en `src/main` ni en `build.gradle.kts`, sin semilla fija (`grep -rn 'seed *=' verificador/src/test` sale vacío) y sin `testFixtures`. Al terminar, `git status --short` sale vacío; solo quedan `build/`, `.gradle/` y `verificador/.jqwik-database`, que están ignorados.

## Criterios de aceptación, verificados por mí
| # | Criterio | Comando que corrí | Resultado |
|---|---|---|---|
| 1 | Propiedades del descriptor | `./gradlew :verificador:test --rerun --tests '*DescriptorPropiedades'` | pasa (BUILD SUCCESSFUL) |
| 2 | 1000 intentos, ninguno descartado, semilla | los dos `grep` de CA-2 | pasa: `desdeYamlNuncaLanza` y `idaYVuelta` dan cada una `tries = 1000` y `checks = 1000`, con `seed = -1480894060853207668` y `seed = 7597754693314229859`; `tests="3" skipped="0" failures="0" errors="0"`; tiempos 0,82 s, 0,51 s y 1,54 s |
| 3 | Cobertura de los textos ambiguos | `--tests '*…generadorCubreTextosAmbiguos'` | pasa, `tests="1" skipped="0"` |
| 4 | Se detecta F-07 | mutación en la copia (quité la línea 328, `.setSchema(new CoreSchema())` de `aYaml`), 4 corridas, y `git diff --stat -- verificador/src/main` en el worktree | pasa: falla 4 de 4 y las 4 reducen a `etiquetas={007=~}`, `descripcion=Optional.empty`; diff vacío |
| 5 | `RangoVersionPropiedades` | `--tests '*RangoVersionPropiedades'` y su `grep` | pasa: 2×`tries = 1000`, 2×`checks = 1000`, skipped 0 |
| 6 | Diseño (C-09 y C-16) | los dos `grep` de CA-6 | pasa: línea 62 de PBT-08 («La semilla cambia en cada ejecución y se registra…») y línea 39 de R-V3, con los tres ejemplos |
| 7 | Todo el verificador | `./gradlew :verificador:test --rerun :verificador:check` | pasa: se ejecutó `> Task :verificador:test`. Los 5 XML tienen marcas de tiempo de esta corrida (17:14Z), 3/28/2/36/16 pruebas, skipped 0 y failures 0 en todos |

## Hallazgos de la ronda 1, comprobados de nuevo
- **F-01 (cerrado).** Con la mutación N+2 en la copia (`Math.addExact(n, 2)` en la línea 53 de `RangoVersion`), 6 corridas sin `.jqwik-database`: las 6 reducen a `arg0: 0, arg1: 1, arg2: 0.0.0, arg3: false`, es decir, llegan a `0, 1`. Como `mismoMayor` desapareció, probé si se perdió detección en la rama del mismo mayor, con dos mutaciones más:
  - mínimo de `N.x` cambiado a `n.1.0`: se detecta en 8 de 8 corridas;
  - máximo de `N.x` cambiado a `n.45.0`: se detecta en 6 de 6, y la reducción da `0, 0, 0.45.0`.
- **F-02 (cerrado).** El generador real dio 0 fallos en 100 muestras de 5000. En otra tanda de 200 muestras, los conteos de bool y float en `descripcion` tienen media de unos 35 y varianza de unos 35, así que siguen una distribución de Poisson; el mínimo observado fue 16 para float y 18 para bool. La probabilidad real de fallo por categoría es del orden de e^-35 ≈ 6e-16, muy por debajo de 1e-6. El cálculo del comentario tiene una premisa equivocada (ver F-08), pero la conclusión se mantiene.
- **F-03 (cerrado).** Las dos mutaciones se detectan:
  - M2 (`String excesiva = null;` en la línea 95): falla 4 de 4 con `java.lang.StackOverflowError`;
  - M1 (cambié `catch (RuntimeException e)` de la línea 107 por `catch (java.util.ConcurrentModificationException e)`, lo que equivale a quitarlo): falla 3 de 3 con ParserException y ReaderException, y una reducción da `"!!nombre: '007' …"`.

  La propiedad hostil sigue en `checks = tries = 1000` y tarda entre 0,26 y 1,5 s.
- **F-04.** Mejoró: en 4 de 4 corridas la reducción llega a una sola etiqueta, `{007=~}`.
- **F-05 y F-06.** No rompieron nada. Las ramas de `mayorQue` están protegidas cuando un componente vale `MAX_COMPONENTE` (`between(0, -1)` no puede ocurrir). La ida y vuelta pasa con los 14 textos nuevos. Bajar el peso de `comun` de 3/8 a 2/10 no redujo la detección de F-07 (4 de 4) ni la de M1/M2.

## Hallazgos
### F-08 · AMARILLO · DescriptorPropiedades.java (javadoc de `generadorCubreTextosAmbiguos`) · El cálculo usa p(descripción) = 1/2, pero la medida es 0,40
En 100 muestras de 5000, la media de descriptores con `descripcion` fue 2001,17. Por eso la media de textos bool en `descripcion` es unos 35 y no los 48 que da el comentario; el fallo real anda en e^-35 ≈ 6e-16, no en 6e-22. La cota de «menos de 1 en un millón» se cumple de sobra. Solo convendría corregir la cifra o decir que es una aproximación.

### F-09 · AMARILLO · bitacoras/U01-T07.md (Ronda 2, CA-4) · La reducción que describe la bitácora no es la que observé
La bitácora dice que el contraejemplo reducido es «descripcion con valor ambiguo», sin pegar la salida. En mis 4 corridas la reducción fue siempre `etiquetas={007=~}` con `descripcion=Optional.empty`. Con otra semilla puede salir otra cosa, pero la afirmación no trae evidencia pegada.

Observación, sin severidad: con M2, jqwik no reduce el contraejemplo, porque `StackOverflowError` es un `VirtualMachineError`. El reporte muestra la cadena original de miles de `[`. Es así como funciona jqwik, no un defecto de la tarea.

## Tareas candidatas
- No hay nuevas. C-19 y C-20 ya están registradas.

## Rutas de transcripciones largas
- Copia: /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/u01t07r2/copia
- N+2: /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/u01t07r2/n2-run1.xml … n2-run6.xml
- M1, M2 y F-07: /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/u01t07r2/m1-1.xml … m1-3.xml, m2-1.xml … m2-4.xml y f07-1.xml … f07-4.xml (con sus `.txt`)
- F-02: /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/u01t07r2/f02-muestra.txt y /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/u01t07r2/f02-cola.txt
- Criterios: /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/u01t07r2/ca1.txt, ca3.txt, ca5.txt y ca7.txt

Mis herramientas son de solo lectura, así que no creé `revisiones/U01-T07/ronda-2.md`: este texto es el informe para que lo archive el orquestador.

```
VEREDICTO: VERDE
AMARILLO|DescriptorPropiedades.java (javadoc generadorCubreTextosAmbiguos)|El cálculo supone p(descripción)=1/2; la medida es 0,40 (e^-35, no e^-48,7)
AMARILLO|bitacoras/U01-T07.md (Ronda 2, CA-4)|La reducción descrita («descripcion con valor ambiguo») no trae salida pegada; observé {007=~}
INFORME: revisiones/U01-T07/ronda-2.md
```
