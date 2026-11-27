# Ronda 2 — U02-T02

VEREDICTO: VERDE

Los cuatro criterios pasan con mis propias ejecuciones. F-01 y F-02 quedaron corregidos y mis mutaciones lo confirman. Acepto el argumento del codificador sobre F-03 porque lo comprobé con dos builds. No queda ningún ROJO ni NARANJA.

Estado de partida: el hash de la tarea es `789c96d0…` y el HEAD es `e00454c`, los dos coinciden con lo esperado. Hay tres commits sobre `1234f82`. El 18081 estaba libre antes de empezar y quedó libre al terminar, sin ningún proceso del core vivo. El worktree quedó limpio después de la revisión. No toqué el `stash@{0}`.

## Criterios de aceptación, verificados por mí
| # | Criterio | Comando que corrí | Resultado |
|---|---|---|---|
| 1 | El JAR lleva la versión compilada | `./gradlew :core:bootJar --rerun-tasks` + `unzip -p core/build/libs/core-*.jar BOOT-INF/classes/synkra-core.properties` | pasa: `BUILD SUCCESSFUL`, hay un único `core-2.1.0.jar` y sale `version=2.1.0` |
| 2 | La versión no se cambia desde fuera | `./gradlew :core:test --rerun --tests '*VersionCoreTest'` + grep del XML | pasa: `tests="4" skipped="0" failures="0" errors="0"`. Los tiempos por caso (0,667 s, 0,169 s, 0,127 s y 0,001 s) suman un contexto de Spring real más dos contextos hijos, así que nada se saltó |
| 3 | El core arranca y responde salud | el bloque literal de CA-3 en el 18081 | pasa: `UP`. Comprobé el puerto con `ss` antes y después: 0 escuchas en ambos casos |
| 4 | El build sigue pasando y el diff queda acotado | el `./gradlew … --rerun … :check` completo + `git diff --stat origin/main...HEAD` | pasa: `BUILD SUCCESSFUL in 46s` y las cuatro tareas `:test` se ejecutaron. Suma de los XML: verificador 178/0 saltadas, plugin-api 4/0, backend 315/0 y core 4/0, sin fallos ni errores. El diff tiene 9 archivos: `core/**`, una línea de `settings.gradle.kts`, la bitácora, `revisiones/U02-T02/ronda-1.md` y `ronda-1-respuesta.md`. No hay desborde |

### Lo que pediste juzgar

**El mutante de `System.getenv` (cierra F-01).** Hice una copia con `git archive HEAD` en mi scratchpad. En `VersionCore` introduje esta lectura: `System.getenv("SYNKRA_CORE_VERSION") != null ? System.getenv(...) : props.getProperty("version")`.
- Con `env -u SYNKRA_CORE_VERSION ./gradlew --no-daemon :core:test --rerun`, es decir, con el entorno padre limpio igual que en la ronda 1, salió `4 tests completed, 3 failed` y `failures="3"`. El mutante queda detectado sin que quien corre las pruebas tenga que exportar nada, porque la variable la pone `tasks.test`.
- Contraprueba de la precondición: restauré `VersionCore`, borré la línea `environment(...)` de `core/build.gradle.kts` en la copia y volví a correr. Falla `ignoraLaVariableDeEntornoSynkraCoreVersion()` (`failures="1"`). Si la variable deja de llegar, la prueba falla y no pasa en falso.

**El argumento del codificador sobre F-03 («el `doLast` siempre sobrescribe»).** Tiene razón y lo acepto. Lo comprobé en la copia:
- Añadí `core/src/main/resources/synkra-core.properties` con `version=9.9.9` y `otra=1` y corrí `bootJar`. El JAR lleva solo `version=2.1.0`.
- Cambié ese archivo a `version=8.8.8` para forzar un build incremental. `processResources` se volvió a ejecutar y el JAR siguió con `version=2.1.0`.

Para D4, que la versión de build pise cualquier otro valor es justo el comportamiento correcto: no hay forma de que gane otra fuente. Lo que se pierde con este diseño es solo el aviso de que se pisó un archivo, y eso no afecta a ningún criterio. Cierro F-03.

