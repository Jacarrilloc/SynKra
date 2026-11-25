# Ronda 1 — U03-T02

VEREDICTO: NO-VERDE

Los cuatro criterios de aceptación pasan con mis propias corridas. El veredicto es NO-VERDE por un solo hallazgo NARANJA: el dialecto ANSI que se añadió para Spring Data JDBC genera SQL que SQLite rechaza.

Contexto verificado:
- **Tarea:** el hash coincide con `c34b208af15cb5d1161df49a576d5c33560924b1`.
- **Worktree:** HEAD está en `ac12a5b`, igual al sha reportado. Hay un solo commit sobre `m6r/U03-T01`.
- **Puerto:** el 18080 estaba libre antes y después.
- **`/data`:** no existe ni antes ni después de las pruebas.
- **Estado final:** el worktree quedó limpio (`git status --short` vacío).

## Criterios de aceptación, verificados por mí
| # | Criterio | Comando que corrí | Resultado |
|---|---|---|---|
| 1 | La prueba de la base pasa, sin caché y sin saltos | `./gradlew :backend:test --rerun --tests '*BaseDeDatosTest'` + grep del XML | pasa: `BUILD SUCCESSFUL`, `tests="3" skipped="0" failures="0" errors="0"`. La suite tardó 2,6 s, así que el contexto arrancó de verdad. |
| 2 | Lectura de vuelta con un cliente SQLite independiente | Comandos de CA-2 tal cual, con `SYNKRA_DATA=$(mktemp -d)` en el scratchpad | pasa: `[('1', 1)]`. El log de Flyway dice "Successfully applied 1 migration". |
| 3 | Reiniciar no vuelve a migrar | Comandos de CA-3 con el mismo `SYNKRA_DATA` | pasa: `1`. El log dice "Schema "main" is up to date. No migration necessary". |
| 4 | Sin `SYNKRA_DATA`, la ruta por defecto es `/data` | `./gradlew :backend:test --rerun --tests '*RutaDatosPorDefectoTest'` + grep | pasa: `tests="1" skipped="0" failures="0" errors="0"`. También pasa con `SYNKRA_DATA` exportado en el entorno, porque la prueba retira las fuentes de entorno y de sistema. `/data` no se creó. |

Comprobaciones adicionales:
- **Suite completa:** `./gradlew :backend:test --rerun` da `BUILD SUCCESSFUL`. BaseDeDatosTest tiene 3/0/0/0, RutaDatosPorDefectoTest 1/0/0/0 y SynkraAppTest 1/0/0/0.
- **Nota de la tarea sobre un directorio que no se puede crear:** con `SYNKRA_DATA=/proc/no-se-puede` la aplicación falla con `No se pudo crear el directorio de datos SYNKRA_DATA: /proc/no-se-puede`. Se cumple.
- **Directorio de trabajo:** no quedó ningún `*.db` en la raíz ni en `backend/`. La base no se crea donde no debe.
- **El pool de una conexión es real:**
  - El `DataSource` inyectado es `HikariDataSource` con `maximumPoolSize=1`, y `minimumIdle` hereda ese 1.
  - El log muestra un único "synkra-sqlite - Added connection".
  - Flyway usa ese mismo `DataSource`: no hay `spring.flyway.url`, y el log de Flyway apunta a la misma ruta jdbc.
  - `DataSourceAutoConfiguration` se retira porque existe un bean propio.

## Juicio sobre los cambios señalados
- **Se quitaron las exclusiones de autoconfiguración en `SynkraApp`.** Está dentro del alcance: el propio comentario de T01 decía que T02 configuraría el DataSource, SQLite y Flyway. Es correcto y CA-2 arranca bien.
- **`SynkraAppTest` (de T01) ahora usa `@TempDir`.** Está dentro del alcance. Es la consecuencia necesaria de quitar las exclusiones: sin ese cambio, la prueba intentaría crear `/data`. Es mínimo y correcto.
- **Bean `Dialect` ANSI.** Hace falta un bean de dialecto para que arranque el contexto con `spring-boot-starter-data-jdbc`, así que está dentro del alcance. Pero ANSI no es un dialecto adecuado para SQLite: ver F-01.
- **Fuera de alcance:** no hay cambios en `verificador/`, en la raíz de Gradle ni en `settings.gradle.kts`. No entra JPA. `V1__base.sql` solo contiene un comentario. No hay desborde.

