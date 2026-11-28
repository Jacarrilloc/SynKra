# U06 Registro y vista general: resumen de la unidad

Unidad: U06 (`backend/`, paquete `io.synkra.registro`). Historias: H-18 y H-19 (dueñas); contribuye a H-16 (plan de la unidad, `aidlc-docs/construction/plans/U06-registro-code-generation-plan.md`). Fecha de las corridas: 2026-11-27. Rama `m6r/U06-D`; las corridas de abajo se hicieron sobre la punta `1a01b96` (el sha completo está en la primera línea de cada archivo de `evidencia/`). Este resumen lo escribe U06-D (U06-T08) sin tocar código, pruebas, `build.gradle.kts` ni `application.properties`; `git diff --stat ade68f6...HEAD` sobre las carpetas de código sale vacío (CA-5, `bitacoras/U06-D-evidencia/ca5.txt`). Nada se corrió contra un clúster (AUTONOMIA-01). Las candidatas C-110, C-111 y C-120 se leyeron de `CANDIDATAS.md` de este árbol (eb33d68), que ya las trae.

## Qué expone

- **Paquetes:** `io.synkra.registro` (interno) y `io.synkra.registro.api` (único paquete público del módulo). Nombres tomados de `backend/src/main/java/io/synkra/registro/`.
- **Migración:** `backend/src/main/resources/db/migration/V3__registro.sql`, con tres tablas: `reg_despliegue` (CHECK de país, de `tipo`, de `estado`, de `veredicto` como JSON y las dos de coherencia tipo/origen y estado/`terminado_en`), `reg_plugin` y `reg_contexto`; las claves foráneas son `ON DELETE RESTRICT`; índice `reg_despliegue_pais` sobre `(pais, confirmado_en)`.
- **`RegistroApi`:** `registrar`, `actualizarResultado`, `obtener`, `historial` y `ultimoListo`.
- **`VistaGeneralApi`:** `vistaGeneral()`, que devuelve `List<VistaPais>`.
- **Rutas de solo lectura:** `GET /api/paises` (`RutasVistaGeneral`), `GET /api/paises/{pais}/despliegues` y `GET /api/despliegues/{id}` (`RutasRegistro`). No hay rutas de escritura; lo ata `FronterasModulosTest`.
- **Piezas internas:** `TruncadoContexto` (R-R4), `PaisesConfigurados` (lee `synkra.paises`), `Concordancia` (R-R6), `RegistroServicio`, `VistaGeneralServicio`, las entidades `Despliegue`, `PluginRegistrado` y `ContextoGuardado` con sus repositorios, y `CodigosRegistro` y `MotivosRegistro`.
- **Tipos públicos de `registro.api`:** `NuevoDespliegue`, `PluginDesplegado`, `ContextoFallo`, `DespliegueRegistrado`, `VistaPais`, `PluginCorriendo`, `LecturaCluster`, `EstadoDespliegue`, `TipoDespliegue` y `TransicionNoPermitidaException`.
- **Quién llama a `RegistroApi`:** `grep -rln 'RegistroApi\|VistaGeneralApi' backend/src/main | grep -v '/registro/'` no da ningún archivo en esta punta. Las tareas de U07 (`tareas/U07-D-confirmar.md`, `tareas/U07-E-vigilancia-reinicio.md`) son las que la usan; este resumen no describe nada más de U07.

Reglas del diseño (`functional-design/business-rules.md`) contra la clase de prueba que las ata; cada clase existe en `backend/src/test/java/io/synkra/registro/` (o en `io.synkra`, la última):

| Regla | Qué fija | Clase de prueba |
|---|---|---|
| R-R1 | registrar en `aplicando`, dentro de la transacción de quien llama | `TransicionesRegistroTest` |
| R-R2 | solo `aplicando` a `listo` o a `fallido`; lo terminado no cambia | `TransicionesRegistroTest` |
| R-R3 | `fallido` exige contexto; `listo` no lo guarda | `TransicionesRegistroTest` |
| R-R4 | contexto truncado (últimas 200 líneas del log del core; 64 KB con la marca `[truncado]`) | `ContextoFalloTest` |
| R-R5 | historial del más reciente al más antiguo | `HistorialTest` |
| R-R6 | concordancia con el último `listo` | `VistaGeneralTest` |
| R-R7 | la vista recorre los países de `synkra.paises` | `VistaGeneralTest` y `PaisesConfiguradosTest` |
| R-R8 | sobrevive a un reinicio | `RegistroReinicioTest` |
| U06-T07 | fronteras del módulo (sin regla R-R) | `FronterasModulosTest` |

