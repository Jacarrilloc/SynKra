# Ronda 3 — U09-A

VEREDICTO: NO-VERDE

En el código y en el diff todo está en verde. Corrí los siete criterios en x86_64, con su comando literal, y el resultado es el mismo que en aarch64: 29 de 29 pruebas sin saltos, las 14 mutaciones caen, el rojo primero da 23 de 23 en falla, y el diff está acotado. La enmienda del CA-2 cierra F-06.

El veredicto es NO-VERDE por un solo ROJO, y no es de código. Dos de los valores esperados de CA-1 dependen de la máquina y en esta no se cumplen: `arm64` y `dotnet-local=1`. Además, el brief parte de un supuesto falso: **esta máquina sí tiene un SDK de .NET instalado** (`/usr/bin/dotnet`, SDK 10.0.115, del paquete `dotnet-host-10.0`, en `/usr/lib/dotnet/sdk` desde el 2026-11-11). Para resolverlo basta una enmienda del orquestador. El codificador no tiene nada que hacer.

Todo lo corrí desde la raíz de `/home/casdav/Projects/Master/wt-m6r-U09-A`, con HEAD en `bfbe91b`, `env -i bash --noprofile --norc` y cada `dotnet` dentro de `scripts/dotnet-docker.sh`. El worktree estaba limpio al empezar, incluidos los ignorados. Hash de la tarea verificado: `d5a5ff409c3f520da348c01bc08f238cfe4bbb8c`.

## Criterios de aceptación, verificados por mí
| # | Criterio | Comando que corrí | Resultado |
|---|---|---|---|
| 1 | Compila sin avisos, con las versiones fijas | los comandos de CA-1, literales | **falla en dos líneas que dependen de la máquina** (F-08). Lo demás pasa: `RepoDigests` `sha256:8c6edfb2…658d` (el mismo de la bitácora), `10.0.404`, `Build succeeded.`, `0 Warning(s)`, `0 Error(s)`, net10.0 1 y 1, 0 paquetes en el front, las 4 versiones exactas, TWAE 1 y 1, IMAGEN 1, `ejecutable`, `sobrantes=1`. Las dos que fallan: la arquitectura sale **`amd64`** (se esperaba `arm64`) y `command -v dotnet` da `/usr/bin/dotnet` con **`dotnet-local=0`** (se esperaba 1) |
| 2 | ApiSynkra con el handler simulado (CA-2 enmendado) | los comandos de CA-2, literales del hash nuevo | pasa: `Passed! Failed: 0, Passed: 19, Skipped: 0, Total: 19`, `salida=0`, trx `total=19 … notExecuted=0` sin FALLA, con los 16 nombres de la decisión 11 más 3. Las claves de los fixtures son camelCase y las coteje con `git show 8306ee7:` de `VistaPaisDto`, `ResumenDespliegueDto`, `DespliegueDto`, `ContextoDto`, `PluginCorriendo`, `PluginDesplegado`, `ErrorApi` y `DetalleError`: casan exactamente. Las 4 opciones están (líneas 12 a 15), `renombres=1`, `escrituras=1`, la clave aparece en `appsettings.json:10` y en `ConfiguracionApi.cs:5,15`, y `Program.cs:7,10` tiene `UrlBase` y `AddHttpClient` |
| 3 | Vista general con bUnit | los comandos de CA-3 | pasa: `Passed: 10, Skipped: 0, Total: 10`, `salida=0`, los 7 nombres más 3, `[('co', 'leido', True), ('pe', 'no_disponible', None)]`, `@page "/"`, un `@inject ApiSynkra`, una sola línea con `VistaGeneralAsync` (la 88) y dos `rendermode` |
| 4 | Sin reglas de versión | los comandos de CA-4, con `bin/` y `obj/` presentes | pasa: `Build succeeded.`, `grep-plan=1`, `grep-todo=1`, `versiones=1` |
| 5 | Toda la suite sin saltos | los comandos de CA-5 | pasa: `Passed: 29, Skipped: 0, Total: 29`, `salida=0`, trx 29/29 con `notExecuted=0`, `saltos=1`. Tarda 339 ms porque el handler es en memoria y no hay estado externo |
| 6 | Cada regla muerde | `bitacoras/U09-A-evidencia/ca6.sh`, con `TMPDIR` en mi scratchpad | pasa: las 14 mutaciones dan `salida=1` y `Failed!`, y en cada una cae la prueba de su regla (`ok: cae`, ninguna `NO CAE`, ninguna `error CS`). Rojo primero: en una copia de `7a1e065` (por `git archive`), `Failed: 23, Passed: 0, Total: 23`, con 23 líneas FALLA y los stubs con `NotImplementedException` |
| 7 | Diff acotado | los comandos de CA-7 | pasa: `fuera=1`, `0`, `0`, y `git merge-base` = `8306ee7`. Desde `c41f004` solo cambian `bitacoras/` y `revisiones/`. `ca7.txt` imprime `4239398`, el padre de `5d64939`, con status 1 |

**Edad de la evidencia:** los seis `caN.txt` de CA-1 a CA-6 dicen `sha: c41f004`, y desde ese commit no ha cambiado ningún archivo de código. La evidencia sale del código actual.

