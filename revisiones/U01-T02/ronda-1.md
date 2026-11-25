# Ronda 1 — U01-T02

VEREDICTO: VERDE

Todos los criterios de aceptación pasan con mis propias ejecuciones. No queda ningún ROJO ni NARANJA. El único hallazgo es un AMARILLO: la carpeta `verificador/.jqwik-database` queda sin ignorar.

Contexto comprobado:
- El worktree está en `2bf3b91`, que coincide con el sha reportado.
- El hash de la tarea es `2a0f93800d3d438661885eac92becb40c5c9d09b` y coincide con el esperado.
- Diff revisado: `git diff m6r/U01-T01...m6r/U01-T02`. Tiene 1 commit y 3 archivos: `bitacoras/U01-T02.md`, `verificador/build.gradle.kts` y `verificador/src/test/java/io/synkra/verificador/HumoMotoresTest.java`.

## Criterios de aceptación, verificados por mí
| # | Criterio | Comando que corrí (desde la raíz del worktree) | Resultado |
|---|---|---|---|
| 1 | Sin framework en ejecución | `./gradlew -q :verificador:dependencies --configuration runtimeClasspath \| grep -ciE "spring\|fabric8"` | `0`, pasa |
| 2a | SnakeYAML Engine en ejecución | `... runtimeClasspath \| grep -c 'org.snakeyaml:snakeyaml-engine'` | `1`, pasa. El runtimeClasspath completo es solo `org.snakeyaml:snakeyaml-engine:3.2` |
| 2b | Nada de pruebas en ejecución | `... runtimeClasspath \| grep -ciE 'jqwik\|junit'` | `0`, pasa |
| 3 | Jupiter, launcher y jqwik en pruebas | `... testRuntimeClasspath \| grep -oE '...' \| sort -u` | Salen las tres líneas (`net.jqwik:jqwik-engine`, `org.junit.jupiter:junit-jupiter-engine`, `org.junit.platform:junit-platform-launcher`), pasa |
| 4 | Los dos motores corren; lectura del XML | Primero borré `verificador/build/test-results`, luego `./gradlew :verificador:test --rerun` y el `grep -ho` del XML | `BUILD SUCCESSFUL`. Reporte `tests="2" skipped="0" failures="0" errors="0"`, con timestamp de mi corrida (`2026-11-25T19:59:58Z`). Pasa |
| 5 | Existe `testFixturesJar` | `./gradlew -q :verificador:tasks --all \| grep -E '^testFixturesJar'` | Una línea `testFixturesJar - Assembles a jar archive ...`, pasa |

Detalle de CA-4:
- El XML trae dos casos: `motorJUnitCarga()` (0.014 s) y `motorJqwikCarga` (0.131 s).
- El informe de jqwik dice `tries = 1000 | checks = 1000`. La propiedad corrió de verdad y no se saltó.
- El `1s` del build se explica porque solo se volvió a ejecutar la tarea `test`.

Otras afirmaciones que comprobé:
- **Rojo primero:** lo reproduje en una copia fuera del worktree (`git archive` de la rama, con el `verificador/build.gradle.kts` de `m6r/U01-T01`). Da `12 errors` y `BUILD FAILED`, igual que en la bitácora.
- **Versiones:** en Maven Central, `maven-metadata.xml` da `release` 3.2 para snakeyaml-engine y 1.10.2 para jqwik, que son las fijadas. Para junit-bom da `6.2.0-M1`, que es un milestone, así que 6.1.3 como estable más reciente es razonable. No hay rangos ni `latest.release`.
- **Plataforma 1.14.4:** jqwik pide `junit-platform-*:1.14.4` y Gradle lo resuelve a 6.1.3 (`1.14.4 -> 6.1.3` en testRuntimeClasspath). CA-4 confirma que funciona así.
- **Alcance:**
  - No hay cambios en `settings.gradle.kts` ni en el `build.gradle.kts` de la raíz.
  - No hay clases de producción (`verificador/src/main` no existe).
  - `testFixtures` no tiene contenido ni dependencias.
  - No hay librerías vetadas.
  - `useJUnitPlatform()` sale de la raíz (U01-T01), no del diff.

## Hallazgos
### F-01 · AMARILLO · verificador/.jqwik-database · El artefacto de jqwik ensucia el worktree
- Al correr las pruebas, jqwik crea `verificador/.jqwik-database` y nada la ignora.
- `git check-ignore -v verificador/.jqwik-database` no devuelve nada, y `git status --short` muestra `?? verificador/.jqwik-database`.
- Ya estaba antes de mi revisión (la dejaron las corridas del codificador) y vuelve a crearse con cada `test`.
- No rompe ningún criterio. Pero el worktree queda sucio después de cada corrida de pruebas, y es fácil que alguien lo suba por error en un `git add -A`.
- El codificador lo registró en la bitácora como candidata, y estoy de acuerdo.

## Tareas candidatas (defectos reales fuera de alcance)
- Ignorar `.jqwik-database` en el `.gitignore` de la raíz, que es de U01-T01. Otra opción es apuntar la base de datos de jqwik a `build/` con `jqwik.database` en `verificador/src/test/resources/junit-platform.properties`.

## Estado del worktree al terminar
- Sucio solo por `?? verificador/.jqwik-database`, que existía antes y que mi corrida de CA-4 regeneró. No hay fuentes modificadas.
- Mi reproducción de «rojo primero» se hizo en el scratchpad, no en el worktree.

## Rutas de transcripciones largas
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/5b13ba94-befc-454f-88e7-3182922701e5/scratchpad/ca4.txt
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/5b13ba94-befc-454f-88e7-3182922701e5/scratchpad/rt.txt
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/5b13ba94-befc-454f-88e7-3182922701e5/scratchpad/trt.txt
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/5b13ba94-befc-454f-88e7-3182922701e5/scratchpad/rojo/ (copia para el rojo primero)

```
VEREDICTO: VERDE
AMARILLO|verificador/.jqwik-database|El artefacto de jqwik ensucia el worktree
INFORME: revisiones/U01-T02/ronda-1.md
```
