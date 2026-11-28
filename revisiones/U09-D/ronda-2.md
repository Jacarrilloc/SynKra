# Ronda 2 — U09-D

VEREDICTO: VERDE

Corrí yo todos los criterios y todos pasan. F-01 y F-02 quedan cerrados, y los dos los comprobé con una mutación propia que muerde. F-05 está corregido. Lo único que queda abierto es un AMARILLO nuevo (F-06), que no bloquea.

Datos de partida:
- **Tarea:** `git hash-object` da `529ca4f3faaa8c5176f77029b96d0770baad67e5`, como se pidió.
- **Rama:** `m6r/U09-D` en d5f30a6.
- **Base:** 27cc6cc, que es el `merge-base` con `main` y coincide con `origin/main`.
- **Último commit de código:** ec6108e. Solo cambia `scripts/desplegar.sh`, en 2 líneas.
- **Edad de mis corridas:** todas son de hoy, entre las 05:38 y las 05:50, sobre esa punta.

## Criterios de aceptación, verificados por mí
| # | Criterio | Comando que corrí | Resultado |
|---|---|---|---|
| 1 | Unión de B y C, pruebas y `resto` | bloque literal con 5d0a48d y 3dfda60 | pasa: `b=0 c=0`; los tres blobs `899488ab…`, `85479d56…` y `d5d7babb…`; 25, 21 y 21 pruebas con `failed=0 notExecuted=0`; `1 1 1`; `todo=0` con 117/117; `resto=1`. Los trx son nuevos (05:38). |
| 2 | Imagen del plan y versión | bloque literal | pasa: RepoDigests `aac50cee…` y `8c6edfb2…`; `10001:10001`; `plan=0`; `latest` borrada; `imagenes=0`; `…:0.1.0 user=10001:10001 arch=amd64`, igual a la del servidor (`amd64`); 2 líneas `FROM @sha256` |
| 3 | Imagen verificada, sin SDK, un solo paquete | `ca3.sh` | pasa: 31 líneas `ok` (incluidas las de U08); `OK imagenes`; `verificar=0`; `etapa=0`; `['Microsoft.AspNetCore.App.Internal.Assets/10.0.15']`; `0`; runtimes 10.0.15; dueño 0; `escribe=1` |
| 4 | Arranca sin root; la URL mala no arranca | `ca4.sh` | pasa: `espera=0`, `200`, las dos líneas, `10001`, `0`, `malo=139`, `1` |
| 5 | Manifiestos sin clúster | `ca5.sh` | pasa: 13 objetos; 4 (+11); versiones `0.1.0/1.0.0/0.1.0`; 3; 2; 3; los 15 objetos; las 3 rutas; `literales=1` |
| 6 | Cada regla muerde | `ca6.sh` (una copia por mutación) | pasa: caen las 12 de la tarea y `raiz-doble`. El render da el orden `['synkra-backend','synkra-front']`. La imagen buena queda intacta, `d1d80b7a…` antes y después. |
| 7 | `make install` y front servido | `ca7.sh` (con `make cluster-delete`) | pasa: `borrar=0`; `install=0` en 1m28s; 4 líneas `subida`; 2 `OK desplegar`, cada una con el digest de su `subida`; `OK limite: 6`; front `1/1`, Service `8080/TCP` e Ingress en `synkra.local`; `Running true …:0.1.0 Always false …@sha256:d1d80b7a…`; `10001`; `["0.1.0"]`; `200 0`; h1, `co` y `pe`; `["co","pe"]`; `UP`; `200`; `4`; `0`; `kubeconfig-global-intacto` |
| 7v | Variante y mutación «desplegar-sin-front» | `ca7-variante.sh` 3+3 veces y `ca7-mutacion.sh` 2+1 veces | pasa, ver F-01 |
| 8 | Diff acotado | bloque literal con `main` | pasa: árbol limpio; `fuera=1`; una sola línea `+    <Version>0.1.0</Version>`; `0`; `0` |

## Hallazgos de la ronda 1

### F-01 · cerrado · `ca7-variante.sh` y `ca7-mutacion.sh`
`corre()` ya descarta los pods con `deletionTimestamp` y los que no están en `Running`. La mutación usa ahora un testigo positivo, `corre-lo-viejo`.

**Variante.** Corrí 3 veces una copia del guion con dos `kubectl get pods` de solo lectura añadidos, uno tras `make desplegar` y otro tras restaurar. Después corrí 3 veces el guion literal. Las 6 corridas dan siempre `variante=0 registro-cambio desplegar=0 corre-la-variante restaurado=0 corre-lo-subido`, sin `corre-lo-viejo`.

La foto prueba que el filtro hace falta y funciona:
- En las corridas 2 y 3, justo después de `make desplegar`, el primer pod de la lista era el viejo: `synkra-front-57d7879b4-blq8n phase=Running del=2026-11-28T10:42:28Z …@d1d80b7a…`, antes que el nuevo `…@8bcdb625…`.
- Con el `items[0]` de la ronda 1 eso habría fallado. Con el filtro sale `corre-la-variante`.
- El pod en `Terminating` sigue en `phase=Running`, así que lo descarta el filtro de `deletionTimestamp`.

