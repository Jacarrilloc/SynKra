# Ronda 2 — U07-G

VEREDICTO: VERDE

Los seis hallazgos de la ronda 1 están corregidos y los he comprobado con mis propias corridas. Todos los criterios pasan. Quedan tres hallazgos nuevos, todos AMARILLO, que no bloquean. El hash de la tarea coincide (`8f146133a5114853422a46452d05324fbc2cd26f`). La punta es `446ee3a`. `git diff ba0b1fd HEAD -- backend` está vacío y no hay commits que toquen `backend` después de `ba0b1fd`. El worktree quedó limpio al terminar (`git status --porcelain | wc -l` = 0). Usé `JAVA_HOME=$HOME/.gradle/jdks/eclipse_adoptium-21-amd64-linux.2`.

## Criterios de aceptación, verificados por mí
| # | Criterio | Comando que corrí | Resultado |
|---|---|---|---|
| 1 | Fronteras (vigente desde la ronda 1; lo repetí) | `:backend:test --rerun --tests '*FronterasModulosTest'` y los `grep` del XML | pasa: `BUILD SUCCESSFUL`, `tests="15" skipped="0" failures="0" errors="0"`, aparecen los 3 `testcase` nuevos |
| 2 | Cada regla muerde | no lo repetí: el código no cambia desde `ba0b1fd`, así que vale la evidencia de la ronda 1 | vigente |
| 3 | Secciones del resumen | CA-3 literal | pasa: los 9 encabezados en orden y 1 vez cada uno. Las historias H-06 a H-24, las 23 candidatas y R-D1 a R-D13 salen al menos 1 vez. Los 6 términos de desviación aparecen en las líneas 244 a 251. `David\|Julian` = 0 y `casdav` = 0 |
| 4 | Salidas frescas | `--tests '*despliegue*'` más el conteo de **todos** los XML; `:backend:check`; `head -3 evidencia/*.txt`; `git log -1 -- backend` | pasa: `BUILD SUCCESSFUL in 21s`. Salen 16 XML: 15 de `despliegue`, que suman 98, y `FronterasModulosTest tests="2"`, en total **100**. Coincide clase por clase con `conteo-por-clase-despliegue.txt`. El resumen trae 3 `BUILD SUCCESSFUL` y 3 `salida=0`. Las 8 cabeceras de `evidencia/` traen fecha, sha `ba0b1fd`, `JAVA_HOME` y comando, y ese sha es el último que toca `backend` |
| 5 | Cifras con fuente | `bash bitacoras/U07-G-evidencia/ca5.sh` | pasa: ningún `FALTA` y al menos un `ok` en cada `revisiones/U07-A` a `U07-F` |
| 6 | Copias literales | `bash bitacoras/U07-G-evidencia/ca6.sh` | pasa: el bucle no imprime nada, `[...]` = 0 y la parte de `citas.txt` sale vacía |
| 7 | Todo el backend | `:backend:test --rerun :backend:check`, recuento de XML, `grep` de REQUIRES_NEW | pasa: `salida=0`, `BUILD SUCCESSFUL in 49s`, 60 XML, 0 con skipped, failures o errors distinto de 0, 623 pruebas, `requiresNew=1` |
| 8 | Diff acotado | CA-8 literal con base `2f47340`, sobre la punta | pasa: `fuera=1`, `0` y `0`. En `ca8.txt` salen el sha `3c09f5b` (el padre de `446ee3a`) y `wc -l` = 1 (solo `ca8.txt` modificado; `ca8.sh` ya estaba en el commit anterior) |

## Respuestas de la ronda 1, comprobadas
- **F-01, resuelto.** La línea 142 dice ahora que el filtro casa con nombres de método y nombra las dos pruebas de `FronterasModulosTest`. Mi conteo de todos los XML da 100, igual que el suyo (ver CA-4).
- **F-02, resuelto.** Repetí con `grep -cF` las 31 entradas de `citas.txt` contra su fuente y todas dan al menos 1. «ConfirmacionServicio sin cambios: lo acepto» está en `revisiones/U07-F/ronda-1.md`. La cita de C-87 dice ahora «en este camino», que es literal. Probé además 11 citas que no están en `citas.txt` (títulos de apartado de las bitácoras, «aceptable», la frase del plan «solo usa …») y todas casan. La cobertura de `citas.txt` es incompleta: ver F-08.
- **F-03, resuelto.** Comparé con `[ "$dec" = "$q" ]` la columna «Decisión» de `CANDIDATAS.md` con la cita del resumen para las 12 candidatas de «Decididas», y las 12 son **idénticas**. C-122 está ahora en «Abiertas», también idéntica. Además, todas las filas cuyo «Salió de» nombra U07 (C-106 a C-127) aparecen en el resumen.
- **F-04, resuelto.** `datos.sh` imprime ahora el comando exacto. Lo corrí y su salida es idéntica a `evidencia/datos.txt`, sin contar la cabecera.
- **F-05, resuelto.** Ver CA-8.
- **F-06, resuelto en `summary.md`.** La ruta usa `$HOME`. Las cabeceras de `evidencia/` siguen con la ruta expandida: ver F-09.

