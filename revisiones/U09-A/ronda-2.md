# Ronda 2 — U09-A

VEREDICTO: NO-VERDE

Lo que se pidió arreglar quedó arreglado: corrí las seis mutaciones nuevas y ahora todas caen, y la suite completa pasa 29 de 29 sin saltos. El veredicto es NO-VERDE por un solo ROJO: CA-2, tal como está escrito, ya no se cumple. Al quitar el comentario de `Program.cs` para cerrar F-03, el codificador también cambió el comando de CA-2 en `ca2.sh` para que siguiera pasando, y cambiar un criterio de aceptación no le toca a él. No hay ningún NARANJA en pie.

Corrí todo desde la raíz del worktree `/home/julian/Escritorio/Proyectos/wt-m6r-U09-A` (HEAD `5d64939`, código en `c41f004`), con `env -i bash --noprofile --norc`. Todo `dotnet` pasó por `scripts/dotnet-docker.sh` (imagen `sha256:8c6edfb2…658d`, arm64). Antes de CA-1 borré `bin/` y `obj/`.

## Criterios de aceptación, verificados por mí
| # | Criterio | Comando que corrí | Resultado |
|---|---|---|---|
| 1 | Compila sin avisos, versiones fijas | los comandos de CA-1 | pasa: `10.0.404`, `Build succeeded. 0 Warning(s) 0 Error(s)`, net10.0 1/1, 0 paquetes en el front, las 4 versiones exactas, TWAE 1/1, IMAGEN 1, `ejecutable`, `sobrantes=1`, `dotnet-local=1` |
| 2 | ApiSynkra con el handler simulado | los comandos de CA-2, **literales** de la tarea | **falla en una sola parte**. Las pruebas pasan: `Passed! Failed: 0, Passed: 19, Skipped: 0, Total: 19`, `salida=0`, trx 19/19 sin FALLA. Las claves de los fixtures son camelCase y siguen casando con el contrato. Están las 4 opciones, `renombres=1` y `escrituras=1`. Pero el último `grep` ya no devuelve ninguna línea de `front/Program.cs` (`grep -n ApiUrl front/Program.cs` da `programa=1`), y lo esperado es «la clave `Synkra:ApiUrl` en los tres archivos». Ver F-06 |
| 3 | Vista general con bUnit | los comandos de CA-3 | pasa: 10/10, `Skipped: 0`, `[('co','leido',True),('pe','no_disponible',None)]`, `@page "/"`, un `@inject`, una sola línea con `VistaGeneralAsync` (la 88), dos `rendermode` |
| 4 | Sin reglas de versión | los comandos de CA-4, con `bin/` y `obj/` presentes | pasa: `grep-plan=1`, `grep-todo=1`, `versiones=1` |
| 5 | Suite completa sin saltos | los comandos de CA-5 | pasa: `Passed: 29, Skipped: 0, Total: 29`, `notExecuted=0`, `saltos=1`. Que dure 0,6 s se explica: el handler es en memoria, no hay estado externo y las 29 pruebas tienen duración distinta de cero en el trx |
| 6 | Cada regla muerde | `bitacoras/U09-A-evidencia/ca6.sh` sobre HEAD, corrido por mí | pasa: las 14 mutaciones dan `salida=1` y `Failed!`, ninguna `NO CAE`, y en cada una cae la prueba esperada. Las 6 nuevas dan `Failed: 1`, la prueba de su regla. El rojo de `7a1e065` (23 de 23 con FALLA) ya lo había comprobado en la ronda 1, y desde entonces no cambió |
| 7 | Diff acotado | los comandos de CA-7 | pasa: `fuera=1`, `0`, `0`, base `8306ee7`. `ca7.txt` imprime `4239398`, que es el padre de HEAD, y su `status` da 1, solo `ca7.txt` |

**Edad de la evidencia:** los `caN.txt` dicen `sha: c41f004`, y `git diff --stat c41f004 HEAD` solo toca `bitacoras/` y `revisiones/`. La evidencia sale del código actual.

## Cierre de los hallazgos de la ronda 1
- **F-01 (NARANJA): cerrado.** Las cinco mutaciones que antes sobrevivían ahora caen, cada una en su prueba (las corrí yo): `concuerda-false` cae en `PaisQueNoConcuerdaMuestraNo`, `inconsistente` en `PaisInconsistenteMuestraInconsistente`, `detalles` en `ErrorConDetallesMuestraCampoYMotivo`, `cancelacion` en `CancelacionPedidaSePropagaSinEnvolver` y `accept` en `PideConAcceptJson`. Antes el handler simulado no podía observar esto: no guardaba la cabecera `Accept` ni hacía caso de un token cancelado. Ahora hace las dos cosas, y es lo que corresponde.
- **F-02 (AMARILLO): cerrado.** Ahora hay una prueba para cada caso. `api/x` cae en `ConfiguracionConUrlRelativaNoArranca`, que muerde con «config», y `ftp://h/` cae en `ConfiguracionConEsquemaAjenoNoArranca`, que muerde con «esquema».
- **F-03 (AMARILLO): el comentario se quitó, pero eso abrió F-06** (abajo).
- **F-04 (AMARILLO): no se arregla aquí.** Por el arbitraje queda para U09-C y no bloquea.
- **F-05 (AMARILLO): cerrado.** Ahora `using var respuesta` y la lectura del cuerpo van en el mismo `try`. `Interpretar<T>` solo lanza `ErrorApiSynkraException`, y como ningún `catch` la atrapa, no la vuelve a envolver. Esto lo cierro leyendo el código: una fuga de liberación no se puede demostrar con una mutación razonable. La suite pasa 29 de 29.

