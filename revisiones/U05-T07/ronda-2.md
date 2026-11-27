# Ronda 2 — U05-T07

VEREDICTO: VERDE

Corrí yo mismo los tres criterios y los tres pasan. No queda ningún ROJO ni NARANJA. Hay cinco AMARILLOS que no bloquean.

**Contexto de la revisión:**
- **Tarea:** el archivo de `orq/U05-U02-tareas` tiene el hash `e9b1766c67def2ad49a3efdc0505c9bfdb164019`, el mismo del despacho.
- **Worktree:** `/home/julian/Escritorio/Proyectos/wt-m6r-U05-T07`, con HEAD en `72b0abf`.
- **Diff revisado:** `5e7a45e...72b0abf`.
- **Entorno:** `/bin/sh` es dash. No usé `kubectl` ni ningún clúster.
- **Estado final:** al terminar, `git status --short` sale vacío, así que el worktree quedó limpio.

## Criterios de aceptación, verificados por mí
| # | Criterio | Comando que corrí | Resultado |
|---|---|---|---|
| 1a | sintaxis | `sh -n init-descarga/descargar.sh && echo sintaxis-ok` | pasa: `sintaxis-ok` |
| 1b | shellcheck | `command -v shellcheck >/dev/null && shellcheck -s sh ...; echo "shellcheck=$?"` | `shellcheck=1` porque shellcheck no está instalado, y la bitácora lo dice. El criterio lo permite, pero esta mitad no se ha corrido nunca |
| 1c | test.sh | `init-descarga/test.sh; echo "salida=$?"` | pasa: 27 casos `ok`, ninguna FALLA, `salida=0`, 0,81 s |
| 2 | el rojo muerde | mutantes en mi scratchpad, cada uno con `DESCARGAR=<mutante> sh test.sh` | pasa: el mutante sin sha256 hace fallar `hash-alterado` y `segundo-alterado` |
| 3 | diff acotado y modos | `git rev-parse --short HEAD; git diff --stat 5e7a45e...HEAD; git ls-files -s init-descarga` | pasa: `72b0abf`, 13 archivos |

**Detalle de CA-3:**
- Los 13 archivos son los dos scripts, la bitácora, `bitacoras/U05-T07-evidencia/*` (que tiene precedente aceptado en la ronda 1) y `revisiones/U05-T07/`.
- Los dos scripts están en `100755`.
- Nada toca `backend/`, ningún Dockerfile ni ningún contexto de clúster.

**Por qué la suite tarda 0,81 s:**
- Todos los casos corren con `SYNKRA_ESPERA_BASE=0`.
- `espera-creciente` usa un `sleep` falso.
- test.sh no tiene ningún mecanismo para omitir casos.
- La única espera real es el sondeo del puerto (`sleep 0.1`, con condición y un tope de 100 vueltas).

**Mutantes que corrí** (salida en `/tmp/claude-1000/-home-julian-Escritorio-Proyectos-SynKra/76af36c7-fa86-447c-9ae5-76b2a7831e37/scratchpad/r2/mutantes.txt`):

| Mutante | Qué falla | ¿Muere? |
|---|---|---|
| sin sha256 | `hash-alterado`, `segundo-alterado` | sí |
| espera fija | `espera-creciente` (`esperas=1 1 1 1`) | sí |
| 4 intentos | `catalogo-caido`, `http-500`, `espera-creciente` | sí |
| sin `mktemp` (vuelve el `.nombre.parcial` fijo) | `enlace-en-temporal-antiguo` (`victima=uno`) | sí |
| sin las comprobaciones de `destino_seguro` | `enlace-en-destino`, `enlace-a-archivo-en-destino` | sí |
| sin la comprobación de que `$PLUGINS` sea un enlace | `plugins-es-enlace` | sí |
| sin `chmod 644` | nada | sobrevive (ver F-04) |

Las llamadas a `destino_seguro` son redundantes a propósito, como defensa en profundidad: quitar una sola no hace fallar ningún caso, quitarlas todas sí.

**Cierre de los hallazgos de la ronda 1:**
- **F-01 y F-02:** cerrados, con un caso en test.sh que cae cuando se quita la corrección.
- **F-03, F-04 y F-05:** cerrados según el arbitraje de `aidlc-docs/audit.md:900-915` en `orq/U05-U02-tareas`. Ahí está ratificado saltar las líneas vacías y aceptado reemplazar un archivo regular previo.
- **F-06 y F-07:** cerrados.
- **Evidencia fresca:** mi salida de test.sh coincide línea por línea con `r2-verde.txt`. `r2-rojo.txt` muestra las 7 FALLA que se declaran.

