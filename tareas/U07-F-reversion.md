# U07-F: reversión como plan nuevo

**Unidad:** U07: `Despliegue y reversión`
**Tareas del plan que agrupa:** U07-T10
**Historias que implementa:** H-20, H-17 (contribuye: un plan de reversión sin confirmar no está en el registro)
**Depende de:** U07-E (terminada), y por ella U07-D, U07-C, U06-C, U07-B, U07-A, U06-A, U06-B, U04 y U05. Sale de `<punta VERDE de U07-E>`. Asume como dado todo lo que fijaron esas tareas: `Plan` (con `REVERSION` y `origenId`), `Plan.nuevo`, `PlanRepositorio`, `PaisPermitido`, `VerificacionVivo`, `PlanServicio`, `RutasPlanes` y su `PlanDto`, `ResumenCatalogo`, `Usuario` (`X-Synkra-Usuario`), el rechazo estático `RECHAZADA` → 422 de U07-D, `GuardaConfirmacion`, `ConfirmacionServicio` y `ReservaConfirmacion` (R-D5 a R-D9, con el registro en `aplicando` antes de aplicar), `VigilanciaDespliegues.revisar()` y `TareasProgramadas` (R-D10, R-D12), `CodigosDespliegue` y `MotivosDespliegue`, el bean `Clock`, `RelojMovible`, `CatalogoDePrueba`, `SimuladorKubernetes` (con `marcaListo`, `marcaFallo`, `fallaAplicar`, `fallaLecturas`), la configuración de pruebas con la programación apagada, `RegistroApi` (`registrar`, `actualizarResultado`, `obtener`, `historial`, `ultimoListo`, `aplicando`), `GET /api/despliegues/{id}` y `GET /api/paises/{pais}/despliegues`, y las carpetas `build/sqlite-pruebas/<Clase>`.

---

## Alcance

**Dentro** (una línea, concreta):

> `POST /api/despliegues/{id}/reversion` (R-D11): el origen `{id}` debe ser un despliegue `listo` del registro; se crea un plan `pendiente` de tipo `reversion` en el país del origen, con su combinación, que pasa por R-D1, R-D3 (vuelve a verificar) y R-D4 (resumen y cambios contra lo que corre) con el mismo código que una creación, y que se confirma y se vigila por el camino de siempre (R-D5 a R-D10), de modo que el registro guarda `tipo` `reversion` y `origen_id` apuntando al origen.

**Fuera** (lo que el codificador debe rechazar aunque lo vea roto):

- U07-T12 y U07-T13: las fronteras de `despliegue` (`allowedDependencies`) y el resumen de U07 (U07-G).
- Una reversión automática, o reintentar algo que falló: la reversión solo existe porque una persona la pide, y su plan solo se aplica si alguien lo confirma (P2, AUTONOMIA-01).
- Elegir en la reversión el país, el core o los plugins: todo sale del origen; la ruta no tiene cuerpo (si llega uno, se ignora).
- Una ruta nueva para confirmar una reversión: se confirma con `POST /api/despliegues/{id}/confirmacion` de U07-D. Un `GET` nuevo sobre `/api/despliegues/**` (es de `registro`) y una cabecera `Location` en el 201 (el plan no está en el registro).
- Cambiar la vigilancia (`VigilanciaDespliegues`, `TareasProgramadas`) o la guarda de R-D5: una reversión `aplicando` se vigila igual que cualquier despliegue.
- `REQUIRES_NEW` en cualquier sitio (C-107). C-90, C-110, C-112, C-113 y C-114 siguen como están.
- Cualquier cambio en el código de producción de `registro`, `catalogo`, `kubernetes` y `plataforma`, en `verificador/`, `core/`, `plugin-api/`, `plugins/`, `init-descarga/`, en las migraciones, `application.properties`, `build.gradle.kts`, `FronterasModulosTest.java` y los `package-info.java`. Usar desde `despliegue` algo de `registro` que no esté en `registro.api`, o leer o escribir las tablas `reg_*` con SQL desde `despliegue`.
- Cambiar pruebas existentes, `SimuladorKubernetes.java` incluido. `CrearPlanTest`, `ConfirmarTest` y `VigilanciaTest` pasan sin cambios.
- Nada de `kubectl` ni de clúster real: el único Kubernetes es el servidor simulado de Fabric8 (AUTONOMIA-01).

---

## Archivos de contexto

