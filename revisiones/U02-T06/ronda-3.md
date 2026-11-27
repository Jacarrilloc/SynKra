# Ronda 3 — U02-T06

VEREDICTO: VERDE

**Contexto de la revisión**
- Hash de la tarea: `git hash-object` da c05f5ff56958720575de95a6a1fac0b9a0a59ecb, que coincide.
- Rama m6r/U02-T06 en 8cfdb03. Base 15ca50f.
- `git diff --quiet d9f9e0c HEAD -- core settings.gradle.kts verificador plugin-api backend plugins` no muestra diferencias. El commit 8cfdb03 solo toca la bitácora, la evidencia y la respuesta.
- Gradle corrió con HOME y `-Duser.home` en mi scratchpad y con `GRADLE_USER_HOME=/root/.gradle`.
- Cada mutante corrió en su propia copia, sacada con `git archive d9f9e0c` fuera del worktree.
- Al terminar, `git status --short` en el worktree no muestra nada: quedó limpio.
- No usé kubectl ni el clúster, y no toqué /home/user/wt-m6r-U05-T06.

## Criterios de aceptación, verificados por mí
| # | Criterio | Comando que corrí | Resultado |
|---|---|---|---|
| 1 | Readiness 503, liveness 200, log, detener, casos verdes | `./gradlew :core:test --rerun --tests '*FalloInicioPluginTest'` + grep del XML | pasa: BUILD SUCCESSFUL, `tests="7" skipped="0" failures="0" errors="0"`. Cada caso tarda entre 0,28 y 2,87 s porque arranca un servidor real, así que no hay casos saltados |
| 2 | Sin la captura en `iniciar`, CA-1 falla **por aserción** | Mutante propio `sin-captura` | pasa: 5 de 7 fallan con `AssertionFailedError` en `assertDoesNotThrow` (línea 73) |
| 3 | El resto del build sigue pasando | `:verificador:test --rerun :verificador:check :plugin-api:test --rerun :plugin-api:check :core:test --rerun :core:check` | pasa: BUILD SUCCESSFUL en 54 s, todo con skipped=0, failures=0, errors=0 (detalle debajo de la tabla) |
| 4 | El diff queda acotado | `git rev-parse --short HEAD; git diff --stat 15ca50f...HEAD` | pasa: 8cfdb03, 19 archivos, solo `core/**`, `bitacoras/U02-T06*` y `revisiones/U02-T06/`. Lo comprobé filtrando los nombres con grep: no queda nada más. `settings.gradle.kts` no cambia porque el fixture está en `@TempDir` |

Conteos de CA-3:
- **core:** ArranquePlugins 10, CargadorPlugins 10, ConcordanciaSuite 10, FalloInicioPlugin 7, VersionCore 4.
- **verificador:** 13 clases, todas en verde.
- **plugin-api:** ContratoPlugin 4.

## Estado de los hallazgos de la ronda 2
- **F-06 (NARANJA, «no seguir con los demás»): cerrado.**
  - Nuevo caso `siFallaElDelMedioNoSeIniciaElSiguiente`, que espera exactamente `[iniciar:kyc-a, iniciar:kyc-b, detener:kyc-a]`.
  - Mi mutante `sin-return` muere: 1 de 7, `AssertionFailedError` en `FalloInicioPluginTest.java:169`. Es la misma línea que cita `ronda3-mutante-sin-return.txt`.
- **F-07 (NARANJA, DOWN en vez de OUT_OF_SERVICE): cerrado.**
  - `Health.outOfService()` en el código.
  - La prueba lee el cuerpo de la readiness (`"status":"OUT_OF_SERVICE"`, 503) y de la liveness (`"status":"UP"`, 200), y afirma 503 en el agregado.
  - Mi mutante `down` muere: 1 de 7, línea 138, la misma que cita `ronda3-mutante-down.txt`.
  - El agregado en 503 se ajusta al arbitraje del orquestador y está documentado en el Javadoc de `PluginsHealthIndicator` y en la bitácora.
- **F-08 (AMARILLO, rutas borradas): cerrado.** Las 3 citas llevan «(en d2554a3)».
- **F-09 (AMARILLO, falta el grep en `ca1.txt`): cerrado.** `ca1.txt` termina con el grep y `tests="7" skipped="0" failures="0" errors="0"`, que coincide con mi ejecución. `ca3.txt` muestra FalloInicioPlugin con 7 casos.
- **F-10 (AMARILLO, literal del detalle): cerrado.** Ahora es la constante `DETALLE_FALLO` y nombra el plugin con `pluginFallido()`.

## Mutantes de regresión sobre d9f9e0c (todos mueren)
| Mutante | Fallos de 7 |
|---|---|
| `indicador-up` | 5 |
| `orden-directo` | 2 |
| `detener-sin-captura` | 1 |
| `sin-detener` | 4 |
| `sin-grupo` (sin `group.readiness.include`) | 5 |
| `log-sin-causa` | 2 |

Todos los fallos son por aserción y ninguno tiene casos saltados.

## Hallazgos nuevos
### F-11 · AMARILLO · `core/src/main/java/io/synkra/core/IniciadorPlugins.java:19` · Javadoc desactualizado
El Javadoc de la clase sigue diciendo que el indicador «reporta DOWN mientras `hayFallo()` sea cierto», pero desde F-07 reporta OUT_OF_SERVICE. Lo encontré con `grep -rn DOWN core/src/main`, y es la única aparición. No bloquea.

### F-12 · AMARILLO · `FalloInicioPluginTest.java` (bloque del agregado, cerca de la línea 143) · El agregado solo se comprueba por código HTTP
La prueba afirma `503` en `/actuator/health` pero no lee el cuerpo. En cambio, el Javadoc de `PluginsHealthIndicator` afirma «503 OUT_OF_SERVICE» para el agregado. No lo verifiqué por HTTP en esta ronda. Si el estado del agregado importa a futuros consumidores (U02-T02, U05-T03), conviene afirmar también el `status`. No bloquea: el arbitraje solo exige que la liveness siga en 200 UP, y eso sí se lee del cuerpo.

Ninguno de los dos hallazgos es ROJO o NARANJA. No hay desborde de alcance, ni literales nuevos sin nombre en puntos de decisión, ni pruebas saltadas.

## Tareas candidatas (defectos reales fuera de alcance)
- Al cerrar el contexto, los plugins que se iniciaron bien nunca reciben `detener()`, porque `ArranquePlugins.cerrar` solo cierra los cargadores. Sigue abierta desde la ronda 1.

## Rutas de transcripciones largas
Todas en /tmp/claude-0/-home-user/311bec3f-8f32-5470-935c-db8e1e6d6a2d/scratchpad/rev-U02-T06-r3/:
- ca1.log y ca3.log
- Un log por mutante: sin-captura.log, sin-return.log, down.log, indicador-up.log, orden-directo.log, detener-sin-captura.log, sin-detener.log, sin-grupo.log y log-sin-causa.log
- Scripts: mutar.py y correr.sh
- Las copias mutadas están en mut/<mutante>/

```
VEREDICTO: VERDE
AMARILLO|core/src/main/java/io/synkra/core/IniciadorPlugins.java:19|Javadoc dice "reporta DOWN"; el indicador reporta OUT_OF_SERVICE
AMARILLO|core/src/test/java/io/synkra/core/FalloInicioPluginTest.java:143|El agregado /actuator/health solo se afirma por código 503, no por status
INFORME: revisiones/U02-T06/ronda-3.md
```
