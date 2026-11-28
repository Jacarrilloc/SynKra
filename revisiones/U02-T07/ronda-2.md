# Ronda 2 — U02-T07

VEREDICTO: VERDE

Corrí los cuatro criterios yo mismo y los cuatro pasan. Los seis hallazgos de la ronda 1 están cerrados: los comprobé con el proceso real y con mis propios mutantes. Quedan dos AMARILLO, que no bloquean.

## Comprobaciones previas
- Hash de la tarea `/home/casdav/Projects/Master/wt-orq-U04/tareas/U02-T07-endpoints-core.md`: `0def91ff8811363a2cd72c4c139ecb76558ecbbd`. Coincide.
- Worktree `/home/casdav/Projects/Master/wt-m6r-U02-T07`: rama `m6r/U02-T07`, HEAD `e69ef05`. Coincide.
- Base: cb2096a, la del despacho.
- Commits de la ronda 2 sobre `3514245`: solo `e69ef05`. Toca `EndpointsCore.java`, `EndpointsCoreTest.java`, `bitacoras/` y `revisiones/U02-T07/ronda-2-respuesta.md`.
- Arbitraje del orquestador: las propiedades desconocidas dan 400. Está implementado (`FAIL_ON_UNKNOWN_PROPERTIES`) y lo fija el caso `"extra":1` en `cuerpoInvalido...`.
- No exigí C-91 ni C-92.
- Al terminar:
  - `git status --short` del worktree está vacío;
  - no queda ningún proceso `core-2.1.0.jar`;
  - el puerto 18081 está libre.
- Sobre la evidencia del codificador: la de `ca2-r2.txt` trae `JAVA_TOOL_OPTIONS` de otro entorno (proxy, `/tmp/tmp.*`). Es coherente con el código de `e69ef05`, y no la uso como prueba: todo lo que sigue sale de mis propias ejecuciones.

## Criterios de aceptación, verificados por mí
| # | Criterio | Comando que corrí | Resultado |
|---|---|---|---|
| 1 | EndpointsCoreTest ≥7, sin saltos | `./gradlew :core:test --rerun --tests '*EndpointsCoreTest'` + grep del XML | pasa: BUILD SUCCESSFUL, `tests="12" skipped="0" failures="0" errors="0"`; los casos HTTP tardan de 0.17 a 2.6 s; las dos pruebas de `invocar()` tardan 1 a 4 ms (son en memoria y no se saltan) |
| 2 | Proceso real | `:core:bootJar`; `java -jar core/build/libs/core-2.1.0.jar --server.port=18081 ...` con plugins propios; espera con `timeout 90` + `until curl -sf .../readiness`; `curl /plugins`; `kill` y espera con condición | pasa (detalle abajo) |
| 3 | Build completo | `./gradlew :verificador:test --rerun :verificador:check :plugin-api:test --rerun :plugin-api:check :core:test --rerun :core:check` | pasa: BUILD SUCCESSFUL. Conteos, todos con skipped=0, failures=0, errors=0: core 10/10/10/12/7/4; verificador 13 clases (15,5,14,3,29,2,36,17,17,9,15,16); plugin-api 4 |
| 4 | Diff acotado | `git diff --name-only cb2096a...HEAD` filtrado fuera de `core/`, `bitacoras/U02-T07*` y `revisiones/U02-T07/` | pasa: no queda nada fuera; sha e69ef05 |

Detalle de CA-2. Uso el puerto 18081 que pidió el orquestador. El armado completo está en mi scratchpad:
- **Plugins:**
  - activos: kyc-a (con KYC; registra si el cargador del hilo es el del plugin), kyc-b (sin `PuntoKyc`), kyc-x (lanza), kyc-n (devuelve `null`) y kyc-l (lanza `NoClassDefFoundError`);
  - presente y no activo: kyc-c.
- **`GET /plugins`:** devuelve `[{"nombre":"kyc-a","version":"1.0.0"},{"nombre":"kyc-b","version":"2.0.0"},{"nombre":"kyc-x",...},{"nombre":"kyc-n",...},{"nombre":"kyc-l",...}] [200]`. Son los activos, en el orden de carga, y kyc-c no aparece.
- **Respuestas que comprobé:**
  - kyc-a con una solicitud válida: `{"aprobado":true,"motivo":"motivo-a tccl=true"} [200]`;
  - kyc-b: 404 `PLUGIN_SIN_KYC`, el mensaje nombra kyc-b;
  - kyc-c y `KYC-A`: 404 `PLUGIN_NO_ACTIVO`, el mensaje nombra el plugin pedido;
  - kyc-x, kyc-n y kyc-l: 500 `PLUGIN_FALLO`, el mensaje nombra el plugin y no aparece ni `secreto`, ni `boom-enlace`, ni ninguna traza.

## Sondas de los hallazgos de la ronda 1, repetidas contra el proceso real
- **F-01 (cerrado).** Todos estos cuerpos dan 400 `SOLICITUD_INVALIDA`:
  - clave duplicada en el nivel superior y también dentro de `datos` (`{"k":"a","k":"b"}`);
  - basura al final;
  - dos objetos seguidos;
  - `"extra":1`;
  - `datos:{"k":null}`;
  - `datos:{"k":{...}}`;
  - `documento:5` y `datos:{"k":5}`;
  - el cuerpo vacío.

  Un espacio al final sigue dando 200, que es lo correcto.
