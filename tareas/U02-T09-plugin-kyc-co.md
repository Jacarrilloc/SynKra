# U02-T09: plugin `kyc-co 1.0.0`

**Unidad:** U02: `Core, contrato y plugins de país`
**Historias que implementa:** H-23
**Depende de:** U02-T07 (contrato `PuntoKyc` ya expuesto por el core); usa solo `plugin-api/`

---

## Alcance

**Dentro** (una línea, concreta):

> Un subproyecto `plugins/kyc-co/` que produce `kyc-co-1.0.0.jar`: un `SynkraPlugin` que además implementa `PuntoKyc` con una regla simulada de Colombia (documento numérico de 6 a 10 dígitos), registrado con `ServiceLoader`, y con un `synkra-plugin.yaml` válido para U01 (`nombre: kyc-co`, `version: 1.0.0`, `core: synkra-core`, `coreCompatible: 2.x`, `etiquetas: {pais: co}`).

**Fuera** (lo que el codificador debe rechazar aunque lo vea roto):

- `kyc-pe` (U02-T10).
- Subir el JAR al catálogo o meterlo en una imagen.
- Cambios en `core/`, `verificador/`, `plugin-api/` o `backend/`. La única línea fuera de `plugins/kyc-co/` es su `include` en `settings.gradle.kts`.

---

## Archivos de contexto

- `aidlc-docs/construction/plans/U02-core-plugins-code-generation-plan.md` (U02-T09)
- `aidlc-docs/construction/U01-verificador/functional-design/business-rules.md` (campos y tipos del descriptor)
- `plugin-api/` (`SynkraPlugin`, `PuntoKyc`, `SolicitudKyc`, `ResultadoKyc`, `ContextoPlugin`)
- `core/src/test/java/io/synkra/core/ArranquePluginsTest.java` y `EndpointsCoreTest.java` (cómo arranca el core con un plugin)

---

## Decisiones del orquestador

1. **Dependencias.** `compileOnly(project(":plugin-api"))`: el JAR no lleva `plugin-api` dentro (lo provee el core). Ninguna otra dependencia en `runtimeClasspath`.
2. **Regla.** `aprobado` si `documento` tiene solo dígitos ASCII y de 6 a 10 caracteres; si no, `aprobado = false` con un `motivo` que dice qué falló. Los límites son constantes con nombre. Una `SolicitudKyc` nula o con documento nulo no lanza: se rechaza con motivo.
3. **Descriptor.** En la raíz del JAR (`synkra-plugin.yaml`), generado o copiado desde `src/main/resources`, con la versión tomada de `version` del proyecto de Gradle para que no se desalineen.
4. **Nombre.** `nombre()` del plugin devuelve `kyc-co`, igual que el descriptor.

---

## Criterios de aceptación

- [ ] **CA-1**: pruebas del plugin y descriptor válido para U01
  ```bash
  ./gradlew :plugins:kyc-co:test --rerun :plugins:kyc-co:check :plugins:kyc-co:jar
  grep -ho 'tests="[0-9]*" skipped="[0-9]*" failures="[0-9]*" errors="[0-9]*"' plugins/kyc-co/build/test-results/test/TEST-*.xml
  unzip -p plugins/kyc-co/build/libs/kyc-co-1.0.0.jar synkra-plugin.yaml
  unzip -l plugins/kyc-co/build/libs/kyc-co-1.0.0.jar | grep -c 'io/synkra/plugin/'
  ```
  Esperado: `BUILD SUCCESSFUL`, sin saltos ni fallos, con pruebas de: 6 y 10 dígitos aprobados; 5, 11, letras, dígitos no ASCII (por ejemplo `١٢٣٤٥٦`), vacío y nulo rechazados con motivo; y una prueba que lee el `synkra-plugin.yaml` del JAR construido con `LectorDescriptor` de U01 (dependencia de prueba) y lo da por válido. El `unzip -p` muestra `pais: co` y `2.x`; el conteo de clases de `plugin-api` dentro del JAR es `0`.

- [ ] **CA-2**: el core real lo carga y lo usa (lectura de vuelta por HTTP)
  ```bash
  ./gradlew :core:bootJar :plugins:kyc-co:jar
  D=$(mktemp -d); mkdir -p "$D/config" "$D/plugins"; cp plugins/kyc-co/build/libs/kyc-co-1.0.0.jar "$D/plugins/"
  printf -- '- nombre: kyc-co\n  version: 1.0.0\n' > "$D/config/plugins-activos.yaml"
  # arranca el core con --synkra.core.config y --synkra.core.plugins, espera la readiness, y:
  curl -s localhost:18083/plugins
  curl -s -X POST -H 'Content-Type: application/json' -d '{"documento":"1234567","datos":{}}' localhost:18083/kyc/kyc-co
  curl -s -X POST -H 'Content-Type: application/json' -d '{"documento":"12ab","datos":{}}' localhost:18083/kyc/kyc-co
  ```
  Esperado: la lista con `kyc-co` `1.0.0`; `aprobado: true` y `aprobado: false` con motivo. Se pega la salida y se mata el proceso.

- [ ] **CA-3**: el resto del build sigue pasando
  ```bash
  ./gradlew :verificador:test --rerun :verificador:check :plugin-api:test --rerun :plugin-api:check :core:test --rerun :core:check
  ```
  Esperado: `BUILD SUCCESSFUL`, sin saltos ni fallos.

- [ ] **CA-4**: el diff queda acotado
  ```bash
  git rev-parse --short HEAD; git diff --stat <base-del-despacho>...HEAD
  ```
  Esperado: `plugins/kyc-co/**`, una línea de `settings.gradle.kts`, la bitácora y `revisiones/U02-T09/`. Se anota el sha.

---

**Rojo primero:** las pruebas de la regla antes que la regla.
