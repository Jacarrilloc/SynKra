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
- `CANDIDATAS.md` llega hasta **C-105**. La siguiente libre es **C-106**.
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
| 9 | #92 | `m6r/U05-T10` → `m6r/U05-T09` | U05-T10 resumen de U05, VERDE ronda 3 (apilado sobre #90) |
| 10 | #93 | `m6r/U02-T12` → `m6r/U02-T11` | U02-T12 humo del core, VERDE ronda 3 (apilado sobre #91) |
| 11 | #94 | `m6r/U02-T13` → `m6r/U02-T12` | U02-T13 resumen de U02, VERDE ronda 1 (apilado sobre #93) |

El #6 es antiguo y no se toca.

### Tareas en curso

| Tarea | Worktree / rama | Desde | Estado | Siguiente paso |
|---|---|---|---|---|
| U05-T07 script de descarga (ola 33) | `../wt-m6r-U05-T07`, `m6r/U05-T07` | `5e7a45e` | **VERDE en la ronda 2** (informe en `696eecc`). PR #85. | Esperar fusión humana |
| U05-T08 imagen del init (ola 34) | `../wt-m6r-U05-T08`, `m6r/U05-T08` | `696eecc` | **VERDE en la ronda 1** (informe en `26b6124`). PR #87, apilado sobre #85. | Esperar fusión humana; U05-T09 sale de esta punta |
| U05-T09 fronteras `kubernetes` (ola 35) | `../wt-m6r-U05-T09`, `m6r/U05-T09` | `26b6124` | **VERDE en la ronda 2** (informe en `03943ec`). PR #90, apilado sobre #87. | Esperar fusión humana; U05-T10 sale de esta punta |
| U05-T10 resumen de U05 (ola 36) | `../wt-m6r-U05-T10`, `m6r/U05-T10` | `03943ec` | **VERDE en la ronda 3** (informe en `b91f7d1`). PR #92, apilado sobre #90. | Cuando la pila de U05 (#85, #87, #90, #92) esté fusionada: commit de cierre de U05 en la rama del orquestador (plan marcado, línea de Code Generation Parte 2 en `aidlc-state.md`, `audit.md`), como el `113bda9` de U04 |
| U02-T09 plugin `kyc-co` (ola 35) | `../wt-m6r-U02-T09`, `m6r/U02-T09` | `d99cf36` | **VERDE en la ronda 2** (informe en `288e5c0`). PR #88, apilado sobre #86. | Esperar fusión humana; U02-T10 sale de esta punta |
| U02-T10 plugin `kyc-pe` (ola 36) | `../wt-m6r-U02-T10`, `m6r/U02-T10` | `288e5c0` | **VERDE en la ronda 1** (informe en `3538d67`). PR #89, apilado sobre #88. | Esperar fusión humana; U02-T11 sale de esta punta |
| U02-T11 imagen del core (ola 37) | `../wt-m6r-U02-T11`, `m6r/U02-T11` | `3538d67` | **VERDE en la ronda 1** (informe en `28a6ac3`). PR #91, apilado sobre #89. | Esperar fusión humana; U02-T12 sale de esta punta |
| U02-T12 humo del core (ola 38) | `../wt-m6r-U02-T12`, `m6r/U02-T12` | `28a6ac3` | **VERDE en la ronda 3** (informe en `63d99fa`). PR #93, apilado sobre #91. | Esperar fusión humana; U02-T13 sale de esta punta |
| U02-T13 resumen de U02 (ola 39) | `../wt-m6r-U02-T13`, `m6r/U02-T13` | `63d99fa` | **VERDE en la ronda 1** (informe en `1ab000c`). PR #94, apilado sobre #93. | Cuando la pila de U02 esté fusionada: commit de cierre de U02 (plan, `aidlc-state.md`, `audit.md`) |

