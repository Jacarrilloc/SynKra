# Ronda 1 — U02-T02

VEREDICTO: NO-VERDE

Una sola cosa bloquea. Los cuatro criterios pasan al correrlos yo, D4 se cumple en el artefacto real y la versión sale de una sola fuente. Lo que falla es la prueba que dice cubrir `SYNKRA_CORE_VERSION`: no detecta una implementación que lea esa variable de entorno.

Contexto: hash de la tarea `789c96d0…` (coincide), HEAD `f145d2c` (coincide), un commit sobre `origin/main`. El worktree quedó limpio después de la revisión. Puerto 18081 libre antes y después. El `stash@{0}` (U04-T07) no es de esta tarea y no lo toqué.

## Criterios de aceptación, verificados por mí
| # | Criterio | Comando que corrí | Resultado |
|---|---|---|---|
| 1 | El JAR lleva la versión compilada | `./gradlew :core:bootJar --rerun-tasks` + `unzip -p core/build/libs/core-*.jar BOOT-INF/classes/synkra-core.properties` | pasa: `BUILD SUCCESSFUL`, `core-2.1.0.jar` (un único JAR), `version=2.1.0` |
| 2 | La versión no se cambia desde fuera | `./gradlew :core:test --rerun --tests '*VersionCoreTest'` + grep del XML | pasa en conteos: `tests="3" skipped="0" failures="0" errors="0"`. El contenido de la prueba de entorno no protege nada (F-01) |
| 3 | El core arranca y responde salud | el bloque de CA-3, en el 18081 | pasa: `UP`. `/actuator/info` da 404 (no está expuesto). Tras el `kill`, el 18081 quedó libre |
| 4 | El build sigue pasando y el diff queda acotado | el `./gradlew … --rerun … :check` completo + `git diff --stat origin/main...HEAD` | pasa: `BUILD SUCCESSFUL in 46s`. Suma de los XML: verificador 178/0 saltadas, plugin-api 4/0, backend 315/0, core 3/0, sin fallos. El diff toca 7 archivos: `core/**`, 1 línea de `settings.gradle.kts` y la bitácora. Sin desborde |

### Lo que pediste juzgar

**Decisión 2: `core` depende de `:verificador` y `VersionCore.version()` devuelve `Version`.** Es correcta.
- La decisión permite esta opción y la bitácora la deja escrita para U02-T04.
- `Version.parsear` valida con el formato R-V1. Si el recurso falta o es inválido, el bean lanza `IllegalStateException` y el contexto no arranca. Eso es lo correcto para D4.
- `implementation(project(":verificador"))` es suficiente, porque ningún otro módulo consume la API de `core`.

**La versión sale de `version` en `core/build.gradle.kts`.** Lo comprobé por mutación en una copia hecha con `git archive` en mi scratchpad, no en el worktree.
- Cambié a `version = "3.4.5"` y reconstruí: salió `core-3.4.5.jar` con `version=3.4.5`.
- En ese estado, las 3 pruebas fallan (`3 tests completed, 3 failed`). Es decir, `ESPERADA = new Version(2, 1, 0)` en la prueba sí detecta el cambio.
- `grep` de `2.1.0` en `core/src` solo encuentra esa expectativa de la prueba. No hay ningún literal repetido en `main`. El `inputs.property("version", v)` invalida la caché como debe.

**D4 en el artefacto real.** No hay endpoint que exponga la versión ni un log, así que no cambié código. Extraje `core-2.1.0.jar` en el scratchpad y lo arranqué con una sonda externa de 6 líneas. La sonda llama a `SpringApplication.run(SynkraCoreApplication.class, args)` sobre `BOOT-INF/classes` y `BOOT-INF/lib/*` del JAR, y después lee el bean. La ejecuté con `SYNKRA_CORE_VERSION=9.9.9` en el entorno y `--version=9.9.9 --synkra.core.version=9.9.9` en los argumentos:
```
SONDA env=9.9.9 prop(version)=9.9.9 VersionCore=2.1.0
```
El entorno de Spring sí ve las dos fuentes con 9.9.9, y el bean sigue en 2.1.0. D4 se cumple en el artefacto.

