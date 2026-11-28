# U06-D: resumen de la unidad U06

**Unidad:** U06: `Registro y vista general`
**Tarea del plan que agrupa:** U06-T08
**Historias que implementa:** H-18 (dueña, con H-19); contribuye H-16 (plan de la unidad: T03)
**Depende de:** U06-A, U06-B y U06-C (las tres fusionadas en `main`). Sale de `ade68f6` (`main`, con #97, #99 y #101). U07 ya tiene partes en esa punta (U07-A a U07-C), pero este resumen no las describe.

---

## Alcance

**Dentro** (una línea, concreta):

> `aidlc-docs/construction/U06-registro/code/summary.md` con lo que construyó la unidad, las desviaciones del diseño que dejaron anotadas U06-A, U06-B y U06-C, la salida real de `./gradlew :backend:test --rerun --tests '*registro*'` y de `./gradlew :backend:test --rerun :backend:check`, la trazabilidad de historias y las candidatas abiertas de U06 (C-107, C-110, C-111, C-120).

**Fuera** (lo que el codificador debe rechazar aunque lo vea roto):

- Cualquier cambio de código, de pruebas, de `build.gradle.kts` o de `application.properties`. Si algo falla al correr los comandos, se para y se reporta; no se arregla.
- Tocar `CANDIDATAS.md`, el plan, `unidades-y-tareas.md` o `aidlc-docs/aidlc-state.md`: lo hace el orquestador al cerrar la unidad. Tampoco se cierran ni se reabren candidatas.
- Describir U07 más allá de «quién llama a `RegistroApi`». No se inventa nada que no esté en los archivos de las fuentes.
- Repetir los criterios de las tareas A, B o C ni volver a correr sus mutaciones: se citan los números de sus revisiones, con su fuente (CA-3).

---

## Archivos de contexto

- Plantillas de resumen: `aidlc-docs/construction/U05-pod-pais/code/summary.md` (formato y `evidencia/`), `aidlc-docs/construction/U04-catalogo/code/summary.md`, `aidlc-docs/construction/U02-core-plugins/code/summary.md`. Y las tareas que los pidieron: `tareas/U05-T10-resumen-unidad.md`, `tareas/U04-T12-resumen.md`, `tareas/U02-T13-resumen-unidad.md`.
- `aidlc-docs/construction/plans/U06-registro-code-generation-plan.md` y `unidades-y-tareas.md` (sección U06)
- `aidlc-docs/construction/U06-registro/functional-design/` (`business-rules.md` R-R1 a R-R8, `domain-entities.md`, `business-logic-model.md`)
- `tareas/U06-A-registro-base.md`, `tareas/U06-B-historial-reinicio.md`, `tareas/U06-C-vista-general-fronteras.md`
- `bitacoras/U06-A.md`, `bitacoras/U06-B.md`, `bitacoras/U06-C.md` y sus carpetas `bitacoras/U06-*-evidencia/`
- `revisiones/U06-A/` (ronda-1, ronda-1-respuesta, ronda-2), `revisiones/U06-B/ronda-1.md`, `revisiones/U06-C/` (ronda-1, ronda-1-respuesta, ronda-2)
- **Candidatas:** C-107 está en `CANDIDATAS.md` de `main`, pero C-110, C-111 y C-120 todavía no (están en la rama del orquestador). Léelas de `/home/casdav/Projects/Master/wt-orq-U04/CANDIDATAS.md` (solo lectura). No las copies a `CANDIDATAS.md` del repo.

---

## Decisiones del orquestador

1. **Archivo y secciones.** El resumen se llama `summary.md` (como los de las demás unidades) y lleva, con estos encabezados exactos de nivel 2, en este orden:
   `## Qué expone`, `## Cómo se prueba`, `## Salidas reales`, `## Desviaciones del diseño`, `## Trazabilidad`, `## Tareas`, `## Candidatas abiertas`, `## Cifras y su fuente`.
   La cabecera (antes de la primera sección) dice la unidad, las historias, la fecha, el sha de la rama y que no se tocó código.
2. **Qué expone.** Paquete `io.synkra.registro` y `registro.api`, la migración `V3__registro.sql` (tres tablas `reg_despliegue`, `reg_plugin`, `reg_contexto`, con su CHECK, la FK `RESTRICT` y el índice `reg_despliegue_pais`), `RegistroApi` (`registrar`, `actualizarResultado`, `obtener`, `historial`, `ultimoListo`), `VistaGeneralApi.vistaGeneral()`, las rutas de solo lectura (`GET /api/paises`, `GET /api/paises/{pais}/despliegues`, `GET /api/despliegues/{id}`), `TruncadoContexto` (R-R4), `PaisesConfigurados` (`synkra.paises`), `Concordancia` (R-R6), y los tipos públicos de `registro.api`. Saca los nombres de los archivos de `backend/src/main/java/io/synkra/registro/`, no de las bitácoras. Una tabla R-R1 a R-R8 contra la clase de prueba que la ata (`TransicionesRegistroTest`, `ContextoFalloTest`, `HistorialTest`, `RegistroReinicioTest`, `VistaGeneralTest`, `PaisesConfiguradosTest`, `FronterasModulosTest`): cada nombre de clase se comprueba con `ls`.
3. **Cómo se prueba.** Sin clúster ni `kubectl` (AUTONOMIA-01): el `AdaptadorKubernetes` es un doble de Mockito en `VistaGeneralTest`; SQLite en `build/sqlite-pruebas/<Clase>`; el reinicio se prueba con dos contextos de Spring sobre el mismo archivo; `sqlite3` no se usa, las lecturas de vuelta salen de `python3 -I`. Anota que el filtro `*registro*` toma las clases de `io.synkra.registro.*`, y que `FronterasModulosTest` está en `io.synkra` y no entra en él (solo en `:backend:check`).
4. **Desviaciones.** Una lista numerada, cada una con: qué decía el diseño o la tarea, qué se hizo, quién lo anotó (archivo de bitácora o revisión) y si el revisor la aceptó. Como mínimo estas, todas ya anotadas en las bitácoras y revisiones:
   - U06-A: `reg_contexto.eventos` es texto, no JSON (bitácora U06-A, «Desviación del diseño (decisión 2)»; `domain-entities.md` dice JSON).
   - U06-B: `RegistroReinicioTest` pasa `synkra.data` con `.run("--synkra.data=...", "--server.port=0")` y no con `.properties(...)`, porque esas propiedades por defecto pierden frente a `application.properties` (revisión U06-B, «Desviación 1», reproducida por el revisor; origen de la nota de C-111).
   - U06-B: la mutación de desempate «quitar `, id desc`» de CA-4 sobrevive por equivalente (plan de SQLite); se usó `id asc` y cae (revisión U06-B, F-01).
   - U06-B: el `jq '.[0].confirmado_en'` del plan se traduce a `confirmadoEn` en el JSON (bitácora U06-B, decisión 5).
   - U06-C: `vistaGeneral` va en `VistaGeneralApi`, no en `RegistroApi` como dice `business-logic-model.md`; el tipo se llama `VistaPais` y no `EstadoPais` (decisión 1 de U06-C).
   - U06-C: R-R6 no compara la versión del core aparte; el digest de la imagen ya identifica el build (decisión 6 de U06-C; `business-rules.md` dice «misma versión del core»).
   - U06-C: `synkra.paises` se valida en `registro` (`PaisesConfigurados`) copiando la regla de `PaisPermitido`; queda repetida (decisión 3; candidata C-110).
   - U06-C: ArchUnit 1.4.2 no tiene `getRecordComponents`; la regla de la API revisa los campos de los `record` y `registroSoloDependeDeKubernetesApiYPlataforma` usa la API de Modulith (bitácora U06-C, «Desviaciones y notas»).
   - U06-C: `VistaGeneralTest` usa `@MockitoBean` y `@DynamicPropertySource` en vez de `SpringApplicationBuilder`.
   - U06-A: `registrar` usa el id del plan (lo da quien registra) y el registro no impone R-D7 (un `aplicando` por país): queda para U07 (bitácora U06-A, «Candidatas»).
   Si encuentras en las bitácoras o revisiones otra desviación no listada, añádela con su fuente; no omitas ninguna de arriba.
5. **Salidas reales.** Se corren en esta tarea, en este orden, con `$JAVA_HOME` en Java 21 y sobre la punta de la rama:
   1. `./gradlew :backend:test --rerun --tests '*registro*'; echo "salida=$?"`, y justo después, del mismo árbol, el conteo por clase de `backend/build/test-results/test/` con el patrón que ya usó `U05-T10` (`evidencia/conteo-por-clase.txt` de U05: se pega el mismo comando, no uno inventado).
   2. `./gradlew :backend:test --rerun :backend:check; echo "salida=$?"`, y el recuento de XML (cuántos, y que ninguno tenga `skipped`, `failures` o `errors` distinto de 0).
   Cada corrida se guarda completa, con fecha, sha, comando y salida, en `aidlc-docs/construction/U06-registro/code/evidencia/<nombre>.txt`. En el resumen se pega solo lo que sale de `tail -6` (o de `grep -n` aparte para líneas citadas, C-29), copiado tal cual y **sin elipsis `[...]`** dentro de los bloques: lo que no cabe se cita por archivo. No uses la cifra de 508 pruebas ni los 40 XML de la ronda 2 de U06-C: en `ade68f6` ya hay código de U07 en el árbol y la cifra de hoy puede ser otra. El resumen dice la de la corrida fresca.
   Si el total de `*registro*` no coincide con la suma de las revisiones (13 + 18 + 9 + 8 + 1 + 12 + 5, de las clases de `registro`), se anota la diferencia con su causa en el resumen y en la bitácora; no se ajusta nada para que cuadre.
6. **Cifras y su fuente.** La última sección es una tabla de tres columnas, `| cifra | ruta | cadena |`, con una fila por cada número de prueba, de mutación, de ronda o de XML que el resumen cite fuera de las salidas pegadas. `ruta` va entre acentos graves, relativa a la raíz del repositorio, y es el archivo de `revisiones/`, `bitacoras/` o `evidencia/` de donde sale. `cadena` va entre acentos graves y es un texto literal que aparece en ese archivo y contiene la cifra (sin el carácter `|`). Si la revisión y la bitácora dan cifras distintas para lo mismo (p. ej. las mutaciones de U06-B o las 28 de U06-C), se citan las dos con su fuente y no se reconcilian. Las cifras de las candidatas C-110, C-111 y C-120 no hacen falta; sus filas del texto sí llevan la ruta absoluta de `CANDIDATAS.md` del orquestador.
7. **Tareas.** Tabla de U06-A, U06-B y U06-C con qué construyó cada una, tareas del plan que cubre (A: T01 a T03; B: T04 y T06; C: T05 y T07; D: T08; comprobar en las tareas), PR (de `git log --merges` y `gh pr list`; #97, #99 y #101 son los de fusión; no inventar los de las ramas) y rondas (los `ronda-N.md` de `revisiones/U06-*/`, contados con `ls`, sin los `-respuesta`). Los veredictos de cada ronda, copiados de la primera línea de cada informe.
8. **Candidatas abiertas.** Se sacan con `grep`, no a mano (C-21): las filas de `CANDIDATAS.md` (la de `main` y la del orquestador) cuyo «Salió de» nombre `U06`. Se listan con su estado y a quién van: C-107 (`REQUIRES_NEW` con el pool de una conexión; avisado en U07-D y U07-E), C-110 (validación de `synkra.paises` repetida; unificar en `plataforma`), C-111 (patrón de país `[a-z]{2}` duplicado en `registro`, orden de plugins sin prueba, notas para pruebas futuras) y C-120 (falso positivo de `registroNoTieneRutasDeEscritura` con `@RequestMapping` de clase sin `method`). Además, C-90 nombra a U06 en su última columna («para U06/U07, alinear `PlanPais`…»): se lista como «relacionada», con su estado, sin decir que U06 la resolvió.
9. **Trazabilidad.** Tabla `Historia | Tareas que la cubren`: H-18 (T01, T02, T04, T06, T07, T08), H-19 (T05), H-16 (contribuye: T03), como en el plan. Una línea aparte dice que el plan nombra H-11 y H-20 como contribuciones de la unidad y que ninguna tarea de U06 las ata por sí sola. Cada fila nombra además la tarea U06-A/B/C que la entregó.
10. **Formato.** Español, sin emojis, sin código nuevo. Los diagramas de Mermaid, si pones alguno, se validan (`common/content-validation.md`); no hace falta ninguno.

---

## Criterios de aceptación

- [ ] **CA-1**: el archivo existe con las secciones, en orden
  ```bash
  f=aidlc-docs/construction/U06-registro/code/summary.md
  test -f "$f" && echo existe
  grep -n '^## ' "$f"
  for s in 'Qué expone' 'Cómo se prueba' 'Salidas reales' 'Desviaciones del diseño' 'Trazabilidad' 'Tareas' 'Candidatas abiertas' 'Cifras y su fuente'; do printf '%s ' "$s"; grep -c "^## $s\$" "$f"; done
  for h in H-16 H-18 H-19; do printf '%s ' $h; grep -c "$h" "$f"; done
  for c in C-107 C-110 C-111 C-120; do printf '%s ' $c; grep -c "$c" "$f"; done
  ```
  Esperado: `existe`; el `grep -n` lista los ocho encabezados en ese orden; cada sección da `1`; cada historia y cada candidata da al menos `1`. En la sección de desviaciones están, buscables con `grep -n -i`, `eventos`, `VistaGeneralApi`, `versión del core` y `.run(`.

- [ ] **CA-2**: las salidas reales son frescas y de la punta
  ```bash
  ./gradlew :backend:test --rerun --tests '*registro*'; echo "salida=$?"
  ./gradlew :backend:test --rerun :backend:check; echo "salida=$?"
  f=aidlc-docs/construction/U06-registro/code/summary.md
  grep -c 'BUILD SUCCESSFUL' "$f"; grep -c 'salida=0' "$f"
  ls aidlc-docs/construction/U06-registro/code/evidencia/
  head -3 aidlc-docs/construction/U06-registro/code/evidencia/*.txt
  ```
  Esperado: las dos corridas dan `BUILD SUCCESSFUL` y `salida=0` (con `$JAVA_HOME` en Java 21, que se anota); el resumen contiene al menos dos `BUILD SUCCESSFUL` y dos `salida=0`; cada archivo de `evidencia/` trae fecha, sha y comando en sus primeras líneas, y el sha es el de la punta de la rama (o su padre, con la nota «tomada justo antes de su propio commit»). El revisor reproduce los mismos conteos de pruebas por clase y de XML.

- [ ] **CA-3**: toda cifra citada sale de un archivo real
  Guardar este guion como `bitacoras/U06-D-evidencia/ca3.sh` (se versiona) y correrlo desde la raíz de la rama:
  ```bash
  f=aidlc-docs/construction/U06-registro/code/summary.md
  sed -n '/^## Cifras y su fuente/,/^## [^C]/p' "$f" | grep '^| ' | tail -n +3 | while IFS='|' read -r _ c r s _; do
    r=$(echo "$r" | tr -d ' `'); s=$(echo "$s" | sed 's/^ *`//; s/` *$//')
    if grep -qF -- "$s" "$r"; then echo "ok $r"; else echo "FALTA $r :: $s"; fi
  done | sort | uniq -c
  ```
  Esperado: ninguna línea `FALTA`, y al menos una fila `ok` por cada una de las tres unidades A, B y C (`revisiones/U06-A/`, `revisiones/U06-B/`, `revisiones/U06-C/`). Para cada número de prueba, mutación, ronda o XML del texto del resumen que no está en la tabla: el revisor lo busca con `grep -rnF '<cifra>' revisiones/U06-* bitacoras/U06-*` y debe aparecer; si no aparece, es hallazgo. Las cifras de la corrida fresca de CA-2 salen de `evidencia/`, no de esta tabla.

- [ ] **CA-4**: toda salida pegada es copia literal de su archivo
  ```bash
  d=aidlc-docs/construction/U06-registro/code
  awk '/^```/{b=!b;next} b' "$d/summary.md" | grep -v '^\$ ' | while IFS= read -r l; do
    [ -z "$l" ] && continue
    grep -rqxF -- "$l" "$d/evidencia" bitacoras/U06-D-evidencia bitacoras/U06-A-evidencia bitacoras/U06-B-evidencia bitacoras/U06-C-evidencia revisiones/U06-A revisiones/U06-B revisiones/U06-C || echo "NO LITERAL: $l"
  done
  grep -c '\[\.\.\.\]' "$d/summary.md"
  ```
  Esperado: el bucle no imprime nada y el último `grep -c` da `0` (ningún bloque con elipsis). Las líneas `$ comando` quedan fuera del bucle; su comando se compara a mano con el de la primera línea del archivo de `evidencia/` correspondiente.

- [ ] **CA-5**: el resto del backend sigue en verde (nada de código cambió)
  ```bash
  ./gradlew :backend:test --rerun :backend:check 2>&1 | tail -6
  git diff --stat ade68f6...HEAD -- backend core verificador plugin-api plugins init-descarga gradle build.gradle.kts settings.gradle.kts | tail -1
  ```
  Esperado: `BUILD SUCCESSFUL`, y el segundo comando no imprime nada (ni una línea de código tocada).

- [ ] **CA-6**: el diff queda acotado
  ```bash
  git rev-parse --short HEAD; git diff --name-only ade68f6...HEAD | grep -v -E '^(aidlc-docs/construction/U06-registro/code/|bitacoras/U06-D\.md$|bitacoras/U06-D-evidencia/)'; echo "fuera=$?"
  ```
  Esperado: la lista vacía y `fuera=1`. Se toma justo antes de su propio commit (el diff no incluye ese commit y el sha impreso es el de su padre); el resultado y la nota van a `bitacoras/U06-D-evidencia/ca6.txt`. Si hace falta evidencia que no cae en esas rutas, se para y se pregunta.