| U02-T08 el core no conoce países (ola 34) | `../wt-m6r-U02-T08`, `m6r/U02-T08` | `9d84b4c` (punta VERDE de U02-T07, #84 sin fusionar) | **VERDE en la ronda 1** (informe en `d99cf36`). PR #86, apilado. | Esperar fusión humana; U02-T09 sale de esta punta |

| U06-A registro base (T01+T02+T03) | `../wt-m6r-U06-A`, `m6r/U06-A` | `97f9f80` (`main` tras fusionar #84 a #94) | **En vuelo (00:35 UTC del 28-nov):** codificador ronda 1, tarea con hash `74dc0ba`. | Si la rama no tiene commits del codificador, redespacharlo; si los tiene, revisor ronda 1 |
| U07-A planes y país permitido (T01+T02) | `../wt-m6r-U07-A`, `m6r/U07-A` | `97f9f80` | **En vuelo (00:35 UTC del 28-nov):** codificador ronda 1, tarea con hash `c6bdfd7`. | Igual |

### Tareas escritas y sin despachar

Están en `tareas/` de `orq/U05-U02-tareas`. Antes de despachar, sacar el hash con `git hash-object` y releer la tarea contra lo que dejaron las previas.

| Ola | Tareas | Sale de |
|---|---|---|
| 34 | U05-T08 imagen del init ∥ U02-T08 el core no conoce países | punta VERDE de U05-T07 y de U02-T07. U05-T08 necesita Docker: `dockerd` se arranca a mano en segundo plano |
| 35 | U05-T09 fronteras `kubernetes` (C-68, C-70, C-73, C-79) ∥ U02-T09 plugin `kyc-co` | U05-T08 y U02-T08 |
| 36 | U05-T10 resumen de U05 ∥ U02-T10 plugin `kyc-pe` | U05-T09 y U02-T09 |
| 37 a 39 | U02-T11 imagen del core, U02-T12 humo del core, U02-T13 resumen de U02 | una tras otra |

## Plan para acelerar (aprobado por Julian el 2026-11-28 a las 00:19 UTC, ver `aidlc-docs/audit.md`)

- Se cortan U11 y U10 (Should, R9). La demo se hace con `curl` si U09 no llega. U08 necesita `kind`: sus tareas se dejan escritas para el equipo.
- Hasta 3 tareas en paralelo; tareas del plan agrupadas en un solo despacho; tope de dos rondas cuando solo quedan AMARILLOS; revisor `sonnet` solo en tareas de documentación (resúmenes).
- Agrupación (archivos en `tareas/`, con el nombre `U06-A`, etc.):
  - U06-A = T01+T02+T03 (migración `V3__registro.sql`, repositorios, transiciones, contexto truncado). Sale de `b91f7d1` (punta VERDE de U05-T10).
  - U06-B = T04+T06 (historial, detalle, reinicio). Sale de U06-A.
  - U06-C = T05+T07 (vista general con concordancia, fronteras). Sale de U06-B.
  - U06-D = T08 (resumen).
  - U07-A = T01+T02 (migración `V4__despliegue.sql`, repositorio de planes, país permitido). Sale de `b91f7d1`, en paralelo con U06-A.
  - U07-B = T03+T04+T05 (verificación en vivo, crear plan, PBT de cambios). Sale de U07-A.
  - U07-C = T06 (plan pendiente nunca aplica y vence). Sale de U07-B.
  - U07-D = T07+T11 (confirmar, carpeta equivocada). Necesita `registro.api` de U06-A: sale de la unión de U07-C y U06-C.
  - U07-E = T08+T09 (vigilancia y retomar). U07-F = T10 (reversión). U07-G = T12+T13 (fronteras y resumen).

## Lo que sigue (en orden)

1. Revisar GitHub (`git fetch`, último commit de cada rama de las tablas). Si alguien fusionó PRs o empujó, ajustar las bases antes de seguir.
2. Repetir el revisor de U02-T07 ronda 1 y despachar el de U05-T07 ronda 1. Guardar cada informe en `revisiones/<tarea>/ronda-N.md` de su rama, commit y push, y actualizar este archivo.
3. Seguir con las olas 34 a 39. Una tarea que llega a la ronda 4 se para y se lleva al equipo; las decisiones se toman con la opción recomendada (autorización de Julian del 27-nov).
4. Al cerrar cada unidad (U05 con U05-T10, U02 con U02-T13), marcar su plan en `aidlc-docs/construction/plans/`, actualizar la línea de Code Generation Parte 2 en `aidlc-docs/aidlc-state.md` y dejar `aidlc-docs/construction/<unidad>/code/summary.md` (el plan y el estado los toca el orquestador, no el codificador).
5. Pendiente de decidir con el equipo:
   - una tarea de pulido de U04 (C-57, C-60, C-61, C-63, C-65, C-66, C-67);
   - otra de U01/U02 (C-77, C-78, C-80, C-84, C-86, C-88, C-89);
   - C-90 para U06/U07.
