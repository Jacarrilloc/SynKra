# U06-B: historial por país, detalle y reinicio

**Unidad:** U06: `Registro y vista general`
**Tareas del plan que agrupa:** U06-T04 y U06-T06
**Historias que implementa:** H-18, H-16 (contribuye: el detalle de un `fallido` trae su contexto)
**Depende de:** U06-A (terminada). Sale de `<punta VERDE de U06-A>`. Asume como dado todo lo que fijó U06-A: `V3__registro.sql`, los ids que da quien registra, las firmas de `RegistroApi.registrar` y `actualizarResultado`, `ContextoFallo` propio, `TruncadoContexto`, el `Clock` privado del servicio (sin bean `Clock`) y las carpetas `build/sqlite-pruebas/<Clase>`. Corre en paralelo con U07-B: no usa nada de `despliegue`.

---

## Alcance

**Dentro** (una línea, concreta):

> `RegistroApi.obtener(id)` y `RegistroApi.historial(pais)` (R-R5: del más reciente al más antiguo), las rutas `GET /api/paises/{pais}/despliegues` y `GET /api/despliegues/{id}` sobre ellas, y la prueba de que el registro sobrevive a cerrar el contexto de Spring y abrir otro sobre el mismo archivo SQLite (R-R8), las dos con lectura de vuelta contra el JAR real.

**Fuera** (lo que el codificador debe rechazar aunque lo vea roto):

- U06-T05, T07 y T08: `ultimoListo`, `vistaGeneral`, la ruta `GET /api/paises`, la concordancia (R-R6), la lista de países configurados (R-R7, así que esta tarea **no** lee `synkra.paises`), las reglas nuevas de `FronterasModulosTest` y el resumen de la unidad.
- Cualquier ruta de escritura en el registro (`POST`, `PUT`, `PATCH`, `DELETE`): solo U07 registra, y lo hace por `RegistroApi`, no por HTTP.
- Mostrar planes `pendientes` en `GET /api/despliegues/{id}`: un plan sin confirmar no está en el registro (H-17) y aquí da 404 (ver la decisión 6).
- Todo lo de U07: `dep_plan`, `PaisPermitido`, `synkra.paises`, la regla de un solo `aplicando` por país (R-D7).
- Importar `io.synkra.kubernetes`, `io.synkra.despliegue` o `io.synkra.catalogo` desde `registro`.
- Cambios en `V3__registro.sql`, en `registrar`, `actualizarResultado` o `TruncadoContexto` (salvo lo que pida la decisión 3 para leer), y en las pruebas de U06-A.
- Cambios en `verificador/`, `core/`, `plugin-api/`, `plugins/`, `init-descarga/`, y en los módulos `catalogo`, `kubernetes` y `plataforma` del backend. Tampoco se tocan `application.properties`, `build.gradle.kts` ni `FronterasModulosTest.java`.
- Nada de `kubectl` ni de clúster: esta tarea no habla con Kubernetes.

---

## Archivos de contexto

- `tareas/U06-A-registro-base.md` (contrato previo: decisiones 1 a 9)
- `aidlc-docs/construction/plans/U06-registro-code-generation-plan.md` (U06-T04 y T06)
- `aidlc-docs/construction/U06-registro/functional-design/business-rules.md` (R-R5 y R-R8), `domain-entities.md` y `business-logic-model.md` (rutas propias del registro)
- `aidlc-docs/inception/user-stories/stories.md` (H-16 y H-18)
- `unidades-y-tareas.md` (sección U06)
- En la punta de U06-A: `backend/src/main/java/io/synkra/registro/` y `registro/api/`, `V3__registro.sql`, y `backend/src/test/java/io/synkra/registro/` (estilo de `TransicionesRegistroTest` y de su carpeta fija)
- `backend/src/main/java/io/synkra/catalogo/RutasCores.java` (controlador de paquete con DTO en records) y `backend/src/test/java/io/synkra/catalogo/RutasCoresTest.java` (`RANDOM_PORT` con `TestRestTemplate`)
- `backend/src/main/java/io/synkra/plataforma/` (`ExcepcionApi`, `ErrorApi`, `DetalleError`, `CodigosError`, `ManejadorErrores`, `BaseDeDatos`)
- `CANDIDATAS.md` (C-05 y C-34)

