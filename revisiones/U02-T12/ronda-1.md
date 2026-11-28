# Ronda 1 — U02-T12

VEREDICTO: NO-VERDE

Los tres criterios pasan cuando los corro yo. Las mutaciones que hice también: el humo detecta lo que debe. Queda en pie un NARANJA: cuando falla un comando externo del script (`curl`, `docker port`, `docker run`, `docker wait`), `set -e` lo corta sin imprimir `FALLA <caso>`, y la decisión 2 de la tarea exige ese mensaje.

## Criterios de aceptación, verificados por mí
| # | Criterio | Comando que corrí | Resultado |
|---|---|---|---|
| 1 | El humo pasa | `./gradlew :core:bootJar :plugins:kyc-pe:assemble` (estaba al día), `docker build -t synkra-core:2.1.0 core/`, `time core/smoke.sh; echo salida=$?`, `docker ps -a ... \| grep -c smoke` | pasa: `ok caso1-kyc-pe-1.3.0`, `ok caso2-kyc-pe-2.0.0`, `salida=0`, `0` contenedores. Tarda 4,8 s, lo que cuadra con un arranque y un rechazo reales; no es un salto. |
| 2 | El humo muerde | Copia con el caso 2 en `preparar "$caso" "1.3.0"`, corrida con `SMOKE_RAIZ=$PWD` | pasa: `FALLA caso2-kyc-pe-2.0.0: el core no termino en 60s`, `salida=1`, 0 contenedores, 1 min 2 s (el tope se respeta) |
| 3 | El diff queda acotado | `git rev-parse --short HEAD; git diff --stat 28a6ac3...HEAD; git ls-files -s core/smoke.sh` | pasa: HEAD `d3a0712`, el diff solo toca `core/smoke.sh` (`100755`) y `bitacoras/U02-T12*`, el merge-base es 28a6ac3 y no hay archivos fuera del alcance |

**Lectura de vuelta, a mano.** Contra la misma imagen, `GET /plugins` devuelve `[{"nombre":"kyc-pe","version":"1.3.0"}]`. Con 2.0.0 el contenedor sale con código 1 y el log dice `Arranque rechazado: VERSION_INCOMPATIBLE plugin=kyc-pe: El plugin 'kyc-pe' 2.0.0 requiere el core 'synkra-core' en 3.x y se pidió 2.1.0`.

**Mutaciones propias** (todas detectadas, 0 contenedores restantes en cada una):
- **M2**, caso 1 con 2.0.0: `FALLA caso1 ... termino antes de estar listo`, `salida=1`.
- **M3**, sin el JAR 1.3.0 en `/plugins`: `FALLA caso1`, `salida=1`.
- **M4**, YAML en el formato viejo `plugins:` (lo que rompió C-81): `FALLA caso1` y `FALLA caso2 ... codigo 1 pero el log no contiene 'VERSION_INCOMPATIBLE plugin=kyc-pe'`, `salida=1`. El script vigila de verdad el formato de C-81.

**Limpieza.** Con SIGTERM a mitad de la corrida la salida es 143, no quedan contenedores ni directorios `/tmp/tmp.*` nuevos y el `trap EXIT` cumple. La prueba de SIGINT no vale: se lo mandé a un proceso en segundo plano, que lo ignora.

**Edad de la evidencia.** `ca1.txt` y `ca2.txt` son del commit 19b9683. Entre 19b9683 y d3a0712 solo cambian la bitácora y `ca3.txt`, así que esa evidencia vale para el script actual. Mis corridas coinciden con ella.

## Hallazgos

### F-01 · NARANJA · core/smoke.sh:48,56,62,74,80 · Un fallo de comando externo corta el script sin `FALLA <caso>`
La decisión 2 dice: «Imprime `ok <caso>` o `FALLA <caso>` y termina con ≠ 0 si un caso falla». Por `set -euo pipefail`, varios comandos que están fuera de un `if` o de un `|| ...` terminan el script en silencio. Eso deja sin mensaje el caso en curso y sin correr el caso 2. La puerta no se abre (la salida sigue siendo ≠ 0), pero el informe incumple la decisión.

