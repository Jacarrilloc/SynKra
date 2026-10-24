# Plan de Code Generation — U-agent

**Estado:** Part 1 (Planning). Este plan es la fuente única de verdad para la Parte 2. En este
trabajo **no se ejecuta la Parte 2**: no se escribe ningún archivo de código ni de configuración.
**Diseño previo aprobado:** `U-agent/functional-design/` y `U-agent/nfr-requirements/`.
**Etapas omitidas por instrucción del usuario:** NFR Design e Infrastructure Design de esta
unidad (ver «Decisiones tomadas sin etapa de diseño»; se corrigen al aprobar este plan).

## Contexto de la unidad

| Campo | Valor |
|---|---|
| Propósito | Agente (`analyze`, `propose`), localizador determinista, informe, narrador, `LlmProvider`, presupuesto, lista blanca de Git, evaluación y límite de autonomía |
| Historias (19) | US-1.1 a US-1.12, US-3.1, US-4.1 a US-4.6 |
| Requisitos | RF-30 a RF-36, RF-40 a RF-44, RF-50 a RF-52, RF-61; NFR-01, NFR-04, NFR-05, NFR-06, NFR-08, NFR-09, NFR-10 |
| Depende de | `U-scaffold`, `U-core` (modelo del manifiesto, JAR plano) y, como datos, `U-plugins` (`plugin.yaml`, `changes/`) |
| Lo usan | `U-deploy` (imagen del agente, Job; referencia `deploy/agent-identity/`) |
| Propietario de | `agent/`, `bin/`, `eval/`, `deploy/agent-identity/`, `scripts/configure-github.sh`, `scripts/verify-autonomy.sh`, sección «perímetro» del `README.md` |
| Orden interno (Q4) | Límite de autonomía (pasos 1 a 4) → código (pasos 5 a 22) → evaluación y documentación (pasos 23 a 27) |

**Convención de comandos.** `PL="mvn -q -am -Dsurefire.failIfNoSpecifiedTests=false"`; se ejecutan
en la raíz del repositorio, en Linux o WSL2. Los pasos marcados **[PERSONA]** los ejecuta un
humano con credenciales de administrador; el agente nunca ejecuta configuración de GitHub ni de
Kubernetes (AUTONOMIA-01).

## Decisiones tomadas sin etapa de diseño (corrígelas al aprobar)

**NFR Design** (preguntas Q1 a Q7 de `U-agent-nfr-design-plan.md` sin responder; se usa la primera opción de cada una):

| # | Decisión por defecto |
|---|---|
| N1 | Spring fuera del classpath del agente mediante **exclusiones** en `agent/pom.xml` sobre su dependencia de `core`. No se modifica `U-core` |
| N2 | Lista blanca como **tabla declarativa** de patrones con validadores y un único `ProcessRunner` que usa `ProcessBuilder`; una prueba lo hace cumplir |
| N3 | Tiempos máximos de `git` y `gh`: 60 s locales y 120 s con red; al vencer, error `1` y recuperación de BR-PR14 |
| N4 | `git push` se autentica con un script `bin/synkra-askpass` (`GIT_ASKPASS`), solo en el entorno del proceso hijo, que imprime el token de `AGENT_TOKEN_FILE` |
| N5 | Red de seguridad: registro central de secretos y filtro `***` sobre toda salida (estándar, error, logs, excepciones, cuerpo del PR) |
| N6 | Reglas de arquitectura hechas cumplir con pruebas JUnit de texto, sin ArchUnit |
| N7 | El informe lleva `formato: 1`; `ReportReader` y `score.py` rechazan otras versiones |
| N8 a N13 | Las seis decisiones propias del plan de NFR Design: `System.Logger` del JDK; tres tipos de excepción mapeados a `0/1/2`; `Clock` inyectable en UTC; escritura atómica con archivo temporal y renombrado; presupuesto reservar y luego registrar; el token de GitHub solo en el entorno del proceso hijo |

**Infrastructure Design** (omitida; valores por defecto):

