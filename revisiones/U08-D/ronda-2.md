# Ronda 2 — U08-D

VEREDICTO: VERDE

F-01 está cerrado, comprobado con mis propias corridas. El caso que encontré en la ronda 1 y tres sitios nuevos que elegí yo salen ahora con 1 y con un `FALLA` legible. Volví a correr CA-2, CA-3 (en un kind nuevo), CA-4, CA-6, CA-7 y CA-8, y todos pasan. CA-1 y CA-5 siguen valiendo: `git diff c202f7c HEAD -- backend` da 0 líneas. No queda ningún ROJO ni NARANJA. Quedan tres AMARILLO.

- **Hash de la tarea:** `00ffee964a116544ecd98ba0f49173629925bffc`, coincide.
- **Punta:** `369d286`, base `367e09f`.
- **Al terminar:** `git status --short` vacío y las copias de `mktemp` borradas.
- **Clúster:** `kind-synkra` arriba con SynKra instalado desde el worktree. `synkra-co` está en el 41 (reversión con origen 38, `listo`), con `concuerda: true`. El core carga `[kyc-co 1.0.0, kyc-pe 1.3.0]`, el pod está `1/1 Running` y el ConfigMap no tiene anotaciones.

## Criterios de aceptación, verificados por mí
| # | Criterio | Comando que corrí | Resultado |
|---|---|---|---|
| 1 | Plantilla cambia con solo plugins | `git diff c202f7c HEAD -- backend \| wc -l` | 0 líneas: el Java es el que verifiqué en la ronda 1 (`35 0 0 0`, backend `626 0 0 0`, `skipped` 0). Sigue vigente |
| 2 | Script según las reglas | los `grep` de CA-2 | pasa: `sintaxis=0`, `set -euo pipefail` en la línea 7, `--resolve` 1 y `--cacert` 1, `prohibidos=1`, `sin-kubeconfig=1`, `arquitectura=1`, formato en la línea 138, `trap` en la línea 33 antes del `mktemp` de la 53 |
| 3 | kind nuevo, install, límite, e2e | `bitacoras/U08-D-evidencia/ca3.sh` | pasa: `jars`, `borrar`, `install`, `limite` y `e2e` en 0, e2e en 3m49s. 94 líneas `ok`, `OK CU1` a `OK CU5`, las 10 líneas `4xx` en su orden, CU4 con antes igual a después, `synkra Apply`. El core carga los plugins de D3. Historial: 5 reversión (origen 2) `listo`, 3 `fallido`, 2 y 1 `listo`, todos de `e2e`. `concuerda: true` con último 5. `synkra-pe` da 1 (`kube-root-ca.crt`, desviación 2 aceptada). D4 tardó 185 s en fallar |
| 4 | Segunda corrida | `./scripts/e2e-api.sh` | pasa: `e2e-2=0`. Los cinco 409 de CU1 y las líneas `4xx` 1 y 3. El `cambios` de CU2 trae `quitados: [kyc-pe 1.3.0]` con `core.igual: true`. 10 líneas `4xx` y `OK e2e` |
| 5 | Rojo primero | sin cambios en `backend` desde `c202f7c`, ni en los commits del rojo | vigente: lo verifiqué en la ronda 1. `ca5-mensajes.txt` ya trae las tres pruebas en rojo, con cabecera y comando |
| 6 | Cada comprobación muerde | mi `mutar.sh -r` con las 4 mutaciones, cada una en una copia | pasa (detalle debajo de la tabla) |
| 7 | Falla antes de tocar nada | `ca7.sh` | pasa: `sin-jars=1` con el mensaje exacto y `sin-kubeconfig=1` con `FALLA guardia … falta el kubeconfig`. Sin `jq` da `sin-jq=1` (`FALLA preparacion: falta jq`). `command -v` está en la línea 51. Historial 8 y 8, `resourceVersion` 1939 y 1939 |
| 8 | Diff acotado | los comandos de `ca8.sh`, con `<punta>`=`367e09f` | pasa: `0` cambios, `fuera=1`, `0` y `0`. En `ca8.txt` el sha impreso es `d1089e1`, el padre del commit `369d286`. El `1` en el estado de `ca8.txt` es el propio archivo que se está escribiendo |

Detalle de CA-6. Las cuatro mutaciones salen con 1, cada una con un solo `FALLA`, y las cuatro restauraciones dan `restaurado=0`:
- `cu4-llega`: `FALLA CU4: el resourceVersion del ConfigMap cambió de 2235 a 2318`. Después, las anotaciones del ConfigMap quedan vacías.
- `cuerpo-ajeno`: `FALLA formato: GET /noexiste 404`.
- `sin-vuelta`: `FALLA CU5: GET /api/despliegues/29 dio 404` (desviación 3 aceptada).
- `codigo-otro`: `FALLA CU4: … dio 422 COMBINACION_RECHAZADA (esperado 422 OTRO_CODIGO)`.