**Mutación.** Corrí 2 veces el guion literal y 1 vez con la foto. Las 3 dan `42:for D in synkra-backend; do`, un solo `OK desplegar` (el del backend), `corre-lo-viejo`, ningún `corre-la-variante` y `corre-lo-subido`. La foto muestra un único pod en `d1d80b7a…` mientras el registro tiene la variante `82dbe69e…`. La mutación ya distingue el código bueno del malo.

### F-02 · cerrado · `scripts/desplegar.sh:60`
Lo comprobé con un señuelo propio en `kind-synkra`:
- Creé el pod `aaa-u09d-revisor-falla` en `synkra`, con la etiqueta `app.kubernetes.io/name=synkra-front`, un contenedor llamado `synkra-front`, la imagen del backend y el comando `java -opcion-que-no-existe`.
- Quedó en `phase=Failed`, sin `deletionTimestamp`, con `imageID …@48b554d0…`, y aparece **primero** en la lista.
- **Código bueno:** `make desplegar` da `desplegar=0`, sin reinicio, y `OK desplegar: …synkra-front:0.1.0 sha256:d1d80b7a…`.
- **Mutación en una copia** (quité `or p['status'].get('phase') != 'Running'`): `desplegar: synkra-front corre sha256:48b554d0… … rollout restart`, después `FALLA desplegar: synkra-front corre sha256:48b554d0…` y `desplegar=2`. La mutación muerde.
- Borré el señuelo con `--wait`.

El cambio no rompe nada de lo de antes:
- `bitacoras/U08-C-evidencia/ca5-variante.sh` da `variante=0 registro-cambio desplegar=0 corre-la-variante restaurado=0 corre-lo-subido`. El backend hizo `rollout restart` hacia `c89f74a6…` y quedó restaurado en `48b554d0…`.
- `bitacoras/U08-C-evidencia/ca9.sh` sale igual a su `ca9.txt` versionado, salvo las líneas de cabecera.

### F-03 y F-04 · arbitrados como C-147
Siguen sin tocar, como decidió el arbitraje.

### F-05 · cerrado
- El comentario de `ca8.sh` ya no afirma que falta `main`.
- La bitácora deja `malo=139` como «distinto de 0» y ya no afirma la causa.
- `ca7-variante.sh:5` usa `KUBECONFIG=/dev/null kubectl kustomize`.

## Hallazgos nuevos

### F-06 · AMARILLO · `revisiones/U09-D/ronda-1-respuesta.md` y cabeceras de la evidencia · Commit mal atribuido y cabecera que esconde cambios de guiones sin confirmar
- La respuesta dice que F-01 y F-05 se corrigieron «en ec6108e (guiones de evidencia)». No es así: ec6108e solo toca `scripts/desplegar.sh`. `ca7-variante.sh`, `ca7.sh`, `ca8.sh` y `repite.sh` entraron en 775408e.
- Las cabeceras de `ca7.txt`, `ca7-variante.txt` y `ca7-mutacion.txt` dicen `sha: ec6108e 0 cambios de codigo sin confirmar`. Se generaron con esos guiones todavía sin confirmar, porque `corre.sh` no cuenta nada de `bitacoras/` como cambio.
- No es un ROJO: mis corridas frescas de los guiones ya confirmados reproducen todos los testigos. Pero la cabecera no puede demostrar con qué versión del guion se generó la evidencia.

## Tareas candidatas (defectos reales fuera de alcance)
- Ninguna nueva. F-03 y F-04 ya están en C-147.

## Rutas de transcripciones largas
Todas en `/tmp/claude-1000/-home-casdav-Projects-Master-SynKra/9d987332-269f-4da9-9d66-0a5449231723/scratchpad/U09-D-r2/`:
- Criterios: `ca1.txt` a `ca7.txt` y `ca2-build.txt`; `id-antes.txt`.
- F-01: `var-foto.sh` (copia del guion con foto), `var-foto-1.txt` a `var-foto-3.txt`, `var-lit-1.txt` a `var-lit-3.txt`; `mut-lit-1.txt`, `mut-lit-2.txt`, `mut-foto.sh` y `mut-foto.txt`.
- F-02: `pod-falla.yaml`, `f02-bueno.txt`, `f02-mutacion.txt`.
- U08-C: `u08c-ca5-variante.txt`, `u08c-ca9.txt`.

## Estado al terminar
- `kind-synkra` queda arriba con SynKra y el front, instalados desde el worktree. Backend y front están `1/1 Running`, cada uno con el digest de su registro: backend `48b554d0…` y front `d1d80b7a…`. `https://synkra.local/` responde `200 0`. El señuelo está borrado.
- En el worktree, `git status --short` está vacío.
- No quedan contenedores `synkra-u09d*` ni imágenes `mutacion.local:1/*`. Tampoco quedan `:latest`, imágenes colgadas ni imágenes con la etiqueta de las variantes.
- En el Docker local quedan solo las cuatro imágenes `localhost:5001/…` buenas.
- No usé `~/.kube/config`, `gitops2` ni `m08-gitops1`, y no hice ninguna descarga.

```
VEREDICTO: VERDE
AMARILLO|revisiones/U09-D/ronda-1-respuesta.md y cabeceras ca7*.txt|Atribuye a ec6108e los guiones que entraron en 775408e; la cabecera "0 cambios" no cuenta los guiones de evidencia sin confirmar
INFORME: revisiones/U09-D/ronda-2.md
```
