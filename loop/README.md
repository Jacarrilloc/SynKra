# Loop de agentes de SynKra (módulo 6)

El loop ejecuta las tareas de [`unidades-y-tareas.md`](../unidades-y-tareas.md). No es una fase de
AI-DLC: es la técnica con la que se hace la Parte 2 de Code Generation, tarea por tarea.

| Papel | Dónde vive | Modelo |
|---|---|---|
| Orquestador | La sesión principal, con el contrato [`orquestador.md`](./orquestador.md) | el más capaz disponible |
| Codificador | [`.claude/agents/codificador.md`](../.claude/agents/codificador.md) | `sonnet` |
| Revisor | [`.claude/agents/revisor.md`](../.claude/agents/revisor.md) (sin `Write` ni `Edit`) | `opus` |
| Humano | El responsable del proyecto | fusiona cada pull request |

## Carpetas

| Carpeta o archivo | Qué guarda | Quién escribe |
|---|---|---|
| `tareas/` | Un archivo por tarea, con la plantilla del curso | El humano, antes de despachar |
| `bitacoras/U0X-T0N.md` | Acuse de lectura y pares falla-pasa | El codificador |
| `revisiones/U0X-T0N/ronda-N.md` | El informe del revisor, tal cual | El orquestador |
| `revisiones/U0X-T0N/ronda-N-respuesta.md` | La respuesta del codificador, hallazgo por hallazgo | El codificador |
| [`CANDIDATAS.md`](../CANDIDATAS.md) | Trabajo que salió del alcance y no se hizo | El orquestador |
| [`DECISIONES-LOOP.md`](../DECISIONES-LOOP.md) | La media página de la entrega | El humano |

## Orden de despacho de la primera ola

Las unidades U01 y U03 no dependen entre sí, pero **U03-T01 necesita la raíz de Gradle de
U01-T01** (`settings.gradle.kts`). Por eso la primera tarea va sola:

| Paso | Tareas | En paralelo | Por qué |
|---|---|---|---|
| Ronda observada | U01-T01 | 1 | La guía pide ver una ronda completa antes de una ola; además, crea la raíz que usan las dos unidades |
| Ola 1 | U01-T02 y U03-T01 | 2 | Tocan archivos distintos: `verificador/build.gradle.kts` y `backend/` más una línea de `settings.gradle.kts` |
| Ola 2 | U01-T03 y U03-T02 | 2 | `verificador/src` y `backend/src` |
| Ola 3 | U01-T04 y U03-T03 | 2 | Igual |

Las tareas siguientes de U01 y U03 se convierten en archivos de tarea después de ver las primeras
rondas, como recomienda la guía.

## Cómo se arranca

```bash
git worktree add ../wt-U01-T01 -b tarea/U01-T01
git hash-object tareas/U01-T01-raiz-gradle.md
```

Y en la sesión principal:

```text
Lee loop/orquestador.md y actúa según ese contrato. Mis unidades y tareas están en
unidades-y-tareas.md y el grafo en aidlc-docs/inception/application-design/unit-of-work-dependency.md.
Despacha U01-T01. Una sola tarea, quiero ver la ronda completa.
```

## Reglas propias del proyecto

- Ningún agente fusiona. El pull request de cada tarea lo fusiona el responsable del proyecto.
- El único clúster permitido es el `kind` local `kind-synkra` (AUTONOMIA-01).
- No se escribe en `aidlc-docs/aidlc-state.md` nada del loop.
