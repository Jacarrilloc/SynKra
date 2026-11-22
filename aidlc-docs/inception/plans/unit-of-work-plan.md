# Plan de unidades de trabajo: SynKra

**Entradas:** `requirements.md`, `stories.md`, `execution-plan.md` y los artefactos de
`application-design/` (aprobados; backend como monolito modular con fronteras comprobadas, D14).
**Salidas:** `application-design/unit-of-work.md`, `unit-of-work-dependency.md` y
`unit-of-work-story-map.md`.

## Pasos

- [ ] 1. Definir las unidades según la descomposición elegida en la pregunta 1, cada una con lo que hace y lo que **no** hace
- [ ] 2. Documentar la organización del código (greenfield) según la pregunta 6
- [ ] 3. Generar `unit-of-work.md`: responsabilidad, componentes, prioridad (Must o Should), cómo se prueba sola y "terminada cuando" con un comando
- [ ] 4. Generar `unit-of-work-dependency.md`: matriz y grafo; comprobar con `scripts/verificar-grafo.py` que no hay ciclos y que hay al menos dos unidades sin dependencias entre sí
- [ ] 5. Generar `unit-of-work-story-map.md`: cada una de las 34 historias asignada a una sola unidad dueña; ninguna huérfana
- [ ] 6. Proponer el orden de construcción según la pregunta 7, con lo que se puede hacer en paralelo
- [ ] 7. Validar las fronteras con la lista de la guía: sin ciclos, sin historias huérfanas, paralelismo, cada unidad probable sola, ninguna unidad es "el backend", el total cabe en el tiempo
- [ ] 8. Revisar cumplimiento de extensiones (AUTONOMIA-01 y 02)

---

## Preguntas

Letra después de `[Answer]:`, o "aprobado" en el chat para aceptar las recomendaciones.

## Question 1 (Story Grouping)
¿Cómo se descompone el sistema en unidades?

A) Por módulo con frontera real (11 unidades). Must: **U01** verificador y suite; **U02** core, contrato y plugins de país; **U03** plataforma del backend (aplicación Spring Boot, SQLite con Flyway, formato de error, prueba de fronteras D14); **U04** catálogo; **U05** pod del país (adaptador de Kubernetes e init container); **U06** registro y vista general; **U07** despliegue y reversión; **U08** instalación y límite (manifiestos, RBAC, `make install`, script de punta a punta); **U09** front. Should: **U10** seguridad y roles; **U11** endurecimiento y entrega (NetworkPolicy, Pod Security, `compose.yaml`, Trivy, GitOps)

B) Más gruesa (6 unidades): verificador; core y plugins; backend (catálogo, despliegue y registro juntos); front; instalación; Should

C) Una unidad por componente (13)

X) Other (please describe after [Answer]: tag below)

**Recomendación: A.** B mete "el backend" en una unidad, que es el error que la guía señala; C
separa piezas que no se prueban solas (la API sin sus módulos). A sigue los módulos del
monolito modular: cada módulo del backend es su propia unidad, con sus controladores.

[Answer]: 

## Question 2 (Dependencies)
¿Cómo se comunican los módulos del backend entre unidades?

A) Llamadas directas a la interfaz pública de cada módulo (un paquete `api` por módulo; lo demás es interno y D14 lo comprueba). Sin eventos

B) Eventos de aplicación de Spring para lo asíncrono (por ejemplo, despliegue → registro) y llamadas directas para el resto

X) Other (please describe after [Answer]: tag below)

**Recomendación: A.** El flujo es corto y síncrono salvo la vigilancia, que ya es una tarea del
propio módulo de despliegue. Los eventos añaden una forma más de fallar sin beneficio en el demo.

[Answer]: 

## Question 3 (Team Alignment)
¿Quién construye las unidades?

A) Julian, con el loop de agentes de los módulos siguientes: cada unidad la puede llevar un codificador solo, y las tareas se dimensionan para un despacho cada una (un cambio revisable con un comando)

B) Un equipo de varias personas, con dueños por unidad

X) Other (please describe after [Answer]: tag below)

**Recomendación: A**, que es el caso real del proyecto.

[Answer]: 

## Question 4 (Technical Considerations)
¿Cómo se tratan los Should en las unidades?

A) Unidades propias (U10 seguridad, U11 endurecimiento y entrega), al final del orden, que se pueden cortar sin tocar las Must. Lo Should que vive dentro de una unidad Must (OpenAPI en U03) va como sus últimas tareas, marcadas Should

B) Todo Should como tareas finales dentro de las unidades Must

X) Other (please describe after [Answer]: tag below)

**Recomendación: A**, por R9: lo que se corta tiene que poder cortarse entero.

[Answer]: 

## Question 5 (Business Domain)
¿Dónde viven los controladores REST?

A) Dentro de cada módulo (corte vertical): el catálogo trae sus rutas, el despliegue las suyas, el registro las suyas. U03 solo aporta el formato de error y la configuración común

B) En un módulo `api` aparte que llama a los demás

X) Other (please describe after [Answer]: tag below)

**Recomendación: A.** Es lo que hace que cada unidad del backend se pruebe sola con `curl` y que
no exista una unidad "API" que sea, en realidad, el backend entero.

[Answer]: 

## Question 6 (Code Organization)
¿Qué estructura de directorios tiene el repositorio?

A) Raíz con `settings.gradle.kts`; proyectos Gradle `verificador/`, `plugin-api/`, `core/`, `plugins/kyc-co/`, `plugins/kyc-pe/` y `backend/` (paquetes `plataforma`, `catalogo`, `kubernetes`, `registro`, `despliegue`, `seguridad`); `init-descarga/`; `front/` (solución .NET); `deploy/` (manifiestos); `scripts/` y `Makefile` existentes

B) Igual, pero con un proyecto Gradle por módulo del backend (`backend-catalogo/`, `backend-despliegue/`…)

X) Other (please describe after [Answer]: tag below)

**Recomendación: A.** Un solo proyecto `backend/` con fronteras comprobadas por D14 es lo que
define al monolito modular; B lo acerca a microservicios en el build sin necesidad.

[Answer]: 

## Question 7 (Orden y alcance)
¿Con qué criterio se ordenan las unidades?

A) Primero lo que da una demo de punta a punta con los Must, respetando el grafo: U01 y U03 en paralelo; luego U02, U04 y U05; luego U06 y U07; luego U08 y U09; al final U10 y U11 (Should)

B) Estrictamente por el plan de fases del PRD §13 (core y plugins primero, luego backend, luego front, luego módulo 8)

X) Other (please describe after [Answer]: tag below)

**Recomendación: A.** Coincide casi con B, pero adelanta la plataforma del backend en paralelo
con el verificador para no dejar el camino crítico en una sola fila (R9).

[Answer]: 
