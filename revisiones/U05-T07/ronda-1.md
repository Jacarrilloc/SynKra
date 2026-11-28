# Ronda 1 — U05-T07

VEREDICTO: NO-VERDE

El archivo de la tarea coincide (`git hash-object` = e9b1766c67def2ad49a3efdc0505c9bfdb164019). Revisé el worktree `/home/casdav/Projects/Master/wt-m6r-U05-T07`, HEAD dd8a95f, con el diff `5e7a45e...m6r/U05-T07`. Al terminar, `git status --short` sale vacío: el worktree está limpio. No usé `kubectl` en ningún momento.

## Criterios de aceptación, verificados por mí
| # | Criterio | Comando que corrí | Resultado |
|---|---|---|---|
| 1a | sintaxis | `sh -n init-descarga/descargar.sh && echo sintaxis-ok` (`/bin/sh` es dash) | pasa: `sintaxis-ok` |
| 1b | shellcheck | `command -v shellcheck >/dev/null && shellcheck -s sh ...; echo "shellcheck=$?"` | **sin correr**: shellcheck no está instalado (`shellcheck=1` sale de `command -v`). La bitácora lo dice. No lo cuento como pasado |
| 1c | test.sh | `init-descarga/test.sh; echo "salida=$?"` | pasa: 16 casos `ok`, `salida=0`, 0,65 s |
| 1c' | test.sh con busybox como `sh` y como coreutils (simula la imagen mínima de T08) | `PATH=<bb>:/usr/bin sh init-descarga/test.sh` | pasa: `todos los casos ok`, `salida=0` |
| 2 | el rojo muerde | mutante con `if false` en lugar de la comparación del sha256, en el scratchpad, con `DESCARGAR=<mutante> sh test.sh` | pasa: falla `hash-alterado` (cod=0, plugins=plugin-a-1.0.jar) y `segundo-alterado`, `salida=1` |
| 3 | diff acotado y modos | `git rev-parse --short HEAD; git diff --stat 5e7a45e...HEAD; git ls-files -s init-descarga` | pasa: dd8a95f. Son 7 archivos: los dos scripts, la bitácora y `bitacoras/U05-T07-evidencia/*`, que tiene precedente en T05 y T06. Los dos scripts están en `100755`. Nada toca `backend/`, el Dockerfile ni otro contexto |

Sobre el tiempo de 0,65 s: se explica porque `SYNKRA_ESPERA_BASE=0` pone todas las esperas en 0. No hay casos omitidos, porque test.sh no tiene omisión. La única espera es `sleep 0.1` dentro de un sondeo con condición y un tope de 100 vueltas, y eso es aceptable.

Comprobé a mano la espera creciente real, con base 1 y un puerto cerrado. Las marcas de tiempo salen en 0,0 / 1,0 / 3,0 / 7,0 / 15,0 s, son 5 intentos y termina con `catálogo no responde tras 5 intentos: <url>`. El comportamiento es correcto, pero ninguna prueba lo cubre (ver F-02).

`set -eu` está en la línea 6. Coincide con el contrato del Deployment: `COMANDO_INIT=/usr/local/bin/descargar.sh`, `VAR_URL=SYNKRA_URL`, `/plugins` y `/config`.

## Hallazgos

### F-01 · NARANJA · init-descarga/descargar.sh:66-71 y :95 · Escribe fuera de `/plugins` si hay enlaces previos dentro de `/plugins`
La validación de formato cierra bien `../`, las rutas absolutas y `/` dentro del nombre; los cuatro casos pasan y no hacen ninguna petición. Los enlaces no están cubiertos. El script escribe en `$PLUGINS/.<nombre>.parcial` con `curl --output`, que sigue enlaces, y después hace `mv -f` hacia `$PLUGINS/<nombre>`, que entra en un directorio si el destino es un enlace a uno. Lo probé en `rev-u05t07-r1/bordes/`:
- **Caso A**: `p/.a-1.jar.parcial` es un enlace a `fuera/victima` (que contiene «original»). Resultado: `rc=0`, `victima=uno`. Escribió el JAR fuera de plugins y dejó `a-1.jar` como enlace a ese archivo de fuera.
- **Caso B**: `p/a-1.jar` es un enlace al directorio `fuera/`. Resultado: `rc=0`, `ok a-1.jar`, y aparece `fuera/.a-1.jar.parcial`. El archivo quedó fuera de `/plugins` y en plugins solo hay un enlace. Además, el sha256 «verificado» describe un archivo que no está donde el core lo va a buscar.

Hace falta que alguien pueda escribir antes en `/plugins` (un emptyDir), así que el riesgo práctico es bajo. Aun así, el alcance dice «escribe solo en `/plugins`» y el despacho pide expresamente cubrir los enlaces. Una salida es negarse si el temporal o el destino ya existen o son enlaces (`[ -e ] || [ -L ]`), o crear el temporal con `mktemp` dentro de `$PLUGINS` y mover con un destino que no se resuelva como directorio. Hay que añadir un caso a test.sh.