| # | Decisión por defecto |
|---|---|
| I1 | El agente se identifica como una **cuenta de máquina** de GitHub (`AGENT_GH_USER`) con rol de escritura (nunca administrador) y un token de acceso fino limitado a este repositorio con permisos de contenido y de pull requests. La cuenta y el token los crea una persona |
| I2 | `main` se protege con **revisión obligatoria** (al menos 1 aprobación, sin empujes forzados ni borrado) |
| I3 | Un **ruleset** del repositorio (`synkra-agent-boundary`) restringe crear y modificar ramas fuera de `synkra/**` para quien no sea administrador; es lo que hace que el token del agente solo llegue a `synkra/*` |
| I4 | Namespace `synkra` y ServiceAccount `agent` en `deploy/agent-identity/`, sin ningún Role ni RoleBinding y con `automountServiceAccountToken: false` |
| I5 | La verificación de que `main` rechaza un empuje directo **no** se hace con un empuje real (si fallara la protección, el commit quedaría en `main`). Se verifica con la API de reglas (ver paso 3 y la enmienda a US-1.10) |
| I6 | `gh pr merge` sobre un PR en borrador falla siempre; esa prueba por sí sola no demuestra el permiso. La garantía real es la revisión obligatoria de `main` (I2) más el rol de escritura sin administración (I1) |

## Interfaces y contratos que entrega

| Entrega | Consumidor | Contrato |
|---|---|---|
| `agent/target/agent-*-all.jar` y `bin/synkra` | Personas, `U-deploy` | `synkra analyze <archivo>` y `synkra propose <id>`; códigos `0`, `1`, `2` |
| `bin/synkra-askpass` | `U-deploy` (imagen del agente) | Imprime el token de `AGENT_TOKEN_FILE` |
| `deploy/agent-identity/` | `U-deploy` | Namespace `synkra` y ServiceAccount `agent` sin permisos |
| `eval/` | Personas | `eval/dataset/`, `eval/expected/`, `python eval/score.py` |

## Pasos de generación

Cada paso termina con el comando que demuestra que está hecho (AUTONOMIA-03).

### Bloque A — Límite de autonomía (antes de la primera tarea que abre un PR)

- [ ] **Paso 1 — `scripts/configure-github.sh` [PERSONA].**
  Script idempotente con `--dry-run` que, con credenciales de administrador, aplica I2 e I3 mediante `gh api` sobre `Jacarrilloc/SynKra`: protección de `main` con al menos 1 revisión, sin empujes forzados ni borrado, y el ruleset `synkra-agent-boundary`. *Historias US-1.9 y US-1.10.*
  *Verificación:* `bash -n scripts/configure-github.sh && scripts/configure-github.sh --dry-run | grep -c "branches/main/protection"` imprime al menos `1`; ejecutado por la persona, `gh api repos/Jacarrilloc/SynKra/branches/main/protection --jq '.required_pull_request_reviews.required_approving_review_count'` imprime al menos `1` y `gh api repos/Jacarrilloc/SynKra/rulesets --jq '[.[]|select(.name=="synkra-agent-boundary")]|length'` imprime `1`.

- [ ] **Paso 2 — Identidad del agente en GitHub [PERSONA].**
  Documentar en `agent/README.md` el procedimiento de I1: crear la cuenta de máquina, darle rol de escritura, crear el token con permisos mínimos, guardarlo en un archivo con modo `0600` y apuntar `AGENT_TOKEN_FILE`. *Historia US-1.10.*
  *Verificación:* `grep -c "AGENT_TOKEN_FILE" agent/README.md` imprime al menos `1` y, ejecutado por la persona, `gh api repos/Jacarrilloc/SynKra/collaborators/$AGENT_GH_USER/permission --jq .permission` imprime `write` (no `admin`).

- [ ] **Paso 3 — `scripts/verify-autonomy.sh` y enmienda a US-1.10 [PERSONA].**
  Script de solo lectura y de pruebas negativas seguras, que la persona ejecuta con `AGENT_TOKEN` y `--pr <n>` (un PR en borrador; los intentos de fusionar y aprobar son pruebas negativas que deben fallar, nunca una tarea del agente): (a) revisión obligatoria en `main`; (b) el token del agente no puede crear una referencia fuera de `synkra/` (`gh api -X POST .../git/refs` con `refs/heads/outside-test` debe fallar; si tuviera éxito, el script lo informa como FALLO y pide a la persona borrarla); (c) la API de reglas muestra que `main` exige `pull_request` (`gh api repos/Jacarrilloc/SynKra/rules/branches/main --jq '[.[].type]'` contiene `pull_request`); (d) `gh pr merge <n>` y `gh pr review <n> --approve` con el token del agente fallan; (e) `kubectl auth can-i create deployments` y `kubectl auth can-i '*' '*'` con `--as=system:serviceaccount:synkra:agent -n synkra` responden `no`. Imprime `PASS` o `FALLO` por comprobación y termina distinto de cero si alguna falla. **Enmienda a US-1.10 (criterio 2):** se reemplaza el empuje real `git push origin HEAD:main` por la comprobación (c), porque un empuje real puede dejar un commit en `main` si la protección falla. *Historias US-1.9 a US-1.12.*
  *Verificación:* `bash -n scripts/verify-autonomy.sh && scripts/verify-autonomy.sh --dry-run | grep -c "comprobación"` imprime `5`.

