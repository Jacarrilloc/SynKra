# Ronda 2 — U06-C

VEREDICTO: VERDE

Volví a correr yo los siete criterios y los siete pasan. Las cuatro respuestas, F-01 a F-04, se sostienen con evidencia mía, y mis mutaciones E3 y E4, que en la ronda 1 sobrevivían, ahora caen. Lo único nuevo es un AMARILLO (F-05), que no bloquea.

**Comprobaciones previas**
- **Hash de la tarea:** `git hash-object` da 004d66e158a2bc2ef9ceffb01e6fb5ab1f31852e.
- **Punta y diff:** la punta es 7f07b11 y el diff es `e0f5c62...HEAD`.
- **De e506ed3 a 5170285** solo cambian `bitacoras/` y `revisiones/`: un grep que excluye esas dos carpetas no imprime nada. La cabecera de `ca2-r2.txt` y `ca3-r2.txt` (sha 5170285) sale, por tanto, del código de e506ed3.
- **Código de la ronda 2** (`git diff ba6655a e506ed3 -- backend`): solo cambian `RegistroServicio.java` (`MSG_PAIS`), `FronterasModulosTest.java` (solo líneas añadidas) y `VistaGeneralTest.java` (dos pruebas nuevas).
- **Worktree:** sigue limpio al terminar (`git status --short` no imprime nada).
- **Correcciones de plantilla:** apliqué las dos ya aceptadas. En CA-4, el grep es `testcase name="[A-Za-z0-9]*()"`. En CA-5, cada copia lleva `git init -q`. Usé `$JAVA_HOME/bin/java` y el puerto 18091, que estaba libre (0 sockets).

## Criterios de aceptación, verificados por mí
| # | Criterio | Comando que corrí | Resultado |
|---|---|---|---|
| 1 | Vista general, R-R6, R-R7 y lectura de vuelta | `./gradlew :backend:test --rerun --tests '*VistaGeneralTest'`, el grep del XML y el `python3 -I` del CA-1 | pasa: `tests="12" skipped="0" failures="0" errors="0"`. Las tres salidas de Python son idénticas a las esperadas |
| 2 | `synkra.paises` en `registro`; un valor inválido no arranca | Las pruebas con `--rerun`, el `diff` contra `8182b9d`, `bootJar` y el JAR con `SYNKRA_PAISES='co,PE'` | pasa: 5/5 sin saltos, `igual=0`, `salida=1`, conteos 1 y 4. El log no repite `co,PE` (0 apariciones) |
| 3 | El JAR sin clúster responde la vista general | El guion de dos arranques del CA-3 | pasa: `200`, la tupla esperada y las 8 claves ordenadas. Extra: `POST /api/paises` da 405 |
| 4 | Fronteras de `registro` | Las pruebas con `--rerun` y los greps (con el grep corregido) | pasa: 12/12. Salen las 9 pruebas previas y las 3 nuevas; `allowedDependencies` en la línea 2; `ajenos=1`, `internos=1`, `k8s-escrituras=1`, `beans=1` |
| 5 | Cada regla muerde | Mi guion: 13 mutaciones del plan y 15 extra, una copia `git archive` por mutación | pasa: las 28 dan `BUILD FAILED` y ningún `sed` quedó sin efecto. «conexión» cae en `laVistaNoRetiene…` con `t=5.018` y `TimeoutException`. «frontera» hace caer `elBackendRealRespetaSusFronteras` y `registroSoloDepende…` |
| 6 | Todo el backend sigue pasando | `./gradlew :backend:test --rerun :backend:check` y el bucle sobre los XML | pasa: `BUILD SUCCESSFUL in 35s`. Son 40 XML y 508 pruebas (506 de la ronda 1 más las 2 nuevas), todas con 0 saltos, fallos y errores |
| 7 | Diff acotado | `--stat`, `--name-status` de `backend/src/test` y conteo de `^-[^-]` | pasa: en pruebas solo hay M `FronterasModulosTest` y A, A. El conteo da 0. Ninguna ruta queda fuera de la lista permitida, salvo `bitacoras/U06-C-evidencia/**`, que sigue el precedente ya aceptado |

**Lo que pediste comprobar aparte**
- **`application.properties`:** `git show 8182b9d:… | diff - …` no imprime nada (`igual=0`). Las líneas de `synkra.paises` son idénticas.
- **Bean `Clock`:** no hay ningún `@Bean` en `registro`. `Clock` solo aparece en el reloj privado de `RegistroServicio` (líneas 18, 35, 46, 50 y 53), igual que en U06-A.

