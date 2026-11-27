# Ronda 1 — U04-T12

VEREDICTO: NO-VERDE

El resumen es sólido en lo que se puede medir. Repetí CA-1 y obtuve la misma cola y los mismos conteos por clase. CA-2 da las mismas líneas que el resumen. PRs, rondas y la lista de 36 candidatas coinciden. Lo que bloquea es una evidencia vieja en la bitácora y una columna «Estado» que contradice al propio resumen.

Comprobaciones previas: el hash de la tarea es `dea3f10290551b3bf45e41cb43c04e150c68b7ad` y coincide. HEAD está en `d3ea4ab`. El worktree quedó limpio después de mi revisión (`git status --short` vacío). El puerto 18080 quedó libre y no queda ningún proceso `backend-0.1.0.jar`.

## Criterios de aceptación, verificados por mí
| # | Criterio | Comando que corrí | Resultado |
|---|---|---|---|
| 1 | Pruebas del catálogo | `./gradlew :backend:test --rerun --tests 'io.synkra.catalogo.*' 2>&1 \| tail -6` y los conteos de los XML | pasa: `BUILD SUCCESSFUL in 14s`; 13 XML con timestamp 2026-11-27T18:55, 259 pruebas, `skipped=0 failures=0 errors=0`; cada clase coincide con el resumen. Mi cola dice `1 executed, 7 up-to-date` y la del resumen `8 executed`: se explica por la caché, no es un defecto |
| 2 | `curl` de H-01 a H-06 y H-15 | el script copiado del resumen tal cual (`sed` entre las cercas) tras `:backend:bootJar`, en el puerto 18080 | pasa: `201`, `[]`, `201`, `['2.1.0', '3.0.0']`, `409`, las dos filas de SQLite; dos hashes iguales, `409`, `200`; `1` y `['kyc-co', 'kyc-pe']`; dos hashes iguales y `404`; H-03 422 `coreCompatible`; H-04 422 `RUTA_NO_PERMITIDA`, `0`. Todo igual al resumen salvo el valor del hash (`ef9f…` frente a `60e3…`), que es esperable: `jar cf` guarda la fecha de los archivos |
| 3 | Resumen completo | `grep -c '^\| U04-T' …summary.md` y `grep -o 'C-[0-9]*' CANDIDATAS.md \| sort -u \| wc -l`, más mi bucle `awk -F'\|' '$4 ~ /U04-T/'` con `grep -q "^\| $c "` | pasa: 13 filas y 66 candidatas en total. Las de U04 son 36 (C-31 a C-66) y no falta ninguna en el resumen |
| 4 | Diff acotado | `git diff --stat origin/main...HEAD` | pasa: solo `summary.md` (234) y `bitacoras/U04-T12.md` (76) |

PRs y rondas: crucé `gh pr list --state merged` con `ls revisiones/U04-*/`, contando solo los `ronda-N.md` y no los `-respuesta`. Los 12 PR de tareas y las rondas por tarea (3,2,3,3,3,2,2,3,2,2,3,1) coinciden con la tabla. Los PR #55, #57, #59, #61, #63 y #65 son de `orq/U04-david`, como dice el resumen.

## Hallazgos

### F-01 · ROJO · bitacoras/U04-T12.md, sección CA-4 · Evidencia de CA-4 que no puede venir del diff actual
La bitácora copia esta salida de `git diff --stat`: `bitacoras/U04-T12.md | 68 ++++++` y `2 files changed, 302 insertions(+)`. Mi ejecución sobre `d3ea4ab` da `76 +++++++` y `310 insertions(+)`. La salida se tomó antes de la última edición de la bitácora, y el contrato (invariante 3) dice que eso es ROJO por sí solo. El criterio pasa; lo que falla es la evidencia. Arreglo: correr CA-4 después de la última edición, o declarar que la salida se tomó antes de escribir esa sección y volver a correrla.

### F-02 · NARANJA · summary.md:189-226, columna «Estado» · El estado de las candidatas lo dedujo el codificador y contradice al propio resumen
`CANDIDATAS.md` no tiene columna de estado (cabecera: `# | Candidata | Salió de | Propuesta por | Decisión`). Los valores «Pendiente», «Cerrada» y «Reasignada» son una clasificación hecha a mano, lo contrario de lo que pide la decisión 1 (C-21). Además son incoherentes. El resumen marca como «Pendiente» cinco candidatas que una tarea fusionada ya tenía dentro de su alcance, y en algunos casos el resumen mismo dice que se hicieron:
- C-38 y C-50: alcance de U04-T07 (`tareas/U04-T07-ruta-subida.md:47-48`). En `summary.md:41` el resumen dice que T07 puso los topes de 64 KB y de 63 caracteres.
- C-44: alcance de U04-T06 (`tareas/U04-T06-subir-plugin.md:60`). En `summary.md:41` dice «entradas duplicadas del JAR dan 422 (C-44)».
- C-45 y C-47: alcance de U04-T05 (`tareas/U04-T05-propiedad-rutas-seguras.md:98,103`).
- C-33 sí tiene una nota (T13); a las cinco anteriores les falta la misma.

