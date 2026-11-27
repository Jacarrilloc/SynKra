# Ronda 2 — U02-T04

VEREDICTO: VERDE

Corrí yo mismo los cuatro criterios y los cuatro pasan. Los hallazgos F-01, F-02, F-05 y F-06 de la ronda 1 están cerrados, y lo comprobé ejecutando. F-03 y F-04 quedan resueltos por el arbitraje. No queda ningún ROJO ni NARANJA. Hay dos AMARILLO nuevos que no bloquean.

Datos de partida, comprobados por mí:
- `git hash-object tareas/U02-T04-arranque-plugins.md` da `9d9a38de6e6e8a5f85ed37a56cc4bc49767d1bb5`, igual al esperado.
- HEAD está en `e726d03`.
- `git diff --name-only ceb914c...HEAD` da 8 archivos: `core/**` (5), `bitacoras/U02-T04.md` y `revisiones/U02-T04/ronda-1{,-respuesta}.md`. No toca `verificador/`, `plugin-api/` ni `backend/`.
- No usé kubectl ni ningún clúster.
- Al terminar, el worktree está limpio: `git status --short` da 0 líneas.

## Criterios de aceptación, verificados por mí
| # | Criterio | Comando que corrí | Resultado |
|---|---|---|---|
| 1 | El arranque verifica y activa según U01 | `./gradlew :core:test --rerun --tests '*ArranquePluginsTest'` y el grep del XML | pasa: `BUILD SUCCESSFUL`, `tests="10" skipped="0" failures="0" errors="0"`. Cada caso tarda entre 0,115 y 2,79 s, así que levanta contextos reales y nada se saltó. Están todos los casos pedidos (S1, S4, S6, S12, sin YAML, etiqueta `!!java...`), más 4 adicionales |
| 2 | El JAR real del core sale con código distinto de 0 ante S4 | `:core:bootJar`. Armé el JAR `kyc-pe-2.0.0` a mano con `javac`/`jar` (`coreCompatible: 3.x`) y lo arranqué con `java -jar core/build/libs/core-2.1.0.jar --server.port=18081 --synkra.core.config=... --synkra.core.plugins=...` | pasa: `salida=1` y 2 líneas con `VERSION_INCOMPATIBLE`. La línea ERROR dice `Arranque rechazado: VERSION_INCOMPATIBLE plugin=kyc-pe: ... requiere el core 'synkra-core' en 3.x y se pidió 2.1.0`. **Control positivo**: el mismo JAR con `2.x` registra `Plugins cargados: [kyc-pe]` y `Started`, y solo termina por mi `timeout 30` (`salida=124`). El 1 viene del rechazo y no de otra falla |
| 3 | El resto del build sigue pasando | `:verificador:test --rerun :verificador:check :plugin-api:test --rerun :plugin-api:check :core:test --rerun :core:check` | pasa: `BUILD SUCCESSFUL in 57s`. Conteos: core 10+10+4, verificador 178 (13 clases), plugin-api 4. Todos con `skipped="0" failures="0" errors="0"` |
| 4 | El diff queda acotado | `git rev-parse --short HEAD; git diff --stat ceb914c...HEAD` (la base que fijó el arbitraje para F-03) | pasa: `e726d03`, 8 archivos, 750 inserciones, todos dentro de lo esperado |

Sobre CA-2: el codificador no lo repitió en esta ronda; la bitácora lo admite («No lo re-corrí a mano»). Ahora la evidencia es la mía y es del binario actual: el bootJar salió del árbol en `e726d03`.

