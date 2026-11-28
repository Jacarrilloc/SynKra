# U07-B: verificación en vivo y plan con resumen y cambios

**Unidad:** U07: `Despliegue y reversión`
**Tareas del plan que agrupa:** U07-T03, U07-T04 y U07-T05
**Historias que implementa:** H-09, H-11, H-08 (contribuye: alternativas por HTTP)
**Depende de:** U07-A (terminada), U01, U04 y U05 (terminadas, en `main`). Sale de `<punta VERDE de U07-A>`. Asume como dado todo lo que fijó U07-A: `V4__despliegue.sql`, `Plan`, `Plan.nuevo`, `Plan.VIGENCIA`, `PlanRepositorio`, `PaisPermitido` (`exigir` y `namespace`), `CodigosDespliegue` y `MotivosDespliegue`, la propiedad `synkra.paises` ya escrita en `application.properties`, el `Clock` privado (sin bean `Clock`) y las carpetas `build/sqlite-pruebas/<Clase>`. Corre **en paralelo con U06-B**: no puede usar nada de `registro` (ni `registro.api` ni las tablas `reg_*`, que en esta rama no existen).

---

## Alcance

**Dentro** (una línea, concreta):

> `POST /api/verificaciones` (R-D2: U01 `verificar` + `alternativas` sobre el catálogo de U04, 200 con el veredicto, sin filas en `dep_plan` y sin hablar con Kubernetes) y `POST /api/despliegues` (R-D3 y R-D4: país permitido, verificación, resumen con namespace, imagen por digest y plugins con sha256, y cambios contra lo que corre **leído del clúster con U05 `leerPais`**; 201 `pendiente`, o 422 sin fila), más la propiedad PBT-03 del cálculo de cambios.

**Fuera** (lo que el codificador debe rechazar aunque lo vea roto):

- U07-T06 en adelante: el paso a `vencido` y el 410, confirmar (R-D5 a R-D9), un solo `aplicando` por país (R-D7), vigilancia, retomar tras reinicio, reversión, el caso P4 de U07-T11 como tarea propia, las fronteras de U07-T12 y el resumen. Ningún cambio de estado de un plan: esta tarea solo inserta planes `pendiente`.
- `GET /api/despliegues/{id}` y cualquier otro `GET` sobre `/api/despliegues/**`: esa ruta es de `registro` (decisión 6 de U06-B). Tampoco `GET /api/despliegues?estado=…`.
- Cualquier uso de `registro` desde `despliegue` (ver la decisión 2), y cualquier llamada a `aplicar`, `construir`, `estado` o `contextoFallo` de U05 desde el código de producción.
- C-35 (longitud de nombres), C-76 (reintentos del cliente de Fabric8), C-82, C-83 y C-87 (colisión de JAR al armar el `PlanPais`): van con la confirmación y la vigilancia (U07-T07 y T08).
- Cambios en `verificador/`, `core/`, `plugin-api/`, `plugins/`, `init-descarga/`, en el código de producción de los módulos `catalogo`, `kubernetes`, `plataforma` y `registro`, en `V4__despliegue.sql`, `application.properties`, `build.gradle.kts` y `FronterasModulosTest.java`. Fuera de `despliegue` solo se **añade** un archivo de prueba: `backend/src/test/java/io/synkra/kubernetes/SimuladorKubernetes.java` (decisión 8). No se cambia ninguna prueba existente.
- Nada de `kubectl` ni de clúster real: el único Kubernetes es el servidor simulado de Fabric8 (AUTONOMIA-01).

---

## Archivos de contexto

