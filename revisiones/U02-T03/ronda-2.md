# Ronda 2 — U02-T03

VEREDICTO: VERDE

Base `36f0cd3`, rama `m6r/U02-T03` en `3e2ae8e`, con el código en `ba7727d`. El hash de la tarea, `1a8b3683…`, coincide. Entre `ba7727d` y `3e2ae8e` solo cambian la bitácora y `revisiones/U02-T03/ronda-1-respuesta.md`. Al terminar, `git status --short` sale vacío. Solo quedan `build/`, `*/build/` y `.gradle/`, que están ignorados. No toqué el `stash@{0}` antiguo, que es de U04-T07.

## Criterios de aceptación, verificados por mí
| # | Criterio | Comando que corrí | Resultado |
|---|---|---|---|
| 1 | Plugins aislados | `./gradlew :core:test --rerun --tests '*CargadorPluginsTest'` + grep del XML | pasa: `BUILD SUCCESSFUL`, `tests="10" skipped="0" failures="0" errors="0"`. Tiempos por caso entre 0.001 y 0.457 s, con compilación real en `@TempDir`. Están las cinco aserciones que pide el criterio, más tres pruebas nuevas y la de JAR inexistente |
| 2 | El resto del build pasa | `./gradlew :plugin-api:test --rerun :plugin-api:check :core:test --rerun :core:check` + el bucle literal del criterio | pasa: `BUILD SUCCESSFUL`. Core da `tests="10" skipped="0" failures="0" errors="0"` y `tests="4" skipped="0" failures="0" errors="0"`. plugin-api (`ContratoPluginTest`) da `tests="4" skipped="0" failures="0" errors="0"` |
| 3 | Diff acotado | `git rev-parse --short HEAD; git diff --stat 36f0cd3...HEAD` | pasa: `3e2ae8e`, 7 archivos y 530 inserciones. Todos caen en `core/**`, `bitacoras/U02-T03.md` o `revisiones/U02-T03/`. Una nota: `origin/main` ya avanzó hasta `7393a0b`, que queda por detrás de la base. Por eso el comando literal `origin/main...HEAD` también lista `aidlc-docs/audit.md`, `loop/README.md` y `tareas/*`. Esos archivos vienen del commit del orquestador `36f0cd3` ("Tareas U05-T03 y U02-T03; ola 29"), no del codificador |

C-71: ninguna línea del diff en `core/` lee `SYNKRA_CORE_VERSION`, ni usa `getenv` ni `sleep`.

## Estado de los hallazgos de la ronda 1
- **F-01 (NARANJA): cerrado.** El `try`/`finally` con bandera de éxito cubre toda salida que no sea éxito, y `ServiceConfigurationError` y `LinkageError` se envuelven en `ErrorCargaPlugin`. Las pruebas `proveedorInexistente…` y `claseFaltante…` matan a M3b, M3b2, M5 y M6.
- **F-02 (NARANJA): cerrado.** El filtro `p.type().getClassLoader() == cargador` funciona. Además, `p.get()` solo se llama sobre los proveedores filtrados, así que los del padre no se instancian (esto lo comprobé leyendo el código). `ignoraLosProveedoresDelCargadorPadre` mata a M4.
- **F-03 (NARANJA): cerrado.** La evidencia de CA-2 está pegada literal con `grep -Ho`. El cambio de bucle está declarado e incluye `plugin-api`. Los números coinciden con mi ejecución.
- **F-04 y F-06 (AMARILLO): cerrados.**
- **F-05 (AMARILLO): sigue igual**, anotado en la bitácora como límite de la prueba (ver F-03 abajo).

## Sondas (copias en el scratchpad, el worktree no se tocó)
| Sonda | Resultado |
|---|---|
| Clase faltante, variantes nuevas: interfaz propia ausente, dependencia que falta en el constructor (falla en `p.get()`), inicializador estático que lanza, y `Base` ausente por el `cargar(Path)` público | las 4 pasan (`tests="14"…0…0…0`). Todas dan `ErrorCargaPlugin` con el nombre del JAR, y en todas el cargador queda cerrado. Por la vía pública sale `El JAR sinbase2.jar no se pudo cargar: java.lang.NoClassDefFoundError: Base` |
| Proveedor en el padre: repetí la sonda de la ronda 1, con un `SynkraPlugin` registrado en el classpath de prueba del core y la vía pública | pasa (`10…0…0…0`). En la ronda 1 fallaban 5 de 7 |
| Control: la misma sonda combinada con M4 (sin filtro) | 6 fallos, entre ellos `jarSinImplementacion…: nothing was thrown`. Reproduce el defecto original, así que la sonda tiene dientes |

## Mutaciones sobre `ba7727d`
| Mutación | Resultado |
|---|---|
| M1a, M1b, M1c: cargador compartido (estático con `addURL`, el primero que se crea, uno por instancia) | muertas: 6, 6 y 5 fallos |
| M2a: padre de plataforma; M2d: padre `null`; M2e: `plugin-api` duplicada; M2f: la costura ignora `padre` | muertas: 3, 3, 4 y 4 |
| M2b: cargador del sistema; M2c: cargador de contexto | sobreviven. Son equivalentes bajo Gradle (F-05 de la ronda 1, sin cambios) |
| M3a: no cerrar con «ninguna» implementación | muerta (1) |
| **M3b**: no cerrar en `ServiceConfigurationError` | **muerta** (1, `proveedorInexistente…`) |
| M3b2: no cerrar en `LinkageError` | muerta (1, `claseFaltante…`) |
| M3c: `close()` nunca se ejecuta | muerta (3) |
| M4: sin filtro de cargador | muerta (1) |
| M5: no envolver `LinkageError`; M6: no envolver `ServiceConfigurationError` | muertas (1 y 1) |
| M3a2: no cerrar con «más de una» implementación | **sobrevive** (ver F-01) |

