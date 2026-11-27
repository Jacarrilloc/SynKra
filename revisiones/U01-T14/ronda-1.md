# Ronda 1 — U01-T14

VEREDICTO: NO-VERDE

Worktree `/home/user/wt-m6r-U01-T14`, rama m6r/U01-T14, HEAD ab20703, base origin/main ee925c4. El hash de la tarea coincide (6534aa42…). Al terminar, `git status --short` sale vacío.

## Criterios de aceptación, verificados por mí
| # | Criterio | Comando que corrí | Resultado |
|---|---|---|---|
| 1 | `check` real en el resumen | `./gradlew :verificador:test --rerun :verificador:check --console=plain` (06:33:03 a 06:33:36 UTC), luego `tail -6`, `grep -n verificarSuite` y `grep -c 'BUILD SUCCESSFUL' summary.md` | pasa: rc=0, `BUILD SUCCESSFUL in 32s`, línea 19 `> Task :verificador:verificarSuite`, 1 en el resumen. `/tmp/u01-check.txt` (mtime 06:28:38) es idéntico al bloque pegado. |
| 2 | Conteos de los XML | el bucle `grep -o 'testsuite …'` sobre `TEST-*.xml` | **falla**: hay 12 XML y 178 pruebas, pero la tabla del resumen tiene 11 filas (ver F-01). Ningún skipped. Hay 10 propiedades con `tries = 1000` y `checks = 1000` en el system-out, y cuadran una por una con los `@Property(tries = 1000)` del código. |
| 3 | Control de la suite | `bash scripts/control-suite.sh; echo rc=$?` (06:33:54 a 06:35:53) | pasa: los tres escenarios, las causas 2 y 3 dicen lo mismo que el resumen, rc=0. |
| 4 | Completo y sin inventar | los 4 greps y el `awk`, más un grep de firmas, `ls`, `git log --merges` y el conteo de `revisiones/` | **falla**: «9 códigos» es falso (F-02). Las secciones, T01 a T14, los `#N`, las rondas y las candidatas están bien. |
| 5 | Sin cambios fuera | `git diff --stat origin/main...HEAD` | pasa: solo `summary.md` y `bitacoras/U01-T14.md`. |

Lo que sí comprobé que está bien:
- **Los 14 `#N`.** Todos están fusionados y corresponden a la tarea que dicen: #8, #9, #11, #13, #18, #20, #24, #27, #30, #31, #32, #35 y #36 son U01-T01 a T13, y #15 es m6r/C-02.
- **Las rondas.** Coinciden con `revisiones/U01-T*/ronda-N.md`: 1, 1, 2, 2, 1, 3, 2, 4, 3, 2, 3, 6 y 2.
- **Las candidatas.** El `awk` da 17 (C-01 a C-28 de U01) y el resumen trae exactamente esas 17. Las columnas «qué», «origen» y «estado» son copia literal de CANDIDATAS.md (lo comparé con un script). C-25 va como pendiente, con la recomendación de las Notas.
- **La huella de `verificarSuite`.** Es fiel a `verificador/build.gradle.kts`: las 5 causas, el mínimo de 17, la huella SHA-256 de main, testFixtures y test, y `outputs.upToDateWhen { false }`.
- **R-V4 y R-V5.** Son fieles a `Verificador.java`: el orden ausente, core equivocado, incompatible; la versión más alta del plugin y la más baja del core.
- **Las rutas.** Existen todas, y los artefactos de build están marcados como tales (línea 106).
- **CA-1 y la línea de `verificarSuite` fuera de `tail -6`.** Lo juzgo aceptable. El bloque de la cola está sin editar, y la línea de `verificarSuite` va aparte, con su comando (`grep -n`) y su procedencia declarada. No se maquilló nada. El criterio pide algo que `tail -6` no puede dar, y eso es un defecto de la especificación, no del codificador (F-05).
- **Nombres de personas.** No hay ninguno en el resumen. El handle de GitHub que aparece en la bitácora (líneas 122 a 135) sale tal cual de la salida de `git log` que pide CA-4. No es un hallazgo.

## Hallazgos

### F-01 · ROJO · summary.md:154-167 (CA-2) · Falta la fila de `VerificadorTest` en la tabla de conteos
Mi corrida del bucle de CA-2 da 12 líneas. Una de ellas es `testsuite name="io.synkra.verificador.VerificadorTest" tests="15" skipped="0" failures="0" errors="0"`. La tabla del resumen tiene 11 filas: salta de `VerificadorPropiedades` a `VersionTest`. Aun así, la fila de total dice «12 clases» y 178. Las 11 filas suman 163, y 163 + 15 = 178. El criterio pide «una fila por clase, los mismos números». La bitácora (línea 52) sí tiene esa línea, así que es un error al transcribir.

