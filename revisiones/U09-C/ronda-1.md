# Ronda 1 — U09-C

VEREDICTO: NO-VERDE

Los ocho criterios pasan cuando los corro yo. Lo que bloquea son tres hallazgos NARANJA, todos de cobertura: mis mutaciones extra rompen reglas de la tarea y la suite sigue en 77/77. No hay ningún ROJO. El código hace lo correcto en los tres casos; lo que falta son las pruebas que lo sostengan.

Comprobaciones previas:
- **Tarea:** `git hash-object` da 81b92afc69bc8ba3e1c402f0ab8eca2d2eb36777, el hash enmendado.
- **Rama:** punta 43fb96c, último commit de código ee83866, base 367e09f.
- **Fusión con `origin/main` (00c54ef, con U09-B):** `git merge-tree` sale limpio (árbol 82f91d8). Los tres archivos comunes tienen en HEAD y en `origin/main` los blobs fijados: 899488ab…, 85479d56… y d5d7babb…. Además corrí la suite completa sobre una copia de esa unión: 112/112, `Skipped: 0`.
- **Entorno:** `scripts/dotnet-docker.sh` con sdk:10.0.404 (`RepoDigests` sha256:8c6edfb2…), x86_64. Sin clúster: no toqué `kind-synkra`.

## Criterios de aceptación, verificados por mí
| # | Criterio | Comando que corrí | Resultado |
|---|---|---|---|
| 1 | base y archivos comunes | los cuatro literales de CA-1 | pasa: `base=0`, `0`, los 3 blobs, 4 líneas (2 `-`, 2 `+`) |
| 2 | compila, sin paquetes nuevos, escrituras acotadas | literal, y además con `--no-incremental` | pasa: `Build succeeded`, 0 Warning(s), 0 Error(s); `0` `.csproj`; el `grep` da solo `ApiSynkra.cs`, `ApiSynkra.Escrituras.cs` y `Program.cs` |
| 3 | `ApiSynkraEscriturasTest` | literal | pasa: 16/16, Skipped 0, `salida=0`. Cotejé las claves de los 4 fixtures con los DTO Java de 0910a09 y coinciden, sin snake_case |
| 4 | `DesplegarTest` | literal | pasa: 17/17, Skipped 0, `salida=0`, los 17 nombres |
| 5 | Catalogo / Detalle / Arranque | literal | pasa: 6, 7 y 2; las tres salidas en 0, Skipped 0 |
| 6 | suite completa y U09-T06 | literal (con el `resto` enmendado) | pasa: 77/77, `saltos=1`, `grep-plan` solo `CoreCompatible`/`Compatible`, `resto=1`, `versiones=1` |
| 7 | rojo primero y cada regla muerde | mis mutaciones, en copias hechas con `git archive` + `git init` | pasa: las 16 y la variante `arranque-sin-validar` caen en su prueba. El rojo de 1bd21da da Failed 16/17/6/7 y Arranque pasa 2/2 |
| 8 | diff acotado | literal | pasa: `fuera=1`, `0`, `0`; el commit 43fb96c trae solo `ca8.sh`, `ca8.txt` y la línea de la bitácora |

Sobre los tiempos (invariante 7): `ConUrlMalaNoArranca` dura 93 ms. Lo comprobé lanzando yo el `.dll` con `ftp://h/`: sale el mensaje exacto con rc=134 en 116 ms. El rojo de 1bd21da, que no tiene `ulimit`, dejó un `core.88` en mi copia, lo que confirma el relato del codificador. La evidencia del codificador se generó después de ee83866 (07:21:59Z frente a 07:21:54Z), así que pudo salir de ese commit.

