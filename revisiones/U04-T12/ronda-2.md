# Ronda 2 — U04-T12

VEREDICTO: VERDE

Corrí yo mismo los cuatro criterios, frescos, sobre `cd90ebd`, y los cuatro pasan. Los seis hallazgos de la ronda 1 están resueltos. Solo quedan tres AMARILLO, que no bloquean.

Comprobaciones previas:
- **Tarea:** el hash del archivo es `dea3f10290551b3bf45e41cb43c04e150c68b7ad` y coincide.
- **Rama:** HEAD está en `cd90ebd`, tres commits sobre `origin/main`.
- **Código:** el diff no toca `backend/`, `verificador/` ni los archivos de build (`git diff --name-only … -- backend verificador build.gradle.kts settings.gradle.kts` sale vacío). Por eso es válido reutilizar la cola de CA-1 de la ronda 1 en el resumen.
- **Limpieza al terminar:** el worktree está limpio (`git status --short` vacío) y el puerto 18080 está libre. No queda ningún proceso `backend-0.1.0.jar`. Borré los dos directorios `mktemp` que dejó mi propia corrida de CA-2. En el scratchpad no quedan archivos de más de 1 MB.

## Criterios de aceptación, verificados por mí
| # | Criterio | Comando que corrí | Resultado |
|---|---|---|---|
| 1 | Pruebas del catálogo | `./gradlew :backend:test --rerun --tests 'io.synkra.catalogo.*' 2>&1 \| tail -6` y el bucle de conteo del resumen sobre los XML | pasa: `BUILD SUCCESSFUL in 14s`. Hay 13 XML con timestamp 2026-11-27T19:01 y suman 259 pruebas con `skipped=0 failures=0 errors=0`; cada clase coincide con `summary.md:64-76`. Mi cola dice `1 executed, 7 up-to-date` y la del resumen `8 executed`: es la caché de compilación, no un defecto. 14 s con `--rerun` es un tiempo coherente con una suite que sí corrió |
| 2 | `curl` de H-01 a H-06 y H-15 | el script copiado del resumen con `awk` entre las cercas `bash`, sin editar, después de `:backend:bootJar` y en el puerto 18080 | pasa, con rc=0. La salida es línea a línea igual a `summary.md:161-188`, salvo el valor del hash (`3060…` frente a `8f11…`). Esa diferencia es esperable porque `jar cf` guarda la fecha de los archivos. H-01: `201`, `[]`, `201`, `['2.1.0', '3.0.0']`, `409` y las dos filas de SQLite. H-02: hashes iguales, `409` y `200`. H-05/H-06: `1` y `['kyc-co', 'kyc-pe']`. H-15: hashes iguales y `404`. H-03: 422 `RANGO_INVALIDO` y 422 `CORE_EQUIVOCADO`. H-04: 422 `RUTA_NO_PERMITIDA` y 413 `DEMASIADO_GRANDE`, con 0 diferencias en `SYNKRA_DATA`, 0 archivos en `.tmp` y 0 `etc/x` fuera. El puerto quedó libre |
| 3 | Resumen completo | `grep -c '^\| U04-T' …summary.md` y `grep -o 'C-[0-9]*' CANDIDATAS.md \| sort -u \| wc -l`, más los cuatro comandos de extracción literales de `summary.md:209-262,306` | pasa. Hay 13 filas de tareas y 66 candidatas en total. Las 36 de U04 (C-31 a C-66) salen iguales y la comprobación `FALTA` solo imprime `fin`. El bucle de «Trabajada en» da la misma salida que `summary.md:217-252`, salvo un espacio final en `C-66:`, que el resumen no trae. Las 9 de otra unidad (C-04 … C-20) también coinciden |
| 4 | Diff acotado | `git diff --stat origin/main...HEAD` | pasa. El diff toca solo `summary.md` (314), `bitacoras/U04-T12.md` (196), `revisiones/U04-T12/ronda-1.md` (68) y `ronda-1-respuesta.md` (6): 4 archivos y 584 inserciones, todo dentro del alcance |

Además de los criterios, comparé, para cada una de las 36 filas, las columnas «Salió de» y «Decisión» del resumen con las columnas 4 y 6 de `CANDIDATAS.md`, recortando los espacios de los extremos. Las 36 son idénticas, así que la «Decisión» es literal de verdad, como pide C-21. `CANDIDATAS.md` no está en el diff.

