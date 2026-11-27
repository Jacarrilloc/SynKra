# U04-T01: Migración del catálogo y repositorios

**Unidad:** U04: `Catálogo`
**Historias que implementa:** H-01 (base)
**Depende de:** U03 (terminada), U01 (terminada)

---

## Alcance

**Dentro** (una línea, concreta):

> `V2__catalogo.sql` con las tablas `cat_core`, `cat_version_core`, `cat_plugin` y `cat_etiqueta` (claves foráneas e índice por `sha256`). Repositorios de Spring Data JDBC en `io.synkra.catalogo` y los PRAGMA de C-04 en el `DataSource`.

**Fuera** (lo que el codificador debe rechazar aunque lo vea roto):

- Reglas de negocio (R-C1 a R-C14), servicios, `CatalogoApi` y controladores: son de U04-T02 en adelante.
- Recepción de JAR y archivos en disco (U04-T04).
- La dependencia de `backend` hacia `:verificador`: entra en U04-T02.
- Tablas de otros módulos (`reg_*`, `dep_*`, `seg_*`).
- Cambios en `verificador/`, en la raíz de Gradle o en `settings.gradle.kts`.
- Los convertidores de fecha de `java.time` (C-05): aquí solo se aplica su regla de columnas `INTEGER`.

---

## Archivos de contexto

- `aidlc-docs/construction/plans/U04-catalogo-code-generation-plan.md` (U04-T01)
- `aidlc-docs/construction/U04-catalogo/functional-design/domain-entities.md` (tablas y claves)
- `aidlc-docs/construction/U03-plataforma-backend/code/summary.md` (dialecto, pool de una conexión)
- `backend/src/main/java/io/synkra/plataforma/BaseDeDatos.java`
- `backend/src/test/java/io/synkra/plataforma/RepositorioSqliteTest.java` (patrón de prueba con `@TempDir`)
- `CANDIDATAS.md` (C-04 y C-05)

---

## Decisiones del orquestador

1. **Esquema.** Se siguen las columnas de `domain-entities.md`, con estas precisiones:
   - `cat_version_core.core` → `cat_core(nombre)`.
   - `cat_plugin.core` → `cat_core(nombre)`.
   - `cat_etiqueta (core, plugin, version)` → `cat_plugin (core, nombre, version)`.
   - Todas las claves foráneas con `ON DELETE RESTRICT`: no hay borrados en el MVP (R-C14).
   - `registrado_en` y `subido_en` son `INTEGER NOT NULL` (epoch en milisegundos), según la regla de C-05.
   - `registrado_por` y `subido_por` son `TEXT NOT NULL`. Hasta U10 se guardan como `'anonimo'`.
   - `origen` en `cat_etiqueta` lleva `CHECK (origen IN ('descriptor','subida'))`.
   - Índice no único `cat_plugin_sha256` sobre `cat_plugin(sha256)`.
2. **PRAGMA (cierra C-04).** El `DataSource` abre la conexión con `foreign_keys=ON`, `busy_timeout=5000` y `journal_mode=WAL`. Se configuran en la URL o en las propiedades de sqlite-jdbc, no con un `execute` después de abrir.
3. **Claves compuestas y altas.** Spring Data JDBC trata como actualización un `save` de una entidad con clave asignada. Por eso las altas usan `JdbcAggregateTemplate.insert(...)` o una entidad `Persistable`. El criterio CA-1 lo prueba: un alta nueva tiene que insertar, no fallar ni actualizar en silencio.
4. Paquete `io.synkra.catalogo`, con las clases de persistencia **no públicas** o en un subpaquete interno. `catalogo.api` llega en U04-T02.

---

## Criterios de aceptación

