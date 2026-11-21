# PRD: SynKra

**Módulo 3 · Tópicos Especiales en Informática · versión 2.0 (en co-creación) · 21 de noviembre de 2026**

> **Insumos:** `../../Modulo 2/pvb.md`, `../docs/overview.md`, `../docs/mercado.md`,
> `../docs/icp.md`, `../docs/critica.md` y las decisiones de `paso-0-conflictos.md`.
> Etiquetas: **[INTERNO]** = proyección propia; **[VERIFICAR]** = afirmación sin fuente primaria
> confirmada. Ningún [VERIFICAR] se usa aquí como hecho.
>
> Reemplaza la versión 1.0 del 17-oct-2026, que especificaba un agente de IA regulatorio (queda
> en el historial de Git). **Este PRD se escribe segmento por segmento y cada uno lo aprueba
> Julian.** Estado de los segmentos: 1 y 2 aprobados; 3 en revisión; 4 a 13 pendientes.

> [!IMPORTANT]
> **Desviación declarada:** SynKra no usa IA (PVB §6). El segmento 11 evalúa la verificación,
> no un modelo.

---

## 1. One-liner y Job to be Done

**One-liner.** SynKra es una herramienta de **despliegue** para aplicaciones micro-kernel ya
compiladas, sobre Kubernetes. Desde un front de acceso, el equipo elige qué core y qué plugins
lleva cada país, y SynKra verifica que la combinación sea compatible y la despliega directo en el
clúster.

**JTBD.** *Cuando* tengo que desplegar mi producto ya compilado en un país, *quiero* elegir desde
un solo lugar qué core y qué plugins lleva, y ver qué corre en cada país, *para* desplegarlo con
la certeza de que la combinación es compatible, sin escribir YAML y sin depender de la persona
que sabe cómo está armado cada país.

**Misión.** Que desplegar el producto en un país nuevo sea elegir un core y sus plugins, no
clonar un repositorio ni escribir manifiestos. Que ninguna combinación incompatible llegue al
clúster. Y, a futuro, que esos despliegues se automaticen.

> [!NOTE]
> **SynKra no compila ni usa Git.** Parte de un proyecto ya compilado: la imagen del core y su
> carpeta de plugins disponibles (`paso-0-conflictos.md`, ajuste posterior).

---

## 2. Contexto y problema

### Dolores del mercado

| Dolor | Evidencia | Fuente |
|---|---|---|
| **Un mismo producto en varios países se mantiene como un núcleo común más diferencias por país** | Oracle FLEXCUBE separa en su instalación los objetos del *kernel* de los del *cluster* de cada país, y Temenos vende "Country Model Banks" | `docs/overview.md` §2 |
| **Mantener cada país como rama o repo propio replica a mano cada cambio del core** | Thoughtworks pone Gitflow en "Hold" por los riesgos de las ramas de larga vida | `docs/overview.md` §3 |
| **Operar el ecosistema de Kubernetes es complejo** | "Too complex to understand or run" es el reto n.º 1 al usar proyectos CNCF en producción (46%, y subió 13% interanual) | CNCF Annual Survey 2024, fig. 15 (`docs/mercado.md` §1) |
| **El freno principal es cultural, no de herramientas** | El reto n.º 1 con contenedores son los cambios culturales con el equipo de desarrollo (46%) | CNCF Annual Survey 2024, fig. 8 |
| **Nadie tiene una vista única de qué corre en cada país** | Hipótesis del ICP: la respuesta vive en la cabeza de una persona o en una hoja de cálculo | `docs/icp.md` **[INTERNO]**, por validar en entrevistas |
| **Desplegar una variante a mano exige YAML y `kubectl` expertos** | Hipótesis del ICP | `docs/icp.md` **[INTERNO]** |

### ¿Por qué ahora?

- **La plataforma interna se volvió un rol estándar.** Gartner predice que "By 2026, 80% of
  large software engineering organizations will establish platform engineering teams", frente al
  45% de 2022 (`docs/overview.md` §5). Ojo: habla de organizaciones **grandes**, no del segmento
  mediano de LatAm.
- **Kubernetes ya es el sustrato común.** Argo CD corre en cerca del 60% de los clústeres
  encuestados (CNCF, 24-jul-2025), lo que indica que desplegar en Kubernetes ya es práctica
  general y no un nicho.
- **Por qué ahora para este proyecto:** la Fase 4 del plan migra los plugins de Colombia y
  Perú al modelo micro-kernel. Sin una herramienta que verifique y despliegue cada combinación,
  esa migración se haría a mano.

> [!WARNING]
> **No hay un evento externo que haga urgente este producto en particular.** La urgencia es
> del segmento (complejidad creciente, equipos de plataforma) y del proyecto (Fase 4). Decirlo
> evita vender una urgencia que los documentos no sostienen.

### Alternativas actuales y por qué no le alcanzan al ICP