- `tareas/U07-A-planes-pais.md` (contrato previo: decisiones 1 a 9)
- `aidlc-docs/construction/plans/U07-despliegue-code-generation-plan.md` (U07-T03, T04 y T05)
- `aidlc-docs/construction/U07-despliegue/functional-design/business-rules.md` (R-D1 a R-D4), `domain-entities.md` (`dep_plan`, `Seleccion`) y `business-logic-model.md` (PBT-03)
- `aidlc-docs/inception/requirements/requirements.md` (RF-11 y RF-13), `aidlc-docs/inception/user-stories/stories.md` (H-08, H-09 y H-11) y `aidlc-docs/inception/application-design/services.md` (S2 y S3)
- `unidades-y-tareas.md` (sección U07)
- `verificador/src/main/java/io/synkra/verificador/` (`Verificador.verificar` y `alternativas`, `Seleccion`, `Descriptor`, `Veredicto`, `Motivo`, `Alternativa`, `Version`, `RangoVersion`) y `verificador/src/testFixtures/java/io/synkra/verificador/SuiteCombinaciones.java` (S1 a S10, en especial S4 y S7)
- `backend/src/main/java/io/synkra/catalogo/api/` (`CatalogoApi`: `catalogoParaVerificar`, `listarCores`, `obtenerCore`, `obtenerPlugin`)
- `backend/src/main/java/io/synkra/kubernetes/api/` (`AdaptadorKubernetes.leerPais`, `LecturaPais`, `PlanPais`, `ClienteNoDisponibleException`, `LecturaFallidaException`, `LecturaPaisInconsistenteException`)
- `backend/src/test/java/io/synkra/catalogo/CatalogoVerificadorTest.java` (cargar la suite en SQLite por `CatalogoApi`, JAR con solo el descriptor) y `RutasCoresTest.java` (`RANDOM_PORT`)
- `backend/src/test/java/io/synkra/kubernetes/LeerPaisTest.java` (servidor simulado en modo CRUD, `requestRetryBackoffLimit(0)`, casa y kubeconfig falsos, conteo de peticiones)
- `backend/src/test/java/io/synkra/catalogo/RutasSegurasPropiedades.java` (estilo jqwik del backend)
- En la punta de U07-A: `backend/src/main/java/io/synkra/despliegue/` y `backend/src/test/java/io/synkra/despliegue/PaisPermitidoTest.java` (`@MockitoBean AdaptadorKubernetes`)
- `CANDIDATAS.md` (C-05, C-34, C-76 y C-90)

---

## Decisiones del orquestador

1. **Paquetes y nombres.** Todo en `io.synkra.despliegue`, no público. Sigue sin paquete `api` (nadie de fuera usa nada de aquí todavía).
   - `PluginPedido(String nombre, String version)` (record): un plugin de la petición.
   - `VerificacionVivo` (`@Service`): `Verificado verificar(String core, String versionCore, List<PluginPedido> plugins)`, con `Verificado(Seleccion seleccion, Veredicto veredicto)` (record).
   - `RutasVerificacion` (`@RestController`, `POST /api/verificaciones`), con su record anidado `SolicitudVerificacion(String core, String versionCore, List<PluginPedido> plugins)`.
   - `VeredictoDto` (record con `static VeredictoDto de(Veredicto)`).
   - `PluginResumen(String nombre, String version, String sha256)`, `Resumen(String namespace, String core, String versionCore, String imagen, List<PluginResumen> plugins)`, `CambioCore(String antes, String despues, boolean igual)` y `Cambios(CambioCore core, List<PluginResumen> anadidos, List<PluginResumen> quitados, List<PluginResumen> iguales, boolean sinCambios)` (records).
   - `CalculoCambios` (clase final con un método estático puro): `Cambios calcular(Optional<LecturaPais> corre, String imagen, List<PluginResumen> seleccion)`.
   - `PlanServicio` (`@Service`): `PlanPreparado prepararPlan(String pais, String core, String versionCore, List<PluginPedido> plugins)`, con `PlanPreparado(Plan plan, Resumen resumen, Cambios cambios)` (record).
   - `RutasPlanes` (`@RestController`, `POST /api/despliegues`), con su record anidado `SolicitudPlan(String pais, String core, String versionCore, List<PluginPedido> plugins)` y su `PlanDto`.
   - En `CodigosDespliegue` se añaden `SELECCION_INVALIDA`, `CORE_NO_REGISTRADO`, `COMBINACION_RECHAZADA`, `KUBERNETES_NO_DISPONIBLE` y `PAIS_INCONSISTENTE`; los textos de sus motivos van en `MotivosDespliegue`.
