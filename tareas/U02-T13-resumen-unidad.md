# U02-T13: resumen de la unidad U02

**Unidad:** U02: `Core, contrato y plugins de país`
**Historias que implementa:** H-21
**Depende de:** U02-T01 a U02-T12

---

## Alcance

**Dentro** (una línea, concreta):

> El resumen `aidlc-docs/construction/U02-core-plugins/code/summary.md` con lo que entregó la unidad, la salida real de `./gradlew :core:check :plugins:kyc-co:check :plugins:kyc-pe:check` y de `core/smoke.sh`, la imagen del core, la trazabilidad de historias y las candidatas abiertas.

**Fuera** (lo que el codificador debe rechazar aunque lo vea roto):

- Cualquier cambio de código, pruebas o scripts. Si un comando falla, se para y se reporta.
- Marcar el plan o tocar `aidlc-docs/aidlc-state.md`: lo hace el orquestador al cerrar la unidad.

---

## Archivos de contexto

- `aidlc-docs/construction/plans/U02-core-plugins-code-generation-plan.md`
- `aidlc-docs/construction/U04-catalogo/code/summary.md` (formato de un resumen aceptado)
- `bitacoras/U02-T*.md`, `revisiones/U02-T*/` y `CANDIDATAS.md`

---

## Decisiones del orquestador

1. **Secciones.** Qué expone la unidad (`plugin-api`, el core con su arranque, readiness y endpoints, los JAR `kyc-co 1.0.0`, `kyc-pe 1.3.0` y `kyc-pe 2.0.0`, la imagen); cómo se prueba (suite de U01 contra el arranque, `docker run` local, nada contra un clúster); salidas reales; trazabilidad H-10, H-21, H-22, H-23 y H-28 contra tareas; rondas por tarea; candidatas abiertas (C-74, C-77, C-78, C-80, C-84, C-85 y las que queden).
2. **Salidas reales.** Tal cual, con fecha, sha y comando, versionadas también en `aidlc-docs/construction/U02-core-plugins/code/evidencia/`.

---

## Criterios de aceptación

- [ ] **CA-1**: salidas reales y frescas
  ```bash
  ./gradlew :core:test --rerun :core:check :plugins:kyc-co:test --rerun :plugins:kyc-co:check :plugins:kyc-pe:test --rerun :plugins:kyc-pe:check
  core/smoke.sh; echo "salida=$?"
  grep -c 'BUILD SUCCESSFUL' aidlc-docs/construction/U02-core-plugins/code/summary.md
  grep -c 'salida=0' aidlc-docs/construction/U02-core-plugins/code/summary.md
  ```
  Esperado: los comandos pasan; el resumen contiene al menos un `BUILD SUCCESSFUL` y un `salida=0`.

- [ ] **CA-2**: trazabilidad completa
  ```bash
  for h in H-10 H-21 H-22 H-23 H-28; do printf '%s ' $h; grep -c "$h" aidlc-docs/construction/U02-core-plugins/code/summary.md; done
  ```
  Esperado: cada historia aparece al menos una vez, con sus tareas.

- [ ] **CA-3**: el diff queda acotado
  ```bash
  git rev-parse --short HEAD; git diff --stat <base-del-despacho>...HEAD
  ```
  Esperado: solo `aidlc-docs/construction/U02-core-plugins/code/**`, la bitácora y `revisiones/U02-T13/`. Se anota el sha.