- `tareas/U07-E-vigilancia-reinicio.md` (la base), `tareas/U07-D-confirmar.md` (decisiones 3, 5 y 6: confirmar), `tareas/U07-B-crear-plan.md` (decisión 6: el orden de la creación) y `tareas/U07-A-planes-pais.md` (R-D1)
- `aidlc-docs/construction/plans/U07-despliegue-code-generation-plan.md` (U07-T10)
- `aidlc-docs/construction/U07-despliegue/functional-design/business-rules.md` (R-D3, R-D4 y R-D11), `business-logic-model.md` (estados y rutas: `prepararReversion`) y `domain-entities.md` (`tipo`, `origen_id`)
- `aidlc-docs/construction/U06-registro/functional-design/domain-entities.md` (`origen_id` en `reg_despliegue`) y `backend/src/main/resources/db/migration/V3__registro.sql` (`origen_id` con clave foránea a `reg_despliegue` y el `CHECK` de tipo y origen)
- `aidlc-docs/inception/user-stories/stories.md` (H-20)
- `unidades-y-tareas.md` (sección U07)
- En la punta de U07-E: `backend/src/main/java/io/synkra/despliegue/` (todo: `PlanServicio`, `RutasPlanes`, `ConfirmacionServicio`, `ReservaConfirmacion`, `ResumenCatalogo`, `Usuario`, `VerificacionVivo`, `PaisPermitido`, `CalculoCambios`) y `backend/src/main/java/io/synkra/registro/api/` (`RegistroApi`, `DespliegueRegistrado`, `EstadoDespliegue`, `TipoDespliegue`, `PluginDesplegado`)
- `backend/src/test/java/io/synkra/despliegue/ConfirmarTest.java`, `VigilanciaTest.java`, `CatalogoDePrueba.java` y `RelojMovible.java`; `backend/src/test/java/io/synkra/kubernetes/SimuladorKubernetes.java`
- `verificador/src/testFixtures/java/io/synkra/verificador/SuiteCombinaciones.java` (S1, S2 y S4)
- `CANDIDATAS.md` (C-34, C-107, C-108)

---

## Decisiones del orquestador

1. **Nombres nuevos.** Todo en `io.synkra.despliegue`, no público.
   - `RutasReversion` (`@RestController`): `POST /api/despliegues/{id}/reversion`, sin cuerpo, con la cabecera opcional `X-Synkra-Usuario` (`@RequestHeader(required = false)`); responde 201 con el `PlanDto` de `RutasPlanes`, sin `Location`.
   - `PlanServicio.prepararReversion(long origenId, String creadoPor)`, que devuelve un `PlanPreparado`. `PlanServicio` pasa a inyectar `RegistroApi`.
   - `PlanDto` gana el campo `Long origenId`, justo después de `tipo` (nulo en un despliegue). `CrearPlanTest` compara campo a campo, así que no cambia.
   - En `CodigosDespliegue` se añaden `ORIGEN_NO_LISTO` y `ORIGEN_DESACTUALIZADO`; sus textos en `MotivosDespliegue`:
     - `ORIGEN_NO_LISTO_MENSAJE = "Solo se puede revertir a un despliegue listo"`;
     - `ORIGEN_DESACTUALIZADO_MENSAJE = "El catálogo ya no tiene lo que se desplegó en el origen"`.
