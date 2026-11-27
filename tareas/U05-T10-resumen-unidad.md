# U05-T10: resumen de la unidad U05

**Unidad:** U05: `Pod del país`
**Historias que implementa:** H-15
**Depende de:** U05-T01 a U05-T09

---

## Alcance

**Dentro** (una línea, concreta):

> El resumen `aidlc-docs/construction/U05-pod-pais/code/summary.md` con lo que entregó la unidad, la salida real de `./gradlew :backend:test --tests '*kubernetes*'` y de `init-descarga/test.sh`, la imagen del init, la trazabilidad de historias y las candidatas abiertas que deja la unidad.

**Fuera** (lo que el codificador debe rechazar aunque lo vea roto):

- Cualquier cambio de código o de pruebas. Si algo falla al correr los comandos, se para y se reporta.
- Marcar el plan o tocar `aidlc-docs/aidlc-state.md`: lo hace el orquestador al cerrar la unidad.

---

## Archivos de contexto

- `aidlc-docs/construction/plans/U05-pod-pais-code-generation-plan.md`
- `aidlc-docs/construction/U04-catalogo/code/summary.md` (formato de un resumen ya aceptado)
- `bitacoras/U05-T0*.md`, `revisiones/U05-T*/` y `CANDIDATAS.md`

---

## Decisiones del orquestador

1. **Secciones.** Qué expone el módulo (`AdaptadorKubernetes` y sus cinco métodos, tipos de `kubernetes.api`, la imagen y el script); cómo se prueba (servidor simulado de Fabric8, servidor HTTP local, nada contra un clúster: AUTONOMIA-01); salidas reales; trazabilidad H-12 a H-19 y H-28 contra tareas; rondas que costó cada tarea; candidatas abiertas que nombran U05, U07 o U08 (C-75, C-76, C-82, C-83 y las que queden).
2. **Salidas reales.** Se pegan tal cual, con la fecha, el sha de la rama y el comando. Se versionan también como archivos en `aidlc-docs/construction/U05-pod-pais/code/evidencia/`.
3. **Imagen.** `docker inspect -f '{{.Config.User}} {{json .Config.Entrypoint}}' synkra-init-descarga:1.0.0` después de construirla.

---

## Criterios de aceptación

- [ ] **CA-1**: el resumen contiene salidas reales y frescas
  ```bash
  ./gradlew :backend:test --rerun --tests '*kubernetes*'
  init-descarga/test.sh; echo "salida=$?"
  grep -c 'BUILD SUCCESSFUL' aidlc-docs/construction/U05-pod-pais/code/summary.md
  grep -c 'salida=0' aidlc-docs/construction/U05-pod-pais/code/summary.md
  ```
  Esperado: los dos comandos pasan; el resumen contiene al menos un `BUILD SUCCESSFUL` y un `salida=0`, y el revisor puede reproducir los mismos conteos de pruebas.

- [ ] **CA-2**: trazabilidad completa
  ```bash
  for h in H-12 H-13 H-14 H-15 H-16 H-19 H-28; do printf '%s ' $h; grep -c "$h" aidlc-docs/construction/U05-pod-pais/code/summary.md; done
  ```
  Esperado: cada historia aparece al menos una vez, con las tareas que la cubren.

- [ ] **CA-3**: el diff queda acotado
  ```bash
  git rev-parse --short HEAD; git diff --stat <base-del-despacho>...HEAD
  ```
  Esperado: solo `aidlc-docs/construction/U05-pod-pais/code/**`, la bitácora y `revisiones/U05-T10/`. Se anota el sha.
