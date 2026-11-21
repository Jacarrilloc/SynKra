# Requisitos: SynKra

**Fuente autoritativa:** `Modulo 3/specs/prd.md` (PRD v2, 13 segmentos aprobados el 21-nov-2026).
Visión en `Modulo 2/pvb.md`. Respuestas en `requirement-verification-questions.md`.
**Profundidad:** estándar con trazabilidad (sistema nuevo de varios componentes; el PRD ya es
detallado, así que este documento formaliza y cierra huecos, no amplía).
**Restricción de proceso:** no se escribe código. El módulo 5 se detiene al terminar el plan de
tareas de cada unidad (Code Generation, Parte 1).

---

## 1. Análisis de intención

| Campo | Valor |
|---|---|
| Solicitud | "listo, empecemos con el modulo 5": especificar SynKra a partir del PRD v2 hasta las unidades y sus tareas |
| Claridad | Clara en producto, principios y alcance; abierta en formatos, API de confirmación y reversión (cerradas en §2) |
| Tipo | New Project (greenfield) |
| Alcance | System-wide: catálogo, verificación, despliegue, registro, API, front, core y plugins |
| Complejidad | Moderada a compleja: dos stacks (Java y .NET), un clúster y una regla de seguridad que cruza todo (P2) |

## 2. Decisiones de las preguntas de aclaración

| # | Tema | Respuesta | Consecuencia |
|---|---|---|---|
| Q1 | Extensión de autonomía | A | Adaptada a P2: AUTONOMIA-01 (solo con confirmación y solo en `synkra-*`) y AUTONOMIA-02 (criterio por comando). Siempre activa |
| Q2 | Security Baseline | B | Desactivada. Lo de seguridad que pide el PRD entra como requisitos propios (§4.8, §5) |
| Q3 | Resiliency Baseline | B | Desactivada |
| Q4 | Property-Based Testing | B | Parcial: PBT-02, 03, 07, 08 y 09 bloqueantes, sobre el motor de verificación (M2) y la lectura del descriptor |
| Q5 | Sintaxis del core compatible | B | Rango semver; `2.x` abrevia `>=2.0.0 <3.0.0` |
| Q6 | Formato del plugin | A | JAR con `synkra-plugin.yaml` dentro |
| Q7 | Registro de un core | A | Por la API: nombre, versión e imagen con digest |
| Q8 | Confirmación en la API | A | Dos pasos: plan `pendiente` y confirmación aparte |
| Q9 | Generación de manifiestos | A | Objetos construidos en Java con Fabric8 y *server-side apply*; sin Kustomize. **Desviación del PRD**, anotada en él |
| Q10 | Autenticación (Should) | A | Usuarios con rol en SQLite y HTTP Basic; los 4 roles se aplican en la API |
| Q11 | Restos del enfoque anterior | A | "Responsable de país" en todo el PRD; fuera la adopción del KPI de CU3. PRD corregido |
| Q12 | Espera de los pods | A | 3 minutos, configurable |
| Q13 | Vuelta atrás | A | Despliegue nuevo de tipo `reversion`, con la misma verificación y confirmación |
| Q14 | Alcance | A | Must y Should, con su prioridad; los Must solos ya dan la demo completa |

### Supuestos que este documento fija (revísalos al aprobar)

| # | Supuesto | Por qué |
|---|---|---|
| S1 | **Los namespaces de los países se crean en la instalación** (`synkra-co`, `synkra-pe`, a partir de una lista configurada), no al desplegar | El RBAC de Kubernetes no puede limitar `create namespaces` a un prefijo: dar ese permiso a `synkra:deployer` rompería P2. Así su cuenta solo tiene `Role` dentro de cada `synkra-<país>` |
| S2 | **La carpeta de plugins es por nombre de core** (por ejemplo `synkra-core`) y reúne sus versiones (`2.1.0`, `3.0.0`). El descriptor del plugin nombra el core al que pertenece | CU4 necesita un plugin `kyc-pe 2.0` compatible con `3.x` en la misma carpeta que el core `2.1` |
| S3 | **`consentimiento-pe` no se construye.** Aparece en los journeys 7.1 y 7.2, pero el MoSCoW solo pide `kyc-co` y `kyc-pe`. Los plugins de prueba de la suite (incompatible, descriptor incompleto, hash alterado) son *fixtures*, no plugins del producto | No ampliar el alcance |
| S4 | **El país se identifica con un código de dos letras en minúscula** (`co`, `pe`), y el namespace es `synkra-<código>` | Valida la entrada antes del RBAC (escenario "escalar el despliegue") |

