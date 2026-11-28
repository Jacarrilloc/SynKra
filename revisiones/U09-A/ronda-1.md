# Ronda 1 — U09-A

VEREDICTO: NO-VERDE

Corrí los siete criterios yo mismo y los siete pasan. El veredicto es NO-VERDE por un solo NARANJA: cinco comportamientos que piden las decisiones 7 y 12 no tienen ninguna prueba que los proteja. El más serio: si la pantalla muestra «Sí» cuando `concuerda` es `false`, la suite sigue pasando 23 de 23.

Lo corrí todo desde la raíz del worktree (`/home/julian/Escritorio/Proyectos/wt-m6r-U09-A`, HEAD `4429f9f`) con `env -i bash --noprofile --norc`. En el shell interactivo, `grep` es una función que envuelve ugrep con `--ignore-files` y se salta `bin/` y `obj/`, y eso habría falseado CA-4. Todo `dotnet` corrió con `scripts/dotnet-docker.sh`. Las mutaciones y el commit rojo los corrí en copias de `mktemp -d` (git archive), ya borradas.

## Criterios de aceptación, verificados por mí
| # | Criterio | Comando que corrí | Resultado |
|---|---|---|---|
| 1 | Compila sin avisos, versiones fijas | los comandos de CA-1 | pasa: `sha256:8c6edfb2…658d arm64`, `10.0.404`, `Build succeeded. 0 Warning(s) 0 Error(s)`, net10.0 1/1, 0 PackageReference en el front, las 4 versiones exactas, TWAE 1/1, IMAGEN 1, ejecutable (modo git 100755), `sobrantes=1`, `dotnet-local=1` |
| 2 | ApiSynkra con el HttpMessageHandler simulado | los comandos de CA-2 (compilación limpia, sin bin/obj) | pasa: `Passed! Failed: 0, Passed: 16, Skipped: 0, Total: 16`, `salida=0`, trx 16/16 con los 16 nombres, las 4 opciones, `renombres=1`, `escrituras=1`, la clave en los 3 archivos |
| 3 | Vista general con bUnit | los comandos de CA-3 | pasa: 7/7, `Skipped: 0`, `[('co','leido',True),('pe','no_disponible',None)]`, una línea con `VistaGeneralAsync`, ninguna con Historial, Despliegue ni HttpClient, 2 rendermode |
| 4 | Sin reglas de versión | los comandos de CA-4, con `front/bin` y `front/obj` presentes | pasa: `grep-plan=1`, `grep-todo=1`, `versiones=1` |
| 5 | Suite completa sin saltos | los comandos de CA-5 | pasa: 23/23, `notExecuted=0`, `saltos=1`. La duración (0,24 s) se explica: el handler es en memoria y no hay estado externo; las 23 pruebas tienen duración no nula en el trx |
| 6 | Rojo primero y mutaciones | `bitacoras/U09-A-evidencia/ca6.sh` del codificador, más el suite completo sobre una copia de `7a1e065` | pasa: las 8 mutaciones dan `salida=1` y `Failed!`, y cae la prueba esperada en cada una. Sobre `7a1e065`: `Failed: 23, Passed: 0`, las 23 con FALLA |
| 7 | Diff acotado | los comandos de CA-7 | pasa: `fuera=1`, `0`, `0`; el sha de `ca7.txt` (`0ff8944`) es el padre de HEAD |

**Contrato contra `main` 8306ee7.** Coteje `VistaPaisDto`, `PluginCorriendo`, `ResumenDespliegueDto`, `DespliegueDto`, `ContextoDto`, `PluginDesplegado`, `ErrorApi` y `DetalleError`, las enums en `toLowerCase`, los mensajes de `MotivosRegistro` y `CodigosError`, y la nulabilidad de `V3__registro.sql`. Los nombres de campo de `Modelos.cs` y de los fixtures coinciden uno a uno, sin snake_case. Las fechas se guardan en epoch con milisegundos (`toEpochMilli`), así que `.123Z` y `:00Z` son las dos formas reales. `ConfiguracionJson` no cambia la inclusión de nulos.

**Edad de la evidencia.** Los `caN.txt` dicen `sha: 0fd4b82`. `git diff --stat 0fd4b82 HEAD` solo toca `bitacoras/`, así que la evidencia sale del código actual.

## Desviaciones declaradas, juzgadas
1. .NET 10 en vez de 8: la fija la decisión 2. Aceptada.
2. HttpMessageHandler en vez de un servidor con puerto: la fija la decisión 9. Aceptada.
3. Interpretación de «estado del último despliegue»: la fija la decisión 12, y la pantalla no llama al historial (lo prueba la mutación «otra llamada»). Aceptada.
4. Proyecto escrito a mano, sin `Error.razor` ni `UseExceptionHandler`: la decisión 1 lo permite («o de la normal, quitando…»). Aceptada. Nada prueba que la aplicación arranque; ver F-04.
5. `<Using Include="Xunit" />`: hace falta para que compile y no añade paquetes. Aceptada.
6. `HistorialEscapaElPais` reforzada con `a/b?c`: justificada. Con solo `p e`, la mutación «escape» sobrevivía porque `Uri` ya normaliza el espacio, así que la prueba de la decisión 11 tal como está escrita no cumplía CA-6. Conserva el nombre y el caso `p e`. Aceptada. Conviene corregir la decisión 11 en la especificación.
7. Los commits rojo-corrección (`5b31de9`, `7a1e065`): solo tocan el csproj y un `!` de nulabilidad, y los stubs no cambian. Comprobé el rojo de `7a1e065` yo mismo. Aceptada.

