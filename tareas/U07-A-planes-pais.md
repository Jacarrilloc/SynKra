# U07-A: tabla de planes y país permitido

**Unidad:** U07: `Despliegue y reversión`
**Tareas del plan que agrupa:** U07-T01 y U07-T02
**Historias que implementa:** H-11 (base), H-13
**Depende de:** U03 y U05 (terminadas). Sale de `97f9f80` (punta VERDE de U05-T10). Corre **en paralelo con U06-A**: no puede usar nada de `registro` (ni `registro.api` ni las tablas `reg_*`, que en esta rama no existen).

---

## Alcance

**Dentro** (una línea, concreta):

> `V4__despliegue.sql` con la tabla `dep_plan`; la entidad `Plan` y su `PlanRepositorio` en `io.synkra.despliegue`; y `PaisPermitido` (R-D1): acepta solo un país de dos letras minúsculas que esté en `SYNKRA_PAISES`, deriva el namespace `synkra-<país>` y rechaza todo lo demás con 422 `PAIS_NO_PERMITIDO`, sin ninguna llamada a U05. Recoge C-30.

**Fuera** (lo que el codificador debe rechazar aunque lo vea roto):

- U07-T03 en adelante: verificación en vivo, crear plan (resumen y cambios), la propiedad de cambios, el vencimiento a los 30 minutos (el paso a `vencido`), confirmar, vigilancia, reversión, carpeta equivocada, fronteras y resumen. Ningún controlador ni ruta en esta tarea, y ningún cambio de estado de un plan.
- Cualquier uso de `registro` o de `catalogo` desde `despliegue`, y cualquier clave foránea hacia `reg_despliegue`.
- Cualquier uso de `io.synkra.kubernetes` desde el código de producción de `despliegue`.
- Un bean `java.time.Clock`: U06-A corre en paralelo, y dos beans del mismo tipo chocarían al unir las ramas.
- C-35, C-76, C-82, C-83, C-87 y C-90: van con tareas posteriores de U07.
- Cambios en `verificador/`, `core/`, `plugin-api/`, `plugins/`, `init-descarga/`, y en los módulos `catalogo`, `kubernetes` y `plataforma` del backend, `build.gradle.kts` y `FronterasModulosTest.java`. Fuera de `despliegue` solo se tocan dos sitios:
  - `application.properties`: una propiedad con su comentario (decisión 6);
  - la línea 3 del plan de U07 (C-30, decisión 9).

---

## Archivos de contexto

- `aidlc-docs/construction/plans/U07-despliegue-code-generation-plan.md` (U07-T01 y T02)
- `aidlc-docs/construction/U07-despliegue/functional-design/business-rules.md` (R-D1) y `domain-entities.md` (`dep_plan`)
- `aidlc-docs/inception/requirements/requirements.md` (S4: país de dos letras en minúscula; RF-15)
- `unidades-y-tareas.md` (sección U07)
- `backend/src/main/resources/db/migration/V2__catalogo.sql` (estilo de migración)
- `backend/src/main/java/io/synkra/catalogo/` (`Plugin`, `PluginRepositorio`: repositorios sin CRUD; `RegistroPermitido`: propiedad validada al arrancar; `CodigosCatalogo`)
- `backend/src/test/java/io/synkra/catalogo/CatalogoRepositoriosTest.java` y `plataforma/RepositorioSqliteTest.java` (ids generados con `integer primary key autoincrement`)
- `backend/src/main/java/io/synkra/plataforma/` (`ExcepcionApi`, `ErrorApi`, `DetalleError`, `CodigosError`)
- `backend/src/main/java/io/synkra/kubernetes/GuardiaNamespace.java` (segunda capa: la forma; esta tarea es la primera: la lista)
- `backend/src/main/java/io/synkra/kubernetes/api/AdaptadorKubernetes.java` (lo que el doble de la prueba sustituye)
- `CANDIDATAS.md` (C-05 y C-30)

---

## Decisiones del orquestador

