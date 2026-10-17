# Plan de Functional Design — U-agent

**Insumos:** `unit-of-work.md`, `unit-of-work-story-map.md`, `application-design/` y
`U-core/functional-design/` (vocabulario de temas y correspondencia tema a punto de extensión),
`U-plugins/functional-design/` (manifiestos). Todos aprobados.
**Alcance de la unidad:** `analyze`, `propose`, localizador determinista, informe, narrador,
`LlmProvider` (`claude`, `fake`), presupuesto de tokens, lista blanca de Git, evaluación y
límite de autonomía. **Historias:** 19 (US-1.1 a US-1.12, US-3.1, US-4.1 a US-4.6).

**Ya decidido (no se vuelve a preguntar):**
- La localización es determinista; el modelo solo redacta (requisitos Q4-C).
- Temas cerrados: `document`, `consent`, `retention`; cada uno corresponde a un punto de extensión (U-core Q1-A).
- Ejecutar `propose` es la aprobación; basta con que el informe exista (Application Design Q9-C, F3-A).
- `propose` agrega `plugins/<pais>/changes/<id>.md` y no modifica código (Application Design Q8-A).
- Un PR por cambio, con un archivo por plugin afectado (D13). Informe en Markdown con tabla fija (Q10-A). Presupuesto y log en archivos locales (Q11-A). `git` y `gh` como procesos con lista blanca (Q7-B).
- Códigos de salida: `0` éxito, `1` error, `2` abstención.
- Un cambio sin país es una abstención (US-4.1); un cambio sin `id`, tema o texto es un error de entrada (US-1.1).

Responde escribiendo la letra después de cada `[Answer]:`. Si ninguna opción encaja, elige la
última (Other) y descríbela.

---

## Parte 1: Preguntas

## Question 1
El PRD dice que un cambio puede aplicar a «uno o varios de los países». ¿Cuántos países declara un cambio YAML?

A) Una lista de uno o más países (`countries`)

B) Un solo país por cambio (`country`); un cambio para dos países son dos archivos

X) Other (please describe after [Answer]: tag below)

[Answer]: B

## Question 2
¿Cuántos temas declara un cambio?

A) Un solo tema (`topic`): `document`, `consent` o `retention`

B) Una lista de uno o más temas (`topics`)

X) Other (please describe after [Answer]: tag below)

[Answer]: A

## Question 3
¿De dónde sale el identificador del cambio (por ejemplo `2026-001`)?

A) Del nombre del archivo (`cambios/2026-001.yaml`); el YAML no lleva campo `id`

B) De un campo `id` del YAML, que debe coincidir con el nombre del archivo; si no coincide es un error de entrada

X) Other (please describe after [Answer]: tag below)

[Answer]: A

## Question 4
Un cambio declara dos países y solo uno tiene plugin (por ejemplo, un país con plugin y otro sin él). ¿Cómo responde `analyze`?

A) Resolución parcial: el informe lista como afectados los plugins que sí existen y lista aparte cada país sin plugin como «no puedo localizar». El código de salida es `2` si queda algún país sin resolver, para que el humano lo note. `propose` queda permitido para los plugins resueltos

B) Abstención total: si cualquier país declarado no tiene plugin, el informe es de abstención y `propose` no procede para ese cambio

C) Resolución parcial con código de salida `0` y una sección de advertencias en el informe

X) Other (please describe after [Answer]: tag below)

[Answer]: B

## Question 5
El tema del YAML no pertenece al conjunto cerrado (por ejemplo `taxes`). ¿Qué es?

A) Un error de entrada (código `1`): se informa qué temas son válidos

B) Una abstención (código `2`): «no puedo localizar: tema desconocido», con el informe de hand-off

X) Other (please describe after [Answer]: tag below)

[Answer]: A

## Question 6
¿El informe incluye el texto completo del cambio?

A) Sí, dentro de un bloque de código delimitado (trazabilidad completa, y el texto queda como dato)

B) No: solo el nombre del archivo y la huella (hash) del cambio

X) Other (please describe after [Answer]: tag below)

[Answer]: B

