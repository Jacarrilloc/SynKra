# Ronda 4 — U01-T08

VEREDICTO: NO-VERDE

Corrí los seis criterios yo mismo y los seis pasan. F-10 y F-11 quedan corregidos tal como se arbitraron, y todas mis sondas de las rondas 1, 2 y 3 se comportan ahora como deben. Quedan dos hallazgos NARANJA, y ninguno rompe un criterio de aceptación:
- **F-13**: busqué a propósito y encontré tablas que pandoc muestra con 11 y con 9 códigos y que dan verde. Todas usan HTML crudo, que el arbitraje de F-10 no cubrió. Con Markdown GFM puro ya no encontré ninguna.
- **F-14**: el arbitraje de F-12 («la evidencia de cada par va completa») no se cumple del todo, y la respuesta del codificador dice que sí.

Si el responsable decide que el HTML crudo queda fuera del modelo de amenaza y acepta los recortes de la bitácora, no queda nada más que bloquee.

Lo que revisé:
- La tarea tiene hash 7420afc7df640bdd3d549092a58552dbec3b6e01 y coincide. HEAD 2db413b, base b4ec449.
- Usé una copia `git archive HEAD` en `u01t08r4/rev/` del scratchpad.
- El documento de diseño real es idéntico byte a byte al de la ronda 3 (`cmp`), y con él pasan CA-1 y CA-6.

## Criterios de aceptación, verificados por mí
| # | Criterio | Comando que corrí | Resultado |
|---|---|---|---|
| 1 | Pruebas del modelo, sin caché | `./gradlew :verificador:test --rerun --tests '*CodigoMotivoTest' --tests '*VeredictoTest'` | pasa: `BUILD SUCCESSFUL`, rc=0. Los XML son de esta ejecución (17:51:45Z). Tardan 0,06 s porque son pruebas en memoria y leen un solo archivo |
| 2 | 10 códigos, iguales al enum | `grep -ho ... TEST-*CodigoMotivoTest.xml` | pasa: `tests="14" skipped="0" failures="0" errors="0"`. Entre las pruebas están `laTablaTieneDiezFilas`, `elEnumCoincideConLaTabla` y `sinNombresRepetidos` |
| 3 | Dos pares falla-pasa | en la copia: (a), revertir, (b), revertir | pasa. (a): rc=1, 2 fallos, `faltan en el enum ...: [X_INVENTADO]` y `<10> but was: <11>`. Al revertir: rc=0, 14/0/0/0. (b): rc=1, `Sobran en el enum: [PLUGIN_AUSENTE]` y `<9>`. Al revertir: rc=0, 14/0/0/0. En el worktree, `git diff --stat -- aidlc-docs verificador/src/main` sale vacío |
| 4 | Invariantes, `tests` ≥ 10 | `grep -ho ... TEST-*VeredictoTest.xml` | pasa: `tests="14" skipped="0" failures="0" errors="0"` |
| 5 | Sin Spring ni Fabric8 | `./gradlew :verificador:dependencies --configuration runtimeClasspath \| grep -ciE "spring\|fabric8"` | pasa: `0` |
| 6 | Todo el verificador | `./gradlew :verificador:test --rerun :verificador:check` | pasa: `BUILD SUCCESSFUL`. Las 7 suites (14/3/28/2/36/14/16) dan skipped/failures/errors en 0, con marcas de 17:51:54Z a 17:51:57Z. `grep -l '<skipped'` da rc=1 |

## Hallazgos

### F-13 · NARANJA · CodigoMotivoTest.java:80-98 (sólo se miran `|` y las aperturas ``` o ~~~) · Con HTML crudo dan verde tablas que pandoc muestra con 11, con 9 y con 0 códigos
El lector no mira los bloques HTML. Un comentario `<!-- ... -->` alrededor de la tabla leída la oculta en el documento, igual que hacía el bloque de código en `fence9`. Además, una tabla `<table>` no tiene ningún `|`. Lo comprobé con `sonda.sh` (Gradle) y con `pandoc -f gfm -t html`, contando dentro de la sección:
- `comentario9`: `<!--` antes de la tabla real, una línea en blanco y `-->` después, y luego una `<table>` HTML con 9 códigos (sin `PLUGIN_AUSENTE`). **rc=0**. Pandoc: `tablas=1 codigos_en_td=9`.
- `comentario11`: igual, pero la `<table>` tiene los 10 códigos más `X_INVENTADO`. **rc=0**. Pandoc: 11 códigos.
- `htmlTrasTabla`: la tabla real, una línea en blanco y `<table><tr><td><code>X_INVENTADO</code>...</table>`. **rc=0**. Pandoc: `tablas=2 codigos_en_td=11`.
- `comentario0` y `scriptOculta`: la tabla queda dentro de `<!-- -->` o de `<script type="text/plain">`, sin ninguna otra. **rc=0**. Pandoc: 0 tablas. Esto choca con la parte de CA-2 que dice que si la tabla no se encuentra, la prueba falla.

