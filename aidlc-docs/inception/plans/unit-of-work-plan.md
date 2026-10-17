# Plan de Units of Work — SynKra

**Insumos:** `requirements.md`, `stories.md` (37 historias), `application-design/` (aprobados).
**Qué es una unidad aquí:** SynKra es un monorepo con un solo repositorio y varias piezas
desplegables (imagen del kernel, JAR de plugin, CLI del agente, manifiestos de Kubernetes).
Una unidad es un conjunto de historias y componentes que se diseña y se planifica junto, con
un plan de tareas propio. Terminología: "módulo" = módulo Maven; "unidad" = unidad de trabajo.

## Restricciones ya decididas (no se vuelven a preguntar)

| Restricción | Origen |
|---|---|
| La evaluación (dataset y `score.py`) va dentro de la unidad del agente, sin unidad propia | Q9-B de requisitos |
| Must y Should entran como unidades planificadas; Could y Won't no | Q8-A de requisitos |
| Monorepo Maven multi-módulo; estructura `core/`, `plugins/co`, `plugins/pe`, `agent/`, `deploy/` | Q2-A de requisitos |
| Cada unidad termina en su plan de Code Generation Part 1; no se escribe código | Instrucción del usuario |

## Propuesta de partida (base para las preguntas)

| Unidad | Componentes | Historias candidatas |
|---|---|---|
| U-core | C1 | US-2.2, US-2.3, US-2.4, US-2.5 |
| U-plugins | C2, C3 | US-2.6, US-2.7, US-2.8, US-2.9, US-2.10 |
| U-agent (incluye `eval`) | C4, C5 | US-1.1 a US-1.8, US-3.1, US-4.1 a US-4.5 |
| U-deploy | C6 (base, overlays, imagen, `Makefile`) | US-2.1, US-2.11, US-2.12, US-2.13, US-2.14 |
| U-cluster-security | C6 (seguridad) | US-2.15, US-2.16 |
| U-gitops-job | C6 (Argo CD, Job) | US-2.17, US-2.18 |
| U-autonomy-boundary | Configuración de GitHub y RBAC | US-1.9, US-1.10, US-1.11, US-1.12, US-4.6 |

---

## Parte 1: Preguntas

## Question 1
¿Con qué granularidad se divide el trabajo de despliegue y seguridad (componente C6)?

A) Tres unidades como en la propuesta: `U-deploy`, `U-cluster-security` y `U-gitops-job`

B) Una sola unidad `U-deploy` que incluya despliegue, seguridad del clúster, GitOps y Job (más historias en una sola unidad y un solo plan)

C) Dos unidades: `U-deploy` (base, overlays, imagen, `Makefile`) y `U-cluster-ops` (seguridad, GitOps y Job juntos)

X) Other (please describe after [Answer]: tag below)

[Answer]: B

## Question 2
Los plugins `co` y `pe` son estructuralmente iguales pero con reglas distintas. ¿Se separan?

A) Una sola unidad `U-plugins` con ambos países (un plan, dos módulos; el segundo repite el patrón del primero)

B) Dos unidades `U-plugin-co` y `U-plugin-pe` (un plan de tareas por país; permite planificar y revisar Colombia y Perú por separado, y su fuente primaria se verifica por país)

X) Other (please describe after [Answer]: tag below)

[Answer]: A

## Question 3
Las historias de límite de autonomía (protección de `main`, token del agente, "el agente no fusiona", ServiceAccount sin escritura) y la declaración del trade-off en el README (US-4.6) son configuración, no código. ¿Dónde viven?

A) Una unidad propia `U-autonomy-boundary` con las historias US-1.9 a US-1.12 y US-4.6; es la que prueba la regla AUTONOMIA-01 con comandos

B) Las de GitHub (US-1.9, US-1.10, US-1.11) en una unidad `U-repo-governance`; la de RBAC (US-1.12) dentro de la unidad de seguridad del clúster; US-4.6 dentro de `U-agent`

C) Todas dentro de `U-agent`, porque son límites del agente

X) Other (please describe after [Answer]: tag below)

[Answer]: C

## Question 4
Las tareas de límite de autonomía requieren que `main` esté protegida y que el token del agente exista antes de que el agente abra su primer PR. ¿En qué orden se planifican las unidades?

A) Seguridad primero: la unidad de límite de autonomía, luego `core`, plugins, agente, despliegue, seguridad del clúster y GitOps/Job

B) Producto primero: `core`, plugins, agente, despliegue, seguridad del clúster, GitOps/Job y, al final, el límite de autonomía

C) Ruta crítica del producto con el límite de autonomía justo antes del agente: `core`, plugins, límite de autonomía, agente, despliegue, seguridad del clúster, GitOps/Job

X) Other (please describe after [Answer]: tag below)

[Answer]: X) core, plugins, agente (dentro de U-agent, la protección de main y el token se configuran antes de la primera tarea que abre un PR) y despliegue

## Question 5
¿Cómo trabaja el equipo? Esto decide si las unidades se marcan como secuenciales o paralelizables.

A) Una persona; todas las unidades se ejecutan en secuencia

B) Dos personas; las unidades sin dependencia entre sí (por ejemplo, `core` y límite de autonomía) se marcan como paralelizables

X) Other (please describe after [Answer]: tag below)

[Answer]: B

