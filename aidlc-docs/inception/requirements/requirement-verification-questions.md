# Preguntas de verificación de requisitos: SynKra (PRD v2)

El PRD v2 (`Modulo 3/specs/prd.md`) cubre casi todo el producto. Estas preguntas cubren solo lo
que el PRD deja abierto, lo que se contradice dentro del PRD y las decisiones que el marco
AI-DLC exige tomar aquí (extensiones). El PRD v2 es la fuente autoritativa; el PVB solo aporta
la visión.

**Cómo responder.** Escribe la letra después de cada `[Answer]:`. Si ninguna opción encaja,
elige X y describe tu respuesta. Cada pregunta trae una **Recomendación** con su porqué; si
estás de acuerdo con todas, basta con decirlo y se transcriben tal cual.

---

## Bloque A: extensiones de AI-DLC

## Question 1
La extensión `extensions/autonomia/limite/limite-autonomia.md` se escribió para el agente de IA
del alcance archivado: sus reglas hablan de ramas `synkra/*`, PRs en borrador, el puerto
`LlmProvider` y la API de Claude. Nada de eso existe en el PRD v2. Como no tiene archivo
`.opt-in.md`, el marco la aplica siempre. ¿Qué se hace con ella?

A) Adaptarla al PRD v2 (principio P2): AUTONOMIA-01 "SynKra solo despliega con confirmación humana y solo en `synkra-*`, y ninguna tarea del plan aplica cambios a un clúster sin un paso de aprobación"; AUTONOMIA-02 "todo criterio de aceptación se verifica con un comando". Se elimina la regla del gasto de IA. Sigue sin opt-in, siempre activa

B) Desactivarla: se registra como desactivada en `aidlc-state.md` y no se aplica en ninguna etapa

C) Dejarla como está

X) Other (please describe after [Answer]: tag below)

**Recomendación: A.** La guía del módulo 5 pide la extensión como entregable (sección 13) y el
PRD v2 sí tiene un límite de autonomía real: P2. B quita evidencia de la sustentación y C
bloquearía cada etapa con reglas que no se pueden cumplir.

[Answer]: 

## Question 2: Security Extensions
Should security extension rules be enforced for this project?

A) Yes — enforce all SECURITY rules as blocking constraints (recommended for production-grade applications)

B) No — skip all SECURITY rules (suitable for PoCs, prototypes, and experimental projects)

X) Other (please describe after [Answer]: tag below)

**Recomendación: B.** Es un ejercicio académico con una semana hasta la sustentación. Lo que el
PRD sí pide de seguridad (RBAC limitado, hash, path traversal, no root, registro permitido) ya
entra como requisitos propios desde los segmentos 6, 8 y 11, sin la carga de toda la línea base.

[Answer]: 

## Question 3: Resiliency Extensions
Should the resiliency baseline be applied to this project?

A) Yes — apply the resiliency baseline as directional best practices and design-time guidance (recommended for business-critical workloads)

B) No — skip the resiliency baseline (suitable for PoCs, prototypes, and experimental projects where rapid iteration matters more than reliability)

X) Other (please describe after [Answer]: tag below)

**Recomendación: B.** Demo con una sola réplica y SQLite; la línea base de AWS no aplica.

[Answer]: 

## Question 4: Property-Based Testing Extension
Should property-based testing (PBT) rules be enforced for this project?

A) Yes — enforce all PBT rules as blocking constraints

B) Partial — enforce PBT rules only for pure functions and serialization round-trips

C) No — skip all PBT rules

X) Other (please describe after [Answer]: tag below)

**Recomendación: B.** El motor de verificación (M2) es una función pura (core + plugins →
veredicto) y es el corazón del P1: es justo donde PBT aporta. El resto es CRUD e integración.

[Answer]: 

---

## Bloque B: huecos y contradicciones del PRD v2

## Question 5
La etiqueta "versión del core compatible" aparece como `core=2.x`. ¿Qué sintaxis acepta el motor?

A) Solo versión mayor con comodín (`2.x`): el plugin sirve para cualquier core `2.*`

B) Rango semver (`>=2.1.0 <3.0.0`), con `2.x` como abreviatura de `>=2.0.0 <3.0.0`

C) Versión exacta del core (`2.1.0`)

X) Other (please describe after [Answer]: tag below)

**Recomendación: B.** Cubre el ejemplo del PRD y permite decir "desde la 2.1", que es lo que
hace falta para explicar "qué versión sí sirve" (métrica de utilidad, segmento 10).

[Answer]: 

## Question 6
¿Cuál es el formato del compilado de un plugin y dónde va su descriptor?

A) Un JAR con un descriptor `synkra-plugin.yaml` dentro (nombre, versión del plugin, versión del core compatible, etiquetas sugeridas). SynKra lo lee al subirlo

B) Un JAR más un descriptor separado que se sube en la misma petición

C) Un JAR con los datos en el `MANIFEST.MF`

X) Other (please describe after [Answer]: tag below)

