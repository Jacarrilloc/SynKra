# Ronda 2 — U01-T13

VEREDICTO: VERDE

Corrí yo mismo los criterios CA-1 a CA-7 y todos pasan. Los dos NARANJA de la ronda 1 están cerrados, y lo comprobé corriendo, no leyendo: F-01 con la huella de contenido y F-02 con la regla de failures/errors. No queda ningún ROJO ni NARANJA. Hay dos AMARILLO. Los dos se refieren a lo que la huella no cubre, y solo se pueden explotar con `-x test`, que está fuera de alcance.

## Comprobaciones previas
- `git hash-object tareas/U01-T13-suite-bloquea-build.md` da `f4c9faef90acb7c3bcfb999428ed919383cfab3b`: coincide. HEAD es 2383bc6 y la base es 08a251e.
- `git status --short` del worktree, al principio, después de CA-2 y al final de toda la revisión: vacío. Las mutaciones las hice en copias `git archive HEAD` y en clones dentro de mi scratchpad (`.../scratchpad/r2/{cc,b4,f02,r,clon-m1,clon-m23}`).
- `--warning-mode all`: ya no aparece el aviso de `registering` (F-03 cerrado). El único aviso que queda es `Configuration.setVisible`, de `:backend`, y viene de antes de este diff.

## Criterios de aceptación, verificados por mí
| # | Criterio | Comando que corrí | Resultado |
|---|---|---|---|
| 1 | `check` corre verificarSuite; la 2.ª corrida, con test UP-TO-DATE, pasa | `./gradlew :verificador:test --rerun :verificador:check --console=plain` (32 s, XML tests=17 skipped=0 failures=0 errors=0) y después `./gradlew :verificador:check` | pasa: `test UP-TO-DATE`, `> Task :verificador:verificarSuite`, BUILD SUCCESSFUL en 798 ms |
| 2 | Control con tres escenarios, desde el worktree | `bash scripts/control-suite.sh; echo rc=$?` (status vacío antes) | pasa: 0 / 1 / 1, rc=0. Causa 2: «"S4" FAILED». Causa 3: «verificarSuite: corrieron 8 pruebas… al menos 17» |
| 3a | XML borrado, con `-x test` | copia: `rm $XML; verificarSuite -x test` | pasa: «…no existe… (la suite no corrió)» |
| 3b | XML verde en su sitio, S4 cambiado, con `-x test` | copia: sed sobre S4 y `verificarSuite -x test` | pasa: «el XML no corresponde a las clases actuales (la huella… cambió…)» |
| 3c | S7 saltado | copia: `assumeFalse(id.equals("S7"))` | pasa: «1 pruebas… se saltaron» (tests=17 skipped=1) |
| 3d | Sin S10 | copia: S10 filtrado de `ejecutables()`, con las pruebas de forma intactas | pasa: «corrieron 16… al menos 17» (failures=0) |
| 4 | El script no deja rastro | `git status --short` después de CA-2; `ls -d /tmp/tmp.qcn8MhfJY7` | pasa: status vacío, «No such file or directory» |
| 5 | Sin Spring ni Fabric8 | `dependencies --configuration runtimeClasspath \| grep -ciE` | pasa: `0`; la única dependencia es snakeyaml-engine 3.2 |
| 6 | Todo pasa sin caché, backend incluido | `./gradlew :verificador:test --rerun :backend:test --rerun :verificador:check :backend:check` (57 s) | pasa: 20 XML (verificador y backend), 213 pruebas, skipped=0, failures=0, errors=0, todos recién escritos |
| 7 | Diff acotado | `git diff --stat origin/main...HEAD` | pasa: build.gradle.kts, control-suite.sh, bitácora y `revisiones/U01-T13/` (2 archivos). Nada en `src/` |