## F-01 (ronda 1, NARANJA): cerrado
Cada mutación se hizo en una copia (`git archive HEAD` más `git init -q`). Tras cada una comprobé que no quedara nada en `aplicando`:
- **`version` por `versión` en `core/build.gradle.kts`** (mi caso de la ronda 1): `FALLA preparacion: no se pudo leer la version de core/build.gradle.kts (grep, head o sed salió con 1)` y `salida=1`.
- **Más casos de la misma clase:**
  - Versión sin comillas: el mismo `FALLA` y `salida=1`.
  - Archivo sin permiso de lectura (`chmod 000`): `… salió con 2` y `salida=1`.
  - Un JAR que no es zip: `FALLA preparacion: python3 leyendo synkra-plugin.yaml … salió con 1: Traceback…`.
- **`jq` sobre un cuerpo que no es JSON, en dos sitios distintos del que eligió el codificador:**
  - En `provocada` (la 4xx 1): `FALLA CU1: jq .codigo salió con 5: jq: parse error…` y `salida=1`.
  - En `leer_pais` (un 200 de `/api/paises` con HTML): `FALLA CU2: jq .[] | select(.pais==$p) | .imagen // "" salió con 5: …` y `salida=1`.
- **Una pieza de tubería que falla** (`sort --opcion-falsa` en `imagenes_ns`): `FALLA CU4: ordenar las imagenes salió con 2` y `salida=1`.
- **Barrido de todo `e2e-api.sh`:** ninguna asignación `local x="$(…)"`. Cada sustitución en una asignación lleva `|| rc=$?`, `|| falla …` o pasa por `ejecutar`/`jq_c`, salvo cuatro:
  - Tres tolerantes a propósito (`|| true`, como dice la bitácora).
  - `TMP="$(mktemp -d)"`, que queda como F-07.

  Los `jq -n` sueltos llevan `|| falla` o `|| rc`, y el único `kubectl` es el de `kc()`.

## F-02 (ronda 1, AMARILLO): cerrado
`ca5-mensajes.sh` está versionado. `ca5-mensajes.txt` y `ca3-pe.txt` traen la cabecera completa con `JAVA_HOME`. Los 12 `.txt` tienen su `.sh` versionado. El único que sigue sin `JAVA_HOME` es `ca5-estado.txt`: queda como F-08.

## Hallazgos
### F-07 · AMARILLO · scripts/e2e-api.sh:53 · `mktemp -d` sin envolver
Con `TMPDIR=/no/existe` el script sale con 1. Solo imprime el error del propio `mktemp` (`failed to create directory…`), sin ningún `FALLA preparacion`. `mktemp` no está en la lista que nombra la decisión 3 y el mensaje sigue siendo legible, por eso no bloquea.

### F-08 · AMARILLO · bitacoras/U08-D-evidencia/ca5-estado.txt · Cabecera con `JAVA_HOME: sin definir`
Es otro caso de la misma clase que F-02, y yo tampoco lo barrí en la ronda 1. La corrida no usa Java, así que el impacto es nulo.

### F-09 · AMARILLO · entorno · Un proceso en segundo plano que no es mío
Es el PID 1945418, hijo de otra sesión de Claude Code: arrancó a las 03:04:59, antes de que empezara mi revisión. Es un bucle `until ! pgrep -f '…e2e-api…'` que espera y luego solo lee `GET /api/despliegues/11..13`. No escribe nada, pero contradice el «Sin procesos propios en segundo plano» de la bitácora. No lo toqué.

## Tareas candidatas
Ninguna nueva. Siguen vigentes las de la ronda 1 y las notas C-143 y C-144.

## Rutas de transcripciones largas
Todas en `/tmp/claude-1000/-home-casdav-Projects-Master-SynKra/9d987332-269f-4da9-9d66-0a5449231723/scratchpad/U08-D-r2/`:
- Criterios: `ca2.txt`, `ca3.txt`, `ca4.txt`, `ca6.txt`, `ca7.txt`
- Mutaciones de F-01: `f01-prep.txt`, `f01-cluster.txt`
- Salidas de cada mutación y restauración: `mut-*.txt`, `rest-*.txt`
- El arnés de mutaciones: `mutar.sh`, `mutar.py`

```
VEREDICTO: VERDE
AMARILLO|scripts/e2e-api.sh:53|mktemp -d sin envolver (sale con su propio error, sin FALLA)
AMARILLO|bitacoras/U08-D-evidencia/ca5-estado.txt|cabecera con JAVA_HOME sin definir (resto de F-02)
AMARILLO|entorno|bucle de espera ajeno (PID 1945418, otra sesión) sigue vivo
INFORME: revisiones/U08-D/ronda-2.md
```
