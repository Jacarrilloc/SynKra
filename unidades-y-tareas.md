# SynKra: Unidades y tareas

> **Entrada del loop de agentes (módulos 6 a 8).** Consolida las unidades de
> [`unit-of-work.md`](aidlc-docs/inception/application-design/unit-of-work.md) y las tareas de cada
> `*-code-generation-plan.md` de [`aidlc-docs/construction/plans/`](aidlc-docs/construction/plans/).
> Las tareas salen de la **Parte 1 de Code Generation** del marco AI-DLC, aprobadas una por una en
> [`audit.md`](aidlc-docs/audit.md). **No hay código todavía:** ninguna tarea se ejecuta sin
> aprobación y sin avisar antes al responsable del proyecto.

**Totales:** 11 unidades (9 Must, 2 Should), 114 tareas y 34 historias. Cada historia tiene al
menos una tarea, y cada tarea tiene un criterio que se comprueba con un comando (AUTONOMIA-02).

## Reglas para el orquestador

1. **Una tarea por codificador.** El revisor acepta una tarea solo si su comando de criterio
   termina como dice la tabla.
2. **Orden.** Una unidad se despacha cuando todas las de su "Depende de" están terminadas. Dentro
   de una unidad, las tareas van en orden, salvo las que la tabla marca *(Should)*: esas van al
   final y se pueden cortar.
3. **Límite (AUTONOMIA-01).** Ninguna tarea aplica cambios a un clúster compartido. Las pruebas de
   U05 a U07 usan el servidor simulado de Fabric8. U08 y U11 corren solo en el `kind` local
   `kind-synkra`, y `make install` se niega a correr con otro contexto. Un despliegue real de
   producto solo ocurre cuando una persona confirma un plan.
4. **Ruta de comandos.** Todo comando corre desde la raíz del repositorio. `$API` es
   `http://localhost:8080/api` en local, o `https://synkra.local/api` después de `make install`.

## Orden de ejecución

Del grafo de
[`unit-of-work-dependency.md`](aidlc-docs/inception/application-design/unit-of-work-dependency.md),
verificado acíclico con `python3 -I scripts/verificar-grafo.py
aidlc-docs/inception/application-design/unit-of-work-dependency.md` (11 nodos, 21 dependencias,
`Sin ciclos: OK`):

| Ola | Unidades | En paralelo | Resultado al terminar la ola |
|---|---|---|---|
| 1 | U01, U03 | 2 | La regla de compatibilidad y la aplicación vacía |
| 2 | U02, U04, U05 | 3 | El core no arranca con un plugin incompatible; el catálogo responde a `curl`; existen los objetos del país |
| 3 | U06, luego U07 | 1 | El despliegue se confirma, se vigila y se registra (contra el servidor simulado) |
| 4 | U08, luego U09 | 1 | **Demo de punta a punta en `kind`**: primero con `curl` y después desde el front |
| 5 | U10, U11 | 2 | Should: roles, políticas, Trivy y GitOps |

**Camino crítico:** U03 → U05 → U06 → U07 → U08 → U09.

**Si falta tiempo (R9):** se corta U11, luego U10. Si U09 no llega, la demo se hace con
`./scripts/e2e-api.sh` (R8). U01 a U08 son el mínimo que demuestra la North Star.

---

## U01: Verificador y suite

- **Responsabilidad:** la regla de compatibilidad (rangos semver, veredicto por plugin y global, core equivocado, alternativas) y la lectura y validación de `synkra-plugin.yaml`. Publica en `testFixtures` la suite del PRD §11.1 para que U02 y U07 la reutilicen.
- **Qué no hace:** no lee el catálogo (recibe una interfaz `Catalogo` de solo lectura); no conoce Spring, Kubernetes, la red ni los países.
- **Depende de:** ninguna
- **Historias que implementa:** H-07, H-08, H-10
- **Terminada cuando:** `./gradlew :verificador:check` pasa con la suite completa, y `./gradlew :verificador:dependencies --configuration runtimeClasspath` no lista Spring ni Fabric8.
- **Plan de origen:** [`U01-verificador-code-generation-plan.md`](aidlc-docs/construction/plans/U01-verificador-code-generation-plan.md)

