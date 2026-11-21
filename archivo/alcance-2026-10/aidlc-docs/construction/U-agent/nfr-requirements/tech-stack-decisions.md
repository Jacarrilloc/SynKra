# Decisiones de tecnología — U-agent

Origen: respuestas del plan de NFR Requirements (Q3-B, Q4-A, Q7-B) y decisiones previas
(Application Design: Java 21 plano, picocli, Maven; Functional Design). Las versiones exactas se
fijan al generar el código, consultando la última versión estable en ese momento; este
documento no afirma números de versión.

## Pila por componente

| Componente | Tecnología | Motivo | Alternativas descartadas |
|---|---|---|---|
| Lenguaje y build | Java 21, Maven multi-módulo | Decisión del PRD y de requisitos Q2-A | Gradle (requisitos Q2-A) |
| CLI | picocli, sin Spring | Application Design Q6-B: arranque más rápido y menos dependencias | Spring Shell; Spring Boot sin servidor web |
| Lectura de YAML y JSON | Jackson (YAML y JSON), que llega con `core` | Sin dependencia nueva; mismo lector que el kernel | SnakeYAML directo |
| Modelo del manifiesto | `com.synkra.core.manifest` (JAR plano de `core`) | Application Design F2-B | Copia propia del modelo |
| Adaptador `claude` | SDK oficial de Anthropic para Java, confinado a `com.synkra.agent.llm.claude` | Q3-B | Cliente HTTP del JDK con Jackson |
| Adaptador `fake` | Clase propia, sin dependencias | RF-40: costo cero en CI | Biblioteca de mocks para el proveedor |
| Hash | `MessageDigest` SHA-256 del JDK | BR-CH8; sin dependencia | Biblioteca externa |
| Procesos `git` y `gh` | `ProcessBuilder` con argumentos separados, tras `CommandWhitelist` | Application Design Q7-B | JGit; API REST de GitHub desde Java |
| Presupuesto y log de tokens | Archivos locales (`agent/state/`, `agent/logs/`); JSON con Jackson | Application Design Q11-A | ConfigMap de Kubernetes |
| Empaquetado | JAR ejecutable único (plugin Maven para empaquetar dependencias) y script `bin/synkra` | Q4-A | Imagen nativa con GraalVM; `make install-cli` (Q4-B no elegida) |
| Pruebas Java | JUnit 5 | Estándar; hereda la raíz de `U-scaffold` | TestNG |
| Pruebas del adaptador `claude` | Servidor HTTP local de prueba con la URL base del SDK sustituida | AUTONOMIA-02: ninguna prueba toca la API real | Mocks del SDK |
| Pruebas de `git` y `gh` | Repositorios Git temporales con un remoto local, y un script `gh` de prueba al inicio del `PATH` que registra los argumentos | Sin red y sin GitHub | Pruebas contra GitHub real |
| Evaluación | Python 3, solo biblioteca estándar (`json`, `re`, `pathlib`) y `unittest` | `score.py` lee Markdown y JSON, no YAML; sin dependencias que instalar | pandas, PyYAML |
| Entorno | Linux o WSL2, Java 21, Maven, Python 3, `git`, `gh` | R10 del PRD y `U-scaffold` | Windows nativo |

## Cómo se aplican las decisiones de riesgo

| Riesgo | Decisión técnica |
|---|---|
| El SDK de Anthropic trae dependencias que pueden chocar con las de `core` (Jackson) | Una sola versión de Jackson gestionada desde el `dependencyManagement` del POM raíz; el árbol de dependencias del agente se revisa en la comprobación final de la unidad |
| `core` arrastra Spring Boot web, y el agente no lo necesita (NFR-AG-PT2) | NFR Design decide cómo excluirlo (exclusiones en el POM del agente o separación del modelo del manifiesto) |
| Dos ejecuciones simultáneas (Q9-B) | Sin bloqueo; se documenta y el Job se define sin concurrencia en `U-deploy` |
| Sin comprobación de permisos de archivos secretos (Q5-B) | Recomendación documentada en el README del agente |

## Estructura de archivos resultante (referencia para Code Generation)

```
agent/pom.xml
agent/README.md
agent/src/main/java/com/synkra/agent/{cli,change,inventory,locator,report,llm,budget,git,propose}/
agent/src/main/java/com/synkra/agent/llm/{claude,fake}/
agent/src/test/java/com/synkra/agent/...
bin/synkra
eval/score.py   eval/tests/   eval/README.md   eval/dataset/   eval/expected/
```

## Pendiente de las siguientes etapas

- **NFR Design:** exclusión de Spring del classpath del agente; patrón de la lista blanca; patrón de reserva de presupuesto; confinamiento del SDK.
- **Infrastructure Design:** protección de `main`, token del agente con permisos mínimos, ServiceAccount `agent`, cómo recibe el Job el repositorio, dónde persiste el presupuesto en Kubernetes.
