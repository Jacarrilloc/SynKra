# U01 · Entidades del dominio

Todas son **valores inmutables** (records de Java), sin identidad ni persistencia: U01 no
guarda nada.

| Entidad | Campos | Invariantes |
|---|---|---|
| `Version` | `mayor`, `menor`, `parche` (enteros ≥ 0) | Solo `MAYOR.MENOR.PARCHE` (Q1); orden total por los tres campos |
| `RangoVersion` | `minimo: Version` (incluido), `maximo: Optional<Version>` (excluido), `texto` original | `minimo < maximo` cuando hay máximo |
| `Descriptor` | `nombre`, `version: Version`, `core` (nombre del core), `coreCompatible: RangoVersion`, `etiquetas: Map<String,String>`, `descripcion?` | `nombre` y `core` cumplen `^[a-z0-9]+(-[a-z0-9]+)*$`; claves de etiqueta igual; ningún campo desconocido (Q4) |
| `Seleccion` | `core` (nombre), `versionCore: Version`, `plugins: List<Descriptor>` | Puede estar vacía de plugins (caso válido del PRD §11.1) |
| `Motivo` | `codigo` (tabla de abajo), `plugin?`, `mensaje`, `campo?` | El mensaje siempre es legible por una persona |
| `Alternativa` | `tipo` (`PLUGIN` o `CORE`), `nombre`, `version` | Solo existe si la sugerencia es compatible de verdad |
| `VeredictoPlugin` | `descriptor`, `compatible: boolean`, `motivos`, `alternativas` | Si `compatible`, sin motivos |
| `Veredicto` | `global: APROBADA` o `RECHAZADA`, `porPlugin: List<VeredictoPlugin>`, `motivosGlobales` | `APROBADA` si y solo si no hay ningún motivo |
| `Catalogo` (interfaz) | `versionesCore(nombre)`, `versionesPlugin(core, plugin)`, `descriptor(core, plugin, version)` | Solo lectura; la implementa U04, y las pruebas con datos fijos |
| `Caso` (testFixtures) | `nombre`, `grupo`, `seleccion`, `catalogo`, `esperado: Veredicto` resumido (global + códigos) | Uno o más por grupo del PRD §11.1 |

## Códigos de motivo

| Código | Cuándo | Lo detecta |
|---|---|---|
| `DESCRIPTOR_FALTA_CAMPO` | Falta `nombre`, `version`, `core` o `coreCompatible` | `LectorDescriptor` |
| `DESCRIPTOR_CAMPO_DESCONOCIDO` | Hay un campo fuera de la lista (Q4) | `LectorDescriptor` |
| `DESCRIPTOR_SIN_ARCHIVO` | El JAR no trae `synkra-plugin.yaml` | `LectorDescriptor` |
| `VERSION_INVALIDA` | Una versión no es `N.N.N` | `Version.parsear` |
| `RANGO_INVALIDO` | El rango no tiene una de las cuatro formas, o `minimo ≥ maximo` | `RangoVersion.parsear` |
| `CORE_EQUIVOCADO` | `descriptor.core ≠ seleccion.core` (P4) | `Verificador` |
| `VERSION_INCOMPATIBLE` | `versionCore` fuera de `coreCompatible` | `Verificador` |
| `PLUGIN_REPETIDO` | Dos descriptores con el mismo `nombre` (Q5) | `Verificador` |
| `PLUGIN_AUSENTE` | La selección pide un plugin que el catálogo no tiene | `Verificador` (con `Catalogo`) |