| # | Tarea | Criterio de aceptación (comando) | Historia |
|---|---|---|---|
| U01-T01 | Raíz de Gradle | `./gradlew projects` lista `:verificador` y `./gradlew --version` muestra JVM 21. | H-07 |
| U01-T02 | Proyecto `verificador/` sin dependencias de framework | `./gradlew :verificador:dependencies --configuration runtimeClasspath \| grep -ciE "spring\|fabric8"` devuelve `0`. | H-07 |
| U01-T03 | `Version` (R-V1) y sus pruebas | `./gradlew :verificador:test --tests '*VersionTest'` pasa, con casos `2.1.0`, `2.01.0`, `2.1.0-rc1`, `2.1`. | H-07 |
| U01-T04 | `RangoVersion` (R-V2) y sus pruebas | `./gradlew :verificador:test --tests '*RangoVersionTest'` pasa con la tabla de R-V2 y los rangos inválidos (`>=3.0.0 <2.0.0`, `2.x.x`). | H-07 |
| U01-T05 | Propiedades de rangos (PBT-03, PBT-07, PBT-08) | `./gradlew :verificador:test --tests '*RangoVersionPropiedades'` pasa con 1000 intentos por propiedad. | H-07 |
| U01-T06 | `Descriptor` y `LectorDescriptor` (R-V3) | `./gradlew :verificador:test --tests '*LectorDescriptorTest'` pasa con: JAR sin archivo, campo faltante, campo desconocido, YAML con etiqueta de tipo (rechazado) y descriptor válido. | H-03 |
| U01-T07 | Propiedad de ida y vuelta del descriptor (PBT-02) | `./gradlew :verificador:test --tests '*DescriptorPropiedades'` pasa con 1000 intentos. | H-03 |
| U01-T08 | Modelo del veredicto y códigos de motivo | `./gradlew :verificador:test --tests '*CodigoMotivoTest'` pasa y afirma que la enumeración tiene exactamente los 9 códigos de `domain-entities.md`. | H-07 |
| U01-T09 | `Verificador.verificar` (R-V4) y la interfaz `Catalogo` | `./gradlew :verificador:test --tests '*VerificadorTest'` pasa con un caso por código (`PLUGIN_REPETIDO`, `PLUGIN_AUSENTE`, `CORE_EQUIVOCADO`, `VERSION_INCOMPATIBLE`) y la selección vacía aprobada. | H-07 |
| U01-T10 | `Verificador.alternativas` (R-V5) | `./gradlew :verificador:test --tests '*AlternativasTest'` pasa con S4 y S5 de la suite. | H-08 |
| U01-T11 | Propiedades del verificador (PBT-03) | `./gradlew :verificador:test --tests '*VerificadorPropiedades'` pasa con 1000 intentos. | H-08 |
| U01-T12 | Suite de combinaciones en `testFixtures` (S1 a S12) | `./gradlew :verificador:test --tests '*SuiteCombinacionesTest'` pasa los 10 casos ejecutables. | H-10 |
| U01-T13 | La suite bloquea el build | `./gradlew :verificador:check` = 0; con el caso S4 alterado en una rama de prueba, `./gradlew :verificador:check` ≠ 0. | H-10 |
| U01-T14 | Resumen de la unidad | el resumen existe y contiene la salida pegada de `./gradlew :verificador:check`. | H-10 |

_14 tareas._

---

## U02: Core, contrato y plugins de país

- **Responsabilidad:** el contrato `SynkraPlugin` y `PuntoKyc`; el core que al arrancar verifica con U01 los plugins de `/plugins` contra su versión compilada, carga cada uno en su propio cargador y no queda listo si algo falla; `kyc-co 1.0.0`, `kyc-pe 1.3.0` (rango `2.x`) y `kyc-pe 2.0.0` (rango `3.x`); el *fixture* de un plugin que falla al iniciar.
- **Qué no hace:** no descarga plugins (U05); no habla con el backend; no tiene lógica de país.
- **Depende de:** U01
- **Historias que implementa:** H-21, H-22, H-23
- **Terminada cuando:** `./gradlew :core:check :plugins:kyc-co:check :plugins:kyc-pe:check` pasa (incluida la suite de U01 aplicada al arranque) y `grep -rniE "colombia|peru" core/src` no devuelve nada.
- **Plan de origen:** [`U02-core-plugins-code-generation-plan.md`](aidlc-docs/construction/plans/U02-core-plugins-code-generation-plan.md)

| # | Tarea | Criterio de aceptación (comando) | Historia |
|---|---|---|---|
| U02-T01 | `plugin-api/` (contrato) | `./gradlew :plugin-api:build` pasa y `./gradlew :plugin-api:dependencies --configuration runtimeClasspath` no lista dependencias. | H-21 |
| U02-T02 | Proyecto `core/` (Spring Boot) con la versión compilada | `./gradlew :core:bootJar && unzip -p core/build/libs/core-*.jar BOOT-INF/classes/synkra-core.properties` muestra `version=2.1.0`. | H-21 |
| U02-T03 | `CargadorPlugins`: un `URLClassLoader` por plugin | `./gradlew :core:test --tests '*CargadorPluginsTest'` pasa y afirma que dos plugins tienen cargadores distintos. | H-22 |
| U02-T04 | `ArranquePlugins`: leer, verificar con U01 y activar | `./gradlew :core:test --tests '*ArranquePluginsTest'` pasa con: válido (S1), incompatible `3.x` (S4), activo ausente (S6), presente y no activo (S12). | H-21 |
| U02-T05 | La suite de U01 aplicada al arranque | `./gradlew :core:test --tests '*ConcordanciaSuiteTest'` pasa todos los casos ejecutables. | H-21, H-10 |
| U02-T06 | Fallo al iniciar un plugin y readiness | `./gradlew :core:test --tests '*FalloInicioPluginTest'` pasa: `/actuator/health/readiness` responde `503` y el log contiene el nombre del plugin. | H-22 |
| U02-T07 | Endpoints del core | `./gradlew :core:test --tests '*EndpointsCoreTest'` pasa; en local, `curl localhost:8080/plugins` lista solo los activos. | H-21 |
| U02-T08 | El core no conoce países (P3) | `grep -rniE "colombia\|peru\|\"co\"\|\"pe\"" core/src/main` no devuelve nada. | H-21 |
| U02-T09 | Plugin `kyc-co 1.0.0` | `./gradlew :plugins:kyc-co:test` pasa y `unzip -p plugins/kyc-co/build/libs/kyc-co-1.0.0.jar synkra-plugin.yaml` muestra `pais: co` y `2.x`. | H-23 |
| U02-T10 | Plugin `kyc-pe 1.3.0` y `kyc-pe 2.0.0` | `./gradlew :plugins:kyc-pe:test` pasa y los dos JAR muestran `2.x` y `3.x` en su descriptor. | H-23 |
| U02-T11 | Imagen del core multi-stage y no root | `docker build -t synkra-core:2.1.0 core/` y `docker inspect -f '{{.Config.User}}' synkra-core:2.1.0` no vacío ni `root`/`0`. | H-28 |
| U02-T12 | Prueba de humo del contenedor | `core/smoke.sh` termina con código 0 (comprueba `curl /plugins` en el primer caso y código de salida ≠ 0 en el segundo). | H-21 |
| U02-T13 | Resumen de la unidad | `aidlc-docs/construction/U02-core-plugins/code/summary.md` contiene la salida real de `./gradlew :core:check` y de `core/smoke.sh`. | H-21 |

