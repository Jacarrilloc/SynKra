# Ronda 1 — U06-A

VEREDICTO: NO-VERDE

El veredicto es NO-VERDE por un solo motivo: dos pruebas de CA-2 (id 9 e id 10) no comprueban la excepción que el criterio exige. Lo demás pasa en mi máquina, incluidas las lecturas de vuelta, que dan exactamente lo esperado.

Comprobaciones previas:
- El hash de la tarea coincide: `git hash-object` = 74dc0bad938806c939b9a83fda6786b31aa96245.
- Punta revisada: 839e25a. `git merge-base 97f9f80 HEAD` = 97f9f80, así que `..` y `...` dan el mismo diff.
- Entorno: JAVA_HOME=~/.gradle/jdks/eclipse_adoptium-21-amd64-linux.2 (21.0.13.1).
- El worktree quedó limpio al terminar (`git status --porcelain` da 0 líneas).

## Criterios de aceptación, verificados por mí
| # | Criterio | Comando que corrí | Resultado |
|---|---|---|---|
| 1 | Repositorios y esquema, y las tres tablas en la app arrancada | el literal de CA-1: `:backend:test --rerun --tests '*RegistroRepositoriosTest'`, grep del XML, bootJar, `java -jar` en el puerto 18086 (libre según `ss`), espera de health con `timeout 90` y el Python | pasa. `BUILD SUCCESSFUL`, `tests="13" skipped="0" failures="0" errors="0"`, suite en 4.4 s. Python da exactamente `['reg_contexto', 'reg_despliegue', 'reg_plugin']`, `[('1',), ('2',), ('3',)]`, `[('reg_despliegue', 'RESTRICT')]` y `[('reg_despliegue_pais',)]` |
| 2 | Transiciones R-R1 a R-R3 y lectura de vuelta | el literal de CA-2 | **falla en parte** (F-01). `tests="15" skipped="0" failures="0" errors="0"` en 3.5 s. Las tres líneas de Python coinciden carácter por carácter con lo esperado. Pero la exigencia de que «cada rechazo afirma su excepción (… o la de clave duplicada)» no se cumple en id9 ni en id10 |
| 3 | Contexto truncado R-R4 y lectura de vuelta | el literal de CA-3 | pasa. `tests="9" skipped="0" failures="0" errors="0"`. Python da `[(1, 200, 'linea-0801', 65536, '[truncado]', 0)]`. Los siete casos que pide el criterio están cubiertos |
| 4 | Rojo primero y mutaciones | las 4 mutaciones de la bitácora, cada una en su copia (`git archive HEAD`), más 3 mías y el rojo de CA-1 rehecho sobre `7aac049` sin V3 | pasa. Las 4 dan `BUILD FAILED` y cae la prueba de cada regla: R-R1 `id7…`, R-R2 `id3`/`id4`, R-R3 `id5`, R-R4 cinco de `ContextoFalloTest`. Mis mutaciones también caen: sin `@Transactional`, cae `id9`; `LISTO` con contexto permitido, cae `id6`; `logCore` sin corte por líneas, caen 3 pruebas. El rojo de CA-1 se reproduce: `13 tests completed, 13 failed` |
| 5 | Todo el backend y las fronteras | el literal de CA-5 | pasa. `BUILD SUCCESSFUL`, 36 XML y todos con 0/0/0; `FronterasModulosTest` 9/0/0/0; `ajenos=1` |
| 6 | Diff acotado | `git rev-parse --short HEAD`; `git diff --name-only 97f9f80...HEAD` filtrado contra la lista | pasa. Sha 839e25a, 31 archivos, ninguno fuera de las rutas permitidas (la carpeta `bitacoras/U06-A-evidencia/` aparte, ver F-05) |

## Hallazgos

### F-01 · ROJO · CA-2 / `backend/src/test/java/io/synkra/registro/TransicionesRegistroTest.java:157,167` · id9 e id10 no comprueban la excepción de clave duplicada
CA-2 pide que cada rechazo afirme su excepción, y nombra «la de clave duplicada». En la línea 157 (`id9DosPluginsConElMismoNombreRechazanElAltaEntera`) y en la 167 (`id10RegistrarDosVecesRechazaLaSegundaYLaFilaNoCambia`), la prueba solo comprueba `.isInstanceOf(RuntimeException.class)`. Así pasa con cualquier excepción. Lo demostré con dos mutaciones que sobreviven, cada una en su copia y corriendo las tres clases del registro:
- `Pattern.compile("[a-z]{2}")` cambiado por `Pattern.compile("pe")` en `RegistroServicio`: `BUILD SUCCESSFUL`. La segunda alta de id10 usa el país `co`, así que cae en la validación con `IllegalArgumentException` y no por la clave primaria. La prueba sigue en verde por la razón equivocada. Además, un validador de país roto pasa desapercibido en toda la suite.
- Añadir `if (plugins.size() > 1) throw new NullPointerException();` después de `validar(nuevo)`: `BUILD SUCCESSFUL`. id9 no distingue un rechazo de la base por la clave `(despliegue_id, nombre)` de una NPE cualquiera.