1. **Paquetes y nombres.**
   - Módulo `io.synkra.despliegue`, con `package-info.java`: «Módulo Despliegue: planes, verificación, confirmación y reversión (prefijo `dep_`)».
   - En esta tarea no hay paquete `api`: nadie fuera del módulo usa todavía nada de aquí.
   - Clases no públicas:
     - `Plan` (record, `@Table("dep_plan")`) y `PlanRepositorio`;
     - `PaisPermitido` (`@Component`);
     - `CodigosDespliegue` (`PAIS_NO_PERMITIDO = "PAIS_NO_PERMITIDO"`) y `MotivosDespliegue` (los textos de los motivos), como `CodigosCatalogo` y `MotivosCatalogo`.
2. **Esquema (`V4__despliegue.sql`).** Se siguen las columnas de `domain-entities.md`, con estas precisiones:
   - `id INTEGER PRIMARY KEY AUTOINCREMENT`. `AUTOINCREMENT` garantiza que un id nunca se reutiliza: U06-A guarda el despliegue con el id de su plan.
   - `pais TEXT NOT NULL` con `CHECK (length(pais) = 2 AND pais GLOB '[a-z][a-z]')`. La lista de países la aplica `PaisPermitido`, no el esquema.
   - `tipo` con `CHECK (tipo IN ('despliegue','reversion'))`.
   - `origen_id INTEGER`, con `CHECK ((tipo = 'reversion') = (origen_id IS NOT NULL))` y **sin** clave foránea: apunta a `reg_despliegue`, que es de otro módulo (D14) y no existe en esta rama. Un comentario en la migración lo dice.
   - `core` y `version_core`: `TEXT NOT NULL`.
   - `plugins`, `resumen` y `cambios`: `TEXT NOT NULL CHECK (json_valid(...))`.
   - `estado` con `CHECK (estado IN ('pendiente','confirmado','vencido'))`.
   - `creado_por TEXT NOT NULL`.
   - `creado_en INTEGER NOT NULL` y `vence_en INTEGER NOT NULL`, en ms (C-05), con `CHECK (vence_en > creado_en)`. Los 30 minutos viven en el código, no en el esquema.
   - Índice no único `dep_plan_pais_estado` sobre `(pais, estado)`.
3. **`Plan` y `PlanRepositorio`.**
   - `Plan(@Id Long id, String pais, String tipo, Long origenId, String core, String versionCore, String plugins, String resumen, String cambios, String estado, String creadoPor, long creadoEn, long venceEn)`.
   - Constantes `Plan.VIGENCIA = Duration.ofMinutes(30)` (Q6), `PENDIENTE`, `CONFIRMADO`, `VENCIDO`, `DESPLIEGUE` y `REVERSION`.
   - Fábrica `Plan.nuevo(pais, tipo, origenId, core, versionCore, plugins, resumen, cambios, creadoPor, Instant creadoEn)`: da `id` nulo, `estado = pendiente` y `venceEn = creadoEn + VIGENCIA`.
   - `PlanRepositorio extends Repository<Plan, Long>` (no `CrudRepository`), solo con `Optional<Plan> findById(Long id)` y `List<Plan> findByPaisAndEstado(String pais, String estado)`.
   - Las altas, con `JdbcAggregateTemplate.insert`, que devuelve el plan con su id. Sin `save` ni `delete`.
4. **R-D1 (`PaisPermitido`).**
   - `String exigir(String pais)` devuelve el país si está permitido.
   - `String namespace(String pais)` devuelve `"synkra-" + exigir(pais)`. El namespace nunca se arma con otra cosa.
   - Rechazo: `ExcepcionApi(HttpStatus.UNPROCESSABLE_ENTITY, new ErrorApi(CodigosDespliegue.PAIS_NO_PERMITIDO, "El país no está permitido", List.of(new DetalleError("pais", motivo))))`. Hay dos motivos:
     - forma inválida (nulo, vacío, mayúsculas, más o menos de dos letras, no ASCII): «El país debe ser un código de dos letras en minúscula (S4)»;
     - forma válida pero fuera de la lista: «El país no está entre los configurados (SYNKRA_PAISES)».
   - El motivo **no** repite lo recibido (lección de C-34).
   - No hay normalización: `PE` y `" co"` se rechazan, no se pasan a minúsculas ni se recortan.