_13 tareas._

---

## U03: Plataforma del backend

- **Responsabilidad:** la aplicación Spring Boot vacía: configuración de SQLite en `/data` con Flyway, formato de error único, salud, la **prueba de fronteras de Spring Modulith** (D14) y el Dockerfile multi-stage no root del backend. OpenAPI como tarea Should final.
- **Qué no hace:** ninguna funcionalidad de negocio; no tiene tablas propias.
- **Depende de:** ninguna
- **Historias que implementa:** H-29 (Should, tarea final)
- **Terminada cuando:** `./gradlew :backend:check` pasa (incluida `ApplicationModules.verify()`) y `curl localhost:8080/actuator/health` responde `UP` con la base creada en `/data/synkra.db`.
- **Plan de origen:** [`U03-plataforma-backend-code-generation-plan.md`](aidlc-docs/construction/plans/U03-plataforma-backend-code-generation-plan.md)

| # | Tarea | Criterio de aceptación (comando) | Historia |
|---|---|---|---|
| U03-T01 | Proyecto `backend/` (Spring Boot 3, Java 21) | `./gradlew :backend:bootJar` pasa. | H-24 |
| U03-T02 | SQLite en `/data` con Flyway | `./gradlew :backend:test --tests '*BaseDeDatosTest'` pasa y, tras arrancar en local, `sqlite3 $SYNKRA_DATA/synkra.db 'select version from flyway_schema_history'` devuelve `1`. | H-18 |
| U03-T03 | Formato de error único | `./gradlew :backend:test --tests '*ErrorApiTest'` pasa con un controlador de prueba que produce cada código. | H-24 |
| U03-T04 | Salud | `curl -s localhost:8080/actuator/health \| jq -r .status` = `UP` con la aplicación local. | H-24 |
| U03-T05 | Prueba de fronteras del monolito modular (D14) | `./gradlew :backend:test --tests '*FronterasModulosTest'` pasa; con una clase de prueba que importa un paquete interno de otro módulo, falla. | H-24 |
| U03-T06 | Imagen del backend multi-stage y no root | `docker build -t synkra-backend backend/` y `docker inspect -f '{{.Config.User}}' synkra-backend` no vacío ni `root`/`0`. | H-28 |
| U03-T07 | Resumen de la unidad | `aidlc-docs/construction/U03-plataforma-backend/code/summary.md` contiene la salida real de `./gradlew :backend:check`. | H-24 |
| U03-T08 | *(Should)* OpenAPI | `curl -s localhost:8080/api/v3/api-docs \| jq -r .openapi` empieza por `3.`. | H-29 |

_8 tareas._

---

## U04: Catálogo

- **Responsabilidad:** registrar cores y versiones; subir plugins (lectura del descriptor con U01, SHA-256, path traversal, límite de tamaño); carpeta por core; filtrar por etiqueta; servir artefactos por hash; sus tablas y sus rutas REST. Should final: registro de imágenes permitido.
- **Qué no hace:** no verifica combinaciones para desplegar (U07); no toca el clúster.
- **Depende de:** U01, U03
- **Historias que implementa:** H-01 a H-06, H-31 (Should)
- **Terminada cuando:** `./gradlew :backend:test --tests '*catalogo*'` pasa y los criterios de H-01 a H-06 se cumplen con `curl` contra el backend local.
- **Plan de origen:** [`U04-catalogo-code-generation-plan.md`](aidlc-docs/construction/plans/U04-catalogo-code-generation-plan.md)