2. **R-D11 (`prepararReversion`), el orden es contrato.**
   0. `Usuario.de(cabecera)` en la ruta (decisión 5 de U07-D): 422 `USUARIO_INVALIDO` antes de todo.
   1. `origenId ≤ 0`, o `registro.obtener(origenId)` vacío: 404 `ErrorApi(CodigosError.NO_ENCONTRADO, CodigosError.MSG_NO_ENCONTRADO, [])`. Un id que no es número da lo que da la plataforma (400 `SOLICITUD_INVALIDA`, como en U07-C).
   2. El origen no es `LISTO` (`APLICANDO` o `FALLIDO`): 422 `ErrorApi(ORIGEN_NO_LISTO, ORIGEN_NO_LISTO_MENSAJE, [])`.
   3. R-D1: `paises.namespace(origen.pais())`. Si el país del origen ya no está en `synkra.paises` (o no cumple S4), el 422 `PAIS_NO_PERMITIDO` de `PaisPermitido` tal cual. El país del plan es **siempre** el del origen («del mismo país»): nunca viene de la petición.
   4. R-D3: `verificacion.verificar(origen.core(), origen.versionCore(), <PluginPedido de origen.plugins()>)`. Si `RECHAZADA`: el mismo 422 `COMBINACION_RECHAZADA` de la creación (mensaje y un `DetalleError` por motivo, con el método estático de U07-D). Así un origen cuyo plugin ya no es compatible, o ya no está en el catálogo, da 422 con el motivo (H-20, tercer criterio). Los demás errores de `VerificacionVivo` (`CORE_NO_REGISTRADO`…) salen tal cual.
   5. El resumen: `resumenCatalogo.armar(pais, verificado.seleccion())`. Su `imagen` debe ser `origen.imagen()` y su conjunto de `(nombre, versión, sha256)` el de `origen.plugins()`. Si no: 422 `ErrorApi(ORIGEN_DESACTUALIZADO, ORIGEN_DESACTUALIZADO_MENSAJE, [])`. Motivo: volver atrás es volver a **lo que corrió**; si el catálogo da hoy otra imagen u otro artefacto con el mismo nombre y versión, el plan no sería una reversión.
   6. Lo que corre (`kubernetes.leerPais`, con los mismos 503 `KUBERNETES_NO_DISPONIBLE` y 409 `PAIS_INCONSISTENTE` de la creación) y los cambios (`CalculoCambios`), y el alta: `Plan.nuevo(pais, Plan.REVERSION, origenId, core, versionCore, <plugins>, <resumen>, <cambios>, creadoPor, reloj.instant())`. Los pasos 6 y 7 de la creación pasan a un método privado de `PlanServicio` que usan `prepararPlan` y `prepararReversion`, sin cambiar lo que produce la creación.
   - Ningún paso escribe en el clúster (R-D13): crear una reversión solo hace `GET`, como crear un plan. Los pasos 1 a 5 no hablan con el clúster.
   - Un plan idéntico a lo que corre se permite y lo dice (`sinCambios`, Q8), como en la creación.
   - No hay comprobación de R-D7 al crear: un `aplicando` en el país solo bloquea la **confirmación** (U07-D).
3. **Confirmar una reversión es confirmar un plan.** `POST /api/despliegues/{id}/confirmacion` hace R-D5 a R-D9 sin ninguna rama propia: vence a los 30 minutos (410), vuelve a verificar (R-D6), compara el resumen con el catálogo (`PLAN_DESACTUALIZADO`), un solo `aplicando` por país (R-D7) y registra en `aplicando` antes de aplicar.
   - `ConfirmacionServicio` debe pasar a `NuevoDespliegue` el `tipo` que sale de `plan.tipo()` (`reversion` → `TipoDespliegue.REVERSION`) y `plan.origenId()`. Si U07-D lo dejó fijo en `DESPLIEGUE`, se corrige en esa sola línea y la bitácora lo anota; si ya lo mapea, `ConfirmacionServicio` no cambia.
   - El origen no se vuelve a mirar al confirmar: un `listo` es inmutable (R-R2), y la clave foránea de `reg_despliegue.origen_id` lo exige en la base.
   - La vigilancia de U07-E cierra la reversión a `listo` o `fallido` como a cualquier otro despliegue.
