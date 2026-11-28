# Prompt para continuar SynKra (loop del módulo 6: U08 y U09)

> Pega este archivo completo al abrir una sesión nueva. Responde en español.
> Lo mantiene al día el orquestador después de cada hito (ver «Puntos de continuidad»).
> **2026-11-28 05:30 UTC: David retoma en su máquina** (`/home/casdav/Projects/Master/SynKra`, x86_64; orquestador en `../wt-orq-U04`). David confirmó (05:21 UTC) que la sesión de Julian está detenida. GitHub coincide con este archivo; lo que Julian no subió (los tres revisores en vuelo y los borradores de U08-C y U08-D) se perdió y se repite. El clúster `kind-synkra` de Julian está en su máquina: aquí se recrea. Las imágenes fijadas por digest no están en caché aquí: David dio permiso (05:23 UTC) y ya están descargadas; David subió inotify a 512/524288 (05:24 UTC). En vuelo: redactor de U08-C y U08-D, revisor U09-A ronda 3 y revisor U08-A ronda 1 (crea `kind-synkra` aquí y lo deja arriba). U08-A ronda 1 NO-VERDE (faltaban pegados de evidencia), ronda 2 **VERDE**, **fusionada en #111**. U08-B ronda 3 **VERDE**, **fusionada en #112** (`main` `05a709e`, 894 pruebas). U08-C y U08-D **escritas** (hashes `9665924` y `00ffee9`; U08-D corrige C-138 en Java). Codificador U08-C ronda 1 terminó (`168ff0c`, código en `a4924e6`; `--provenance=false --sbom=false` en `imagenes.sh`). **Revisor U08-C ronda 1 en vuelo**. El clúster `kind-synkra` está ARRIBA con SynKra instalado y el kubeconfig `../wt-m6r-U08-C/.kube/kind-synkra.yaml`. El clúster `kind-synkra` está ARRIBA en la máquina de David con el kubeconfig `../wt-m6r-U08-A/.kube/kind-synkra.yaml`. **U09-A**: ronda 3 NO-VERDE solo por el CA-1 atado a aarch64; David decidió (05:31 UTC) que todo corre en su máquina: CA-1 enmendado; ronda 4 corta **VERDE**; **fusionada en #110**.
> Última actualización anterior: 2026-11-28 05:20 UTC (00:20 en Bogotá), sesión **local** de Julian (`/home/julian/Escritorio/Proyectos/SynKra`; la rama del orquestador está en el checkout principal; **comprobar `git branch --show-current` antes de cada commit**: una vez apareció cambiado a `main`).
> **Punto de retomar:** el commit de este archivo en `orq/U05-U02-tareas` (`git log -1 --format=%h -- loop/CONTINUAR.md`).

---

## Resumen en una línea