5. **Sin llamadas a U05: con un doble del `AdaptadorKubernetes`, no con el servidor simulado.**
   - `PaisPermitidoTest` es un `@SpringBootTest` (con `@TempDir` como `synkra.data`) que reemplaza el bean con `@MockitoBean AdaptadorKubernetes adaptador`. Tras todos los casos, aceptados y rechazados, comprueba `verifyNoInteractions(adaptador)`.
   - Por qué un doble: `PaisPermitido` nunca arma un cliente, y el doble ve cualquier llamada al adaptador (`existeNamespace` incluido), no solo las que llegarían por HTTP. Además, `despliegue` no necesita Fabric8 ni en las pruebas.
   - Lo completa un `grep` de CA-2: el código de producción de `despliegue` no importa `io.synkra.kubernetes`.
6. **Propiedad `synkra.paises`.**
   - En `application.properties`: `synkra.paises=${SYNKRA_PAISES:co,pe}`, con un comentario de una línea (R-D1 y R-R7; U06 la leerá también).
   - `PaisPermitido` la recibe con `@Value("${synkra.paises}")` y la valida en el constructor. Separa por `,` y recorta los espacios de cada entrada. Cada entrada debe ser `[a-z]{2}`, sin vacías ni repetidas, y con al menos una.
   - Si no cumple, `IllegalStateException` al arrancar, con un mensaje que nombra `synkra.paises (SYNKRA_PAISES)`, como `RegistroPermitido`.
   - Ningún otro módulo lee la propiedad en esta tarea.
7. **SQLite de la prueba, para leerlo de vuelta.**
   - `PlanRepositorioTest` usa como `synkra.data` la carpeta fija `build/sqlite-pruebas/PlanRepositorioTest` (relativa a `backend/`). El `@DynamicPropertySource` la borra y la crea de nuevo antes de abrir la base.
   - No se borran filas entre pruebas, y cada prueba usa su propio país:
     - `aa`: alta y lectura con `Plan.nuevo`;
     - `ab`: `reversion` con `origenId = 41`, un id que no existe en ninguna tabla, aceptado porque no hay clave foránea;
     - `ac` y `ad`: los ids no se reutilizan (se inserta `ac`, se borra con SQL directo, se inserta `ad` y su id es mayor);
     - `ae` y `af`: SQL directo con estado `confirmado` y `vencido`;
     - `ba` a `be`: cinco rechazos (estado `otro`, `reversion` sin origen, `despliegue` con origen, JSON inválido, `vence_en <= creado_en`); el sexto es el país `PE`. Ninguno deja fila.
8. **Flyway V4 sin V3 en esta rama.** Las bases de las pruebas son nuevas en cada ejecución y no hay ninguna base persistente desplegada, así que el hueco no rompe nada. Al unir con U06-A, una base nueva aplica de V1 a V4 en orden. No se activa `outOfOrder`.
9. **C-30.** En la línea 3 de `aidlc-docs/construction/plans/U07-despliegue-code-generation-plan.md`, «aviso previo a Julian» pasa a «aviso previo al responsable del proyecto». Ningún otro cambio en ese archivo.
10. **Bitácora en `bitacoras/U07-A.md`**, y el revisor en `revisiones/U07-A/`.

---

## Criterios de aceptación

