# Ronda 3: U05-T10

VEREDICTO: VERDE

Corrí yo mismo los tres criterios, y también la decisión 3 (la imagen). Todos pasan. F-07 y F-08 de la ronda 2 quedan cerrados. Lo único que queda es un AMARILLO en la bitácora, que no bloquea. Al terminar, el worktree está limpio (`git status --short` da 0 líneas, HEAD 8c5e713) y borré el directorio de `mktemp -d`.

## Criterios de aceptación, verificados por mí
Todo se corrió desde /home/julian/Escritorio/Proyectos/wt-m6r-U05-T10, con HEAD 8c5e713, a partir de 2026-11-27T19:09:53-05:00. Usé HOME y KUBECONFIG falsos en `mktemp -d` y GRADLE_USER_HOME real.

| # | Criterio | Comando que corrí | Resultado |
|---|---|---|---|
| 1a | Pruebas del adaptador | `./gradlew :backend:test --rerun --tests '*kubernetes*'` | **pasa**. salida=0, BUILD SUCCESSFUL in 12s, `9 actionable tasks: 1 executed, 8 up-to-date`. Hay 10 XML con 127 pruebas, skipped=0, failures=0 y errors=0. Los timestamps (00:09:54Z a 00:10:05Z) son de esta corrida. Los tiempos por clase van de 0,001 a 3,6 s, coherentes con el servidor simulado, así que no es una suite saltada. |
| 1a' | Conteo por clase | el comando literal de la cabecera de `evidencia/conteo-por-clase.txt` | **pasa**. `diff` contra la evidencia: vacío. stderr vacío. |
| 1b | Script del init | `init-descarga/test.sh; echo "salida=$?"` | **pasa**. salida=0 y 27 casos `ok`. `diff` contra `evidencia/init-descarga-test.txt`: vacío. Tarda 0,8 s porque el `sleep` es falso, como declara el resumen. |
| 1c | Conteos en summary.md | `grep -c 'BUILD SUCCESSFUL'` / `grep -c 'salida=0'` | **pasa**. 1 y 3, y el criterio pide al menos 1 de cada uno. |
| 1d | Imagen (decisión 3) | `docker build --progress=plain -t synkra-init-descarga:1.0.0 init-descarga/` y luego `docker inspect -f '{{.Config.User}} {{json .Config.Entrypoint}}' …` | **pasa**. build=0 y `10001:10001 ["/usr/local/bin/descargar.sh"]`. El config `sha256:7b356a14…cc72` es idéntico al de `evidencia/imagen-init.txt`. |
| 2 | Trazabilidad | bucle `for h in H-12 … H-28` | **pasa**. H-12 2, H-13 3, H-14 2, H-15 3, H-16 2, H-19 2, H-28 2. Coincide con la bitácora. |
| 3 | Diff acotado | `git rev-parse --short HEAD`; `git diff --stat 03943ec...HEAD` | **pasa**. 8c5e713, 10 archivos, 500 inserciones. Ningún archivo queda fuera de `code/**`, `bitacoras/U05-T10.md` y `revisiones/U05-T10/`: el filtro `grep -v` no dejó ninguna línea. El diff de la ronda (82bf33d..8c5e713) tampoco sale de esa cerca. |

Barrido de candidatas: tomé cada fila de CANDIDATAS.md que nombra U05, U07 o U08 (llega hasta C-103, sin filas nuevas) y busqué su identificador en summary.md. No falta ninguna.

## Cierre de los hallazgos de la ronda 2
- **F-07 (NARANJA), cerrado.**
  - El bloque de Gradle del resumen es el comando de cabecera seguido de `[...]` y las 6 líneas literales de `tail -6 evidencia/gradle-kubernetes.txt`: 13s, `1 executed, 8 up-to-date` y `salida=0`. Lo comprobé línea por línea.
  - La entradilla nombra el archivo, el sha 82bf33d y N=6.
  - El bloque de la imagen son las 3 últimas líneas literales de `imagen-init.txt`, también comprobado.
  - El script y el conteo por clase son idénticos a su evidencia (`diff` vacío).
  - Los cuatro archivos de evidencia traen fecha, sha 82bf33d y comando. Ninguna línea pegada viene de una corrida anterior.
- **F-08 (AMARILLO), cerrado.** Las cabeceras de `gradle-kubernetes.txt` e `imagen-init.txt` traen ahora `; echo "salida=$?"`. La de `imagen-init.txt` encadena además el `docker inspect`, y su salida aparece después de `salida=0`, como corresponde.

## Hallazgos

### F-09 · AMARILLO · bitacoras/U05-T10.md:61-67 · La nota de la ronda 3 parafrasea el comando y su fecha no cuadra con la evidencia
- La línea 64, `$ grep -c BUILD\ SUCCESSFUL y grep -c salida=0 sobre summary.md`, describe el comando en vez de escribirlo tal como se corrió.
- La línea 67 resume la evidencia («ver evidencia/») en vez de citarla.
- La línea 61 fecha la regeneración a las 19:09:02. Los archivos de evidencia dicen 19:08:13 y 19:08:27.

No afecta a ningún criterio: el resumen y la evidencia versionada son correctos y los reproduje. Es la misma clase de imprecisión que F-08, pero en la bitácora, que no es el artefacto entregable.

Nota sobre CA-3 en la bitácora: anota c923dfe y 484 inserciones «antes del commit de esta nota». El valor actual es 8c5e713 con 500 inserciones, y la diferencia son las 16 líneas de la propia nota. Lo acepto con el mismo criterio que en la ronda 2: un commit no puede contener su propio sha.

## Tareas candidatas (defectos reales fuera de alcance)
- Ninguna nueva.

## Rutas de transcripciones largas
Todas están en mi scratchpad, fuera del worktree:
- /tmp/claude-1000/-home-julian-Escritorio-Proyectos-SynKra/76af36c7-fa86-447c-9ae5-76b2a7831e37/scratchpad/gradle-r3.txt
- /tmp/claude-1000/-home-julian-Escritorio-Proyectos-SynKra/76af36c7-fa86-447c-9ae5-76b2a7831e37/scratchpad/conteo-r3.txt
- /tmp/claude-1000/-home-julian-Escritorio-Proyectos-SynKra/76af36c7-fa86-447c-9ae5-76b2a7831e37/scratchpad/init-r3.txt
- /tmp/claude-1000/-home-julian-Escritorio-Proyectos-SynKra/76af36c7-fa86-447c-9ae5-76b2a7831e37/scratchpad/docker-r3.txt

No escribí `revisiones/U05-T10/ronda-3.md` porque mis herramientas son de solo lectura. Este texto es el informe de la ronda y lo versiona el orquestador.

```
VEREDICTO: VERDE
AMARILLO|bitacoras/U05-T10.md:61-67|Nota de r3 con el comando de CA-1 parafraseado y una hora (19:09:02) distinta de la de la evidencia (19:08:13 y 19:08:27)
INFORME: revisiones/U05-T10/ronda-3.md
```