2. **«Lo que corre» sale del clúster, con U05 `leerPais`, no del registro.** R-D4 dice «contra el último `listo` del país (U06)», pero U06 va en otra rama y esta tarea no puede depender de él. Se usa `AdaptadorKubernetes.leerPais(pais)` (`kubernetes.api`, ya en `main`), que es lo que de verdad corre (RF-13: «qué cambia frente a lo que corre hoy») y es la misma fuente con la que U06 calcula la concordancia (R-R6). Se anota en la bitácora como desviación del diseño. Consecuencias:
   - `despliegue` depende de `kubernetes.api` y de `catalogo.api` (lo permite U07-T12), nunca de sus paquetes internos ni de `io.fabric8`.
   - C-90 queda igual: `leerPais` es más laxo que el core, así que los cambios muestran el ConfigMap tal como se lee aunque el core no arrancara con él. No se corrige aquí.
   - Si al unir con U06 se prefiere el registro, lo decide el orquestador en U07-T07; esta tarea no deja ninguna costura para eso.
3. **Validación de la selección (común a las dos rutas).**
   - `plugins` nulo cuenta como lista vacía (S3: core sin plugins).
   - `core` nulo o fuera de `Descriptor.NOMBRE`; `versionCore` nulo o que `Version.parsear` rechaza; un plugin nulo, o con `nombre` fuera de `Descriptor.NOMBRE` o `version` que no parsea: 422 `SELECCION_INVALIDA`, con **todos** los detalles juntos, de campos `core`, `versionCore`, `plugins[<i>]`, `plugins[<i>].nombre` y `plugins[<i>].version`. Ningún motivo repite lo recibido (C-34).
   - El core debe estar registrado en esa versión: si `catalogoParaVerificar().versionesCore(core)` está vacía, 422 `CORE_NO_REGISTRADO` con campo `core`; si no contiene la versión, el mismo código con campo `versionCore`. El verificador no lo comprueba y el resumen necesita la imagen de esa versión.
4. **Descriptores de la selección.** La petición trae nombre y versión; el verificador juzga con descriptores. Para cada plugin pedido:
   - se usa `catalogo.descriptor(core, nombre, version)` de la carpeta del core pedido;
   - si no está, se busca en las carpetas de los demás cores (`listarCores()`, en orden de nombre) y se usa el primero que lo tenga: así el verificador da `CORE_EQUIVOCADO` (S7, y prepara P4 de U07-T11);
   - si no está en ninguna, un descriptor provisional con `core` = el pedido y `coreCompatible` = la versión exacta del core pedido; el verificador lo marca `PLUGIN_AUSENTE` (S6).
   - `VerificacionVivo.verificar` es `@Transactional(readOnly = true)`: `verificar` y `alternativas` leen el mismo catálogo (la conexión es una sola), y `alternativas` nunca ve un veredicto distinto del suyo.
5. **R-D2 (`POST /api/verificaciones`).**
   - 200 siempre que la selección sea válida (decisión 3), también con `RECHAZADA` (RF-11).
   - Cuerpo `VeredictoDto`: `{global, plugins: [{nombre, version, core, compatible, motivos: [{codigo, plugin, mensaje, campo}], alternativas: [{tipo, nombre, version}]}], motivosGlobales: [...]}`. Las versiones van como texto (`"1.3.0"`); `plugin` y `campo` son texto o `null`. Así `jq .plugins[0].alternativas` de H-08 funciona tal cual.
   - El veredicto es exactamente `Verificador.alternativas(seleccion, Verificador.verificar(seleccion, catalogo), catalogo)`.
   - No toca `dep_plan` ni el `AdaptadorKubernetes`, y no pide país.
6. **R-D3 (`POST /api/despliegues`): el orden es contrato.**
   1. `PaisPermitido.namespace(pais)` (R-D1, de U07-A). Un país no permitido da su 422 `PAIS_NO_PERMITIDO` aunque la selección también sea inválida.
   2. La validación y la verificación de las decisiones 3 a 5. Si `RECHAZADA`: 422 `COMBINACION_RECHAZADA`, mensaje «La combinación no es compatible», un `DetalleError` por motivo (campo `plugins.<nombre>` si el motivo nombra plugin, `plugins` si no; motivo = el mensaje del verificador). Ni fila ni **ninguna** petición a Kubernetes. El veredicto completo, con alternativas, se pide a `/api/verificaciones`.
   3. El resumen, del catálogo: la imagen con digest de `obtenerCore(core).imagenes()` para esa versión y el `sha256` de cada plugin con `obtenerPlugin`. Plugins ordenados por nombre.
   4. `leerPais(pais)`. `ClienteNoDisponibleException` o `LecturaFallidaException`: 503 `KUBERNETES_NO_DISPONIBLE` («No se puede leer lo que corre en el país»). `LecturaPaisInconsistenteException`: 409 `PAIS_INCONSISTENTE`. En los dos casos, sin fila. Los motivos no copian el mensaje de la excepción.
   5. `CalculoCambios.calcular` y el alta con `Plan.nuevo(pais, "despliegue", null, core, versionCore, plugins, resumen, cambios, "anonimo", ahora)`, insertada con `JdbcAggregateTemplate.insert` en una transacción corta que **no** incluye la lectura del clúster (la base tiene una sola conexión). `creadoPor` es `"anonimo"` mientras no haya usuarios (como `CatalogoServicio.AUTOR`).
   - Respuesta 201 sin cabecera `Location` (el `GET` de un plan pendiente no existe todavía, decisión 6 de U06-B). Cuerpo `PlanDto`: `{id, pais, tipo, estado, core, versionCore, creadoPor, creadoEn, venceEn, resumen, cambios}`, con `tipo` y `estado` en minúsculas y las fechas en ISO-8601 (el `ObjectMapper` de Spring Boot).
   - Un plan idéntico a lo que corre se crea igual (Q8), con `sinCambios: true`.