**Recomendación: A.** Un solo archivo que no se puede separar de su descriptor, y el core lee el
mismo descriptor al arrancar (doble verificación con la misma fuente).

[Answer]: 

## Question 7
Un core es una imagen en el registro permitido. ¿Cómo se registra en SynKra?

A) Por la API: nombre, versión del core y referencia de la imagen (con digest). SynKra valida que el registro esté permitido y crea su carpeta de plugins vacía

B) SynKra lee la versión de una etiqueta OCI de la imagen (`org.opencontainers.image.version`)

X) Other (please describe after [Answer]: tag below)

**Recomendación: A.** No obliga a SynKra a hablar con la API del registro, y el digest fija la
imagen exacta que se despliega.

[Answer]: 

## Question 8
P2 exige confirmación humana antes de desplegar. ¿Cómo se expresa en la API?

A) Dos pasos: `POST /despliegues` crea un plan verificado en estado `pendiente` (con el resumen); `POST /despliegues/{id}/confirmacion` lo aplica. Un plan sin confirmar nunca toca el clúster

B) Un solo paso con un campo `confirmado: true` en la petición

X) Other (please describe after [Answer]: tag below)

**Recomendación: A.** La confirmación queda como un acto separado y registrable, el escenario
"despliegue sin confirmación" del red-teaming se prueba con un `curl`, y la misma API sirve a la
CLI futura. Nota: un plan pendiente no es un despliegue en el registro (journey 7.3).

[Answer]: 

## Question 9
¿Cómo genera y aplica el motor de despliegue (M3) los manifiestos del país?

A) Construye los objetos en Java con el cliente Fabric8 de Kubernetes y los aplica con *server-side apply*. Sin Kustomize

B) Escribe un overlay de Kustomize por país y lo aplica invocando el binario `kustomize`/`kubectl` desde el backend

C) Plantillas de Kustomize renderizadas en memoria, aplicadas con Fabric8

X) Other (please describe after [Answer]: tag below)

**Recomendación: A.** El PRD dice "Kustomize por debajo", pero aplicar desde Java con Fabric8
evita meter binarios en la imagen del backend y es más fácil de probar. Si eliges A, el PRD se
anota con esta desviación.

[Answer]: 

## Question 10
La autenticación es Should. ¿Qué entra en la especificación?

A) Usuarios con rol en la tabla de SQLite y HTTP Basic sobre HTTPS en la API; el front pide usuario y clave. Los 4 roles del segmento 9.2 se aplican en la API

B) Sin autenticación en el MVP; los roles se documentan pero no se aplican

C) Token fijo por rol (API key) configurado en un Secret

X) Other (please describe after [Answer]: tag below)

**Recomendación: A**, como Should. Es poco código con Spring Security y sostiene la respuesta a
R3 en la sustentación. Si falta tiempo, se cae sin romper nada más.

[Answer]: 

## Question 11
El PRD tiene dos restos del enfoque anterior: el actor de CU2 y CU3 se llama "Product Owner de
país" (en el segmento 3 es "Responsable de país") y el KPI de CU3 incluye "adopción", que la
decisión de quitar el contenido de negocio eliminó. ¿Cómo se tratan?

A) Se unifica en "Responsable de país" y se quita la adopción del KPI de CU3. Se corrige también el PRD

B) Se corrige solo en los requisitos; el PRD queda como está

X) Other (please describe after [Answer]: tag below)

**Recomendación: A**, para que la trazabilidad PRD → requisito no tenga saltos.

[Answer]: 

## Question 12
¿Cuánto espera SynKra a que los pods estén listos antes de marcar un despliegue como fallido
(journey 7.4)?

A) 3 minutos, configurable

B) 5 minutos, configurable (igual a la meta del KPI de CU2)

X) Other (please describe after [Answer]: tag below)

**Recomendación: A.** En `kind` un core de Spring Boot arranca en menos de un minuto; 3 minutos
deja margen y mantiene el KPI de 5 minutos.

[Answer]: 

## Question 13
¿Cómo funciona la vuelta atrás (CU5)?

A) Se crea un despliegue nuevo de tipo `reversion` con la combinación del despliegue elegido; pasa por la misma verificación y la misma confirmación, y queda en el registro apuntando al original

B) Se reaplican los manifiestos guardados del despliegue anterior, sin volver a verificar

X) Other (please describe after [Answer]: tag below)

**Recomendación: A.** Una sola ruta de despliegue que siempre verifica (P1) y siempre registra (P7).

[Answer]: 

## Question 14
La sustentación es la semana siguiente. ¿Qué entra en los requisitos y, después, en las
unidades?

A) Must y Should como requisitos, cada uno con su prioridad; Could y Won't documentados como fuera de alcance. Las unidades se ordenan para que los Must den una demo completa aunque los Should no se construyan

B) Solo los Must; los Should quedan fuera hasta después de la sustentación

C) Must, Should y Could

X) Other (please describe after [Answer]: tag below)

**Recomendación: A.** Respeta el MoSCoW aprobado sin ampliarlo y deja claro qué se cae primero
si no hay tiempo (riesgo R9).

[Answer]: 