`RegistroRepositoriosTest` ata el esquema de `V3__registro.sql` (U06-T01).

## Cómo se prueba

- **Sin clúster ni `kubectl`:** en `VistaGeneralTest` el `AdaptadorKubernetes` es un doble de Mockito (`@MockitoBean`); las propiedades van por `@DynamicPropertySource`.
- **SQLite:** cada clase usa su carpeta `build/sqlite-pruebas/<Clase>` y las lecturas de vuelta se hacen con `python3 -I` (no se usa `sqlite3`).
- **Reinicio:** `RegistroReinicioTest` abre dos contextos de Spring sobre el mismo archivo SQLite.
- **Filtro `*registro*`:** toma las clases de `io.synkra.registro.*`. `FronterasModulosTest` está en `io.synkra` y no entra en él; solo corre con `:backend:check`.

## Salidas reales

Todas se produjeron en esta tarea, con `--rerun`, sobre la punta `1a01b96`, con `JAVA_HOME=$HOME/.gradle/jdks/eclipse_adoptium-21-amd64-linux.2` (Java 21). Cada archivo de `evidencia/` (junto a este resumen) trae fecha, sha, comando y salida completa.

### Pruebas de `registro`

Comando exacto de `evidencia/gradle-registro.txt` y sus últimas 6 líneas, copiadas con `tail -6`:

```
$ ./gradlew :backend:test --rerun --tests '*registro*'; echo "salida=$?"
For more on this, please refer to https://docs.gradle.org/9.9.2/userguide/command_line_interface.html#sec:command_line_warnings in the Gradle documentation.

BUILD SUCCESSFUL in 13s
9 actionable tasks: 9 executed
Consider enabling configuration cache to speed up this build: https://docs.gradle.org/9.9.2/userguide/configuration_cache_enabling.html
salida=0
```

Conteo por clase, de los XML de `backend/build/test-results/test/` tomado del mismo árbol justo después (`evidencia/conteo-por-clase.txt`, el mismo comando que `U05-pod-pais/code/evidencia/conteo-por-clase.txt` con `*registro*` en vez de `*kubernetes*`):

```
registro.ContextoFalloTest tests="9" skipped="0" failures="0" errors="0"
registro.HistorialTest tests="8" skipped="0" failures="0" errors="0"
registro.PaisesConfiguradosTest tests="5" skipped="0" failures="0" errors="0"
registro.RegistroReinicioTest tests="1" skipped="0" failures="0" errors="0"
registro.RegistroRepositoriosTest tests="13" skipped="0" failures="0" errors="0"
registro.TransicionesRegistroTest tests="18" skipped="0" failures="0" errors="0"
registro.VistaGeneralTest tests="12" skipped="0" failures="0" errors="0"
```

Suman 66 pruebas (9 + 8 + 5 + 1 + 13 + 18 + 12), sin saltos ni fallos. Es la misma suma de las revisiones, 13 + 18 + 9 + 8 + 1 + 12 + 5, así que no hay diferencia que explicar.

### Todo el backend

Comando de `evidencia/gradle-check.txt` y sus últimas 6 líneas (`tail -6`):

```
$ ./gradlew :backend:test --rerun :backend:check; echo "salida=$?"
For more on this, please refer to https://docs.gradle.org/9.9.2/userguide/command_line_interface.html#sec:command_line_warnings in the Gradle documentation.

BUILD SUCCESSFUL in 41s
9 actionable tasks: 1 executed, 8 up-to-date
Consider enabling configuration cache to speed up this build: https://docs.gradle.org/9.9.2/userguide/configuration_cache_enabling.html
salida=0
```

Recuento de XML de `backend/build/test-results/test/` (`evidencia/conteo-xml.txt`: cuántos XML, cuántos no traen `skipped="0" failures="0" errors="0"` y la suma de `tests`):

```
$ ls backend/build/test-results/test/TEST-*.xml | wc -l
48
$ grep -L 'skipped="0" failures="0" errors="0"' backend/build/test-results/test/TEST-*.xml | wc -l
0
$ cat backend/build/test-results/test/TEST-*.xml | grep -o -h '<testsuite [^>]*' | grep -o ' tests="[0-9]*"' | grep -o '[0-9]*' | paste -sd+ | bc
566
```

