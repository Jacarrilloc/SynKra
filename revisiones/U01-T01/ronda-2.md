# Ronda 2 — U01-T01

VEREDICTO: VERDE

F-01 está corregido: el wrapper versionado es ahora el oficial de Gradle 9.9.2, idéntico byte a byte a mi referencia. Corrí yo los cinco criterios en HEAD `7aa2ec6` y todos pasan. No queda ningún ROJO ni NARANJA en pie.

Comprobaciones previas:
- **Tarea y HEAD.** El hash de la tarea es `857eed73…` y coincide. HEAD está en `7aa2ec6`.
- **Commits nuevos.** `04e9bcd`, del codificador, toca solo el jar, `gradlew`, `gradlew.bat`, `gradle-wrapper.properties`, la bitácora y `ronda-1-respuesta.md`. `7aa2ec6`, del orquestador, solo toca `CANDIDATAS.md`. No hay desborde de alcance.
- **Worktree.** Sigue limpio después de la revisión: `git status --short` sale vacío.

## Criterios de aceptación, verificados por mí
| # | Criterio | Comando que corrí (desde /home/user/wt-U01-T01) | Resultado |
|---|---|---|---|
| 1 | El build reconoce `:verificador` | `./gradlew -q projects \| grep -F "':verificador'"` | pasa: `\--- Project ':verificador'` |
| 2 | Gradle corre sobre Java 21 | `./gradlew --version \| grep -E 'JVM: +21\.'` | pasa: `Gradle 9.9.2`, `Launcher JVM:  21.0.13.0.3` |
| 3 | Wrapper fijado con checksum | los dos `grep` sobre `gradle-wrapper.properties` | pasa: `gradle-9.9.2-bin.zip` y `971fcd71…c76b`, que es el checksum oficial (lo contrasté en la ronda 1) |
| 4 | Convenciones Java 21 y JUnit 5 | los dos `grep` + `./gradlew :verificador:build --rerun-tasks` | pasa: líneas 6 y 10, `BUILD SUCCESSFUL in 753ms`, exit 0 |
| 5 | Artefactos ignorados | `git check-ignore build/ .gradle/ verificador/build/` | pasa: imprime las tres rutas |

- **Lectura de vuelta de CA-4.** Volví a correr mi init script del scratchpad y sale `PROBE toolchain=21` y `PROBE test framework=JUnitPlatformTestFramework_Decorated`. Las convenciones siguen aplicadas a `:verificador`.
- **Tiempo del build.** Sigue por debajo de un segundo por la misma razón que en la ronda 1: no hay fuentes ni pruebas, todo sale `NO-SOURCE`, y la tarea dice que sus pruebas son los cinco criterios.

## Verificación de F-01, con mi propia comprobación
- **Checksum del jar.** `sha256sum gradle/wrapper/gradle-wrapper.jar` da `621008afc4f5d59357fcf06ffa644dd5aade5e091bdc363742710445f73cdaf8`. Es el `wrapperChecksum` oficial de 9.9.2 según `services.gradle.org`, y también el del jar embebido en `gradle-wrapper-main-9.9.2.jar`.
- **Comparación con mi referencia.** Comparé con `cmp` los cuatro archivos contra el wrapper 9.9.2 que generé yo en la ronda 1 (/tmp/claude-0/-home-user/3fea55ad-bdab-5467-8f03-a0035d5d8161/scratchpad/w). Los cuatro son idénticos byte a byte: `gradlew`, `gradlew.bat`, `gradle-wrapper.properties` y `gradle-wrapper.jar`.
- **Permisos.** `gradlew` conserva el modo `100755` en el índice.
- **Bitácora.** La sección «Ronda 2» corrige la afirmación falsa de la ronda 1, con rojo primero (el checksum viejo `7d3a4ac4…`) y verde (`621008af…`). Mi propia ejecución confirma lo que dice.

**Estado: F-01 cerrado.**

## Hallazgos
No queda ninguno en pie.
- **F-01 (NARANJA):** cerrado, como se explica arriba.
- **F-02 y F-03 (AMARILLO):** el orquestador los arbitró como candidatas C-02 y C-03. No los reabro porque el diff nuevo no toca `build.gradle.kts` ni la configuración del daemon, así que no los empeora.

## Tareas candidatas (defectos reales fuera de alcance)
Ya están registradas en `CANDIDATAS.md` como C-01, C-02 y C-03. No hay nuevas.

## Rutas de transcripciones largas
- Wrapper de referencia 9.9.2: /tmp/claude-0/-home-user/3fea55ad-bdab-5467-8f03-a0035d5d8161/scratchpad/w
- Init script de lectura de vuelta: /tmp/claude-0/-home-user/3fea55ad-bdab-5467-8f03-a0035d5d8161/scratchpad/probe.init.gradle.kts

```
VEREDICTO: VERDE
INFORME: revisiones/U01-T01/ronda-2.md
```
