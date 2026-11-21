# Mercado y competencia: SynKra

> Investigación de **validación** para el alcance vigente desde el 21-nov-2026: una plataforma
> de despliegue micro-kernel sobre Kubernetes, con un front de arrastrar y soltar. Sustituye
> el análisis del 17-oct-2026, que trataba del mercado RegTech y de los agentes de IA.
>
> **[INTERNO]** marca una proyección propia. **[VERIFICAR]** marca lo que no se pudo confirmar.
> Las fuentes se consultaron el 21-nov-2026.

## 1. El dolor existe, y está documentado

| Dato | Fuente |
|---|---|
| "Too complex to understand or run" es el reto n.º 1 al usar proyectos CNCF en producción (46%, empatado con "proyectos inactivos"), y subió 13% interanual | [CNCF Annual Survey 2024, fig. 15 (n=552), mar-2025](https://www.cncf.io/wp-content/uploads/2025/04/cncf_annual_survey24_031225a.pdf) |
| El reto n.º 1 con contenedores son los cambios culturales con el equipo de desarrollo (46%), por delante de CI/CD (40%), la falta de formación (38%) y la seguridad (37%) | Misma encuesta, fig. 8 (n=373) |
| "By 2026, 80% of large software engineering organizations will establish platform engineering teams", frente al 45% de 2022 | [Gartner](https://www.gartner.com/en/articles/what-is-platform-engineering) |

Lectura honesta: la complejidad es un dolor real, pero **el primer problema es cultural, no de
herramientas**. Una herramienta más no lo resuelve sola (ver `critica.md`, §2).

## 2. Competencia y sustitutos

### Capa gratuita (la que fija el precio de todo lo demás)

| Actor | Qué cubre | Relación con SynKra |
|---|---|---|
| Kustomize components + Argo CD (≥ v2.10) | Activar o desactivar componentes por overlay, reconciliado por GitOps ([docs](https://argo-cd.readthedocs.io/en/stable/user-guide/kustomize/)) | **Es el motor que SynKra usa.** Sin front, ya hace la selección de plugins en YAML |
| Helm (subcharts con `condition` y `tags`) | Encender o apagar partes opcionales de una aplicación ([Helm](https://helm.sh/docs/chart_best_practices/dependencies)) | Sustituto directo del contrato de despliegue |
| KubeVela + VelaUX (CNCF Incubating) | Entrega de aplicaciones con componentes y traits (OAM), con una interfaz web para "non-k8s-expert users" ([CNCF](https://www.cncf.io/blog/2023/02/27/kubevela-brings-software-delivery-control-plane-capabilities-to-cncf-incubator/)) | **El competidor más cercano.** Usado en producción por Alibaba, Bytedance y China Merchants Bank (misma fuente) |
| Backstage (CNCF Incubating) | Portal con Software Templates para crear proyectos ([CNCF](https://cncf.io/blog/2022/03/15/backstage-project-joins-the-cncf-incubator)) | Cubre el "elijo y despliego" con formularios, no con un lienzo |
| Kratix (Apache-2.0) | "An open-source framework for building platforms" ([GitHub](https://github.com/syntasso/kratix)) | Alternativa para equipos de plataforma que construyen su propia oferta |
| PF4J (Apache-2.0) | Plugins dentro de un proceso Java ([GitHub](https://github.com/pf4j/pf4j)) | Pieza que SynKra puede usar, no competidor |

### Capa comercial

| Actor | Precio publicado | Relación |
|---|---|---|
| Port | Plan gratis hasta 5 usuarios; Basic desde USD 30 y Standard desde USD 48 por usuario al mes; Enterprise a medida ([Port, pricing](https://port.io/pricing)) | Portal con acciones de autoservicio. Su plan gratis fija el piso de precio |
| Humanitec | No verificado en esta investigación **[VERIFICAR]** | Orquestador de plataforma |

## 3. Dónde queda el espacio

Ninguno de los actores verificados ofrece, como foco principal, **la combinación de tres
cosas**:

1. un **contrato de plugins** para una aplicación micro-kernel (no solo para servicios
   sueltos),
2. **validación de compatibilidad** entre el core y los plugins antes de desplegar, y
3. un **front de acceso** que despliega directo artefactos ya compilados, sin pasar por Git, y
   muestra qué corre en cada país.

Es un espacio estrecho. KubeVela cubre buena parte de los puntos 1 y 3 con otro modelo (OAM),
y la diferencia puede no bastar a un comprador (ver `critica.md`, §1).

## 4. Pricing

Para el curso, SynKra es un proyecto académico sin modelo de cobro validado. Cualquier precio
sería **[INTERNO]** y tendría que competir con un piso de USD 0 (Port Free, KubeVela, Backstage).

## 5. Vacíos de evidencia

1. Cuántos equipos despliegan un producto micro-kernel con variantes por país (no hay dato
   público).
2. Si un front visual acelera de verdad la configuración frente a editar YAML (no se encontró
   ningún estudio): se mide en el piloto **[INTERNO]**.
3. El precio de Humanitec y su encaje con este caso.