7. **R-D4 (`CalculoCambios`) y su JSON.**
   - Un plugin es la terna `(nombre, version, sha256)` y se compara entera: si cambia la versión o el hash de un plugin, sale en `quitados` (lo viejo) y en `anadidos` (lo nuevo).
   - `anadidos` = selección − corre; `quitados` = corre − selección; `iguales` = corre ∩ selección. Las tres, ordenadas por nombre y versión.
   - `core`: `antes` = la imagen leída del Deployment (o `null` si el país está vacío), `despues` = la imagen del resumen, `igual` = `antes.equals(despues)`.
   - `sinCambios` = `core.igual` y `anadidos` y `quitados` vacíos.
   - En `dep_plan`: `plugins` = `[{nombre, version}]` ordenado por nombre; `resumen` = `{namespace, core, versionCore, imagen, plugins}`; `cambios` = `{core: {antes, despues, igual}, anadidos, quitados, iguales, sinCambios}`. Las claves van en ASCII (`anadidos`, no `añadidos`). El JSON de la respuesta tiene la misma forma.
8. **Pruebas contra el servidor simulado, con un ayudante en el paquete de prueba de `kubernetes`.** `AdaptadorFabric8` y `VariablesPod` son privados de paquete, así que una prueba de `despliegue` no puede armar el adaptador real sobre el simulador. Se añade **un** archivo de prueba, `backend/src/test/java/io/synkra/kubernetes/SimuladorKubernetes.java` (clase pública, `AutoCloseable`), que:
   - arranca un `KubernetesMockServer` en modo CRUD con `KubernetesMixedDispatcher` (como `LeerPaisTest`), con casa y kubeconfig falsos que restaura al cerrar, y un cliente con `requestRetryBackoffLimit(0)`;
   - expone `AdaptadorKubernetes adaptador()` (un `AdaptadorFabric8` sobre ese cliente), `void siembra(PlanPais)` (Deployment y ConfigMap de `construir`), `void siembraSoloDeployment(PlanPais)`, `void fallaLecturas(String pais)` (500 al `GET` del Deployment de ese país) y `List<String> nuevasPeticiones()` (`"MÉTODO ruta"` de lo recibido desde la llamada anterior);
   - no menciona ningún tipo de `io.fabric8` en su API pública.

   Las pruebas de `despliegue` reemplazan el bean con `@TestBean AdaptadorKubernetes` y una fábrica estática que devuelve `simulador.adaptador()`; si `@TestBean` no sirve en este contexto, se usa un `@Bean @Primary` en una `@TestConfiguration` y se anota en la bitácora. `VerificacionVivoTest` usa en cambio `@MockitoBean AdaptadorKubernetes` y `verifyNoInteractions`, como `PaisPermitidoTest`.
