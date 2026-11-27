# Ronda 1 — U01-T13

VEREDICTO: NO-VERDE

Los 7 criterios de aceptación pasan con mis propias corridas. Aun así quedan dos NARANJA en `verificarSuite`. El primero hace fallar `check` sin motivo y de forma persistente en un caso de uso normal. El segundo deja una forma de saltarse la suite sin detectar.

## Comprobaciones previas
- `git hash-object tareas/U01-T13-suite-bloquea-build.md` da `f4c9faef90acb7c3bcfb999428ed919383cfab3b`: coincide. HEAD es c13b424 y el merge-base es 08a251e (origin/main).
- Diff: `bitacoras/U01-T13.md`, `scripts/control-suite.sh` y `verificador/build.gradle.kts`. No toca nada en `src/`.
- Rojo primero, reproducido por mí: sobre origin/main con `@Disabled` en la clase, `check` da rc=0 y el XML trae `tests="8" skipped="8"`. El hueco existía.
- `git status --short` del worktree, al final de toda la revisión: vacío. No escribí en el worktree salvo `build/` y `.gradle/`, que están en `.gitignore`.

## Criterios de aceptación, verificados por mí
| # | Criterio | Comando que corrí | Resultado |
|---|---|---|---|
| 1 | check corre verificarSuite; la 2.ª corrida, con test UP-TO-DATE, pasa | `./gradlew :verificador:test --rerun :verificador:check --console=plain` (30 s, XML con tests=17 skipped=0) y después `./gradlew :verificador:check` (`test UP-TO-DATE`) | pasa, pero ver F-01 |
| 1b | Caché de configuración dos veces, en copia limpia | `check --configuration-cache` x2 en `git archive HEAD` | pasa: la 1.ª da «entry stored» y la 2.ª «reused», y en las dos se ejecuta verificarSuite |
| 2 | Control con tres escenarios | `bash scripts/control-suite.sh; echo rc=$?` desde el worktree (1m55s) | pasa: 0 / 1 / 1, rc=0. Causa 2: «"S4" FAILED». Causa 3: «verificarSuite: corrieron 8 pruebas…» |
| 3a | El XML borrado, con `-x test` | copia: `rm $XML; verificarSuite -x test` | pasa: falla con «no existe» |
| 3b | XML verde viejo y S4 cambiado, con `-x test` | copia: `cp -p` del XML verde, sed sobre S4 | pasa: falla con «más viejo que las clases» (recompila testFixtures) |
| 3c | S7 saltado | copia: `Assumptions.assumeFalse(id.equals("S7"))` | pasa: falla con «1 pruebas … se saltaron» (tests=17 skipped=1) |
| 3d | Sin S10, forma ajustada a 11 | copia: quito S10 y ajusto las 7 pruebas de forma sin borrar ninguna | pasa: falla con «corrieron 16 … al menos 17»; failures=0 |
| 4 | El script no deja rastro | `git status --short` tras CA-2; `ls -d /tmp/tmp.pJDUCGjcAR` | pasa: el status sale vacío y el temporal ya no existe |
| 5 | Sin Spring ni Fabric8 | `dependencies --configuration runtimeClasspath \| grep -ciE` | pasa: 0 |
| 6 | Todo pasa sin caché | `./gradlew :verificador:test --rerun :backend:test --rerun :verificador:check :backend:check` (41 s) | pasa: 20 XML (12 del verificador y 8 del backend), 213 pruebas, todas con skipped=0, failures=0 y errors=0 |
| 7 | Diff acotado | `git diff --stat origin/main...HEAD` | pasa: 3 archivos y nada en `src/` |

## Desviaciones declaradas
1. `assumeFalse` sobre S7: aceptable. `@Disabled` no se puede poner sobre una sola fila de `@MethodSource`, y JUnit informa la suposición fallida como `skipped`.
2. Ajuste de las pruebas de forma en (d): aceptable. Yo lo repetí sin borrar ninguna prueba de forma y da 16 < 17, así que la regla no depende del borrado.
3. En el escenario 3 salta la regla del mínimo: aceptable. La tarea pide «por verificarSuite», y con 8 pruebas skipped=8 la regla de skipped también lo habría detectado.
4. El script prueba HEAD: es lo que pide la tarea (`git archive HEAD`).
5. Mínimo 17: correcto. Cuento 7 métodos `@Test` más los 10 casos ejecutables.

## Hallazgos

### F-01 · NARANJA · verificador/build.gradle.kts:64-65 · `check` falla sin motivo y de forma persistente cuando test está UP-TO-DATE y las clases se recompilan con el mismo bytecode
La comparación de mtime choca con la forma en que Gradle decide si `test` está al día. `test` compara las clases por contenido; `verificarSuite` compara la hora de los archivos. Basta un cambio que obligue a recompilar sin cambiar el bytecode, como añadir `// comentario` al final de `SuiteCombinacionesTest.java` o de cualquier otra prueba, por ejemplo `VersionTest.java`. Entonces `compileTestJava` se ejecuta, `test` queda UP-TO-DATE y `verificarSuite` falla con «más viejo que las clases».

Quitar el comentario no lo arregla, porque recompila otra vez. Una tercera corrida sin cambios también falla. Solo `test --rerun` lo devuelve a verde. Logs en el scratchpad: `b4.log`, `b4b.log`, `b4c.log` y `b4d.log`.