### F-02 · NARANJA · init-descarga/test.sh (casos 3 y 3b) · Ninguna prueba cubre la espera creciente (decisión 5)
Las pruebas corren con `SYNKRA_ESPERA_BASE=0`, así que todas las esperas valen 0s y la secuencia 1, 2, 4, 8 nunca se comprueba. Mutante en el scratchpad: `espera=$ESPERA_BASE`, es decir, espera fija. `DESCARGAR=<mutante> sh test.sh` da `salida=0`, sin ninguna FALLA: **el mutante sobrevive**. Para cubrirlo sin esperas largas basta poner un `sleep` falso al frente del `PATH` que registre sus argumentos, correr con base 1 y exigir la secuencia `1 2 4 8`. Así no hay ningún `sleep` real.

### F-03 · AMARILLO · descargar.sh:50-51 · Decisión propia sobre líneas vacías: aceptable, pero sin prueba y sin ratificar
La decisión 3 dice «formato exacto; una línea que no cumple se rechaza». Una línea vacía no cumple el formato, así que saltarla se aparta de la letra. Mi juicio es que es razonable:
- `ConstructorObjetosPais.java:124-126` nunca emite líneas vacías. Una lista vacía es `""`, y un `"\n"` suelto también da 0 (caso D4).
- No abre ninguna vía de escritura.
- Es coherente: una línea de solo espacios sí da código 3 (D3), y un CRLF también da 3 (D2).

Dos cosas faltan. Ninguna prueba ejerce la línea vacía en medio de la lista; mi caso D (`\n<sha> a-1.jar\n\n`) da rc=0 con `a-1.jar`. Y que el orquestador ratifique la decisión en la tarea.

### F-04 · AMARILLO · descargar.sh:10,84 · `SYNKRA_ESPERA_BASE` sin validar
Con `SYNKRA_ESPERA_BASE=0.5` el script muere con `descargar.sh: 84: Illegal number: 0.5`. Sale con código distinto de 0 y la limpieza corre, pero el mensaje no es el «mensaje claro» de la tarea. Hay que validar que sea un entero al arrancar, con código 2.

### F-05 · AMARILLO · descargar.sh:95 y limpiar · Pisa y luego borra un JAR previo del mismo nombre
Caso C: con un `a-1.jar` previo y el segundo JAR alterado, el `a-1.jar` previo queda sobrescrito y después borrado; `otro.jar` sobrevive. En un emptyDir recién creado es inocuo, y lo que pide la tarea («los de esta corrida») se cumple. Lo anoto porque `/plugins` sí sobrevive a los reinicios del contenedor dentro del mismo pod.

### F-06 · AMARILLO · test.sh · Los mutantes de `set -eu` y de nombre duplicado no se distinguen
Quitar `set -eu` (mutante `sin-seteu`) deja `salida=0`. El script lo tiene y no hay variables sin valor por defecto, así que esto no bloquea. Solo deja constancia de que la suite no protege esa línea. Un nombre duplicado en la lista da rc=0 y un solo JAR (caso F), lo cual es aceptable.

### F-07 · AMARILLO · bitacoras/U05-T07.md:4 · Ruta del acuse inexacta
La bitácora registra el acuse desde `/home/user/wt-orq-U04`, pero la ruta real es `/home/casdav/Projects/Master/wt-orq-U04`. El hash coincide, así que no invalida nada. Aun así, una evidencia con una ruta que no existe en esta máquina conviene corregirla.

Sobre la frescura de la evidencia: `ca1.txt`, `ca2-mutantes.txt` y `rojo.txt` coinciden con mis ejecuciones. `ca3.txt` muestra 6 archivos y el sha 438a9a8, que es lo que había antes de confirmar el propio `ca3.txt`. Eso es coherente y no es evidencia vieja.

## Tareas candidatas (defectos reales fuera de alcance)
- U05-T08 debería montar `/plugins` en el core como `readOnly` y declarar el emptyDir solo para el init y el core. Eso reduce la superficie de F-01 y F-05 entre reinicios. Toca el Deployment de U05-T03 y la imagen de T08, no este diff.
- Instalar shellcheck en el entorno, o en la CI, para que la mitad de CA-1 que falta deje de ser opcional en las tareas de shell (T07 y T08).

## Rutas de transcripciones largas
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/5b13ba94-befc-454f-88e7-3182922701e5/scratchpad/rev-u05t07-r1/busybox.txt
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/5b13ba94-befc-454f-88e7-3182922701e5/scratchpad/rev-u05t07-r1/mut-espera-fija.txt
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/5b13ba94-befc-454f-88e7-3182922701e5/scratchpad/rev-u05t07-r1/mut-sinsha.txt
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/5b13ba94-befc-454f-88e7-3182922701e5/scratchpad/rev-u05t07-r1/mut-sin-seteu.txt
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/5b13ba94-befc-454f-88e7-3182922701e5/scratchpad/rev-u05t07-r1/mut-reintenta-siempre6.txt
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/5b13ba94-befc-454f-88e7-3182922701e5/scratchpad/rev-u05t07-r1/bordes/ (casos A a F, cada uno con su `log`)

Una nota: no borré nada en el scratchpad (el control de seguridad bloqueó un `rm -rf` sobre una ruta relativa). Cada caso de borde va en su propio directorio nuevo dentro de `bordes/`.

```
VEREDICTO: NO-VERDE
NARANJA|init-descarga/descargar.sh:66-71,95|Escribe fuera de /plugins si el temporal o el destino ya son enlaces
NARANJA|init-descarga/test.sh casos 3 y 3b|Ninguna prueba cubre la espera creciente 1-2-4-8 (el mutante de espera fija sobrevive)
INFORME: revisiones/U05-T07/ronda-1.md
```
