# Ronda 1 — U02-T13

VEREDICTO: VERDE

Revisé HEAD 00b1b77 (base 63d99fa) en /home/julian/Escritorio/Proyectos/wt-m6r-U02-T13. El worktree quedó limpio y no dejé contenedores ni temporales. El informe no se guardó como archivo (mis herramientas no escriben). El orquestador debe guardarlo en `revisiones/U02-T13/ronda-1.md`.

## Criterios de aceptación, verificados por mí
| # | Criterio | Comando que corrí | Resultado |
|---|---|---|---|
| 1a | Gradle con `--rerun` pasa | `./gradlew :core:test --rerun :core:check :plugins:kyc-co:test --rerun :plugins:kyc-co:check :plugins:kyc-pe:test --rerun :plugins:kyc-pe:check` | pasa, `salida=0`, BUILD SUCCESSFUL |
| 1b | Conteos del XML | Conteos de los `TEST-*.xml` de los tres módulos | pasa. Los 9 archivos tienen `skipped=0`, `failures=0` y `errors=0`. Los conteos son idénticos a los del resumen y suman 89 (54 core, 17 kyc-co, 18 kyc-pe) |
| 1c | `core/smoke.sh` | Reconstruí los JAR y corrí `core/smoke.sh; echo salida=$?`. La imagen `synkra-core:2.1.0` ya existía | pasa: `ok caso1-kyc-pe-1.3.0`, `ok caso2-kyc-pe-2.0.0`, `salida=0` |
| 1d | El resumen contiene las marcas | `grep -c 'BUILD SUCCESSFUL'` y `grep -c 'salida=0'` sobre summary.md | 1 y 3: pasa |
| 2 | Trazabilidad | `grep -c` de H-10, H-21, H-22, H-23 y H-28 | 3, 2, 2, 2 y 2. La tabla coincide con la sección Trazabilidad del plan (H-21: T01, T02, T04, T05, T07, T08, T12 y T13; H-22: T03 y T06; H-23: T09 y T10; H-28: T11; H-10: T05). El resumen suma T07 en H-21 y T05 en H-10 tal como el plan |
| 3 | Diff acotado | `git diff --name-only 63d99fa...HEAD` | pasa. Solo hay 7 archivos de `evidencia/`, `summary.md` y `bitacoras/U02-T13.md`. No hay código, scripts ni plan ni `aidlc-state.md` tocados. El `revisiones/U02-T13/` lo crea esta revisión. El resumen explica que el sha de las salidas es 63d99fa, la punta de T12, y que T13 solo añade documentos |

## Contraste del resumen con los archivos reales
- **Salidas pegadas, copia literal.** Comprobé con un script que cada bloque del resumen es una subcadena exacta de su archivo de evidencia:
  - `tail -6` de gradle-check.txt.
  - conteo-por-clase.txt sin las 4 líneas de cabecera.
  - smoke.txt sin las 3 líneas de cabecera.
  - `tail -4` de imagen.txt.
  - descriptores.txt sin las 3 líneas de cabecera.
  - Las dos únicas elisiones, `[...]` en gradle y en imagen, están declaradas.
- **Cabeceras de la evidencia.** Los 7 archivos traen fecha (2026-11-28T00:20:39Z a 00:21:02Z, consistente con el log de Spring en -05:00), sha 63d99fa, cwd y comando exacto con `salida=$?` donde corresponde.
- **Rondas.** Coinciden con los informes reales: T01=1, T02=2, T03=2, T04=2, T05=2, T06=3, T07=2 (en `origin/m6r/U02-T07`), T08=1, T09=2, T10=1, T11=1, T12=3. Cada última ronda es VERDE y las anteriores NO-VERDE.
- **PR.**
  - `gh pr list` y `git log --merges` confirman #69, #72, #75, #79, #80 y #82 como fusionados.
  - #84, #86, #88, #89, #91 y #93 están abiertos, con las bases apiladas que dice el resumen.
  - #68, #73 y #74 son de la rama del orquestador.
- **Candidatas.**
  - Barrí `CANDIDATAS.md` completo (105 filas, C-01 a C-105). Todas las que nombran U02 están incluidas.
  - Las que no la nombran pero tocan el core (C-81, C-88, C-90, C-94, C-97, C-98) también aparecen.
  - Ninguna lleva marca de cierre.
  - Las notas de C-81 y C-84 coinciden con lo que dice el resumen.
- **Detalles técnicos.** El Dockerfile usa `temurin-alpine` por digest y el plan decía distroless: el resumen lo declara. La salida de `docker inspect` (usuario 10001:10001, JarLauncher, puerto 8080) coincide con la evidencia.

## Hallazgos
### F-01 · AMARILLO · summary.md, bloques de salida · elisiones `[...]`
Los bloques de Gradle e imagen muestran el comando seguido de `[...]` y la cola. Está declarado y el archivo completo está en `evidencia/`, así que no bloquea. Quien lea solo el resumen no ve los tasks intermedios. Es la regla de C-29, ya aceptada.

### F-02 · AMARILLO · bitácora/CA-3 · sha anotado
CA-3 pide anotar el sha de HEAD (00b1b77) y el resumen anota el de la base (63d99fa). Se explica en el texto y los commits se ven en `git log`. Es una convención, no un defecto.

No hay hallazgos ROJO ni NARANJA. La tarea no introduce literales de decisión ni cambios de estado que exijan lectura de vuelta.

## Tareas candidatas
- Ninguna nueva.

## Rutas de transcripciones largas
- /tmp/claude-1000/-home-julian-Escritorio-Proyectos-SynKra/76af36c7-fa86-447c-9ae5-76b2a7831e37/scratchpad/g.txt (mi corrida de Gradle)

VEREDICTO: VERDE
AMARILLO|summary.md bloques de salida|elisiones `[...]` declaradas (F-01); sha de base en vez de HEAD (F-02)
INFORME: revisiones/U02-T13/ronda-1.md
