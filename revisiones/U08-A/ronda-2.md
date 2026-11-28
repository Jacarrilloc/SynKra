# Ronda 2 — U08-A

VEREDICTO: VERDE

F-01 queda cerrado. Los tres pegados que faltaban ya están en la evidencia y sus horas cuadran. Volví a correr CA-4, CA-6 y CA-9, y después CA-2 y CA-3: los cinco pasan con la punta `f228cc6`. No queda ningún ROJO ni NARANJA.

Comprobaciones previas:
- **Hash de la tarea:** coincide, `eedd72470cd67ad30c75c54b4f0349900e80e0a3`.
- **Punta de la rama:** `f228cc6`.
- **Código sin cambios:** `git diff 3a738aa HEAD -- Makefile deploy scripts` sale con 0 líneas.
- **Qué tocó esta ronda:** `git diff --stat 16740a8 HEAD` solo muestra evidencia, la bitácora y la respuesta (7 archivos). Los `.sh` de criterios no cambiaron.
- **Contexto:** `kind-synkra`, con `$PWD/.kube/kind-synkra.yaml`, que tiene permisos 600. Nunca usé `~/.kube/config` y no listé otros clústeres.

## F-01, verificado con evidencia mía
- **`make -n install` con el falso.** Está pegado en `bitacoras/U08-A-evidencia/ca4.txt`. La primera receta es `scripts/guardia-contexto.sh --antes-de-crear`, antes de `make cluster`, `make registro` y `make aplicar-base`. Lo corrí yo con el kubeconfig real y la primera línea es la misma. La bitácora aclara bien que con `-n` sale 0: el pegado demuestra el orden de las recetas. El rechazo real con salida 2 lo da `ca4.sh`.
- **CA-2 y CA-3 después de CA-9.**
  - Horas de los pegados: `ca2-tras-ca9.txt` es de las 00:32:34 y `ca3-tras-ca9.txt` de las 00:32:36. Las dos son posteriores a `ca9.txt` (00:32:31), y las tres anteriores al commit `f228cc6` (00:32:50).
  - El encabezado dice `sha=16740a8`, que tiene el mismo código que `3a738aa`, así que la evidencia sí pudo salir del diff actual.
  - Contenido: el Id es igual antes y después; `true registry@sha256:1be5527… always bridge kind`; `5000/tcp -> 127.0.0.1:5001`; `{} v2=0`; hosts.toml en los dos nodos; `push=0`, `tags:["1"]`, `pull=0` en los dos nodos, y las `rmi` limpian.
- **`T=<redactado>`.** Es la última línea de `ca6.txt`. `grep -rc eyJ` sobre toda la evidencia no encuentra ninguna coincidencia, ni en mi propia corrida.

## Criterios de aceptación, verificados por mí
| # | Comando que corrí | Resultado |
|---|---|---|
| 4 | `bash ca4.sh` (00:33:42) | pasa. Los seis objetivos salen con 2 y `1`; mismo-nombre, global y falta salen con 2; `falso-intacto`; `nada-aplicado`; `real salida=0` |
| 6 | `bash ca6.sh` (00:33:43) | pasa. 8 `yes` y 10 `no`; `propio=0`, `ajeno=1`, `lista-pais=0`, `lista-synkra=1`; `system:serviceaccount:synkra:deployer`; `[]` ClusterRoleBindings; 0 tokens en la salida |
| 9 | `bash ca9.sh` (00:33:45) | pasa. `sintaxis=0`; `trap` en la línea 18 y `docker run` en la 40; `error: docker network connect red-que-no-existe kind-registry falló con código 1`; `salida=1`; `quedo=1`; `restaurado=0` |
| 2 | `bash ca2.sh` después de CA-9 (00:33:48) | pasa. `salida=0` dos veces con el mismo Id `705713a7…`; la etiqueta, la imagen, la política de reinicio y las redes están bien; `127.0.0.1:5001`; `v2=0`; hosts.toml en los dos nodos |
| 3 | `bash ca3.sh` después de CA-2 | pasa. `push=0`, `tags:["1"]`, `pull=0` en los dos nodos, las `rmi` limpian; quedan 0 imágenes `localhost:5001/*` |
| 1, 5, 7, 8, 10 | no los repetí, porque el código no cambió | siguen vigentes los de mi ronda 1 |

## Hallazgos
No hay hallazgos nuevos desde F-05.
- **F-01 (NARANJA):** cerrado.
- **F-02 y F-03 (AMARILLO):** los ratificó el orquestador.
- **F-04 (AMARILLO):** aceptado.
- **Caché de `kubectl` y esperados de entorno:** quedan como C-135 y C-136, según tu arbitraje. Aquí CA-1 da `amd64` y CA-7 da `false/false` por diferencia de máquina, no por un defecto.

## Tareas candidatas
Ninguna nueva.

## Estado al terminar
- `kind-synkra` está arriba: los dos nodos Ready, v1.36.1, API en `https://127.0.0.1:44323`.
- `kind-registry` está arriba en `127.0.0.1:5001`. CA-9 lo recreó, como está previsto, y quedó con la etiqueta de SynKra.
- Quedan 0 imágenes `localhost:5001/*`.
- `git status --short` sale vacío: el worktree está limpio.

## Rutas de transcripciones
En /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/9d987332-269f-4da9-9d66-0a5449231723/scratchpad/U08-A-r2/:
- ca4.txt
- ca6.txt
- ca9.txt
- ca2.txt
- ca3.txt

```
VEREDICTO: VERDE
INFORME: revisiones/U08-A/ronda-2.md
```
