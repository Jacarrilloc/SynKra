# Ronda 1 — U02-T03

VEREDICTO: NO-VERDE

Base `36f0cd3`, rama `m6r/U02-T03` en `f229484` (código en `4b1171d`). Hash de la tarea `1a8b3683…` coincide. El worktree quedó limpio después de la revisión: `git status --short` sale vacío y solo aparece `.gradle/`, que está ignorado.

## Criterios de aceptación, verificados por mí
| # | Criterio | Comando que corrí | Resultado |
|---|---|---|---|
| 1 | Plugins aislados | `./gradlew :core:test --rerun --tests '*CargadorPluginsTest'` + grep del XML | pasa: `BUILD SUCCESSFUL`, `tests="7" skipped="0" failures="0" errors="0"`. Tiempos por caso entre 0.001 y 0.428 s: hay compilación real con `javax.tools`, ningún caso se saltó |
| 2 | El resto del build pasa | `./gradlew :plugin-api:test --rerun :plugin-api:check :core:test --rerun :core:check` + bucle sobre los XML | pasa: `BUILD SUCCESSFUL`. Core: `tests="7" …0…0…0` y `tests="4" …0…0…0`. plugin-api: `tests="4" …0…0…0` |
| 3 | Diff acotado | `git rev-parse --short HEAD; git diff --stat 36f0cd3...HEAD` | pasa: `f229484`, 5 archivos, todos en `core/**` o `bitacoras/U02-T03.md` |

C-71: ninguna prueba nueva lee `SYNKRA_CORE_VERSION`. No hacía falta una tarea `Test` aparte.

## Mutaciones (copias en el scratchpad, el worktree no se tocó)
| Mutación | Resultado |
|---|---|
| M1a: un cargador compartido estático, con `addURL` por cada JAR | muerta (3 fallos: aislamiento, `Comun`, cierre) |
| M1b: un cargador compartido, el primero que se crea | muerta (3) |
| M1c: un cargador compartido por instancia de `CargadorPlugins` | muerta (3) |
| M2a: padre = cargador de plataforma | muerta (4) |
| M2d: padre = `null` (bootstrap) | muerta (4) |
| M2e: padre sin `plugin-api`, con `plugin-api` duplicado en el cargador hijo | muerta (3; `synkraPluginEsElMismoClass…` lo detecta) |
| M2b: padre = `ClassLoader.getSystemClassLoader()` | **sobrevive** (ver F-05) |
| M2c: padre = cargador de contexto del hilo | **sobrevive** (ver F-05) |
| M3a: no cerrar en `catch (RuntimeException)` | muerta (1) |
| M3c: `cerrar` sin efecto | muerta (1) |
| M3b: no cerrar en `catch (ServiceConfigurationError)` | **sobrevive** (ver F-01) |

Logs: `/tmp/claude-1000/-home-casdav-Projects-Master-SynKra/5b13ba94-befc-454f-88e7-3182922701e5/scratchpad/mut/*.log`

## Rojo primero, ¿basta como par falla-pasa?
Sí. Lo reproduje: quité las tres clases de una copia y volví a correr el comando. Sale `24 errors`, `BUILD FAILED`, todos `cannot find symbol`. Coincide con la bitácora. Que el rojo sea de compilación es lo esperable cuando la prueba va antes que unas clases que todavía no existen. El recorte con `tail -25` está declarado y no oculta nada. Las mutaciones de arriba muestran que las aserciones fallan por la razón correcta.

Hay un detalle inexacto: «cada uno repetido por los tres símbolos». El conteo real es 22 para `CargadorPlugins`, 14 para `ErrorCargaPlugin` y 10 para `PluginCargado`. Es AMARILLO y va en F-06.

## Hallazgos

### F-01 · NARANJA · core/src/main/java/io/synkra/core/CargadorPlugins.java:33-52 · El cargador no se cierra si la carga falla por un `LinkageError`, y la ruta de `ServiceConfigurationError` no tiene prueba
La decisión 3 dice que el cargador se cierra si la carga falla. Es una clase de defecto con dos casos:

