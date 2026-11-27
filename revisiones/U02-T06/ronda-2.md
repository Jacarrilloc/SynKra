# Ronda 2 — U02-T06

VEREDICTO: NO-VERDE

El hash de la tarea coincide: `git hash-object` da c05f5ff56958720575de95a6a1fac0b9a0a59ecb. La rama m6r/U02-T06 está en f444036. El código de HEAD es idéntico al de dfbcd7a en `core/` y `settings.gradle.kts` (lo comprobé con `git diff --quiet dfbcd7a HEAD`). Base 15ca50f. Gradle corrió con HOME y `-Duser.home` en mi scratchpad y con `GRADLE_USER_HOME=/root/.gradle`. Cada mutante corrió en su propia copia, sacada con `git archive dfbcd7a`, fuera del worktree. Al terminar, `git status --short` en el worktree no muestra nada: quedó limpio.

## Criterios de aceptación, verificados por mí
| # | Criterio | Comando que corrí | Resultado |
|---|---|---|---|
| 1 | Readiness 503, liveness 200, log, detener, casos verdes | `./gradlew :core:test --rerun --tests '*FalloInicioPluginTest'` + grep del XML | pasa: BUILD SUCCESSFUL, `tests="6" skipped="0" failures="0" errors="0"`. Cada caso tarda entre 0,28 y 3,0 s porque arranca un servidor real, así que no hay casos saltados |
| 2 | Sin la captura en `iniciar`, CA-1 falla **por aserción** | Mutante propio `sin-captura` en una copia | pasa: 4 de 6 fallan con `AssertionFailedError at FalloInicioPluginTest.java:73` (`assertDoesNotThrow`). La `IllegalStateException` aparece solo como causa |
| 3 | El resto del build sigue pasando | `:verificador:test --rerun :verificador:check :plugin-api:test --rerun :plugin-api:check :core:test --rerun :core:check` | pasa: BUILD SUCCESSFUL en 1 min 6 s. core: ArranquePlugins 10, CargadorPlugins 10, ConcordanciaSuite 10, FalloInicioPlugin 6, VersionCore 4. verificador: 13 clases. plugin-api: 4. Todo con skipped=0, failures=0, errors=0 |
| 4 | El diff queda acotado | `git rev-parse --short HEAD; git diff --stat 15ca50f...HEAD` | pasa: f444036, 15 archivos, solo `core/**`, `bitacoras/U02-T06*` y `revisiones/U02-T06/`. No hay cambios en `verificador/`, `plugin-api/`, `backend/` ni `settings.gradle.kts` |

## Estado de los hallazgos de la ronda 1
- **F-01 (ROJO, CA-2): cerrado.** Todos los casos arrancan con `vivo()`, que es `assertDoesNotThrow`. Mi mutante sin la captura falla 4 de 6 por aserción en la línea 73. La decisión 1 («el proceso sigue vivo») ya es una aserción explícita.
- **F-02 (NARANJA, orden inverso y `detener()` que lanza): cerrado.** Los dos mutantes mueren:
  - `orden-directo`: 2 fallos, líneas 159 y 175;
  - `detener-sin-captura`: 1 fallo, línea 73, porque la excepción tumba el arranque.
  - Además, el caso nuevo afirma el log de `PREFIJO_FALLO_DETENER`, la causa original y que `detener:kyc-a` se ejecuta después del `detener` que lanza.
- **F-03 (AMARILLO, arbitrado: el resultado es 503 sin depender del orden de listeners): cerrado en lo esencial.**
  - `PluginsHealthIndicator` está en el grupo readiness y `vigilar` ya no existe.
  - `fallo = true` se pone dentro del listener de `ApplicationReadyEvent`. Spring Boot publica `ACCEPTING_TRAFFIC` en el mismo hilo después de ese evento, así que no hay ventana con 200.
  - Mutantes que mueren: `indicador-up` (4 fallos), `sin-grupo` (quita `group.readiness.include`, 4 fallos) y `sin-detener` (3 fallos).
  - El efecto colateral sobre el cuerpo de la respuesta está en F-07.
- **F-04 (AMARILLO, literal de detener): cerrado.** La constante `PREFIJO_FALLO_DETENER` existe y la prueba la usa.
- **F-05 (AMARILLO, evidencia vieja): cerrado.** `ca3.txt` muestra FalloInicioPlugin con 6 casos, que coincide con el código actual. Las líneas de los cuatro `ronda2-mutante-*.txt` coinciden con mis ejecuciones sobre dfbcd7a, así que esa evidencia pudo salir del diff actual. El detalle de `ca1.txt` está en F-09.

## Sobre el borrado de la evidencia de mutantes de la ronda 1
**No rompe la cadena de evidencia, pero la deja con referencias colgando.**
- Los archivos borrados siguen en el historial de la rama (`git ls-tree d2554a3`, que es ancestro de HEAD): `ca2-sin-captura.txt`, `ca2-mutacion-b.diff` y `ca2-mutacion-b.txt`. Se pueden recuperar con `git show d2554a3:<ruta>`.
- Lo que sí está mal: la sección «Ronda 1» de `bitacoras/U02-T06.md` sigue citando esas tres rutas como si estuvieran en el árbol, y en HEAD ya no existen.
- Si el humano fusiona con squash, la evidencia de la ronda 1 se pierde de la rama base.
- Además, la detección de renombres de git presenta `ca2-sin-captura.txt → ronda2-mutante-detener-sin-captura.txt` como un renombre, lo que confunde a quien lea el diff.
- Lo clasifico como AMARILLO (F-08), porque esa evidencia ya se juzgó en la ronda 1 y no sostiene ningún criterio actual.

