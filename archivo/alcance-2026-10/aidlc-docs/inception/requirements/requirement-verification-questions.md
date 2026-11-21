# Preguntas de verificación de requisitos — SynKra

`entradas/prd.md` y `entradas/pvb.md` cubren casi todo el producto. Estas preguntas cubren solo
lo que los dos documentos dejan abierto o se contradicen. Donde el PRD y el PVB difieren, esta
especificación toma el PRD como autoritativo (el PVB mismo lo declara en su "Ajuste de
dirección"). La pregunta 1 confirma ese supuesto.

Responde escribiendo la letra después de cada `[Answer]:`. Si ninguna opción encaja, elige la
última (Other) y describe tu respuesta.

---

## Question 1
El PVB original describe un conector que propaga cambios entre ramas y redacta historias de
usuario y tickets (Jira/Bitbucket, modelo local por defecto). El PRD lo reemplaza por un
micro-kernel con plugins y un agente que localiza el plugin y abre un PR en borrador (Claude con
tope de gasto). ¿Cómo se resuelve el conflicto en la especificación?

A) El PRD manda por completo. Del PVB solo se conservan visión, ICP, principios de confianza y métricas. Fuera de alcance: historias de usuario y tickets, ramas downstream, Jira, Bitbucket

B) El PRD manda, pero el borrador de historia de usuario (además del PR) queda como Could

C) Se mantienen ambos alcances: PRD como MVP y la propagación entre ramas del PVB como fase posterior documentada

X) Other (please describe after [Answer]: tag below)

[Answer]: B

## Question 2
El PRD fija Java 21 / Spring Boot para el kernel y los plugins, pero no dice nada del sistema de
build ni de la estructura del repositorio. ¿Qué se especifica?

A) Monorepo con Maven multi-módulo (`core/`, `plugins/co`, `plugins/pe`, `agent/`, `deploy/`)

B) Monorepo con Gradle multi-proyecto (misma estructura)

C) Monorepo donde kernel y plugins van en Maven/Gradle y el agente en otro lenguaje con su propio build (ver pregunta 3)

X) Other (please describe after [Answer]: tag below)

[Answer]: A

## Question 3
El PRD define el agente como CLI (`synkra analyze`, `synkra propose`) y como Job de Kubernetes,
pero no fija su lenguaje. ¿En qué se especifica?

A) Java 21 (mismo ecosistema que el kernel; un solo toolchain y una sola imagen base)

B) Python (ecosistema de SDKs de LLM y de scripts de evaluación)

C) Java para el agente y Python solo para el script de evaluación del dataset

X) Other (please describe after [Answer]: tag below)

[Answer]: C

## Question 4
El PRD dice que el agente "localiza" el plugin y el punto de extensión, pero no cómo obtiene la
evidencia. ¿Cómo se especifica la estrategia de localización?

A) Híbrida: un inventario determinista (manifiesto por plugin con país, puntos de extensión que implementa y temas) acota los candidatos, y el LLM razona sobre ese inventario más fragmentos de código. La evidencia debe citar archivos reales del repo

B) Solo LLM sobre el contenido del repositorio, sin manifiestos

C) Solo reglas deterministas (país y tema del YAML contra el manifiesto), con el LLM únicamente para redactar el informe y el PR

X) Other (please describe after [Answer]: tag below)

[Answer]: C

## Question 5
El PRD no define si el kernel guarda estado ni cuánto del dominio KYC se construye. ¿Cuál es el
alcance de la API REST de KYC?

A) Sin estado y sin base de datos: endpoints que validan documento, consultan la política de consentimiento y la de retención usando el plugin activo, y exponen `/plugins` y health

B) Con persistencia (PostgreSQL) para guardar consentimientos y solicitudes KYC

C) Sin estado, y la API es mínima: un endpoint por punto de extensión más `/plugins` y health

X) Other (please describe after [Answer]: tag below)

[Answer]: C

## Question 6
Los plugins `co` y `pe` implementan reglas de país simuladas (el PRD las marca como riesgo R8).
¿Qué contenido se especifica para `DocumentValidator`, `ConsentPolicy` y `RetentionPolicy`?

A) Reglas ficticias y sencillas (por ejemplo, formato y dígito de control del documento, campos obligatorios del consentimiento, años de retención), etiquetadas "simulado" en el código, en la salida de la API y en la demo

B) Reglas basadas en normativa real (Ley 1581 en Colombia, Ley 29733 en Perú) con verificación de fuentes primarias

X) Other (please describe after [Answer]: tag below)

[Answer]: B

