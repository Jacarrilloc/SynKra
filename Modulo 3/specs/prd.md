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

**One-liner.** SynKra es una plataforma **visual** para desplegar aplicaciones micro-kernel en
Kubernetes: el equipo ve el core y arrastra los plugins que necesita cada país, y SynKra verifica
que la combinación sea compatible antes de proponerla como cambio en Git para que Argo CD la
despliegue.

**JTBD.** *Cuando* tengo que armar o cambiar el despliegue de mi producto para un país, *quiero*
ver de un vistazo qué core y qué plugins lleva, y armarlo sin escribir YAML, *para* desplegarlo
con la certeza de que la combinación es compatible y sin depender de la persona que sabe cómo
está armado cada país.

**Misión.** Que desplegar el producto en un país nuevo sea elegir plugins en un lienzo, no clonar
un repositorio. Que cualquiera del equipo entienda, mirando una pantalla, qué corre en cada país.
Y que ninguna combinación incompatible llegue al clúster.

---

## 2. Contexto y problema

_Pendiente._
