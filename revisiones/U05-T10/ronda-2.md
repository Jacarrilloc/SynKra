# Ronda 2: U05-T10

VEREDICTO: NO-VERDE

Ejecuté los tres criterios yo mismo y los tres pasan. Cinco de los seis hallazgos de la ronda 1 quedan cerrados. F-01 queda abierto por una sola instancia: el arreglo dejó en el resumen la cola de Gradle de la corrida anterior, ahora atribuida a una corrida que no la produjo. El worktree quedó limpio (`git status --short` sin líneas, HEAD 46be66d) y borré los temporales de mktemp.

## Criterios de aceptación, verificados por mí
Todo se corrió desde /home/julian/Escritorio/Proyectos/wt-m6r-U05-T10, con HEAD 46be66d, el 2026-11-27T19:06:45-05:00, y HOME y KUBECONFIG falsos.

| # | Criterio | Comando que corrí | Resultado |
|---|---|---|---|
| 1a | Pruebas del adaptador | `./gradlew :backend:test --rerun --tests '*kubernetes*'` | **pasa**: salida=0, BUILD SUCCESSFUL in 14s. En los XML hay 10 clases y 127 pruebas, con skipped, failures y errors en 0. Los timestamps son de esta corrida y los tiempos por clase van de 0,002 a 4,31 s, coherentes con el servidor simulado. Los conteos por clase coinciden con `evidencia/conteo-por-clase.txt`. |
| 1b | Script del init | `init-descarga/test.sh; echo "salida=$?"` | **pasa**: salida=0 y 27 casos `ok`. Las líneas son idénticas a las de `evidencia/init-descarga-test.txt` (diff vacío). Tarda 0,83 s porque el `sleep` es falso, como declara el resumen. |
| 1c | Conteos en el resumen | `grep -c 'BUILD SUCCESSFUL'` y `grep -c 'salida=0'` sobre summary.md | **pasa**: 1 y 1 |
| 1d | Imagen (decisión 3) | `docker build` y luego `docker inspect -f '{{.Config.User}} {{json .Config.Entrypoint}}' synkra-init-descarga:1.0.0` | **pasa**: build=0 y `10001:10001 ["/usr/local/bin/descargar.sh"]` |
| 2 | Trazabilidad | el bucle `for h in H-12 … H-28` | **pasa**: H-12 2, H-13 3, H-14 2, H-15 3, H-16 2, H-19 2, H-28 2. Coincide con la bitácora. |
| 3 | Diff acotado | `git rev-parse --short HEAD` y `git diff --stat 03943ec...HEAD` | **pasa**: 46be66d, 8 archivos, 384 inserciones. Solo hay archivos en `code/**`, `bitacoras/U05-T10.md` y `revisiones/U05-T10/`. |

Barrido de candidatas: tomé cada fila de CANDIDATAS.md (hasta C-103) que nombra U05, U07 o U08 y busqué su identificador en summary.md. No falta ninguna. C-30 y C-103 cuadran con sus filas, y C-71 aparece con su explicación.

## Cierre de los hallazgos de la ronda 1
- **F-01 (NARANJA), cerrado en parte, sigue abierto como F-07.**
  - Lo cerrado: los cuatro archivos de `evidencia/` traen ahora fecha, sha 587e7a4, comando y salida completa. El `docker build` va íntegro con `--progress=plain`, y la bitácora trae el comando y la salida de CA-1, CA-2 y CA-3.
  - CA-3: anota el sha 9fec51a y 370 inserciones, rotulado «antes del commit de esta nota». Un commit no puede contener su propio sha, así que lo acepto.
  - Lo que queda: la cola pegada en el resumen (ver F-07).
