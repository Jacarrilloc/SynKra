# Ronda 2 — U03-T02

VEREDICTO: NO-VERDE

Los cuatro criterios de aceptación pasan con corridas mías hechas ahora. El veredicto es NO-VERDE por un hallazgo NARANJA en el `DialectoSqlite` nuevo: la paginación y el bloqueo ya funcionan, pero `saveAll` de agregados con id generado falla con este dialecto.

Contexto verificado:
- **Tarea:** el hash coincide con `c34b208af15cb5d1161df49a576d5c33560924b1`.
- **Rama:** HEAD está en `01ab88a`, el sha reportado. Hay tres commits sobre `m6r/U03-T01`.
- **Puerto:** el 18080 estaba libre.
- **`/data`:** no existe ni antes ni después.
- **Base fuera de sitio:** no quedó ningún `*.db` fuera de `backend/build`.
- **Estado final:** el worktree quedó limpio (`git status --short` vacío).

## Criterios de aceptación, verificados por mí
| # | Criterio | Comando que corrí | Resultado |
|---|---|---|---|
| 1 | La prueba de la base pasa, sin caché y sin saltos | `./gradlew :backend:test --rerun --tests '*BaseDeDatosTest'` + grep del XML | pasa: `BUILD SUCCESSFUL`, `tests="3" skipped="0" failures="0" errors="0"`, en 2,7 s. El contexto arrancó de verdad. |
| 2 | Lectura de vuelta con un cliente SQLite independiente | `bootJar`, después los comandos de CA-2 tal cual, con `SYNKRA_DATA=$(mktemp -d)` en el scratchpad y `--server.port=18080` | pasa: `[('1', 1)]` |
| 3 | Reiniciar no vuelve a migrar | Comandos de CA-3 con el mismo `SYNKRA_DATA` | pasa: `1` |
| 4 | Sin `SYNKRA_DATA`, la ruta por defecto es `/data` | `./gradlew :backend:test --rerun --tests '*RutaDatosPorDefectoTest'` + grep | pasa: `tests="1" skipped="0" failures="0" errors="0"`. También pasa con `SYNKRA_DATA=/tmp/xx` exportado. No se creó `/data` ni `/tmp/xx`. |

Comprobaciones adicionales:
- **Suite completa:** `./gradlew :backend:test --rerun` da `BUILD SUCCESSFUL`. BaseDeDatosTest 3/0/0/0, DialectoSqliteTest 2/0/0/0, RutaDatosPorDefectoTest 1/0/0/0 y SynkraAppTest 1/0/0/0.
- **Fuera de alcance:** el diff no toca `verificador/`, la raíz de Gradle, `settings.gradle.kts` ni `backend/build.gradle.kts`. No hay JPA ni tablas de negocio, así que no hay desborde.

## Estado de los hallazgos de la ronda 1
- **F-01 (paginación y bloqueo):** corregido. Lo verifiqué contra el bootJar, no solo con su prueba. El SQL que genera `DialectoSqlite.INSTANCE`, ejecutado en sqlite-jdbc 3.49.1:
  - `limit` da `LIMIT 2` y funciona.
  - `offset` solo da `LIMIT -1 OFFSET 1` y funciona.
  - `limitOffset` con `orderBy` da `ORDER BY t.x LIMIT 2 OFFSET 1` y funciona.
  - El bloqueo con `orderBy` y límite no emite nada y funciona.
  - El bloqueo con `where` funciona.

  Además lo probé de punta a punta con un contexto de Spring Data JDBC (`AbstractJdbcConfiguration` más un repositorio real con este dialecto). Funcionan:
  - `findAll(PageRequest.of(1,2,Sort.by("x")))`
  - `findFirstByOrderByXDesc`
  - `findTop2By…`
  - `@Lock(PESSIMISTIC_WRITE) findByX`
  - `Page<…> findByXGreaterThan(…, Pageable)` con su conteo
  - `existsBy…`
  - `save()` uno a uno, que devuelve los ids generados
- **F-02 (rojo primero sin salida):** atendido. La bitácora pega la salida real del rojo de la ronda 2, que concuerda con lo que el ANSI producía en mi ronda 1. El rojo de la ronda 1 sigue sin poder reconstruirse; sigue siendo AMARILLO y no bloquea.
- **F-03 (`spring.flyway.locations` redundante):** corregido. La línea ya no está en `application.properties`.
- **F-04 (la prueba resolvía la propiedad por su cuenta):** corregido. `ConfigDataEnvironmentPostProcessor.applyTo` carga la configuración como lo hace Boot, sin fuentes de entorno ni de sistema. Pasa con y sin `SYNKRA_DATA` exportado.

