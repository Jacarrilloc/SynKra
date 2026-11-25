# Ronda 3 — U03-T02

VEREDICTO: NO-VERDE

Los cuatro criterios pasan en corridas mías hechas ahora. F-05 y F-06 están corregidos. El veredicto es NO-VERDE por un NARANJA nuevo: dos de los tres convertidores de fecha que el diff añadió en el barrido fallan en casos que reproduje, y el tercero nunca se ejecuta.

Contexto verificado:
- **Tarea:** el hash coincide con `c34b208af15cb5d1161df49a576d5c33560924b1`.
- **Rama:** HEAD está en `31d2d0a`, el sha reportado. Hay 5 commits sobre `m6r/U03-T01`.
- **Puerto:** el 18080 estaba libre.
- **`/data`:** no existe ni antes ni después.
- **Base fuera de sitio:** no hay ningún `synkra.db` fuera del scratchpad.
- **Estado final:** el worktree quedó limpio (`git status --short` vacío).

## Criterios de aceptación, verificados por mí
| # | Criterio | Comando que corrí | Resultado |
|---|---|---|---|
| 1 | La prueba de la base pasa, sin caché y sin saltos | `./gradlew :backend:test --rerun --tests '*BaseDeDatosTest'` + grep del XML | pasa: `BUILD SUCCESSFUL`, `tests="3" skipped="0" failures="0" errors="0"`, 2,7 s. El contexto arrancó de verdad. |
| 2 | Lectura de vuelta con un cliente SQLite independiente | `bootJar` y después CA-2 tal cual, con `SYNKRA_DATA=$(mktemp -d)` en el scratchpad | pasa: `[('1', 1)]` |
| 3 | Reiniciar no vuelve a migrar | CA-3 con el mismo `SYNKRA_DATA` | pasa: `1`. El log dice `Schema "main" is up to date. No migration necessary.` |
| 4 | Sin `SYNKRA_DATA`, la ruta por defecto es `/data` | `./gradlew :backend:test --rerun --tests '*RutaDatosPorDefectoTest'` + grep | pasa: `tests="1" skipped="0" failures="0" errors="0"`. También pasa con `SYNKRA_DATA=/tmp/xx` exportado. No se creó `/data` ni `/tmp/xx`. |

Suite completa: `./gradlew :backend:test --rerun` da `BUILD SUCCESSFUL`, sin ningún `skipped`.

| Clase | tests / skipped / failures / errors | Tiempo |
|---|---|---|
| BaseDeDatosTest | 3/0/0/0 | 0,41 s |
| DialectoSqliteTest | 6/0/0/0 | 0,03 s |
| RepositorioSqliteTest | 4/0/0/0 | 0,42 s |
| RutaDatosPorDefectoTest | 1/0/0/0 | 0,005 s |
| SynkraAppTest | 1/0/0/0 | 2,7 s |

Los tiempos bajos tienen explicación:
- SynkraAppTest paga el arranque y las demás clases reutilizan el arranque en caliente.
- DialectoSqliteTest corre sobre SQLite en memoria.
- RutaDatosPorDefectoTest no abre ninguna base.

## Estado de los hallazgos de la ronda 2
- **F-05 (`saveAll` con id generado):** corregido.
  - `getIdGeneration()` ya no usa lotes.
  - `RepositorioSqliteTest.saveAllDevuelveLosIdsGenerados` lee los ids de vuelta y pasa.
  - Con `DialectoSqlite.INSTANCE` tal cual, en mi propio contexto de Spring Data JDBC, `save` y `saveAll` devuelven ids.
- **F-06 (cobertura de la prueba del dialecto):** corregido. La prueba cubre `limit` solo, `offset` solo con `LIMIT -1 OFFSET`, `ORDER BY` con paginación, y que no se usan lotes.

## ¿Los convertidores son desborde o están dentro del alcance?
Están dentro del alcance. El dialecto entró en esta tarea porque sin él Spring Data JDBC no funciona sobre SQLite (F-01). Que el dialecto traduzca los tipos que SQLite no tiene es parte de «SQLite con Flyway», igual que la paginación.

Lo que el diff no hace:
- No añade tablas de negocio.
- No añade migraciones.
- El esquema `elemento` y su repositorio viven solo en `src/test`.
- No toca `verificador/`, la raíz de Gradle, `settings.gradle.kts` ni `backend/build.gradle.kts`.
- No hay JPA.

Lo que sí es de C-05 es la regla de declarar las fechas como INTEGER, y no la exijo aquí. Lo que se juzga aquí es si los convertidores del diff son correctos, y no todos lo son (F-07).