- **F-02 (cerrado).** kyc-n, que devuelve `null`, da 500 `PLUGIN_FALLO` con el nombre. Lo fija la prueba `pluginQueDevuelveNullDa500...`.
- **F-03 (cerrado).** kyc-l da 500 `PLUGIN_FALLO`. Lo fija la prueba `pluginQueLanzaUnErrorDeEnlace...`.
- **F-04 (cerrado).** Los 404 comprueban que el mensaje lleva el nombre, y `/kyc/KYC-A` da 404.
- **F-05 (cerrado).** El prefijo del log es ahora la constante `PREFIJO_FALLO_KYC`.
- **F-06 (cerrado).** `ca2-r2.txt` pega para cada plugin el fuente, `synkra-plugin.yaml`, `META-INF/services` y `jar tf`.

**Mis mutantes.** Los hice sobre copias de `git archive HEAD` en mi scratchpad. Los ocho mueren, todos por `AssertionFailedError` y ninguno por error de compilación:
- quitar `FAIL_ON_TRAILING_TOKENS`, `STRICT_DUPLICATE_DETECTION` o `FAIL_ON_UNKNOWN_PROPERTIES`: falla `cuerpoInvalido...`;
- convertir la rama del `null` en `if (false)`: falla `pluginQueDevuelveNull...`;
- capturar solo `Exception`: falla `pluginQueLanzaUnErrorDeEnlace...`;
- buscar con `equalsIgnoreCase`: falla `pluginNoActivo...`;
- no poner el cargador del hilo: fallan 3 pruebas;
- no restaurarlo en el `finally`: fallan las 2 pruebas de `invocar()`.

Otras comprobaciones:
- **P3:** ninguna línea añadida en `core/` nombra un país.
- **Esperas:** las pruebas no tienen `sleep`.
- **Literales nuevos:** solo hay uno, `": el plugin devolvio null"`, y va únicamente al log; no es un punto de decisión.

## Hallazgos
### F-01 · AMARILLO · core/src/test/java/io/synkra/core/EndpointsCoreTest.java:229 · El comentario dice lo contrario de lo que se prueba
El comentario dice «el nombre se busca exacto, sin distinguir nada de mayusculas». La prueba fija justo lo contrario: la búsqueda **sí** distingue mayúsculas, porque `KYC-A` da 404. Hay que corregir la redacción.

### F-02 · AMARILLO · core/src/main/java/io/synkra/core/EndpointsCore.java:61 · `POST /kyc` no exige `Content-Type: application/json`
El cuerpo se recibe como `byte[]`, así que se acepta con cualquier tipo de contenido. Con el proceso real, `-H 'Content-Type: text/plain'` y una solicitud válida da `{"aprobado":true,...} [200]`. La tarea dice «recibe una `SolicitudKyc` en JSON» pero no define qué hacer con otro tipo, así que no rompe ningún criterio. Se puede arreglar con `consumes = APPLICATION_JSON` o dejarlo como candidata junto con C-92 (el formato del 415).

## Tareas candidatas (defectos reales fuera de alcance)
- Formato del error cuando el `Content-Type` no es JSON (415 o 400 con `{codigo, mensaje}`), si se decide exigirlo. Encaja con C-92.
- Siguen en pie, ya registradas: C-91 (límite de tamaño del cuerpo), C-92 (formato de error por defecto de Spring) y C-86 (`detener()` al cerrar el contexto).

## Rutas de transcripciones largas
Todas en `/tmp/claude-1000/-home-casdav-Projects-Master-SynKra/5b13ba94-befc-454f-88e7-3182922701e5/scratchpad/rev-u02t07-r2/`:
- CA-1: `/tmp/claude-1000/-home-casdav-Projects-Master-SynKra/5b13ba94-befc-454f-88e7-3182922701e5/scratchpad/rev-u02t07-r2/ca1.txt`
- CA-3: `/tmp/claude-1000/-home-casdav-Projects-Master-SynKra/5b13ba94-befc-454f-88e7-3182922701e5/scratchpad/rev-u02t07-r2/ca3.txt`
- CA-2, armado (fuentes, yaml, services, JARs y config): `/tmp/claude-1000/-home-casdav-Projects-Master-SynKra/5b13ba94-befc-454f-88e7-3182922701e5/scratchpad/rev-u02t07-r2/ca2/`
- CA-2, log del proceso: `/tmp/claude-1000/-home-casdav-Projects-Master-SynKra/5b13ba94-befc-454f-88e7-3182922701e5/scratchpad/rev-u02t07-r2/ca2/log.txt`
- Mutantes, un archivo por mutante, de `/tmp/claude-1000/-home-casdav-Projects-Master-SynKra/5b13ba94-befc-454f-88e7-3182922701e5/scratchpad/rev-u02t07-r2/mut-trailing.txt` a `/tmp/claude-1000/-home-casdav-Projects-Master-SynKra/5b13ba94-befc-454f-88e7-3182922701e5/scratchpad/rev-u02t07-r2/mut-sinfinally.txt`

```
VEREDICTO: VERDE
AMARILLO|core/src/test/java/io/synkra/core/EndpointsCoreTest.java:229|Comentario contradice la prueba (la búsqueda sí distingue mayúsculas)
AMARILLO|core/src/main/java/io/synkra/core/EndpointsCore.java:61|POST /kyc acepta cualquier Content-Type (text/plain da 200)
INFORME: revisiones/U02-T07/ronda-2.md
```
