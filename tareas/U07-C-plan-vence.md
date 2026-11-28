# U07-C: un plan sin confirmar nunca aplica y vence

**Unidad:** U07: `Despliegue y reversión`
**Tareas del plan que agrupa:** U07-T06
**Historias que implementa:** H-17, H-12 (contribuye: la guarda de R-D5 que usará la confirmación)
**Depende de:** U07-B (terminada), U07-A, U04 y U05 (terminadas). Sale de `<punta VERDE de U07-B>`. Asume como dado todo lo que fijaron U07-A y U07-B: `V4__despliegue.sql`, `Plan` (con `VIGENCIA`, `PENDIENTE`, `CONFIRMADO` y `VENCIDO`), `Plan.nuevo`, `PlanRepositorio`, `PaisPermitido`, `PlanServicio.prepararPlan`, `RutasPlanes` (`POST /api/despliegues`), `CodigosDespliegue`, `MotivosDespliegue`, `synkra.paises` en `application.properties`, el ayudante de prueba `SimuladorKubernetes` y las carpetas `build/sqlite-pruebas/<Clase>`. Corre **en paralelo con U06-C**: no puede usar nada de `registro` (ni `registro.api` ni las tablas `reg_*`, que en esta rama no existen).

---

## Alcance

**Dentro** (una línea, concreta):

> El bean `java.time.Clock` de la aplicación (decisión 2), que `PlanServicio` pasa a usar; el paso de `pendiente` a `vencido` cuando llega `vence_en` (Q6), con `VencimientoPlanes`; la guarda de R-D5 (`GuardaConfirmacion`) y la ruta `POST /api/despliegues/{id}/confirmacion`, que en esta tarea solo responde 404, 409 o 410 `PLAN_VENCIDO`, y 501 para un plan vigente; y la prueba de que crear planes, dejarlos vencer o intentar confirmarlos no escribe **nada** en el clúster (R-D13), contra el servidor simulado.

**Fuera** (lo que el codificador debe rechazar aunque lo vea roto):

- U07-T07 en adelante: la confirmación de verdad (volver a verificar R-D6, un solo `aplicando` por país R-D7, los dos usuarios R-D8, `construir`, `aplicar`, `registrar` y el 202 de R-D9), la vigilancia, retomar tras reinicio, la reversión, P4, las fronteras de `despliegue` y el resumen. El 501 de la decisión 5 lo reemplaza U07-D.
- La segunda mitad de H-17 («el plan no aparece en el registro»): en esta rama no hay registro. Lo comprueba U07-D, la primera tarea que une las dos ramas.
- Un barrido periódico de planes vencidos (`@Scheduled`, `@EnableScheduling` o una propiedad nueva): esta tarea vence al intentar confirmar y deja `vencerPendientes()` lista para quien la programe (decisión 6). Se propone como candidata.
- `GET /api/despliegues/{id}` y cualquier `GET` sobre `/api/despliegues/**` (son de `registro`, decisión 6 de U06-B), y una cabecera `Location`.
- Cualquier uso de `registro` desde `despliegue`, y cualquier llamada a `aplicar`, `construir`, `estado`, `contextoFallo` o `leerPais` desde `VencimientoPlanes`, `GuardaConfirmacion` o `RutasConfirmacion`.
- `REQUIRES_NEW` en cualquier sitio (C-107: con el pool de una conexión, espera hasta agotar el plazo).
- C-35, C-76, C-82, C-83 y C-87: van con la confirmación y la vigilancia (U07-D y U07-E).
- Cambios en `verificador/`, `core/`, `plugin-api/`, `plugins/`, `init-descarga/`, en el código de producción de los módulos `catalogo`, `kubernetes` y `registro`, en `V4__despliegue.sql`, `application.properties`, `build.gradle.kts` y `FronterasModulosTest.java`. En `plataforma` solo se **añade** `Reloj.java` (decisión 2). No se cambia ninguna prueba existente, `SimuladorKubernetes.java` incluido.
- Nada de `kubectl` ni de clúster real: el único Kubernetes es el servidor simulado de Fabric8 (AUTONOMIA-01).

