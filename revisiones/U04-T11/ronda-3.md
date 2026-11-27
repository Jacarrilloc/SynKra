# Ronda 3 — U04-T11

VEREDICTO: VERDE

Corrí yo los cuatro criterios y todos pasan. F-07 está cerrado: el filtro de `ningunTipoPublicoFueraDeApi` ahora usa `resideInAPackage("io.synkra.catalogo.api..")`. Con eso, M8 (`catalogo.apix`) da rojo, una variante `catalogo.apis` también da rojo y M9 (`catalogo.api.sub`) sigue en verde. M0 a M3 dan lo mismo que la bitácora de la ronda 3, línea por línea. No queda ningún ROJO ni NARANJA abierto.

Comprobaciones previas:
- Tarea: el hash `5582e7a5d8e42032ac4130c7d60fc5b92f8cbe50` coincide.
- HEAD es 4afd9ef, el sha reportado.
- Desde 374b8ce hay dos commits. e88d651 cambia una sola línea de `InterfazCatalogoTest.java:33`. 4afd9ef solo añade la bitácora y `ronda-2-respuesta.md`.

## Criterios de aceptación, verificados por mí
| # | Criterio | Comando que corrí | Resultado |
|---|---|---|---|
| 1 | Las fronteras pasan sin caché | `./gradlew :backend:test --rerun --tests '*FronterasModulosTest' --tests '*InterfazCatalogoTest'` + grep de los XML | pasa. `BUILD SUCCESSFUL in 3s`. FronterasModulosTest: tests=5, time=1.763 s. InterfazCatalogoTest: tests=6, time=0.513 s. Las dos tienen skipped=0, failures=0 y errors=0. Por los tiempos, las pruebas corrieron de verdad. Los 11 `testcase` cubren cada regla de la decisión 1, más C-36 y el control `verifyFalla...` |
| 2 | Las reglas detectan las tres mutaciones | Copia con `git archive 4afd9ef` en el scratchpad (`r3copia/`) y la orden de CA-1 tras cada mutación (`r3-mut.sh`) | pasa. M0: verde. M1: 2/11 rojas. M2: 3/11. M3: 4/11. Son las mismas pruebas que nombra la bitácora en la ronda 3 |
| 3 | Todo el backend pasa | `./gradlew :backend:test --rerun :backend:check` + bucle de XML | pasa. `BUILD SUCCESSFUL in 15s`. 22 XML y 306 pruebas, sin saltos, fallos ni errores en ningún XML. Comparé con `diff` mis 22 líneas con el bloque CA-3 de la ronda 3 de la bitácora: **idénticas** |
| 4 | El diff queda acotado | `git diff --stat origin/main...HEAD` | pasa. 16 archivos, 439+/13−, todos dentro del alcance: pruebas de fronteras, fixtures de C-14 y las pruebas que los cargan, `SynkraApp.java` por C-36 (ya aceptado en las rondas 1 y 2), `CANDIDATAS.md`, la bitácora y `revisiones/U04-T11/`. El `--stat` de la bitácora (15 archivos, 346+/13−) es exactamente `origin/main...e88d651`. La bitácora no puede contar su propio commit, y por eso la diferencia está explicada |

### Mutaciones repetidas en la copia (sobre 4afd9ef)
| Mutación | Resultado mío | Bitácora, ronda 3 |
|---|---|---|
| M0, sin mutación | verde | verde |
| M1, `CodigosCatalogo` public | 2 rojas: `ningunTipoPublicoFueraDeApi` y `laInterfazSinNombreNoExponeTipos` | igual (la bitácora usa `RutasCores` en vez de `CodigosCatalogo`, mismas pruebas) |
| M2, `CatalogoApi` referencia un tipo interno | 3 rojas: las dos anteriores y `catalogoApiNoDependeDeTiposInternosDeCatalogo` | igual |
| M3, `ExcepcionApi` de plataforma referencia catalogo | 4 rojas: `ningunTipoPublico...`, `plataformaNoDependeDeCatalogo`, `laInterfazSinNombre...` y `elBackendRealRespetaSusFronteras` | igual |
| M8, `catalogo.apix.Interno` public | 1 roja: `ningunTipoPublicoFueraDeApi` (en la ronda 2 daba verde) | igual: rojo tras el arreglo, verde antes |
| M8b, `catalogo.apis.Interno` public (variante mía) | 1 roja: `ningunTipoPublicoFueraDeApi`. El XML nombra `io.synkra.catalogo.apis.Interno` | no consta |
| M9, `catalogo.api.sub.Interno` public | **verde**, como debe ser porque está dentro de `api..` | no se repitió, y la bitácora lo declara |

## Hallazgos
No quedan hallazgos bloqueantes.

### Seguimiento de la ronda 2
- **F-07 (NARANJA): cerrado.** El filtro de `InterfazCatalogoTest.java:33` ahora usa la semántica de paquetes de ArchUnit. M8 y M8b dan rojo y M9 sigue en verde, así que el arreglo no se pasó de restrictivo.
- **F-08 (AMARILLO): cerrado.** M0 a M3, M8, CA-1, CA-3 y CA-4 están repetidos sobre e88d651, con su salida en la bitácora. Mis ejecuciones los reproducen. La bitácora además deja constancia de que el primer intento de M1 no aplicó el `sed`, y esa transparencia es correcta.
- F-04 (AMARILLO, ronda 1): sigue sin cambios y justificado. No bloquea.

### F-09 · AMARILLO · bitacoras/U04-T11.md, ronda 3 · M9 no se repitió tras el cambio de predicado
Era la contraprueba de que el arreglo no excluye de más, y la bitácora la da por supuesta «por construcción». La corrí yo y da verde, así que no bloquea. Conviene que una mutación de frontera traiga siempre su contraprueba ejecutada.

## Tareas candidatas (defectos reales fuera de alcance)
- Ninguna nueva. Siguen en pie las de la ronda 1 (F-02 y F-04): endurecer las reglas cuando lleguen U06 o U07.

## Estado del worktree
Quedó limpio después de la revisión: `git status --short` vacío y HEAD en 4afd9ef. Todas las mutaciones se hicieron en `r3copia/` del scratchpad, no en el worktree del codificador. `git worktree list` no muestra ningún worktree temporal huérfano del script del codificador. No toqué el `stash@{0}`.

## Rutas de transcripciones largas
Todas están en `/tmp/claude-1000/-home-casdav-Projects-Master-SynKra/5b13ba94-befc-454f-88e7-3182922701e5/scratchpad/`:
- `r3-ca1.log`: CA-1
- `r3-ca3.log`: CA-3
- `r3-ca3.cnt`: mis conteos de CA-3, uno por XML
- `r3-bit-ca3.cnt`: los conteos de la bitácora (ronda 3) con los que los comparé
- `r3-mut.sh`: el script de mutaciones
- `r3-mut-resumen.txt`: resumen de las mutaciones
- `r3-mut-<nombre>.log`: una salida por mutación
- `r3copia/`: la copia

```
VEREDICTO: VERDE
AMARILLO|bitacoras/U04-T11.md, ronda 3|M9 (catalogo.api.sub) no se repitió tras el arreglo; el revisor lo corrió y está verde
INFORME: revisiones/U04-T11/ronda-3.md
```