Lo demostré cambiando la URL de la línea 62 a `127.0.0.1:1`. Salida completa: ninguna línea impresa, `salida=7`, el caso 2 no corrió.

Barrido de la misma clase:
- línea 48: `docker run -d` del caso 1;
- línea 56: `docker port | head | sed`, que con pipefail aborta si el contenedor muere entre el `inspect` y el `port`;
- línea 62: `curl` de `/plugins`;
- línea 74: `docker run -d` del caso 2;
- línea 80: `docker wait`.

Las líneas 53 y 79 están bien porque van dentro de un `if`. Hace falta convertir cada fallo en un `falla "$caso" ...` (con `|| { falla ...; return; }`).

### F-02 · AMARILLO · core/smoke.sh:63 · El chequeo de `GET /plugins` no exige el par
Hoy se buscan por separado `"kyc-pe"` y `1\.3\.0` en cualquier parte del cuerpo. Un cuerpo como `[{"nombre":"kyc-pe","version":"9.9.9"},{"nombre":"x","version":"1.3.0"}]` pasaría. La decisión 3 pide «`kyc-pe` y `1.3.0`», y el cuerpo real es estable (`{"nombre":"kyc-pe","version":"1.3.0"}`), así que se puede buscar el par exacto.

### F-03 · AMARILLO · core/smoke.sh:63,86 · `pipefail` con `grep -q` puede dar un FALLA espurio
Con pipefail, si `grep -q` termina antes de leer todo, el productor (`echo` o `docker logs`) puede recibir SIGPIPE: el pipeline sale con 141 y el `if` cuenta como fallo. Hoy no pasa porque los logs caben en el búfer del pipe; en 7 corridas no lo vi. Un log más largo podría dispararlo. La salida es leer primero a una variable o usar `grep -c`.

### F-04 · AMARILLO · bitacoras/U02-T12.md · Bitácora con una línea vieja y un rótulo que no cumple
- Hay dos líneas de CA-3; la primera («ver informe final») quedó vieja.
- `ca1.txt` se presenta como «comandos completos», pero la salida de `docker build` está recortada con un tail.
- `ca2.txt` no muestra cómo se definió `$D`.

## Tareas candidatas (defectos reales fuera de alcance)
- C-84 dice que «S5 se valora en la prueba de humo de U02-T12», pero la tarea no lo pide y la bitácora no lo menciona. El orquestador debería anotar si S5 queda descartado o se reasigna.

## Estado del worktree
Limpio después de la revisión (`git status --short` vacío). No dejé contenedores (`smoke*` ni `rev-t12*`) y borré mis temporales de `mktemp`.

## Rutas de transcripciones largas
- /tmp/claude-1000/-home-julian-Escritorio-Proyectos-SynKra/76af36c7-fa86-447c-9ae5-76b2a7831e37/scratchpad/ca1.txt
- /tmp/claude-1000/-home-julian-Escritorio-Proyectos-SynKra/76af36c7-fa86-447c-9ae5-76b2a7831e37/scratchpad/mut.txt (M1 a M4)

No tengo Write, así que no escribí el informe en el repositorio. Este texto es el contenido de `revisiones/U02-T12/ronda-1.md`.

```
VEREDICTO: NO-VERDE
NARANJA|core/smoke.sh:48,56,62,74,80|Un fallo de comando externo corta el script sin FALLA <caso>
AMARILLO|core/smoke.sh:63|El chequeo de GET /plugins no exige el par
AMARILLO|core/smoke.sh:63,86|pipefail con grep -q puede dar un FALLA espurio
AMARILLO|bitacoras/U02-T12.md|Bitácora con una línea vieja y un rótulo que no cumple
INFORME: revisiones/U02-T12/ronda-1.md
```
