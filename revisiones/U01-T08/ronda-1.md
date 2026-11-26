# Ronda 1 — U01-T08

VEREDICTO: NO-VERDE

Los seis criterios pasan cuando los corro yo. Lo único que bloquea es un hallazgo NARANJA en el lector del Markdown de `CodigoMotivoTest`: una fila mal formada de la tabla se descarta sin avisar, así que una tabla con 11 filas da verde.

Lo que revisé: tarea `tareas/U01-T08-modelo-veredicto.md`, con hash 7420afc7df640bdd3d549092a58552dbec3b6e01, que coincide con el que me diste. Worktree `/home/user/wt-m6r-U01-T08`, HEAD dfe1ca5, diff `origin/main...HEAD` con base b4ec449.

## Criterios de aceptación, verificados por mí
| # | Criterio | Comando que corrí | Resultado |
|---|---|---|---|
| 1 | Pruebas del modelo sin caché | `./gradlew :verificador:test --rerun --tests '*CodigoMotivoTest' --tests '*VeredictoTest'` | pasa, `BUILD SUCCESSFUL`. Los XML son de esta ejecución (17:26:59Z), en 0,045 s y 0,042 s: son pruebas puras en memoria, sin estado externo |
| 2 | 10 códigos, igual al enum | `grep -ho ... TEST-*CodigoMotivoTest.xml` | pasa, `tests="3" skipped="0" failures="0" errors="0"` |
| 3 | Dos pares falla-pasa | `git archive HEAD` en `u01t08r1/copia/` del scratchpad, con la fila (a) agregada y luego `PLUGIN_AUSENTE` borrada | pasa. (a) falla y nombra `faltan en el enum: [X_INVENTADO]` y `expected: <10> but was: <11>`; al revertir, pasa. (b) falla y nombra `Sobran en el enum: [PLUGIN_AUSENTE]` y `<9>`; al revertir, pasa. En el worktree, `git diff --stat -- aidlc-docs verificador/src/main` da vacío |
| 4 | Invariantes, `tests` ≥ 10 | `grep -ho ... TEST-*VeredictoTest.xml` | pasa, `tests="12" skipped="0" failures="0" errors="0"` |
| 5 | Sin Spring ni Fabric8 en ejecución | `./gradlew :verificador:dependencies --configuration runtimeClasspath \| grep -ciE "spring\|fabric8"` | pasa, `0`. La única dependencia es `snakeyaml-engine:3.2` |
| 6 | Todo el verificador sin caché | `./gradlew :verificador:test --rerun :verificador:check` | pasa, `BUILD SUCCESSFUL`. Las 7 suites dan `skipped="0" failures="0" errors="0"` (3+3+28+2+36+12+16) y ningún XML tiene `<skipped` |

## Hallazgos

### F-01 · NARANJA · verificador/src/test/java/io/synkra/verificador/CodigoMotivoTest.java:53-56 · Las filas que no encajan con `FILA` se descartan en silencio
Una línea que empieza por `|` pero no encaja con ``^\|\s*`?([A-Za-z0-9_]+)`?\s*\|.*`` no se cuenta ni da error. Por eso «hay 10 filas» cuenta los nombres que el lector logró extraer, no las filas de la tabla. Lo comprobé en la copia, agregando una fila 11 después de `DESCRIPTOR_INVALIDO`:
- `| X-INVENTADO | ... |` da `rc=0`, `BUILD SUCCESSFUL` (`p-guion.txt`).
- ``| `X_INVENTADO` (nuevo) | ... |`` da `rc=0`, `BUILD SUCCESSFUL` (`p-textoExtra.txt`).

Las dos tablas tienen 11 filas de datos y la prueba de CA-2 queda en verde.

El mismo fallo hace que algunos mensajes digan lo contrario de lo que pasa. Con ``| `PLUGIN_AUSENTE`, `X_INVENTADO` | ...``, la prueba falla con `Sobran en el enum: [PLUGIN_AUSENTE]`, aunque el código sí está en la tabla (`p-dosEnCelda.txt`).

Lo que se espera: toda fila de datos (todo lo que no sea el encabezado ni el separador `|---|`) cuyo primer campo no se pueda leer debe hacer fallar la prueba, con un mensaje que cite la línea.

### F-02 · AMARILLO · CodigoMotivoTest.java:54 · `!m.group(1).equals("Código")` no hace nada
La `ó` no está en `[A-Za-z0-9_]`, así que el encabezado `| Código |` nunca encaja con `FILA`. El encabezado se descarta por la misma vía silenciosa de F-01, no por esta comprobación. Cuando se arregle F-01, el encabezado y el separador se deben tratar de forma explícita, por posición.

