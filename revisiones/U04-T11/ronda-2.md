# Ronda 2 — U04-T11

VEREDICTO: NO-VERDE

Los cuatro criterios pasan en mis propias ejecuciones. La evidencia nueva de la bitácora coincide con mis salidas, XML por XML. Las mutaciones M1 a M3 dan rojo en la copia, y la mutación en el subpaquete nuevo `catalogo.x` también se detecta en las tres reglas. Solo bloquea un hallazgo, F-07. La generalización de F-06 en `ningunTipoPublicoFueraDeApi` compara prefijos de texto (`startsWith("io.synkra.catalogo.api")`). Por eso un tipo `public` en un paquete hermano como `catalogo.apix` pasa sin que ninguna prueba se ponga roja.

Tarea: hash 5582e7a5d8e42032ac4130c7d60fc5b92f8cbe50, coincide. HEAD 174b89e, coincide con el reportado. Desde la ronda 1 hay dos commits: 56292aa (código) y 174b89e (solo la bitácora y la respuesta).

## Criterios de aceptación, verificados por mí
| # | Criterio | Comando que corrí | Resultado |
|---|---|---|---|
| 1 | Las fronteras pasan sin caché | `./gradlew :backend:test --rerun --tests '*FronterasModulosTest' --tests '*InterfazCatalogoTest'` + grep de los XML | pasa. `BUILD SUCCESSFUL in 4s`. FronterasModulosTest: tests=5, InterfazCatalogoTest: tests=6, las dos con skipped=0, failures=0 y errors=0. Los tiempos son de 1,78 s y 0,69 s: corrieron de verdad. Hay una prueba por cada regla de la decisión 1, más la de C-36 |
| 2 | Las reglas detectan las tres mutaciones | Copia con `git archive 174b89e` en el scratchpad y la orden de CA-1 tras cada mutación (`r2-mut.sh`) | pasa. M0 sin mutación: verde. M1: 2/11 rojas. M2: 3/11. M3: 4/11. Coinciden línea por línea con la bitácora |
| 3 | Todo el backend pasa | `./gradlew :backend:test --rerun :backend:check` + bucle de XML | pasa. `BUILD SUCCESSFUL in 15s`, 22 XML, 306 pruebas, 0 saltos, 0 fallos y 0 errores. `diff` entre mis 22 líneas y las de la bitácora (ronda 2): **idénticas** |
| 4 | El diff queda acotado | `git diff --stat origin/main...HEAD` | pasa. 14 archivos. El `--stat` de la bitácora (13 archivos, 219+/13−) corresponde exactamente a `origin/main...56292aa`. El archivo 14 es `ronda-1-respuesta.md`, que también está en alcance (`revisiones/U04-T11/`) |

### Mutaciones de subpaquete (las que pediste para F-06)
- **M7a**: `catalogo.x.Interno` `public`. Falla `ningunTipoPublicoFueraDeApi` (1/11).
- **M7b**: `CatalogoApi` referencia `catalogo.x.Interno`. Fallan además `catalogoApiNoDependeDeTiposInternosDeCatalogo` y `ningunTipoPublicoFueraDeApi` (2/11). En el XML, la violación de ArchUnit nombra `catalogo.x.Interno`.
- **M7c**: `plataforma.ExcepcionApi` referencia `catalogo.x.Interno`. Fallan `plataformaNoDependeDeCatalogo`, `elBackendRealRespetaSusFronteras` y `ningunTipoPublicoFueraDeApi` (3/11).
- **M7d**: `Interno` package-private y `CatalogoApi` referencia un `catalogo.x.Puente` público. Falla `catalogoApiNoDependeDeTiposInternosDeCatalogo`, así que la regla de dependencia detecta el subpaquete por sí misma.
- **M8**: `catalogo.apix.Interno` `public`. **Todo en verde** (ver F-07).
- **M9**: `catalogo.api.sub.Interno` `public`. Verde, que es lo correcto: está dentro de `api..`.

