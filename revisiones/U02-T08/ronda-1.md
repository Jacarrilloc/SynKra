# Ronda 1: U02-T08

VEREDICTO: VERDE

Corrí los cuatro criterios yo mismo en `/home/julian/Escritorio/Proyectos/wt-m6r-U02-T08` (HEAD 2ec73a3, diff `9d84b4c..2ec73a3`). Los cuatro pasan. No queda ningún ROJO ni NARANJA; hay tres AMARILLO que no bloquean.

## Criterios de aceptación, verificados por mí
| # | Criterio | Comando que corrí | Resultado |
|---|---|---|---|
| 1 | El control pasa sobre el core | `./gradlew :core:test --rerun --tests '*CoreSinPaisesTest'`, el grep del XML, el `grep -rniE` de la tarea y `scripts/control-core-sin-paises.sh` | pasa: `BUILD SUCCESSFUL`, `tests="1" skipped="0" failures="0" errors="0"`, `grep=1`, `salida=0` |
| 2 | El control muerde | Dos copias `git archive HEAD` en `mktemp -d` (sin stash, borradas). (a) `"pe"` añadido en una clase de otro archivo que el del codificador, `VersionCore.java:44`. (b) `core/src/main/resources/colombia.yaml` | pasa: en ambos casos el script sale con 1 y la prueba da `BUILD FAILED`. Los mensajes nombran el archivo y la línea (`VersionCore.java:44: class PaisRevisor ...`) y el archivo (`resources/colombia.yaml: el nombre del archivo nombra un país`) |
| 3 | El resto del build sigue pasando | `./gradlew :verificador:test --rerun :verificador:check :plugin-api:test --rerun :plugin-api:check :core:test --rerun :core:check` y los conteos XML de los tres módulos | pasa: `BUILD SUCCESSFUL in 28s`. Hay 20 XML: core 54 pruebas, verificador 178, plugin-api 4. Todos con `skipped="0" failures="0" errors="0"` |
| 4 | El diff queda acotado | `git rev-parse --short HEAD; git diff --stat 9d84b4c...HEAD; git ls-files -s scripts/control-core-sin-paises.sh` | pasa: `2ec73a3`, tres archivos (bitácora, `CoreSinPaisesTest.java`, script), el script con `100755`. No toca `core/src/main`, `plugins/`, `backend/`, `verificador/` ni `plugin-api/` |

Sobre el tiempo de la prueba: `elCoreNoNombraPaises()` tarda 0.038 s. Esa prueba no depende de estado externo, solo recorre unos 13 archivos, y CA-2 muestra que de verdad falla cuando debe. No es una prueba saltada.

Sobre la evidencia de la bitácora: coincide con mis ejecuciones. El sha 5bf53d7 de CA-4 está anotado con honestidad: era el HEAD antes del commit de evidencia, y el código está en 94d846b. La bitácora también reconoce que la primera inyección de CA-2 no cambió nada y que la repitió.

## Hallazgos

### F-01 · AMARILLO · `scripts/control-core-sin-paises.sh:17` · El script y la prueba no buscan exactamente igual
Hice una tercera copia con siete inyecciones y comparé lo que reporta cada uno.
- Mayúsculas en la extensión: la prueba pasa el nombre a minúsculas y detecta `X5.YAML` con `"colombia"`. El script no lo detecta, porque `--include='*.yaml'` distingue mayúsculas.
- Configuración regional: con `LC_ALL=C`, el script deja de detectar `País = PE`, porque en ERE `[ií]` se lee byte a byte. La prueba sí lo detecta. Con la configuración por defecto de esta máquina el script lo detecta bien.

Las otras cinco inyecciones las detectan los dos, también con `LC_ALL=C`: `pais=pe` en `.properties`, `pais: co` en `.yml`, `'CO'`, `PERÚ` y `peru.txt` como nombre de archivo.

La tarea pide «el mismo control». Ninguno de los dos casos rompe un criterio, y Spring no carga `.YAML` en mayúsculas. Se arreglaría con `export LC_ALL=C.UTF-8` en el script y comparando las extensiones sin distinguir mayúsculas.

### F-02 · AMARILLO · `scripts/control-core-sin-paises.sh:17-23` · Un error de `grep` cuenta como «limpio»
`if grep ...` trata igual el código 2 (error, por ejemplo un archivo que no se puede leer) que el 1 (no encontró nada). Ante un error de lectura, el script imprimiría `OK` y saldría con 0. Lo saqué leyendo el código, no lo reproduje.

### F-03 · AMARILLO · `CoreSinPaisesTest.java:26` · `per[uú]` sin límite de palabra
El patrón también coincide con palabras que contienen «peru», como `superuser` o `Perusal`. Hoy no ocurre en el core, y la decisión 2 de la tarea dice que en ese caso se cambia la palabra. Lo anoto para que quien escriba código futuro en el core no se sorprenda.

## Tareas candidatas (defectos reales fuera de alcance)
- Ninguna.

## Estado del worktree
`git status --short` sale vacío al terminar. Las copias temporales de `/tmp/tmp.3v9kN7YCOE` quedaron borradas.

## Rutas de transcripciones largas
- /tmp/claude-1000/-home-julian-Escritorio-Proyectos-SynKra/76af36c7-fa86-447c-9ae5-76b2a7831e37/scratchpad/ca1.log
- /tmp/claude-1000/-home-julian-Escritorio-Proyectos-SynKra/76af36c7-fa86-447c-9ae5-76b2a7831e37/scratchpad/ca2-a.log
- /tmp/claude-1000/-home-julian-Escritorio-Proyectos-SynKra/76af36c7-fa86-447c-9ae5-76b2a7831e37/scratchpad/ca2-b.log
- /tmp/claude-1000/-home-julian-Escritorio-Proyectos-SynKra/76af36c7-fa86-447c-9ae5-76b2a7831e37/scratchpad/ca2-c.log
- /tmp/claude-1000/-home-julian-Escritorio-Proyectos-SynKra/76af36c7-fa86-447c-9ae5-76b2a7831e37/scratchpad/ca3.log

```
VEREDICTO: VERDE
AMARILLO|scripts/control-core-sin-paises.sh:17|El script y la prueba no buscan exactamente igual (.YAML en mayúsculas, LC_ALL=C con país)
AMARILLO|scripts/control-core-sin-paises.sh:17-23|Un error de grep (código 2) cuenta como «limpio»
AMARILLO|core/src/test/java/io/synkra/core/CoreSinPaisesTest.java:26|per[uú] sin límite de palabra
INFORME: revisiones/U02-T08/ronda-1.md
```
