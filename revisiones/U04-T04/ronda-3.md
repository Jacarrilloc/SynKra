# Ronda 3 — U04-T04

VEREDICTO: VERDE

El trabajo quedó completo en `6dcb817`, pese al corte del codificador: el código, las pruebas, la respuesta y la bitácora están en el commit. Corrí yo los cuatro criterios y los cuatro pasan. F-08, F-09, F-10, F-11 y F-12 quedan cerrados con evidencia propia. No queda ningún ROJO ni NARANJA; quedan cuatro AMARILLO que no bloquean. Fusiona el humano.

## Comprobaciones previas
- **Hash de la tarea:** `a2b1e767b55d3f5456311029b2e0d25c6cdc2e1e`. Coincide.
- **HEAD:** `6dcb817`. Los commits desde la ronda 2 son `89ca918` (el informe) y `6dcb817` (el retrabajo).
- **Worktree:** `git status --short` salió vacío antes y después.
- **Copias:** las sondas y las mutaciones se hicieron en copias `git archive HEAD` del scratchpad.

## Criterios de aceptación, verificados por mí
| # | Criterio | Comando que corrí | Resultado |
|---|---|---|---|
| 1 | Recepción, sin caché y sin saltos | `./gradlew :backend:test --rerun --tests '*RecepcionArtefactoTest'` + `grep` del XML | pasa: `BUILD SUCCESSFUL`, `tests="59" skipped="0" failures="0" errors="0"`. El timestamp del XML (13:27:57Z) es de mi corrida. La prueba que lanza una JVM aparte con `-Xmx256m` tarda 0,28 s y de verdad lanza el proceso. |
| 2 | Hash en la misma pasada | el `grep` de CA-2 | pasa: hay un solo `DigestInputStream`, en `RecepcionArtefacto.java:95`. M05 ahora cae (F-11). |
| 3 | Todo el backend y las fronteras | `./gradlew :backend:test --rerun :backend:check` + bucle sobre los XML | pasa: 15 XML y 159 pruebas, todas con `skipped=0 failures=0 errors=0`. `InterfazCatalogoTest` 3/3 y `FronterasModulosTest` 3/3. |
| 4 | Diff acotado | `git diff --stat origin/main...HEAD` | pasa: 12 archivos, todos en la lista. `EstructuraZip` ya no existe. Las tres propiedades de `application.properties` están autorizadas. La bitácora no trae la salida (F-16). |

## Los hallazgos de la ronda 2
- **F-08: cerrado.** Las cuatro sondas de ZIP ocultos dan 422 `RUTA_NO_PERMITIDA` con `ENTRADAS_NO_COINCIDEN`:
  - `solo_locales_eocd_cero` y `comentario_eocd_falso_malo`;
  - `oculta_sin_bit3` y `oculta_bit3_dd`.

  `local_metodo_distinto` da 422 `ARTEFACTO_INVALIDO`. También se rechazan `mm_local_malo`, `pre_zipmalo_luego_ok`, `cd_vacio_local_malo`, `mismo_offset` y `solapadas` (`sonda.log`, con las 64 sondas de las tres rondas).
- **F-09: cerrado.**
  - Un fuzz de 150.000 mutaciones sobre 16 bases, incluidas las sondas hostiles, da 17.020 aceptados, 112.748 `ARTEFACTO_INVALIDO` y 20.232 `RUTA_NO_PERMITIDA`. No hay **ninguna excepción fuera de `ExcepcionApi`** y no queda ningún temporal (`fuzz3.log`).
  - Es diferencial: en los aceptados, `JarInputStream`, `ZipArchiveInputStream` y `java.util.zip.ZipFile` ven los mismos nombres (0 discrepancias).
  - `e64_cdoff_negativo` da 422.
- **F-10: cerrado.** `eocd_miente_20mb`, con 446.201 entradas reales y un EOCD que dice 1, se ejecuta con `-Xmx256m`: 422 `DEMASIADAS_ENTRADAS` en 107 ms, con un pico de unos 56 MB de heap (`memoria.log`). En la ronda 2 la JVM moría por OOM. `e64_entradas_negativas` da 422.
- **F-11: cerrado.** M05, con exactamente el mismo texto que en la ronda 2, cae en `f11_elSha256DevueltoSaleDelDigestDeLaCopia`.
- **F-12: cerrado.** Su código ya no existe, y sus casos pasaron a pruebas de caja negra que caen con C01, C13 y C14.
- **La bomba de 1 GiB:** ahora da 422 «El contenido descomprimido supera el límite» en 108 ms. C04, C05 y C16 caen.
- **JAR legítimos:** de 130 JAR reales, se aceptan los 130 (`legitimos.log`):
  - 129 de la caché de Gradle, entre ellos `sqlite-jdbc` (14 MB, 169 ms), `byte-buddy` (9 MB), `archunit` y `tomcat-embed-core`;
  - y el `bootJar` de Spring del propio backend (45 MB, con el límite subido a 100 MB en la sonda, 84 ms).