## Hallazgos
### F-07 · NARANJA · backend/src/test/java/io/synkra/catalogo/InterfazCatalogoTest.java:33 · «Fuera de api» se decide por prefijo de texto
```java
.filter(c -> !c.getPackageName().startsWith("io.synkra.catalogo.api"))
```
Este filtro excluye `catalogo.api`, `catalogo.api.*` y además `catalogo.apix`, `catalogo.apiexterna` o `catalogo.apis`. La decisión 1 pide que ningún tipo de `catalogo` fuera de `api` sea `public`. La mutación M8 crea un `public class Interno` en `io.synkra.catalogo.apix` y deja las 11 pruebas en verde (`r2-mut-M8-apix-publico.log`): ni esta regla ni `laInterfazSinNombreNoExponeTipos` ni `verify()` la detectan. La respuesta a F-06 dice «todo `catalogo..` salvo `api..`». En la regla de dependencia es cierto, porque `resideInAPackage("io.synkra.catalogo.api..")` usa la semántica de paquetes de ArchUnit y no la de texto. En esta regla no es cierto. El arreglo es una línea: aplicar el mismo predicado `resideInAPackage("io.synkra.catalogo.api..")` (o `equals(api) || startsWith(api + ".")`), más la mutación M8 en la bitácora.

### F-08 · AMARILLO · bitacoras/U04-T11.md, ronda 2 · M1, M3 y CA-1 no se repitieron después de cambiar las reglas
La ronda 2 cambió el filtro de `ningunTipoPublicoFueraDeApi`, del que dependen M1 y M3. Aun así, solo volvió a correr M2. La evidencia de M1 y M3, y la de CA-1, sigue siendo la de dc7e76e. La bitácora no la presenta como nueva, y mis ejecuciones sobre 174b89e la reproducen línea por línea. Por eso no bloquea. Cuando el codificador corrija F-07, conviene repetir las tres mutaciones y CA-1 sobre el sha final.

### Seguimiento de la ronda 1
- F-01 (ROJO): **cerrado**. CA-3 tiene la salida completa y CA-4 el `--stat` real, y los dos coinciden con mis ejecuciones.
- F-02, F-03 y F-05: cerrados. Están el comentario, la nota de C-36 y `basePackageClasses`.
- F-04: sin cambio y justificado. Sigue como AMARILLO, sin bloquear.
- F-06: cerrado en la regla de dependencia y a medias en la de visibilidad (ver F-07).

## Tareas candidatas (defectos reales fuera de alcance)
- Ninguna nueva. Siguen en pie las de la ronda 1: F-02 y F-04, para endurecer las reglas cuando lleguen U06 o U07.

## Estado del worktree
Limpio después de la revisión (`git status --short` vacío, HEAD 174b89e). Todas las mutaciones se hicieron en la copia del scratchpad. No toqué el `stash@{0}`.

## Rutas de transcripciones largas
Todas en `/tmp/claude-1000/-home-casdav-Projects-Master-SynKra/5b13ba94-befc-454f-88e7-3182922701e5/scratchpad/`:
- `r2-ca1.log`, `r2-ca3.log` y `r2-ca3.cnt`: CA-1 y CA-3, con los conteos por XML
- `r2-mut.sh`: el script de mutaciones
- `r2-mut-resumen.txt`: M0 a M3. Sus líneas M7 a M9 son de un intento con un error de mi script (no compilaba) y no cuentan
- `r2-mut-resumen2.txt`: M8 y M9
- `r2-mut-resumen3.txt`: M7a a M7d
- `r2-mut-<nombre>.log`: una salida por mutación, y `r2-mut-M7b-xml.log`
- `r2copia/`: la copia

```
VEREDICTO: NO-VERDE
NARANJA|backend/src/test/java/io/synkra/catalogo/InterfazCatalogoTest.java:33|ningunTipoPublicoFueraDeApi usa startsWith y no detecta un tipo public en catalogo.apix (M8 en verde)
INFORME: revisiones/U04-T11/ronda-2.md
```
