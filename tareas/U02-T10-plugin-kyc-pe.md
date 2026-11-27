# U02-T10: plugin `kyc-pe 1.3.0` y `kyc-pe 2.0.0`

**Unidad:** U02: `Core, contrato y plugins de país`
**Historias que implementa:** H-23 (y prepara la demo de CU4)
**Depende de:** U02-T09 (mismo patrón de subproyecto)

---

## Alcance

**Dentro** (una línea, concreta):

> Un subproyecto `plugins/kyc-pe/` que produce dos JAR desde el mismo código: `kyc-pe-1.3.0.jar` con `coreCompatible: 2.x` y `kyc-pe-2.0.0.jar` con `coreCompatible: 3.x`, los dos con `etiquetas: {pais: pe}` y una regla simulada de Perú (DNI de exactamente 8 dígitos).

**Fuera** (lo que el codificador debe rechazar aunque lo vea roto):

- Cambiar `kyc-co` o extraer código común entre los dos plugins.
- Subir los JAR o meterlos en una imagen; la prueba de humo es U02-T12.
- Cambios en `core/`, `verificador/`, `plugin-api/` o `backend/`. La única línea fuera de `plugins/kyc-pe/` es su `include` en `settings.gradle.kts`.

---

## Archivos de contexto

- `aidlc-docs/construction/plans/U02-core-plugins-code-generation-plan.md` (U02-T10)
- `plugins/kyc-co/` (U02-T09: el patrón a seguir) y su revisión en `revisiones/U02-T09/`
- `aidlc-docs/construction/U01-verificador/functional-design/business-rules.md`

---

## Decisiones del orquestador

1. **Dos JAR, un código.** Dos tareas `Jar` (por ejemplo `jar130` y `jar200`) o una variante por versión; cada una escribe su propio `synkra-plugin.yaml` desde una plantilla con la versión y el rango como parámetros. `assemble` produce los dos. No se copia el código fuente.
2. **Regla.** Igual que en `kyc-co` (constantes con nombre, sin lanzar ante nulos), con 8 dígitos ASCII exactos.
3. **Nombre.** `kyc-pe` en los dos JAR, en `nombre()` y en el descriptor.

---

## Criterios de aceptación

- [ ] **CA-1**: pruebas y los dos descriptores
  ```bash
  ./gradlew :plugins:kyc-pe:test --rerun :plugins:kyc-pe:check :plugins:kyc-pe:assemble
  grep -ho 'tests="[0-9]*" skipped="[0-9]*" failures="[0-9]*" errors="[0-9]*"' plugins/kyc-pe/build/test-results/test/TEST-*.xml
  for v in 1.3.0 2.0.0; do echo "== $v"; unzip -p plugins/kyc-pe/build/libs/kyc-pe-$v.jar synkra-plugin.yaml; done
  ```
  Esperado: `BUILD SUCCESSFUL`, sin saltos ni fallos, con pruebas de 8 dígitos aprobado; 7, 9, letras, no ASCII, vacío y nulo rechazados; y una prueba que lee los **dos** JAR construidos con `LectorDescriptor` de U01 y comprueba `version` y `coreCompatible` de cada uno. Los `unzip -p` muestran `1.3.0` con `2.x` y `2.0.0` con `3.x`, los dos con `pais: pe`.

- [ ] **CA-2**: el core real acepta uno y rechaza el otro
  ```bash
  ./gradlew :core:bootJar :plugins:kyc-pe:assemble
  # el codificador pega cómo arma $D/config y $D/plugins para cada versión
  # 1.3.0: el core arranca, GET /plugins lista kyc-pe 1.3.0 y POST /kyc/kyc-pe con 8 dígitos da aprobado
  # 2.0.0: java -jar ... ; echo "salida=$?"  y  grep -c 'VERSION_INCOMPATIBLE plugin=kyc-pe' "$D/log.txt"
  ```
  Esperado: con `1.3.0`, la lista y el `aprobado: true`; con `2.0.0`, `salida=` distinta de 0 y al menos una línea `VERSION_INCOMPATIBLE plugin=kyc-pe`. Se pegan las dos corridas.

- [ ] **CA-3**: el resto del build sigue pasando
  ```bash
  ./gradlew :verificador:test --rerun :verificador:check :plugin-api:test --rerun :plugin-api:check :core:test --rerun :core:check :plugins:kyc-co:test --rerun :plugins:kyc-co:check
  ```
  Esperado: `BUILD SUCCESSFUL`, sin saltos ni fallos.

- [ ] **CA-4**: el diff queda acotado
  ```bash
  git rev-parse --short HEAD; git diff --stat <base-del-despacho>...HEAD
  ```
  Esperado: `plugins/kyc-pe/**`, una línea de `settings.gradle.kts`, la bitácora y `revisiones/U02-T10/`. Se anota el sha.

---

**Rojo primero:** las pruebas de la regla antes que la regla.
