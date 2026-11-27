# Ronda 2 — U04-T07

VEREDICTO: VERDE

Worktree `/home/casdav/Projects/Master/wt-m6r-U04-T07`, HEAD `3256dc9`, el que se reportó. Hash de la tarea `03d32aa4befc3df8cddd839d8544a8fdc005e617`: coincide. Al terminar, el worktree sigue limpio (`git status --short` sale vacío). No toqué `stash@{0}`. No quedan servidores escuchando en los puertos 18080 a 18083.

## Criterios de aceptación, verificados por mí
| # | Criterio | Comando que corrí | Resultado |
|---|---|---|---|
| 1 | RutasPluginsTest | `./gradlew :backend:test --rerun --tests '*RutasPluginsTest'` y el grep del XML | pasa: `BUILD SUCCESSFUL`, `tests="27" skipped="0" failures="0" errors="0"`. Tarda 5,8 s con el contexto real, y la primera prueba 0,71 s (arranque de Tomcat), así que no se saltó nada. Las 27 pruebas cubren los nueve puntos del criterio. |
| 2 | curl de H-02 en el puerto 18080 | el script del CA tal cual, con el bootJar recién construido (contiene `LimiteCuerpoJsonAvance.class`; el puerto 18080 estaba libre) | pasa: `201`, dos hashes iguales (`b6a7e8aa…0dc8`), `409`, `200`. Leí el estado de vuelta: el GET devuelve el DTO completo con `etiquetas:{pais:pe}` y existe `artefactos/synkra-core/kyc-pe/1.3.0.jar` |
| 3 | todo el backend | `./gradlew :backend:test --rerun :backend:check` y el grep de cada XML | pasa: `BUILD SUCCESSFUL` en 13 s. Hay 18 XML y todos dan `skipped="0" failures="0" errors="0"` |
| 4 | diff acotado | `git diff --stat origin/main...HEAD` | pasa: 13 archivos. Son `catalogo/**`, `plataforma/{ManejadorErrores,CodigosError,LimiteCuerpoJson,LimiteCuerpoJsonAvance,CuerpoJsonDemasiadoGrande}` (la configuración de plataforma para C-38), `application.properties`, `RutasPluginsTest`, la bitácora y `revisiones/U04-T07/` |

## Sondas contra el bootJar (rama, configuración de producción)

**F-01, cerrado.** Mandé una versión de 3 MB a 8 variantes de ruta, cada una con `Content-Length` y en chunked:
- Con `Content-Length` dan 413 `ErrorApi` en las 8: `/api/cores`, `;x=1`, `%63ores`, `/api/cores/`, `//api/cores`, `/api/./cores`, `/api/x/../cores`, `/API/cores`. La respuesta mide 186 bytes, así que el cuerpo ya no vuelve.
- En chunked, las que llegan a la ruta dan 413: `/api/cores`, `;x=1` y `%63ores`. Las demás dan un 404 sin leer el cuerpo.

**F-02, cerrado.**
- En chunked dan 413: 70 000 bytes con espacios al final, 10 070 000 bytes (en 1,0 s), 70 000 de basura detrás del objeto, y relleno con `;x=1`.
- Los bordes salen bien: chunked de 65 536 exactos da 201, de 65 537 da 413, y con `Content-Length` de 65 536 da 201.
- Lo leí de vuelta con `GET /api/cores/synkra-core`. Solo están 2.1.0, 2.6.0, 2.8.0 y 2.9.0; ninguna de las rechazadas (2.2.0 a 2.5.0 y 2.7.0) se registró.

**Variantes de `Content-Type` (3 MB en chunked).**
- Dan 413: `APPLICATION/JSON`, `;charset=UTF-8`, `;charset=utf-16`, `application/vnd.x+json`, `application/json;;` y uno con espacio delante.
- Dan 415 sin leer el cuerpo: `text/json`, `x-ndjson`, `charset="` y una lista con dos tipos.
- `*/*` y `application/*` dan 500. Ya pasa igual en origin/main, así que es una candidata (abajo).

Hay un detalle que no se puede alcanzar hoy: el filtro y el conversor de Spring leen el mismo `getContentType()` con el mismo `MediaType.parseMediaType`, y en el bootJar no hay `jackson-dataformat-yaml`, `cbor` ni `smile`.

**Acotar todo JSON no rompe otras rutas ni actuator.**
- Sin cuerpo, todo responde igual que antes, también con `Content-Type: application/json`: `GET /actuator/health` 200, `/actuator` 200, `GET /api/cores` 200, `/api/cores/synkra-core` 200, `/…/plugins` 200 `[]`, `/api/cores/nope` 404, `/no-existe` 404.
- El POST de JSON también sigue igual: `POST /no-existe` 404, `POST /actuator/health` 405, JSON roto 400, cuerpo vacío 400 (con `Content-Length` y en chunked), `text/plain` 415, sin `Content-Type` 415, 409 con la versión repetida y 201 en chunked pequeño.
- El único cambio de comportamiento: `GET /actuator/health` con un cuerpo JSON de 70 KB ahora da 413, porque actuator lee el cuerpo como `Map`. Lo veo correcto: las sondas de Kubernetes no mandan cuerpo.

