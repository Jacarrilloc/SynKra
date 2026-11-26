# Ronda 1 — U03-T06

VEREDICTO: NO-VERDE

Los 7 criterios pasan cuando los corro yo. Lo que impide el VERDE es un solo NARANJA: el nombre del JAR, con su versión, está escrito a mano en el Dockerfile. No hay ningún ROJO. El alcance está limpio: el diff `41596ac...HEAD` toca solo `backend/Dockerfile`, `backend/.dockerignore` y `bitacoras/U03-T06.md`, y no hay nada en `src`, `application.properties` ni `build.gradle.kts`. El hash de la tarea coincide (0bd4bff…).

## Criterios de aceptación, verificados por mí
Daemon: `docker info` respondió sin arrancarlo. Usé la imagen `synkra-backend-rev`, contenedores y volúmenes `*-rev` y los puertos 18090 y 18091.

| # | Criterio | Comando que corrí | Resultado |
|---|---|---|---|
| 1 | Construye desde el JAR recién compilado | `./gradlew :backend:bootJar --rerun && docker build -t synkra-backend-rev backend/` | pasa (`BUILD SUCCESSFUL`, `naming to …synkra-backend-rev`) |
| 2 | Sin root | `docker inspect -f '[{{.Config.User}}]'` y `docker run --rm --entrypoint id` | pasa (`[10001:10001]`, `uid=10001 gid=10001`) |
| 3 | Multi-stage, sin javac, sin JAR gordo, /data y 8080 | los 3 comandos del criterio | pasa (`2`, `sin-javac`, `0`, `{"/data":{}} {"8080/tcp":{}}`). Como root tampoco hay otro `*.jar` fuera de `BOOT-INF/lib` ni de `/opt/java`. |
| 4 | Arranca, crea la base en el volumen y la salud dice UP | criterio con `synkra-datos-rev` y `-p 18090` | pasa (`UP`, `db` en `UP`, `synkra.db` de 10001:10001). Al rearrancar sobre el mismo volumen, Flyway responde `Schema "main" is up to date`. |
| 5 | /data de solo lectura | criterio con `synkra-ro-rev` y `-p 18091`; esperé a `exited` con un bucle de condición, no con `sleep 20` | pasa (variante «termina con error»): exit 1 a los 6 s, `SQLException: opening db: '/data/synkra.db': Permission denied` |
| 6 | El contexto no arrastra el árbol | `cat .dockerignore`; `docker build --no-cache … \| grep 'transferring context'`; `DOCKER_BUILDKIT=0 docker build …` | pasa: el builder clásico da `Sending build context to Docker daemon 42.26MB`, que es exactamente el JAR (42 253 555 B). Ver F-02. |
| 7 | `:backend:check` sin caché | `./gradlew :backend:check --rerun`, y además `./gradlew :backend:test --rerun :backend:check` | pasa. XML frescos (16:38Z): 8 suites, 35 pruebas, skipped=0, failures=0, errors=0. Ver F-03. |

## Puntos que pediste mirar
- **SIGTERM**: el PID 1 es `java …JarLauncher` (forma exec). `docker stop` tarda 0,24 s. En el log aparecen `GracefulShutdown complete` y `HikariDataSource … Shutdown completed`. Sale con 143, que es la salida normal de la JVM ante SIGTERM. No espera los 10 s del kill.
- **`/data` con un `synkra.db` ajeno**: todos los casos fallan cerrados con un error legible.
  - Basura de root con permisos 644: `SQLITE_NOTADB`, exit.
  - Base de UID 4242 con permisos 644 en un directorio 777: `SQLITE_READONLY`, exit.
  - En ningún caso queda un `UP`.
- **`SYNKRA_DATA`**:
  - `=/otra`: `AccessDeniedException: /otra`, exit.
  - `=/tmp`: arranca en `UP` y la base queda en `/tmp/synkra.db`. Es correcto, pero el `VOLUME /data` queda como un volumen anónimo que no se usa. No es hallazgo.
- **`--user 12345` arbitrario**: con el `/data` por defecto o con un volumen nombrado nuevo falla con `Permission denied` (exit legible). Con `--user 12345:0` pasa lo mismo. Solo funciona si `/data` pertenece a ese UID (por ejemplo `--tmpfs /data:uid=12345`). Ver F-04.
- **El JAR que entra**: es el ejecutable. El manifiesto trae `Spring-Boot-Version: 3.5.16` e `Implementation-Version: 0.1.0`, y `BOOT-INF/lib` tiene 45 dependencias. Para comprobar el `.dockerignore`, sembré en una copia fuera del worktree `-plain.jar`, `otro-1.jar`, `build/classes`, `.gradle/`, `synkra.db`, `synkra.db-wal`, `.env`, `sub/backend-9.jar` y `build/libs/sub/backend-x.jar`. Con `COPY . /ctx`, lo único que entra es `build/libs/backend-0.1.0.jar`.