- **(a) Defecto real, demostrado sobre el código sin modificar.** Armé un JAR cuyo proveedor `PluginL extends Base`, con `Base` fuera del JAR: es una dependencia que falta en un compilado subido. `ServiceLoader` deja escapar un `NoClassDefFoundError` sin envolverlo. Ese error no es `RuntimeException` ni `ServiceConfigurationError`, así que no lo atrapa ningún `catch`. Salida de la prueba sonda: `SONDA tipo=java.lang.NoClassDefFoundError msg=Base cerrado=false`. Pasan dos cosas: el cargador queda abierto, y lo que sale no es un error propio del core que nombre el JAR (decisión 2). La mutación M2a muestra lo mismo: allí también escapa un `NoClassDefFoundError` sin envolver.
- **(b) Falta cobertura.** La rama `catch (ServiceConfigurationError)` no tiene ninguna prueba, por eso M3b sobrevive. Una sonda con `META-INF/services` que nombra `NoExiste` pasa con el código actual y mata a M3b. Así que la prueba es barata.

Qué se pide:
- Cerrar en todas las salidas que no son éxito, por ejemplo con `try`/`finally` y una bandera de éxito, o atrapando `Throwable`.
- Envolver `LinkageError` en `ErrorCargaPlugin` con el nombre del JAR.
- Dos pruebas: proveedor inexistente y clase faltante. Las dos deben comprobar el cierre y el nombre del JAR.

### F-02 · NARANJA · CargadorPlugins.java:35 · `ServiceLoader` también cuenta los proveedores del cargador padre
`ServiceLoader.load(SynkraPlugin.class, cargador)` delega primero en el padre. Lo probé en una copia: registré un `SynkraPlugin` en el classpath de prueba del core. Resultados:
- 5 de 7 pruebas fallan.
- `a.jar` da `El JAR a.jar contiene 2 implementaciones de SynkraPlugin; se espera una`, un error que culpa al JAR equivocado.
- Peor: `vacio.jar` **devuelve un plugin**, el del classpath, en vez de fallar (`Expected ErrorCargaPlugin to be thrown, but nothing was thrown`).

Eso rompe la decisión 2: el conteo y el plugin devuelto tienen que ser los del JAR. Hoy no hay ningún proveedor en el classpath del core, así que ningún criterio cae. Pero el defecto queda latente en el componente que está en alcance.

Arreglo propuesto: `ServiceLoader.load(...).stream()`, quedarse solo con los proveedores cuyo `p.type().getClassLoader() == cargador`, y añadir una prueba con un proveedor en el padre.

### F-03 · NARANJA · bitacoras/U02-T03.md, sección Verde (CA-2) · La evidencia de CA-2 no está pegada tal cual
CA-2 pide «Se pega cada línea tal cual». La bitácora muestra `grep -o '...'`, con el patrón elidido, y líneas con el prefijo `TEST-io.synkra.core.….xml:`. `grep -o` sobre un solo archivo no imprime el nombre del archivo, así que esas líneas no salieron del comando, se editaron a mano. Los números coinciden con mi ejecución, pero el formato no es el de la salida real. Además falta la línea de `plugin-api`, que también es parte de CA-2. Hay que pegar la salida literal.

### F-04 · AMARILLO · CargadorPlugins.java:55-58 · `crearCargador` es `protected` y su Javadoc lo anuncia como «Punto de extensión»
`CargadorPlugins` es pública y no es `final`. Una subclase en cualquier paquete puede ignorar `padre`, o devolver un cargador compartido, y saltarse la decisión 1. Es exactamente lo que hacen M1 y M2 en forma de override. La prueba vive en el mismo paquete (`io.synkra.core`), así que basta con hacer el método package-private y quitar «Punto de extensión» del Javadoc. Así la costura de prueba queda sin abrir una vía de extensión pública. No es una frontera de seguridad: por eso es AMARILLO.