---

## Archivos de contexto

- `tareas/U07-A-planes-pais.md` y `tareas/U07-B-crear-plan.md` (contratos previos)
- `aidlc-docs/construction/plans/U07-despliegue-code-generation-plan.md` (U07-T06)
- `aidlc-docs/construction/U07-despliegue/functional-design/business-rules.md` (R-D5 y R-D13), `domain-entities.md` (`vence_en = creado_en + 30 min`) y `business-logic-model.md` (diagrama de estados del plan)
- `aidlc-docs/inception/user-stories/stories.md` (H-17)
- `unidades-y-tareas.md` (sección U07)
- En la punta de U07-B: `backend/src/main/java/io/synkra/despliegue/` (`Plan`, `PlanRepositorio`, `PlanServicio`, `RutasPlanes`, `CodigosDespliegue`, `MotivosDespliegue`) y `backend/src/test/java/io/synkra/despliegue/CrearPlanTest.java` (carga del catálogo con `SuiteCombinaciones`, `@TestBean AdaptadorKubernetes` con fábrica estática, `nuevasPeticiones()`)
- `backend/src/test/java/io/synkra/kubernetes/SimuladorKubernetes.java` (servidor simulado en modo CRUD, casa y kubeconfig falsos)
- `backend/src/main/java/io/synkra/plataforma/` (`BaseDeDatos`: pool de **una** conexión; `ExcepcionApi`, `ErrorApi`, `CodigosError`, `ManejadorErrores`)
- `CANDIDATAS.md` (C-05, C-34 y C-107)

---

## Decisiones del orquestador

1. **Nombres nuevos.** Todo en `io.synkra.despliegue`, no público, salvo el bean de la decisión 2.
   - `VencimientoPlanes` (`@Component`): `int vencerPendientes()` y `boolean vencerSiToca(long id)` (decisión 3).
   - `GuardaConfirmacion` (`@Component`): `Plan exigirConfirmable(long id)` (decisión 4). U07-D la llamará como primer paso de la confirmación.
   - `RutasConfirmacion` (`@RestController`, `POST /api/despliegues/{id}/confirmacion`), decisión 5.
   - En `CodigosDespliegue` se añaden `PLAN_VENCIDO`, `PLAN_NO_PENDIENTE` y `CONFIRMACION_NO_DISPONIBLE`; sus textos van en `MotivosDespliegue`:
     - `PLAN_VENCIDO_MENSAJE = "El plan venció sin confirmarse"`;
     - `PLAN_NO_PENDIENTE_MENSAJE = "El plan ya no está pendiente"`;
     - `CONFIRMACION_NO_DISPONIBLE_MENSAJE = "La confirmación todavía no está disponible"`.
2. **El bean `Clock` es uno solo, de la aplicación, y vive en `plataforma`.**
   - Archivo nuevo `backend/src/main/java/io/synkra/plataforma/Reloj.java`: `@Configuration` no pública con `@Bean Clock reloj()` que devuelve `Clock.systemUTC()`. Es el único bean `Clock` del backend.
   - Por qué aquí y ahora: ni U06-A ni U07-A lo crearon para no chocar mientras corrían en paralelo, y U06-C (que corre en paralelo con esta) lo tiene prohibido. Así, al unir U07-C con U06-C en U07-D hay exactamente un bean. Va en `plataforma` y no en `despliegue` porque es de toda la aplicación: cualquier módulo lo inyecta como `java.time.Clock` sin depender de `plataforma` (ni Modulith ni `FronterasModulosTest` lo ven como dependencia).
   - `PlanServicio` recibe el `Clock` por su constructor y deja su `Clock.systemUTC()` privado. `creadoEn` sale de `reloj.instant()`, como hoy. `VencimientoPlanes` usa el mismo bean. En `despliegue` no queda ningún `Clock.system…`.
   - `RegistroServicio` (otra rama) no se toca: sigue con su reloj privado. Si debe pasar al bean, lo decide U07-D (es cambiar el constructor `@Autowired`).
   - En las pruebas, el reloj se reemplaza con `@TestBean Clock reloj` y una fábrica estática que devuelve un `RelojMovible`: clase de prueba nueva en `backend/src/test/java/io/synkra/despliegue/`, que extiende `Clock`, con zona UTC y `void fija(Instant)`. No hay `Thread.sleep` en ninguna prueba.
