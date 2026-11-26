# U01-T14: Resumen de la unidad

**Unidad:** U01: `Verificador y suite`
**Historias que implementa:** H-10
**Depende de:** U01-T13

---

## Alcance

**Dentro** (una línea, concreta):

> `aidlc-docs/construction/U01-verificador/code/summary.md` explica qué expone U01 y cómo lo usan U02, U04 y U07. Incluye las tareas T01 a T14 con su PR y sus rondas, los archivos que dejó, cómo se construye y prueba, la salida real de `:verificador:check` y del script de control, los conteos de los XML, y todas las candidatas cuyo origen sea una tarea de U01.

**Fuera** (lo que el codificador debe rechazar aunque lo vea roto):

- Cualquier cambio de código, de pruebas, de `build.gradle.kts` o de scripts. Si algo falla al correr los comandos, se registra en la bitácora y se para: el resumen no se escribe sobre una unidad rota.
- Resolver candidatas: solo se listan, con su estado y su destino. C-25 se lista como pendiente, con la recomendación del orquestador (ver Notas).
- Marcar casillas en el plan de U01 o tocar `aidlc-state.md`: lo hace el orquestador al cerrar la unidad.

---

## Archivos de contexto

- `aidlc-docs/construction/plans/U01-verificador-code-generation-plan.md`
- `aidlc-docs/construction/U01-verificador/functional-design/` (los tres archivos)
- `tareas/U01-T01-*.md` a `tareas/U01-T13-*.md`
- `bitacoras/U01-T*.md` y `revisiones/U01-T*/`
- `CANDIDATAS.md`
- `aidlc-docs/construction/U03-plataforma-backend/code/summary.md` (como modelo de forma)
- `verificador/` y `scripts/control-suite.sh`: se listan, no se editan

---

## Criterios de aceptación

- [ ] **CA-1**: el resumen trae la salida real de `check`, de esta ejecución
  ```bash
  ./gradlew :verificador:test --rerun :verificador:check --console=plain 2>&1 | tee /tmp/u01-check.txt | tail -6
  grep -c 'BUILD SUCCESSFUL' aidlc-docs/construction/U01-verificador/code/summary.md
  ```
  Esperado: `BUILD SUCCESSFUL` en la corrida y ≥ 1 en el resumen. El bloque pegado es la cola de
  `/tmp/u01-check.txt` sin editar. Incluye la línea de `verificarSuite` y la de
  `BUILD SUCCESSFUL in ...`, y al lado va la fecha y hora UTC.

- [ ] **CA-2**: los conteos de pruebas coinciden con los XML
  ```bash
  for f in verificador/build/test-results/test/TEST-*.xml; do grep -o 'testsuite name="[^"]*" tests="[0-9]*" skipped="[0-9]*" failures="[0-9]*" errors="[0-9]*"' "$f"; done
  ```
  Esperado: una tabla con una fila por clase, los mismos números y una fila de total. Ninguna clase con
  `skipped` distinto de 0. Para las clases de jqwik, la tabla dice también `tries` por propiedad.

- [ ] **CA-3**: el control de la suite se corre de nuevo y va al resumen
  ```bash
  bash scripts/control-suite.sh; echo "rc=$?"
  ```
  Esperado: `rc=0` y los tres escenarios; la salida va pegada en el resumen.

- [ ] **CA-4**: el resumen está completo y no inventa nada
  ```bash
  grep -nE '^## ' aidlc-docs/construction/U01-verificador/code/summary.md
  grep -oE 'U01-T[0-9]{2}' aidlc-docs/construction/U01-verificador/code/summary.md | sort -u
  grep -oE '#[0-9]+' aidlc-docs/construction/U01-verificador/code/summary.md | sort -u
  awk -F'|' '$4 ~ /U01/ {print $2}' CANDIDATAS.md
  ```
  Esperado:
  - Secciones, al menos: qué expone (API pública), quién lo usa y cómo, tareas, archivos, cómo
    construir y probar, evidencia, candidatas y lo que queda fuera.
  - En «qué expone» y «quién lo usa» van las firmas públicas reales: `Version`, `RangoVersion`,
    `Descriptor`, `LectorDescriptor.leer`/`desdeYaml`, `Seleccion`, `Veredicto`, `Catalogo`,
    `Verificador.verificar`/`alternativas`, y `SuiteCombinaciones` y `CatalogoEnMemoria` en
    `testFixtures`. Se comprueban con `grep` en el código, y el resultado se pega en la bitácora.
  - Aparecen U01-T01 a U01-T14. El número de rondas de cada tarea se toma de `revisiones/U01-T*/`.
  - Cada `#N` citado está fusionado: `git log --oneline --merges origin/main | grep 'pull request #N '`,
    pegado en la bitácora.
  - Cada ruta de archivo que nombra el resumen existe (`ls`, pegado en la bitácora). Si nombra
    artefactos de build, lo dice.
  - La lista de candidatas del resumen es **exactamente** la que da el `awk` (las que salieron de U01),
    con su estado y destino. No se elige a mano (C-21).

- [ ] **CA-5**: no hay cambios fuera del resumen
  ```bash
  git diff --stat origin/main...HEAD
  ```
  Esperado: `aidlc-docs/construction/U01-verificador/code/summary.md`, la bitácora y
  `revisiones/U01-T14/`. Nada más.

---

## Plan de pruebas

- No hay pruebas nuevas: los criterios vuelven a correr la evidencia y comprueban que lo escrito
  coincide.
- **Rojo primero:** antes de escribir el resumen, CA-1 y CA-4 fallan porque el archivo no existe. Se
  registra en la bitácora.

---

## Notas

- En español, sin nombres de personas: solo roles (el responsable, el codificador, el revisor, el
  orquestador).
- Recomendación del orquestador para C-25, que el resumen pone tal cual como pendiente: decidir en U11
  (CI) si se fija `jqwik.failures.after.default = RANDOM_SEED` en `junit-platform.properties`, junto
  con C-02.
- Es Markdown en `aidlc-docs/`: no lleva código de la aplicación.