| # | Tarea | Criterio de aceptación (comando) | Historia |
|---|---|---|---|
| U04-T01 | Migración y repositorios | `./gradlew :backend:test --tests '*CatalogoRepositoriosTest'` pasa y `sqlite3 $SYNKRA_DATA/synkra.db '.tables'` lista las cuatro `cat_*`. | H-01 |
| U04-T02 | Registrar un core (R-C1, R-C2) | `./gradlew :backend:test --tests '*RegistrarCoreTest'` pasa con: primera versión (201 y carpeta vacía), segunda versión (mismo core), imagen sin digest (422), versión repetida (409). | H-01 |
| U04-T03 | Rutas de cores | con el backend local, `curl -s -o /dev/null -w '%{http_code}' -X POST localhost:8080/api/cores -d @core.json -H 'Content-Type: application/json'` = `201` y `curl -s localhost:8080/api/cores/synkra-core/plugins` = `[]`. | H-01 |
| U04-T04 | Recepción segura del JAR (R-C5, R-C6, R-C11) | `./gradlew :backend:test --tests '*RecepcionArtefactoTest'` pasa con: JAR de 25 MB (413, nada en disco), entrada `../../etc/x` (422 `RUTA_NO_PERMITIDA`, nada en disco), JAR válido (hash igual a `sha256sum`). | H-04, H-02 |
| U04-T05 | Propiedad de rutas seguras (PBT-03) | `./gradlew :backend:test --tests '*RutasSegurasPropiedades'` pasa: ninguna entrada aceptada resuelve fuera del directorio de destino. | H-04 |
| U04-T06 | Subir un plugin (R-C4, R-C7 a R-C10) | `./gradlew :backend:test --tests '*SubirPluginTest'` pasa con: válido (201 con descriptor y sha256), sin descriptor, campo desconocido, `core` de otra carpeta (422 `CORE_EQUIVOCADO`), versión repetida (409), etiqueta en conflicto (422). | H-02, H-03 |
| U04-T07 | Ruta de subida | `curl -F archivo=@plugins/kyc-pe/build/libs/kyc-pe-1.3.0.jar localhost:8080/api/cores/synkra-core/plugins \| jq -r .sha256` = `sha256sum` del mismo archivo. | H-02 |
| U04-T08 | Filtrar por etiqueta y aislamiento de carpetas (R-C12) | `./gradlew :backend:test --tests '*ListarPluginsTest'` pasa; `curl -s "localhost:8080/api/cores/synkra-core/plugins?pais=pe" \| jq length` = `1` y la carpeta de `otro-core` no muestra `kyc-pe`. | H-05, H-06 |
| U04-T09 | `Catalogo` de U01 implementado por el módulo | `./gradlew :backend:test --tests '*CatalogoVerificadorTest'` pasa los casos S1 a S7 de la suite de U01 usando datos guardados en SQLite. | H-06 |
| U04-T10 | Servir artefactos por hash (R-C13) | `curl -s localhost:8080/api/artefactos/<sha> \| sha256sum` coincide con `<sha>`; un hash inexistente da 404. | H-15 |
| U04-T11 | Fronteras del módulo | `./gradlew :backend:test --tests '*FronterasModulosTest'` sigue pasando con `catalogo` y solo `catalogo.api` es público. | H-01 |
| U04-T12 | Resumen | `aidlc-docs/construction/U04-catalogo/code/summary.md` contiene la salida real de `./gradlew :backend:test --tests '*catalogo*'` y de los `curl` de H-01 a H-06. | H-01 |
| U04-T13 | *(Should)* Registro de imágenes permitido (R-C3) | con `SYNKRA_REGISTRO_PERMITIDO=localhost:5001`, registrar `docker.io/x@sha256:…` da 422 (`./gradlew :backend:test --tests '*RegistroPermitidoTest'`). | H-31 |

_13 tareas._

---

## U05: Pod del país

- **Responsabilidad:** construir los objetos del país (Deployment con el init container, ConfigMap de activos y hashes, Service) y aplicarlos con Fabric8 en `synkra-<país>`; rechazar por código todo namespace fuera de `synkra-*`; leer estado, log del core, eventos y lo que corre en un país. La imagen del init container y su script de descarga con comprobación de hash y 5 reintentos.
- **Qué no hace:** no decide cuándo desplegar ni registra nada; no crea namespaces (S1); no verifica compatibilidad.
- **Depende de:** U03
- **Historias que implementa:** H-15
- **Terminada cuando:** `./gradlew :backend:test --tests '*kubernetes*'` pasa y `init-descarga/test.sh` pasa los casos de hash correcto, hash alterado y catálogo caído.
- **Plan de origen:** [`U05-pod-pais-code-generation-plan.md`](aidlc-docs/construction/plans/U05-pod-pais-code-generation-plan.md)