3. **Q6: el paso a `vencido` (`VencimientoPlanes`).**
   - Un plan `pendiente` está vencido cuando `reloj.instant().toEpochMilli() >= vence_en`: a `vence_en − 1 ms` sigue vigente, a `vence_en` ya no.
   - `vencerSiToca(id)` es un solo `UPDATE dep_plan SET estado = 'vencido' WHERE id = ? AND estado = 'pendiente' AND vence_en <= ?` (con `JdbcTemplate`); devuelve si cambió una fila. `vencerPendientes()` es el mismo `UPDATE` sin el `id` y devuelve cuántas cambió. La guarda vive en el `WHERE`, como en U06-A: nunca se lee y luego se escribe.
   - Los dos métodos son `@Transactional` (propagación por defecto): cada `UPDATE` se confirma al volver del método. Nunca `REQUIRES_NEW`.
   - Ningún otro estado se toca: un plan `confirmado` o `vencido` no cambia, por viejo que sea.
4. **R-D5 (`GuardaConfirmacion.exigirConfirmable(id)`), el orden es contrato.**
   1. Id ≤ 0: 404. Si no, `vencimiento.vencerSiToca(id)`.
   2. `planes.findById(id)`. No existe: 404 `ErrorApi(CodigosError.NO_ENCONTRADO, CodigosError.MSG_NO_ENCONTRADO, [])`.
   3. `vencido`: 410 `ErrorApi(PLAN_VENCIDO, PLAN_VENCIDO_MENSAJE, [])`.
   4. `confirmado`: 409 `ErrorApi(PLAN_NO_PENDIENTE, PLAN_NO_PENDIENTE_MENSAJE, [])`.
   5. `pendiente` (y vigente, por el paso 1): devuelve el plan.
   - `exigirConfirmable` **no** es `@Transactional`: el `UPDATE` del paso 1 queda confirmado aunque después se lance el 410. Si la guarda abriera una transacción, el 410 la revertiría y el plan seguiría `pendiente` (lo ata la mutación «persistir» de CA-4).
   - No inyecta ni llama al `AdaptadorKubernetes`: decidir si un plan se puede confirmar no habla con el clúster.
5. **La ruta (`RutasConfirmacion`), a medias a propósito.**
   - `POST /api/despliegues/{id}/confirmacion`, sin cuerpo. Llama a `guarda.exigirConfirmable(id)`, que da los 404, 409 y 410 de la decisión 4.
   - Si la guarda deja pasar el plan: 501 `ErrorApi(CONFIRMACION_NO_DISPONIBLE, CONFIRMACION_NO_DISPONIBLE_MENSAJE, [])`. El plan sigue `pendiente` y no se habla con el clúster. U07-D reemplaza este 501 por la confirmación de verdad (202). Va así para que el criterio del plan, «confirmar da 410», se pruebe por HTTP sobre la ruta real, y para que la ruta nunca aplique nada mientras no exista la confirmación (R-D13).
   - Un id que no es número lo resuelve la plataforma tal cual (se espera 400 `SOLICITUD_INVALIDA`; si no es así, se anota en la bitácora y **no** se toca `plataforma`).