- [ ] **CA-1**: U07-T01. Repositorio de planes, lectura de vuelta del archivo de la prueba y tabla en la aplicación arrancada
  ```bash
  ./gradlew :backend:test --rerun --tests '*PlanRepositorioTest'
  grep -ho 'tests="[0-9]*" skipped="[0-9]*" failures="[0-9]*" errors="[0-9]*"' backend/build/test-results/test/TEST-*PlanRepositorioTest.xml
  python3 -I -c "import sqlite3,sys; c=sqlite3.connect(sys.argv[1]); print(c.execute('select pais, tipo, origen_id, estado, typeof(creado_en), vence_en - creado_en, json_valid(plugins) + json_valid(resumen) + json_valid(cambios) from dep_plan order by pais').fetchall()); print(c.execute(\"select count(*) from dep_plan where pais like 'b%' or pais = 'PE'\").fetchone()); print(c.execute(\"select name from sqlite_sequence where name='dep_plan'\").fetchall())" backend/build/sqlite-pruebas/PlanRepositorioTest/synkra.db
  ./gradlew :backend:bootJar
  export SYNKRA_DATA=$(mktemp -d)
  java -jar backend/build/libs/backend-*.jar --server.port=18087 & PID=$!
  timeout 90 bash -c 'until curl -sf localhost:18087/actuator/health >/dev/null; do sleep 1; done'
  python3 -I -c "import sqlite3,sys; c=sqlite3.connect(sys.argv[1]); print(sorted(r[0] for r in c.execute(\"select name from sqlite_master where type='table' and name like 'dep_%'\"))); print(c.execute('select version from flyway_schema_history order by installed_rank').fetchall()); print(c.execute(\"select count(*) from pragma_foreign_key_list('dep_plan')\").fetchone()); print(c.execute(\"select name from sqlite_master where type='index' and name='dep_plan_pais_estado'\").fetchall())" "$SYNKRA_DATA/synkra.db"
  kill $PID; wait $PID 2>/dev/null
  ```
  Esperado:
  - `BUILD SUCCESSFUL`, `tests` ≥ 8 y `skipped="0" failures="0" errors="0"`. Las pruebas afirman:
    - alta y lectura por id y por `(pais, estado)`;
    - `vence_en - creado_en = 1 800 000` con `Plan.nuevo`;
    - `typeof(creado_en) = 'integer'` y el instante en ms de ida y vuelta;
    - que los ids no se reutilizan;
    - los seis rechazos de la decisión 7, con `SQLITE_CONSTRAINT_CHECK` y sin fila;
    - que el repositorio no ofrece `save`, `delete` ni `existsById`.
  - Python, primera parte:
    - `[('aa', 'despliegue', None, 'pendiente', 'integer', 1800000, 3), ('ab', 'reversion', 41, 'pendiente', 'integer', 1800000, 3), ('ad', 'despliegue', None, 'pendiente', 'integer', 1800000, 3), ('ae', 'despliegue', None, 'confirmado', 'integer', 1800000, 3), ('af', 'despliegue', None, 'vencido', 'integer', 1800000, 3)]`;
    - `(0,)`;
    - `[('dep_plan',)]`.
  - Python, segunda parte:
    - `['dep_plan']`;
    - `[('1',), ('2',), ('4',)]` (V3 llega con U06-A; tras la unión serán `1` a `4`);
    - `(0,)`;
    - `[('dep_plan_pais_estado',)]`.

  Si el puerto 18087 está ocupado, se anota en la bitácora y se usa otro en todo el criterio.

- [ ] **CA-2**: U07-T02. País permitido R-D1, sin ninguna llamada a U05
  ```bash
  ./gradlew :backend:test --rerun --tests '*PaisPermitidoTest'
  grep -ho 'tests="[0-9]*" skipped="[0-9]*" failures="[0-9]*" errors="[0-9]*"' backend/build/test-results/test/TEST-*PaisPermitidoTest.xml
  grep -rn "io.synkra.kubernetes\|io.synkra.registro\|io.synkra.catalogo" backend/src/main/java/io/synkra/despliegue; echo "ajenos=$?"
  ```
  Esperado:
  - `BUILD SUCCESSFUL`, sin saltos ni fallos, con `synkra.paises=co,pe` fijada en la prueba.
  - Aceptados: `co` y `pe`, con `namespace` `synkra-co` y `synkra-pe`.
  - Rechazados con `ExcepcionApi` de estado 422, `codigo` `PAIS_NO_PERMITIDO` y un detalle con campo `pais`:
    - `mx` (forma válida, fuera de la lista);
    - `default`, `kube-system`, `PE`, `synkra-co`, `c`, `col`, `" co"`, `""`, `null`;
    - un país con una letra no ASCII, por ejemplo la `с` cirílica.
  - Cada rechazo afirma su motivo, y que el motivo no contiene lo recibido.
  - `verifyNoInteractions(adaptador)` sobre el `@MockitoBean` pasa.
  - Pruebas del constructor: `"co, pe"` es válido; `""`, `"co,,pe"`, `"co,co"`, `"CO"` y `"col"` lanzan `IllegalStateException`, con `SYNKRA_PAISES` en el mensaje.
  - `ajenos=1`.

