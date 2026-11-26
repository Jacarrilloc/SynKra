# Ronda 3 — U01-T08

VEREDICTO: NO-VERDE

Corrí los seis criterios yo mismo y los seis pasan. F-07, F-08 y F-09 están corregidos en los casos que yo había reportado, y todas las sondas de las rondas 1 y 2 se comportan bien. Aun así, encontré tres formas de que una tabla que pandoc muestra con 11 o con 9 códigos dé verde. Las tres vienen de la misma causa: fuera de la ventana de la tabla, el lector solo cuenta como línea de tabla la que empieza por `|`. El residual 2 que declara el codificador es justamente una de esas tres formas, así que no se puede aceptar. Lo que bloquea es un solo hallazgo, NARANJA. El resto es AMARILLO.

Lo que revisé: la tarea tiene hash 7420afc7df640bdd3d549092a58552dbec3b6e01 y coincide. HEAD d37b112, base b4ec449. Usé una copia `git archive HEAD` en `u01t08r3/rev/`. El documento de diseño es idéntico byte a byte al de la ronda 2.

## Criterios de aceptación, verificados por mí
| # | Criterio | Comando que corrí | Resultado |
|---|---|---|---|
| 1 | Pruebas del modelo, sin caché | `./gradlew :verificador:test --rerun --tests '*CodigoMotivoTest' --tests '*VeredictoTest'` | pasa: `BUILD SUCCESSFUL`, rc=0. Los XML son de esta ejecución (17:39:56Z). Duran 0,05 s porque son pruebas puras en memoria |
| 2 | 10 códigos, iguales al enum | `grep -ho ... TEST-*CodigoMotivoTest.xml` | pasa: `tests="9" skipped="0" failures="0" errors="0"` |
| 3 | Dos pares falla-pasa | copia `u01t08r3/rev/`: mutación (a), revertir, mutación (b), revertir | pasa. (a): rc=1, 2 fallos, `faltan en el enum ...: [X_INVENTADO]`. (b): rc=1, `Sobran en el enum: [PLUGIN_AUSENTE]`. Al revertir cada una: rc=0, 9/0/0/0. En el worktree, `git diff --stat -- aidlc-docs verificador/src/main` sale vacío |
| 4 | Invariantes, `tests` ≥ 10 | `grep -ho ... TEST-*VeredictoTest.xml` | pasa: `tests="14" skipped="0" failures="0" errors="0"` |
| 5 | Sin Spring ni Fabric8 en ejecución | `./gradlew :verificador:dependencies --configuration runtimeClasspath \| grep -ciE "spring\|fabric8"` | pasa: `0` |
| 6 | Todo el verificador, sin caché | `./gradlew :verificador:test --rerun :verificador:check` | pasa: `BUILD SUCCESSFUL`. Las 7 suites (9+3+28+2+36+14+16) dan skipped/failures/errors en 0, con marcas de hora de 17:40:05Z a 17:40:09Z. `grep -l '<skipped'` da rc=1 |

## Hallazgos

### F-10 · NARANJA · CodigoMotivoTest.java:67-78 (`if (terminada)` y `if (posicion == 0)`) · Las líneas de tabla GFM sin `|` inicial, antes o después de la tabla leída, se ignoran: dan verde tablas con 11 y con 9 códigos
Tanto en `posicion == 0` como en `terminada`, una línea que no empieza por `|` se salta en silencio. En GFM, esa línea puede ser el encabezado, el separador o una fila de una tabla. Las tres instancias las comprobé con `sonda.sh` (Gradle) y con `pandoc -f gfm -t html`, contando solo dentro de la sección:

1. **`encabezadoSinPipe`: una sola tabla con 11 códigos da verde.** Cambié el encabezado por `Código | Cuándo | Lo detecta` y el separador por `--- | --- | ---`. Debajo puse `` | `X_INVENTADO` | x | y | `` y `| | | |`, y después las 10 filas reales. El lector toma la fila de `X_INVENTADO` como encabezado y la fila vacía como separador. Resultado: rc=0, 9/0/0/0. Pandoc muestra 1 tabla con 12 filas de datos: 11 `<td><code>` y una vacía. La variante `encabezadoSinPipe2`, con `X_INVENTADO` también sin `|`, da lo mismo: rc=0.
2. **`segundaSinPipe`: el arbitraje de F-08 no se cumple.** Después de una línea en blanco agregué `Otro | A | B`, `--- | --- | ---` y `` `X_INVENTADO` | x | y ``. Resultado: rc=0, 9/0/0/0. Pandoc muestra 2 tablas, y la segunda tiene `<td><code>X_INVENTADO</code>`. Este es el residual 2 del codificador, que en la bitácora dice que eso «es lo que GFM muestra como párrafo». No es cierto: GFM lo muestra como una tabla.
3. **`fence9`: una tabla que se ve con 9 códigos da verde.** Metí la tabla original dentro de un bloque ```` ``` ````. Después de la línea en blanco y del cierre, agregué la misma tabla sin `|` inicial y sin la fila `PLUGIN_AUSENTE`. Resultado: rc=0, 9/0/0/0. El lector cuenta las 10 filas del bloque de código. Pandoc muestra 1 tabla con 9 `<td><code>`, sin `PLUGIN_AUSENTE`.

Sobre los dos residuales:
- **Residual 1** (una fila de datos sin `|` inicial falla aunque GFM la acepte): es conservador. Solo produce rojos de más y no deja pasar ninguna tabla incorrecta. Se acepta.
- **Residual 2** (la prosa después de la línea en blanco se ignora): deja pasar la instancia 2 y es una pieza de la instancia 3. No se acepta.

Lo que se espera: toda línea de la sección `## Códigos de motivo` que quede fuera de la tabla leída y contenga un `|` sin escapar falla citando la línea, esté antes o después de la tabla. Una apertura de bloque de código (```` ``` ```` o `~~~`) dentro de la sección también falla. Hace falta una prueba en memoria para cada una de las tres formas.

