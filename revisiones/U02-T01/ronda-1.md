# Ronda 1 — U02-T01

VEREDICTO: VERDE

Contexto comprobado: hash de la tarea `7f2a63466fb58155dcd6eeeb668879c8d6ae0314` (coincide), HEAD `399fc5e` en `m6r/U02-T01`, base `origin/orq/U05-U02-tareas`. Worktree limpio antes y después de la revisión (`git status --short` vacío). El `stash@{0}` (de U04-T07) se ignoró como se pidió.

## Criterios de aceptación, verificados por mí
| # | Criterio | Comando que corrí | Resultado |
|---|---|---|---|
| 1 | Compila sin dependencias de ejecución | `./gradlew :plugin-api:build --rerun-tasks --console=plain` y `./gradlew -q :plugin-api:dependencies --configuration runtimeClasspath` | pasa: `BUILD SUCCESSFUL`, se corrieron las 11 tareas (compileJava…test, check, build). La consulta de dependencias devuelve `No dependencies`. Con `jdeps -s` el JAR solo depende de `java.base` |
| 2 | Pruebas que pasan sin saltarse, como mínimo 4 | `./gradlew :plugin-api:test --rerun` y el `grep` del XML | pasa: `tests="4" skipped="0" failures="0" errors="0"`. El XML se escribió a las 14:12:54, durante mi ejecución. Cubren null en los dos records, la configuración inmodificable (incluida la copia defensiva), y el plugin de prueba con `SynkraPlugin`+`PuntoKyc` (aprueba y rechaza) |
| 3 | El JAR trae su versión y Java 21 | `unzip -p … MANIFEST.MF \| grep Implementation-Version`, luego `javap -v SynkraPlugin.class \| grep 'major version'` | pasa: `Implementation-Version: 1.0.0`, `major version: 65`. Las 5 clases del JAR son 65 |
| 4 | El resto del build pasa y el diff está acotado | `./gradlew :verificador:test --rerun :verificador:check :backend:test --rerun :backend:check` y `git diff --stat origin/orq/U05-U02-tareas...HEAD` | pasa: `BUILD SUCCESSFUL in 37s`, `:verificador:test`, `:verificarSuite` y `:backend:test` se ejecutaron. XML: 484 pruebas, 0 saltadas, 0 fallos, 0 errores. El diff toca 9 archivos: `plugin-api/**` (7), `settings.gradle.kts` (solo `+include("plugin-api")`) y la bitácora |

Nota sobre CA-4: el comando literal de la tarea (`./gradlew :verificador:check :backend:check --rerun`) dejó `:verificador:test` y `:backend:test` en `UP-TO-DATE` en mi primera corrida (`11 actionable tasks: 1 executed, 10 up-to-date`). La razón es que `--rerun` se aplica solo a la tarea que lo precede, y `check` es una tarea de ciclo de vida. Por eso repetí la corrida con `--rerun` puesto en cada tarea `test`. Es un defecto del texto del criterio, no del diff (ver tareas candidatas).

## Decisiones juzgadas
- **Decisión 1 de la tarea (`ContextoPlugin`).** Se cumple. Es `record ContextoPlugin(Map<String,String> configuracion, System.Logger logger)`, y `javap` confirma `java.lang.System$Logger`. Rechaza null en los dos componentes y copia la configuración de forma inmodificable. No hay ningún campo, método ni literal de país en `plugin-api/`.
- **`Map.copyOf`.** Correcto. Cumple a la vez "inmodificable" (decisión 1) y "validan null" (decisión 2), y también aísla de cambios posteriores al mapa de origen, cosa que la prueba comprueba. Rechazar claves o valores null dentro del mapa es más estricto que lo pedido, pero va en la dirección de la decisión 2. Ver F-01: no tiene prueba.
- **Versión `1.0.0`.** Correcto. La decisión 3 exige fijarla en `plugin-api/build.gradle.kts`, y ahí está, en `version = "1.0.0"`. El manifiesto la toma de `project.version`, no de un literal repetido. No cuenta como literal incrustado en un punto de decisión.
- **Firmas contra `component-methods.md` (C02).** `nombre(): String`, `iniciar(ContextoPlugin)`, `detener()` y `validar(SolicitudKyc): ResultadoKyc` coinciden. Lo comprobé con `javap -p`.