## Hallazgos
### F-05 · NARANJA · backend/src/main/java/io/synkra/plataforma/DialectoSqlite.java:12-62 · `saveAll` de agregados con id generado falla con este dialecto
`DialectoSqlite` no sobrescribe `getIdGeneration()`, así que hereda `supportedForBatchOperations() == true`. sqlite-jdbc no devuelve las claves generadas en un insert por lotes, y Spring Data JDBC aborta. Lo comprobé con el mismo contexto de punta a punta, sobre `create table item (id integer primary key autoincrement, x integer)` y `record Item(@Id Long id, int x)`:
```
save() uno a uno, ids generados: [1, 2, 3, 4, 5]
saveAll FAIL: java.lang.IllegalArgumentException: After saving the identifier must not be null
idGeneration batch: true
dialect bean: class io.synkra.plataforma.DialectoSqlite
```
Con un dialecto que delega `limit()` y `lock()` en `DialectoSqlite` y solo cambia `IdGeneration.supportedForBatchOperations()` a `false`, `saveAll` funciona: `saveAll ids: [6, 7]`.

Lo que se espera:
- Que `DialectoSqlite` sobrescriba `getIdGeneration()` y devuelva un `IdGeneration` con `supportedForBatchOperations() == false`.
- Una prueba que lea de vuelta los ids generados tras un `saveAll` contra SQLite. Lo ideal es una prueba de punta a punta con un repositorio mínimo en `src/test`, no solo el render.

Es de la misma clase que F-01: el dialecto no está ajustado a SQLite y lo descubre el primer repositorio futuro, en ejecución. Hoy no rompe ningún criterio.

Nota del revisor: el defecto ya existía con `AnsiDialect` en la ronda 1 y no lo detecté entonces. En la ronda 1 solo probé el render del SQL, no la persistencia. El fallo de revisión es mío, no del codificador.

### F-06 · AMARILLO · backend/src/test/java/io/synkra/plataforma/DialectoSqliteTest.java · La prueba solo cubre `limitOffset` y el bloqueo sin `ORDER BY`
Lo cubrí por mi cuenta y funciona: `limit` solo, `offset` solo con `LIMIT -1 OFFSET`, y la combinación con `ORDER BY`. La prueba no cubre esos casos. El caso de `offset` sin `limit` es el más delicado de SQLite y merece su propia prueba. No bloquea.

Sobre los valores incrustados (`synkra.db`, pool de 1, `/data`, `LIMIT -1`): los fija la especificación, RNF-07 o la sintaxis de SQLite. No los cuento como hallazgo.

## Tareas candidatas (defectos reales fuera de alcance)
- Ninguna nueva. La C-04 (PRAGMA de SQLite) sigue siendo del orquestador, como indicó.

## Rutas de transcripciones largas
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/5b13ba94-befc-454f-88e7-3182922701e5/scratchpad/r2/ca1.log
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/5b13ba94-befc-454f-88e7-3182922701e5/scratchpad/r2/ca4.log
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/5b13ba94-befc-454f-88e7-3182922701e5/scratchpad/r2/ca2-app.log
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/5b13ba94-befc-454f-88e7-3182922701e5/scratchpad/r2/ca3-app.log
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/5b13ba94-befc-454f-88e7-3182922701e5/scratchpad/r2/full.log
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/5b13ba94-befc-454f-88e7-3182922701e5/scratchpad/r2/dialecto.log (punta a punta con `DialectoSqlite`)
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/5b13ba94-befc-454f-88e7-3182922701e5/scratchpad/r2/dialecto2.log (con el lote de ids desactivado)
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/5b13ba94-befc-454f-88e7-3182922701e5/scratchpad/r2/prog/io/synkra/plataforma/Verif.java y Verif2.java (programas de verificación)

```
VEREDICTO: NO-VERDE
NARANJA|backend/src/main/java/io/synkra/plataforma/DialectoSqlite.java:12-62|saveAll de agregados con id generado falla con este dialecto
INFORME: revisiones/U03-T02/ronda-2.md
```
