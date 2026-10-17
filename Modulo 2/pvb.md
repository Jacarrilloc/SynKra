# Product Vision Board — SynKra

**Módulo 2 — AI & Agentic Engineering · Tópicos Especiales en Informática**

**Fecha:** 15 de agosto de 2026

> **Convención de evidencia usada en este documento:**
>
> - **[INTERNO]** — proyección, supuesto o estimación propia. Defendible, no verificada externamente.
> - **[VERIFICAR]** — afirmación sobre el mundo exterior que no logré confirmar con fuente primaria.
> - Toda cifra sin etiqueta lleva fuente y fecha en línea. Los enlaces completos están en
>   `docs/mercado.md` (validación) y `docs/critica.md` (crítica adversarial).

---

> **Ajuste de dirección (17 de octubre de 2026).** Las variantes por país dejan de ser ramas
> de larga vida y pasan a ser **plugins de un micro-kernel**, cada país desplegado como un
> overlay de Kustomize con solo los plugins que necesita. Es la salida que este mismo PVB
> proponía en el riesgo #4. El agente ya no propaga cambios entre ramas: **localiza qué plugin
> y qué punto de extensión** toca un cambio regulatorio y abre un PR en borrador. Para el MVP
> del curso la IA es Claude con tope de gasto, detrás de un adaptador intercambiable por un
> modelo local; en modo Claude la promesa de la sección 3 ("el código nunca sale del
> perímetro") **no se cumple**, y así se declara. El resto del documento se conserva como la
> visión original; el detalle está en `../Modulo 3/specs/prd.md`.

---

## PRODUCTO

**Nombre del producto:** SynKra

**Descripción en una línea (qué hace y para quién):**
Conector auto-hospedado que, ante un cambio de requerimiento **ya interpretado por un humano**,
localiza qué servicios y qué ramas _downstream_ del producto quedan afectados, y entrega
borradores de historias de usuario y de pull requests de propagación — con aprobación humana
obligatoria en cada salida. Para equipos de plataforma que mantienen **un core y N variantes
obligatorias** sobre Kubernetes.

**Tagline interno:** _"El humano decide qué cambia; el agente encuentra dónde."_

**Alcance de la primera iteración (adición a la plantilla, para sustentar viabilidad):**
Un producto core de ejemplo en Java/Spring Boot, dos ramas downstream simuladas (dos
jurisdicciones), un cambio de requerimiento estructurado como entrada, salida en borradores de
tickets + PRs de propagación. Sin interpretación normativa, sin operación del clúster, sin
resolución automática de merges. Todo lo demás es backlog explícito.

---

## 1. PROBLEMA

Una empresa que despliega el mismo producto a varios clientes o países no mantiene un
repositorio: mantiene **un core más N variantes que deben seguir vivas y sincronizadas**.
Ese patrón está documentado como práctica real de industria, no como hipótesis: Santander
declara que más del 80% de sus soluciones globales se extrapolan a mercados locales (Santander
Digital Services, "Software Engineering"; nota de prensa de mayo de 2022, ~16.500
desarrolladores); BBVA agrupó más de 16.000 desarrolladores para reutilizar componentes entre
países (BBVA, "BBVA creates a Software area to drive global scalable solutions"); Nubank opera
plataformas compartidas con marcos regulatorios específicos por país (Umbrex, "Nubank Strategy
and Business Model"). Los vendors de core bancario venden esto como producto: Oracle FLEXCUBE
ofrece clústeres localizados por país y Temenos "Country Model Banks" para 150+ mercados.

Cuando llega un cambio obligatorio —una circular nueva, un requisito de un cliente grande, un
parche de seguridad en el core— aparecen tres costos que hoy nadie cubre:

- **Nadie sabe con certeza a qué variantes aplica.** La respuesta vive en la cabeza de dos o
  tres personas, y esas personas rotan. El error caro no es el falso positivo: es la variante
  que _nadie recordó_ y que se quedó sin el cambio.
- **Escribir el ticket cuesta más que hacerlo.** El Product Owner necesita saber qué servicio,
  qué repositorio y qué rama toca antes de poder redactar una historia útil. Sin eso escribe
  una historia genérica que el desarrollador tiene que volver a investigar.
- **La propagación upstream→downstream se hace a mano, tarde y bajo presión.** Es donde el
  cambio se pierde o se aplica mal.

Es un dolor caro y creciente en el segmento regulado: el Bank Policy Institute reportó que los
bancos pasaron de gastar 9,6% de su presupuesto de IT en compliance en 2016 a 13,4% en 2023,
con un aumento de 61% en horas-empleado dedicadas a cumplir regulaciones ("Survey Finds
Compliance is a Growing Demand on Bank Resources", oct. 2024). El costo total de cumplimiento
de delitos financieros en EE.UU. y Canadá se estimó en USD 61.000 millones, con incrementos
para el 99% de las instituciones (LexisNexis Risk Solutions / Forrester Consulting, "True Cost
of Financial Crime Compliance Study – U.S. and Canada", feb. 2024).

**Lo que NO es el problema.** No es interpretar la norma: eso ya lo hacen Norm Ai, CUBE,
Corlytics y AscentAI, con equipos de analistas humanos y financiación de nueve cifras. No es
desplegar en Kubernetes: eso lo resuelven Argo CD, Kustomize y Kyverno, gratis y bien. El hueco
está exactamente en medio, y a agosto de 2026 sigue abierto: ningún producto público cierra el
bucle **obligación → repositorio/servicio → rama de despliegue** (`docs/critica.md`, sección 4).
El precedente más cercano, Regnology "Ticket-to-Code Writer" sobre Vertex AI (blog de Google
Cloud, 10-sep-2024), va de _ticket_ a código, no de _cambio_ a ticket, y no toca ramas de
despliegue.

**¿Este problema sobrevive a las próximas 2-3 generaciones de modelos foundation?**

**[X] Sí, porque es un problema de WORKFLOW/INTEGRACIÓN, no de OUTPUT**

Un modelo mejor redacta mejor la historia de usuario, pero no sabe cuáles de mis catorce
variantes existen, quién las aprueba, qué rama es protegida ni qué pasó la última vez que
propagamos este tipo de cambio. Ese conocimiento es organizacional y vive en el repositorio y
en el historial de aprobaciones del cliente, no en los pesos del modelo. Cuanto más capaces son
los modelos, **más urgente** se vuelve el problema de acotar dónde pueden escribir.

**Durability Score: 4/5**

No se toma 5 y por la siguiente justificación: la mitad "redactar el borrador" **ya se está
comoditizando** —Atlassian Rovo crea work items desde documentos, GitHub Copilot y Cursor hacen
ticket→PR—. Lo que no se comoditiza es el mapa de localización construido sobre el historial
del cliente. Si mi tesis fuera "genero tickets con IA", el score sería 2.

---

## 2. SEGMENTO TARGET

**¿Para quién es este producto?**

**Beachhead:** equipos de plataforma o de entrega de **5 a 20 ingenieros** en empresas de
software que mantienen **un producto core desplegado como 3 o más variantes obligatorias**
(por país, por regulador o por cliente grande), y que **ya operan Kubernetes con GitOps en
producción**. Vertical inicial: vendors de software financiero, de salud y govtech en Colombia
y LatAm, más fintechs multi-país. Banca corporativa es un cliente posible, no el mercado: el
patrón de variantes obligatorias es el mismo en cualquier industria regulada, y entrar por un
vendor de software es un ciclo de venta mucho más corto que entrar por un banco.

Cuatro señales de que una empresa está en el beachhead:

1. Tienen Argo CD o Flux andando, aunque sea a medias.
2. Tienen más variantes desplegadas que personas que sepan explicar en qué se diferencian.
3. Alguien mantiene, a mano, una hoja de cálculo de "qué cliente tiene qué".
4. Su política de seguridad les impide mandar el código fuente a un SaaS externo.

**NO es para:** productos de una sola variante (no hay nada que propagar), equipos sin
Kubernetes ni GitOps (el producto no tiene dónde engancharse), organizaciones donde nadie tiene
que aprobar nada (ahí el valor de un borrador es cero), ni empresas que ya resolvieron la
variabilidad con parametrización pura tipo Temenos, donde el cambio se activa por configuración
y no por código.

**¿Quién controla el veto de confianza?**

Dos personas, en este orden:

1. **El arquitecto de seguridad / CISO.** Su pregunta es una sola: _"¿el código fuente sale de
   mi perímetro?"_ Si la respuesta no es **"no: el modelo corre en tu clúster y no hay egress
   hacia proveedores de inferencia"**, en banca y en salud la conversación se acabó. Esta
   pregunta es la razón principal de la decisión de modelos locales, y no una preferencia
   técnica.
2. **El tech lead o release manager dueño de las ramas protegidas.** Su pregunta es: _"¿qué
   puede escribir este agente en mi repositorio?"_ La respuesta tiene que ser verificable en
   la configuración del servidor Git, no prometida en una diapositiva.

El Product Owner es el **usuario**, no el veto. Ama recibir el borrador; no puede autorizar la
instalación.

---

## 3. VENTAJA COMPETITIVA PRIMARIA

**[X] Trust** — Ofrecemos reliability/safety/compliance que otros no pueden igualar

Tres capas, en orden de qué tan pronto existen:

1. **El código nunca sale del perímetro (existe desde el día 1).** Inferencia con modelos
   abiertos servidos dentro del clúster del cliente, con NetworkPolicies de egress en allowlist.
   No es una cláusula contractual: es una propiedad observable con `kubectl describe
networkpolicy`. Los competidores RegTech y las herramientas de generación de tickets son SaaS;
   este es el único eje donde un proyecto pequeño gana estructuralmente a uno de USD 1.200
   millones.
2. **El agente no puede mergear, aprobar ni desplegar (existe desde el día 1).** Escribe solo
   en ramas con prefijo reservado (`SynKra/*`) y abre PRs en borrador. Nunca toca ramas
   protegidas, nunca aprueba su propio PR, nunca mueve un ticket más allá del backlog. El peor
   resultado posible de un fallo —incluido un fallo por manipulación— es **un borrador malo que
   un humano tiene que aprobar**.
3. **El mapa de localización validado (se acumula operando).** Cada borrador aprobado, editado o
   rechazado enseña qué obligaciones caen sobre qué servicios y qué variantes en _esa_ empresa.
   Ese conjunto de datos no se compra ni se descarga: es el subproducto de haber operado meses
   dentro del cliente.

**El foso real no es el agente.** El agente es replicable. Lo que no se replica es el permiso
para correr dentro del perímetro más el historial de aciertos acumulado ahí adentro.

**Por qué no elegí Data:** al día 1 no tengo ningún dato propio. La capa 3 es una ventaja
_futura_ y condicional, y presentarla como primaria sería exactamente el humo que este PVB
debe evitar.
**Por qué no elegí Distribution:** no controlo ningún canal. Me cuelgo de Git y del tracker que
ya usa el cliente, que son canales de otros.

---

## 4. ARENA COMPETITIVA

**[X] Disruptor (AI-Disrupted)** — El workflow (propagar un cambio obligatorio a N variantes y
convertirlo en trabajo) existe desde que existe el software multi-cliente. Lo estoy
reimaginando, no inventando.

**Cómo sobrevivo o complemento a los gigantes:**

- **Contra el ecosistema CNCF: no compito, me monto encima.** Argo CD corre en cerca del 60% de
  los clústeres de Kubernetes, el 97% de los encuestados lo usa en producción y tiene un NPS de
  79 (CNCF, "2025 Argo CD End User Survey", 24-jul-2025). Kyverno y OPA (graduados) cubren
  policy-as-code. Reimplementar cualquiera de esas piezas sería una derrota. SynKra consume Argo
  CD y escribe a Git; el clúster converge solo.
- **Contra las RegTech (Norm Ai, CUBE, Corlytics, AscentAI): soy su downstream, no su rival.**
  Norm Ai levantó una Serie C de USD 120 millones a valuación de ~USD 1.200 millones el
  7-jul-2026, liderada por Khosla Ventures, con más de USD 260 millones totales (PR Newswire y
  TechCrunch, 7-jul-2026). CUBE cubre 750+ jurisdicciones con más de 250 analistas humanos. No
  hay ninguna versión de esta historia en la que yo gane interpretando normas mejor que ellos.
  Todos terminan en obligaciones, políticas y controles; **ninguno toca repositorios ni
  Kubernetes**. Su salida es mi entrada.
- **Contra las plataformas de entrega (Harness Worker Agents, Atlassian Rovo, GitHub Copilot):
  tienen la mitad de despliegue, no la de localización multi-variante.** Harness es el flanco
  peligroso, no las RegTech.

**Riesgo honesto de esta arena:** estoy exactamente entre dos frentes bien financiados y el
hueco está abierto porque es difícil y arriesgado, no porque nadie lo haya visto. Si Harness
añade un front regulatorio o una RegTech añade integración con repositorios, mi diferenciación
se reduce a "on-premise, más barato y localizado". Está en la sección 9 y es el riesgo #1.

---

## 5. UX PARADIGM

**[X] Agent** — La IA ejecuta tareas autónomamente **dentro de límites duros**

**Por qué este paradigma:** el agente sí trabaja sin que un humano lo guíe paso a paso —se
dispara por un webhook o por un cambio registrado, recorre repositorios, correlaciona y
redacta—. Eso es un agente, no un asistente. Pero cada una de sus salidas es un **borrador
dirigido a un aprobador con nombre**.

**Qué hace sin preguntar:** leer los repositorios conectados, indexarlos, correlacionar el
cambio de entrada con servicios y variantes, redactar historias de usuario con criterios de
aceptación, crear ramas `SynKra/*`, abrir pull requests en borrador y adjuntar su evidencia.

**Qué no hace nunca:** mergear, aprobar, tocar ramas protegidas, resolver conflictos de merge
por su cuenta, aplicar nada al clúster, mover un ticket más allá del backlog, ni decidir si una
norma aplica.

**Por qué no Autonomous:** porque es ilegal en mi mercado objetivo y porque nadie lo firmaría.
Las obligaciones plenas de alto riesgo del EU AI Act (Reg. UE 2024/1689) aplican desde el
**2 de agosto de 2026**, con exigencia de trazabilidad y **supervisión humana**, y multas de
hasta €35M o 7% de la facturación. En EE.UU. SR 11-7 exige monitoreo humano continuo de
modelos. En Colombia la SFC empuja en la misma dirección. La supervisión humana no es un
disclaimer al pie: **es el producto**, y por eso está en la sección 3 y no aquí.

**Costo honesto de esta decisión:** con humano en el bucle, buena parte del ROI que prometería
una automatización total desaparece. Lo asumo. El valor que queda no es "no hacer el trabajo",
es "no _buscar_ dónde hacerlo".

---

## 6. AI DECISION TRIANGLE

**[X] Capability**

**Trade-offs que acepto:**

- **Sacrifico costo y velocidad al elegir inferencia local.** Un modelo abierto servido en el
  clúster del cliente es más lento y, a baja escala, más caro por token que una API comercial.
  Lo pago porque la confidencialidad es la ventaja competitiva (sección 3): si el código sale
  del perímetro, no tengo producto.
- **Sacrifico capacidad bruta, y esta es la contradicción real de mi diseño.** Modelos abiertos
  del tamaño que cabe en un clúster de cliente rinden por debajo de los frontera. La evidencia
  disponible es dura: en preguntas verificables sobre casos judiciales federales de EE.UU., las
  tasas de alucinación medidas fueron de 58% para GPT-4 y 88% para Llama 2 (Dahl, Magesh,
  Suzgun & Ho, "Large Legal Fictions", _Journal of Legal Analysis_ 16(1), 2024), y aun con RAG
  las herramientas legales comerciales fallaron entre 17% y 33% (Magesh et al., _Journal of
  Empirical Legal Studies_ 22, 2025).
  **Mi respuesta a esa evidencia, y es toda la tesis del producto:** el modelo local **no hace
  esa tarea**. No interpreta derecho ni decide si una norma aplica — eso entra ya resuelto por
  un humano. Hace localización en código y redacción de borradores, con retrieval anclado al
  repositorio real del cliente, que es donde el grounding funciona y donde el error es
  verificable en segundos por quien lo recibe.
  _Salvedad:_ esos benchmarks son de derecho federal estadounidense en inglés y con modelos de
  2023-2024; no son un veredicto sobre modelos abiertos actuales en tareas de código.
  **[VERIFICAR: benchmark propio de localización servicio↔obligación con el modelo elegido]**
- **Sacrifico cobertura.** Prefiero acertar en un tipo de cambio y una familia de servicios que
  opinar mal sobre todo el portafolio.
- **Uso modelos pequeños donde se puede.** Clasificación, filtrado y detección de duplicados con
  modelos baratos; el modelo grande solo para localización y redacción.

**Razón de fondo:** el costo de un borrador equivocado no es el token. Es que un PO apruebe una
historia que manda a un desarrollador a modificar el servicio que no era — y esa confianza se
pierde una sola vez.

---

## 7. MODELO ECONÓMICO

**Modelo de pricing:** **[X] Hybrid Tiered** — licencia auto-hospedada por **producto core
conectado**, con límites crecientes de variantes y repositorios. **[INTERNO]**

La unidad de cobro **no es el asiento**: el valor no crece con cuánta gente mira el tablero,
crece con cuántas variantes hay que mantener sincronizadas.

- **Starter — USD 600/mes:** 1 producto core, hasta 5 variantes, hasta 20 repositorios.
- **Business — USD 2.000/mes:** hasta 20 variantes, SSO, políticas de aprobación
  personalizadas, soporte para el hosting del modelo.
- **Enterprise — a convenir:** despliegue air-gapped, evaluaciones a medida, acuerdo de
  auditoría.

**¿El pricing escala si tienes 10x usuarios?** **[X] Necesita ajuste**

Y quiero ser preciso sobre por qué, porque es el número que más me preocupa: el costo marginal
dominante **no es la inferencia** —la paga el cliente en su propia GPU—, es el **onboarding y
el soporte de una instalación on-premise**. Ese costo escala casi lineal con el número de
clientes. 10x clientes con este modelo es 10x equipo de soporte. La única salida real es un
instalador reproducible (Helm chart + operador) que convierta el onboarding en horas y no en
semanas; hasta que eso exista, el modelo no escala y decir lo contrario sería mentir.

**Economía por cuenta (tier Business) [INTERNO]:**

| Concepto                                                   | Mensual    |
| ---------------------------------------------------------- | ---------- |
| Soporte y onboarding prorrateado                           | $350       |
| Mantenimiento del conector y suite de evaluaciones         | $180       |
| Infraestructura propia (CI de releases, telemetría opt-in) | $70        |
| **Costo total**                                            | **$600**   |
| **Revenue**                                                | **$2.000** |
| **Gross margin**                                           | **70%**    |

_Nota:_ la casilla "costo por usuario/mes" de la plantilla no aplica a este modelo y prefiero
declararlo antes que rellenarla con un número inventado. El costo de GPU vive del lado del
cliente y es su decisión de dimensionamiento. **[VERIFICAR: costo real de servir un modelo
abierto de 7-14B cuantizado para un equipo de este tamaño]**

---

## 8. MÉTRICAS DE ÉXITO

**Métricas de usuario:**

1. **Tasa de borradores de historia aprobados por el PO sin reescritura sustancial**, medida por
   distancia de edición sobre el texto original. Es la métrica que dice si el producto sirve.
   _Target: >50% al mes 6._ **[INTERNO]**
2. **Lead time de "cambio registrado" → "PR de propagación abierto en todas las variantes
   afectadas"**, contra la línea base del propio cliente en sus 3 meses previos.
   _Target: -50%._ **[INTERNO]** Este dato no existe públicamente y hay que medirlo con
   entrevistas antes del piloto (`docs/mercado.md`, huecos de evidencia #1).

**Métricas específicas de IA:**

1. **Recall de variantes afectadas**, validado contra un experto humano. Reporto recall y
   precisión por separado y optimizo **recall**, porque el error caro es la variante olvidada,
   no la sugerida de más. _Target: recall >90%, precisión >70%._ **[INTERNO]**
2. **Tasa de borradores aprobados por un humano que después se revierten o causan defecto.** Es
   la métrica de seguridad: mide cuántas veces el producto convenció a alguien de algo
   equivocado. _Target: <2%. Por encima de 5% el agente se apaga hasta corregirlo._ **[INTERNO]**

**Métrica de salud secundaria:** **tasa de abstención** — con qué frecuencia el agente responde
"no tengo evidencia suficiente para localizar esto". Una abstención de 0% no es excelencia, es
la señal de que el sistema está inventando. Se premia, no se penaliza.

---

## 9. RIESGOS CRÍTICOS

**1. ¿Qué pasa si el problema desaparece en 12 meses por comoditización?**

**Riesgo alto, y es el principal.** La mitad genérica ya está comoditizada: Atlassian Rovo crea
work items desde documentos, GitHub Copilot y Cursor hacen ticket→PR, y el ensamblaje GitOps
multi-región con Argo CD ApplicationSets + Kustomize + Kyverno está publicado como blueprint
gratuito y se replica en semanas (`docs/critica.md`, sección 2). El flanco más peligroso no es
una RegTech: es **Harness**, que ya tiene la mitad de despliegue construida y solo necesitaría
añadir el front de entrada.

_Mitigación:_ no apostar a la generación —apostar a la **localización multi-variante on-premise**,
que exige estar dentro del perímetro y acumular historial. Y montarme sobre las piezas
comoditizadas en vez de contra ellas: si Argo CD mejora, SynKra mejora.
_Umbral que cambia la decisión:_ si antes de la sustentación Harness o Norm Ai anuncian este
conector, el argumento pivota a "por qué mi implementación es más barata, localizada y
auto-hospedada" — o la idea muere y hay que decirlo.

**2. ¿Puede un competidor replicar tu producto con la misma API en menos de 6 semanas?**

**El software sí. El permiso no.** Un equipo competente conecta un LLM a Bitbucket y a Jira y
abre PRs en borrador en un mes; no me voy a engañar con eso. Lo que no se replica en seis
semanas:

- La autorización del CISO para correr dentro del perímetro con acceso de lectura a todo el
  código fuente. Ese es el activo, y se gana una vez por cliente.
- El mapa de localización obligación↔servicio↔variante validado por humanos de _esa_ empresa.
- La suite de evaluaciones que demuestra el recall, que es lo único que convierte "confía en mí"
  en un número.

**3. Si tienes éxito a escala, ¿cuál es la primera forma en que se rompe la confianza?**

En orden de probabilidad:

1. **Falso negativo silencioso: el agente no reporta una variante afectada y nadie lo nota hasta
   la auditoría.** Es el más probable y el más grave, porque el fallo es _invisible_ — no hay un
   borrador malo que revisar, hay un borrador que nunca existió. _Mitigación:_ optimizar recall
   sobre precisión, listar siempre el inventario completo de variantes marcando cuáles descartó
   **y por qué**, y bloquear la ejecución silenciosa cuando el índice del repositorio está
   desactualizado.
2. **Fatiga de aprobación: el PO aprueba en bloque a las 5 p.m. de un viernes.** Si el producto
   funciona, el volumen de borradores sube y la revisión se vuelve un trámite — y ahí el
   "humano en el bucle" se convierte en una ficción de cumplimiento. _Mitigación:_ límite duro
   de borradores por lote, nivel de confianza explícito por ítem, y medir la distancia de
   edición: si cae a cero, es señal de sello de goma, no de calidad.
3. **Manipulación del agente por el contenido que él mismo lee.** Mensajes de commit, issues y
   documentos de entrada son texto que alguien puede influir. _Mitigación:_ todo insumo se trata
   como dato no confiable, allowlist explícito de herramientas por MCP, y el límite estructural
   de la sección 3: sin permiso de merge, el peor caso sigue siendo un borrador que un humano
   debe aprobar.
4. **Fuga de código o de datos sensibles.** Mitigado por diseño con inferencia local y egress en
   allowlist, pero **un solo despliegue mal configurado destruye el único argumento de venta que
   tengo**. La configuración de red debe ser verificable por el cliente, no prometida.

**4. Riesgo de arquitectura: estoy construyendo sobre un anti-patrón, y lo sé.**

_(Riesgo adicional a la plantilla. Es el que más me van a atacar y prefiero traerlo yo.)_

Las ramas _downstream_ de larga vida son **clone-and-own**, un anti-patrón explícitamente
desaconsejado por la literatura: Thoughtworks mantiene trunk-based development en "Adopt" y las
long-lived branches aparecen en "Hold"; DORA lo lista como capacidad de entrega (dora.dev); y
el SEI/CMU usa justamente el ejemplo bancario —una ley estatal que afecta 25 líneas de un
componente de 8.000— para explicar que la estrategia de clonar "quickly gets out of hand"
(Bachmann & Clements, "Variability in Software Product Lines", sei.cmu.edu). Un paper de 2025
atribuye ~55,2% de la deuda técnica del ecosistema Apache a migración de features en backports
entre ramas (arXiv 2511.09000) — exactamente el mecanismo que estoy automatizando.

**No lo voy a negar y tampoco lo voy a vender como virtud.** Lo asumo por una razón concreta:
es lo que los equipos objetivo hacen **hoy**, y un producto que exige migrar primero a
variabilidad gestionada no se adopta nunca. La forma correcta de decirlo es que SynKra **no
promueve** las ramas largas: **mide su costo y financia la salida**.

_Compromisos que acepto por escrito:_

- Instrumentar el costo de divergencia (distancia entre core y cada variante, tiempo de merge,
  conflictos por propagación) y **mostrárselo al cliente aunque lo incomode**.
- Diseñar la salida desde el día 1: cuando una variante muestra que su divergencia es puramente
  de configuración, el producto propone convertirla en overlay de Kustomize o feature flag —
  es decir, **propone eliminar su propia razón de existir en ese caso**.
- Si en el piloto el costo de merge sube en vez de bajar, es evidencia en contra de la tesis y
  se reporta, no se esconde.

**Verdad incómoda final:** este producto vive de una autorización que da una sola persona por
cuenta —el arquitecto de seguridad— para correr dentro de su perímetro leyendo todo el código.
Esa autorización se pierde con un solo incidente de egress. Por eso la inferencia local y el
allowlist de red no se relajan nunca, ni siquiera cuando el cliente lo pida para "ir más rápido".

---

## Kubernetes e IA: por qué no son decorativos

_(Adición a la plantilla, para el criterio de coherencia técnica.)_

**Si quito la IA, ¿el producto sigue funcionando igual?** No: queda una plantilla de ticket
vacía. Todo el valor está en inferir **a qué de mis N variantes aplica este cambio** a partir de
texto no estructurado y de código, y eso no tiene solución determinista.

**¿Por qué Kubernetes y no un contenedor suelto?**

- Los agentes son **controladores con reconcile loop** sobre CRDs propios (`ChangeRequest`,
  `PropagationPlan`, `DraftStory`). El estado deseado en el clúster es la arquitectura, no un
  detalle de despliegue.
- La **inferencia local vive en el clúster** sobre un node pool con GPU, escalada por profundidad
  de cola. El modelo no sale de ahí: ese es el argumento de venta.
- Los **servidores MCP son Services en el clúster**, cada agente con su propia ServiceAccount,
  su RBAC y su NetworkPolicy de egress hacia el servidor Git y el tracker, y nada más.
  **La frontera de permisos del agente está implementada con primitivas de Kubernetes**, así que
  es auditable con las herramientas que el cliente ya tiene.
- **Multi-tenancy y residencia de datos** por namespace (Capsule, Sandbox) o por clúster virtual
  (vCluster) cuando el regulador exige aislamiento duro.
- La trazabilidad exigida por el EU AI Act sale como Events de Kubernetes y trazas
  OpenTelemetry (graduado) hacia el stack de observabilidad del propio cliente.

**Dependencias que acepto y su madurez** (`docs/mercado.md`, sección 3): construyo sobre
proyectos **graduados** —Argo CD, Kyverno, OPA, OpenTelemetry, Falco—. Trato **kagent**
(Sandbox desde el 22-may-2025) y KAITO (Sandbox) como exploratorios: **no dependo de un proyecto
Sandbox para funcionalidad core.** Contexto de adopción: según datos CNCF de 2025, el 66% de las
organizaciones que usan GenAI corren inferencia sobre Kubernetes y el 57% ya corre agentes de IA
en producción.

---

## Checklist de entrega

- [x] Producto seleccionado (corre sobre Kubernetes, usa IA de forma defendible)
- [x] Deep research de validación completado y estudiado → `docs/mercado.md`
- [x] Deep research de crítica completado y estudiado → `docs/critica.md`
- [x] Investigación documentada en archivos Markdown, con fuentes
- [x] Product Vision Board completado (este documento)
- [ ] `prd.md` co-creado a partir de este PVB (ver `../modulo3/prompts-para-especificacion.md`)
- [ ] Listo para presentar en la sesión 3 (sábado 15 de agosto de 2026)

---

## Fuentes principales

Enlaces completos y fechas de consulta en `docs/mercado.md` y `docs/critica.md`.

**Existencia y costo del problema:** LexisNexis Risk Solutions / Forrester Consulting, "True
Cost of Financial Crime Compliance Study – U.S. and Canada" (feb. 2024) · Bank Policy Institute,
"Survey Finds Compliance is a Growing Demand on Bank Resources" (oct. 2024) · Santander Digital
Services, "Software Engineering" y nota de prensa de core banking en la nube (may. 2022) · BBVA,
"BBVA creates a Software area to drive global scalable solutions" · Umbrex, "Nubank Strategy and
Business Model" · páginas de producto de Temenos y Oracle FLEXCUBE.

**Ecosistema y madurez:** CNCF, "2025 Argo CD End User Survey" (24-jul-2025) · fichas de
proyecto CNCF de Argo, Flux, Kyverno, OPA, OpenTelemetry, Falco, Karmada, Capsule y kagent
(cncf.io/projects) · datos de adopción CNCF 2025 sobre inferencia y agentes en Kubernetes.

**Competencia:** PR Newswire y TechCrunch sobre la Serie C de Norm Ai (7-jul-2026) · RegTech
Analyst y Corlytics sobre CUBE y cobertura de jurisdicciones · Google Cloud blog, Regnology
"Ticket-to-Code Writer" (10-sep-2024) · community.atlassian.com sobre creación de work items
con IA.

**Riesgo de IA y marco regulatorio:** Dahl, Magesh, Suzgun & Ho, "Large Legal Fictions",
_Journal of Legal Analysis_ 16(1) (2024) · Magesh et al., _Journal of Empirical Legal Studies_
22 (2025) · Reglamento (UE) 2024/1689 (EU AI Act), obligaciones de alto riesgo desde el
2-ago-2026 · SR 11-7 (Fed/OCC) · superfinanciera.gov.co sobre uso de IA en supervisión
(3-oct-2025) · AI Incident Database #1152 (incidente Replit, jul. 2025) · cobertura AP / Fortune
/ CFO Dive sobre el reembolso de Deloitte Australia (oct. 2025).

**Anti-patrón de branching:** Thoughtworks, "Enabling Trunk Based Development with Deployment
Pipelines" y Technology Radar · dora.dev, "Capabilities: Trunk-based development" · Bachmann &
Clements, "Variability in Software Product Lines", sei.cmu.edu · arXiv 2511.09000 (2025) sobre
deuda técnica en backports entre ramas.
