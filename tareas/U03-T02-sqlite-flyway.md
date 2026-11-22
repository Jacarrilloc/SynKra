# U03-T02: SQLite en `/data` con Flyway

**Unidad:** U03: `Plataforma del backend`
**Historias que implementa:** H-18 (contribuye)
**Depende de:** U03-T01

---

## Alcance

**Dentro** (una línea, concreta):

> La base SQLite en `$SYNKRA_DATA/synkra.db` (por defecto `/data`), creada al arrancar, con Flyway aplicando `V1__base.sql` vacía y un pool de una sola conexión.

**Fuera** (lo que el codificador debe rechazar aunque lo vea roto):

- Tablas de negocio (`cat_*`, `reg_*`, `seg_*`): son de U04, U06 y U10.
- `ErrorApi` (U03-T03), salud personalizada (U03-T04), Spring Modulith (U03-T05), Dockerfile (U03-T06).
- JPA o Hibernate: el diseño usa Spring Data JDBC (D7).
- Cambios en `verificador/` o en la raíz de Gradle.

---

## Archivos de contexto

- `aidlc-docs/construction/plans/U03-plataforma-backend-code-generation-plan.md`
- `aidlc-docs/inception/application-design/application-design.md` (D7 y la tabla de persistencia)
- `aidlc-docs/inception/requirements/requirements.md` (RNF-07: una réplica, un escritor)

---

## Criterios de aceptación

- [ ] **CA-1**: la prueba de la base pasa, sin caché
  ```bash
  ./gradlew :backend:test --rerun --tests '*BaseDeDatosTest'
  ```
  Esperado: `BUILD SUCCESSFUL`. La prueba arranca el contexto con `SYNKRA_DATA` en un directorio temporal y afirma que existe `synkra.db`, que Flyway aplicó la versión `1` y que el pool tiene `maximumPoolSize` = 1.

- [ ] **CA-2**: lectura de vuelta con la herramienta real, sobre la aplicación arrancada
  ```bash
  ./gradlew :backend:bootJar
  export SYNKRA_DATA=$(mktemp -d)
  java -jar backend/build/libs/backend-*.jar & PID=$!
  timeout 60 bash -c 'until curl -sf localhost:8080/actuator/health >/dev/null; do sleep 1; done'
  sqlite3 "$SYNKRA_DATA/synkra.db" 'select version from flyway_schema_history'
  kill $PID
  ```
  Esperado: `1`.

- [ ] **CA-3**: reiniciar no vuelve a migrar ni borra la base (base de H-18)
  ```bash
  java -jar backend/build/libs/backend-*.jar & PID=$!
  timeout 60 bash -c 'until curl -sf localhost:8080/actuator/health >/dev/null; do sleep 1; done'
  sqlite3 "$SYNKRA_DATA/synkra.db" 'select count(*) from flyway_schema_history'
  kill $PID
  ```
  Esperado: `1` (con el mismo `SYNKRA_DATA` de CA-2).

---

## Plan de pruebas

- `BaseDeDatosTest` (`@SpringBootTest`) con `@TempDir` como `SYNKRA_DATA`.
- Prueba negativa: sin `SYNKRA_DATA` la ruta es `/data` (se comprueba la propiedad resuelta, sin escribir en `/data`).

**Rojo primero:** escribe `BaseDeDatosTest` antes de la configuración y registra el fallo.

---

## Notas

- Se necesita el cliente `sqlite3` para CA-2 y CA-3. Si falta en la máquina, es un bloqueo que se reporta.
- El puerto 8080 debe estar libre; si no, se reporta en la bitácora y se usa `--server.port` en los dos criterios.
