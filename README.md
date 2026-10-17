# SynKra

⚖️ **Agente Autónomo de Compliance (RegTech) vía GitOps sobre Kubernetes.**

Este repositorio contiene los artefactos de producto y especificaciones técnicas desarrollados para el módulo de AI & Agentic Engineering de la asignatura **Tópicos Especiales en Informática**, como parte de la Maestría en Ingeniería de Sistemas con enfasis en Sistemas de Información e Ingenieria de Software de la **Pontificia Universidad Javeriana** (Bogotá).

## Descripción del Proyecto

Plataforma de agentes de IA diseñada para auditar y actualizar continuamente la infraestructura de sistemas bancarios distribuidos. El agente cruza las políticas de infraestructura (RBAC, gestión de secretos, NetworkPolicies) con la regulación financiera y de protección de datos de cada país anfitrión (ej. Colombia, Perú). Al detectar un desfase normativo, el agente no modifica el clúster directamente; en su lugar, propone *Pull Requests* con los manifiestos corregidos y la evidencia regulatoria adjunta. Un SRE o analista de *compliance* aprueba el cambio, y GitOps reconcilia la configuración, garantizando la confianza y el cumplimiento sin intervención manual pesada.

## Estructura del Repositorio

- `pvb.md` — Product Vision Board: Definición del problema, ventaja competitiva basada en auditoría y control de permisos (Trust), riesgos críticos y modelo económico.
- `docs/` — Insumos de investigación profunda, incluyendo el *deep research* de validación, análisis de mercado y la investigación adversarial.
- `specs/prd.md` — Product Requirements Document (PRD) consolidado, co-creado segmento por segmento a partir de la visión del producto.

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
