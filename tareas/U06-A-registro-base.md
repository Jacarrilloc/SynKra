# U06-A: base del registro (migración, repositorios, transiciones y contexto truncado)

**Unidad:** U06: `Registro y vista general`
**Tareas del plan que agrupa:** U06-T01, U06-T02 y U06-T03
**Historias que implementa:** H-18 (base), H-16 (contribuye)
**Depende de:** U03 y U05 (terminadas). Sale de `b91f7d1` (punta VERDE de U05-T10). Corre en paralelo con U07-A: no usa nada de `despliegue`.

---

## Alcance

**Dentro** (una línea, concreta):

> `V3__registro.sql` con `reg_despliegue`, `reg_plugin` y `reg_contexto`; sus repositorios de Spring Data JDBC en `io.synkra.registro`; y `RegistroApi` (en `io.synkra.registro.api`) con `registrar` (R-R1: siempre `aplicando`, dentro de la transacción de quien llama) y `actualizarResultado` (R-R2 y R-R3: solo `aplicando → listo` o `aplicando → fallido`, y `fallido` exige contexto), que guarda el contexto truncado según R-R4 (últimas 200 líneas del log del core; cada texto a 64 KB como máximo, con la marca `[truncado]`).

**Fuera** (lo que el codificador debe rechazar aunque lo vea roto):

- U06-T04 a U06-T08: `obtener`, `historial`, `ultimoListo`, `vistaGeneral`, las rutas `GET /api/paises`, `GET /api/paises/{pais}/despliegues` y `GET /api/despliegues/{id}`, la concordancia (R-R5 a R-R7), la prueba de reinicio (R-R8), las reglas nuevas de `FronterasModulosTest` y el resumen. Ningún controlador en esta tarea.
- Todo lo de U07: `dep_plan`, `PaisPermitido`, la propiedad `synkra.paises`, y la regla de un solo despliegue `aplicando` por país (R-D7). El registro no la impone, ni en el esquema ni en el servicio.
- Importar `io.synkra.kubernetes` o `io.synkra.despliegue` desde `registro`: `ContextoFallo` es un tipo propio (ver la decisión 4).
- C-83 (qué eventos y qué logs junta U05): se decide en U07 (S5). Aquí solo se guarda y se trunca lo que llega.
- Cambios en `verificador/`, `core/`, `plugin-api/`, `plugins/`, `init-descarga/`, y en los módulos `catalogo`, `kubernetes` y `plataforma` del backend. Tampoco se tocan `application.properties`, `build.gradle.kts` ni `FronterasModulosTest.java`.

---

## Archivos de contexto

- `aidlc-docs/construction/plans/U06-registro-code-generation-plan.md` (U06-T01, T02 y T03)
- `aidlc-docs/construction/U06-registro/functional-design/business-rules.md` (R-R1 a R-R4)
- `aidlc-docs/construction/U06-registro/functional-design/domain-entities.md` (tablas y columnas)
- `unidades-y-tareas.md` (sección U06)
- `backend/src/main/resources/db/migration/V2__catalogo.sql` (estilo de migración: fechas `INTEGER`, `ON DELETE RESTRICT`, `CHECK`)
- `backend/src/main/java/io/synkra/catalogo/` (`Core`, `Plugin`, `CoreRepositorio`, `PluginRepositorio`, `CatalogoServicio`: repositorios sin CRUD y altas con `JdbcAggregateTemplate.insert`) y `catalogo/api/package-info.java` (`@NamedInterface("api")`)
- `backend/src/test/java/io/synkra/catalogo/CatalogoRepositoriosTest.java` (pruebas de esquema con SQL directo y `foreign_keys` activo)
- `backend/src/main/java/io/synkra/plataforma/BaseDeDatos.java` (archivo `synkra.db`, PRAGMA, pool de una conexión)
- `backend/src/main/java/io/synkra/kubernetes/api/ContextoFalloK8s.java` (lo que U07 le pasará al registro)
- `CANDIDATAS.md` (C-05 y C-83)

---

## Decisiones del orquestador

