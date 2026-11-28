# Ronda 1 — U08-B

VEREDICTO: NO-VERDE

Corrí yo mismo los nueve criterios. Todos pasan, y también las tres desviaciones declaradas. Queda en pie un NARANJA: `verificar-imagenes.sh` sale a la red cuando falta la imagen del init, y la decisión 11 dice «sin red». No hay ningún ROJO.

## Criterios de aceptación, verificados por mí
Todo se corrió desde el worktree `/home/julian/Escritorio/Proyectos/wt-m6r-U08-B`, HEAD `a06869b`, con `KUBECONFIG=/dev/null`. La única orden de `kubectl` fue `kustomize`.

| # | Criterio | Comando que corrí | Resultado |
|---|---|---|---|
| 1 | Base: PVC, Deployment `Recreate`, Service | Los 3 comandos literales de CA-1 | **pasa**: `OK backend: 3 objetos`, `salida=0`, las 3 tuplas, `Recreate deployer True True 10001 10001`, `[('datos','/data'),('tmp','/tmp')]`, `0` |
| 2 | Overlay `kind` con imagen, variables e Ingress TLS | Los 4 comandos literales de CA-2 | **pasa**: `kustomize=0`, `OK backend: 4 objetos`, la imagen, las 6 variables exactas, `nginx` con TLS, las 2 rutas, `0` en los 3 archivos |
| 3 | Certificado autofirmado | CA-3 literal, más `-text` y el sha antes y después | **pasa**: solo `tls.crt` y `tls.key`, `600`, `CN` y SAN correctos, EC P-256 (prime256v1), 365 días, `vigente=0`, `par-ok`, `repetido=1` con el sha idéntico, `sin-dir=1`; sin argumento también da 1 |
| 4 | Las tres imágenes: sin root, `arm64`, multi-stage | `bash scripts/imagenes.sh` (las reconstruí yo) y el resto de CA-4 | **pasa**: `imagenes=0`, las 3 con `user=10001:10001 arch=arm64`, servidor `arm64`, 21 líneas `ok`, `OK imagenes`, `verificar=0`, 6 `FROM` con digest, 2/2/2. Un build mío con `--no-cache --target revision` corre `RUN shellcheck -s sh /descargar.sh` y termina en `DONE 0.2s` |
| 5 | El init corre como 10001 y sin `apk` | CA-5 literal, más `command -v` de un comando inexistente | **pasa** (con la desviación): `uid=10001(synkra)…`, `apk=127`, `/usr/bin/curl` y `/usr/bin/sha256sum`, `-r-xr-xr-x`, `mismo-script`, `test=0` con los 9 casos `ok` |
| 6 | El backend arranca sin root, raíz de solo lectura, y sus sondas responden | Mi propio `ca6-mio.sh`, en las dos variantes | **pasa** (con la desviación): con el `--tmpfs /tmp` literal, `espera=124` y `UnsatisfiedLinkError … libsqlitejdbc.so: Operation not permitted`. Con `exec`: `espera=0` a los 14 s, `200` en las 3 rutas, `10001`, `synkra.db` (y `-shm`, `-wal`) de `10001:10001`, `('4',)`, `0`. `touch /app/x` da `Read-only file system` |
| 7 | Los scripts fallan con mensaje claro | CA-7 literal (el `PATH` falso solo en el subproceso) | **pasa**: `sintaxis=0`, las 3 líneas, `yaml=1`, `vacio=1`, `java=1`, `FALLA certificado: openssl req -x509 salió con 7`, `openssl=1`, `0` |
| 8 | Cada regla nueva muerde (rojo primero) | Mis scripts `ca8-man.sh` y `ca8-img.sh`, una copia con `git archive` por mutación | **pasa**: caen las 10 mutaciones. Las 6 de manifiestos salen con 1 (`registro-base` cae por `--base`; la salida del overlay es idéntica, así que ningún chequeo del overlay puede verla). `user-0` da `User … visto 0`, `sin-digest` da `FROM`, `uid-java` da `User: esperado 10002:10001 / visto 10001:10001`, y `shellcheck` da SC2086 y `FALLA synkra-init-descarga: docker build … salió con 1`. Los `Id` de las imágenes buenas no cambian. Se borraron las 3 `mutacion.local`. El rojo contra `8306ee7` lo reproduje: `FROM` del backend y del init, y `FALLA entrada` |
| 9 | Diff acotado | CA-9 literal, más una intersección con `m6r/U08-A` | **pasa**: `fuera=1`, `0`, `0`. Ninguna ruta compartida con U08-A (c3fcff0) |

