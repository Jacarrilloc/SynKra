# Prompt para continuar SynKra (loop del módulo 6, olas de U05 y U02)

> Pega este archivo completo al abrir una sesión nueva en `/home/casdav/Projects/Master/SynKra`.
> Responde en español. Fecha del corte: 2026-11-27, 20:20 UTC.

---

## Cómo se trabaja

- **El loop** (módulo 6 del curso): el orquestador es la sesión principal y sigue `loop/orquestador.md`. El `codificador` (subagente sonnet, `.claude/agents/codificador.md`) implementa. El `revisor` (subagente opus, solo lectura, `.claude/agents/revisor.md`) juzga. El orden de las olas está en `loop/README.md`.
- **Cada tarea:**
  - Rama `m6r/<tarea>`, con su worktree en `../wt-m6r-<tarea>`.
  - Despachar al codificador con la ruta y el hash de la tarea.
  - Despachar al revisor.
  - Guardar el informe del revisor **tal cual** en `revisiones/<tarea>/ronda-N.md`, en la rama de la tarea, con commit y push.
  - Si sale NO-VERDE: arbitrar y redespachar al mismo codificador, que responde en `ronda-N-respuesta.md`.
  - Si sale VERDE: abrir el PR contra `main`.
- **El orquestador** solo escribe tareas, informes del revisor tal cual, `CANDIDATAS.md`, `aidlc-docs/audit.md` y `loop/README.md`. Lo hace en la rama `orq/U05-U02-tareas` (worktree `../wt-orq-U04`). Esa rama se reinicia sobre `origin/main` después de cada fusión, con `--force-with-lease` (solo lleva historial ya fusionado).
- **Las olas** salen apiladas sobre la rama de tareas: primero se escriben las tareas en `orq/U05-U02-tareas`, después se crean los worktrees desde `origin/orq/U05-U02-tareas`, y la base del diff es esa rama.
- **Regla de fusión de David (2026-11-27, 16:14Z):** si hay un solo PR, lo fusiona David con un clic. Si son varios, los fusiona el orquestador, en orden, y lo anota en `aidlc-docs/audit.md`. Se le advirtió que el curso penaliza que un agente fusione.
- **Push.** Hay que subir cada ronda a GitHub, para que el compañero pueda seguir. Git no tiene credenciales propias:
  ```bash
  git -c credential.helper= -c 'credential.helper=!gh auth git-credential' push origin <rama>
  ```

## Reglas de esta máquina

- Solo hay JDK 25. Java 21 lo provisiona Gradle. No hay cliente `sqlite3`: se lee con `python3 -I -c "import sqlite3..."`.
- `~/.kube/config` tiene contextos `kind` reales. **Nada de `kubectl` y ninguna prueba contra un clúster.** Solo se usa el servidor simulado de Fabric8, con `kubeconfig`, `KUBECONFIG`, `HOME` y `user.home` falsos.
- Nada de `git stash` para el «rojo primero». Se usa una copia o un worktree temporal: un corte de red deja el worktree roto.
- Pasar `</dev/null` a `git commit` y a `gh`. Pedir siempre la evidencia pegada tal cual, con el sha del `--stat` anotado.
- Escribir los criterios de Gradle como `:X:test --rerun :X:check` (C-69).

## Estado al corte

- `main` está en `cf00671`. Las unidades U01, U03 y U04 están terminadas.
- Hoy se fusionaron los PRs #54 a #76:
  - U04-T07 a T13 y el cierre de U04 (#66 y #67).
  - U05-T01 a T03 y U02-T01 a T03, en las olas 27, 28 y 29, dos tareas en paralelo por ola.
- `CANDIDATAS.md` llega hasta C-74. La siguiente libre es **C-75**.

### Ola 30, en curso (dos tareas en paralelo, sin fusionar)

| Tarea | Rama y worktree | Estado |
|---|---|---|
| U05-T04 `aplicar` (server-side apply) | `m6r/U05-T04`, `../wt-m6r-U05-T04`, push en `a280041` | Ronda 1 hecha por el codificador. **El revisor de la ronda 1 se lanzó** y su informe no se ha guardado. Si no llegó, hay que redespachar al revisor. El codificador usó la vía alterna de la tarea: el modo CRUD no crea con apply, así que lee de vuelta el PATCH |
| U02-T04 `ArranquePlugins` | `m6r/U02-T04`, `../wt-m6r-U02-T04`, push en `cfd660e` | Ronda 1 hecha por el codificador. **El revisor de la ronda 1 se lanzó** y su informe no se ha guardado. Puntos a juzgar: `CargadorPlugins` pasó a `@Component`, se añadió `snakeyaml-engine` al core y el rojo se hizo contra un stub |

- Las tareas de la ola 30 están en el PR abierto **#77** (`orq/U05-U02-tareas`, en `ceb914c`), que hay que fusionar **antes** que los PRs de U05-T04 y U02-T04.
- El PR #6 (`tarea/U03-T01`) es antiguo y está abierto. No se toca; que lo cierre David si quiere.

## Lo que sigue

1. Terminar la ola 30:
   - guardar el informe de cada revisor;
   - si sale NO-VERDE, hacer las rondas que hagan falta; una tarea que llega a la ronda 4 se para y se le lleva a David;
   - con las dos en VERDE, abrir sus PRs y fusionar en orden: #77, U05-T04, U02-T04;
   - cerrar con un PR de candidatas y auditoría desde `orq/U05-U02-tareas`, reiniciada sobre main.
2. Escribir las siguientes tareas en `orq/U05-U02-tareas` y añadir las olas a `loop/README.md`.
   - **U05:** T05 `estado` y `contextoFallo`, T06 `leerPais`, T07 `init-descarga/descargar.sh`, T08 imagen del init, T09 fronteras y T10 resumen.
   - **U02:** T05 suite de U01 en el arranque, T06 fallo al iniciar y readiness, T07 endpoints, T08 el core no conoce países, T09 y T10 plugins `kyc-co` y `kyc-pe`, T11 imagen del core, T12 prueba de humo y T13 resumen.
   - Los planes están en `aidlc-docs/construction/plans/U05-*.md` y `U02-*.md`.
   - Candidatas que hay que meter en esas tareas:
     - C-70 y C-73 van en U05-T09.
     - C-72 obliga a U05-T07, U05-T08 y U02-T11: el script en `/usr/local/bin/descargar.sh` y `USER 10001` numérico.
     - C-64 va en U05-T07: comprobar el sha256 de lo descargado.
     - C-71 y C-74 afectan a las pruebas del core.
3. Pendiente de decidir con David: una tarea de pulido de U04 con C-57, C-60, C-61, C-63, C-65, C-66 y C-67, y las candidatas de otras unidades que nombran U04 (C-05, C-08, C-12, C-13, C-19 y C-20).
4. Al cerrar cada unidad (U05 y U02):
   - marcar su plan en `aidlc-docs/construction/plans/`;
   - actualizar la línea de Code Generation Parte 2 en `aidlc-docs/aidlc-state.md`;
   - dejar el resumen en `aidlc-docs/construction/<unidad>/code/summary.md`.
