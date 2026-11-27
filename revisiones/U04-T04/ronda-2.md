# Ronda 2 — U04-T04

VEREDICTO: NO-VERDE

Corrí yo los cuatro criterios y los cuatro pasan. F-01, F-03, F-06 y F-07 (los permisos) quedan cerrados con evidencia propia. Pero `EstructuraZip` no cierra F-04, que ahora es obligatorio. Hay dos maneras de colar una entrada `../../evil` que `JarInputStream` ve y la revisión no: un EOCD que declara cero entradas y una entrada escondida dentro del `csize` del directorio central. Además, la nueva separación de `IOException` deja salir como 500 ZIP que simplemente están mal formados.

## Comprobaciones previas
- **Hash de la tarea:** `a2b1e767b55d3f5456311029b2e0d25c6cdc2e1e`. Coincide.
- **HEAD y base:** HEAD es `d19d08f`. Desde la ronda 1 hay dos commits: `7d81c51` (el informe) y `d19d08f` (el retrabajo).
- **Worktree:** `git status --short` salió vacío antes y después.
- **Copias:** las sondas y las mutaciones se hicieron en copias `git archive HEAD` del scratchpad.

## Criterios de aceptación, verificados por mí
| # | Criterio | Comando que corrí | Resultado |
|---|---|---|---|
| 1 | Recepción, sin caché y sin saltos | `./gradlew :backend:test --rerun --tests '*RecepcionArtefactoTest'` + `grep` del XML | pasa: `BUILD SUCCESSFUL`, `tests="44" skipped="0" failures="0" errors="0"`. El timestamp del XML (12:49:42Z) es de mi corrida. |
| 2 | Hash en la misma pasada | el `grep` de CA-2 | pasa: hay un solo `DigestInputStream`, en `RecepcionArtefacto.java:89`. Pero ver F-11. |
| 3 | Todo el backend y las fronteras | `./gradlew :backend:test --rerun :backend:check` + bucle sobre los XML | pasa: 15 XML y 144 pruebas, todas con `skipped=0 failures=0 errors=0`. `InterfazCatalogoTest` 3/3 y `FronterasModulosTest` 3/3. |
| 4 | Diff acotado | `git diff --stat origin/main...HEAD` | pasa: 11 archivos, todos en la lista. `synkra.max-entradas-artefacto` está autorizado. |

