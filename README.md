# SynKra

⚖️ **Plataforma micro-kernel con plugins por país, y un agente de IA que localiza dónde aplicar cada cambio regulatorio, sobre Kubernetes y GitOps.**

Este repositorio contiene los artefactos de producto y especificaciones técnicas desarrollados para el módulo de AI & Agentic Engineering de la asignatura **Tópicos Especiales en Informática**, como parte de la Maestría en Ingeniería de Sistemas con enfasis en Sistemas de Información e Ingenieria de Software de la **Pontificia Universidad Javeriana** (Bogotá).

## Descripción del Proyecto

SynKra despliega un mismo producto en cada país con un **kernel común** y solo los **plugins** que ese país necesita (por ejemplo, validación de documento, consentimiento y retención de datos para Colombia y Perú). Cada país es un overlay de Kustomize sobre Kubernetes.

Cuando llega un cambio regulatorio ya interpretado por un humano, un agente de IA (Claude, con tope de gasto, o un modelo local) localiza qué plugin y qué punto de extensión hay que cambiar y abre un *Pull Request* en borrador en una rama `synkra/*`. El agente no puede fusionar ni aplicar nada al clúster: un humano aprueba y GitOps despliega.

## Estructura del Repositorio

- `Modulo 2/pvb.md` — Product Vision Board (incluye el ajuste al enfoque micro-kernel).
- `Modulo 3/docs/` — Insumos del PRD: panorama del dominio, mercado, ICP y crítica adversarial.
- `Modulo 3/specs/prd.md` — Product Requirements Document.
- `scripts/`, `Makefile` — Entorno de desarrollo para Linux y WSL2.

## Entorno de desarrollo (Linux y Windows)

Todo se construye y prueba en Linux. En Windows se usa WSL2 con Ubuntu, así los dos sistemas corren los mismos scripts.

**Linux (Ubuntu/Debian):**

```bash
git clone https://github.com/Jacarrilloc/SynKra.git ~/proyectos/SynKra
cd ~/proyectos/SynKra
make setup      # instala Docker, kubectl, kind, Helm, Java 21, Maven, Node y Claude Code
make doctor     # verifica que todo esté
make cluster    # crea el clúster kind local
```

**Windows:** en PowerShell, desde esta carpeta, ejecuta `.\scripts\setup-windows.ps1`. Clona el repo en `~/proyectos/SynKra` dentro de Ubuntu y corre `make setup` allí. Después trabaja con `wsl -d Ubuntu` y `cd ~/proyectos/SynKra`.

> Clona el repo en el disco de Linux (`~/`), no en `/mnt/c/...`: el disco de Windows hace lentos a Docker y a `git worktree`.