9. **Catálogo de las pruebas.** `VerificacionVivoTest` y `CrearPlanTest` cargan `SuiteCombinaciones.contenido()` por `CatalogoApi`, como `CatalogoVerificadorTest` (JAR con solo `synkra-plugin.yaml`), con una imagen por versión: `synkra-core` 2.1.0 → `localhost:5001/synkra-core@sha256:` + 64 `a`; 3.0.0 → 64 `b`; `otro-core` 1.0.0 → `localhost:5001/otro-core@sha256:` + 64 `c`.
10. **SQLite de las pruebas, para leerlo de vuelta.** `VerificacionVivoTest` y `CrearPlanTest` usan `build/sqlite-pruebas/<NombreDeLaClase>`, que el `@DynamicPropertySource` borra y crea de nuevo; no se borran filas entre pruebas. `CrearPlanTest` fija `synkra.paises=pe,co,cl,ar,uy,bo` (no `mx`). Cada caso usa su propio país:
    - `pe`: país vacío en el simulador, S2 (`synkra-core` 2.1.0 con `kyc-pe` 1.3.0) → 201, todo en `anadidos`, `core.antes` nulo.
    - `co`: corre 2.1.0 (imagen `a`) con `kyc-co` 1.0.0 (sha del catálogo) y `kyc-viejo` 0.9.0 (sha 64 `9`); el plan pide `kyc-co` 1.0.0 y `kyc-pe` 1.3.0 → `anadidos` `kyc-pe`, `quitados` `kyc-viejo`, `iguales` `kyc-co`, `core.igual` verdadero.
    - `cl`: corre exactamente S2 → 201 con `sinCambios: true`.
    - `ar`: S4 → 422 `COMBINACION_RECHAZADA`, cero peticiones al simulador.
    - `uy`: solo el Deployment sembrado → 409 `PAIS_INCONSISTENTE`.
    - `bo`: `fallaLecturas("bo")` → 503 `KUBERNETES_NO_DISPONIBLE`.
    - `mx` con S4 → 422 `PAIS_NO_PERMITIDO`, cero peticiones al simulador.
    - `pe` con selección mal formada → 422 `SELECCION_INVALIDA`, sin fila nueva.
11. **PBT-03 (`CambiosPropiedades`, jqwik).** Generadores con nombres, versiones, hashes e imágenes de alfabetos pequeños (para que haya coincidencias), nombres únicos dentro de lo que corre y dentro de la selección, y `Optional.empty()` para un país vacío. La función `aplicar(corre, cambios)` vive **solo en la prueba**. Propiedades, cada una con al menos 1000 intentos:
    - aplicar: `(corre − quitados) ∪ anadidos = selección`, como conjuntos;
    - partición: `iguales = corre ∩ selección`, `anadidos ⊆ selección`, `quitados ⊆ corre`, y `anadidos`, `quitados` e `iguales` disjuntos dos a dos, sin repetidos y ordenados;
    - `sinCambios ⇔ (core.igual ∧ corre = selección)`, y `core.antes` es la imagen leída o `null`;
    - país vacío: `quitados` e `iguales` vacíos, `anadidos` = selección y `core.igual` falso.
    - Con `Statistics.label(...).coverage(...)`, al menos el 5 % de los intentos de la primera propiedad caen en cada una de estas clases: «mismo nombre, otra versión o hash», «país vacío» y «sin cambios».
12. **Bitácora en `bitacoras/U07-B.md`**, y el revisor en `revisiones/U07-B/`.

---

## Criterios de aceptación

- [ ] **CA-1**: U07-T03. Verificación en vivo (R-D2) con S4, sin filas en `dep_plan` y sin Kubernetes
  ```bash
  ./gradlew :backend:test --rerun --tests '*VerificacionVivoTest'
  grep -ho 'tests="[0-9]*" skipped="[0-9]*" failures="[0-9]*" errors="[0-9]*"' backend/build/test-results/test/TEST-*VerificacionVivoTest.xml
  python3 -I -c "import sqlite3,sys; c=sqlite3.connect(sys.argv[1]); print(c.execute('select count(*) from dep_plan').fetchone()); print(c.execute('select count(*) from cat_plugin').fetchone(), c.execute('select count(*) from cat_version_core').fetchone())" backend/build/sqlite-pruebas/VerificacionVivoTest/synkra.db
  ```
  Esperado:
  - `BUILD SUCCESSFUL`, `tests` ≥ 10 y `skipped="0" failures="0" errors="0"`. Las pruebas (por HTTP con `RANDOM_PORT`) afirman:
    - para S1 a S7 y S10, 200, y que `global`, los motivos (código, plugin y campo, en orden) y las alternativas son exactamente los de `caso.esperado()` y los de `Verificador.alternativas` sobre `SuiteCombinaciones.catalogo()` en memoria;
    - S4: `plugins[0].alternativas` es `[{tipo: PLUGIN, nombre: kyc-pe, version: "1.3.0"}, {tipo: CORE, nombre: synkra-core, version: "3.0.0"}]`;
    - S7 da `CORE_EQUIVOCADO` (decisión 4) y S6 da `PLUGIN_AUSENTE`;
    - la selección mal formada (`core` `Synkra`, `versionCore` `2.1`, plugin `nombre` `KYC`, `version` `x`, un plugin nulo) da 422 `SELECCION_INVALIDA` con todos los campos, y ningún motivo contiene lo recibido;
    - `no-existe` y `synkra-core` 9.9.9 dan 422 `CORE_NO_REGISTRADO` con campo `core` y `versionCore`;
    - `dep_plan` sigue con 0 filas tras todas las llamadas, y `verifyNoInteractions(adaptador)` pasa.
  - Python: `(0,)` y luego `(4,) (3,)`.

