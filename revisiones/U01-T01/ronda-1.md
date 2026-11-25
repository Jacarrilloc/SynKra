# Ronda 1 — U01-T01

VEREDICTO: VERDE

Contexto que comprobé: el worktree `/home/casdav/Projects/Master/wt-m6r-U01-T01` está en HEAD `54636903863da9cf144cff049de8ba2067347c3e`, que coincide con el sha reportado. El hash del archivo de la tarea es `80cfe4dcab067f30e97a449d76825810f2087190`, el esperado. El diff es un solo commit sobre `modulo-6-rehecho` y toca 10 archivos. El JDK de la máquina es `openjdk 25.0.5.1`.

## Criterios de aceptación, verificados por mí
| # | Criterio | Comando que corrí | Resultado |
|---|---|---|---|
| 1 | El build reconoce `:verificador` | `./gradlew -q projects \| grep -F "':verificador'"` | pasa: `\--- Project ':verificador'`, exit 0 |
| 2 | Wrapper 9.9.2 con checksums oficiales | los dos `grep` de la tarea, `sha256sum gradle/wrapper/gradle-wrapper.jar`, `curl .../gradle-9.9.2-bin.zip.sha256` y `curl .../versions/all` (lo filtré con python, versión 9.9.2) | pasa: `distributionSha256Sum=971fcd71…c76b` es igual al `.sha256` oficial, y el jar `621008af…cdaf8` es igual al `wrapperChecksum` oficial de 9.9.2 |
| 3 | El daemon corre sobre Java 21 | `./gradlew --version \| grep '^Daemon JVM:'`, el bucle `pgrep`/`readlink` de la tarea, `./gradlew --status` y `./gradlew help --info` | pasa: `Compatible with Java 21 … (from gradle/gradle-daemon-jvm.properties)`. Además, `--info` muestra que rechaza el daemon 168724 (Java 25) con «JVM is incompatible» y que el build corre en el daemon 174364, cuyo exe es `~/.gradle/jdks/eclipse_adoptium-21-amd64-linux.2/bin/java`, versión `21.0.13.1` |
| 4 | Convenciones Java 21 y JUnit Platform | `grep -n 'JavaLanguageVersion.of(21)'`, `grep -n 'useJUnitPlatform()'` y `./gradlew :verificador:build --rerun-tasks` | pasa: las líneas 6 y 10 aparecen, y sale `BUILD SUCCESSFUL in 631ms` |
| 5 | Los artefactos del build no se versionan | `git check-ignore build/ .gradle/ verificador/build/ .kotlin/` | pasa: imprime las cuatro rutas, exit 0 |

Pruebas: la tarea no tiene lógica ni pruebas JUnit. No existe `verificador/build/test-results`, así que no hay XML que contar ni `skipped` que vigilar. Que el build dure menos de un segundo se explica porque no hay fuentes: `compileJava` y `test` salen NO-SOURCE. No es una suite que se haya saltado.

Alcance:
- No hay cambios en `Makefile`, `scripts/`, `aidlc-docs/`, `tareas/`, `backend/` ni `buildSrc/`.
- No hay clases `.java` ni dependencias del verificador.
- `verificador/build.gradle.kts` solo tiene `java-library`.
- El plugin foojay está en `settings.gradle.kts`, como pide la nota de la tarea.
- `gradlew` está versionado con modo 100755.

Evidencia del codificador: la bitácora incluye el «rojo primero» (no había `gradlew` y `check-ignore` salió con exit 1) y el verde. Mis ejecuciones repiten el verde con salidas idénticas en checksums y greps. No encontré evidencia que no pudiera venir de este diff.

Worktree después de mi revisión: limpio. `git status --porcelain` sale vacío; solo están `.gradle/` y `verificador/build/`, y los dos están ignorados.

## Hallazgos
### F-01 · AMARILLO · tareas/U01-T01-raiz-gradle.md CA-3 · El bucle de lectura de vuelta no distingue qué daemon sirve a este worktree
El bucle `pgrep -f GradleDaemon` lista todos los daemons de la máquina. Encontró uno de 9.6.1 y uno de 9.9.2 que corren sobre Java 25 (quedan del arranque antes de `updateDaemonJvm`), más el propio shell zsh (`zsh: no such option: n`). Cualquier daemon de Java 21 de otro proyecto satisfaría el criterio. Esta vez confirmé con `./gradlew help --info` que el build se conecta al daemon de Java 21 (pid 174364). No bloquea: el criterio, tal como está escrito, se cumple. Es un criterio débil en la especificación, no un defecto del diff.

### F-02 · AMARILLO · CA-4 · El toolchain 21 se comprueba con grep, no con una compilación
Sin fuentes Java, `:verificador:build` no ejercita la compilación con el toolchain 21. La primera compilación real será en U01-T02. No bloquea, porque cae fuera del alcance de esta tarea.

## Tareas candidatas (defectos reales fuera de alcance)
- Hacer más estricto el CA-3 de la plantilla: filtrar el daemon por el pid que reporta `./gradlew --info` (o por `--status` más el registro del daemon) en vez de leer todos los procesos `GradleDaemon`.

## Rutas de transcripciones largas
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/5b13ba94-befc-454f-88e7-3182922701e5/scratchpad/versions.json (respuesta oficial de `services.gradle.org/versions/all` que usé en CA-2)

```
VEREDICTO: VERDE
AMARILLO|tareas/U01-T01-raiz-gradle.md CA-3|El bucle de lectura de vuelta no distingue qué daemon sirve a este worktree
AMARILLO|CA-4|El toolchain 21 se comprueba con grep, no con una compilación
INFORME: revisiones/U01-T01/ronda-1.md
```
