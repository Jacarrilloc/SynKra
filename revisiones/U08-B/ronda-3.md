# Ronda 3 — U08-B

VEREDICTO: VERDE

Corrí yo mismo los nueve criterios en la máquina x86_64 de David y pasan todos. F-06 (NARANJA) y F-07 (AMARILLO) de la ronda 2 están cerrados, y siguen cerrados los cinco de la ronda 1. No queda ningún ROJO ni NARANJA. Hay un AMARILLO nuevo, F-08: un archivo de evidencia que cita la respuesta no demuestra lo que se le atribuye, aunque otro archivo sí lo demuestra.

Contexto de la corrida:
- **Hash de la tarea:** `git hash-object` da `73f5a268…`, coincide.
- **Worktree:** `/home/casdav/Projects/Master/wt-m6r-U08-B`, HEAD `60c81f5`, base `8306ee7`, con `KUBECONFIG=/dev/null` y `JAVA_HOME=~/.gradle/jdks/eclipse_adoptium-21-amd64-linux.2`.
- **Clúster:** ninguno. La única orden de `kubectl` fue `kustomize`, así que no hizo falta `kind-synkra`.
- **Imágenes:** reconstruí las tres yo mismo. Las bases por digest ya estaban en caché, y en red solo se bajaron los paquetes `apk` de `shellcheck` y `curl`, que la tarea permite.

## Criterios de aceptación, verificados por mí
| # | Criterio | Comando que corrí | Resultado |
|---|---|---|---|
| 1 | Base del backend | CA-1 literal | pasa: `OK backend: 3 objetos`, `salida=0`, las 3 tuplas, `Recreate deployer True True 10001 10001`, `[('datos','/data'),('tmp','/tmp')]`, `0` |
| 2 | Overlay `kind` | CA-2 literal, más `--versiones` | pasa: `kustomize=0`, `OK backend: 4 objetos`, `salida=0`, `localhost:5001/synkra-backend:0.1.0`, las 6 variables exactas, `nginx` con TLS, las 2 rutas, `0` en los 3 archivos; `OK versiones: backend 0.1.0, init 1.0.0` |
| 3 | Certificado TLS | CA-3 (temporales en el scratchpad), más `-text` y sha antes y después | pasa: solo `tls.crt` y `tls.key`, `600`, `subject=CN=synkra.local`, `DNS:synkra.local`, prime256v1, 365 días, `vigente=0`, `par-ok`, `repetido=1` con sha igual, `sin-dir=1`, sin argumento `1` |
| 4 | Imágenes multi-stage sin root | `bash scripts/imagenes.sh` y el resto de CA-4 | pasa: `imagenes=0`; las 3 con `user=10001:10001 arch=amd64`; servidor `amd64`; 21 líneas `ok`, `OK imagenes`, `verificar=0`; 6 `FROM` con `@sha256:`; 2/2/2; `literales=1`. Build fresco: `Installing shellcheck (0.10.0-r1)` y `[revision 4/4] RUN shellcheck -s sh /descargar.sh` `DONE 0.4s` |
| 5 | El init | CA-5 (con `--pull=never`) | pasa con la desviación aceptada: `uid=10001(synkra) gid=10001(synkra)`; `apk=127` (un `noexiste` también da 127); ni `/sbin/apk` ni `/usr/bin/apk` existen; `/usr/bin/curl` y `/usr/bin/sha256sum`; `-r-xr-xr-x`; `mismo-script`; `test=0` con 27 `ok` |
| 6 | Backend en `docker run` | `bash bitacoras/U08-B-evidencia/ca6.sh` (tmpfs con `exec`), y además el literal con `--tmpfs /tmp` | pasa: `espera=0` a los 16 s, `200` en las 3 rutas, `10001`, `synkra.db*` de `10001:10001`, `('4',)`, `0`. El literal reproduce aquí la desviación: `espera=124` y `UnsatisfiedLinkError … libsqlitejdbc.so: Operation not permitted` |
| 7 | Fallos con mensaje claro | CA-7 (el `PATH` falso va en un subshell) | pasa: `sintaxis=0`, 3 líneas, `FALLA entrada` con `yaml=1` y con `vacio=1`, `java=1` con `/no/existe` y también con el JDK 25 real, `FALLA certificado: openssl req -x509 salió con 7`, `openssl=1`, `0` |
| 8 | Cada regla muerde | `bash bitacoras/U08-B-evidencia/ca8.sh` (11 mutaciones), más 7 derivas mías | pasa: caen las 11. `registro-base` cae por `--base`. `user-0` da `User … visto 0`, `sin-digest` da `FROM`, `uid-java` da `10002:10001 / 10001:10001`, `shellcheck` da SC2086 y `FALLA synkra-init-descarga: docker build …`. La mutación de versiones da `FALLA versiones` y `docker build` 0 veces. Los `Id` buenos son iguales antes y después (`a7893ce7…`, `c137a7fe…`, `03e8ac0c…`) |
| 9 | Diff acotado | `bash bitacoras/U08-B-evidencia/ca9.sh` | pasa: `60c81f5`, `0`, `fuera=1`, `0`, `0`. Ninguna ruta toca Java, `descargar.sh`, `test.sh`, `core/Dockerfile`, `aidlc-docs`, `CANDIDATAS.md` ni `loop/` |

**Diferencias de máquina** (no son defectos):
- CA-4 espera `arm64`. Aquí todo da `amd64`, imágenes y servidor por igual, que es lo que pide la decisión 8 (misma arquitectura que el servidor Docker).
- El `JAVA_HOME` de CA-4 apunta al JDK aarch64; aquí usé el amd64.
- Los `Id` de las imágenes difieren de los de Julian, como ya se anotó con containerd.