6. **Sin barrido programado.** Un plan pendiente que nadie toca sigue `pendiente` en la base aunque haya pasado `vence_en`, y nunca se puede confirmar (paso 1 de la guarda). `vencerPendientes()` queda probada y sin llamar desde producción; programarla (y su intervalo) se propone como candidata para U07-E, que trae la primera tarea programada (la vigilancia). CA-3 lo deja a la vista con el plan 74.
7. **R-D13 contra el servidor simulado (`PlanPendienteTest`).**
   - `@SpringBootTest(webEnvironment = RANDOM_PORT)`, con `@TestBean AdaptadorKubernetes adaptador` (fábrica que devuelve `SIM.adaptador()`, como `CrearPlanTest`) y `@TestBean Clock reloj` (decisión 2). El catálogo se carga como en `CrearPlanTest` (`SuiteCombinaciones.contenido()` por `CatalogoApi`, imágenes `a`, `b` y `c`); si hace falta compartir código con `CrearPlanTest`, va en un ayudante de prueba **nuevo**, sin cambiar `CrearPlanTest`.
   - Se drena `SIM.nuevasPeticiones()` justo antes de cada acción que se mide.
   - «Escritura» es toda petición recibida por el simulador cuyo método no sea `GET`. En ninguna prueba hay una.
   - Cada intento de confirmación (404, 409, 410 o 501) deja `nuevasPeticiones()` **vacía**.
8. **SQLite y datos de `PlanPendienteTest`.**
   - Carpeta fija `build/sqlite-pruebas/PlanPendienteTest`, que el `@DynamicPropertySource` borra y crea de nuevo; no se borran filas entre pruebas. Fija `synkra.paises=pe,co,cl,py,ar,uy,bo,ec`.
   - `T0 = 2026-11-18T11:00:00.123Z` (`1794999600123` ms). Cada prueba usa su propio país y su propia hora base, fijada con `reloj.fija(...)` antes de crear sus planes. Todos los planes son S2 (`synkra-core` 2.1.0 con `kyc-pe` 1.3.0), creados por `POST /api/despliegues`:
     - `ec`, base `T0`: el barrido. Un plan a `T0` y otro a `T0 + 10 min`; con `fija(T0 + 35 min)`, `vencerPendientes()` devuelve **1** y solo vence el primero. Es la hora base más baja, así que no toca planes de otras pruebas, sea cual sea el orden en que corran.
     - `pe`, `co` y `cl`, base `T0 + 1 h`: crear no escribe en el clúster. `pe` vacío; `co` sembrado con 2.1.0 (imagen `a`) y `kyc-co` 1.0.0; `cl` sembrado con S2 (sale `sinCambios`). Los tres dan 201, `creadoEn` es exactamente `T0 + 1 h` y las peticiones son todas `GET`.
     - `py`, base `T0 + 2 h`: un plan vigente. Confirmar da 501 con el mensaje exacto, sin peticiones, y la fila sigue `pendiente`.
     - `ar`, base `T0 + 3 h`: el criterio del plan. Dos planes: el primero a `T0 + 3 h` y el segundo a `T0 + 3 h + 30 s`. Con `fija(T0 + 3 h + 31 min)` (los dos ya pasaron su `vence_en`), confirmar el primero da 410 `PLAN_VENCIDO` con el mensaje exacto y su fila pasa a `vencido`; el segundo, que nadie tocó, sigue `pendiente` (decisión 6), y el número de filas `vencido` de la tabla sube exactamente en 1; un segundo intento sobre el primero vuelve a dar 410.
     - `uy`, base `T0 + 4 h`: la frontera. Con `fija(venceEn − 1 ms)`, 501 y `pendiente`; con `fija(venceEn)`, 410 y `vencido`.
     - `bo`, base `T0 + 5 h`: el plan se pasa a `confirmado` con SQL directo; con `fija(T0 + 5 h + 31 min)`, confirmar da 409 `PLAN_NO_PENDIENTE` y la fila sigue `confirmado`.
     - Sin país: `POST /api/despliegues/999/confirmacion`, `/0/` y `/-1/` dan 404 `NO_ENCONTRADO`; `/abc/` da lo que fije la decisión 5.