**Comparación con origin/main** (bootJar construido desde `git archive` en el scratchpad): `health` con `{}` da 200 y con `{"a":1}` da 422, y `*/*` y `application/*` dan 500. Es igual en main y en la rama.

**Multipart en producción.**
- 5 MB que no son JAR: 422 `ARTEFACTO_INVALIDO`.
- 20 MB + 1: el 413 de R-C5, «supera el límite de 20 MB».
- 23 MB: el 413 de Tomcat, con `ErrorApi`.
- Una parte `etiquetas` de 70 KB: 413, «El cuerpo de la solicitud…», `campo=etiquetas`.
- Después de esas subidas no queda nada en `artefactos/`.

**Resto de hallazgos de la ronda 1.**
- F-03 y F-08: los revisé en el diff y están corregidos.
- F-05, F-06 y F-07: los cubren pruebas que corrí y pasan.
- F-04: lo ata `losLimitesDeMultipartSalenDeMaxArtefacto`. Con el bean de Boot (1 MB) esa prueba fallaría, porque Boot cede su `MultipartConfigElement` con `@ConditionalOnMissingBean`.

## Hallazgos
### F-10 · AMARILLO · `RutasPluginsTest` · `unArchivoPorEncimaDelArtefactoYDebajoDeTomcatDa413DeRC5` no distingue el límite propio del de Boot
Usa 500 000 bytes, que también caben bajo el valor por defecto de Boot (1 MB). La atadura real la hace la prueba que lee el bean de vuelta. Con un archivo de unos 1 050 000 bytes (entre 1 MB y 1 MB + 16 KB) la prueba de comportamiento también lo demostraría. No bloquea.

### F-11 · AMARILLO · `application.properties:17` · El comentario de `synkra.max-cuerpo-json` quedó desfasado
Dice «tope del cuerpo JSON de las rutas de /api/cores», pero desde la ronda 2 el tope aplica a todo cuerpo JSON, actuator incluido.

### F-12 · AMARILLO · `plataforma/LimiteCuerpoJsonAvance.java:33` · El drenaje no tiene tope propio
`afterBodyRead` lee hasta `-1` y confía en que el flujo sea el `FlujoAcotado` del filtro. Hoy no se puede alcanzar sin tope, porque el filtro y Jackson reconocen los mismos tipos y no hay conversores YAML, CBOR ni Smile. Si mañana se añade uno, se drenaría sin límite. Bastaría un tope local de `limite + 1` bytes o comprobar `instanceof FlujoAcotado`.

## Tareas candidatas (defectos reales fuera de alcance)
- `Content-Type: */*` o `application/*` en `POST /api/cores` da 500 `ERROR_INTERNO`: Spring lanza `IllegalArgumentException` en `ServletServerHttpRequest.getHeaders`. Pasa igual en origin/main. Debería dar 415. Encaja con la familia de C-43.
- `GET /actuator/health` con el cuerpo JSON `{"a":1}` da 422: actuator lee `Map<String,String>` y la coerción está apagada. Pasa igual en origin/main. Es menor.
- Se mantienen C-55, C-56 y C-57 (etiquetas del descriptor de más de 63 caracteres), como quedó en el arbitraje.

## Rutas de transcripciones largas
Todo está en el subdirectorio `/tmp/claude-1000/-home-casdav-Projects-Master-SynKra/5b13ba94-befc-454f-88e7-3182922701e5/scratchpad/r2/`:
- `ca1.txt` y `ca3.txt`: salida de Gradle.
- `ca2.txt`: CA-2 y la lectura de vuelta.
- `sondas.txt`: F-01, F-02, las variantes de `Content-Type` y las otras rutas y actuator. `boot2.log` tiene la traza del 500.
- `cmp-main.txt` y `cmp-rama.txt`: la comparación con origin/main.
- `scripts/`: `sondas.py` y `cmp.sh`.
- Archivos grandes, unos 70 MB en total, que puedes borrar:
  - `grandes/`: `5m.bin`, `20m1.bin`, `23m.bin` y `etq70k.json`.
  - `main-src/`: el árbol de origin/main con su bootJar de 45 MB.
  - Los directorios `tmp.*`: los `SYNKRA_DATA` de cada arranque.

```
VEREDICTO: VERDE
AMARILLO|RutasPluginsTest|La prueba de 500 KB no distingue el límite propio del de Boot (lo ata la lectura del bean)
AMARILLO|application.properties:17|El comentario de synkra.max-cuerpo-json dice "rutas de /api/cores"; ahora aplica a todo JSON
AMARILLO|plataforma/LimiteCuerpoJsonAvance.java:33|El drenaje de afterBodyRead no tiene tope propio (hoy no se puede alcanzar)
INFORME: revisiones/U04-T07/ronda-2.md
```
