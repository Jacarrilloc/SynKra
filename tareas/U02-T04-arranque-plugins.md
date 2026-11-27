# U02-T04: `ArranquePlugins`: leer, verificar con U01 y activar

**Unidad:** U02: `Core, contrato y plugins de país`
**Historias que implementa:** H-21
**Depende de:** U02-T03

---

## Alcance

**Dentro** (una línea, concreta):

> Un `ArranquePlugins` en `io.synkra.core` que, al arrancar, lee `/config/plugins-activos.yaml` y los JAR de `/plugins`, arma un `Catalogo` de U01 con los descriptores presentes, llama a `Verificador.verificar` con la versión compilada (`VersionCore`) y, si el veredicto es `RECHAZADA`, hace que el proceso termine con código distinto de 0 y el motivo en el log; si es `APROBADA`, carga con `CargadorPlugins` solo los plugins activos.

**Fuera** (lo que el codificador debe rechazar aunque lo vea roto):

- Llamar a `iniciar` y la readiness (U02-T06). Aquí los plugins se cargan, no se inician.
- La suite completa de U01 contra el arranque (U02-T05). Aquí van solo S1, S4, S6 y S12.
- Endpoints (U02-T07) y plugins reales (U02-T09 y T10): las pruebas arman JAR en un `@TempDir`.
- Cambios en `verificador/`, `plugin-api/` o `backend/`.

---

## Archivos de contexto

- `aidlc-docs/construction/plans/U02-core-plugins-code-generation-plan.md` (U02-T04)
- `aidlc-docs/inception/application-design/services.md` (S8, arranque del core)
- `aidlc-docs/construction/U01-verificador/functional-design/` (R-V3, R-V4 y la suite S1 a S12)
- `verificador/src/main/java/io/synkra/verificador/` (`LectorDescriptor`, `Verificador`, `Catalogo`, `Seleccion`, `Veredicto`)
- `verificador/src/testFixtures/` (`SuiteCombinaciones`)
- `core/src/main/java/io/synkra/core/` (`VersionCore`, `CargadorPlugins`) y `CANDIDATAS.md` (C-71 y C-74)

---

## Decisiones del orquestador

1. **Rutas.** `/config` y `/plugins` por defecto, configurables con `synkra.core.config` y `synkra.core.plugins` (para las pruebas). La versión **no** es configurable (D4).
2. **`plugins-activos.yaml`.** Es una lista de `{nombre, version}`. Se lee con el lector seguro de U01 (SnakeYAML Engine, sin etiquetas de tipo). Si el archivo no existe, la selección está vacía: el core arranca sin plugins.
3. **Catálogo.** Se arma con el descriptor de cada JAR de `/plugins`, leído con `LectorDescriptor`. Un JAR con descriptor inválido no entra al catálogo y se registra en el log con su nombre.
4. **Rechazo.** Con `RECHAZADA`, el arranque lanza una excepción que hace fallar el contexto de Spring. El log contiene cada código de motivo y el plugin. Por eso el proceso termina con un código distinto de 0. No hay `System.exit` dentro de la lógica.
5. **Activación.** Solo se cargan con `CargadorPlugins` los plugins de la selección. Un JAR presente que no está en la selección **no** se carga: su clase nunca se define.
6. **C-74.** Se usa `CargadorPlugins.cargar(Path)`, no la costura con padre. La delegación padre primero queda como está; si una prueba la toca, se anota.

---

## Criterios de aceptación

- [ ] **CA-1**: el arranque verifica y activa según U01
  ```bash
  ./gradlew :core:test --rerun --tests '*ArranquePluginsTest'
  grep -ho 'tests="[0-9]*" skipped="[0-9]*" failures="[0-9]*" errors="[0-9]*"' core/build/test-results/test/TEST-*ArranquePluginsTest.xml
  ```
  Esperado: `BUILD SUCCESSFUL`, `tests` ≥ 6, sin saltos ni fallos. Casos:
  - S1, válido: el contexto arranca y el plugin está cargado;
  - S4, plugin `3.x` con el core 2.1.0: el contexto no arranca y el log nombra `VERSION_INCOMPATIBLE` y el plugin;
  - S6, activo ausente: no arranca, con `PLUGIN_AUSENTE`;
  - S12, presente y no activo: arranca, y la clase del plugin no activo no se carga (se lee de vuelta, por ejemplo con un contador estático o comprobando que su cargador nunca se creó);
  - sin `plugins-activos.yaml`: arranca sin plugins;
  - un YAML con etiqueta de tipo (`!!java...`) se rechaza.

- [ ] **CA-2**: lectura de vuelta del proceso real: el JAR del core sale con código distinto de 0 ante S4
  ```bash
  ./gradlew :core:bootJar
  D=$(mktemp -d); mkdir -p "$D/config" "$D/plugins"
  # el codificador pega aquí cómo arma el JAR de S4 y el plugins-activos.yaml en $D
  java -jar core/build/libs/core-*.jar --server.port=18081 --synkra.core.config="$D/config" --synkra.core.plugins="$D/plugins" > "$D/log.txt" 2>&1; echo "salida=$?"
  grep -c VERSION_INCOMPATIBLE "$D/log.txt"; rm -rf "$D"
  ```
  Esperado: `salida=` con un valor distinto de 0, y al menos una línea con `VERSION_INCOMPATIBLE`.

- [ ] **CA-3**: el resto del build sigue pasando
  ```bash
  ./gradlew :verificador:test --rerun :verificador:check :plugin-api:test --rerun :plugin-api:check :core:test --rerun :core:check
  for f in core/build/test-results/test/TEST-*.xml; do grep -Ho 'tests="[0-9]*" skipped="[0-9]*" failures="[0-9]*" errors="[0-9]*"' "$f"; done
  ```
  Esperado: `BUILD SUCCESSFUL`, sin saltos ni fallos. Se pega la salida tal cual.

- [ ] **CA-4**: el diff queda acotado
  ```bash
  git rev-parse --short HEAD; git diff --stat origin/main...HEAD
  ```
  Esperado: `core/**`, la bitácora y `revisiones/U02-T04/`. Se anota el sha.

---

## Plan de pruebas

- `ArranquePluginsTest`, con JAR armados en un `@TempDir` (como `CargadorPluginsTest`) con descriptores de la suite de U01, y un `SpringApplicationBuilder` que apunta `synkra.core.config` y `synkra.core.plugins` al `@TempDir`.

**Rojo primero:** la prueba antes que `ArranquePlugins`, en una copia o con la prueba sola, sin `git stash`.