| # | Tarea | Criterio de aceptación (comando) | Historia |
|---|---|---|---|
| U05-T01 | Cliente Fabric8 configurado con la cuenta del pod | `./gradlew :backend:test --tests '*ClienteKubernetesTest'` pasa contra el servidor simulado. | H-12 |
| U05-T02 | Guardia de namespace (segunda capa, D13) | `./gradlew :backend:test --tests '*GuardiaNamespaceTest'` pasa con `default`, `kube-system` y `synkra` rechazados y **cero** peticiones registradas en el servidor simulado. | H-13 |
| U05-T03 | `construir`: objetos del país | `./gradlew :backend:test --tests '*ConstruirObjetosPaisTest'` pasa comparando con un YAML esperado en `src/test/resources`. | H-12, H-15 |
| U05-T04 | `aplicar` con server-side apply | `./gradlew :backend:test --tests '*AplicarTest'` pasa: tras `aplicar`, el servidor simulado tiene los tres objetos en `synkra-pe`, con el *field manager* `synkra`. | H-12 |
| U05-T05 | `estado` y `contextoFallo` | `./gradlew :backend:test --tests '*EstadoYContextoTest'` pasa con un pod listo, uno en `CrashLoopBackOff` y uno en `Init:Error`. | H-14, H-16 |
| U05-T06 | `leerPais` | `./gradlew :backend:test --tests '*LeerPaisTest'` pasa con un país desplegado y uno vacío. | H-19 |
| U05-T07 | Script `init-descarga/descargar.sh` | `init-descarga/test.sh` pasa los casos: hash correcto (archivo en `/plugins`), hash alterado (código ≠ 0, nada en `/plugins`), catálogo caído (5 intentos y código ≠ 0), nombre con `../` en la lista (rechazado). | H-15 |
| U05-T08 | Imagen del init container | `docker build -t synkra-init-descarga init-descarga/` y `docker inspect -f '{{.Config.User}}' synkra-init-descarga` no vacío ni `root`/`0`. | H-28 |
| U05-T09 | Fronteras del módulo | `./gradlew :backend:test --tests '*FronterasModulosTest'` pasa con `kubernetes`; `grep -rn "io.fabric8" backend/src/main/java --include=*.java \| grep -v "/kubernetes/"` no devuelve nada. | H-12 |
| U05-T10 | Resumen | `aidlc-docs/construction/U05-pod-pais/code/summary.md` contiene la salida real de `./gradlew :backend:test --tests '*kubernetes*'` y de `init-descarga/test.sh`. | H-15 |

_10 tareas._

---

## U06: Registro y vista general

- **Responsabilidad:** guardar cada despliegue confirmado con su resultado y contexto; historial por país; vista general leída del clúster con U05 y la marca de concordancia; sus tablas y rutas.
- **Qué no hace:** no crea despliegues ni reversiones (U07).
- **Depende de:** U03, U05
- **Historias que implementa:** H-18, H-19
- **Terminada cuando:** `./gradlew :backend:test --tests '*registro*'` pasa, incluida la prueba de que el registro sobrevive al reinicio de la aplicación con el mismo archivo SQLite.
- **Plan de origen:** [`U06-registro-code-generation-plan.md`](aidlc-docs/construction/plans/U06-registro-code-generation-plan.md)

| # | Tarea | Criterio de aceptación (comando) | Historia |
|---|---|---|---|
| U06-T01 | Migración y repositorios | `./gradlew :backend:test --tests '*RegistroRepositoriosTest'` pasa y `sqlite3 $SYNKRA_DATA/synkra.db '.tables'` lista las tres `reg_*`. | H-18 |
| U06-T02 | `registrar` y `actualizarResultado` con sus transiciones (R-R1 a R-R3) | `./gradlew :backend:test --tests '*TransicionesRegistroTest'` pasa: `aplicando→listo` y `aplicando→fallido` permitidas; `listo→fallido` y `fallido→listo` rechazadas; `fallido` sin contexto rechazado. | H-18 |
| U06-T03 | Contexto truncado (R-R4) | `./gradlew :backend:test --tests '*ContextoFalloTest'` pasa: log de 1000 líneas guarda 200; texto de 100 KB guarda 64 KB con `[truncado]`. | H-16 |
| U06-T04 | Historial por país y detalle (R-R5) | `./gradlew :backend:test --tests '*HistorialTest'` pasa y `curl -s localhost:8080/api/paises/pe/despliegues \| jq '.[0].confirmado_en'` es el más reciente. | H-18 |
| U06-T05 | Vista general con concordancia (R-R6, R-R7) | `./gradlew :backend:test --tests '*VistaGeneralTest'` pasa con: país igual al registro (`concuerda=true`), ConfigMap con otro hash (`false`), imagen con otro digest (`false`), país sin nada (`true`). | H-19 |
| U06-T06 | Sobrevive al reinicio (R-R8) | `./gradlew :backend:test --tests '*RegistroReinicioTest'` pasa: se registra, se cierra el contexto de Spring, se abre otro sobre el mismo archivo SQLite y el historial es igual. | H-18 |
| U06-T07 | Fronteras del módulo | `./gradlew :backend:test --tests '*FronterasModulosTest'` pasa con `registro`. | H-18 |
| U06-T08 | Resumen | `aidlc-docs/construction/U06-registro/code/summary.md` contiene la salida real de `./gradlew :backend:test --tests '*registro*'`. | H-18 |

_8 tareas._

---

## U07: Despliegue y reversión