Rojo primero: el commit `345adc4` trae solo los dos verificadores (225 inserciones), como pide la tarea.

## Juicio de las tres desviaciones
- **CA-4, seis `FROM` y no cinco: aceptada.** La tarea se contradice: pide «2, 2 y 2», que suman seis. El codificador cumple lo sustantivo.
- **CA-5, `apk=127` y no `1`: aceptada.** En el `ash` de busybox, `command -v` de algo inexistente devuelve 127; con `nosuchcmd` también me dio 127. Ni `/sbin/apk` ni `/usr/bin/apk` existen. Añadir `command -v sha256sum` aparte está bien hecho, porque `command -v a b` solo imprime el primero.
- **CA-6, `tmpfs` con `exec`: aceptada.** Reproduje el fallo del literal: `sqlite-jdbc` carga su `.so` desde `/tmp`, y Docker monta ese tmpfs `noexec`. Un `emptyDir` no es `noexec`, así que la variante con `exec` reproduce lo que hace el Deployment, que no cambia. La candidata está bien anotada.

## Hallazgos
### F-01 · NARANJA · scripts/verificar-imagenes.sh:684 · Sale a la red cuando falta la imagen del init
La decisión 11 pide «sin red». La comprobación `sin-apk` hace `docker run … "$INIT"` fuera del bucle que primero mira si la imagen existe, y sin `--pull=never`. Si la imagen no está en local, Docker intenta descargarla de `$REG`. Lo demostré con `SYNKRA_REGISTRO_IMAGENES=rojo-revisor.local:1`, que dio `failed to do request: Head "https://rojo-revisor.local:1/v2/synkra-init-descarga/manifests/1.0.0"`.

Con el prefijo por defecto (`localhost:5001`, el registro real de U08-A), cuando U08-C ya haya subido las imágenes, ese `run` puede traer la copia del registro al Docker local. Esa ejecución sale con 1 igual, por el chequeo `existe`, pero la siguiente aprobaría sobre la imagen traída y no sobre la construida en local. La corrección es `--pull=never` en los dos `docker run` (líneas 672 y 684), o poner `sin-apk` detrás del mismo chequeo de existencia.

### F-02 · AMARILLO · scripts/verificar-imagenes.sh:660 · El chequeo de digest solo ve `FROM` en mayúsculas
`grep -E '^FROM '` no ve `from …` ni un `FROM` con sangría, y Docker los acepta. Mi mutación `extra-from-minusculas` (`from alpine:3.20 AS revision`, sin digest) construyó la imagen y `verificar-imagenes.sh` dio `OK imagenes`, `rc=0`. Se arregla con `grep -iE '^[[:space:]]*FROM[[:space:]]'`.

### F-03 · AMARILLO · scripts/imagenes.sh:435-437 y scripts/verificar-imagenes.sh:651-653 · Versiones repetidas como literales
`0.1.0`, `2.1.0` y `1.0.0` aparecen en los dos scripts. Subir una versión obliga a editar ambos, y si se olvida uno falla sin ruido. Lo corrige un solo origen, por ejemplo leer las versiones del mismo `grep` en los dos.

### F-04 · AMARILLO · scripts/certificado-tls.sh:404 · Se descarta el diagnóstico de openssl
Por `2>/dev/null`, si `openssl` falla de verdad el usuario solo ve el código de salida, no la causa. Además, si falla el segundo `mv` (línea 408), queda `tls.key` sin `tls.crt`, y el script ya no se puede repetir sin borrar a mano. Es un caso de borde.

