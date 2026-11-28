# Ronda 3: U02-T12

VEREDICTO: VERDE

Corrí yo mismo los tres criterios de aceptación en HEAD 0f428f9 y los tres pasan. Repetí mis mutaciones de F-01 y F-05, más una nueva para F-06, y los dos hallazgos de la ronda 2 quedan cerrados. Lo que añadió el codificador por su cuenta, el manejo de señales y el trap antes del temporal, está dentro del alcance. No queda ningún ROJO ni NARANJA. Solo queda un AMARILLO, que no bloquea.

## Criterios de aceptación, verificados por mí
| # | Criterio | Comando que corrí | Resultado |
|---|---|---|---|
| 1 | El humo pasa | `./gradlew :core:bootJar :plugins:kyc-pe:assemble && docker build -t synkra-core:2.1.0 core/` (build=0); `time core/smoke.sh; echo salida=$?`; `docker ps -a --format '{{.Names}}' \| grep -c smoke` | pasa: `ok caso1-kyc-pe-1.3.0`, `ok caso2-kyc-pe-2.0.0`, `salida=0`, `0` contenedores, sin temporales en /tmp. Tarda 4,8 s, lo que corresponde a un arranque y un rechazo reales, no a algo saltado. |
| 2 | El humo muerde | Copia en mktemp con `sed 's/preparar "$caso" "2.0.0"/preparar "$caso" "1.3.0"/'`; el diff es de una sola línea, la 85. La corrí con `SMOKE_RAIZ=$PWD`. | pasa: `ok caso1`, `FALLA caso2-kyc-pe-2.0.0: el core no termino en 60s`, `salida=1`, 0 contenedores. Tarda 1 min 2,9 s, así que se respeta el tope de 60 s. |
| 3 | El diff queda acotado | `git rev-parse --short HEAD; git diff --name-only 28a6ac3...HEAD` filtrado por la lista permitida; `git ls-files -s core/smoke.sh` | pasa: HEAD `0f428f9`, merge-base `28a6ac3`, ningún archivo fuera de `core/smoke.sh`, `bitacoras/U02-T12*` y `revisiones/U02-T12/`. `core/smoke.sh` es `100755` (blob deb6331). No aparece ni `kubectl` ni `kind`. |

## Cierre de los hallazgos de la ronda 2
- **F-05 (NARANJA): cerrado.** Ahora `CONTENEDORES+=` va antes de los dos `docker run` (líneas 54 y 86). Repetí la mutación: ocupé el puerto 127.0.0.1:18089 con `rev-t12-ocupa` y cambié la línea 55 a `-p 127.0.0.1:18089:8080`. Sale `FALLA caso1-kyc-pe-1.3.0: docker run fallo`, el caso 2 da `ok` y `salida=1`. El único contenedor que queda es mi `rev-t12-ocupa`; ya no queda ningún `smoke-c1-* Created`, que en la ronda 2 sí quedaba. El par de evidencia es honesto: `f05-rojo.txt` sale del commit 9bb3d61, que no toca el script, y `f05-verde.txt` de cdea5b5.
- **F-06 (AMARILLO): cerrado.** `preparar` encadena ahora sus pasos con `&&` (líneas 39 a 43). Lo probé con una mutación nueva y más exigente: `cp "$JAR_COMPATIBLE" /no/existe.jar ...`. Así copia un JAR, falla en el segundo, y el `chmod` final igual tendría éxito.
  - Con el script de la ronda 3, los dos casos dan `FALLA ...: no se pudo preparar /plugins y /config` y `salida=1`.
  - La misma mutación sobre el script de la ronda 2 (615c4f3) daba `ok caso1-kyc-pe-1.3.0`: el fallo pasaba sin aviso. Esto confirma que el defecto existía y que el arreglo es real.

## Trabajo por iniciativa del codificador: dentro del alcance
El trap antes de `mktemp` (con `TMP=""` y la guarda en `limpiar`) y las líneas `trap 'exit 130' INT` y `trap 'exit 143' TERM HUP` están en `core/smoke.sh`, el único archivo de código del alcance. Además sirven a la decisión 2 («`trap` que los borra y borra sus temporales»). No es desborde.

Lo comprobé con `timeout -s {TERM,INT,HUP}` a los 1,5 s, cuando el caso 1 todavía no está listo, y a los 3,5 s, durante el caso 2. Se cubren así las dos fases de la corrida. Las seis veces quedaron 0 contenedores smoke y ningún temporal nuevo. Con `--preserve-status` y TERM, la salida es 143.

Una observación: el script de la ronda 2 también limpiaba con TERM, HUP e INT, y salía con 143, 129 y 130 sin dejar restos. El bash local ya ejecuta el trap EXIT al morir por esas señales. Los traps explícitos son, por tanto, defensivos y redundantes aquí. No rompen nada; lo único que cambia es que HUP pasa a salir con 143 en lugar de 129. No es un hallazgo.

## Hallazgos

### F-07 · AMARILLO · bitacoras/U02-T12-evidencia/f06.txt y senal.txt · La evidencia está resumida, no transcrita
- `f06.txt` cita el comando abreviado (`bash -c 'RAIZ=...; ... preparar x 1.3.0 ...'`), así que no se puede volver a ejecutar tal cual.
- `senal.txt` pone `kill -TERM <pid>` y la frase «sin temporales nuevos» en lugar del comando real y su salida.

En las dos, lo que se afirma es cierto: lo comprobé yo, con los resultados de arriba. Pero tal como están escritas no son transcripciones, y no alcanzarían como evidencia por sí solas. No bloquea, porque los criterios y el cierre de F-05 y F-06 tienen evidencia completa y reproducible. Puede ir a la tarea de pulido junto con C-85, que es la misma clase de defecto.

## Tareas candidatas (defectos reales fuera de alcance)
- Ninguna nueva. C-84 sigue recogiendo que S5 queda fuera del humo.

## Estado del worktree y limpieza
- `git status --short` sigue vacío después de la revisión.
- Borré `rev-t12-ocupa` y mi directorio de mktemp (`/tmp/tmp.whxKgdhP90`). Al final, `docker ps -a | grep -cE 'smoke|rev-t12'` da 0 y no queda ningún `/tmp/tmp.*`.
- No usé kubectl, kind ni ningún registro. La imagen `synkra-core:2.1.0` local quedó reconstruida desde este worktree, como pide CA-1.

## Rutas de transcripciones largas
- /tmp/claude-1000/-home-julian-Escritorio-Proyectos-SynKra/76af36c7-fa86-447c-9ae5-76b2a7831e37/scratchpad/r3/build.txt (gradlew y docker build)
- /tmp/claude-1000/-home-julian-Escritorio-Proyectos-SynKra/76af36c7-fa86-447c-9ae5-76b2a7831e37/scratchpad/r3/ca1.txt
- /tmp/claude-1000/-home-julian-Escritorio-Proyectos-SynKra/76af36c7-fa86-447c-9ae5-76b2a7831e37/scratchpad/r3/ca2.txt

No tengo Write, así que no escribí el informe en el repositorio. Este texto es el contenido de `revisiones/U02-T12/ronda-3.md`.

```
VEREDICTO: VERDE
AMARILLO|bitacoras/U02-T12-evidencia/f06.txt,senal.txt|La evidencia está resumida, no transcrita (no bloquea)
INFORME: revisiones/U02-T12/ronda-3.md
```
