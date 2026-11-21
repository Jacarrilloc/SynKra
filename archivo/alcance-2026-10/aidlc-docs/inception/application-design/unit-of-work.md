# Unidades de trabajo — SynKra

Origen: `plans/unit-of-work-plan.md` (respuestas Q1-Q7 y cinco decisiones derivadas, aprobadas).
Terminología: "módulo" = módulo Maven; "unidad" = unidad de trabajo; cada unidad termina en un
plan de Code Generation Part 1 (no se escribe código en este trabajo).

## Resumen

| Unidad | Propósito | Componentes | Historias |
|---|---|---|---|
| U-scaffold | Estructura del monorepo, `pom.xml` raíz, base del `Makefile` y README | Raíz del repositorio | 0 (sin historias propias, Q6-B) |
| U-core | Kernel: contratos de extensión, modelo de manifiesto, cargador y registro de plugins, API de KYC | C1 | 4 |
| U-plugins | Plugins `co` y `pe` con reglas reales y fuente primaria | C2, C3 | 5 |
| U-agent | Agente (`analyze`, `propose`), `LlmProvider`, presupuesto, lista blanca de Git, evaluación, límite de autonomía | C4, C5 y configuración de GitHub y de identidad | 19 |
| U-deploy | Imágenes, overlays, despliegue, seguridad del clúster, GitOps y Job | C6 | 9 |

Total: 37 historias.

**Orden de planificación (Q4):** `U-scaffold` → `U-core` → `U-plugins` → `U-agent` → `U-deploy`.
Dentro de `U-agent`, las tareas de protección de `main` y del token del agente van antes de la
primera tarea que abre un PR.

---

## U-scaffold — Estructura del monorepo

**Responsabilidades.**
- `pom.xml` raíz (groupId `com.synkra`, Java 21, gestión de versiones y módulos).
- Directorios de trabajo: `eval/`, `deploy/`, `images/`, `cambios/`, `informes/`. Para los módulos Maven (`core`, `plugins`, `plugins/co`, `plugins/pe`, `agent`), un `pom.xml` esqueleto que la unidad dueña reemplaza (enmienda de `U-scaffold-code-generation-plan.md`: evita que el `pom.xml` raíz falle por módulos sin POM y que las unidades editen el POM raíz).
- Herramientas de entorno: `python3` y `gh` en `scripts/doctor.sh` y `scripts/setup-linux.sh`.
- Ampliación del `Makefile` existente con los objetivos `build` y `test` (los de despliegue los añade `U-deploy`).
- `.gitignore` y esqueleto del `README.md`.

**Fuera de alcance.** Historias, lógica y manifiestos de Kubernetes.
**Verificación (ejemplos).** `mvn -q validate` termina en `0`; `make help` lista `build` y `test`.

## U-core — Kernel

**Responsabilidades.** Componentes C1.1 a C1.8: contratos `DocumentValidator`, `ConsentPolicy`,
`RetentionPolicy`; modelo y lector del manifiesto; cargador de plugins (cargador de clases único
y `ServiceLoader`); registro; guardia de país; API de KYC sin estado; `GET /plugins`; health.
**Historias:** US-2.2, US-2.3, US-2.4, US-2.5.
**Salida para otras unidades.** JAR plano de `core` (contratos y manifiesto) y JAR ejecutable de Spring Boot.
**Estructura de código.**
```
core/pom.xml
core/src/main/java/com/synkra/core/{spi,manifest,plugin,web,config}/
core/src/main/resources/application.yaml
core/src/test/java/com/synkra/core/...
```

## U-plugins — Plugins de país

**Responsabilidades.** Componentes C2 y C3: implementaciones de los tres puntos de extensión,
catálogo de reglas con `source` o `verificar`, manifiesto `plugin.yaml`, directorio `changes/`.
**Historias:** US-2.6, US-2.7, US-2.8, US-2.9, US-2.10.
**Dependencia dura:** el JAR plano de `core`. Cada plugin es independiente del otro.
**Estructura de código.**
```
plugins/pom.xml                           (agregador de los dos plugins)
plugins/co/pom.xml
plugins/co/src/main/java/com/synkra/plugin/colombia/...
plugins/co/src/main/resources/plugin.yaml
plugins/co/src/main/resources/META-INF/services/...
plugins/co/changes/                       (recibe archivos del agente)
plugins/pe/...                            (misma estructura, paquete com.synkra.plugin.peru)
```

## U-agent — Agente, evaluación y límite de autonomía