## Question 7
El PRD verifica el límite de autonomía con `gh api repos/Jacarrilloc/SynKra/...`, pero el
usuario de git de este repositorio es `Jacarrillo`. ¿Cuál es el repositorio remoto real que
usarán los comandos de verificación y el agente?

A) `Jacarrilloc/SynKra` tal como está en el PRD

B) `Jacarrillo/SynKra`

C) El repositorio aún no existe en GitHub; las unidades deben incluir crearlo y proteger `main` con revisión obligatoria

X) Other (please describe after [Answer]: tag below)

[Answer]: A

## Question 8
El PRD marca como Should la NetworkPolicy de egress, el RBAC mínimo, Pod Security `restricted`
y Argo CD, y como Could el adaptador `local` y el CRD `ChangeRequest`. ¿Qué entra en la
especificación y en las unidades?

A) Must + Should entran como unidades planificadas (con su plan de tareas). Could y Won't quedan documentados como backlog, sin unidad

B) Solo Must. Should y Could quedan como backlog documentado

C) Must + Should + el adaptador `local` (modelo abierto en el clúster)

X) Other (please describe after [Answer]: tag below)

[Answer]: A

## Question 9
El PRD pide un dataset de 20 cambios regulatorios simulados (10 CO, 8 PE, 2 de un país sin
plugin) y un script que compara el informe del agente con la respuesta esperada. ¿Cómo se
trata?

A) Es una unidad propia ("Evaluación de IA") con su plan de tareas: dataset, script de comparación, métricas de recall, precisión y abstención, y casos de red-teaming

B) Se incluye como tareas dentro de la unidad del agente, sin unidad separada

X) Other (please describe after [Answer]: tag below)

[Answer]: B

## Question 10
El PRD aclara que el modo `claude` envía código a la API externa y acepta ese trade-off para el
MVP. ¿Cómo se refleja en los requisitos?

A) Como requisito no funcional explícito: el trade-off se documenta, se muestra una advertencia al usar `LLM_PROVIDER=claude` y se limita el contexto enviado a los fragmentos mínimos

B) Solo se documenta el trade-off, sin advertencia ni límite de contexto

X) Other (please describe after [Answer]: tag below)

[Answer]: B

## Question 11
El entorno de desarrollo ya usa Linux/WSL2 con kind (`Makefile`, `scripts/`). ¿Qué versión de
Kubernetes y qué registro de imágenes se asumen para el despliegue?

A) kind local, con las imágenes cargadas en el clúster (`kind load docker-image`), sin registro externo

B) kind local, con un registro local en contenedor

C) kind local y publicación en GitHub Container Registry

X) Other (please describe after [Answer]: tag below)

[Answer]: A

---

## Question: Security Extensions
Should security extension rules be enforced for this project?

A) Yes — enforce all SECURITY rules as blocking constraints (recommended for production-grade applications)

B) No — skip all SECURITY rules (suitable for PoCs, prototypes, and experimental projects)

X) Other (please describe after [Answer]: tag below)

[Answer]: B

## Question: Resiliency Extensions
Should the resiliency baseline be applied to this project?

**What this extension is.** Enabling it applies a set of **directional, design-time best practices** for building resilient systems, derived from the **AWS Well-Architected Framework (Reliability Pillar)** and resilience-review guidance. It steers requirements, design, and code toward fault tolerance, high availability, observability, and recoverability — covering 15 practice areas across business goals, change management, observability, high availability, disaster recovery, and continuous improvement.

**What this extension is NOT.** Enabling it does **not** make your workload production-ready, nor does it certify or guarantee any availability, RTO, or RPO target. It is a **starting point** that scaffolds good resiliency decisions early — it is not a substitute for a formal **AWS Well-Architected Review** of the built system.

Treat the output as a well-grounded **first draft of your resiliency posture** to build on and validate — not a finished, production-certified result.

A) Yes — apply the resiliency baseline as directional best practices and design-time guidance (recommended for business-critical workloads, as an informed starting point that you can validate and harden before go-live)

B) No — skip the resiliency baseline (suitable for PoCs, prototypes, and experimental projects where rapid iteration matters more than reliability)

X) Other (please describe after [Answer]: tag below)

[Answer]: B

## Question: Property-Based Testing Extension
Should property-based testing (PBT) rules be enforced for this project?

A) Yes — enforce all PBT rules as blocking constraints (recommended for projects with business logic, data transformations, serialization, or stateful components)

B) Partial — enforce PBT rules only for pure functions and serialization round-trips (suitable for projects with limited algorithmic complexity)

C) No — skip all PBT rules (suitable for simple CRUD applications, UI-only projects, or thin integration layers with no significant business logic)

X) Other (please describe after [Answer]: tag below)

[Answer]: C
