# Ronda 2 — U01-T08

VEREDICTO: NO-VERDE

Corrí los seis criterios yo mismo y los seis pasan. F-01 a F-06 de la ronda 1 están corregidos y repetí todas mis sondas. Aun así encontré otra forma de que una tabla con 11 filas dé verde, de la misma clase que F-01, y eso bloquea. Se me pasó en la ronda 1: allí escribí que cortar en la primera línea que no empieza por `|` estaba bien, y no lo está.

Lo que revisé: tarea `tareas/U01-T08-modelo-veredicto.md`, hash 7420afc7df640bdd3d549092a58552dbec3b6e01, coincide con el tuyo. Worktree `/home/user/wt-m6r-U01-T08`, HEAD bb9fc8a, base b4ec449.

## Criterios de aceptación, verificados por mí
| # | Criterio | Comando que corrí | Resultado |
|---|---|---|---|
| 1 | Pruebas del modelo sin caché | `./gradlew :verificador:test --rerun --tests '*CodigoMotivoTest' --tests '*VeredictoTest'` | pasa, `BUILD SUCCESSFUL`. XML de esta ejecución (17:32:13Z). Son pruebas puras en memoria, de ahí los 0,06 s |
| 2 | 10 códigos, igual al enum | `grep -ho ... TEST-*CodigoMotivoTest.xml` | pasa, `tests="7" skipped="0" failures="0" errors="0"` |
| 3 | Dos pares falla-pasa | Copia `git archive HEAD` en `u01t08r2/rev/` | pasa. (a) falla con `faltan en el enum ...: [X_INVENTADO]` y `<10>` frente a `<11>`. (b) falla con `Sobran en el enum: [PLUGIN_AUSENTE]` y `<9>`. Al revertir, `rc=0` con 7/0/0/0. En el worktree, `git diff --stat -- aidlc-docs verificador/src/main` da vacío |
| 4 | Invariantes, `tests` ≥ 10 | `grep -ho ... TEST-*VeredictoTest.xml` | pasa, `tests="14" skipped="0" failures="0" errors="0"` |
| 5 | Sin Spring ni Fabric8 en ejecución | `./gradlew :verificador:dependencies --configuration runtimeClasspath \| grep -ciE "spring\|fabric8"` | pasa, `0` |
| 6 | Todo el verificador sin caché | `./gradlew :verificador:test --rerun :verificador:check` | pasa, `BUILD SUCCESSFUL`. Las 7 suites dan `skipped="0" failures="0" errors="0"` (7+3+28+2+36+14+16), todas con marca de hora de esta ejecución, y ningún XML tiene `<skipped` |

## Hallazgos

### F-07 · NARANJA · CodigoMotivoTest.java, `nombresDeLasLineas` (rama `else if (enTabla) break;`) · Una fila GFM sin `|` inicial o con sangría se descarta en silencio: 11 filas dan verde
El lector da por terminada la tabla en la primera línea que no empieza por `|`. GFM no la termina ahí: la tabla sigue hasta una línea en blanco o hasta otro bloque. El `|` inicial es opcional y se admiten hasta 3 espacios de sangría. Probé dos filas agregadas después de `DESCRIPTOR_INVALIDO`:

- `` `X_INVENTADO` | x | y | `` da `rc=0`, 7/0/0/0 (`ev/p/sinPipeInicial.txt`).
- `` ··| `X_INVENTADO` | x | y | `` (dos espacios delante) da `rc=0`, 7/0/0/0 (`ev/p/filaIndentada.txt`).

Comprobé cómo se ve el documento: con `pandoc -f gfm -t html` sobre la sección salen 1 tabla y 12 `<tr>`, es decir, encabezado más 11 filas de datos, y `<td><code>X_INVENTADO</code></td>` es la última (`ev/r-sinPipeInicial.html`, `ev/r-filaIndentada.html`). La tabla que se ve tiene 11 códigos y CA-2 queda en verde. Es el mismo defecto de F-01, pero en el borde final de la tabla.

Cuando la línea sin `|` cae en medio de la tabla, la prueba sí falla, porque la cuenta no cuadra. El mensaje, eso sí, engaña: `Sobran en el enum: [DESCRIPTOR_INVALIDO]` (`ev/p/sinPipeMedio.txt`).

Lo que se espera: la tabla termina en una línea en blanco o en un `##`. Toda línea no vacía que llegue antes es una fila de datos, y si no se puede leer falla citando la línea, igual que las demás.