- **F-02 (NARANJA), cerrado.** C-30 está en el resumen y C-71 se explica.
- **F-03 (AMARILLO), cerrado.** La línea 99 declara que la tabla amplía la del plan y por qué.
- **F-04 (AMARILLO), cerrado.** #85 aparece con base `main`; #87 y #90 con su base.
- **F-05 (AMARILLO), cerrado.**
- **F-06 (AMARILLO), cerrado.** C-103 está en la lista de U08.

## Hallazgos

### F-07 · NARANJA · aidlc-docs/construction/U05-pod-pais/code/summary.md:24-31 · La cola de Gradle pegada no sale de la corrida que se cita
**Qué pega el resumen (líneas 25 a 28):**
```
$ ./gradlew :backend:test --rerun --tests '*kubernetes*' 2>&1 | tail -8
BUILD SUCCESSFUL in 15s
9 actionable tasks: 9 executed
```
La línea 31 dice: «Se pega la cola; la salida completa, con fecha, sha `587e7a4` y comando, está en `evidencia/gradle-kubernetes.txt`».

**Qué dice ese archivo:** `BUILD SUCCESSFUL in 13s` y `9 actionable tasks: 1 executed, 8 up-to-date`.

**De dónde sale el bloque:** las líneas «15s / 9 executed» son de la corrida de la ronda 1, en el sha 03943ec. Eran el contenido del `gradle-kubernetes.txt` anterior, que se sobrescribió en 1a7ab3b.

**Qué está mal:**
- El bloque es real, pero se presenta como la cola de un archivo que no lo contiene.
- La ronda 2 le quitó la fecha y el sha que traía. La decisión 2 de la tarea pide «tal cual, con la fecha, el sha de la rama y el comando».
- El comando dice `tail -8` y el bloque muestra 3 líneas, así que tampoco está «tal cual».

**Comprobación:**
- `git diff 587e7a4..46be66d -- aidlc-docs/.../summary.md` no toca las líneas 25 a 28.
- `git show 587e7a4:aidlc-docs/construction/U05-pod-pais/code/evidencia/gradle-kubernetes.txt` contiene «BUILD SUCCESSFUL in 15s / 9 executed».

Es la misma clase que F-01 y C-103: evidencia cuyo texto no sostiene la atribución. Para cerrarlo, la cola del resumen tiene que ser la cola literal de la evidencia que cita, o traer su propia fecha y sha.

### F-08 · AMARILLO · evidencia/gradle-kubernetes.txt:2 y evidencia/imagen-init.txt:2 · El comando anotado no produce la línea `salida=0`
Los dos archivos terminan su bloque con `salida=0`, pero el comando de cabecera no incluye `; echo "salida=$?"`. `init-descarga-test.txt` sí lo anota bien. La corrección es menor: escribir el comando exacto que generó cada archivo.

## Tareas candidatas (defectos reales fuera de alcance)
- Ninguna nueva.

## Rutas de transcripciones largas
- /tmp/claude-1000/-home-julian-Escritorio-Proyectos-SynKra/76af36c7-fa86-447c-9ae5-76b2a7831e37/scratchpad/gradle-r2.txt
- /tmp/claude-1000/-home-julian-Escritorio-Proyectos-SynKra/76af36c7-fa86-447c-9ae5-76b2a7831e37/scratchpad/init-r2.txt

Nota: no pude escribir `revisiones/U05-T10/ronda-2.md` porque tengo solo lectura. Este texto es el informe de la ronda; lo versiona el orquestador.

```
VEREDICTO: NO-VERDE
NARANJA|aidlc-docs/construction/U05-pod-pais/code/summary.md:24-31|Cola de Gradle de la corrida de r1 (15s, 9 executed) atribuida a la evidencia de 587e7a4 (13s, 1 executed), sin fecha ni sha y no «tal cual» (tail -8 con 3 líneas)
AMARILLO|evidencia/gradle-kubernetes.txt:2, evidencia/imagen-init.txt:2|El comando anotado no incluye el echo que produce salida=0
INFORME: revisiones/U05-T10/ronda-2.md
```
