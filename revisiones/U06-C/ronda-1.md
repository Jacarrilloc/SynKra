# Ronda 1 — U06-C

VEREDICTO: NO-VERDE

Corrí yo los siete criterios y todos pasan. Las trece mutaciones del plan muerden. Queda en pie un NARANJA de cobertura: dos mutaciones que añadí yo sobreviven con la suite en verde.

**Comprobaciones previas**
- El archivo de la tarea tiene el hash esperado: `git hash-object` da 004d66e158a2bc2ef9ceffb01e6fb5ab1f31852e.
- El diff es `e0f5c62...23abbd7`. El merge-base es e0f5c62.
- Lo que va de f73b9f0 a la punta solo toca `bitacoras/`, así que la evidencia del codificador sale del código actual.
- Los commits rojos 6ccd2dc y 30a9ab2 solo traen firmas que lanzan `UnsupportedOperationException`. Las pruebas no cambian después del rojo.
- El worktree sigue limpio al terminar (`git status --short` no imprime nada).

**Las dos correcciones de plantilla que aplicué**
1. En CA-4, el grep literal `testcase name="[A-Za-z]*"` no imprime nada (`literal=1`). Tampoco bastaría `[A-Za-z0-9]*"`, porque los nombres terminan en `()`. Usé `testcase name="[A-Za-z0-9]*()"`.
2. En CA-5 hice `git init -q` en cada copia de `git archive HEAD`.

## Criterios de aceptación, verificados por mí
| # | Criterio | Comando que corrí | Resultado |
|---|---|---|---|
| 1 | Vista general, R-R6 y R-R7, y lectura de vuelta de la base | `./gradlew :backend:test --rerun --tests '*VistaGeneralTest'`, grep del XML y el `python3 -I` del CA-1 | pasa: `tests="10" skipped="0" failures="0" errors="0"`; las tres salidas de Python son idénticas a las esperadas |
| 2 | `synkra.paises` en `registro`; un valor inválido no arranca | Las pruebas con `--rerun`, el `diff` contra `8182b9d`, `bootJar` y el JAR con `SYNKRA_PAISES='co,PE'` en el puerto 18091 | pasa: 5/5 sin saltos, `igual=0`, `salida=1`, conteos 1 y 4; el log no repite `co,PE` (0 apariciones) |
| 3 | El JAR sin clúster responde la vista general | El guion de dos arranques del CA-3 con `$JAVA_HOME/bin/java` en 18091 | pasa: `200`, la tupla esperada y las 8 claves ordenadas. Extra: `POST /api/paises` da 405 |
| 4 | Fronteras de `registro` | `./gradlew :backend:test --rerun --tests '*FronterasModulosTest'` y los greps, con el grep corregido | pasa: 12/12 sin saltos; salen las 9 pruebas previas y las 3 nuevas; `allowedDependencies` en la línea 2; `ajenos=1`, `internos=1`, `k8s-escrituras=1`, `beans=1` |
| 5 | Cada regla muerde | Mi guion propio: 13 mutaciones del plan más 7 extra, cada una en su copia | pasa para las 13: todas dan `BUILD FAILED` y cae la prueba dueña. «conexión» cae solo en `laVistaNoRetiene…`, con `t=5.022` y `TimeoutException`. «frontera» hace caer `elBackendRealRespetaSusFronteras` y `registroSoloDepende…`. Ver F-01 para las extra |
| 6 | Todo el backend sigue pasando | `./gradlew :backend:test --rerun :backend:check` y el bucle sobre los XML | pasa: `BUILD SUCCESSFUL in 39s`; 40 XML y 506 pruebas, todas con `skipped="0" failures="0" errors="0"` |
| 7 | Diff acotado | `git diff --name-status` y el conteo de `^-[^-]` en `FronterasModulosTest` | pasa: en código solo hay rutas de `registro/**`, `application.properties` y las de prueba esperadas (A, A y la M de `FronterasModulosTest`); conteo 0. Fuera del código solo está `bitacoras/U06-C*`; `bitacoras/U06-C-evidencia/**` no está en la lista, pero sigue el precedente de U06-A y U06-B |

**Lo que pediste comprobar aparte**
- **`application.properties`**: el `diff` contra `git show 8182b9d:…` sale vacío. Solo se añaden la línea en blanco, el comentario y `synkra.paises=${SYNKRA_PAISES:co,pe}`, al final del archivo y tras el comentario de U05-T03. La unión con U07-C debería ser limpia.
- **Bean `Clock`**: no hay ningún `@Bean` en `registro`. `Clock` solo aparece en el reloj privado que `RegistroServicio` ya tenía en U06-A, que este diff no toca.

