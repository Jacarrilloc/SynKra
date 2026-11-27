# U02-T06: fallo al iniciar un plugin y readiness

**Unidad:** U02: `Core, contrato y plugins de país`
**Historias que implementa:** H-22
**Depende de:** U02-T04 (y va después de U02-T05 porque las dos tocan `core/src/test`)

---

## Alcance

**Dentro** (una línea, concreta):

> Después de cargar los plugins activos, el core llama a `iniciar(ContextoPlugin)` de cada uno; si alguno lanza una excepción, el core no queda a medias: `/actuator/health/readiness` responde `503` (`OUT_OF_SERVICE`), el log nombra el plugin y la causa, y los plugins que ya se habían iniciado se detienen. Se crea el *fixture* `plugins/fixture-falla-al-iniciar/`, solo para pruebas.

**Fuera** (lo que el codificador debe rechazar aunque lo vea roto):

- Endpoints `GET /plugins` y `POST /kyc/{plugin}` (U02-T07).
- Plugins reales `kyc-co` y `kyc-pe` (U02-T09 y T10).
- Reintentar `iniciar` o seguir con los demás plugins como si nada.
- Cambios en `verificador/`, `plugin-api/` o `backend/`.

---

## Archivos de contexto

- `aidlc-docs/construction/plans/U02-core-plugins-code-generation-plan.md` (U02-T06)
- `aidlc-docs/inception/application-design/services.md` (S8, arranque del core)
- `aidlc-docs/inception/user-stories/stories.md` (H-22)
- `plugin-api/src/main/java/io/synkra/plugin/` (`SynkraPlugin`, `ContextoPlugin`)
- `core/src/main/java/io/synkra/core/` (`ArranquePlugins`, `CargadorPlugins`, `PluginCargado`)
- `core/src/test/java/io/synkra/core/ArranquePluginsTest.java` y `CANDIDATAS.md` (C-71, C-74)

---

## Decisiones del orquestador

1. **Dónde vive.** Un componente propio (por ejemplo `IniciadorPlugins`) que corre al terminar el arranque del contexto, no dentro de `ArranquePlugins`: el rechazo de U01 hace fallar el contexto (el proceso termina), y el fallo de `iniciar` **no**: el proceso sigue vivo, sin readiness, para que Kubernetes no le mande tráfico y el SRE pueda leer el log.
2. **Readiness.** Se publica un `AvailabilityChangeEvent` con `ReadinessState.REFUSING_TRAFFIC` cuando falla un plugin, y `ACCEPTING_TRAFFIC` solo después de iniciar todos. Las sondas de Spring Boot se activan con `management.endpoint.health.probes.enabled=true` en `application.properties` si no lo están ya. La liveness no cambia.
3. **Orden y limpieza.** Se inician en el orden en que se cargaron. Si falla el plugin *k*, se llama a `detener()` de los que ya se iniciaron, en orden inverso; una excepción en `detener()` se registra y no tapa la causa original.
4. **Log.** Una línea ERROR con el formato `Plugin fallo al iniciar plugin=<nombre>: <causa>`, con el nombre que devuelve `nombre()` del plugin. El prefijo es una constante con nombre.
5. **Contexto.** `ContextoPlugin` recibe la configuración del plugin (un mapa vacío en esta tarea) y un `System.Logger` con el nombre del plugin.
6. **Fixture.** `plugins/fixture-falla-al-iniciar/` es un subproyecto de Gradle (se añade a `settings.gradle.kts`) que produce un JAR con `synkra-plugin.yaml` compatible con el core 2.x y un `SynkraPlugin` cuyo `iniciar` lanza `IllegalStateException`. No se publica ni entra a ninguna imagen. Si es más sencillo armarlo en un `@TempDir` como en `ArranquePluginsTest`, también vale, y la bitácora dice cuál se eligió.

---

## Criterios de aceptación

- [ ] **CA-1**: readiness y log, con lectura de vuelta por HTTP
  ```bash
  ./gradlew :core:test --rerun --tests '*FalloInicioPluginTest'
  grep -ho 'tests="[0-9]*" skipped="[0-9]*" failures="[0-9]*" errors="[0-9]*"' core/build/test-results/test/TEST-*FalloInicioPluginTest.xml
  ```
  Esperado: `BUILD SUCCESSFUL`, `tests` ≥ 4, sin saltos ni fallos. Casos, con el servidor en un puerto aleatorio y peticiones HTTP reales:
  - un plugin que falla: `GET /actuator/health/readiness` responde `503`, `GET /actuator/health/liveness` responde `200`, y el log contiene `plugin=<nombre>` y la causa;
  - dos plugins, el segundo falla: el primero recibió `iniciar` y después `detener` (se lee de vuelta con un contador del plugin de prueba);
  - todos inician bien: readiness `200`;
  - sin plugins activos: readiness `200`.

- [ ] **CA-2**: el rojo muerde
  En una copia (sin `git stash`), con `iniciar` sin la captura de la excepción, CA-1 falla por aserción. Se pega la salida.

- [ ] **CA-3**: el resto del build sigue pasando
  ```bash
  ./gradlew :verificador:test --rerun :verificador:check :plugin-api:test --rerun :plugin-api:check :core:test --rerun :core:check
  for f in core/build/test-results/test/TEST-*.xml; do grep -Ho 'tests="[0-9]*" skipped="[0-9]*" failures="[0-9]*" errors="[0-9]*"' "$f"; done
  ```
  Esperado: `BUILD SUCCESSFUL`, sin saltos ni fallos (incluidas `ArranquePluginsTest` y `ConcordanciaSuiteTest`). Se pega la salida tal cual.

- [ ] **CA-4**: el diff queda acotado
  ```bash
  git rev-parse --short HEAD; git diff --stat <base-del-despacho>...HEAD
  ```
  Esperado: `core/**`, `plugins/fixture-falla-al-iniciar/**` y una línea de `settings.gradle.kts` (si se eligió el subproyecto), la bitácora y `revisiones/U02-T06/`. Se anota el sha.

---

## Plan de pruebas

- `FalloInicioPluginTest` con `@SpringBootTest(webEnvironment = RANDOM_PORT)` o un `SpringApplicationBuilder` con `server.port=0`, apuntando `synkra.core.config` y `synkra.core.plugins` a un `@TempDir`, y un cliente HTTP real para leer las sondas.

**Rojo primero:** la prueba antes que el código, en una copia o con la prueba sola, sin `git stash`.
