# Servicios y orquestación — SynKra

Un servicio es la pieza que coordina varios componentes para una acción completa.

## S1 — Servicio de KYC (en `core`)

**Responsabilidad.** Atender una petición KYC con el plugin del país activo.
**Coordina:** C1.5 Guardia de país → C1.4 Registro → plugin (C2 o C3).

```
Petición /kyc/{country}/...
  1. CountryGuard.check(country)         -> 404 si no es el país activo
  2. PluginRegistry.forCountry(country)  -> 404 si el plugin no está cargado
  3. Punto de extensión del plugin       -> resultado
  4. Respuesta JSON                      (sin estado, sin base de datos)
```

**Arranque.** Al iniciar, `PluginLoader.load(synkra.plugins.dir)` registra los plugins. La
sonda de readiness (`/actuator/health`) pasa a `UP` cuando el plugin del país activo está
cargado.

## S2 — Servicio de análisis (`AnalyzeService`, en `agent`)

**Responsabilidad.** Convertir un cambio YAML en un informe de localización sin efectos
secundarios sobre Git.
**Coordina:** C4.2, C4.3, C4.4, C4.5, C4.7, C4.8 (vía C4.11) y C4.6.

```
synkra analyze cambios/<id>.yaml
  1. ChangeReader.read              -> ChangeRequest (o error, código 1)
  2. PluginInventory.list           -> manifiestos de plugins/*
  3. ChangeLocator.locate           -> Localization (determinista)
  4. EvidenceResolver.resolve       -> archivos reales por veredicto
  5. Si hay abstención              -> informe de abstención, código 2, fin
  6. TokenBudget.reserve            -> Exhausted: "presupuesto agotado", sin llamar al modelo
  7. Narrator.reportText            -> texto vía LlmProvider (solo prosa)
  8. TokenBudget.record + TokenLog  -> gasto registrado
  9. ReportWriter.write             -> informes/<id>.md
```

Reglas del servicio:
- La decisión (pasos 3 y 4) se toma antes de tocar el modelo y no depende de su respuesta (P5, RF-41).
- Si el presupuesto está agotado, el informe se escribe igualmente con los veredictos y sin prosa del modelo, con la nota "presupuesto agotado" (el análisis determinista es gratuito).
- No hay llamadas a `GitClient`; `git status` solo ve `informes/<id>.md`.

## S3 — Servicio de propuesta (`ProposalService`, en `agent`)

**Responsabilidad.** Abrir un PR en borrador a partir de un informe existente.
**Coordina:** C4.6 (lectura), C4.12/C4.13 (Git detrás de la lista blanca), C4.7 y C4.14.

```
synkra propose <id>
  1. ReportReader.read(id)           -> falla con código 1 si no hay informe (Q9-C)
  2. Si el informe es de abstención  -> falla: no hay plugin al que proponer
  3. GitClient.createBranch(id)      -> synkra/<id>
  4. Por cada plugin afectado:
       ChangeRequestFileWriter.write -> plugins/<pais>/changes/<id>.md
  5. GitClient.commitPaths           -> solo rutas bajo los plugins del informe
  6. GitClient.pushBranch            -> origin synkra/<id>
  7. Narrator.prBody (vía LlmProvider y TokenBudget)
  8. GitClient.createDraftPr         -> PR en borrador
  9. Salida: enlace del PR
```

Reglas del servicio:
- Ejecutar `propose` es la aprobación del humano (Q9-C, F3-A). No hay confirmación interactiva.
- Todo proceso pasa por `CommandWhitelist`. No existe ningún camino de código a `merge`,
  `push` a otra rama, `gh pr merge` o `gh pr review` (AUTONOMIA-01).
- Las rutas añadidas al commit deben estar bajo `plugins/<pais>/changes/` de un plugin
  afectado; cualquier otra ruta hace fallar el servicio antes del commit (US-1.8).

## S4 — Servicio de presupuesto (`TokenBudget`, en `agent`)

**Responsabilidad.** Garantizar el tope diario y dejar rastro (P4).
**Estado.** Un archivo por día en el directorio de estado del agente (Q11-A):
`budget-YYYY-MM-DD.json` con el total gastado, y `tokens.log` con una línea por ejecución.
**Regla.** Con tope 0 o gasto agotado, `reserve` devuelve `Exhausted` y ningún adaptador hace
red (AUTONOMIA-02).

## S5 — Servicio de evaluación (`eval`)

**Responsabilidad.** Ejecutar `analyze` sobre el dataset con `fake` y puntuar.

```
for cada YAML del dataset:  synkra analyze <yaml> (LLM_PROVIDER=fake)
python eval/score.py:       lee informes y respuestas esperadas, imprime recall, precisión, abstención
```

## S6 — Servicio de despliegue (`deploy`, `Makefile`)

**Responsabilidad.** Dejar un país corriendo con solo su plugin.

```
make deploy COUNTRY=xx
  1. kubectl kustomize deploy/overlays/xx  -> manifiestos (render)
  2. kubectl apply -k (lo ejecuta el humano / Argo CD, nunca el agente)
  3. Init container copia el JAR del plugin al volumen /plugins
  4. El kernel arranca con synkra.country=xx y sondas de readiness
```

## Patrones de coordinación

| Patrón | Dónde |
|---|---|
| Orquestación síncrona en un proceso | S2 y S3 (CLI de un solo proceso) |
| Puerto y adaptador | Solo `LlmProvider` (Q5-B) |
| Lista blanca de comandos | S3 vía C4.13 |
| Estado en archivos | S4 |
| Sin estado | S1 |
| Reconciliación GitOps | S6, con Argo CD |
