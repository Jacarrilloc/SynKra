# Ronda 1 — U07-G

VEREDICTO: NO-VERDE

Las fronteras están bien: las tres pruebas nuevas detectan las siete mutaciones y los dos pasos del rojo. El veredicto es NO-VERDE por el resumen: hay tres hallazgos NARANJA. El hash de la tarea coincide (`8f146133…`). El worktree estaba limpio antes y después (`git status --porcelain | wc -l` = 0). El último commit que toca `backend` es `ba0b1fd`. Corrí todo yo, con `JAVA_HOME=~/.gradle/jdks/eclipse_adoptium-21-amd64-linux.2`.

## Criterios de aceptación, verificados por mí
| # | Criterio | Comando que corrí | Resultado |
|---|---|---|---|
| 1 | Fronteras de `despliegue` | los comandos literales de CA-1 | pasa: `BUILD SUCCESSFUL`, `tests="15" skipped="0" failures="0" errors="0"`, las 12 pruebas previas más las 3 nuevas, `1` y `1`, `imports-ajenos=1`, `fabric8=1`, `inverso=1`, `publicas=1`, `api=2` |
| 2 | Cada regla muerde | mi propio `mut.sh` (sed distinto del suyo, `git archive` + `git init -q`, `--rerun --no-build-cache`), una copia de `ba0b1fd` por mutación | pasa, las 7 con `BUILD FAILED`: interno de registro, de kubernetes y de catalogo caen en `elBackendReal…` y `despliegueSoloUsaLas…`; fabric8 en `soloElModuloKubernetes…` y `despliegueSoloUsaLas…`; módulo nuevo en `elBackendReal…`, `despliegueSoloDepende…` y `despliegueSoloUsaLas…`; inverso en `ningunModulo…` y `elBackendReal…`; allowed solo en `elBackendReal…` |
| 2b | Rojo en dos pasos | `mut.sh registro 2f47340` y `mut.sh registro 6a97c2b` | pasa. En 2f47340 da `BUILD SUCCESSFUL` con `tests="12"`: el hueco. En 6a97c2b da `BUILD FAILED` y cae solo `despliegueSoloUsaLasApisDeOtrosModulos`. 6a97c2b solo cambia `FronterasModulosTest` (+40); ba0b1fd cambia `package-info.java` (+3) |
| 3 | Secciones del resumen | CA-3 literal | pasa: 9 encabezados en orden, cada uno 1; historias, candidatas y reglas al menos 1; los 6 términos; `David\|Julian` = 0 |
| 4 | Salidas frescas | CA-4 literal, más el conteo por clase | pasa: `BUILD SUCCESSFUL`, `salida=0` en las dos corridas, 3 y 3 en el resumen, cabeceras con fecha, sha `ba0b1fd`, `JAVA_HOME` y comando. Los conteos por clase coinciden con los suyos (ver F-01) |
| 5 | Cifras con fuente | `bash bitacoras/U07-G-evidencia/ca5.sh` | pasa: ningún `FALTA`, al menos un `ok` en cada `revisiones/U07-A..F` |
| 6 | Copias literales | `bash bitacoras/U07-G-evidencia/ca6.sh` | pasa: el bucle no imprime nada, `[...]` = 0 |
| 7 | Todo el backend | CA-7 literal | pasa: `BUILD SUCCESSFUL in 48s`, 60 XML, 0 con skipped, failures o errors distinto de 0, 623 pruebas, `requiresNew=1` |
| 8 | Diff acotado | CA-8 contra 2f47340 | pasa en lo esencial: `fuera=1`, `0` y `0`. `FronterasModulosTest` solo tiene líneas añadidas, y el import va intercalado. El `wc -l` anotado es 3, no 2 (F-05) |

Corrí también la corrida 2 (FronterasModulosTest 15, ContextoEventos 3, EstadoRollout 6, ReintentosCliente 3, RelojTest 1, AplicandoTest 1). Repetí el `git diff --name-status 39a242c...555acad` y salen las mismas 4 clases. Recalculé el diff de cada fusión fuera de `despliegue`, y coincide con `datos.txt`.