U01 a U07 **terminadas y fusionadas en `main`** (`8306ee7`, 894 pruebas en verde). U10 y U11 cortadas (R9). **El mínimo que falta es U08** (instalación en `kind`); U09 (front) va en paralelo porque solo necesita la API. Julian pidió (04:54 UTC): **al terminar U08, fusionar a `main` sus PRs y la colección de Postman (#109) y darle las instrucciones para correrlo**; las fusiones de U08 las hace el orquestador.

## Lo que se hizo en la sesión de Julian (28 de noviembre, desde las 04:35 UTC)

- Retomó desde el punto de David (`6994ff1`/`ab090d0`). U08 corre en la **máquina de Julian**: tiene `kind` v0.33 y `kubectl` v1.37 (aarch64). Julian subió `fs.inotify.max_user_instances` a 512 y `max_user_watches` a 524288 (05:09 UTC; vuelve al valor anterior si reinicia: entonces repetir `sudo sysctl fs.inotify.max_user_instances=512 fs.inotify.max_user_watches=524288`).
- **Colección de Postman** del backend en el **PR #109** (`m6r/postman`, abierto): colección v2.1, entorno local y de `kind`, README; 112 tests en verde contra el backend real sin clúster.
- Tareas escritas (agrupadas, en `tareas/`): U08-A, U08-B, U09-A. En redacción: U08-C y U08-D.
- `CANDIDATAS.md` llega hasta **C-138**. La siguiente libre es **C-139**.

## Plan para acelerar (aprobado por Julian el 2026-11-28 a las 00:19 UTC, sigue vigente)

- Se cortan U11 y U10 (*Should*, R9). La demo se hace con `curl` si U09 no llega. Mínimo de la North Star: U01 a U08.
- Hasta **3 tareas en paralelo**; tareas del plan **agrupadas**; **tope de dos rondas** cuando solo quedan AMARILLOS (ROJO y NARANJA nunca se bajan); una tarea en la ronda 4 se para y se lleva al equipo; revisor `sonnet` solo en resúmenes. Ante decisiones, la opción recomendada (autorización permanente de Julian).

## Estado al corte (2026-11-28 05:20 UTC)

### PRs abiertos

| PR | Rama → base | Contenido |
|---|---|---|
| #109 | `m6r/postman` → `main` | Colección de Postman. Se fusiona junto con U08 |

### Tareas en curso (todas salen de `main` `8306ee7`)

| Tarea | Worktree / rama | Estado | Qué hacer al retomar |
|---|---|---|---|
| U08-A clúster, guardia, namespaces, RBAC (T01 a T04) | `../wt-m6r-U08-A`, `m6r/U08-A` | Codificador ronda 1 completo en `4799a82` (código en `3a738aa`), 10 de 10 CA. **`kind-synkra` y `kind-registry` están ARRIBA** con la base aplicada; kubeconfig del proyecto en `../wt-m6r-U08-A/.kube/kind-synkra.yaml`. **Revisor ronda 1 en vuelo** | Si no hay `revisiones/U08-A/ronda-1.md` en la rama: repetir el revisor (base `8306ee7`, HEAD `4799a82`) |
| U08-B backend, Ingress con TLS, imágenes (T05 a T07) | `../wt-m6r-U08-B`, `m6r/U08-B` | Ronda 1 NO-VERDE (red sin `--pull=never`), ronda 2 NO-VERDE (el chequeo de versiones no podía fallar). Codificador ronda 3 en `a2b9abd` (código en `63c85b9`). **Revisor ronda 3 en vuelo** | Si no hay `revisiones/U08-B/ronda-3.md`: repetir el revisor ronda 3. Si sale NO-VERDE, la tarea llega a la ronda 4: parar y llevarla al equipo |
| U09-A proyecto Blazor, cliente de la API, Vista general (T01+T02+T06) | `../wt-m6r-U09-A`, `m6r/U09-A` | Ronda 1 NO-VERDE (5 comportamientos sin prueba), ronda 2 NO-VERDE solo porque el CA-2 escrito no se cumplía tras el arbitraje de F-03: **el orquestador enmendó el CA-2** (hash nuevo `d5a5ff4`, ver `audit.md`). Código sin cambios (`c41f004`, HEAD `bfbe91b`). **Revisor ronda 3 en vuelo** | Si no hay `revisiones/U09-A/ronda-3.md`: repetir el revisor ronda 3 con la tarea enmendada |
| U08-C y U08-D (redacción) | `tareas/` de `orq/U05-U02-tareas` | Un ayudante (`general-purpose`) las está redactando | Si no existen `tareas/U08-C-install-limite.md` y `tareas/U08-D-e2e-api.md`, redactarlas con U08-A y U08-B como plantilla y contrato |

## Lo que sigue (en orden)

1. Cerrar U08-A, U08-B y U09-A (guardar cada informe tal cual, PR a `main` con la VERDE, asignado a Jacarrilloc). Las de U08 las **fusiona el orquestador** (pedido de Julian); U09-A también puede fusionarse si sale VERDE.
2. **U08-C** (`make install` y `make verificar-limite`, T08+T09): sale de la **unión** de `m6r/U08-A` y `m6r/U08-B` (o de `main` tras fusionarlas). Usa el clúster `kind-synkra` que dejó U08-A.
3. **U08-D** (`scripts/e2e-api.sh` de CU1 a CU5 con `curl --resolve synkra.local:443:127.0.0.1` y formato único de error, T10+T11): sale de U08-C.
4. **U08-E** (resumen de U08, T12, revisor `sonnet`). Después: fusionar U08 y el #109 a `main`, cierre de U08 (plan marcado, `aidlc-state.md`, `audit.md`) y **mensaje a Julian con las instrucciones para correr SynKra en `kind`** (`make install`, `curl --resolve`, Postman con `baseUrlKind`, el `sysctl` de inotify si hay otros clústeres `kind`, C-130).
5. U09 en paralelo con U08-C a U08-E. Reparto propuesto: **U09-B** = T03 (Catálogo); **U09-C** = T04+T07+T08 (Desplegar, imagen, manifiesto; el arranque de F-04 de U09-A va aquí); **U09-D** = T05+T09 (Detalle, resumen). Aplicar C-133 (escape con `/` y `?`) y C-134 (comprobar uso, no texto de la clave).
6. Pendiente de decidir con el equipo: pulido de U04, de U01/U02, de U05, de imágenes en U08 y de U06/U07 (ver los resúmenes y `CANDIDATAS.md`).

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
- **Lecciones de las sesiones para los despachos** (ahorran rondas): cada regla de las decisiones necesita una prueba que caiga si se rompe (la causa más común de NO-VERDE en U06 y U07); no drenar peticiones del simulador entre la acción y su afirmación; no acomodar código ni comentarios a un grep; un resumen cita cada frase con `grep -F` contra su fuente; en las copias de mutación que corren `FronterasModulosTest`, hacer `git init -q` (si no, `laDocumentacionGeneradaNoSeVersiona` cae siempre); el grep de nombres de `testcase` usa `[A-Za-z0-9]*()`; toda la evidencia sale del último commit que toca código; evidencia con fecha, sha y comando exacto, salida completa; toda salida pegada en un resumen es copia literal de su archivo; pruebas de rechazo que comparan el motivo exacto; un commit «rojo» de verdad no trae la implementación; en los scripts de shell, todo fallo de comando externo da `FALLA <caso>` y el `trap` registra recursos antes de crearlos.
- **Fusiones**: las hace un humano, o el orquestador cuando el humano lo pide por PR. En una pila, antes de fusionar cada PR se cambia su base a `main` (`gh pr edit N --base main`).

## Reglas del entorno

- **Máquina de Julian**: `kind` v0.33, `kubectl` v1.37, `make`, `jq`; sin SDK de .NET (U09 usa `scripts/dotnet-docker.sh` con la imagen `mcr.microsoft.com/dotnet/sdk:10.0.404`). No hay `java` en el PATH: `JAVA_HOME=~/.gradle/jdks/eclipse_adoptium-21-aarch64-linux.2` (aarch64). Docker funciona sin arrancar `dockerd`. `gh` configurado con Jacarrilloc; `git push` funciona.
- **Máquina de David**: JDK 25 en el PATH; Java 21 lo provisiona Gradle en `~/.gradle/jdks/eclipse_adoptium-21-amd64-linux.2` (x86_64). `git push` por HTTPS con `credential.https://github.com.helper '!gh auth git-credential'` en la config del repo. `kind`, `kubectl` y `docker` disponibles.
- **Nada de `kubectl` ni de clúster real salvo `kind-synkra` en U08** (AUTONOMIA-01): solo con el kubeconfig del proyecto (`$PWD/.kube/kind-synkra.yaml`), nunca `~/.kube/config`; los clústeres `kind` de Julian (`gitops2`, `m08-gitops1`) no se tocan ni se listan. Fuera de U08, solo el servidor simulado de Fabric8, con `kubeconfig`, `KUBECONFIG`, `HOME` y `user.home` falsos (`mktemp -d`), y `GRADLE_USER_HOME` real.
- No hay cliente `sqlite3`: se lee con `python3 -I -c "import sqlite3..."`.
- Nada de `git stash` para el «rojo primero»: copia (`git archive HEAD | tar -x -C $(mktemp -d)`) o worktree temporal.
- Pasar `</dev/null` a `git commit` y a `gh`.
- Criterios de Gradle como `:X:test --rerun :X:check` (C-69). En CA de diff acotado, la base es la del despacho.