| Alternativa | Qué resuelve | Qué le falta para este caso |
|---|---|---|
| **A mano** (YAML + `kubectl`) | Todo, si hay un experto | Depende de una persona; nada impide desplegar una combinación incompatible |
| **Helm** con subcharts opcionales (`condition`, `tags`) | Encender o apagar partes de una aplicación | Los plugins del micro-kernel no son subcharts: viven dentro del compilado. Helm no sabe qué versión del contrato implementa cada plugin |
| **Kustomize components + Argo CD** (GitOps) | Activar componentes por overlay y reconciliarlos desde Git | Exige un flujo de Git que este caso no usa (se parte de artefactos ya compilados). Tampoco verifica el contrato |
| **KubeVela + VelaUX** (CNCF Incubating) | Entrega de aplicaciones por componentes, con una interfaz web para quien no es experto | Modela servicios con OAM, no un core con plugins y un contrato de versiones. Es la alternativa más fuerte |
| **Backstage / Port** | Portal con plantillas o acciones de autoservicio (Port es gratis hasta 5 usuarios) | Hay que construir encima toda la lógica del micro-kernel. Backstage tiene problemas conocidos de adopción (10% de adopción media según Spotify, vía TechTarget) |

> [!IMPORTANT]
> **Qué parte del problema ya está resuelta gratis.** Generar y aplicar los manifiestos de cada
> país ya lo resuelven Kustomize y Helm. Una interfaz web de despliegue para no expertos ya
> existe en parte (VelaUX). **SynKra compite con herramientas gratuitas.** Lo que ninguna
> resuelve como foco es:
>
> 1. verificar, **antes de desplegar**, que los plugins elegidos son compatibles con el contrato
>    del core, y
> 2. mostrar en un solo lugar qué core y qué plugins corren en cada país.
>
> Si ese aporte no le vale a un equipo, la respuesta honesta es que use Kustomize o KubeVela
> (`docs/critica.md` §1 y §8).

---

## 3. ICP detallado

> Hipótesis explícita y falsable (`docs/icp.md`). Nada de este segmento sale de entrevistas
> todavía: todo es **[INTERNO]** hasta validarlo.

### Perfil y firmographics

| Dimensión | Valor |
|---|---|
| **Tamaño del equipo** | Plataforma o entrega de **3 a 15 ingenieros** |
| **Sector** | Vendors de software financiero y fintechs (vertical inicial) |
| **Geografía** | Producto desplegado en **2 o más países**; empieza por Colombia y Perú |
| **Madurez en Kubernetes** | Ya corre Kubernetes en producción. **No se exige GitOps**: SynKra despliega directo |
| **Arquitectura** | Producto **micro-kernel ya compilado**: un core más una carpeta de plugins, o la intención de migrar a ese modelo |

**Señales de calificación:**

1. Hoy cada país es una rama, un repo o un fork, o se despliega a mano con YAML distinto.
2. "Qué tiene activado el país X" se responde preguntándole a una persona.
3. Hay gente de producto o de soporte que necesita desplegar o revisar una variante sin escribir
   YAML.
4. El equipo quiere, a futuro, automatizar esos despliegues.

**Fuera del ICP:** productos de un solo país; equipos sin Kubernetes; equipos que **exigen** que
todo despliegue pase por un PR y GitOps (SynKra no usa Git); y equipos contentos con Helm,
Kustomize, KubeVela o Backstage.

> [!WARNING]
> **Cambio frente a `docs/icp.md`.** La señal "tiene algo de GitOps" se quitó: desde el ajuste
> sin Git del 21-nov, un equipo que exige GitOps queda **fuera** del ICP, no dentro.

### Buyer personas y veto de confianza

| Persona | Rol | Qué le importa | Poder |
|---|---|---|---|
| **Head of Platform / SRE Lead** | Dueño del clúster | "¿Qué permisos de escritura tiene esto en mi clúster?" | **Decisor y veto** |
| **Arquitecto de software** | Dueño del core y del contrato de plugins | Que un plugin no pueda romper el core | **Veto técnico** |
| **Product Owner de país** | Decide qué funciones lleva su país | Desplegar y ver su variante sin depender de un ingeniero | Usuario |
| **Desarrollador de plugins** | Construye y empaqueta un plugin | Un contrato claro y un error legible cuando no cumple | Usuario |

El veto lo tiene el **Head of Platform**: si la cuenta de servicio de SynKra puede escribir fuera
de sus namespaces, la conversación se acaba.

### Pains (de `docs/icp.md`)

- Un cambio del core hay que replicarlo a mano en cada país.
- Nadie tiene una vista única de qué plugin y qué versión corre en cada país.
- Desplegar una variante nueva toma días de YAML y revisiones.

### Triggers de compra

- Se abre un país nuevo y hay que montar su variante.
- Un país se quedó con una versión vieja del core o con un plugin incompatible.
- Rota la persona que sabía cómo estaba armado cada país.

### Objeciones probables y respuesta

| Objeción | Respuesta |
|---|---|
| "Eso ya lo hago con Kustomize o con Helm" | Cierto, y SynKra puede usar Kustomize por debajo. Añade la verificación del contrato antes de desplegar y la vista por país. Si eso no le vale al equipo, no es ICP |
| "¿Por qué no KubeVela?" | KubeVela modela servicios con OAM; SynKra modela un core y sus plugins con un contrato de versiones. La diferencia está por validar en entrevistas **[VERIFICAR]** |
| "Yo despliego con GitOps y reviso PRs" | SynKra no pasa por Git. Para un equipo que exige PRs, hoy no es la herramienta, y se dice así |
| "Es otra pieza con permisos de escritura en mi clúster" | Permisos limitados a `synkra-*`, solo imágenes de un registro permitido y registro de cada despliegue. Se verifica con `kubectl auth can-i` (`docs/critica.md` §5) |

---

## 4. Propuesta de valor única y diferenciadores

_Pendiente._
