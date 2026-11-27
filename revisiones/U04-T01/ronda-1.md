# Ronda 1 — U04-T01

VEREDICTO: NO-VERDE

## Comprobaciones previas
- Hash de la tarea: `git hash-object tareas/U04-T01-migracion-repositorios.md` = `19573bfd02b1f5c05f20af54851f253f94af7216`. Coincide.
- HEAD = `b7fdf20`. La base de fusión con `origin/main` es `02d1e24`. Hay un solo commit.
- `git status --short` del worktree salió vacío antes y después de la revisión. Todas las mutaciones y sondas se hicieron en copias `git archive HEAD` en el scratchpad.

## Criterios de aceptación, verificados por mí
| # | Criterio | Comando que corrí (desde la raíz del worktree) | Resultado |
|---|---|---|---|
| 1 | Repositorios sin caché ni saltos | `./gradlew :backend:test --rerun --tests '*CatalogoRepositoriosTest'` y después el `grep` del XML | pasa: `BUILD SUCCESSFUL`, `tests="6" skipped="0" failures="0" errors="0"`. El XML es de las 10:52:00 y la corrida, de las 10:52:05. La primera prueba tarda 0,614 s por el contexto. Ninguna prueba se salta. |
| 2 | Lectura de vuelta sobre el JAR | `bootJar`, `java -jar backend-0.1.0.jar --server.port=18080` con un `SYNKRA_DATA` nuevo, la espera `timeout 90` contra health y la línea Python del criterio | pasa: `['cat_core','cat_etiqueta','cat_plugin','cat_version_core']`, `[('1',),('2',)]`, `[('cat_plugin_sha256',)]`, `('wal',)` |
| 3 | Todo el backend | `./gradlew :backend:test --rerun` y el bucle sobre los XML | pasa: `BUILD SUCCESSFUL`. Los 9 XML salen con `skipped=0 failures=0 errors=0`: Fronteras 3, SynkraApp 2, Catalogo 6, BaseDeDatos 4, Dialecto 6, ErrorApi 13, RepositorioSqlite 4, RutaDatos 1, Salud 3. `FronterasModulosTest.verify()` pasa con `catalogo`. |
| 4 | Diff acotado | `git diff --stat origin/main...HEAD` | pasa, con una salvedad: el diff toca `BaseDeDatosTest.java` de U03, que no está en la lista. La juzgo abajo; no lo considero desborde. |

## Esquema, leído de vuelta con `sqlite3` de Python sobre la base de CA-2
Las cuatro tablas, las PK y los tipos coinciden con `domain-entities.md` y con la decisión 1:
- `registrado_en` y `subido_en` son INTEGER NOT NULL.
- `descripcion` es la única columna que admite nulos; el dominio no pide NOT NULL.
- Hay tres FK, todas con `on_delete=RESTRICT`. La de `cat_etiqueta` es compuesta `(core, plugin, version)` hacia `cat_plugin (core, nombre, version)`.
- `cat_plugin_sha256` es un índice no único (`unique=0`).

Sondas directas sobre una copia de esa base, con `foreign_keys=ON`:
- `origen='otro'` falla por el CHECK.
- Una etiqueta de un plugin inexistente falla por FK.
- Una versión de un core inexistente falla por FK.
- `delete from cat_core` con hijos falla por RESTRICT.
- Dos plugins con el mismo sha256 entran sin error.
- Un `subido_por` nulo falla por NOT NULL.

## PRAGMA
Probé con `PragmaReaperturaTest` en la copia. `synkra.data` apuntaba a un directorio que todavía no existía (`no/existe/aun`). La conexión 1 y tres conexiones físicas nuevas tras `evictConnection`, todas distintas, más una tras `softEvictConnections`, devolvieron `foreign_keys=1 busy_timeout=5000 journal_mode=wal`. El directorio nuevo se crea con `synkra.db`, `-wal` y `-shm`: WAL no rompe un `SYNKRA_DATA` nuevo.

