# U02-T07: endpoints del core

**Unidad:** U02: `Core, contrato y plugins de país`
**Historias que implementa:** H-21
**Depende de:** U02-T06

---

## Alcance

**Dentro** (una línea, concreta):

> Dos endpoints en el core: `GET /plugins`, que devuelve el nombre y la versión de los plugins activos e iniciados, y `POST /kyc/{plugin}`, que recibe una `SolicitudKyc` en JSON y delega en el `PuntoKyc` de ese plugin, devolviendo su `ResultadoKyc`.

**Fuera** (lo que el codificador debe rechazar aunque lo vea roto):

- Lógica de KYC de país: las reglas viven en los plugins (U02-T09 y T10); aquí las pruebas usan plugins armados en `@TempDir`.
- Autenticación (U10), métricas o documentación OpenAPI.
- Cambios en `verificador/`, `plugin-api/` o `backend/`.

---

## Archivos de contexto

- `aidlc-docs/construction/plans/U02-core-plugins-code-generation-plan.md` (U02-T07)
- `aidlc-docs/inception/application-design/components.md` (C03) y `component-methods.md`
- `plugin-api/src/main/java/io/synkra/plugin/` (`PuntoKyc`, `SolicitudKyc`, `ResultadoKyc`)
- `core/src/main/java/io/synkra/core/` (`ArranquePlugins`, el iniciador de U02-T06, `VersionCore`)
- `backend/src/main/java/io/synkra/plataforma/ErrorApi.java` (formato de error de U03-T03), solo como referencia de estilo: el core no depende del backend

---

## Decisiones del orquestador

1. **`GET /plugins`.** Responde `200` con una lista JSON `[{"nombre": "...", "version": "..."}]`, en el orden de carga. Solo los plugins **iniciados**: si el arranque de U02-T06 falló, la lista está vacía (y la readiness ya está en `503`). La versión sale del descriptor del JAR, no del plugin.
2. **`POST /kyc/{plugin}`.** El `{plugin}` es el nombre del descriptor. Si no está activo: `404`. Si está activo y no implementa `PuntoKyc`: `404` con otro motivo. Si el cuerpo no es una `SolicitudKyc` válida: `400`. Si el plugin lanza una excepción: `500` que nombra el plugin y no filtra la traza. Si no, `200` con el `ResultadoKyc` en JSON.
3. **Errores.** Cuerpo JSON con `codigo` y `mensaje`; los códigos son constantes con nombre (`PLUGIN_NO_ACTIVO`, `PLUGIN_SIN_KYC`, `SOLICITUD_INVALIDA`, `PLUGIN_FALLO`).
4. **Cargador de clases.** La llamada al plugin se hace con el *context class loader* del hilo puesto en el cargador del plugin y restaurado después, en un `finally`.
5. **El core no conoce países (P3).** Ninguna ruta, mensaje ni constante nombra un país.

---

## Criterios de aceptación

- [ ] **CA-1**: endpoints con lectura de vuelta por HTTP
  ```bash
  ./gradlew :core:test --rerun --tests '*EndpointsCoreTest'
  grep -ho 'tests="[0-9]*" skipped="[0-9]*" failures="[0-9]*" errors="[0-9]*"' core/build/test-results/test/TEST-*EndpointsCoreTest.xml
  ```
  Esperado: `BUILD SUCCESSFUL`, `tests` ≥ 7, sin saltos ni fallos. Con servidor en puerto aleatorio y un cliente HTTP real:
  - dos plugins activos y uno presente no activo: `GET /plugins` lista exactamente los dos activos, con su versión;
  - `POST /kyc/<activo>` con una solicitud: `200` y el resultado que devuelve el plugin de prueba (que además registra la llamada, para leerla de vuelta);
  - `POST /kyc/<no-activo>`: `404` `PLUGIN_NO_ACTIVO`;
  - plugin activo sin `PuntoKyc`: `404` `PLUGIN_SIN_KYC`;
  - cuerpo inválido: `400` `SOLICITUD_INVALIDA`;
  - plugin que lanza: `500` `PLUGIN_FALLO`, el mensaje nombra el plugin y el cuerpo no contiene `at io.` ni `Exception`;
  - el *context class loader* dentro del plugin es el del plugin, y después de la petición vuelve a ser el anterior.

- [ ] **CA-2**: proceso real
  ```bash
  ./gradlew :core:bootJar
  # el codificador pega cómo arma $D/config y $D/plugins con un plugin activo
  java -jar core/build/libs/core-*.jar --server.port=18082 --synkra.core.config="$D/config" --synkra.core.plugins="$D/plugins" > "$D/log.txt" 2>&1 &
  # espera a la readiness y luego:
  curl -s localhost:18082/plugins
  ```
  Esperado: la lista con solo el plugin activo. Se pega la salida y se mata el proceso.

- [ ] **CA-3**: el resto del build sigue pasando
  ```bash
  ./gradlew :verificador:test --rerun :verificador:check :plugin-api:test --rerun :plugin-api:check :core:test --rerun :core:check
  for f in core/build/test-results/test/TEST-*.xml; do grep -Ho 'tests="[0-9]*" skipped="[0-9]*" failures="[0-9]*" errors="[0-9]*"' "$f"; done
  ```
  Esperado: `BUILD SUCCESSFUL`, sin saltos ni fallos. Se pega la salida tal cual.

- [ ] **CA-4**: el diff queda acotado
  ```bash
  git rev-parse --short HEAD; git diff --stat <base-del-despacho>...HEAD
  ```
  Esperado: `core/**`, la bitácora y `revisiones/U02-T07/`. Se anota el sha.

---

## Plan de pruebas

- `EndpointsCoreTest` con plugins de prueba armados en `@TempDir` (como `ArranquePluginsTest`), uno que implementa `PuntoKyc`, uno que no y uno que lanza.

**Rojo primero:** la prueba antes que el código, en una copia o con la prueba sola, sin `git stash`.
