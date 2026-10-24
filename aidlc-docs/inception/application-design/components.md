# Componentes — SynKra

Insumos: `requirements.md`, `stories.md`, respuestas Q1-Q11 y F1-F3 de `application-design-plan.md`.
Paquete base Java: `com.synkra` (Q2-C). Build: Maven multi-módulo (Q2-A de requisitos).

## Decisiones de diseño que condicionan los componentes

| Decisión | Efecto |
|---|---|
| Q1-B | Los contratos (`DocumentValidator`, `ConsentPolicy`, `RetentionPolicy`) y el modelo del manifiesto viven en `core`. Los plugins dependen de `core` como librería |
| F2-B | El agente depende de `core` solo para el modelo del manifiesto. `core` publica un JAR plano (para plugins y agente) y un JAR ejecutable de Spring Boot (para la imagen) |
| Q3-B | Un solo cargador de clases para todos los JAR del directorio de plugins, con `ServiceLoader` |
| Q4-C + F1-A | El kernel arranca con un país activo (configuración). La ruta lleva el país (`/kyc/{country}/...`); si no coincide con el activo, 404 |
| Q5-B | El agente usa capas simples. El único puerto formal es `LlmProvider` |
| Q6-B | CLI con picocli sobre Java plano (sin Spring) |
| Q7-B | El agente ejecuta `git` y `gh` como procesos, con lista blanca de subcomandos |
| Q8-A | `propose` agrega un archivo de solicitud de cambio dentro del plugin; no modifica código fuente |
| Q9-C + F3-A | `propose` exige solo que el informe exista. Ejecutarlo es la aprobación del humano; el control real es la revisión del PR |
| Q10-A | El informe es Markdown con una tabla de formato fijo |
| Q11-A | Presupuesto y log de tokens en archivos locales del agente |

## Mapa de módulos

```
SynKra/
  pom.xml                     (raíz Maven, groupId com.synkra)
  core/                       C1  kernel + contratos + modelo de manifiesto
  plugins/co/                 C2  plugin Colombia
  plugins/pe/                 C3  plugin Perú
  agent/                      C4  CLI de localización y propuesta
  eval/                       C5  dataset y script Python de evaluación
  deploy/                     C6  Kustomize base, overlays y seguridad del clúster
  informes/                   salida de `analyze` (un .md por cambio)
  cambios/                    entrada de `analyze` (un .yaml por cambio)
```

## C1 — `core` (kernel)

**Propósito.** Definir los contratos regulatorios, cargar plugins y exponer la API de KYC sin
conocer ningún país (P2).

| Subcomponente | Responsabilidad | Paquete |
|---|---|---|
| C1.1 Contratos de extensión | Interfaces `DocumentValidator`, `ConsentPolicy`, `RetentionPolicy` y sus tipos de entrada y salida | `com.synkra.core.spi` |
| C1.2 Modelo de manifiesto | Tipos `PluginManifest`, `ExtensionPointBinding`, `TopicBinding` y su lector YAML | `com.synkra.core.manifest` |
| C1.3 Cargador de plugins | Lista los JAR del directorio configurado, crea un cargador de clases y descubre implementaciones con `ServiceLoader` | `com.synkra.core.plugin` |
| C1.4 Registro de plugins | Guarda los plugins cargados con su manifiesto y entrega el plugin del país activo | `com.synkra.core.plugin` |
| C1.5 Guardia de país | Compara el país de la ruta con el país activo y decide 404 | `com.synkra.core.web` |
| C1.6 API de KYC | Un endpoint por punto de extensión, sin estado | `com.synkra.core.web` |
| C1.7 API de plugins | `GET /plugins` con los plugins cargados | `com.synkra.core.web` |
| C1.8 Configuración | `KernelProperties`: país activo y directorio de plugins | `com.synkra.core.config` |

**Interfaces expuestas.** HTTP (`/kyc/{country}/...`, `/plugins`, `/actuator/health`); librería
Java (`com.synkra.core.spi`, `com.synkra.core.manifest`) para plugins y agente.
**No contiene** ninguna palabra de país (NFR-02) ni acceso a base de datos (RF-05).

## C2 — `plugins/co` (plugin Colombia) y C3 — `plugins/pe` (plugin Perú)

**Propósito.** Implementar los tres puntos de extensión con reglas reales de cada país, con
fuente primaria por regla (RF-12), en un JAR independiente (RF-13).

| Subcomponente | Responsabilidad |
|---|---|
| Implementaciones | Una clase por punto de extensión, registrada en `META-INF/services` |
| Manifiesto `plugin.yaml` | País, versión, puntos de extensión con su clase y archivo fuente, y temas que cubre cada punto |
| Catálogo de reglas | Cada regla con campo `source` (artículo de la norma) o marca `verificar` |
| Directorio `changes/` | Recibe los archivos de solicitud de cambio que escribe el agente (Q8-A). Ni el kernel ni el plugin los leen |

Cada plugin depende de `core` (JAR plano) y de nada más de SynKra; nunca del otro plugin.

## C4 — `agent` (CLI de SynKra)

