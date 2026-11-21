# PRD: SynKra

**Módulo 3 · Tópicos Especiales en Informática · versión 2.0 (en co-creación) · 21 de noviembre de 2026**

> **Insumos:** `../../Modulo 2/pvb.md`, `../docs/overview.md`, `../docs/mercado.md`,
> `../docs/icp.md`, `../docs/critica.md` y las decisiones de `paso-0-conflictos.md`.
> Etiquetas: **[INTERNO]** = proyección propia; **[VERIFICAR]** = afirmación sin fuente primaria
> confirmada. Ningún [VERIFICAR] se usa aquí como hecho.
>
> Reemplaza la versión 1.0 del 17-oct-2026, que especificaba un agente de IA regulatorio (queda
> en el historial de Git). **Este PRD se escribe segmento por segmento y cada uno lo aprueba
> Julian.** Estado de los segmentos: 1 en revisión; 2 a 13 pendientes.

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

_Pendiente._
