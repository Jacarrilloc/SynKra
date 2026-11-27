# U02-T01: `plugin-api/` (contrato de los plugins)

**Unidad:** U02: `Core, contrato y plugins de país`
**Historias que implementa:** H-21 (base)
**Depende de:** U01 (terminada)

---

## Alcance

**Dentro** (una línea, concreta):

> El subproyecto Gradle `plugin-api/` (`java-library`, sin dependencias de ejecución) con `SynkraPlugin` (`nombre`, `iniciar(ContextoPlugin)`, `detener`), `ContextoPlugin`, `PuntoKyc` (`validar(SolicitudKyc): ResultadoKyc`) y los records `SolicitudKyc` y `ResultadoKyc`, en `io.synkra.plugin.api`.

**Fuera** (lo que el codificador debe rechazar aunque lo vea roto):

- El proyecto `core/` y cualquier carga de plugins (U02-T02 en adelante).
- Los plugins `kyc-co` y `kyc-pe` (U02-T09 y T10).
- Cualquier dependencia de `plugin-api` hacia `verificador`, `backend`, Spring u otra librería.
- Cambios en `backend/` o `verificador/`. Fuera de `plugin-api/`, solo se toca la línea `include` de `settings.gradle.kts`.

---

## Archivos de contexto

- `aidlc-docs/construction/plans/U02-core-plugins-code-generation-plan.md` (U02-T01)
- `aidlc-docs/inception/application-design/component-methods.md` (`SynkraPlugin`, `PuntoKyc`)
- `aidlc-docs/inception/application-design/components.md` (contrato sin lógica)
- `settings.gradle.kts`, `verificador/build.gradle.kts` (patrón de un `java-library` del proyecto)

---

## Decisiones del orquestador

1. **Contenido de `ContextoPlugin`.** Solo lo que un plugin necesita para iniciar sin conocer el core: su configuración como `Map<String,String>` inmodificable y un `java.lang.System.Logger` (de la JDK, para no traer dependencias). Nada de países.
2. **`SolicitudKyc` y `ResultadoKyc`.** Records inmutables. `SolicitudKyc(String documento, Map<String,String> datos)`; `ResultadoKyc(boolean aprobado, String motivo)`. Validan `null` en el constructor.
3. **Versión del contrato.** El JAR de `plugin-api` lleva en su manifiesto `Implementation-Version` igual a la versión del proyecto, que se fija en `plugin-api/build.gradle.kts`.
4. **Java 21**, por el toolchain de la raíz. Paquete `io.synkra.plugin.api`.

---

## Criterios de aceptación

- [ ] **CA-1**: el contrato compila y no tiene dependencias de ejecución
  ```bash
  ./gradlew :plugin-api:build --rerun-tasks
  ./gradlew -q :plugin-api:dependencies --configuration runtimeClasspath
  ```
  Esperado: `BUILD SUCCESSFUL`, y la salida de dependencias dice `No dependencies` (lectura de vuelta de Gradle, no del código de salida).

- [ ] **CA-2**: las pruebas del contrato pasan y no se saltan
  ```bash
  ./gradlew :plugin-api:test --rerun
  grep -ho 'tests="[0-9]*" skipped="[0-9]*" failures="[0-9]*" errors="[0-9]*"' plugin-api/build/test-results/test/TEST-*.xml
  ```
  Esperado: `BUILD SUCCESSFUL`, al menos 4 pruebas, sin saltos ni fallos. Cubren: `null` rechazado en los dos records, la configuración de `ContextoPlugin` inmodificable, y un plugin de prueba que implementa `SynkraPlugin` y `PuntoKyc` y responde.

- [ ] **CA-3**: el JAR lleva su versión y Java 21 (lectura de vuelta del artefacto)
  ```bash
  unzip -p plugin-api/build/libs/plugin-api-*.jar META-INF/MANIFEST.MF | grep -E '^Implementation-Version:'
  D=$(mktemp -d); unzip -o -q plugin-api/build/libs/plugin-api-*.jar 'io/synkra/plugin/api/SynkraPlugin.class' -d "$D"
  javap -v "$D/io/synkra/plugin/api/SynkraPlugin.class" | grep 'major version'; rm -rf "$D"
  ```
  Esperado: una línea `Implementation-Version:` con la versión del proyecto y `major version: 65`.

- [ ] **CA-4**: el resto del build sigue pasando y el diff queda acotado
  ```bash
  ./gradlew :verificador:check :backend:check --rerun
  git diff --stat origin/main...HEAD
  ```
  Esperado: `BUILD SUCCESSFUL`; el diff toca `plugin-api/**`, una línea de `settings.gradle.kts`, la bitácora y `revisiones/U02-T01/`.

---

## Plan de pruebas

- Pruebas JUnit en `plugin-api/src/test` con un plugin de prueba mínimo.

**Rojo primero:** las pruebas antes que el contrato, en una copia o con las pruebas solas, sin `git stash` en el worktree.
