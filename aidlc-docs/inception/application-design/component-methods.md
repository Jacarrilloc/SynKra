# Métodos de los componentes — SynKra

Firmas de alto nivel. Son notación de diseño, no código. Las reglas de negocio detalladas se
definen en Functional Design, por unidad.

## C1 — `core`

### C1.1 Contratos de extensión (`com.synkra.core.spi`)

| Método | Propósito | Entrada → Salida |
|---|---|---|
| `DocumentValidator.validate(DocumentRequest)` | Validar el número y tipo de documento según la regla del país | `DocumentRequest(type, number)` → `ValidationResult(valid, reasonCodes, ruleRefs)` |
| `ConsentPolicy.requirements()` | Declarar qué debe contener el consentimiento | — → `ConsentRequirements(requiredFields, purposeRequired, ruleRefs)` |
| `RetentionPolicy.retention(DataCategory)` | Declarar el periodo de retención por categoría | `DataCategory` → `RetentionRule(period, ruleRefs)` |
| `<each>.country()` | País del plugin (para el registro) | — → `String` (código alpha-2) |

`ruleRefs` apunta a las reglas del catálogo con su `source` o su marca `verificar` (RF-12).

### C1.2 Modelo de manifiesto (`com.synkra.core.manifest`)

| Método | Propósito | Entrada → Salida |
|---|---|---|
| `ManifestReader.read(Path)` | Leer y validar un `plugin.yaml` | `Path` → `PluginManifest` o `ManifestError` |
| `PluginManifest.country()`, `.version()`, `.extensionPoints()`, `.topics()` | Datos del plugin | — → valores |
| `ExtensionPointBinding(point, implementationClass, sourceFile)` | Liga cada punto con su clase y archivo | — |
| `TopicBinding(topic, point)` | Liga un tema con el punto de extensión que lo cubre | — |

### C1.3 y C1.4 Cargador y registro (`com.synkra.core.plugin`)

| Método | Propósito | Entrada → Salida |
|---|---|---|
| `PluginLoader.load(Path dir)` | Listar JAR, crear un cargador de clases, descubrir implementaciones con `ServiceLoader` y leer cada manifiesto | `Path` → `List<LoadedPlugin>` |
| `PluginRegistry.register(LoadedPlugin)` | Guardar un plugin | `LoadedPlugin` → — |
| `PluginRegistry.forCountry(String)` | Obtener el plugin de un país | código → `Optional<LoadedPlugin>` |
| `PluginRegistry.all()` | Todos los plugins cargados | — → `List<LoadedPlugin>` |

### C1.5 a C1.7 Web (`com.synkra.core.web`)

| Método | Propósito | Entrada → Salida |
|---|---|---|
| `CountryGuard.check(String country)` | Comparar con el país activo | código → pasa o `CountryNotServed` (HTTP 404) |
| `KycController.validateDocument(country, body)` | Delegar en `DocumentValidator` | → `ValidationResult` |
| `KycController.consentPolicy(country)` | Delegar en `ConsentPolicy` | → `ConsentRequirements` |
| `KycController.retentionPolicy(country, category)` | Delegar en `RetentionPolicy` | → `RetentionRule` |
| `PluginsController.list()` | Listar plugins cargados | — → `List<PluginSummary(id, country, version, extensionPoints)>` |

Rutas provisionales: `POST /kyc/{country}/document/validate`, `GET /kyc/{country}/consent`,
`GET /kyc/{country}/retention`, `GET /plugins`, `GET /actuator/health`.

### C1.8 Configuración

| Propiedad | Propósito |
|---|---|
| `synkra.country` | País activo del despliegue |
| `synkra.plugins.dir` | Directorio de JAR de plugin |

## C2 y C3 — plugins

| Método | Propósito |
|---|---|
| `<Pais>DocumentValidator.validate`, `<Pais>ConsentPolicy.requirements`, `<Pais>RetentionPolicy.retention` | Implementaciones de los contratos de C1.1 |
| `RuleCatalog.rules()` | Lista de reglas con `source` o `verificar` |

## C4 — `agent`