---

## Decisiones del orquestador

1. **Nombres nuevos.**
   - En `io.synkra.registro.api` (público): el record `DespliegueRegistrado` y dos métodos nuevos en `RegistroApi`.
   - En `io.synkra.registro` (no públicos):
     - `RutasRegistro` (`@RestController`), con sus DTO como records anidados: `DespliegueDto` (detalle) y `ResumenDespliegueDto` (elemento del historial);
     - `CodigosRegistro` (`PAIS_INVALIDO = "PAIS_INVALIDO"`) y `MotivosRegistro` (textos de los motivos), como `CodigosCatalogo` y `MotivosCatalogo`.
   - `RegistroServicio` (de U06-A) implementa los dos métodos nuevos. No hay otro servicio.
2. **Firmas.**
   - `Optional<DespliegueRegistrado> obtener(long id)`.
   - `List<DespliegueRegistrado> historial(String pais)`.
   - `DespliegueRegistrado(long id, String pais, TipoDespliegue tipo, Long origenId, String core, String versionCore, String imagen, String creadoPor, String confirmadoPor, Instant confirmadoEn, EstadoDespliegue estado, Instant terminadoEn, String veredicto, List<PluginDesplegado> plugins, ContextoFallo contexto)`.
     - `plugins` va ordenado por `nombre` y nunca es nulo.
     - `contexto` solo es no nulo en `obtener` de un `fallido`; en `historial` siempre es nulo.
     - Las etiquetas de cada `PluginDesplegado` se leen del JSON de `reg_plugin` con el `ObjectMapper` de Spring.
   - Ambos métodos son `@Transactional(readOnly = true)`.
   - `historial` con un país que no son dos letras minúsculas (nulo incluido): `IllegalArgumentException`. Un país bien formado sin filas da lista vacía; no se mira `synkra.paises` (R-R7 es de U06-T05).
   - `obtener` de un id que no existe (o ≤ 0): `Optional.empty()`.
3. **R-R5 (orden).** El historial sale de un `SELECT … WHERE pais = ? ORDER BY confirmado_en DESC, id DESC` (usa el índice `reg_despliegue_pais` de U06-A). El desempate por `id` es parte de la regla: dos confirmaciones en el mismo milisegundo salen con la de id mayor primero. Los plugins se leen con una sola consulta por llamada (`WHERE despliegue_id IN (…)` o un `JOIN`), no una por despliegue. Si hace falta, se añaden métodos de lectura a los repositorios de U06-A, sin tocar sus métodos existentes.
4. **Rutas (`RutasRegistro`).**
   - `GET /api/paises/{pais}/despliegues` → 200 con una lista de `ResumenDespliegueDto`, en el orden de R-R5. País mal formado (`PE`, `p`, `per`, `p1`): 422 con `ErrorApi(PAIS_INVALIDO, "El país no es válido", [DetalleError("pais", MotivosRegistro.PAIS_FORMA)])`, con el motivo «El país debe ser un código de dos letras en minúscula (S4)». El motivo **no** repite lo recibido (C-34).
   - `GET /api/despliegues/{id}` → 200 con un `DespliegueDto`. Id inexistente: 404 con `ErrorApi(CodigosError.NO_ENCONTRADO, CodigosError.MSG_NO_ENCONTRADO, [])`. Un id que no es número lo resuelve la plataforma tal cual (la prueba fija lo que dé `ManejadorErrores` hoy, se espera 400 `SOLICITUD_INVALIDA`; si no es así, se anota en la bitácora y **no** se toca `plataforma`).