9. **`RelojTest`** (archivo nuevo en `backend/src/test/java/io/synkra/plataforma/`): un `@SpringBootTest` sin reemplazos (con `synkra.data` en una carpeta temporal) afirma que el contexto tiene exactamente un bean de tipo `Clock`, con zona `ZoneOffset.UTC`, y que su `instant()` está a menos de 5 s de `Instant.now()`.
10. **Bitácora en `bitacoras/U07-C.md`**, y el revisor en `revisiones/U07-C/`.

---

## Criterios de aceptación

- [ ] **CA-1**: U07-T06. Un plan sin confirmar nunca aplica (R-D13) y vence (Q6), contra el servidor simulado, con lectura de vuelta
  ```bash
  ./gradlew :backend:test --rerun --tests '*PlanPendienteTest'
  grep -ho 'tests="[0-9]*" skipped="[0-9]*" failures="[0-9]*" errors="[0-9]*"' backend/build/test-results/test/TEST-*PlanPendienteTest.xml
  python3 -I -c "import sqlite3,sys; c=sqlite3.connect(sys.argv[1]); print(c.execute('select pais, estado, creado_en from dep_plan order by pais, creado_en').fetchall()); print(c.execute('select count(*) from dep_plan where vence_en - creado_en != 1800000').fetchone())" backend/build/sqlite-pruebas/PlanPendienteTest/synkra.db
  ```
  Esperado:
  - `BUILD SUCCESSFUL`, `tests` ≥ 8 y `skipped="0" failures="0" errors="0"`, con los casos de la decisión 8. Las pruebas afirman además:
    - en cada creación, `nuevasPeticiones()` no está vacía y solo trae `GET`;
    - en cada intento de confirmación, `nuevasPeticiones()` está vacía;
    - los 404, 409, 410 y 501 comparan `codigo`, `mensaje` exacto y `detalles` vacío;
    - `vencerPendientes()` devuelve 1 en el caso `ec`, y el segundo plan de `ec` sigue `pendiente`.
  - Python:
    - `[('ar', 'vencido', 1795010400123), ('ar', 'pendiente', 1795010430123), ('bo', 'confirmado', 1795017600123), ('cl', 'pendiente', 1795003200123), ('co', 'pendiente', 1795003200123), ('ec', 'vencido', 1794999600123), ('ec', 'pendiente', 1795000200123), ('pe', 'pendiente', 1795003200123), ('py', 'pendiente', 1795006800123), ('uy', 'vencido', 1795014000123)]`;
    - `(0,)`.

- [ ] **CA-2**: un solo bean `Clock`, y `despliegue` no tiene relojes propios
  ```bash
  ./gradlew :backend:test --rerun --tests '*RelojTest'
  grep -ho 'tests="[0-9]*" skipped="[0-9]*" failures="[0-9]*" errors="[0-9]*"' backend/build/test-results/test/TEST-*RelojTest.xml
  grep -rln "Clock\.system" backend/src/main/java
  grep -rn "Clock\.\(fixed\|offset\|tick\)" backend/src/main/java; echo "otros=$?"
  ```
  Esperado: `BUILD SUCCESSFUL` sin saltos ni fallos, con lo que pide la decisión 9; la segunda orden da solo `backend/src/main/java/io/synkra/plataforma/Reloj.java`; `otros=1`.