- [ ] **CA-3**: un `SYNKRA_PAISES` inválido impide arrancar (lectura de vuelta del proceso real)
  ```bash
  ./gradlew :backend:bootJar
  L=$(mktemp)
  SYNKRA_DATA=$(mktemp -d) SYNKRA_PAISES='co,PE' timeout 60 java -jar backend/build/libs/backend-*.jar --server.port=18088 > "$L" 2>&1; echo "salida=$?"
  grep -c "SYNKRA_PAISES" "$L"
  ```
  Esperado: `salida=` distinto de 0 y de 124 (falla al arrancar, no por el `timeout`), y un conteo ≥ 1.

- [ ] **CA-4**: rojo primero. Cada regla nueva tiene una prueba que muerde, demostrado en una copia
  ```bash
  D=$(mktemp -d); git archive HEAD | tar -x -C "$D"
  # Mutación anotada literalmente en la bitácora; una por regla, por ejemplo:
  #   lista:    exigir solo comprueba la forma (mx pasa)
  #   forma:    comparar sin distinguir mayúsculas (PE pasa)
  #   sin U05:  inyectar AdaptadorKubernetes en PaisPermitido y llamar a existeNamespace(namespace) antes de responder
  #   esquema:  quitar el CHECK de estado de V4__despliegue.sql
  sed -i '<mutación>' "$D/backend/src/main/<ruta>"
  (cd "$D" && ./gradlew :backend:test --tests '*PaisPermitidoTest' --tests '*PlanRepositorioTest')
  grep -l '<failure' "$D"/backend/build/test-results/test/TEST-*.xml
  rm -rf "$D"
  ```
  Esperado: con cada una de las cuatro mutaciones (una copia por mutación), `BUILD FAILED`, y el XML de la clase dueña de la regla muestra al menos un `<failure` en la prueba de esa regla. La mutación «sin U05» cae en la prueba de `verifyNoInteractions`. La bitácora pega el `sed` (o el parche) y el nombre de la prueba que cayó. Sin `git stash` ni cambios en el worktree. Además, la bitácora registra que CA-1 falla antes de escribir `V4__despliegue.sql`.

- [ ] **CA-5**: todo el backend sigue pasando, incluidas las fronteras con el módulo nuevo
  ```bash
  ./gradlew :backend:test --rerun :backend:check
  for f in backend/build/test-results/test/TEST-*.xml; do grep -Ho 'tests="[0-9]*" skipped="[0-9]*" failures="[0-9]*" errors="[0-9]*"' "$f"; done
  ```
  Esperado: `BUILD SUCCESSFUL` y ningún XML con `skipped`, `failures` o `errors` distinto de 0 (`FronterasModulosTest` pasa sin cambios con `despliegue`).

- [ ] **CA-6**: el diff queda acotado
  ```bash
  git rev-parse --short HEAD; git diff --stat 97f9f80...HEAD
  git diff 97f9f80...HEAD -- backend/src/main/resources/application.properties aidlc-docs/construction/plans/U07-despliegue-code-generation-plan.md
  ```
  Esperado: solo estas rutas. Se anota el sha.
  - `backend/src/main/resources/db/migration/V4__despliegue.sql`
  - `backend/src/main/java/io/synkra/despliegue/**`
  - `backend/src/test/java/io/synkra/despliegue/**`
  - `backend/src/main/resources/application.properties`: solo `synkra.paises` y su comentario.
  - `aidlc-docs/construction/plans/U07-despliegue-code-generation-plan.md`: solo la línea 3 (C-30).
  - `bitacoras/U07-A.md` y `revisiones/U07-A/`

---

**Rojo primero:** `PlanRepositorioTest` antes de la migración; `PaisPermitidoTest` antes de `PaisPermitido`. Cada rojo queda en la bitácora, y CA-4 demuestra que cada regla muerde con el código terminado.
