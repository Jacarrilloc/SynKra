# Prompt para continuar SynKra (loop del módulo 6, olas de U05 y U02)

> Pega este archivo completo al abrir una sesión nueva. Responde en español.
> Lo mantiene al día el orquestador después de cada hito (ver «Puntos de continuidad»).
> Última actualización: 2026-11-27 ~20:55 UTC (sesión de Julian, que retomó la de David).

---

## Puntos de continuidad (metodología de varios commits)

La sesión anterior se cortó a mitad de una ronda y se perdió el informe del revisor, que no estaba guardado.
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
5. **El informe del revisor se guarda tal cual.** Si llega como mensaje del subagente, se extrae del transcript (la última llamada de entrega) con un script, sin reescribirlo.

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

## Estado al corte

- `main` en `cf00671`. U01, U03 y U04 terminadas. PRs #54 a #76 fusionados.
- `CANDIDATAS.md` llega hasta **C-83**. La siguiente libre es **C-84**.
- PRs abiertos: **#77** (`orq/U05-U02-tareas`: tareas de las olas 30 y 31, candidatas C-75 a C-78, auditoría y este archivo) y el #6, antiguo, que no se toca.

### Ola 30 (U05-T04 ∥ U02-T04)

| Tarea | Rama | Ronda | Estado |
|---|---|---|---|
| U05-T04 `aplicar` | `m6r/U05-T04` | 2 | Ronda 1 NO-VERDE (informe en `revisiones/U05-T04/ronda-1.md`, commit `e085e5f`): F-01 a F-03 NARANJA, F-04 y F-05 AMARILLO; todo se arregla. **VERDE en la ronda 2** (`ronda-2.md`, rama en `0a52e2e`). **PR #78** abierto contra `main`, sin fusionar. AMARILLO F-06/F-07 → C-79 |
| U02-T04 `ArranquePlugins` | `m6r/U02-T04` | 2 | Ronda 1 NO-VERDE (informe en `revisiones/U02-T04/ronda-1.md`, commit `6917a8c`): F-01 y F-02 NARANJA; F-03 es de la especificación; F-04 fuera de alcance (C-78); F-05 y F-06 se arreglan. F-02 se arregla dentro de `core/` igualando las salvaguardas de `LectorDescriptor` (profundidad 32, etiquetas explícitas) y con una sola fuente para nombre y límites; la extracción a `verificador/` es C-77. **VERDE en la ronda 2** (`ronda-2.md`, rama en `ff3f184`). **PR #79** abierto contra `main`, sin fusionar. AMARILLO F-07/F-08 → C-80 |

Orden de fusión: #77, luego #78 (U05-T04), luego #79 (U02-T04).

**U05-T05 despachada** (ola 31): worktree `../wt-m6r-U05-T05`, rama `m6r/U05-T05` desde `0a52e2e`; ronda 1 NO-VERDE (`revisiones/U05-T05/ronda-1.md`, `7c85e17`): F-01 a F-03 NARANJA, F-04 a F-06 AMARILLO se arreglan, F-07 no. **Codificador de la ronda 2 despachado**; si la sesión se corta, mirar sus commits en `origin/m6r/U05-T05` y redespacharlo.

**U02-T05 despachada** (ola 31): worktree `../wt-m6r-U02-T05`, rama `m6r/U02-T05` desde `ff3f184`; ronda 1 del codificador hecha (`0aadf63`). **Revisor de la ronda 1 despachado**; si la sesión se corta, repetirlo.

### Ola 31 (U05-T05 ∥ U02-T05), escrita, sin despachar

- `tareas/U05-T05-estado-contexto.md` (hash `65a449cecd38706d8a1f17df2d817a5e51150f09`).
- `tareas/U02-T05-concordancia-suite.md` (hash `f95311817e9138f94cf701f894d68d17d2519d5c`).
- Se despachan cuando su T04 salga VERDE: `m6r/U05-T05` desde la punta VERDE de `m6r/U05-T04`, `m6r/U02-T05` desde la de `m6r/U02-T04`. El archivo de la tarea se lee desde `../wt-orq-U04` (la rama de tareas); la base del diff es la punta de la T04.

### Ola 32 (U05-T06 ∥ U02-T06), escrita, sin despachar

- `tareas/U05-T06-leer-pais.md` (hash `e94a10dc07aa29b842e2f2601efe3b0cfde155be`).
- `tareas/U02-T06-fallo-inicio-readiness.md` (hash `c05f5ff56958720575de95a6a1fac0b9a0a59ecb`).
- Cada una sale de la punta en VERDE de su T05.

### Ola 33 (U05-T07 ∥ U02-T07), escrita, sin despachar

- `tareas/U05-T07-script-descarga.md` y `tareas/U02-T07-endpoints-core.md` (hash con `git hash-object` en la rama de tareas).
- Cada una sale de la punta en VERDE de su T06.

### Ola 34 (U05-T08 ∥ U02-T08), escrita, sin despachar

- `tareas/U05-T08-imagen-init.md` y `tareas/U02-T08-core-sin-paises.md`. U05-T08 necesita Docker: en el contenedor en la nube hay que arrancar `dockerd` a mano.

## Lo que sigue

1. Terminar la ola 30: revisor de la ronda 2 de cada tarea, guardar su informe, más rondas si hace falta (una tarea que llega a la ronda 4 se para y se lleva al equipo), y abrir los PRs.
2. Ola 31 como arriba.
3. Escribir las tareas restantes en `orq/U05-U02-tareas` y añadir sus olas a `loop/README.md`:
   - **U05:** T09 fronteras, T10 resumen.
   - **U02:** T09 y T10 plugins `kyc-co` y `kyc-pe`, T11 imagen del core, T12 prueba de humo, T13 resumen.
   - Los planes están en `aidlc-docs/construction/plans/U05-*.md` y `U02-*.md`.
   - Candidatas que hay que meter: C-70, C-73 y C-68 en U05-T09; C-72 en U05-T07, U05-T08 y U02-T11 (script en `/usr/local/bin/descargar.sh`, `USER 10001` numérico); C-64 en U05-T07 (comprobar el sha256); C-71 y C-74 en las pruebas del core; C-76 en U07.
4. Pendiente de decidir con el equipo: una tarea de pulido de U04 (C-57, C-60, C-61, C-63, C-65, C-66, C-67) y otra de U01/U02 (C-77, C-78).
5. Al cerrar cada unidad: marcar su plan en `aidlc-docs/construction/plans/`, actualizar la línea de Code Generation Parte 2 en `aidlc-docs/aidlc-state.md` y dejar `aidlc-docs/construction/<unidad>/code/summary.md`.
