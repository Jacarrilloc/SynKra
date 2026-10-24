# Reglas de negocio — U-agent

Cada regla indica la respuesta que la origina y la historia que la prueba. Códigos de salida de
`analyze`: `0` localizado, `1` error, `2` abstención. Códigos de `propose`: `0` PR creado,
`1` cualquier fallo.

## Entrada: el archivo del cambio

| Id | Regla | Origen | Historia |
|---|---|---|---|
| BR-CH1 | El `id` es el nombre del archivo sin extensión y cumple `[a-z0-9][a-z0-9-]*`. Si no, error `1` | Q3-A | US-1.1 |
| BR-CH2 | El YAML debe ser un único documento con campos `country`, `topic`, `text` y, opcionalmente, `reference`. Un campo desconocido es error `1` que lo nombra | Supuesto 8 | US-1.1 |
| BR-CH3 | `topic` y `text` ausentes o vacíos: error `1` que nombra el campo | Q5-A | US-1.1 |
| BR-CH4 | `topic` fuera de `document`, `consent`, `retention`: error `1` que lista los valores válidos | Q5-A | US-1.1 |
| BR-CH5 | `country` ausente o vacío: **abstención** (código `2`), no error. El informe de traspaso dice que el cambio no declara país | US-4.1 | US-4.1 |
| BR-CH6 | `country` presente pero que, recortado, no es un código de dos letras: error `1` | Q1-B | US-1.1 |
| BR-CH7 | El país se normaliza (recortar y minúsculas) antes de comparar. `CO`, ` co ` y `co` son el mismo país | Supuesto 8, Q11 | US-1.4 |
| BR-CH8 | La huella es el SHA-256 de los bytes del archivo | Supuesto 10 | US-1.4 |

## Inventario de plugins

| Id | Regla | Origen |
|---|---|---|
| BR-IN1 | El inventario son los `plugins/*/plugin.yaml` del repositorio, leídos con el `ManifestReader` de `core` | Application Design F2-B |
| BR-IN2 | Un manifiesto inválido es error `1` que nombra el archivo y el campo; el agente no continúa con un inventario roto | Suposición |
| BR-IN3 | El inventario se ordena por ruta de plugin (resultado determinista) | US-1.4 |

## Localizador (determinista)

| Id | Regla | Origen | Historia |
|---|---|---|---|
| BR-LC1 | Para cada plugin: si su `country` es distinto del país del cambio → `descartado` con motivo «país del cambio (xx) distinto del país del plugin (yy)» | Requisitos Q4-C | US-1.3 |
| BR-LC2 | Si el país coincide pero el tema no está en los `topics` del plugin → `descartado` con motivo «el plugin no cubre el tema» | Requisitos Q4-C | US-1.3 |
| BR-LC3 | Si el país coincide y el tema está cubierto → `afectado`, con el punto de extensión de la correspondencia fija tema a punto | U-core Q1-A | US-1.4 |
| BR-LC4 | El localizador **no lee** `text` ni `reference`. Su resultado depende solo de país, tema e inventario | P5, RF-41 | US-4.2 |
| BR-LC5 | La tabla lista **todos** los plugins del inventario, afectados y descartados, cada uno con motivo (P6) | PRD P6 | US-1.3 |
| BR-LC6 | Si ningún plugin queda `afectado`, el resultado es abstención. Motivos posibles: «no existe plugin para <país>» (nadie tiene ese país) o «ningún plugin del país cubre el tema» | Requisitos RF-33 | US-4.1 |
| BR-LC7 | Evidencia de un plugin afectado: la ruta `sourceFile` de su manifiesto para ese punto y `plugins/<dir>/plugin.yaml`. Cada ruta debe existir en el árbol y estar versionada (`git ls-files --error-unmatch`). Si falta alguna, error `1` | Requisitos NFR-10 | US-1.8 |
| BR-LC8 | El resultado de una misma entrada (cambio, inventario) es idéntico en cada ejecución; la marca de tiempo del informe no entra en el cálculo | RF-31 | US-1.4 |

## Informe

