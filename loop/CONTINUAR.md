# Prompt para continuar SynKra (loop del módulo 6, olas de U05 y U02)

> Pega este archivo completo al abrir una sesión nueva. Responde en español.
> Lo mantiene al día el orquestador después de cada hito (ver «Puntos de continuidad»).
> Última actualización: 2026-11-27 23:45 UTC (18:45 en Bogotá), sesión **local** de Julian en su máquina (`/home/julian/Escritorio/Proyectos/SynKra`). Desde ahora todo se hace en local. Julian configuró `gh` (23:41 UTC) y aprobó (23:42 UTC) que el orquestador haga commit **y push** en cada hito y abra el PR de cada tarea VERDE, para que David pueda continuar si la sesión se queda sin tokens. El orquestador no fusiona. Java 21: `JAVA_HOME=~/.gradle/jdks/eclipse_adoptium-21-aarch64-linux.2`.

---

## Puntos de continuidad (metodología de varios commits)

La sesión de David se cortó a mitad de una ronda y se perdió el informe del revisor, que no estaba guardado. La de Julian (27-nov) siguió esta metodología: unos 20 commits de continuidad en `orq/U05-U02-tareas` y en cada rama de tarea.
Para que eso no vuelva a pasar:

1. **Un commit y un push por hito, nunca al final.** Los hitos son:
   - informe del revisor guardado en `revisiones/<tarea>/ronda-N.md`, en la rama de la tarea;
   - codificador despachado o vuelto, con su sha;
   - candidatas nuevas en `CANDIDATAS.md`;
   - tareas nuevas escritas;
   - PR abierto.
2. **Este archivo se actualiza en cada hito** (tabla «Estado al corte»), con commit y push en `orq/U05-U02-tareas`. Así, si la sesión se corta, la tabla dice exactamente qué agente estaba corriendo y qué falta.
3. **Lo que está en vuelo se anota antes de lanzarlo.** Si un agente estaba corriendo al cortarse la sesión, su resultado se perdió: la sesión nueva vuelve a despachar esa misma ronda (el codificador revisa primero si ya hay commits suyos en la rama; el revisor siempre se repite).
4. **Antes de despachar nada, se revisa GitHub**: `git fetch` y la fecha del último commit de cada rama de la tabla. Si otra persona empujó después de la última actualización de este archivo, se avisa y no se pisa.
5. **El informe del revisor se guarda tal cual.** Si llega como mensaje del subagente, se extrae del transcript con un script, sin reescribirlo: se toma la entrada de texto más larga de la llamada de entrega (`SubagentHandback`) en el archivo `.output` del agente. Si no hay script, se copia el texto del mensaje sin tocarlo.
6. **Commit pequeño y frecuente en las ramas de tarea también**: el codificador hace push al terminar cada ronda, y el orquestador hace commit del informe del revisor en la rama de la tarea en cuanto llega.

## Cómo se trabaja

- **El loop**: el orquestador es la sesión principal y sigue `loop/orquestador.md`. El `codificador` (subagente sonnet, `.claude/agents/codificador.md`) implementa. El `revisor` (subagente opus, solo lectura, `.claude/agents/revisor.md`) juzga. El orden de las olas está en `loop/README.md`.
- **Cada tarea:** rama `m6r/<tarea>` con su worktree en `../wt-m6r-<tarea>`; despachar al codificador con la ruta y el hash de la tarea; despachar al revisor; guardar su informe tal cual; si sale NO-VERDE, arbitrar y redespachar al mismo codificador, que responde en `ronda-N-respuesta.md`; si sale VERDE, abrir el PR.
- **El orquestador** solo escribe tareas, informes del revisor tal cual, `CANDIDATAS.md`, `aidlc-docs/audit.md`, `loop/README.md` y este archivo. Lo hace en la rama `orq/U05-U02-tareas` (worktree `../wt-orq-U04`).
- **Fusiones (regla de David, reafirmada el 2026-11-27 a las 21:51 UTC).** Si hay un solo PR listo, lo fusiona un humano con un clic. Si hay varios, los fusiona el orquestador, en orden, con merge commit, y lo anota en `aidlc-docs/audit.md`. En una pila, antes de fusionar cada PR apilado se cambia su base a `main` con `gh pr edit N --base main`. (En la sesión de Julian el orquestador no fusionaba.)
- **Olas apiladas.** Mientras los PRs de una ola no se fusionen, la ola siguiente sale de la punta en VERDE de la tarea de la que depende, y su PR apunta a esa rama.
- **Push** (git no tiene credenciales propias):
  ```bash
  git -c credential.helper= -c 'credential.helper=!gh auth git-credential' push origin <rama>
  ```
  En el contenedor en la nube, `gh` solo sirve para la API REST (`gh api repos/...`), no GraphQL: los PRs se abren con `gh api repos/Jacarrilloc/SynKra/pulls -f title=... -f head=... -f base=... -f body=...`.