**Edad de la evidencia:** entre `63c85b9` y `60c81f5` solo cambian la evidencia, la bitácora y `ronda-2-respuesta.md`. Por tanto la evidencia de las cabeceras `63c85b9` sí sale del código final. `ca9.txt` dice `a2b9abd`, que es el padre de `60c81f5`.

## Cierre de los hallazgos de las rondas 1 y 2
- **F-01 a F-05 (ronda 1): se sostienen.** Los `docker run` de `verificar-imagenes.sh` llevan `--pull=never`. Por F-02, `grep -iE '^[[:space:]]*FROM'`. F-03 sigue sin literales de versión (`literales=1`). Por F-04 vuelve el diagnóstico de openssl, sin huérfanos. F-05, la bitácora, está corregido.
- **F-06 (NARANJA, ronda 2): cerrado.**
  - `verificar-backend.py` lee `build.gradle.kts` e `IMAGEN_INIT_POR_DEFECTO` y gana `--versiones`.
  - `imagenes.sh` lo llama antes de compilar y de construir. `verificar-imagenes.sh` también lo llama.
  - Mis derivas aisladas, una por copia (`deriva-r3.txt`):
    - `solo-gradle` (0.2.0), `solo-newTag` (0.3.0), `solo-init-overlay` (1.0.1), `solo-init-java` (1.2.0) y `sin-var-init` dan las cinco `imagenes.sh=1` con `docker-build=0`, `versiones=1`, `backend=1` y `verificar-imagenes=1`.
    - `registro-overlay` pasa `--versiones`, porque no es versión, pero cae en el `verificar-backend.py` completo (`FALLA imagen`).
    - `solo-core` no se ata a nada, porque ningún manifiesto usa el core. Es lo esperado y está en el barrido de la bitácora.
- **F-07 (AMARILLO, ronda 2): cerrado.** La bitácora corrige los sha de `ca4.txt` y `ca9.txt`, y todas las cabeceras de esta ronda dicen `63c85b9`.

## Hallazgos
### F-08 · AMARILLO · bitacoras/U08-B-evidencia/ronda3-verde.txt:5-7 · El verde de `imagenes.sh` cae por `JAVA_HOME`, no por versiones
La cabecera dice `JAVA_HOME=` (vacío), y la parte de `imagenes.sh` muestra `FALLA entorno: JAVA_HOME (sin definir) no es Java 21` con `imagenes.sh salida=1`. Ese archivo no prueba `FALLA versiones` en `imagenes.sh`, aunque `ronda-2-respuesta.md` y la bitácora (sección Ronda 3, «Verde») lo citan para eso. Las partes de `verificar-imagenes.sh` y `verificar-backend.py` del mismo archivo sí lo prueban.

La afirmación sí la sostiene `ca8.txt:196-200`, con el mismo sha y `JAVA_HOME` puesto: `FALLA versiones: newTag del overlay '0.1.0' …` y `docker build` 0 veces. Mi corrida la confirma. No bloquea. Basta con quitar `ronda3-verde.txt` de la cita de `imagenes.sh`, o regenerarlo con `JAVA_HOME`.

## Tareas candidatas (defectos reales fuera de alcance)
- Siguen en pie las de las rondas 1 y 2:
  - el comentario de `core/Dockerfile:2` cita C-98;
  - el `Id` de las imágenes cambia entre corridas con containerd;
  - `sqlite-jdbc` carga su biblioteca desde `/tmp` y falla con un `/tmp` `noexec` (`org.sqlite.tmpdir`). Hoy lo reconfirmé en amd64.

## Estado al terminar
- **Worktree:** `git status --short` vacío y HEAD en `60c81f5`. Gradle sí escribió `build/libs/*.jar`, que están ignorados.
- **Contenedores:** no queda ninguno `synkra-u08b*`.
- **Imágenes:** no queda ninguna `mutacion.local`, `revisor.local`, `rojo` ni `verde`. Quedan las tres `localhost:5001/…`, construidas por mí: `a7893ce735ca`, `c137a7fec8a5` y `03e8ac0caab5`. Las dejo a propósito para U08-C, como pide la decisión 14.
- **Lo que no toqué:** ningún `push`, ningún clúster, ni `~/.kube/config`, `gitops2` o `m08-gitops1`. Los temporales del scratchpad están borrados.
- **El informe:** no lo escribí en el repositorio, porque soy de solo lectura. Este texto hay que guardarlo en `revisiones/U08-B/ronda-3.md`.

## Rutas de transcripciones largas
En `/tmp/claude-1000/-home-casdav-Projects-Master-SynKra/9d987332-269f-4da9-9d66-0a5449231723/scratchpad/U08-B-r3/`:
- `ca12.txt`
- `ca37.sh` y `ca37.txt`
- `ca4.txt`
- `ca5.sh` y `ca5.txt`
- `ca6.txt`
- `ca6-literal.sh` y `ca6-literal.txt`
- `ca8.txt`
- `ca9.txt`
- `deriva-r3.sh` y `deriva-r3.txt`

```
VEREDICTO: VERDE
AMARILLO|bitacoras/U08-B-evidencia/ronda3-verde.txt:5-7|El verde de imagenes.sh en ese archivo cae por JAVA_HOME vacío, no por FALLA versiones (lo prueba ca8.txt:196-200)
INFORME: revisiones/U08-B/ronda-3.md
```