### C4.1 CLI

| Método | Propósito | Entrada → Salida |
|---|---|---|
| `AnalyzeCommand.call()` | Ejecutar `analyze` | ruta del YAML → código 0, 1 o 2 |
| `ProposeCommand.call()` | Ejecutar `propose` | id del cambio → código 0 o 1 |

### C4.2 a C4.5 Localización

| Método | Propósito | Entrada → Salida |
|---|---|---|
| `ChangeReader.read(Path)` | Leer y validar el YAML | `Path` → `ChangeRequest(id, country, topic, text, hash)` o `InvalidChange(field)` |
| `PluginInventory.list(Path pluginsRoot)` | Leer todos los manifiestos | `Path` → `List<PluginEntry(dir, manifest)>` |
| `ChangeLocator.locate(ChangeRequest, List<PluginEntry>)` | Decidir afectado o descartado por plugin, o abstenerse | → `Localization(verdicts, abstention?)` |
| `Verdict(plugin, status, extensionPoint?, reason)` | Una fila del informe | — |
| `EvidenceResolver.resolve(Verdict, Path repoRoot)` | Archivos reales que respaldan un veredicto | → `List<Path>` o error si no existen |

### C4.6 y C4.7 Informe

| Método | Propósito | Entrada → Salida |
|---|---|---|
| `ReportWriter.write(ChangeRequest, Localization, Narrative, TokenUse)` | Escribir `informes/<id>.md` con la tabla de formato fijo | → `Path` |
| `ReportReader.read(String id)` | Leer el informe para `propose` | id → `Report` o `ReportMissing` |
| `Narrator.reportText(Localization)` / `.prBody(Report)` | Texto redactado por el modelo; no cambia veredictos | → `Narrative` |

### C4.8 a C4.11 Modelo y presupuesto

| Método | Propósito | Entrada → Salida |
|---|---|---|
| `LlmProvider.complete(LlmRequest)` | Pedir texto al modelo | `LlmRequest(prompt, maxTokens)` → `LlmResponse(text, tokensIn, tokensOut)` |
| `LlmProviderFactory.create(String name)` | Elegir `claude` o `fake` según `LLM_PROVIDER` | → `LlmProvider` |
| `TokenBudget.reserve(int tokens)` | Verificar que cabe en el presupuesto del día | → `Granted` o `Exhausted` ("presupuesto agotado", sin llamar al modelo) |
| `TokenBudget.record(int tokens)` | Acumular el gasto del día | — |
| `TokenLog.append(runId, tokens)` | Una línea por ejecución en `tokens.log` | — |

### C4.12 a C4.14 Git y propuesta

| Método | Propósito | Entrada → Salida |
|---|---|---|
| `CommandWhitelist.allow(List<String> argv)` | Decidir si un comando está permitido | → `true` o `Denied(reason)` |
| `GitClient.run(List<String> argv)` | Ejecutar un comando permitido; lanza si no lo está | → `ProcessResult` |
| `GitClient.createBranch(String id)` | `git checkout -b synkra/<id>` | → — |
| `GitClient.commitPaths(List<Path>, String message)` | Añadir solo rutas permitidas y confirmar | → — |
| `GitClient.pushBranch(String id)` | `git push origin synkra/<id>` | → — |
| `GitClient.createDraftPr(String id, String title, String body)` | `gh pr create --draft` | → `PrRef(number, url)` |
| `ChangeRequestFileWriter.write(Verdict, ChangeRequest, List<Path> evidence)` | Escribir `plugins/<pais>/changes/<id>.md` | → `Path` |
| `ProposalService.propose(String id)` | Orquestar todo lo anterior | id → `PrRef` o error |

## C5 — `eval`

| Función | Propósito |
|---|---|
| `score.py: load_expected(dir)`, `parse_report(path)`, `score(expected, actual)` | Comparar y calcular recall, precisión, tasa de abstención |

## C6 — `deploy`

Sin métodos. Interfaces: `make cluster`, `make deploy COUNTRY=xx`, `kubectl kustomize deploy/overlays/xx`.