4. **Lo que se ve por HTTP.** El 201 de la reversión trae `tipo` `reversion`, `origenId` el id del origen, `estado` `pendiente`, `core` y `versionCore` del origen, `creadoPor`, `creadoEn`, `venceEn`, `resumen` y `cambios`. Tras confirmar, `GET /api/despliegues/{id}` (de `registro`) trae `tipo` `reversion` y `origenId`: es lo que H-20 llama «el registro apunta al original» (`jq .origenId`; el `.original` de la historia es este campo, y se anota en la bitácora).
5. **`ReversionTest` (U07-T10): datos y casos.**
   - `@SpringBootTest(webEnvironment = RANDOM_PORT)`, `@TestBean AdaptadorKubernetes adaptador` (`SIM.adaptador()`) y `@TestBean Clock reloj` (`RelojMovible`), como `VigilanciaTest`; catálogo con `CatalogoDePrueba` (`SuiteCombinaciones`: S1 = `synkra-core` 2.1.0 + `kyc-co` 1.0.0, S2 = `synkra-core` 2.1.0 + `kyc-pe` 1.3.0, imagen `a`; `kyc-pe` 2.0.0 pide `3.x`). Carpeta fija `build/sqlite-pruebas/ReversionTest` (el `@DynamicPropertySource` la borra y crea), `synkra.paises=pe,py,co,cl,ar,ec,bo` (sin `mx`). La programación sigue apagada: la prueba llama a `vigilancia.revisar()` para cerrar despliegues.
   - `T3 = 2026-11-23T15:00:00.321Z` (`1795446000321` ms). Cada prueba usa su propio país y su propia hora base y **termina sin filas `aplicando`** (la pasada de la vigilancia recorre todos los países). Los planes de despliegue se crean con `X-Synkra-Usuario: ana`, las reversiones con `marta`, y todo se confirma con `luis`. Los orígenes «manipulados» se escriben con `RegistroApi.registrar` y `actualizarResultado` (ids 970 en adelante), sin plan. Se drena `SIM.nuevasPeticiones()` justo antes de cada acción que se mide.
     - `pe`, base `T3` (H-20, el camino entero): A = S2, confirmado, `SIM.marcaListo("pe")` y `revisar()` → `listo`. B = S1 a `T3 + 5 min`, confirmado, `SIM.marcaFallo("pe", "CrashLoopBackOff")` y `revisar()` → `fallido`. A `T3 + 10 min`, `POST /api/despliegues/{A}/reversion` con `marta` da 201 con `tipo` `reversion`, `origenId` A, `estado` `pendiente`, `creadoPor` `marta`, `creadoEn` `T3 + 10 min`, `venceEn` `+ 30 min`, el `resumen` igual al de A (imagen `a` y `kyc-pe` 1.3.0 con el sha256 del catálogo) y `cambios` con el core igual, `kyc-pe` 1.3.0 añadido, `kyc-co` 1.0.0 quitado, nada igual y `sinCambios` falso. Las peticiones son todas `GET`; `GET /api/despliegues/{plan}` da 404 y el historial de `pe` sigue con dos filas (H-17). Confirmar con `luis` da 202 y exactamente los tres `PATCH`; `registro.obtener(plan)` es `REVERSION`, `origenId` A, `creadoPor` `marta`, `confirmadoPor` `luis`, `APLICANDO`. `SIM.marcaListo("pe")` y `revisar()` → `LISTO`; `SIM.adaptador().leerPais("pe")` da la imagen `a` y solo `kyc-pe` 1.3.0; `GET /api/despliegues/{plan}` trae `tipo` `reversion`, `origenId` A y `estado` `listo`.
     - `py`, base `T3 + 1 h` (Q8 y R-D5): A = S2, confirmado y `listo`. La reversión a A mientras A corre da 201 con `sinCambios` verdadero, el core igual y `kyc-pe` 1.3.0 en `iguales`. Con `fija(venceEn)`, confirmarla da 410 `PLAN_VENCIDO`, el plan queda `vencido` y el historial de `py` sigue con una fila.
     - `co`, base `T3 + 2 h` (el origen no es `listo`): 970 (`co`, S2, termina `FALLIDO` con contexto) y 971 (`co`, S2, sigue `APLICANDO`). Las dos reversiones dan 422 `ORIGEN_NO_LISTO` con el mensaje exacto, sin fila en `dep_plan` y sin peticiones. Al final, 971 se cierra con `actualizarResultado(971, FALLIDO, new ContextoFallo("", "", "cerrado por la prueba"))`.
     - `cl`, base `T3 + 3 h` (registro manipulado, R-D3): 972 (`cl`, `synkra-core` 2.1.0 + `kyc-pe` 2.0.0 con su sha256 del catálogo, `LISTO`) da 422 `COMBINACION_RECHAZADA` con exactamente un detalle `plugins.kyc-pe` cuyo `motivo` es el `mensaje` que da `POST /api/verificaciones` para la misma selección; 977 (`cl`, `synkra-core` 2.1.0 + `kyc-qq` 1.0.0, que no está en el catálogo, `LISTO`) da 422 `COMBINACION_RECHAZADA` con un detalle `plugins.kyc-qq` (el plugin «se borró», H-20). Sin filas y sin peticiones.
     - `ar`, base `T3 + 4 h` (el catálogo ya no tiene lo que corrió): 973 (`ar`, S2 con el sha256 `7` × 64, `LISTO`) y 975 (`ar`, S2 con la imagen `b`, `LISTO`) dan 422 `ORIGEN_DESACTUALIZADO` con el mensaje exacto y `detalles` vacío; sin filas y sin peticiones.
     - `mx` (R-D1): 974 (`mx`, S2, `LISTO`) da 422 `PAIS_NO_PERMITIDO` con el detalle de `PaisPermitido` (`pais`, `PAIS_FUERA_DE_LISTA`); sin filas y sin peticiones.
     - `ec`, base `T3 + 5 h` (R-D7 al confirmar la reversión): A = S2, `listo`; C = S1, confirmado y sin marcar (sigue `aplicando`). La reversión a A da 201 (crear no mira R-D7); confirmarla da 409 `DESPLIEGUE_EN_CURSO` sin peticiones y el plan sigue `pendiente`. Al final, `SIM.marcaFallo("ec", "CrashLoopBackOff")` y `revisar()` cierran C en `fallido`.
     - `bo`, base `T3 + 6 h` (usuario): 976 (`bo`, S2, `LISTO`); con `X-Synkra-Usuario: Marta`, 422 `USUARIO_INVALIDO` (campo `X-Synkra-Usuario`, motivo exacto de U07-D), sin fila y sin peticiones.
     - Sin país: `/999/reversion`, `/0/` y `/-1/` dan 404 `NO_ENCONTRADO`; `/abc/` da 400 `SOLICITUD_INVALIDA`; sin peticiones.
   - Todos los rechazos comparan `codigo`, `mensaje` exacto y `detalles` (campo y motivo) exactos.
