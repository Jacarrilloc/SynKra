# Ronda 1 — U01-T02

VEREDICTO: NO-VERDE

Los cinco criterios pasan cuando los corro yo. El bloqueo viene de una nota de la tarea que el diff no cumple: las tres versiones fijadas tienen unos 16 meses y no son «las estables al momento de escribir la tarea».

Diff revisado: `git diff 93a5e0e..tarea/U01-T02` (sha 0229b30). Toca solo `verificador/build.gradle.kts` (+16 líneas) y `bitacoras/U01-T02.md`. No se desborda del alcance: no hay clases Java, ni cambios en `settings.gradle.kts` o en el `build.gradle.kts` de la raíz, ni en `backend/`, ni otras librerías.

## Criterios de aceptación, verificados por mí
Todos corridos desde /home/user/wt-U01-T02.

| # | Criterio | Comando que corrí | Resultado |
|---|---|---|---|
| 1 | Sin framework en ejecución | `./gradlew -q :verificador:dependencies --configuration runtimeClasspath \| grep -ciE "spring\|fabric8"` | `0`, pasa |
| 2a | SnakeYAML Engine en ejecución | `... runtimeClasspath \| grep -c 'org.snakeyaml:snakeyaml-engine'` | `1`, pasa |
| 2b | jqwik fuera de ejecución | `... runtimeClasspath \| grep -c 'net.jqwik'` | `0`, pasa |
| 3 | JUnit 5 y jqwik en pruebas | `... testRuntimeClasspath \| grep -cE 'net\.jqwik:jqwik\|org\.junit\.jupiter'` | `18`, pasa |
| 4 | Existen los test fixtures | `./gradlew -q :verificador:tasks --all \| grep -E '^testFixturesJar'` | una línea `testFixturesJar - Assembles a jar ...`, pasa |
| 5 | El proyecto construye | `./gradlew :verificador:check --rerun` | `BUILD SUCCESSFUL`, pasa (`test NO-SOURCE`) |

Comprobaciones extra, con el árbol completo de dependencias:
- `runtimeClasspath` y `compileClasspath` contienen solo `org.snakeyaml:snakeyaml-engine:2.10`.
- `testFixturesRuntimeClasspath` contiene solo el proyecto y snakeyaml-engine.
- En `testRuntimeClasspath`, jqwik 1.9.3 pide junit-platform 1.13.1 y el BOM lo sube a 1.13.4. No hay conflicto de versiones.

Que CA-5 termine en 1 s con `test NO-SOURCE` está explicado: la tarea prohíbe clases de prueba y su plan dice «sin código, las pruebas son los criterios». No es una prueba saltada, y no hay XML en `build/test-results` contra el que contar.

Evidencia de la bitácora: el «rojo primero» (CA-2a dio `0`, CA-4 dio `exit=1`) y el «verde» coinciden con lo que obtengo yo y pueden haber salido del diff actual.

Estado del worktree al terminar: limpio. `git status --short --ignored` muestra solo `.gradle/` y `verificador/build/`, que son cachés ignorados.

## Hallazgos

### F-01 · NARANJA · verificador/build.gradle.kts:12,15,17 · Versiones fijas que no son las estables al escribir la tarea
La nota de la tarea pide «Versiones fijas (sin `+` ni `latest`), las estables al momento de escribir la tarea». El archivo de la tarea es del 2026-11-22. Las tres versiones fijadas son de mediados de 2025.

| Librería | Versión en el diff | Estable más reciente en Maven Central |
|---|---|---|
| snakeyaml-engine | 2.10 (2025-07-12) | 3.2 (2026-10-03) |
| junit-bom | 5.13.4 (2025-07-21) | 5.14.4 en la línea 5; 6.1.3 (2026-08-07) en la línea 6 |
| jqwik | 1.9.3 (2025-06-07) | 1.10.2 (2026-10-10) |

Cómo lo comprobé: leí `<latest>` y `<release>` en `repo1.maven.org/maven2/<grupo>/<artefacto>/maven-metadata.xml` y las fechas en el listado de cada versión.

Para elegir el arreglo:
- El POM de `jqwik-engine:1.10.2` declara `junit-platform-engine` y `junit-platform-commons` en 1.14.4, que corresponde a JUnit 5.14.4. La pareja coherente con la tarea («JUnit 5») sería junit-bom 5.14.4 con jqwik 1.10.2.
- Saltar a snakeyaml-engine 3.x es un cambio de versión mayor. Si el codificador decide quedarse en 2.x, tiene que justificarlo en la bitácora. Hoy la bitácora no dice nada sobre la elección de versiones.

### F-02 · AMARILLO · bitacoras/U01-T02.md · La evidencia de CA-5 no es el comando del criterio
La bitácora registra `./gradlew :verificador:check --rerun-tasks | tail -5`, no el comando literal del criterio. Es equivalente y lo reproduje con `--rerun`, así que no bloquea.

## Tareas candidatas (defectos reales fuera de alcance)
- Cuando U01-T12 ponga la suite en `testFixtures`, `testFixturesApi`/`testFixturesImplementation` necesitarán JUnit y jqwik. Hoy `testFixturesRuntimeClasspath` no los tiene, lo cual es correcto para esta tarea.

## Rutas de transcripciones largas
- Ninguna. Las salidas son cortas y están resumidas arriba.

```
VEREDICTO: NO-VERDE
NARANJA|verificador/build.gradle.kts:12,15,17|Versiones fijas que no son las estables al escribir la tarea (snakeyaml-engine 2.10 vs 3.2, junit-bom 5.13.4 vs 5.14.4, jqwik 1.9.3 vs 1.10.2)
INFORME: revisiones/U01-T02/ronda-1.md
```