Son 48 XML y 566 pruebas, y ninguno trae `skipped`, `failures` o `errors` distinto de 0. La última ronda de U06-C reportó 40 XML y 508 pruebas, en la punta de esa rama; la diferencia de hoy (8 XML y 58 pruebas) no se ajustó. Lo que sí se comprobó: `git diff --name-status --diff-filter=A 7f07b11 HEAD -- backend/src/test` lista como clases de prueba nuevas siete de `despliegue` (U07-A a U07-C) y `plataforma/RelojTest.java`, ocho en total, igual que los XML de más; las pruebas de esas ocho clases no se desglosaron una por una.

## Desviaciones del diseño

Todas anotadas por los codificadores en sus bitácoras; se dice quién la anotó y qué dijo el revisor.

1. **`reg_contexto.eventos` es texto, no JSON.** `domain-entities.md` dice JSON; U05 entrega los eventos como líneas y truncar un JSON a 64 KB lo rompería. Anotada en `bitacoras/U06-A.md` («Desviación del diseño (decisión 2)»). Los informes de `revisiones/U06-A/` no la comentan; la ronda 2 es VERDE.
2. **`RegistroReinicioTest` pasa `synkra.data` con `.run("--synkra.data=...", "--server.port=0")`** y no con `.properties(...)`, porque esas propiedades por defecto pierden frente a `application.properties` (`/data` no existe y da `AccessDeniedException`). Anotada en `bitacoras/U06-B.md`; el revisor la reprodujo y la juzgó justificada (`revisiones/U06-B/ronda-1.md`, «Desviación 1»). Origen de la nota de C-111.
3. **Mutación de desempate de R-R5.** El plan pedía quitar `, id desc`; esa mutación sobrevive por equivalente (el plan de SQLite recorre el índice al revés y los empates salen por rowid descendente). Se usó `id asc` y cae `historialDePeVaDelMasRecienteAlMasAntiguoYDesempataPorId`. Anotada en `bitacoras/U06-B.md`; aceptada por el revisor (`revisiones/U06-B/ronda-1.md`, F-01, AMARILLO).
4. **`jq '.[0].confirmado_en'` del plan se traduce a `confirmadoEn`** en el JSON. Anotada en `bitacoras/U06-B.md` (decisión 5); el informe de U06-B no la comenta.
5. **`vistaGeneral` va en `VistaGeneralApi`, no en `RegistroApi`** como dice `business-logic-model.md`, y el tipo se llama `VistaPais`, no `EstadoPais` (decisión 1 de U06-C, `bitacoras/U06-C.md`). El revisor: «Es aceptable» (`revisiones/U06-C/ronda-1.md`).
6. **R-R6 no compara la versión del core aparte:** el digest de la imagen ya identifica el build (decisión 6 de U06-C; `business-rules.md` dice «misma versión del core»). Anotada en `bitacoras/U06-C.md`; el revisor: «Es aceptable».
7. **`synkra.paises` se valida en `registro` (`PaisesConfigurados`)** copiando la regla de `PaisPermitido`; queda repetida (decisión 3 de U06-C, `bitacoras/U06-C.md`; candidata C-110). El informe de U06-C no la juzga como desviación; la unificación queda abierta.
8. **ArchUnit 1.4.2 no tiene `getRecordComponents`:** la regla `laApiDeRegistroSoloMencionaTiposPropios` revisa los campos de los `record`, y `registroSoloDependeDeKubernetesApiYPlataforma` usa la API de Modulith. Anotada en `bitacoras/U06-C.md` («Desviaciones y notas»); el revisor: «Es aceptable» las dos.
9. **`VistaGeneralTest` usa `@MockitoBean` y `@DynamicPropertySource`** en vez de `SpringApplicationBuilder`. Anotada en `bitacoras/U06-C.md`; el informe de U06-C no la comenta.
10. **`registrar` usa el id del plan** (lo da quien registra) y el registro no impone R-D7 (un `aplicando` por país): queda para U07. Anotada en `bitacoras/U06-A.md` («Candidatas»).
11. **Correcciones de plantilla en la tarea de U06-C:** el `grep` de nombres de prueba del CA-4 no casaba con los paréntesis (se usa `testcase name="[A-Za-z0-9]*()"`) y las copias de `git archive` para `FronterasModulosTest` necesitan `git init -q`. Anotadas en `bitacoras/U06-C.md` («Candidatas»); el revisor aplicó las dos (`revisiones/U06-C/ronda-1.md`). Esta desviación no estaba en la lista de la tarea; sale de las bitácoras.

## Trazabilidad

El plan nombra H-11 y H-20 como contribuciones de la unidad; ninguna tarea de U06 las ata por sí sola, así que no figuran en la tabla.

