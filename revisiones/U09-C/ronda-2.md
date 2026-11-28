# Ronda 2 — U09-C

VEREDICTO: VERDE

Corrí yo los ocho criterios y todos pasan. Cayeron todas las mutaciones que sobrevivían en la ronda 1, y no queda ningún ROJO ni NARANJA. Hay cuatro hallazgos AMARILLO nuevos (F-09 a F-12). Ninguno bloquea, pero F-09 y F-12 piden una decisión del orquestador.

**Comprobaciones previas**
- **Tarea:** el hash de la tarea es 81b92afc69bc8ba3e1c402f0ab8eca2d2eb36777.
- **Rama:** HEAD está en 572d601. El último commit de código es f49d08d. La base es 367e09f (`base=0`).
- **Fusión con `origin/main` (00c54ef, con U09-B):** `git merge-tree` sale limpio, con el árbol e63fd4f.
  - Los tres archivos comunes conservan sus blobs (899488ab…, 85479d56…, d5d7babb…) en HEAD, en `origin/main` y en el árbol unido. Son los únicos archivos de `front/` y `front.Tests/` que tocan las dos ramas.
  - La suite completa sobre una copia de la unión da 117/117 con `Skipped: 0`. En la unión, `--filter CatalogoTest` da 25 y `--filter DetalleTest` da 21.
- **Entorno:** x86_64, sdk:10.0.404 (sha256:8c6edfb2…). No toqué `kind-synkra`.
- **Edad de la evidencia:** `ca1.txt` a `ca7.txt` se generaron entre las 07:40:25Z y las 07:40:52Z, después de f49d08d (07:40:23Z). `ca8.txt` se generó a las 07:46:27Z, con el sha 5502e94, que es el padre de 572d601.
  - Las 25 mutaciones de `ca7.txt` entran en 5 minutos y medio. Es plausible: mis copias tardan unos 13 s cada una.

## Criterios de aceptación, verificados por mí
| # | Criterio | Comando que corrí | Resultado |
|---|---|---|---|
| 1 | base y archivos comunes | los literales de CA-1 | pasa: `base=0`, `0`, los 3 blobs y 4 líneas (2 `-`, 2 `+`) |
| 2 | compila; escrituras acotadas | literal, con `--no-incremental` | pasa: `Build succeeded`, 0 Warning(s), 0 Error(s); `0` `.csproj`; el grep da solo `ApiSynkra.cs`, `ApiSynkra.Escrituras.cs` y `Program.cs` |
| 3 | `ApiSynkraEscriturasTest` | literal | pasa: 16/16, Skipped 0, `salida=0`. Las claves de los 4 fixtures no cambian respecto de la ronda 1 (ya cotejadas con el contrato) |
| 4 | `DesplegarTest` | literal | pasa: 21/21, Skipped 0, `salida=0`. Están los 17 nombres de la decisión 11 y 4 más del arbitraje (ver F-12) |
| 5 | Catalogo / Detalle / Arranque | literal | pasa: 7 (6 más `ArchivoPorEncimaDelTope…`), 7 y 2. Las tres salidas en 0, Skipped 0 |
| 6 | suite completa y U09-T06 | literal, sin el `rm -rf` (uso nombres de trx nuevos) | pasa: 82/82, `failed=0 notExecuted=0`, `saltos=1`, `grep-plan` solo con `CoreCompatible` y `Compatible`, `resto=1`, `versiones=1` |
| 7 | rojo primero y cada regla muerde | mis 17 mutaciones de CA-7 y 8 de la ronda 1, en copias `git archive` + `git init -q` | pasa: las 25 caen en su prueba. El rojo de 1bd21da ya lo verifiqué en la ronda 1 y no ha cambiado |
| 8 | diff acotado | literal | pasa: `fuera=1`, `0`, `0`. El commit 572d601 trae solo `ca8.txt` y la línea de la bitácora |

## Respuestas a la ronda 1, verificadas con mis mutaciones
| Hallazgo | Mutación | Resultado |
|---|---|---|
| F-01 Desplegar | `ConfirmarAsync(_plan.Id, null)` | cae `ConfirmarLlamaALaConfirmacionDelPlanYEnlazaAlDetalle` |
| F-01 Reversion | `ConfirmarAsync(_plan.Id, null)` | cae `LaVueltaSoloSeConfirmaDespuesDeConfirmar` |
| F-02 | `x-pais-no-descarta` | cae `CambiarElPaisDescartaElPlan` |
| F-02 | `x-filtro-no-desmarca` | cae `UnFiltroQueOcultaUnPluginMarcadoLoDesmarcaYVuelveAVerificar` |
| F-02 | `x-filtro-sin-igual-ignora` | cae `UnFiltroSinIgualMandaLaClaveConValorVacio` |
| F-03 | `@onclick="PrepararAsync"` en «Cancelar» | cae `CancelarNoLlamaANada` |
| F-07 | `x-preparar-sin-clave` | cae `UnPlanQueLlegaTrasCambiarLaSeleccionNoSeMuestra` |
| F-07 | sin el `catch (IOException)` | cae `ArchivoPorEncimaDelTopeMuestraUnErrorYNoLlamaALaApi` |

Probé además variantes nuevas:
- `@onclick="() => {}"` en «Cancelar»: cae.
- Mandar el filtro sin `=` como clave y valor a la vez: cae.
- Que el filtro desmarque pero no vuelva a verificar: cae.

F-06: los dos fixtures ahora tienen los textos literales de `MotivosDespliegue.SELECCION_CORE` y `USUARIO_FORMA` en `origin/main`. Los `campo` también coinciden: `core` en `VerificacionVivo.java:48` y `X-Synkra-Usuario` en `Usuario.java:31`. Revisé además todos los fixtures de error y sus encabezados (`codigo` y `mensaje`) coinciden con `main`. Queda un detalle que no coincide: F-09.