- [ ] **CA-3**: el JAR real, sin clúster: un plan vencido da 410 y queda `vencido`, y uno vigente no se confirma
  ```bash
  ./gradlew :backend:bootJar
  export SYNKRA_DATA=$(mktemp -d); J=$(mktemp -d); H=$(mktemp -d)
  arranca() { HOME="$H" KUBECONFIG="$H/kubeconfig-falso" SYNKRA_PAISES=pe java -Duser.home="$H" -jar backend/build/libs/backend-*.jar --server.port=18092 > "$J/log-$1.txt" 2>&1 & PID=$!; timeout 90 bash -c 'until curl -sf localhost:18092/actuator/health >/dev/null; do sleep 1; done'; }
  # 1. Primer arranque: solo migra. Se para y se siembran planes con fechas reales (el JAR no puede crear planes sin clúster).
  arranca 0; kill $PID; wait $PID 2>/dev/null
  python3 -I - "$SYNKRA_DATA/synkra.db" <<'EOF'
  import sqlite3, sys, time
  c = sqlite3.connect(sys.argv[1]); ahora = int(time.time() * 1000); m = 60000
  for i, estado, en in ((71, "pendiente", ahora - 31 * m), (72, "pendiente", ahora), (73, "confirmado", ahora - 31 * m), (74, "pendiente", ahora - 31 * m)):
      c.execute("insert into dep_plan (id, pais, tipo, origen_id, core, version_core, plugins, resumen, cambios, estado, creado_por,"
                " creado_en, vence_en) values (?, 'pe', 'despliegue', null, 'synkra-core', '2.1.0', '[]', '{}', '{}', ?, 'anonimo', ?, ?)",
                (i, estado, en, en + 30 * m))
  c.commit()
  EOF
  # 2. Segundo arranque: intentos de confirmación.
  arranca 1
  for id in 71 71 72 73 999; do
    curl -s -o "$J/r.json" -w '%{http_code} ' -X POST localhost:18092/api/despliegues/$id/confirmacion
    python3 -I -c "import json,sys; r=json.load(open(sys.argv[1])); print(r['codigo'], r['mensaje'])" "$J/r.json"; done
  kill $PID; wait $PID 2>/dev/null
  python3 -I -c "import sqlite3,sys; c=sqlite3.connect(sys.argv[1]); print(c.execute('select id, estado from dep_plan order by id').fetchall())" "$SYNKRA_DATA/synkra.db"
  ```
  Esperado, en orden:
  - `410 PLAN_VENCIDO El plan venció sin confirmarse` (dos veces);
  - `501 CONFIRMACION_NO_DISPONIBLE La confirmación todavía no está disponible`;
  - `409 PLAN_NO_PENDIENTE El plan ya no está pendiente`;
  - `404 NO_ENCONTRADO Recurso no encontrado`;
  - `[(71, 'vencido'), (72, 'pendiente'), (73, 'confirmado'), (74, 'pendiente')]` (el 74 ya pasó su `vence_en` pero nadie lo tocó: sin barrido, decisión 6).

  Si el puerto 18092 está ocupado, se anota en la bitácora y se usa otro en todo el criterio. La bitácora pega la salida tal cual.

- [ ] **CA-4**: rojo primero. Cada regla nueva tiene una prueba que muerde, demostrado en una copia
  ```bash
  D=$(mktemp -d); git archive HEAD | tar -x -C "$D"
  # Mutación anotada literalmente en la bitácora; una por regla, por ejemplo:
  #   frontera Q6:     «vence_en <= ?» pasa a «vence_en < ?» (uy a venceEn da 501)
  #   persistir:       @Transactional en GuardaConfirmacion.exigirConfirmable (el 410 revierte el UPDATE: ar sigue pendiente)
  #   solo pendiente:  quitar «estado = 'pendiente' AND» del UPDATE (bo da 410)
  #   por id:          quitar «id = ? AND» de vencerSiToca (el 410 de ar vence también el segundo plan de ar)
  #   409:             la guarda deja pasar un plan confirmado (bo da 501)
  #   reloj:           PlanServicio vuelve a Clock.systemUTC() (creadoEn no es el del reloj fijo)
  #   R-D13 crear:     tras el alta, PlanServicio llama a kubernetes.aplicar(kubernetes.construir(new PlanPais(pais, imagen, List.of())))
  #   R-D13 confirmar: RutasConfirmacion inyecta AdaptadorKubernetes y llama a leerPais(plan.pais()) antes del 501
  sed -i '<mutación>' "$D/backend/src/main/java/io/synkra/despliegue/<Clase>.java"
  (cd "$D" && ./gradlew :backend:test --tests '*PlanPendienteTest')
  grep -l '<failure' "$D"/backend/build/test-results/test/TEST-*.xml
  rm -rf "$D"
  ```
  Esperado: con cada una de las ocho mutaciones (una copia por mutación), `BUILD FAILED`, y el XML de `PlanPendienteTest` muestra al menos un `<failure` en la prueba de esa regla. La bitácora pega el `sed` (o el parche) y el nombre de la prueba que cayó. Sin `git stash` ni cambios en el worktree. Además, la bitácora registra el rojo de `PlanPendienteTest` antes de `VencimientoPlanes`, `GuardaConfirmacion` y `RutasConfirmacion`, y el de `RelojTest` antes de `Reloj`.