1. **Paquetes y nombres.**
   - Módulo `io.synkra.registro` (con `package-info.java`: «Módulo Registro: despliegues confirmados, su resultado y su contexto (prefijo `reg_`)»).
   - API pública en `io.synkra.registro.api`, con `@NamedInterface("api")` como `catalogo.api`. Tipos públicos:
     - `RegistroApi`;
     - `NuevoDespliegue`, `PluginDesplegado` y `ContextoFallo` (records);
     - `EstadoDespliegue` (`APLICANDO`, `LISTO`, `FALLIDO`) y `TipoDespliegue` (`DESPLIEGUE`, `REVERSION`) (enums);
     - `TransicionNoPermitidaException`.
   - Internas, no públicas, en `io.synkra.registro`:
     - entidades `Despliegue` (`reg_despliegue`), `PluginRegistrado` (`reg_plugin`) y `ContextoGuardado` (`reg_contexto`);
     - repositorios `DespliegueRepositorio`, `PluginRegistradoRepositorio` y `ContextoRepositorio`;
     - el servicio `RegistroServicio implements RegistroApi` (`@Service`);
     - `TruncadoContexto` (clase final con métodos estáticos).
2. **Esquema (`V3__registro.sql`).** Se siguen las columnas de `domain-entities.md`, con estas precisiones:
   - `reg_despliegue`:
     - `id INTEGER PRIMARY KEY` **sin** autoincremento: el id lo da quien registra (ver la decisión 3).
     - `pais TEXT NOT NULL` con `CHECK (length(pais) = 2 AND pais GLOB '[a-z][a-z]')`.
     - `tipo` con `CHECK (tipo IN ('despliegue','reversion'))`.
     - `origen_id INTEGER REFERENCES reg_despliegue (id) ON DELETE RESTRICT`, con `CHECK ((tipo = 'reversion') = (origen_id IS NOT NULL))`.
     - `core`, `version_core`, `imagen`, `creado_por` y `confirmado_por`: `TEXT NOT NULL`.
     - `confirmado_en INTEGER NOT NULL` y `terminado_en INTEGER`: epoch en ms (C-05). Lleva `CHECK ((estado = 'aplicando') = (terminado_en IS NULL))`.
     - `estado` con `CHECK (estado IN ('aplicando','listo','fallido'))`.
     - `veredicto TEXT NOT NULL CHECK (json_valid(veredicto))`.
     - Índice no único `reg_despliegue_pais` sobre `(pais, confirmado_en)`, para U06-T04.
   - `reg_plugin`:
     - `despliegue_id INTEGER NOT NULL REFERENCES reg_despliegue (id) ON DELETE RESTRICT`;
     - `nombre`, `version` y `sha256`: `TEXT NOT NULL`;
     - `etiquetas TEXT NOT NULL CHECK (json_valid(etiquetas))`;
     - `PRIMARY KEY (despliegue_id, nombre)`.
   - `reg_contexto`:
     - `despliegue_id INTEGER PRIMARY KEY REFERENCES reg_despliegue (id) ON DELETE RESTRICT`;
     - `log_core`, `log_init` y `eventos`: `TEXT NOT NULL`.
     - `eventos` es **texto, no JSON**, a diferencia de `domain-entities.md`. U05 entrega los eventos como líneas (`ContextoFalloK8s.eventos`) y truncar un JSON a 64 KB lo rompería. Se anota en la bitácora como desviación del diseño.
   - Sin tablas, índices ni restricciones de otros módulos.
3. **El id del despliegue lo da U07: es el id de su plan.** `NuevoDespliegue.id` (`long`, mayor que 0) se guarda tal cual. Así, `/api/despliegues/{id}` nombra lo mismo antes y después de confirmar. Un id repetido se rechaza (clave primaria) y la fila existente no cambia.
4. **Firmas de `RegistroApi`.**
   - `void registrar(NuevoDespliegue nuevo)`.
   - `void actualizarResultado(long id, EstadoDespliegue resultado, ContextoFallo contexto)`.
   - Records:
     - `NuevoDespliegue(long id, String pais, TipoDespliegue tipo, Long origenId, String core, String versionCore, String imagen, String creadoPor, String confirmadoPor, Instant confirmadoEn, String veredicto, List<PluginDesplegado> plugins)`;
     - `PluginDesplegado(String nombre, String version, String sha256, Map<String, String> etiquetas)`;
     - `ContextoFallo(String logCore, String logInit, String eventos)`. Un campo nulo se normaliza a `""`. Es un tipo propio, no `ContextoFalloK8s`: el mapeo lo hace U07, así el registro no depende de `kubernetes`.
   - En la base, los enums se guardan en minúsculas (`aplicando`, `despliegue`…). En las entidades son `String`, y la conversión vive en el servicio.
   - `plugins` nulo cuenta como lista vacía: un despliegue sin plugins es válido (core sin países, U02-T08).
   - `etiquetas` se serializa con el `ObjectMapper` de Spring, con las claves ordenadas.