---

## 3. Objetivo y alcance

**Objetivo:** desplegar la variante por país de un producto micro-kernel ya compilado sin que una
combinación incompatible de core y plugins llegue al clúster, y ver en un solo lugar qué corre en
cada país (PRD §1 y §4).

**North Star:** % de despliegues por país hechos desde SynKra en los que ningún plugin
incompatible llegó al clúster. Meta 100% **[INTERNO]** (PRD §10).

**Dentro (Must y Should):** ver §4. **Fuera:** CLI, borradores de selección, dependencias entre
plugins y métricas en Prometheus (Could); Git o GitOps en el flujo de productos, compilar o
versionar código, IA, multi-tenant o varios clústeres y despliegues sin humano (Won't). PRD §8.

---

## 4. Requisitos funcionales

Prioridad según el MoSCoW del PRD §8. La columna **Verificación** es el comando o la prueba que
demuestra el requisito (AUTONOMIA-02); el detalle exacto se fija en las tareas.

### 4.1 Catálogo de compilados (M1)

| ID | Requisito | Prio. | Traza | Verificación |
|---|---|---|---|---|
| RF-01 | Registrar un core con nombre, versión semver e imagen con digest (`registro/imagen@sha256:…`). Si es el primer core con ese nombre, se crea su carpeta de plugins vacía | Must | CU1, Q7, S2 | `curl -X POST /api/cores` → 201; `curl /api/cores/{nombre}/plugins` → lista vacía |
| RF-02 | Subir un plugin (JAR) a la carpeta de un core. SynKra lee `synkra-plugin.yaml`: nombre, versión del plugin, nombre del core y rango de core compatible, más etiquetas | Must | CU1, Q6 | `curl -F archivo=@kyc-pe.jar /api/cores/synkra-core/plugins` → 201 con el descriptor leído |
| RF-03 | Rechazar con motivo legible un descriptor ausente o incompleto (sin nombre, versión del plugin o core compatible), un rango mal formado o un core que no es el de la carpeta | Must | CU1, §11.1 | Casos "descriptor incompleto" de la suite; `curl` → 422 con el campo que falta |
| RF-04 | Calcular y guardar el SHA-256 de cada artefacto al subirlo, y servirlo junto al artefacto | Must | §11.1 integridad, R5 | Prueba de API: el hash devuelto coincide con `sha256sum kyc-pe.jar` |
| RF-05 | Rechazar artefactos con entradas de ruta absoluta o con `..` (path traversal) y artefactos sobre el límite de tamaño (20 MB por defecto, configurable) | Must | §11.4, R5 | Casos de red-teaming: `curl` con el JAR malicioso → 422 |
| RF-06 | Etiquetar plugins por país, región o cliente (clave=valor) y filtrar la carpeta por etiqueta | Must | CU1, CU2, §9.1 | `curl '/api/cores/synkra-core/plugins?pais=pe'` → solo los de `pais=pe` |
| RF-07 | Una carpeta es independiente: un plugin de la carpeta del core A nunca se lista ni se acepta en un despliegue del core B | Must | P4 | Prueba de API que lo intenta → rechazo |

### 4.2 Motor de verificación (M2)

| ID | Requisito | Prio. | Traza | Verificación |
|---|---|---|---|---|
| RF-08 | Una librería Java pura, sin dependencias de Spring ni de Kubernetes, que recibe la versión del core y los descriptores elegidos y devuelve un veredicto por plugin y uno global | Must | P1, §9.1 | Pruebas unitarias y de propiedades del módulo (`mvn -pl verificador test`) |
| RF-09 | Interpretar rangos semver; `N.x` equivale a `>=N.0.0 <N+1.0.0` | Must | Q5 | Pruebas de propiedades (PBT-03): toda versión `2.*` satisface `2.x` y ninguna `3.*` |
| RF-10 | Todo rechazo explica el motivo y qué versión del core o del plugin sí sirve, buscando en el catálogo | Must | CU4, §10 utilidad | Suite: el 100% de los rechazos por versión trae una alternativa o dice que no existe |
| RF-11 | Exponer la verificación sin desplegar (`POST /api/verificaciones`) para la verificación en vivo del front | Must | §9.1, §9.4 | `curl` con una combinación inválida → 200 con veredicto "rechazada" y motivo |
| RF-12 | Una suite de combinaciones versionada (los 7 grupos del PRD §11.1) corre en cada build del verificador y del core y **bloquea el build** si un caso falla | Must | §11.1, §11.3 | `mvn verify` falla si se altera un resultado esperado |

### 4.3 Motor de despliegue (M3)

| ID | Requisito | Prio. | Traza | Verificación |
|---|---|---|---|---|
| RF-13 | `POST /api/despliegues` (país, versión del core, plugins) verifica la combinación y, si pasa, crea un **plan** en estado `pendiente` con el resumen: namespace, imagen, plugins, versiones y qué cambia frente a lo que corre hoy. Si no pasa, responde 422 con los motivos y no crea nada | Must | CU2, CU3, CU4, P2, Q8 | `curl` → 201 `pendiente`; `kubectl get deploy -n synkra-pe` no cambia |
| RF-14 | `POST /api/despliegues/{id}/confirmacion` aplica el plan con la cuenta de servicio `synkra:deployer`: Deployment del core con el init container, ConfigMap de plugins activos y Service, en `synkra-<país>`, con Fabric8 y *server-side apply*. Registra quién confirmó y cuándo | Must | CU2, P2, Q9 | `curl` → 202; `kubectl get deploy,cm -n synkra-pe` muestra la combinación confirmada |
| RF-15 | Rechazar en el backend un país fuera de la lista configurada o cualquier namespace que no sea `synkra-<país>` (`default`, `kube-system`) | Must | P2, §11.4, S1, S4 | `curl` con `pais=default` → 422; no llega a Kubernetes |
| RF-16 | Esperar a que los pods estén listos hasta 3 minutos (configurable) y marcar el despliegue `listo` o `fallido`. **Nunca** reintenta ni revierte por su cuenta | Must | §7.4, P2, Q12 | Prueba con un plugin que falla al iniciar → estado `fallido` y ningún despliegue nuevo en el registro |
| RF-17 | El init container descarga del catálogo **solo** los plugins elegidos, verifica el SHA-256 de cada uno, escribe solo en `/plugins` y reintenta un número acotado de veces (5, con espera creciente). Si falla, el pod no arranca | Must | §9.3, R6, R7 | `kubectl exec … ls /plugins` solo muestra los elegidos; con un hash alterado el pod queda en `Init:Error` |
| RF-18 | En un despliegue fallido, guardar el contexto para el humano: combinación, resultado de la verificación, log de arranque del core, eventos del pod y quién confirmó | Must | §7.4 | `curl /api/despliegues/{id}` de un fallido trae los cinco campos |

### 4.4 Registro y reversión (M4)

| ID | Requisito | Prio. | Traza | Verificación |
|---|---|---|---|---|
| RF-19 | Registrar cada despliegue confirmado en SQLite: quién, cuándo, país, core, plugins, versiones y etiquetas, tipo (`despliegue` o `reversion`), despliegue original si es reversión, y resultado. Un plan sin confirmar no es un despliegue | Must | P7, §7.3 | `sqlite3 synkra.db 'select …'` tras un despliegue; un plan abandonado no aparece |
| RF-20 | Vista general por país leída **del clúster** (core, plugins, versiones y estado), con la marca de si coincide con el último despliegue del registro | Must | CU5, §10 concordancia | `curl /api/paises` coincide con `kubectl get deploy,cm -n synkra-*` |
| RF-21 | Historial de despliegues de un país | Must | CU5 | `curl /api/paises/pe/despliegues` |
| RF-22 | Volver a un despliegue anterior: `POST /api/despliegues/{id}/reversion` crea un plan de tipo `reversion` con esa combinación, que pasa por la misma verificación (RF-13) y la misma confirmación (RF-14) | Must | CU5, P7, Q13 | Prueba de punta a punta: tras la confirmación, `kubectl` muestra la combinación anterior y el registro apunta al original |
| RF-23 | El registro y el catálogo sobreviven a un reinicio del pod del backend (SQLite y artefactos en el mismo PVC) | Must | §9.1 persistencia | `kubectl delete pod -l app=synkra-backend`; `curl /api/cores` sigue respondiendo lo mismo |

### 4.5 Core (M7) y plugins de país (M8)

| ID | Requisito | Prio. | Traza | Verificación |
|---|---|---|---|---|
| RF-24 | El core (Java 21 / Spring Boot) define sus puntos de extensión en un contrato de plugins (interfaces Java) y no contiene lógica de país | Must | P3, PVB §3 | `grep -rniE "colombia\|peru" core/src` sin resultados |
| RF-25 | Al arrancar, el core lee `/plugins`, lee cada descriptor, lo verifica con la **misma librería** M2 contra su propia versión y lo compara con el ConfigMap de plugins activos. No arranca si falta un plugin activo o si uno es incompatible. Un plugin presente y no activo no se carga | Must | P1, §9.3, §11.1 | Prueba de integración: con un plugin `3.x` en `/plugins` el proceso termina con código ≠ 0 y el motivo en el log |
| RF-26 | Cada plugin se carga con su propio cargador de clases; si su inicialización falla, el core no arranca | Must | R2, §7.4 | Prueba con un *fixture* que lanza una excepción al iniciar → el core no queda listo |
| RF-27 | El core expone `GET /plugins` (activos y sus versiones) y salud; la sonda de readiness solo pasa con los plugins cargados | Must | §9.1 M7 | `curl localhost:8080/plugins`; `kubectl get pod` en `Ready` solo tras la carga |
| RF-28 | Plugins `kyc-co` y `kyc-pe` con reglas simuladas, etiquetados `pais=co` y `pais=pe` y compatibles con `2.x`; más un `kyc-pe 2.0.0` compatible con `3.x` para la demo de CU4 | Must | §8, CU4, S3 | `curl` a cada endpoint de KYC del país responde según su regla simulada |

### 4.6 API REST (M5) y front (M6)

| ID | Requisito | Prio. | Traza | Verificación |
|---|---|---|---|---|
| RF-29 | Toda la funcionalidad de M1 a M4 existe en la API REST, con un formato de error uniforme | Must | P5 | Un script `curl` ejecuta los 5 casos de uso de punta a punta, sin el front |
| RF-30 | API documentada con OpenAPI | Should | §8 | `curl /api/v3/api-docs` devuelve el documento |
| RF-31 | Front en Blazor con las 4 pantallas del PRD §9.4 (vista general, catálogo, desplegar con verificación en vivo, detalle con contexto del fallo y vuelta atrás). Solo consume la API y no tiene lógica de negocio | Must | P5, §9.4 | Recorrido de la demo; `grep` sin reglas de versión en el código del front |

### 4.7 Instalación y límite en el clúster

| ID | Requisito | Prio. | Traza | Verificación |
|---|---|---|---|---|
| RF-32 | SynKra (backend, front, PVC) se instala en `kind` con un solo comando, que también crea los namespaces de los países (S1) y la cuenta `synkra:deployer` | Must | §7.2, §10 KPI | `make install` y `kubectl get pods -n synkra` listos |
| RF-33 | La cuenta `synkra:deployer` solo tiene `Role` y `RoleBinding` en cada `synkra-<país>`; ningún permiso de clúster | Must | P2 | `kubectl auth can-i create deployments -n default --as=system:serviceaccount:synkra:deployer` → `no`; en `synkra-pe` → `yes` |
| RF-34 | Imágenes multi-stage, mínimas y con usuario no root para el backend, el front, el core y el init container | Must | §8, módulo 3 | `docker inspect -f '{{.Config.User}}'` no vacío ni `root` |
| RF-35 | SynKra mismo se instala con GitOps y CI (módulo 8); los productos los sigue desplegando SynKra sin Git | Should | §13 | Reconciliación del repositorio de instalación visible en el controlador de GitOps |

### 4.8 Seguridad adicional (Should)

| ID | Requisito | Prio. | Traza | Verificación |
|---|---|---|---|---|
| RF-36 | Usuarios con rol en SQLite y HTTP Basic en la API; el front pide usuario y clave. Se aplican los 4 roles del PRD §9.2 | Should | §9.2, R3, Q10 | `curl -u lector:… -X POST /api/despliegues` → 403 |
| RF-37 | Solo se registran cores cuya imagen viene del registro permitido (configurable) | Should | §11.4, R3 | `curl` con `docker.io/…` → 422 |
| RF-38 | NetworkPolicy *deny-by-default*, Pod Security `restricted` y límites de recursos en `synkra` y `synkra-*` | Should | §8, módulos 1 y 7 | `kubectl label --dry-run=server ns … pod-security.kubernetes.io/enforce=restricted` sin advertencias |
| RF-39 | `compose.yaml` de desarrollo con las 6 prácticas del módulo 3 y escaneo de imágenes con Trivy | Should | §8 | `trivy image` sin hallazgos críticos |

---

## 5. Requisitos no funcionales

| ID | Atributo | Requisito | Traza |
|---|---|---|---|
| RNF-01 | Rendimiento | Desplegar un país nuevo, de la confirmación a los pods listos, en < 5 min en `kind` **[INTERNO]** | §10 |
| RNF-02 | Rendimiento | De la instalación al primer país desplegado, < 30 min **[INTERNO]** | §10 |
| RNF-03 | Rendimiento | Volver al despliegue anterior en < 5 min **[INTERNO]** | §10 |
| RNF-04 | Fiabilidad | > 90% de despliegues `listos` al primer intento **[INTERNO]** | §10 |
| RNF-05 | Corrección | 100% de combinaciones inválidas de la suite detectadas, 0% de falsos rechazos, 0 discrepancias motor-core | §10, §11 |
| RNF-06 | Seguridad | 0 intentos de despliegue fuera de `synkra-*` permitidos por el RBAC; defensa en dos capas (backend y RBAC) | P2, §10 |
| RNF-07 | Persistencia | SQLite en un PVC, una sola réplica del backend con estrategia `Recreate` (SQLite no admite dos escritores sobre el volumen) | §9.1, Q14 |
| RNF-08 | Mantenibilidad | Una sola librería de verificación compartida por el backend y el core, sin copia | P1, R1 |
| RNF-09 | Testabilidad | La suite del §11.1 bloquea el build; pruebas de propiedades sobre M2 y sobre la lectura del descriptor (PBT parcial) | §11.3, Q4 |
| RNF-10 | Portabilidad | El demo corre en Linux y WSL2 con `kind`, `kubectl`, Docker, JDK 21 y .NET 8 | Módulo 1, `scripts/` |
| RNF-11 | Trazabilidad | Todo despliegue confirmado tiene registro; el registro coincide con el clúster (concordancia) | P7, §10 |
| RNF-12 | Stack | Backend, verificador, core y plugins en Java 21 / Spring Boot; front en Blazor (.NET) como servicio aparte | §9.6 |

---

## 6. Escenarios de usuario (resumen del PRD §5 y §7)

| Escenario | Requisitos |
|---|---|
| CU1 Agregar compilados | RF-01 a RF-07, RF-37 |
| CU2 Desplegar un país nuevo (journey 7.1) | RF-11, RF-13, RF-14, RF-16, RF-17, RF-25 |
| CU3 Cambiar los plugins de un país | RF-13 (qué cambia), RF-14, RF-19 |
| CU4 Rechazar una combinación incompatible | RF-09, RF-10, RF-11, RF-13 |
| CU5 Ver qué corre y volver atrás | RF-20, RF-21, RF-22 |
| Journey 7.2 Operador prepara SynKra | RF-32, RF-33, RF-01, RF-02 |
| Journey 7.3 Abandono a mitad del flujo | RF-13 (un plan pendiente no toca el clúster), RF-19 |
| Journey 7.4 Falla y traspaso al humano | RF-16, RF-18, RF-22, RF-26 |

## 7. Riesgos que moldean los requisitos (PRD §12)

R1 → RF-08, RF-25, RNF-08 · R2 → RF-26, RF-18 · R3 → RF-33, RF-36, RF-37 · R4 → RF-13, RF-19,
RF-22 · R5 → RF-04, RF-05, RF-17 · R6 → RF-17 · R7 → RF-17, RF-25 · R8 → RF-31 sin lógica ·
R9 → Q14 (los Must solos dan la demo) · R10 → declarado, sin requisito.

---

## 8. Cumplimiento de extensiones

| Regla | Estado | Motivo |
|---|---|---|
| AUTONOMIA-01 | Cumple | Confirmación separada del plan (RF-13, RF-14); sin reintentos ni reversiones automáticas (RF-16, RF-22); doble capa de namespace (RF-15, RF-33) con su comprobación `kubectl auth can-i` |
| AUTONOMIA-02 | Cumple | Cada RF tiene su comprobación por comando o prueba; los criterios exactos se fijan en las tareas |
| PBT-02, 03, 07, 08, 09 (parcial) | N/A en esta etapa | Se aplican en el diseño y en las tareas de M2 y de la lectura del descriptor; aquí solo queda el requisito (RNF-09) |
| Security, Resiliency | Desactivadas | Q2 y Q3 |
