# Ronda 2 — U08-B

VEREDICTO: NO-VERDE

Los nueve criterios pasan en mis propias corridas, y F-01, F-02, F-04 y F-05 quedan cerrados. Queda en pie un NARANJA nuevo, y lo provocó el arreglo de F-03 (que yo mismo sugerí en la ronda 1). Las versiones ahora se derivan de `build.gradle.kts`, y con eso el chequeo de la decisión 10 ya no puede fallar. Si cambia una versión, toda la cadena da verde, pero el overlay sigue apuntando a la etiqueta vieja. En la ronda 1 eso salía con 1.

## Criterios de aceptación, verificados por mí
Worktree `/home/julian/Escritorio/Proyectos/wt-m6r-U08-B`, HEAD `8a6b076`, `KUBECONFIG=/dev/null`. La única orden de `kubectl` fue `kustomize`. Reconstruí las tres imágenes yo mismo.

| # | Criterio | Comando que corrí | Resultado |
|---|---|---|---|
| 1 | Base del backend | CA-1 literal | pasa: `OK backend: 3 objetos`, `salida=0`, las 3 tuplas, `Recreate deployer True True 10001 10001`, `[('datos','/data'),('tmp','/tmp')]`, `0` |
| 2 | Overlay `kind` | CA-2 literal | pasa: `kustomize=0`, `OK backend: 4 objetos`, `salida=0`, la imagen, las 6 variables exactas, `nginx` con TLS, las 2 rutas, `0`, `0`, `0` |
| 3 | Certificado TLS | CA-3 literal, más `-text` y sha antes y después | pasa: solo `tls.crt` y `tls.key`, `600`, CN y SAN, prime256v1, 365 días, `vigente=0`, `par-ok`, `repetido=1` con sha igual, `sin-dir=1` |
| 4 | Imágenes sin root, `arm64` | `bash scripts/imagenes.sh` y el resto de CA-4 | pasa: `imagenes=0`, las 3 con `user=10001:10001 arch=arm64`, servidor `arm64`, 21 `ok`, `OK imagenes`, `verificar=0`, 6 `FROM` con digest, 2/2/2, `literales=1` |
| 5 | El init | CA-5 literal | pasa con la desviación ya aceptada: `uid=10001(synkra)…`, `apk=127`, curl y sha256sum, `-r-xr-xr-x`, `mismo-script`, `test=0` |
| 6 | Backend en `docker run` | `bash bitacoras/U08-B-evidencia/ca6.sh` (tmpfs `exec`, desviación aceptada) | pasa: `espera=0` a los 15 s, `200` en las tres rutas, `10001`, `synkra.db*` de `10001`, `('4',)`, `0` |
| 7 | Fallos con mensaje claro | CA-7 literal (el `PATH` falso va en un subshell) | pasa: `sintaxis=0`, 3 líneas, `yaml=1`, `vacio=1`, `java=1`, `FALLA certificado: openssl req -x509 salió con 7`, `openssl=1`, `0` |
| 8 | Cada regla muerde | Mis propios `ca8-man.sh` y `ca8-img.sh`, una copia con `git archive HEAD` por mutación | pasa: caen las 10 mutaciones. Las 6 de manifiestos salen con 1 (`registro-base` cae por `--base`). `user-0` da `User … visto 0`, `sin-digest` da `FROM`, `uid-java` da `User: esperado 10002:10001 / visto 10001:10001`, y `shellcheck` da SC2086 y `FALLA synkra-init-descarga: docker build …`. Mis 5 sondeos extra de manifiestos también caen. Los `Id` buenos no cambian dentro de la corrida y se borraron las 4 etiquetas `mutacion.local` |
| 9 | Diff acotado | CA-9 literal | pasa: `8a6b076`, `0`, `fuera=1`, `0`, `0`. `ca9.txt` está hecho sobre `6eb0066`, que es el padre de su commit |

Edad de la evidencia: entre `fb00045` y `8a6b076` solo cambian la bitácora y la evidencia, así que la evidencia sobre `fb00045` sí salió del código final.

## Cierre de los hallazgos de la ronda 1
- **F-01 (NARANJA), cerrado.** Con `SYNKRA_REGISTRO_IMAGENES=rojo-revisor.local:1`, `sin-apk` da `No such image` y no aparece ningún `Head https://…`. Sale `rc=1` y no se trajo ninguna imagen.
- **F-02 (AMARILLO), cerrado.** Cae `from …` en minúsculas, y también un `FROM` con sangría (mutación nueva mía `extra-from-sangria`). Las dos dan `FALLA init-descarga/Dockerfile FROM` y `rc=1`.
- **F-03 (AMARILLO), cerrado en lo que pedía, pero abrió F-06.** Ya no hay literales en los `.sh` (`literales=1`).
- **F-04 (AMARILLO), cerrado.** Un `openssl` falso con stderr muestra su diagnóstico antes de `FALLA … salió con 9`. Si falla el primer o el segundo `mv`, sale `rc=1` y no queda ningún archivo. Después, una corrida normal genera el par.
- **F-05 (AMARILLO), cerrado.** La bitácora aclara lo de `40c992e` y `0ae9306`.

