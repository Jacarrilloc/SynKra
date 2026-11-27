# U04-T12: Resumen de U04

**Unidad:** U04: `Catálogo`
**Historias que implementa:** H-01
**Depende de:** U04-T01 a U04-T11 y U04-T13

---

## Alcance

**Dentro** (una línea, concreta):

> `aidlc-docs/construction/U04-catalogo/code/summary.md`, con la salida real de las pruebas del catálogo y de los `curl` de H-01 a H-06 y H-15, más la lista de tareas, PRs, decisiones y candidatas de la unidad.

**Fuera** (lo que el codificador debe rechazar aunque lo vea roto):

- Código. Si algo falla, se anota y se para.
- Cerrar candidatas.

---

## Archivos de contexto

- `aidlc-docs/construction/U01-verificador/code/summary.md` (el formato)
- `tareas/U04-*.md`, `bitacoras/U04-*.md` y `revisiones/U04-*/`
- `CANDIDATAS.md` (C-21 y C-29: la plantilla de resúmenes)

---

## Decisiones del orquestador

1. **Candidatas.** Se listan todas las candidatas cuyo origen sea una tarea de U04, sacadas con `grep` y no enumeradas a mano (C-21), con su estado.
2. **Salida.** La cola de Gradle va sin editar (`tail -6`), y `grep -n` aparte para las líneas que haga falta citar (C-29).
3. **Script.** Los `curl` corren en un solo script, con el backend arrancado sobre un `SYNKRA_DATA` temporal. El script queda en el resumen, no como archivo nuevo del repositorio.

---

## Criterios de aceptación

- [ ] **CA-1**: las pruebas del catálogo pasan
  ```bash
  ./gradlew :backend:test --rerun --tests 'io.synkra.catalogo.*' 2>&1 | tail -6
  ```
  Esperado: `BUILD SUCCESSFUL`, copiado en el resumen.

- [ ] **CA-2**: los `curl` de H-01 a H-06 y H-15
  Esperado: cada uno con su código y su salida, iguales a lo que piden los CA-2 de U04-T03, T07, T08 y T10.

- [ ] **CA-3**: el resumen está completo
  ```bash
  grep -c '^| U04-T' aidlc-docs/construction/U04-catalogo/code/summary.md
  grep -o 'C-[0-9]*' CANDIDATAS.md | sort -u | wc -l
  ```
  Esperado: 13 filas de tareas, y cada candidata de U04 aparece en el resumen.

- [ ] **CA-4**: el diff queda acotado
  ```bash
  git diff --stat origin/main...HEAD
  ```
  Esperado: el resumen, la bitácora y `revisiones/U04-T12/`.
