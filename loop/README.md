# Loop de agentes de SynKra (módulo 6)

El loop ejecuta las tareas de [`unidades-y-tareas.md`](../unidades-y-tareas.md). **No es una fase de
AI-DLC**: es la técnica con la que se hace la Parte 2 de Code Generation, tarea por tarea. Se rehízo
desde cero en la rama `modulo-6-rehecho`; `main` conserva el intento anterior sin cambios.

| Papel | Dónde vive | Modelo |
|---|---|---|
| Orquestador | La sesión principal, con el contrato [`orquestador.md`](./orquestador.md) | el más capaz disponible |
| Codificador | [`.claude/agents/codificador.md`](../.claude/agents/codificador.md) | `sonnet` |
| Revisor | [`.claude/agents/revisor.md`](../.claude/agents/revisor.md) (sin `Write` ni `Edit`) | `opus` |
| Humano | El responsable del proyecto | fusiona cada pull request |

## Carpetas

| Carpeta o archivo | Qué guarda | Quién escribe |
|---|---|---|
| `tareas/` | Un archivo por tarea, con la plantilla del curso | El humano (o el orquestador con su visto bueno), antes de despachar |
| `bitacoras/U0X-T0N.md` | Acuse de lectura y pares falla-pasa | El codificador |
| `revisiones/U0X-T0N/ronda-N.md` | El informe del revisor, tal cual | El orquestador |
| `revisiones/U0X-T0N/ronda-N-respuesta.md` | La respuesta del codificador, hallazgo por hallazgo | El codificador |
| [`CANDIDATAS.md`](../CANDIDATAS.md) | Trabajo que salió del alcance y no se hizo | El orquestador |
| [`DECISIONES-LOOP.md`](../DECISIONES-LOOP.md) | La media página de la entrega | El humano |

## Ramas

```text
main ──(intacta)
  └─ modulo-6-rehecho            rama base del loop; PR final hacia main
       ├─ m6r/U01-T01            una rama y un worktree por tarea; PR hacia modulo-6-rehecho
       ├─ m6r/U01-T02
       └─ m6r/U03-T01 ...
```

## Orden de despacho

U01 y U03 no dependen entre sí en el grafo, pero **U03-T01 necesita la raíz de Gradle de U01-T01**
(`settings.gradle.kts`). Por eso la primera tarea va sola:

| Paso | Tareas | En paralelo | Por qué |
|---|---|---|---|
| Ronda observada | U01-T01 | 1 | La guía pide ver una ronda completa antes de una ola; además crea la raíz que usan las dos unidades |
| Ola 1 | U01-T02 y U03-T01 | 2 | Tocan archivos distintos: `verificador/build.gradle.kts`, y `backend/` más una línea de `settings.gradle.kts` |
| Ola 2 | U01-T03 y U03-T02 | 2 | `verificador/src` y `backend/src` |
| Ola 3 | U01-T04 y U03-T03 | 2 | Igual |
| Ola 4 | U01-T05 y U03-T04 | 2 | `verificador/src/test` y `backend/` (configuración y una prueba). Sale de `main` |
| Ola 5 | U01-T06 y U03-T05 | 2 | `verificador/src` y `backend/src/test`. Sale de `main` después de fusionar la ola 4 |
| Ola 6 | U01-T07 y U03-T06 | 2 | `verificador/src/test` y diseño de U01; `backend/Dockerfile` y `backend/.dockerignore`. Sale de `main` después de fusionar la ola 5 |
| Ola 7 | U01-T08 y U03-T07 | 2 | `verificador/src` (records nuevos); `aidlc-docs/construction/U03-plataforma-backend/code/`. Cierra U03 (T08 es *Should*). Sale de `main` después de fusionar la ola 6 |
| Ola 8 | U01-T09 | 1 | `Verificador` y `Catalogo`; el resto depende de esta API. Sale de `main` después de fusionar la ola 7 |
| Ola 9 | U01-T10 | 1 | `Verificador.java` y `VeredictoPlugin.java`: no se puede repartir. Sale de `main` después de la ola 8 |
| Ola 10 | U01-T11 | 1 | Solo `src/test`. Podría ir junto con U01-T12 cuando esa tarea exista. Sale de `main` después de la ola 9 |
| Ola 11 | U01-T12 | 1 | `SuiteCombinaciones` en `testFixtures`; toca `VerificadorPropiedades` por C-26, así que va sola. Sale de `main` después de la ola 10 |
| Ola 12 | U01-T13 | 1 | `verificador/build.gradle.kts` y `scripts/control-suite.sh`. Sale de `main` después de la ola 11 |
| Ola 13 | U01-T14 | 1 | Solo el resumen de U01. Cierra la unidad. Sale de `main` después de la ola 12 |
| Ola 14 | U04-T01 | 1 | Migración `V2__catalogo.sql`, repositorios y PRAGMA (C-04). Primera tarea de U04. Sale de `main` después de cerrar U01 |
| Ola 15 | U04-T02 | 1 | `CatalogoApi.registrarCore`; `backend` pasa a depender de `:verificador`. Sale de `main` después de la ola 14 |
| Ola 16 | U04-T03 | 1 | Rutas de cores y C-07. Sale de `main` después de la ola 15 |

Lo previsto era que cada ola saliera de la punta de `modulo-6-rehecho` después de fusionar los PRs de
la anterior. El responsable pidió avanzar sin abrir PRs todavía, así que **las ramas quedaron
apiladas**: cada tarea sale de la rama de la tarea de la que depende.

```text
modulo-6-rehecho
  └─ m6r/U01-T01
       ├─ m6r/U01-T02 ─ m6r/U01-T03 ─ m6r/U01-T04
       └─ m6r/U03-T01 ─ m6r/U03-T02 ─ m6r/U03-T03
```

Los PRs se fusionan en ese orden, de arriba hacia abajo.

## Cómo se arranca una tarea

```bash
git switch modulo-6-rehecho
git worktree add ../wt-m6r-U01-T01 -b m6r/U01-T01 modulo-6-rehecho
git hash-object tareas/U01-T01-raiz-gradle.md
```

Y en la sesión principal:

```text
Lee loop/orquestador.md y actúa según ese contrato. Mis unidades y tareas están en
unidades-y-tareas.md y el grafo en aidlc-docs/inception/application-design/unit-of-work-dependency.md.
Despacha U01-T01. Una sola tarea, quiero ver la ronda completa.
```

## Reglas propias del proyecto

- Ningún agente fusiona. Cada pull request lo fusiona el responsable del proyecto.
- El único clúster permitido es el `kind` local `kind-synkra` (AUTONOMIA-01).
- No se escribe en `aidlc-docs/aidlc-state.md` nada del loop.
- La máquina tiene JDK 25 y no tiene Java 21 ni el cliente `sqlite3`: Java 21 se provisiona desde
  Gradle y las lecturas de vuelta de SQLite usan el módulo `sqlite3` de Python.
