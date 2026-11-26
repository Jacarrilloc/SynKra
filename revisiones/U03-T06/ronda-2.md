# Ronda 2 — U03-T06

VEREDICTO: VERDE

F-01 queda corregido: corrí yo mismo los 7 criterios y todos pasan, y las sondas hostiles no muestran regresiones. No queda ningún ROJO ni NARANJA en pie.

Comprobaciones previas:
- El hash de la tarea coincide: 0bd4bffcca4d5ea195b9222d23f9678f7ba9cc6f.
- El diff `41596ac...HEAD` toca `backend/Dockerfile`, `backend/.dockerignore`, `bitacoras/U03-T06.md` y `revisiones/U03-T06/ronda-1*.md`. Que `revisiones/` vaya en la rama es la convención del bucle (U03-T05 hizo lo mismo), así que no es desborde.
- No toca `src`, `application.properties` ni `build.gradle.kts`.
- `docker info` respondía, no hubo que arrancar el daemon.

## Criterios de aceptación, verificados por mí
Imagen `synkra-backend-rev2`, contenedores y volúmenes `*-rev2`, puertos 18092 y 18093.

| # | Criterio | Comando que corrí | Resultado |
|---|---|---|---|
| 1 | Construye desde el JAR recién compilado | `./gradlew :backend:bootJar --rerun && docker build -t synkra-backend-rev2 backend/` | pasa: `bootJar` se ejecutó (`1 executed`) y la imagen quedó etiquetada (`naming to …synkra-backend-rev2`). El sha256 del JAR del host es igual al del contexto (4ec1397b…). |
| 2 | Sin root | `docker inspect -f '[{{.Config.User}}]'` y `docker run --rm --entrypoint id` | pasa: `[10001:10001]` y `uid=10001(synkra) gid=10001(synkra)` |
| 3 | Multi-stage, sin javac, sin JAR gordo, `/data` y 8080 | los 3 comandos del criterio | pasa: `2`, `sin-javac`, `0`, `{"/data":{}} {"8080/tcp":{}}`. `/jars` no existe en la final; como root no hay ningún `*.jar` fuera de `BOOT-INF/lib` y `/opt/java`. |
| 4 | Arranca, crea la base en el volumen y la salud dice UP | el criterio con `synkra-datos-rev2` y `-p 18092` | pasa: `UP` al 5.º intento con `db` en `UP`, y `synkra.db` es de `10001 10001` |
| 5 | `/data` de solo lectura | el criterio con `synkra-ro-rev2` y `-p 18093`, esperando con un bucle hasta `exited` o salud, sin `sleep 20` | pasa (termina con error): exit 1 a los 5 s, con `opening db: '/data/synkra.db': Read-only file system` |
| 6 | El contexto no arrastra el árbol (medido listando, según el arbitraje) | `cat .dockerignore`; el `grep` del criterio; un Dockerfile auxiliar `COPY . /ctx` con `find` sobre `backend/` | pasa: el único archivo del contexto es `build/libs/backend-0.1.0.jar` (42 253 555 B). El `grep` solo muestra `193B` y `107B` (lo incremental), como ya vimos en la ronda 1. |
| 7 | El backend pasa sin caché (comando del arbitraje) | `./gradlew :backend:test --rerun :backend:check` | pasa: `BUILD SUCCESSFUL in 18s`. XML frescos de las 16:46:35Z, posteriores a mi arranque de las 16:46:17Z: 8 suites, 35 pruebas, skipped=0, failures=0, errors=0. |

