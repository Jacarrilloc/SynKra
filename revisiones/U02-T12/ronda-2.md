# Ronda 2 — U02-T12

VEREDICTO: NO-VERDE

Los tres criterios pasan cuando los corro yo, y los cuatro hallazgos de la ronda 1 quedan cerrados. Queda en pie un NARANJA nuevo, en la misma zona que F-01. Si `docker run -d` crea el contenedor pero no consigue arrancarlo, el script imprime bien el `FALLA`, pero ese contenedor queda huérfano porque se registra para el `trap` después del `run`. Es un defecto que ya existía en la ronda 1 y que no vi entonces; lo señalo ahora, no es una regresión del codificador.

## Criterios de aceptación, verificados por mí (worktree en HEAD 46252bf)
| # | Criterio | Comando que corrí | Resultado |
|---|---|---|---|
| 1 | El humo pasa | `./gradlew :core:bootJar :plugins:kyc-pe:assemble` (14 tareas al día), `docker build -t synkra-core:2.1.0 core/`, `time core/smoke.sh; echo salida=$?`, `docker ps -a --format '{{.Names}}' \| grep -c smoke` | pasa: `ok caso1-kyc-pe-1.3.0`, `ok caso2-kyc-pe-2.0.0`, `salida=0`, `0` contenedores. Tarda 4,8 s, lo que cuadra con un arranque y un rechazo reales; no se saltó nada. |
| 2 | El humo muerde | Copia en mktemp con `sed 's/preparar "$caso" "2.0.0"/preparar "$caso" "1.3.0"/'` (diff de una línea, la 80), corrida con `SMOKE_RAIZ=$PWD` | pasa: `ok caso1`, `FALLA caso2-kyc-pe-2.0.0: el core no termino en 60s`, `salida=1`, 0 contenedores, 1 min 2,9 s (se respeta el tope de 60 s) |
| 3 | El diff queda acotado | `git rev-parse --short HEAD; git diff --stat 28a6ac3...HEAD; git ls-files -s core/smoke.sh` | pasa: HEAD `46252bf`, el merge-base es 28a6ac3, 11 archivos, solo `core/smoke.sh` (`100755`), `bitacoras/U02-T12*` y `revisiones/U02-T12/`. No hay nada fuera del alcance ni se toca ningún clúster. |

## Cierre de los hallazgos de la ronda 1
- **F-01 (NARANJA), cerrado.** Repetí mi mutación: el curl de `/plugins` apunta a `127.0.0.1:1`. Resultado: `FALLA caso1-kyc-pe-1.3.0: curl a GET /plugins fallo`, el caso 2 corre y da `ok`, `salida=1`, 0 contenedores. En la ronda 1 el script no imprimía nada y salía con `salida=7`. El barrido de la clase está completo: `preparar`, los dos `docker run`, `docker port`, `docker inspect`, `docker wait`, `docker logs` y los dos curl. El par de evidencia es honesto: `f01-rojo.txt` es del commit 8cc4426, que no toca el script, y `f01-verde.txt` es del commit 1cfe87d.
- **F-02 (AMARILLO), cerrado.** Probé el patrón de la línea 70 con cuatro cuerpos:
  - `kyc-pe 9.9.9` junto a `x 1.3.0`: lo rechaza.
  - `{"nombre":"kyc-pe","version":"1.3.0"}`: lo acepta.
  - `1.3.0-rc`: lo rechaza.
  - `11.3.0`: lo rechaza.
- **F-03 (AMARILLO), cerrado.** El cuerpo y los logs se leen primero a una variable y se buscan con `grep <<<`. Ya no queda ningún pipe hacia `grep -q`.
- **F-04 (AMARILLO), cerrado.** La bitácora tiene una sola sección de CA-3, `ca1.txt` trae el `docker build` completo y `ca2.txt` muestra cómo se define `$D`. `ca3.txt` marca el sha 1cfe87d, el último commit de código, como pide la bitácora.

## Hallazgos

### F-05 · NARANJA · core/smoke.sh:49-52 y 81-84 · Si `docker run` falla al arrancar, el contenedor queda creado y el `trap` no lo borra
`CONTENEDORES+=("$nombre")` va después del `docker run -d ... || { falla; return; }`. Pero `docker run` puede crear el contenedor y fallar después, al arrancarlo: por la red, por el puerto o por el montaje. En ese caso el nombre nunca llega a `CONTENEDORES` y `limpiar` no lo borra. La decisión 2 pide un «`trap` que los borra», y el contenedor huérfano además ensucia el conteo de CA-1 en todas las corridas siguientes, porque `grep -c smoke` ya no da 0.

Cómo lo demostré: ocupé el puerto 127.0.0.1:18089 con un contenedor propio (`rev-t12-ocupa`) y mutué la línea 49 a `-p 127.0.0.1:18089:8080`. Salida:
```
docker: Error response from daemon: ... Bind for 127.0.0.1:18089 failed: port is already allocated
FALLA caso1-kyc-pe-1.3.0: docker run fallo
ok caso2-kyc-pe-2.0.0
salida=1
$ docker ps -a --format '{{.Names}} {{.Status}}' | grep smoke
smoke-c1-189541-1795824695 Created
```
El mismo problema está en los dos `docker run` (caso 1 y caso 2). La solución es registrar el nombre antes del `run`: `limpiar` ya usa `docker rm -f ... || true`, así que borrar un nombre que no existe es inocuo.

### F-06 · AMARILLO · core/smoke.sh:48,80 · `preparar ... || falla` apaga `set -e` dentro de `preparar`
Cuando una función se llama a la izquierda de `||`, bash ignora `set -e` en todo su cuerpo, y el estado de salida queda en manos del último `chmod`. Si un paso intermedio falla y el último tiene éxito, el fallo pasa sin aviso. Lo comprobé aparte, fuera del script: con `set -euo pipefail; p(){ false; echo sigue; true; }; p || echo no` se imprime `sigue` y no se detecta el `false`. Hoy apenas hay riesgo: los JAR se comprueban antes y un `printf` fallido haría fallar después el `chmod` del YAML. No bloquea.

## Tareas candidatas (defectos reales fuera de alcance)
- Ninguna nueva. C-84 ya recoge que S5 queda fuera del humo.

## Estado del worktree y limpieza
- `git status --short` sigue vacío después de la revisión.
- Borré el contenedor huérfano de F-05 (`smoke-c1-189541-1795824695`), el `rev-t12-ocupa` y mi directorio de mktemp (`/tmp/tmp.5AOLlGR8FO`). Al final, `docker ps -a | grep -cE 'smoke|rev-t12'` da 0.
- No usé kubectl, kind ni ningún registro.

## Rutas de transcripciones largas
- /tmp/claude-1000/-home-julian-Escritorio-Proyectos-SynKra/76af36c7-fa86-447c-9ae5-76b2a7831e37/scratchpad/r2/ca1.txt
- /tmp/claude-1000/-home-julian-Escritorio-Proyectos-SynKra/76af36c7-fa86-447c-9ae5-76b2a7831e37/scratchpad/r2/mut.txt (CA-2, mutación de F-01 y mutación de F-05)

No tengo Write, así que no escribí el informe en el repositorio. Este texto es el contenido de `revisiones/U02-T12/ronda-2.md`.

```
VEREDICTO: NO-VERDE
NARANJA|core/smoke.sh:49-52,81-84|Si docker run falla al arrancar, el contenedor queda creado y el trap no lo borra
AMARILLO|core/smoke.sh:48,80|preparar ... || falla apaga set -e dentro de preparar
INFORME: revisiones/U02-T12/ronda-2.md
```
