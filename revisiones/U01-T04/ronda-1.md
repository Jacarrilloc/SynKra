# Ronda 1 — U01-T04

VEREDICTO: NO-VERDE

Contexto verificado: el `git hash-object` de la tarea da `d0526a33cf27934175d1b07f69c5b49c23abe10c`, que coincide con el hash esperado. HEAD de `m6r/U01-T04` es `710313b`. Hay un solo commit en `m6r/U01-T03..m6r/U01-T04`. El diff toca 3 archivos: `RangoVersion.java`, `RangoVersionTest.java` y la bitácora. No hay desborde de alcance: no cambian `Version`, `Resultado`, `Motivo`, `CodigoMotivo` ni `Descriptor`, y no aparece jqwik.

## Criterios de aceptación, verificados por mí
| # | Criterio | Comando que corrí | Resultado |
|---|---|---|---|
| 1 | Pruebas de `RangoVersion` sin caché | `./gradlew :verificador:test --rerun --tests '*RangoVersionTest'` | pasa (`BUILD SUCCESSFUL in 3s`) |
| 2 | Lectura de vuelta del XML: tests ≥ 12 y sin saltos | `grep -ho 'tests=... errors=...' verificador/build/test-results/test/TEST-*RangoVersionTest.xml` | pasa: `tests="30" skipped="0" failures="0" errors="0"`, suite de 0,455 s. Los 30 casos son 7 válidos, 17 inválidos más el nulo, y 5 `@Test`. Todos los casos mínimos de CA-2 están en la tabla. |
| 3 | Todo el verificador sin caché | `./gradlew :verificador:check --rerun` | pasa (`BUILD SUCCESSFUL in 20s`). Informes: RangoVersionTest 30/0/0/0 y VersionTest 16/0/0/0. |

Las afirmaciones del codificador, contrastadas con mi propia ejecución:
- tests=30: **confirmado**.
- Los desbordes de int dan RANGO_INVALIDO: **confirmado**. Probé `2147483648.x`, `>=2147483648.0.0`, `2.1.2147483648` y `>=2.1.0 <2147483648.0.0`, y las pruebas cubren los casos de `incrementExact`. Ninguno lanza excepción.
- Los intervalos vacíos dan RANGO_INVALIDO: **confirmado**.
- `>=2.1.0<3.0.0` se acepta: **confirmado**.
- Los espacios al inicio o al final se rechazan: **confirmado**. Vale para `2.x` y para `>=2.1.0` (` >=2.1.0` y `>=2.1.0 ` fallan).

El rojo primero de la bitácora (fallo de compilación porque faltaba `RangoVersion`) es coherente con el diff. La evidencia en verde coincide con mi ejecución y es del diff actual.

## Hallazgos

### F-01 · NARANJA · RangoVersion.java:17-19 (`INTERVALO`) · `\s` acepta tabuladores y saltos de línea, que R-V2 manda a RANGO_INVALIDO
R-V2 dice: «Espacios opcionales alrededor de `>=` y `<`; cualquier otra cosa → `RANGO_INVALIDO`». La regex usa `\s*`, y en Java `\s` admite `[ \t\n\x0B\f\r]`, no solo el espacio. Lo comprobé con una sonda propia compilada contra `verificador/build/classes/java/main`, fuera del worktree:
```
[>=2.1.0\t<3.0.0] -> Exito[... minimo=2.1.0, maximo=Optional[3.0.0] ...]
[>=2.1.0\n<3.0.0] -> Exito[... minimo=2.1.0, maximo=Optional[3.0.0] ...]
[>=\n2.1.0]       -> Exito[... minimo=2.1.0, maximo=Optional.empty ...]
```
El texto llega de `coreCompatible`, un YAML de terceros (R-V3), donde un escalar de bloque mete `\n` con facilidad. El rango se acepta y el `texto` original conserva el salto de línea, que acabará en mensajes y vistas. No rompe ningún caso listado en CA-2, pero acepta entradas que la regla dice rechazar. Además, ninguna prueba fija qué separadores valen: la tabla solo prueba el espacio U+0020.

Lo que se espera:
- usar un separador de espacio literal (` *`) en las tres posiciones;
- añadir a `formaInvalidaDevuelveRangoInvalidoSinExcepcion` al menos `">=2.1.0\t<3.0.0"` y `">=\n2.1.0"`.

Si el orquestador lee «espacios» como «espacio en blanco», que lo diga la especificación. Hoy la palabra es «espacios».

### F-02 · AMARILLO · RangoVersionTest.java:41-61 · Falta prueba de espacios al inicio o al final en la forma intervalo
El rechazo de ` >=2.1.0` y `>=2.1.0 ` funciona (lo vi en la sonda), pero las pruebas solo lo fijan para `2.x` (` 2.x` y `2.x `). «Alrededor de `>=`» admite la lectura de que un espacio antes de `>=` es legítimo. La elección del codificador, rechazarlo, es defendible y coherente con no recortar el texto. Conviene fijarla con un caso para que no se mueva sin que nadie lo note.

### F-03 · AMARILLO · RangoVersion.java:46 · El campo `"coreCompatible"` va incrustado en el tipo de dominio
`parsear` pone `campo = "coreCompatible"`, lo que ata `RangoVersion` al nombre de campo del descriptor (R-V3). Sigue el precedente ya aprobado de `Version.parsear` (`"version"`), así que no bloquea. Si en algún momento se usa un rango fuera del descriptor, el campo quedará mal atribuido. Un candidato natural es que `LectorDescriptor` (U01) asigne el campo.

## Tareas candidatas (defectos reales fuera de alcance)
- Ninguna nueva. `verificador/.jqwik-database` sigue sin rastrear y no está en el diff: es C-02, como indicó el orquestador. Es el único elemento sucio del worktree después de mi revisión, y ya estaba antes.

## Rutas de transcripciones largas
- La sonda de casos borde es `/tmp/claude-1000/-home-casdav-Projects-Master-SynKra/5b13ba94-befc-454f-88e7-3182922701e5/scratchpad/probe/Probe.java`. La salida relevante está citada en F-01. Se compiló y corrió fuera del worktree.

```
VEREDICTO: NO-VERDE
NARANJA|verificador/src/main/java/io/synkra/verificador/RangoVersion.java:17-19|\s acepta tabuladores y saltos de línea, que R-V2 manda a RANGO_INVALIDO
AMARILLO|verificador/src/test/java/io/synkra/verificador/RangoVersionTest.java:41-61|Falta prueba de espacios al inicio o al final en la forma intervalo
AMARILLO|verificador/src/main/java/io/synkra/verificador/RangoVersion.java:46|El campo "coreCompatible" va incrustado en el tipo de dominio
INFORME: revisiones/U01-T04/ronda-1.md
```