## Question 6
El monorepo necesita un `pom.xml` raíz, configuración compartida de Java 21 y la ampliación del `Makefile`. ¿Quién los crea?

A) La primera unidad de código (`U-core`) crea el `pom.xml` raíz y la configuración compartida; las demás añaden su módulo

B) Una unidad inicial `U-scaffold` (sin historias propias) crea la estructura del monorepo, el `pom.xml` raíz y los objetivos del `Makefile`; todas las demás dependen de ella

X) Other (please describe after [Answer]: tag below)

[Answer]: A

## Question 7
El kernel se despliega con una imagen única (US-2.11) y el JAR del plugin llega por un init container. Pero el principio P3 prohíbe "imágenes por país". ¿Cómo llega el JAR al init container?

A) Una imagen única `synkra/plugins` que contiene los JAR de todos los plugins; el init container copia solo el del país activo (no hay imagen por país)

B) Un `ConfigMap` generado por Kustomize (`binaryData`) con el JAR del país en cada overlay (sin imagen; el límite de tamaño de un ConfigMap es 1 MiB)

C) Una imagen por plugin (`synkra/plugin-co`, `synkra/plugin-pe`), aceptando que esto contradice la letra de P3

X) Other (please describe after [Answer]: tag below)

[Answer]: A

---

## Decisiones resueltas (análisis de respuestas)

| # | Respuesta | Regla de generación |
|---|---|---|
| Q1 | B | Una sola unidad `U-deploy`: despliegue, seguridad del clúster, GitOps y Job |
| Q2 | A | Una sola unidad `U-plugins` con `co` y `pe` |
| Q3 | C | Todas las historias de límite de autonomía (US-1.9 a US-1.12) y US-4.6 dentro de `U-agent` |
| Q4 | X | Orden: `U-core`, `U-plugins`, `U-agent`, `U-deploy`. Dentro de `U-agent`, las tareas de protección de `main` y del token del agente van antes de la primera tarea que abre un PR |
| Q5 | B | Dos personas. Las unidades sin dependencia entre sí se marcan como paralelizables en la matriz |
| Q6 | B | Unidad inicial `U-scaffold` sin historias propias; todas las demás dependen de ella |
| Q7 | A | Imagen única `synkra/plugins` con los JAR de todos los plugins; el init container copia solo el del país activo. No hay imagen por país |

Unidades resultantes: **`U-scaffold`, `U-core`, `U-plugins`, `U-agent`, `U-deploy`** (cinco).

**Contradicciones o ambigüedades:** ninguna entre las respuestas. Q4 fija el orden de
planificación y Q5 permite ejecución en paralelo donde la matriz de dependencias lo muestre; no
se contradicen, porque el orden es de planificación y el paralelismo es de ejecución.

**Decisiones derivadas que tomo para cerrar huecos (confirma o corrige al aprobar):**

1. **Dueño de las dos imágenes.** `U-deploy` construye la imagen del kernel y la imagen
   `synkra/plugins`, a partir de los JAR que produce `U-plugins`. Q7-A no decía quién la construye.
2. **US-1.12 dentro de `U-agent`.** La ServiceAccount `agent` y su (ausencia de) RBAC se
   definen en `deploy/agent-identity/`, ruta propiedad de `U-agent`. `U-deploy` la referencia
   desde su base de Kustomize y no la modifica. Esto evita que dos unidades editen los mismos archivos.
3. **US-2.4 en `U-core`.** La historia se asigna a la unidad que implementa el endpoint de
   health; su segundo criterio (probes en el Deployment) lo verifica una tarea de `U-deploy`.
4. **US-2.1 en `U-deploy`.** `make cluster` y `make doctor` ya existen en el `Makefile`; la
   tarea los verifica y amplía con `make deploy`. `U-scaffold` conserva solo la estructura del monorepo.
5. **Cada historia en exactamente una unidad.** Las verificaciones compartidas entre unidades
   se listan en `unit-of-work-story-map.md` como "criterios compartidos".

Asignación prevista de las 37 historias: `U-core` 4, `U-plugins` 5, `U-agent` 19, `U-deploy` 9,
`U-scaffold` 0.

---

## Parte 2: Pasos de generación (tras responder y aprobar)

- [x] 1. Analizar las respuestas y resolver ambigüedades o contradicciones
- [x] 2. Crear `aidlc-docs/inception/application-design/unit-of-work.md`: definición y responsabilidades de cada unidad, componentes, historias, estructura de código (monorepo, rutas de cada módulo) y el orden elegido
- [x] 3. Crear `unit-of-work-dependency.md`: matriz de dependencias, orden de construcción y marcas de paralelismo
- [x] 4. Crear `unit-of-work-story-map.md`: cada una de las 37 historias asignada a exactamente una unidad
- [x] 5. Para cada unidad, decidir qué etapas de diseño se ejecutan (Functional Design, NFR Requirements, NFR Design, Infrastructure Design) y anotarlo en `unit-of-work.md`
- [x] 6. Validar fronteras y dependencias: sin ciclos, sin historias huérfanas, sin unidad que dé al agente permisos de `merge`, `push` a `main` ni `kubectl apply` (AUTONOMIA-01)
- [x] 7. Verificar que cada requisito Must y Should queda cubierto por al menos una unidad
- [x] 8. Actualizar `aidlc-state.md` y registrar la aprobación en `audit.md`