## Hallazgos
### F-07 · NARANJA · backend/src/main/java/io/synkra/plataforma/DialectoSqlite.java:84-127 · Convertidores de fecha: dos fallan y uno nunca se ejecuta
Lo comprobé con un programa propio: `DialectoSqlite.INSTANCE` real, `AbstractJdbcConfiguration`, sqlite-jdbc 3.49.1 y una tabla con columnas `integer`. Los tipos almacenados los leí con el `sqlite3` de Python. Encontré tres problemas de la misma clase, agrupados aquí.

**a) Un valor pequeño vuelve como `Integer`, no como `Long`.** sqlite-jdbc devuelve `Integer` para cualquier valor que quepa en un int, es decir, a menos de unos 24,8 días de la época. Los convertidores solo aceptan `Long`.
- Leer `Instant.EPOCH` falla con `ConverterNotFoundException: No converter found capable of converting from type [java.lang.Integer] to type [java.time.Instant]`.
- Por arrastre falla también `findByInstanteBefore(2000-01-01)`, porque la consulta devuelve esa fila.
- `LocalDate.of(1970,1,1)` en UTC guarda `0` y fallaría igual.
- Que el origen sea `Number` en lugar de `Long` lo resolvería.

**b) `LocalDate` depende de la zona horaria del sistema y se corrompe sin avisar.** El driver guarda la medianoche local en milisegundos y `EnteroAFecha` lee con `ZoneId.systemDefault()`.
- Escribí con `Asia/Tokyo`: la columna guarda `1767279600000`.
- Leí con `UTC`: `fecha=2026-01-01`, cuando se había guardado `2026-01-02`.
- Lo mismo pasa con `1969-06-01`, que se lee como `1969-05-31`. No hay ningún error: la fecha sale un día corrida.
- La base vive en un volumen persistente que sobrevive a los reinicios (H-18). Si cambia la TZ del pod o de la imagen, cambian todas las fechas.
- Hay que fijar una zona explícita en la escritura y en la lectura, o quitar el convertidor y dejarlo todo a C-05.

**c) `EnteroAFechaHora` nunca se ejecuta y su javadoc es falso.**
- `LocalDateTime` no se guarda en milisegundos. Se guarda como TEXT ISO: `typeof(fecha_hora)='text'`, valor `'2026-01-02T03:04:05'`, aunque la columna sea `integer`.
- Se lee con la conversión de texto que Spring ya trae. `tiposComunesHacenViajeDeIdaYVuelta` pasa sin pasar por ese convertidor.
- El comentario de las líneas 95-98 («guarda las fechas como milisegundos… al leer vuelven como Long») no vale para `LocalDateTime`.

**Cobertura.** `RepositorioSqliteTest` prueba un solo valor (2026), en una sola zona y con el mismo proceso escribiendo y leyendo. Por eso no ve ni (a) ni (b).

Lo que se espera: convertidores correctos, con pruebas que lean de vuelta al menos `Instant.EPOCH` y un `LocalDate` escrito en una zona y leído en otra. La otra salida es retirar los convertidores de fecha de esta tarea y llevarlos a C-05. Eso lo decide el orquestador, pero hoy no pueden quedar en el código así.

### F-08 · AMARILLO · bitacoras/U03-T02.md (Ronda 3, «Intento descartado») · Afirmación sin evidencia
La bitácora dice que «Spring Data JDBC no los aplica a java.time», refiriéndose a los convertidores de escritura. No pega ni comando ni salida. Si se arregla F-07 por el lado de la escritura, hace falta esa evidencia. No bloquea por sí solo.

El literal `LIMIT -1` y el uso de `ZoneId.systemDefault()` no son valores visibles para el usuario. `ZoneId.systemDefault()` ya cuenta dentro de F-07(b).

## Tareas candidatas (defectos reales fuera de alcance)
- Ninguna nueva. C-04 (las PRAGMA) y C-05 (fechas como INTEGER) siguen pendientes del orquestador. F-07(b) refuerza C-05: la representación de las fechas tiene que quedar independiente de la zona horaria.

## Rutas de transcripciones largas
Todas en `/tmp/claude-1000/-home-casdav-Projects-Master-SynKra/5b13ba94-befc-454f-88e7-3182922701e5/scratchpad/r3/`:
- `ca1.log`, `ca4.log`, `ca2-app.log`, `ca3-app.log`, `full.log`: los criterios y la suite completa.
- `prog/io/synkra/plataforma/Verif3.java`: el programa de verificación.
- `v3-escribir.log` y `v3-leer-tokio.log`: los casos de (a) y (c), escritos con Bogotá y leídos con Tokio.
- `v3b-escribir-tokio.log` y `v3b-leer-utc.log`: el corrimiento de un día de (b).

```
VEREDICTO: NO-VERDE
NARANJA|backend/src/main/java/io/synkra/plataforma/DialectoSqlite.java:84-127|Convertidores de fecha: dos fallan y uno nunca se ejecuta
INFORME: revisiones/U03-T02/ronda-3.md
```
