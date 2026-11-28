# Ronda 2 — U06-A

VEREDICTO: VERDE

Corrí yo mismo los seis criterios con su comando literal y todos pasan. Las cinco respuestas a los hallazgos de la ronda 1 se sostienen con mi propia evidencia, incluidas las dos mutaciones que sobrevivían en la ronda 1, que ahora caen. No queda ningún ROJO ni NARANJA en pie. Quedan dos AMARILLO de trazabilidad, que no bloquean.

Comprobaciones previas:
- El hash de la tarea coincide: `git hash-object` da 74dc0bad938806c939b9a83fda6786b31aa96245.
- La punta es c132c58 y `merge-base 97f9f80 HEAD` = 97f9f80.
- El delta frente a la ronda 1 (839e25a..c132c58) en `backend/` toca un solo archivo, `TransicionesRegistroTest.java`. No hay ningún cambio de producción.
- El código no cambia después de 220229a (`git diff 220229a..HEAD -- backend` sale vacío), así que la evidencia del codificador fechada en 220229a sí pudo salir del diff actual.
- Al terminar, el worktree está limpio (`git status --porcelain` da 0 líneas) y el puerto 18086 quedó libre.

## Criterios de aceptación, verificados por mí
| # | Criterio | Comando que corrí | Resultado |
|---|---|---|---|
| 1 | Esquema, repositorios y tablas en la aplicación arrancada | literal de CA-1, puerto 18086 (estaba libre) | pasa. `BUILD SUCCESSFUL`, `tests="13" skipped="0" failures="0" errors="0"`, suite en 3.2 s. La aplicación respondió en health, y Python dio exactamente `['reg_contexto', 'reg_despliegue', 'reg_plugin']`, `[('1',), ('2',), ('3',)]`, `[('reg_despliegue', 'RESTRICT')]` y `[('reg_despliegue_pais',)]` |
| 2 | Transiciones R-R1 a R-R3, con lectura de vuelta | literal de CA-2 | pasa. `tests="18" skipped="0" failures="0" errors="0"`. Las tres líneas de Python coinciden carácter por carácter con las esperadas. id9 e id10 ya afirman la causa raíz `SQLiteException` con `SQLITE_CONSTRAINT_PRIMARYKEY` |
| 3 | Contexto truncado R-R4, con lectura de vuelta | literal de CA-3 | pasa. `tests="9"`, 0/0/0. Python da `[(1, 200, 'linea-0801', 65536, '[truncado]', 0)]` |
| 4 | Rojo primero y mutaciones | `git archive HEAD` en una copia por mutación (ninguna en el worktree), con el comando de prueba literal de CA-4 | pasa. Las 9 mutaciones dan `BUILD FAILED`, y el `<failure` cae en el XML de la clase dueña de la regla. Detalle debajo de la tabla |
| 5 | Todo el backend y las fronteras | literal de CA-5 | pasa. `BUILD SUCCESSFUL in 46s` y los 36 XML dan 0/0/0. `FronterasModulosTest` da 9/0/0/0, `ajenos=1` |
| 6 | Diff acotado | `git rev-parse --short HEAD`; `git diff --name-only 97f9f80...HEAD` filtrado contra la lista | pasa. Sha c132c58. Lo único que queda fuera de la lista literal es `bitacoras/U06-A-evidencia/`, que es la convención del orquestador (F-07) |

Las 9 mutaciones de CA-4 y la prueba que cayó con cada una:
- **R-R1** (`REQUIRES_NEW`): caen 5 pruebas, entre ellas `id7RegistradoDentroDeUnaTransaccionRevertidaNoQuedaNada`. Tardó 2 min 37 s, por C-107.
- **R-R2** (sin `and estado = 'aplicando'`): caen `id3` e `id4`.
- **R-R3** (sin el rechazo de `FALLIDO` sin contexto): cae `id5`.
- **R-R4** (`bytes.length <= BYTES_MAX` cambiado por `true`): caen 5 pruebas de `ContextoFalloTest`.
- **Las dos que sobrevivían en la ronda 1:**
  - patrón `pe`: cae `cualquierPaisDeDosLetrasMinusculasEsValido`;
  - NPE con más de un plugin: cae `id9`.
