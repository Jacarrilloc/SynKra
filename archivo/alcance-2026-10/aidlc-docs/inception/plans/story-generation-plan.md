# Plan de generación de historias de usuario — SynKra

**Insumos:** `aidlc-docs/inception/requirements/requirements.md` (aprobado), `entradas/prd.md` §3, §5, §7.
**Nota:** este plan trata de metodología. No incluye diseño técnico, tareas de código ni calendario.

Responde escribiendo la letra después de cada `[Answer]:`. Las respuestas quedan en el registro
de auditoría. Si ninguna opción encaja, elige la última (Other) y descríbela.

---

## Parte 1: Preguntas

## Question 1
¿Cómo se organizan las historias?

A) Por recorrido de usuario (user journey): una épica por flujo de PRD §7 (analista, DevOps, abandono, abstención), con historias en el orden del flujo

B) Por módulo del PRD (M1 Kernel, M2 Plugins, M3 Despliegue, M4 Agente, M5 Proveedor de IA), una épica por módulo

C) Híbrido con regla fija: épicas por módulo (M1..M5) y, dentro de cada una, las historias ordenadas siguiendo el recorrido del usuario

X) Other (please describe after [Answer]: tag below)

[Answer]: A

## Question 2
¿Qué personas se modelan? El PRD nombra analista de compliance, Product Owner, desarrollador, DevOps,
tech lead y CISO. El agente es un actor del sistema, no una persona.

A) Cuatro personas: analista de compliance, DevOps, tech lead y desarrollador de plugins. El CISO y el Product Owner aparecen solo como restricciones (veto y usuario indirecto)

B) Seis personas: las cuatro anteriores más CISO y Product Owner, cada una con sus historias

C) Cuatro personas más el CISO como persona con historias de verificación (egress, permisos del agente). El Product Owner solo como restricción

X) Other (please describe after [Answer]: tag below)

[Answer]: A

## Question 3
¿Cómo se trata al agente en las historias?

A) Como sistema: las historias están escritas desde el humano ("como analista quiero que el agente..."), y el comportamiento del agente va en los criterios de aceptación

B) Como actor con historias propias ("como agente, quiero..."), además de las de las personas humanas

X) Other (please describe after [Answer]: tag below)

[Answer]: A

## Question 4
¿Qué tamaño tiene una historia?

A) Una historia por requisito funcional o agrupación pequeña de RF (aprox. 30 a 40 historias en total), cada una verificable con un comando

B) Una historia por caso de uso del PRD más una por cada principio P1..P6 (aprox. 12 a 15 historias), con criterios de aceptación más largos

C) Una historia por requisito funcional y no funcional que tenga verificación propia (más de 40)

X) Other (please describe after [Answer]: tag below)

[Answer]: A

## Question 5
¿En qué formato se escriben los criterios de aceptación? (AUTONOMIA-03 exige que cada criterio sea comprobable con un comando.)

A) Given/When/Then, y en Then va el comando y el resultado esperado entre comillas de código

B) Lista de comprobaciones: cada línea es `comando` → resultado esperado

C) Given/When/Then para el comportamiento y, aparte, una línea `Verificación:` con el comando

X) Other (please describe after [Answer]: tag below)

[Answer]: A

## Question 6
¿En qué idioma se escriben las historias y las personas?

A) Español, con los nombres de comandos y de componentes en su forma original (`synkra analyze`, `LlmProvider`)

B) Inglés

X) Other (please describe after [Answer]: tag below)

[Answer]: B

## Question 7
Los requisitos Could (borrador de historia de usuario, adaptador `local`) están fuera de alcance. ¿Cómo aparecen?

A) No aparecen en las historias; quedan solo en el backlog de `requirements.md`

B) Aparecen como historias marcadas "Backlog / fuera de alcance", sin criterios completos

X) Other (please describe after [Answer]: tag below)

[Answer]: A

