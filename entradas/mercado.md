# Mercado y competencia — SynKra

> Validación del mercado. Las cifras vienen del PVB y se contrastaron con la fuente el
> 17-oct-2026. **[VERIFICAR]** = no confirmado con fuente primaria. **[INTERNO]** = proyección
> propia.

## 1. Costo del problema

| Dato | Fuente |
|---|---|
| Los bancos pasaron de gastar 9,6% del presupuesto de IT en compliance (2016) a 13,4% (2023); las horas dedicadas a cumplir regulación subieron 61% mientras las horas totales subieron 20% | Joshua Smith y Benjamin Gross, [Bank Policy Institute, 29-oct-2024](https://bpi.com/survey-finds-compliance-is-growing-demand-on-bank-resources/). Encuesta a miembros de BPI, en general bancos de más de USD 100.000 millones en activos |
| Costo de cumplimiento de delitos financieros en EE.UU. y Canadá: USD 61.000 millones; subió para el 99% de las instituciones | [LexisNexis Risk Solutions / Forrester Consulting, 21-feb-2024](https://risk.lexisnexis.com/about-us/press-room/press-release/20240221-true-cost-of-compliance-us-ca) |

Ojo: ambas cifras son de EE.UU. y Canadá, y de bancos grandes. No hay un dato equivalente para
Colombia o Perú (vacío de evidencia #2).

## 2. Competencia

| Actor | Qué cubre | Relación con SynKra |
|---|---|---|
| Norm Ai, CUBE, Corlytics, AscentAI | Interpretación de normas → obligaciones | **Upstream**: su salida es la entrada de SynKra. No compite |
| Atlassian Rovo, GitHub Copilot, Cursor | Documento → ticket, ticket → PR | Cubren la mitad generativa. **Comoditizado** |
| Harness (agentes de entrega) | Pipeline y despliegue con agentes | **Flanco más peligroso** (PVB riesgo #1) |
| Regnology "Ticket-to-Code Writer" ([Google Cloud blog, 10-sep-2024](https://cloud.google.com/blog/products/ai-machine-learning/regnology-automates-ticket-to-code-with-genai-on-vertex-ai)) | Ticket → código | Precedente más cercano; no va de cambio regulatorio a plugin |
| Kustomize, Helm, Argo CD, Flux, PF4J, OpenFeature | Empaquetado, overlays, GitOps, plugins, feature flags | **Gratis**. SynKra se monta encima, no compite |

Financiamiento: Norm Ai levantó una Serie C de USD 120 millones a una valuación de USD 1.200
millones, liderada por Khosla Ventures, en julio de 2026
([LawNext](https://www.lawnext.com/2026/07/norm-ai-hits-unicorn-status-with-120m-series-c-at-1-2-billion-valuation.html)).
El total levantado varía según la fuente (más de USD 256-260 millones).

## 3. Adopción del ecosistema

- Argo CD: cerca del 60% de los clústeres de Kubernetes, 97% de los encuestados lo usa en
  producción, NPS 79
  ([CNCF, "2025 Argo CD End User Survey", 24-jul-2025](https://cncf.io/announcements/2025/07/24/cncf-end-user-survey-finds-argo-cd-as-majority-adopted-gitops-solution-for-kubernetes)).
- 66% de las organizaciones que alojan modelos de IA generativa usan Kubernetes para parte o
  toda su inferencia ([CNCF Annual Cloud Native Survey, publicado el 15-ene-2026](https://www.linuxfoundation.org/hubfs/Research%20Reports/CNCF_Annual_Survey_Report_1.15.26.pdf)).
- El dato del PVB "57% ya corre agentes de IA en producción" **no aparece** en ese reporte y no
  se encontró otra fuente. **[VERIFICAR]**: no usarlo en la sustentación hasta confirmarlo.

## 4. Pricing

Hybrid Tiered por producto core conectado (Starter USD 600/mes, Business USD 2.000/mes,
Enterprise a convenir). Todo **[INTERNO]**.

## 5. Vacíos de evidencia

1. Lead time real de llevar un cambio regulatorio a N países (no hay dato público; medir con
   entrevistas).
2. Costo de cumplimiento en Colombia y Perú.
3. Cuántas empresas del beachhead ya usan plugins o micro-kernel frente a ramas por país.
