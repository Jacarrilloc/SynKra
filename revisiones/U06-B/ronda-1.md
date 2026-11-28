# Ronda 1 — U06-B

VEREDICTO: VERDE

Hash de la tarea comprobado: `git hash-object tareas/U06-B-historial-reinicio.md` = c729f5ff5416110854fd30741dc895dd47d208bc, coincide. Worktree en af22e4e, base 66c7e5a. Antes y después de mi revisión el worktree está limpio (`git status --porcelain` vacío, sucio=0). El puerto 18089 quedó libre al terminar y no toqué el 18090.

## Criterios de aceptación, verificados por mí
Ejecuciones frescas desde la raíz del worktree, con JAVA_HOME=~/.gradle/jdks/eclipse_adoptium-21-amd64-linux.2.

| # | Criterio | Comando que corrí | Resultado |
|---|---|---|---|
| 1 | Historial y detalle | literal de CA-1 (`--rerun --tests '*HistorialTest'`, grep del XML, python3 -I) | pasa: BUILD SUCCESSFUL, `tests="8" skipped="0" failures="0" errors="0"`. Python da `[(25,), (22,), (23,), (21,)]`, las 5 filas esperadas y `[(22,)]` |
| 2 | Sobrevive al reinicio (R-R8) | literal de CA-2 | pasa: `tests="1" skipped="0"…`. Python da `[(31,'listo',1),(32,'fallido',1),(33,'listo',1)]`, `[('1',),('2',),('3',)]` y `[(32,)]`. En el XML se ven dos arranques sobre el mismo `synkra.db`: el 1.º «Successfully applied 3 migrations», el 2.º «Schema "main" is up to date». El tiempo (3,2 s) es coherente con dos contextos |
| 3 | JAR real reiniciado | literal de CA-3 bajo bash, con `java` cambiado a `$JAVA_HOME/bin/java`, puerto 18089 | pasa: a.txt idéntico a lo esperado (`[12, 13, 11] 2026-11-18T11:06:43.456Z [...]`, `fallido luis [...] Warning BackOff: x APROBADA {'pais': 'pe'}`, `404`, `422`), `diff=0`. Hubo tres arranques: el 1.º migra y los otros dos dicen «up to date»; flyway queda en `[('1',),('2',),('3',)]` |
| 4 | Rojo primero, mutaciones | una copia `git archive HEAD` por mutación, en mi scratchpad (sin stash) | pasa, con la desviación aceptada (ver F-01): las mutaciones orden, país, contexto nulo y R-R8 dan BUILD FAILED con `<failure` en la clase dueña. El desempate literal («quitar `, id desc`») da BUILD SUCCESSFUL y `id asc` da BUILD FAILED (cae `historialDePeVaDelMasRecienteAlMasAntiguoYDesempataPorId`). El rojo previo está en rojo.txt, en el commit 1572ff3, que solo trae las pruebas: no compila, 17 errores |
| 5 | Backend completo y fronteras | literal de CA-5 | pasa: BUILD SUCCESSFUL en 36 s, 38 XML y 488 pruebas, todos con `skipped="0" failures="0" errors="0"`. `ajenos=1`, `escrituras=1` |
| 6 | Diff acotado | `git rev-parse --short HEAD; git diff --stat 66c7e5a...HEAD` | pasa: af22e4e. Solo hay `registro/**` en main, dos archivos de prueba nuevos y `bitacoras/U06-B.md` con `bitacoras/U06-B-evidencia/` (convención vigente desde U02, ver F-04) |

### Desviación 1 (decisión 7: `.run("--synkra.data=…")` en vez de `.properties(…)`): justificada
La reproduje en una copia, devolviendo la prueba a `.properties("synkra.data=…", "server.port=0").run()`. Resultado: BUILD FAILED, `Caused by: java.nio.file.AccessDeniedException at RegistroReinicioTest.java:32`. La causa es que `SpringApplicationBuilder.properties` fija valores por defecto, que tienen menos precedencia que `application.properties` (`synkra.data=${SYNKRA_DATA:/data}`), y `/data` no existe en esta máquina. Lo que cambia es solo cómo se pasa la propiedad. El resto de la decisión 7 se cumple tal cual: una carpeta fija que se borra una vez, dos arranques, `finally`, `isActive()==false`, `equals` de records y las transiciones tras el reinicio. El defecto está en la especificación, no en el diff.

## Hallazgos

