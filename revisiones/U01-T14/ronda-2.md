# Ronda 2 — U01-T14

VEREDICTO: VERDE

La ronda 1 dejó dos ROJO, un NARANJA y un AMARILLO por corregir. Esos cuatro (F-01 a F-04) y F-06 están cerrados. Volví a correr los cinco criterios y todos pasan. No encontré errores nuevos de la misma clase. Quedan dos AMARILLO, y ninguno bloquea.

Worktree `/home/user/wt-m6r-U01-T14`, rama m6r/U01-T14, HEAD f855f2d, base origin/main ee925c4. El hash de la tarea es `6534aa420ea0cf8dd44ffbbf7b448cb39bfd0f79`, el que debe ser. Al terminar, `git status --short` sale vacío.

## Criterios de aceptación, verificados por mí
| # | Criterio | Comando que corrí | Resultado |
|---|---|---|---|
| 1 | `check` real en el resumen | `./gradlew :verificador:test --rerun :verificador:check --console=plain`, de 06:40:25 a 06:40:59 UTC. Mandé la salida al scratchpad para no pisar `/tmp/u01-check.txt`. Luego `tail -6`, `grep -n verificarSuite` y `grep -c 'BUILD SUCCESSFUL' summary.md` | pasa: rc=0, `BUILD SUCCESSFUL in 34s`, línea 19 `> Task :verificador:verificarSuite`, 1 aparición en el resumen. `diff <(tail -6 /tmp/u01-check.txt) <(sed -n 126,131p summary.md)` no da diferencias. `grep -n` sobre `/tmp/u01-check.txt` sigue dando `19:> Task :verificador:verificarSuite`, igual a la línea 137 del resumen. |
| 2 | Conteos de los XML | el bucle de CA-2, normalizado, contra las filas 158-169 de la tabla (`diff`) | pasa: 12 XML y 12 filas, sin diferencias. Suma 178/0/0/0, igual a la fila de total. En el system-out hay 10 `tries = 1000` y 10 `checks = 1000`. Cuadran una por una con los 10 `@Property(tries = 1000)` y con los nombres de la tabla. Ninguna clase tiene skipped. |
| 3 | Control de la suite | `bash scripts/control-suite.sh; echo rc=$?`, de 06:42:16 a 06:44:12 UTC | pasa: los tres escenarios y rc=0. Comparé mi salida con las líneas 146-151 del resumen: sin diferencias, salvo la ruta temporal, que el resumen ya avisa que cambia en cada corrida. |
| 4 | Completo y sin inventar | `grep '^## '`, `U01-T..`, `#N`, el `awk` de candidatas, un grep de firmas `public` en main y testFixtures, conteo de `revisiones/U01-T*/ronda-N.md` | pasa: están las 8 secciones y T01 a T14. Rondas: 1,1,2,2,1,3,2,4,3,2,3,6,2. El `awk` da las mismas 17 candidatas que el resumen. Las firmas coinciden con el código. |
| 5 | Sin cambios fuera | `git diff --stat origin/main...HEAD` | pasa: solo `summary.md`, `bitacoras/U01-T14.md`, `revisiones/U01-T14/ronda-1.md` y `ronda-1-respuesta.md`. |

## Cierre de los hallazgos de la ronda 1
- **F-01 (ROJO), cerrado.** La fila `VerificadorTest | 15 | 0 | 0 | 0` está en la línea 168, y `diff` contra mi corrida de CA-2 sale vacío.
- **F-02 (ROJO), cerrado.**
  - La línea 22 dice ahora «10 códigos; el último, `DESCRIPTOR_INVALIDO`». `CodigoMotivo.java` tiene 10 constantes y la última es esa.
  - La línea 48 explica que en U01-T03 eran 9. Lo comprobé: `git show f2bf808:…CodigoMotivo.java` da 9 constantes, y 4c1a396 (U01-T06) añadió la décima.
- **F-03 (NARANJA), cerrado.** Comprobé cada fuente de «Quién lo usa».
  - U04 ya no aparece como consumidor de `CatalogoEnMemoria`. La línea 28 cita `unidades-y-tareas.md:52` («para que U02 y U07 la reutilicen») y `tareas/U01-T09-verificador.md:13` («para U02 y U07 después»). Las dos fuentes dicen eso.
  - **U02.** `unidades-y-tareas.md:94-95` (U02-T04 y U02-T05) dice lo que el resumen le atribuye. S12 queda «pendiente en U02», como dice `SuiteCombinaciones.java:261`.
  - **U04.** `unidades-y-tareas.md:152` y `U04-catalogo-code-generation-plan.md:48` (la línea es exacta) hablan de S1 a S7 «usando datos guardados en SQLite». S11 queda «pendiente en U04 y U05», como dice `SuiteCombinaciones.java:258`. C-13, C-19 y C-20 remiten a U04 y a la recepción del JAR en `CANDIDATAS.md`. La frase rota se reescribió.
  - **U07.** `unidades-y-tareas.md:52`, `:216` y `:225-226` dicen lo mismo que el resumen: S4 con alternativas, e inválido con 422 y sin fila.
  - **Límite de U01.** Coincide con `unidades-y-tareas.md:53`.
