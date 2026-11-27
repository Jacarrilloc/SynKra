# Ronda 1 — U04-T07

VEREDICTO: NO-VERDE

Worktree `/home/casdav/Projects/Master/wt-m6r-U04-T07`, HEAD `c985e46`. Hash de la tarea `03d32aa4befc3df8cddd839d8544a8fdc005e617`: coincide. El worktree sigue limpio después de la revisión. No toqué el `stash@{0}`.

## Criterios de aceptación, verificados por mí
| # | Criterio | Comando que corrí | Resultado |
|---|---|---|---|
| 1 | RutasPluginsTest | `./gradlew :backend:test --rerun --tests '*RutasPluginsTest'` + grep del XML | pasa: `BUILD SUCCESSFUL`, `tests="19" skipped="0" failures="0" errors="0"`, 5,7 s con contexto real. Pero ver F-01 y F-02: «un JSON de 65 KB en POST /api/cores: 413» **no se cumple** para todos los JSON de 65 KB |
| 2 | curl de H-02 en el puerto 18080 | el script del CA, tal cual (puerto libre: `ss` dio 0) | pasa: `201`, dos hashes iguales (`1f7baac1…2730`), `409`, `200`. Lo leí de vuelta: la fila está en `cat_plugin` (python sqlite3) y existe `artefactos/synkra-core/kyc-pe/1.3.0.jar` |
| 3 | todo el backend | `./gradlew :backend:test --rerun :backend:check` + grep de los XML | pasa: 18 XML, todos con `skipped="0" failures="0" errors="0"`, 13 s |
| 4 | diff acotado | `git diff --stat origin/main...m6r/U04-T07` | pasa: `catalogo/**`, `plataforma/{ManejadorErrores,CodigosError,LimiteCuerpoJson,CuerpoJsonDemasiadoGrande}`, `application.properties`, la prueba nueva y la bitácora |

Las afirmaciones del codificador:
- **415 si el cuerpo no es multipart**: verificado. JSON da 415, sin `Content-Type` da 415, y los dos vuelven con `ErrorApi` `TIPO_NO_SOPORTADO`.
- **El bean `ConfiguracionMultipart` ligado a `synkra.max-artefacto`**: verificado con la configuración de producción. Un archivo de 5 MB que no es JAR da 422 `ARTEFACTO_INVALIDO`. Uno de 20 MB + 1 da el 413 de R-C5 («supera el límite de 20 MB»). Uno de 23 MB da el 413 de Tomcat, también con `ErrorApi`. Ninguna prueba lo ata (F-04).
- **El hueco de C-38 en chunked**: verificado, y es más amplio de lo que se declara: un relleno de 10 MB también da 201 (F-02). Además hay un segundo hueco, que no se declara: la ruta cruda (F-01).
- **C-50 en la ruta**: verificado. Un valor de 64 caracteres da 422 `etiquetas.region`; uno de 63 da 201.

## Hallazgos

### F-01 · ROJO · `plataforma/LimiteCuerpoJson.java:44,55` · El filtro de C-38 se esquiva con la ruta cruda
`shouldNotFilter` compara `getRequestURI()`, que es la ruta sin decodificar y sin normalizar, con `"/api/cores"`. Spring sí resuelve `/api/cores;x=1` (parámetro de ruta) y `/api/%63ores` (percent-encoding) hacia `POST /api/cores`, pero el filtro no los reconoce y no actúa. Con eso vuelve exactamente el defecto original de C-38 junto con C-34. Lo corrí contra el bootJar, con una versión de 3 MB:
```
3MB version to /api/cores:          413 resp_len=171
3MB version to /api/cores;x=1:      422 resp_len=3000332
3MB version to /api/%63ores:        422 resp_len=3000332
```
El cuerpo se lee entero y vuelve entero en el 422. Ninguna prueba envía variantes de ruta.

Hay que decidir el alcance contra la ruta ya resuelta, no contra la URI cruda. Otra salida es acotar todo cuerpo JSON sin mirar la ruta, si el orquestador lo admite. Las dos cosas caben en `plataforma/`.

### F-02 · ROJO · `plataforma/LimiteCuerpoJson.java:61,70` · En chunked, un JSON de más de 64 KB con relleno al final da 201
Jackson deja de leer al cerrar el objeto, así que el `FlujoAcotado` nunca llega al byte 64 KB + 1. Después Tomcat drena el resto, de modo que el cuerpo se lee y se acepta. Lo corrí contra el bootJar y lo leí de vuelta con `GET /api/cores/synkra-core`:
```
CL 70000 con espacios al final:        413
chunked 70000 con espacios al final:   201  (se registró la versión 2.2.0)
chunked 10 070 000 bytes:              201  t=1.0 s (se registró la 2.3.0)
```
La decisión 3 pide 413 para cualquier cuerpo JSON de más de 64 KB, y CA-1 pide 413 para «un JSON de 65 KB en POST /api/cores» sin limitarlo a `Content-Length`. Que la bitácora lo declare no hace que se cumpla.

Se puede arreglar dentro de `plataforma/`. Por ejemplo, después de leer el cuerpo (un `RequestBodyAdvice.afterBodyRead`) se piden al flujo acotado como mucho los `limite + 1 - leidos` bytes que faltan: si llega alguno más allá del tope, 413. Así sigue sin leerse el cuerpo entero. Falta además una prueba en chunked con relleno: hoy `unJsonSinContentLengthDe65KbDa413` solo cubre el relleno dentro de un valor.