6. **Bitácora en `bitacoras/U07-F.md`, evidencia en `bitacoras/U07-F-evidencia/`** (un `caN.txt` por criterio, salida completa con fecha, sha y comando), y el revisor en `revisiones/U07-F/`. La bitácora anota si `ConfirmacionServicio` cambió (decisión 3) y el nombre `origenId` frente al `.original` de H-20 (decisión 4).

---

## Criterios de aceptación

- [ ] **CA-1**: U07-T10. La reversión como plan nuevo, contra el servidor simulado, con lectura de vuelta
  ```bash
  ./gradlew :backend:test --rerun --tests '*ReversionTest'
  grep -ho 'tests="[0-9]*" skipped="[0-9]*" failures="[0-9]*" errors="[0-9]*"' backend/build/test-results/test/TEST-*ReversionTest.xml
  grep -o 'testcase name="[A-Za-z0-9]*()"' backend/build/test-results/test/TEST-*ReversionTest.xml
  python3 -I - backend/build/sqlite-pruebas/ReversionTest/synkra.db <<'EOF'
  import sqlite3, sys
  c = sqlite3.connect(sys.argv[1])
  q = lambda s: print(c.execute(s).fetchall())
  q("select p.pais, p.tipo, p.origen_id is not null, p.estado, p.creado_por, r.tipo, r.estado, r.confirmado_por from dep_plan p"
    " left join reg_despliegue r on r.id = p.id order by p.pais, p.id")
  q("select p.pais, p.origen_id = (select min(a.id) from dep_plan a where a.pais = p.pais) from dep_plan p where p.tipo = 'reversion' order by p.pais")
  q("select r.pais, r.origen_id = (select min(a.id) from dep_plan a where a.pais = r.pais), r.imagen = o.imagen,"
    " (select group_concat(nombre || ' ' || version || ' ' || sha256) from reg_plugin where despliegue_id = r.id)"
    " = (select group_concat(nombre || ' ' || version || ' ' || sha256) from reg_plugin where despliegue_id = o.id)"
    " from reg_despliegue r join reg_despliegue o on o.id = r.origen_id where r.tipo = 'reversion'")
  q("select a.resumen = r.resumen from dep_plan r join dep_plan a on a.id = r.origen_id where r.pais = 'pe'")
  q("select id, pais, estado from reg_despliegue where id not in (select id from dep_plan) order by id")
  q("select (select count(*) from dep_plan where pais in ('co', 'cl', 'ar', 'mx', 'bo')), (select count(*) from reg_despliegue where estado = 'aplicando')")
  EOF
  ```
  Esperado:
  - `BUILD SUCCESSFUL`, `tests` ≥ 9 y `skipped="0" failures="0" errors="0"`, con los casos y afirmaciones de la decisión 5 (nombres en la evidencia).
  - Python, en orden:
    - `[('ec', 'despliegue', 0, 'confirmado', 'ana', 'despliegue', 'listo', 'luis'), ('ec', 'despliegue', 0, 'confirmado', 'ana', 'despliegue', 'fallido', 'luis'), ('ec', 'reversion', 1, 'pendiente', 'marta', None, None, None), ('pe', 'despliegue', 0, 'confirmado', 'ana', 'despliegue', 'listo', 'luis'), ('pe', 'despliegue', 0, 'confirmado', 'ana', 'despliegue', 'fallido', 'luis'), ('pe', 'reversion', 1, 'confirmado', 'marta', 'reversion', 'listo', 'luis'), ('py', 'despliegue', 0, 'confirmado', 'ana', 'despliegue', 'listo', 'luis'), ('py', 'reversion', 1, 'vencido', 'marta', None, None, None)]`;
    - `[('ec', 1), ('pe', 1), ('py', 1)]` (cada reversión apunta al primer despliegue de su país);
    - `[('pe', 1, 1, 1)]` (el registro apunta al origen y guarda su imagen y sus plugins);
    - `[(1,)]`;
    - `[(970, 'co', 'fallido'), (971, 'co', 'fallido'), (972, 'cl', 'listo'), (973, 'ar', 'listo'), (974, 'mx', 'listo'), (975, 'ar', 'listo'), (976, 'bo', 'listo'), (977, 'cl', 'listo')]`;
    - `[(0, 0)]`.