- [ ] **Paso 4 — `deploy/agent-identity/` (US-1.12).**
  Crear `namespace.yaml` (namespace `synkra`), `serviceaccount.yaml` (`agent`, sin montaje automático del token) y `kustomization.yaml`. Ningún Role, ClusterRole ni binding (I4).
  *Verificación:* `kubectl kustomize deploy/agent-identity | grep -cE "kind: (Role|ClusterRole|RoleBinding|ClusterRoleBinding)"` imprime `0`, y con el clúster en marcha tras aplicarlo una persona, `kubectl auth can-i create deployments --as=system:serviceaccount:synkra:agent -n synkra` imprime `no`.

### Bloque B — Código del agente

- [ ] **Paso 5 — `agent/pom.xml`.**
  Reemplazar el esqueleto: dependencia de `com.synkra:core` con exclusiones de Spring (N1), picocli, SDK oficial de Anthropic, JUnit 5; empaquetado de un JAR ejecutable único; todas las versiones fijas (la versión estable más reciente al generar). *NFR-AG-PT2, NFR-AG-MT2.*
  *Verificación:* `$PL -pl agent package -DskipTests` termina en `0`, `mvn -pl agent dependency:tree | grep -ciE "spring-boot-starter-web|tomcat"` imprime `0` y `grep -n -e '\[' -e LATEST -e RELEASE agent/pom.xml` no imprime nada.

- [ ] **Paso 6 — Tipos, errores y utilidades transversales.**
  Crear `ChangeRequest`, `Verdict`, `Localization`, `Report`, `TokenUse`, las excepciones de entrada, operativa y de abstención con `ExitCodeMapper` (`0/1/2`), `Clock` inyectable, `AtomicFiles` (temporal y renombrado) y `SecretRegistry` con el filtro de salida `***` (N5, N8 a N13).
  *Verificación:* `$PL -pl agent test -Dtest=ExitCodeMapperTest,AtomicFilesTest,SecretRegistryTest` termina en `0`.

- [ ] **Paso 7 — `ChangeReader` (BR-CH1 a BR-CH8).**
  Leer y validar el YAML: id del nombre de archivo, campos obligatorios y desconocidos, país ausente (abstención), país mal formado, tema fuera del conjunto, huella SHA-256. *Historias US-1.1, US-4.1.*
  *Verificación:* `$PL -pl agent test -Dtest=ChangeReaderTest` termina en `0`, con una prueba por BR-CH1 a BR-CH8 (`grep -c "BR_CH" agent/src/test/java/com/synkra/agent/change/ChangeReaderTest.java` imprime al menos `8`).

- [ ] **Paso 8 — `PluginInventory` (BR-IN1 a BR-IN3).**
  Leer `plugins/*/plugin.yaml` con el `ManifestReader` de `core`, ordenado, y error si un manifiesto es inválido.
  *Verificación:* `$PL -pl agent test -Dtest=PluginInventoryTest` termina en `0`.

- [ ] **Paso 9 — `ChangeLocator` y `EvidenceResolver` (BR-LC1 a BR-LC8).**
  Veredicto por plugin, abstención, motivos, evidencia verificada con `git ls-files`. Incluye la prueba con manifiesto de prueba que omite un tema. *Historias US-1.3, US-1.4, US-4.1.*
  *Verificación:* `$PL -pl agent test -Dtest=ChangeLocatorTest,EvidenceResolverTest,LocatorDeterminismTest` termina en `0`, con pruebas nombradas `BR_LC1` a `BR_LC8`.

- [ ] **Paso 10 — Informe: escritor y lector (BR-RP1 a BR-RP5, N7).**
  `ReportWriter` (Markdown con tabla fija, `formato: 1`, escritura atómica, sin texto del cambio) y `ReportReader`. *Historias US-1.2, US-1.3, US-4.1.*
  *Verificación:* `$PL -pl agent test -Dtest=ReportWriterTest,ReportWriterAtomicTest,ReportReaderTest` termina en `0`.

