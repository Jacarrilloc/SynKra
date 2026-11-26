# U03-T07: Resumen de la unidad

**Unidad:** U03: `Plataforma del backend`
**Historias que implementa:** H-24
**Depende de:** U03-T06

---

## Alcance

**Dentro** (una línea, concreta):

> `aidlc-docs/construction/U03-plataforma-backend/code/summary.md`: qué construyó U03 (tareas T01 a T06 con su PR), los archivos de código y de prueba que dejó, cómo se construye, prueba y empaqueta, la salida real de `./gradlew :backend:check --rerun` y de la imagen, los conteos de pruebas leídos de los XML, y las candidatas que U03 deja para otras unidades.

**Fuera** (lo que el codificador debe rechazar aunque lo vea roto):

- Cualquier cambio de código, de pruebas, de `build.gradle.kts` o del `Dockerfile`. Si al correr los comandos algo falla, se registra en la bitácora y se para: el resumen no se escribe sobre una unidad rota.
- U03-T08 (OpenAPI, *Should*): no se hace. El resumen la lista como pendiente opcional.
- Marcar casillas en el plan de U03 o tocar `aidlc-state.md`: lo hace el orquestador al cerrar la unidad.
- Resolver candidatas (C-04, C-05, C-07, C-08, C-11, C-12, C-14): solo se listan con su unidad de destino.

---

## Archivos de contexto

- `aidlc-docs/construction/plans/U03-plataforma-backend-code-generation-plan.md`
- `tareas/U03-T01-proyecto-backend.md` a `tareas/U03-T06-imagen-backend.md`
- `bitacoras/U03-T0*.md` y `revisiones/U03-T0*/` (rondas y veredictos)
- `CANDIDATAS.md`
- `backend/` (para listar archivos; no se edita)

---

## Criterios de aceptación

- [ ] **CA-1**: el resumen trae la salida real de `:backend:check`, de esta ejecución
  ```bash
  ./gradlew :backend:check --rerun 2>&1 | tee /tmp/u03-check.txt | tail -5
  grep -c 'BUILD SUCCESSFUL' aidlc-docs/construction/U03-plataforma-backend/code/summary.md
  ```
  Esperado: `BUILD SUCCESSFUL` en la corrida y ≥ 1 en el resumen. El bloque pegado en el resumen es
  la cola de `/tmp/u03-check.txt` sin editar (con su línea de tiempo `BUILD SUCCESSFUL in ...`), y lleva
  al lado la fecha y hora UTC de la corrida.

- [ ] **CA-2**: los conteos de pruebas del resumen coinciden con los XML
  ```bash
  for f in backend/build/test-results/test/TEST-*.xml; do grep -o 'testsuite name="[^"]*" tests="[0-9]*" skipped="[0-9]*" failures="[0-9]*" errors="[0-9]*"' "$f"; done
  ```
  Esperado: el resumen tiene una tabla con una fila por clase de prueba y los mismos `tests`,
  `skipped`, `failures` y `errors` que esta salida, más una fila de total. Ninguna clase tiene
  `skipped` distinto de 0.

- [ ] **CA-3**: la imagen se comprueba de nuevo y su resultado va al resumen
  ```bash
  ./gradlew :backend:bootJar && docker build -t synkra-backend backend/ && docker inspect -f '{{.Config.User}}' synkra-backend
  ```
  Esperado: el usuario no vacío ni `root`/`0`, el mismo UID que dejó U03-T06; la salida va pegada en
  el resumen. (Si `docker info` no responde, se arranca el daemon como dice U03-T06.)

- [ ] **CA-4**: el resumen está completo y no inventa nada
  ```bash
  grep -nE '^## ' aidlc-docs/construction/U03-plataforma-backend/code/summary.md
  grep -oE 'U03-T0[1-8]' aidlc-docs/construction/U03-plataforma-backend/code/summary.md | sort -u
  grep -oE '#[0-9]+' aidlc-docs/construction/U03-plataforma-backend/code/summary.md | sort -u
  ```
  Esperado:
  - secciones al menos para: tareas, archivos, cómo construir y probar, evidencia, candidatas
    pendientes y lo que queda fuera;
  - aparecen U03-T01 a U03-T08 (T08 como pendiente opcional);
  - cada número de PR citado existe y está fusionado: se comprueba con `git log --oneline --merges main | grep 'pull request #N'`
    por cada `#N`, y la comprobación se pega en la bitácora;
  - cada ruta de archivo que nombra el resumen existe (se comprueba con `ls` y se pega en la bitácora).

- [ ] **CA-5**: no hay cambios fuera del resumen y la bitácora
  ```bash
  git diff --stat main...HEAD
  ```
  Esperado: solo `aidlc-docs/construction/U03-plataforma-backend/code/summary.md` y
  `bitacoras/U03-T07.md`.

---

## Plan de pruebas

- No hay pruebas nuevas: los criterios releen la evidencia (`check`, XML, imagen) y verifican que lo
  escrito coincide.
- **Rojo primero:** antes de escribir el resumen, CA-1 y CA-4 fallan (el archivo no existe); se
  registra en la bitácora.

---

## Notas

- El resumen va en español, sin nombres de personas: solo roles (el responsable, el codificador, el
  revisor, el orquestador).
- Número de rondas por tarea: se toma de `revisiones/U03-T0N/` y se pone en la tabla de tareas.
- Las candidatas se listan con su número, una línea y la unidad que la recibe (casi todas, U04).
- Es Markdown en `aidlc-docs/`: no lleva código de la aplicación.
