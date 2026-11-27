# Ronda 2 — U04-T06

VEREDICTO: VERDE

Corrí yo, con `--rerun`, los cuatro criterios en HEAD `33a0fd8`, y los cuatro pasan. Los cinco hallazgos de la ronda 1 (F-01 a F-05) quedan cerrados, cada uno comprobado con mis propias mutaciones o sondas. El enlace duro no introduce ninguna regresión en el temporal, los permisos ni las rutas. Acepto la desviación de la decisión 6, porque el no-reemplazo y la atomicidad quedan atados. No queda ningún ROJO ni NARANJA; queda un AMARILLO nuevo. Fusiona el humano.

Este informe no lo escribí en el worktree, porque no lo edito. El contenido va aquí, para `revisiones/U04-T06/ronda-2.md`.

## Comprobaciones previas
- **Hash de la tarea:** `9397b8ad71b341398dc53a55129451c873179afa`. Coincide.
- **Commits desde la ronda 1:**
  - `017800d`: el informe;
  - `1933324`: el código;
  - `d53bad8`: la respuesta y la bitácora;
  - `33a0fd8`: CA-4 en la bitácora.
- **`revisiones/U04-T06/ronda-1.md`:** no cambió desde `017800d`.
- **Worktree:** `git status --short` salió vacío antes y después.
- **Sondas y mutaciones:** se hicieron en copias `git archive HEAD` del scratchpad.

## Criterios de aceptación, verificados por mí
| # | Criterio | Comando que corrí | Resultado |
|---|---|---|---|
| 1 | R-C4 y R-C7 a R-C10 | `./gradlew :backend:test --rerun --tests '*SubirPluginTest'` + `grep` del XML | pasa: `BUILD SUCCESSFUL` y `tests="50" skipped="0" failures="0" errors="0"`. El XML es de mi corrida (`timestamp` 14:12:26Z). |
| 2 | Descriptor leído con U01 | los dos `grep` | pasa: la llamada está en `SubidaPlugin.java:104` y la segunda línea imprime `sin copia`. |
| 3 | Backend y fronteras | `./gradlew :backend:test --rerun :backend:check :verificador:check` + bucle | pasa: `BUILD SUCCESSFUL`. Los 16 XML del backend y los 12 de `verificador` dan 0 saltos, 0 fallos y 0 errores. `InterfazCatalogoTest` da 3/3 y `FronterasModulosTest` (Modulith) 3/3. |
| 4 | Diff acotado | `git diff --stat origin/main...HEAD` | pasa: 12 archivos. El código y las pruebas están bajo `catalogo/**`, y el resto son la bitácora y `revisiones/U04-T06/` (el informe y la respuesta). |

## Los hallazgos de la ronda 1
- **F-01: cerrado.** M23, la copia del patrón con `-?`, ahora **cae**. La prueba paramétrica compara el resultado de `subirPlugin` con `Descriptor.NOMBRE`.
- **F-02: cerrado.**
  - Repetí con el código nuevo la sonda de dos hilos con barrera, con **pool 4**: 40 rondas, con 40 éxitos y 40 respuestas 409, y en ninguna el sha256 de la fila difiere del del disco (`sonda-r2-pool4.log`).
  - En la ronda 1, ese mismo cambio daba una fila sin archivo. La exclusión ya no depende del pool.
  - Las mutaciones que lo atan caen todas: N03 (marcar `colocado` antes de colocar), N04 (el destino ocupado da 500), N11 y N12.
- **F-03: cerrado.** En las cinco inyecciones de fallo quedan 0 filas, nada en `.tmp` y ningún directorio propio: solo `.tmp/`. Las inyecciones son antes del enlace, después del enlace, en el insert del plugin, en el de la etiqueta y en el commit. Un estorbo que ya existía queda intacto. N05, N06, N07 y N14 caen.
- **F-04: cerrado.** N01 (`move` con `REPLACE_EXISTING`) y N02 (`move` con `ATOMIC_MOVE`) caen.
- **F-05: cerrado.** Un mapa nulo sube sin error, con 2 filas. N09 cae.