- [ ] **CA-2**: el JAR real, sin clúster: el orden de los rechazos de la reversión, sin crear nada
  ```bash
  ./gradlew :backend:bootJar
  export SYNKRA_DATA=$(mktemp -d); J=$(mktemp -d); H=$(mktemp -d)
  arranca() { HOME="$H" KUBECONFIG="$H/kubeconfig-falso" SYNKRA_PAISES=co,pe java -Duser.home="$H" -jar backend/build/libs/backend-*.jar --server.port=18095 > "$J/log-$1.txt" 2>&1 & PID=$!; timeout 90 bash -c 'until curl -sf localhost:18095/actuator/health >/dev/null; do sleep 1; done'; }
  python3 -I -c "import sys, zipfile
  with zipfile.ZipFile(sys.argv[1], 'w') as z: z.writestr('synkra-plugin.yaml', 'nombre: kyc-pe\nversion: 1.3.0\ncore: synkra-core\ncoreCompatible: 2.x\n')" "$J/a.jar"
  # 1. Primer arranque: catálogo por la API (dos cores y kyc-pe 1.3.0).
  arranca 0
  for c in synkra-core:2.1.0:a otro-core:1.0.0:c; do IFS=: read n v x <<< "$c"; curl -s -o /dev/null -w '%{http_code}\n' -H 'Content-Type: application/json' \
    -d "{\"nombre\":\"$n\",\"version\":\"$v\",\"imagen\":\"localhost:5001/$n@sha256:$(printf "$x%.0s" $(seq 64))\"}" localhost:18095/api/cores; done
  curl -s -o /dev/null -w '%{http_code}\n' -F "archivo=@$J/a.jar" localhost:18095/api/cores/synkra-core/plugins
  kill $PID; wait $PID 2>/dev/null
  # 2. Orígenes sembrados con SQL en el registro (el JAR no puede desplegar sin clúster).
  python3 -I - "$SYNKRA_DATA/synkra.db" "$J/a.jar" <<'EOF'
  import hashlib, sqlite3, sys, time
  c = sqlite3.connect(sys.argv[1]); sha = hashlib.sha256(open(sys.argv[2], "rb").read()).hexdigest()
  ahora = int(time.time() * 1000)
  def img(n, x): return "localhost:5001/%s@sha256:%s" % (n, x * 64)
  filas = ((61, "pe", "synkra-core", "2.1.0", img("synkra-core", "a"), sha, "listo"),
           (62, "pe", "synkra-core", "2.1.0", img("synkra-core", "a"), sha, "fallido"),
           (63, "pe", "synkra-core", "2.1.0", img("synkra-core", "a"), sha, "aplicando"),
           (64, "pe", "synkra-core", "2.1.0", img("synkra-core", "a"), "7" * 64, "listo"),
           (65, "pe", "otro-core", "1.0.0", img("otro-core", "c"), sha, "listo"),
           (66, "mx", "synkra-core", "2.1.0", img("synkra-core", "a"), sha, "listo"))
  for i, pais, core, v, imagen, s, estado in filas:
      c.execute("insert into reg_despliegue (id, pais, tipo, origen_id, core, version_core, imagen, creado_por, confirmado_por,"
                " confirmado_en, estado, terminado_en, veredicto) values (?, ?, 'despliegue', null, ?, ?, ?, 'ana', 'luis', ?, ?, ?,"
                " '{\"global\":\"APROBADA\",\"porPlugin\":[],\"motivosGlobales\":[]}')",
                (i, pais, core, v, imagen, ahora, estado, None if estado == "aplicando" else ahora))
      c.execute("insert into reg_plugin (despliegue_id, nombre, version, sha256, etiquetas) values (?, 'kyc-pe', '1.3.0', ?, '{}')", (i, s))
  c.execute("insert into reg_contexto (despliegue_id, log_core, log_init, eventos) values (62, '', '', 'sembrado')")
  c.commit()
  EOF
  # 3. Segundo arranque: pedidos de reversión.
  arranca 1
  revierte() { curl -s -o "$J/r.json" -w '%{http_code} ' -X POST "${@:2}" localhost:18095/api/despliegues/$1/reversion
    python3 -I -c "import json,sys; r=json.load(open(sys.argv[1])); print(r['codigo'], r['mensaje'], [d['campo'] for d in r['detalles']])" "$J/r.json"; }
  revierte 61 -H 'X-Synkra-Usuario: Marta Ruiz'; for id in 61 62 63 64 65 66 999; do revierte $id -H 'X-Synkra-Usuario: marta'; done
  kill $PID; wait $PID 2>/dev/null
  python3 -I -c "import sqlite3,sys; c=sqlite3.connect(sys.argv[1]); print(c.execute('select count(*) from dep_plan').fetchone()); print(c.execute('select id, pais, estado from reg_despliegue order by id').fetchall())" "$SYNKRA_DATA/synkra.db"
  ```
  Esperado, en orden:
  - `201`, `201`, `201`;
  - `422 USUARIO_INVALIDO El usuario no es válido ['X-Synkra-Usuario']`;
  - `503 KUBERNETES_NO_DISPONIBLE No se puede leer lo que corre en el país []` (61 pasa todas las comprobaciones y solo falta leer lo que corre);
  - `422 ORIGEN_NO_LISTO Solo se puede revertir a un despliegue listo []` (dos veces: 62 `fallido` y 63 `aplicando`);
  - `422 ORIGEN_DESACTUALIZADO El catálogo ya no tiene lo que se desplegó en el origen []`;
  - `422 COMBINACION_RECHAZADA La combinación no es compatible ['plugins.kyc-pe']`;
  - `422 PAIS_NO_PERMITIDO El país no está permitido ['pais']`;
  - `404 NO_ENCONTRADO Recurso no encontrado []`;
  - `(0,)` y `[(61, 'pe', 'listo'), (62, 'pe', 'fallido'), (63, 'pe', 'aplicando'), (64, 'pe', 'listo'), (65, 'pe', 'listo'), (66, 'mx', 'listo')]` (63 sigue `aplicando`: su plazo de 3 min no ha vencido).

  Si el puerto 18095 está ocupado, se anota en la bitácora y se usa otro en todo el criterio. La bitácora pega la salida tal cual.

