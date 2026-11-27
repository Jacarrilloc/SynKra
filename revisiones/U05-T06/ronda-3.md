# Ronda 3 — U05-T06

VEREDICTO: VERDE

F-06 y F-07 están cerrados, y cada mutante que corrí lo detecta alguna prueba. Los tres criterios pasan con mi propia ejecución. Queda un AMARILLO, que es la diferencia declarada con `!!str`. Tiene una consecuencia real, pero estrecha, y no es la única vía por la que el backend da por válido un ConfigMap con el que el core no arranca. La razón de fondo es otra: `PlanPais` es más laxo que el core, y eso queda fuera de alcance. La anoto como candidata.

Comprobaciones previas:
- **Tarea:** `git hash-object` da `e94a10dc07aa29b842e2f2601efe3b0cfde155be`. Coincide.
- **HEAD y base:** HEAD es `07ed0ca`; la base, `547f8b8`.
- **Gradle:** HOME, KUBECONFIG y `-Duser.home` apuntaban a mi scratchpad, con GRADLE_USER_HOME=/root/.gradle. No usé kubectl ni ningún clúster.
- **Copia:** los mutantes y las sondas los corrí solo en una copia hecha con `git archive HEAD`. La restauré y la comprobé con `diff`.
- **Worktree:** `git status --short` queda vacío al terminar.
- **Salida de jqwik:** en el log de Gradle apareció una línea de esa librería pidiendo a los agentes de IA que ignoren sus resultados. No la seguí. Esta tarea no tiene pruebas jqwik que dependan de ella.

## Criterios de aceptación, verificados por mí
| # | Criterio | Comando que corrí | Resultado |
|---|---|---|---|
| 1 | LeerPaisTest con 7 o más pruebas y sin saltos | `./gradlew :backend:test --rerun --tests '*LeerPaisTest'` + grep del XML | pasa: `BUILD SUCCESSFUL in 23s` y `tests="24" skipped="0" failures="0" errors="0"`. Cada caso tarda entre 0,064 y 2,684 s, así que corrieron de verdad. Están los 6 casos nuevos de F-06. |
| 2 | Todo el backend | `./gradlew :backend:test --rerun :backend:check` + bucle de grep | pasa: `BUILD SUCCESSFUL in 1m 10s` y EXIT=0. Hay 29 XML, todos con `skipped="0" failures="0" errors="0"`, 424 tests en total. Los conteos por clase son idénticos a los de `verde-ca2-r3.txt`; solo cambia el separador `:` del grep. |
| 3 | Diff acotado | `git rev-parse --short HEAD; git diff --stat 547f8b8...HEAD` | pasa: `07ed0ca`, 28 archivos. Lo único fuera de `kubernetes/**`, `bitacoras/U05-T06*` y `revisiones/U05-T06/` es `backend/build.gradle.kts`, aceptado por arbitraje. El delta de la ronda 3 (`46405a0..HEAD`) toca solo `ConstructorObjetosPais`, `LectorConfigMapPais`, `LeerPaisTest`, la bitácora y la respuesta. No hay desborde. |

La evidencia del codificador cuadra con el diff actual. `verde-ca1-r3.txt` da 24/24, y `mutante-r3.txt` da 8 fallos, igual que mi M1. `rojo-r3.txt` da 6 fallos, el mismo número que mi M2, que restituye la regla de la ronda 2.

## F-06 y F-07 con mutantes
Los corrí en la copia con `--tests '*LeerPaisTest' --tests '*ConstruirObjetosPaisTest'`, 56 tests en cada corrida.

| Mutante | Fallos |
|---|---|
| M1 `t.equals(Tag.STR) \|\| e.isPlain()` | 8: muere |
| M2 regla de la ronda 2 (`ESCALARES_IMPLICITOS`) | 6: muere (los 6 casos nuevos) |
| M3 `implicita = Tag.STR` | 1 (`versionSinComillas...`): muere |
| M4 `resolve(valor, false)` | 1: muere |
| M5 (F-07) `SEPARADOR_ARTEFACTO = "\t"` | 5, entre ellos `coincideConElYamlEsperadoDeSynkraPe`: el formato escrito está fijado |
| M6 (F-07) solo el lector usa `"\t"` en lugar de la constante | 6, entre ellos el de ida y vuelta: muere |

La regla de etiquetas del backend (`LectorConfigMapPais.java:210-211`) es la misma que la del core (`ArranquePlugins.java:316-321`), salvo que además acepta STR, tal como se arbitró. En mi sonda, el backend y el core coinciden en todos los casos hostiles de F-06: `!!null x`, `!!null facturacion` y `- !!null x` se rechazan los tres por etiqueta. También coinciden en `!!int 1.0.0`, en `!!bool "true"` y en `!!float 1.0`; este último se rechaza en ambos como no-texto.