## Los hallazgos de la ronda 1
- **F-01: cerrado.**
  - `upath_crudo_malo` y `upath_unicode_malo` dan 422.
  - M27 y U01 caen.
  - U02 (sin CP437) sobrevive, pero es equivalente para los caracteres ASCII que se comprueban (`.`, `/`, `\`, NUL y `:`).
- **F-02: arreglado para la interrupción, pero con una regresión inversa.** Va en F-09.
- **F-03: cerrado.** Las unidades son constantes fijadas en `textosDeMotivoFijados`, y M16 y M17 caen.
- **F-04: no cerrado.** Va en F-08.
- **F-05: no cerrado, sigue AMARILLO.** Va en F-11.
- **F-06: cerrado.** L01 cae. Cuidado: M02 (sin el corte) ahora cuelga en un bucle de `read(…, 0)`, y solo lo mata el timeout.
- **F-07: los permisos están cerrados.** `.tmp` queda en 0700 y P01 y P02 caen. El tope, en cambio, no protege la memoria: va en F-10.

## Desviaciones declaradas
1. **Se rechaza cualquier dato antepuesto: aceptada.** Es lo que pide la decisión. `pre_junk_ok` da 422.
2. **Un 0x7075 distinto del nombre se rechaza, aunque su CRC sea inválido: aceptada.** Es conservadora. Como nota: un ZIP de Info-ZIP con nombres no ASCII, sin el bit 11 y con un 0x7075 correcto, también se rechazaría. Para un JAR (UTF-8 con el bit 11) es aceptable.
3. **«Un directorio central vacío con entradas locales da `ARTEFACTO_INVALIDO»`: falsa como afirmación general.** Solo vale para el offset que usa la prueba. Va en F-08.
4. **El EOCD que miente, acotado por los 20 MB: no se acepta.** La cota real es de unos 690 MB de heap por petición. Va en F-10.

## Hallazgos
### F-08 · ROJO · `EstructuraZip.java:79-118` · entradas locales que `JarInputStream` ve y la revisión no
- **Sondas aceptadas, con `JarInputStream` viendo `../../evil` (`traza.log` y `sonda.log`):**
  - `solo_locales_eocd_cero`: una cabecera local `../../evil` seguida de un EOCD con 0 entradas y offset 0. Con 0 entradas, `desajuste` compara `inicioDirectorio(0) - esperado(0)`, le da 0 y lo da por bueno. La JDK ve `[]`.
  - `comentario_eocd_falso_malo`: un EOCD falso dentro del comentario del verdadero. Pasa lo mismo.
  - `oculta_sin_bit3`: la cabecera local dice el `csize` real, pero el directorio central dice un `csize` mayor, que tapa una cabecera local `../../evil` puesta detrás del deflate. La JDK ve `[a.txt]` y `JarInputStream` ve `[a.txt, ../../evil]`.
  - `oculta_bit3_dd`: la misma idea, con el bit 3 y un descriptor de datos dentro del hueco.
- **La raíz:** solo se comparan el offset y el nombre. Nunca se comprueba que el método, los flags, el CRC y los tamaños de la cabecera local (o del descriptor) coincidan con el directorio central. Tampoco se exige que el central termine justo en el EOCD (`inicio + tamaño == posEocd`), ni que haya al menos una entrada si el archivo tiene bytes antes del EOCD.
- **Otro caso de la misma clase:** `local_metodo_distinto` (STORED en la cabecera local, DEFLATED en el central) también se acepta.
- **Ojo:** con el bit 3, el fin real del deflate solo se conoce inflando. Hace falta inflar acotado y descartando la salida, o rechazar los casos ambiguos.

### F-09 · ROJO · `RecepcionArtefacto.java:133-151` · ZIP mal formados dan 500 en vez de 422
- La decisión 3 exige un 422 `ARTEFACTO_INVALIDO` para todo lo que no es un ZIP legible.
- En el fuzz de 60.000 casos (`fuzz.log`) escapan dos clases:
  - **1618 `IOException`** con «Too many disks…» o «Error reading Zip content…». El heurístico de mensajes no las reconoce.
  - **41 `IllegalArgumentException`** que vienen de `FileChannel.position` dentro de commons (`positionAtCentralDirectory64`). Una se reproduce de forma determinista con `e64_cdoff_negativo.zip`.
- El temporal sí se borra (`quedan=0`).
- **Arreglo esperado:** clasificar por el origen (el formato, en la apertura y el parseo) y no por el texto del mensaje, y capturar las `RuntimeException` del parser.

### F-10 · NARANJA · `RecepcionArtefacto.java:108-118` · el tope de entradas no protege la memoria
- `eocd_miente_20mb`: 446.201 entradas reales con un EOCD que dice 1.
  - Con 1 GB de heap, se rechaza en 2,8 s con un pico de unos 687 MB.
  - Con `-Xmx256m`, la JVM muere con «Java heap space».
- El conteo previo también se salta con un conteo ZIP64 negativo (`e64_entradas_negativas` se acepta), porque `>` con un `long` negativo nunca es verdadero.
- **Arreglo esperado:** contar los registros del central sin asignar memoria, o acotar por `tamaño del central / 46`, antes de llamar a commons.

### F-11 · AMARILLO · `RecepcionArtefactoTest.java:617` · la pasada única del hash sigue sin atar
- Mi M05 sobrevive: hashea con `nuevoDigest().digest(Files.readAllBytes(temporal))` y no toca el espía.
- `f05` comprueba que el espía se alimenta por trozos, no que el hash devuelto salga de él.
- **Arreglo posible:** que el espía devuelva un digest centinela y afirmar que `sha256()` es igual a ese centinela.

### F-12 · NARANJA · `EstructuraZip.java` · el código nuevo de más riesgo, sin cobertura
De los 17 mutantes sobre `EstructuraZip`, sobreviven 10 (`mutaciones.txt`):
- **E03:** sin la comprobación de la firma local.
- **E05:** sin ordenar las entradas.
- **E09 y E14:** se admite un descriptor de datos sin el bit 3.
- **E10, E16 y E17:** toda la rama ZIP64 del EOCD.
- **E11:** sin soporte de comentario en el EOCD.
- **E12:** un `EOFException` sale como otro código.
- **E13:** sin la guarda de `pos < 0`. Sin ella, `e64_loc_negativo` daría un 500.

Faltan pruebas con un EOCD comentado, con ZIP64 (en el EOCD y en la cabecera local) y con una firma local corrupta.

## Ataques a `EstructuraZip`: lo que aguanta (`sonda.log`)
- **Rechazados:**
  - Con 422 `RUTA_NO_PERMITIDA`: `mismo_offset`, `mm_local_malo`, `pre_zipmalo_luego_ok` y `pre_junk_*`.
  - Con 422 `ARTEFACTO_INVALIDO`: `solapadas`, `offset_fuera`, `csize_enorme`, `nombre_local_largo_fuera`, `e64_loc_fuera`, `e64_loc_negativo`, `extra_ffff` y `z64_offset_en_extra`.
  - `entradas_100k`: rechazado con el tope en 25 ms.
- **Aceptados, y es correcto:**
  - los descriptores de datos con firma, sin firma y ZIP64 de 24 bytes;
  - ZIP64 en la cabecera local y en el central, y `e64_ok`;
  - comentarios de 1000 bytes y de 65535 bytes.
- **Bomba de 1 GiB:** se acepta sin inflarse.
- **Fuzz diferencial (`fuzz.log`):** en los aceptados, ninguna discrepancia entre los nombres del flujo y los del central. Las mutaciones al azar no encuentran F-08: hace falta construirlo a mano.

## Mutaciones (60 mutantes, `RecepcionArtefactoTest` en cada corrida; `mutaciones.txt`)
- **Caen 47:**
  - las de la ronda 1, menos M05;
  - U01, I01 a I06, T01 a T05, L01, P01 y P02;
  - E01, E02, E04, E06, E07, E08 y E15.
- **Cuelga 1:** M02 da timeout a los 180 s.
- **Sobreviven 12:**
  - M05 (F-11);
  - U02 (equivalente);
  - E03, E05, E09 a E14, E16 y E17 (F-12).

## Tareas candidatas (defectos reales fuera de alcance)
- Siguen como en la ronda 1: los enlaces solo en `AsiExtraField` y los tipos FIFO o de dispositivo. Info-ZIP los extrae como archivos regulares.

## Rutas de transcripciones largas
Todas en `/tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/u04t04/r2/`:
- `ca1.log` y `ca3.log`
- `scripts/gen2.py`, `gen3.py` y `gen4.py`: generan los ZIP, que quedan en `zips2/`, `zips3/` y `zips4/` (todos juntos en `todos/`).
- `sonda.log`, `traza.log`, `fuzz.log` y `memoria.log`, junto con `sondad-256m.log` (el OOM). Las sondas están en `sonda/backend/src/test/java/io/synkra/catalogo/SondaR2*Test.java`.
- `mutaciones.txt`, `mut-progreso.txt`, `mut-*.log` y `scripts/mut2.py`.

```
VEREDICTO: NO-VERDE
ROJO|backend/src/main/java/io/synkra/catalogo/EstructuraZip.java:79-118|entradas locales ocultas que JarInputStream ve y la revisión no: EOCD con 0 entradas, EOCD falso en el comentario, csize del central que tapa una cabecera local; tamaños, método y CRC locales sin comparar
ROJO|backend/src/main/java/io/synkra/catalogo/RecepcionArtefacto.java:133-151|ZIP mal formados dan 500: 1618 IOException ("Too many disks", "Error reading Zip content") y 41 IllegalArgumentException de 60.000 en el fuzz
NARANJA|backend/src/main/java/io/synkra/catalogo/RecepcionArtefacto.java:108-118|el tope de entradas no protege la memoria: un EOCD que miente carga 446.000 entradas (unos 687 MB, OOM con 256 MB); un conteo ZIP64 negativo salta el previo
NARANJA|backend/src/main/java/io/synkra/catalogo/EstructuraZip.java|10 de 17 mutantes sobreviven: ZIP64, comentario del EOCD, firma local, orden, descriptor sin bit 3, guarda de pos<0
AMARILLO|backend/src/test/java/io/synkra/catalogo/RecepcionArtefactoTest.java:617|la pasada única del hash sigue sin atar: M05 con nuevoDigest() sobrevive
INFORME: revisiones/U04-T04/ronda-2.md
```
