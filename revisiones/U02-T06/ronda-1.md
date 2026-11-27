# Ronda 1 — U02-T06

VEREDICTO: NO-VERDE

Hash de la tarea comprobado: `git hash-object` da c05f5ff56958720575de95a6a1fac0b9a0a59ecb y coincide. Rama m6r/U02-T06 en d2554a3. Base 15ca50f. El worktree quedó limpio después de la revisión (`git status --short` no muestra nada).

## Criterios de aceptación, verificados por mí
| # | Criterio | Comando que corrí | Resultado |
|---|---|---|---|
| 1 | Readiness 503, liveness 200, log, detener, casos verdes | `./gradlew :core:test --rerun --tests '*FalloInicioPluginTest'` + grep del XML | pasa: BUILD SUCCESSFUL, `tests="4" skipped="0" failures="0" errors="0"`. Duración de 0,58 a 3,03 s por caso: arranca un servidor real, no es una suite vacía |
| 2 | El mutante «sin la captura» hace fallar CA-1 **por aserción** | Mutante propio en una copia fuera del worktree (`git archive HEAD`), con el try/catch de `iniciar` quitado | **falla**: 2 de 4 fallan, pero por `java.lang.IllegalStateException at FalloInicioPluginTest.java:68` (dentro de `arrancar()`), no por aserción |
| 3 | El resto del build sigue pasando | `:verificador:test --rerun :verificador:check :plugin-api:test --rerun :plugin-api:check :core:test --rerun :core:check` | pasa: BUILD SUCCESSFUL. core: ArranquePlugins 10, CargadorPlugins 10, ConcordanciaSuite 10, FalloInicioPlugin 4, VersionCore 4. verificador: 13 clases. plugin-api: 4. Todo con skipped=0, failures=0, errors=0 |
| 4 | El diff queda acotado | `git rev-parse --short HEAD; git diff --stat 15ca50f...HEAD` | pasa: d2554a3, 11 archivos, solo `core/**` y `bitacoras/U02-T06*`. `settings.gradle.kts` no cambia, lo que es correcto con el fixture en @TempDir |

## Hallazgos

### F-01 · ROJO · CA-2 · El mutante «sin la captura» falla por excepción, no por aserción
CA-2 pide que, quitando la captura en `iniciar`, CA-1 falle **por aserción**. Repetí el mutante yo mismo y fallan 2 pruebas por la `IllegalStateException: boom-…` que se escapa de `SpringApplicationBuilder.run()` (línea 68). El codificador lo reconoce en la bitácora.
- La mutación B (dejar la captura pero quitar `detener` y `REFUSING_TRAFFIC`) no es el mutante que pide el criterio. Demuestra que las aserciones de 503 muerden, pero no cubre CA-2 tal como está escrito.
- Que el contexto «no arranque» no hace imposible cumplir el criterio. Es una aserción que falta. La decisión 1 exige que **el proceso siga vivo**, y hoy ninguna prueba lo afirma: solo se cumple de forma implícita.
- Arreglo, en una línea por caso que falla: `var ctx = assertDoesNotThrow(this::arrancar, "el core debe seguir vivo aunque falle un plugin")`. Con eso el mutante sin captura falla por `AssertionFailedError` y la decisión 1 queda escrita como aserción.

Mi decisión sobre el punto (1): **CA-2 no se cumple**.

### F-02 · NARANJA · `IniciadorPlugins.java:68-76` y `FalloInicioPluginTest` · La decisión 3 (orden inverso y `detener()` que lanza) no tiene prueba
Corrí dos mutantes en copias y los dos **sobreviven**, con BUILD SUCCESSFUL y 4/4 en verde:
- (a) `detener` recorriendo hacia delante: `for (int i = 0; i < iniciados.size(); i++)`. Sobrevive porque la prueba de «el segundo falla» solo deja **un** plugin iniciado, así que el orden de parada no se observa.
- (b) Sin el try/catch en `detener()`: si un `detener()` lanza, la excepción tapa la causa original y se salta los `detener` siguientes y el `REFUSING_TRAFFIC`. Ninguna prueba tiene un plugin cuyo `detener` lance.

Faltan dos casos, como mínimo:
- tres plugins con el tercero fallando, esperando `[iniciar:a, iniciar:b, iniciar:c, detener:b, detener:a]`;
- un plugin iniciado cuyo `detener` lanza, comprobando que la readiness sigue en 503, que el log contiene la causa original del `iniciar` y que los demás `detener` se ejecutan.

### F-03 · AMARILLO (aviso al orquestador por la decisión 2) · `IniciadorPlugins.java:59-64` · `vigilar` depende de un orden implícito y deja una ventana con 200
Sobre el punto (2): **sí hay una ventana**. En `EventPublishingRunListener.ready`, Spring Boot publica `ApplicationReadyEvent` y justo después `ACCEPTING_TRAFFIC`. Ese evento lo reciben, en el mismo hilo, `ApplicationAvailabilityBean` (que guarda ACCEPTING) y después `vigilar` (que publica REFUSING de nuevo). Entre esas dos llamadas el estado vale ACCEPTING_TRAFFIC, y Tomcat ya escucha, así que una sonda concurrente que caiga ahí recibe 200. La ventana dura microsegundos y en la práctica un kubelet no la toca, pero existe.

