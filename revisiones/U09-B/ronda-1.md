# Ronda 1 — U09-B

VEREDICTO: VERDE

La tarea cumple los siete criterios. Los corrí todos yo, con el CA-5 enmendado, sobre la punta 530058b (código de 8d7a11e, base 367e09f). No queda ningún ROJO ni NARANJA. Hay cuatro AMARILLOS, que no bloquean. El hash de la tarea es el indicado: `git hash-object` da d8040d5f4ea6778623ee254cff508bb223b6eb07. Todo `dotnet` corrió en `scripts/dotnet-docker.sh` con la imagen `sdk@sha256:8c6edfb26c85…658d`. No toqué el clúster.

## Criterios de aceptación, verificados por mí
| # | Criterio | Comando que corrí | Resultado |
|---|---|---|---|
| 1 | Base y los tres archivos comunes | bloque literal del CA-1 | pasa. `base=0` y `0`. Blobs `899488ab…`, `85479d56…`, `d5d7babb…`. Exactamente 4 líneas (2 `-` y 2 `+` de la decisión 1). Los tres blobs son iguales en la punta de `m6r/U09-C` (ad1faf9) y ya estaban así en el commit rojo 021de37. |
| 2 | Compila sin avisos y sin paquetes nuevos | bloque literal del CA-2 | pasa. `Build succeeded.`, `0 Warning(s)`, `0 Error(s)`, `.csproj` cambiados = 0 |
| 3 | Lecturas del catálogo y pantalla «Catálogo» | bloque literal del CA-3 | pasa. `Passed! Failed 0, Passed 18, Skipped 0, Total 18`, `salida=0`. `trx.py` da 18/18/0/0 con los 18 nombres de las decisiones 4 y 11. El `python3` da la lista esperada, exacta. |
| 4 | Pantalla «Detalle», «Vista general» y menú | bloque literal del CA-4 | pasa. `Passed! 27/27, Skipped 0`, `salida=0`. Salen los 14 nombres de `DetalleTest`, 12 de `VistaGeneralTest` (10 de base y 2 nuevos) y 1 de `MenuTest`. |
| 5 | Suite completa, U09-T06 y ninguna escritura (enmendado) | bloque literal del CA-5 con el `grep` enmendado | pasa. `Passed! 64/64, Skipped 0`, `salida=0`. `trx.py` da `total=64 … notExecuted=0`. Luego `saltos=1`, `resto=1`, `versiones=1`, `escrituras=1`. El `grep` del plan da 3 líneas: `ModelosCatalogo.cs:11`, `Catalogo.razor:96` (`@p.CoreCompatible`) y `Catalogo.razor:84` (`<th>Core compatible</th>`, ver F-02). |
| 6 | Rojo primero y cada regla muerde | `ca6.sh` y `rojo.sh 021de37` del codificador, corridos por mí en copias bajo el scratchpad | pasa. Las 13 mutaciones dan `salida=1` y `Failed!`, con la prueba esperada en `FALLA`. Rojo: CA-3 `Failed!` 10/18 (las 8 de `ApiSynkraCatalogoTest` pasan, como prevé la decisión 14) y CA-4 `Failed!` 17/27 (las 10 que pasan son las de U09-A). |
| 7 | Diff acotado | bloque literal del CA-7 | pasa. `fuera=1`, `0`, `0`, `0`. El commit de `ca7` solo trae `ca7.sh`, `ca7.txt` y la bitácora. `ca7.txt` imprime 327d27b, que es el padre. |

Los tiempos de 370 a 400 ms son normales: es bUnit en memoria sin estado externo, y el trx muestra `executed=64`.

## Reparto con U09-C (decisión 1)
- La intersección entre `git diff --name-only 367e09f...m6r/U09-B` y `367e09f...m6r/U09-C` son exactamente los tres archivos comunes, con blobs idénticos.
- `git merge-tree --write-tree m6r/U09-B m6r/U09-C` termina con salida 0, sin conflictos.
- No se toca ningún archivo de U09-C (`ManejadorHttpSimulado.cs`, `Escrituras`, `ModelosDespliegue`, `Desplegar`, `Reversion`, `SubirPlugin`).

## Los cuatro puntos declarados por el codificador
- **«filtra» y el refuerzo de `NoFiltraNiComparaVersiones`: acepto.** Corrí la mutación literal sobre una copia de efcf909, antes del refuerzo: `Passed! 64`, `salida=0`, sobrevive. Con el fixture de la decisión 5, `synkra-core` tiene versiones 2 y 3, así que la mutación no oculta nada. El refuerzo solo añade líneas a una prueba propia, usa fixtures existentes y hace que la mutación muerda. Que la mutación literal no muerda con el fixture de la tarea es un defecto de la especificación, para el orquestador.
- **«enlace-vista» como `@($"#{id}")`: acepto.** Dentro del bloque `@if { }` de Razor, una línea que empieza con `#` es una directiva de preprocesador. La mutación compila y cae `UltimoDespliegueEnlazaAlDetalle`.
- **`[Parameter]` en el commit rojo: acepto.** `Render<Detalle>(p => p.Add(x => x.Id …))` no compila sin ellos, y las rutas `{Id:long}` y `{Pais}` los exigen. Solo están en `Detalle.razor`: `Catalogo.razor` en 021de37 no lleva ninguno (ver F-04).
- **Dos fixtures extra: acepto.** Los piden los nombres de la decisión 11 (`catalogo-cores-raro.json`, `catalogo-plugins-etiquetas-desordenadas.json`). Están dentro del prefijo permitido, y `ManejadorHttpSimulado.cs` no cambia.
- Cotejé los nombres de campo de los fixtures `catalogo-*` y `detalle-*` con `RutasCores.java`, `RutasPlugins.java`, `RutasRegistro.java` y `VeredictoDto.java` de 0910a09. Coinciden.