Arreglo esperado: que la prueba afirme la causa de clave duplicada, por ejemplo `DuplicateKeyException` en la cadena de causas o `SQLITE_CONSTRAINT_PRIMARYKEY` en el mensaje de la causa raíz, como ya hace `RegistroRepositoriosTest`. En id10, que la segunda alta sea válida en todo menos en el id.

### F-02 · AMARILLO · `TransicionesRegistroTest.java:33` · pruebas que dependen del orden
`id8` necesita que `id1` haya corrido antes, y lo consigue con `@TestMethodOrder(MethodName)`. Funciona, pero acopla las pruebas entre sí: si id8 corre sola, falla. Una opción es que id8 registre su propio origen. Si se elige eso, hay que mantener la primera línea del Python de CA-2.

### F-03 · AMARILLO · `RegistroServicio.java` (constructor de paquete con `Clock`) · el reloj inyectable no se usa en las pruebas
La decisión 6 pide el constructor de paquete «para las pruebas», pero ninguna prueba lo usa: `grep Clock` en las pruebas no encuentra nada. Las pruebas solo miran que `terminado_en` no sea nulo, nunca su valor. Tampoco hay ninguna prueba que muerda si el insert de `reg_contexto` queda fuera de la transacción del `UPDATE`.

### F-04 · AMARILLO · `TransicionesRegistroTest.java` (lambdas de `revertida`) · sangría rota
El cuerpo de las lambdas de los 4 casos extra que usan `revertida` no tiene sangría.

### F-05 · AMARILLO · trazabilidad de la bitácora y de los commits
- El commit 7aac049 dice «rojo, sin V3» pero ya incluye `V3__registro.sql`. La bitácora lo reconoce, y yo reproduje el rojo quitando V3 de esa copia.
- La bitácora anota como sha de CA-6 el 2512df7, y `ca6.txt` el 8e0e744. Ninguno es la punta (839e25a). El código no cambia después de 2512df7, pero CA-6 pide anotar el sha de HEAD.
- `bitacoras/U06-A-evidencia/` no está, literalmente, en la lista de rutas de CA-6. El orquestador la anuncia como convención, así que solo lo anoto.

## Tareas candidatas (defectos reales fuera de alcance)
- Con la propagación `REQUIRES_NEW` y el pool de una sola conexión de `BaseDeDatos`, la transacción interna espera a que se agote el tiempo de conexión: la corrida tardó 2 min 7 s, contra los 6 s de las demás. Cualquier código futuro que abra una transacción anidada con `REQUIRES_NEW` (por ejemplo, en U07) se bloqueará así en lugar de fallar rápido. Conviene dejarlo documentado para U07.

## Rutas de transcripciones largas
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/9d987332-269f-4da9-9d66-0a5449231723/scratchpad/ca1-test.txt
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/9d987332-269f-4da9-9d66-0a5449231723/scratchpad/ca4.txt (las 4 mutaciones de la tarea, las 3 mías y el rojo de CA-1)
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/9d987332-269f-4da9-9d66-0a5449231723/scratchpad/ca4-extra.txt (las dos mutaciones que sobreviven, F-01)
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/9d987332-269f-4da9-9d66-0a5449231723/scratchpad/ca5.txt y ca5-xml.txt
- Scripts: /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/9d987332-269f-4da9-9d66-0a5449231723/scratchpad/mut.sh y mut2.sh

```
VEREDICTO: NO-VERDE
ROJO|backend/src/test/java/io/synkra/registro/TransicionesRegistroTest.java:157,167 (CA-2)|id9 e id10 solo comprueban RuntimeException, no la excepción de clave duplicada; dos mutaciones sobreviven
INFORME: revisiones/U06-A/ronda-1.md
```

(Mis herramientas son de solo lectura, así que no escribí `revisiones/U06-A/ronda-1.md`: este mensaje es su contenido.)
