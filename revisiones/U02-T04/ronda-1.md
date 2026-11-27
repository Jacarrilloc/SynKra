# Ronda 1 — U02-T04

VEREDICTO: NO-VERDE

Los cuatro criterios de aceptación pasan cuando los corro yo. El NO-VERDE se debe a dos hallazgos NARANJA (F-01 y F-02). No hay ningún ROJO.

Datos de partida comprobados por mí:
- `git hash-object tareas/U02-T04-arranque-plugins.md` da `9d9a38de6e6e8a5f85ed37a56cc4bc49767d1bb5`, igual al esperado.
- HEAD está en `cfd660e`.
- El diff `ceb914c...HEAD` toca 6 archivos: `core/**` y la bitácora. No toca `verificador/`, `plugin-api/` ni `backend/`.
- Al terminar, el worktree está limpio (`git status --short` da 0 líneas). Solo quedan cachés ignoradas (`.gradle/`, `.jqwik-database`, `build/`).
- No usé ningún clúster.

## Criterios de aceptación, verificados por mí
| # | Criterio | Comando que corrí | Resultado |
|---|---|---|---|
| 1 | Arranque verifica y activa según U01 | `./gradlew :core:test --rerun --tests '*ArranquePluginsTest'` y el grep del XML | pasa: `BUILD SUCCESSFUL`, `tests="7" skipped="0" failures="0" errors="0"`. Cada caso tarda entre 0,22 y 1,65 s, así que de verdad arranca contextos (no se saltaron) |
| 2 | El JAR real del core sale con código distinto de 0 ante S4 | `:core:bootJar`; JAR `kyc-pe-2.0.0` armado a mano con `javac`/`jar` (`coreCompatible: 3.x`); `java -jar core-2.1.0.jar --server.port=18081 ...` | pasa: `salida=1`, 2 líneas con `VERSION_INCOMPATIBLE`, y la línea ERROR nombra `kyc-pe`. **Control positivo**: el mismo JAR con `2.x` arranca (`Plugins cargados: [kyc-pe]`, `Started`) y solo termina por mi `timeout 25` (salida 124). El 1 se debe al rechazo y no a otra falla |
| 3 | El resto del build sigue pasando | `:verificador:test --rerun :verificador:check :plugin-api:test --rerun :plugin-api:check :core:test --rerun :core:check` | pasa: `BUILD SUCCESSFUL in 1m 5s`. core 7+10+4, verificador 178, plugin-api 4. Todo `skipped="0" failures="0" errors="0"` |
| 4 | El diff queda acotado | `git rev-parse --short HEAD; git diff --stat origin/main...HEAD` | pasa contra la base declarada (`ceb914c`): `cfd660e`, 6 archivos, todos en `core/**` o la bitácora. Tal como está escrito (contra `origin/main`) también salen `aidlc-docs/audit.md`, `loop/README.md` y `tareas/*`, pero vienen del commit del orquestador `ceb914c`, no del codificador. Ver F-03 |

Transcripciones: `/tmp/claude-0/-home-user/311bec3f-8f32-5470-935c-db8e1e6d6a2d/scratchpad/ca1.txt`, `.../ca3.txt`, `.../boot.txt`. Los logs de CA-2 están en `.../tmp.4BjovmAWuV/log.txt` y `log-ok.txt`.

## Los tres puntos que pidió el orquestador

- **`CargadorPlugins` pasó a `@Component`: aceptable.** Es un cambio de una línea dentro de `core/**`, que ninguna línea de «fuera» prohíbe. Sirve para inyectar el cargador y para que la prueba lo sustituya con un `@Primary` que lleva la cuenta, y así leer de vuelta qué JAR se cargaron (S12). No cambia la delegación padre primero (C-74). Los usos con `new CargadorPlugins()` en `CargadorPluginsTest` siguen pasando (10/10). Lo único es estético: la anotación va con su nombre completo en lugar de un `import` (AMARILLO, F-05).
- **`snakeyaml-engine` añadido al core: justificado.** La dependencia del `verificador` es `implementation`, así que el core no la ve al compilar, y tocar `verificador/` está fuera de alcance. Pero el lector que se escribió no es «el lector seguro de U01» que pide la Decisión 2: es una copia parcial con literales repetidos (F-02). La versión `3.2` también está repetida en dos `build.gradle.kts` (F-04).
- **Rojo contra un stub: evidencia válida, y la reproduje.** En una copia en mi scratchpad (`git archive HEAD`, sin tocar el worktree) cambié `ArranquePlugins` por un stub `@Component` cuyo `cargados()` devuelve una lista vacía. Resultado: `7 tests completed, 6 failed`. Las seis fallan por `AssertionFailedError` en las líneas 113, 123, 131, 142, 160 y 169, no por compilación ni por contexto. Era la única manera de tener un rojo que no fuera de compilación. La bitácora no pegaba las causas de los fallos; ahora están confirmadas (`.../scratchpad/rojo.txt`).

## Hallazgos

### F-01 · NARANJA · `core/src/test/java/io/synkra/core/ArranquePluginsTest.java:155-160` · La prueba de la etiqueta de tipo no distingue la causa
El YAML de prueba es `- !!java.util.ArrayList [x]` y la única comprobación es `assertThrows(RuntimeException.class, ...)`.
- Si el lector aceptara la etiqueta, la entrada sería la lista `["x"]`, que no es un `Map`. El chequeo de forma (`ArranquePlugins.java:203-206`) lanzaría `ArranqueRechazado` igual, y la prueba seguiría en verde.
- También pasaría con cualquier otra falla del contexto.
- En mi corrida el motivo real sí fue la etiqueta (`could not determine a constructor for the tag tag:yaml.org,2002:java.util.ArrayList`), pero la prueba no lo comprueba.