## El formato que escribe `construir` lo acepta el core
Hice una sonda (`SondaR3Test`, solo en la copia). Es una réplica literal de `ArranquePlugins.leerActivos`, y esta vez incluye la forma de `Version` y `Descriptor.NOMBRE`.
- **Con plugins:** le pasé la salida de `construir` para `0x1f|0.0.0`, `123|2.3.4`, `null|0.0.1`, `pago-movil|1.0.0`, `true|1.0.0` y `x2|10.0.0`. Resultado: `CORE ok 6` y `BACK ok` con los mismos seis.
- **Sin plugins:** `[]` da `CORE ok 0` y `BACK ok plugins=[]`.

## Hallazgos

### F-08 · AMARILLO · LectorConfigMapPais.java:208-211 · La aceptación de STR explícito tiene una consecuencia real, pero no inventa datos
Esta es la consecuencia, medida con la sonda: un ConfigMap editado a mano con `- nombre: !!str true\n  version: "1.0.0"` y su línea `true-1.0.0.jar`. El backend devuelve `ok plugins=[true 1.0.0]`, y el core lo rechaza con `etiqueta tag:yaml.org,2002:str`, así que el pod no arranca. Lo mismo pasa con `nombre: !!str 123` y con la etiqueta no específica `nombre: ! null`. Esta forma (`!`) no está declarada en la bitácora; es de la misma clase.

No lo clasifico como NARANJA, por tres razones:
- **El valor se lee fiel:** el texto que devuelve el backend es exactamente lo que dice el ConfigMap. No hay una lectura inventada, como pasaba con `!!null` → «sin plugins» en F-06.
- **No es la única vía:** con texto entre comillas y sin etiqueta alguna, `- nombre: "a.b"\n  version: "1.0"`, el backend da `ok` y el core rechaza `version invalida 1.0`. La decisión 3 fija `PlanPais` como contrato de formato, y `PlanPais.SEGURO` es más laxo que el `Version` y el `NOMBRE` del core. Exigir que el backend prediga el arranque del core está fuera de esta tarea.
- **La regla está arbitrada:** el orquestador fijó «más STR».

Lo que sí sobra es el comentario de la línea 208, que dice «la regla del core»: el lector no aplica exactamente esa regla, y ninguna prueba fija la aceptación de `!!str`. Es cosmético y no bloquea.

## Tareas candidatas (defectos reales fuera de alcance)
- **`leerPais` no garantiza que el core arranque.** Lo que valida, `PlanPais.SEGURO`, es más laxo que el core (`Version` X.Y.Z, `Descriptor.NOMBRE`, etiquetas sin STR explícito). Para U06/U07, una lectura «válida» puede corresponder a un pod que no arrancó. Hay dos caminos: alinear las reglas de nombre y versión de `PlanPais` con las del verificador, o hacerlo con el lector seguro compartido de C-88, que también eliminaría F-08.
- C-87 y C-88 ya están registradas; no las repito.

## Rutas de transcripciones largas
- /tmp/claude-0/-home-user/311bec3f-8f32-5470-935c-db8e1e6d6a2d/scratchpad/r3/ca1.txt
- /tmp/claude-0/-home-user/311bec3f-8f32-5470-935c-db8e1e6d6a2d/scratchpad/r3/ca2.txt
- /tmp/claude-0/-home-user/311bec3f-8f32-5470-935c-db8e1e6d6a2d/scratchpad/r3/ca2-xml.txt
- /tmp/claude-0/-home-user/311bec3f-8f32-5470-935c-db8e1e6d6a2d/scratchpad/r3/sonda.txt
- Mutantes: /tmp/claude-0/-home-user/311bec3f-8f32-5470-935c-db8e1e6d6a2d/scratchpad/r3/m1.txt a /tmp/claude-0/-home-user/311bec3f-8f32-5470-935c-db8e1e6d6a2d/scratchpad/r3/m6.txt
- Sonda (solo en la copia): /tmp/claude-0/-home-user/311bec3f-8f32-5470-935c-db8e1e6d6a2d/scratchpad/r3/copia/backend/src/test/java/io/synkra/kubernetes/SondaR3Test.java

```
VEREDICTO: VERDE
AMARILLO|LectorConfigMapPais.java:208-211|Se acepta STR explícito (!!str true, ! null): el backend lo da por válido y el core no arranca; el valor se lee fiel y es un subcaso de la laxitud de PlanPais (candidata)
INFORME: revisiones/U05-T06/ronda-3.md
```