**Lo que depende de la arquitectura:**
- La única diferencia es la palabra `arm64` de CA-1 y de las cabeceras `imagen:` de los `caN.txt`.
- El `RepoDigests` es el de la lista de manifiestos, así que da el mismo valor en las dos máquinas: el pin se sostiene en ambas.
- Los conteos, los nombres de las pruebas y las mutaciones salen idénticos en amd64. Ningún resultado de prueba depende de la arquitectura.

## Las respuestas de la ronda 2 se sostienen
- **F-06 (ROJO): cerrado por la enmienda del orquestador** (audit, 2026-11-28T06:10Z). El CA-2 enmendado pasa con su comando literal.
- **F-01, F-02 y F-05 (cerrados en la ronda 2): siguen cerrados.** Volví a correr las 6 mutaciones nuevas y cada una cae en su prueba. `Interpretar<T>` y `using var respuesta` van dentro del mismo `try`.
- **F-04:** sigue fuera de alcance, para U09-C, por el arbitraje.
- **F-07 (AMARILLO): sigue en pie, sin cambios.** La bitácora y la respuesta no han cambiado.

## Hallazgos

### F-08 · ROJO · CA-1 (`arm64` y `dotnet-local=1`) · CA-1 espera hechos de la máquina aarch64 y en esta máquina no se cumplen
- **Lo que pasa.** En esta máquina (`uname -m` da `x86_64`), CA-1 literal imprime `… 8c6edfb2…658d amd64`, después `/usr/bin/dotnet` y `dotnet-local=0`. Que haya SDK local se ve en estas salidas:
  - `dotnet --list-sdks`: `10.0.115 [/usr/lib/dotnet/sdk]`
  - `dpkg -S`: `dotnet-host-10.0`
  - el directorio del SDK tiene fecha 2026-11-11, de antes de la tarea.
- **Por qué no es culpa del diff.** Los 14 commits de `8306ee7..HEAD` son de Julian Carrillo, hechos en aarch64. Su `ca1.txt` muestra `arm64` y `dotnet-local=1`. En mi corrida, todo `dotnet` pasó por el contenedor (las rutas de salida son `/src/...`).
- **Por qué es ROJO igual.** Un criterio escrito que no se cumple en la ejecución del revisor no puede ir a VERDE. Es el mismo criterio que apliqué en F-06.
- **El patrón de las tres rondas.** F-03 y luego F-06 en el CA-2, y ahora F-08 en el CA-1: son criterios que comprueban un texto o un entorno en lugar de un comportamiento. Se rompen cuando cambia algo que no es el código: un comentario en la ronda 2, la máquina en la ronda 3.
- **Cómo se resuelve (lo decide el orquestador).** Cualquiera de estas dos opciones:
  - (a) enmendar el CA-1 para que acepte `amd64|arm64`, y para que `dotnet-local` se refiera a la máquina del codificador, con la evidencia de su `ca1.txt` y la autoría de los commits;
  - (b) que el CA-1 lo vuelva a correr alguien en la máquina aarch64.

  Con cualquiera de las dos, y sin cambiar el código, el veredicto pasa a VERDE.
- **Aviso para el orquestador.** Para U09-B a U09-D en la máquina de David, el supuesto «no hay SDK» es falso. El SDK local existe, aunque la decisión 3 prohíbe usarlo.

### F-07 · AMARILLO · `bitacoras/U09-A.md:61`, `ronda-1-respuesta.md` · Los conteos de la bitácora no casan con la evidencia (viene de la ronda 2)
- La bitácora dice «18 mutaciones», y son 14 bloques.
- La respuesta dice «siete pruebas nuevas», y son 6.
- La línea de la bitácora que cita `ca7.txt` está en `4239398`, no en `5d64939`.

No bloquea.

## Tareas candidatas (defectos reales fuera de alcance)
- C-135 (propuesta, especificación): los criterios de entorno de U09 (arquitectura y «sin SDK local») deberían ser neutros respecto a la máquina, o declarar en qué máquina se corren. La máquina x86_64 de David tiene el SDK 10.0.115 del sistema.

## Estado al terminar
- Worktree limpio: `git status --short --ignored` sale vacío. Borré los `bin/`, `obj/` y `TestResults/` que generó mi corrida; al empezar no estaban.
- No quedan contenedores del SDK, ni copias en mi `TMPDIR`. Borré la copia del rojo.

## Rutas de transcripciones largas
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/9d987332-269f-4da9-9d66-0a5449231723/scratchpad/U09-A-r3/ca1.txt (y ca1.sh)
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/9d987332-269f-4da9-9d66-0a5449231723/scratchpad/U09-A-r3/ca25.txt (CA-2 a CA-5; y ca25.sh)
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/9d987332-269f-4da9-9d66-0a5449231723/scratchpad/U09-A-r3/ca6.txt (las 14 mutaciones)
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/9d987332-269f-4da9-9d66-0a5449231723/scratchpad/U09-A-r3/rojo.txt (`7a1e065`, 23 de 23 en falla)

No escribí nada en el repositorio, porque mis herramientas son de solo lectura. Este texto es el contenido de `revisiones/U09-A/ronda-3.md`.

```
VEREDICTO: NO-VERDE
ROJO|CA-1 (arm64 y dotnet-local=1)|CA-1 espera hechos de la máquina aarch64: en x86_64 da amd64 y hay SDK local en /usr/bin/dotnet; el código está bien y lo resuelve una enmienda del orquestador
INFORME: revisiones/U09-A/ronda-3.md
```