### F-05 · AMARILLO · CargadorPluginsTest.java:101-108 · El padre equivocado no se distingue en el entorno de prueba
M2b (cargador del sistema) y M2c (cargador de contexto) sobreviven. Bajo Gradle son el mismo objeto que `SynkraPlugin.class.getClassLoader()`, así que son mutantes equivalentes en ese entorno. En el fat-jar de Spring Boot no lo son: el cargador del sistema no ve `BOOT-INF/lib/plugin-api.jar`. La implementación está bien, porque usa `SynkraPlugin.class.getClassLoader()`. Lo anoto como límite de la prueba, no como defecto. Si se quiere cerrar, haría falta una prueba que cargue el core en un cargador aislado.

### F-06 · AMARILLO · varios · Detalles menores
- `CargadorPluginsTest.java:60`: el parámetro `valorComun` de `plugin(...)` no se usa.
- Bitácora: el resumen del rojo dice «repetido por los tres símbolos», y el conteo real es 22/14/10. Además hay dos encabezados `### CA-3`, uno solo de remisión.
- `java.util.Map` aparece calificado en cada uso en vez de importarse.

## Juicio de diseño pedido
- **`crearCargador` protegido:** abre una vía de extensión innecesaria (F-04). Con package-private alcanza.
- **Padre, decisión 1:** correcto. `SynkraPlugin.class.getClassLoader()` es el cargador de `plugin-api` en cualquier empaquetado. `synkraPluginEsElMismoClassEnCoreYPlugin` mata la duplicación del tipo (M2e) y los padres que no lo ven (M2a, M2d). Queda abierta la delegación hacia los proveedores del padre (F-02).
- **Cierre tras fallo:** correcto en las rutas de «ninguna» y «más de una» implementación. Falla con `LinkageError` y la rama de `ServiceConfigurationError` no tiene prueba (F-01).

## Tareas candidatas (defectos reales fuera de alcance)
- Ninguna nueva.

## Rutas de transcripciones largas
- `/tmp/claude-1000/-home-casdav-Projects-Master-SynKra/5b13ba94-befc-454f-88e7-3182922701e5/scratchpad/ca1.txt`
- `/tmp/claude-1000/-home-casdav-Projects-Master-SynKra/5b13ba94-befc-454f-88e7-3182922701e5/scratchpad/ca2.txt`
- `/tmp/claude-1000/-home-casdav-Projects-Master-SynKra/5b13ba94-befc-454f-88e7-3182922701e5/scratchpad/mut/` (copias mutadas, `rojo.log`, `S_clase_faltante.log`, `S_padre_con_proveedor.log`, `S_base.log`, `S_M3b_sin_cerrar_sce.log`)
- Scripts de mutación y sondas: `/tmp/claude-1000/-home-casdav-Projects-Master-SynKra/5b13ba94-befc-454f-88e7-3182922701e5/scratchpad/mutar.py`, `/tmp/claude-1000/-home-casdav-Projects-Master-SynKra/5b13ba94-befc-454f-88e7-3182922701e5/scratchpad/sonda.py`, `/tmp/claude-1000/-home-casdav-Projects-Master-SynKra/5b13ba94-befc-454f-88e7-3182922701e5/scratchpad/sonda2.py`

```
VEREDICTO: NO-VERDE
NARANJA|core/src/main/java/io/synkra/core/CargadorPlugins.java:33-52|El cargador no se cierra si la carga falla por un LinkageError, y la ruta de ServiceConfigurationError no tiene prueba
NARANJA|core/src/main/java/io/synkra/core/CargadorPlugins.java:35|ServiceLoader también cuenta los proveedores del cargador padre
NARANJA|bitacoras/U02-T03.md (Verde, CA-2)|La evidencia de CA-2 no está pegada tal cual
AMARILLO|core/src/main/java/io/synkra/core/CargadorPlugins.java:55-58|crearCargador es protected y su Javadoc lo anuncia como punto de extensión
AMARILLO|core/src/test/java/io/synkra/core/CargadorPluginsTest.java:101-108|El padre equivocado no se distingue en el entorno de prueba
AMARILLO|varios|Detalles menores
INFORME: revisiones/U02-T03/ronda-1.md
```
