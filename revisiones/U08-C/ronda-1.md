# Ronda 1 — U08-C

VEREDICTO: NO-VERDE

Hash de la tarea verificado: `git hash-object` = `966592462c1bd71976e93befe77912dd8b677a8c`, coincide. Worktree en `168ff0c`. Desde `a4924e6` no cambia código (`git diff --name-only a4924e6 HEAD` solo toca `bitacoras/`). Al terminar, `git status --short` está vacío y `kind-synkra` y `kind-registry` siguen arriba con SynKra instalado: los dos nodos `Ready`, el backend `1/1 Running`, el controlador de Ingress `1/1` y `/actuator/health` responde `200`. No queda ningún binding `synkra-prueba-limite`.

## Criterios de aceptación, verificados por mí
| # | Criterio | Comando que corrí | Resultado |
|---|---|---|---|
| 1 | Unión (adaptada por el orquestador) | padres de `05a709e`, `merge-base --is-ancestor`, comparación archivo por archivo de A (40) y B (52) contra `05a709e`, `comm` de rutas, merges en `05a709e..HEAD` | pasa: padres `2eb1fe7` y `d6ca8b9`; `a=0`, `b=0`; ningún archivo de A ni de B difiere en la base; A y B no comparten rutas; `0` merges propios |
| 2 | Manifiestos y Ingress fijado | `bash bitacoras/U08-C-evidencia/ca2.sh`, más el grep de arquitectura original | pasa: 11 objetos, 4 más 8, 3, la lista de doce, `manifiesto-ok`, `ingress-ok`, `literales=1`, `arquitectura=1` (con exclusión: ver la desviación 3) |
| 3 | La guardia frena los seis objetivos | `ca3.sh`, con SynKra recién instalado | pasa: `salida=2` y `1` en los seis, el bloque `install` con las nueve líneas en orden, `nada-aplicado` |
| 4 | `make install` sobre un clúster recién creado | `make cluster-delete` y luego `ca4.sh` | pasa: `borrar=0`, `install=0` en 1m32 s, los dos nodos Ready, `misma-arquitectura`, controlador en el control-plane `Running true`, `kubernetes.io/tls`, `secreto-es-el-certificado`, `700`/`600`, `.gitignore:11:/.tls/`, `Bound standard 1Gi`, `deployer … Always …@sha256:38d18ff7…` (el mismo digest en `subida` y en `OK desplegar`), `10001`, `/data` de `10001`, las tres etiquetas, `200 0`, `{"status":"UP","db":"UP"}`, `co` y `pe` con `lectura` en `vacio`, `0` en los logs, seis `ok` y `OK limite`, `kubeconfig-global-intacto` |
| 5 | Idempotencia y digest | `ca5.sh` y `ca5-variante.sh` | pasa: `segunda=0`, `certificado-igual`, `corre-lo-subido`; `variante=0`, `registro-cambio`, se ve el `rollout restart`, `desplegar=0`, `corre-la-variante`, `restaurado=0`, `corre-lo-subido` |
| 6 | `verificar-limite` solo pregunta y no se deja engañar por un `kubectl` roto | `ca6.sh` (idéntico al de la tarea, comparado con `diff`) | pasa: seis `ok`, `salida=0`, `solo-pregunta`, `1`, `escrituras=1`, seis `FALLA … salió con 7`, `roto=1` |
| 7 | Los bindings de prueba hacen caer el límite | `ca7.sh` | pasa salvo el código de salida (desviación 2): cuando cae, `con-crb=2` y `con-rb=2`; caen la 3, 4 y 5, y luego solo la 5; `crb-borrado=1`, `rb-borrado=1`, `limpio=0` |
| 8 | Las mutaciones muerden; rojo primero | `ca8.sh` (10 mutaciones) y una copia de `f379e71` con `ca2.sh` y `make verificar-limite` | pasa: cada mutación cae donde debe, y después `desplegar=0` y `limite=0`. En el rojo: `base=1`, `overlay=1`, sin `manifiesto-ok`, `No rule to make target 'verificar-limite'` |
| 9 | Fallos claros y `trap` | `ca9.sh`, más el `trap` de `secreto-tls.sh` con `TMPDIR` propio en el caso que pasa y en el caso «solo tls.key» | pasa con el grep ajustado, pero ese grep tiene un hueco (F-01). Con el grep literal de la tarea da `sin-kubeconfig=0`. El temporal de `secreto-tls.sh` queda vacío en los dos casos |
| 10 | Diff acotado | órdenes de `ca10.sh` sobre `168ff0c`, más `git diff --summary` de los scripts de U08-B | pasa: `fuera=1`, `0`, `0`, `0`; no hay cambio de modo en `certificado-tls.sh` ni en `verificar-imagenes.sh` |

Pruebas propias de borde sobre `verificar-limite.sh`:
- Contra un servidor inalcanzable da seis `FALLA … salió con 1` y sale con 1.
- Un `kubectl` falso que responde siempre `no` da `FALLA` en la 1 y en la 2.

Ningún error se cuenta como `no`.