- [ ] **CA-2**: U07-T04. Crear plan con resumen y cambios (R-D3, R-D4) contra el servidor simulado, con lectura de vuelta
  ```bash
  ./gradlew :backend:test --rerun --tests '*CrearPlanTest'
  grep -ho 'tests="[0-9]*" skipped="[0-9]*" failures="[0-9]*" errors="[0-9]*"' backend/build/test-results/test/TEST-*CrearPlanTest.xml
  python3 -I -c "import sqlite3,sys; c=sqlite3.connect(sys.argv[1]); print(c.execute(\"select pais, tipo, estado, json_extract(resumen, '$.namespace'), json_array_length(cambios, '$.anadidos'), json_array_length(cambios, '$.quitados'), json_array_length(cambios, '$.iguales'), json_extract(cambios, '$.sinCambios'), json_extract(cambios, '$.core.igual'), vence_en - creado_en from dep_plan order by pais\").fetchall()); print(c.execute(\"select count(*) from dep_plan where pais in ('ar', 'uy', 'bo', 'mx')\").fetchone()); print(c.execute(\"select json_extract(cambios, '$.quitados[0].nombre'), json_extract(cambios, '$.anadidos[0].nombre'), json_extract(cambios, '$.iguales[0].nombre'), json_extract(resumen, '$.imagen') like '%@sha256:%' from dep_plan where pais = 'co'\").fetchall())" backend/build/sqlite-pruebas/CrearPlanTest/synkra.db
  ```
  Esperado:
  - `BUILD SUCCESSFUL`, `tests` ≥ 8 y `skipped="0" failures="0" errors="0"`, con los casos de la decisión 10. Las pruebas afirman además:
    - en `pe`: 201, `estado` `pendiente`, `tipo` `despliegue`, `creadoPor` `anonimo`, `venceEn − creadoEn` = 30 min, `resumen.namespace` `synkra-pe`, `resumen.imagen` igual a la imagen de 2.1.0 del catálogo y `resumen.plugins[0].sha256` igual al de `obtenerPlugin`;
    - en cada caso, `nuevasPeticiones()` solo trae `GET` (el clúster no cambia), y en `ar` y `mx` no trae nada;
    - en `ar`, `uy`, `bo`, `mx` y la selección mal formada, el número de filas de `dep_plan` no cambia.
  - Python:
    - `[('cl', 'despliegue', 'pendiente', 'synkra-cl', 0, 0, 1, 1, 1, 1800000), ('co', 'despliegue', 'pendiente', 'synkra-co', 1, 1, 1, 0, 1, 1800000), ('pe', 'despliegue', 'pendiente', 'synkra-pe', 1, 0, 0, 0, 0, 1800000)]`;
    - `(0,)`;
    - `[('kyc-viejo', 'kyc-pe', 'kyc-co', 1)]`.

- [ ] **CA-3**: U07-T05. Propiedad PBT-03 del cálculo de cambios
  ```bash
  ./gradlew :backend:test --rerun --tests '*CambiosPropiedades'
  grep -ho 'tests="[0-9]*" skipped="[0-9]*" failures="[0-9]*" errors="[0-9]*"' backend/build/test-results/test/TEST-*CambiosPropiedades.xml
  grep -o 'tries = [0-9]*' backend/build/test-results/test/TEST-*CambiosPropiedades.xml
  ```
  Esperado: `BUILD SUCCESSFUL`, `tests` ≥ 4 y sin saltos ni fallos; un `tries = N` por propiedad, todos con N ≥ 1000; las coberturas de la decisión 11 se cumplen (si no, la prueba falla). La bitácora pega las tablas de `Statistics` del reporte.

