# U01-T06: `Descriptor` y `LectorDescriptor` (R-V3)

**Unidad:** U01: `Verificador y suite`
**Historias que implementa:** H-03
**Depende de:** U01-T04

---

## Alcance

**Dentro** (una línea, concreta):

> El record `Descriptor` y la clase `LectorDescriptor` con `leer(Path jar)`, `desdeYaml(String)` y `aYaml(Descriptor)`: lee `synkra-plugin.yaml` de la raíz del JAR con snakeyaml-engine en modo seguro, valida R-V3 y devuelve `Resultado<Descriptor>` con **todos** los motivos de una vez.

**Fuera** (lo que el codificador debe rechazar aunque lo vea roto):

- La propiedad de ida y vuelta `desdeYaml(aYaml(d)) == d` con jqwik: es de U01-T07. Aquí basta un caso de ejemplo.
- `Seleccion`, `Veredicto`, `Verificador` y `Catalogo` (U01-T08 y U01-T09).
- C-06 (que `LectorDescriptor` asigne el `campo` del `Motivo`): no hace falta aquí, porque `Version.parsear` ya pone `version` y `RangoVersion.parsear` ya pone `coreCompatible`, que son justo los campos del descriptor. Sigue como candidata para U01-T09.
- Cambios en `Version`, `RangoVersion`, `Resultado` o `Motivo`.
- Leer el JAR por la red o desde el catálogo: solo un `Path` local.
- Nuevas dependencias: snakeyaml-engine 3.2 ya está declarado y es la única de ejecución.

---

## Archivos de contexto

- `aidlc-docs/construction/U01-verificador/functional-design/business-rules.md` (R-V3)
- `aidlc-docs/construction/U01-verificador/functional-design/domain-entities.md` (`Descriptor` y la tabla de códigos)
- `aidlc-docs/construction/U01-verificador/functional-design/business-logic-model.md` (casos S8 y S9)
- `verificador/src/main/java/io/synkra/verificador/` (`Version`, `RangoVersion`, `Resultado`, `Motivo`, `CodigoMotivo`)

---

## Criterios de aceptación

- [ ] **CA-1**: las pruebas del lector pasan, sin caché
  ```bash
  ./gradlew :verificador:test --rerun --tests '*LectorDescriptorTest'
  ```
  Esperado: `BUILD SUCCESSFUL`.

- [ ] **CA-2**: lectura de vuelta del reporte: todos los casos y ninguno saltado
  ```bash
  grep -ho 'tests="[0-9]*" skipped="[0-9]*" failures="[0-9]*" errors="[0-9]*"' verificador/build/test-results/test/TEST-*LectorDescriptorTest.xml
  ```
  Esperado: `tests` ≥ 10, `skipped="0" failures="0" errors="0"`. Casos mínimos, cada uno con el código
  exacto que espera:
  - JAR sin `synkra-plugin.yaml` → `DESCRIPTOR_SIN_ARCHIVO`.
  - Falta `coreCompatible` (S8) → `DESCRIPTOR_FALTA_CAMPO` con `campo = coreCompatible`.
  - `coreCompatibel` en lugar de `coreCompatible` (S9) → **dos** motivos en el mismo resultado:
    `DESCRIPTOR_CAMPO_DESCONOCIDO` (`coreCompatibel`) y `DESCRIPTOR_FALTA_CAMPO` (`coreCompatible`).
  - Faltan `nombre` y `version` a la vez → dos `DESCRIPTOR_FALTA_CAMPO`.
  - `version: 2.1` → `VERSION_INVALIDA`; `coreCompatible: ^2.1` → `RANGO_INVALIDO`.
  - YAML con etiqueta de tipo (`!!java.net.URL "http://x"` o una etiqueta local `!algo`) → `DESCRIPTOR_INVALIDO`, sin instanciar nada.
  - YAML con un alias recursivo o mal formado → `DESCRIPTOR_INVALIDO`.
  - `nombre: Kyc_PE` (no cumple `^[a-z0-9]+(-[a-z0-9]+)*$`) → `DESCRIPTOR_INVALIDO` con `campo = nombre`.
  - Descriptor válido con etiquetas `pais: pe` y `descripcion` → `Exito` con todos los campos.
  - `aYaml` de un descriptor válido, leído con `desdeYaml`, da el mismo descriptor.

- [ ] **CA-3**: el verificador sigue sin Spring ni Fabric8 en ejecución
  ```bash
  ./gradlew :verificador:dependencies --configuration runtimeClasspath | grep -ciE "spring|fabric8"
  ```
  Esperado: `0`.

- [ ] **CA-4**: todo el verificador sigue pasando, sin caché
  ```bash
  ./gradlew :verificador:check --rerun
  ```
  Esperado: `BUILD SUCCESSFUL`.

---

## Plan de pruebas

- `LectorDescriptorTest`: los JAR de prueba se construyen en un `@TempDir` con `java.util.zip` (no se
  versionan binarios).
- El caso de etiqueta de tipo comprueba que el resultado es un `Fallo` y que no salió ninguna
  excepción del lector.

**Rojo primero:** prueba antes que código; registra el fallo de CA-1 y luego el mismo comando pasando.

---

## Notas

- Paquete: `io.synkra.verificador`.
- **Código nuevo `DESCRIPTOR_INVALIDO`** (decisión del equipo antes de despachar): R-V3 pide rechazar
  el YAML con etiquetas de tipo, pero ninguno de los 9 códigos de `domain-entities.md` cubre «YAML
  ilegible, con etiqueta de tipo, con un valor del tipo equivocado o un nombre que no cumple el
  patrón». Esta tarea añade `DESCRIPTOR_INVALIDO` a `CodigoMotivo` y a la tabla de
  `domain-entities.md`, y U01-T08 pasa a contar 10 códigos.
- Modo seguro de snakeyaml-engine: `LoadSettings` con `setAllowRecursiveKeys(false)`, un límite de
  alias (`setMaxAliasesForCollections`) y un límite de tamaño; cualquier etiqueta que no sea de los
  tipos básicos de YAML es un fallo.
- `leer` y `desdeYaml` nunca lanzan excepción por culpa del archivo: devuelven `Fallo`.
- `etiquetas` vacías es válido; sus claves cumplen el mismo patrón que `nombre`.
