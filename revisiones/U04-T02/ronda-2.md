# Ronda 2 — U04-T02

VEREDICTO: VERDE

Corrí yo los cuatro criterios y los cuatro pasan. F-01 a F-06 quedan cerrados con evidencia propia y no queda ningún ROJO ni NARANJA. Quedan tres AMARILLO, que no bloquean.

## Comprobaciones previas
- **Hash de la tarea:** `3fc7ec316c7c3e4668a753f80ad3d87ce7809b09`. Coincide.
- **HEAD:** `61f2f35`. La base de fusión con `origin/main` es `c4a033f`.
- **Commits desde la ronda 1:** `9d70a76` (el informe de la ronda 1), `1394b5b` (el retrabajo) y `61f2f35` (la evidencia de CA-4).
- **Worktree:** `git status --short` salió vacío antes y después. Todas las sondas y mutaciones se hicieron en copias `git archive HEAD` del scratchpad.

## Criterios de aceptación, verificados por mí
| # | Criterio | Comando que corrí (desde la raíz del worktree) | Resultado |
|---|---|---|---|
| 1 | R-C1 y R-C2 sin caché ni saltos | `./gradlew :backend:test --rerun --tests '*RegistrarCoreTest'` y después el `grep` del XML | pasa: `BUILD SUCCESSFUL`, `tests="15" skipped="0" failures="0" errors="0"`. La corrida empezó a las 11:32:45 y el XML es de las 11:32:47. La primera prueba tarda 0,48 s por el contexto y las demás entre 4 y 95 ms, con SQLite real. |
| 2 | La validación usa U01 | los dos `grep` del criterio | pasa: `Descriptor.NOMBRE` en :50, `Version.parsear` en :54 y :99, y se imprime `sin copia`. El juicio sobre `[0-9a-z]` está abajo. |
| 3 | Fronteras y todo el backend | `./gradlew :backend:test --rerun :backend:check :verificador:check` y el bucle sobre los XML | pasa: `BUILD SUCCESSFUL`. Los 11 XML del backend salen con `skipped=0 failures=0 errors=0`: Fronteras 3, SynkraApp 2, CatalogoRepositorios 19, InterfazCatalogo 3, RegistrarCore 15, BaseDeDatos 4, Dialecto 6, ErrorApi 13, RepositorioSqlite 4, RutaDatos 1, Salud 3. Los 12 XML de `verificador` también salen en 0. |
| 4 | Diff acotado | `git diff --stat origin/main...HEAD` | pasa: 11 archivos, 665 líneas añadidas. Todo cae en la lista: `catalogo/**`, las pruebas, la bitácora y `revisiones/U04-T02/`. En `build.gradle.kts` solo están las dos líneas ya aceptadas. |

## Hallazgos de la ronda 1, verificados
- **F-01 (imagen laxa): cerrado.** Volví a pasar mi sonda hostil con 18 casos más (`r2/sonda.log`).
  - Se rechazan todos los casos que antes pasaban: dos tags, `registro:1:2`, tag en un segmento intermedio, tag vacío, `../../etc/passwd`, mayúsculas en registro o repo, `rëpo`, NBSP, U+2028, U+0085, NUL y 5000 caracteres.
  - También se rechazan `\r\n` al final, hex en mayúsculas, `g` en el digest, `[::1]:5000`, `registro..local`, `registro.local./`, `registro:/` y los tags que empiezan por `-` o por `.`.
  - Los límites están en su sitio: con 255 caracteres se acepta y con 256 se rechaza; un tag de 128 se acepta y uno de 129 se rechaza.
  - Se aceptan `registro.local:5000/…:2.0.0`, `localhost/repo` y el tag `V1.0`.
- **F-02 (mutantes que sobrevivían): cerrado.** Corrí 28 mutantes con `./gradlew :backend:test --rerun` (`r2/mutaciones.txt`).
  - Ahora caen los 8 que sobrevivían en la ronda 1: M02, M03, M04, M19, M06, M14, M13 y M20.
  - Siguen cayendo los demás de la ronda 1: M01, M05, M07 a M12, M15, M16 y M18.
  - De los 8 nuevos caen 6: M22 (sin límite de longitud), M24 y M25 (mayúsculas en registro o repo), M26 (motivo de U01 sustituido), M27 (`MotivosCatalogo` público) y M28 (varios tags).
  - Sobreviven M17, que es equivalente, y M21 y M23, que van en F-07.
- **F-03 (fronteras): cerrado.** Con M20 fallan `ningunTipoPublicoFueraDeApi` y `laInterfazSinNombreNoExponeTipos`; con M27 también falla `InterfazCatalogoTest`. El paquete base de `catalogo` queda atado.
- **F-04 (motivos incrustados): cerrado.**
  - Los textos están en `MotivosCatalogo` y `losMotivosSonLosTextosFijados` los fija; M13 cae.
  - El motivo de `version` ahora sale del `Motivo` de U01; si se sustituye, M26 cae.
