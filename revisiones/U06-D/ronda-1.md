# Ronda 1 — U06-D

VEREDICTO: VERDE

El hash de la tarea coincide: `git hash-object tareas/U06-D-resumen.md` da b9219a0280ef0be4a44161dd4779c4ffdadbd850. `sha1sum` da otro valor porque no es el hash de git. Mi último `git status --short` del worktree salió vacío, después de que corrí Gradle. Mis salidas están en el scratchpad `/tmp/claude-1000/-home-casdav-Projects-Master-SynKra/9d987332-269f-4da9-9d66-0a5449231723/scratchpad/U06-D-r1/` (`reg.txt` y `chk.txt`).

## Criterios de aceptación, verificados por mí
| # | Criterio | Comando que corrí | Resultado |
|---|---|---|---|
| 1 | Secciones, historias, candidatas | los `grep` literales de CA-1 | pasa. Hay 8 encabezados en orden y cada sección da 1. H-16, H-18 y H-19 dan 2 cada una. C-107, C-110, C-111 y C-120 dan 4, 4, 3 y 2. Aparecen `eventos`, `VistaGeneralApi`, `versión del core` y `.run(` en la sección de desviaciones. |
| 2 | Salidas frescas y de la punta | `JAVA_HOME=~/.gradle/jdks/eclipse_adoptium-21-amd64-linux.2`; `:backend:test --rerun --tests '*registro*'` y `:backend:test --rerun :backend:check` | pasa. Las dos dan `BUILD SUCCESSFUL` y `salida=0`. Mi conteo por clase es 9, 8, 5, 1, 13, 18 y 12, es decir 66 pruebas, todas con skipped, failures y errors en 0. Mi recuento de XML es 48, con 0 fuera de `skipped="0" failures="0" errors="0"`, y 566 pruebas. Las cifras coinciden con la evidencia. El resumen tiene 2 `BUILD SUCCESSFUL` y 2 `salida=0`. Cada archivo de `evidencia/` trae fecha, sha y comando en su cabecera. |
| 3 | Cifras con fuente | `bitacoras/U06-D-evidencia/ca3.sh` | pasa. No hay ningún `FALTA`. Hay filas `ok` de las tres unidades: `bitacoras/U06-A.md`, `U06-B.md` y `U06-C.md`, y `revisiones/U06-A/ronda-1.md`, `ronda-2.md`, `U06-B/ronda-1.md`, `U06-C/ronda-1.md` y `ronda-2.md`. |
| 4 | Salidas pegadas literales | el bucle `awk`/`grep -rqxF` de CA-4 | pasa. El bucle no imprime nada y `grep -c '\[\.\.\.\]'` da 0. |
| 5 | Backend en verde y sin código tocado | la corrida de CA-2 y `git diff --stat ade68f6...HEAD -- backend core verificador plugin-api plugins init-descarga gradle *.kts` | pasa. `BUILD SUCCESSFUL` en 47 s, y el diff de carpetas de código no imprime nada. |
| 6 | Diff acotado | `git diff --name-only eb33d68...HEAD` con el filtro de CA-6 | pasa. La lista sale vacía y `fuera=1`. Contra `ade68f6` salen archivos de la rama del orquestador, como ya aclaraste, y `ca6.txt` lo documenta con las dos bases. `git diff --name-only 1a01b96 HEAD` toca solo `aidlc-docs/.../code/`, `bitacoras/U06-D.md` y `bitacoras/U06-D-evidencia/`. |