## Hallazgos nuevos
### F-06 · NARANJA · `IniciadorPlugins.java:48-60` · «No seguir con los demás plugins» no tiene prueba: el mutante sobrevive
El alcance excluye expresamente «seguir con los demás plugins como si nada». Mi mutante `sigue-con-los-demas` quita el `return;` después de `detener(iniciados)` y **sobrevive**: BUILD SUCCESSFUL con `tests="6" skipped="0" failures="0" errors="0"`.
- Sobrevive porque en todos los casos de fallo el plugin que falla es el último de la lista, así que nunca se observa si el bucle sigue.
- Falta un caso como «a, b que falla, c» que espere exactamente `[iniciar:a, iniciar:b, detener:a]`, sin `iniciar:c`.
- Lo marco NARANJA y no ROJO porque CA-1 no enumera este caso. Aun así es cobertura ausente de una regla explícita de la tarea.

### F-07 · NARANJA · `PluginsHealthIndicator.java:22` · La readiness responde `DOWN`, no `OUT_OF_SERVICE` como dice el alcance
El alcance dice: «`/actuator/health/readiness` responde `503` (`OUT_OF_SERVICE`)». Lo leí de vuelta por HTTP en una copia con una prueba instrumentada (`sonda.log`):
```
/actuator/health/readiness -> 503 {"status":"DOWN"}
/actuator/health/liveness  -> 200 {"status":"UP"}
/actuator/health           -> 503 {"status":"DOWN","groups":["liveness","readiness"]}
```
- `Health.down()` cambió el estado visible respecto del que pide la tarea. Con el diseño anterior, `REFUSING_TRAFFIC`, salía `OUT_OF_SERVICE`.
- Ninguna prueba lee el cuerpo, solo el código HTTP, y por eso nada lo detectó.
- Arreglo: `Health.outOfService()` y una aserción sobre el `status` del cuerpo de la readiness.
- Efecto colateral que hay que dejar decidido en la bitácora: el agregado `/actuator/health` también pasa a 503 DOWN cuando falla un plugin. Para la sonda de liveness no importa, porque el grupo liveness solo incluye `livenessState`. Pero cualquier espera futura con `curl -sf .../actuator/health` sobre el core verá el fallo; las tareas U02-T02 y U05-T03 usan esas rutas.

### F-08 · AMARILLO · `bitacoras/U02-T06.md` (sección Ronda 1) · Rutas de evidencia borradas que se siguen citando
Ver la sección anterior. Basta anotar junto a cada cita «en d2554a3», o restaurar los archivos.

### F-09 · AMARILLO · `bitacoras/U02-T06-evidencia/ca1.txt` · Falta la salida del grep del XML
CA-1 tiene dos comandos. `ca1.txt` (32 líneas) solo trae la salida de Gradle; el conteo `tests="6" skipped="0"…` aparece únicamente como afirmación en la bitácora. Yo lo verifiqué y es correcto, pero la evidencia versionada está incompleta.

### F-10 · AMARILLO · `PluginsHealthIndicator.java:22` · Detalle `"fallo al iniciar"` en un literal
Hoy no se ve, porque `show-details` está en `never` por defecto. Si se expone en el futuro, convendría que fuera una constante o que nombrara el plugin.

## Tareas candidatas (defectos reales fuera de alcance)
- Al cerrar el contexto, los plugins que se iniciaron bien nunca reciben `detener()`, porque `ArranquePlugins.cerrar` solo cierra los cargadores. Sigue abierta desde la ronda 1.
- Decidir si el agregado `/actuator/health` del core debe caer con un fallo de plugin o si el indicador `plugins` debe quedar solo en el grupo readiness. Se configura con `management.endpoint.health.group` / `management.health.defaults`.

## Rutas de transcripciones largas
Todas en /tmp/claude-0/-home-user/311bec3f-8f32-5470-935c-db8e1e6d6a2d/scratchpad/rev-U02-T06-r2/:
- ca1.log
- ca3.log
- sin-captura.log
- orden-directo.log
- detener-sin-captura.log
- indicador-up.log
- sin-grupo.log
- sin-detener.log
- log-sin-causa.log (muere, 2 fallos)
- sigue-con-los-demas.log (sobrevive, F-06)
- sonda.log (cuerpos de las respuestas, F-07); las líneas `SONDA` están en mut/sonda/core/build/test-results/test/
- mutar.py y correr.sh (scripts de los mutantes)

```
VEREDICTO: NO-VERDE
NARANJA|IniciadorPlugins.java:48-60|«No seguir con los demás» sin prueba: el mutante sin return sobrevive 6/6
NARANJA|PluginsHealthIndicator.java:22|Readiness 503 con status DOWN, el alcance pide OUT_OF_SERVICE; ninguna prueba lee el cuerpo
AMARILLO|bitacoras/U02-T06.md|La sección Ronda 1 cita evidencia borrada del árbol (solo está en d2554a3)
AMARILLO|bitacoras/U02-T06-evidencia/ca1.txt|Falta la salida del grep del XML de CA-1
AMARILLO|PluginsHealthIndicator.java:22|Detalle "fallo al iniciar" en un literal
INFORME: revisiones/U02-T06/ronda-2.md
```