## Estado de los hallazgos de la ronda 1
| Id | Estado | Cómo lo comprobé |
|---|---|---|
| F-01 · NARANJA | **Cerrado** | `yamlConEtiquetaDeTipoSeRechazaPorLaEtiqueta` (l. 169-179) usa `- !!java.util.HashMap {nombre: kyc-co, version: 1.0.0}`. Primero corre un control positivo: el mismo documento sin la etiqueta arranca y carga 1 plugin. Después lee de vuelta la causa raíz: comprueba que es `ArranqueRechazado` y que el mensaje dice exactamente `etiqueta de tipo 'tag:yaml.org,2002:java.util.HashMap' no permitida`. **Rojo reproducido** en una copia de `git archive HEAD` con el `ArranquePlugins` de `cfd660e`. Hubo que hacer visible `MAX_BYTES` solo en la copia para que compilara la prueba nueva. Resultado: `10 tests completed, 3 failed`. Esta prueba falla con `...no es válido o no es seguro...: could not determine a constructor for the tag ...java.util.Hash...`, así que ahora la prueba distingue la causa del rechazo |
| F-02 · NARANJA | **Cerrado** | `CORE`, `MAX_BYTES`, `MAX_ALIAS` y `MAX_PROFUNDIDAD` son ahora constantes con nombre y documentadas, una sola fuente en `core/` (`ArranquePlugins.java:57-67`), con los mismos valores que `LectorDescriptor.java:40-42`. La profundidad máxima de 32 se cuenta con eventos del parser antes de componer, igual que en U01. Las etiquetas explícitas que no sean la implícita se rechazan (l. 285-328). En el mismo rojo, `yamlDemasiadoAnidadoSeRechaza` y `etiquetaExplicitaQueNoEsStrSeRechaza` fallan contra el código anterior (el segundo, con `NumberFormatException` por `!!int 1.0.0`) y pasan con el actual |
| F-03 · AMARILLO | Resuelto por el arbitraje | Es un defecto de la especificación. CA-4 lo corrí contra `ceb914c` |
| F-04 · AMARILLO | Fuera de alcance | Pasó a la candidata C-78. `3.2` sigue repetida, como estaba previsto |
| F-05 · AMARILLO | **Cerrado** | `CargadorPlugins.java` importa `org.springframework.stereotype.Component` y usa `@Component` |
| F-06 · AMARILLO | **Cerrado** | Las líneas 124 y 131 de la prueba comprueban `"VERSION_INCOMPATIBLE plugin=kyc-pe"` y `"PLUGIN_AUSENTE plugin=kyc-mx"`. Cuadra con el formato del log del proceso real que vi en CA-2 |

## Hallazgos nuevos
### F-07 · AMARILLO · `core/src/main/java/io/synkra/core/ArranquePlugins.java:65,210` · El límite de alias no tiene prueba
`MAX_ALIAS = 10` decide si se rechaza un `plugins-activos.yaml`, pero ninguna prueba lo ejercita: `grep -n "MAX_ALIAS\|alias\|&a\|\*a" ArranquePluginsTest.java` no da nada. Ya existía en la ronda 1 y no está entre los casos de CA-1 ni entre las salvaguardas que pidió el arbitraje (profundidad y etiquetas), así que no bloquea. Bastaría con un caso de 11 o más alias en una colección que se rechace con un motivo leído de vuelta.

### F-08 · AMARILLO · `ArranquePlugins.java:212-226` · El YAML se analiza tres veces
Hay tres pasadas sobre el mismo texto: una con el parser para la profundidad, otra con el `Composer` para las etiquetas y otra con `Load` para el valor. Es correcto, porque las tres usan los mismos `ajustes` y el texto está acotado a 64 KB. Pero `Load` vuelve a analizar un texto que ya se compuso. Se podría construir el valor desde el nodo ya compuesto. Es solo una mejora menor.

## Tareas candidatas (defectos reales fuera de alcance)
- Ninguna nueva. Siguen en pie C-77 (exponer el lector seguro desde `verificador/` para no repetir los límites en `core/`) y C-78 (centralizar la versión de `snakeyaml-engine`).

## Rutas de transcripciones largas
- `/tmp/claude-0/-home-user/311bec3f-8f32-5470-935c-db8e1e6d6a2d/scratchpad/r2/ca1.txt`
- `/tmp/claude-0/-home-user/311bec3f-8f32-5470-935c-db8e1e6d6a2d/scratchpad/r2/ca3.txt`
- `/tmp/claude-0/-home-user/311bec3f-8f32-5470-935c-db8e1e6d6a2d/scratchpad/r2/tmp.YdtZMir2AC/log-3.x.txt` (CA-2, S4)
- `/tmp/claude-0/-home-user/311bec3f-8f32-5470-935c-db8e1e6d6a2d/scratchpad/r2/tmp.YdtZMir2AC/log-2.x.txt` (control positivo)
- `/tmp/claude-0/-home-user/311bec3f-8f32-5470-935c-db8e1e6d6a2d/scratchpad/r2/rojo.txt` (el rojo reproducido; la copia está en `/tmp/claude-0/-home-user/311bec3f-8f32-5470-935c-db8e1e6d6a2d/scratchpad/r2/copia-rojo/`)

```
VEREDICTO: VERDE
AMARILLO|core/src/main/java/io/synkra/core/ArranquePlugins.java:65,210|El límite de alias no tiene prueba
INFORME: revisiones/U02-T04/ronda-2.md
```
