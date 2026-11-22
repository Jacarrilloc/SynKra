# Ronda 1 — U03-T01

VEREDICTO: VERDE

Contexto comprobado: HEAD 04659b4 (rama tarea/U03-T01). El `git hash-object` del archivo de la tarea da 2ecdb7f…, igual al recibido y al que anotó la bitácora. El diff contra 893193d toca 4 archivos: `settings.gradle.kts` (+1), `backend/build.gradle.kts`, `backend/src/main/java/io/synkra/SynkraApp.java` y `bitacoras/U03-T01.md`. No toca el `build.gradle.kts` de la raíz ni `verificador/`, y no trae configuración de SQLite o Flyway, ni ErrorApi, ni Dockerfile, ni módulos de negocio. No hay desborde de alcance.

## Criterios de aceptación, verificados por mí
| # | Criterio | Comando que corrí | Resultado |
|---|---|---|---|
| 1 | El build reconoce `:backend` | `./gradlew -q projects \| grep -F "':backend'"` | pasa: `+--- Project ':backend'` |
| 2 | `bootJar` con `Start-Class` | `./gradlew :backend:bootJar --rerun-tasks` y luego `unzip -p backend/build/libs/backend-*.jar META-INF/MANIFEST.MF \| grep -F 'Start-Class: io.synkra.SynkraApp'` | pasa: `BUILD SUCCESSFUL in 1s`, se ejecutaron 3 tareas, sale `backend-0.1.0.jar` y la línea `Start-Class: io.synkra.SynkraApp` |
| 3 | Dependencias de la plataforma | los dos comandos del criterio, tal cual | pasa: las 5 líneas esperadas (flyway-core, sqlite-jdbc, starter-actuator, starter-data-jdbc, starter-web). El conteo de modulith en testRuntimeClasspath da 6. Ninguna dependencia FAILED |
| 4 | El verificador no se rompió | `./gradlew :verificador:check --rerun` y `--rerun-tasks` | pasa: `BUILD SUCCESSFUL in 696ms`. Ver la nota sobre el tiempo |

Nota sobre el tiempo de CA-4 (invariante 7): tarda menos de 1 s porque `verificador/` no tiene ningún `.java`. Gradle informa `:verificador:test NO-SOURCE` y no se genera `build/test-results`, así que no hay XML que leer ni pruebas omitidas. Eso ya pasa en la base y no lo causa este diff. El criterio se cumple, pero hoy no protege nada.

El plan de pruebas no promete ninguna prueba: el «rojo primero» de CA-1 está en la bitácora. `:backend:test` da NO-SOURCE, que es lo esperado.

## Hallazgos
### F-01 · AMARILLO · backend/build.gradle.kts:3-4 · Aviso de deprecación nuevo en Gradle 9.9.2
Con `--warning-mode all`, tanto `:backend:bootJar` como `:verificador:check` muestran ahora: `The Configuration.setVisible(boolean) method has been deprecated. This is scheduled to be removed in Gradle 11.` El aviso viene de los plugins de Spring Boot 3.5.7 y dependency-management 1.1.7 en Gradle 9. Hoy no bloquea, pero se romperá con Gradle 11. Va como candidata.

### F-02 · AMARILLO · backend/build.gradle.kts:7 · `version = "0.1.0"` incrustado
Está justificado en la bitácora: sin versión, el JAR se llama `backend.jar` y el glob de CA-2 no casa. Es la versión del artefacto, no un valor que decida comportamiento. Lo dejo anotado para cuando haya una política de versionado; no bloquea.

## Tareas candidatas (defectos reales fuera de alcance)
- Revisar la compatibilidad del plugin Spring Boot 3.5.x y de dependency-management con Gradle 10/11 por la deprecación de `Configuration.setVisible` (F-01).
- `:verificador:check` no tiene fuentes ni pruebas, así que CA-4 y los criterios parecidos de otras tareas pasan sin verificar nada mientras U01 no aporte pruebas.

## Estado del worktree al terminar
Limpio: `git status --short` vacío. Solo están los ignorados `.gradle/`, `build/` y `backend/build/`, que son cachés de compilación.

## Rutas de transcripciones largas
- /tmp/claude-0/-home-user/3fea55ad-bdab-5467-8f03-a0035d5d8161/scratchpad/rev-U03-T01/ca2.log
- /tmp/claude-0/-home-user/3fea55ad-bdab-5467-8f03-a0035d5d8161/scratchpad/rev-U03-T01/ca4.log

```
VEREDICTO: VERDE
AMARILLO|backend/build.gradle.kts:3-4|Aviso de deprecación nuevo en Gradle 9.9.2 (Configuration.setVisible), se rompe en Gradle 11
AMARILLO|backend/build.gradle.kts:7|version = "0.1.0" incrustado, justificado por el glob de CA-2
INFORME: revisiones/U03-T01/ronda-1.md
```