Lo matizo: hace falta escribir HTML crudo a mano en el diseño. Es un mecanismo nuevo, no una instancia del F-10 que se arbitró. Lo marco NARANJA por coherencia con F-10, que fue NARANJA por la misma consecuencia: verde con 9 o con 11 códigos visibles. Si el responsable decide que el HTML crudo queda fuera del modelo de amenaza, puede bajarlo. El arreglo barato es el mismo que se aplicó a las aperturas de bloque de código: hacer fallar, dentro de la sección y fuera de la tabla leída, toda línea que empiece por `<` (inicio de bloque HTML), con una prueba en memoria.

### F-14 · NARANJA · bitacoras/U01-T08.md:186-193 y revisiones/U01-T08/ronda-3-respuesta.md · La evidencia de los pares sigue recortada, y la respuesta dice que está completa
El arbitraje de F-12 pide que la evidencia de cada par vaya completa. La respuesta dice que «cada par falla-pasa ... trae el mensaje completo». La bitácora no lo cumple:
- (a): `laTablaTieneDiezFilas(): filas de la tabla: [DESCRIPTOR_FALTA_CAMPO, ..., PLUGIN_AUSENTE, ...]`.
- (b): `elEnumCoincideConLaTabla(): ... Sobran en el enum` y `[..., PLUGIN_REPETIDO, DESCRIPTOR_INVALIDO]`.
- Ningún par trae la línea `$ comando`.
- Cada revertido dice solo `-> BUILD SUCCESSFUL, failures="0"`, sin el comando y sin la línea `tests=... skipped=... failures=... errors=...`.
- Los criterios 1, 5 y 6 vuelven a ir resumidos (`-> BUILD SUCCESSFUL`, `-> 0`), sin la salida pegada que se había corregido en la ronda 3 (F-09).

El contenido es cierto: lo reproduje y los mensajes coinciden. Lo que bloquea es que no se cumplió una regla arbitrada y que la respuesta dice lo contrario. El arreglo es pegar las líneas completas.

### F-15 · AMARILLO · CodigoMotivoTest.java:29 (`SEPARADOR`) y :67 (`indexOf`) · Casos residuales sin desvío entre el enum y la tabla
- `sepSinGuion`: el separador `| | | |`, sin ningún `-`, coincide con `^\|[\s:|-]+$`. Da rc=0, y pandoc no muestra ninguna tabla porque GFM exige al menos un `-` por celda. Es la misma clase que `sepColumnas` (F-11). Los códigos leídos siguen siendo los 10 del enum.
- `parrafoAntes` y `listaAntes`: una línea de prosa o de lista pegada al encabezado da rc=0, y pandoc 3.1.3 lo muestra todo como párrafo (0 tablas). cmark-gfm sí suele mostrar la tabla, así que aquí los renderizadores no coinciden. No hay desvío de códigos.
- `dosSecciones`: un segundo `## Códigos de motivo` con 11 filas da rc=0, porque el lector toma la primera sección, que es lo que dice CA-2. Pandoc muestra dos secciones con el mismo título, y la segunda trae `X_INVENTADO`. Haría falta poco para que falle si el título aparece dos veces.

## Búsqueda pedida: tablas con 9 u 11 códigos que den verde
- **Con HTML crudo, sí las hay** (F-13): `comentario9`, `comentario11` y `htmlTrasTabla`.
- **Con Markdown GFM puro, no encontré ninguna.** Pandoc da 10 en todas y el lector falla o da verde con 10, salvo en las de F-15, que dan 0 o duplican la sección. Lo que probé:
  - Líneas que Java toma por blancas, seguidas de una fila `` `X_INVENTADO` `` sin `|`: U+3000, U+1680, U+2000, U+200A, U+205F, U+2028, U+2029, U+001C, U+001F, VT, FF, espacios y tabulador. El lector da rc=0 y pandoc corta la tabla en 10, así que coinciden.
  - Con NBSP y U+2007 el lector falla, que es lo conservador.
  - `filaSinPipeTrasVacio` da rc=0 con 10. `filaSinPipeDirecta` falla (residual 1, aceptado).
  - `fenceIndent4` y `indentCode` fallan. `h1Antes` da rc=0 con 10.
  - Razonamiento: toda tabla GFM necesita `|` en el encabezado y en el separador. Fuera de la ventana de lectura, eso ya hace fallar. Dentro de la ventana no puede empezar otra tabla, porque un separador nunca cumple `FILA`.

