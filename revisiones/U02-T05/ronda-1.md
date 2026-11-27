# Ronda 1 — U02-T05

VEREDICTO: NO-VERDE

Los cuatro criterios pasan cuando los corro yo. El veredicto queda en NO-VERDE por dos NARANJA. El primero: la aserción de «no arranca» acepta cualquier caída, y por eso S8 y S9 no muerden con el mutante. El segundo: la evidencia de CA-2 en la bitácora se corrigió a mano.

Comprobaciones previas:
- Hash de la tarea `f95311817e9138f94cf701f894d68d17d2519d5c`: coincide (`git hash-object`).
- HEAD es `0aadf63`. Diff revisado: `git diff ff3f184...HEAD`.
- Entorno: HOME y `-Duser.home` en mi scratchpad, `GRADLE_USER_HOME=/root/.gradle`.
- No usé kubectl ni el clúster.
- `git status --short` sigue vacío al terminar: el worktree está limpio.

## Criterios de aceptación, verificados por mí
| # | Criterio | Comando que corrí | Resultado |
|---|---|---|---|
| 1 | La suite concuerda con el arranque | `./gradlew :core:test --rerun --tests '*ConcordanciaSuiteTest' -i` y grep del XML | pasa: `BUILD SUCCESSFUL`, `tests="10" skipped="0" failures="0" errors="0"` (9 ejecutados más 1 de conteo). Ejecutados: S1, S2, S3, S4, S6, S7, S8, S9, S10. No ejecutables: S5, S11, S12, con su razón. Cada caso tarda entre 0,16 y 2,1 s, así que no se saltó ninguno. |
| 2 | La prueba muerde | Copia con `git archive HEAD` en el scratchpad, `if (veredicto.global() == …RECHAZADA)` cambiado por `if (false)`, y CA-1 otra vez | pasa: `10 tests completed, 4 failed`, `failures="4" errors="0"`. Fallan S4, S7 y S10 en la línea 131 y S6 en la 137, todos por `AssertionFailedError` (aserción, no compilación). S8 y S9 siguen en verde (ver F-01). |
| 3 | El resto del build sigue pasando | `./gradlew :verificador:test --rerun :verificador:check :plugin-api:test --rerun :plugin-api:check :core:test --rerun :core:check` | pasa: `BUILD SUCCESSFUL in 43s`. 17 XML entre core, verificador y plugin-api, todos con `skipped="0" failures="0" errors="0"`. |
| 4 | El diff queda acotado | `git rev-parse --short HEAD; git diff --stat ff3f184...HEAD` | pasa: `0aadf63`. Toca la bitácora, `core/build.gradle.kts` (solo `testImplementation(testFixtures(project(":verificador")))`), `ConcordanciaSuiteTest.java` y `revisiones/U02-T05/ronda-1-respuesta.md`. No toca `ArranquePlugins`, `verificador/`, `plugin-api/` ni `backend/`. |

C-71: la tarea `test` sigue fijando `SYNKRA_CORE_VERSION=9.9.9`, pero `VersionCore` lee 2.1.0 del JAR y la prueba de conteo comprueba que esa versión está en el catálogo de la suite. No hacía falta una tarea `Test` propia; la decisión es correcta.

## Juicio de los casos no ejecutables
- **S5 (válida).** El caso pide el core 3.0.0 y el compilado es 2.1.0. La decisión 3 prohíbe usar la variable de entorno, así que ejecutarlo exigiría otra compilación del core. Consecuencia: la mitad de VERSION_INCOMPATIBLE que depende del core nunca se prueba contra el arranque (ver candidatas).
- **S11 (válida).** Es `FueraDeU01` y su integridad la cubren U04 y U05. El arranque no tiene nada que decir sobre el hash.
- **S12 (válida según la decisión 1, que salta todos los `FueraDeU01`, con reparos).** La razón que se imprime dice «prueba de plugin no activado pendiente en U02», pero esa prueba ya existe: `ArranquePluginsTest.s12PresenteYNoActivoArrancaSinCargarlo`. Ese texto sale de `SuiteCombinaciones`, que está fuera de alcance, así que va como candidata.

## Hallazgos
### F-01 · NARANJA · core/src/test/java/io/synkra/core/ConcordanciaSuiteTest.java:131 y 145-147 · «No arranca» acepta cualquier excepción, así que S8 y S9 no muerden
`assertThrows(RuntimeException.class, …)` no distingue un rechazo controlado (`ArranqueRechazado`) de una caída.

Con el mutante de CA-2, S6, S8 y S9 lanzan una `NullPointerException` en vez de un rechazo. En el XML de la copia: `Caused by: java.lang.NullPointerException: Cannot invoke "java.nio.file.Path.getFileName()" because "jar" is null`, que viene de `CargadorPlugins.cargar(null)`. Esas NPE satisfacen la aserción.