Las desviaciones 4 (fechas en `long` ms) y 5 (PRAGMA como propiedades de sqlite-jdbc) las acepto: cumplen las decisiones 1 y 2.

La desviación 2 (afirmar la causa raíz `SQLITE_CONSTRAINT_*`) la acepto, con el traductor de excepciones como candidata.

## Hallazgos

### F-01 · ROJO · `backend/src/main/java/io/synkra/catalogo/{VersionCore,Plugin,Etiqueta}Repositorio.java:*` · los repositorios exponen `save` y `deleteById`, que pisan y borran filas de otras claves sin avisar
La desviación 1 deja `@Id` solo en `core`, mientras los tres repositorios extienden `ListCrudRepository`. Lo probé con un consumidor en la copia (`RiesgoRepositoriosTest`):
- **VersionCore.** Partiendo de `c/1.0.0`, `versiones.save(c/1.1.0)` termina OK. En la tabla queda solo `{core=c, version=1.1.0, imagen=img-1.1.0}`: la versión 1.0.0, que es inmutable, se reescribió sin ningún error. `findByCoreAndVersion(c,1.0.0)` devuelve vacío.
- **Plugin.** Partiendo de `c/p/1.0.0`, `plugins.save(c/q/2.0.0)` termina OK y en la tabla queda solo `c/q/2.0.0`. Se perdió otro plugin del mismo core.
- **Etiqueta.** Partiendo de `pais=co`, `etiquetas.save(region=andina)` termina OK y `pais=co` desaparece.
- **`deleteById("c")`** borra todas las versiones de un core, o todos sus plugins si no tienen etiquetas. Contradice R-C14, que no admite borrados.
- **`findById("c")`** con dos filas lanza `IncorrectResultSizeDataAccessException`. Con una sola fila devuelve una fila cualquiera de ese core.
- **`existsById("c")`** devuelve `true` para cualquier versión. `count()` funciona.

Esto choca con la decisión 3 y con el texto de CA-1: «un alta nueva tiene que insertar, no fallar ni actualizar en silencio». Las pruebas pasan solo porque usan `plantilla.insert`, pero el tipo entregado ofrece una vía de alta que actualiza en silencio, y U04-T02 la tendrá a mano.

`@Embedded` no es la salida en esta versión. Lo reproduje en la copia con spring-data-jdbc 3.5.13: `@Id @Embedded.Nullable Clave` genera `WHERE "cat_version_core"."CLAVE" = ?` y el insert falla. La afirmación del codificador se sostiene.

**Lo que se exige.** Que `VersionCoreRepositorio`, `PluginRepositorio` y `EtiquetaRepositorio` (y por coherencia `CoreRepositorio`) extiendan `org.springframework.data.repository.Repository<T, String>`. Que declaren solo las consultas derivadas por clave completa o por campos de búsqueda: `findByCore`, `findByCoreAndVersion`, `findBySha256`, `findByCoreAndPluginAndVersion` y, si hacen falta, `existsBy…` y `findAll`. Las altas siguen por `JdbcAggregateTemplate.insert`. Además, una prueba que lo ate, por ejemplo afirmar que `CrudRepository.class.isAssignableFrom(VersionCoreRepositorio.class)` es falso. Hay dos alternativas: `Persistable` con `isNew()=true`, que cierra `save` pero no `findById` ni `deleteById`, o `JdbcTemplate`.