## Rojo filtrado con `grep -E`: ¿es un par falla-pasa honesto?
Sí. Lo reconstruí en una copia con las pruebas de `ba7727d` y la lógica de `4b1171d`, añadiendo solo las dos costuras de paquete. El resultado es `BUILD FAILED` con `tests="10" skipped="0" failures="2" errors="0"`. Fallan `claseFaltante…`, con `NoClassDefFoundError` en la línea 163, e `ignoraLos…`, con «nothing was thrown» en la línea 197. La salida filtrada sale **idéntica, línea a línea**, a la que está pegada en la bitácora.

Comparé el filtro contra la salida completa. Lo único que quita son las líneas de tareas de Gradle, `10 tests completed, 2 failed`, una línea `Caused by: ClassNotFoundException` y el bloque genérico de `FAILURE`. No oculta ningún fallo ni ningún error de compilación. Los conteos del XML confirman los dos fallos. El verde de `ba7727d` es mi CA-1. El par se sostiene.

## Juicio sobre la costura `cargar(Path, ClassLoader)` de paquete
No abre ninguna vía que la decisión 1 prohíba:
- Cada llamada sigue creando su propio `URLClassLoader`, así que el aislamiento no se toca. Lo único que la costura deja elegir es el padre.
- Solo se llega a ella desde `io.synkra.core`. Un JAR de plugin no puede llamarla aunque declare ese mismo paquete: su cargador es otro, así que es otro paquete en tiempo de ejecución y el acceso de paquete falla. Esto lo razoné, no lo probé con una sonda.
- La única llamada en producción es la delegación desde `cargar(Path)`, que usa `SynkraPlugin.class.getClassLoader()`. M2a y M2d prueban que ese padre está fijado.
- Queda un riesgo latente: código futuro del mismo paquete (U02-T04) podría pasar otro padre. Va en F-02 como AMARILLO.

## Hallazgos
### F-01 · AMARILLO · CargadorPluginsTest.java:127-134 · No se comprueba el cierre en la ruta de «más de una implementación»
M3a2 sobrevive: si se fija `exito = true` en la rama `size() > 1`, el cargador queda abierto y nada falla. El criterio se cumple, porque se comprueba el cierre tras un fallo y en tres rutas distintas. Además, el `finally` único hace que el cierre sea estructural para todas las salidas. Bastaría con usar `afirmarFalla(jar, "doble.jar")` en `jarConDosImplementaciones…`.

### F-02 · AMARILLO · CargadorPlugins.java:25-26 · La costura deja elegir el padre a cualquier clase del paquete
Es correcta y está justificada para probar F-02 de la ronda 1. Sugiero reforzar el Javadoc («solo para pruebas; producción usa `cargar(Path)`») para que U02-T04 no la use.

### F-03 · AMARILLO · CargadorPluginsTest.java:101-108 · Sigue F-05 de la ronda 1
M2b y M2c son equivalentes bajo Gradle. Ya está declarado en la bitácora como límite de la prueba.

## Tareas candidatas (defectos reales fuera de alcance)
- La delegación del cargador es padre primero. Si una clase del JAR del plugin, incluido el proveedor, tiene el mismo nombre que una clase visible desde el cargador de la aplicación, gana la del padre. Si es el proveedor, el error dice «no contiene ninguna implementación», que confunde. En el fat-jar de Spring Boot el padre ve toda la aplicación. Esto lo deduje leyendo el código y no lo probé con una sonda. Decidir si hace falta delegar primero al hijo, o detectar ese caso, es una pregunta de diseño para U02-T04 o T09.

## Rutas de transcripciones largas
- `/tmp/claude-1000/-home-casdav-Projects-Master-SynKra/5b13ba94-befc-454f-88e7-3182922701e5/scratchpad/r2/ca1.txt`
- `/tmp/claude-1000/-home-casdav-Projects-Master-SynKra/5b13ba94-befc-454f-88e7-3182922701e5/scratchpad/r2/ca2.txt`
- `/tmp/claude-1000/-home-casdav-Projects-Master-SynKra/5b13ba94-befc-454f-88e7-3182922701e5/scratchpad/r2/mutaciones.txt`
- `/tmp/claude-1000/-home-casdav-Projects-Master-SynKra/5b13ba94-befc-454f-88e7-3182922701e5/scratchpad/r2/sondas.txt`
- `/tmp/claude-1000/-home-casdav-Projects-Master-SynKra/5b13ba94-befc-454f-88e7-3182922701e5/scratchpad/r2/log/` (un log por mutación o sonda, más `rojo2_completo.log` y `rojo2_filtrado.log`)
- `/tmp/claude-1000/-home-casdav-Projects-Master-SynKra/5b13ba94-befc-454f-88e7-3182922701e5/scratchpad/r2/scripts/` (`mutar2.py`, `correr.sh`, `fallos.py`, `sonda_faltante.py`, `sonda_padre.py`)
- `/tmp/claude-1000/-home-casdav-Projects-Master-SynKra/5b13ba94-befc-454f-88e7-3182922701e5/scratchpad/r2/copias/` (copias mutadas y la del rojo reconstruido)

```
VEREDICTO: VERDE
AMARILLO|core/src/test/java/io/synkra/core/CargadorPluginsTest.java:127-134|No se comprueba el cierre en la ruta de más de una implementación
AMARILLO|core/src/main/java/io/synkra/core/CargadorPlugins.java:25-26|La costura deja elegir el padre a cualquier clase del paquete
AMARILLO|core/src/test/java/io/synkra/core/CargadorPluginsTest.java:101-108|Sigue F-05 de la ronda 1
INFORME: revisiones/U02-T03/ronda-2.md
```
