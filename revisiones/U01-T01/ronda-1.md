# Ronda 1 — U01-T01

VEREDICTO: NO-VERDE

Los cinco criterios pasan cuando los corro yo. Hay un hallazgo NARANJA en pie: el wrapper que se versionó no es el de Gradle 9.9.2. Las propiedades apuntan a 9.9.2, pero el jar y los scripts salieron del Gradle 8.14.3 instalado en la máquina, y la bitácora afirma lo contrario.

Comprobaciones previas: el hash del archivo de tarea es `857eed73…` y coincide. HEAD está en `5fe9966` y coincide. El diff `f35d13c..tarea/U01-T01` toca solo archivos dentro del alcance más la bitácora, así que no hay desborde. El worktree sigue limpio después de la revisión: `git status --short` sale vacío y solo aparecen ignorados `.gradle/` y `verificador/build/`.

## Criterios de aceptación, verificados por mí
| # | Criterio | Comando que corrí (desde /home/user/wt-U01-T01) | Resultado |
|---|---|---|---|
| 1 | El build reconoce `:verificador` | `./gradlew -q projects \| grep -F "':verificador'"` | pasa: `\--- Project ':verificador'` |
| 2 | Gradle corre sobre Java 21 | `./gradlew --version \| grep -E 'JVM: +21\.'` | pasa: `Launcher JVM:  21.0.13.0.3` (Gradle 9.9.2) |
| 3 | Wrapper fijado con checksum | los dos `grep` sobre `gradle-wrapper.properties` | pasa: `gradle-9.9.2-bin.zip` y `971fcd71…c76b`. Contrasté el valor con `services.gradle.org/versions/current` y coincide con el `checksum` oficial |
| 4 | Convenciones Java 21 y JUnit 5 | los dos `grep` + `./gradlew :verificador:build --rerun-tasks` | pasa: líneas 6 y 10, `BUILD SUCCESSFUL in 710ms` |
| 5 | Artefactos ignorados | `git check-ignore build/ .gradle/ verificador/build/` | pasa: imprime las tres rutas |

- **Lectura de vuelta de CA-4.** Lancé `help` con un init script mío del scratchpad (no está en el worktree) y leí la configuración aplicada: `PROBE toolchain=21` y `PROBE test framework=JUnitPlatformTestFramework_Decorated`. Las convenciones se aplican de verdad a `:verificador`.
- **Tiempo de la suite.** El build tarda menos de un segundo porque no hay fuentes ni pruebas: todas las tareas salen `NO-SOURCE`. Eso coincide con la tarea, que no tiene lógica y cuyas pruebas son los cinco criterios. No hay XML de pruebas que contar.
- **Rojo primero.** La bitácora registra que CA-1 y CA-5 fallaban antes del cambio, con salida y código de salida.

## Hallazgos

### F-01 · NARANJA · gradle/wrapper/gradle-wrapper.jar, gradlew, gradlew.bat · El wrapper versionado es el de Gradle 8.14.3, no el de 9.9.2, y la bitácora dice lo contrario
- **El jar.** El `gradle-wrapper.jar` versionado tiene sha256 `7d3a4ac4de1c32b59bc6a4eb8ecb8e612ccd0cf1ae1e99f66902da64df296172` (43764 bytes).
  - En `services.gradle.org/versions/all` ese checksum corresponde a las versiones 8.14 a 8.14.5, no a 9.9.2.
  - El `wrapperChecksum` oficial de 9.9.2 es `621008afc4f5d59357fcf06ffa644dd5aade5e091bdc363742710445f73cdaf8`. Es idéntico al `gradle-wrapper.jar` embebido en `gradle-wrapper-main-9.9.2.jar` de la distribución descargada (49154 bytes).
  - La máquina tiene `/opt/gradle/bin/gradle` = Gradle 8.14.3, y el jar embebido en esa instalación da exactamente `7d3a4ac4…`.
- **Los scripts.** Generé un wrapper de referencia en el scratchpad con `./gradlew wrapper --gradle-version 9.9.2 --distribution-type bin --gradle-distribution-sha256-sum 971fcd71…` y lo comparé con el versionado:
  - `gradlew`: le sobran `CLASSPATH="\\\"\\\""` y la conversión con cygpath, y la cabecera es la antigua (`2015-2021`, `blob/HEAD`).
  - `gradlew.bat`: le faltan `setlocal EnableExtensions` y el bloque de seguridad `afterSafetyNet`.
  - `gradle-wrapper.properties`: le faltan `retries=0` y `retryBackOffMs=500`.
- **Qué pasó.** El wrapper se generó con el Gradle 8.14.3 del sistema y `--gradle-version 9.9.2`. Faltó el segundo paso, volver a correr `./gradlew wrapper` con 9.9.2, que es lo que reemplaza el jar y los scripts por los de la versión fijada.
- **Por qué importa.**
  - El alcance pide el wrapper fijado. Hoy solo está fijada la distribución; el ejecutable que arranca todo es de otra versión.
  - La bitácora afirma «Wrapper generado con Gradle 9.9.2», y eso no se sostiene.
  - Cualquier validación contra el checksum oficial de 9.9.2 (por ejemplo `gradle/actions/wrapper-validation`) daría desajuste.
- **Por qué no es ROJO.** No rompe ningún criterio, y el jar es un binario oficial y verificable (de 8.14.x), no uno alterado.

### F-02 · AMARILLO · build.gradle.kts:2 · Convenciones en `subprojects {}`
Funciona y es lo que pide la tarea. A medida que crezca el multi-proyecto (U03-T01, con Spring Boot), un plugin de convención en `buildSrc` o en un build incluido evita la configuración entre proyectos. No bloquea.

### F-03 · AMARILLO · CA-2 · El daemon no tiene JVM fijada
`./gradlew --version` muestra `Daemon JVM: … (no Daemon JVM specified, using current Java home)`. CA-2 pasa por la JVM del launcher, y el toolchain fija la compilación en 21, así que el riesgo es bajo. No bloquea.

## Tareas candidatas (defectos reales fuera de alcance)
- U01-T02: con Gradle 9, `useJUnitPlatform()` exige declarar `testRuntimeOnly("org.junit.platform:junit-platform-launcher")`, además de JUnit Jupiter, en cuanto haya pruebas. Si falta, `test` falla con «Failed to load JUnit Platform». Hay que tenerlo en cuenta al añadir jqwik y SnakeYAML Engine.

## Rutas de transcripciones largas
- Wrapper de referencia 9.9.2 generado para comparar: /tmp/claude-0/-home-user/3fea55ad-bdab-5467-8f03-a0035d5d8161/scratchpad/w
- Jars extraídos (9.9.2 y 8.14.3): /tmp/claude-0/-home-user/3fea55ad-bdab-5467-8f03-a0035d5d8161/scratchpad/j
- Init script de lectura de vuelta: /tmp/claude-0/-home-user/3fea55ad-bdab-5467-8f03-a0035d5d8161/scratchpad/probe.init.gradle.kts

```
VEREDICTO: NO-VERDE
NARANJA|gradle/wrapper/gradle-wrapper.jar, gradlew, gradlew.bat|El wrapper versionado es el de Gradle 8.14.3, no el de 9.9.2, y la bitácora dice lo contrario
INFORME: revisiones/U01-T01/ronda-1.md
```
