# Mapa de historias a unidades — SynKra

Cada una de las 37 historias pertenece a exactamente una unidad. Las verificaciones que cruzan
unidades se listan al final como "criterios compartidos".

## Historias por unidad

### U-scaffold (0 historias)
Sin historias propias (Q6-B). Sostiene a todas las demás.

### U-core (4 historias)

| Historia | Título | Prioridad |
|---|---|---|
| US-2.2 | The kernel defines three extension points and has no country logic | Must |
| US-2.3 | The kernel loads plugins from a directory and lists them | Must |
| US-2.4 | Health endpoints and probes | Must |
| US-2.5 | The KYC API: one endpoint per extension point, stateless, 404 for an unknown country | Must |

### U-plugins (5 historias)

| Historia | Título | Prioridad |
|---|---|---|
| US-2.6 | Colombia plugin | Must |
| US-2.7 | Peru plugin | Must |
| US-2.8 | Each plugin carries a manifest | Must |
| US-2.9 | Each country rule cites a primary source | Must |
| US-2.10 | Plugins build and package independently | Must |

### U-agent (19 historias)

| Historia | Título | Prioridad |
|---|---|---|
| US-1.1 | Register a regulatory change as YAML | Must |
| US-1.2 | Analyze has no side effects on the repository | Must |
| US-1.3 | The report lists every plugin, affected or discarded, with a reason | Must |
| US-1.4 | Localization is decided by deterministic rules | Must |
| US-1.5 | The narrative is drafted through the `LlmProvider` port and cannot change the verdict | Must |
| US-1.6 | `propose` needs an existing report | Must |
| US-1.7 | `propose` opens a draft PR on `synkra/<id>` | Must |
| US-1.8 | The PR touches only the indicated plugin and cites real evidence | Must |
| US-1.9 | `main` requires review | Must |
| US-1.10 | The agent's token reaches only `synkra/*` and draft PRs | Must |
| US-1.11 | The agent cannot merge or approve; a human merges | Must |
| US-1.12 | The agent's cluster identity cannot write | Must |
| US-3.1 | Not running `propose` leaves the repository unchanged | Must |
| US-4.1 | Abstention with a full hand-off | Must |
| US-4.2 | Hostile text in the change cannot redirect the agent | Must |
| US-4.3 | Token budget enforced, with an exhausted-budget response, and logged | Must |
| US-4.4 | Tests never call the model API | Must |
| US-4.5 | Evaluation dataset and scoring | Must |
| US-4.6 | The external-API trade-off is declared | Must |

Orden interno de tareas (Q4): US-1.9, US-1.10, US-1.11 y US-1.12 se planifican antes de US-1.7.

### U-deploy (9 historias)

| Historia | Título | Prioridad |
|---|---|---|
| US-2.1 | Local cluster and tool check | Must |
| US-2.11 | A distroless kernel image with no country plugin inside | Must |
| US-2.12 | One Kustomize overlay per country renders | Must |
| US-2.13 | `make deploy COUNTRY=xx` deploys one country in minutes | Must |
| US-2.14 | At runtime, a country shows only its own plugin | Must |
| US-2.15 | Hardened pods and the API key mounted as a file | Should |
| US-2.16 | Egress allowlist for the agent | Should |
| US-2.17 | Argo CD reconciles one overlay per country | Should |
| US-2.18 | The agent can run as a Kubernetes Job | Should |

## Comprobación de totales

| Unidad | Historias |
|---|---|
| U-scaffold | 0 |
| U-core | 4 |
| U-plugins | 5 |
| U-agent | 19 |
| U-deploy | 9 |
| **Total** | **37** |

Historias huérfanas: 0. Historias en más de una unidad: 0.

## Criterios compartidos entre unidades

| Historia (unidad dueña) | Criterio | Lo verifica además |
|---|---|---|
| US-2.4 (U-core) | Segundo criterio: las probes están en el Deployment desplegado | U-deploy |
| US-2.8 (U-plugins) | Los puntos del manifiesto coinciden con las clases | U-core aporta el lector del manifiesto |
| US-1.12 (U-agent) | `kubectl auth can-i ... --as=system:serviceaccount:synkra:agent` | U-deploy aplica la base que crea el namespace `synkra` |
| US-2.18 (U-deploy) | El Job usa la ServiceAccount `agent` | U-agent define esa ServiceAccount |
| US-4.4 (U-agent) | Las pruebas no llaman a la API | Todas las unidades con pruebas (`LLM_PROVIDER=fake`, NFR-08) |
| US-2.11, US-2.14 (U-deploy) | La imagen y el runtime usan los JAR de U-plugins | U-plugins |

## Cobertura de requisitos por unidad

| Requisito | Unidad |
|---|---|
| RF-01, RF-02, RF-03, RF-04, RF-05, RF-06, RF-07 | U-core |
| RF-10, RF-11, RF-12, RF-13 | U-plugins |
| RF-20, RF-21, RF-22, RF-23, RF-24, RF-25 | U-deploy (RF-23 con apoyo de U-core para el endpoint de health) |
| RF-30 a RF-36 | U-agent |
| RF-37 | U-deploy |
| RF-40 a RF-44 | U-agent |
| RF-50, RF-51, RF-52 | U-agent |
| RF-60, RF-62, RF-63 | U-deploy |
| RF-61 | U-agent |
| NFR-01 | U-agent |
| NFR-02 | U-core |
| NFR-03 | U-deploy |
| NFR-04, NFR-05, NFR-06 | U-agent |
| NFR-07 | U-deploy |
| NFR-08 | U-agent y todas las unidades con pruebas |
| NFR-09 | U-agent |
| NFR-10 | U-agent |
| NFR-11 | U-scaffold y U-deploy (`make doctor`) |
| NFR-12 | U-deploy |

Todo requisito Must y Should tiene al menos una unidad. RF-38 y RF-45 (Could) no se asignan, por
la decisión Q7-A de historias.