- [ ] **CA-3**: rojo primero. Cada regla nueva tiene una prueba que muerde, demostrado en una copia
  ```bash
  D=$(mktemp -d); git archive HEAD | tar -x -C "$D"; (cd "$D" && git init -q)
  # Mutación anotada literalmente en la bitácora; una por regla, por ejemplo:
  #   origen listo:     se quita la comprobación de LISTO (co da 201)
  #   R-D1:             no se valida el país del origen (mx da 201)
  #   R-D3:             no se verifica la combinación del origen (cl da 201)
  #   resumen y origen: no se compara el resumen con el origen (ar da 201)
  #   tipo:             el plan se guarda con Plan.DESPLIEGUE (pe: tipo despliegue)
  #   origen:           el plan se guarda con origenId nulo (pe: sin origen)
  #   confirmar:        ConfirmacionServicio registra siempre TipoDespliegue.DESPLIEGUE (pe: el registro no apunta al origen)
  #   usuario:          prepararReversion usa PlanServicio.CREADO_POR (pe: creadoPor anonimo)
  #   sin clúster:      leerPais va antes de la comprobación de LISTO (co trae peticiones)
  sed -i '<mutación>' "$D/backend/src/main/java/io/synkra/despliegue/<Clase>.java"
  (cd "$D" && ./gradlew :backend:test --tests '*ReversionTest')
  grep -l '<failure' "$D"/backend/build/test-results/test/TEST-*.xml
  rm -rf "$D"
  ```
  Esperado: con cada una de las nueve mutaciones (una copia por mutación, con `git init -q`), `BUILD FAILED`, y el XML de `ReversionTest` muestra al menos un `<failure` en la prueba de esa regla. La bitácora pega el `sed` (o el parche) y el nombre de la prueba que cayó. Sin `git stash` ni cambios en el worktree. Además, la bitácora registra el rojo de `ReversionTest` antes de `RutasReversion` y `prepararReversion`.