- [ ] **CA-4**: el JAR real, sin clúster: verificación por `curl`, y ningún plan sin poder leer lo que corre
  ```bash
  ./gradlew :backend:bootJar
  export SYNKRA_DATA=$(mktemp -d); J=$(mktemp -d)
  python3 -I -c "import sys, zipfile
  for ruta, v, r in ((sys.argv[1], '1.3.0', '2.x'), (sys.argv[2], '2.0.0', '3.x')):
      with zipfile.ZipFile(ruta, 'w') as z:
          z.writestr('synkra-plugin.yaml', 'nombre: kyc-pe\nversion: %s\ncore: synkra-core\ncoreCompatible: %s\n' % (v, r))" "$J/a.jar" "$J/b.jar"
  SYNKRA_PAISES=pe java -jar backend/build/libs/backend-*.jar --server.port=18090 > "$J/log.txt" 2>&1 & PID=$!
  timeout 90 bash -c 'until curl -sf localhost:18090/actuator/health >/dev/null; do sleep 1; done'
  for v in 2.1.0:a 3.0.0:b; do curl -s -o /dev/null -w '%{http_code}\n' -H 'Content-Type: application/json' \
    -d "{\"nombre\":\"synkra-core\",\"version\":\"${v%%:*}\",\"imagen\":\"localhost:5001/synkra-core@sha256:$(printf "${v##*:}%.0s" $(seq 64))\"}" localhost:18090/api/cores; done
  for f in a b; do curl -s -o /dev/null -w '%{http_code}\n' -F "archivo=@$J/$f.jar" localhost:18090/api/cores/synkra-core/plugins; done
  pide() { curl -s -o "$J/r.json" -w '%{http_code} ' -H 'Content-Type: application/json' -d "$2" "localhost:18090/api/$1"; }
  S4='"core":"synkra-core","versionCore":"2.1.0","plugins":[{"nombre":"kyc-pe","version":"2.0.0"}]'
  S2='"core":"synkra-core","versionCore":"2.1.0","plugins":[{"nombre":"kyc-pe","version":"1.3.0"}]'
  pide verificaciones "{$S4}"; python3 -I -c "import json,sys; v=json.load(open(sys.argv[1])); print(v['global'], v['plugins'][0]['alternativas'])" "$J/r.json"
  for cuerpo in "{\"pais\":\"pe\",$S4}" "{\"pais\":\"mx\",$S2}" "{\"pais\":\"pe\",$S2}"; do
    pide despliegues "$cuerpo"; python3 -I -c "import json,sys; print(json.load(open(sys.argv[1]))['codigo'])" "$J/r.json"; done
  kill $PID; wait $PID 2>/dev/null
  python3 -I -c "import sqlite3,sys; c=sqlite3.connect(sys.argv[1]); print(c.execute('select count(*) from dep_plan').fetchone())" "$SYNKRA_DATA/synkra.db"
  ```
  Esperado, en orden:
  - `201`, `201`, `201`, `201` (los dos cores y los dos plugins);
  - `200 RECHAZADA [{'tipo': 'PLUGIN', 'nombre': 'kyc-pe', 'version': '1.3.0'}, {'tipo': 'CORE', 'nombre': 'synkra-core', 'version': '3.0.0'}]`;
  - `422 COMBINACION_RECHAZADA`;
  - `422 PAIS_NO_PERMITIDO`;
  - `503 KUBERNETES_NO_DISPONIBLE` (el JAR no tiene cuenta de servicio: no se crea un plan sin saber qué corre);
  - `(0,)`.

  Si el puerto 18090 está ocupado, se anota en la bitácora y se usa otro en todo el criterio. La bitácora pega la salida tal cual.