## Reglas del entorno

- **Nada de `kubectl` ni de clúster real.** Solo el servidor simulado de Fabric8, con `kubeconfig`, `KUBECONFIG`, `HOME` y `user.home` falsos (`mktemp -d`), y `GRADLE_USER_HOME` real para reutilizar cachés.
- En la máquina de David solo hay JDK 25 (Java 21 lo provisiona Gradle). En el contenedor de Julian hay JDK 21. Maven Central a veces responde 429: esperar y reintentar.
- No hay cliente `sqlite3`: se lee con `python3 -I -c "import sqlite3..."`.
- Nada de `git stash` para el «rojo primero»: copia o worktree temporal.
- Pasar `</dev/null` a `git commit` y a `gh`. Pedir la evidencia pegada tal cual, con el sha del `--stat` tomado después del último commit.
- Criterios de Gradle como `:X:test --rerun :X:check` (C-69). En CA de diff acotado, la base es la del despacho, no `origin/main` (F-03 de U02-T04).

## Estado al corte (2026-11-27 22:10 UTC)

- `main` en `9289073`. U01, U03 y U04 terminadas. PRs #54 a #83 fusionados (#77 a #83 los fusionó el orquestador a las 21:55 UTC, según la regla de David).
- `CANDIDATAS.md` llega hasta **C-103**. La siguiente libre es **C-104**.
- **Nadie corre en este momento** (pausa de David a las 22:08 UTC). No hay agentes en vuelo.

### PRs abiertos

| Orden | PR | Rama → base | Contenido |
|---|---|---|---|
| 1 | #84 | `m6r/U02-T07` → `main` | U02-T07 endpoints del core, VERDE ronda 2 |
| 2 | #85 | `m6r/U05-T07` → `main` | U05-T07 script de descarga, VERDE ronda 2 |
| 3 | #86 | `m6r/U02-T08` → `m6r/U02-T07` | U02-T08 el core no conoce países, VERDE ronda 1 (apilado sobre #84) |
| 4 | #87 | `m6r/U05-T08` → `m6r/U05-T07` | U05-T08 imagen del init, VERDE ronda 1 (apilado sobre #85) |
| 5 | #88 | `m6r/U02-T09` → `m6r/U02-T08` | U02-T09 plugin `kyc-co`, VERDE ronda 2 (apilado sobre #86) |
| 6 | #89 | `m6r/U02-T10` → `m6r/U02-T09` | U02-T10 plugin `kyc-pe`, VERDE ronda 1 (apilado sobre #88) |
| 7 | #90 | `m6r/U05-T09` → `m6r/U05-T08` | U05-T09 fronteras `kubernetes`, VERDE ronda 2 (apilado sobre #87) |
| 8 | #91 | `m6r/U02-T11` → `m6r/U02-T10` | U02-T11 imagen del core, VERDE ronda 1 (apilado sobre #89) |

El #6 es antiguo y no se toca.

### Tareas en curso

