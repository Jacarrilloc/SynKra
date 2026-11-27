# U02-T03: `CargadorPlugins`: un `URLClassLoader` por plugin

**Unidad:** U02: `Core, contrato y plugins de país`
**Historias que implementa:** H-22
**Depende de:** U02-T02

---

## Alcance

**Dentro** (una línea, concreta):

> Un `CargadorPlugins` en `io.synkra.core` que, dado un JAR de plugin, crea **su propio** `URLClassLoader` (con el cargador de `plugin-api` como padre) y obtiene el `SynkraPlugin` con `ServiceLoader` dentro de ese cargador.

**Fuera** (lo que el codificador debe rechazar aunque lo vea roto):

- Leer `/config/plugins-activos.yaml`, verificar con U01 o decidir qué plugins se activan (U02-T04).
- Llamar a `iniciar` o manejar sus fallos (U02-T06), y los endpoints (U02-T07).
- Los plugins reales `kyc-co` y `kyc-pe` (U02-T09 y T10). Las pruebas arman JAR de prueba en un `@TempDir`.
- Cambios en `plugin-api/`, `verificador/` o `backend/`.

---

## Archivos de contexto

- `aidlc-docs/construction/plans/U02-core-plugins-code-generation-plan.md` (U02-T03)
- `aidlc-docs/inception/application-design/components.md` y `component-methods.md`
- `plugin-api/src/main/java/io/synkra/plugin/api/`
- `core/` (de U02-T02) y `CANDIDATAS.md` (C-71)

---

## Decisiones del orquestador

1. **Aislamiento.** Cada JAR tiene su cargador. El padre es el cargador que carga `plugin-api`, para que `SynkraPlugin` sea el mismo tipo en el core y en el plugin. Dos plugins con una clase del mismo nombre no se pisan.
2. **Un plugin por JAR.** Si `ServiceLoader` no encuentra ninguna implementación, o encuentra más de una, es un error propio del core que nombra el JAR. No se devuelve un plugin a medias.
3. **Recursos.** El cargador se cierra si la carga falla. Lo que devuelve `cargar` incluye el cargador, para que quien lo use pueda cerrarlo.
4. **C-71.** La variable `SYNKRA_CORE_VERSION=9.9.9` que fija `tasks.test` en `:core` no debe influir en estas pruebas. Si alguna la lee, va en una tarea `Test` aparte.

---

## Criterios de aceptación

- [ ] **CA-1**: los plugins quedan aislados
  ```bash
  ./gradlew :core:test --rerun --tests '*CargadorPluginsTest'
  grep -ho 'tests="[0-9]*" skipped="[0-9]*" failures="[0-9]*" errors="[0-9]*"' core/build/test-results/test/TEST-*CargadorPluginsTest.xml
  ```
  Esperado: `BUILD SUCCESSFUL`, `tests` ≥ 5, sin saltos ni fallos. Las pruebas afirman: dos plugins tienen cargadores distintos; una clase `Comun` con el mismo nombre en los dos JAR devuelve valores distintos; `SynkraPlugin` del core y del plugin es el mismo `Class`; un JAR sin implementación y uno con dos dan el error que nombra el JAR; y el cargador queda cerrado tras un fallo.

- [ ] **CA-2**: el resto del build sigue pasando
  ```bash
  ./gradlew :plugin-api:test --rerun :plugin-api:check :core:test --rerun :core:check
  for f in core/build/test-results/test/TEST-*.xml; do grep -o 'tests="[0-9]*" skipped="[0-9]*" failures="[0-9]*" errors="[0-9]*"' "$f"; done
  ```
  Esperado: `BUILD SUCCESSFUL`, sin saltos ni fallos. Se pega cada línea tal cual.

- [ ] **CA-3**: el diff queda acotado
  ```bash
  git rev-parse --short HEAD; git diff --stat origin/main...HEAD
  ```
  Esperado: `core/**`, la bitácora y `revisiones/U02-T03/`. Se anota el sha sobre el que se tomó.

---

## Plan de pruebas

- `CargadorPluginsTest`, que compila dos plugins de prueba pequeños en un `@TempDir` (con `javax.tools` o fuentes precompiladas en `src/test/resources`) y los empaqueta con su `META-INF/services/io.synkra.plugin.api.SynkraPlugin`.

**Rojo primero:** la prueba antes que el cargador, en una copia o con la prueba sola, sin `git stash`.