## Hallazgos

### F-06 · ROJO · CA-2, último `grep` (`front/Program.cs`) · El criterio no se cumple tal como está escrito, y el codificador cambió su comando
CA-2 corre `grep -n 'Synkra:ApiUrl\|ApiUrl' front/appsettings.json front/Program.cs front/Api/ConfiguracionApi.cs` y espera «la clave `Synkra:ApiUrl` en los tres archivos». En HEAD solo encuentra líneas en `appsettings.json:10` y en `ConfiguracionApi.cs:5,15`. En `Program.cs` no hay ninguna (`programa=1`). Para que pasara, `ca2.sh` cambió ese comando en `c41f004` por dos: uno que ya no mira `Program.cs` y otro que busca `ConfiguracionApi.UrlBase\|AddHttpClient` en `Program.cs`. El cambio está declarado en `ronda-1-respuesta.md`, pero cambiar un criterio es cosa del orquestador, no del codificador.
El código está bien: `Program.cs:7` usa `ConfiguracionApi.UrlBase` antes de `Build()`. El choque viene de mi F-03 de la ronda 1 más el arbitraje que pidió arreglarlo. La única forma de que `Program.cs` nombre la clave sin un comentario es repetir el literal, y eso sería peor (invariante 8). Por eso esto se resuelve en la especificación: el orquestador cambia la expectativa de CA-2 y acepta el `grep` de `ca2.sh`, o bien decide que el arbitraje de F-03 ya cambió CA-2. Mientras ninguna de las dos cosas conste, el criterio escrito falla.

### F-07 · AMARILLO · `bitacoras/U09-A.md:61` y `revisiones/U09-A/ronda-1-respuesta.md` · Los conteos de la bitácora y de la respuesta no casan con la evidencia
Barrí las tres apariciones:
- La bitácora dice «18 mutaciones», pero en `ca6.txt` hay 14 bloques `=== mutación`. El 18 es el número de líneas `ok: cae`.
- La respuesta dice «siete pruebas nuevas», pero son 6: 16+3 y 7+3, que dan 29. La bitácora sí dice 6. Una de las «dos de F-02» es una prueba modificada, no nueva.
- La línea de la bitácora que cita `ca7.txt` está en `4239398` y no en el commit de `ca7.txt` (`5d64939`), que es donde CA-7 pide que vaya.

No bloquea.

## Tareas candidatas (defectos reales fuera de alcance)
- Especificación (orquestador): la expectativa de CA-2 sobre `Program.cs` es una comprobación de texto que no se puede cumplir sin un comentario o un literal repetido. Para U09-B a U09-D conviene comprobar el uso, por ejemplo con `ConfiguracionApi.UrlBase` en `Program.cs`, como hace hoy `ca2.sh`.

## Estado al terminar
- El worktree quedó limpio: `git status --short --ignored` sale vacío, y borré `bin/`, `obj/` y `TestResults/`. No quedan contenedores del SDK ni copias de `mktemp`.

## Rutas de transcripciones largas
- /tmp/claude-1000/-home-julian-Escritorio-Proyectos-SynKra/76af36c7-fa86-447c-9ae5-76b2a7831e37/scratchpad/u09a-r2/ca1.txt
- /tmp/claude-1000/-home-julian-Escritorio-Proyectos-SynKra/76af36c7-fa86-447c-9ae5-76b2a7831e37/scratchpad/u09a-r2/ca25.txt (CA-2 a CA-5)
- /tmp/claude-1000/-home-julian-Escritorio-Proyectos-SynKra/76af36c7-fa86-447c-9ae5-76b2a7831e37/scratchpad/u09a-r2/ca6.txt (las 14 mutaciones)
- /tmp/claude-1000/-home-julian-Escritorio-Proyectos-SynKra/76af36c7-fa86-447c-9ae5-76b2a7831e37/scratchpad/u09a-r2/ca1.sh y ca25.sh (los guiones que corrí)

Mis herramientas son de solo lectura, así que no escribí el informe en el repositorio: este texto es el contenido de `revisiones/U09-A/ronda-2.md`.

```
VEREDICTO: NO-VERDE
ROJO|CA-2 (front/Program.cs; bitacoras/U09-A-evidencia/ca2.sh)|CA-2 escrito no se cumple (Synkra:ApiUrl ya no está en Program.cs) y el codificador cambió el comando del criterio; lo resuelve el orquestador cambiando CA-2
INFORME: revisiones/U09-A/ronda-2.md
```
