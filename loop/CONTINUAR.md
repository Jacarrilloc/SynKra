# Prompt para continuar SynKra (loop del módulo 6: U08 en adelante)

> Pega este archivo completo al abrir una sesión nueva. Responde en español.
> Lo mantiene al día el orquestador después de cada hito (ver «Puntos de continuidad»).
> Última actualización: 2026-11-28 04:35 UTC (23:35 en Bogotá), sesión **local** de David (`/home/casdav/Projects/Master/SynKra`, worktree del orquestador `../wt-orq-U04`). **David pausó (04:31 UTC) para que Julian continúe.** No hay ningún agente corriendo.
> **Punto de retomar:** el commit de este archivo en `orq/U05-U02-tareas` (el sha lo da `git log -1 --format=%h -- loop/CONTINUAR.md`).

---

## Resumen en una línea

U01 a U07 están **terminadas y fusionadas en `main`** (`8306ee7`, 894 pruebas en verde, `./gradlew test --rerun check` con `salida=0`). U10 y U11 están cortadas (R9). **Lo que sigue es U08** (instalación y límite en `kind-synkra`); sus tareas **todavía no están escritas**. U09 (front) solo si sobra tiempo.

## Lo que se hizo en la sesión de David (28 de noviembre, 00:44 a 04:31 UTC)