## Question 7
¿Qué recibe el modelo cuando redacta el texto del informe y de la descripción del PR? (En modo `claude` esto sale hacia la API externa; la decisión de requisitos Q10-B no impone límite de contexto, pero aquí se puede elegir.)

A) La tabla de veredictos y el texto del cambio, este último dentro de un bloque delimitado que el prompt del sistema declara como datos, no instrucciones

B) Solo la tabla de veredictos, el tema y los países; el texto libre del cambio no se envía al modelo (menos superficie de inyección y de fuga; la redacción es más genérica)

X) Other (please describe after [Answer]: tag below)

[Answer]: B

## Question 8
La llamada al modelo falla (red caída, error de la API) después de que el análisis determinista terminó. ¿Qué hace `analyze`?

A) Escribe el informe sin la narrativa, con la nota «narrativa no disponible», y termina con código `0` (el veredicto no depende del modelo)

B) Falla con código `1` y no escribe informe

C) Reintenta una vez; si vuelve a fallar, procede como en A

X) Other (please describe after [Answer]: tag below)

[Answer]: A

## Question 9
¿Qué valores por defecto tienen el presupuesto diario y el tamaño máximo por llamada? El PRD pide modelo pequeño y `max_tokens` bajo.

A) Presupuesto diario de 20.000 tokens y `max_tokens` de 400 por llamada

B) Presupuesto diario de 50.000 tokens y `max_tokens` de 800 por llamada

C) Presupuesto diario de 5.000 tokens y `max_tokens` de 300 por llamada (muy estricto; sirve para ver el caso de presupuesto agotado con facilidad)

X) Other (please describe after [Answer]: tag below)

[Answer]: C

## Question 10
`propose` encuentra que la rama `synkra/<id>` ya existe (localmente o en el remoto). ¿Qué hace?

A) Se niega (código `1`), indicando la rama y, si existe, el PR abierto; nunca modifica una rama existente

B) Añade un nuevo commit a la rama existente y actualiza el PR

X) Other (please describe after [Answer]: tag below)

[Answer]: A

## Question 11
El dataset de evaluación son 20 cambios (10 de Colombia, 8 de Perú, 2 de un país sin plugin). ¿Qué contienen?

A) Casos simples de un solo país, salvo: algún cambio con dos países (uno con plugin y otro sin él, si la pregunta 1 lo permite), un cambio con texto de inyección de instrucciones y un cambio de un tema que ningún plugin cubre. Los cuentan dentro de los 20

B) Los 20 son casos simples de un solo país y un solo tema; la inyección y los casos difíciles se cubren solo con las pruebas de red-teaming

X) Other (please describe after [Answer]: tag below)

[Answer]: B

---

## Supuestos que tomo para cerrar huecos (confirma o corrige al aprobar el diseño)

1. **Fecha de presupuesto.** El día del presupuesto se mide en UTC.
2. **Configuración.** Variables: `LLM_PROVIDER`, `DAILY_TOKEN_BUDGET`, `LLM_MAX_TOKENS`, `SYNKRA_STATE_DIR`, `SYNKRA_REPO_ROOT` y `ANTHROPIC_API_KEY_FILE` (la clave se lee de un archivo).
3. **Formato de `tokens.log`.** Una línea por ejecución: marca de tiempo, id de ejecución, proveedor, tokens de entrada, de salida y total.
4. **`propose` y el árbol de trabajo.** Exige árbol limpio y parte de `origin/main` recién obtenido; la lista blanca incluye `git fetch origin main`.
5. **Idioma.** Informe, título y cuerpo del PR y mensajes de commit en español. Título del PR: `[SynKra] <id>: <tema> en <países>`.
6. **Archivo de solicitud de cambio.** Incluye el texto del cambio en un bloque delimitado, el tema, el punto de extensión, los archivos de evidencia, la huella del informe y el estado «pendiente de revisión humana».
7. **Autenticación de GitHub.** El token del agente llega a `gh` por la variable `GH_TOKEN` leída de un archivo (`AGENT_TOKEN_FILE`).

---

## Parte 2: Pasos de generación (tras responder)

