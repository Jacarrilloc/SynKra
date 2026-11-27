# Ronda 1 — U02-T07

VEREDICTO: NO-VERDE

Los cuatro criterios pasan con mis propias ejecuciones. Aun así el veredicto es NO-VERDE por dos NARANJA en `POST /kyc`: acepta cuerpos JSON ambiguos o con basura al final, y devuelve 200 sin cuerpo cuando el plugin devuelve `null`.

## Comprobaciones previas
- Hash de la tarea `/home/user/wt-orq-U04/tareas/U02-T07-endpoints-core.md`: `0def91ff8811363a2cd72c4c139ecb76558ecbbd`. Coincide.
- Worktree `/home/user/wt-m6r-U02-T07`: rama `m6r/U02-T07`, HEAD `95719db`. Coincide con lo esperado.
- Commits sobre cb2096a: 5dfcc1e (prueba en rojo), 518b9ca, f255684, 95719db. Este último solo toca `bitacoras/`.
- Base:
  - No existe ninguna rama local `modulo-6-rehecho`.
  - `origin/modulo-6-rehecho` (7e7281d) no comparte ancestro común con HEAD porque el repositorio es superficial (`is-shallow=true`).
  - La tarea está apilada sobre U02-T06 (cb2096a), así que usé cb2096a como base, igual que el despacho.
- Evidencia del codificador: es coherente con el código, porque entre f255684 y HEAD no cambia nada fuera de `bitacoras/`.
- Al terminar: `git status --short` del worktree está vacío y no queda ningún proceso `core-2.1.0.jar` (`pgrep` no devuelve nada).

## Criterios de aceptación, verificados por mí
| # | Criterio | Comando que corrí | Resultado |
|---|---|---|---|
| 1 | EndpointsCoreTest ≥7, sin saltos | `./gradlew :core:test --rerun --tests '*EndpointsCoreTest'` + grep XML | pasa: BUILD SUCCESSFUL, `tests="10" skipped="0" failures="0" errors="0"`; tiempos de 0.47 a 4.6 s por caso HTTP (no son saltos) |
| 2 | Proceso real | `:core:bootJar`; `java -jar core/build/libs/core-2.1.0.jar --server.port=18082 ...` con plugins propios en mi scratchpad; espera con `timeout 60` hasta que la readiness da 200; `curl /plugins`; `kill` y espera con condición | pasa (detalle debajo de la tabla) |
| 3 | Build completo | `./gradlew :verificador:test --rerun :verificador:check :plugin-api:test --rerun :plugin-api:check :core:test --rerun :core:check` | pasa (detalle debajo de la tabla) |
| 4 | Diff acotado | `git diff --name-only cb2096a...HEAD` filtrado fuera de `core/`, `bitacoras/U02-T07*` y `revisiones/U02-T07/` | pasa: no queda nada fuera; sha 95719db |

Detalle de CA-2:
- Plugins que armé: kyc-a, kyc-b y kyc-x activos; kyc-c presente y no activo; kyc-n activo, que devuelve `null`.
- `curl /plugins` lista solo los activos, en el orden de carga y con la versión del descriptor.
- También comprobé por proceso real cada respuesta:
  - 200 con el resultado del plugin, y `tccl=true` dentro del plugin;
  - 404 `PLUGIN_NO_ACTIVO` y 404 `PLUGIN_SIN_KYC`;
  - 400 `SOLICITUD_INVALIDA` con `documento:5` y con `datos:{"k":5}`;
  - 500 `PLUGIN_FALLO` que nombra kyc-x y no deja pasar ni "secreto" ni la traza.

Detalle de CA-3: BUILD SUCCESSFUL. Conteos con skipped=0, failures=0, errors=0:
- core: 10 / 10 / 10 / 10 / 7 / 4.
- verificador: 12 clases.
- plugin-api: 4.