## Los puntos que pediste revisar
- **Confirmar solo tras la acción explícita:** se cumple. Lo cubren `SinClicNoHayConfirmacion` y `LaVueltaSoloSeConfirmaDespuesDeConfirmar`. Una mutación mía que prepara el plan solo al recibir un `APROBADA` cae en dos pruebas.
- **La verificación en vivo no escribe:** solo hace `POST api/verificaciones`.
- **Rechazos con código y mensaje exactos de la API:** las pruebas comparan el texto completo `CODIGO: mensaje` y los detalles.
- **Archivos de U09-B:** no se tocan.
- **Handler ampliado:** lo que ya hacía sigue igual. Pasan las pruebas de U09-A y las 112 de la unión.
- **Tope de 25 MB:** está como constante comentada y anotado en la bitácora.
- **`motivo-oculto` con `Count > 100`:** es válida. Mi variante que borra el bloque de detalles también cae.
- **`revertir-al-abrir`:** muerde, también en mi variante que captura el 404.

## Hallazgos
### F-01 · NARANJA · `Desplegar.razor:441`, `Reversion.razor:195` · El usuario no llega a la confirmación y ninguna prueba lo nota
La tarea pide `ConfirmarAsync(plan.Id, usuario)` en las dos páginas, y la API guarda `confirmadoPor` a partir de esa cabecera. Probé `s#Api.ConfirmarAsync(_plan.Id, _usuario)#Api.ConfirmarAsync(_plan.Id, null)#` en cada página por separado: las dos dan `Passed! 77/77, salida=0`. Ni `ConfirmarLlamaALaConfirmacionDelPlanYEnlazaAlDetalle` ni `LaVueltaSoloSeConfirmaDespuesDeConfirmar` escriben un usuario ni leen `Usuarios[i]` de la petición `/confirmacion`. El código actual es correcto, pero nada lo sostiene. Hay que afirmarlo en las dos pruebas.

### F-02 · NARANJA · `Desplegar.razor:288`, `:366` y el filtro sin `=` · Reglas de la decisión 7 sin prueba
Tres mutaciones sobreviven con 77/77:
- **`x-pais-no-descarta`:** `CambiaPais` deja de poner `_plan = null`. Con eso, si cambias de país después de preparar, «Confirmar» sigue confirmando el plan del país anterior. La regla («plan de la selección actual (país, …)») no tiene prueba: `CambiarLaSeleccionDescartaElPlan` solo desmarca un plugin.
- **`x-filtro-no-desmarca`:** `RemoveWhere` deja de quitar nada. Un filtro que oculta un plugin marcado ya no desmarca ni vuelve a verificar, así que el veredicto en pantalla corresponde a otra selección que la que enviaría «Preparar».
- **`x-filtro-sin-igual-ignora`:** un filtro sin `=` llama a `PluginsAsync`, cuando la regla es mandar la clave con valor vacío.

### F-03 · NARANJA · `ReversionDetalleTest.cs:119` · `CancelarNoLlamaANada` no puede fallar
La prueba ya no hace clic, así que dice lo mismo que `AbrirLaPaginaSoloLee` más el `href`. Puse `@onclick="PrepararAsync"` en el enlace «Cancelar», que es una escritura: `Passed! 77/77`. Arreglo determinista, sin clic real: `Assert.Throws<MissingEventHandlerException>(() => cancelar.Click())`, que demuestra que el enlace no tiene manejador.

### F-04 · AMARILLO (para el orquestador) · decisión 12 y CA-7 «arranque» · La prueba de arranque no distingue la URL configurada de una URL fija inalcanzable
Probé `c.BaseAddress = new Uri("http://127.0.0.1:5999/")`: `Passed! 77/77`. Cualquier URL fija en la que no escuche nadie da el mismo `API_NO_DISPONIBLE` que la de la prueba. La mutación de `ca7.sh` muerde solo porque apunta a la propia aplicación; está escogida para caer, aunque la bitácora lo declara abiertamente. Es un límite del diseño de la decisión 12 («no escucha nadie»), no un defecto del codificador. Por eso queda abajo como tarea candidata y no se le pide arreglo aquí.

### F-05 · AMARILLO · `DesplegarTest.cs:384` · Espera fija de 300 ms en `RespuestaViejaNoPisaLaNueva`
Es una prueba negativa y está declarada en la bitácora. Si falla, falla hacia un falso verde, y `vieja-pisa` la hace caer. Aun así es un `sleep` fijo: con la máquina cargada la guarda se debilita.

