# Application Design — SynKra (consolidado)

Documentos detallados: [components.md](components.md), [component-methods.md](component-methods.md),
[services.md](services.md), [component-dependency.md](component-dependency.md).

## 1. Resumen de la arquitectura

SynKra es un monorepo Maven multi-módulo (`com.synkra`) con seis componentes:

| Id | Componente | Tecnología | Qué hace |
|---|---|---|---|
| C1 | `core` | Java 21, Spring Boot | Contratos de extensión, modelo de manifiesto, carga de plugins, API de KYC sin estado |
| C2 | `plugins/co` | Java 21, JAR | Reglas de Colombia en los tres puntos de extensión |
| C3 | `plugins/pe` | Java 21, JAR | Reglas de Perú en los tres puntos de extensión |
| C4 | `agent` | Java 21 plano, picocli | `analyze` (localización determinista e informe) y `propose` (PR en borrador) |
| C5 | `eval` | Python | Dataset de 20 cambios y puntuación de recall, precisión y abstención |
| C6 | `deploy` | Kustomize, Makefile | Base, overlays `co` y `pe`, seguridad del clúster, Job del agente, Argo CD |

Un solo puerto formal: `LlmProvider` (adaptadores `claude` y `fake`).

## 2. Decisiones de diseño tomadas

| Id | Decisión | Origen |
|---|---|---|
| D1 | Contratos y manifiesto dentro de `core`; plugins y agente dependen de su JAR plano | Q1-B, F2-B |
| D2 | Paquete base `com.synkra` | Q2-C |
| D3 | Un único cargador de clases para el directorio de plugins, con `ServiceLoader` | Q3-B |
| D4 | País activo por configuración; la ruta lleva el país y un país distinto da 404 | Q4-C, F1-A |
| D5 | Agente en capas simples; solo `LlmProvider` es un puerto formal | Q5-B |
| D6 | CLI con picocli sobre Java plano | Q6-B |
| D7 | `git` y `gh` como procesos tras una lista blanca de subcomandos | Q7-B |
| D8 | `propose` agrega un archivo de solicitud de cambio en `plugins/<pais>/changes/<id>.md`; no modifica código fuente | Q8-A |
| D9 | Ejecutar `propose` es la aprobación; basta con que el informe exista | Q9-C, F3-A |
| D10 | Informe en Markdown con tabla de formato fijo | Q10-A |
| D11 | Presupuesto diario y log de tokens en archivos locales | Q11-A |
| D12 | (Derivada) El análisis determinista es gratuito: con presupuesto agotado, `analyze` escribe el informe sin prosa del modelo y lo indica con "presupuesto agotado" | AUTONOMIA-02, RF-43 |
| D13 | (Derivada) Un PR por cambio, con un archivo de solicitud por cada plugin afectado | Suposición 1 de `components.md` |

## 3. Cumplimiento de las reglas obligatorias

| Regla | Estado | Cómo |
|---|---|---|
| AUTONOMIA-01 | Cumple | `CommandWhitelist` no permite `merge`, `push` fuera de `synkra/*`, `gh pr merge`, `gh pr review`, `kubectl` ni `helm`. El agente no tiene componente de despliegue. Se comprobará con las historias US-1.9 a US-1.12 |
| AUTONOMIA-02 | Cumple | Todo uso del modelo pasa por `LlmProvider`; el SDK o cliente de Anthropic solo existe en `com.synkra.agent.llm.claude`; `TokenBudget` precede a toda llamada; `fake` en pruebas |
| AUTONOMIA-03 | Pendiente para los planes | Cada tarea de cada plan nombrará su comando de verificación |

## 4. Historia a componente

| Historia | Componentes |
|---|---|
| US-1.1, US-1.2 | C4.1, C4.2, C4.6 |
| US-1.3, US-1.4 | C4.3, C4.4, C4.6 |
| US-1.5 | C4.7, C4.8, C4.9, C4.10 |
| US-1.6 | C4.6 (lectura), C4.14 |
| US-1.7, US-1.8 | C4.5, C4.12, C4.13, C4.14, C2/C3 (`changes/`) |
| US-1.9, US-1.10, US-1.11 | Configuración de GitHub, C4.13 |
| US-1.12 | C6 (ServiceAccount `agent`) |
| US-2.1 | `Makefile` (C6) |
| US-2.2 | C1.1 |
| US-2.3 | C1.3, C1.4, C1.7 |
| US-2.4 | C1.8, C6 (probes) |
| US-2.5 | C1.5, C1.6 |
| US-2.6, US-2.7 | C2, C3 |
| US-2.8 | C1.2, C2, C3 |
| US-2.9 | C2, C3 (catálogo de reglas) |
| US-2.10 | Estructura Maven (C1, C2, C3) |
| US-2.11 | C6 (imagen), C1 |
| US-2.12, US-2.13, US-2.14 | C6 |
| US-2.15, US-2.16 | C6 (seguridad) |
| US-2.17 | C6 (Argo CD) |
| US-2.18 | C6 (Job), C4 |
| US-3.1 | C4.1, C4.14 |
| US-4.1 | C4.4, C4.6 |
| US-4.2 | C4.4, C4.7 |
| US-4.3 | C4.11 |
| US-4.4 | C4.10 y la estructura de pruebas |
| US-4.5 | C5 |
| US-4.6 | `README.md` |

Todas las 37 historias tienen al menos un componente.

## 5. Enmiendas a artefactos ya aprobados

Las respuestas de diseño obligan a ajustar cuatro puntos de los artefactos anteriores. Cada
cambio está marcado en el archivo con "Enmienda (Application Design)":

1. **RF-06** (`requirements.md`): el 404 aplica cuando el país de la ruta no es el país activo
   del kernel (D4).
2. **RF-34** (`requirements.md`): `propose` agrega un archivo de solicitud de cambio dentro del
   plugin; no edita código fuente (D8).
3. **RF-35** (`requirements.md`) y **US-1.6** (`stories.md`): se elimina el paso de aprobación
   aparte; `propose` exige solo que el informe exista (D9). Se elimina el segundo criterio de US-1.6.
4. **US-1.7** (`stories.md`) y **personas.md** (P-ANA): se retira la expresión "aprobado" para el
   informe.

## 6. Riesgos de diseño abiertos

| # | Riesgo | Dónde se resuelve |
|---|---|---|
| 1 | El agente arrastra Spring Boot en el classpath al depender de `core` | NFR Design del agente |
| 2 | Cómo el Job de Kubernetes ve el repositorio (clon o volumen) | Infrastructure Design del agente |
| 3 | Sin confirmación en `propose` aumenta la fatiga de aprobación (R7) | Se mide con la distancia de edición del PRD §10; no hay mitigación técnica adicional |
| 4 | Un cargador único puede chocar con clases duplicadas | Prueba de integración en `core` |
| 5 | Con presupuesto en un volumen del Job, el tope diario real depende de que el volumen persista | Infrastructure Design del agente |