| Tarea | Worktree / rama | Desde | Estado | Siguiente paso |
|---|---|---|---|---|
| U05-T07 script de descarga (ola 33) | `../wt-m6r-U05-T07`, `m6r/U05-T07` | `5e7a45e` | **VERDE en la ronda 2** (informe en `696eecc`). PR #85. | Esperar fusión humana |
| U05-T08 imagen del init (ola 34) | `../wt-m6r-U05-T08`, `m6r/U05-T08` | `696eecc` | **VERDE en la ronda 1** (informe en `26b6124`). PR #87, apilado sobre #85. | Esperar fusión humana; U05-T09 sale de esta punta |
| U05-T09 fronteras `kubernetes` (ola 35) | `../wt-m6r-U05-T09`, `m6r/U05-T09` | `26b6124` | **VERDE en la ronda 2** (informe en `03943ec`). PR #90, apilado sobre #87. | Esperar fusión humana; U05-T10 sale de esta punta |
| U05-T10 resumen de U05 (ola 36) | `../wt-m6r-U05-T10`, `m6r/U05-T10` | `03943ec` (punta VERDE de U05-T09) | Codificador ronda 1 hecho, `f2a5806`. **En vuelo (01:50 UTC del 28-nov):** revisor ronda 1. | Si no hay `revisiones/U05-T10/ronda-1.md`, repetir el revisor (base `03943ec`, HEAD `f2a5806`) |
| U02-T09 plugin `kyc-co` (ola 35) | `../wt-m6r-U02-T09`, `m6r/U02-T09` | `d99cf36` | **VERDE en la ronda 2** (informe en `288e5c0`). PR #88, apilado sobre #86. | Esperar fusión humana; U02-T10 sale de esta punta |
| U02-T10 plugin `kyc-pe` (ola 36) | `../wt-m6r-U02-T10`, `m6r/U02-T10` | `288e5c0` | **VERDE en la ronda 1** (informe en `3538d67`). PR #89, apilado sobre #88. | Esperar fusión humana; U02-T11 sale de esta punta |
| U02-T11 imagen del core (ola 37) | `../wt-m6r-U02-T11`, `m6r/U02-T11` | `3538d67` | **VERDE en la ronda 1** (informe en `28a6ac3`). PR #91, apilado sobre #89. | Esperar fusión humana; U02-T12 sale de esta punta |
| U02-T12 humo del core (ola 38) | `../wt-m6r-U02-T12`, `m6r/U02-T12` | `28a6ac3` (punta VERDE de U02-T11) | **En vuelo (01:40 UTC del 28-nov):** codificador ronda 1, tarea con hash `0f03487`. | Si la rama no tiene commits del codificador, redespacharlo; si los tiene, revisor ronda 1 |

| U02-T08 el core no conoce países (ola 34) | `../wt-m6r-U02-T08`, `m6r/U02-T08` | `9d84b4c` (punta VERDE de U02-T07, #84 sin fusionar) | **VERDE en la ronda 1** (informe en `d99cf36`). PR #86, apilado. | Esperar fusión humana; U02-T09 sale de esta punta |

### Tareas escritas y sin despachar

Están en `tareas/` de `orq/U05-U02-tareas`. Antes de despachar, sacar el hash con `git hash-object` y releer la tarea contra lo que dejaron las previas.

| Ola | Tareas | Sale de |
|---|---|---|
| 34 | U05-T08 imagen del init ∥ U02-T08 el core no conoce países | punta VERDE de U05-T07 y de U02-T07. U05-T08 necesita Docker: `dockerd` se arranca a mano en segundo plano |
| 35 | U05-T09 fronteras `kubernetes` (C-68, C-70, C-73, C-79) ∥ U02-T09 plugin `kyc-co` | U05-T08 y U02-T08 |
| 36 | U05-T10 resumen de U05 ∥ U02-T10 plugin `kyc-pe` | U05-T09 y U02-T09 |
| 37 a 39 | U02-T11 imagen del core, U02-T12 humo del core, U02-T13 resumen de U02 | una tras otra |

## Lo que sigue (en orden)

1. Revisar GitHub (`git fetch`, último commit de cada rama de las tablas). Si alguien fusionó PRs o empujó, ajustar las bases antes de seguir.
2. Repetir el revisor de U02-T07 ronda 1 y despachar el de U05-T07 ronda 1. Guardar cada informe en `revisiones/<tarea>/ronda-N.md` de su rama, commit y push, y actualizar este archivo.
3. Seguir con las olas 34 a 39. Una tarea que llega a la ronda 4 se para y se lleva al equipo; las decisiones se toman con la opción recomendada (autorización de Julian del 27-nov).
4. Al cerrar cada unidad (U05 con U05-T10, U02 con U02-T13), marcar su plan en `aidlc-docs/construction/plans/`, actualizar la línea de Code Generation Parte 2 en `aidlc-docs/aidlc-state.md` y dejar `aidlc-docs/construction/<unidad>/code/summary.md` (el plan y el estado los toca el orquestador, no el codificador).
5. Pendiente de decidir con el equipo:
   - una tarea de pulido de U04 (C-57, C-60, C-61, C-63, C-65, C-66, C-67);
   - otra de U01/U02 (C-77, C-78, C-80, C-84, C-86, C-88, C-89);
   - C-90 para U06/U07.