### F-08 · AMARILLO · CodigoMotivoTest.java, `nombresDeLasLineas` · Una segunda tabla en la misma sección se ignora
Probé agregar, después de una línea en blanco, otra tabla con encabezado, separador y una fila `` `X_INVENTADO` ``. Da `rc=0` (`ev/p/segundaTabla.txt`), y pandoc muestra 2 tablas. Es lo que dice la letra de CA-2 («la tabla que sigue»), así que no bloquea. Ahora que el lector se para en el `##`, lo coherente sería fallar si la sección trae más de una tabla.

### F-09 · AMARILLO · bitacoras/U01-T08.md, «Ronda 2 / Verde» · El verde y los pares de CA-3 de esta ronda están resumidos, sin comando
- El rojo trae el comando y un resumen de la salida.
- El verde («CodigoMotivoTest tests=7 failures=0; ...»), los pares de CA-3, CA-5 y CA-6 se dan en prosa, sin `$ comando` ni salida pegada.

Por sí sola, esa parte de la bitácora es una afirmación sin verificar. No bloquea porque yo reproduje todo lo que afirma.

No reconstruí el lector viejo para repetir el rojo. Lo que dice es coherente con el diff: las 3 pruebas nuevas que deberían fallar con el lector viejo son justo las que nombra.

## Lo que comprobé y está bien
- **Sondas de la ronda 1, repetidas en la copia** (`ev/p/*.txt`):
  - `X-INVENTADO`, `` `X_INVENTADO` (nuevo) `` y dos códigos en una celda fallan con «Fila de la tabla que no empieza por un único nombre de código, línea N: ...». Queda corregido el mensaje engañoso de F-01.
  - Encabezado cambiado: falla con «No se encontró la sección». Lo mismo con un espacio al final del encabezado.
  - Sección sin tabla: falla con «no trae una tabla». Lo mismo con la tabla indentada.
  - Archivo ausente: falla con «No existe el archivo de diseño».
  - Desde `verificador/` con `../gradlew`, y con `-p` desde otro directorio: pasa, con el XML de esa misma ejecución. Con la mutación (a), falla también desde el subdirectorio.
- **Formas nuevas**:
  - Una fila vacía `| | | |` falla citando la línea.
  - Un `|` escapado en la primera celda falla citando la línea. Escapado en otra celda, se lee bien y detecta `X_INVENTADO`.
  - El separador con `:` pasa, y un separador roto falla.
  - CRLF y BOM pasan, igual que el original.
  - Si falta el encabezado, falla con «separador».
  - Un comentario HTML o una línea en blanco seguida de `| X |` sin separador no forman parte de la tabla según pandoc, y el lector los ignora bien.
- **F-02 a F-06**:
  - Ya no está la comprobación muerta de `Código`.
  - El lector se para en el `##`.
  - `hayMotivos` es único (es el único cambio en `src/main`, dentro de un archivo que agrega esta tarea).
  - Están las pruebas de nulos en las listas, de `alternativas == null` y de veredictos distintos.
  - Las mutaciones se hicieron en una copia.
- **Alcance**: `git diff --name-status origin/main...HEAD` solo trae `A`, ningún archivo existente de `src/main` cambia, y `build.gradle.kts` no se toca.
- **Worktree**: limpio al terminar (`git status --short` vacío).

## Tareas candidatas
- Ninguna nueva. C-22 sigue en pie.

## Rutas de transcripciones largas
- /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/u01t08r2/ev/ca1.txt
- /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/u01t08r2/ev/ca6.txt
- /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/u01t08r2/ev/p/ (una transcripción por sonda: `a.txt`, `b.txt`, `sinPipeInicial.txt`, `filaIndentada.txt`, `segundaTabla.txt`, etc.)
- /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/u01t08r2/ev/r-sinPipeInicial.html (y `r-filaIndentada.html`, `r-segundaTabla.html`, `r-lineaVaciaYFila.html`)
- /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/u01t08r2/bin/mut.py y /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/u01t08r2/bin/sonda.sh

No escribí el archivo del informe, porque soy de solo lectura; este texto es el informe de `revisiones/U01-T08/ronda-2.md`.

```
VEREDICTO: NO-VERDE
NARANJA|verificador/src/test/java/io/synkra/verificador/CodigoMotivoTest.java:nombresDeLasLineas|Una fila GFM sin | inicial o con sangría se descarta en silencio: una tabla de 11 filas pasa
INFORME: revisiones/U01-T08/ronda-2.md
```