La propiedad de seguridad que pide CA-1 («se rechaza por la etiqueta») queda sin evidencia que la discrimine. Hace falta:
- leer de vuelta el motivo (el mensaje de `ArranqueRechazado` o el log, como S4 y S6), y
- usar un documento que, sin la etiqueta, sí fuera válido, por ejemplo `- !!java.util.HashMap {nombre: kyc-co, version: 1.0.0}`.

### F-02 · NARANJA · `core/src/main/java/io/synkra/core/ArranquePlugins.java:45,47,190` · Literales incrustados en puntos de decisión, que repiten y se apartan del lector de U01
Es un barrido de clase; todas las apariciones son del diff:
- `CORE = "synkra-core"` (l. 45): decide qué core devuelve el catálogo, el nombre de la `Seleccion` y el de los descriptores provisionales. Aparece en los logs y en los motivos. Ninguna fuente de producción lo define; el diff lo introduce.
- `MAX_BYTES = 64 * 1024` (l. 47): copia de `LectorDescriptor.MAX_BYTES` y visible para el usuario en el mensaje «supera el tamaño máximo de 65536 bytes».
- `setMaxAliasesForCollections(10)` (l. 190): copia de `LectorDescriptor.MAX_ALIAS`, en un literal sin nombre.
- Además, la copia se aparta del lector de U01. `LectorDescriptor` también aplica `MAX_PROFUNDIDAD = 32` y `etiquetaExplicitaNoStr`, y `ArranquePlugins` no aplica ninguno de los dos. La Decisión 2 dice «se lee con el lector seguro de U01»; lo que hay es otro lector, menos estricto.

Sin tocar `verificador/`, se puede arreglar de dos maneras:
- derivar los valores de una sola fuente del core, propiedad o constante documentada, e igualar las salvaguardas (profundidad y etiquetas explícitas);
- o anotar la divergencia como candidata y que el orquestador lo decida.

### F-03 · AMARILLO · CA-4 (especificación) · El comando del criterio usa `origin/main`, que no es la base de la tarea
Contra `origin/main`, el `--stat` incluye el commit del orquestador `ceb914c` (`tareas/*`, `audit.md`, `loop/README.md`). El codificador lo anotó y usó otra base. No es un defecto del codificador; es para el orquestador.

### F-04 · AMARILLO · `core/build.gradle.kts:21` · Versión de `snakeyaml-engine` repetida
`"3.2"` aparece también en `verificador/build.gradle.kts:53`. Si las dos se desalinean, el core y U01 leerían YAML con motores distintos.

### F-05 · AMARILLO · `CargadorPlugins.java:19` · La anotación va con su nombre completo
`@org.springframework.stereotype.Component` en lugar de un `import`, a diferencia de `VersionCore` y `ArranquePlugins`.

### F-06 · AMARILLO · `ArranquePluginsTest.java:124,132` · El nombre del plugin no se ata al motivo
`contains("kyc-pe")` y `contains("kyc-mx")` buscan el nombre en todo el log, sin ligarlo a la línea del motivo. Bastaría con comprobar `"VERSION_INCOMPATIBLE plugin=kyc-pe"`.

## Tareas candidatas (defectos reales fuera de alcance)
- Exponer desde `verificador/` un cargador de YAML seguro y público (esquema, tamaño, alias, profundidad, etiquetas) para que el core lo reutilice en lugar de copiarlo. Así se cierra de raíz F-02 sin repetir límites.
- Centralizar las versiones de dependencias compartidas (catálogo de versiones de Gradle) para `snakeyaml-engine` y otras.

## Rutas de transcripciones largas
- `/tmp/claude-0/-home-user/311bec3f-8f32-5470-935c-db8e1e6d6a2d/scratchpad/ca1.txt`
- `/tmp/claude-0/-home-user/311bec3f-8f32-5470-935c-db8e1e6d6a2d/scratchpad/ca3.txt`
- `/tmp/claude-0/-home-user/311bec3f-8f32-5470-935c-db8e1e6d6a2d/scratchpad/boot.txt`
- `/tmp/claude-0/-home-user/311bec3f-8f32-5470-935c-db8e1e6d6a2d/scratchpad/tmp.4BjovmAWuV/log.txt` (CA-2, S4) y `log-ok.txt` (control positivo)
- `/tmp/claude-0/-home-user/311bec3f-8f32-5470-935c-db8e1e6d6a2d/scratchpad/rojo.txt` (rojo reproducido con el stub, en `.../scratchpad/copia-rojo/`)

```
VEREDICTO: NO-VERDE
NARANJA|core/src/test/java/io/synkra/core/ArranquePluginsTest.java:155-160|La prueba de la etiqueta de tipo no distingue la causa del rechazo
NARANJA|core/src/main/java/io/synkra/core/ArranquePlugins.java:45,47,190|Literales incrustados (CORE, 64 KB, 10 alias) que repiten y se apartan del lector seguro de U01
INFORME: revisiones/U02-T04/ronda-1.md
```
