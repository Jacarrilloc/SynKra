# Ronda 1 — U03-T07

VEREDICTO: NO-VERDE

Los cinco criterios pasan en lo que importa cuando los corro yo, y el resumen no inventa nada de lo que comprobé. No sale VERDE por dos fallos: falta pegar en la bitácora el `ls` de las rutas, como exige CA-4, y falta una candidata de U03, la C-03.

## Criterios de aceptación, verificados por mí
| # | Criterio | Comando que corrí (desde `/home/user/wt-m6r-U03-T07`) | Resultado |
|---|---|---|---|
| 1 | Salida real de `:backend:check` | `./gradlew :backend:test --rerun :backend:check 2>&1 \| tee <scratch>/u03-check.txt \| tail -5`, más `grep -c 'BUILD SUCCESSFUL' summary.md` | pasa: `BUILD SUCCESSFUL in 12s` (de 17:27:01Z a 17:27:13Z), y el grep da 1. El bloque pegado (líneas 67 a 71 del resumen) es idéntico byte a byte a `tail -5 /tmp/u03-check.txt` (`diff` vacío). La fecha de modificación de ese archivo es 17:24:39.65Z, la misma hora que cita el resumen. Su lista de tareas muestra `compileJava` ejecutada, lo que explica «4 executed». Es la cola de una corrida real, sin editar |
| 2 | Conteos frente a los XML | el bucle `grep -o 'testsuite name=...'` sobre `backend/build/test-results/test/TEST-*.xml` (8 XML, con fecha de mi corrida, 17:27:03Z a 17:27:10Z) | pasa: 3/2/3/6/13/4/1/3, total 35, `skipped=0` en las 8 clases, igual que la tabla. Tiempos de suite entre 0,007 y 3,4 s, así que no hay suite saltada |
| 3 | Imagen sin root | `./gradlew :backend:bootJar && docker build -t synkra-backend-rev backend/ && docker inspect -f '{{.Config.User}}' synkra-backend-rev` | pasa: `10001:10001`. `docker run --rm --entrypoint id` da `uid=10001(synkra)`. Es el mismo UID que deja `bitacoras/U03-T06.md:48`. Docker 29.9.3 respondía. Etiqueta borrada (`docker image ls synkra-backend-rev -q` da 0) |
| 4 | Completo y sin inventar | los 3 greps del criterio; `git log --oneline --merges origin/main \| grep "pull request #N "` por cada N; `ls` de cada ruta | **falla en la evidencia**: secciones, T01 a T08 y los 7 PR (#10 #12 #14 #16 #19 #21 #23) están bien y todos están fusionados. Las 29 rutas existen. Pero la bitácora no pega el `ls` (F-01) y falta la candidata C-03 (F-02) |
| 5 | Solo dos archivos | `git diff --stat origin/main...HEAD` | pasa: solo `summary.md` y `bitacoras/U03-T07.md`, 212 inserciones |

## Contraste con las fuentes (sin invenciones)
- **PR:** los 7 existen y están fusionados. Por la API REST, #10, #12 y #14 van a `modulo-6-rehecho` y #16, #19, #21 y #23 van a `main`.
- **Rondas:** las carpetas de `revisiones/` dan T01=2, T02=4, T03=2, T04=2, T05=3, T06=2. Coinciden con la tabla.
- **Archivos:** las 29 rutas de `backend/` del resumen coinciden una a una con `git ls-files backend`. No falta ninguna de `backend/` ni sobra ninguna.
- **Otras afirmaciones:**
  - Historias H-24, H-18 y H-28: coinciden con las cabeceras de las tareas y con `stories.md:280`.
  - D14: coincide.
  - Spring Boot 3.5.16: coincide.
  - Instrucción `dockerd`: coincide con U03-T06.
  - C-04, C-05, C-07, C-08, C-11, C-12, C-14 y C-17: coinciden con `CANDIDATAS.md`, también en la unidad de destino.
- **C-17:** es correcto incluirla, no es desborde. La línea de alcance pide «las candidatas que U03 deja para otras unidades», y C-17 sale de U03-T06 (ronda 1, F-04) con destino U08. «Fuera» prohíbe *resolver* candidatas, no listarlas. Tampoco toca código, y el resumen declara la diferencia con la lista de la tarea (línea 115). La lista entre paréntesis de la tarea estaba incompleta: es un defecto de la especificación, no del codificador.

## Hallazgos
### F-01 · ROJO · CA-4 / `bitacoras/U03-T07.md` (sección CA-4) · el `ls` de las rutas no está pegado
CA-4 pide que la existencia de cada ruta «se comprueba con `ls` y se pega en la bitácora». La bitácora solo lo afirma: «`ls` de todas las rutas ... sin error; todas existen». No hay comando ni salida, así que es una afirmación sin verificar. Mi `ls` confirma que las 29 rutas existen, de modo que el contenido es correcto. Lo que no se cumple es la evidencia que exige el criterio. Arreglo: pegar el comando `ls` y su salida.

### F-02 · NARANJA · `summary.md:101-115` · falta la candidata C-03, que sale de U03
`CANDIDATAS.md` tiene la C-03 (centralizar versión y repositorios en `settings.gradle.kts`). Salió de la revisión de U03-T01, ronda 1, y sigue pendiente, con destino «una tarea que pueda tocar la raíz». El codificador aplicó el criterio «sale de U03» para añadir C-17, pero no lo aplicó a C-03. Así el barrido queda inconsistente y la sección no está completa.

He revisado la clase entera: las candidatas que salen de U03 son C-03, C-04, C-05, C-07, C-08, C-11, C-12, C-14, C-17 y C-18. La C-18 ya está aplicada («corregido ya en U01-T08 y U03-T07»), así que omitirla se puede defender, aunque convendría una línea que lo diga. La única pendiente que falta es la C-03. La bitácora dice además «Sin candidatas nuevas» y no registra la decisión de ampliar la lista de la tarea.

### F-03 · AMARILLO · `summary.md:21-45` · falta `settings.gradle.kts` en la sección Archivos
U03-T01 (#10) modificó `settings.gradle.kts` para incluir `backend` (`git diff --name-only 2abd4d6^1 2abd4d6`). Es el único archivo fuera de `backend/` que dejaron los PR de U03, y el resumen no lo nombra.

### F-04 · AMARILLO · `summary.md:19` · redacción imprecisa sobre el PR #16
«llegaron a `main` dentro de la rama `modulo-6-rehecho` (PR #16)». La rama de origen del PR #16 es `claude/project-thread-ti5y5k`, que contenía `modulo-6-rehecho` (`merge-base --is-ancestor` da verdadero). El hecho es correcto, pero la frase da a entender que #16 era la rama `modulo-6-rehecho`.

### F-05 · AMARILLO · `bitacoras/U03-T07.md` (sección CA-5) · el diff pegado es anterior al último commit
El `diff --stat` pegado es del commit 40f16b4 (83 líneas de bitácora). El HEAD 3048df3 da 90 líneas y 212 inserciones. Está rotulado y el alcance no cambia, pero no es la evidencia del HEAD.

## Tareas candidatas (defectos reales fuera de alcance)
- Especificación: la lista entre paréntesis de la tarea U03-T07 omitía C-03 y C-17. La plantilla de tareas de resumen debería decir «todas las candidatas cuyo origen sea la unidad» en vez de enumerarlas a mano.
- CA-4 y CA-5 de la tarea usan `main`, que en esta copia está desfasado (en 611d41a). La plantilla debería usar `origin/main` o la rama base.

## Estado del worktree
`git status --short` sale vacío al terminar (solo cambiaron cachés de `build/`, que git ignora). La etiqueta `synkra-backend-rev` está borrada. La imagen `synkra-backend` del codificador no la toqué.

## Rutas de transcripciones largas
- /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/u03t07r1/u03-check.txt
- /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/u03t07r1/bootjar.txt
- /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/u03t07r1/docker-build.txt
- Salida original del codificador: /tmp/u03-check.txt

```
VEREDICTO: NO-VERDE
ROJO|CA-4 / bitacoras/U03-T07.md|el ls de las rutas del resumen no está pegado en la bitácora
NARANJA|aidlc-docs/construction/U03-plataforma-backend/code/summary.md:101-115|falta la candidata C-03, que sale de U03-T01
INFORME: revisiones/U03-T07/ronda-1.md
```