- **F-04 (AMARILLO), cerrado.** Los 9 constructores que nombra la línea 8 tienen su `throw new IllegalArgumentException` (grep en main). También `Verificador.java:61-62`. Que los fallos de datos no lancen es cierto:
  - `Version.parsear` y `RangoVersion.parsear` documentan «Nunca lanza». `RangoVersion` atrapa la IAE interna de `version()`.
  - `LectorDescriptor.leer(null)` y `desdeYaml(null)` devuelven `Fallo`.
- **F-05.** Aceptado por el orquestador, como indicaste. La línea de `verificarSuite` va aparte, con su comando y su procedencia.
- **F-06 (AMARILLO), cerrado con un resto (ver F-08).** La bitácora rotula el stat como «antes de este último commit, que solo toca la bitácora». La fecha queda declarada.

## Barrido de la misma clase en todo el resumen
- **Cambios entre rondas.** `git diff ab20703 HEAD -- summary.md` solo toca las líneas 8, 22, 28, 33-40, 48 y 168. Los bloques de evidencia de CA-1 (125-138) y CA-3 (144-152) no cambiaron.
- **Cifras.** Comprobé contra el código y contra mis corridas:
  - 10 códigos.
  - 12 casos, 2 `FueraDeU01` y 10 ejecutables.
  - 17, que son 7 `@Test` más 10 parametrizados, igual a `pruebasMinimasSuite = 17`.
  - 10 propiedades: 2 + 2 + 6.
  - 13 archivos en main, 2 en testFixtures y 13 en test.
  - 178 pruebas, 17 candidatas y 14 `#N` fusionados (ya verificados en la ronda 1, sin cambios).
- **Clases y métodos.** Todos los tipos y firmas de las tablas de «qué expone» y `testFixtures` existen con esa forma: `public` en `Version`, `RangoVersion`, `Descriptor`, `LectorDescriptor`, `Seleccion`, `Veredicto`, `VeredictoPlugin`, `Alternativa`, `Catalogo`, `Verificador`, `Resultado`, `Motivo`, `CatalogoEnMemoria` y `SuiteCombinaciones`.
- **U02, U04 y U07 fuera de «Quién lo usa».** La línea 203 (S11 y S12 en U04, U05 y U02) es fiel a `SuiteCombinaciones.java`. Las demás menciones son texto literal de `CANDIDATAS.md`. No encontré afirmaciones sin fuente.

## Hallazgos

### F-07 · AMARILLO · summary.md:8 · Las precondiciones con nulo lanzan `NullPointerException`, no `IllegalArgumentException`
La línea 8 solo nombra la IAE. Pero los mismos constructores, y `Verificador.verificar`/`alternativas`, usan `Objects.requireNonNull`: hay 9 en `Verificador.java` y de 1 a 5 en cada record. No es falso: la frase habla de invariantes. Aun así, quien llame con un nulo recibirá NPE. Basta con media frase.

### F-08 · AMARILLO · ronda-1-respuesta.md, F-06 · La respuesta dice más de lo que hace la bitácora
La respuesta dice «el stat de CA-5 se rehízo al final, tras la última edición». La bitácora, en cambio, lo rotula «antes de este último commit». Mi stat actual da 343 líneas en la bitácora, y la bitácora dice 332. Lo honesto es la bitácora; la respuesta lo exagera. Es autorreferencial y no se puede cerrar del todo: CA-5 lo verifiqué yo.

## Tareas candidatas (defectos reales fuera de alcance)
- Ninguna nueva. Las dos de la ronda 1 las registra el orquestador.

## Rutas de transcripciones largas
- /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/check-r2.txt
- /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/control-r2.txt
- /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/xml.txt y tabla.txt (el diff de CA-2)
- /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/r2diff.txt (el diff del resumen entre rondas)
- Resumen: /home/user/wt-m6r-U01-T14/aidlc-docs/construction/U01-verificador/code/summary.md
- /tmp/u01-check.txt quedó intacto: no lo reescribí.

```
VEREDICTO: VERDE
AMARILLO|summary.md:8|Las precondiciones con nulo lanzan NullPointerException, no IllegalArgumentException; AMARILLO|ronda-1-respuesta.md F-06|La respuesta dice que el stat se rehízo tras la última edición, la bitácora dice que es de antes
INFORME: revisiones/U01-T14/ronda-2.md
```
