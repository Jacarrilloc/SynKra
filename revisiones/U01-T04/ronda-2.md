# Ronda 2 — U01-T04

VEREDICTO: VERDE

Contexto verificado. La tarea da `git hash-object` = `d0526a33cf27934175d1b07f69c5b49c23abe10c`, igual al hash esperado. HEAD de `m6r/U01-T04` es `f783853`, igual al reportado. El diff contra la base apilada `m6r/U01-T03...m6r/U01-T04` toca 5 archivos. Dos son de código: `RangoVersion.java` y `RangoVersionTest.java`. Los otros tres son del flujo: la bitácora, `ronda-1.md` y `ronda-1-respuesta.md`. En la ronda 2 (`710313b..f783853`) el código cambia en una sola línea de `RangoVersion.java`, la regex `INTERVALO`, donde `\s*` pasa a ` *` en las tres posiciones. Además se añaden 6 casos inválidos a `RangoVersionTest.java`. No hay desborde de alcance: no se tocan `Version`, `Resultado`, `Motivo`, `CodigoMotivo` ni `Descriptor`, y no aparece jqwik.

## Criterios de aceptación, verificados por mí
| # | Criterio | Comando que corrí | Resultado |
|---|---|---|---|
| 1 | Pruebas de `RangoVersion` sin caché | `./gradlew :verificador:test --rerun --tests '*RangoVersionTest'` | pasa: rc=0, `BUILD SUCCESSFUL in 3s` |
| 2 | Lectura de vuelta del XML: tests ≥ 12 y sin saltos | `grep -ho 'tests=... errors=...' verificador/build/test-results/test/TEST-*RangoVersionTest.xml` | pasa (detalle abajo) |
| 3 | Todo el verificador sin caché | `./gradlew :verificador:check --rerun` | pasa: rc=0, `BUILD SUCCESSFUL in 13s`. RangoVersionTest da 36/0/0/0 y VersionTest 16/0/0/0. |

Detalle de CA-2:
- El XML da `tests="36" skipped="0" failures="0" errors="0"` y la suite tarda 0,467 s.
- Hay 36 `<testcase>`: 24 inválidos (23 cadenas más el nulo), 7 válidos de la tabla y 5 `@Test`.
- Todos los casos mínimos de CA-2 están presentes por nombre.

Edad de la evidencia:
- Los XML tienen timestamp `2026-11-25T20:38:44Z`, es decir, 15:38 hora local. El commit `f783853` es de las 15:34. Así que los XML salen de mi ejecución sobre el diff actual.
- El tiempo de 0,467 s no indica saltos: las pruebas no dependen de estado externo, y los 36 casos aparecen ejecutados.

Afirmaciones del codificador en la respuesta y la bitácora, contrastadas con mi ejecución:
- Verde con 36 pruebas y 0 fallos: **confirmado**.
- Con la regex anterior (`\s`) fallaban 3 casos, `[18]`, `[19]` y `[20]` (tab, `\n` y `\r\n`): es coherente con el diff. Los otros 3 casos nuevos (espacio al inicio o al final del intervalo) ya pasaban, y eso coincide con mi sonda de la ronda 1.
- Los casos nuevos usan escapes de Java (`"\t"`, `"\n"`, `"\r\n"`) en `@ValueSource`, así que el argumento lleva el carácter de control real y no la barra literal.

Sonda propia, compilada y corrida fuera del worktree contra `verificador/build/classes/java/main` en `/tmp/claude-1000/-home-casdav-Projects-Master-SynKra/5b13ba94-befc-454f-88e7-3182922701e5/scratchpad/probe2/Probe2.java`:
```
[>=2.1.0\t<3.0.0]    -> Fallo[RANGO_INVALIDO, campo=coreCompatible]
[>=\n2.1.0]          -> Fallo[RANGO_INVALIDO]
[>=2.1.0NBSP<3.0.0]  -> Fallo[RANGO_INVALIDO]
[>=2.1.0\f<3.0.0]    -> Fallo[RANGO_INVALIDO]
[>= 2.1.0  <  3.0.0] -> Exito[minimo=2.1.0, maximo=Optional[3.0.0]]
[2.x]                -> Exito[minimo=2.0.0, maximo=Optional[3.0.0]]
```
La sonda cubre dos casos que las pruebas no tienen: espacio duro (U+00A0) y salto de página (`\f`). Ambos se rechazan, como pide el arbitraje: solo vale U+0020. Ninguna entrada lanza excepción.

Barrido de clase: ninguna otra regex de `verificador/src/main/java/io/synkra/verificador/*.java` usa `\s` (`grep` sin resultados).

## Estado de los hallazgos de la ronda 1
- **F-01 · NARANJA**: cerrado. La regex usa un espacio literal y las pruebas fijan como inválidos el tab, `\n` y `\r\n`. Lo verifiqué corriendo CA-1 y con la sonda.
- **F-02 · AMARILLO**: cerrado. Los casos ` >=2.1.0`, `>=2.1.0 ` y `>=2.1.0 <3.0.0 ` están fijados como inválidos (`[21]` a `[23]` del XML).
- **F-03 · AMARILLO**: cerrado por arbitraje del orquestador como candidata C-06. El literal `"coreCompatible"` sigue en `RangoVersion.java`, como se esperaba, y no bloquea.

## Hallazgos
No queda ningún hallazgo ROJO, NARANJA ni AMARILLO en pie.

## Tareas candidatas (defectos reales fuera de alcance)
- Ninguna nueva. C-06 (asignación del campo por `LectorDescriptor`) ya está registrada.
- El worktree está sucio solo por `?? verificador/.jqwik-database`, sin rastrear. No está en el diff y ya estaba antes de esta revisión (C-02). Mi revisión no ensució nada más: compilé y corrí la sonda en el scratchpad.

## Rutas de transcripciones largas
- `/tmp/claude-1000/-home-casdav-Projects-Master-SynKra/5b13ba94-befc-454f-88e7-3182922701e5/scratchpad/r2-ca1.txt` (salida de CA-1)
- `/tmp/claude-1000/-home-casdav-Projects-Master-SynKra/5b13ba94-befc-454f-88e7-3182922701e5/scratchpad/r2-ca3.txt` (salida de CA-3)
- `/tmp/claude-1000/-home-casdav-Projects-Master-SynKra/5b13ba94-befc-454f-88e7-3182922701e5/scratchpad/probe2/Probe2.java` (sonda de separadores)

```
VEREDICTO: VERDE
INFORME: revisiones/U01-T04/ronda-2.md
```