5. **Forma del JSON.** Se sigue la convención de la API que ya existe (`RutasPlugins`: claves en camelCase, como `coreCompatible`):
   - `ResumenDespliegueDto`: `id`, `pais`, `tipo`, `origenId`, `core`, `versionCore`, `imagen`, `plugins` (`[{nombre, version, sha256, etiquetas}]`), `creadoPor`, `confirmadoPor`, `confirmadoEn`, `estado`, `terminadoEn`. Sin `veredicto` ni `contexto`.
   - `DespliegueDto`: lo mismo, más `veredicto` (el JSON guardado, como objeto, no como texto: `ObjectMapper.readTree`) y `contexto` (`{logCore, logInit, eventos}` o `null`).
   - `tipo` y `estado` van en minúsculas, como en la base (`despliegue`, `reversion`, `aplicando`, `listo`, `fallido`).
   - Las fechas son `Instant` y salen como las escribe el `ObjectMapper` de Spring Boot (ISO-8601 en UTC, por ejemplo `2026-11-18T11:06:43.456Z`); `terminadoEn` es `null` mientras está `aplicando`.
   - El comando del plan (`jq '.[0].confirmado_en'`) se traduce a `confirmadoEn`. Se anota en la bitácora.
6. **El detalle solo muestra lo registrado.** `GET /api/despliegues/{id}` es de `registro` (D14: solo lee sus tablas). Como el id del despliegue es el id de su plan (decisión 3 de U06-A), un plan `pendiente` da 404 aquí hasta que se confirma. Quién muestra un plan pendiente lo decide una tarea posterior de U07, que no podrá declarar otro `GET` sobre esta misma ruta.
7. **R-R8 (`RegistroReinicioTest`).**
   - No es un `@SpringBootTest`: arranca la aplicación real dos veces con `new SpringApplicationBuilder(SynkraApp.class).properties("synkra.data=<carpeta>", "server.port=0").run()`, una detrás de otra, sobre la carpeta fija `build/sqlite-pruebas/RegistroReinicioTest` (se borra y se crea de nuevo **una sola vez**, antes del primer arranque).
   - Primer contexto: registra en `pe` los ids 31 (luego `LISTO`), 32 (luego `FALLIDO`, con contexto) y 33 (queda `aplicando`), y guarda `historial("pe")` y `obtener(32)`. Lo cierra y comprueba `isActive() == false`.
   - Segundo contexto: `historial("pe")` y `obtener(32)` son **iguales** (`equals` de records, plugins y contexto incluidos) a lo guardado; `actualizarResultado(33, LISTO, null)` funciona, y `actualizarResultado(31, FALLIDO, …)` sigue rechazado con `TransicionNoPermitidaException` (las reglas de U06-A valen tras el reinicio). Lo cierra.
   - Los dos contextos se cierran en un `finally`, también si una aserción falla.
8. **Carpetas de SQLite de las pruebas** (como la decisión 8 de U06-A): `HistorialTest` usa `build/sqlite-pruebas/HistorialTest`, sin borrar filas entre pruebas, y cada prueba usa sus propios ids.
9. **Ids fijos de `HistorialTest`** (todos con un plugin `kyc-pe`; `T0` es un instante fijo con milisegundos distintos de cero):
   - 21: `pe`, `T0 + 1 s`, luego `LISTO`.
   - 22: `pe`, `T0 + 3 s`, luego `FALLIDO` con contexto.
   - 23: `pe`, `T0 + 2 s`, queda `aplicando`.
   - 24: `co`, `T0 + 5 s`, luego `LISTO`.
   - 25: `pe`, `REVERSION` con `origenId = 21`, `T0 + 3 s` (empata con el 22), queda `aplicando`.
   - Orden esperado de `pe`: 25, 22, 23, 21.
10. **Bitácora en `bitacoras/U06-B.md`**, y el revisor en `revisiones/U06-B/`.

---

## Criterios de aceptación

