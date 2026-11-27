# Ronda 1 — U04-T04

VEREDICTO: NO-VERDE

Corrí yo los cuatro criterios y los cuatro pasan. Aun así hay un ROJO: un JAR cuya entrada del directorio central se llama `../../evil` se acepta. La razón es el campo extra «Unicode Path» (0x7075) de commons-compress. Además quedan dos NARANJA: cualquier `IOException` al abrir el ZIP se informa como culpa del cliente, y las unidades del motivo están escritas como literales.

## Comprobaciones previas
- **Hash de la tarea:** `a2b1e767b55d3f5456311029b2e0d25c6cdc2e1e`. Coincide.
- **HEAD y base:** HEAD es `8a071a9`, con un solo commit sobre `672f30d`.
- **Worktree:** `git status --short` salió vacío antes y después.
- **Copias:** las sondas y las mutaciones se hicieron en copias `git archive HEAD` del scratchpad.

## Criterios de aceptación, verificados por mí
| # | Criterio | Comando que corrí | Resultado |
|---|---|---|---|
| 1 | Recepción, sin caché y sin saltos | `./gradlew :backend:test --rerun --tests '*RecepcionArtefactoTest'` + `grep` del XML | pasa: `BUILD SUCCESSFUL`, `tests="25" skipped="0" failures="0" errors="0"`. El timestamp del XML es de mi corrida. La primera prueba tarda 0,5 s (arranque) y las demás entre 3 y 71 ms. |
| 2 | Hash en la misma pasada | el `grep` de CA-2 | pasa: solo `RecepcionArtefacto` usa el digest, con `DigestInputStream` en la línea 62. La bitácora lo explica en una línea. |
| 3 | Todo el backend y las fronteras | `./gradlew :backend:test --rerun :backend:check` + bucle sobre los XML | pasa: 15 XML y 125 pruebas, todas con `skipped=0 failures=0 errors=0`. `InterfazCatalogoTest` 3/3 y `FronterasModulosTest` 3/3. |
| 4 | Diff acotado | `git diff --stat origin/main...HEAD` | pasa: 8 archivos, todos en la lista. `build.gradle.kts` solo añade commons-compress y `application.properties` solo `synkra.max-artefacto`. |

## Las desviaciones declaradas
1. **commons-compress fijado en 1.27.1: aceptada.** El BOM `spring-boot-dependencies-3.5.16.pom` no lo menciona (`grep -c` da 0). Arrastra `commons-io` 2.16.1 y `commons-lang3` como transitivas: va como nota, no bloquea.
2. **Un ZIP válido y vacío se acepta: aceptada.** Ninguna entrada es culpable, y U04-T06 lo rechaza por falta de descriptor. Comprobé además que un directorio central vacío con entradas locales `../../evil` da 422 `ARTEFACTO_INVALIDO` (`cd_vacio_local_malo`).
3. **Un nombre de entrada vacío se rechaza: aceptada.** M07 cae.
4. **Un enlace simbólico da `RUTA_NO_PERMITIDA` con su propio motivo: aceptada.** Cumple «un motivo que nombra la entrada».
5. **El ZIP se abre sobre el temporal ya copiado: aceptada.** Esa lectura es del directorio central, no del hash. Pero M05 sobrevive: un hash hecho con una segunda lectura pasa todas las pruebas, así que solo lo protege el `grep` de CA-2 (F-05).
6. **Las pruebas de más: aceptadas.**
7. **El formato del límite: el formato vale, los literales no.** Salen «20 MB», «1 KB» y «126 bytes», pero las unidades son literales visibles para el usuario. Va en F-03.

## Hallazgos
### F-01 · ROJO · `RecepcionArtefacto.java:78` · el campo 0x7075 tapa una entrada `..` del directorio central
- `ZipFile.builder().setPath(jar).get()` usa por defecto `useUnicodeExtraFields=true`. Si el CRC coincide, el nombre del extra sustituye al nombre crudo.
- La sonda `upath_crudo_malo.zip` tiene el nombre crudo `../../evil` tanto en la cabecera local como en el directorio central, y el extra 0x7075 dice `ok/evil123`.
- **RecepcionArtefacto la acepta.**
- `java.util.zip.ZipFile` y `JarInputStream` (es decir, JarFile, el cargador de clases y cualquier extractor de la JDK) ven `../../evil`. Solo commons e Info-ZIP ven el nombre «bueno».
- Esto incumple la decisión 3 («se recorren todas las entradas del directorio central… se rechaza si contiene `..`») y R-C6.
- M27 (`setUseUnicodeExtraFields(false)`) sobrevive: ninguna prueba ata este caso.
- **Arreglo esperado:** revisar el nombre crudo (`getRawName()` decodificado) además del nombre que entrega commons, o desactivar los extras unicode. Hace falta una prueba con este JAR.
- Evidencia: `sonda.log`, fila `upath_crudo_malo`. En el fuzz, 1475 de las 10.000 variantes de este JAR caen solo cuando el CRC se rompe.

