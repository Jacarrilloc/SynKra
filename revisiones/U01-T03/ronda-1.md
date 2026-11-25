# Ronda 1: U01-T03

VEREDICTO: NO-VERDE

Contexto verificado: el hash de la tarea es `98f7fceacee96e51716207248e851342c86bfefc` y coincide. HEAD es `f2bf808`, con un solo commit sobre `m6r/U01-T02`. El merge-base es igual a la punta de `m6r/U01-T02` (`d627e08`). Revisé el diff con `git diff m6r/U01-T02...m6r/U01-T03`. El worktree estaba limpio antes de revisar y sigue limpio después, aunque hay un matiz que explico en F-01. La JVM de la máquina es 25.0.5.1.

## Criterios de aceptación, verificados por mí
| # | Criterio | Comando que corrí | Resultado |
|---|---|---|---|
| 1 | Las pruebas de `VersionTest` pasan sin caché | `./gradlew :verificador:test --rerun --tests '*VersionTest'` | pasa (`BUILD SUCCESSFUL`, exit 0) |
| 2 | Lectura de vuelta del XML: tests ≥ 7 y ningún saltado, fallo ni error | `grep -ho 'tests=... errors=...' verificador/build/test-results/test/TEST-*VersionTest.xml` | pasa: `tests="15" skipped="0" failures="0" errors="0"`. Los 15 testcase aparecen con su nombre y cubren todos los casos mínimos del criterio y además `null` |
| 3 | `CodigoMotivo` tiene los 9 códigos | el bucle `grep -rqw` de la tarea | pasa (no imprime nada) |
| 4 | Bytecode de Java 21 aunque la máquina tenga JDK 25 | `javap -v .../Version.class \| grep 'major version'` | pasa: `major version: 65` |

Sobre el tiempo de la suite: los casos tardan entre 0,005 y 0,08 s. Las pruebas son unitarias puras y no dependen de ningún estado externo, así que ese tiempo no indica que se hayan saltado.

Fidelidad a R-V1 y a `domain-entities.md`, revisada leyendo el código y corriendo las pruebas:
- **Forma:** `Version` usa la expresión `(0|[1-9][0-9]*)` tres veces, separadas por puntos, con `matches()` anclado. Eso rechaza ceros a la izquierda, pre-release, metadatos de build, signos y espacios.
- **Desborde:** un número que no cabe en `int` cae a `VERSION_INVALIDA` sin lanzar excepción.
- **Campos:** son `mayor`, `menor` y `parche`, y el constructor exige que sean ≥ 0.
- **Orden:** compara mayor, luego menor, luego parche. Es coherente con `equals`.
- **`toString`:** con texto válido devuelve exactamente la entrada.
- **`Motivo`:** tiene `codigo`, `plugin?`, `mensaje` y `campo?`, y el mensaje no puede estar en blanco. Coincide con la tabla de entidades.
- **`Resultado<T>`:** es un valor o una lista no vacía de `Motivo`. Coincide con `component-methods.md`.
- **`CodigoMotivo`:** tiene exactamente los 9 códigos, en el orden de la tabla.
- **`HumoMotoresTest`:** borrado, tal como permite la tarea. El directorio `test-results` solo contiene el XML de `VersionTest`, y `./gradlew :verificador:test --rerun` completo también pasa.

## Hallazgos

### F-01 · NARANJA · `verificador/.jqwik-database` · Artefacto de ejecución versionado en el diff
El commit añade `verificador/.jqwik-database` (4 bytes: `aced 0005`, la cabecera de un stream serializado de Java). Es la base de datos de semillas que jqwik genera al correr. No es código de la tarea ni aparece en «Dentro»: entró al diff como efecto secundario de las ejecuciones.

Comprobación: `git ls-files verificador` lo lista como rastreado. Su fecha de modificación pasó de 15:08:34 a 15:09:28 solo por correr CA-1. Hoy no ensucia el worktree porque el contenido es el mismo, pero cuando una propiedad de jqwik falle (U01-T05 en adelante), jqwik escribirá semillas en ese archivo. A partir de ahí cada revisión dejará el worktree sucio y habrá diffs binarios espurios. Hay que sacarlo del commit (`git rm --cached`).

Añadirlo a `.gitignore` queda fuera del alcance de esta tarea; lo anoto como tarea candidata.

### F-02 · AMARILLO · `VersionTest.java` (`diezEsMayorQueNueve`) · El orden se prueba con el constructor, no con `parsear`
El criterio dice «`2.10.0` es mayor que `2.9.0`». La prueba compara `new Version(2, 10, 0)` con `new Version(2, 9, 0)`, así que no ejercita la ruta texto → orden. Hoy es correcto, porque `parsear` convierte a `int`, pero compararlas vía `Version.parsear("2.10.0")` cubriría el criterio tal como está escrito.

### F-03 · AMARILLO · `Version.java:29` · Mensaje con `'null'` literal
Con `parsear(null)` el mensaje dice «La versión 'null' no tiene la forma…». Sigue siendo legible, pero un texto específico para la ausencia de versión sería más claro.

## Tareas candidatas (defectos reales fuera de alcance)
- Añadir `.jqwik-database` (o `**/.jqwik-database`) al `.gitignore` de la raíz, para que ninguna tarea vuelva a versionar el estado de jqwik.

## Rutas de transcripciones largas
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/5b13ba94-befc-454f-88e7-3182922701e5/scratchpad/u01t03-ca1.log

```
VEREDICTO: NO-VERDE
NARANJA|verificador/.jqwik-database|Artefacto de ejecución versionado en el diff
INFORME: revisiones/U01-T03/ronda-1.md
```