## Desviaciones declaradas, juzgadas
1. **`--provenance=false --sbom=false`**: aceptada. `rojo-digest.txt` muestra que el mismo contenido daba dos digests distintos, y que el nodo tenía cinco `repoDigests` para un solo `Id`. Mi CA-4, sobre otro clúster recién creado, volvió a dar `sha256:38d18ff7…`, el mismo digest que en la evidencia del codificador, así que es reproducible. Además `imagenes.sh` está en el reparto de la tarea.
2. **`make` sale con 2**: aceptada. GNU make devuelve 2 siempre que falla una receta. El script sí sale con 1 (`roto=1`, y `salida=1` en `limite-sin-as`). El defecto está en la especificación (CA-7 y H-27), no en el código.
3. **Exclusión de `setup-linux.sh` en CA-2**: aceptada. Es anterior a U08, no cambia en el diff y se excluye por nombre exacto, así que no debilita la regla para los archivos nuevos.
4. **`ca8.sh`**: aceptada. Pasar `KUBECONFIG_SYNKRA` hace falta: sin él, la guardia de la copia falla y la mutación no mide nada. `nueva()` además se para si el `sed` no cambia el archivo.
5. **`ca9.sh`**: el ajuste hacía falta, porque la decisión 11 exige el texto «kubectl auth can-i salió con» dentro de un `echo`. Pero excluir las líneas con `|| { echo` es demasiado amplio: ver F-01.
6. **Modos 0644**: aceptada. Lo verifiqué con `git diff --summary`. Que `imagenes.sh` pase a 0755 es necesario, porque el `Makefile` lo ejecuta directamente.

## Hallazgos
### F-01 · NARANJA · bitacoras/U08-C-evidencia/ca9.sh:6 · El grep `sin-kubeconfig` no ve un `kubectl` real escrito con el patrón de error del propio repositorio
`ca9.sh` descarta toda línea que contenga `|| { echo`. Lo probé en una copia de HEAD: cambié `secreto-tls.sh:32` por `kubectl apply -f "$TMP/secreto.yaml" || { echo "FALLA tls: apply"; exit 1; }`, que es la forma que ya usa `imagenes.sh` (`./gradlew … || { echo …`). El grep de `ca9.sh` da `sin-kubeconfig-mutado=1`: no lo detecta. Corrido a mano, sin el `KUBECONFIG` que exporta `make`, ese `kubectl` usaría `~/.kube/config` (AUTONOMIA-01). La mutación que trae `ca9.sh` (el envoltorio `k()`) no toca este patrón, así que tampoco demuestra que la regla muerda aquí.

Arreglo esperado: que el grep solo ignore lo que está entre comillas, por ejemplo con `sed 's/"[^"]*"//g'` antes del `grep -vF -- --kubeconfig`, en vez de descartar líneas enteras. Y añadir a la evidencia una mutación con este patrón.

### F-02 · AMARILLO · scripts/imagenes.sh:20 y scripts/desplegar.sh:9 · Constantes duplicadas
La cadena `Accept` (`ACEPTA`) y el cambio de `localhost` por `127.0.0.1` están copiados en los dos scripts, y también en `ca5.sh` y `ca5-variante.sh`. Si uno cambia y el otro no, el digest del registro dejaría de compararse con el mismo criterio.

### F-03 · AMARILLO · scripts/verificar-limite.sh:6 · `KC` por defecto relativo a `$PWD`
Los otros scripts nuevos parten de `RAIZ`. Corrido desde fuera de la raiz sin `KUBECONFIG_SYNKRA`, este apunta a un kubeconfig que no existe. Falla de forma segura (`FALLA … salió con 1`), pero no es coherente con el resto.

### F-04 · AMARILLO · scripts/desplegar.sh:15-19 · La espera del webhook no está acotada por llamada
El plazo de 60 s se comprueba con `SECONDS` entre iteraciones, pero cada `k get endpointslices` va sin `--request-timeout`. Si el apiserver se cuelga, la espera supera los 60 s de la decisión 9.

### F-05 · AMARILLO · scripts/verificar-limite.sh:25 · No se comprueba que la salida y el código coincidan
Una salida `yes` con código 1 se cuenta como `yes`. Con el `kubectl` real no pasa; sirve como defensa en profundidad.

## Tareas candidatas (defectos reales fuera de alcance)
- Especificación: CA-7 y H-27 piden que `make verificar-limite` salga con 1, pero GNU make devuelve 2 cuando falla una receta. Hay que reescribir el criterio o mirar el código del script, no el de `make`. El codificador ya lo propone.
- Especificación: el grep de CA-9 de la tarea no puede dar `1` mientras la decisión 11 exija el texto «kubectl auth can-i» en un mensaje. Los dos se contradicen.
- Especificación: el grep de arquitectura de CA-2 abarca `scripts/*.sh` e incluye `setup-linux.sh`, anterior a U08, que nombra arquitecturas por necesidad.
- `desplegar.sh`: la comparación de digest se vuelve frágil si algún día se reactivan las atestaciones. El codificador ya lo propone.
- Ingress NGINX `controller-v1.15.1` está retirado y no tendrá parches: plan de cambio de controlador.

## Rutas de transcripciones largas
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/9d987332-269f-4da9-9d66-0a5449231723/scratchpad/U08-C-r1/ca4-mio.txt (salida completa de `make install`)
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/9d987332-269f-4da9-9d66-0a5449231723/scratchpad/U08-C-r1/ca8-mio.txt
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/9d987332-269f-4da9-9d66-0a5449231723/scratchpad/U08-C-r1/ (ca1, ca2, ca3, ca5, ca5-variante, ca6, ca7, ca9 y rojo, todos `-mio.txt`)

No escribí en el worktree; el archivo `revisiones/U08-C/ronda-1.md` tiene que confirmarlo el orquestador con este contenido.

```
VEREDICTO: NO-VERDE
NARANJA|bitacoras/U08-C-evidencia/ca9.sh:6|El grep sin-kubeconfig no ve un kubectl real escrito con el patrón de error del propio repositorio
INFORME: revisiones/U08-C/ronda-1.md
```