### F-02 · NARANJA · `backend/src/main/resources/db/migration/V2__catalogo.sql` y `CatalogoRepositoriosTest` · nueve decisiones del esquema sin una prueba que las ate (barrido de clase)
El esquema actual es correcto (ver la lectura de vuelta), pero estas mutaciones de la copia pasan `CatalogoRepositoriosTest` y `BaseDeDatosTest` con `BUILD SUCCESSFUL`:
- M02: quitar la FK de `cat_version_core.core`.
- M03: quitar la FK compuesta de `cat_etiqueta`.
- M05c: cambiar el PK de `cat_plugin` a `(nombre, version)` con `UNIQUE (core, nombre, version)`. Dos cores ya no podrían tener el mismo plugin y la misma versión (R-C12).
- M06a: quitar el índice. Solo lo detecta CA-2, ninguna prueba JUnit.
- M06b: volver `UNIQUE` el índice. No lo detecta ni CA-2.
- M08: quitar el CHECK de `origen`.
- M09: cambiar `RESTRICT` por `CASCADE`.
- M11: dejar `subido_en` como TEXT anulable. Solo `registrado_en` tiene la prueba `typeof`.
- M12: quitar el NOT NULL de `imagen`.

Mutaciones que sí fallan, como debe ser:
- M01: sin la FK de `cat_plugin`.
- M04: sin `foreign_keys`.
- M05a y M05b: otros cambios del PK de `cat_plugin`.
- M07: `save` en lugar de `insert`, con 6 de 6 en rojo.
- M10: `registrado_en` como TEXT.
- M13: sin `busy_timeout`.
- M14: sin `journal_mode`.

Lo que se exige es una prueba por decisión del orquestador:
- FK de versión y FK de etiqueta;
- rechazo del CHECK;
- borrado rechazado por RESTRICT;
- dos plugins con el mismo sha256 aceptados (índice no único) y `pragma index_list` con `unique=0`;
- mismo plugin y versión en dos cores;
- `typeof(subido_en)='integer'`.

### F-03 · AMARILLO · `backend/src/test/java/io/synkra/plataforma/BaseDeDatosTest.java:37-49` · el cambio de U03 es inevitable, pero frágil
El cambio era necesario: con V2, el `queryForObject` de una sola fila de `flywayAplicoLaVersion1` revienta, y CA-3 exige que U03 pase. Es mínimo y está bien hecho. La prueba de PRAGMA cabe en «las pruebas nuevas» y pertenece a la clase que prueba, así que no lo considero desborde.

Lo frágil es `containsExactly("1","2")`: se romperá con cada migración futura (V3 de U05). Bastaría con `startsWith("1","2")` o con afirmar solo que V1 sigue aplicada.

### F-04 · AMARILLO · `bitacoras/U04-T01.md` (CA-4) · evidencia ausente
CA-4 dice «ver tras el commit», sin comando ni salida. La corrí yo y pasa, pero en la bitácora esa afirmación no está verificada.

## Tareas candidatas (defectos reales fuera de alcance)
- Un traductor de `SQLITE_CONSTRAINT_PRIMARYKEY`/`FOREIGNKEY` a `DuplicateKeyException`/`DataIntegrityViolationException`, para U04-T02 (R-C1). Coincido con el codificador.
- La clave compuesta de verdad, cuando Spring Data JDBC la soporte con este dialecto. En 3.5.13 no funciona (prueba `EmbProbe`).

## Rutas de transcripciones largas
Todas en `/tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/`:
- `ca1.log`
- `ca3.log`
- `ca2-app.log`
- `riesgo.log` (consumidor de F-01 y reapertura de PRAGMA)
- `emb.log` (prueba de `@Embedded`)
- `mutaciones.txt` y `mut-M*.log`; el script de mutaciones es `mutar.sh`
- Copias usadas: `copia/`, `emb/` y `mut/`. Las sondas están en `copia/backend/src/test/java/io/synkra/catalogo/RiesgoRepositoriosTest.java` y `copia/backend/src/test/java/io/synkra/plataforma/PragmaReaperturaTest.java`.

```
VEREDICTO: NO-VERDE
ROJO|backend/src/main/java/io/synkra/catalogo/*Repositorio.java|ListCrudRepository con @Id parcial: save() pisa otra versión/plugin/etiqueta en silencio y deleteById borra todo el core
INFORME: revisiones/U04-T01/ronda-1.md
```