5. **R-R1.**
   - `registrar` inserta siempre en `aplicando`, con `terminado_en` nulo.
   - Es `@Transactional` con la propagación por defecto (`REQUIRED`): se une a la transacción de quien llama, nunca abre otra (`REQUIRES_NEW` queda prohibido).
   - El despliegue y sus plugins entran juntos o no entra nada: un plugin repetido en `plugins` hace fallar todo el alta.
   - Las altas usan `JdbcAggregateTemplate.insert`.
   - Argumentos inválidos (nulo, país con otra forma, `REVERSION` sin `origenId` o `DESPLIEGUE` con él, textos obligatorios vacíos, `confirmadoEn` nulo, `veredicto` que no es JSON, sha256 que no son 64 hexadecimales en minúsculas): `IllegalArgumentException` y ninguna fila.
6. **R-R2 y R-R3.**
   - El cambio de estado es un `UPDATE … WHERE id = ? AND estado = 'aplicando'`, con `JdbcTemplate` o `NamedParameterJdbcTemplate`, dentro de una transacción. La guarda vive en el `WHERE`, no en una lectura previa.
   - Si actualiza 0 filas: `NoSuchElementException` si el id no existe, o `TransicionNoPermitidaException` (con el estado actual y el pedido en el mensaje) si ya terminó.
   - Pedir `APLICANDO` como resultado: `TransicionNoPermitidaException`.
   - `FALLIDO` con `contexto` nulo, o `LISTO` con `contexto` no nulo: `IllegalArgumentException`, y nada cambia (R-R3: `listo` no guarda contexto).
   - `FALLIDO` inserta la fila de `reg_contexto` en la misma transacción que el `UPDATE`.
   - `terminado_en` sale de un `Clock` privado del servicio (`Clock.systemUTC()`, con un constructor de paquete para las pruebas). **No** se declara un bean `Clock`: U07-A corre en paralelo y dos beans del mismo tipo chocarían.
7. **R-R4 (`TruncadoContexto`).**
   - Constantes `LINEAS_LOG_CORE = 200`, `BYTES_MAX = 64 * 1024` y `MARCA = "[truncado]"`.
   - `logCore(String)`: se queda con las últimas 200 líneas. Separa por `\n`; un `\n` final no cuenta como una línea más. Cada línea conserva su `\n`, también la última si lo tenía. Ese corte por líneas no pone marca. Después aplica `texto(...)`.
   - `texto(String)`: si el texto ocupa más de 65 536 bytes en UTF-8, guarda `MARCA + "\n"` seguida del **final** del texto. Todo junto mide como máximo 65 536 bytes, la marca incluida, y nunca se corta un carácter multibyte por la mitad. Si cabe, queda igual.
   - Se aplica `texto` a `log_init` y a `eventos`, y `logCore` a `log_core`, antes de insertar.
8. **SQLite de las pruebas, para leerlo de vuelta.**
   - `TransicionesRegistroTest` y `ContextoFalloTest` usan como `synkra.data` la carpeta fija `build/sqlite-pruebas/<NombreDeLaClase>` (relativa a `backend/`, que es el directorio de trabajo de Gradle). El `@DynamicPropertySource` la borra y la crea de nuevo antes de abrir la base.
   - No se borran filas entre pruebas: cada prueba usa sus propios ids, para que el archivo final se pueda leer con Python (CA-2 y CA-3).
   - `RegistroRepositoriosTest` puede usar `@TempDir` y limpiar entre pruebas, como `CatalogoRepositoriosTest`.
9. **Ids fijos de `TransicionesRegistroTest`.**
   - 1: `aplicando → listo`.
   - 2: `aplicando → fallido` con contexto.
   - 3: `listo` y luego `FALLIDO` rechazado.
   - 4: `fallido` y luego `LISTO` rechazado.
   - 5: `FALLIDO` sin contexto rechazado.
   - 6: `LISTO` con contexto rechazado.
   - 7: registrado dentro de una transacción que se revierte (`TransactionTemplate` con `setRollbackOnly`).
   - 8: `REVERSION` con `origenId = 1`.
   - 9: dos plugins con el mismo nombre (alta rechazada entera).
   - 10: registrado dos veces (la segunda se rechaza).
   - Cada alta válida lleva un plugin, `kyc-pe`.
10. **Bitácora en `bitacoras/U06-A.md`**, y el revisor en `revisiones/U06-A/`.

---

## Criterios de aceptación