- [ ] **CA-1**: U06-T04. Historial y detalle, con lectura de vuelta del archivo de la prueba
  ```bash
  ./gradlew :backend:test --rerun --tests '*HistorialTest'
  grep -ho 'tests="[0-9]*" skipped="[0-9]*" failures="[0-9]*" errors="[0-9]*"' backend/build/test-results/test/TEST-*HistorialTest.xml
  python3 -I -c "import sqlite3,sys; c=sqlite3.connect(sys.argv[1]); print(c.execute(\"select id from reg_despliegue where pais = 'pe' order by confirmado_en desc, id desc\").fetchall()); print(c.execute('select id, pais, tipo, origen_id, estado from reg_despliegue order by id').fetchall()); print(c.execute('select despliegue_id from reg_contexto').fetchall())" backend/build/sqlite-pruebas/HistorialTest/synkra.db
  ```
  Esperado:
  - `BUILD SUCCESSFUL`, `tests` ≥ 8 y `skipped="0" failures="0" errors="0"`. Las pruebas (por HTTP con `RANDOM_PORT` y `TestRestTemplate`, y por `RegistroApi`) afirman:
    - `GET /api/paises/pe/despliegues` da 200 con los ids `[25, 22, 23, 21]`, y `confirmadoEn` del primero es el máximo;
    - cada elemento trae las claves de `ResumenDespliegueDto` y **no** trae `veredicto` ni `contexto`; el 25 trae `tipo` `reversion` y `origenId` 21;
    - `GET /api/paises/co/despliegues` da `[24]` y `GET /api/paises/cl/despliegues` da `[]` con 200;
    - `PE`, `p`, `per` y `p1` dan 422 `PAIS_INVALIDO` con un detalle de campo `pais`, y el motivo no contiene lo recibido;
    - `GET /api/despliegues/22` trae `estado` `fallido`, `contexto` con exactamente las claves `eventos`, `logCore` y `logInit`, y `veredicto` como objeto JSON;
    - `GET /api/despliegues/21` trae `estado` `listo`, `terminadoEn` no nulo y `contexto` `null`; el 23 trae `terminadoEn` `null`;
    - `GET /api/despliegues/999` da 404 `NO_ENCONTRADO`; `GET /api/despliegues/abc` da lo que fije la decisión 4;
    - `RegistroApi.obtener(999)` es vacío; `historial("PE")` y `historial(null)` lanzan `IllegalArgumentException`.
  - Python:
    - `[(25,), (22,), (23,), (21,)]`;
    - `[(21, 'pe', 'despliegue', None, 'listo'), (22, 'pe', 'despliegue', None, 'fallido'), (23, 'pe', 'despliegue', None, 'aplicando'), (24, 'co', 'despliegue', None, 'listo'), (25, 'pe', 'reversion', 21, 'aplicando')]`;
    - `[(22,)]`.

- [ ] **CA-2**: U06-T06. Sobrevive al reinicio (R-R8), con lectura de vuelta del archivo de la prueba
  ```bash
  ./gradlew :backend:test --rerun --tests '*RegistroReinicioTest'
  grep -ho 'tests="[0-9]*" skipped="[0-9]*" failures="[0-9]*" errors="[0-9]*"' backend/build/test-results/test/TEST-*RegistroReinicioTest.xml
  python3 -I -c "import sqlite3,sys; c=sqlite3.connect(sys.argv[1]); print(c.execute('select id, estado, terminado_en is not null from reg_despliegue order by id').fetchall()); print(c.execute('select version from flyway_schema_history order by installed_rank').fetchall()); print(c.execute('select despliegue_id from reg_contexto').fetchall())" backend/build/sqlite-pruebas/RegistroReinicioTest/synkra.db
  ```
  Esperado:
  - `BUILD SUCCESSFUL`, sin saltos ni fallos, con lo que pide la decisión 7.
  - Python:
    - `[(31, 'listo', 1), (32, 'fallido', 1), (33, 'listo', 1)]` (el 33 pasó a `listo` en el segundo contexto);
    - `[('1',), ('2',), ('3',)]` (el segundo arranque no reaplica migraciones; en esta rama no hay V4);
    - `[(32,)]`.