## Hallazgos

### F-01 · AMARILLO · init-descarga/test.sh:194-205 · `espera-creciente` solo funciona si `sleep` es un programa externo
Si corro la suite con busybox como `sh` y como herramientas (`PATH=<bb>:/usr/bin sh test.sh`), `espera-creciente` falla con `esperas=` vacío. La causa es que el ash de busybox ejecuta `sleep` como orden interna (`type sleep` → `sleep is sleep`), así que nunca llega al `sleep` falso del `PATH`. El script en sí funciona bien bajo busybox. El fallo está solo en la prueba, y la tarea deja que test.sh corra en `sh` o `bash`, así que no bloquea. Aun así, en la ronda 1 la suite entera pasaba con busybox, así que esto es una regresión de portabilidad de la prueba. Arreglos posibles:
- correr ese caso con `dash` o `bash` explícitos;
- o leer las esperas del log del script (`reintento en Ns`).

Salida en `/tmp/claude-1000/-home-julian-Escritorio-Proyectos-SynKra/76af36c7-fa86-447c-9ae5-76b2a7831e37/scratchpad/r2/busybox.txt`.

### F-02 · AMARILLO · descargar.sh:12 y :21-26 · Hay variables numéricas que no se validan bien
- **`SYNKRA_ESPERA_BASE=08`** pasa la validación, porque solo son dígitos. Pero el script muere después en la línea 109 con `Illegal number: 08`, porque dash lee el cero inicial como octal. Lo que queda de F-04 de la ronda 1 son los ceros a la izquierda.
- **`SYNKRA_CURL_MAX_TIME=abc`** no se valida. curl rechaza la opción, el script la reintenta 5 veces y termina con `catálogo no responde tras 5 intentos`. El mensaje es engañoso, porque el problema es la configuración y no la red.

Un arreglo para los dos: validar ambas variables como enteros decimales al arrancar y salir con código 2.

### F-03 · AMARILLO · bitacoras/U05-T07-evidencia/r2-mutantes.txt · La evidencia de mutantes no trae el código de salida
Los tres bloques terminan en `salida=?` y no en el código real. Las líneas FALLA bastan para el criterio, pero el registro queda incompleto.

### F-04 · AMARILLO · descargar.sh:120 · Ninguna prueba cubre el `chmod 644`
`mktemp` crea el temporal con permisos 0600. Si el init container y el core corren con usuarios distintos, el `chmod` es lo único que permite al core leer el JAR. Quitar esa línea no hace fallar ningún caso. Basta con que `hash-correcto` compruebe el modo del JAR que queda en plugins.

### F-05 · AMARILLO · test.sh:281-283 · `set-eu-presente` busca el texto, no prueba el comportamiento
Es un `grep` de la línea `set -eu`. La respuesta del codificador ya lo admite y no hay otra forma razonable de probarlo, así que solo lo dejo anotado.

## Tareas candidatas (defectos reales fuera de alcance)
- Siguen en pie las dos de la ronda 1:
  - montar `/plugins` en el core como `readOnly` (U05-T03 y U05-T08);
  - instalar shellcheck en el entorno o en la CI para que la mitad 1b de CA-1 deje de ser opcional.
- Al decidir la imagen de U05-T08, conviene comprobar que el usuario del init container y el del core puedan leer los JAR. Eso mismo justifica F-04.

## Rutas de transcripciones largas
- `/tmp/claude-1000/-home-julian-Escritorio-Proyectos-SynKra/76af36c7-fa86-447c-9ae5-76b2a7831e37/scratchpad/r2/mutantes.txt`
- `/tmp/claude-1000/-home-julian-Escritorio-Proyectos-SynKra/76af36c7-fa86-447c-9ae5-76b2a7831e37/scratchpad/r2/busybox.txt`
- `/tmp/claude-1000/-home-julian-Escritorio-Proyectos-SynKra/76af36c7-fa86-447c-9ae5-76b2a7831e37/scratchpad/r2/mut/`, con las copias de cada mutante

No escribí el archivo `revisiones/U05-T07/ronda-2.md`, porque mis herramientas son de solo lectura. Este mensaje es el informe que hay que guardar en esa ruta.

```
VEREDICTO: VERDE
AMARILLO|init-descarga/test.sh:194-205|espera-creciente depende de que sleep sea un programa externo y falla con busybox ash
INFORME: revisiones/U05-T07/ronda-2.md
```