## Seguimiento de la ronda 1
- **F-01 (ROJO), resuelto:** la bitácora (líneas 183-196) trae CA-4 con el aviso de que no se cuenta a sí mismo. Las cuentas cuadran: 181 + 15 líneas del bloque = 196, y 569 + 15 = 584, que es lo que obtuve yo.
- **F-02 (NARANJA), resuelto:** el resumen ya no tiene la columna «Estado». Trae la «Decisión» literal y la columna «Trabajada en», que sale de un comando. Además dice que una mención no prueba que la candidata se resolviera.
- **F-03 (NARANJA), resuelto en el resumen:** los comandos están escritos literalmente y los volví a correr. En la bitácora queda un resto (A-02).
- **F-04, F-05 y F-06 (AMARILLO), resueltos:**
  - **F-04:** H-03 y H-04 usan los casos de `stories.md:43-60`, y la lectura de vuelta cubre todo `SYNKRA_DATA`.
  - **F-05:** el script termina con rc=0.
  - **F-06:** el título de la sección ya corresponde a su contenido.

## Hallazgos
### A-01 · AMARILLO · summary.md:89 · El script deja dos directorios `mktemp` en `/tmp` en cada corrida
`SYNKRA_DATA` y `W` nunca se borran. Solo se borra `grande.jar`. En `/tmp` encontré seis directorios de las corridas del codificador de hoy (de 13:53 a 13:59, unos 50 KB cada uno), más los dos míos, que ya borré. Arreglo propuesto: un `trap 'rm -rf "$SYNKRA_DATA" "$W"' EXIT` al principio del script.

### A-02 · AMARILLO · bitacoras/U04-T12.md:138-181 · Los comandos de CA-3 en la bitácora no se pueden copiar tal cual
- El `tr '\n' ' '` quedó partido en dos líneas: el `\n` se expandió al pegarlo. En bash sigue funcionando, pero no es el texto que se corrió.
- La última comprobación sigue abreviada: `for c in $(awk ... U04-T ...)`.

El resumen sí los trae literales, en `summary.md:209` y `summary.md:260`, así que la evidencia existe y la verifiqué. Es un defecto de transcripción, no una afirmación sin verificar.

### A-03 · AMARILLO · summary.md:252 · La salida pegada no es byte a byte la del comando
El comando imprime `C-66: ` con un espacio final y el resumen pone `C-66:`. Es cosmético, pero hace que un `diff` contra una corrida fresca falle por una línea.

## Tareas candidatas (defectos reales fuera de alcance)
- **Lectura de vuelta de H-02:** `stories.md` (líneas 40-41) la pide sobre el detalle, con `GET …/plugins/kyc-pe/1.3.0 | jq -r .sha256`. El script toma el `sha256` de la respuesta del POST y del GET solo mira el código 200. La tarea remite a la CA-2 de U04-T07, así que no es un defecto de T12. Si el orquestador quiere ceñirse a la historia, va en la tarea de pulido de U04.
- **Cuerpo del 413:** no nombra el límite configurado (`detalles: []`), y H-04 dice «413 con el límite configurado». El resumen ya lo registra en `summary.md:202`. Toca código, así que va en la tarea de pulido de U04.

## Rutas de transcripciones largas
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/5b13ba94-befc-454f-88e7-3182922701e5/scratchpad/r2-ca1.txt
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/5b13ba94-befc-454f-88e7-3182922701e5/scratchpad/r2-ca2.sh
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/5b13ba94-befc-454f-88e7-3182922701e5/scratchpad/r2-ca2-out.txt
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/5b13ba94-befc-454f-88e7-3182922701e5/scratchpad/r2-trab.txt

```
VEREDICTO: VERDE
AMARILLO|summary.md:89|El script deja los directorios mktemp de SYNKRA_DATA y W en /tmp
AMARILLO|bitacoras/U04-T12.md:138-181|Comandos de CA-3 en la bitácora con el tr partido y un awk abreviado
AMARILLO|summary.md:252|La salida pegada pierde el espacio final de C-66
INFORME: revisiones/U04-T12/ronda-2.md
```