## Hallazgos

### F-01 · NARANJA · `summary.md:142` y `evidencia/conteo-por-clase-despliegue.txt` · El filtro `*despliegue*` sí toma pruebas de `FronterasModulosTest`
El resumen dice: «El filtro `*despliegue*` … **No** toma `FronterasModulosTest`». Es falso desde esta misma tarea. Gradle aplica el patrón también a los nombres de método, y dos pruebas nuevas empiezan por `despliegue…`. Tras `./gradlew :backend:test --rerun --tests '*despliegue*'`, el árbol trae `TEST-io.synkra.FronterasModulosTest.xml` con `tests="2"`: `despliegueSoloDependeDeSusModulosPermitidos()` y `despliegueSoloUsaLasApisDeOtrosModulos()`. `ningunModuloDependeDeDespliegue` no entra porque lleva `D` mayúscula.
- La corrida 1 ejecuta 100 pruebas, pero el conteo por clase pegado, con el glob `TEST-*despliegue*.xml`, muestra 98.
- El resumen afirma algo que no comprobó y presenta un conteo incompleto de su propia corrida.
- Hay que corregir la frase y anotar las 2 de `FronterasModulosTest` en la corrida 1.
- Mi transcripción está en `ca4-despliegue.txt`.

### F-02 · NARANJA · `summary.md:261` y `summary.md:306` · Citas que no salen así de la fuente que nombran
Comprobé con `grep -F` unas 27 citas del resumen contra su fuente. Dos fallan:
- **Desviación 23.** «ConfirmacionServicio sin cambios: lo acepto» se atribuye a `revisiones/U07-F/ronda-3.md`, que no la contiene (`grep -c` da 0). Está en `revisiones/U07-F/ronda-1.md:35`. Es una aceptación atribuida a un archivo que no la dice.
- **C-87.** Se cita «queda cerrada por construcción en ese camino»; `bitacoras/U07-D.md:43` dice «en este camino». La cita no es literal.

Las demás citas casan, salvo diferencias de negrita en Markdown (R-D9 y C-35), que no cuento.

### F-03 · NARANJA · `summary.md:302-312` · Las «decididas» no llevan su «Decisión» tal como está
La decisión 14 pide, para cada candidata de U07, en pocas palabras qué es, su columna «Decisión» tal como está y, si existe, la frase de la bitácora. En «Decididas en U07» falta la columna «Decisión» de C-30, C-87, C-76, C-82, C-83, C-108, C-109, C-115, C-116, C-107 y C-122. C-35 y C-114 la traen parafraseada o a medias.

Además, C-122 está bajo «según su bitácora o revisión», pero su única fuente es la propia `CANDIDATAS.md`, que es su Decisión y no constancia de que se resolviera. Va con las abiertas o con las relacionadas.

### F-04 · AMARILLO · `evidencia/datos.txt` y `bitacoras/U07-G-evidencia/datos.sh` · La línea `$ …` no es el comando que corrió
- `datos.sh` imprime `-- backend/src ':!despliegue (main y test)'`, que es una etiqueta y no un pathspec. Lo que de verdad corre excluye también `FronterasModulosTest.java`.
- Imprime `$ grep -rn Sharedmodules`, pero corre `grep -n 'sharedModules' …SynkraApp.java`.

Las salidas son correctas (las recalculé), pero el comando anotado no se puede reproducir tal cual.

### F-05 · AMARILLO · CA-8 · Cuenta de `git status` y commit posterior
- `ca8.txt` da `3`, no los 2 que pide la tarea: la bitácora estaba editada al generarlo.
- En b89c397 la bitácora decía «dio 2», que es falso. Lo corrigió el propio codificador en 3570249, un commit posterior al de `ca8.txt`, aunque solo de bitácora.
- No afecta a `fuera=1`, `0` y `0`, que reproduje.