Esto contradice el propósito de la decisión 1 y de la segunda parte de CA-1: que un `test` UP-TO-DATE deje un XML válido. La causa está en el diseño prescrito (mtime contra las clases), así que el arreglo probablemente pide una decisión del orquestador.

### F-02 · NARANJA · verificador/build.gradle.kts:54-63 · `verificarSuite` no mira `failures` ni `errors`: con `ignoreFailures = true` la suite deja de bloquear
Lo probé en una copia: S4 alterado más `tasks.named<Test>("test") { ignoreFailures = true }`. El log muestra «"S4" FAILED», pero `verificarSuite` pasa y el build termina en `BUILD SUCCESSFUL`. El XML trae `failures="1"` (log: `b2.log`).

Respuesta a «¿quién lo detecta?»: nadie. Es un cambio de una línea en el build, de la misma clase que `@Disabled`, que es justo lo que la tarea quiere cerrar. Leer `failures` y `errors` ya está dentro del alcance de `verificarSuite`.

### F-03 · AMARILLO · verificador/build.gradle.kts:38 · API obsoleta en Gradle 10
`val verificarSuite by tasks.registering` produce, con `--warning-mode all`, el aviso «The 'val name by registering { }' property delegate syntax has been deprecated… removed in Gradle 10». Es una obsolescencia nueva que trae el diff. La alternativa es `tasks.register("verificarSuite")`.

### F-04 · AMARILLO · verificador/build.gradle.kts:59 · Literal duplicado en el mensaje
El texto «(10 casos S1-S10 más 7 de forma)» está escrito a mano y no sale de `pruebasMinimasSuite`. Si cambia el 17, el mensaje miente. El 17 en sí lo fija la tarea y está documentado, así que no lo trato como valor incrustado bloqueante.

### F-05 · AMARILLO · scripts/control-suite.sh:23,26,28 · `set -e` no rige dentro de `$(correr …)`
Bash desactiva errexit en las sustituciones de comando si no está `inherit_errexit`. Lo comprobé con mutaciones en copias del script:
- **`git archive` falla** (`m2.log`): el script no aborta. Corre `./gradlew` en un directorio vacío (rc=127 en los tres escenarios). El rc final sale bien (1) solo porque los grep de causa rescatan el resultado.
- **El sed de S4 no altera nada** (`m3.log`): el mensaje «no se pudo alterar S4» acaba dentro de `rc2` y nunca se imprime. El script sale con rc=2 mostrando solo la línea del escenario 1.

Con la mutación del build (desconectar `verificarSuite` de `check`, `m1.log`), el script da rc=1 con «escenario 3: el log no trae el mensaje». Ese es el comportamiento correcto. En las tres mutaciones el `trap` borró el temporal.

### F-06 · AMARILLO · scripts/control-suite.sh:10 · `raiz` depende del directorio actual
`git rev-parse --show-toplevel` usa el directorio actual. Si se invoca el script desde otro repositorio, archiva ese otro repositorio. Conviene resolver la raíz a partir de la carpeta del script.

### Robustez comprobada sin hallazgo
- `test --tests OtraClase`: Gradle limpia `test-results/test` y `verificarSuite` falla con «no existe».
- `--project-cache-dir` y `--no-daemon` están presentes en el script.
- `trap`, `mktemp -d` y `pipefail` están presentes.
- No hay `sleep`.
- El nombre del testsuite no se valida, pero el nombre de archivo que fija Gradle ya identifica la clase. Es un riesgo despreciable.
- La resolución de mtime en ext4 es de milisegundos y la comparación es estricta (`<`).

## Tareas candidatas (defectos reales fuera de alcance)
- La comparación de hora no incluye las clases de `src/main`. Con `test` sin ejecutarse (`-x test` o `test.enabled = false`), un cambio que rompe `Veredicto` deja pasar `verificarSuite` con el XML verde viejo. Lo probé en una copia: `verificarSuite -x test` dio rc=0 (log: `b5.log`). Con `test.enabled = false` y el XML viejo, `check` también da `BUILD SUCCESSFUL` (log: `b3.log`). En un clon limpio no hay XML y sí falla. La tarea fija test y testFixtures y saca `-x test` del alcance, así que va como candidata.

## Rutas de transcripciones largas
- /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/ca1a.log, ca1b.log, a-cc1.log, a-cc2.log (CA-1 y caché de configuración)
- /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/ca2.log (CA-2 y CA-4)
- /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/ca3a.log, ca3b.log, ca3c.log, ca3d.log (CA-3)
- /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/ca6.log, ca6-xml.txt (CA-6)
- /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/b4.log, b4b.log, b4c.log, b4d.log (F-01)
- /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/b2.log (F-02)
- /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/m1.log, m2.log, m3.log (mutaciones del script)
- /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/b3.log, b5.log (tarea candidata)
- /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/rojo.log (rojo primero sobre la base)

```
VEREDICTO: NO-VERDE
NARANJA|verificador/build.gradle.kts:64-65|check falla de forma persistente con test UP-TO-DATE tras recompilar sin cambio de bytecode (un comentario basta)
NARANJA|verificador/build.gradle.kts:54-63|verificarSuite ignora failures/errors: con ignoreFailures=true y S4 roto, check da BUILD SUCCESSFUL
INFORME: revisiones/U01-T13/ronda-1.md
```