## Question 8
Las historias de límite de autonomía (el agente no puede fusionar, escribir en `main` ni aplicar cambios al clúster) ¿cómo se redactan?

A) Como historias propias del tech lead y del CISO, con los comandos de verificación del PRD como criterios

B) Como criterios de aceptación transversales que se repiten en las historias del agente, sin historias propias

C) Ambas: una historia propia por límite y, además, un criterio transversal que enlaza a ella desde cada historia del agente

X) Other (please describe after [Answer]: tag below)

[Answer]: C

---

## Decisiones resueltas (análisis de respuestas)

| # | Respuesta | Regla de generación |
|---|---|---|
| Q1 | A | Una épica por recorrido de PRD §7: E1 analista (happy path), E2 DevOps (happy path), E3 abandono, E4 el agente no puede resolver. Las historias de plataforma (kernel, plugins, límites) se asignan al recorrido que las usa primero |
| Q2 | A | Personas: analista de compliance, DevOps, tech lead, desarrollador de plugins. CISO y Product Owner solo como restricciones |
| Q3 | A | El agente es sistema: las historias se escriben desde el humano y el comportamiento del agente va en los criterios |
| Q4 | A | Una historia por RF o agrupación pequeña de RF; objetivo de 30 a 40 historias |
| Q5 | A | Given/When/Then; en Then va el comando y el resultado esperado entre comillas de código |
| Q6 | B | Historias y personas en inglés; los nombres de comandos y componentes en su forma original |
| Q7 | A | Los requisitos Could no aparecen en las historias |
| Q8 | C | Una historia propia por límite de autonomía y, en cada historia del agente, un criterio transversal que enlaza a ella |

**Contradicciones o ambigüedades:** ninguna. Q1-A (por recorrido) y Q4-A (por requisito) son
compatibles: la épica agrupa y la matriz de trazabilidad liga cada historia a sus RF/NFR. Los
documentos del proyecto siguen en español; solo `personas.md` y `stories.md` van en inglés (Q6-B).

---

## Parte 2: Pasos de generación (se ejecutan tras aprobar este plan)

Opciones de desglose consideradas y sus compromisos:

| Enfoque | Ventaja | Costo |
|---|---|---|
| Por recorrido | Refleja el uso real | Mezcla módulos; difícil de asignar a unidades |
| Por módulo | Se alinea con las futuras unidades | Pierde el hilo del usuario |
| Por persona | Claro para revisión de veto | Una misma función aparece en varias personas |
| Épicas | Jerarquía clara | Puede esconder dependencias |

Pasos (se marcan `[x]` al completarse):

- [x] 1. Crear `aidlc-docs/inception/user-stories/personas.md` con las personas elegidas en la pregunta 2: rol, objetivo, frustración, qué verifica y cuánta autoridad tiene
- [x] 2. Definir las épicas según el enfoque elegido en la pregunta 1
- [x] 3. Redactar las historias de cada épica con el tamaño de la pregunta 4 y el formato de la pregunta 5
- [x] 4. Redactar las historias de límite de autonomía según la pregunta 8, con los comandos de `AUTONOMIA-01` y `AUTONOMIA-02`
- [x] 5. Redactar los casos de abstención, abandono y presupuesto agotado (PRD §7 casos 3 y 4)
- [x] 6. Aplicar INVEST a cada historia (Independiente, Negociable, Valiosa, Estimable, Pequeña, Testeable) y marcar las dependencias
- [x] 7. Mapear personas a historias y historias a requisitos RF/NFR, y verificar que cada RF Must/Should esté cubierto por al menos una historia
- [x] 8. Verificar que ningún criterio de aceptación usa formulaciones sin comando o umbral (AUTONOMIA-03)
- [x] 9. Guardar `aidlc-docs/inception/user-stories/stories.md` con la matriz de trazabilidad
- [x] 10. Actualizar `aidlc-state.md` y registrar la aprobación en `audit.md`
