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
- **Límites del lector** (constantes `MAX_BYTES`, `MAX_ALIAS`, `MAX_PROFUNDIDAD` de `LectorDescriptor`;
  superarlos es `DESCRIPTOR_INVALIDO`): tamaño máximo 64 KB (65536 bytes), como máximo 10 alias a colecciones y
  anidación máxima de 32 niveles (se cuenta con los eventos del parser antes de componer).
- **Tipos de los campos** (esquema Core de YAML): `nombre`, `core`, `descripcion` y los valores de `etiquetas`
  exigen un escalar de texto; un escalar plano que YAML resuelve como bool, int, float o nulo es del tipo
  equivocado (`DESCRIPTOR_INVALIDO`), salvo el nulo en un campo obligatorio (`DESCRIPTOR_FALTA_CAMPO`) o en
  `descripcion` (cuenta como ausente). `version` y `coreCompatible` pasan su texto a `Version`/`RangoVersion`
  (`version: 2.1` da `VERSION_INVALIDA`); una etiqueta explícita que no sea `str` (`!!int 1.0.0`) es
  `DESCRIPTOR_INVALIDO`. Una clave repetida (en la raíz o en `etiquetas`) es `DESCRIPTOR_INVALIDO`.
  Las claves de `etiquetas` siguen la regla de `nombre`: una clave que YAML resuelve como bool, int, float o nulo
  es `DESCRIPTOR_INVALIDO`. Una etiqueta explícita igual a la que YAML resolvería sola (`!!int 1`) se trata como
  el escalar plano (`version: !!int 1` da `VERSION_INVALIDA`). `aYaml` y `desdeYaml` usan el mismo esquema, así
  que los textos ambiguos (`~`, `007`, `True`) salen entre comillas y vuelven iguales.
- **Etiqueta explícita en `etiquetas`**: una etiqueta explícita que no sea `str` en una clave o en un valor de `etiquetas` (`{!!int pais: pe}`, `{pais: !!int pe}`) es `DESCRIPTOR_INVALIDO`, a diferencia de los campos (`version: !!int 1` da `VERSION_INVALIDA`).

## R-V4 Verificación de una selección
Para cada plugin de la selección, en este orden, acumulando motivos (a lo sumo uno por plugin, ver abajo):
1. `PLUGIN_REPETIDO` si su `nombre` ya apareció (motivo global, Q5).
2. `PLUGIN_AUSENTE` si el `Catalogo` no tiene ese plugin y versión en la carpeta del core.
3. `CORE_EQUIVOCADO` si `descriptor.core ≠ seleccion.core`.
4. `VERSION_INCOMPATIBLE` si `!coreCompatible.incluye(versionCore)`.

El plugin se busca en la carpeta que declara su propio descriptor (`descriptor.core`), no en la de
`seleccion.core`: así un plugin de otro core da `CORE_EQUIVOCADO` y no `PLUGIN_AUSENTE`. Los pasos 2 a 4
se evalúan en orden y el primero que falla corta los siguientes para ese plugin (a lo sumo un motivo por
plugin). La primera aparición de un `nombre` se verifica normal; las apariciones repetidas no entran en
`porPlugin` y dan un único `PLUGIN_REPETIDO` global por nombre.

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
- La alternativa de core se calcula con el `coreCompatible` del plugin **pedido** (el que dio
  `VERSION_INCOMPATIBLE`), entre las versiones registradas del core de la selección.
- Si no existe ninguna de las dos, el motivo conserva código, `plugin` y `campo` y su mensaje termina en
  "no hay una versión compatible registrada".
- Toda alternativa sugerida, aplicada, debe dar `APROBADA` para ese plugin (se comprueba en la suite).
- **Entrada:** el veredicto que recibe `alternativas` debe ser exactamente `verificar(seleccion, catalogo)`
  para esa selección; si no, `IllegalArgumentException`. Así no se aplican dos veces ni sobre otra selección.
- **Invariante (C-22):** un plugin sin `VERSION_INCOMPATIBLE` nunca trae alternativas; `VeredictoPlugin`
  lo rechaza con `IllegalArgumentException`. Un plugin con `compatible = true` no trae alternativas.
- El `global`, los motivos globales, el orden de `porPlugin` y los plugins sin `VERSION_INCOMPATIBLE` no cambian.

## R-V6 Lo que el core usa
El core llama a `Verificador.verificar` con su versión compilada y un `Catalogo` construido con
los descriptores que encuentra en `/plugins` (lo que no está ahí es `PLUGIN_AUSENTE`). No pide
alternativas.