- [ ] **CA-5**: todo el backend sigue pasando, y nada de lo nuevo habla con el clúster
  ```bash
  ./gradlew :backend:test --rerun :backend:check
  for f in backend/build/test-results/test/TEST-*.xml; do grep -Ho 'tests="[0-9]*" skipped="[0-9]*" failures="[0-9]*" errors="[0-9]*"' "$f"; done
  grep -rn "io.synkra.registro" backend/src/main/java/io/synkra/despliegue; echo "registro=$?"
  grep -rnE "\.(aplicar|construir)\(" backend/src/main/java/io/synkra/despliegue; echo "aplica=$?"
  grep -n "AdaptadorKubernetes\|kubernetes" backend/src/main/java/io/synkra/despliegue/VencimientoPlanes.java backend/src/main/java/io/synkra/despliegue/GuardaConfirmacion.java backend/src/main/java/io/synkra/despliegue/RutasConfirmacion.java; echo "k8s=$?"
  grep -rn "REQUIRES_NEW" backend/src/main/java; echo "requiresNew=$?"
  grep -rn "@Scheduled\|@EnableScheduling" backend/src/main/java; echo "programado=$?"
  ```
  Esperado: `BUILD SUCCESSFUL` y ningún XML con `skipped`, `failures` o `errors` distinto de 0 (`CrearPlanTest`, `VerificacionVivoTest`, `CambiosPropiedades`, `PaisPermitidoTest` y `PlanRepositorioTest` pasan sin cambios, ahora con el bean `Clock`). `registro=1`, `aplica=1`, `k8s=1`, `requiresNew=1` y `programado=1`.

- [ ] **CA-6**: el diff queda acotado
  ```bash
  git rev-parse --short HEAD; git diff --stat <punta VERDE de U07-B>...HEAD
  git diff --name-status <punta VERDE de U07-B>...HEAD -- backend/src/main/java/io/synkra/plataforma backend/src/test
  ```
  Esperado: solo estas rutas. Se anota el sha.
  - `backend/src/main/java/io/synkra/despliegue/**` (nuevos: `VencimientoPlanes.java`, `GuardaConfirmacion.java` y `RutasConfirmacion.java`; cambian `PlanServicio.java`, `CodigosDespliegue.java` y `MotivosDespliegue.java`)
  - `backend/src/main/java/io/synkra/plataforma/Reloj.java`
  - `backend/src/test/java/io/synkra/despliegue/**` y `backend/src/test/java/io/synkra/plataforma/RelojTest.java`
  - La segunda orden solo da líneas `A` (`Reloj.java`, `PlanPendienteTest.java`, `RelojMovible.java`, `RelojTest.java` y algún ayudante de prueba nuevo si hace falta): ninguna prueba existente cambia.
  - `bitacoras/U07-C.md` y `revisiones/U07-C/`

---

**Rojo primero:** `PlanPendienteTest` antes de `VencimientoPlanes`, `GuardaConfirmacion`, `RutasConfirmacion` y del cambio de reloj de `PlanServicio`; `RelojTest` antes de `Reloj`. Un commit «rojo» trae solo la prueba (y `RelojMovible`) y, si hace falta para compilar, firmas vacías que lanzan `UnsupportedOperationException`, nunca la lógica: la bitácora pega su sha y su `git show --stat`. Cada rojo queda en la bitácora, y CA-4 demuestra que cada regla muerde con el código terminado.