**Responsabilidades.**
- C4.1 a C4.14: CLI, lector de cambios, inventario, localizador determinista, resolutor de evidencia, informes, narrador, `LlmProvider` con `claude` y `fake`, presupuesto, cliente Git con lista blanca, proponente.
- C5: dataset de 20 cambios, `score.py`, casos de red-teaming.
- Límite de autonomía: protección de `main` en GitHub, token del agente restringido, comprobación de que el agente no fusiona ni aprueba, ServiceAccount `agent` sin permisos de escritura (`deploy/agent-identity/`), declaración del trade-off en el README.

**Historias:** US-1.1 a US-1.12, US-3.1, US-4.1 a US-4.6.
**Dependencias:** el JAR plano de `core` (modelo del manifiesto) y, como datos, los manifiestos de `U-plugins`.
**Estructura de código.**
```
agent/pom.xml
agent/src/main/java/com/synkra/agent/{cli,change,inventory,locator,report,llm,budget,git,propose}/
agent/src/main/java/com/synkra/agent/llm/{claude,fake}/
agent/src/test/java/com/synkra/agent/...   (incluye RedTeam*)
eval/dataset/*.yaml   eval/expected/*.json   eval/score.py   eval/tests/
deploy/agent-identity/                       (ServiceAccount; propiedad de esta unidad)
scripts/configure-github.sh                  (protección de main y permisos del token)
README.md                                    (sección "perímetro")
```
**Propiedad de rutas.** `deploy/agent-identity/` es de `U-agent`; `U-deploy` la referencia y no la edita.
**Orden interno.** (1) protección de `main` y token; (2) `analyze`; (3) `propose`; (4) evaluación y red-teaming.

## U-deploy — Despliegue, seguridad del clúster, GitOps y Job

**Responsabilidades.** C6: imagen distroless del kernel y imagen única `synkra/plugins` (Q7-A),
base de Kustomize, overlays `co` y `pe`, init container, ConfigMap, probes, `make deploy`,
Pod Security `restricted`, NetworkPolicy de egress del agente, Secret montado por archivo,
Argo CD, Job del agente.
**Historias:** US-2.1, US-2.11, US-2.12, US-2.13, US-2.14, US-2.15, US-2.16, US-2.17, US-2.18.
**Dependencias:** `U-core` (imagen), `U-plugins` (JAR), `U-agent` (CLI del Job y ServiceAccount).
**Estructura de código.**
```
images/core/Dockerfile       images/plugins/Dockerfile
deploy/base/...              deploy/overlays/{co,pe}/...
deploy/security/...          deploy/gitops/...         deploy/agent-job/...
Makefile                     (objetivos images, deploy, status)
```
**Reglas.** El agente nunca ejecuta `kubectl apply`; lo hace una persona con `make deploy` o Argo CD.

---

## Etapas de diseño por unidad (Construction)

Basado en `execution-plan.md`. La columna Code Generation Part 1 es el entregable final y el
punto de parada de cada unidad.

| Unidad | Functional Design | NFR Requirements | NFR Design | Infrastructure Design | Code Generation Part 1 |
|---|---|---|---|---|---|
| U-scaffold | SKIP (sin lógica) | SKIP | SKIP | SKIP | EXECUTE |
| U-core | EXECUTE (registro, guardia de país, manifiesto, contratos) | SKIP (pila decidida) | SKIP | SKIP | EXECUTE |
| U-plugins | EXECUTE (reglas de país con fuente primaria) | SKIP | SKIP | SKIP | EXECUTE |
| U-agent | EXECUTE (localizador, informe, presupuesto, lista blanca) | EXECUTE (inyección, gasto, límite de autonomía) | **SKIPPED por el usuario** (2026-10-17) | **SKIPPED por el usuario** (2026-10-17) | EXECUTE |
| U-deploy | SKIP (sin lógica de negocio) | **SKIPPED por el usuario** (2026-10-17) | **SKIPPED por el usuario** (2026-10-17) | **SKIPPED por el usuario** (2026-10-17) | EXECUTE |

Las etapas omitidas por el usuario se reemplazan por las secciones «Decisiones tomadas sin
etapa de diseño» de `U-agent-code-generation-plan.md` y `U-deploy-code-generation-plan.md`.

Nota: el plan de ejecución preveía Infrastructure Design para despliegue, endurecimiento, GitOps
y Job. Al asignar el límite de autonomía a `U-agent` (Q3-C), esa unidad también necesita
Infrastructure Design por la configuración de GitHub y la ServiceAccount.

## Estrategia de organización del código (greenfield, multi-módulo)

- Código de aplicación en la raíz del repositorio, nunca en `aidlc-docs/`.
- Un módulo Maven por directorio; `pom.xml` raíz como agregador y gestor de versiones.
- Documentos de código por unidad: `aidlc-docs/construction/{unidad}/code/` (solo Markdown).
- Planes de código: `aidlc-docs/construction/plans/{unidad}-code-generation-plan.md`.
