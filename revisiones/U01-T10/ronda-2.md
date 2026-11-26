# Ronda 2 — U01-T10

VEREDICTO: VERDE

F-01 está cerrado. Las seis variantes de orden de lista (findFirst y findLast, en plugin, en core y en los dos) ahora fallan. Corrí yo todos los criterios de aceptación y todos pasan. Repetí el banco de la ronda 1 completo y le sumé 7 mutaciones nuevas. No queda ningún ROJO ni NARANJA. Quedan dos AMARILLO: la prueba nueva de nulos es parcialmente vacía, y un orden por texto sobrevive.

Datos de la revisión:
- **Revisión:** HEAD `7c0eca1`; el código está en `ca0e825`; base `origin/main` `1dee7b4`.
- **Hash de la tarea:** `d04fa61…` sin cambios.
- **Worktree al terminar:** limpio (`git status --short` vacío).

## Diff de esta ronda (`git diff 795e23a 7c0eca1`)
- **`Verificador.java`:** `requireNonNull` con mensaje sobre `versionesPlugin`, `versionesCore` y `descriptor` (este último con `sinNulo`). Sin cambios de lógica. `verificar` sigue sin líneas borradas.
- **`AlternativasTest.java`:**
  - `masAltaYMasBajaDeVerdadSinImportarElOrden` corre el catálogo en orden directo e inverso.
  - Prueba nueva `catalogoQueDevuelveNullEsErrorConMensaje`.
- **R-V5:** «elegido» pasa a «**pedido**».
- **Bitácora:** sección «Ronda 2».
- **`revisiones/U01-T10/`:** `ronda-1.md` y `ronda-1-respuesta.md`. Las dos están permitidas por `loop/orquestador.md:168-170`.

La evidencia de la bitácora tiene una edad coherente con el diff. La copia `u01t10c2` del codificador se creó a las 18:44:48–50, justo después del commit `ca0e825` (18:44:48). Sus `Verificador.java` y `AlternativasTest.java` son idénticos a los de HEAD (comprobado con `diff`).

## Criterios de aceptación, verificados por mí
| # | Criterio | Comando que corrí | Resultado |
|---|---|---|---|
| 1 | Pruebas de alternativas | `./gradlew :verificador:test --rerun --tests '*AlternativasTest'` + grep del XML | pasa: `BUILD SUCCESSFUL`, `tests="15" skipped="0" failures="0" errors="0"` |
| 2 | Invariante C-22 | `./gradlew :verificador:test --rerun --tests '*VeredictoTest'` | pasa: 17/0/0/0. La fila C-22 sigue cerrada |
| 3 | Mutaciones (a) a (d) | banco completo sobre `git archive HEAD` en `u01t10r2rev/copia` | pasa: (a) y (b) fallan en `masAltaYMasBajaDeVerdadSinImportarElOrden`; (c) hace fallar 10 pruebas; (d) falla en `veredictoAjenoOConAlternativasSeRechaza`. La copia sin mutar da `BUILD SUCCESSFUL`. `git diff --stat origin/main...HEAD -- verificador/src/main` muestra solo `Verificador.java` y `VeredictoPlugin.java` |
| 4 | R-V5 | `sed -n '/## R-V5/,/## R-V6/p' …` | pasa: «pedido» (2 veces), «exactamente `verificar(seleccion, catalogo)`» y «nunca trae alternativas» |
| 5 | Sin Spring ni Fabric8 | `./gradlew :verificador:dependencies --configuration runtimeClasspath \| grep -ciE "spring\|fabric8"` | pasa: `0` |
| 6 | Verificador entero | `./gradlew :verificador:test --rerun :verificador:check` | pasa: `BUILD SUCCESSFUL`; 10 XML, tests=152, skipped=0, failures=0, errors=0 |
| 7 | Diff acotado | `git diff --stat origin/main...HEAD` | pasa: 9 archivos, los 7 de la ronda 1 más `revisiones/U01-T10/ronda-1.md` y `ronda-1-respuesta.md` |

Los tiempos de alrededor de 1 s se explican: es JUnit en memoria y Gradle reporta `1 executed`.

