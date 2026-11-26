# Ronda 2 — U03-T05

VEREDICTO: NO-VERDE

Revisé el worktree `/home/user/wt-m6r-U03-T05` en la rama `m6r/U03-T05`, con HEAD `f478dc6`. El diff de la tarea es `b18d6e3...HEAD` y el de esta ronda es `77abb01..f478dc6`. El `git hash-object` de la tarea da `ad26b382…` y coincide con el despacho. Las mutaciones las hice solo en una copia sacada con `git archive`, en mi scratchpad. Al terminar, `git status --short` en el worktree sale vacío, sin ningún cambio.

## Criterios de aceptación, verificados por mí
| # | Criterio | Comando que corrí | Resultado |
|---|---|---|---|
| 1 | La prueba de fronteras pasa sin caché | `./gradlew :backend:test --rerun --tests '*FronterasModulosTest'` | pasa, `BUILD SUCCESSFUL`. El XML tiene timestamp 02:20:17Z y la corrida empezó a las 02:20:15Z |
| 2 | Lectura de vuelta del reporte | `grep -ho 'tests=…' …TEST-*FronterasModulosTest.xml` | pasa, `tests="3" skipped="0" failures="0" errors="0"`. Tiempos 1.44 s, 0.039 s y 0.061 s |
| 3 | La documentación de módulos se genera | antes borré `backend/build/spring-modulith-docs` y después corrí `ls` y `grep -l plataforma` | pasa. Se regeneran `components.puml` y `module-plataforma.adoc`, y el `.adoc` nombra `plataforma` |
| 4 | Modulith no llega a runtime | `./gradlew :backend:dependencies --configuration runtimeClasspath \| grep -ci modulith` | pasa, `0` |
| 5 | Todo el backend pasa sin caché | `./gradlew :backend:check --rerun` | pasa, `BUILD SUCCESSFUL`. Son 7 XML con 32 tests, todos con skipped=0, failures=0 y errors=0. `SynkraAppTest` ahora tiene 2 tests |

## Verificación de las respuestas de la ronda 1

**F-01, corregido en lo que se pidió.** Releí los beans del contexto real con una sonda `@SpringBootTest(classes = SynkraApp.class)` que filtra por el tipo de cada bean, no por su nombre. Encontró 366 beans; los de `io.synkra*` son `synkraApp`, `controladorDePrueba`, `baseDeDatos`, `manejadorErrores`, `jdbcDialect` y `elementoRepositorio`. Ya no aparece `appFronteras` ni ninguna otra clase de `io.synkra.fronteras`. También reproduje el rojo: con el `AppFronteras` de `acfead5`, el que tenía `@SpringBootApplication`, `laAppDePruebaDeFronterasNoEntraAlContextoReal()` falla (5 tests, 1 fallo) y el XML nombra `appFronteras`. Con `@Modulithic`, la app de prueba sigue sirviendo de raíz: CA-2 pasa. Queda un defecto en la prueba de regresión que se agregó, descrito en F-01 abajo.

**F-02, corregido.** Corrí el `sed` que pega la bitácora, que quita el import y el campo de `Uno.java`, y da `verifyFallaCuandoUnModuloImportaUnPaqueteInternoDeOtro() FAILED`, 3 tests con 1 fallo. El XML trae `Expecting code to raise a throwable`, igual que en la bitácora.

**F-03, corregido.** El comentario ahora va encima de `var modulos = …` (`FronterasModulosTest.java:39-41`).

**F-04, respuesta razonada y la acepto.** Se agregó un comentario en `FronterasModulosTest.java:31-32` que explica de qué depende la ruta. No es un valor visible para el usuario y el arbitraje dejaba este punto a criterio del codificador.

**Desborde de alcance: ninguno.** La línea de «Dentro» no nombra `SynkraAppTest.java`, pero el único cambio en ese archivo es una prueba que vigila que el fixture de esta tarea no se mezcle con los módulos reales. Eso es justo lo que pide el plan de pruebas, y responde a F-01 de la ronda 1. No toca nada de la lista de «Fuera»: no hay cambios en `src/main` ni en `build.gradle.kts`, y Modulith sigue solo en pruebas.

## Hallazgos

### F-01 · NARANJA · backend/src/test/java/io/synkra/SynkraAppTest.java:34 · La regresión filtra por el nombre del bean y no detecta la fuga que su nombre promete vigilar
La prueba `laAppDePruebaDeFronterasNoEntraAlContextoReal` afirma `noneMatch(n -> n.toLowerCase().contains("fronteras"))` sobre los nombres de los beans. Ese filtro solo detectó la fuga original porque el bean se llamaba `appFronteras`. El riesgo que motivó F-01 en la ronda 1 era que *alguien anote una clase del fixture y entre al contexto real*, y ese riesgo sigue en pie: `@Modulithic` le quitó a `AppFronteras` su propio escaneo, pero `SynkraApp` sigue escaneando todo `io.synkra`, y ahí está incluido `io.synkra.fronteras`.

Lo demostré en la copia anotando `Uno` con `@Component`:
```
SONDA-BEAN uno -> io.synkra.fronteras.uno.Uno
SynkraAppTest: tests="2" skipped="0" failures="0" errors="0"   (BUILD SUCCESSFUL)
```
Una clase del fixture queda en el contexto real y la prueba que dice vigilarlo sigue en verde. Es cobertura ausente sobre justo el caso que el nombre de la prueba promete. La corrección cabe dentro del alcance porque toca solo pruebas: filtrar por el paquete del tipo de cada bean, por ejemplo con `getBeanFactory().getType(n)` y comprobar si el paquete empieza por `io.synkra.fronteras`, y no por su nombre. Tiene que volver a mostrar el rojo con esta misma mutación.

## Tareas candidatas (defectos reales fuera de alcance)
- `components.puml` sigue sin componentes, porque hoy hay un solo módulo. Viene de la ronda 1 y aplica cuando llegue U04.
- `io.synkra.plataforma.prueba.ControladorDePrueba` y `ElementoRepositorio` son fixtures de prueba de otras tareas y también entran al contexto real de todos los `@SpringBootTest`. Son de la misma clase que F-01, pero ajenos a esta tarea. Puede ser intencional, porque `ErrorApiTest` los usa; conviene decidirlo de forma explícita.

## Rutas de transcripciones largas
- /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/u03t05r2/ca1.txt
- /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/u03t05r2/ca5.txt
- /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/u03t05r2/sonda.txt (beans del contexto real, por tipo)
- /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/u03t05r2/mut-uno.txt (`@Component` en `Uno`: fuga presente y regresión en verde)
- /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/u03t05r2/rojo-f01.txt (rojo de F-01 reproducido)
- /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/u03t05r2/mut-f02.txt (mutación de la bitácora reproducida)
- Copia mutada: /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/u03t05r2/copia

No tengo herramientas de escritura, así que no creé `revisiones/U03-T05/ronda-2.md`. Este mensaje es el contenido del informe, para que el orquestador lo guarde en esa ruta.

```
VEREDICTO: NO-VERDE
NARANJA|backend/src/test/java/io/synkra/SynkraAppTest.java:34|La regresión filtra por el nombre del bean y no detecta la fuga que su nombre promete vigilar
INFORME: revisiones/U03-T05/ronda-2.md
```