## Hallazgos
### F-09 · AMARILLO · `front.Tests/Fixtures/desplegar-error-combinacion.json:5`, `ApiSynkraEscriturasTest.cs:173`, `DesplegarTest.cs:293` · El detalle de `COMBINACION_RECHAZADA` lleva un prefijo que la API no manda
Es de la misma clase que F-06 y se me escapó en el barrido de la ronda 1.
- El fixture pone `"motivo": "VERSION_INCOMPATIBLE: El plugin 'kyc-pe' …"`.
- La API manda solo el mensaje. En `PlanServicio.java:144-145` (`origin/main`, sin cambios desde 0910a09), `detalle(m)` es `new DetalleError("plugins." + n, m.mensaje())`, sin el código. `CrearPlanTest.java:251` lo fija: el `motivo` es igual al `mensaje` del motivo.
- Las dos pruebas afirman `StartsWith("VERSION_INCOMPATIBLE: ")`, así que codifican un contrato falso.
- El formato `<CODIGO>: <mensaje>` solo es real en la subida (`SubidaPlugin`).
- El front muestra el detalle tal cual, así que el comportamiento no se ve afectado. El arreglo es quitar el prefijo del fixture y de las dos aserciones. Lo decide el orquestador.

### F-10 · AMARILLO · `DesplegarTest.cs:452` · Espera fija nueva de 300 ms, que se puede evitar
`UnPlanQueLlegaTrasCambiarLaSeleccionNoSeMuestra` añade un `Task.Delay(300)`. Es un caso nuevo, no la espera de F-05, que pasó a C-142. Aquí sí hay una condición determinista: cuando el plan soltado se procesa, «Preparar» vuelve a estar habilitado.
- Lo probé en una copia, cambiando la línea por `cut.WaitForAssertion(() => Assert.False(Deshabilitado(cut, "preparar")));`.
- Con el código actual pasa (`base=0`). Con `x-preparar-sin-clave` cae (`mut=1`).
- La espera de 250 ms de `ArranqueTest.cs:91` es un sondeo con plazo y condición, así que está bien.

### F-11 · AMARILLO · `Desplegar.razor:292`, `SubirPlugin.razor:100` · Dos detalles sin prueba
- `x-pais-no-limpia-confirmado` sobrevive con 82/82. Si después de confirmar se cambia de país y `_confirmado` no se limpia, «Preparar» queda deshabilitado para siempre (`PuedePreparar` exige `_confirmado is null`). La regla no está escrita de forma explícita en la decisión 7.
- `x-ioexception-sin-mensaje` sobrevive: la prueba solo mira el prefijo «No se pudo leer el archivo:». Además, el texto que sigue es el mensaje en inglés de .NET.

### F-12 · AMARILLO (para el orquestador) · CA-4, CA-5, CA-6 · Los conteos de la tarea quedaron atrás
La tarea pide 17, 6 y 77. Hay 21, 7 y 82, por las pruebas que pidió el arbitraje. La bitácora lo declara en la línea 57. Los nombres de la decisión 11 están todos, sin saltos ni fallos, así que lo cuento como pasa. Hay que enmendar los números en la tarea, y en U09-D la unión da 117.

## Tareas candidatas (defectos reales fuera de alcance)
- Siguen las de la ronda 1, ya en C-142: el arranque contra una API falsa que sí escuche, y `core.*` en `front/.gitignore`.
- La política de errores no esperados de la tarea `_ = VerificarAsync(...)`, que el codificador dejó en C-142.

## Limpieza
- No queda ningún proceso `Synkra.Front.dll`.
- Los únicos contenedores vivos son los tres de kind, que no son míos.
- No hay archivos `core.*` ni en el worktree ni en el scratchpad. Las copias de mutación y de la unión se borraron.
- `git status --short --ignored` sale idéntico al de antes y `--porcelain` da 0. Solo añadí `r2*.trx` dentro de `front.Tests/TestResults/`, que ya existía y está ignorado.

## Rutas de transcripciones largas
Todas en /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/9d987332-269f-4da9-9d66-0a5449231723/scratchpad/U09-C-r2/:
- `ca1-ca2.txt`, `ca3-ca5.txt`, `ca3-ca6.txt`, `ca8.txt`
- `ca7-revisor.txt` y `ca7-revisor-extra2.txt`: las mutaciones
- `copias/*.log`: un log por mutación
- `union-todo.log`, `union-CatalogoTest.log`, `union-DetalleTest.log`
- `espera-base.log` y `espera-mut.log`: la sonda de F-10
- `bin/mut.sh`, `bin/filtros.sh`, `bin/ca6.sh`: los scripts que corrí
- `status-antes.txt` y `status-fin.txt`

```
VEREDICTO: VERDE
AMARILLO|Fixtures/desplegar-error-combinacion.json:5|El detalle de COMBINACION_RECHAZADA lleva un prefijo VERSION_INCOMPATIBLE: que la API no manda
AMARILLO|DesplegarTest.cs:452|Espera fija nueva de 300 ms que se puede cambiar por una condición determinista
AMARILLO|Desplegar.razor:292, SubirPlugin.razor:100|Sin prueba: limpiar la confirmación al cambiar de país, y el mensaje de la IOException
AMARILLO|CA-4/CA-5/CA-6|Conteos de la tarea (17/6/77) desactualizados por el arbitraje (21/7/82)
INFORME: revisiones/U09-C/ronda-2.md
```
