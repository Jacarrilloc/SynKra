# U01-T03: `Version` (R-V1) y los tipos base del resultado

**Unidad:** U01: `Verificador y suite`
**Historias que implementa:** H-07
**Depende de:** U01-T02

---

## Alcance

**Dentro** (una línea, concreta):

> El record `Version` (`parsear`, orden natural, `toString`) y los tipos que `parsear` necesita para devolver errores sin excepciones: `Resultado<T>`, `Motivo` y la enumeración `CodigoMotivo` con sus 9 códigos.

**Fuera** (lo que el codificador debe rechazar aunque lo vea roto):

- `RangoVersion` (U01-T04) y las propiedades de jqwik sobre versiones o rangos (U01-T05).
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

- [ ] **CA-1**: las pruebas de `Version` pasan, sin caché
  ```bash
  ./gradlew :verificador:test --rerun --tests '*VersionTest'
  ```
  Esperado: `BUILD SUCCESSFUL`.

- [ ] **CA-2**: lectura de vuelta del reporte: cubre R-V1 y no se saltó ningún caso
  ```bash
  grep -ho 'tests="[0-9]*" skipped="[0-9]*" failures="[0-9]*" errors="[0-9]*"' verificador/build/test-results/test/TEST-*VersionTest.xml
  ```
  Esperado: `tests` ≥ 7, `skipped="0" failures="0" errors="0"`. Casos mínimos: `2.1.0` válida;
  `2.01.0`, `2.1.0-rc1`, `2.1`, `2.1.0+build` y la cadena vacía devuelven un error con código
  `VERSION_INVALIDA`, sin lanzar excepción; `2.10.0` es mayor que `2.9.0`; `toString` de una versión
  válida devuelve el texto de entrada.

- [ ] **CA-3**: `CodigoMotivo` tiene exactamente los 9 códigos de `domain-entities.md`
  ```bash
  for c in DESCRIPTOR_FALTA_CAMPO DESCRIPTOR_CAMPO_DESCONOCIDO DESCRIPTOR_SIN_ARCHIVO VERSION_INVALIDA RANGO_INVALIDO CORE_EQUIVOCADO VERSION_INCOMPATIBLE PLUGIN_REPETIDO PLUGIN_AUSENTE; do grep -rqw "$c" verificador/src/main/java || echo "falta $c"; done
  ```
  Esperado: no imprime nada.

- [ ] **CA-4**: el bytecode es de Java 21 aunque la máquina tenga JDK 25 (lectura de vuelta del `.class`)
  ```bash
  javap -v verificador/build/classes/java/main/io/synkra/verificador/Version.class | grep 'major version'
  ```
  Esperado: `major version: 65`.

---

## Plan de pruebas

- `VersionTest` parametrizada con los casos de CA-2.
- Prueba negativa: `parsear(null)` devuelve `VERSION_INVALIDA`, no `NullPointerException`.

**Rojo primero:** escribe `VersionTest` antes que `Version`, corre CA-1 y registra que falla.
Después, el mismo comando pasando.

---

## Notas

- Paquete: `io.synkra.verificador`.
- `Version.parsear` devuelve `Resultado<Version>`. Un texto inválido nunca lanza excepción.
- Si existe `HumoMotoresTest` de U01-T02, puedes borrarlo.
