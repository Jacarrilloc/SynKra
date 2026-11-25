# Ronda 2 — U01-T03

VEREDICTO: VERDE

Antes de revisar comprobé el contexto:
- El hash de la tarea es `98f7fceacee96e51716207248e851342c86bfefc` y coincide con el esperado.
- HEAD es `6cd34ff`, como reportó el codificador. Tiene 3 commits sobre `m6r/U01-T02` (`d627e08`), que es ancestro de la rama.
- Revisé el diff con `git diff m6r/U01-T02...m6r/U01-T03`. Toca 9 archivos: el código de `verificador/src`, el borrado de `HumoMotoresTest`, la bitácora y los informes de revisión.
- Ningún archivo de Gradle cambia, y no aparece nada de T04, T05, T06, T08 ni T09.
- La JVM de la máquina es 25.0.5.1.

## Criterios de aceptación, verificados por mí
| # | Criterio | Comando que corrí | Resultado |
|---|---|---|---|
| 1 | Las pruebas de `VersionTest` pasan sin caché | `./gradlew :verificador:test --rerun --tests '*VersionTest'` | pasa (`BUILD SUCCESSFUL`, exit 0, la tarea `test` se ejecutó) |
| 2 | Lectura de vuelta del XML: tests ≥ 7 y ningún saltado, fallo ni error | `grep -ho 'tests=... errors=...' verificador/build/test-results/test/TEST-*VersionTest.xml` | pasa: `tests="16" skipped="0" failures="0" errors="0"`. El XML es fresco: su timestamp `2026-11-25T20:11:29Z` corresponde a mi ejecución. Los 16 testcase cubren todos los casos mínimos del criterio, además de `null`, desborde y el mensaje de `null` |
| 3 | `CodigoMotivo` tiene los 9 códigos | el bucle `grep -rqw` de la tarea | pasa (no imprime nada) |
| 4 | Bytecode de Java 21 aunque la máquina tenga JDK 25 | `javap -v .../Version.class \| grep 'major version'` | pasa: `major version: 65` |

También corrí la suite completa del módulo, `./gradlew :verificador:test --rerun`, y pasa: `BUILD SUCCESSFUL`, exit 0. Solo existe el XML de `VersionTest`, porque `HumoMotoresTest` está borrado, cosa que la tarea permite. Los tiempos son de milisegundos, lo esperable en pruebas unitarias puras que no dependen de estado externo.

## Hallazgos de la ronda 1, comprobados
- **F-01 (NARANJA), cerrado.** `git ls-files verificador | grep -c jqwik` da `0`, y `.jqwik-database` no aparece en `git diff --stat`. Como indicó el orquestador, `.gitignore` no se tocó.
- **F-02 (AMARILLO), cerrado.** `diezEsMayorQueNueve` ahora compara `Version.parsear("2.10.0")` con `Version.parsear("2.9.0")`. Las comparaciones con el constructor siguen ahí como casos extra.
- **F-03 (AMARILLO), cerrado.** Con `null` el mensaje es «Falta la versión: se esperaba MAYOR.MENOR.PARCHE…» (`Version.java:30`). Lo cubre la prueba `nuloTieneMensajePropioSinLiteralNull`. La bitácora registra el rojo previo: 16 tests, 1 fallido, `BUILD FAILED`.

## Hallazgos

### F-04 · AMARILLO · `verificador/.jqwik-database` · El worktree queda con un archivo sin rastrear
`git status --short` muestra `?? verificador/.jqwik-database`, y ya estaba así antes de mi revisión: su fecha de modificación es 15:10:48 y mi primera ejecución fue a las 15:11. Es la consecuencia esperada de sacarlo del índice sin ignorarlo. No forma parte del diff ni del commit, así que F-01 queda cerrado. Aun así, cualquier `git add -A` o `git add .` en esta tarea o en las siguientes lo volvería a versionar.

El remedio es `.gitignore`, que está fuera de alcance y pendiente en C-02, así que no bloquea esta tarea. Lo dejo anotado porque, según mi contrato, un worktree sucio después de una revisión es un defecto del bucle: el origen es C-02, no esta tarea.

## Tareas candidatas (defectos reales fuera de alcance)
- C-02, ya registrada: añadir `**/.jqwik-database` al `.gitignore` de la raíz. Hasta que se resuelva, cada worktree que corra jqwik va a mostrar ese archivo como no rastreado.

## Rutas de transcripciones largas
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/5b13ba94-befc-454f-88e7-3182922701e5/scratchpad/u01t03-r2-ca1.log

```
VEREDICTO: VERDE
AMARILLO|verificador/.jqwik-database|El worktree queda con un archivo sin rastrear (depende de C-02)
INFORME: revisiones/U01-T03/ronda-2.md
```