Arreglo sin cerrar candidatas: poner la columna «Decisión» literal y, aparte, «Trabajada en» con la tarea que la tuvo en su alcance, sacada con `grep -l C-NN tareas/U04-*.md`, para que el orquestador decida el cierre.

### F-03 · NARANJA · summary.md:185 y 230, bitácora CA-3 · Los comandos de extracción no están escritos literalmente
- `summary.md:185` dice «con `awk` … (`$4 ~ /U04-T/`)», pero no da el comando completo. Lo reconstruí (`awk -F'|' '$4 ~ /U04-T/ {print $2}' CANDIDATAS.md`) y da las mismas 36, pero quien lea el resumen no puede copiarlo y correrlo.
- En `summary.md:230` pasa lo mismo con las candidatas de otras unidades. Reconstruido (`$4 !~ /U04-T/ && $6 ~ /U04/`), da las mismas 9.
- La bitácora pone para CA-3 la línea `$ cada candidata con origen U04 aparece como fila en el resumen (bucle con grep -q "^| C-NN ")`. Es una paráfrasis, no un comando con su salida: es una afirmación sin verificar (invariante 3). Yo la verifiqué y se cumple; aun así hay que poner el comando real.

### F-04 · AMARILLO · summary.md:126-137 · Los `curl` de H-03 y H-04 están dentro del alcance, pero no son los que pide la historia
No es un desborde. El alcance de la tarea dice «los `curl` de H-01 a H-06 y H-15», y ese rango incluye H-03 y H-04. Llamarlos «extra» es una lectura equivocada en la dirección contraria. Lo que flojea es el contenido:
- `stories.md:43-60` pide `curl` para tres casos: rango `>=2.x<`, `core: otro-core` y el 413 con 25 MB. El script prueba «sin coreCompatible», que la historia deja a `./gradlew check`.
- La lectura de vuelta de H-04 (`find … -name 'p' -o -name 'p.jar'`) solo mira la carpeta de ese plugin. No comprueba `artefactos/.tmp` ni que exista un `etc/x` fuera de `$SYNKRA_DATA`, y la historia pide «no queda nada en el volumen».

### F-05 · AMARILLO · summary.md:139 · El script termina con código 1 cuando todo va bien
La última línea es `ss -ltn | grep -c ':18080 '`. Cuando el puerto está libre, `grep -c` da `0` y sale con código 1, así que el script sale con 1 justo en el caso bueno (en mi ejecución, `rc=1`). Conviene `|| true` o un `test`.

### F-06 · AMARILLO · summary.md:228 · El título de la sección no corresponde a su contenido
«Candidatas de otra unidad que U04 cerró» lista 9 candidatas, de las que solo 3 están cerradas (C-04, C-07, C-14). Un título como «… que nombran U04» sería más preciso.

## Tareas candidatas (defectos reales fuera de alcance)
- Las candidatas C-05, C-08, C-12, C-13, C-19 y C-20 iban destinadas a U04 («se resuelven en U04-T01», «se propone para U04-T04», etc.). U04 termina sin que su decisión diga «cerrada». El orquestador tiene que decidir si se cerraron de hecho o si pasan a la tarea de pulido de U04.
- Las candidatas C-38, C-44, C-45, C-47 y C-50 se trabajaron en T05, T06 y T07, pero su fila en `CANDIDATAS.md` no lo dice. Actualizar ese archivo le toca al orquestador; está fuera del alcance de T12.

## Rutas de transcripciones largas
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/5b13ba94-befc-454f-88e7-3182922701e5/scratchpad/ca1.txt
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/5b13ba94-befc-454f-88e7-3182922701e5/scratchpad/ca2.sh
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/5b13ba94-befc-454f-88e7-3182922701e5/scratchpad/ca2-out.txt
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/5b13ba94-befc-454f-88e7-3182922701e5/scratchpad/cand-u04.txt

```
VEREDICTO: NO-VERDE
ROJO|bitacoras/U04-T12.md (CA-4)|Evidencia de CA-4 anterior al diff actual (68/302 frente a 76/310)
NARANJA|summary.md:189-226|Columna «Estado» deducida a mano e incoherente (C-38, C-44, C-45, C-47, C-50)
NARANJA|summary.md:185,230 y bitácora CA-3|Comandos de extracción de candidatas no escritos literalmente
AMARILLO|summary.md:126-137|Los curl de H-03 y H-04 no son los casos de la historia y la lectura de vuelta es débil
AMARILLO|summary.md:139|El script sale con 1 cuando todo va bien
AMARILLO|summary.md:228|Título de sección que no corresponde a su contenido
INFORME: revisiones/U04-T12/ronda-1.md
```