**Mis mutantes**, en una copia hecha con `git archive HEAD` en el scratchpad:
- **Los atrapa la suite (8 de 11):**
  - m1: quitar la coerción estricta;
  - m2: no poner el cargador del hilo (3 fallos);
  - m3: código cambiado en el caso sin `PuntoKyc`;
  - m4: publicar la lista parcial cuando falla un plugin al iniciar;
  - m6: dejar pasar un cuerpo inválido como 200;
  - m7: responder 200 en vez de 500;
  - m10: quitar la restauración del cargador (fallan las dos pruebas de `invocar()`).
- **Sobreviven:**
  - m5: capturar solo `RuntimeException`;
  - m8: el mensaje del 404 sin el nombre;
  - m9: buscar el plugin con `equalsIgnoreCase`.

**Lo que el codificador pidió evaluar:**
1. **Tomcat restaura el cargador del hilo.** Lo confirmo con m10: sin el `finally`, ninguna prueba HTTP falla y solo fallan las dos de `invocar()`. Probar la restauración sobre `invocar()`, visible en el paquete, es el nivel correcto, y por HTTP se sigue probando el cargador dentro del plugin. Lo acepto como cumplimiento del último punto de CA-1.
2. **Coerción desactivada.** Es correcta y la prueba muerde (m1). Pero cierra solo una parte de la clase de entrada laxa: ver F-01.

## Hallazgos
### F-01 · NARANJA · core/src/main/java/io/synkra/core/EndpointsCore.java:64 · Se aceptan como `SolicitudKyc` válidos cuerpos ambiguos o que no son JSON válido
La decisión 2 dice que un cuerpo que no es una `SolicitudKyc` válida es 400. Se fijó la coerción, pero no `FAIL_ON_TRAILING_TOKENS` ni `STRICT_DUPLICATE_DETECTION`. Contra el proceso real:
- `{"documento":"NO","documento":"OK-dup","datos":{}}` → `{"aprobado":true,...} [200]`. Con una clave duplicada gana el último valor, así que se aprueba un KYC sobre un documento ambiguo.
- `{"documento":"OK","datos":{}} basura` → 200.
- `{"documento":"OK","datos":{}}{"x":1}` → 200.

Falla así: el cliente o un intermediario lee el primer `documento` y el plugin valida el segundo. Hay que activar ambas opciones en la copia del `ObjectMapper` y añadir esos tres cuerpos a `cuerpoInvalidoDa400...`. Las propiedades desconocidas (`"extra":1` → 200) son discutibles: hay que decidir y dejar una prueba que fije la decisión.

### F-02 · NARANJA · core/src/main/java/io/synkra/core/EndpointsCore.java:85 · Si el plugin devuelve `null`, la respuesta es 200 sin cuerpo
`ResponseEntity.ok(null)` responde así. Con el plugin kyc-n, que devuelve `null`, `POST /kyc/kyc-n` da ` [200]` con el cuerpo vacío. La decisión 2 promete «200 con el `ResultadoKyc` en JSON». Un plugin que rompe el contrato termina como éxito vacío que el cliente no puede interpretar. Debería ser 500 `PLUGIN_FALLO` con el nombre del plugin, y no hay ninguna prueba para este caso.

### F-03 · AMARILLO · EndpointsCore.java:86 · La rama `LinkageError` no tiene prueba
m5 (`catch (RuntimeException e)`) sobrevive. Un `NoClassDefFoundError` dentro del plugin saldría como el 500 por defecto de Spring, sin `codigo`. Falta un plugin de prueba que lance un `Error` de enlace.

### F-04 · AMARILLO · EndpointsCoreTest.java (`esperarError`) · Las pruebas de 404 no fijan el mensaje ni que el nombre se busque exacto
m8 (el mensaje del 404 sin el nombre) y m9 (`equalsIgnoreCase`) sobreviven. Bastaría con comprobar que el mensaje contiene el nombre y con pedir `/kyc/KYC-A`, que debe dar 404.