| Id | Regla | Origen | Historia |
|---|---|---|---|
| BR-RP1 | `analyze` escribe `informes/<id>.md` (o `SYNKRA_REPORT_DIR`) y nada más en el árbol de Git. El estado del presupuesto y el log van a `agent/state/` y `agent/logs/`, ignorados por Git | RF-30 | US-1.2 |
| BR-RP2 | El informe tiene las secciones de `domain-entities.md`. Las filas de veredictos comienzan con la barra de tabla y `plugins/`; las palabras de veredicto son `afectado` y `descartado` | Q10-A | US-1.3 |
| BR-RP3 | El informe no contiene el texto del cambio (Q6-B): solo ruta y huella | Q6-B | US-4.2 |
| BR-RP4 | En abstención, el informe incluye la frase «no puedo localizar» y la sección de traspaso con `cambio original`, `plugins revisados`, `motivo de abstención` y `tokens gastados` (que es `0`: no se llama al modelo) | NFR-10 | US-4.1 |
| BR-RP5 | Volver a ejecutar `analyze` con el mismo id sobrescribe el informe de ese id | Suposición | US-1.4 |

## Narrador y modelo

| Id | Regla | Origen | Historia |
|---|---|---|---|
| BR-NR1 | Todo uso del modelo pasa por el puerto `LlmProvider`. Ninguna clase fuera del adaptador `claude` conoce la API de Anthropic | AUTONOMIA-02 | US-1.5 |
| BR-NR2 | El modelo recibe solo la tabla de veredictos, el país y el tema (Q7-B). Nunca el texto ni la referencia del cambio | Q7-B | US-4.2 |
| BR-NR3 | El prompt del sistema declara que el contenido recibido son datos y que el modelo solo redacta un resumen de 3 a 5 frases; no puede cambiar veredictos ni decidir destinos | P5 | US-1.5 |
| BR-NR4 | La respuesta del modelo se inserta únicamente en la sección «Narrativa». No se interpreta ni se ejecuta | P5 | US-4.2 |
| BR-NR5 | En abstención no se llama al modelo | Ahorro de gasto | US-4.1 |
| BR-NR6 | Si la llamada falla (red, error de la API), el informe se escribe con la nota «narrativa no disponible» y `analyze` termina con `0` | Q8-A | — |
| BR-NR7 | `LLM_PROVIDER` solo admite `claude` y `fake`; otro valor es error `1`. Por defecto, `fake` | Supuesto 9 | US-4.4 |
| BR-NR8 | El adaptador `claude` toma la clave de `ANTHROPIC_API_KEY_FILE`; si falta y se necesita, error `1` sin llamar a la red. La clave nunca se imprime ni se registra | Requisitos RF-63 | — |
| BR-NR9 | El adaptador `fake` devuelve un texto fijo que contiene la palabra `fake` y conteos de tokens deterministas | Requisitos RF-40 | US-1.5 |

## Presupuesto

| Id | Regla | Origen | Historia |
|---|---|---|---|
| BR-BU1 | El presupuesto diario por defecto es 5.000 tokens y `LLM_MAX_TOKENS` es 300 | Q9-C | US-4.3 |
| BR-BU2 | Antes de cada llamada se reserva `estimación del prompt + LLM_MAX_TOKENS` (estimación: un token por cuatro caracteres, redondeado hacia arriba). Si lo gastado hoy más la reserva supera el límite, el resultado es «presupuesto agotado» y **no** se llama al modelo | P4, RF-43 | US-4.3 |
| BR-BU3 | Con `DAILY_TOKEN_BUDGET=0` siempre hay «presupuesto agotado» | RF-43 | US-4.3 |
| BR-BU4 | Con presupuesto agotado, `analyze` escribe el informe con los veredictos, la narrativa «presupuesto agotado», imprime «presupuesto agotado» y termina con `0` | D12 | US-4.3 |
| BR-BU5 | Tras una llamada se suman al día los tokens reales de entrada y salida. El día se mide en UTC | Supuesto 1 | US-4.3 |
| BR-BU6 | Cada ejecución (también las de abstención o sin llamada) añade una línea a `agent/logs/tokens.log`: marca de tiempo, ejecución, cambio, proveedor, entrada, salida y total | Supuesto 3, RF-44 | US-4.3 |
| BR-BU7 | La evaluación completa del dataset con `claude` puede agotar 5.000 tokens en un día; para esa corrida manual se sube `DAILY_TOKEN_BUDGET` de forma explícita | Q9-C | US-4.5 |

## `propose`

Orden de comprobaciones. La primera que falla termina con `1` y no deja nada a medias.