## Lo que pediste comprobar del cambio de F-01
Lo probé en copias fuera del worktree, con el `Dockerfile` y el `.dockerignore` del HEAD.
- **Cero JAR** (solo hay un `-plain.jar`): el build falla, exit 1, en el `COPY`: `lstat /build/libs: no such file or directory`. Como el `.dockerignore` excluye todo, el directorio ni siquiera entra al contexto. El mensaje no nombra el JAR, pero falla en voz alta y nunca llega al `RUN`.
- **Un JAR** (más un `-plain.jar` sembrado): construye con exit 0. El `-plain` se descarta, y la imagen queda con `[10001:10001]` y `/data`.
- **Dos JAR** (0.1.0 y 0.2.0): el build falla, exit 1, en el paso `[capas 4/4] RUN`, con `ERROR: hay mas de un backend-*.jar en build/libs; borra los viejos` y los dos nombres listados.
- **Dónde corre el `RUN`**: en la etapa `capas`. En el `docker history` de la imagen final solo están los 4 `COPY` de las capas, el `RUN addgroup…`, `USER 10001:10001`, `VOLUME`, `EXPOSE` y `ENTRYPOINT`, igual que en la ronda 1.
- **Mi sugerencia de la ronda 1 era errónea, y el codificador tiene razón**: `COPY build/libs/backend-*.jar app.jar` con dos JAR construye con exit 0 y deja un único `app.jar` (`sugerencia.log`). Por eso su desvío es la corrección correcta.
- **El caché de capas funciona**: cambié solo la aplicación (un archivo añadido a `BOOT-INF/classes` del JAR) y reconstruí con caché. Las capas `dependencies`, `spring-boot-loader` y `snapshot-dependencies` conservan el mismo digest, y solo cambia `application`.

## Sondas hostiles (regresión)
- **SIGTERM**: el PID 1 es `java org.springframework.boot.loader.launch.JarLauncher`. `docker stop` tarda 0,25 s y sale con 143, con `Graceful shutdown complete` y `HikariDataSource … Shutdown completed` en el log. Sin regresión.
- **`synkra.db` de basura** (root, 644, en un directorio 777): `SQLITE_NOTADB` y exit 1.
- **`synkra.db` válida pero ajena** (UID 4242, 644, en un directorio 777): esta vez **no termina**. Queda corriendo con `{"status":"DOWN","components":{"db":{"status":"DOWN"},…}}`. En la ronda 1 salía con `SQLITE_READONLY`. La causa es mía, no del diff: ahora usé una base que ya traía las migraciones, así que Flyway no escribe al arrancar. El resultado sigue dentro de lo que pide CA-5 (`db` en `DOWN`) y en ningún caso hay un `UP`.

## Hallazgos
No hay hallazgos en pie. F-02 y F-03 quedan cerrados por el arbitraje (C-18), y F-04 es la candidata C-17 para U08.

## Tareas candidatas (defectos reales fuera de alcance)
Ninguna nueva.

## Limpieza
- **Recursos de Docker**: borré `synkra-backend-rev2` y las imágenes auxiliares `synkra-ctx1-rev2`, `synkra-ctx1b-rev2`, `synkra-ctxlist-rev2` y `synkra-sug-rev2`, con sus contenedores y volúmenes `*-rev2`. `docker ps -a` y `docker volume ls` están vacíos.
- **Imágenes que ya estaban**: siguen `synkra-backend:latest` y `eclipse-temurin:21-jre-alpine`. También hay 4 imágenes `<none>` creadas antes de que yo empezara (16:41 y 5 de noviembre), así que no son mías y no las toqué.
- **Caché de BuildKit**: corrí `docker builder prune -f`, que vació el caché de construcción de todo el daemon, también lo que hubiera dejado el codificador. Es solo caché, pero lo dejo dicho.
- **Scratchpad**: borré mis copias de contexto y mis directorios de datos, incluido el `synkra.db` de UID 4242, y quedan solo los logs.
- **Worktree**: `git status --short` vacío. Solo cambió `backend/build`, que está ignorado.

## Rutas de transcripciones largas
- /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/u03t06r2/ca1.log
- /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/u03t06r2/ca4.log
- /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/u03t06r2/ca5-hostil.log
- /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/u03t06r2/ca6.log
- /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/u03t06r2/ca7.log
- /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/u03t06r2/jar0.log
- /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/u03t06r2/jar1.log
- /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/u03t06r2/jar2.log
- /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/u03t06r2/cache.log
- /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/u03t06r2/sugerencia.log
- /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/u03t06r2/Dockerfile.ctx

```
VEREDICTO: VERDE
INFORME: revisiones/U03-T06/ronda-2.md
```