### F-06 · AMARILLO · `summary.md:8` · Login de usuario en la ruta
`JAVA_HOME=/home/casdav/...` deja ver un login derivado de un nombre. La tarea pide anotar la ruta. Bastaría `$HOME/.gradle/jdks/...` para cumplir y no dejar rastro de persona. El grep de CA-3 da 0, así que no rompe el criterio.

## Sobre la cifra 852
El codificador tiene razón. `grep -rnF 852` sobre `revisiones/`, `bitacoras/`, `aidlc-docs/`, `tareas/` y `CANDIDATAS.md`, tanto en esta rama como en `wt-orq-U04`, solo encuentra trozos de hashes, semillas y marcas de tiempo, nunca un conteo de pruebas. Por la regla de que «la que no esté en ningún archivo no se escribe», no debía usarse. La cifra fresca es 623 pruebas en 60 XML, que reproduje.

## Otras comprobaciones que pasan
- Tareas, PR y rondas: por `ls` y las líneas `VEREDICTO:`, A tiene 1 ronda, B, C, D y E tienen 2 y F tiene 3, con los veredictos copiados bien. Los PR #96, #98, #100, #104, #105 y #106 aparecen en `gh pr list`.
- Las cifras de la tabla «Cifras y su fuente» están en su archivo.
- Comprobados contra su fuente:
  - `leerPais` en `PlanServicio.java:120` y `VigilanciaDespliegues.java:117`;
  - Fabric8 7.9.0;
  - las mutaciones 10 y 11 de U07-E;
  - F-02 y F-03 AMARILLO en U07-B y U07-D;
  - F-03 de U07-F, ratificada;
  - el crecimiento de `ConfirmarSinClusterTest`, `VencimientoPlanesTest`, `VigilanciaCoincideTest` y `VigilanciaDobleTest`;
  - las 4 propiedades nuevas.
- Las «Decisión» de la tabla de abiertas son subcadenas literales de `CANDIDATAS.md`.

## Tareas candidatas (defectos reales fuera de alcance)
- La plantilla de las tareas da por hecho que `--tests '*<módulo>*'` solo toma clases del paquete. Gradle también casa nombres de método, así que una prueba llamada `despliegue…` o `registro…` en otra clase entra en el filtro. Las plantillas deberían usar `'io.synkra.<módulo>.*'`. Va con C-122, que es sobre los `grep` de las plantillas.

## Rutas de transcripciones largas
Todo está en `/tmp/claude-1000/-home-casdav-Projects-Master-SynKra/9d987332-269f-4da9-9d66-0a5449231723/scratchpad/U07-G-r1/`:
- `ca1.txt`
- `ca2-ba0b1fd.txt` (las 7 mutaciones)
- `ca2-rojos.txt` (los dos pasos del rojo)
- `mut.sh` (mi guion de mutaciones)
- `ca3.txt`
- `ca4-despliegue.txt` (F-01)
- `ca7.txt`
- `corrida2.txt`
- `cand.txt`

```
VEREDICTO: NO-VERDE
NARANJA|summary.md:142, evidencia/conteo-por-clase-despliegue.txt|El filtro *despliegue* sí toma 2 pruebas de FronterasModulosTest; el resumen dice que no y el conteo omite 2 de 100
NARANJA|summary.md:261, summary.md:306|Aceptación de U07-F atribuida a ronda-3.md (está en ronda-1.md:35); cita de C-87 no literal
NARANJA|summary.md:302-312|Candidatas «decididas» sin su columna «Decisión» tal como está; C-122 con fuente CANDIDATAS.md bajo «según su bitácora»
AMARILLO|evidencia/datos.txt|Las líneas $ no son el comando ejecutado (pathspec y grep de sharedModules)
AMARILLO|CA-8|wc -l = 3 en lugar de 2; la bitácora dijo «dio 2» y se corrigió en un commit posterior
AMARILLO|summary.md:8|Login de usuario en la ruta de JAVA_HOME
INFORME: revisiones/U07-G/ronda-1.md
```