- [ ] **CA-3**: el JAR real, reiniciado sobre el mismo `SYNKRA_DATA`, responde igual (H-18, «borro el pod y el historial sigue igual»)
  ```bash
  ./gradlew :backend:bootJar
  export SYNKRA_DATA=$(mktemp -d); R=$(mktemp -d)
  arranca() { java -jar backend/build/libs/backend-*.jar --server.port=18089 > "$R/log-$1.txt" 2>&1 & PID=$!; timeout 90 bash -c 'until curl -sf localhost:18089/actuator/health >/dev/null; do sleep 1; done'; }
  # 1. Primer arranque: solo migra. Se para y se siembran filas con sqlite3 (el registro no tiene ruta de escritura).
  arranca 0; kill $PID; wait $PID 2>/dev/null
  python3 -I - "$SYNKRA_DATA/synkra.db" <<'EOF'
  import sqlite3, sys, json
  c = sqlite3.connect(sys.argv[1])
  ver = json.dumps({"global": "APROBADA", "porPlugin": [], "motivosGlobales": []})
  img = "localhost:5001/synkra-core@sha256:" + "a" * 64
  filas = [(11, "pe", 1795000001123, "listo", 1795000001999), (12, "pe", 1795000003456, "fallido", 1795000003999),
           (13, "pe", 1795000002789, "aplicando", None), (14, "co", 1795000005321, "listo", 1795000005999)]
  for i, pais, en, estado, fin in filas:
      c.execute("insert into reg_despliegue (id, pais, tipo, origen_id, core, version_core, imagen, creado_por, confirmado_por,"
                " confirmado_en, estado, terminado_en, veredicto) values (?, ?, 'despliegue', null, 'synkra-core', '2.1.0', ?,"
                " 'ana', 'luis', ?, ?, ?, ?)", (i, pais, img, en, estado, fin, ver))
      c.execute("insert into reg_plugin (despliegue_id, nombre, version, sha256, etiquetas) values (?, 'kyc-pe', '1.3.0', ?, ?)",
                (i, "b" * 64, json.dumps({"pais": pais})))
  c.execute("insert into reg_contexto (despliegue_id, log_core, log_init, eventos) values (12, 'linea-1\n', '', 'Warning BackOff: x')")
  c.commit()
  EOF
  consulta() {
    curl -s localhost:18089/api/paises/pe/despliegues | python3 -I -c "import json,sys; l=json.load(sys.stdin); print([d['id'] for d in l], l[0]['confirmadoEn'], sorted(l[0]))"
    curl -s localhost:18089/api/despliegues/12 | python3 -I -c "import json,sys; d=json.load(sys.stdin); print(d['estado'], d['confirmadoPor'], sorted(d['contexto']), d['contexto']['eventos'], d['veredicto']['global'], d['plugins'][0]['etiquetas'])"
    curl -s -o /dev/null -w '%{http_code}\n' localhost:18089/api/despliegues/99
    curl -s -o /dev/null -w '%{http_code}\n' localhost:18089/api/paises/PE/despliegues
  }
  # 2. Segundo y tercer arranque sobre el mismo archivo: la misma salida.
  arranca 1; consulta > "$R/a.txt"; kill $PID; wait $PID 2>/dev/null
  arranca 2; consulta > "$R/b.txt"; kill $PID; wait $PID 2>/dev/null
  cat "$R/a.txt"; diff "$R/a.txt" "$R/b.txt"; echo "diff=$?"
  ```
  Esperado:
  - `a.txt`:
    - `[12, 13, 11] 2026-11-18T11:06:43.456Z ['confirmadoEn', 'confirmadoPor', 'core', 'creadoPor', 'estado', 'id', 'imagen', 'origenId', 'pais', 'plugins', 'terminadoEn', 'tipo', 'versionCore']`;
    - `fallido luis ['eventos', 'logCore', 'logInit'] Warning BackOff: x APROBADA {'pais': 'pe'}`;
    - `404`;
    - `422`.
  - `diff=0`.

  Si el puerto 18089 está ocupado, se anota en la bitácora y se usa otro en todo el criterio. La bitácora pega `a.txt` y el `diff` tal cual.