## Lo que comprobé y está bien
- **Sondas de las rondas 1 y 2 (24 casos)**. Fallan citando la línea: `guion`, `textoExtra`, `dosEnCelda`, `filaVacia`, `pipeEscapado`, `comillaSola`, `filaHtml`, `sinPipeInicial`, `filaIndentada`, `sinPipeMedio`, `h3`. Fallan con «'|' fuera de la tabla leída»: `indentada`, `lineaVaciaYFila` y `segundaTabla`. Fallan con su propio mensaje: `encabezado`, `espacioFinal`, `sinTabla`, `ausente`, `sepRoto` y `sinEncabezado`. `pipeEscapadoCola` detecta `X_INVENTADO`. `sepDosPuntos`, `crlf` y `bom` pasan.
- **Sondas de la ronda 3 (14 casos), todas rc=1 con el mensaje correcto**:
  - `encabezadoSinPipe` y `encabezadoSinPipe2`: «'|' fuera de la tabla leída, línea 21».
  - `segundaSinPipe`: el mismo mensaje, en la línea 34.
  - `fence9`: «no admite bloques de código; apertura en la línea 21».
  - `encabezadoX`: «debe empezar por '| Código |'».
  - `sepColumnas`: «2 columnas y el encabezado 3».
  - `c9vacia`: «Fila ... línea 31».
  - `ideografico`, `tabVertical`, `formFeed`, `us1f`, `espaciosYSinPipe`, `mid3000` y `nbsp` también fallan.
- **Pruebas en memoria nuevas**: `encabezadoSinPipe`, `segundaSinPipe`, `fence9`, `pipeAntesDeLaTablaYEscapado` y `encabezadoSeparadorYCeldaMalFormados`. Comprueban el número de línea y el texto citado. El residual 2 queda anulado, como pidió el arbitraje.
- **Alcance**:
  - `git diff --name-status b4ec449...HEAD` solo trae `A`. En `a87bf94..2db413b` solo cambian `CodigoMotivoTest.java` y la bitácora, y se agrega la respuesta.
  - No cambia ningún archivo existente de `src/main` ni `build.gradle.kts`.
- **Worktree**: limpio al terminar (`git status --short` vacío, HEAD 2db413b). La copia del documento en `rev/` quedó restaurada (`cmp`).

## Tareas candidatas
- Ninguna nueva. C-22 sigue en pie.

## Rutas de transcripciones largas
- /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/u01t08r4/ev/ca1.txt, /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/u01t08r4/ev/ca5.txt y /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/u01t08r4/ev/ca6.txt
- /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/u01t08r4/ev/ca3-pares.txt
- /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/u01t08r4/ev/sondas-r12.txt, /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/u01t08r4/ev/sondas-r3.txt y /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/u01t08r4/ev/sondas-r4.txt
- /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/u01t08r4/ev/pandoc-r4.txt (recuento de pandoc por sonda)
- /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/u01t08r4/ev/p/ (una salida de Gradle por sonda)
- /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/u01t08r4/ev/r-comentario9.html, /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/u01t08r4/ev/r-htmlTrasTabla.html y /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/u01t08r4/ev/r-comentario11.html (HTML de pandoc)
- /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/u01t08r4/pd-comentario9.md y /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/u01t08r4/pd-htmlTrasTabla.md (documentos mutados)
- /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/u01t08r4/bin/mut.py, /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/u01t08r4/bin/sonda.sh y /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/u01t08r4/bin/pd.sh

No escribí el archivo del informe porque soy de solo lectura. Este texto es el contenido de `revisiones/U01-T08/ronda-4.md`.

```
VEREDICTO: NO-VERDE
NARANJA|verificador/src/test/java/io/synkra/verificador/CodigoMotivoTest.java:80-98|Con HTML crudo (comentario que oculta la tabla leída o <table>) dan verde tablas que pandoc muestra con 9, 11 y 0 códigos
NARANJA|bitacoras/U01-T08.md:186-193|La evidencia de los pares sigue recortada (...) sin $ comando ni XML del revertido, y la respuesta dice que está completa
INFORME: revisiones/U01-T08/ronda-4.md
```