## Hallazgos
### F-13 · AMARILLO · `RecepcionArtefacto.java:124,137` · la comparación con commons y el orden de las listas no están atados
- **C02** (sin comparar la lista de commons con la del flujo) sobrevive. El codificador ya lo declaró como redundante: si el flujo coincide con la JDK, commons coincide en todo lo que probé. Pero las reglas de enlace y de 0x7075 recorren la lista de commons, así que ninguna prueba ata que sea la misma.
- **C03** (comparar conjuntos en vez de listas) sobrevive.
- **C21** (`distinct()` sobre la lista de la JDK) sobrevive.
- Ninguna prueba usa nombres duplicados ni un orden distinto entre el directorio central y las cabeceras locales.

### F-14 · AMARILLO · `RecepcionArtefacto.java:212-214` · la rama `isInterrupted()` no tiene prueba
- C11 sobrevive.
- Es casi equivalente: una interrupción real sobre `FileChannel` siempre llega como `ClosedByInterruptException`, que ya se reconoce en la cadena de causas.

### F-15 · AMARILLO · `RecepcionArtefacto.java:96-104` · el mutante sin corte cuelga en vez de fallar
- M02 (sin la comprobación del límite) entra en un bucle de `read(…, 0)` y solo lo para el timeout.
- El código real no cuelga, porque siempre corta antes. Pero una prueba con `@Timeout` lo convertiría en un fallo limpio.

### F-16 · AMARILLO · `bitacoras/U04-T04.md`, sección «Ronda 3» · CA-4 sin salida, y evidencia que no se puede reproducir
- CA-4 dice «ver git diff --stat» sin pegar la salida: es una afirmación sin evidencia. Mi corrida la confirma.
- El fuzz de 23.200 casos y el `mut.py` se hicieron con clases ya borradas, así que no se pueden reproducir. Mis corridas los sustituyen.

## Mutaciones (54 mutantes, `RecepcionArtefactoTest` con 59 pruebas en cada corrida; `mutaciones.txt`)
- **Caen 47:**
  - las de la ronda 1, de M01 a M30, menos M02;
  - U01, L01 y P01;
  - C01, C04 a C10, C12 a C20.
- **Cuelga 1:** M02 da timeout a los 180 s (F-15).
- **Sobreviven 6:**
  - U02: equivalente, porque commons ya usa UTF-8 por defecto;
  - C02, C03 y C21 (F-13);
  - C11 (F-14).

## Tareas candidatas (defectos reales fuera de alcance)
- **Nueva, para U04-T06:** las entradas con nombre duplicado se aceptan. En `duplicado.zip`, con dos `META-INF/synkra.json`, `ZipFile.getEntry` lee `{"core":"B"}` y el lector en flujo ve A y luego B (`duplicado.log`). Al leer el descriptor, U04-T06 debería rechazar los duplicados.
- **Siguen en pie:** los enlaces solo en `AsiExtraField` y las entradas FIFO o de dispositivo. Info-ZIP los extrae como archivos regulares.

## Rutas de transcripciones largas
Todas en `/tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/u04t04/r3/`:
- `ca1.log` y `ca3.log`
- `sonda.log`: las 64 sondas, que están en `todos/`.
- `fuzz3.log`, `memoria.log`, `legitimos.log` (con `jars-legitimos.txt`) y `duplicado.log`.
- Las sondas `SondaR3*Test.java`, en `sonda/backend/src/test/java/io/synkra/catalogo/`.
- `mutaciones.txt`, `mut-progreso.txt`, `mut-*.log` y `scripts/mut3.py`.

```
VEREDICTO: VERDE
AMARILLO|backend/src/main/java/io/synkra/catalogo/RecepcionArtefacto.java:124,137|la comparación con commons, el orden y los duplicados no están atados (C02, C03 y C21 sobreviven)
AMARILLO|backend/src/main/java/io/synkra/catalogo/RecepcionArtefacto.java:212-214|la rama isInterrupted() no tiene prueba (C11 sobrevive; casi equivalente)
AMARILLO|backend/src/main/java/io/synkra/catalogo/RecepcionArtefacto.java:96-104|el mutante sin corte (M02) cuelga en vez de fallar; falta un @Timeout
AMARILLO|bitacoras/U04-T04.md|CA-4 sin salida en la bitácora; el fuzz y las mutaciones del codificador no se pueden reproducir (los sustituyen mis corridas)
INFORME: revisiones/U04-T04/ronda-3.md
```