## Hallazgos
### F-06 · NARANJA · scripts/imagenes.sh:18-23 y scripts/verificar-imagenes.sh:69-74 · El chequeo de versiones de la decisión 10 ya no puede fallar
La decisión 10 pide que `imagenes.sh` salga con 1 antes de construir si la versión de una etiqueta no es la de `build.gradle.kts` o la de `IMAGEN_INIT_POR_DEFECTO`. Ahora la etiqueta se toma de esos mismos archivos, así que la comparación siempre cuadra. Lo único que queda es exigir que no esté vacía.

Siguen como literales, y ningún script los ata al origen:
- `deploy/overlays/kind/kustomization.yaml:11` (`newTag: 0.1.0`)
- `deploy/overlays/kind/backend-kind.yaml:15` (`…synkra-init-descarga:1.0.0`)
- `scripts/verificar-backend.py:28` y `scripts/verificar-backend.py:120`

En una copia puse `version = "0.2.0"` en el backend e `IMAGEN_INIT_POR_DEFECTO` en `1.1.0` (`scratchpad/r2/deriva.txt`):
- **`a06869b`:** `FALLA versiones: backend/build.gradle.kts dice 0.2.0, la etiqueta es 0.1.0`, rc=1.
- **`8a6b076`:** `versiones aceptadas: VB=0.2.0 … VI=1.1.0`, rc=0, y `verificar-backend.py` da `OK backend: 4 objetos`.
- **Corrida completa con la mutación `extra-version-gradle`:** `imagenes rc=0` y `OK imagenes`.

En ese caso se construye `synkra-backend:0.2.0`, pero el overlay despliega `0.1.0`, y los países recibirían un `SYNKRA_KUBERNETES_IMAGEN_INIT` 1.0.0 que ya no es el de Java. Nada lo detecta.

Arreglo posible: que `imagenes.sh` (o `verificar-imagenes.sh`) compare cada versión del origen con el `newTag` del overlay y con el `SYNKRA_KUBERNETES_IMAGEN_INIT` de `backend-kind.yaml`, leídos de `kubectl kustomize deploy/overlays/kind` o con `grep`. Así queda un solo origen y además el chequeo puede fallar. Hay que añadir esta mutación a CA-8.

### F-07 · AMARILLO · bitacoras/U08-B.md (sección Ronda 2) · Sha de `ca4.txt` distinto al que dice la bitácora
La bitácora dice que la evidencia fresca de CA-1 a CA-8 se hizo sobre `fb00045`, pero la cabecera de `ca4.txt` dice `5e387bb`. Entre los dos commits solo cambian la bitácora y la evidencia (`ca4.sh` ganó la línea `literales`), así que la evidencia sigue siendo válida. Es la misma clase de error que F-05: hay que corregir la frase.

## Tareas candidatas (defectos reales fuera de alcance)
- Siguen en pie las de la ronda 1: el comentario de `core/Dockerfile:2` cita C-98, el `Id` cambia entre corridas con containerd, y `org.sqlite.tmpdir` con `/tmp` `noexec`.

## Estado al terminar
- Worktree limpio: `git status --porcelain` da 0 y HEAD sigue en `8a6b076`.
- No queda ningún contenedor `synkra-u08b*` ni imagen `mutacion`, `rojo` o `verde`. Las tres buenas quedaron reconstruidas por mí: `7055f329…`, `c77c8f2d…`, `28a27122…`.
- Los temporales de `mktemp` se borraron. No hablé con ningún clúster ni toqué `~/.kube/config`.
- No escribí `revisiones/U08-B/ronda-2.md` porque mis herramientas son de solo lectura: este texto es el informe que hay que guardar ahí.

## Rutas de transcripciones largas
Todas en `/tmp/claude-1000/-home-julian-Escritorio-Proyectos-SynKra/76af36c7-fa86-447c-9ae5-76b2a7831e37/scratchpad/r2/`:
- `ca1237.txt`
- `ca4.txt`
- `ca5.txt`
- `ca6.txt`
- `ca8-man.txt`
- `ca8-img.txt` y `ca8-img.sh`
- `deriva.txt` y `deriva.sh`
- `f014.txt` y `f014.sh`

```
VEREDICTO: NO-VERDE
NARANJA|scripts/imagenes.sh:18-23|El chequeo de versiones de la decisión 10 ya no puede fallar: con otra versión en gradle todo da verde y el overlay sigue en 0.1.0
INFORME: revisiones/U08-B/ronda-2.md
```
