# Ronda 1 — U02-T11

VEREDICTO: VERDE

Reconstruí la imagen yo mismo y corrí los cuatro criterios: todos pasan, sin ningún ROJO ni NARANJA. Quedan cuatro AMARILLO que no bloquean. El rojo con el Dockerfile ya incluido no invalida nada: la tarea no exige un rojo y la evidencia roja se reproduce en la base.

Revisé el diff `3538d67..a430a1f` desde el worktree `/home/julian/Escritorio/Proyectos/wt-m6r-U02-T11` (HEAD `a430a1f`), con la tarea en el hash `f9132858`.

## Criterios de aceptación, verificados por mí
| # | Criterio | Comando que corrí | Resultado |
|---|---|---|---|
| 1 | Imagen construida, no root | `./gradlew :core:bootJar`; `docker build --no-cache -t synkra-core:2.1.0 core/`; `docker inspect -f '{{.Config.User}} {{json .Config.Volumes}} {{json .Config.ExposedPorts}}'`; `docker run --rm --entrypoint id` | pasa: `10001:10001 {"/config":{},"/plugins":{}} {"8080/tcp":{}}`, `uid=10001(synkra) gid=10001(synkra)`, ENTRYPOINT exec `["java","org.springframework.boot.loader.launch.JarLauncher"]`, Cmd `null` (sin argumentos de rutas) |
| 2 | Arranca sin plugins y queda listo | `docker run -d --name core-t11 -p 18084:8080`, luego una espera con `timeout 60` y la condición `until curl ... = 200` (no un `sleep` fijo), después los `curl` y el `grep -c` | pasa: listo a los 3 s, `200` con `{"status":"UP"}`, `/plugins` devuelve `[]`, `grep -c 'Plugins cargados'` da `1`. El log dice «No hay /config/plugins-activos.yaml: el core arranca sin plugins». El PID 1 corre como `synkra`; `/plugins` y `/config` son `synkra:synkra`. Contenedor borrado |
| 3 | El resto del build pasa | `./gradlew :core:test --rerun :core:check` más el conteo de `core/build/test-results/test/TEST-*.xml` | pasa: `BUILD SUCCESSFUL`, XML recién escritos: tests 54, failures 0, errors 0, **skipped 0**, 8,1 s acumulados (no son 0,0 s) |
| 4 | Diff acotado | `git rev-parse --short HEAD; git diff --stat 3538d67...HEAD` | pasa: sha `a430a1f`. Solo cambian `core/Dockerfile`, `core/.dockerignore`, `bitacoras/U02-T11.md` y `bitacoras/U02-T11-evidencia/*`. Cero cambios en `core/src/**` |

Comprobaciones adicionales que hice:
- **Guarda de más de un jar** (en una copia del contexto en `mktemp -d`, ya borrada). Con `core-2.0.0.jar` y `core-2.1.0.jar` juntos, el build falla con `ERROR: hay mas de un core-*.jar en build/libs; borra los viejos` y lista los dos. Con solo `core-2.1.0.jar` y un `*-plain.jar`, el build pasa: `.dockerignore` excluye bien el plain.
- **Base fijada.** El digest `sha256:5d7689f4...ce70` coincide con el RepoDigest local de `eclipse-temurin:21-jre-alpine`. La JRE es `21.0.13.1`.
- **Sin plugins en la imagen.** `/app` solo contiene `BOOT-INF`, `META-INF` y `org`; `/plugins` y `/config` están vacíos.
- **Distroless.** La bitácora declara la sustitución por la base alpine (decisión 1).
- **Usuario creado una sola vez.** `/etc/passwd` tiene `synkra:x:10001:10001::/home/synkra:/sbin/nologin`.

Sobre el rojo: la tarea no exige un rojo primero, y para un Dockerfile solo puede ser trivial. Que el commit `3105b77` ya traiga los archivos significa que ese commit no reproduce su propio rojo. Aun así, `rojo.txt` es coherente con la base (`git show 3538d67:core/Dockerfile` dice que no existe en `3538d67`), así que no es evidencia falsa. Lo anoto como AMARILLO (F-01).

## Hallazgos

### F-01 · AMARILLO · commit 3105b77 · El commit «rojo» no es rojo
Su título dice «sin Dockerfile», pero trae `core/Dockerfile` y `core/.dockerignore` completos, como el codificador admite. La salida roja solo se reproduce en `3538d67`, no en el commit que la acompaña. No bloquea: la tarea no pedía un rojo. La historia de commits engaña a quien la lea; conviene evitarlo en próximas tareas.

### F-02 · AMARILLO · bitacoras/U02-T11-evidencia/ca1.txt, ca2.txt, rojo.txt · La evidencia no trae los comandos completos
- `ca2.txt` muestra `$ docker run -d` truncado y «tras 3s:» sin decir cómo se esperó: no se puede saber si fue un `sleep 3` fijo o una espera con `timeout` y una condición.
- `ca1.txt` resume `docker inspect` como `$ inspect`.

Mi propia corrida confirma los valores, así que no bloquea. Aun así, la evidencia del codificador sin comando es una afirmación sin verificar.

### F-03 · AMARILLO · core/Dockerfile:18-23 · Contratos repetidos como literales sin prueba que los ate
`10001`, `8080`, `/plugins` y `/config` repiten `UID_CONTENEDORES`, `GID_CONTENEDORES`, `PUERTO` y las rutas de `ConstructorObjetosPais`, y ninguna prueba los ata. La tarea fija estos valores y un Dockerfile no puede importarlos de Java, así que no lo subo a NARANJA. Es la misma clase que C-98 en la imagen del init; se debería resolver una sola vez para las tres imágenes, en U08 o en el pulido.

### F-04 · AMARILLO · core/.dockerignore:5-6 · Líneas muertas
`src/` y `.gradle/` sobran después de `**`. Vienen copiadas del patrón del backend y no hacen daño.

## Tareas candidatas (defectos reales fuera de alcance)
- Ampliar C-98 y C-101: una prueba o un chequeo de construcción que ate el UID/GID, el puerto y las rutas de los Dockerfile (core, backend, init) a las constantes de `ConstructorObjetosPais`.

## Estado al terminar
- El worktree está limpio (`git status --short` vacío) y no queda ningún contenedor `core-t11`.
- La imagen temporal de la guarda está borrada.
- La imagen `synkra-core:2.1.0` queda en el demonio local, porque es el artefacto del criterio.

## Rutas de transcripciones largas
- /tmp/claude-1000/-home-julian-Escritorio-Proyectos-SynKra/76af36c7-fa86-447c-9ae5-76b2a7831e37/scratchpad/build.txt
- /tmp/claude-1000/-home-julian-Escritorio-Proyectos-SynKra/76af36c7-fa86-447c-9ae5-76b2a7831e37/scratchpad/ca3.txt

No tengo herramientas de escritura, así que no escribí `revisiones/U02-T11/ronda-1.md`. Hay que guardar este texto en esa ruta.

```
VEREDICTO: VERDE
AMARILLO|commit 3105b77|El commit «rojo» ya incluye el Dockerfile
AMARILLO|bitacoras/U02-T11-evidencia/ca2.txt|Evidencia sin comandos completos ni forma de espera
AMARILLO|core/Dockerfile:18-23|UID, puerto y rutas repetidos sin prueba que los ate (clase C-98)
AMARILLO|core/.dockerignore:5-6|Líneas muertas después de **
INFORME: revisiones/U02-T11/ronda-1.md
```
