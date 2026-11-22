# U01-T03: `Version` (R-V1) y sus pruebas

**Unidad:** U01: `Verificador y suite`
**Historias que implementa:** H-07
**Depende de:** U01-T02

---

## Alcance

**Dentro** (una línea, concreta):

> El record `Version` (`parsear`, orden, `toString`) y los tipos base que `parsear` necesita para devolver errores sin excepciones: `Resultado<T>`, `Motivo` y la enumeración `CodigoMotivo` con los 9 códigos.

**Fuera** (lo que el codificador debe rechazar aunque lo vea roto):

- `RangoVersion` (U01-T04) y las propiedades de jqwik (U01-T05).
- `Descriptor` y `LectorDescriptor` (U01-T06).
- `Seleccion`, `Alternativa`, `VeredictoPlugin`, `Veredicto` y la prueba que cuenta los códigos (U01-T08).
- `Verificador` y la interfaz `Catalogo` (U01-T09).
- Cambios en archivos de Gradle.

---

## Archivos de contexto

- `aidlc-docs/construction/U01-verificador/functional-design/domain-entities.md` (`Version`, `Motivo` y la tabla de códigos)
- `aidlc-docs/construction/U01-verificador/functional-design/business-rules.md` (R-V1)
- `aidlc-docs/inception/application-design/component-methods.md` (qué es `Resultado<T>`)
- `aidlc-docs/construction/plans/U01-verificador-code-generation-plan.md`

---

## Criterios de aceptación

- [ ] **CA-1**: las pruebas de `Version` pasan, corridas de nuevo (sin caché)
  ```bash
  ./gradlew :verificador:test --rerun --tests '*VersionTest'
  ```
  Esperado: `BUILD SUCCESSFUL`.

- [ ] **CA-2**: la prueba cubre los casos de R-V1 y no se saltó ninguno. Lectura de vuelta del reporte, no del código de salida:
  ```bash
  grep -ho 'tests="[0-9]*" skipped="[0-9]*" failures="[0-9]*" errors="[0-9]*"' verificador/build/test-results/test/TEST-*VersionTest.xml
  ```
  Esperado: `tests` ≥ 6, `skipped="0" failures="0" errors="0"`. Los casos mínimos son: `2.1.0` válida; `2.01.0`, `2.1.0-rc1`, `2.1` y `2.1.0+build` devuelven un error con código `VERSION_INVALIDA` (sin lanzar excepción); `2.10.0` es mayor que `2.9.0`; `toString` devuelve el texto de entrada para una versión válida.

- [ ] **CA-3**: `CodigoMotivo` tiene los 9 códigos de `domain-entities.md`, con esos nombres
  ```bash
  for c in DESCRIPTOR_FALTA_CAMPO DESCRIPTOR_CAMPO_DESCONOCIDO DESCRIPTOR_SIN_ARCHIVO VERSION_INVALIDA RANGO_INVALIDO CORE_EQUIVOCADO VERSION_INCOMPATIBLE PLUGIN_REPETIDO PLUGIN_AUSENTE; do grep -rqw "$c" verificador/src/main/java || echo "falta $c"; done
  ```
  Esperado: no imprime nada.

---

## Plan de pruebas

- `VersionTest` (JUnit 5): los casos de CA-2, uno por método o parametrizados.
- Prueba negativa: ninguna entrada inválida lanza excepción; todas devuelven `Resultado` con error.

**Rojo primero:** escribe `VersionTest` antes que `Version`, corre CA-1 y registra el fallo de
compilación o de aserción. Después, pasando.

---

## Notas

- Paquete: `io.synkra.verificador`.
- `Resultado<T>` es un valor o una lista de `Motivo`, nunca una excepción para errores de negocio.
- Esta tarea crea la enumeración completa porque `parsear` necesita `VERSION_INVALIDA`; la prueba
  que cuenta exactamente 9 códigos sigue siendo de U01-T08.