### F-03 · NARANJA · `catalogo/ConfiguracionMultipart.java:26` · Literal incrustado en el cálculo de `max-request-size`
`Math.max(MARGEN_PETICION.toBytes(), maxCuerpoJson.toBytes() + 64 * 1024)`: el `64 * 1024` es un margen sin nombre que fija el umbral del 413 de Tomcat, y el usuario lo ve. Los otros dos márgenes sí tienen nombre (`MARGEN_ARCHIVO`, `MARGEN_PETICION`). Lo barrí en el resto del diff: es el único literal de este tipo. Con el texto «63» de `MotivosCatalogo.java:47` hay una duplicación menor (F-08).

### F-04 · NARANJA · `RutasPluginsTest` · Ninguna prueba ata los límites de multipart a `synkra.max-artefacto`
La prueba fija `synkra.max-artefacto=16384B`. Con eso, los valores por defecto de Boot (1 MB por archivo, 10 MB por petición) también dejarían pasar los dos casos:
- el JAR de 16 KB + 1 seguiría dando el 413 de R-C5;
- el cuerpo de 1,3 MB seguiría dando el 413 de Tomcat.

Por razonamiento (no pude ejecutarlo, porque no edito), si se borra `ConfiguracionMultipart` las 19 pruebas siguen pasando. La decisión 1 («que el 413 venga de R-C5 y no de Tomcat») solo la comprobé a mano, con la configuración de producción. Hace falta una prueba que distinga los dos 413: un archivo de más de 1 MB y por debajo del límite que no dé 413, o que mire los `detalles` del 413.

### F-05 · AMARILLO · `catalogo/RutasPlugins.java:106` · C-50 cuenta unidades UTF-16, no caracteres
32 emojis son 32 caracteres, y aun así dan 422 «supera los 63 caracteres». Usar `codePointCount`, o dejar escrito que el tope se cuenta en unidades.

### F-06 · AMARILLO · `catalogo/RutasPlugins.java:97` · `etiquetas` con basura al final se acepta
`{"region":"x"} basura` da 201 (`FAIL_ON_TRAILING_TOKENS` está apagado). Es de la misma familia que C-39.

### F-07 · AMARILLO · `plataforma/LimiteCuerpoJson.java` y `CuerpoJsonDemasiadoGrande` · El 413 de un JSON dice «El archivo supera el tamaño permitido»
Reutiliza `MSG_DEMASIADO_GRANDE`, pero lo que se pasó es un cuerpo JSON, no un archivo.

### F-08 · AMARILLO · `catalogo/MotivosCatalogo.java:47` · El 63 está duplicado
El texto del motivo lleva el 63 escrito a mano, aparte de `RutasPlugins.MAX_VALOR_ETIQUETA`. Si cambia la constante, el mensaje miente.

### F-09 · AMARILLO · `RutasPlugins.leerEtiquetas` · La parte `etiquetas` se lee entera antes del tope
El 413 de 64 KB sale bien, pero con `threshold=0` Tomcat ya escribió la parte entera a disco. Puede llegar al máximo por archivo, unos 21 MB en producción. No es el «sin leerlo entero» del cuerpo JSON; basta con dejarlo anotado en la bitácora.

## Tareas candidatas (defectos reales fuera de alcance)
- **C-55 (propuesta)**: no la reproduje, así que queda sin verificar por mí. Parece plausible y fuera de alcance; va con C-35.
- **C-56 (propuesta)**: confirmada. `RutasCores.java:49` todavía dice «Hasta U04-T07 y U04-T08 no hay plugins». Va con U04-T08.
- **Nueva**: C-50 no cubre las etiquetas del descriptor. Un `synkra-plugin.yaml` con `pais: <100 caracteres>` da 201 y la etiqueta se guarda con sus 100 caracteres. La tarea limita C-50 a la ruta y prohíbe tocar `SubidaPlugin`. Si «cada valor de etiqueta de la subida» incluía el descriptor, eso lo decide el orquestador.

## Rutas de transcripciones largas
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/5b13ba94-befc-454f-88e7-3182922701e5/scratchpad/ca1.txt
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/5b13ba94-befc-454f-88e7-3182922701e5/scratchpad/ca3.txt
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/5b13ba94-befc-454f-88e7-3182922701e5/scratchpad/ca2.txt (CA-2 y las sondas de F-02, F-05, F-06 y la candidata nueva)
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/5b13ba94-befc-454f-88e7-3182922701e5/scratchpad/p2.txt (F-01 y los límites de multipart en producción)

Quedan en el scratchpad archivos de prueba grandes: `5m.bin`, `20m1.bin`, `23m.bin`, `v3m.json`, `ws10m.json` y `big.ws`, unos 70 MB en total. No los borré porque el chequeo de seguridad bloqueó el `rm`; los puedes borrar tú.

```
VEREDICTO: NO-VERDE
ROJO|plataforma/LimiteCuerpoJson.java:44,55|El filtro de C-38 se esquiva con la ruta cruda (;x=1, %63ores): 3 MB se leen y vuelven enteros
ROJO|plataforma/LimiteCuerpoJson.java:61,70|En chunked, un JSON de más de 64 KB (hasta 10 MB) con relleno final da 201
NARANJA|catalogo/ConfiguracionMultipart.java:26|Literal 64 * 1024 en el cálculo de max-request-size
NARANJA|RutasPluginsTest|Ninguna prueba ata los límites de multipart a synkra.max-artefacto
INFORME: revisiones/U04-T07/ronda-1.md
```