### F-02 · ROJO · summary.md:22 (CA-4, API pública) · `enum CodigoMotivo (9 códigos)` es falso: son 10
`verificador/src/main/java/io/synkra/verificador/CodigoMotivo.java` tiene 10 constantes, la última `DESCRIPTOR_INVALIDO`. Según `git log --follow`, f2bf808 (U01-T03) dejó 9 y 4c1a396 (U01-T06) añadió `DESCRIPTOR_INVALIDO`. `domain-entities.md`, en «Códigos de motivo», también lista 10. La sección «qué expone» describe el estado actual, así que el 9 es una afirmación inventada.

Barrido de la misma clase:
- Línea 46, fila de U01-T03 («9 códigos»): históricamente es correcta.
- Línea 49, fila de U01-T06: omite que esa tarea añadió `DESCRIPTOR_INVALIDO`. No hay más apariciones.

### F-03 · NARANJA · summary.md:28 y :36 («Quién lo usa», U04) · Uso de `CatalogoEnMemoria` por U04 sin fuente, y frase rota
- **Línea 28.** Dice que `CatalogoEnMemoria` es un «`Catalogo` para pruebas de U02, U04 y U07». Las fuentes (`unit-of-work.md:41`, `unidades-y-tareas.md:52`, `tareas/U01-T09-verificador.md:13` y `tareas/U01-T12-suite-combinaciones.md:115`) solo nombran a U02 y U07. El plan de U04 (`U04-catalogo-code-generation-plan.md:48`) prueba S1 a S7 «usando datos guardados en SQLite», y U04 implementa `Catalogo` él mismo. No encontré dónde se dice que U04 consuma `CatalogoEnMemoria`.
- **Línea 36.** Tampoco dice cómo consume U04 `testFixtures`: falta nombrar `SuiteCombinaciones` como fuente de los casos S1 a S7.
- **Línea 36, redacción.** La frase está rota: «**U04 (catálogo):** además, S11 (…) queda pendiente en U04 y U05. la recepción del JAR…». Hay un «además» que no sigue a nada y una oración que empieza en minúscula; parece un fragmento movido de sitio.

### F-04 · AMARILLO · summary.md:8 · «sin excepciones para entradas inválidas» se lee más amplio de lo que es
Hay constructores que lanzan `IllegalArgumentException`: `Descriptor`, `VeredictoPlugin` (C-22), `Veredicto` y `Resultado.Fallo`. `Verificador.alternativas` también lanza si el veredicto no es `verificar(seleccion, catalogo)` (`Verificador.java:60-63`). La frase limita la regla a «fallos de datos», pero conviene decir que las precondiciones de programación sí lanzan.

### F-05 · AMARILLO · tarea CA-1 (especificación) · El criterio es contradictorio en sí mismo
Pide «la cola sin editar» de `tail -6` y que «incluya la línea de `verificarSuite`», que está en la línea 19 de unas 25. La solución del codificador es honesta. Va para el orquestador.

### F-06 · AMARILLO · bitacoras/U01-T14.md:186-189 · El stat de CA-5 es de antes de la última edición de la bitácora
La bitácora dice `184 +++`. Mi `git diff --stat` da `189`. Es autorreferencial e inocuo, pero es evidencia que no sale del diff final.

## Tareas candidatas (defectos reales fuera de alcance)
- Corregir la especificación de CA-1 en la plantilla de resúmenes de unidad: pedir `tail -6` y aparte `grep -n verificarSuite`, o una cola más larga.
- `aidlc-docs/construction/plans/U07-despliegue-code-generation-plan.md:3` nombra a una persona. Está fuera de este diff; habría que revisarlo según la regla de roles.

## Rutas de transcripciones largas
- /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/check.txt
- /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/control.txt
- Resumen revisado: /home/user/wt-m6r-U01-T14/aidlc-docs/construction/U01-verificador/code/summary.md
- Bitácora: /home/user/wt-m6r-U01-T14/bitacoras/U01-T14.md

```
VEREDICTO: NO-VERDE
ROJO|summary.md:154-167 (CA-2)|Falta la fila de VerificadorTest (15) en la tabla de conteos; ROJO|summary.md:22 (CA-4)|CodigoMotivo tiene 10 códigos, no 9; NARANJA|summary.md:28,36|Uso de CatalogoEnMemoria por U04 sin fuente y frase de U04 rota
INFORME: revisiones/U01-T14/ronda-1.md
```
