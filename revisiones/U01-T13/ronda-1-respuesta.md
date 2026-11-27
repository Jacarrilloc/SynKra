# Respuesta a la ronda 1 — U01-T13

F-01: Corregido en el commit de la ronda 2: la frescura pasa de mtime a contenido. `test` escribe `test-results/test/huella-suite.txt` (SHA-256 de rutas relativas ordenadas y bytes de las clases de main, testFixtures y test; salida declarada de `test`) y `verificarSuite` la recalcula. Un comentario añadido a `SuiteCombinacionesTest.java` deja `test` UP-TO-DATE y `check` en BUILD SUCCESSFUL en tres corridas seguidas; S4 cambiado con `-x test` y el XML viejo falla con «el XML no corresponde a las clases actuales».
F-02: Corregido en el commit de la ronda 2: `verificarSuite` falla si `failures` o `errors` son distintos de 0. Con `ignoreFailures = true` y S4 roto: «tiene failures=1 y errors=0 (deben ser 0)».
F-03: Corregido en el commit de la ronda 2: `tasks.register("verificarSuite")`; `--warning-mode all` ya no menciona `registering`.
F-04: Corregido en el commit de la ronda 2: el mensaje del mínimo usa `$minimo`; el texto fijo ya no repite el 7 ni el 17.
F-05: Corregido en el commit de la ronda 2: `correr` ya no se llama dentro de `$(...)` y deja el código en `$RC`; `git archive` fallido y sed que no altera abortan con rc=2 y mensaje visible (mutaciones m2 y m3 probadas, ver bitácora).
F-06: Corregido en el commit de la ronda 2: la raíz se resuelve desde `dirname "${BASH_SOURCE[0]}"`.
Candidata del revisor (main fuera de la comparación): No es un defecto tras F-01: la huella incluye main; un cambio de bytecode en `src/main` con `-x test` y el XML viejo falla (ver bitácora).