### F-11 · AMARILLO · CodigoMotivoTest.java:74-90 · El lector no valida ni el encabezado ni el ancho del separador
- `encabezadoX`: cambié la primera celda del encabezado por `` `X_INVENTADO` `` y da rc=0. Pandoc la muestra como `<th><code>X_INVENTADO</code>`. No es una fila de datos, así que no bloquea, aunque cuesta poco exigir que diga `Código`.
- `sepColumnas`: con un separador `|---|---|` (2 columnas frente a las 3 del encabezado) da rc=0, y pandoc muestra 0 tablas. Los códigos siguen coincidiendo con el enum. Lo que no se detecta es que el documento ya no muestra la tabla.
- `c9vacia`: `` | PLUGIN_AUSENTE` | `` (con una comilla invertida suelta) da rc=0. Es basura menor.

### F-12 · AMARILLO · bitacoras/U01-T08.md, «Ronda 3» · Detalles de la evidencia
F-09 está corregido: el verde, CA-5 y CA-6 traen `$ comando` y la salida pegada. En CA-3 y en (c)-(f), los mensajes están recortados (`message=...`) y el «revertido -> BUILD SUCCESSFUL» aparece una sola vez para los dos pares. Lo reproduje todo, así que no bloquea.

Aparte, el codificador dejó `mut/`, `rojo/`, `orig.md` y `verde.txt` dentro de `u01t08r3/`, que es el scratchpad del revisor. No lo toqué. Usé `u01t08r3/rev/`, `bin/` y `ev/`.

## Lo que comprobé y está bien
- **Sondas de las rondas 1 y 2 (26 casos)** (`ev/sondas-r12.txt`):
  - Las de F-01 (`guion`, `textoExtra`, `dosEnCelda`, `filaVacia`, `pipeEscapado`) fallan citando la línea.
  - Las de F-07 (`sinPipeInicial`, `filaIndentada`, `sinPipeMedio`) fallan citando la línea 33 o la 32. Ya no aparece el mensaje engañoso.
  - F-08 (`segundaTabla`) falla con «más de una tabla, línea 34».
  - Encabezado de sección cambiado o con un espacio al final: «No se encontró la sección». Sin tabla o con la tabla indentada: «no trae una tabla». Sin archivo: «No existe». Sin encabezado o con el separador roto: «separador».
  - CRLF, BOM y separador con `:` pasan. `pipeEscapadoCola` y `comillaSola` detectan `X_INVENTADO`.
  - `lineaVaciaYFila` ahora falla («más de una tabla»). Es un rojo de más y no hace daño.
- **Formas nuevas que no abren hueco** (`ev/sondas-r3.txt`):
  - Una línea con U+3000, VT, FF o U+001F seguida de una fila sin `|` da rc=0, pero pandoc también corta la tabla ahí (10 filas). El `isBlank` de Java coincide con pandoc.
  - Con NBSP falla, que es lo conservador. `###` falla. Una línea en blanco de espacios termina la tabla, igual que en GFM.
  - U+3000 en medio de la tabla falla por «segunda tabla».
- **Alcance**:
  - `git diff --name-status b4ec449...HEAD` solo trae `A`, y en esta ronda solo cambian la prueba y la bitácora.
  - No se toca ningún archivo existente de `src/main` ni `build.gradle.kts`.
- **Worktree**: limpio al terminar (`git status --short` vacío), HEAD d37b112.

## Tareas candidatas
- Ninguna nueva. C-22 sigue en pie.

## Rutas de transcripciones largas
- /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/u01t08r3/ev/ca1.txt
- /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/u01t08r3/ev/ca6.txt
- /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/u01t08r3/ev/ca3-pares.txt
- /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/u01t08r3/ev/sondas-r12.txt
- /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/u01t08r3/ev/sondas-r3.txt
- /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/u01t08r3/ev/p/ (una transcripción por sonda: `encabezadoSinPipe.txt`, `segundaSinPipe.txt`, `fence9.txt`, etc.)
- /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/u01t08r3/ev/r-encabezadoSinPipe.html, /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/u01t08r3/ev/r-segundaSinPipe.html y /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/u01t08r3/ev/r-fence9.html (salida de pandoc). Los documentos mutados están en /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/u01t08r3/pd-encabezadoSinPipe.md, /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/u01t08r3/pd-segundaSinPipe.md y /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/u01t08r3/pd-fence9.md
- /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/u01t08r3/bin/mut.py, /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/u01t08r3/bin/sonda.sh y /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/u01t08r3/bin/pd.sh

No escribí el archivo del informe porque soy de solo lectura. Este texto es el contenido de `revisiones/U01-T08/ronda-3.md`.

```
VEREDICTO: NO-VERDE
NARANJA|verificador/src/test/java/io/synkra/verificador/CodigoMotivoTest.java:67-78|Las líneas de tabla GFM sin | inicial, antes o después de la tabla leída, se ignoran: dan verde tablas con 11 y con 9 códigos
INFORME: revisiones/U01-T08/ronda-3.md
```