- [ ] **Paso 11 — Puerto `LlmProvider`, `fake` y fábrica (BR-NR7, BR-NR9).**
  Interfaz, adaptador `fake` (texto con la palabra `fake`, conteos deterministas), fábrica por `LLM_PROVIDER` (por defecto `fake`; otro valor desconocido es error) y configuración con los valores por defecto. *Historia US-1.5.*
  *Verificación:* `$PL -pl agent test -Dtest=LlmProviderFactoryTest,FakeLlmProviderTest,ConfigDefaultsTest` termina en `0`.

- [ ] **Paso 12 — `TokenBudget` y `TokenLog` (BR-BU1 a BR-BU6).**
  Reserva antes de la llamada y registro real después, día en UTC, archivos en `agent/state/` y `agent/logs/`, con presupuesto 0 siempre agotado. *Historia US-4.3.*
  *Verificación:* `$PL -pl agent test -Dtest=TokenBudgetTest,BudgetExhaustedTest,TokenLogTest` termina en `0`, con pruebas nombradas `BR_BU1` a `BR_BU6`.

- [ ] **Paso 13 — `Narrator` (BR-NR2 a BR-NR6).**
  Entrada solo con veredictos, país y tema; prompt del sistema; manejo de «presupuesto agotado» y «narrativa no disponible»; sin llamada en abstención. *Historias US-1.5, US-4.2, US-4.3.*
  *Verificación:* `$PL -pl agent test -Dtest=NarratorTest,PromptContentTest,NarratorFailureTest` termina en `0`.

- [ ] **Paso 14 — Adaptador `claude` (BR-NR8, NFR-AG-PF2, NFR-AG-SC6).**
  Adaptador con el SDK de Anthropic confinado a `com.synkra.agent.llm.claude`, clave leída de `ANTHROPIC_API_KEY_FILE`, espera máxima de 30 segundos, modelo configurable. Se prueba contra un servidor HTTP local de prueba con la URL base sustituida. *Historias US-1.5, US-4.4.*
  *Verificación:* `$PL -pl agent test -Dtest=ClaudeAdapterTest,ClaudeAdapterTimeoutTest` termina en `0` y `grep -rln "com.anthropic" agent/src | grep -v "/llm/claude/"` no imprime nada.

- [ ] **Paso 15 — `ProcessRunner` y `CommandWhitelist` (BR-WL1 a BR-WL16).**
  Tabla declarativa de patrones permitidos con validadores, denegación por defecto, tiempos máximos de 60 y 120 segundos, entorno mínimo, registro de intentos denegados. *Historias US-1.10, US-1.11.*
  *Verificación:* `$PL -pl agent test -Dtest=CommandWhitelistTest,ProcessRunnerTimeoutTest` termina en `0`, con pruebas nombradas `BR_WL1` a `BR_WL16`; una incluye cada comando prohibido (`merge`, `rebase`, `reset`, `push` a otra rama, `gh pr merge`, `gh pr review`, `gh pr ready`, `gh api`, `kubectl`, `helm`).

- [ ] **Paso 16 — `bin/synkra-askpass` y `GitClient` (N4).**
  Script que imprime el token de `AGENT_TOKEN_FILE` (usuario y contraseña según el aviso de `git`) y `GitClient` con las operaciones permitidas, que pasan por la lista blanca. Pruebas con repositorios temporales y un remoto local, y un `gh` de prueba al inicio del `PATH`. *Historia US-1.7.*
  *Verificación:* `bash -n bin/synkra-askpass && $PL -pl agent test -Dtest=GitClientTest` termina en `0`.

- [ ] **Paso 17 — `ChangeRequestFileWriter` (BR-PR7, BR-PR8).**
  Archivo `plugins/<dir>/changes/<id>.md` con ambas huellas, `coinciden: sí/no` y el texto en un bloque cuya valla supera cualquier secuencia de comillas invertidas. *Historias US-1.8, US-4.2.*
  *Verificación:* `$PL -pl agent test -Dtest=ChangeRequestFileWriterTest` termina en `0`, con una prueba de texto con tres, cinco y ocho comillas invertidas.

