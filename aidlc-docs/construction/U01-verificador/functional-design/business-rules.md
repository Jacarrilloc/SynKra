# U01 · Reglas de negocio

## R-V1 Versiones (Q1)
- Una versión es `MAYOR.MENOR.PARCHE`, tres enteros sin ceros a la izquierda (`2.01.0` es
  inválida). Se rechazan pre-release y metadatos de build → `VERSION_INVALIDA`.
- Orden: se compara `mayor`, luego `menor`, luego `parche`.

## R-V2 Rangos (Q2): cuatro formas, un solo intervalo `[minimo, maximo)`

| Forma | Ejemplo | Intervalo |
|---|---|---|
| `N.x` | `2.x` | `[2.0.0, 3.0.0)` |
| `N.M.x` | `2.1.x` | `[2.1.0, 2.2.0)` |
| Exacta | `2.1.0` | `[2.1.0, 2.1.1)` |
| Intervalo | `>=2.1.0 <3.0.0`, `>=2.1.0` | `[2.1.0, 3.0.0)`, `[2.1.0, ∞)` |

- Espacios opcionales alrededor de `>=` y `<`; cualquier otra cosa → `RANGO_INVALIDO`.
- `incluye(v)` ⇔ `minimo ≤ v` y (`sin máximo` o `v < maximo`).

## R-V3 Descriptor (Q4)
- Archivo `synkra-plugin.yaml` en la raíz del JAR; si no está → `DESCRIPTOR_SIN_ARCHIVO`.
- Obligatorios: `nombre`, `version`, `core`, `coreCompatible`. Opcionales: `etiquetas`,
  `descripcion`. Cualquier otro campo → `DESCRIPTOR_CAMPO_DESCONOCIDO`.
- Se informan **todos** los motivos de una vez, no solo el primero (para corregir en una vuelta).
- El YAML se lee en modo seguro (sin etiquetas de tipo ni anclas recursivas): es dato de un tercero.

## R-V4 Verificación de una selección
Para cada plugin de la selección, en este orden, acumulando motivos:
1. `PLUGIN_REPETIDO` si su `nombre` ya apareció (motivo global, Q5).
2. `PLUGIN_AUSENTE` si el `Catalogo` no tiene ese plugin y versión en la carpeta del core.
3. `CORE_EQUIVOCADO` si `descriptor.core ≠ seleccion.core`.
4. `VERSION_INCOMPATIBLE` si `!coreCompatible.incluye(versionCore)`.

`global = APROBADA` si no hay ningún motivo; una selección sin plugins es `APROBADA`.

**Determinismo:** el mismo `(Seleccion, Catalogo)` da siempre el mismo `Veredicto`, con los
plugins en el orden recibido. Es lo que permite medir la concordancia motor-core (RNF-05): el
core llama exactamente a esta función.

## R-V5 Alternativas (Q3)
Solo para `VERSION_INCOMPATIBLE`:
- **Plugin:** la versión **más alta** del mismo plugin, en la carpeta del mismo core, cuyo
  `coreCompatible` incluye `versionCore`.
- **Core:** la versión **más baja** registrada del core que está en el `coreCompatible` del plugin
  elegido.
- Si no existe ninguna de las dos, el motivo dice "no hay una versión compatible registrada".
- Toda alternativa sugerida, aplicada, debe dar `APROBADA` para ese plugin (se comprueba en la suite).

## R-V6 Lo que el core usa
El core llama a `Verificador.verificar` con su versión compilada y un `Catalogo` construido con
los descriptores que encuentra en `/plugins` (lo que no está ahí es `PLUGIN_AUSENTE`). No pide
alternativas.
