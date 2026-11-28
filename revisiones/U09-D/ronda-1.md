# Ronda 1 — U09-D

VEREDICTO: NO-VERDE

Verifiqué el hash de la tarea: `git hash-object` da 529ca4f3faaa8c5176f77029b96d0770baad67e5. Revisé `m6r/U09-D` en f044d44, con el último commit de código en 1bc94ae y la base en `origin/main` 27cc6cc (es la misma `main` local). La evidencia del codificador sale de 1bc94ae, y esa edad es correcta. Las puntas VERDES son 5d0a48d (U09-B) y 3dfda60 (U09-C), las segundas madres de las fusiones 00c54ef y 5e00878.

Qué falta para VERDE: en la variante de CA-7, la lectura de vuelta de `ca7-variante.sh` sale bien o mal según el azar, y la mutación «desplegar-sin-front» no distingue código bueno de código malo (F-01). El arreglo de `desplegar.sh` sí es correcto. Lo comprobé con lectura de vuelta en el front y en el backend.

## Criterios de aceptación, verificados por mí
| # | Criterio | Comando que corrí | Resultado |
|---|---|---|---|
| 1 | Unión B+C, pruebas y `resto` | bloque literal con 5d0a48d y 3dfda60 | pasa: `b=0 c=0`, los blobs `899488ab…`, `85479d56…`, `d5d7babb…`, 25/21/21 sin fallos y `notExecuted=0`, `1 1 1`, `todo=0` con 117/117, `resto=1`. Los trx se crearon a las 05:14 y la suite completa tardó 1,6 s, con las 117 ejecutadas. |
| 2 | Imagen del plan y versión | bloque literal | pasa: los dos RepoDigests `aac50cee…` y `8c6edfb2…`, `10001:10001`, `plan=0`, `latest` borrada, `imagenes=0`, `…:0.1.0 user=10001:10001 arch=amd64` igual a `amd64`, dos `FROM @sha256` y `2` |
| 3 | Verificada, sin SDK, un solo paquete | `bash bitacoras/U09-D-evidencia/ca3.sh` | pasa: todas las líneas `ok` (también las de U08), `OK imagenes`, `verificar=0`, `etapa=0`, `['Microsoft.AspNetCore.App.Internal.Assets/10.0.15']`, `0`, los runtimes 10.0.15, dueño 0 y `escribe=1` |
| 4 | Arranca sin root y la URL mala no arranca | `ca4.sh` | pasa: `espera=0`, `200`, `blazor.web.js 200`, las dos líneas, `10001`, `0`, `malo=139`, `1` |
| 5 | Manifiestos sin clúster | `ca5.sh` | pasa: 13, 4 (+11), versiones `0.1.0/1.0.0/0.1.0`, 3, base del front, solo-backend, los 15 objetos, las 3 rutas, `literales=1` |
| 6 | Cada regla muerde, y el rojo primero | mi propio `mut.sh` y `mut2.sh` (28 mutaciones, una copia con `git archive` por mutación) | pasa: caen las 12 de la tarea, escritas por mí, y 13 más (estrategia, variable de más, sonda, `IfNotPresent`, `emptyDir` sin límite, `serviceAccountName`, ruta `/api` en el Ingress del front, anotación, versión del csproj, entrypoint, puerto 9090, y U08: backend sin `runAsNonRoot`, tag del backend, objeto de más, `USER 0` en el backend). La imagen buena queda intacta (`d1d80b7a…`). En la copia de c533215: `base=1 backend=1 front=1` y `FALLA front/Dockerfile existe` |
| 7 | `make install` y front servido | `ca7.sh` | pasa: `borrar=0`, `install=0` en 1m34s, 4 `subida`, 2 `OK desplegar` con su digest, `OK limite: 6`, `Running true …:0.1.0 Always false …@sha256:d1d80b7a…`, `10001`, `["0.1.0"]`, `200 0`, h1 y co/pe, `["co","pe"]`, `UP`, `200`, `4`, `0`, `kubeconfig-global-intacto` |
| 7v | Variante y mutación | `ca7-variante.sh` 3 veces, `ca7-mutacion.sh` 1 vez | **falla**, ver F-01 |
| 8 | Diff acotado | bloque literal, con `main` | pasa: `fuera=1`, la única línea `+    <Version>0.1.0</Version>`, `0`, `0` |

## Hallazgos

### F-01 · ROJO · `bitacoras/U09-D-evidencia/ca7-variante.sh:8` (`corre`) · La lectura de vuelta de la variante toma `items[0]` y lee el pod en `Terminating`
Es el mismo defecto que el codificador arregló en `desplegar.sh`, pero sigue vivo en el guion que debe demostrarlo. `corre()` lee `{.items[0]…imageID}` sin filtrar `deletionTimestamp`. Con `RollingUpdate`, el pod viejo sigue un rato en la lista y su orden por nombre es azaroso.

Corrí el guion literal tres veces. Las tres veces `desplegar.sh` terminó bien: imprimió `rollout restart` y luego `OK desplegar … <digest de la variante>`. Aun así:
- Corrida 1: sale `corre-la-variante` y **no** sale `corre-lo-subido`.
- Corridas 2 y 3: **no** sale `corre-la-variante` y sí sale `corre-lo-subido`.

Mi guion de revisor fotografía los pods justo después de `make desplegar`. Salió `synkra-front-58d465bb8c-zbz4d del=2026-11-28T10:22:27Z …@d1d80b7a…` junto al pod nuevo `…@a813015f…`, y lo mismo después de restaurar.