### F-05 · AMARILLO · bitacoras/U08-B.md:45 · La bitácora dice un sha distinto para ca8
Afirma «corridos sobre 0ae9306», pero la cabecera de `ca8.txt` dice `40c992e`. Comprobé que entre los dos commits solo cambia `ca8.txt` (`git diff 40c992e 0ae9306 --name-only`), así que la evidencia sí pudo salir del código final. Hay que corregir la frase.

## Tareas candidatas (defectos reales fuera de alcance)
- `core/Dockerfile:2`: el comentario cita C-98 para el digest; el que corresponde es C-101 o C-103.
- Con el almacén containerd de Docker, el `Id` de una imagen cambia en cada reconstrucción aunque todo salga de caché: el mío cambió de `a33f5d46` a `cddedaad` con el mismo `Created`. El invariante «el `Id` antes y después de CA-8 es el mismo» vale dentro de una corrida, pero no sirve para comparar entre corridas. Que U08-C no lo use así.
- La candidata del codificador (`org.sqlite.tmpdir` o un `/tmp` `noexec`) está confirmada por mi reproducción.

## Estado al terminar
- Worktree limpio: `git status --porcelain` da 0 y HEAD sigue en `a06869b`.
- No quedan contenedores `synkra-u08b*` ni imágenes `mutacion`, `rojo`, `verde` o `revisor`. Siguen en el Docker local las tres buenas, reconstruidas por mí: `cddedaad…`, `da4cabcf…` y `137cb5f1…`.
- No hablé con ningún clúster ni toqué `~/.kube/config`.

## Rutas de transcripciones largas
- /tmp/claude-1000/-home-julian-Escritorio-Proyectos-SynKra/76af36c7-fa86-447c-9ae5-76b2a7831e37/scratchpad/ca12.txt
- /tmp/claude-1000/-home-julian-Escritorio-Proyectos-SynKra/76af36c7-fa86-447c-9ae5-76b2a7831e37/scratchpad/ca37.txt
- /tmp/claude-1000/-home-julian-Escritorio-Proyectos-SynKra/76af36c7-fa86-447c-9ae5-76b2a7831e37/scratchpad/ca4.txt
- /tmp/claude-1000/-home-julian-Escritorio-Proyectos-SynKra/76af36c7-fa86-447c-9ae5-76b2a7831e37/scratchpad/ca4-revision.txt
- /tmp/claude-1000/-home-julian-Escritorio-Proyectos-SynKra/76af36c7-fa86-447c-9ae5-76b2a7831e37/scratchpad/ca5.txt
- /tmp/claude-1000/-home-julian-Escritorio-Proyectos-SynKra/76af36c7-fa86-447c-9ae5-76b2a7831e37/scratchpad/ca6-literal.txt
- /tmp/claude-1000/-home-julian-Escritorio-Proyectos-SynKra/76af36c7-fa86-447c-9ae5-76b2a7831e37/scratchpad/ca6-exec.txt
- /tmp/claude-1000/-home-julian-Escritorio-Proyectos-SynKra/76af36c7-fa86-447c-9ae5-76b2a7831e37/scratchpad/ca8-man.txt
- /tmp/claude-1000/-home-julian-Escritorio-Proyectos-SynKra/76af36c7-fa86-447c-9ae5-76b2a7831e37/scratchpad/ca8-img.txt

Mis herramientas son de solo lectura, así que no escribí el informe en el repositorio. Este texto es el informe completo: hay que guardarlo en `revisiones/U08-B/ronda-1.md`.

```
VEREDICTO: NO-VERDE
NARANJA|scripts/verificar-imagenes.sh:684|sin-apk hace docker run sin --pull=never: sale a la red si falta la imagen (decisión 11 «sin red»)
INFORME: revisiones/U08-B/ronda-1.md
```
