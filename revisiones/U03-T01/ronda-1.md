# Ronda 1 — U03-T01

VEREDICTO: NO-VERDE

Contexto: worktree `/home/casdav/Projects/Master/wt-m6r-U03-T01`, HEAD `d745d3f` (coincide con el sha reportado). Hash de la tarea `3c16bf16538f9cfc3aacf0e9de73b43b25cdd279` (coincide). Diff revisado: `git diff m6r/U01-T01...m6r/U03-T01`, con la base apilada que indicó el orquestador. La sección «En SynKra» del contrato dice `modulo-6-rehecho`; seguí la instrucción del orquestador. El diff tiene 6 archivos, todos dentro de `backend/`, salvo `settings.gradle.kts` (+1 línea) y la bitácora.

## Criterios de aceptación, verificados por mí
| # | Criterio | Comando que corrí | Resultado |
|---|---|---|---|
| 1 | El build reconoce `:backend` | `./gradlew -q projects \| grep -F "':backend'"` | pasa: `+--- Project ':backend'` |
| 2 | `bootJar` con `Start-Class` correcto y Boot 3.x | `./gradlew :backend:bootJar --rerun-tasks` y después `unzip -p backend/build/libs/backend-*.jar META-INF/MANIFEST.MF \| grep ...` | pasa: `BUILD SUCCESSFUL`, `Start-Class: io.synkra.SynkraApp`, `Spring-Boot-Version: 3.5.16` |
| 3 | Dependencias de la plataforma | los dos `dependencies \| grep` de la tarea | pasa: las 5 líneas; `11` coincidencias de Modulith en testRuntimeClasspath |
| 4 | El contexto arranca y la prueba no se saltó | `./gradlew :backend:test --rerun --tests '*SynkraAppTest'` y después el `grep` del XML | pasa: `BUILD SUCCESSFUL`, `tests="1" skipped="0" failures="0" errors="0"`; la prueba dura 0.484 s y el log del XML dice `Started SynkraAppTest in 1.315 seconds`, así que el contexto cargó de verdad |
| 5 | Bytecode de Java 21 | `javap -v .../SynkraApp.class \| grep 'major version'` | pasa: `major version: 65` |

Lectura de vuelta adicional del «JAR ejecutable» del alcance: arranqué `java -jar backend/build/libs/backend-0.1.0.jar --server.port=18089` y esperé con `timeout 40` hasta que `/actuator/health` respondiera. Respondió `{"status":"UP"}` y el log dice `Started SynkraApp in 2.323 seconds`. Después maté el proceso y confirmé que no quedó ninguno vivo.

Afirmaciones del codificador, comprobadas:
- **Boot 3.5.16 es la última 3.x:** confirmado en maven-metadata de Central (las dos últimas 3.x son 3.5.15 y 3.5.16).
- **Modulith 1.4.14 es la última 1.4.x**, la línea que va con Boot 3.5: confirmado.
- **`mavenCentral()` y `version = "0.1.0"`** están en `backend/build.gradle.kts`. Los cuatro excludes de `SynkraApp` están presentes y la aplicación arranca sin DataSource.
- **La evidencia de la bitácora es coherente con el diff actual.** El commit es de las 15:01:49 y el `backend.jar` sin versión es de las 15:01, anterior a fijar la versión. Mis ejecuciones reproducen las mismas salidas.

## Hallazgos

### F-01 · ROJO · backend/build.gradle.kts:26 · Desborde de alcance: Spring Modulith declarado como dependencia de producción
La tarea, en «Fuera», dice: «La prueba de Spring Modulith: U03-T05. **Aquí solo se declara la dependencia de pruebas.**» El plan de la unidad (`aidlc-docs/construction/plans/U03-plataforma-backend-code-generation-plan.md:20-21`) dice lo mismo: «Spring Modulith (pruebas)». El diff añade además:
```
implementation("org.springframework.modulith:spring-modulith-starter-core")
```
Eso mete en el `runtimeClasspath` y en el JAR de producción `spring-modulith-api`, `-apt` (un procesador de anotaciones), `-core` y `-moments`, este último con su autoconfiguración. Lo confirman `./gradlew -q :backend:dependencies --configuration runtimeClasspath | grep -i modulith` y `unzip -l backend-0.1.0.jar | grep modulith` (`BOOT-INF/lib/spring-modulith-{api,apt,core,moments}-1.4.14.jar`).

Ninguno de los criterios lo necesita. El CA-3b cuenta coincidencias en `testRuntimeClasspath`, que ya cubre `spring-modulith-starter-test`, presente en el nivel superior del árbol. Hay que quitar esa línea, o justificarla contra la tarea si el orquestador decide que el runtime de Modulith es «plataforma». Con la redacción actual, no lo es.

### F-02 · AMARILLO · backend/build.gradle.kts:7 · Versión del artefacto fijada en el subproyecto
`version = "0.1.0"` se puede ver desde fuera (nombre del JAR, log `Starting SynkraApp v0.1.0`). Hacía falta para que el glob `backend-*.jar` de CA-2 encajara, y es el sitio donde Gradle declara la versión, no un punto de decisión, así que no lo subo a NARANJA. Cuando la raíz deje de estar fuera de alcance, conviene centralizarla (`gradle.properties` o la raíz) para que `verificador` y `backend` no diverjan.

### F-03 · AMARILLO · backend/build/libs/ · JAR viejo sin versión junto al nuevo
En `backend/build/libs/` quedan `backend.jar` (de antes de fijar la versión) y `backend-0.1.0.jar`. Está ignorado por git y el glob de CA-2 no lo coge. Lo anoto solo para que nadie lea de vuelta el artefacto equivocado. Con un `clean` desaparece.

Estado del worktree al terminar: `git status --short` sale vacío. Solo hay directorios ignorados (`.gradle/`, `build/`, `backend/build/`), que son cachés y salidas de build.

## Tareas candidatas (defectos reales fuera de alcance)
- La raíz no declara repositorios, y cada subproyecto tendrá que repetir `mavenCentral()`. Podría centralizarse en `settings.gradle.kts` con `dependencyResolutionManagement`, en una tarea que sí pueda tocar la raíz y `verificador/`.

## Rutas de transcripciones largas
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/5b13ba94-befc-454f-88e7-3182922701e5/scratchpad/ca2.log
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/5b13ba94-befc-454f-88e7-3182922701e5/scratchpad/ca4.log
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/5b13ba94-befc-454f-88e7-3182922701e5/scratchpad/run.log

```
VEREDICTO: NO-VERDE
ROJO|backend/build.gradle.kts:26|Desborde de alcance: spring-modulith-starter-core como dependencia de producción
INFORME: revisiones/U03-T01/ronda-1.md
```