- [ ] **CA-4**: todo el backend sigue pasando, y la reversión usa el registro y el clúster solo por donde debe
  ```bash
  ./gradlew :backend:test --rerun :backend:check
  for f in backend/build/test-results/test/TEST-*.xml; do grep -Ho 'tests="[0-9]*" skipped="[0-9]*" failures="[0-9]*" errors="[0-9]*"' "$f"; done
  grep -rnE "io\.synkra\.registro\." backend/src/main/java/io/synkra/despliegue | grep -vE "io\.synkra\.registro\.api\."; echo "registro-interno=$?"
  grep -rn "reg_" backend/src/main/java/io/synkra/despliegue; echo "tablas-reg=$?"
  grep -rlE "kubernetes\.(aplicar|construir)\(" backend/src/main/java/io/synkra/despliegue
  grep -n "kubernetes\|AdaptadorKubernetes\|RegistroApi" backend/src/main/java/io/synkra/despliegue/RutasReversion.java; echo "rutas=$?"
  grep -rn "REQUIRES_NEW" backend/src/main/java; echo "requiresNew=$?"
  grep -rln "@Scheduled\|@EnableScheduling" backend/src/main/java
  grep -n "TipoDespliegue\." backend/src/main/java/io/synkra/despliegue/ConfirmacionServicio.java
  ```
  Esperado:
  - `BUILD SUCCESSFUL` y ningún XML con `skipped`, `failures` o `errors` distinto de 0 (`CrearPlanTest`, `ConfirmarTest`, `CarpetaEquivocadaPlanTest`, `VigilanciaTest`, `RetomarVigilanciaTest`, `PlanPendienteTest` y `FronterasModulosTest` pasan sin cambios).
  - `registro-interno=1` y `tablas-reg=1`.
  - Solo `backend/src/main/java/io/synkra/despliegue/ConfirmacionServicio.java` (la reversión no aplica nada por su cuenta).
  - `rutas=1` (la ruta solo llama a `PlanServicio`).
  - `requiresNew=1`, y solo `backend/src/main/java/io/synkra/despliegue/TareasProgramadas.java`.
  - Las líneas de `ConfirmacionServicio` con `TipoDespliegue.` mapean los dos tipos desde `plan.tipo()` (o no hay ninguna si usa `valueOf`); la bitácora las pega y explica cuál es el caso.

- [ ] **CA-5**: el diff queda acotado
  ```bash
  git rev-parse --short HEAD; git diff --stat <punta VERDE de U07-E>...HEAD
  git diff --name-status <punta VERDE de U07-E>...HEAD -- backend/src/test backend/src/main/resources backend/src/main/java/io/synkra/registro backend/src/main/java/io/synkra/plataforma backend/src/main/java/io/synkra/kubernetes backend/src/main/java/io/synkra/catalogo
  ```
  Esperado: solo estas rutas. Se anota el sha.
  - `backend/src/main/java/io/synkra/despliegue/**` (nuevo: `RutasReversion.java`; cambian `PlanServicio.java`, `RutasPlanes.java` (solo `PlanDto`), `CodigosDespliegue.java`, `MotivosDespliegue.java` y, solo si hace falta (decisión 3), `ConfirmacionServicio.java`)
  - `backend/src/test/java/io/synkra/despliegue/ReversionTest.java` (y algún ayudante de prueba nuevo si hace falta): la segunda orden solo da líneas `A`
  - ninguna línea de `backend/src/main/resources`, `registro`, `plataforma`, `kubernetes` ni `catalogo`
  - `bitacoras/U07-F.md`, `bitacoras/U07-F-evidencia/` y `revisiones/U07-F/`

  `ca5.txt` se genera justo antes de su propio commit, con el árbol limpio y sin commits de código después: ese commit solo trae `ca5.txt` (y la línea de la bitácora que lo cita). Toda la demás evidencia sale del último commit que toca código.

---

**Rojo primero:** `ReversionTest` antes de `RutasReversion`, `prepararReversion`, el campo `origenId` de `PlanDto` y, si hace falta, el cambio de `ConfirmacionServicio`. Un commit «rojo» trae solo la prueba y, si hace falta para compilar, firmas vacías que lanzan `UnsupportedOperationException`, nunca la lógica: la bitácora pega su sha y su `git show --stat`. Cada rojo queda en la bitácora, y CA-3 demuestra que cada regla muerde con el código terminado.