## Las desviaciones declaradas, juzgadas con evidencia
- **`vistaGeneral` en `VistaGeneralApi`**: lo fija la decisión 1. Es aceptable.
- **R-R6 sin comparar la versión del core**: lo fija la decisión 6. Es aceptable.
- **ArchUnit 1.4.2 sin `getRecordComponents`**: la regla revisa todos los campos de un record, además de los métodos públicos. Muerde con la mutación 12 (un componente `LecturaPais`) y con mi extra E2 (un método `default` de `VistaGeneralApi` que devuelve `Optional<LecturaPais>`). Es aceptable.
- **La API de Modulith para `registroSoloDepende…`**: `getDependencies(...).uniqueModules()` funciona. Con la mutación 11 la prueba falla con `["catalogo", "kubernetes", "plataforma"]`. Es aceptable.
- **El multi-catch de «no disponible»**: mi extra 9b parte el catch y cambia solo `py`. Igual caen `daLosDiezPaises…` (`[…, null, false, false]`) y `getApiPaises…`. Así que cada caso muerde por separado. Es aceptable.

## Hallazgos

### F-01 · NARANJA · VistaGeneralTest (decisión 4, H-19) · No se ata que `imagen` y `plugins` salgan del clúster y no del registro
- La decisión 4 dice que `imagen` y `plugins` son lo leído del clúster. Es lo central de H-19: «qué corre de verdad».
- Las pruebas solo afirman `imagen` y `plugins` con contenido en `pe`. Ahí el registro (41) y el clúster coinciden, así que no distinguen el origen.
- Dos mutaciones extra sobreviven, ambas en `VistaGeneralServicio.java` y con `BUILD SUCCESSFUL` (10/10 y 12/12):
  - **E3**: la `imagen` toma la del último `listo` (`ultimo.map(DespliegueRegistrado::imagen).orElse(lectura.imagenCore())`).
  - **E4**: los `plugins` toman los del registro cuando hay último `listo`.
- Con E3 y E4, la vista mostraría el registro como si fuera el clúster, justo en los países que no concuerdan.
- Los datos de la decisión 9 ya permiten cerrarlo, porque en tres países el registro y el clúster difieren:
  - `cl`: el 48 tiene la imagen B y el clúster tiene A;
  - `sv`: el clúster trae además `kyc-extra` (`9`);
  - `co`: el clúster trae sha `f` y el registro sha `c`.
- Falta afirmar, por la API y por el JSON, la `imagen` de `cl` y los `plugins` de `sv` y de `co`.

### F-02 · AMARILLO · FronterasModulosTest `registroNoTieneRutasDeEscritura` · Solo mira anotaciones de método con verbo explícito
- Cumple la letra de la decisión 10. Mi extra E1 muerde: un `@RequestMapping(method = {GET, DELETE})` de método la hace caer.
- No detecta dos casos:
  - un `@RequestMapping("/x")` sin `method`, que acepta POST, PUT y DELETE;
  - un `@RequestMapping` a nivel de clase.

### F-03 · AMARILLO · FronterasModulosTest `ajenoALaApiDeRegistro` y su condición · Dos huecos menores
- `startsWith("io.synkra")` también casa con un paquete como `io.synkrax`.
- La condición no revisa las anotaciones públicas ni los límites de los parámetros de tipo de la clase (por ejemplo, `class X<T extends LecturaPais>`).

### F-04 · AMARILLO · RegistroServicio.java:157 · Literal repetido
- `"pais: dos letras minúsculas"` aparece ya por tercera vez (líneas 137, 157 y 221), y también el patrón `PAIS`.
- Sigue el estilo previo y no es visible por ninguna ruta de esta tarea, pero convendría una constante.

## Tareas candidatas (defectos reales fuera de alcance)
- Ninguna nueva. La unificación de la validación de países ya está registrada como C-110.
- Las dos correcciones de la plantilla (el grep de CA-4 y el `git init -q` en las copias de CA-5) conviene llevarlas a la plantilla de tareas.

## Rutas de transcripciones largas
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/9d987332-269f-4da9-9d66-0a5449231723/scratchpad/U06-C-r1/ca1.txt
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/9d987332-269f-4da9-9d66-0a5449231723/scratchpad/U06-C-r1/ca2.txt
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/9d987332-269f-4da9-9d66-0a5449231723/scratchpad/U06-C-r1/ca3.sh
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/9d987332-269f-4da9-9d66-0a5449231723/scratchpad/U06-C-r1/ca3.txt
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/9d987332-269f-4da9-9d66-0a5449231723/scratchpad/U06-C-r1/ca4.txt
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/9d987332-269f-4da9-9d66-0a5449231723/scratchpad/U06-C-r1/ca5-revisor.sh
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/9d987332-269f-4da9-9d66-0a5449231723/scratchpad/U06-C-r1/ca5-revisor.txt
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/9d987332-269f-4da9-9d66-0a5449231723/scratchpad/U06-C-r1/ca6.txt

```
VEREDICTO: NO-VERDE
NARANJA|backend/src/test/java/io/synkra/registro/VistaGeneralTest.java (decisión 4)|No se ata que imagen y plugins salgan del clúster: las mutaciones E3 y E4 sobreviven
INFORME: revisiones/U06-C/ronda-1.md
```