Más importante: el resultado depende de un desempate implícito entre los dos listeners, que tienen la misma precedencia (LOWEST) y se ordenan por orden de registro. Lo comprobé con un mutante que solo añade `@Order(HIGHEST_PRECEDENCE)` a `vigilar`: la readiness **acaba en 200** y fallan 2 pruebas (`AssertionFailedError` en las líneas 121 y 134). Que esa regresión la atrapen las pruebas es lo que hace que esto no bloquee.

Hay que documentar esa dependencia en el Javadoc de `vigilar`. La solución robusta (un `HealthIndicator` propio `plugins` en el grupo `readiness`, que no depende del orden de eventos) choca con la decisión 2 tal como está redactada. Por eso queda como señal sobre la especificación y como candidata, no como exigencia de esta ronda.

### F-04 · AMARILLO · `IniciadorPlugins.java:73` · Literal de log sin constante
`"Plugin fallo al detener plugin={}: {}"` es un literal. La decisión 4 solo exige constante para el prefijo de `iniciar`. No está en un punto de decisión, así que no bloquea, pero lo coherente sería tener `PREFIJO_FALLO_DETENER`.

### F-05 · AMARILLO · `bitacoras/U02-T06-evidencia/ca3.txt` · Evidencia anterior al último cambio
La bitácora reconoce que CA-3 se corrió antes del cambio de Javadoc en `ArranquePlugins.java`. No tiene efecto: mi ejecución sobre d2554a3 pasa igual. Aun así, la evidencia versionada no sale del diff final y en la próxima ronda debe regenerarse.

### Sobre el punto (3), fixture en @TempDir
Lo acepto. La decisión 6 lo permite expresamente y la bitácora dice cuál se eligió. El plugin de prueba lanza `IllegalStateException` en `iniciar`, como pide la decisión, y declara `coreCompatible: 2.x`. Por eso `settings.gradle.kts` no cambia. Sin hallazgo.

## Tareas candidatas (defectos reales fuera de alcance)
- Al cerrar el contexto, los plugins que se iniciaron bien nunca reciben `detener()`, porque `ArranquePlugins.cerrar` solo cierra los cargadores. Coincide con la candidata de la bitácora.
- Readiness por `HealthIndicator` en el grupo `readiness`, en lugar de volver a publicar REFUSING_TRAFFIC. Elimina la ventana y la dependencia de orden de F-03. Pide revisar la decisión 2.

## Rutas de transcripciones largas
- /tmp/claude-0/-home-user/311bec3f-8f32-5470-935c-db8e1e6d6a2d/scratchpad/rev-U02-T06-r1/ca1.log
- /tmp/claude-0/-home-user/311bec3f-8f32-5470-935c-db8e1e6d6a2d/scratchpad/rev-U02-T06-r1/ca3.log
- /tmp/claude-0/-home-user/311bec3f-8f32-5470-935c-db8e1e6d6a2d/scratchpad/rev-U02-T06-r1/sincaptura.log (CA-2, falla por excepción)
- /tmp/claude-0/-home-user/311bec3f-8f32-5470-935c-db8e1e6d6a2d/scratchpad/rev-U02-T06-r1/orden.log (F-02a, sobrevive)
- /tmp/claude-0/-home-user/311bec3f-8f32-5470-935c-db8e1e6d6a2d/scratchpad/rev-U02-T06-r1/detenerlanza.log (F-02b, sobrevive)
- /tmp/claude-0/-home-user/311bec3f-8f32-5470-935c-db8e1e6d6a2d/scratchpad/rev-U02-T06-r1/orderalta.log (F-03, readiness 200 con el orden invertido)

Nota: el scratchpad es compartido con otras sesiones (allí ya había `copia/`, `r2/` y otros). Trabajé solo en el subdirectorio `rev-U02-T06-r1/`.

```
VEREDICTO: NO-VERDE
ROJO|CA-2|El mutante sin captura falla por IllegalStateException, no por aserción (falta assertDoesNotThrow sobre arrancar)
NARANJA|IniciadorPlugins.java:68-76|Orden inverso y detener() que lanza sin prueba: los dos mutantes sobreviven
AMARILLO|IniciadorPlugins.java:59-64|vigilar depende del orden implícito de los listeners y deja una ventana de microsegundos con readiness 200
AMARILLO|IniciadorPlugins.java:73|Literal de log de detener sin constante
AMARILLO|bitacoras/U02-T06-evidencia/ca3.txt|Evidencia de CA-3 anterior al último cambio
INFORME: revisiones/U02-T06/ronda-1.md
```