## Las respuestas de la ronda 1, verificadas
- **F-01 (NARANJA): cerrado.**
  - **E3** (la `imagen` sale del registro) da `BUILD FAILED` y caen `imagenYPluginsSonLosDelClusterNoLosDelRegistro` y `elJsonTambienTraeLaImagenYLosPluginsDelCluster`: en `cl` se esperaba A y llegó B.
  - **E4** (los `plugins` salen del registro) cae en las mismas dos pruebas: en `sv` se esperaba tamaño 2 y llegó 1.
  - **Desempate:** la mutación 7 también hace caer ahora la prueba del JSON (`48L` contra `45L`).
- **F-02 (AMARILLO): cerrado.**
  - **E7** (un `@RequestMapping("/api/z")` de método, sin `method`) y **E8** (un `@RequestMapping(method = POST)` de clase) hacen caer `registroNoTieneRutasDeEscritura`.
  - El arreglo es demasiado amplio: ver F-05.
- **F-03 (AMARILLO): cerrado.**
  - **E10** (`Fuga<T extends LecturaPais>`) y **E11** (un límite de tipo ajeno en un método `default`) caen en `laApiDeRegistroSoloMencionaTiposPropios`.
  - **E12** (anotación ajena de clase) y **E13** (anotación ajena de método) también caen ahí. Para las dos usé una anotación nueva, `io.synkra.plataforma.Marca`, creada solo en la copia.
  - **`startsWith("io.synkra.")`:** el punto se ve en el diff; no lo comprobé corriendo.
- **F-04 (AMARILLO): cerrado.** La constante `MSG_PAIS` reemplaza los tres literales. No cambia nada fuera de `registro/**`, y las pruebas de U06-A y U06-B siguen verdes (CA-6).

## Hallazgos

### F-05 · AMARILLO · FronterasModulosTest.java `escribe` y la revisión de anotaciones de clase · Rechaza un prefijo de clase que solo sirve rutas GET
- **El problema:** con el arreglo de F-02, un `@RequestMapping` sin `method` cuenta como escritura también cuando está en la clase.
  - En la clase, ese `@RequestMapping` solo fija el prefijo de la ruta. El verbo lo deciden los métodos.
  - Con métodos `@GetMapping`, la clase solo sirve GET. Es el estilo que ya usa `catalogo` (`RutasCores`, `RutasArtefactos`, `RutasPlugins`).
- **Cómo lo comprobé (mutación E9, solo en la copia):**
  - Añadí `@RequestMapping("/api")` en la clase `RutasVistaGeneral` y cambié `@GetMapping("/api/paises")` por `@GetMapping("/paises")`.
  - `VistaGeneralTest` sigue en 12/12 verde, así que la ruta GET funciona igual.
  - Aun así, `registroNoTieneRutasDeEscritura` falla: `FronterasModulosTest failures=1`.
- **Por qué no bloquea:** el error va hacia el lado estricto, ningún criterio se rompe y el código actual no usa prefijo de clase.
- **Cómo cerrarlo:** en la clase, contar solo un `method` explícito con un verbo de escritura. En el método, seguir contando también la ausencia de `method`.

## Tareas candidatas (defectos reales fuera de alcance)
- Ninguna nueva. La unificación de la validación de países sigue registrada como C-110.

## Rutas de transcripciones largas
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/9d987332-269f-4da9-9d66-0a5449231723/scratchpad/U06-C-r2/ca1.txt
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/9d987332-269f-4da9-9d66-0a5449231723/scratchpad/U06-C-r2/ca2.txt
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/9d987332-269f-4da9-9d66-0a5449231723/scratchpad/U06-C-r2/ca3.sh
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/9d987332-269f-4da9-9d66-0a5449231723/scratchpad/U06-C-r2/ca3.txt
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/9d987332-269f-4da9-9d66-0a5449231723/scratchpad/U06-C-r2/ca4.txt
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/9d987332-269f-4da9-9d66-0a5449231723/scratchpad/U06-C-r2/ca5-revisor.sh
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/9d987332-269f-4da9-9d66-0a5449231723/scratchpad/U06-C-r2/ca5-revisor.txt
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/9d987332-269f-4da9-9d66-0a5449231723/scratchpad/U06-C-r2/ca6.txt
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/9d987332-269f-4da9-9d66-0a5449231723/scratchpad/U06-C-r2/ca7.txt

```
VEREDICTO: VERDE
AMARILLO|backend/src/test/java/io/synkra/FronterasModulosTest.java (escribe, anotaciones de clase)|registroNoTieneRutasDeEscritura rechaza un prefijo de clase que solo sirve rutas GET (falso positivo, mutación E9)
INFORME: revisiones/U06-C/ronda-2.md
```
