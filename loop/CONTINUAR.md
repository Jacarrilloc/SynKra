# Prompt para continuar SynKra (loop del módulo 6: U06 y U07)

> Pega este archivo completo al abrir una sesión nueva. Responde en español.
> Lo mantiene al día el orquestador después de cada hito (ver «Puntos de continuidad»).
> Última actualización: 2026-11-28 00:55 UTC (19:55 en Bogotá), sesión **local** de David (`/home/casdav/Projects/Master/SynKra`, worktree del orquestador `../wt-orq-U04`). David confirmó a las 00:46 UTC que la sesión de Julian está detenida.
> **Punto de retomar:** el commit de este archivo en `orq/U05-U02-tareas` (el sha lo da `git log -1 --format=%h -- loop/CONTINUAR.md`).

---

## Resumen en una línea

U01, U02, U03, U04 y U05 están **terminadas y fusionadas en `main`** (`97f9f80`, 710 pruebas en verde). Ahora se hacen U06 y U07 con tareas **agrupadas**, según el plan para acelerar que aprobó Julian. U10 y U11 se cortaron (R9). U08 necesita `kind` y se deja escrita para el equipo.

## Lo que se hizo en la sesión de Julian (27 y 28 de noviembre)

- **U05-T07 a T10** (script de descarga, imagen del init, fronteras de `kubernetes`, resumen) y **U02-T07 a T13** (endpoints, «el core no conoce países», plugins `kyc-co` y `kyc-pe`, imagen del core, humo, resumen): todas VERDES, PRs #84 a #94.
- **Fusión**: Julian pidió (00:25 UTC) que el orquestador fusionara. Se fusionaron en orden, con merge commit y cambiando antes la base a `main`: U05 (#85, #87, #90, #92) y U02 (#84, #86, #88, #89, #91, #93, #94). Está en `aidlc-docs/audit.md`.
- **Comprobación**: `./gradlew test --rerun check` sobre `main` `97f9f80` → `salida=0`, 710 pruebas, 0 saltos, 0 fallos.
- **Cierre de U05 y U02**: planes marcados, `aidlc-state.md` y auditoría, en la rama `orq/U05-U02-tareas`. Va en el **PR #95** (rama del loop → `main`), **abierto**. El control de permisos de la sesión de Julian bloqueó la fusión porque no era uno de los PRs que él nombró: lo fusiona un humano.
- `CANDIDATAS.md` llega hasta **C-124**. La siguiente libre es **C-125**.

## Plan para acelerar (aprobado por Julian el 2026-11-28 a las 00:19 UTC)

- Se cortan U11 y U10 (*Should*, R9). La demo se hace con `curl` si U09 no llega. Mínimo de la North Star: U01 a U08.
- Hasta **3 tareas en paralelo**; tareas del plan **agrupadas** en un solo despacho; **tope de dos rondas** cuando solo quedan AMARILLOS (los ROJO y NARANJA nunca se bajan); revisor `sonnet` solo en tareas de documentación (resúmenes).
- **U08 necesita `kind`**: en la máquina de Julian no hay clúster. Sus tareas se escriben para que las corra el equipo.

### Agrupación de U06 y U07 (archivos en `tareas/`)

| Grupo | Tareas del plan | Sale de | Notas |
|---|---|---|---|
| U06-A | T01+T02+T03: `V3__registro.sql`, repositorios, `RegistroApi` (`registrar`, `actualizarResultado`, transiciones R-R1 a R-R3), contexto truncado R-R4 | `97f9f80` | Escrita, **en curso** |
| U06-B | T04+T06: historial y detalle (R-R5), sobrevive al reinicio (R-R8) | punta VERDE de U06-A | Escribiéndose (ver «En vuelo») |
| U06-C | T05+T07: vista general con concordancia (R-R6, R-R7), fronteras de `registro` | U06-B | Por escribir. Lee `synkra.paises` por su cuenta (no puede depender de `despliegue`) |
| U06-D | T08: resumen de U06 | U06-C | Por escribir. Revisor `sonnet` |
| U07-A | T01+T02: `V4__despliegue.sql`, repositorio de planes, país permitido (R-D1) | `97f9f80` | **VERDE**, PR #96 |
| U07-B | T03+T04+T05: verificación en vivo (R-D2, S4), crear plan con resumen y cambios (R-D3, R-D4), PBT-03 de los cambios | punta VERDE de U07-A | Escribiéndose. No puede depender de `registro`: «lo que corre» sale de `leerPais` de U05 |
| U07-C | T06: un plan sin confirmar nunca aplica (R-D13) y vence (Q6) | U07-B | Por escribir. Tiene que decidir el bean `Clock` (ni U06-A ni U07-A lo crean) |
| U07-D | T07+T11: confirmar (R-D5 a R-D9), carpeta equivocada (P4) | **unión** de U07-C y U06-C | Por escribir. Primera que usa `registro.api` |
| U07-E | T08+T09: vigilancia sin reintentos (R-D10), retomar tras reinicio (R-D12) | U07-D | Por escribir. Ver C-76, C-82, C-83 |
| U07-F | T10: reversión como plan nuevo (R-D11) | U07-E | Por escribir |
| U07-G | T12+T13: fronteras de `despliegue` y resumen de U07 | U07-F | Por escribir |

Contratos que fijaron U06-A y U07-A (los asumen las tareas siguientes; están en sus «Decisiones»): el id del despliegue es el id del plan (`dep_plan.id` con `AUTOINCREMENT`, `reg_despliegue.id` sin él); `ContextoFallo` es un tipo propio de `registro.api` (U07 mapea desde `ContextoFalloK8s`); `reg_contexto.eventos` es texto; truncado R-R4 guarda el final con `[truncado]\n`; `synkra.paises=${SYNKRA_PAISES:co,pe}` en `application.properties`; Flyway sin `outOfOrder` (en la rama de U07-A hay V1, V2, V4); `dep_plan.origen_id` sin clave foránea; puertos de prueba 18086 (U06-A), 18087 y 18088 (U07-A), 18089 y 18090 (U06-B y U07-B).

## Estado al corte (2026-11-28 00:40 UTC)

### PRs

#95 a #104 **fusionados** por autorización de David (la última tanda, #100 U07-C y #101 U06-C, a las 02:02 UTC). `main` en `39a242c` (852 pruebas en verde). **U06 TERMINADA** (#97, #99, #101, #103). El #6 es antiguo y no se toca.

### En vuelo (sesión de David, 2026-11-28 00:55 UTC; si la sesión se cortó, se perdieron)

| Tarea | Worktree / rama | Desde | Estado | Qué hacer al retomar |
|---|---|---|---|---|
| U06-A registro base | `m6r/U06-A` | `97f9f80` | **VERDE ronda 2**, fusionado (#97) | — |
| U07-A planes y país | `m6r/U07-A` | `97f9f80` | **VERDE ronda 1**, fusionado (#96) | — |
| U07-E vigilancia y reinicio | `../wt-m6r-U07-E`, `m6r/U07-E` | `39a242c` | Codificador ronda 1 terminó (`2d9d21f`, código en `4869522`). **Revisor ronda 1 despachado** | Repetir el revisor ronda 1 (base `39a242c`) si no hay `revisiones/U07-E/ronda-1.md` |
| U07-F y U07-G | (sin rama) | — | U07-G **escrita** (fronteras de `despliegue` con `allowedDependencies` en su `package-info.java`, y resumen de U07; revisor normal, no `sonnet`, porque toca código). Antes de U07-G, fusionar la rama del orquestador a `main` para que C-121 a C-123 estén en su árbol |
| U07-F reversión | `../wt-m6r-U07-F`, `m6r/U07-F` | `2d9d21f` (punta de U07-E en ronda 1, **especulativo**: pedido de David de correr en paralelo) | **Codificador ronda 1 despachado** (tarea con hash `2f97d8d`). Si U07-E cambia en una ronda 2, U07-F se rebasa sobre la punta VERDE de U07-E | Sin commits, redespachar; con bitácora completa, revisor ronda 1 |
| (fila anterior de U07-E y U07-F) | (sin rama) | — | **Escritas** (hashes `4252363` y `2f97d8d`, tras corregir los greps de C-122). U07-E: `TareasProgramadas` con `synkra.programacion.activa`, apagada en pruebas; toca `kubernetes` por C-76, C-82 y C-83. U07-F: 422 `ORIGEN_NO_LISTO` y `ORIGEN_DESACTUALIZADO` | En cadena tras U07-D |
| U06-C y U07-C | (sin rama) | — | **Escritas** (hashes `004d66e` y `7dbe5d3`). U07-C crea el bean `Clock` en `plataforma/Reloj.java` (U06-C no puede crear otro); U06-C usa `@MockitoBean AdaptadorKubernetes`; confirmar da 501 hasta U07-D | Despachar U06-C cuando U06-B salga VERDE y U07-C cuando U07-B salga VERDE |

En la máquina de Julian, las sesiones de Julian siguen trabajando mientras no se diga lo contrario: **antes de despachar, David revisa `git fetch` y la fecha del último commit de cada rama** (punto 4 de abajo) para no pisar a nadie.

## Lo que sigue (en orden)

1. `git fetch`; revisar las ramas de la tabla «En vuelo» y el PR #95. Fusionar el #95 (lo hace un humano).
2. Cerrar U06-A y U07-A (revisor, rondas, PR a `main`).
3. Escribir y despachar U06-B y U07-B en paralelo (salen de las puntas VERDES de U06-A y U07-A). Después U06-C ∥ U07-C, luego U06-D (resumen, revisor `sonnet`) y U07-D (unión de U07-C y U06-C), luego U07-E, U07-F y U07-G.
4. Al cerrar U06 y U07: planes marcados en `aidlc-docs/construction/plans/`, línea de Code Generation Parte 2 en `aidlc-docs/aidlc-state.md`, `audit.md`.
5. Escribir las tareas de U08 (instalación y límite, `kind-synkra`) para que el equipo las corra a mano. El loop no toca clúster.
6. U09 (front) solo si sobra tiempo; si no, la demo es con `curl`.
7. Pendiente de decidir con el equipo: pulido de U04 (C-57, C-60, C-61, C-63, C-65 a C-67); pulido de U01/U02 (C-77, C-78, C-80, C-84 a C-86, C-88, C-89, C-96, C-99, C-100, C-105); pulido de U05 (C-93, C-94, C-97, C-102, C-104); imágenes en U08 (C-95, C-98, C-101, C-103); C-90 para U06/U07.

---

## Puntos de continuidad (metodología de varios commits)

1. **Un commit y un push por hito, nunca al final.** Los hitos son:
   - informe del revisor guardado en `revisiones/<tarea>/ronda-N.md`, en la rama de la tarea;
   - codificador despachado o vuelto, con su sha;
   - candidatas nuevas en `CANDIDATAS.md`;
   - tareas nuevas escritas;
   - PR abierto.
2. **Este archivo se actualiza en cada hito**, con commit y push en `orq/U05-U02-tareas`.
3. **Lo que está en vuelo se anota antes de lanzarlo.** Si un agente estaba corriendo al cortarse la sesión, su resultado se perdió: la sesión nueva vuelve a despachar esa misma ronda (el codificador revisa primero si ya hay commits suyos en la rama; el revisor siempre se repite).
4. **Antes de despachar nada, se revisa GitHub**: `git fetch` y la fecha del último commit de cada rama. Si otra persona empujó después de la última actualización de este archivo, se avisa y no se pisa.
5. **El informe del revisor se guarda tal cual.** Se extrae del transcript del subagente sin reescribirlo: el texto más largo de la llamada `SubagentHandback` en el archivo `.output` del agente. Script de la sesión de Julian:
   ```python
   import json, sys
   src, dst = sys.argv[1], sys.argv[2]; mejor = ""
   def cadenas(x):
       if isinstance(x, str): yield x
       elif isinstance(x, dict):
           for v in x.values(): yield from cadenas(v)
       elif isinstance(x, list):
           for v in x: yield from cadenas(v)
   for linea in open(src, encoding="utf-8"):
       try: o = json.loads(linea)
       except ValueError: continue
       msg = o.get("message") if isinstance(o, dict) else None
       if not isinstance(msg, dict) or not isinstance(msg.get("content"), list): continue
       for c in msg["content"]:
           if isinstance(c, dict) and c.get("type") == "tool_use" and c.get("name") == "SubagentHandback":
               for s in cadenas(c.get("input")):
                   if len(s) > len(mejor): mejor = s
   open(dst, "w", encoding="utf-8").write(mejor.rstrip("\n") + "\n")
   ```
   Uso: `python3 -I extraer_handback.py <agente>.output <worktree>/revisiones/<tarea>/ronda-N.md`.
6. **Commit pequeño y frecuente en las ramas de tarea también**: el codificador hace push después de cada commit.

## Cómo se trabaja

- **El loop**: el orquestador es la sesión principal y sigue `loop/orquestador.md`. El `codificador` (`.claude/agents/codificador.md`) implementa. El `revisor` (`.claude/agents/revisor.md`, solo lectura) juzga. Si la ronda sale NO-VERDE, se arbitra y se redespacha al **mismo** codificador (con `SendMessage`, para que conserve el contexto), que responde en `ronda-N-respuesta.md`. Una tarea que llega a la ronda 4 se para y se lleva al equipo.
- **Cada tarea:** rama `m6r/<tarea>` con worktree en `../wt-m6r-<tarea>`; despacho con la ruta y el `git hash-object` de la tarea, la base y la ruta de la bitácora; PR a `main` (o a la rama de la que sale, si está apilada) cuando sale VERDE, asignado a Jacarrilloc.
- **El orquestador** solo escribe tareas, informes del revisor tal cual, `CANDIDATAS.md`, `aidlc-docs/audit.md`, `loop/README.md` y este archivo, en `orq/U05-U02-tareas`. En la sesión de Julian las tareas de U06 y U07 las redactó un ayudante (`general-purpose`) con U05-T09 y U02-T09 como plantilla, y el orquestador revisó sus decisiones antes de despachar.
- **Lecciones de esta sesión para los despachos** (ahorran rondas): en las copias de mutación que corren `FronterasModulosTest`, hacer `git init -q` (si no, `laDocumentacionGeneradaNoSeVersiona` cae siempre); el grep de nombres de `testcase` usa `[A-Za-z0-9]*()`; toda la evidencia sale del último commit que toca código; evidencia con fecha, sha y comando exacto, salida completa; toda salida pegada en un resumen es copia literal de su archivo; pruebas de rechazo que comparan el motivo exacto; un commit «rojo» de verdad no trae la implementación; en los scripts de shell, todo fallo de comando externo da `FALLA <caso>` y el `trap` registra recursos antes de crearlos.
- **Fusiones**: las hace un humano, o el orquestador cuando el humano lo pide por PR. En una pila, antes de fusionar cada PR se cambia su base a `main` (`gh pr edit N --base main`).

## Reglas del entorno

- **Máquina de Julian**: no hay `java` en el PATH: `JAVA_HOME=~/.gradle/jdks/eclipse_adoptium-21-aarch64-linux.2` (aarch64). Docker funciona sin arrancar `dockerd`. `gh` configurado con Jacarrilloc; `git push` funciona.
- **Máquina de David**: JDK 25; Java 21 lo provisiona Gradle.
- **Nada de `kubectl` ni de clúster real.** Solo el servidor simulado de Fabric8, con `kubeconfig`, `KUBECONFIG`, `HOME` y `user.home` falsos (`mktemp -d`), y `GRADLE_USER_HOME` real.
- No hay cliente `sqlite3`: se lee con `python3 -I -c "import sqlite3..."`.
- Nada de `git stash` para el «rojo primero»: copia (`git archive HEAD | tar -x -C $(mktemp -d)`) o worktree temporal.
- Pasar `</dev/null` a `git commit` y a `gh`.
- Criterios de Gradle como `:X:test --rerun :X:check` (C-69). En CA de diff acotado, la base es la del despacho.
