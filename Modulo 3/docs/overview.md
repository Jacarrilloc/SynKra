# Overview — panorama del dominio de SynKra

> Insumo del PRD (módulo 3). Resume el dominio a partir del PVB (`../../Modulo 2/pvb.md`) y
> de la nueva dirección del producto decidida el 17-oct-2026 (micro-kernel con plugins por
> país). Las fuentes son las que cita el PVB; donde el PVB no da enlace se marca
> **[VERIFICAR: enlace]**. Proyecciones propias: **[INTERNO]**.

## 1. El dominio: un producto, varios países

Una empresa que despliega el mismo producto en varios países no mantiene un sistema: mantiene
un núcleo común más las diferencias que exige cada jurisdicción (regulador, protección de
datos, documentos de identidad, retención de información).

Evidencia de que el patrón es real (citada en el PVB):

- Santander declara que más del 80% de sus soluciones globales se extrapolan a mercados locales
  (Santander Digital Services, "Software Engineering"; nota de prensa de mayo de 2022).
  **[VERIFICAR: enlace]**
- BBVA agrupó más de 16.000 desarrolladores para reutilizar componentes entre países (BBVA,
  "BBVA creates a Software area to drive global scalable solutions"). **[VERIFICAR: enlace]**
- Oracle FLEXCUBE ofrece clústeres localizados por país y Temenos vende "Country Model Banks"
  (páginas de producto de cada vendor). **[VERIFICAR: enlace]**

## 2. Dos formas de resolver la variación por país

| Enfoque | Cómo funciona | Problema |
|---|---|---|
| **Clone-and-own** (ramas o repos por país) | Se copia el core y cada país diverge | Anti-patrón documentado: Thoughtworks pone las ramas largas en "Hold"; SEI/CMU (Bachmann & Clements, "Variability in Software Product Lines") muestra que clonar "quickly gets out of hand". **[VERIFICAR: enlace]** |
| **Micro-kernel + plugins** (dirección elegida) | Un kernel común define puntos de extensión; cada país aporta un plugin que se carga en su despliegue | Hay que diseñar bien los puntos de extensión; un cambio regulatorio todavía exige saber qué plugin tocar |

SynKra adopta el segundo enfoque. Es la "salida" que el propio PVB proponía en su riesgo #4.

## 3. Dónde se monta: Kubernetes y GitOps

- Kustomize (overlays), Helm y Argo CD/Flux ya resuelven el empaquetado y el despliegue por
  entorno, gratis. Argo CD es proyecto graduado de la CNCF; la CNCF reporta su uso en cerca del
  60% de los clústeres encuestados ("2025 Argo CD End User Survey", 24-jul-2025).
  **[VERIFICAR: enlace]**
- Un despliegue por país = un overlay de Kustomize que elige qué plugins monta el kernel.

## 4. Dónde entra la IA

La interpretación de normas la hacen RegTech especializadas (Norm Ai, CUBE, Corlytics). Lo que
nadie cierra, según el PVB, es el paso **obligación ya interpretada → qué parte del código
tocar**. En el micro-kernel eso se traduce en: **qué plugin y qué punto de extensión** debe
cambiar, con un borrador de historia y de PR para que un humano apruebe.

## 5. Marco regulatorio de ejemplo (dominio KYC del MVP)

| País | Protección de datos | Documento de identidad |
|---|---|---|
| Colombia | Ley 1581 de 2012 (Habeas Data) | Cédula de ciudadanía |
| Perú | Ley 29733 de 2011 (Protección de Datos Personales) | DNI |

Las reglas concretas de cada plugin (formato de documento, texto de consentimiento, plazos de
retención) se tratan como **[VERIFICAR]** hasta contrastarlas con la norma primaria; para el MVP
son reglas simuladas y así se declara.