| Id | Regla | Origen | Historia |
|---|---|---|---|
| BR-PR1 | Debe existir `informes/<id>.md` y ser legible. Si no, error `1` | Q9-C | US-1.6 |
| BR-PR2 | El informe debe ser de resultado `localizado`. Un informe de abstención da error `1` | RF-33 | US-1.6 |
| BR-PR3 | El árbol de trabajo debe estar limpio, ignorando archivos sin versionar bajo `informes/` y `cambios/`. Si no, error `1` | Supuesto 4 | US-1.8 |
| BR-PR4 | Si la rama `synkra/<id>` existe en local o en el remoto, error `1` que nombra la rama y, si hay, el PR abierto. Nunca se modifica una rama existente | Q10-A | US-1.7 |
| BR-PR5 | `propose` hace `git fetch origin main` y crea la rama desde `origin/main` | Supuesto 4 | US-1.7 |
| BR-PR6 | `propose` relee `cambios/<id>.yaml`. Si no existe, error `1`. Usa lo que haya, sin exigir que su huella coincida con la del informe (F2-B) | F2-B | US-1.6 |
| BR-PR7 | Por cada plugin `afectado` del informe, escribe `plugins/<dir>/changes/<id>.md` con el contenido de `ChangeRequestFile`, incluidas ambas huellas y `coinciden: sí/no` | D13, F2-B | US-1.8 |
| BR-PR8 | El texto del cambio va en un bloque delimitado por una valla más larga que cualquier secuencia de comillas invertidas del texto; el agente nunca lo interpreta | P5 | US-4.2 |
| BR-PR9 | El commit añade solo las rutas `plugins/<dir>/changes/<id>.md` de los plugins afectados. Ninguna otra ruta | AUTONOMIA-01 | US-1.8 |
| BR-PR10 | Mensaje de commit: `synkra: solicitud de cambio <id> (<tema>, <país>)` | Supuesto 5 | US-1.7 |
| BR-PR11 | Empuja solo `origin synkra/<id>` | AUTONOMIA-01 | US-1.7 |
| BR-PR12 | Crea el PR con `--draft`, base `main` y rama `synkra/<id>`. Título y cuerpo según `domain-entities.md`. El cuerpo no incluye el texto del cambio | Supuesto 5 | US-1.7 |
| BR-PR13 | La descripción del PR usa el narrador con las mismas reglas BR-NR y BR-BU. Si no hay narrativa, el cuerpo la omite y lo indica | Q8-A | US-1.7 |
| BR-PR14 | Si falla un paso después de crear la rama, el agente vuelve a la rama original, deja la rama nueva (no existe operación de borrado permitida) y el error indica que debe eliminarla una persona | Lista blanca | US-1.7 |
| BR-PR15 | Salida: el enlace del PR. Código `0` | RF-34 | US-1.7 |
| BR-PR16 | `propose` nunca ejecuta `merge`, aprueba, marca el PR como listo, ni toca ramas distintas de `synkra/<id>` | AUTONOMIA-01 | US-1.11 |

## Lista blanca de comandos

Los procesos se lanzan con argumentos separados (sin intérprete de órdenes). Todo lo que no esté
en la tabla se deniega.

| Id | Comando permitido | Restricción de argumentos |
|---|---|---|
| BR-WL1 | `git rev-parse` | Solo lectura |
| BR-WL2 | `git status --porcelain` | Solo lectura |
| BR-WL3 | `git log --oneline -n <número>` | Solo lectura |
| BR-WL4 | `git cat-file -e <referencia>:<ruta>` | Solo lectura |
| BR-WL5 | `git ls-files [--error-unmatch] <ruta>` | Solo lectura |
| BR-WL6 | `git branch --list synkra/<id>` | Solo lectura |
| BR-WL7 | `git ls-remote --heads origin synkra/<id>` | Solo lectura |
| BR-WL8 | `git fetch origin main` | Argumentos exactos |
| BR-WL9 | `git checkout -b synkra/<id> origin/main` | El nombre de rama debe cumplir `synkra/[a-z0-9][a-z0-9-]*` |
| BR-WL10 | `git checkout <rama original>` | Solo para volver (BR-PR14) |
| BR-WL11 | `git add <ruta>...` | Cada ruta cumple `plugins/<dir>/changes/<id>.md` |
| BR-WL12 | `git commit -m <mensaje>` | Sin otras opciones (sin `--no-verify`, sin `--amend`) |
| BR-WL13 | `git push origin synkra/<id>` | Sin `--force`, sin referencias con dos puntos |
| BR-WL14 | `gh pr create --draft --base main --head synkra/<id> --title <t> --body-file <archivo>` | `--draft` obligatorio |
| BR-WL15 | `gh pr view <rama>` y `gh pr list --head synkra/<id>` | Solo lectura |
| BR-WL16 | Cualquier otro comando, en particular `git merge`, `git rebase`, `git reset`, `git push` a otra rama, `gh pr merge`, `gh pr review`, `gh pr ready`, `gh pr close`, `gh api`, `kubectl`, `helm` | **Denegado**. Se registra el intento y el servicio falla con error `1` |