| Historia | Tareas que la cubren | Entregada por |
|---|---|---|
| H-18 (dueña) | U06-T01, U06-T02, U06-T04, U06-T06, U06-T07, U06-T08 | T01 y T02 por U06-A; T04 y T06 por U06-B; T07 por U06-C; T08 por U06-D |
| H-19 (dueña) | U06-T05 | U06-C |
| H-16 (contribuye) | U06-T03 | U06-A |

## Tareas

Los PR salen de `git log --merges` y `gh pr list --state merged`; las rondas, de `ls revisiones/U06-*/` sin los `-respuesta`. El veredicto es la línea `VEREDICTO:` de cada informe.

| Tarea | Qué construyó | Tareas del plan | PR | Rondas y veredictos |
|---|---|---|---|---|
| U06-A | `V3__registro.sql`, repositorios, `RegistroApi` con `registrar` y `actualizarResultado`, `TruncadoContexto` | U06-T01 a U06-T03 | #97 | ronda-1 (`VEREDICTO: NO-VERDE`), ronda-2 (`VEREDICTO: VERDE`) |
| U06-B | `obtener` e `historial`, `RutasRegistro` (las dos rutas de despliegues), prueba de reinicio | U06-T04 y U06-T06 | #99 | ronda-1 (`VEREDICTO: VERDE`) |
| U06-C | `ultimoListo`, `VistaGeneralApi`, `GET /api/paises`, `PaisesConfigurados`, `Concordancia`, fronteras de `registro` en `FronterasModulosTest` | U06-T05 y U06-T07 | #101 | ronda-1 (`VEREDICTO: NO-VERDE`), ronda-2 (`VEREDICTO: VERDE`) |
| U06-D | este resumen | U06-T08 | sin PR todavía | por revisar |

## Candidatas abiertas