- [ ] **CA-4**: rojo primero. Cada regla nueva tiene una prueba que muerde, demostrado en una copia
  ```bash
  D=$(mktemp -d); git archive HEAD | tar -x -C "$D"
  # Mutación anotada literalmente en la bitácora; una por regla, por ejemplo:
  #   R-R5 orden:      «confirmado_en DESC» pasa a «confirmado_en ASC»
  #   R-R5 desempate:  quitar «, id DESC»
  #   R-R5 país:       quitar «pais = ? AND» (o el filtro equivalente) de la consulta del historial
  #   detalle:         que obtener devuelva siempre contexto nulo
  #   R-R8:            en BaseDeDatos.urlJdbc, devolver "jdbc:sqlite::memory:" (solo en la copia)
  sed -i '<mutación>' "$D/backend/src/main/java/io/synkra/<ruta>.java"
  (cd "$D" && ./gradlew :backend:test --tests '*HistorialTest' --tests '*RegistroReinicioTest')
  grep -l '<failure' "$D"/backend/build/test-results/test/TEST-*.xml
  rm -rf "$D"
  ```
  Esperado: con cada una de las cinco mutaciones (una copia por mutación), `BUILD FAILED`, y el XML de la clase dueña de la regla (`HistorialTest` para las cuatro primeras, `RegistroReinicioTest` para la quinta) muestra al menos un `<failure` en la prueba de esa regla. La bitácora pega el `sed` y el nombre de la prueba que cayó. Sin `git stash` ni cambios en el worktree. Además, la bitácora registra que `HistorialTest` falla (o no compila) antes de escribir `historial`, `obtener` y `RutasRegistro`.

- [ ] **CA-5**: todo el backend sigue pasando, y `registro` no depende de módulos ajenos
  ```bash
  ./gradlew :backend:test --rerun :backend:check
  for f in backend/build/test-results/test/TEST-*.xml; do grep -Ho 'tests="[0-9]*" skipped="[0-9]*" failures="[0-9]*" errors="[0-9]*"' "$f"; done
  grep -rn "io.synkra.kubernetes\|io.synkra.despliegue\|io.synkra.catalogo" backend/src/main/java/io/synkra/registro; echo "ajenos=$?"
  grep -rn "@PostMapping\|@PutMapping\|@PatchMapping\|@DeleteMapping\|RequestMethod\.\(POST\|PUT\|PATCH\|DELETE\)" backend/src/main/java/io/synkra/registro; echo "escrituras=$?"
  ```
  Esperado: `BUILD SUCCESSFUL` y ningún XML con `skipped`, `failures` o `errors` distinto de 0 (`FronterasModulosTest` pasa sin cambios, y las pruebas de U06-A siguen verdes). `ajenos=1` y `escrituras=1`.

- [ ] **CA-6**: el diff queda acotado
  ```bash
  git rev-parse --short HEAD; git diff --stat <punta VERDE de U06-A>...HEAD
  ```
  Esperado: solo estas rutas. Se anota el sha.
  - `backend/src/main/java/io/synkra/registro/**` (incluye `registro/api/`)
  - `backend/src/test/java/io/synkra/registro/**` (solo archivos nuevos: `HistorialTest.java` y `RegistroReinicioTest.java`, más algún ayudante de prueba nuevo si hace falta)
  - `bitacoras/U06-B.md` y `revisiones/U06-B/`

---

**Rojo primero:** `HistorialTest` antes de `historial`, `obtener` y `RutasRegistro`; `RegistroReinicioTest` antes de cualquier cambio que necesite (si pasa a la primera porque U06-A ya persiste todo, se anota así en la bitácora y CA-4 demuestra que muerde con la mutación de R-R8). Cada rojo queda en la bitácora, y CA-4 demuestra que cada regla muerde con el código terminado.