### F-02 · NARANJA · `RecepcionArtefacto.java:87` · toda `IOException` al abrir el ZIP se vuelve 422 «no es un JAR»
- `catch (IOException noEsZip)` traga los errores de disco y la interrupción, y además pierde la causa.
- La sonda interrumpe el hilo al final de la copia: sale `422 ARTEFACTO_INVALIDO … no es un JAR (ZIP) legible`, por una `ClosedByInterruptException`. Un fallo del servidor se informa como culpa del cliente.
- El temporal sí se borra (`quedan=0`).
- **Arreglo esperado:** distinguir el formato inválido (`ZipException` y el «central directory» de commons) de la E/S. Ninguna prueba ata esa distinción.

### F-03 · NARANJA · `RecepcionArtefacto.java:108-113` · las unidades del motivo son literales incrustados
- `" MB"`, `" KB"` y `" bytes"` son textos nuevos visibles para el usuario, escritos como literales dentro de las ramas de `texto()`.
- La nota de la tarea pide los motivos «en constantes», y `textosDeMotivoFijados` no los fija.
- Se aplica la invariante 8.

### F-04 · AMARILLO · cabecera local o datos antepuestos distintos del directorio central
- `mm_local_malo` (local `../../evil`, central `ok/evil123`) y `pre_zipmalo_luego_ok` (un ZIP malo antepuesto a uno bueno) se aceptan.
- `JarInputStream` y `ZipArchiveInputStream` leen `../../evil`.
- La decisión 3 se limita al directorio central, así que el comportamiento está dentro de lo especificado. Pero el riesgo existe para cualquier lector en flujo de U04-T06 o del kernel. Va como candidata.

### F-05 · AMARILLO · `RecepcionArtefacto.java:50` · ninguna prueba ata la pasada única del hash
- M05 (hashear con `Files.readAllBytes(temporal)`) sobrevive.
- Hoy solo lo protege el `grep` de CA-2.

### F-06 · AMARILLO · `RecepcionArtefacto.java:60-66` · se lee hasta un búfer más allá del límite
- Con el límite en 1000 bytes se leen 8192 bytes del flujo, aunque no se escriben.
- La decisión 1 dice «se corta al leer el byte límite + 1». Leer `min(buffer, límite + 1 - total)` sería literal.
- El flujo no se drena y no se usa `Content-Length`.

### F-07 · AMARILLO · `.tmp` con 0755 y bomba de entradas sin tope
- El temporal se crea con 0600, pero el directorio `.tmp` queda con `rwxr-xr-x` (depende de la umask).
- 100.000 entradas tardan unos 780 ms y unos 82 MB de heap. Por debajo de 20 MB caben unas 220.000.
- Sin tope de entradas, cada petición concurrente multiplica ese coste.

## Ataques al JAR, verificados (`sonda.log`)
- **Rechazados con 422 `RUTA_NO_PERMITIDA`, y nada queda en disco:**
  - `a/./../..`, `..\x`, `a/b\..\../x`, `../`, `dir/../` y `\\srv\share\x`;
  - un ZIP64 con `../x`;
  - un ZIP con datos antepuestos y `../x`;
  - datos pospuestos de menos de 64 KB;
  - un enlace Unix con 0120777.
- **Aceptados, y es correcto:**
  - `./x`, `%2e%2e/x`, `‥/x` (U+2025), `．．/x` (fullwidth), `a/.../x` y `.. /x`;
  - un nombre de 65535 bytes (el motivo se recorta a 200).
