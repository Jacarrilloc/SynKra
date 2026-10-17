# Mercado y competencia — SynKra

> Validación del mercado. Las cifras vienen del PVB con la fuente que allí se nombra; ninguna
> cifra de este archivo es nueva. **[VERIFICAR: enlace]** = el PVB cita la fuente pero no el
> enlace; hay que confirmarlo antes de la sustentación. **[INTERNO]** = proyección propia.

## 1. Costo del problema

| Dato | Fuente (según PVB) | Estado |
|---|---|---|
| Los bancos pasaron de gastar 9,6% del presupuesto de IT en compliance (2016) a 13,4% (2023); +61% en horas-empleado dedicadas a cumplir regulación | Bank Policy Institute, "Survey Finds Compliance is a Growing Demand on Bank Resources", oct-2024 | **[VERIFICAR: enlace]** |
| Costo de cumplimiento de delitos financieros en EE.UU. y Canadá: USD 61.000 millones | LexisNexis Risk Solutions / Forrester, "True Cost of Financial Crime Compliance Study", feb-2024 | **[VERIFICAR: enlace]** |

Ojo: ambas cifras son de EE.UU. y Canadá. No hay en los docs un dato equivalente para Colombia
o Perú (vacío de evidencia #2).

## 2. Competencia

| Actor | Qué cubre | Relación con SynKra |
|---|---|---|
| Norm Ai, CUBE, Corlytics, AscentAI | Interpretación de normas → obligaciones | **Upstream**: su salida es la entrada de SynKra. No compite |
| Atlassian Rovo, GitHub Copilot, Cursor | Documento → ticket, ticket → PR | Cubren la mitad generativa. **Comoditizado** |
| Harness (agentes de entrega) | Pipeline y despliegue con agentes | **Flanco más peligroso** (PVB riesgo #1) |
| Regnology "Ticket-to-Code Writer" (Google Cloud blog, 10-sep-2024) | Ticket → código | Precedente más cercano; no va de cambio regulatorio a plugin |
| Kustomize, Helm, Argo CD, Flux, PF4J, OpenFeature | Empaquetado, overlays, GitOps, plugins, feature flags | **Gratis**. SynKra se monta encima, no compite |

Dato de financiamiento citado en el PVB: Norm Ai, Serie C de USD 120 millones a valuación de
~USD 1.200 millones, 7-jul-2026 (PR Newswire, TechCrunch). **[VERIFICAR: enlace]**

## 3. Adopción del ecosistema

- Argo CD: ~60% de los clústeres, 97% lo usa en producción, NPS 79 (CNCF, "2025 Argo CD End
  User Survey", 24-jul-2025). **[VERIFICAR: enlace]**
- 66% de organizaciones con GenAI corren inferencia sobre Kubernetes; 57% corren agentes en
  producción (datos CNCF 2025 citados en el PVB). **[VERIFICAR: enlace y nombre del reporte]**

## 4. Pricing

Hybrid Tiered por producto core conectado (Starter USD 600/mes, Business USD 2.000/mes,
Enterprise a convenir). Todo **[INTERNO]**.

## 5. Vacíos de evidencia

1. Lead time real de llevar un cambio regulatorio a N países (no hay dato público; medir con
   entrevistas).
2. Costo de cumplimiento en Colombia y Perú.
3. Cuántas empresas del beachhead ya usan plugins o micro-kernel frente a ramas por país.