## Muestra amplia contra las fuentes
- **Rondas y veredictos:** la primera línea `VEREDICTO:` de cada informe coincide con la tabla. U06-A da NO-VERDE y VERDE, U06-B da VERDE, U06-C da NO-VERDE y VERDE. `ls revisiones/U06-*/` da las mismas rondas.
- **PR:** `git log --merges` confirma #97 (U06-A), #99 (U06-B) y #101 (U06-C).
- **Candidatas:** las filas de C-107, C-110, C-111, C-120 y C-90 existen en `CANDIDATAS.md` del árbol, con estado y destino como el resumen los cita. «Relacionada» para C-90 y «sin que U06 la resolviera» respetan la fuente.
- **Desviaciones:** las 11 existen en su fuente.
  - Las atribuciones de aceptación se sostienen. «Es aceptable» está en `revisiones/U06-C/ronda-1.md` líneas 34 a 36, para las desviaciones 5, 6 y 8. «Justificada» está en `revisiones/U06-B/ronda-1.md` línea 19, para la 2, reproducida con `AccessDeniedException`. El F-01 AMARILLO de U06-B está presente y aceptado, para la 3.
  - Donde el resumen dice que el revisor no comenta una desviación (1, 4, 7 y 9), `grep` sobre `revisiones/` no encuentra nada. En la 7 solo aparece la referencia a C-110. El resumen no inventa aceptación.
  - La desviación 11 es la que el resumen añade por su cuenta, con fuente en la bitácora y la ronda 1 de U06-C (líneas 15 y 16).
- **Cifras de la unidad, ejemplos:** 40 XML y 506 pruebas en la ronda 1 de U06-C, 40 XML y 508 pruebas en la ronda 2, 38 XML y 488 pruebas en U06-B, 36 XML en U06-A y las 28 mutaciones. Las dos cifras distintas de 2 min 37 s y 2 min 38 s se citan sin reconciliar, como pide la decisión 6.
- **Qué expone:** coincide con el código real.
  - Los 16 archivos de `registro/` y los 13 de `registro/api/` existen con los nombres citados.
  - La migración `V3__registro.sql` tiene el CHECK de país, tipo, estado y `json_valid`, las dos CHECK de coherencia, `ON DELETE RESTRICT` y el índice `reg_despliegue_pais`.
  - Las tres rutas son `@GetMapping`, y `TruncadoContexto` usa 200 líneas y 64 KB.
  - `grep -rln 'RegistroApi\|VistaGeneralApi' backend/src/main` fuera de `registro/` da rc=1, sin archivos.
- **Diferencia de hoy frente a U06-C:** de 40 XML y 508 pruebas a 48 XML y 566. El resumen la anota sin ajustarla. Los 8 XML de más tienen origen real: 7 clases en `despliegue` y `RelojTest`. Los pocos ficheros `A` de `backend/src/test` que no son clases de prueba (`CatalogoDePrueba`, `RelojMovible`, `SimuladorKubernetes`) no producen XML.

## Hallazgos
### F-01 · AMARILLO · cabecera de `summary.md` y `evidencia/*.txt` · las corridas llevan el sha `1a01b96`, que no es la punta ni su padre
La decisión 5 y el CA-2 piden «la punta de la rama o su padre, con la nota». Las corridas se hicieron en `1a01b96`, el primer commit de la tarea (acuse de lectura), antes de crear `summary.md`. La cabecera lo dice con claridad y `git diff 1a01b96 HEAD` toca solo documentación, así que no hay riesgo real. Yo reproduje las mismas cifras en la punta `8d3f0a8`. No bloquea.

### F-02 · AMARILLO · sección «Cómo se prueba» y «Salidas reales» · la causa de los 58 tests de diferencia no está desglosada
El resumen lo reconoce («no se desglosaron una por una»). Lo que sí verificó es suficiente: 40 a 48 XML y las 8 clases nuevas. No bloquea.

## Tareas candidatas (defectos reales fuera de alcance)
- Ninguna nueva.

## Rutas de transcripciones largas
- `/tmp/claude-1000/-home-casdav-Projects-Master-SynKra/9d987332-269f-4da9-9d66-0a5449231723/scratchpad/U06-D-r1/reg.txt`
- `/tmp/claude-1000/-home-casdav-Projects-Master-SynKra/9d987332-269f-4da9-9d66-0a5449231723/scratchpad/U06-D-r1/chk.txt`

```
VEREDICTO: VERDE
AMARILLO|cabecera de summary.md y evidencia/*.txt|corridas con el sha 1a01b96, no con la punta ni su padre
AMARILLO|Salidas reales|58 pruebas de diferencia con U06-C sin desglosar (anotado en el resumen)
INFORME: revisiones/U06-D/ronda-1.md
```