- **Pospuestos de más de 64 KB:** dan 422 `ARTEFACTO_INVALIDO`, igual que la JDK.
- **Enlace solo en `AsiExtraField`, plataforma FAT con bits de enlace, FIFO, dispositivos de carácter y de bloque, y socket: se aceptan.** Info-ZIP `unzip` los extrae como archivos regulares (lo comprobé), así que no es un hallazgo.
- **Enlace duro:** ZIP no tiene ese tipo de entrada.
- **Bomba de 1 GiB (1 MB comprimido):** se acepta en 58 ms sin descomprimir nada.
- **Fuzz de 20.000 mutaciones:** ninguna excepción no prevista y ningún temporal huérfano.

## Límite, temporal y SHA-256 (`sonda.log.b`)
- **El corte:** el límite + 1, leído byte a byte, da 413. Exactamente el límite se acepta (M01 cae).
- **Borrado en toda ruta de error:** con `OutOfMemoryError`, `InterruptedIOException` y una interrupción a mitad, el temporal se borra en las tres.
- **La carrera:** 64 recepciones concurrentes dan 64 rutas distintas, sin errores.
- **El SHA-256:** sale de `HexFormat.of()`, en minúsculas (M04 cae).
- **Las fronteras:** `RecepcionArtefacto`, `ArtefactoRecibido` y `CodigosCatalogo` no son públicos, y `api` no cambia.

## Mutaciones (30 mutantes, 25 pruebas en cada corrida; `mutaciones.txt`)
- **Caen 28:** M01 a M04, M06 a M26 y M28 a M30. Entre ellas:
  - el límite, el borrado, las mayúsculas;
  - cada regla de ruta, el enlace y el tope de 10 detalles y de 200 caracteres;
  - el código de error, el campo y el estado;
  - el directorio `.tmp`, el tamaño y el formato de unidades.
- **Sobreviven 2:** M05 (la segunda lectura para el hash, F-05) y M27 (sin los extras unicode, F-01).

## Tareas candidatas (defectos reales fuera de alcance)
- Validar los nombres de la cabecera local frente al directorio central, o la lectura en flujo, antes de que U04-T06 o el kernel lean el JAR (F-04).
- Un tope de número de entradas y de memoria en la revisión (F-07).
- Los permisos de `$SYNKRA_DATA/artefactos/.tmp`, independientes de la umask (F-07).

## Rutas de transcripciones largas
Todas en `/tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/u04t04/r1/`:
- `ca1.log` y `ca3.log`
- `scripts/gen.py`: genera los ZIP hostiles, que quedan en `zips/`.
- `sonda.log`, `sonda.log.b` y `sonda.log.c`: las sondas `SondaR1Test`, `SondaR1bTest` y `SondaR1cTest`, que están en `sonda/backend/src/test/java/io/synkra/catalogo/`.
- `unz/`: lo que extrae `unzip` de cada sonda.
- `mutaciones.txt`, `mut-M*.log` y `scripts/mut.py`.

```
VEREDICTO: NO-VERDE
ROJO|backend/src/main/java/io/synkra/catalogo/RecepcionArtefacto.java:78|el extra Unicode Path (0x7075) tapa una entrada '../../evil' del directorio central y el JAR se acepta (M27 sobrevive)
NARANJA|backend/src/main/java/io/synkra/catalogo/RecepcionArtefacto.java:87|toda IOException al abrir el ZIP (disco, interrupción) se informa como 422 ARTEFACTO_INVALIDO y pierde la causa
NARANJA|backend/src/main/java/io/synkra/catalogo/RecepcionArtefacto.java:108-113|unidades " MB", " KB" y " bytes" del motivo como literales incrustados, no como constantes fijadas
AMARILLO|backend/src/main/java/io/synkra/catalogo/RecepcionArtefacto.java:78|cabecera local o ZIP antepuesto distintos del directorio central se aceptan (candidata)
AMARILLO|backend/src/test/java/io/synkra/catalogo/RecepcionArtefactoTest.java|ninguna prueba ata la pasada única del hash (M05 sobrevive)
AMARILLO|backend/src/main/java/io/synkra/catalogo/RecepcionArtefacto.java:60-66|se leen hasta 8 KB más allá del límite + 1
AMARILLO|backend/src/main/java/io/synkra/catalogo/RecepcionArtefacto.java:42|.tmp con 0755 y sin tope de entradas (100.000 dan unos 780 ms y 82 MB)
INFORME: revisiones/U04-T04/ronda-1.md
```