**Propósito.** Dado un cambio ya interpretado, localizar de forma determinista qué plugin y
punto de extensión tocar, redactar el informe y, si el humano lo pide, abrir un PR en borrador.
Java 21 plano con picocli (Q6-B).

| Subcomponente | Responsabilidad | Paquete |
|---|---|---|
| C4.1 CLI | Comandos `analyze <archivo>` y `propose <id>`, códigos de salida (0 éxito, 1 error, 2 abstención) | `com.synkra.agent.cli` |
| C4.2 Lector de cambios | Lee y valida el YAML (país, tema, texto) y calcula su huella | `com.synkra.agent.change` |
| C4.3 Inventario de plugins | Lee `plugins/*/plugin.yaml` con el modelo de `core` | `com.synkra.agent.inventory` |
| C4.4 Localizador | Reglas deterministas: cruza país y tema con los manifiestos y emite un veredicto por plugin (afectado, descartado con motivo) o abstención | `com.synkra.agent.locator` |
| C4.5 Resolutor de evidencia | Convierte cada veredicto en archivos reales del repositorio (clase del punto de extensión, manifiesto) y verifica que existen | `com.synkra.agent.locator` |
| C4.6 Escritor y lector de informes | Genera y parsea la tabla Markdown de formato fijo | `com.synkra.agent.report` |
| C4.7 Narrador | Pide al `LlmProvider` solo el texto del informe y de la descripción del PR; no recibe poder de decisión | `com.synkra.agent.report` |
| C4.8 Puerto `LlmProvider` | Interfaz única de acceso al modelo | `com.synkra.agent.llm` |
| C4.9 Adaptador `claude` | Cliente HTTP a la API de Anthropic; único lugar donde aparece la API | `com.synkra.agent.llm.claude` |
| C4.10 Adaptador `fake` | Respuestas fijas y deterministas para pruebas y CI | `com.synkra.agent.llm.fake` |
| C4.11 Presupuesto y log de tokens | Contador diario en archivo, tope, mensaje "presupuesto agotado" y `tokens.log` | `com.synkra.agent.budget` |
| C4.12 Cliente Git | Ejecuta `git` y `gh` como procesos mediante la lista blanca | `com.synkra.agent.git` |
| C4.13 Lista blanca de comandos | Única puerta de salida a procesos; rechaza todo lo que no esté permitido | `com.synkra.agent.git` |
| C4.14 Proponente | Orquesta `propose`: rama, archivos de solicitud de cambio, commit, push y PR en borrador | `com.synkra.agent.propose` |

**Lista blanca (P1, P5).** Permitido: `git rev-parse`, `git cat-file`, `git log`, `git status`,
`git checkout -b synkra/<id>`, `git add` (solo rutas bajo el plugin indicado),
`git commit`, `git push origin synkra/<id>`, `gh pr create --draft`, `gh pr view`.
*Ampliación (Functional Design de U-agent):* también `git fetch origin main`,
`git ls-files`, `git branch --list synkra/<id>`, `git ls-remote --heads origin synkra/<id>`,
`git checkout <rama original>` (solo para volver) y `gh pr list --head synkra/<id>`.
Prohibido y sin camino de código: `git merge`, `git rebase`, `git push` a cualquier otra rama,
`gh pr merge`, `gh pr review`, `gh pr ready`, `kubectl`, `helm`.

## C5 — `eval` (evaluación)

| Subcomponente | Responsabilidad |
|---|---|
| Dataset | 20 cambios en YAML (10 CO, 8 PE, 2 de país sin plugin) y su respuesta esperada |
| `score.py` | Lee los informes (tabla de formato fijo) y calcula recall, precisión y tasa de abstención |
| Red-team | Casos de inyección, ambigüedad, país sin plugin y presupuesto agotado; se ejecutan con `fake` |

## C6 — `deploy` (despliegue y seguridad del clúster)

| Subcomponente | Responsabilidad |
|---|---|
| Base de Kustomize | Deployment del kernel, Service, ConfigMap, probes, init container que monta el JAR del plugin |
| Overlays `co` y `pe` | Eligen plugin, país activo y namespace (`synkra-co`, `synkra-pe`) |
| Seguridad | Pod Security `restricted`, NetworkPolicy de egress del agente, ServiceAccount `agent` sin permisos de escritura, Secret por archivo |
| Job del agente | Manifiesto del Job con volumen para presupuesto y log |
| Argo CD | `Application` por overlay |
| `Makefile` | `cluster`, `deploy COUNTRY=xx`, `doctor` |

## Suposiciones de diseño (para confirmar en Functional Design)

1. **Un PR por cambio.** `propose` crea una sola rama `synkra/<id>` con un archivo de solicitud
   por cada plugin afectado. Cuando un cambio afecta a más de un plugin, el PR toca solo esos
   plugins.
2. **Códigos de país.** Se comparan en minúsculas de dos letras (ISO 3166-1 alpha-2). El
   cambio YAML puede escribirlo en mayúsculas; el lector lo normaliza.
3. **Rutas provisionales.** Los endpoints exactos se fijan en Functional Design de `core`.