- **Responsabilidad:** verificación en vivo; plan `pendiente` con resumen y cambios; validación de país (S4); confirmación que aplica con U05 y registra con U06; vigilancia asíncrona de 3 minutos sin reintentos; contexto del fallo; reversión como plan nuevo; sus rutas.
- **Qué no hace:** no reintenta ni revierte por su cuenta (P2); no habla con Kubernetes salvo a través de U05.
- **Depende de:** U01, U03, U04, U05, U06
- **Historias que implementa:** H-09, H-11, H-12, H-13, H-14, H-16, H-17, H-20
- **Terminada cuando:** `./gradlew :backend:test --tests '*despliegue*'` pasa, incluida la prueba de que un plan sin confirmar no llama nunca a U05.
- **Plan de origen:** [`U07-despliegue-code-generation-plan.md`](aidlc-docs/construction/plans/U07-despliegue-code-generation-plan.md)

| # | Tarea | Criterio de aceptación (comando) | Historia |
|---|---|---|---|
| U07-T01 | Migración y repositorio de planes | `./gradlew :backend:test --tests '*PlanRepositorioTest'` pasa. | H-11 |
| U07-T02 | País permitido (R-D1) | `./gradlew :backend:test --tests '*PaisPermitidoTest'` pasa con `co`, `pe` aceptados y `mx`, `default`, `kube-system`, `PE` rechazados (422) sin ninguna llamada a U05. | H-13 |
| U07-T03 | Verificación en vivo (R-D2) | `./gradlew :backend:test --tests '*VerificacionVivoTest'` pasa con S4 (alternativas presentes) y no crea filas en `dep_plan`. | H-09, H-08 |
| U07-T04 | Crear plan con resumen y cambios (R-D3, R-D4) | `./gradlew :backend:test --tests '*CrearPlanTest'` pasa: válido (201 `pendiente`, resumen con digest y hashes), inválido (422, sin fila), cambios contra lo que corre (añadidos, quitados, iguales), plan idéntico permitido con "sin cambios". | H-11 |
| U07-T05 | Propiedad del cálculo de cambios (PBT-03) | `./gradlew :backend:test --tests '*CambiosPropiedades'` pasa: aplicar los cambios a lo que corre da la selección. | H-11 |
| U07-T06 | Un plan sin confirmar nunca aplica (R-D13) y vence (Q6) | `./gradlew :backend:test --tests '*PlanPendienteTest'` pasa: tras crear planes, el servidor simulado registra **cero** escrituras; con el reloj avanzado 31 min, el plan queda `vencido` y confirmar da 410. | H-17 |
| U07-T07 | Confirmar (R-D5 a R-D9) | `./gradlew :backend:test --tests '*ConfirmarTest'` pasa con: confirmación correcta (objetos en el simulador, fila `aplicando`), segunda confirmación (409), catálogo cambiado entre medias (422), otro despliegue en curso (409), confirmado por otro Desplegador (se guardan los dos usuarios). | H-12 |
| U07-T08 | Vigilancia asíncrona sin reintentos (R-D10) | `./gradlew :backend:test --tests '*VigilanciaTest'` pasa: pod listo → `listo`; plazo vencido → `fallido`; `CrashLoopBackOff` → `fallido` con contexto; en ningún caso hay una segunda escritura en el simulador. | H-14, H-16 |
| U07-T09 | Retomar la vigilancia tras un reinicio (R-D12) | `./gradlew :backend:test --tests '*RetomarVigilanciaTest'` pasa: con una fila `aplicando` al arrancar, se vigila y no se reaplica (cero escrituras). | H-14 |
| U07-T10 | Reversión como plan nuevo (R-D11) | `./gradlew :backend:test --tests '*ReversionTest'` pasa: origen `listo` → plan `reversion` `pendiente`; origen `fallido` → 422; registro manipulado con un plugin incompatible → 422; tras confirmar, el registro apunta al origen. | H-20 |
| U07-T11 | Plugin de otra carpeta en un plan (P4) | `./gradlew :backend:test --tests '*CarpetaEquivocadaPlanTest'` pasa con `otro-core` + `kyc-pe` → 422. | H-06 |
| U07-T12 | Fronteras del módulo | `./gradlew :backend:test --tests '*FronterasModulosTest'` pasa con `despliegue`, que solo usa `catalogo.api`, `kubernetes.api` y `registro.api`. | H-12 |
| U07-T13 | Resumen | `aidlc-docs/construction/U07-despliegue/code/summary.md` contiene la salida real de `./gradlew :backend:test --tests '*despliegue*'`. | H-12 |

_13 tareas._

---

## U08: Instalación y límite

- **Responsabilidad:** registro local junto a `kind`; namespaces `synkra`, `synkra-co` y `synkra-pe`; `ServiceAccount` `synkra:deployer` con `Role` y `RoleBinding` solo en cada país; PVC, Deployment (`Recreate`) y Service del backend; `make install` y `make verificar-limite`; comprobación de imágenes no root; el script de punta a punta de CU1 a CU5 con `curl`.
- **Qué no hace:** no despliega productos (eso lo hace SynKra); el front lo añade U09.
- **Depende de:** U02, U07
- **Historias que implementa:** H-24, H-26, H-27, H-28
- **Terminada cuando:** en un `kind` limpio, `make install && make verificar-limite && ./scripts/e2e-api.sh` termina con código 0.
- **Plan de origen:** [`U08-instalacion-limite-code-generation-plan.md`](aidlc-docs/construction/plans/U08-instalacion-limite-code-generation-plan.md)