- [ ] **Paso 18 — `ProposalService` (BR-PR1 a BR-PR16).**
  Orquesta las comprobaciones en orden, la rama desde `origin/main`, los archivos, el commit solo de rutas permitidas, el empuje de `synkra/<id>` y el PR en borrador; recupera la rama original si algo falla. *Historias US-1.6, US-1.7, US-1.8.*
  *Verificación:* `$PL -pl agent test -Dtest=ProposalServiceTest,ProposalFailureRecoveryTest` termina en `0`, en un repositorio temporal con remoto local y `gh` de prueba; incluye que un informe de abstención y un informe ausente fallan sin crear rama.

- [ ] **Paso 19 — `AnalyzeService` (flujo 1).**
  Orquesta lectura, inventario, localización, evidencia, narrativa e informe; abstención sin llamar al modelo; línea en `tokens.log` en todos los casos. *Historias US-1.1 a US-1.5, US-3.1, US-4.1.*
  *Verificación:* `$PL -pl agent test -Dtest=AnalyzeServiceTest` termina en `0`; en un repositorio temporal, tras `analyze`, `git status --porcelain` lista solo `informes/<id>.md`.

- [ ] **Paso 20 — CLI con picocli y `bin/synkra`.**
  Comandos `analyze` y `propose`, `--help`, manejador de salida `0/1/2`, mensajes en español, y el script `bin/synkra` que invoca el JAR. *Historias US-1.1, US-1.6.*
  *Verificación:* `$PL -pl agent test -Dtest=CliExitCodesTest` termina en `0`, y `make build && bin/synkra --help | grep -c -e analyze -e propose` imprime `2` (NFR-AG-PT1).

- [ ] **Paso 21 — Red-teaming y fugas.**
  `RedTeamInjectionTest` con las tres cargas, `RedTeamNoCountryTest`, `RedTeamUnknownCountryTest`, `RedTeamBudgetZeroTest`, `SecretLeakTest` y `LogContentTest` (BR-RT1, NFR-AG-SC2, NFR-AG-SC4, NFR-AG-SC5, NFR-AG-PV2). *Historias US-4.1 a US-4.3.*
  *Verificación:* `$PL -pl agent test -Dtest='RedTeam*,SecretLeakTest,LogContentTest'` termina en `0`.

- [ ] **Paso 22 — Pruebas de arquitectura (N6).**
  Pruebas JUnit que hacen cumplir: el SDK solo en su paquete, `ProcessBuilder` solo en `ProcessRunner`, ninguna referencia a `kubectl`, `helm` ni Kubernetes fuera de la lista de comandos denegados, ninguna dependencia de Spring web, ninguna prueba que contacte `api.anthropic.com`. *NFR-AG-SC6 a SC8.*
  *Verificación:* `$PL -pl agent test -Dtest=ArchitectureRulesTest` termina en `0`.

### Bloque C — Evaluación y documentación

- [ ] **Paso 23 — Dataset de evaluación (BR-EV1).**
  20 archivos `eval/dataset/<id>.yaml` con país en mayúsculas: 10 `CO`, 8 `PE`, 2 de un país sin plugin (por ejemplo `EC` y `AR`), con temas repartidos; y los 20 `eval/expected/<id>.json`. *Historia US-4.5.*
  *Verificación:* `ls eval/dataset/*.yaml | wc -l` imprime `20`, `grep -l "country: CO" eval/dataset/*.yaml | wc -l` imprime `10`, `grep -l "country: PE" eval/dataset/*.yaml | wc -l` imprime `8` y `ls eval/expected/*.json | wc -l` imprime `20`.

- [ ] **Paso 24 — `eval/score.py` y sus pruebas (BR-EV3 a BR-EV5).**
  Solo biblioteca estándar; lee `eval/out/<id>.md` y `eval/expected/`; rechaza `formato` distinto de 1; imprime recall, precisión, tasa de abstención y exactitud de abstención. `unittest` con informes de ejemplo. *Historia US-4.5.*
  *Verificación:* `python3 -m unittest discover -s eval/tests` termina en `0`.

- [ ] **Paso 25 — Corrida de evaluación con `fake`.**
  Script `eval/run.sh`: ejecuta `analyze` sobre los 20 cambios con `LLM_PROVIDER=fake` y `SYNKRA_REPORT_DIR=eval/out`, y luego `score.py`. `eval/README.md` explica cómo subir `DAILY_TOKEN_BUDGET` para una corrida con `claude`.
  *Verificación:* `make build && eval/run.sh` termina en `0` e imprime recall mayor que `0.90`, precisión mayor que `0.70` y tasa de abstención mayor que `0`; `grep -c "DAILY_TOKEN_BUDGET" eval/README.md` imprime al menos `1`.