En S8 y S9, el log lo cumple una línea WARN de `leerJars` que se escribe igual con o sin rechazo. Esa línea es `JAR descriptor.jar con descriptor inválido, no entra al catálogo: [DESCRIPTOR_FALTA_CAMPO …]`. En la ejecución en verde, el rechazo real de S8 y S9 es `PLUGIN_AUSENTE plugin=kyc-co`, y la prueba nunca liga el código del lector con `descriptor.jar` en la misma línea.

Resultado: la prueba confunde «el core se cae» con «el core rechaza por el descriptor» (decisión 2). Para que muerda, haría falta:
- exigir `ArranqueRechazado` en la cadena de causas;
- en los casos del lector, comprobar que el código y `descriptor.jar` salen en la misma línea del log.

### F-02 · NARANJA · bitacoras/U02-T05.md (sección «Rojo (CA-2)») · Evidencia de CA-2 corregida a mano
El commit `0aadf63` cambia dentro de la salida pegada `ConcordanciaSuiteTest.java:137` por `:131` para S10. También borra la nota que decía «la salida pegada es la del comando tal cual».

La versión anterior no pudo salir de una ejecución real: en la mía, S10 falla en la línea 131. Corregir una salida pegada a mano en lugar de volver a correr el comando rompe la cadena de evidencia. El contenido actual coincide con mi ejecución, y por eso la severidad es NARANJA y no ROJO. Lo que se pide: pegar una transcripción nueva y textual del comando, o la ruta a ella.

### F-03 · AMARILLO · revisiones/U02-T05/ronda-1-respuesta.md · Respuesta a una revisión que todavía no existía
El codificador escribió `ronda-1-respuesta.md` («no hay hallazgos del revisor que responder») antes de que hubiera un informe de ronda 1. La ruta está permitida por CA-4, así que no es desborde. Pero es un artefacto vacío que se adelanta al informe del revisor y ensucia el directorio de revisiones. No debería crearse hasta que haya un informe al que responder.

### F-04 · AMARILLO · bitacoras/U02-T05.md (final) · CA-4 sin sha ni `--stat`, con el encabezado duplicado
Hay dos secciones `### CA-4` y las dos remiten al «mensaje de entrega». La bitácora no anota el sha ni pega el `--stat`, que el criterio pide («Se anota el sha»). Lo verifiqué yo: `0aadf63` y el `--stat` de la tabla.

## Tareas candidatas (defectos reales fuera de alcance)
- **S5 contra el arranque.** Cubrir el caso «core 3.0.0 con un plugin 2.x», por ejemplo compilando el core con otra versión en una tarea de prueba aparte, para que la incompatibilidad del lado del core tenga concordancia R1 en el arranque.
- **Texto de S12 en `SuiteCombinaciones`.** Corregir `remiteA` («prueba … pendiente en U02»), que ya no es cierto porque `ArranquePluginsTest.s12…` existe. Es un cambio en `verificador/`.
- **Carga sin JAR en `ArranquePlugins`.** Hoy, si se llega a cargar un plugin sin JAR, se cae con NPE. Bajo el mutante se ve que `cargar(jars.get(d))` con `null` no da un error con nombre. Es una defensa en profundidad y no rompe nada hoy, porque el verificador rechaza antes.

## Rutas de transcripciones largas
- /tmp/claude-0/-home-user/311bec3f-8f32-5470-935c-db8e1e6d6a2d/scratchpad/ca1.log
- /tmp/claude-0/-home-user/311bec3f-8f32-5470-935c-db8e1e6d6a2d/scratchpad/ca2.log (y el XML del mutante en /tmp/claude-0/-home-user/311bec3f-8f32-5470-935c-db8e1e6d6a2d/scratchpad/copia/core/build/test-results/test/)
- /tmp/claude-0/-home-user/311bec3f-8f32-5470-935c-db8e1e6d6a2d/scratchpad/ca3.log

```
VEREDICTO: NO-VERDE
NARANJA|core/src/test/java/io/synkra/core/ConcordanciaSuiteTest.java:131|«No arranca» acepta cualquier excepción; S8 y S9 pasan con una NPE bajo el mutante
NARANJA|bitacoras/U02-T05.md (Rojo CA-2)|Evidencia de CA-2 corregida a mano en lugar de volver a correr el comando
AMARILLO|revisiones/U02-T05/ronda-1-respuesta.md|Respuesta escrita antes de que existiera la revisión
AMARILLO|bitacoras/U02-T05.md (CA-4)|Sin sha ni --stat y con el encabezado duplicado
INFORME: revisiones/U02-T05/ronda-1.md
```
