# Ronda 3 — U08-C

VEREDICTO: VERDE

F-06 y F-07 están cerrados. Lo comprobé con mi propia ejecución: la regla nueva detecta mis seis variantes de la ronda 2. También probé 18 variantes nuevas: siete no se detectan, pero ninguna de esas formas aparece en los tres scripts que revisa la regla. Por eso es AMARILLO y no bloquea.

Comprobaciones previas:
- **Hash de la tarea:** `git hash-object` da `966592462c1bd71976e93befe77912dd8b677a8c`. Coincide.
- **Punta:** el worktree está en `4f9fcbf`.
- **Código sin cambios:** `git diff a4924e6 HEAD -- Makefile deploy scripts` sale con 0 líneas. CA-1 a CA-8 siguen vigentes.
- **Edad de la evidencia:** `ca9.txt` dice que salió de `c5779f1`. `ca9.sh` no cambia entre `c5779f1` y HEAD (`git diff c5779f1 HEAD -- bitacoras/U08-C-evidencia/ca9.sh` sale vacío). La evidencia corresponde al diff actual.
- **Clúster:** el contexto es `kind-synkra`, los dos nodos están `Ready` y `synkra-backend` está en `1/1`. No borré nada. No toqué `~/.kube/config`, `gitops2` ni `m08-gitops1`.
- **Worktree:** al terminar, `git status --short` sale vacío.

## Criterios de aceptación, verificados por mí
| # | Criterio | Comando que corrí | Resultado |
|---|---|---|---|
| 1-8 | Sin cambios de código desde `a4924e6` | diff vacío en `Makefile`, `deploy` y `scripts` | siguen vigentes los de la ronda 1 |
| 9 | Fallos claros, `trap` y la regla sin-kubeconfig | `bash bitacoras/U08-C-evidencia/ca9.sh` sobre HEAD (tardó 1,0 s) | pasa. Ver el detalle debajo de la tabla. Mi salida coincide línea por línea con `ca9.txt`, quitando la cabecera. |
| 10 | Diff acotado | `bash bitacoras/U08-C-evidencia/ca10.sh` | pasa. Ver el detalle debajo de la tabla. |

Detalle de CA-9:
- `sintaxis=0`, y `set -euo pipefail` está en los tres scripts.
- En `secreto-tls.sh` el `trap` está antes del `mktemp`.
- `sin-kubeconfig=1`, `registro-caido=1` y `media-pareja=1`. `.tls` queda solo con `tls.crt` y sale `secreto-intacto`.
- Las ocho mutaciones de `ca9.sh` dan `=0` (detectadas): `k`, `patron-revisor-echo` y `v1` a `v6`.

Detalle de CA-10:
- La salida es `4f9fcbf`, `0`, `fuera=1`, `0`, `0`, `0`.
- Los 13 archivos de fuera de `bitacoras/` y `revisiones/` están todos dentro del alcance.
- El único cambio de modo es el de la ronda 1: `imagenes.sh` pasa a 0755.

## Hallazgos de rondas anteriores
### F-06 · NARANJA · resuelto
Corrí mis variantes `v1-canI`, `v2-tipo` y `v3-encluster` con `sin_kc` cargado literal del `ca9.sh` actual. Las tres dan `0`: detectadas. La regla ya ve los `kubectl` dentro de `"$(...)"`.

### F-07 · AMARILLO · resuelto
`v5-dos-ordenes` da `0`. Con la línea partida en órdenes, el `--kubeconfig` de la primera orden ya no tapa la segunda. `v4-jsonpath-dq` y `v6-kc-en-mensaje` también dan `0`.

## Hallazgos nuevos
### F-08 · AMARILLO · bitacoras/U08-C-evidencia/ca9.sh:20-22 · El partidor de órdenes no ve `kubectl` cuando no es la primera palabra de la orden
Cada variante va en su copia de HEAD, mutando sobre todo `secreto-tls.sh:32`.