## Hallazgos
### F-01 · NARANJA · backend/Dockerfile:4 · La versión del JAR está escrita a mano y la imagen puede empaquetar un JAR viejo sin avisar
`COPY build/libs/backend-0.1.0.jar app.jar` repite el `version = "0.1.0"` de `build.gradle.kts:7` en el punto que decide qué artefacto entra en la imagen. Gradle no borra los JAR de versiones anteriores de `build/libs`. Lo probé en una copia fuera del worktree, con `backend-0.1.0.jar` y `backend-0.2.0.jar` presentes a la vez: `docker build` termina bien y empaqueta el **0.1.0**. Así, un cambio de versión sin `clean` produce una imagen con el código viejo y ninguna señal. Si solo existe el 0.2.0, el build sí falla (`"/build/libs/backend-0.1.0.jar": not found`).

Arreglo dentro del alcance y sin tocar `build.gradle.kts`: `COPY build/libs/backend-*.jar app.jar`. El `.dockerignore` ya descarta `-plain`, y si hay más de un JAR, Docker falla en voz alta porque el destino no es un directorio. La otra opción es un `ARG` con la versión.

### F-02 · AMARILLO · CA-6 (texto de la especificación) · «unos MB» no describe 42 MB, y el comando del criterio no mide nada
Para mí CA-6 **se cumple**. Lo que el criterio exige es «del orden del JAR, no del árbol completo», y el contexto es el JAR y nada más (42,26 MB con el builder clásico, más la prueba con archivos sembrados).

Lo que falla es la especificación:
- El paréntesis «(unos MB)» subestima el tamaño. Debería decir «≈ el tamaño de `backend-0.1.0.jar` (~42 MB)».
- `grep 'transferring context'` bajo BuildKit muestra solo la transferencia incremental (`193B`, `107B`), aun con `--no-cache`. Así no distingue un `.dockerignore` bueno de uno malo.
- Además, hoy el árbol `backend/` entero pesa unos 42 MB, porque casi todo es el JAR. Ni siquiera la cifra total discrimina. Lo que sí discrimina es listar el contexto.

Esto va para el orquestador (corregir la tarea), no es defecto del codificador. Su bitácora lo declaró con honestidad.

### F-03 · AMARILLO · CA-7 (texto de la especificación) · `check --rerun` no vuelve a correr las pruebas
`--rerun` se aplica solo a `check`, que es una tarea de ciclo de vida. Mi primera corrida dio `4 actionable tasks: 4 up-to-date`, con los XML todavía de las 16:29Z, de antes de que yo corriera. La evidencia fresca salió de `:backend:test --rerun :backend:check`. Hay que corregir el comando en la plantilla de criterios.

### F-04 · AMARILLO · backend/Dockerfile:13-14 · `/data` solo lo puede escribir el UID 10001
La tarea pide un UID fijo, y eso está cumplido. Con un `runAsUser` distinto o con OpenShift, el arranque falla, aunque de forma limpia. Ver la tarea candidata.

## Tareas candidatas (defectos reales fuera de alcance)
- **U08**: decidir `fsGroup: 10001` en el `securityContext`, o en la imagen `chgrp 0 /data && chmod g+rwX /data` (la convención de OpenShift), para que funcione con un UID arbitrario.
- **Plantilla de tareas**: CA-6 debería medir el contexto listándolo, no con «transferring context». CA-7 debería usar `:backend:test --rerun`.

## Limpieza
- Borré `synkra-backend-rev`, los contenedores `*-rev`, `synkra-datos-rev` y los 3 volúmenes anónimos que crearon mis corridas. También borré `busybox:latest`, que bajé para el Dockerfile auxiliar.
- Al terminar, `docker ps -a` y `docker volume ls` están vacíos. Siguen las imágenes que ya había: `synkra-backend:latest` (del codificador) y `eclipse-temurin:21-jre-alpine`.
- El worktree queda limpio: `git status --short` vacío. Solo `backend/build` cambió, por la compilación, y está ignorado por git.
- Sobrescribí logs con los mismos nombres que dejó una revisión anterior en el scratchpad compartido (`ca1.log`, `ca4.log`, `ca5.log`, `ca7.log`). Es posible que el borrado de los `tmp.*` también se llevara directorios temporales previos.

## Rutas de transcripciones largas
- /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/ca1.log
- /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/ca4.log
- /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/ca5.log
- /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/ca6.log
- /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/ca6-legacy.log
- /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/ca7.log
- /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/ca7b.log
- /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/hostil.sh
- /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/Dockerfile.aux

```
VEREDICTO: NO-VERDE
NARANJA|backend/Dockerfile:4|La versión del JAR está escrita a mano y la imagen puede empaquetar un JAR viejo sin avisar
INFORME: revisiones/U03-T06/ronda-1.md
```
