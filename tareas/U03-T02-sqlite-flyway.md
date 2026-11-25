# U03-T02: SQLite en `SYNKRA_DATA` con Flyway

**Unidad:** U03: `Plataforma del backend`
**Historias que implementa:** H-18 (contribuye)
**Depende de:** U03-T01

---

## Alcance

**Dentro** (una línea, concreta):

> La base SQLite en `$SYNKRA_DATA/synkra.db` (por defecto `/data`), creada al arrancar, con Flyway aplicando una `V1__base.sql` sin tablas de negocio y un pool de una sola conexión (un escritor, RNF-07).

**Fuera** (lo que el codificador debe rechazar aunque lo vea roto):

- Tablas de negocio (`cat_*`, `reg_*`, `dep_*`, `seg_*`): son de U04, U06, U07 y U10.
- `ErrorApi` (U03-T03), salud personalizada (U03-T04), Spring Modulith (U03-T05), Dockerfile (U03-T06).
- JPA o Hibernate: el diseño usa Spring Data JDBC (D7).
- Cambios en `verificador/`, en la raíz de Gradle o en `settings.gradle.kts`.

---

## Archivos de contexto

- `aidlc-docs/construction/plans/U03-plataforma-backend-code-generation-plan.md`
- `aidlc-docs/inception/application-design/application-design.md` (D7 y la tabla de persistencia)
- `aidlc-docs/inception/requirements/requirements.md` (RNF-07: una réplica, un escritor)

---

## Criterios de aceptación

- [ ] **CA-1**: la prueba de la base pasa, sin caché, y no se saltó
  ```bash
  ./gradlew :backend:test --rerun --tests '*BaseDeDatosTest'
  grep -ho 'tests="[0-9]*" skipped="[0-9]*" failures="[0-9]*" errors="[0-9]*"' backend/build/test-results/test/TEST-*BaseDeDatosTest.xml
  ```
  Esperado: `BUILD SUCCESSFUL` y `tests` ≥ 3, `skipped="0" failures="0" errors="0"`. Las pruebas
  arrancan el contexto con `SYNKRA_DATA` en un directorio temporal y afirman: que existe
  `synkra.db` en ese directorio, que Flyway aplicó la versión `1`, y que el pool tiene
  `maximumPoolSize` = 1.

- [ ] **CA-2**: lectura de vuelta con un cliente SQLite independiente, sobre la aplicación arrancada
  ```bash
  ./gradlew :backend:bootJar
  export SYNKRA_DATA=$(mktemp -d)
  java -jar backend/build/libs/backend-*.jar --server.port=18080 & PID=$!
  timeout 90 bash -c 'until curl -sf localhost:18080/actuator/health >/dev/null; do sleep 1; done'
  python3 -I -c "import sqlite3,sys; c=sqlite3.connect(sys.argv[1]); print(c.execute('select version, success from flyway_schema_history').fetchall())" "$SYNKRA_DATA/synkra.db"
  kill $PID; wait $PID 2>/dev/null
  ```
  Esperado: `[('1', 1)]`. La máquina no tiene el cliente `sqlite3`; se usa el módulo `sqlite3`
  de Python, que lee el mismo archivo sin pasar por la aplicación.

- [ ] **CA-3**: reiniciar no vuelve a migrar ni borra la base (base de H-18)
  ```bash
  java -jar backend/build/libs/backend-*.jar --server.port=18080 & PID=$!
  timeout 90 bash -c 'until curl -sf localhost:18080/actuator/health >/dev/null; do sleep 1; done'
  python3 -I -c "import sqlite3,sys; c=sqlite3.connect(sys.argv[1]); print(c.execute('select count(*) from flyway_schema_history').fetchone()[0])" "$SYNKRA_DATA/synkra.db"
  kill $PID; wait $PID 2>/dev/null
  ```
  Esperado: `1`, con el mismo `SYNKRA_DATA` de CA-2.

- [ ] **CA-4**: sin `SYNKRA_DATA`, la ruta por defecto es `/data`
  ```bash
  ./gradlew :backend:test --rerun --tests '*RutaDatosPorDefectoTest'
  grep -ho 'tests="[0-9]*" skipped="[0-9]*" failures="[0-9]*" errors="[0-9]*"' backend/build/test-results/test/TEST-*RutaDatosPorDefectoTest.xml
  ```
  Esperado: `BUILD SUCCESSFUL` y `skipped="0" failures="0" errors="0"`. La prueba comprueba la URL
  JDBC resuelta (`jdbc:sqlite:/data/synkra.db`) **sin abrir ni crear nada en `/data`**.

---

## Plan de pruebas

- `BaseDeDatosTest` (`@SpringBootTest`) con `@TempDir` como `SYNKRA_DATA`.
- `RutaDatosPorDefectoTest`, que resuelve la propiedad sin arrancar el `DataSource`.

**Rojo primero:** escribe `BaseDeDatosTest` antes de la configuración, corre CA-1 y registra el
fallo. Después, el mismo comando pasando.

---

## Notas

- Si el directorio de `SYNKRA_DATA` no existe, la aplicación lo crea o falla con un mensaje que nombra
  la ruta. No crea la base en el directorio de trabajo.
- El puerto 18080 debe estar libre; si no, se reporta en la bitácora y se cambia el mismo número en
  CA-2 y CA-3.