## La desviación de la decisión 6 (`createLink` en vez de `ATOMIC_MOVE`)
La acepto, por tres razones:
- **El enlace no reemplaza:** N01 y N02 caen.
- **Es atómico:** el nombre aparece de una vez, sobre un contenido ya escrito y con su hash ya calculado.
- **El temporal no queda:** después del éxito, el archivo final tiene `nlink=1` y `.tmp` está vacío. N10 cae.

## Regresiones que busqué con el enlace duro (`sonda-r2.log`)
- **Permisos:** el archivo final queda en `rw-------`, igual que con el `move`, porque es el mismo inodo.
- **Enlaces simbólicos:**
  - `<core>` o `<core>/<nombre>` como enlace simbólico a un directorio de fuera dan 500, y fuera no se escribe nada.
  - Un destino que es un enlace simbólico colgante da 409.
- **Rutas hostiles:** los nombres y versiones de la ronda 1 siguen dando todos 422, antes de llegar a la ruta.
- **Otra versión:** subir la 1.1.0 no pisa la 1.0.0.
- **C-44:** sin cambios respecto de la ronda 1.

## Hallazgos
### F-06 · AMARILLO · `SubidaPlugin.java`, en `colocar` · la protección contra directorios que son enlaces simbólicos no está atada
- **N08** (`isDirectory` sin `NOFOLLOW_LINKS`) y **N13** (quitar la comprobación de que es un directorio) **sobreviven**.
- Con cualquiera de los dos, si `<core>` fuera un enlace simbólico a otro sitio, el JAR se escribiría fuera de la raíz. `rutaFinal` no lo impide, porque `normalize().startsWith` es una comprobación léxica.
- El código actual sí lo rechaza: mi sonda da 500 y no escribe nada fuera. Pero ninguna prueba lo fija.
- El riesgo es bajo, porque hace falta acceso al sistema de archivos de `$SYNKRA_DATA`. Una prueba con `Files.createSymbolicLink` lo ataría.

## Mutaciones (55 mutantes, `mutaciones-r2.txt`)
- **Caen 51:** los 40 de la ronda 1 que siguen aplicando, entre ellos M23, y 11 de los 15 nuevos, de N01 a N15.
- **Sobreviven 4:**
  - N08 y N13 (F-06);
  - M42, que es equivalente, como en la ronda 1;
  - N15, que solo cambia si una `DirectoryNotEmptyException` se adjunta como suprimida. Es equivalente, porque no se observa desde fuera.

## Tareas candidatas (defectos reales fuera de alcance)
- **Archivo huérfano sin fila, tras una caída entre el enlace y el commit.**
  - Mi sonda lo confirma: esa versión da 409 «ya tiene la versión» para siempre, mientras que `obtenerPlugin` da 404.
  - El codificador ya lo declara. Hace falta una reconciliación de disco contra la base.
- **Un fallo al borrar el temporal después de un commit con éxito** sale como `IOException`, porque viene del `finally`, aunque el plugin ya quedó guardado. Es un caso raro.
- **Siguen las de la ronda 1:**
  - `a/` junto a `a` (U07, con C-46);
  - los nombres que solo difieren en mayúsculas;
  - los valores de etiqueta sin tope de longitud (U04-T07);
  - C-13.

## Rutas de transcripciones largas
- mutaciones-r2.txt, scripts/mut2.py, sonda-r2.log, sonda-r2-pool4.log y SondaRevisorTest.java, en el scratchpad de la sesión.

```
VEREDICTO: VERDE
AMARILLO|SubidaPlugin.java (colocar)|la protección contra directorios que son enlaces simbólicos no está atada (N08 y N13 sobreviven)
INFORME: revisiones/U04-T06/ronda-2.md
```