## Hallazgos

### F-01 · NARANJA · decisiones 7 y 12 · Cinco comportamientos especificados no los protege ninguna prueba
Barrido de la clase. Corrí cada mutación en una copia de HEAD con la suite completa, y las cinco dan `Passed! Failed: 0, Passed: 23` y `salida=0`:
- `VistaGeneral.razor:108`, `false => "No"` cambiado a `false => "Sí"`: **sobrevive**. Ningún fixture trae `concuerda: false`. Es el caso que el North Star necesita ver: con esta mutación, un país que no concuerda se mostraría como «Sí».
- `VistaGeneral.razor:100`, `Inconsistente` cambiado a `"Leído"`: **sobrevive**. Ningún fixture trae `lectura: inconsistente`.
- `VistaGeneral.razor:17`, el `<li>{campo}: {motivo}</li>` de los detalles de error cambiado a `<li>x</li>`: **sobrevive**. Ninguna prueba de pantalla usa un error con detalles.
- `ApiSynkra.cs:50`, quitar `when (!ct.IsCancellationRequested)`: **sobrevive**. Nada prueba que una cancelación pedida se propague sin envolver (decisión 7).
- `ApiSynkra.cs:37`, quitar la cabecera `Accept: application/json`: **sobrevive**.

El código, leído, hace lo correcto, pero leerlo no es evidencia. Se arregla con pruebas adicionales; CA-2 y CA-3 admiten «o más».

### F-02 · AMARILLO · `front.Tests/ApiSynkraTest.cs` (ConfiguracionConUrlRelativaNoArranca) · La prueba de «URL relativa» en realidad prueba el esquema
En Linux, `Uri.TryCreate("/api", UriKind.Absolute)` da `file:///api`. Si quito la comprobación del esquema, esa prueba cae con `No exception was thrown`. La mutación «config» (`RelativeOrAbsolute`) solo cae porque `url.Scheme` lanza otra `InvalidOperationException` con un mensaje distinto, y la bitácora lo atribuye a medias. Conviene probar una URL relativa de verdad (`api/x`) y un esquema ajeno (`ftp://h/`) por separado.

### F-03 · AMARILLO · `front/Program.cs:6` · La clave aparece solo en un comentario
`Synkra:ApiUrl` está en `Program.cs` únicamente en un comentario, añadido para que el `grep` de CA-2 casara. El uso real va por `ConfiguracionApi.Clave`. No hace daño, pero el `grep` comprueba texto, no uso.

### F-04 · AMARILLO · `front/Program.cs` · Ninguna prueba arranca la aplicación
No hay prueba de humo del arranque, y tampoco se comprueba que una URL mala impida arrancar, cosa que solo sostiene la lectura del orden antes de `Build()`. La compilación pasa. Encaja en U09-C (imagen).

### F-05 · AMARILLO · `front/Api/ApiSynkra.cs:44-55` · La respuesta puede quedar sin liberar
Si `ReadAsStringAsync` lanza, `respuesta` no se libera, porque el `using (respuesta)` empieza después del `try`. Es menor.

## Tareas candidatas (defectos reales fuera de alcance)
- Especificación: la decisión 11 describe `HistorialEscapaElPais` solo con `"p e"`, y así la mutación «escape» de CA-6 no puede caer. Hay que corregirla para U09-B a U09-D.

## Estado al terminar
- Worktree limpio: `git status --short --ignored` sale vacío. Borré `bin/`, `obj/` y `TestResults/`.
- No queda ningún contenedor de la imagen del SDK (`--rm`) ni copias temporales en `/tmp`.

## Rutas de transcripciones largas
- /tmp/claude-1000/-home-julian-Escritorio-Proyectos-SynKra/76af36c7-fa86-447c-9ae5-76b2a7831e37/scratchpad/ca1.txt
- /tmp/claude-1000/-home-julian-Escritorio-Proyectos-SynKra/76af36c7-fa86-447c-9ae5-76b2a7831e37/scratchpad/ca25.txt (CA-2 a CA-5)
- /tmp/claude-1000/-home-julian-Escritorio-Proyectos-SynKra/76af36c7-fa86-447c-9ae5-76b2a7831e37/scratchpad/ca6.txt
- /tmp/claude-1000/-home-julian-Escritorio-Proyectos-SynKra/76af36c7-fa86-447c-9ae5-76b2a7831e37/scratchpad/extra.txt (rojo de 7a1e065 y mis 7 mutaciones adicionales)
- /tmp/claude-1000/-home-julian-Escritorio-Proyectos-SynKra/76af36c7-fa86-447c-9ae5-76b2a7831e37/scratchpad/extra.sh y ca25.sh (los guiones que corrí)

No pude escribir el informe en el repositorio porque mis herramientas son de solo lectura: el texto de arriba es el contenido de `revisiones/U09-A/ronda-1.md`.

```
VEREDICTO: NO-VERDE
NARANJA|front/Components/Pages/VistaGeneral.razor:100,108,17; front/Api/ApiSynkra.cs:37,50|Cinco comportamientos de las decisiones 7 y 12 sin prueba que los proteja (concuerda=false, Inconsistente, detalles de error, cancelación pedida, cabecera Accept)
INFORME: revisiones/U09-A/ronda-1.md
```