### F-05 · AMARILLO · EndpointsCore.java:87 · Prefijo de log como literal
`"Plugin fallo al validar KYC plugin="` está escrito inline, mientras que `IniciadorPlugins:25` usa la constante `PREFIJO_FALLO`. Es incoherente con el estilo del módulo. Es solo un log, no un punto de decisión.

### F-06 · AMARILLO · bitacoras/U02-T07-evidencia/ca2.txt · El armado de `$D` no está pegado entero
Muestran `javac` y `jar cf -C $D/out .`, pero no el fuente `PluginKycA.java`, ni el `synkra-plugin.yaml`, ni el archivo de `META-INF/services`. CA-2 pide pegar cómo se arma. Yo lo reproduje de forma independiente y pasa.

## Tareas candidatas (defectos reales fuera de alcance)
- Límite de tamaño de cuerpo propio para `/kyc` (propuesta del codificador; de acuerdo).
- C-86: falta `detener()` de los plugins al cerrar el contexto (de acuerdo, sigue pendiente).
- Las rutas fuera de las decisiones (`GET /kyc/x` da 405, `POST /kyc/` da 404) devuelven el cuerpo de error por defecto de Spring (`timestamp/status/error/path`), no `{codigo, mensaje}`. Conviene unificar el formato de error del core.

## Rutas de transcripciones largas
Todas en mi scratchpad, `/tmp/claude-0/-home-user/311bec3f-8f32-5470-935c-db8e1e6d6a2d/scratchpad/`:
- CA-1: `/tmp/claude-0/-home-user/311bec3f-8f32-5470-935c-db8e1e6d6a2d/scratchpad/ca1.txt`
- CA-3: `/tmp/claude-0/-home-user/311bec3f-8f32-5470-935c-db8e1e6d6a2d/scratchpad/ca3.txt`
- Logs del proceso de CA-2: `/tmp/claude-0/-home-user/311bec3f-8f32-5470-935c-db8e1e6d6a2d/scratchpad/ca2/log.txt` y `/tmp/claude-0/-home-user/311bec3f-8f32-5470-935c-db8e1e6d6a2d/scratchpad/ca2/log2.txt`
- Plugins de CA-2: `/tmp/claude-0/-home-user/311bec3f-8f32-5470-935c-db8e1e6d6a2d/scratchpad/ca2/plugins/`
- Mutantes, un archivo por mutante, de `/tmp/claude-0/-home-user/311bec3f-8f32-5470-935c-db8e1e6d6a2d/scratchpad/m1-sin-coercion.txt` a `/tmp/claude-0/-home-user/311bec3f-8f32-5470-935c-db8e1e6d6a2d/scratchpad/m10-sin-restaurar.txt`
- Lista de mutantes: `/tmp/claude-0/-home-user/311bec3f-8f32-5470-935c-db8e1e6d6a2d/scratchpad/muts.txt`

```
VEREDICTO: NO-VERDE
NARANJA|core/src/main/java/io/synkra/core/EndpointsCore.java:64|Cuerpos con clave duplicada o basura al final se aceptan como SolicitudKyc válida (200)
NARANJA|core/src/main/java/io/synkra/core/EndpointsCore.java:85|Plugin que devuelve null produce 200 sin cuerpo en vez de PLUGIN_FALLO
AMARILLO|core/src/main/java/io/synkra/core/EndpointsCore.java:86|Rama LinkageError sin prueba (mutante sobrevive)
AMARILLO|core/src/test/java/io/synkra/core/EndpointsCoreTest.java|404 no fija mensaje con nombre ni búsqueda exacta (mutantes sobreviven)
AMARILLO|core/src/main/java/io/synkra/core/EndpointsCore.java:87|Prefijo de log literal, incoherente con PREFIJO_FALLO
AMARILLO|bitacoras/U02-T07-evidencia/ca2.txt|Armado de $D incompleto (sin fuente, yaml ni services)
INFORME: revisiones/U02-T07/ronda-1.md
```