- [ ] **CA-5**: rojo primero. Cada regla nueva tiene una prueba que muerde, demostrado en una copia
  ```bash
  D=$(mktemp -d); git archive HEAD | tar -x -C "$D"
  # Mutación anotada literalmente en la bitácora; una por regla, por ejemplo:
  #   R-D2 alternativas:  VerificacionVivo devuelve el veredicto de verificar, sin pasar por alternativas
  #   otra carpeta:       quitar la búsqueda en los demás cores (S7 da PLUGIN_AUSENTE)
  #   R-D3 rechazo:       no cortar con RECHAZADA (el plan de ar se crea)
  #   R-D1 primero:       llamar a PaisPermitido después de verificar (mx con S4 da COMBINACION_RECHAZADA)
  #   clúster después:    llamar a leerPais antes de verificar (ar trae peticiones al simulador)
  #   R-D4 terna:         comparar los plugins solo por nombre en CalculoCambios
  sed -i '<mutación>' "$D/backend/src/main/java/io/synkra/despliegue/<Clase>.java"
  (cd "$D" && ./gradlew :backend:test --tests '*VerificacionVivoTest' --tests '*CrearPlanTest' --tests '*CambiosPropiedades')
  grep -l '<failure' "$D"/backend/build/test-results/test/TEST-*.xml
  rm -rf "$D"
  ```
  Esperado: con cada una de las seis mutaciones (una copia por mutación), `BUILD FAILED`, y el XML de la clase dueña de la regla muestra al menos un `<failure` en la prueba de esa regla (`VerificacionVivoTest` para las dos primeras, `CrearPlanTest` para las tres siguientes y `CambiosPropiedades` para la última). La bitácora pega el `sed` (o el parche) y el nombre de la prueba que cayó. Sin `git stash` ni cambios en el worktree. Además, la bitácora registra el rojo de cada clase de prueba antes de su código.

- [ ] **CA-6**: todo el backend sigue pasando, y `despliegue` solo usa las API públicas
  ```bash
  ./gradlew :backend:test --rerun :backend:check
  for f in backend/build/test-results/test/TEST-*.xml; do grep -Ho 'tests="[0-9]*" skipped="[0-9]*" failures="[0-9]*" errors="[0-9]*"' "$f"; done
  grep -rn "io.synkra.registro" backend/src/main/java/io/synkra/despliegue; echo "registro=$?"
  grep -rnE "io\.synkra\.(catalogo|kubernetes)\." backend/src/main/java/io/synkra/despliegue | grep -vE "io\.synkra\.(catalogo|kubernetes)\.api\."; echo "internos=$?"
  grep -rn "io.fabric8" backend/src/main/java/io/synkra/despliegue; echo "fabric8=$?"
  grep -rnE "\.(aplicar|construir|estado|contextoFallo)\(" backend/src/main/java/io/synkra/despliegue; echo "escrituras=$?"
  ```
  Esperado: `BUILD SUCCESSFUL` y ningún XML con `skipped`, `failures` o `errors` distinto de 0 (`FronterasModulosTest` y las pruebas de U07-A, `PaisPermitidoTest` incluida, pasan sin cambios). `registro=1`, `internos=1`, `fabric8=1` y `escrituras=1`. El `grep` «ajenos» de CA-2 de U07-A ya no aplica (ahora `despliegue` usa `catalogo.api` y `kubernetes.api` a propósito); se anota en la bitácora.

- [ ] **CA-7**: el diff queda acotado
  ```bash
  git rev-parse --short HEAD; git diff --stat <punta VERDE de U07-A>...HEAD
  git diff --name-status <punta VERDE de U07-A>...HEAD -- backend/src/test/java/io/synkra/kubernetes
  ```
  Esperado: solo estas rutas. Se anota el sha.
  - `backend/src/main/java/io/synkra/despliegue/**`
  - `backend/src/test/java/io/synkra/despliegue/**` (archivos nuevos: `VerificacionVivoTest.java`, `CrearPlanTest.java` y `CambiosPropiedades.java`; las pruebas de U07-A no cambian)
  - `backend/src/test/java/io/synkra/kubernetes/SimuladorKubernetes.java`: la segunda orden da exactamente `A	backend/src/test/java/io/synkra/kubernetes/SimuladorKubernetes.java`
  - `bitacoras/U07-B.md` y `revisiones/U07-B/`

---

**Rojo primero:** `VerificacionVivoTest` antes de `VerificacionVivo` y `RutasVerificacion`; `CambiosPropiedades` antes de `CalculoCambios`; `CrearPlanTest` antes de `PlanServicio` y `RutasPlanes`. Cada rojo queda en la bitácora, y CA-5 demuestra que cada regla muerde con el código terminado.