- [ ] **CA-1**: U06-T01. Repositorios y esquema, y las tres tablas en la aplicación arrancada
  ```bash
  ./gradlew :backend:test --rerun --tests '*RegistroRepositoriosTest'
  grep -ho 'tests="[0-9]*" skipped="[0-9]*" failures="[0-9]*" errors="[0-9]*"' backend/build/test-results/test/TEST-*RegistroRepositoriosTest.xml
  ./gradlew :backend:bootJar
  export SYNKRA_DATA=$(mktemp -d)
  java -jar backend/build/libs/backend-*.jar --server.port=18086 & PID=$!
  timeout 90 bash -c 'until curl -sf localhost:18086/actuator/health >/dev/null; do sleep 1; done'
  python3 -I -c "import sqlite3,sys; c=sqlite3.connect(sys.argv[1]); print(sorted(r[0] for r in c.execute(\"select name from sqlite_master where type='table' and name like 'reg_%'\"))); print(c.execute('select version from flyway_schema_history order by installed_rank').fetchall()); print(sorted({(r[2], r[6]) for t in ('reg_despliegue','reg_plugin','reg_contexto') for r in c.execute('pragma foreign_key_list(' + t + ')')})); print(c.execute(\"select name from sqlite_master where type='index' and name='reg_despliegue_pais'\").fetchall())" "$SYNKRA_DATA/synkra.db"
  kill $PID; wait $PID 2>/dev/null
  ```
  Esperado:
  - `BUILD SUCCESSFUL`, `tests` ≥ 8 y `skipped="0" failures="0" errors="0"`.
  - Las pruebas afirman:
    - alta y lectura de un despliegue con dos plugins y su contexto;
    - que `confirmado_en` vuelve con el mismo instante en ms y `typeof` es `integer`;
    - que un `reg_plugin` con un `despliegue_id` inexistente falla por clave foránea;
    - que los `CHECK` de `estado`, `tipo`/`origen_id`, `terminado_en` y `json_valid` rechazan con `SQLITE_CONSTRAINT_CHECK`;
    - que borrar un despliegue con plugins se rechaza por `RESTRICT`;
    - que los repositorios no ofrecen `save`, `delete`, `findById` ni `existsById` (como en `CatalogoRepositoriosTest`).
  - La salida de Python:
    - `['reg_contexto', 'reg_despliegue', 'reg_plugin']`;
    - `[('1',), ('2',), ('3',)]`;
    - `[('reg_despliegue', 'RESTRICT')]`;
    - `[('reg_despliegue_pais',)]`.

  Si el puerto 18086 está ocupado, se anota en la bitácora y se usa otro en todo el criterio.

- [ ] **CA-2**: U06-T02. Transiciones R-R1 a R-R3, con lectura de vuelta del archivo de la prueba
  ```bash
  ./gradlew :backend:test --rerun --tests '*TransicionesRegistroTest'
  grep -ho 'tests="[0-9]*" skipped="[0-9]*" failures="[0-9]*" errors="[0-9]*"' backend/build/test-results/test/TEST-*TransicionesRegistroTest.xml
  python3 -I -c "import sqlite3,sys; c=sqlite3.connect(sys.argv[1]); print(c.execute('select id, tipo, origen_id, estado, terminado_en is not null from reg_despliegue order by id').fetchall()); print(c.execute('select despliegue_id from reg_contexto order by 1').fetchall()); print(c.execute('select despliegue_id, count(*) from reg_plugin group by 1 order by 1').fetchall())" backend/build/sqlite-pruebas/TransicionesRegistroTest/synkra.db
  ```
  Esperado:
  - `BUILD SUCCESSFUL`, sin saltos ni fallos, con una prueba por caso de la decisión 9. Cada rechazo afirma su excepción (`TransicionNoPermitidaException`, `IllegalArgumentException` o la de clave duplicada) y que la fila sigue como estaba.
  - Primera línea:
    `[(1, 'despliegue', None, 'listo', 1), (2, 'despliegue', None, 'fallido', 1), (3, 'despliegue', None, 'listo', 1), (4, 'despliegue', None, 'fallido', 1), (5, 'despliegue', None, 'aplicando', 0), (6, 'despliegue', None, 'aplicando', 0), (8, 'reversion', 1, 'aplicando', 0), (10, 'despliegue', None, 'aplicando', 0)]`.
    El 7 (revertido) y el 9 (plugins repetidos) no están.
  - Segunda línea: `[(2,), (4,)]`.
  - Tercera línea: `[(1, 1), (2, 1), (3, 1), (4, 1), (5, 1), (6, 1), (8, 1), (10, 1)]`.