## Evaluación y red-teaming

| Id | Regla | Origen | Historia |
|---|---|---|---|
| BR-EV1 | El dataset son 20 archivos `eval/dataset/<id>.yaml` con país en mayúsculas: 10 `CO`, 8 `PE`, 2 de un país sin plugin; cada uno con `eval/expected/<id>.json` | F1-B, RF-50 | US-4.5 |
| BR-EV2 | La evaluación ejecuta `analyze` con `LLM_PROVIDER=fake` y `SYNKRA_REPORT_DIR=eval/out` | AUTONOMIA-02 | US-4.5 |
| BR-EV3 | `score.py` lee las filas que comienzan con `plugins/` y la línea `resultado:` de cada informe | Q10-A | US-4.5 |
| BR-EV4 | `recall` = aciertos / (aciertos + omitidos) sobre los plugins esperados como afectados en todos los casos. `precision` = aciertos / (aciertos + sobrantes). `abstention_rate` = abstenciones / casos. `abstention_accuracy` = abstenciones correctas / abstenciones esperadas | PRD §10 | US-4.5 |
| BR-EV5 | Umbrales: recall > 0,90, precisión > 0,70, tasa de abstención > 0 | NFR-06 | US-4.5 |
| BR-RT1 | Pruebas de red-teaming con `fake`: (1) texto de inyección en el cambio no altera los veredictos ni llega al modelo (el prompt capturado no contiene el texto); (2) sin país → abstención con código `2`; (3) país sin plugin → abstención; (4) presupuesto 0 → «presupuesto agotado» y cero llamadas al adaptador; (5) texto con comillas invertidas no rompe el bloque delimitado del archivo de solicitud | RF-52 | US-4.1, US-4.2, US-4.3 |
| BR-RT2 | El adaptador `claude` se prueba contra un servidor HTTP local de prueba; ninguna prueba toca `api.anthropic.com` | AUTONOMIA-02, NFR-08 | US-4.4 |

## Límite de autonomía (AUTONOMIA-01)

| Id | Regla | Historia |
|---|---|---|
| BR-AU1 | `main` exige al menos una revisión aprobatoria (configuración de GitHub, comprobable con `gh api`) | US-1.9 |
| BR-AU2 | El token del agente solo puede crear ramas `synkra/*` y abrir PR en borrador; una operación fuera de eso es rechazada por GitHub | US-1.10 |
| BR-AU3 | El agente no puede fusionar ni aprobar: ni la lista blanca ni el token lo permiten | US-1.11 |
| BR-AU4 | La ServiceAccount `agent` no tiene permisos de escritura en el clúster; el agente no usa `kubectl` | US-1.12 |
| BR-AU5 | La declaración del trade-off de privacidad del modo `claude` está en la sección «perímetro» del README | US-4.6 |

La implementación de BR-AU1, BR-AU2 y BR-AU4 pertenece a Infrastructure Design de esta unidad.

## Cumplimiento de reglas obligatorias

| Regla | Estado |
|---|---|
| AUTONOMIA-01 | Cumple. BR-PR9, BR-PR11, BR-PR16, BR-WL1 a BR-WL16 y BR-AU1 a BR-AU4 |
| AUTONOMIA-02 | Cumple. BR-NR1, BR-BU1 a BR-BU4, BR-RT1, BR-RT2 |
| AUTONOMIA-03 | Cumple en este diseño; cada regla tiene una historia o una prueba que la comprueba y los planes de tareas nombrarán el comando |