## Hallazgos
### F-01 · NARANJA · backend/src/main/java/io/synkra/plataforma/BaseDeDatos.java:38-42 · El dialecto ANSI genera SQL de paginación y de bloqueo que SQLite rechaza
El Javadoc afirma "el ANSI basta para el uso previsto", pero eso no está verificado y resulta falso para la paginación. Lo comprobé con un programa en el scratchpad, con el classpath del bootJar. Uso el `SqlRenderer` de Spring Data Relational con `AnsiDialect.INSTANCE` y ejecuto el resultado en SQLite (sqlite-jdbc del jar):
```
FAIL SELECT t.x FROM t OFFSET 10 ROWS FETCH FIRST 5 ROWS ONLY -> [SQLITE_ERROR] (near "10": syntax error)
FAIL SELECT t.x FROM t FOR UPDATE -> [SQLITE_ERROR] (near "UPDATE": syntax error)
OK   SELECT t.x FROM t LIMIT 5 OFFSET 10
```
El efecto es que cualquier repositorio futuro que use `Pageable`, `findFirst…` o `findTop…` (paginan con `limit()`), o `@Lock`, fallará en tiempo de ejecución con un error de sintaxis. Lo que se espera es:
- Un `Dialect` propio para SQLite que extienda `AbstractDialect`, con `LimitClause` en la forma `LIMIT n OFFSET m` y un `LockClause` que no emita nada. Con un solo escritor (RNF-07) basta: SQLite serializa las escrituras.
- Una prueba que renderice una consulta paginada con ese dialecto y la ejecute contra SQLite.

Hoy no rompe ningún criterio porque todavía no hay repositorios, pero es un defecto real del código del diff que hereda el resto del proyecto.

### F-02 · AMARILLO · bitacoras/U03-T02.md:11-13 · No se puede comprobar el «rojo primero»
La bitácora resume el fallo en rojo, pero no pega la salida. La rama tiene un único commit, así que el estado en rojo no puede reproducirse a partir del diff. La afirmación queda sin verificar. No bloquea porque el plan no lo exige como criterio de aceptación.

### F-03 · AMARILLO · backend/src/main/resources/application.properties:3 · `spring.flyway.locations` es redundante
`spring.flyway.locations=classpath:db/migration` ya es el valor por defecto de Spring Boot. Esa línea sobra.

### F-04 · AMARILLO · RutaDatosPorDefectoTest.java:20-23 · La prueba resuelve la propiedad por su cuenta
La prueba carga `application.properties` a mano con `PropertiesPropertySourceLoader`. Si la propiedad pasa a `application.yml` o a un perfil, la prueba deja de reflejar lo que hace Spring Boot. Es aceptable mientras se cumpla la restricción de no abrir nada en `/data`.

Sobre los valores incrustados (`synkra.db`, `maximumPoolSize` 1, `/data`): los tres los fija la especificación o RNF-07. Además, `/data` vive en las propiedades y no en el código. No los cuento como hallazgo.

## Tareas candidatas (defectos reales fuera de alcance)
- Ninguna nueva. Las PRAGMA de SQLite (`busy_timeout`, `journal_mode=WAL`, `foreign_keys=ON`) no las pide la tarea. Conviene que el orquestador decida si las cubre U04, cuando lleguen las primeras tablas con claves foráneas.

## Rutas de transcripciones largas
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/5b13ba94-befc-454f-88e7-3182922701e5/scratchpad/ca1.log
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/5b13ba94-befc-454f-88e7-3182922701e5/scratchpad/ca4.log
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/5b13ba94-befc-454f-88e7-3182922701e5/scratchpad/full.log
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/5b13ba94-befc-454f-88e7-3182922701e5/scratchpad/ca2-app.log
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/5b13ba94-befc-454f-88e7-3182922701e5/scratchpad/ca3-app.log
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/5b13ba94-befc-454f-88e7-3182922701e5/scratchpad/prog/Dia.java (prueba del dialecto)

```
VEREDICTO: NO-VERDE
NARANJA|backend/src/main/java/io/synkra/plataforma/BaseDeDatos.java:38-42|El dialecto ANSI genera SQL de paginación y de bloqueo que SQLite rechaza
INFORME: revisiones/U03-T02/ronda-1.md
```