- **F-05 (cast sin comprobar): cerrado.** `leerVersion` usa un `switch` y lanza `IllegalStateException` con el core y el texto.
- **F-06 (evidencia de CA-4): cerrado.** El comando y su salida están en la bitácora. El conteo de la bitácora (648 líneas) es anterior a la línea que la propia bitácora añade, y lo dice. Mi corrida da 665.
- **Rollback y carrera, repetidos:**
  - Si falla el insert de la versión, quedan `cat_core(nuevo)=0` y `versiones=0`.
  - 40 pares de altas simultáneas dan `{ok=40, 409=40}` y ningún 500.

## El juego de la expresión y CA-2
Confirmo la lectura del orquestador: se cumple la intención de CA-2.
- El nombre se valida con `Descriptor.NOMBRE`. Que la llamada está viva lo prueba M05: con `find()` en su lugar, la suite cae.
- `REPO` (`[0-9a-z]+([._-][0-9a-z]+)*`) y `REGISTRO` son gramáticas distintas, con otros separadores y el puerto, para otro campo. Es la expresión propia que pide la decisión 1, no una copia del patrón de nombre.
- Escribir `[0-9a-z]` para esquivar el `grep` es un rodeo de la letra del criterio, no de su intención, y el codificador lo declaró.
- El efecto colateral es que el `grep` de CA-2 ya no detecta esa clase. Va como tarea candidata, no como hallazgo contra el codificador.

## ArchUnit
No hay ninguna dependencia nueva. `dependencyInsight --configuration testCompileClasspath` muestra la cadena `com.tngtech.archunit:archunit:1.4.2` ← `spring-modulith-core:1.4.14` ← `spring-modulith-test` y `spring-modulith-docs` ← `spring-modulith-starter-test`, que ya estaba en `testImplementation`. `FronterasModulosTest` ya importaba `com.tngtech.archunit` antes de este diff. En `compileClasspath` no aparece: «No dependencies matching».

## Hallazgos nuevos

### F-07 · AMARILLO · `CatalogoServicio.java:26-29` · dos bordes de longitud sin prueba
- M21 (`> IMAGEN_MAX` cambiado por `>=`) sobrevive. No hay ninguna prueba exactamente en 255 y 256; mi sonda los comprueba y el código está bien.
- M23 (`{0,127}` cambiado por `*`) sobrevive. Un tag de 129 caracteres con un repo corto cabe en 255, y nada lo prueba.
- El 255 es una decisión del codificador que el PRD no pide. Coincide con el máximo de la gramática de referencia de distribution. Conviene que el orquestador lo ratifique.

### F-08 · AMARILLO · `CatalogoServicio.java:27` · rechazos y aceptaciones de más
- `REPO` rechaza `mi--repo` y `mi__repo`, que la gramática de distribution sí admite. Es un falso negativo sobre imágenes reales.
- `REGISTRO` acepta el puerto `99999999`.
- Ninguno de los dos rompe R-C1.

### F-09 · AMARILLO · `CatalogoServicio.java:60` · el motivo de `version` devuelve la entrada cruda
Esto viene de mi propia exigencia en F-04: reutilizar el motivo de U01. Ese motivo incluye el texto recibido sin recortar. Una versión de 3000 caracteres devuelve un motivo de unos 3200, y `"\0\n<script>"` vuelve tal cual en el error. JSON lo escapa, pero el tamaño no tiene límite. El tope de cuerpo de U04-T03 lo acotará.

## Tareas candidatas (defectos reales fuera de alcance)
- El `grep` de CA-2 depende del orden `[a-z0-9]` y ya no detecta `[0-9a-z]`. En tareas futuras conviene vigilar la copia comparando con `Descriptor.NOMBRE.pattern()` o con una mutación.
- `Version.parsear` (U01) devuelve en el motivo el texto recibido sin recortar.
- Siguen en pie las de la ronda 1:
  - `Descriptor.NOMBRE` no limita la longitud: un nombre de 300 caracteres se acepta.
  - C-31 sigue abierta para el caso de varios procesos.
  - `verificador` aparece como módulo de Modulith.

## Rutas de transcripciones largas
Todas en `/tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/u04t02/r2/`:
- `ca1.log` y `ca3.log`
- `archunit.log`
- `sonda.log`: la sonda está en `sonda/backend/src/test/java/io/synkra/catalogo/SondaHostilTest.java`.
- `mutaciones.txt`, `mut-M*.log`, `mutar.sh` y `correr.sh`. Las copias están en `mut/`.

```
VEREDICTO: VERDE
AMARILLO|backend/src/main/java/io/synkra/catalogo/CatalogoServicio.java:26-29|bordes 255/256 y tag 128/129 sin prueba (M21 y M23 sobreviven); límite 255 por ratificar
AMARILLO|backend/src/main/java/io/synkra/catalogo/CatalogoServicio.java:27|REPO rechaza mi--repo/mi__repo válidos; REGISTRO acepta el puerto 99999999
AMARILLO|backend/src/main/java/io/synkra/catalogo/CatalogoServicio.java:60|el motivo de version devuelve la entrada cruda sin límite de longitud
INFORME: revisiones/U04-T02/ronda-2.md
```