## Hallazgos

### F-01 · AMARILLO · bitacoras/U09-B-evidencia/ca5.txt y ca5.sh; bitacoras/U09-B.md desviación 1 · La evidencia del CA-5 es anterior a la enmienda
`ca5.sh:9` corre el segundo `grep` sin el filtro enmendado, y `ca5.txt:35` registra `resto=0`. La bitácora sigue describiendo la contradicción como abierta y su acuse cita el hash viejo (d2852e7…). No falta evidencia: es la del código actual (8d7a11e) y mi ejecución del comando enmendado da `resto=1`. Pero el registro versionado no muestra el criterio vigente cumplido. Conviene regenerar `ca5` con el comando enmendado y anotar la enmienda en la bitácora. No hace falta tocar código.

### F-02 · AMARILLO · tarea, CA-5, texto «Esperado» del `grep` del plan · La enmienda dejó una frase contradictoria (es del orquestador, no del codificador)
El CA-5 sigue diciendo que el `grep` del plan «solo da líneas con `CoreCompatible`», y la decisión 10 dice «ningún texto … con compatib». Pero la decisión 6 obliga a `<th>Core compatible</th>`, que sale en ese `grep` (`Catalogo.razor:84`). La enmienda del 2026-11-28 solo reescribió el segundo `grep`. Lo juzgo cumplido por la intención expresa de la enmienda («el texto visible de una cabecera … no cuenta»). Convendría alinear la redacción en U09-B y U09-C.

### F-03 · AMARILLO · front/Components/Pages/VistaGeneral.razor:50 · El escape del país en el enlace de «Vista general» no tiene prueba
Probé una mutación propia: `paises/@Uri.EscapeDataString(p.Pais)` cambiado a `paises/@p.Pais`. Da `Passed! 64`, sobrevive. La decisión 7 manda escapar y C-133 pide probar los escapes con `/` y `?`. El riesgo real es bajo, porque la API valida `[a-z]{2}` (`RutasRegistro.PAIS`), y la decisión 13 no lo exige.

### F-04 · AMARILLO · bitacoras/U09-B.md · Dos inexactitudes en la bitácora
1. Dice que `Detalle.razor` y `Catalogo.razor` llevan `[Parameter]` en el commit rojo. Solo `Detalle.razor` los lleva (`git show 021de37:front/Components/Pages/Catalogo.razor`).
2. `catalogo-cores.json` usa `<64 f>` para la imagen de `3.0.0`. La decisión 5 dice «cada una con `<64 a>`». Las imágenes distintas son mejores para la prueba, pero la desviación no está anotada.

### Observación (no es hallazgo)
La variante «el contexto solo se muestra si el estado es Fallido y además la API lo trae» sobrevive (`Passed! 64`). Bajo el contrato real es una mutación equivalente: `RegistroServicio.actualizarResultado` prohíbe contexto en `LISTO` y lo exige en `FALLIDO` (R-R3). La mutación de la tarea sí muerde.

## Tareas candidatas
- Ninguna nueva fuera de alcance. El fixture de la decisión 5, que no puede mostrar la mutación «filtra», es para el orquestador (ya lo resuelve el refuerzo).

## Estado del worktree
`git status --short --ignored` sale igual que al empezar: solo `bin/`, `obj/` y `TestResults/` ignorados, y HEAD sigue en 530058b. Las copias de las mutaciones se borraron.

## Transcripciones, en /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/9d987332-269f-4da9-9d66-0a5449231723/scratchpad/U09-B-r1/
- `ca2.txt`, `ca3.txt`, `ca4.txt`, `ca5.txt`: salidas de compilación y pruebas.
- `ca6-mio.txt`: las 13 mutaciones.
- `rojo-mio.txt`: el commit rojo 021de37.
- `mias.sh` y `mias.txt`: mis mutaciones propias (`filtra` sobre efcf909, contexto y estado, país sin escape).

```
VEREDICTO: VERDE
AMARILLO|bitacoras/U09-B-evidencia/ca5.txt:35|La evidencia del CA-5 es anterior a la enmienda (resto=0, comando viejo)
AMARILLO|tarea CA-5 «Esperado» del grep del plan|La enmienda dejó una frase contradictoria con la decisión 6
AMARILLO|front/Components/Pages/VistaGeneral.razor:50|El escape del país en el enlace no tiene prueba
AMARILLO|bitacoras/U09-B.md|[Parameter] atribuido a Catalogo.razor en el rojo; imagen <64 f> no anotada
INFORME: revisiones/U09-B/ronda-1.md
```