- [x] 1. Analizar las respuestas y resolver ambigüedades o contradicciones
- [x] 2. Crear `aidlc-docs/construction/U-agent/functional-design/domain-entities.md`: cambio, veredicto, localización, informe, narrativa, uso de tokens, presupuesto, solicitud de cambio, caso del dataset
- [x] 3. Crear `business-rules.md`: validación del cambio, reglas del localizador, abstención, formato del informe, presupuesto, reglas de `propose`, lista blanca de comandos, reglas de evaluación y de red-teaming
- [x] 4. Crear `business-logic-model.md`: flujos de `analyze`, `propose`, presupuesto, evaluación y los casos de abstención, inyección y presupuesto agotado, con Mermaid validado y alternativa en texto
- [x] 5. Verificar que las 19 historias se pueden satisfacer con el diseño y registrar enmiendas a artefactos aprobados si hacen falta
- [x] 6. Actualizar `aidlc-state.md` y registrar la aprobación en `audit.md`

---

## Preguntas de seguimiento (aclaraciones de las respuestas)

Revisé tus respuestas: Q1 B, Q2 A, Q3 A, Q4 B, Q5 A, Q6 B, Q7 B, Q8 A, Q9 C, Q10 A, Q11 A.
Dos combinaciones necesitan una decisión. Además, una nota: con Q1-B (un solo país por cambio),
la Q4 deja de aplicar porque un cambio no puede declarar dos países; tu respuesta B queda
registrada como «país sin plugin es abstención», que es el comportamiento de un solo país.

### Seguimiento 1 — Q11-A contra Q1-B y los temas cerrados
Q11-A pedía incluir en el dataset «un cambio con dos países» y «un cambio de un tema que ningún
plugin cubre». Con Q1-B no existen cambios de dos países, y con temas cerrados que ambos
plugins cubren, tampoco existe un tema que ningún plugin cubra. De Q11-A solo sobrevive el caso
con texto de inyección. Falta decidir qué casos difíciles entran en su lugar.

## Question F1
¿Qué contiene el dataset de 20 cambios (10 de Colombia, 8 de Perú, 2 de un país sin plugin)?

A) Casos de un solo país y un solo tema, con tres casos difíciles dentro de los 20: un cambio con texto de inyección de instrucciones, un cambio con el país escrito en mayúsculas y con espacios alrededor, y un cambio cuyo tema coincide con el punto de extensión pero con texto largo (para comprobar que el texto no altera el veredicto)

B) Los 20 son casos simples de un solo país y un solo tema; la inyección y los demás casos difíciles se cubren solo con las pruebas de red-teaming

C) Los 20 de la distribución 10/8/2, más 4 casos difíciles adicionales (24 en total), para no desplazar la distribución del PRD

X) Other (please describe after [Answer]: tag below)

[Answer]: B

### Seguimiento 2 — Q6-B contra `propose` (el texto del cambio)
Con Q6-B, el informe no contiene el texto del cambio, solo el nombre del archivo y su huella.
Pero el archivo de solicitud que `propose` escribe en el plugin necesita ese texto (para que
quien implemente sepa qué se pide). `propose` tiene que leerlo otra vez de `cambios/<id>.yaml`.
Entre el análisis y `propose` ese archivo pudo editarse, y entonces el informe describiría un
cambio y el PR llevaría otro.

## Question F2
¿Qué hace `propose` con el archivo del cambio?

A) Lo lee de `cambios/<id>.yaml`. Si no existe, falla (código `1`). Si su huella no coincide con la guardada en el informe, falla (código `1`) y pide volver a ejecutar `analyze`. No hay confirmación interactiva (coherente con F3-A)

B) Lo lee de `cambios/<id>.yaml` y usa lo que haya, sin comprobar la huella; la huella del informe queda escrita en el archivo de solicitud para que quien revise pueda ver si cambió

C) No copia el texto: el archivo de solicitud solo referencia `cambios/<id>.yaml` y la huella (el archivo del cambio debe estar ya en `main`)

X) Other (please describe after [Answer]: tag below)

[Answer]: B
