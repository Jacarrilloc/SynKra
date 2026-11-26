# Ronda 2 — U03-T07

VEREDICTO: VERDE

Corrí yo mismo los cinco criterios y pasan los cinco. Las cinco correcciones de la ronda 1 (F-01 a F-05) están aplicadas y las comprobé contra las fuentes. No encontré regresiones. Quedan dos AMARILLO que no bloquean.

Comprobaciones previas:
- Hash de la tarea: `a30a428d…`, coincide.
- HEAD: 260d340. `main` y `origin/main` están los dos en b4ec449.
- Esta ronda cambió 4 archivos: el resumen, la bitácora y los dos archivos de `revisiones/U03-T07/`.

## Criterios de aceptación, verificados por mí
Todos los comandos se corrieron desde `/home/user/wt-m6r-U03-T07`.

| # | Criterio | Comando que corrí | Resultado |
|---|---|---|---|
| 1 | Salida real de `:backend:check` | `./gradlew :backend:test --rerun :backend:check 2>&1 \| tee <scratch>/u03t07r2/u03-check.txt \| tail -5` (de 17:31:01Z a 17:31:11Z), más `grep -c 'BUILD SUCCESSFUL' summary.md` | pasa: `BUILD SUCCESSFUL in 9s`, `1 executed, 3 up-to-date` y el grep da 1. Mi `tail -5` es idéntico byte a byte a las líneas 68-72 del resumen. La cola de `u03-check2.txt` del codificador (modificado a las 17:29:26.59Z) también es idéntica, y esa hora coincide con «17:29 UTC» del resumen y con «fin 17:29:26Z» de la bitácora. «1 executed» se explica porque `compileJava` y `compileTestJava` salen UP-TO-DATE y solo `test` se ejecuta. Es la cola de una corrida real, sin editar |
| 2 | Conteos frente a los XML | el bucle `grep -o 'testsuite name=…'` del criterio, sobre 8 XML con fecha de mi corrida (17:31:02Z a 17:31:08Z) | pasa: 3/2/3/6/13/4/1/3, total 35, `skipped=0` en las 8 clases, igual que la tabla. Las suites tardan entre 0,006 s y 3,047 s, así que ninguna se saltó |
| 3 | Imagen sin root | `./gradlew :backend:bootJar && docker build -t synkra-backend-rev2 backend/ && docker inspect -f '{{.Config.User}}' synkra-backend-rev2` | pasa: `10001:10001`. `docker run --rm --entrypoint id` da `uid=10001(synkra) gid=10001(synkra)`. Borré la etiqueta: `docker image ls synkra-backend-rev2 -q` da 0 |
| 4 | Completo y sin inventar | los 3 greps del criterio; `git log --oneline --merges origin/main \| grep "pull request #N "` para cada N; `xargs ls` de las rutas | pasa: hay 6 secciones, aparecen U03-T01 a U03-T08, y los 7 PR (#10 #12 #14 #16 #19 #21 #23) están fusionados. Las 31 rutas del `ls` pegado existen (`rc=0`) |
| 5 | Diff acotado | `git diff --stat main...HEAD` | pasa en su intención: 4 archivos, el resumen, la bitácora y los dos archivos de `revisiones/U03-T07/`. Ver F-01 |

## Contraste de lo que cambió esta ronda
- **Tabla de candidatas.** Apliqué la regla de la bitácora («origen en una tarea de U03») sobre la columna «Salió de» de `CANDIDATAS.md` con `awk -F'|' '$4 ~ /U03/'`. Salen C-03, 04, 05, 07, 08, 11, 12, 14, 17 y 18, el mismo conjunto del resumen.
  - Las 9 pendientes coinciden en texto y en la unidad de destino que da la columna «Decisión». Por ejemplo: C-03 va a «una tarea que pueda tocar la raíz», C-08 a U04-T04, C-14 a «cuando U04 traiga sus propios controladores» y C-17 a U08 con el `securityContext`.
  - C-18 figura como aplicada («corregido ya en U01-T08 y U03-T07»), igual que en `CANDIDATAS.md`.
  - No faltan candidatas ni sobran. Como pediste, no se exige C-21 ni C-22.
- **Frase del PR #16.** Es cierta. Lo comprobé por la API REST:
  - #10, #12 y #14 son `m6r/U03-T0x -> modulo-6-rehecho`, con `merged=true`.
  - #16 es `claude/project-thread-ti5y5k -> main`, con `merged=true`.

  Lo confirmé también en git:
  - Los tres merges están en el `--first-parent` de `origin/modulo-6-rehecho`.
  - La punta de esa rama, 7e7281d, es ancestro de `3eb4dca^2`.
  - `modulo-6-rehecho` está entera dentro de `origin/main`.
- **`ls` pegado.** Las 31 rutas de la bitácora coinciden una a una con `git ls-files backend` más `CANDIDATAS.md` y `settings.gradle.kts` (`diff` vacío), y todas existen.
- **`settings.gradle.kts`.** La línea nueva de Archivos es exacta: `git diff 2abd4d6^1 2abd4d6` da solo `+include("backend")`.
- **Sin regresiones.** El diff `3048df3..260d340` del resumen se limita a cuatro cosas:
  - la frase del PR #16;
  - `settings.gradle.kts`;
  - el bloque de CA-1 y su hora;
  - las candidatas.

  Las rondas por tarea (2/4/2/2/3/2) siguen coincidiendo con `revisiones/`.

## Hallazgos
### F-01 · AMARILLO · CA-5 / `bitacoras/U03-T07.md` (sección «CA-5 y F-05») · el diff tiene 4 archivos y la bitácora no dice por qué
CA-5 espera solo el resumen y la bitácora. `git diff --stat main...HEAD` da además `revisiones/U03-T07/ronda-1.md` y `ronda-1-respuesta.md`.

Esos dos archivos son del propio bucle. Todos los PR fusionados de la unidad los traen (#19, #21 y #23 llevan sus `revisiones/U03-T0N/`), y una revisión anterior ya los dio por buenos (`revisiones/U01-T07/ronda-2.md:7`). Por eso no lo cuento como desborde.

Lo que falla es la evidencia: la bitácora pega los 4 archivos sin decir que no coinciden con lo esperado en CA-5. El defecto de fondo está en la especificación, la misma clase que C-21: la plantilla debería decir «más `revisiones/<tarea>/`».

### F-02 · AMARILLO · CA-4 / `bitacoras/U03-T07.md` (sección F-01) · el `ls` deja fuera tres rutas que el resumen nombra
El resumen nombra tres rutas que el `ls` no cubre:
- `aidlc-state.md`, que la bitácora excluye a propósito;
- `backend/build/test-results/test/`;
- `components.puml`, en C-12.

Las tres existen. Las encontré en `aidlc-docs/aidlc-state.md`, en el directorio de XML de mi corrida y en `backend/build/spring-modulith-docs/components.puml`. Así que no se inventa nada, pero el `ls` pegado no cubre todas las rutas que menciona el resumen. Las dos últimas son artefactos de build, no archivos versionados.

## Tareas candidatas (defectos reales fuera de alcance)
- Plantilla de tareas: el criterio de «diff acotado» debería contar con los archivos de `revisiones/<tarea>/` que el bucle añade a la rama. Es la misma clase que C-21; puede ir dentro de C-21.

## Estado del worktree
`git status --short` sale vacío al terminar. Solo hay ignorados (`.gradle/`, `build/`, `backend/build/`). La etiqueta `synkra-backend-rev2` está borrada. La imagen `synkra-backend` del codificador no la toqué.

## Rutas de transcripciones largas
Están en `/tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/u03t07r2/`:
- `u03-check.txt`
- `bootjar.txt`
- `docker-build.txt`
- `bloque.txt`
- `rutas-bitacora.txt`
- `esperado.txt`

```
VEREDICTO: VERDE
AMARILLO|CA-5 / bitacoras/U03-T07.md|el diff tiene 4 archivos (más revisiones/U03-T07/) y la bitácora no declara la diferencia con CA-5
AMARILLO|CA-4 / bitacoras/U03-T07.md|el ls pegado no cubre aidlc-state.md, backend/build/test-results/test/ ni components.puml (existen)
INFORME: revisiones/U03-T07/ronda-2.md
```
