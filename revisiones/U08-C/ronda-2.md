# Ronda 2 — U08-C

VEREDICTO: NO-VERDE

Comprobaciones previas:
- El hash de la tarea coincide: `git hash-object` da `966592462c1bd71976e93befe77912dd8b677a8c`.
- El worktree está en `b3c8b65`.
- `git diff a4924e6 HEAD -- Makefile deploy scripts` sale con 0 líneas: el código no cambió, y CA-1 a CA-8 de mi ronda 1 siguen vigentes.
- Al terminar, `git status --short` sale vacío.
- `kind-synkra` sigue arriba con los dos nodos `Ready` y `synkra-backend` en `1/1`. No borré nada ni toqué `~/.kube/config`, `gitops2` ni `m08-gitops1`.

## Criterios de aceptación, verificados por mí
| # | Criterio | Comando que corrí | Resultado |
|---|---|---|---|
| 1-8 | Sin cambios de código desde `a4924e6` | diff vacío en `Makefile`, `deploy` y `scripts` | siguen vigentes los de la ronda 1 |
| 9 | Fallos claros, `trap` y la regla sin-kubeconfig | `bash bitacoras/U08-C-evidencia/ca9.sh` sobre HEAD, más mis variantes en copias | `sin-kubeconfig=1`, `mutado-k=0`, `mutado-patron-revisor=0`, `registro-caido=1`, `media-pareja=1`, `secreto-intacto` (salen igual que en `ca9.txt`). **Pero la regla nueva tiene un hueco peor que el de la ronda 1 (F-06).** |
| 10 | Diff acotado | `bash bitacoras/U08-C-evidencia/ca10.sh` | pasa: `b3c8b65`, `0` cambios en el árbol, `fuera=1`, `0`, `0`, `0`. Los únicos cambios de modo son los de la ronda 1 (`imagenes.sh` pasa a 0755). |

## F-01 · resuelto para su caso
Mi mutación con `|| { echo` en una copia de `secreto-tls.sh:32` ya se detecta: da `sin_kc=0` y muestra la línea. La mutación de `k()` también se detecta.

## Hallazgos
### F-06 · NARANJA · bitacoras/U08-C-evidencia/ca9.sh:8 · Quitar el texto entre comillas borra los `kubectl` dentro de `"$(...)"`, que es la forma más común en estos scripts
`sed -E 's/"[^"]*"//g'` no distingue entre un mensaje y una sustitución de orden entre comillas.

**En HEAD esas líneas no se revisan.** Esto es lo que ve `sin_kc` en ellas:
- `verificar-limite.sh:23` queda como `respuesta=$KC || rc=$?`.
- `secreto-tls.sh:37` queda como `TIPO=$KC || rc=$?`.
- `secreto-tls.sh:41` queda como `EN_CLUSTER=$KC || rc=$?`.

La palabra `kubectl` desaparece en las tres antes del grep.

**Mutaciones que no se detectan.** En copias de HEAD quité `--kubeconfig "$KC"` en cada una de esas tres líneas (variantes `v1-canI`, `v2-tipo` y `v3-encluster`):

| Mutación | `sin_kc` (ronda 2) | grep de la ronda 1 |
|---|---|---|
| cada una de las tres | `1`: no la detecta | `0`: sí la detecta |

Es una regresión: el arreglo cerró el hueco de `|| { echo` y abrió otro más grande. Tres de las órdenes `kubectl` reales del diff quedan fuera de la regla, y una de ellas es la pregunta `auth can-i` de `verificar-limite.sh`. Sin `--kubeconfig`, esas órdenes usarían `~/.kube/config` (AUTONOMIA-01).

Asumo mi parte: el `sed` era mi sugerencia de la ronda 1, y no la probé contra `"$(...)"`.

Arreglo esperado: no borrar las comillas a ciegas. Por ejemplo, quitar solo los argumentos de `echo` (`sed -E 's/echo "[^"]*"/echo/g'`) y conservar el resto, o bien contar `kubectl` como orden cuando va tras el inicio de línea, `$(`, `;`, `&&`, `||`, `|` o `{`. Hay que añadir a la evidencia al menos una mutación del tipo `X="$(kubectl …)"`.

Otras variantes, que sí se detectan:
- `v4`: un `kubectl get … -o jsonpath="{.type}" …` sin `--kubeconfig`, con comillas dobles dentro de los argumentos. Se detecta (`0`).
- `v6`: un `--kubeconfig` que solo aparece dentro de un mensaje. Se detecta (`0`).

### F-07 · AMARILLO · bitacoras/U08-C-evidencia/ca9.sh:8 · Una línea con dos `kubectl` se juzga entera
Probé `kubectl --kubeconfig "$KC" get ns >/dev/null && kubectl apply -f … || rc=$?` (variante `v5`). Da `1` tanto con la regla nueva como con la de la ronda 1: el `--kubeconfig` de la primera orden tapa la segunda. Este hueco ya existía y ningún script lo usa hoy. Si F-06 se arregla partiendo la línea por órdenes, este también se cierra.

## Arbitraje respetado
F-02 a F-05 siguen como C-140 y las notas de especificación como C-139: no los exijo aquí. Las desviaciones 1 a 6 de la ronda 1 siguen aceptadas.

## Tareas candidatas (fuera de alcance)
- Ninguna nueva. La regla sin-kubeconfig debería vivir en un único script versionado, no copiada en la evidencia. Puede ir a C-139 o C-140, según decida el orquestador.

## Rutas de transcripciones
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/9d987332-269f-4da9-9d66-0a5449231723/scratchpad/U08-C-r2/ca9-mio.txt
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/9d987332-269f-4da9-9d66-0a5449231723/scratchpad/U08-C-r2/variantes.sh (las variantes v1 a v6; `sin_kc` se carga literal desde `ca9.sh`)
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/9d987332-269f-4da9-9d66-0a5449231723/scratchpad/U08-C-r2/variantes-mio.txt
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/9d987332-269f-4da9-9d66-0a5449231723/scratchpad/U08-C-r2/lo-que-ve-sin_kc.txt
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/9d987332-269f-4da9-9d66-0a5449231723/scratchpad/U08-C-r2/ca10-mio.txt

No escribí en el worktree. El orquestador tiene que guardar este informe en `revisiones/U08-C/ronda-2.md`.

```
VEREDICTO: NO-VERDE
NARANJA|bitacoras/U08-C-evidencia/ca9.sh:8|Quitar el texto entre comillas borra los kubectl dentro de "$(...)": tres órdenes reales quedan fuera de la regla sin-kubeconfig
AMARILLO|bitacoras/U08-C-evidencia/ca9.sh:8|Una línea con dos kubectl se juzga entera
INFORME: revisiones/U08-C/ronda-2.md
```