- [ ] **CA-1**: los repositorios pasan sus pruebas sin caché y sin saltos
  ```bash
  ./gradlew :backend:test --rerun --tests '*CatalogoRepositoriosTest'
  grep -ho 'tests="[0-9]*" skipped="[0-9]*" failures="[0-9]*" errors="[0-9]*"' backend/build/test-results/test/TEST-*CatalogoRepositoriosTest.xml
  ```
  Esperado: `BUILD SUCCESSFUL`, `tests` ≥ 6 y `skipped="0" failures="0" errors="0"`. Las pruebas afirman:
  - alta y lectura de un core con dos versiones;
  - alta de un plugin con dos etiquetas y su lectura por `(core, nombre, version)`;
  - búsqueda de plugins por `sha256`;
  - un `registrado_en` que vuelve con el mismo instante en milisegundos;
  - que una versión de core repetida falla con la excepción de clave duplicada (no se sobrescribe);
  - que un plugin con un core inexistente falla por clave foránea, lo que demuestra que `foreign_keys=ON` está activo.

- [ ] **CA-2**: lectura de vuelta con un cliente SQLite independiente, sobre la aplicación arrancada
  ```bash
  ./gradlew :backend:bootJar
  export SYNKRA_DATA=$(mktemp -d)
  java -jar backend/build/libs/backend-*.jar --server.port=18080 & PID=$!
  timeout 90 bash -c 'until curl -sf localhost:18080/actuator/health >/dev/null; do sleep 1; done'
  python3 -I -c "import sqlite3,sys; c=sqlite3.connect(sys.argv[1]); print(sorted(r[0] for r in c.execute(\"select name from sqlite_master where type='table' and name like 'cat_%'\"))); print(c.execute('select version from flyway_schema_history order by installed_rank').fetchall()); print(c.execute(\"select name from sqlite_master where type='index' and name='cat_plugin_sha256'\").fetchall()); print(c.execute('pragma journal_mode').fetchone())" "$SYNKRA_DATA/synkra.db"
  kill $PID; wait $PID 2>/dev/null
  ```
  Esperado:
  - `['cat_core', 'cat_etiqueta', 'cat_plugin', 'cat_version_core']`;
  - `[('1',), ('2',)]`;
  - `[('cat_plugin_sha256',)]`;
  - `('wal',)`.

  La máquina no tiene el cliente `sqlite3`, así que se usa el módulo de Python.

- [ ] **CA-3**: las pruebas de U03 siguen pasando, incluidas las fronteras
  ```bash
  ./gradlew :backend:test --rerun
  for f in backend/build/test-results/test/TEST-*.xml; do grep -o 'tests="[0-9]*" skipped="[0-9]*" failures="[0-9]*" errors="[0-9]*"' "$f"; done
  ```
  Esperado: `BUILD SUCCESSFUL` y ningún XML con `skipped`, `failures` o `errors` distinto de 0. `FronterasModulosTest` pasa con el módulo `catalogo` nuevo.

- [ ] **CA-4**: el diff queda acotado
  ```bash
  git diff --stat origin/main...HEAD
  ```
  Esperado:
  - `backend/src/main/resources/db/migration/V2__catalogo.sql`;
  - `backend/src/main/java/io/synkra/catalogo/**`;
  - `BaseDeDatos.java`, solo por los PRAGMA;
  - las pruebas nuevas;
  - `CANDIDATAS.md` (C-04 cerrada);
  - la bitácora y `revisiones/U04-T01/`.

---

## Plan de pruebas

- `CatalogoRepositoriosTest` (`@SpringBootTest` con `@TempDir` como `synkra.data`, igual que `RepositorioSqliteTest`).

**Rojo primero:** se escribe `CatalogoRepositoriosTest` antes de la migración y se registra que CA-1 falla. Después el mismo comando debe pasar. Para la prueba de clave foránea también se registra el rojo: sin el PRAGMA, el alta con un core inexistente **no** falla.

---

## Notas

- Paquete `io.synkra.catalogo`.
- Si el puerto 18080 está ocupado, se anota en la bitácora y se usa otro en todo CA-2.
