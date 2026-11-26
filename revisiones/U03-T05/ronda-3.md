# Ronda 3 — U03-T05

VEREDICTO: VERDE

Revisé el worktree `/home/user/wt-m6r-U03-T05` en la rama `m6r/U03-T05`, con HEAD `42d55a4f3cd3…`. El diff de la tarea es `b18d6e3...HEAD` y el de esta ronda es `ae9922c..42d55a4`. El `git hash-object` del archivo de la tarea da `ad26b382…` y coincide con el despacho.

Hice la mutación solo en una copia sacada con `git archive` en mi scratchpad, nunca en el worktree. Al terminar, `git status --short` en el worktree sale vacío.

## Criterios de aceptación, verificados por mí
| # | Criterio | Comando que corrí | Resultado |
|---|---|---|---|
| 1 | La prueba de fronteras pasa sin caché | `./gradlew :backend:test --rerun --tests '*FronterasModulosTest'` | pasa, `BUILD SUCCESSFUL`. La corrida empezó a las 02:24:14Z y el XML tiene timestamp 02:24:16Z |
| 2 | Lectura de vuelta del reporte | `grep -ho 'tests=…' …TEST-*FronterasModulosTest.xml` | pasa, `tests="3" skipped="0" failures="0" errors="0"`. Tiempos de 1.607 s, 0.037 s y 0.06 s, que corresponden a análisis de bytecode y no a pruebas saltadas |
| 3 | La documentación de módulos se genera | primero borré `backend/build/spring-modulith-docs`, volví a correr la prueba y luego `ls` y `grep -l plataforma` | pasa. Se regeneran `components.puml` y `module-plataforma.adoc`, y el `.adoc` nombra `plataforma` |
| 4 | Modulith no llega a runtime | `./gradlew :backend:dependencies --configuration runtimeClasspath \| grep -ci modulith` | pasa, `0` |
| 5 | Todo el backend pasa sin caché | `./gradlew :backend:check --rerun` | pasa, `BUILD SUCCESSFUL`. Son 7 XML con 32 tests en total, todos con skipped=0, failures=0 y errors=0, y todos con timestamps de esta corrida (02:24:30Z a 02:24:36Z). `SynkraAppTest` tiene 2 tests |

## Verificación de la respuesta a la ronda 2

**F-01 (NARANJA): corregido como pedía el arbitraje.** `SynkraAppTest.java:35-41` ahora pasa cada bean a su tipo con `contexto.getType(n)`. Luego filtra los que tienen `t.getName().startsWith("io.synkra.fronteras")` y exige que la lista quede vacía. Es decir, filtra por el paquete del tipo y ya no por el nombre del bean.

Reproduje el par falla-pasa yo mismo, en la copia:
- **Con la mutación** (`@org.springframework.stereotype.Component` sobre `Uno`): `SynkraAppTest > laAppDePruebaDeFronterasNoEntraAlContextoReal() FAILED`, con `tests="2" failures="1"` y `BUILD FAILED`. El XML nombra al intruso `io.synkra.fronteras.uno.Uno`.
- **Sin la mutación** (`Uno.java` idéntico al del worktree según `diff`): `BUILD SUCCESSFUL`, con `tests="2" failures="0"`.

La ronda 2 vio la misma mutación quedar en verde con el filtro por nombre. Ahora la prueba sí detecta la fuga.

**Alcance: sin desborde.** El cambio de esta ronda toca solo una prueba (`SynkraAppTest.java`) y la bitácora. En todo el diff de la tarea no hay cambios en `src/main` ni en ningún `build.gradle.kts`, y Modulith sigue solo en pruebas (CA-4).

## Hallazgos

### F-01 · AMARILLO · bitacoras/U03-T05.md:84 · La línea de evidencia de la ronda 3 resume los cinco criterios sin pegar la salida
La bitácora dice «salida arriba en la ejecución de esta ronda», pero no trae esa salida. Lo mismo pasa con el par falla-pasa: aparece resumido, sin el comando completo ni la salida pegada. No bloquea, porque yo mismo corrí los cinco criterios y la mutación, y todo coincide con lo que dice. Aun así, por sí sola es una afirmación sin evidencia.

### F-02 · AMARILLO · backend/src/test/java/io/synkra/SynkraAppTest.java:38 · Dos casos de borde secundarios en el filtro
- El prefijo `"io.synkra.fronteras"` no termina en punto, así que también atraparía un paquete hipotético `io.synkra.fronterasX`. Es un falso positivo y no deja pasar ninguna fuga.
- Los beans con tipo `null` se descartan sin decir nada. Hoy no tiene efecto, pero un bean del fixture cuyo tipo no se pueda resolver pasaría sin detectarse.

## Tareas candidatas (defectos reales fuera de alcance)
- `components.puml` sigue sin componentes porque hoy hay un solo módulo. Viene de rondas anteriores y aplica cuando llegue U04.
- `ControladorDePrueba` y `ElementoRepositorio` también entran al contexto real. Ya está registrada como C-14; no la repito.

## Rutas de transcripciones largas
- /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/u03t05r3/ca1.txt
- /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/u03t05r3/ca5.txt
- /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/u03t05r3/mut-uno.txt (`@Component` en `Uno`: la prueba falla)
- /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/u03t05r3/mut-revertida.txt (mutación revertida: la prueba pasa)
- Copia de trabajo de la mutación: /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/u03t05r3/copia

No tengo herramientas de escritura, así que no creé `revisiones/U03-T05/ronda-3.md`. Este mensaje es el contenido del informe, para que el orquestador lo guarde en esa ruta.

```
VEREDICTO: VERDE
AMARILLO|bitacoras/U03-T05.md:84|La línea de evidencia de la ronda 3 resume los cinco criterios sin pegar la salida
AMARILLO|backend/src/test/java/io/synkra/SynkraAppTest.java:38|Dos casos de borde secundarios en el filtro
INFORME: revisiones/U03-T05/ronda-3.md
```