- **Una mía nueva:** una comprobación previa del id que lanza `IllegalArgumentException` en lugar de dejar actuar a la clave primaria. Cae `id10`, así que id10 ya distingue la clave duplicada de cualquier otro rechazo.
- **Reloj** (`System.currentTimeMillis()` en lugar de `reloj.millis()`): cae `terminadoEnUsaElRelojDelConstructorDePaquete`.
- **Transacción** (sin `@Transactional` en `actualizarResultado`): cae `siFallaElContextoElEstadoNoQuedaEnFallido`.

Rojo de CA-1 rehecho: sobre una copia de HEAD sin `V3__registro.sql`, el resultado es `13 tests completed, 13 failed` y `BUILD FAILED`.

## Estado de los hallazgos de la ronda 1
- **F-01 (ROJO): cerrado.** id9 e id10 afirman la causa raíz `SQLITE_CONSTRAINT_PRIMARYKEY`. La segunda alta de id10 usa `pe` y un plugin, así que es válida en todo menos en el id. Las dos mutaciones que sobrevivían ahora caen, y también cae mi mutación nueva de comprobación previa.
- **F-02 (AMARILLO): cerrado.** Ya no hay `@TestMethodOrder`, y quien llega primero registra el despliegue 1 con `asegurarRegistrado1()`. Corrí `--tests '*TransicionesRegistroTest.id8*'` sola: `tests="1"`, 0/0/0, `BUILD SUCCESSFUL`. En la corrida completa, id8 se ejecutó en segundo lugar y la primera línea del Python no cambió.
- **F-03 (AMARILLO): cerrado.** El reloj del constructor de paquete y la transacción única del contexto tienen pruebas, y las dos muerden (mutaciones reloj y transacción). `siFallaElContexto…` borra en `finally` solo sus propias filas (id 40), así que el archivo de CA-2 no cambia (lo comprobé con Python).
- **F-04 (AMARILLO): cerrado.** La sangría de las lambdas de `revertida` está corregida.
- **F-05 (AMARILLO): cerrado en parte.** El commit 7aac049 sigue sin reescribirse, y yo reproduje el rojo sin V3. Lo que queda abierto está en F-06 y F-07.

## Hallazgos
### F-06 · AMARILLO · `bitacoras/U06-A-evidencia/ca6-ronda2.txt:3` · el sha anotado no es el HEAD
`ca6-ronda2.txt` anota 2ad5c72, pero HEAD es c132c58, el commit que añade ese mismo archivo. Es inevitable si la evidencia de CA-6 se guarda dentro del propio diff, y el código no cambia entre los dos commits. Aun así, la bitácora dice «sha de HEAD», y no lo es. Basta con aclarar que la evidencia de CA-6 se toma justo antes de su propio commit.

### F-07 · AMARILLO · CA-6 · `bitacoras/U06-A-evidencia/` sigue fuera de la lista literal
Son 14 archivos. La carpeta es la convención del orquestador, así que solo lo anoto y no le toca al codificador. La lista de CA-6 debería nombrarla en las próximas tareas.

## Tareas candidatas (defectos reales fuera de alcance)
- Ninguna nueva. El bloqueo con `REQUIRES_NEW` ya está registrado como C-107: lo volví a medir y tardó 2 min 37 s.

## Rutas de transcripciones largas
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/9d987332-269f-4da9-9d66-0a5449231723/scratchpad/U06-A-r2/ca1.txt
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/9d987332-269f-4da9-9d66-0a5449231723/scratchpad/U06-A-r2/ca2-ca3.txt
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/9d987332-269f-4da9-9d66-0a5449231723/scratchpad/U06-A-r2/ca4.txt (las 9 mutaciones; registro de cada una en `mut-*.log`)
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/9d987332-269f-4da9-9d66-0a5449231723/scratchpad/U06-A-r2/ca5.txt
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/9d987332-269f-4da9-9d66-0a5449231723/scratchpad/U06-A-r2/id8.txt
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/9d987332-269f-4da9-9d66-0a5449231723/scratchpad/U06-A-r2/ca1-rojo.txt
- Script de las mutaciones: /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/9d987332-269f-4da9-9d66-0a5449231723/scratchpad/U06-A-r2/mut.sh

Mis herramientas son de solo lectura, así que no escribí `revisiones/U06-A/ronda-2.md`. Este mensaje es su contenido.

```
VEREDICTO: VERDE
AMARILLO|bitacoras/U06-A-evidencia/ca6-ronda2.txt:3|el sha anotado (2ad5c72) no es el HEAD (c132c58)
AMARILLO|CA-6|bitacoras/U06-A-evidencia/ no figura en la lista literal de rutas
INFORME: revisiones/U06-A/ronda-2.md
```