## Banco de mutaciones (28, cada una revertida; `u01t10r2rev/mutaciones.txt`)
| Mutación | Resultado |
|---|---|
| a, b, c, d (CA-3) | detectadas |
| findFirst en plugin, en core y en los dos (F-01) | **detectadas** (antes sobrevivían) |
| findLast (`reduce((x, y) -> y)`) en plugin, en core y en los dos: el máximo confundido con el último elemento | **detectadas**: el orden directo atrapa el «último» del plugin y el inverso el del core, y al revés |
| f rango de la alternativa, g sin `core == seleccion.core()`, h, i y q mensaje/`campo`/motivo de «ninguna», j orden core-plugin, k y l variantes de la decisión 1 (incluido «otro catálogo»), m descriptor inexistente, n C-22 solo si `compatible`, o globales perdidos, p «ninguna» si falta una | detectadas, igual que en la ronda 1 |
| r `versionesCore(pedido.core())`, s `versionesPlugin(pedido.core(), …)` | sobreviven; **equivalentes**: con `VERSION_INCOMPATIBLE`, `pedido.core() == seleccion.core()` |
| v sin `requireNonNull` en `versionesCore` | detectada |
| t comparar versiones por `toString` | **sobrevive** → F-06 |
| u sin `requireNonNull` en `versionesPlugin` | **sobrevive** → F-05 |
| w `sinNulo` devuelve `d` sin comprobar | **sobrevive** → F-05 |

Por qué la prueba de orden ya distingue: hay 2 versiones compatibles de plugin (1.2.0, 1.3.0) y 2 de core (3.0.0, 3.1.0), y la prueba corre los dos órdenes. Cualquier selección por posición (la primera o la última) falla en alguno de los dos.

## Estado de los hallazgos de la ronda 1
- **F-01 NARANJA:** cerrado. Reproducido arriba.
- **F-02 AMARILLO:** cerrado a medias. El código está bien, pero la prueba solo protege `versionesCore`; ver F-05.
- **F-03 AMARILLO:** cerrado.
- **F-04 AMARILLO:** cerrado. La bitácora pega el sed de CA-4 y la vuelta a verde.

## Hallazgos
### F-05 · AMARILLO · `AlternativasTest.java` (`catalogoQueDevuelveNullEsErrorConMensaje`) · dos de las tres comprobaciones de nulos no las protege ninguna prueba
- **Mutación w** (`sinNulo` devuelve `d` sin comprobar): la prueba sigue en verde. El NPE implícito de la JDK (helpful NullPointerException) también contiene la palabra «Optional». Mi sonda lo muestra: `Cannot invoke "java.util.Optional.filter(...)" because the return value of "…Verificador.sinNulo(java.util.Optional)" is null`. La aserción `contains("Optional")` no distingue el mensaje propio del implícito.
- **Mutación u** (sin `requireNonNull` en `versionesPlugin`): ninguna prueba la cubre.

La respuesta a F-02 dice «con su prueba» para los tres; eso es cierto solo para `versionesCore`. Arreglo barato: afirmar el prefijo propio (`"Catalogo.descriptor devolvió null"`) y agregar el caso de `versionesPlugin`. No bloquea, igual que F-02.

### F-06 · AMARILLO · catálogos de `AlternativasTest` · el orden por texto no se distingue del orden por versión
Todas las versiones de la prueba tienen componentes de una sola cifra. Por eso `max` o `min` con `Comparator.comparing(Version::toString)` dan lo mismo que con `naturalOrder()`, y la mutación t sobrevive. Una versión como `1.10.0` frente a `1.9.0` la atraparía. Riesgo bajo: el código usa `Version` comparable, que ya está probado en U01-T03.

## Tareas candidatas (defectos reales fuera de alcance)
- Ninguna.

## Rutas de transcripciones largas
- /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/u01t10r2rev/ca1.log, ca2.log, ca6.log
- /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/u01t10r2rev/mutaciones.txt y mut-*.log
- /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/u01t10r2rev/sonda-npe.log
- Scripts: /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/u01t10r2rev/bin/mut.py, /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/u01t10r2rev/bin/run.sh

```
VEREDICTO: VERDE
AMARILLO|AlternativasTest.java:catalogoQueDevuelveNullEsErrorConMensaje|Las comprobaciones de nulos de descriptor y versionesPlugin no las protege ninguna prueba (mutaciones w y u sobreviven)
INFORME: revisiones/U01-T10/ronda-2.md
```