- [ ] **Paso 26 — README del agente y sección «perímetro» (NFR-AG-MT4, NFR-AG-PV1).**
  `agent/README.md` con comandos, variables de entorno, códigos de salida, permisos recomendados `0600` y «una sola ejecución a la vez»; y la sección del `README.md` raíz, en el marcador de `U-scaffold`, que declara que `LLM_PROVIDER=claude` envía veredictos, país y tema fuera del perímetro. *Historia US-4.6.*
  *Verificación:* `grep -n "perímetro" README.md` imprime la declaración, `grep -c "LLM_PROVIDER=claude" README.md` imprime al menos `1`, `grep -c "0600" agent/README.md` imprime al menos `1` y `grep -c "una sola ejecución" agent/README.md` imprime al menos `1`.

- [ ] **Paso 27 — Resumen de la unidad.**
  `aidlc-docs/construction/U-agent/code/summary.md` (solo Markdown) con archivos creados, pasos a historias, y el estado de las decisiones N1 a N13 e I1 a I6.
  *Verificación:* `test -s aidlc-docs/construction/U-agent/code/summary.md && echo OK` imprime `OK`.

- [ ] **Paso 28 — Comprobación final de la unidad.**
  Suite completa con proveedor `fake`, nombres de prueba por regla y rutas tocadas.
  *Verificación:* `LLM_PROVIDER=fake mvn -q -pl agent -am verify && for r in $(seq -f 'BR_WL%g' 1 16) $(seq -f 'BR_LC%g' 1 8) $(seq -f 'BR_BU%g' 1 6); do grep -rq "$r" agent/src/test || echo FALTA $r; done` termina con la parte final sin imprimir nada, y `git status --porcelain | grep -vE "^( M|\?\?) (agent/|bin/|eval/|deploy/agent-identity/|scripts/|README.md|aidlc-docs/)"` no imprime nada.

## Cobertura de historias

| Historia | Pasos |
|---|---|
| US-1.1 | 7, 19, 20 |
| US-1.2 | 10, 19 |
| US-1.3 | 9, 10 |
| US-1.4 | 9, 19 |
| US-1.5 | 11, 13, 14, 19 |
| US-1.6 | 18, 20 |
| US-1.7 | 16, 18 |
| US-1.8 | 17, 18 |
| US-1.9 | 1, 3 |
| US-1.10 | 1, 2, 3, 15 |
| US-1.11 | 3, 15 |
| US-1.12 | 3, 4 |
| US-3.1 | 19 |
| US-4.1 | 7, 9, 10, 19, 21 |
| US-4.2 | 13, 17, 21 |
| US-4.3 | 12, 13, 21 |
| US-4.4 | 11, 14, 22 |
| US-4.5 | 23, 24, 25 |
| US-4.6 | 26 |

Las 19 historias aparecen en al menos un paso. El **criterio compartido** de US-1.12 (la
ServiceAccount no puede escribir) se completa en `U-deploy` cuando el namespace `synkra` existe.

## Secciones estándar que no aplican

| Sección estándar | Motivo |
|---|---|
| API Layer, Repository Layer, Base de datos, Frontend | El agente es una CLI sin persistencia relacional ni interfaz |
| Deployment Artifacts (imagen, Job) | Corresponden a `U-deploy` |

## Cumplimiento de reglas

| Regla | Estado |
|---|---|
| AUTONOMIA-01 | Cumple. Ningún paso del agente ejecuta `git merge`, `gh pr merge`, `git push` a `main`, `kubectl apply` ni `helm install`; los pasos 1 a 4 son de una persona o de solo lectura; los pasos 3 y 4 comprueban el límite con `gh api` y `kubectl auth can-i ... = no` |
| AUTONOMIA-02 | Cumple. Todo uso del modelo pasa por `LlmProvider`; el paso 12 y el 21 demuestran «presupuesto agotado» sin llamada; la suite corre con `LLM_PROVIDER=fake` (paso 28) |
| AUTONOMIA-03 | Cumple. Los 28 pasos nombran su comando de verificación |

## Resumen del alcance

28 pasos en tres bloques; 1 `pom.xml`, 2 scripts de GitHub, 2 de `bin/`, `deploy/agent-identity/`
(3 archivos), aproximadamente 45 clases principales y 40 clases de prueba, 20 casos y 20
respuestas esperadas, `score.py`, `run.sh` y 2 README. Sin manifiestos de Kubernetes salvo la
identidad.