### F-03 · AMARILLO · CodigoMotivoTest.java:50-60 · El lector no se para en el siguiente `##`
Si la sección «Códigos de motivo» se quedara sin tabla, el lector tomaría la primera tabla que venga después, en cualquier sección. Hoy no lleva a un falso verde, porque los nombres no cuadrarían con el enum, pero el mensaje engañaría. Una tabla que viene después de la buena sí se ignora bien: la lectura corta en la primera línea que no empieza por `|`.

### F-04 · AMARILLO · Veredicto.java:18 y :31 · `hayMotivos` está duplicado
La misma expresión aparece en el constructor canónico y en `de`. No hay incoherencia, porque el constructor vuelve a validar, pero son dos fuentes para una sola regla.

### F-05 · AMARILLO · VeredictoTest.java · Faltan casos secundarios
- No hay prueba de `null` dentro de las listas de `VeredictoPlugin` ni de `Veredicto`, ni de `alternativas == null`. El código sí los rechaza: mi sonda da NPE en los seis caminos.
- `igualdadYHash` solo prueba igualdad entre objetos iguales; no comprueba que dos veredictos distintos no sean `equals`.

CA-4 solo pide nulos para `Seleccion`, así que esto no bloquea.

### F-06 · AMARILLO · bitacoras/U01-T08.md, CA-3 · El par falla-pasa se hizo sobre el worktree
El codificador editó el archivo del propio worktree y lo restauró con `cp`, en vez de usar una copia aparte. El worktree terminó limpio y yo reproduje los dos pares en una copia, así que la evidencia queda confirmada. Es solo una desviación del procedimiento.

## Lo que comprobé y está bien
- **Invariantes** (con la sonda `u01t08r1/sonda/`, compilada contra `build/classes`):
  - `null` en cualquier campo o dentro de cualquier lista da NPE, en el constructor canónico y en `de`.
  - `APROBADA` con motivos, sean globales o solo de plugins, da IAE, y `RECHAZADA` sin motivos también.
  - No se puede construir un `Veredicto` incoherente: un `VeredictoPlugin` incompatible siempre trae motivos.
  - `Alternativa` rechaza una versión `null` y un nombre que no cumple el patrón (`" a"` incluido).
  - Las listas se copian con `List.copyOf` y no se filtran mutables por los accesores.
- **Equals y hashCode**: coinciden entre una lista mutable y una `List.of`, y entre el constructor canónico y `de`, incluido el `Optional` de `Descriptor`.
- **Lector**:
  - Si el archivo no está, falla («No existe el archivo de diseño»). Si cambia el encabezado, falla («No se encontró la sección»). Si la tabla está indentada, falla. En ningún caso se salta.
  - Las filas con espacios y comillas invertidas se leen bien.
  - Da lo mismo correr desde `verificador/` con `../gradlew` o con `-p` desde `/tmp`: pasa y el XML se escribe en esa misma ejecución.
- **Fidelidad al diseño**: los campos y tipos son los de `domain-entities.md`, con `Version` en `Alternativa`, `Descriptor` en `VeredictoPlugin` y los dos enums anidados.
- **Alcance**: el diff solo agrega archivos (`git diff --name-status`). No modifica ningún archivo existente de `src/main` ni `build.gradle.kts`, y no hay dependencias nuevas. `revisiones/U01-T08/ronda-1-respuesta.md` es lo que pide `loop/README.md:21`.
- **Worktree**: limpio al terminar (`git status --short` vacío).

## Tareas candidatas (defectos reales fuera de alcance)
- `VeredictoPlugin` acepta `compatible = true` con alternativas. Según R-V5, las alternativas solo existen para `VERSION_INCOMPATIBLE`. Corresponde decidirlo en U01-T10.

## Rutas de transcripciones largas
- /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/u01t08r1/ca1.txt
- /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/u01t08r1/ca6.txt
- /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/u01t08r1/ca3-a-falla.txt (también `ca3-a-pasa.txt`, `ca3-b-falla.txt`, `ca3-b-pasa.txt`)
- /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/u01t08r1/p-guion.txt (también `p-textoExtra.txt`, `p-dosEnCelda.txt`, `p-espacios.txt`, `p-encabezado.txt`, `p-sinArchivo.txt`, `p-indentada.txt`, `p-subdir.txt`, `p-p.txt`)
- /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/u01t08r1/sonda/io/synkra/verificador/Sonda.java

```
VEREDICTO: NO-VERDE
NARANJA|verificador/src/test/java/io/synkra/verificador/CodigoMotivoTest.java:53-56|Las filas que no encajan con FILA se descartan en silencio: una tabla de 11 filas pasa
INFORME: revisiones/U01-T08/ronda-1.md
```