### F-06 · AMARILLO · `Fixtures/desplegar-error-seleccion.json`, `desplegar-error-usuario.json` · Textos de detalle inventados
Los detalles no son los textos que manda la API:
- `"El core 'otro' no existe"`: la API usa `SELECCION_CORE`, y un core inexistente sería `CORE_NO_REGISTRADO`.
- `"El usuario debe estar en minúsculas, sin espacios"`: la API usa `USUARIO_FORMA` («de 1 a 64 caracteres…»).

La forma es correcta. Lo mismo se repite en las aserciones de `ApiSynkraEscriturasTest`.

### F-07 · AMARILLO · robustez sin prueba
- La guarda `clave == Clave()` de `PrepararAsync` (`:415`) no tiene prueba (`x-preparar-sin-clave` sobrevive).
- `_ = VerificarAsync(...)` (`:380`) se lanza sin esperar el resultado: una excepción que no sea `ErrorApiSynkraException` se pierde sin rastro.
- `OpenReadStream(TopeTransporte)` (`SubirPlugin.razor:82`): un archivo de más de 25 MB lanza una `IOException` que nadie captura, y el circuito de Blazor se cae.

### F-08 · AMARILLO · duplicación
El bloque de error está copiado 4 veces en `Desplegar.razor`, 2 en `Reversion.razor` y 1 en `SubirPlugin.razor`. La sección `#plan` está copiada entre `Desplegar` y `Reversion`.

## Tareas candidatas (defectos reales fuera de alcance)
- U09-D o posterior: probar el arranque contra una API falsa que sí escuche (que devuelva un JSON reconocible), para comprobar que se usa de verdad la URL de la configuración (F-04).
- `front/.gitignore` no ignora `core.*` (lo dice también la bitácora).

## Limpieza
- No queda ningún proceso `Synkra.Front.dll`.
- Los únicos contenedores vivos son los tres de kind, que no son míos.
- No hay archivos `core.*` en el worktree. Las copias de mutación se borraron.
- `git status --short --ignored` sale igual que al empezar.

## Rutas de transcripciones largas
Todas en `/tmp/claude-1000/-home-casdav-Projects-Master-SynKra/9d987332-269f-4da9-9d66-0a5449231723/scratchpad/U09-C-r1/`:
- `ca1-ca2.txt`, `ca3.txt`, `ca4-ca5.txt`, `ca6.txt`, `ca8.txt`
- `ca7-revisor.txt`: las 17 mutaciones de CA-7 y los sondeos.
- `sondeo-cancel.txt`: el enlace «Cancelar» con manejador y el filtro sin `=`.
- `rojo-revisor.txt`
- `union.log`
- `bin/mut.sh`: el script de mutaciones.
- `copias/*.log`: un log por mutación.

```
VEREDICTO: NO-VERDE
NARANJA|Desplegar.razor:441, Reversion.razor:195|El usuario no llega a la confirmación y ninguna prueba lo nota
NARANJA|Desplegar.razor:288, :366, filtro sin '='|Reglas de la decisión 7 sin prueba (país descarta el plan, el filtro desmarca, sin '=')
NARANJA|ReversionDetalleTest.cs:119|CancelarNoLlamaANada no puede fallar (un @onclick con escritura sobrevive)
AMARILLO|decisión 12 / CA-7 arranque|La prueba de arranque no distingue la URL configurada de una URL fija inalcanzable
AMARILLO|DesplegarTest.cs:384|Espera fija de 300 ms en una prueba negativa
AMARILLO|Fixtures desplegar-error-seleccion/usuario|Textos de detalle que no son los de la API
AMARILLO|Desplegar.razor:380,415; SubirPlugin.razor:82|Guarda de carrera sin prueba, tarea lanzada sin esperar, IOException por encima del tope sin capturar
AMARILLO|páginas razor|Bloque de error y sección del plan duplicados
INFORME: revisiones/U09-C/ronda-1.md
```