Detecta (`0`) estas formas:
- `then`, `do` y `if !`
- un comentario de fin de línea que dice `--kubeconfig`
- una continuación con `\`
- la sustitución de proceso `<(kubectl …)`
- un heredoc, con o sin sangría
- `kubectl … | grep -- --kubeconfig`

No detecta (`1`) siete formas de la misma clase:
1. Una asignación delante, como `LC_ALL=C kubectl …` o `KUBECONFIG=$HOME/.kube/config kubectl …`. La segunda es justo el caso que AUTONOMIA-01 prohíbe.
2. Un envoltorio delante: `command`, `timeout 30` o `xargs`.
3. `bash -c 'kubectl …'`, con la orden entre comillas simples.
4. `echo "$(kubectl …)"`: quitar los argumentos de `echo` se lleva también la sustitución. Es la clase de F-06, ahora limitada a `echo`.
5. Una rama de `case`: `x) kubectl …`, porque `)` no parte la línea.
6. `\kubectl`.

**¿El hueco es real en este diff?** No. Busqué todas las formas de la lista en `secreto-tls.sh`, `desplegar.sh` y `verificar-limite.sh` (`grep -nE 'timeout|KUBECONFIG=|command |xargs|bash -c|sh -c|case |echo "[^"]*\$\(|\\kubectl|exec |env '`). Solo salen tres casos inofensivos:
- el `case` de `desplegar.sh:17`, que no lleva `kubectl`;
- el `--timeout=300s` de `desplegar.sh:31`, que es un argumento de `k`, ya cubierto;
- las líneas `#!/usr/bin/env bash`.

Todos los `kubectl` reales de esos scripts tienen una forma que la regla sí revisa. El hueco es teórico: solo afecta a cambios futuros.

Nota: la tarea permite `KUBECONFIG` apuntando al archivo del proyecto, así que la regla no puede marcar cualquier asignación. Debería marcar las que no apuntan a `--kubeconfig` ni al kubeconfig del proyecto.

No bloquea. Esto encaja en la candidata ya anotada en C-140, que pide una regla única y versionada. Esa regla debería saltar asignaciones y envoltorios conocidos (`command`, `exec`, `env`, `timeout N`, `xargs`, `\`), partir también por `)` en `case`, y no borrar `$(...)` dentro de los argumentos de `echo`.

### Patrón de las tres rondas (para el orquestador)
Las tres rondas tratan del mismo punto: comprobar AUTONOMIA-01 con regex sobre texto de shell.
- Ronda 1: el grep se saltaba un caso.
- Ronda 2: el `sed` sobre las comillas abrió un hueco mayor.
- Ronda 3: el partidor cierra los casos reales y deja huecos teóricos.

El código de producción no cambia desde `a4924e6`. Cerrar esto del todo exige un parser de shell o una regla versionada única, y eso es materia de C-140, no de esta tarea.

## Arbitraje respetado
F-02 a F-05 siguen como C-140 y la especificación sigue en C-139. Las desviaciones 1 a 6 siguen aceptadas.

## Tareas candidatas (fuera de alcance)
- C-140: regla sin-kubeconfig única y versionada que cubra F-08: asignaciones delante, envoltorios, `bash -c`, `echo "$(…)"`, `case` y `\kubectl`.

## Rutas de transcripciones
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/9d987332-269f-4da9-9d66-0a5449231723/scratchpad/U08-C-r3/ca9-mio.txt
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/9d987332-269f-4da9-9d66-0a5449231723/scratchpad/U08-C-r3/variantes.sh (carga `sin_kc` con `sed -n '/^sin_kc() {/,/^}/p'` del `ca9.sh` actual; contiene `v1` a `v6` y `n01` a `n18`)
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/9d987332-269f-4da9-9d66-0a5449231723/scratchpad/U08-C-r3/variantes-mio.txt
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/9d987332-269f-4da9-9d66-0a5449231723/scratchpad/U08-C-r3/ca10-mio.txt

No escribí en el worktree. El orquestador tiene que guardar este informe en `revisiones/U08-C/ronda-3.md`. El humano es quien fusiona.

```
VEREDICTO: VERDE
AMARILLO|bitacoras/U08-C-evidencia/ca9.sh:20-22|El partidor de órdenes no ve kubectl tras una asignación, un envoltorio, bash -c, echo "$(…)", case o \kubectl (teórico: ninguna de esas formas está en los scripts del diff)
INFORME: revisiones/U08-C/ronda-3.md
```