## Evidencia de la bitácora y su edad
- Rojo primero: hay captura de un fallo de compilación (`cannot find symbol … SolicitudKyc`, 21 errores) y se declara que no se usó `git stash`. Es verosímil.
- Captura de CA-4: el primer `git diff --stat` de la bitácora sale vacío porque se tomó antes de cualquier commit. El segundo muestra la bitácora con 103 líneas, y en HEAD tiene 115. El codificador lo avisó. `git diff --stat 21fee4f HEAD` muestra que entre el commit del código y HEAD solo cambió la bitácora (+12). Las líneas de los 8 archivos de código en la captura coinciden con el diff actual. No es evidencia falsa: una bitácora no puede contener el diff-stat final de sí misma. No lo levanto como ROJO. Mi propia corrida de CA-4 es la evidencia vigente.
- En la captura de CA-3, el `javap` aparece sin el `unzip` ni la ruta previos. Mi ejecución lo reemplaza.

## Hallazgos
### F-01 · AMARILLO · plugin-api/src/main/java/io/synkra/plugin/api/SolicitudKyc.java:10, ContextoPlugin.java:11 · Falta probar el rechazo de null dentro de los mapas
`Map.copyOf` lanza NPE si hay una clave o un valor null en `datos` o en `configuracion`. Es una decisión del codificador visible para quien escriba plugins, y ninguna prueba la fija (por ejemplo, `new SolicitudKyc("1", mapaConValorNull)`). Si alguien la cambia más adelante, nada se rompe. No bloquea.

### F-02 · AMARILLO · ResultadoKyc.java, SolicitudKyc.java · Records públicos del contrato sin Javadoc
`SynkraPlugin`, `PuntoKyc` y `ContextoPlugin` tienen comentario de tipo, pero los dos records no. Son API pública de cara a los plugins de país, y les vendría bien una línea. No bloquea.

No queda ningún ROJO ni NARANJA en pie.

## Tareas candidatas (defectos reales fuera de alcance)
- Texto del CA-4, y de cualquier criterio con el mismo patrón en otras tareas: `./gradlew :X:check :Y:check --rerun` no vuelve a correr las pruebas, porque `--rerun` es una opción de la tarea anterior y `check` es de ciclo de vida. Debería ser `./gradlew :X:test --rerun :X:check :Y:test --rerun :Y:check` (o `--rerun-tasks`). Conviene barrerlo en las tareas de U02 y U05 de la rama de tareas.
- Las pruebas del verificador y del backend terminan en 37 s con 484 casos, así que no hay señal de saltos. No se tocó ningún clúster, porque esta tarea no tiene criterio de clúster.

## Rutas de transcripciones largas
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/5b13ba94-befc-454f-88e7-3182922701e5/scratchpad/ca1a.txt
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/5b13ba94-befc-454f-88e7-3182922701e5/scratchpad/ca2.txt
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/5b13ba94-befc-454f-88e7-3182922701e5/scratchpad/ca4.txt (comando literal: pruebas UP-TO-DATE)
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/5b13ba94-befc-454f-88e7-3182922701e5/scratchpad/ca4-fresco.txt (pruebas ejecutadas de nuevo)

```
VEREDICTO: VERDE
AMARILLO|plugin-api/src/main/java/io/synkra/plugin/api/SolicitudKyc.java:10|Falta probar el rechazo de null dentro de los mapas
AMARILLO|plugin-api/src/main/java/io/synkra/plugin/api/ResultadoKyc.java|Records públicos del contrato sin Javadoc
INFORME: revisiones/U02-T01/ronda-1.md
```