## Hallazgos

### F-07 · AMARILLO · `bitacoras/U07-G-evidencia/corridas.sh:9` · El guion versionado ya no reproduce la evidencia de la corrida 1
- El commit `3c09f5b` borró la línea `C1="./gradlew :backend:test --rerun --tests '*despliegue*'; echo \"salida=\$?\""`, pero la línea 9 sigue haciendo `echo "\$ $C1"`. Así, el guion actual escribiría `$ ` sin comando.
- El guion tampoco genera las dos últimas líneas de `conteo-por-clase-despliegue.txt` (la orden `$ cat … | bc` y su `100`).
- Conclusión: `gradle-despliegue.txt` y `conteo-por-clase-despliegue.txt` no salieron de este guion. Lo comprobé con `git log -p -- bitacoras/U07-G-evidencia/corridas.sh | grep -n C1`.
- Los datos son correctos, porque los reproduje. Es la misma clase de defecto que F-04 (la orden anotada no es la que corrió), que se corrigió en `datos.sh` pero no en `corridas.sh`.

### F-08 · AMARILLO · `summary.md:322-343` · Las citas de «Abiertas» y «Relacionadas» no son la columna «Decisión» tal como está
`citas.txt` cubre 34 de las 95 citas entre «» del resumen. Ninguna de las citas de `CANDIDATAS.md` está en esa lista. Por eso no se detectó lo siguiente:
- **Abiertas.** El encabezado dice «con su "Decisión" tal como está», pero en 14 de las 15 filas se quitó el prefijo «Aceptada como candidata (por el orquestador):». Son C-90, C-106, C-110, C-112, C-113, C-117, C-118, C-119, C-121, C-123, C-124, C-125, C-126 y C-127; solo C-122 va entera. Son subcadenas literales y no se pierde contenido.
- **Relacionadas.** C-40 cita «se decide con el primero (U05 o U07)» y C-62 cita «se evalúa en U07». La fuente dice «Se decide…» y «Se evalúa…», con mayúscula, así que con `grep -F` no casan.

La decisión 14 no exige la Decisión literal para las relacionadas, y las de «Decididas» son exactas. Por eso no bloquea. La afirmación de la respuesta a F-02 («`citas.txt` lista cada cita») es más amplia de lo que es en realidad.

### F-09 · AMARILLO · `aidlc-docs/construction/U07-despliegue/code/evidencia/*.txt:1` · El login sigue en `aidlc-docs`
Las 8 cabeceras de `evidencia/` traen `JAVA_HOME=/home/casdav/...`. El resumen ya está limpio. Estos archivos también están bajo `aidlc-docs`, donde rige la regla de C-30 de no dejar rastro de persona. El codificador lo anota a propósito en la bitácora («conservan la ruta expandida»). Basta `cab()` con `$HOME` sin expandir.

## Tareas candidatas (defectos reales fuera de alcance)
- Ya registrada como C-122 en la rama del orquestador: el filtro `--tests '*<módulo>*'` de las plantillas también casa con nombres de método. No hay ninguna nueva.

## Rutas de transcripciones largas
Todo está en `/tmp/claude-1000/-home-casdav-Projects-Master-SynKra/9d987332-269f-4da9-9d66-0a5449231723/scratchpad/U07-G-r2/`:
- `ca4-despliegue.txt`: corrida 1 y conteo de todos los XML.
- `ca7.txt` y `ca7-gradle.txt`: `:backend:check`, recuento de XML y REQUIRES_NEW.
- `ca5-ca6-citas.txt`: `ca5.sh`, `ca6.sh`, mi repetición de `citas.txt` con `grep -cF` y las 11 citas extra.
- `citas-resumen.txt`, `cobertura.txt` y `no-listadas-fuente.txt`: las 95 citas del resumen frente a `citas.txt` y su fuente.
- `datos.txt`: mi corrida de `datos.sh`.

```
VEREDICTO: VERDE
AMARILLO|bitacoras/U07-G-evidencia/corridas.sh:9|$C1 borrado en 3c09f5b: el guion ya no reproduce gradle-despliegue.txt ni la suma de conteo-por-clase-despliegue.txt
AMARILLO|summary.md:322-343|Abiertas sin el prefijo de su Decisión (14 filas) y relacionadas C-40/C-62 con la mayúscula cambiada; citas.txt no cubre las citas de CANDIDATAS.md
AMARILLO|evidencia/*.txt:1|El login sigue en las 8 cabeceras de evidencia bajo aidlc-docs
INFORME: revisiones/U07-G/ronda-2.md
```