## Hallazgos

### F-01 · NARANJA · core/src/test/java/io/synkra/core/VersionCoreTest.java:32-36 (CA-2) · La prueba de `SYNKRA_CORE_VERSION` no detecta que alguien lea esa variable

- **Qué hace la prueba.** `ignoraLaVariableDeEntornoSynkraCoreVersion` pasa `SYNKRA_CORE_VERSION=9.9.9` como `defaultProperties` del `SpringApplicationBuilder`. En el proceso no existe ninguna variable de entorno real.
- **Por qué no protege.** La regresión que D4 quiere impedir es leer el entorno, por ejemplo con `System.getenv`. Esa regresión pasa la prueba.
- **Cómo lo demostré.** En la copia del scratchpad introduje este mutante en `VersionCore`:
  `String t = System.getenv("SYNKRA_CORE_VERSION") != null ? System.getenv("SYNKRA_CORE_VERSION") : props.getProperty("version");`
  Luego corrí `env -u SYNKRA_CORE_VERSION ./gradlew --no-daemon -q :core:test --rerun`, que es como corre CA-2. Resultado: `tests="3" skipped="0" failures="0" errors="0"`. El mutante pasa la suite.
- **La justificación de la bitácora es falsa.** La bitácora dice que la variable se inyecta como propiedad porque "no se puede fijar una variable real dentro de la JVM de prueba". Sí se puede, porque Gradle pasa el entorno a la JVM de prueba que lanza. Con el mismo mutante y `SYNKRA_CORE_VERSION=9.9.9 ./gradlew --no-daemon … :core:test --rerun` el resultado es `failures="3"`. Por la misma vía, `tasks.test { environment("SYNKRA_CORE_VERSION", "9.9.9") }` en `core/build.gradle.kts`, que está dentro del alcance, la fija de forma reproducible.
- **Qué falta.** La prueba debe correr con una variable real y comprobar como precondición que `System.getenv("SYNKRA_CORE_VERSION")` vale `9.9.9`. Sin esa precondición, si la variable no llega, la prueba pasa sin haber probado nada. También vale un proceso hijo con `ProcessBuilder` y el entorno puesto.
- **Por qué NARANJA y no ROJO.** Los conteos de CA-2 se cumplen y "en el contexto de prueba" admite la lectura de "propiedad de Spring". Aun así, el nombre de la prueba promete algo que no comprueba.

### F-02 · AMARILLO · VersionCoreTest (plan de pruebas) · La prueba de humo es implícita

El plan pide "una prueba de humo del contexto". Hoy la cubre de forma implícita `@SpringBootTest(RANDOM_PORT)` en `daLaVersionCompilada`. No hay ninguna prueba que pida `/actuator/health` dentro de la suite; eso solo lo cubre CA-3 a mano. No bloquea.

### F-03 · AMARILLO · core/build.gradle.kts:25-32 · El recurso se escribe en un `doLast` de `processResources`

Funciona y la caché es correcta. Pero si alguien pone un `synkra-core.properties` en `src/main/resources`, se pisa sin aviso. Una tarea generadora propia, cuya salida se añada a `sourceSets.main.resources`, lo dejaría explícito. No bloquea.

## Tareas candidatas (defectos reales fuera de alcance)
- Ninguna.

## Rutas de transcripciones largas
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/5b13ba94-befc-454f-88e7-3182922701e5/scratchpad/ca4.log
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/5b13ba94-befc-454f-88e7-3182922701e5/scratchpad/ca3.log
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/5b13ba94-befc-454f-88e7-3182922701e5/scratchpad/probe/Sonda.java (la sonda de D4)
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/5b13ba94-befc-454f-88e7-3182922701e5/scratchpad/copia/ (la copia con el mutante de `getenv`; está fuera del worktree)

```
VEREDICTO: NO-VERDE
NARANJA|core/src/test/java/io/synkra/core/VersionCoreTest.java:32-36 (CA-2)|La prueba de SYNKRA_CORE_VERSION no detecta que alguien lea esa variable
INFORME: revisiones/U02-T02/ronda-1.md
```