| # | Tarea | Criterio de aceptación (comando) | Historia |
|---|---|---|---|
| U08-T01 | Clúster `kind` y registro local | `make cluster` termina con 0 y `kubectl get nodes --context kind-synkra` lista 2 nodos `Ready`. | H-26 |
| U08-T02 | Guardia de contexto | `kubectl config use-context otro && make install` sale con código distinto de 0 y el mensaje `contexto no permitido`, sin aplicar nada. | H-26 |
| U08-T03 | Namespaces (supuesto S1) | `kubectl kustomize deploy/base \| grep -c 'kind: Namespace'` = 3. | H-26 |
| U08-T04 | RBAC de `synkra:deployer` | `kubectl kustomize deploy/base \| grep -cE 'kind: Cluster(Role\|RoleBinding)'` = 0. | H-27 |
| U08-T05 | PVC y Deployment del backend | `kubectl kustomize deploy/base \| yq 'select(.kind=="Deployment" and .metadata.name=="synkra-backend") \| .spec.strategy.type'` = `Recreate`. | H-26 |
| U08-T06 | Overlay `kind`: imágenes e Ingress | `kubectl kustomize deploy/overlays/kind \| kubeconform -strict -summary` termina sin errores. | H-26 |
| U08-T07 | Imágenes multi-stage sin root | `make imagenes && for i in synkra-backend synkra-core-ejemplo synkra-init-descarga; do docker inspect -f '{{.Config.User}}' localhost:5001/$i; done` no imprime vacío, `root` ni `0`. | H-28 |
| U08-T08 | `make install` | sobre un `kind` recién creado, `make install` termina con 0; `kubectl get pods -n synkra` muestra el backend `Running` y `kubectl get ns` lista `synkra-co` y `synkra-pe`. | H-26 |
| U08-T09 | `make verificar-limite` | `make verificar-limite` termina con 0 e imprime las seis respuestas esperadas; si se añade a mano un ClusterRoleBinding de prueba, termina con 1. | H-27, H-13 |
| U08-T10 | Script de punta a punta por la API | `./scripts/e2e-api.sh` termina con 0 después de `make install`; tras CU4, `kubectl get deploy -n synkra-co -o jsonpath='{..image}'` no cambió. | H-24 |
| U08-T11 | Formato único de error | `./scripts/e2e-api.sh` comprueba que cada respuesta 4xx que provoca tiene `codigo`, `mensaje` y `detalles` (`jq -e 'has("codigo") and has("mensaje") and has("detalles")'`). | H-24 |
| U08-T12 | Resumen | `aidlc-docs/construction/U08-instalacion-limite/code/summary.md` contiene la salida real de `make install`, `make verificar-limite` y `./scripts/e2e-api.sh`. | H-26 |

_12 tareas._

---

## U09: Front

- **Responsabilidad:** Blazor Server con las 4 pantallas del PRD §9.4, que solo llama a la API; imagen multi-stage no root; Deployment y Service en `synkra`.
- **Qué no hace:** ninguna regla de versión ni de compatibilidad (P5).
- **Depende de:** U04, U06, U07, U08
- **Historias que implementa:** H-25
- **Terminada cuando:** `dotnet test front.Tests` pasa, `grep -rniE "semver|compatib" front/` solo devuelve llamadas a la API, y la pantalla "Desplegar" completa CU2 en el clúster de la demo.
- **Plan de origen:** [`U09-front-code-generation-plan.md`](aidlc-docs/construction/plans/U09-front-code-generation-plan.md)

| # | Tarea | Criterio de aceptación (comando) | Historia |
|---|---|---|---|
| U09-T01 | Proyecto Blazor Server y cliente de la API | `dotnet test front.Tests --filter ApiSynkraTest` pasa contra un servidor HTTP simulado. | H-25 |
| U09-T02 | Pantalla "Vista general" | `dotnet test front.Tests --filter VistaGeneralTest` pasa (bUnit) con dos países simulados. | H-25 |
| U09-T03 | Pantalla "Catálogo" | `dotnet test front.Tests --filter CatalogoTest` pasa, incluido el caso de descriptor inválido con el motivo visible. | H-25 |
| U09-T04 | Pantalla "Desplegar" con verificación en vivo | `dotnet test front.Tests --filter DesplegarTest` pasa: con veredicto incompatible el botón está deshabilitado. | H-25 |
| U09-T05 | Pantalla "Detalle" | `dotnet test front.Tests --filter DetalleTest` pasa y la reversión llama a la API solo después de confirmar. | H-25 |
| U09-T06 | Sin reglas de versión en el front | `grep -rniE "semver\|compatib" front/ --include='*.cs' --include='*.razor'` solo encuentra llamadas a `ApiSynkra`. | H-25 |
| U09-T07 | Imagen multi-stage sin root | `docker build -t localhost:5001/synkra-front front/ && docker inspect -f '{{.Config.User}}' localhost:5001/synkra-front` no es vacío, `root` ni `0`. | H-28 |
| U09-T08 | Manifiesto del front | `kubectl kustomize deploy/overlays/kind \| kubeconform -strict -summary` sin errores y `make install` deja `synkra-front` `Running`. | H-26 |
| U09-T09 | Recorrido de la demo y resumen | `aidlc-docs/construction/U09-front/code/summary.md` registra el recorrido de CU2, CU4 y CU5 en `https://synkra.local` junto al log de la API, paso por paso, y la salida de `dotnet test`. | H-25 |