### F-01 · AMARILLO · CA-4, desempate R-R5 · la mutación de ejemplo es equivalente; la prueba es suficiente
Lo comprobé yo con SQLite 3.46.1, sobre la consulta real con todas las columnas de `COLUMNAS` (el codificador usó `select id`, que sale como COVERING INDEX y no es el plan real). Transcripción en `plan.txt`.
- Con o sin `, id desc`, el plan es `SEARCH reg_despliegue USING INDEX reg_despliegue_pais (pais=?)` y ambas consultas dan `[25, 22, 23, 21]`.
- `id` es `INTEGER PRIMARY KEY`, o sea el rowid. Toda entrada de índice de SQLite termina en el rowid, así que recorrer el índice al revés da los empates en id descendente. Ninguna prueba de caja negra puede distinguir los dos casos, porque no se puede sembrar un empate con el orden de rowid distinto del orden de id.
- Si el plan cambiara (lo simulé con `NOT INDEXED`), la consulta sin la cláusula da `[22, 25, 23, 21]` y la prueba actual (`containsExactly(25,22,23,21)`) caería. Con la cláusula sigue en `[25, 22, …]`. Tras un ANALYZE con un único país dominante, el plan sigue usando el índice.
- Por qué la acepto: la prueba no depende del plan de consulta. Fija el comportamiento, y es la cláusula la que lo hace independiente del plan. La mutante solo sobrevive mientras el plan la hace equivalente; en cuanto el plan cambie, la prueba la mata. `id asc` demuestra que la prueba muerde el desempate.
- Por qué la mantengo como hallazgo: las mutaciones de CA-4 eran «por ejemplo», pero el «Esperado» habla de «las cinco». El orquestador debería corregir el ejemplo en la especificación (y en la de U06-C si repite el patrón) para que diga `id desc` → `id asc`.

### F-02 · AMARILLO · RutasRegistro.java:27 y RegistroServicio.java:39 · patrón `PAIS` duplicado
`Pattern.compile("[a-z]{2}")` está dos veces en el mismo paquete. Si las dos copias divergen, un país que acepte la ruta y rechace el servicio daría un 500 (`IllegalArgumentException`) en lugar de un 422.

### F-03 · AMARILLO · HistorialTest · «plugins ordenado por nombre» (decisión 2) sin prueba
Cada despliegue sembrado tiene un solo plugin, así que quitar `, nombre` del `order by despliegue_id, nombre` sobreviviría. Lo más probable es que también sea equivalente, por la clave primaria `(despliegue_id, nombre)`. Ningún criterio de aceptación lo exige, por eso no bloquea.

### F-04 · AMARILLO · CA-6 · `bitacoras/U06-B-evidencia/` no está en la lista literal de rutas
Es la convención de evidencia que ya existe en la base (U02-T06…, U05-T05…) y la que nombra el orquestador. No es desborde de alcance. Conviene que las plantillas de las tareas la incluyan en CA-6.

No quedan hallazgos ROJO ni NARANJA. No encontré literales nuevos visibles para el usuario fuera de `CodigosRegistro` y `MotivosRegistro`. Tampoco hay `sleep` fijo en las pruebas, ni pruebas omitidas, ni evidencia más vieja que el código: ca1, ca2 y ca3 son del commit c4a4c1c, que es el último que toca código, y ca4 sale de copias de 6043f15, que tiene el mismo código.

## Tareas candidatas (defectos reales fuera de alcance)
- Especificación (para el orquestador): la decisión 7 debe usar `.run("--clave=valor")`, o un `@SpringBootTest` con propiedades, para cualquier prueba de reinicio futura. Con `.properties()`, el valor siempre pierde frente a `application.properties`.
- Especificación: el ejemplo de mutación del desempate en CA-4 debe ser `id desc` → `id asc` (F-01).

## Rutas de transcripciones largas
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/9d987332-269f-4da9-9d66-0a5449231723/scratchpad/U06-B-r1/ca12.txt (CA-1 y CA-2)
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/9d987332-269f-4da9-9d66-0a5449231723/scratchpad/U06-B-r1/ca3.sh y ca3.txt (CA-3)
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/9d987332-269f-4da9-9d66-0a5449231723/scratchpad/U06-B-r1/ca4.sh y ca4.txt (8 mutaciones: las 5 de la tarea, `id asc`, la desviación 1 y una extra sobre plugins)
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/9d987332-269f-4da9-9d66-0a5449231723/scratchpad/U06-B-r1/plan.py y plan.txt (planes de SQLite, NOT INDEXED, ANALYZE)
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/9d987332-269f-4da9-9d66-0a5449231723/scratchpad/U06-B-r1/ca56.txt (CA-5 y CA-6)

```
VEREDICTO: VERDE
AMARILLO|CA-4 desempate R-R5|la mutación de ejemplo «quitar , id desc» es equivalente; la prueba muerde con id asc y caería si cambia el plan
AMARILLO|RutasRegistro.java:27, RegistroServicio.java:39|patrón PAIS duplicado
AMARILLO|HistorialTest|orden de plugins por nombre sin prueba
AMARILLO|CA-6|bitacoras/U06-B-evidencia/ fuera de la lista literal (convención vigente)
INFORME: revisiones/U06-B/ronda-1.md
```
