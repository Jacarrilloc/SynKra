# Ronda 1 — U08-D

VEREDICTO: NO-VERDE

Los ocho criterios pasan en mis propias corridas: rojo primero, kind nuevo, segunda corrida, las cuatro mutaciones y los fallos tempranos. El arreglo de C-138 es correcto y está probado. Lo que bloquea es un solo NARANJA: el script tiene errores mudos de `set -e`, y la decisión 3 los prohíbe de forma expresa. Lo comprobé con una mutación: el script sale con 1 sin imprimir ningún `FALLA`.

Hash de la tarea: `git hash-object` da `00ffee964a116544ecd98ba0f49173629925bffc`, coincide. Base `367e09f`, punta `ae3907c`. El worktree quedó limpio (`git status --short` vacío) y el clúster quedó arriba con SynKra: `synkra-co` en el 39 (reversión, `listo`), `concuerda: true`.

## Criterios de aceptación, verificados por mí
| # | Criterio | Comando que corrí | Resultado |
|---|---|---|---|
| 1 | Plantilla cambia con solo plugins | `./gradlew :backend:test --rerun :backend:check` y el XML | pasa: `gradle=0`, 52 s. `ConstruirObjetosPaisTest` da `35 0 0 0` con las 3 pruebas nuevas. Backend completo `626 0 0 0`, `skipped` 0. La constante está en la línea 84 del `.java` y la anotación en la línea 55 del YAML. Recalculé el sha con Python desde el ConfigMap del YAML y da `cdcd487f…6239f`. El diff de `backend` son exactamente los 3 archivos de la decisión 2 |
| 2 | Script según las reglas | los `grep` de CA-2 | pasa: `sintaxis=0`, `--resolve` y `--cacert` 1 cada uno, `prohibidos=1`, `sin-kubeconfig=1`, `arquitectura=1`, `trap` en la línea 33 antes del `mktemp` de la 53. El único `kubectl` es el de `kc()` y lleva `--kubeconfig` |
| 3 | kind nuevo, install, límite y e2e | `bitacoras/U08-D-evidencia/ca3.sh` corrido por mí | pasa: `jars`, `borrar`, `install`, `limite` y `e2e` en 0, e2e en 3m56s. Las 10 líneas `4xx` en su orden, CU4 con antes igual a después, `synkra Apply` en Deployment, ConfigMap y Service. El core carga `[kyc-co 1.0.0, kyc-pe 1.3.0]`. Historial 5 reversión (`origenId` 2) `listo`, 3 `fallido`, 2 `listo`, 1 `listo`, todos de `e2e`. `concuerda: true`, último 5. En el clúster, la anotación es igual al sha que calculé del ConfigMap real (`8e171718…`). `synkra-pe` da 1, ver desviación 2 |
| 4 | Segunda corrida | `./scripts/e2e-api.sh` | pasa: `e2e-2=0`. Los cinco 409 de CU1 y las líneas `4xx` 1 y 3. En CU2, `cambios` trae `quitados: [kyc-pe 1.3.0]` e `iguales: [kyc-co]` con `core.igual: true`. 10 líneas `4xx` y `OK e2e` |
| 5 | Rojo primero | copia `git archive d8390bd` con `git init`, Gradle, `make cluster-delete` desde el worktree, `make install` desde la copia, e2e | pasa: `gradle=1` con las 3 nuevas en `FAILED` (35 corridas, 3 fallos) e `install=0`. El e2e pasa CU1 y CU2 y cae con `FALLA CU3: la plantilla del pod no cambió…`, `e2e=1`. Leí el estado de vuelta: el ConfigMap lista kyc-co y kyc-pe, `proxy/plugins` da solo `[kyc-co 1.0.0]`, el pod tiene 0 reinicios y la API dice `concuerda: true` con último 2. Es C-138 reproducida |
| 6 | Cada comprobación muerde | mis 4 mutaciones en copias (`ca6-mio.sh`) | pasa: `cu4-llega` da `FALLA CU4: el resourceVersion del ConfigMap cambió de 2144 a 2222` y después las anotaciones del ConfigMap quedan vacías. `cuerpo-ajeno` da `FALLA formato: GET /noexiste 404` (comprobé que ese 404 es `text/html` de NGINX). `codigo-otro` muestra `COMBINACION_RECHAZADA` frente a `OTRO_CODIGO`. `sin-vuelta` da `FALLA CU5: GET /api/despliegues/34 dio 404…`. Las cuatro salen con 1 y las cuatro restauraciones con 0 |
| 7 | Falla antes de tocar nada | los comandos de CA-7 | pasa: `sin-jars=1` con el mensaje exacto y `sin-kubeconfig=1` con `FALLA guardia … falta el kubeconfig`. `command -v` está en la línea 51, antes de los JAR. Historial 31 antes y 31 después; `resourceVersion` del Deployment 5162 antes y después |
| 8 | Diff acotado | los comandos de `ca8.sh` | pasa: `fuera=1`, `0`, `0`. El padre del commit de `ca8.txt` es `3c399f2`, el sha impreso |