_9 tareas._

---

## U10: Seguridad y roles *(Should)*

- **Responsabilidad:** usuarios con rol en SQLite (BCrypt), HTTP Basic, reglas por ruta de los 4 roles; el endpoint de artefactos por hash queda sin credenciales (D10).
- **Qué no hace:** no cambia las reglas de negocio de los demás módulos.
- **Depende de:** U04, U06, U07
- **Historias que implementa:** H-30
- **Terminada cuando:** `./gradlew :backend:test --tests '*seguridad*'` pasa con la matriz de roles de H-30 (401, 403 y 2xx esperados por ruta).
- **Plan de origen:** [`U10-seguridad-code-generation-plan.md`](aidlc-docs/construction/plans/U10-seguridad-code-generation-plan.md)

| # | Tarea | Criterio de aceptación (comando) | Historia |
|---|---|---|---|
| U10-T01 | Tabla de usuarios y roles | `./gradlew :backend:test --tests '*UsuariosRepositorioTest'` pasa y ninguna contraseña queda en claro en `synkra.db`. | H-30 |
| U10-T02 | HTTP Basic y 401 | `./gradlew :backend:test --tests '*AutenticacionTest'` pasa; `curl -s -o /dev/null -w '%{http_code}' $API/despliegues` = `401`. | H-30 |
| U10-T03 | Matriz de roles del PRD §9.2 | `./gradlew :backend:test --tests '*MatrizRolesTest'` pasa con todas las celdas de la tabla. | H-30 |
| U10-T04 | Excepción del init container | `./gradlew :backend:test --tests '*ArtefactosSinCredencialesTest'` pasa; el resto de `/api` exige credenciales. | H-30, H-15 |
| U10-T05 | Gestión de usuarios (Operador) | `curl -u operador:… -X POST $API/usuarios` crea un Lector y `curl -u lector:… -X POST $API/usuarios` = `403`. | H-30 |
| U10-T06 | Registro con autor | `./gradlew :backend:test --tests '*RegistroAutorTest'` pasa y `curl $API/despliegues/<id>` incluye `autor`. | H-30, H-18 |
| U10-T07 | Resumen | `aidlc-docs/construction/U10-seguridad/code/summary.md` contiene la salida real de `./gradlew :backend:test --tests '*seguridad*'` y los `curl` de H-30. | H-30 |

_7 tareas._

---

## U11: Endurecimiento y entrega *(Should)*

- **Responsabilidad:** NetworkPolicy *deny-by-default* con las excepciones necesarias (init → catálogo, front → backend), Pod Security `restricted`, límites de recursos; `compose.yaml` con las 6 prácticas del módulo 3; escaneo con Trivy; instalación de SynKra con GitOps (módulo 8).
- **Qué no hace:** no cambia el código de las unidades Must.
- **Depende de:** U08
- **Historias que implementa:** H-32, H-33, H-34
- **Terminada cuando:** `make install` con las políticas sigue pasando `./scripts/e2e-api.sh`, y `trivy image --severity CRITICAL --exit-code 1` pasa en todas las imágenes.
- **Plan de origen:** [`U11-endurecimiento-code-generation-plan.md`](aidlc-docs/construction/plans/U11-endurecimiento-code-generation-plan.md)

| # | Tarea | Criterio de aceptación (comando) | Historia |
|---|---|---|---|
| U11-T01 | Pod Security `restricted` | `kubectl get ns -L pod-security.kubernetes.io/enforce` muestra `restricted` en los tres y `./scripts/e2e-api.sh` sigue terminando con 0. | H-32 |
| U11-T02 | NetworkPolicy *deny-by-default* | `kubectl get netpol -A` lista la política por defecto en los tres namespaces; `kubectl run prueba -n default --rm -i --image=curlimages/curl -- curl -m 5 http://synkra-backend.synkra.svc:8080` falla por *timeout*. | H-32 |
| U11-T03 | Límites de recursos por namespace | `kubectl get limitrange,resourcequota -n synkra-co` lista ambos objetos. | H-32 |
| U11-T04 | `compose.yaml` de desarrollo | `docker compose up -d && curl -s localhost:8080/actuator/health \| jq -r .status` = `UP`. | H-33 |
| U11-T05 | Escaneo con Trivy | `trivy image --severity CRITICAL --exit-code 1 <imagen>` termina con 0 en las cuatro. | H-33 |
| U11-T06 | Instalación de SynKra con GitOps | `flux get kustomizations` muestra `synkra` en `Ready=True` y el backend y el front quedan con la revisión del repositorio. | H-34 |
| U11-T07 | Resumen | `aidlc-docs/construction/U11-endurecimiento/code/summary.md` contiene la salida real de los comandos de T01 a T06. | H-32 |

_7 tareas._