**¿`tasks.test { environment(...) }` vuelve dependiente de la variable a otra prueba?** No.
- La variable afecta solo a la tarea `test` de `:core`. En el repo, `SYNKRA_CORE_VERSION` aparece únicamente en `core/build.gradle.kts:36` y en `VersionCoreTest.java:38`.
- El único `System.getenv` de producción es `backend/.../VariablesPod.java:22`. Pertenece a otro módulo, cuya JVM de prueba no recibe esta variable.
- Por relaxed binding, Spring ve la variable como `synkra.core.version`, pero ningún bean ni archivo `.properties` enlaza esa clave.
- Dentro de `core`, `daLaVersionCompilada` y `ignoraLaPropiedadVersion` también fallan con el mutante. Eso es redundancia útil, no una dependencia oculta: sin la variable, esas dos pruebas pasan (la contraprueba solo hizo fallar a la prueba de la precondición).

Queda un riesgo hacia adelante, que anoto como F-04.

## Hallazgos

### F-04 · AMARILLO · core/build.gradle.kts:35-37 · La variable de entorno alcanza a todas las pruebas futuras de `:core`
La variable está fijada para toda la tarea `test` de `:core`, así que U02-T03, U02-T04 y U02-T07 heredarán `SYNKRA_CORE_VERSION=9.9.9` en sus JVM de prueba. Hoy no afecta a nada. Si alguna prueba futura del arranque verificado llegara a depender del entorno, ese 9.9.9 podría confundir. El comentario de la línea 34 lo deja dicho. Otra opción sería una tarea `Test` propia, filtrada a `VersionCoreTest`. No bloquea.

### F-05 · AMARILLO · bitacoras/U02-T02.md (Ronda 2, «Verde», CA-4) · El `git diff --stat` de la bitácora es anterior al commit
La salida pegada muestra 8 archivos, con `VersionCoreTest.java | 46` y las líneas ` M core/build.gradle.kts` y ` M core/src/test/...`. Es decir, el stat de `c791dd4` con cambios todavía sin commitear. El diff actual tiene 9 archivos y 56 líneas en la prueba. No es evidencia falsa:
- Las líneas ` M` revelan honestamente el estado del worktree en ese momento.
- La parte del build sí corrió sobre el código actual (los 4 tests de CA-2 coinciden).
- Una bitácora que va dentro del commit no puede contener el stat exacto de ese mismo commit.

La evidencia que vale para CA-4 es mi ejecución fresca, que pasa. Para la próxima vez, convendría indicar que el stat se tomó antes del commit. No bloquea.

Los demás hallazgos de la ronda 1 quedan cerrados: F-01 y F-02 corregidos y verificados, F-03 aceptado con la comprobación de arriba. No encontré literales nuevos visibles para el usuario: `2.1.0` es el valor que exige la tarea y `9.9.9` es un dato de prueba.

## Tareas candidatas (defectos reales fuera de alcance)
- Ninguna.

## Rutas de transcripciones largas
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/5b13ba94-befc-454f-88e7-3182922701e5/scratchpad/r2/ca1.log
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/5b13ba94-befc-454f-88e7-3182922701e5/scratchpad/r2/ca2.log
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/5b13ba94-befc-454f-88e7-3182922701e5/scratchpad/r2/ca3.log (y ca3-app.log, el log del core)
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/5b13ba94-befc-454f-88e7-3182922701e5/scratchpad/r2/ca4.log
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/5b13ba94-befc-454f-88e7-3182922701e5/scratchpad/r2/copia/ (la copia de las mutaciones; quedó con el `synkra-core.properties` 8.8.8 de la prueba de F-03 y está fuera del worktree)

```
VEREDICTO: VERDE
AMARILLO|core/build.gradle.kts:35-37|La variable de entorno alcanza a todas las pruebas futuras de :core
AMARILLO|bitacoras/U02-T02.md (Ronda 2, CA-4)|El git diff --stat de la bitácora es anterior al commit
INFORME: revisiones/U02-T02/ronda-2.md
```
