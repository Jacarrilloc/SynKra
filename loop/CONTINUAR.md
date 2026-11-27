# Prompt para continuar SynKra (loop del módulo 6, olas de U05 y U02)

> Pega este archivo completo al abrir una sesión nueva. Responde en español.
> Lo mantiene al día el orquestador después de cada hito (ver «Puntos de continuidad»).
> Última actualización: 2026-11-27 21:40 UTC (16:40 en Bogotá), sesión de Julian, que retomó la de David.

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
- **Fusiones.** En la sesión de Julian **el orquestador no fusiona** (la guía del curso lo penaliza). Los PRs quedan abiertos y los fusiona un humano del equipo (Julian o David), en el orden que dice la tabla. Antes, David había acordado que el orquestador fusionara cuando hubiera varios PRs; esa regla no se usa aquí.
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

## Estado al corte (2026-11-27 21:40 UTC)

- `main` en `cf00671`. U01, U03 y U04 terminadas. PRs #54 a #76 fusionados.
- `CANDIDATAS.md` llega hasta **C-90**. La siguiente libre es **C-91**.
- **Nadie corre en este momento salvo lo que dice «en vuelo»**: al cortarse la sesión, ese agente se perdió y hay que repetirlo.

### PRs abiertos (los fusiona un humano, con merge commit, en este orden)

| Orden | PR | Rama → base | Contenido |
|---|---|---|---|
| 1 | #77 | `orq/U05-U02-tareas` → `main` | Tareas de las olas 30 a 39, candidatas C-75 a C-90, auditoría, `loop/README.md` y este archivo. Sigue recibiendo commits |
| 2 | #78 | `m6r/U05-T04` → `main` | U05-T04 `aplicar`, VERDE ronda 2 |
| 3 | #79 | `m6r/U02-T04` → `main` | U02-T04 `ArranquePlugins`, VERDE ronda 2 |
| 4 | #80 | `m6r/U02-T05` → `m6r/U02-T04` | U02-T05 concordancia con la suite, VERDE ronda 2 |
| 5 | #81 | `m6r/U05-T05` → `m6r/U05-T04` | U05-T05 `estado` y `contextoFallo`, VERDE ronda 3 |
| 6 | #82 | `m6r/U02-T06` → `m6r/U02-T05` | U02-T06 fallo de inicio y readiness, VERDE ronda 3 |
| 7 | #83 | `m6r/U05-T06` → `m6r/U05-T05` | U05-T06 `leerPais` y formato C-81, VERDE ronda 3 |

Al fusionar la base de un PR apilado, GitHub cambia su base a `main`. El #6 es antiguo y no se toca. Ninguno tiene CI (0 check runs) y todos estaban `clean` al corte.

### Tareas en curso

| Tarea | Worktree / rama | Desde | Estado | Siguiente paso |
|---|---|---|---|---|
| U02-T07 endpoints del core (ola 33) | `../wt-m6r-U02-T07`, `m6r/U02-T07` | `cb2096a` | Codificador ronda 1 hecho, `95719db` (4/4 CA; `EndpointsCoreTest` 10 pruebas; 7 mutantes). **En vuelo: revisor de la ronda 1** (su informe no está guardado) | Repetir el revisor de la ronda 1 con base `cb2096a` y HEAD `95719db`. Candidatas que propuso el codificador: límite de tamaño de cuerpo propio y C-86 (sin `detener()` al cerrar). Si VERDE: PR a `m6r/U02-T06` |
| U05-T07 script de descarga (ola 33) | `../wt-m6r-U05-T07`, `m6r/U05-T07` | `5e7a45e` | Codificador ronda 1 hecho, `dd8a95f` (16 casos en `init-descarga/test.sh`; 5 mutantes; rojo 15/16). **No se ha despachado el revisor.** `shellcheck` no está instalado: esa parte de CA-1 no corrió | Instalar shellcheck sin root (`pip download shellcheck-py` ya funciona en el contenedor; el wheel trae el binario) o declararlo al revisor; despachar el revisor de la ronda 1 con base `5e7a45e`. Decisión del codificador a revisar: líneas vacías de `artefactos.txt` se saltan, el resto fuera de formato da código 3. Si VERDE: PR a `m6r/U05-T06` |

Historia corta de lo terminado hoy (detalle en `revisiones/<tarea>/` de cada rama):
- U05-T04 y U02-T04: NO-VERDE en ronda 1, VERDE en ronda 2.
- U02-T05: VERDE en ronda 2. U05-T05 y U02-T06: VERDE en ronda 3.
- U05-T06: NO-VERDE en rondas 1 y 2 (F-06: `!!null x` en la raíz se leía como cero plugins), VERDE en ronda 3; el AMARILLO F-08 pasó a C-90.

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