- **U06 terminada** (registro y vista general): U06-A (#97, 2 rondas), U06-B (#99, 1), U06-C (#101, 2), U06-D resumen (#103, 1, revisor `sonnet`). Resumen en `aidlc-docs/construction/U06-registro/code/summary.md`.
- **U07 terminada** (despliegue y reversión): U07-A (#96, 1), U07-B (#98, 2), U07-C (#100, 2), U07-D (#104, 2), U07-E (#105, 2), U07-F (#106, 3), U07-G fronteras y resumen (#108, 2). Resumen en `aidlc-docs/construction/U07-despliegue/code/summary.md`.
- **Rama del orquestador** fusionada en #95, #102 y #107. El cierre de U07 (plan marcado, `aidlc-state.md`, auditoría, C-128) está en `orq/U05-U02-tareas` **sin fusionar todavía**: abrir PR a `main` con lo que se acumule.
- Plan de U03: T01 a T07 marcadas (estaban fusionadas desde el PR #26); U03-T08 (OpenAPI, *Should*) queda pendiente opcional.
- Decisiones de David, todas en `aidlc-docs/audit.md`: fusionar por tandas los PRs VERDES sin preguntar (01:25 UTC); en U07-D, registrar `aplicando` antes de aplicar (desviación de R-D9, 01:30 UTC); U07-F en paralelo con U07-E (03:07 UTC); U08 se corre en la máquina de David, que tiene `kind`, al cerrar U07 (01:29 UTC).
- `CANDIDATAS.md` llega hasta **C-130** (en `orq`). La siguiente libre es **C-131**.

## Plan para acelerar (aprobado por Julian el 2026-11-28 a las 00:19 UTC, sigue vigente)

- Se cortan U11 y U10 (*Should*, R9). La demo se hace con `curl` si U09 no llega. Mínimo de la North Star: U01 a U08.
- Hasta **3 tareas en paralelo**; tareas del plan **agrupadas** en un solo despacho; **tope de dos rondas** cuando solo quedan AMARILLOS (los ROJO y NARANJA nunca se bajan); revisor `sonnet` solo en tareas de documentación (resúmenes).

## Estado al corte (2026-11-28 04:35 UTC)

### PRs

#95 a #108 **fusionados**. Abierto: #109 (`m6r/postman` → `main`, colección de Postman del backend, pedida por Julian a las 04:54 UTC; se fusiona junto con U08). El #6 es antiguo y no se toca.

### En vuelo

**Sesión local de Julian (04:55 UTC):** tareas `U08-A-cluster-guardia-rbac.md` (`eedd724`) y `U08-B-backend-ingress-imagenes.md` (`73f5a26`) escritas (commit `c454107`). **U08-A desbloqueada (05:10 UTC; Julian subió inotify a 512): codificador redespachado para los CA de clúster.** Antes, BLOQUEADA: código escrito en `c3fcff0`, pero `kind create cluster` falla en la máquina de Julian por `fs.inotify.max_user_instances=128` («Too many open files»; lo consumen sus otros clústeres). Hace falta que Julian corra `sudo sysctl fs.inotify.max_user_instances=512` (o pare un clúster suyo); después, redespachar al codificador de U08-A para correr los CA de clúster (`bitacoras/U08-A-evidencia/caN.sh`). **En vuelo:** codificador ronda 1 de U08-A (terminado, bloqueado) y de U08-B (`../wt-m6r-U08-B`, `m6r/U08-B`; ronda 1 NO-VERDE: F-01 NARANJA, `verificar-imagenes.sh` sale a la red sin `--pull=never`; codificador ronda 2 hecho en `8a6b076`, **revisor ronda 2 en vuelo**, base `8306ee7`), las dos desde `8306ee7`; U09-A (`../wt-m6r-U09-A`, `m6r/U09-A`, tarea `16d8ea7`; ronda 1 NO-VERDE: F-01 NARANJA, cinco comportamientos de las decisiones 7 y 12 sin prueba; **codificador ronda 2 en vuelo**, base `8306ee7`; commit `f7a0410`; U09-T01+T02+T06; reparto propuesto para el resto: U09-B = T03, U09-C = T04+T07+T08, U09-D = T05+T09; el front corre en paralelo con U08 y se construye con la imagen `mcr.microsoft.com/dotnet/sdk`, porque no hay .NET instalado). Si una rama no tiene commits del codificador, redespacharlo; si los tiene, revisor ronda 1. U08 corre en la máquina de Julian: `kind` v0.33, `kubectl`, puertos 80, 443 y 5001 libres; sus clústeres `gitops2` y `m08-gitops1` no se tocan.

## Lo que sigue (en orden)

1. `git fetch`; comprobar que `main` sigue en `8306ee7` o después y que `orq/U05-U02-tareas` tiene el cierre de U07.
2. **U08 en la máquina de David** (en la de Julian no hay `kind`). Agrupación propuesta, por escribir en `tareas/`:
   - **U08-A** = T01+T02+T03+T04: clúster `kind` `synkra` y registro `kind-registry` en `localhost:5001`, guardia de contexto del `Makefile`, namespaces, RBAC sin ClusterRole.
   - **U08-B** = T05+T06+T07: PVC y Deployment del backend, overlay `kind` con Ingress `synkra.local` y TLS, imágenes multi-stage sin root (ver C-95, C-98, C-101, C-103). En paralelo con U08-A, con reparto de archivos fijado para no chocar (p. ej. el `Makefile` solo para U08-A).
   - **U08-C** = T08+T09: `make install` y `make verificar-limite` (sobre la unión de A y B).
   - **U08-D** = T10+T11: `scripts/e2e-api.sh` (CU1 a CU5) y formato único de error.
   - **U08-E** = T12: resumen (revisor `sonnet`).
   - Reglas que las tareas deben fijar: el único clúster es `kind-synkra` (AUTONOMIA-01); **kubeconfig propio del proyecto** (`KUBECONFIG=$PWD/.kube/kind-synkra.yaml`, ignorado por git; nunca `~/.kube/config`); la guardia comprueba el contexto de ese archivo; sin `sudo` ni `/etc/hosts` (`curl --resolve`); recursos en el `trap` antes de crearlos.
   - Herramientas en la máquina de David: `kind` v0.33, `kubectl`, `docker`, `make`, `jq`, `curl`, `python3` con PyYAML 6; imágenes `kindest/node:v1.37.0` y `registry` en caché. **No hay `yq` ni `kubeconform`**: usar `kubectl kustomize | python3 -I` y `kubectl apply --dry-run=server` (desviación del plan) en vez de descargarlos.
   - El plan de U08 dice que la Parte 2 lleva **aviso previo a Julian**: con Julian al mando, queda cumplido.
3. Al cerrar U08: plan marcado, línea de Code Generation Parte 2 en `aidlc-state.md`, `audit.md`.
4. U09 (front) solo si sobra tiempo; si no, la demo es con `curl`.
5. Pendiente de decidir con el equipo: pulido de U04 (C-57, C-60, C-61, C-63, C-65 a C-67); de U01/U02 (C-77, C-78, C-80, C-84 a C-86, C-88, C-89, C-96, C-99, C-100, C-105); de U05 (C-93, C-94, C-97, C-102, C-104); imágenes en U08 (C-95, C-98, C-101, C-103); de U06 y U07 (C-90, C-106 a C-128 abiertas, ver los resúmenes de U06 y U07).

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
- **Lecciones de las sesiones para los despachos** (ahorran rondas): cada regla de las decisiones necesita una prueba que caiga si se rompe (la causa más común de NO-VERDE en U06 y U07); no drenar peticiones del simulador entre la acción y su afirmación; no acomodar código ni comentarios a un grep; un resumen cita cada frase con `grep -F` contra su fuente; en las copias de mutación que corren `FronterasModulosTest`, hacer `git init -q` (si no, `laDocumentacionGeneradaNoSeVersiona` cae siempre); el grep de nombres de `testcase` usa `[A-Za-z0-9]*()`; toda la evidencia sale del último commit que toca código; evidencia con fecha, sha y comando exacto, salida completa; toda salida pegada en un resumen es copia literal de su archivo; pruebas de rechazo que comparan el motivo exacto; un commit «rojo» de verdad no trae la implementación; en los scripts de shell, todo fallo de comando externo da `FALLA <caso>` y el `trap` registra recursos antes de crearlos.
- **Fusiones**: las hace un humano, o el orquestador cuando el humano lo pide por PR. En una pila, antes de fusionar cada PR se cambia su base a `main` (`gh pr edit N --base main`).

## Reglas del entorno

- **Máquina de Julian**: no hay `java` en el PATH: `JAVA_HOME=~/.gradle/jdks/eclipse_adoptium-21-aarch64-linux.2` (aarch64). Docker funciona sin arrancar `dockerd`. `gh` configurado con Jacarrilloc; `git push` funciona.
- **Máquina de David**: JDK 25 en el PATH; Java 21 lo provisiona Gradle en `~/.gradle/jdks/eclipse_adoptium-21-amd64-linux.2` (x86_64). `git push` por HTTPS con `credential.https://github.com.helper '!gh auth git-credential'` en la config del repo. `kind`, `kubectl` y `docker` disponibles.
- **Nada de `kubectl` ni de clúster real.** Solo el servidor simulado de Fabric8, con `kubeconfig`, `KUBECONFIG`, `HOME` y `user.home` falsos (`mktemp -d`), y `GRADLE_USER_HOME` real.
- No hay cliente `sqlite3`: se lee con `python3 -I -c "import sqlite3..."`.
- Nada de `git stash` para el «rojo primero»: copia (`git archive HEAD | tar -x -C $(mktemp -d)`) o worktree temporal.
- Pasar `</dev/null` a `git commit` y a `gh`.
- Criterios de Gradle como `:X:test --rerun :X:check` (C-69). En CA de diff acotado, la base es la del despacho.