- [ ] **CA-3**: U06-T03. Contexto truncado R-R4, con lectura de vuelta
  ```bash
  ./gradlew :backend:test --rerun --tests '*ContextoFalloTest'
  grep -ho 'tests="[0-9]*" skipped="[0-9]*" failures="[0-9]*" errors="[0-9]*"' backend/build/test-results/test/TEST-*ContextoFalloTest.xml
  python3 -I -c "import sqlite3,sys; c=sqlite3.connect(sys.argv[1]); print(c.execute(\"select despliegue_id, length(log_core) - length(replace(log_core, char(10), '')), substr(log_core, 1, 10), length(cast(eventos as blob)), substr(eventos, 1, 10), length(cast(log_init as blob)) from reg_contexto order by 1\").fetchall())" backend/build/sqlite-pruebas/ContextoFalloTest/synkra.db
  ```
  Esperado:
  - `BUILD SUCCESSFUL`, sin saltos ni fallos. Las pruebas de `TruncadoContexto` cubren:
    - un log de 1000 líneas (`linea-0001\n` … `linea-1000\n`) que guarda 200, de `linea-0801` a `linea-1000`;
    - 200 líneas o menos, que quedan iguales;
    - un texto de 100 KB (102 400 bytes ASCII) que queda en exactamente 65 536 bytes, empieza con `[truncado]\n` y termina con el final del original;
    - un texto de exactamente 65 536 bytes, que queda igual y sin marca;
    - 100 KB de `ñ` (2 bytes), que da como máximo 65 536 bytes de UTF-8 válido, sin un carácter partido;
    - 1000 líneas largas que pasan de 64 KB tras el corte por líneas y reciben los dos cortes;
    - nulos, que se guardan como `""`.
  - Una prueba de integración registra el id 1 y lo pasa a `FALLIDO` con ese log de 1000 líneas, `eventos` de 100 KB y `log_init` vacío. Python imprime `[(1, 200, 'linea-0801', 65536, '[truncado]', 0)]`.

- [ ] **CA-4**: rojo primero. Cada regla nueva tiene una prueba que muerde, demostrado en una copia
  ```bash
  D=$(mktemp -d); git archive HEAD | tar -x -C "$D"
  # Mutación anotada literalmente en la bitácora; una por regla, por ejemplo:
  #   R-R1: propagation = Propagation.REQUIRES_NEW en registrar
  #   R-R2: quitar "AND estado = 'aplicando'" del UPDATE
  #   R-R3: quitar el rechazo de FALLIDO sin contexto
  #   R-R4: que TruncadoContexto.texto devuelva el texto sin cortar
  sed -i '<mutación>' "$D/backend/src/main/java/io/synkra/registro/<Clase>.java"
  (cd "$D" && ./gradlew :backend:test --tests '*TransicionesRegistroTest' --tests '*ContextoFalloTest')
  grep -l '<failure' "$D"/backend/build/test-results/test/TEST-*.xml
  rm -rf "$D"
  ```
  Esperado: con cada una de las cuatro mutaciones (una copia por mutación), `BUILD FAILED`, y el XML de la clase dueña de la regla muestra al menos un `<failure` en la prueba de esa regla. La bitácora pega el `sed` y el nombre de la prueba que cayó. Sin `git stash` ni cambios en el worktree. Además, la bitácora registra que CA-1 falla antes de escribir `V3__registro.sql`.

- [ ] **CA-5**: todo el backend sigue pasando, incluidas las fronteras con el módulo nuevo
  ```bash
  ./gradlew :backend:test --rerun :backend:check
  for f in backend/build/test-results/test/TEST-*.xml; do grep -Ho 'tests="[0-9]*" skipped="[0-9]*" failures="[0-9]*" errors="[0-9]*"' "$f"; done
  grep -rn "io.synkra.kubernetes\|io.synkra.despliegue" backend/src/main/java/io/synkra/registro; echo "ajenos=$?"
  ```
  Esperado: `BUILD SUCCESSFUL` y ningún XML con `skipped`, `failures` o `errors` distinto de 0 (`FronterasModulosTest` pasa sin cambios con `registro`). `ajenos=1`.

- [ ] **CA-6**: el diff queda acotado
  ```bash
  git rev-parse --short HEAD; git diff --stat b91f7d1...HEAD
  ```
  Esperado: solo estas rutas. Se anota el sha.
  - `backend/src/main/resources/db/migration/V3__registro.sql`
  - `backend/src/main/java/io/synkra/registro/**`
  - `backend/src/test/java/io/synkra/registro/**`
  - `bitacoras/U06-A.md` y `revisiones/U06-A/`

---

**Rojo primero:** `RegistroRepositoriosTest` antes de la migración; `TransicionesRegistroTest` antes de `RegistroServicio`; las pruebas de `TruncadoContexto` antes del truncado. Cada rojo queda en la bitácora, y CA-4 demuestra que cada regla muerde con el código terminado.