Salen de `grep -n 'U06' CANDIDATAS.md` en este árbol (la fila de C-107 ya estaba en `main`; las otras entraron con la rama del orquestador, #102). El archivo es `CANDIDATAS.md` de la raíz de este árbol (no la ruta de `wt-orq-U04`); no se copió nada ni se cerró ni reabrió ninguna.

| Candidata | Qué es | A quién va / estado |
|---|---|---|
| C-107 | con el pool de una conexión, `REQUIRES_NEW` espera hasta agotar el tiempo de conexión; no usarlo | aceptada como candidata, no bloquea; avisada en las tareas de U07-D y U07-E |
| C-110 | validación de `synkra.paises` repetida en `despliegue` y `registro`; unificar en `plataforma` | aceptada como candidata: pulido después de U07-D |
| C-111 | patrón de país `[a-z]{2}` duplicado en `registro`; orden de plugins sin prueba; notas para pruebas futuras (`.run(...)`, `id asc`) | aceptada como candidata, no bloquea; el patrón se junta con C-110 |
| C-120 | falso positivo de `registroNoTieneRutasDeEscritura` con `@RequestMapping` de clase sin `method` | aceptada como candidata, no bloquea; pulido de U06 |

Relacionada: **C-90** (`leerPais` más laxo que el core) nombra a U06 en su última columna («para U06/U07, alinear `PlanPais`…»); su estado es «Aceptada como candidata por el orquestador». U06 no la resolvió.

## Cifras y su fuente

Cifras de pruebas, mutaciones, rondas y XML citadas fuera de las salidas pegadas. Cuando la revisión y la bitácora dan cifras distintas para lo mismo, se citan las dos y no se reconcilian. Las cifras de la corrida fresca salen de `evidencia/`, no de esta tabla. Esta tabla se verifica con `bitacoras/U06-D-evidencia/ca3.sh`.

| cifra | ruta | cadena |
|---|---|---|
| U06-A: 13 pruebas de repositorios (ronda 1 de la bitácora) | `bitacoras/U06-A.md` | `tests="13" skipped="0" failures="0" errors="0"` |
| U06-A: 13 pruebas de repositorios (revisor, ronda 2) | `revisiones/U06-A/ronda-2.md` | `tests="13" skipped="0" failures="0" errors="0"` |
| U06-A: 15 pruebas de transiciones en la ronda 1 (bitácora) | `bitacoras/U06-A.md` | `tests="15" skipped="0" failures="0" errors="0"` |
| U06-A: 15 pruebas de transiciones en la ronda 1 (revisor) | `revisiones/U06-A/ronda-1.md` | `tests="15" skipped="0" failures="0" errors="0"` |
| U06-A: 18 pruebas de transiciones tras la ronda 2 (bitácora) | `bitacoras/U06-A.md` | `tests="18" skipped="0" failures="0" errors="0"` |
| U06-A: 18 pruebas de transiciones tras la ronda 2 (revisor) | `revisiones/U06-A/ronda-2.md` | `tests="18" skipped="0" failures="0" errors="0"` |
| U06-A: 9 pruebas de contexto (bitácora) | `bitacoras/U06-A.md` | `tests="9" skipped="0" failures="0" errors="0"` |
| U06-A: 4 mutaciones de la ronda 1 (revisor) | `revisiones/U06-A/ronda-1.md` | `las 4 mutaciones de la bitácora` |
| U06-A: 9 mutaciones de CA-4 en la ronda 2 (revisor) | `revisiones/U06-A/ronda-2.md` | `Las 9 mutaciones dan` |
| U06-A: 36 XML en CA-5 (revisor, ronda 2) | `revisiones/U06-A/ronda-2.md` | `los 36 XML dan 0/0/0` |
| U06-A: mutación R-R1 con `REQUIRES_NEW` tarda 2 min 37 s (revisor) | `revisiones/U06-A/ronda-2.md` | `Tardó 2 min 37 s, por C-107` |
| U06-A: la misma mutación tarda 2 min 38 s (bitácora; no se reconcilia con la del revisor) | `bitacoras/U06-A.md` | `R-R1 (REQUIRES_NEW, 2 min 38 s por C-107)` |
| U06-A: 13 pruebas fallan sin V3 (rojo) | `bitacoras/U06-A.md` | `13 tests completed, 13 failed` |
| U06-B: 8 pruebas de historial (bitácora) | `bitacoras/U06-B.md` | `tests="8" skipped="0" failures="0" errors="0"` |
| U06-B: 8 pruebas de historial (revisor) | `revisiones/U06-B/ronda-1.md` | `tests="8" skipped="0" failures="0" errors="0"` |
| U06-B: 1 prueba de reinicio (revisor) | `revisiones/U06-B/ronda-1.md` | `tests="1" skipped="0"` |
| U06-B: 8 mutaciones en la lista de transcripciones del revisor | `revisiones/U06-B/ronda-1.md` | `8 mutaciones: las 5 de la tarea` |
| U06-B: 38 XML y 488 pruebas en CA-5 (revisor) | `revisiones/U06-B/ronda-1.md` | `38 XML y 488 pruebas` |
| U06-B: 17 errores de compilación en el rojo (bitácora) | `bitacoras/U06-B.md` | `17 errors` |
| U06-C: 10 pruebas de la vista general en la ronda 1 (bitácora) | `bitacoras/U06-C.md` | `tests="10" skipped="0" failures="0" errors="0"` |
| U06-C: 12 pruebas de la vista general tras la ronda 2 (bitácora) | `bitacoras/U06-C.md` | `tests="12" skipped="0" failures="0" errors="0"` |
| U06-C: 12 pruebas de la vista general tras la ronda 2 (revisor) | `revisiones/U06-C/ronda-2.md` | `tests="12" skipped="0" failures="0" errors="0"` |
| U06-C: 5 pruebas de `PaisesConfiguradosTest` (bitácora) | `bitacoras/U06-C.md` | `tests="5" ... failures="0" errors="0"` |
| U06-C: 13 mutaciones del plan, bitácora de la ronda 1 | `bitacoras/U06-C.md` | `trece mutaciones en copias de` |
| U06-C: 13 mutaciones del plan y 7 extra (revisor, ronda 1) | `revisiones/U06-C/ronda-1.md` | `13 mutaciones del plan más 7 extra` |
| U06-C: 13 mutaciones del plan y 15 extra (revisor, ronda 2) | `revisiones/U06-C/ronda-2.md` | `13 mutaciones del plan y 15 extra` |
| U06-C: las 28 mutaciones caen (revisor, ronda 2; no se reconcilia con la bitácora) | `revisiones/U06-C/ronda-2.md` | `las 28 dan` |
| U06-C: 40 XML y 506 pruebas en la ronda 1 (revisor) | `revisiones/U06-C/ronda-1.md` | `40 XML y 506 pruebas` |
| U06-C: 40 XML y 508 pruebas en la ronda 2 (revisor) | `revisiones/U06-C/ronda-2.md` | `40 XML y 508 pruebas` |
| U06-C: 40 XML en CA-6 de la bitácora | `bitacoras/U06-C.md` | `en los 40 XML` |