## Robustez de la huella (pedido 1)
- **FROM-CACHE**: corrí `check --build-cache`, moví `build/` fuera y volví a correr `check --build-cache`. `test` salió FROM-CACHE, `verificarSuite` pasó y la huella restaurada es idéntica byte a byte (`cmp`).
- **UP-TO-DATE**: en CA-1, en b4 y en la caché de configuración, `verificarSuite` pasa con `test` UP-TO-DATE.
- **Clases cubiertas**: main, testFixtures y test. Probé una cadena: cambiar S4 en testFixtures con `-x test`, y añadir un método a `Veredicto.java` en main con `-x test`. Las dos fallan con «la huella… cambió». Al revertir main vuelve a pasar. La candidata de la ronda 1 queda cerrada.
- **Huella borrada con el XML en su sitio**: `-x test` falla con «falta la huella». `check` vuelve a ejecutar `test`, porque falta una salida declarada, y pasa.
- **`--tests VersionTest`**: Gradle limpia `test-results/test`, y `verificarSuite` falla con «no existe». Con `--tests SuiteCombinacionesTest` pasa: corrieron las 17 pruebas, así que es correcto.
- **Después de un `test` fallido**: la huella vieja sigue en su sitio, porque `doLast` no corre. Aun así la regla de failures rechaza ese XML. Sin hallazgo.
- **XML editado a mano** (tests=8/skipped=8 cambiado a 17/0): con `-x test` pasa (ver F-01). Con `check` normal, Gradle detecta que la salida se modificó, vuelve a correr `test` y falla con «corrieron 8».
- **Recursos**: hoy no hay ninguno en `src/*/resources`. Con un `junit-platform.properties` nuevo, `check -x test` pasa sin ejecutar siquiera `processTestResources` (ver F-02). Con `check` normal, `test` se vuelve a ejecutar.
- **Caché de configuración** (copia limpia): la 1.ª corrida da «entry stored» y la 2.ª «reused», con `test UP-TO-DATE` y verificarSuite verde. La 3.ª, con S4 cambiado y `check -x test --configuration-cache`, falla por huella, y la 4.ª reutiliza la entrada y vuelve a fallar. La 5.ª, `check` con la entrada reutilizada, ejecuta `test` y falla en «"S4" FAILED». La huella se calcula en ejecución y no queda vieja.

## b4, F-02 y mutaciones (pedidos 2 y 3)
- **b4**: con un `// comentario` al final de `SuiteCombinacionesTest.java`, las corridas 1, 2 y 3 dan rc=0 y `test UP-TO-DATE`; en la 1.ª se recompila. Con el mismo comentario en `VersionTest.java`, también rc=0. F-01 de la ronda 1 queda cerrado.
- **F-02**: con S4 roto e `ignoreFailures = true`, sale «"S4" FAILED» y después «verificarSuite: SuiteCombinacionesTest tiene failures=1 y errors=0», BUILD FAILED. Cerrado.
- **m1** (verificarSuite desconectado de `check`, en un clon con commit): da 0/1/0, «escenario 3: el log no trae el mensaje de verificarSuite» y rc=1. Es correcto.
- **m2** (`archive NOEXISTE`): «ERROR: git archive HEAD falló en s1», rc=2. **m3** (el sed de S4 no coincide): escenario 1 y después «ERROR: no se pudo alterar S4», rc=2. F-05 cerrado. En las tres mutaciones el `trap` borró el temporal.
- **Invocado desde otro repositorio git** (`git init` vacío en el scratchpad): da 0/1/1 y rc=0, y archivó el worktree correcto. F-06 cerrado.

## Hallazgos

### F-01 · AMARILLO · verificador/build.gradle.kts:111,114 · La huella no amarra el XML, y el mensaje afirma que sí
La huella solo cubre las clases. Edité a mano un XML de suite desactivada (de 8/8 a 17/0) y corrí `check -x test`: rc=0 (log: `r2/x2.log`). Con `check` normal no se puede explotar (`x3.log`), y `-x test` está fuera de alcance, así que no bloquea. Aun así, el mensaje «el XML no corresponde a las clases actuales» promete más de lo que se comprueba. Endurecerlo es barato: incluir el SHA-256 del XML en la huella, que `doLast` escribe después del XML.

### F-02 · AMARILLO · verificador/build.gradle.kts:45-49,80 · La huella no cubre recursos ni el classpath de dependencias
Las entradas son solo `classesDirs`. Un recurso nuevo en `src/test/resources` (o en main), o un cambio de versión de una dependencia, no invalida la huella con `-x test`. Lo probé con un recurso: rc=0 (`res1.log`). Lo de las dependencias lo saco de leer el código; no lo corrí. Hoy no existe ningún recurso. Sería más sólido usar `sourceSets[x].output` completo, con `resourcesDir`, y el `runtimeClasspath` de test.

## Tareas candidatas (defectos reales fuera de alcance)
- Ninguna nueva. La candidata de la ronda 1 (main fuera de la comparación) quedó resuelta dentro del alcance.

## Rutas de transcripciones largas
- /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/r2/ca1a.log, ca1b.log, ca2.log, ca6.log, ca6-xml.txt
- /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/r2/ca3-0.log, ca3-a.log, ca3-b.log, ca3-c.log, ca3-d.log, ca3-f02.log
- /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/r2/cc1.log … cc6.log (caché de configuración)
- /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/r2/b4-0.log … b4-3.log, b4-v.log, h1.log, h2.log, t1.log, t2.log, bc1.log, bc2.log, m-main.log, m-main2.log, x1.log, x2.log, x3.log, res0.log … res2.log
- /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/r2/m1.log, m2.log, m3.log, otrodir.log

```
VEREDICTO: VERDE
AMARILLO|verificador/build.gradle.kts:45-49,111-114|la huella no amarra el XML, los recursos ni las dependencias (solo explotable con -x test)
INFORME: revisiones/U01-T13/ronda-2.md
```