## Desviaciones declaradas, juzgadas
1. **CU4 compara las imágenes ordenadas.** La acepto. En 30 lecturas seguidas, `{..image}` salió 25 veces como «core init» y 5 como «init core». Ordenar conserva el criterio del plan (el mismo conjunto de imágenes) sin falsos rojos.
2. **`synkra-pe` da 1 y no 0.** La acepto. Lo único que hay es `configmap/kube-root-ca.crt`; el namespace lo crea `aplicar-base`. El fallo es del criterio, no del código.
3. **`sin-vuelta` cae con un 404.** La acepto. `RegistroServicio.obtener` solo lee `reg_despliegue`, donde no hay fila hasta confirmar, así que un plan pendiente da 404. Sigue siendo `FALLA CU5` y `salida=1`.

## Hallazgos
### F-01 · NARANJA · scripts/e2e-api.sh:91 y :210 (clase) · Errores mudos de `set -e`, contra la decisión 3
La decisión 3 pide envolver todo comando externo que pueda fallar. Hay dos sitios que no lo hacen:
- **Línea 91.** `VC="$(grep … | head -1 | sed …)"`. Con `pipefail`, si `grep` no encuentra nada el script sale sin decir nada. Lo comprobé en una copia con la línea `version` de `core/build.gradle.kts` cambiada: `salida=1` sin ningún `FALLA`, y la traza de `bash -x` termina en `+ VC=` y luego `limpiar`. El `case` que diría «no cae en 2.x» nunca llega a correr.
- **Línea 210.** `est="$(jq -r .estado "$CUERPO")"` dentro de `esperar_estado`: es un `jq` sin envolver, y `jq` está nombrado en la decisión 3. Con un 200 cuyo cuerpo no sea JSON, la salida sería muda.
- **Miembros menores de la misma clase.** Las tuberías de las líneas 237 (`tr|sort|paste`), 251 (`paste`) y 311 (`tr|awk`): fallan poco, pero no están envueltas.

Arreglo esperado: pasar las líneas 91 y 210 por `ejecutar`, o añadirles `|| falla …`, y revisar el resto de la clase.

### F-02 · AMARILLO · bitacoras/U08-D-evidencia/ca5-mensajes.txt · Evidencia sin cabecera ni comando
No tiene la cabecera de la decisión 9, no trae el comando y no tiene un `.sh` versionado. Además, `ca3-pe.txt` dice `JAVA_HOME: sin definir`. Mi propia corrida reproduce los mensajes, así que no lo cuento como evidencia falsa.

### F-03 · AMARILLO · scripts/e2e-api.sh:205-219 · `esperar_estado` sobre un plan sin confirmar
Da un 404 genérico en lugar de algo como «¿se confirmó el plan?». Es la desviación 3 y el codificador ya la anota como pulido.

### F-04 · AMARILLO · scripts/e2e-api.sh:14-16 · Registro y repositorio como literales
`localhost:5001`, `127.0.0.1:5001` y `synkra-core-ejemplo` van escritos a mano, mientras que `scripts/imagenes.sh` respeta `SYNKRA_REGISTRO_IMAGENES`. La decisión 4 dicta esos mismos valores y el HEAD al registro los comprueba de forma indirecta, por eso no lo subo a NARANJA.

### F-05 · AMARILLO · scripts/e2e-api.sh:355 · El core 3.0.0 con 409 no confirma «misma imagen»
Cuando el 3.0.0 da 409, `subir_core` comprueba que el registro sirve la imagen, pero no que sea igual a `$IMAGEN`.

### F-06 · AMARILLO · ConstruirObjetosPaisTest · La prueba (b) añade un plugin en vez de cambiarlo
Los dos planes de la prueba (b) se distinguen en que uno tiene un plugin de más. Un caso con la misma lista y otra versión de un plugin cerraría mejor la decisión 1.

## Tareas candidatas (defectos reales fuera de alcance)
- El D4 se cierra en `fallido` por **vencimiento del plazo**: 184 s en mis dos corridas, `eventos` empieza por «Venció el plazo de vigilancia». Eso pasa aunque haya un `BackOff` del contenedor `core`. `LectorPais` no detecta el CrashLoopBackOff a tiempo (relacionada con C-82).
- Los `eventos` del contexto de fallo incluyen eventos de los ReplicaSets de despliegues anteriores, sin filtrar.
- El criterio de `synkra-pe` debería excluir `kube-root-ca.crt`.
- Cualquier criterio que compare `{..image}` como texto exacto es frágil, porque el orden no es estable.

## Rutas de transcripciones largas
Todas en `/tmp/claude-1000/-home-casdav-Projects-Master-SynKra/9d987332-269f-4da9-9d66-0a5449231723/scratchpad/U08-D-r1/`:
- CA-1: `ca1-gradle.txt`
- CA-3: `ca3.txt`
- CA-4: `ca4.txt`
- CA-5: `ca5-gradle.txt`, `ca5-borrar.txt`, `ca5-install.txt`, `ca5-e2e.txt`
- CA-6: `ca6.txt`, `ca6-mio.sh`, `mutar.py`, `mut-*.txt`, `rest-*.txt`

```
VEREDICTO: NO-VERDE
NARANJA|scripts/e2e-api.sh:91,210|errores mudos de set -e (grep/sed y jq sin envolver; decisión 3)
INFORME: revisiones/U08-D/ronda-1.md
```