La consecuencia más grave está en `ca7-mutacion.sh`. Su prueba es la **ausencia** de `corre-la-variante`, y esa ausencia también aparece con el código bueno (2 de 3 veces). Así, «desplegar-sin-front» no demuestra nada.

Arreglo sugerido, dentro de los guiones de evidencia: filtrar `deletionTimestamp` como en `desplegar.sh`, o esperar a que se borre el pod viejo con `kubectl wait --for=delete … --timeout=`. Además, que la mutación imprima un testigo positivo, por ejemplo `corre-lo-viejo`, en vez de depender de una ausencia. Luego hay que regenerar `ca7-variante.txt` y `ca7-mutacion.txt`.

Barrido de la clase: `ca7.sh:9` (`P=… items[0].metadata.name`) tiene la misma forma. Hoy no falla porque, recién instalado, no hay rollout. Conviene filtrar igual. `bitacoras/U08-C-evidencia/ca5-variante.sh` lee el backend (`Recreate`), así que no le afecta.

### Sobre el arreglo de `desplegar.sh` (1bc94ae): correcto
- Con el front, mi variante de revisor da `vivo-corre-la-variante` y `vivo-corre-lo-subido`. La foto confirma que el pod en `Terminating` existe justo después del `rollout status`.
- Con el backend (`Recreate`), mi variante da `rollout restart` del backend, un solo pod y los dos testigos.
- El guion de U08-C, `ca5-variante.sh`, sigue completo: `variante=0 registro-cambio desplegar=0 corre-la-variante restaurado=0 corre-lo-subido`.
- U08-C `ca9.sh` sale idéntico a su `ca9.txt` versionado (sintaxis, `sin-kubeconfig=1`, `registro-caido=1`, `media-pareja=1` y las mutaciones `k`/v1–v6).
- U08-C `ca2.sh` sin clúster pasa: `base`, `overlay`, `solo-backend`, `manifiesto-ok`, `ingress-ok`, `literales=1`, `arquitectura=1`.
- `--versiones` y `desplegar.sh` eligen el Deployment por nombre. Las mutaciones `primero` (tomar `[1]`, es decir el front, como backend) dan `FALLA versiones`.
- Los tres verificadores rechazan un YAML roto y una entrada vacía con salida 1.

### F-02 · AMARILLO · `scripts/desplegar.sh:305-311` · `corre` no filtra los pods `Failed`/`Evicted`
Un pod desalojado de un ReplicaSet viejo no lleva `deletionTimestamp` y conserva su `imageID`. Con `head -1` podría disparar un restart de más y luego `FALLA`. Filtrar por `status.phase == Running` lo cierra.

### F-03 · AMARILLO · `scripts/verificar-imagenes.sh:701,703` · Los `docker run --rm` del front no llevan `--name` ni entran en `CONTENEDORES`
Si se interrumpe, el `trap` no los borra. Las comprobaciones de U08 sí les ponen nombre.

### F-04 · AMARILLO · `scripts/verificar-backend.py:388` · `VERSION_FRONT` se lee al importar
Por eso `--base` del backend falla si el csproj no tiene `<Version>`: en c533215 sale `solo-backend=1` por eso. Es un acoplamiento nuevo del verificador del backend con el front.

### F-05 · AMARILLO · Bitácora y evidencia
- `ca8.sh` dice «este worktree no tiene rama main local». No es cierto: `main` existe y es 27cc6cc, igual a `origin/main`. Sin efecto en el resultado.
- `malo=139` se explica como «lo que Docker devuelve» por una excepción no controlada. 139 es SIGSEGV; un aborto daría 134. El criterio solo pide distinto de 0, pero la explicación no está verificada.
- `ca7-variante.sh:5` corre `kubectl kustomize` sin `KUBECONFIG=/dev/null`, que es la regla de «Fuera». No habla con ningún servidor.

### Nota (no es hallazgo)
Con la mutación literal «primero», que vuelve a usar `[0]` para los dos Deployments, no cae nada, porque el backend y el front tienen los dos la versión 0.1.0. La tarea lo prevé («la mutación se escribe para que tome el otro»), y la variante que toma el front como backend sí cae.

## Tareas candidatas (defectos reales fuera de alcance)
- Ninguna nueva.

## Rutas de transcripciones largas
Todas en `/tmp/claude-1000/-home-casdav-Projects-Master-SynKra/9d987332-269f-4da9-9d66-0a5449231723/scratchpad/U09-D-r1/`:
- `ca1.txt` a `ca5.txt`, `ca7.txt`
- `mut.txt`, `mut2.txt`, y los guiones `mut.sh` y `mut2.sh`
- `ca7-variante.txt`, `ca7-variante-1.txt`, `ca7-variante-2.txt`, `ca7-mutacion.txt`
- `variante-revisor.sh`, `variante-revisor-front.txt`, `variante-revisor-backend.txt`
- `u08c-ca5-variante.txt`, `u08c-ca9.txt`

Estado al terminar:
- `kind-synkra` está arriba. Backend y front `1/1 Running`, y cada pod corre el digest de su registro (backend `48b554d0…`, front `d1d80b7a…`). `https://synkra.local/` responde `200 0`.
- En el worktree, `git status --short` está vacío.
- No quedan imágenes `mutacion.local:1/*`, ni `synkra-u09d*`, ni `:latest`, ni con las etiquetas de prueba de las variantes, y tampoco contenedores `synkra-u09d*`.

```
VEREDICTO: NO-VERDE
ROJO|bitacoras/U09-D-evidencia/ca7-variante.sh:8 (CA-7 variante y mutación)|corre() lee items[0] e incluye el pod en Terminating: los testigos salen al azar y la mutación desplegar-sin-front no distingue
INFORME: revisiones/U09-D/ronda-1.md
```
