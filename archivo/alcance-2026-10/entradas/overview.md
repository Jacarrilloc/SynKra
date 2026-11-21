# Overview — panorama del dominio de SynKra

> Insumo del PRD (módulo 3). Resume el dominio a partir del PVB (`../../Modulo 2/pvb.md`) y
> de la nueva dirección del producto (micro-kernel con plugins por país). Fuentes consultadas
> el 17-oct-2026. **[VERIFICAR]** = afirmación que no se pudo confirmar con fuente primaria.
> **[INTERNO]** = proyección propia.

## 1. El dominio: un producto, varios países

Una empresa que despliega el mismo producto en varios países no mantiene un sistema: mantiene
un núcleo común más las diferencias que exige cada jurisdicción (regulador, protección de
datos, documentos de identidad, retención de información).

Evidencia de que el patrón es real:

- **BBVA** creó un área global de software con más de 16.000 personas para que sus soluciones
  se puedan escalar y compartir entre los países donde opera
  ([BBVA](https://www.bbva.com/es/innovacion/bbva-crea-un-area-de-desarrollo-de-software-para-impulsar-las-soluciones-globales-y-escalables/);
  [Finextra](https://www.finextra.com/newsarticle/39537/bbva-creates-global-software-development-unit)).
- **Oracle FLEXCUBE** ofrece "localized clusters" por país o región sobre un núcleo común; su
  documentación de instalación separa explícitamente los objetos del **kernel** y los del
  **cluster** de cada país (México, Italia)
  ([Oracle](https://www.oracle.com/financial-services/banking/flexcube/core-banking-software);
  [guía de instalación FCUBS](https://docs.oracle.com/cd/F20443_01/PDF/Installation/FLEXCUBE_UBS_Installation/FCUBS%20Components/FCUBS/FCUBS_DB_Setup.pdf)).
  Es, en la práctica, el patrón micro-kernel que adopta SynKra.
- **Temenos** vende "Country Model Banks" con funcionalidad regional para más de 150 países
  ([Temenos](https://www.temenos.com/?p=6398)). Es material de marketing del vendor.
- **Santander** migró su core bancario a una plataforma propia (Gravity) para todos sus
  mercados ([nota de prensa, mayo de 2022](https://santander.com/en/press-room/press-releases/2022/05/santander-digitalizes-its-core-banking-with-cloud-technology-to-improve-service-and-efficiency)).
  La cifra del PVB "más del 80% de sus soluciones globales se extrapolan a mercados locales"
  **no se pudo confirmar**: el 80% de esa nota se refiere a infraestructura en la nube.
  **[VERIFICAR]**

## 2. Dos formas de resolver la variación por país

| Enfoque | Cómo funciona | Problema |
|---|---|---|
| **Clone-and-own** (ramas o repos por país) | Se copia el core y cada país diverge | Anti-patrón documentado: Thoughtworks pone Gitflow, por su flujo de ramas largas, en "Hold" ([Radar, 2015](https://www.thoughtworks.com/radar/techniques/gitflow)) y explica los riesgos de las ramas de larga vida ([Thoughtworks](https://www.thoughtworks.com/insights/blog/enabling-trunk-based-development-deployment-pipelines)); el SEI/CMU lo analiza en Bachmann & Clements, "Variability in Software Product Lines", CMU/SEI-2005-TR-012 ([SEI](https://www.sei.cmu.edu/library/variability-in-software-product-lines)) |
| **Micro-kernel + plugins** (dirección elegida) | Un kernel común define puntos de extensión; cada país aporta un plugin que se carga en su despliegue | Hay que diseñar bien los puntos de extensión; un cambio regulatorio todavía exige saber qué plugin tocar |

SynKra adopta el segundo enfoque. Es la "salida" que el propio PVB proponía en su riesgo #4.

## 3. Dónde se monta: Kubernetes y GitOps

- Kustomize (overlays), Helm y Argo CD/Flux ya resuelven el empaquetado y el despliegue por
  entorno, gratis. Argo CD corre en cerca del 60% de los clústeres de Kubernetes encuestados
  ([CNCF, 24-jul-2025](https://cncf.io/announcements/2025/07/24/cncf-end-user-survey-finds-argo-cd-as-majority-adopted-gitops-solution-for-kubernetes)).
- Un despliegue por país = un overlay de Kustomize que elige qué plugins monta el kernel.

## 4. Dónde entra la IA

La interpretación de normas la hacen RegTech especializadas (Norm Ai, CUBE, Corlytics). Lo que
nadie cierra, según el PVB, es el paso **obligación ya interpretada → qué parte del código
tocar**. En el micro-kernel eso se traduce en: **qué plugin y qué punto de extensión** debe
cambiar, con un borrador de historia y de PR para que un humano apruebe. Precedente cercano:
Regnology "Ticket-to-Code Writer", que va de un ticket a cambios de código
([Google Cloud blog, 10-sep-2024](https://cloud.google.com/blog/products/ai-machine-learning/regnology-automates-ticket-to-code-with-genai-on-vertex-ai)).

## 5. Marco regulatorio de ejemplo (dominio KYC del MVP)

| País | Protección de datos | Documento de identidad |
|---|---|---|
| Colombia | Ley 1581 de 2012 (Habeas Data) | Cédula de ciudadanía |
| Perú | Ley 29733 de 2011 (Protección de Datos Personales) | DNI |

Las reglas concretas de cada plugin (formato de documento, texto de consentimiento, plazos de
retención) se tratan como **[VERIFICAR]** hasta contrastarlas con la norma primaria; para el MVP
son reglas simuladas y así se declara.

## 6. Contexto regulatorio de la IA

El EU AI Act (Reg. UE 2024/1689) exige supervisión humana para sistemas de alto riesgo. El
Digital Omnibus (Reg. UE 2026/1744, vigente desde el 27-jul-2026) aplazó esas obligaciones del
2-ago-2026 al **2-dic-2027**; las obligaciones de transparencia del artículo 50 sí aplican desde
el 2-ago-2026 ([Gibson Dunn](https://www.gibsondunn.com/eu-ai-act-omnibus-agreement-postponed-high-risk-deadlines-and-other-key-changes/);
[Cuatrecasas](https://www.cuatrecasas.com/en/global/intellectual-property/art/digital-omnibus-ai-has-been-published)).
El PVB citaba el 2-ago-2026; queda corregido.
