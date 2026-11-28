# U09-A: proyecto Blazor Server, cliente `ApiSynkra` y pantalla «Vista general»

**Unidad:** U09: `Front`
**Tareas del plan que agrupa:** U09-T01, U09-T02 y U09-T06
**Historias que implementa:** H-25 (las tres tareas, como en el plan)
**Depende de:** U04, U06 y U07 (terminadas), solo por el contrato de la API REST. Sale de `main` `8306ee7` (fusión de #108, U07-G). **No** depende de U08: no toca `deploy/`, ni el `Makefile`, ni el clúster, así que corre en paralelo con U08-A y U08-B. Es la primera tarea de U09: después vienen U09-B, U09-C y U09-D (decisión 15).

---

## Alcance

**Dentro** (una línea, concreta):

> El proyecto Blazor Server `front/` (`Synkra.Front`, .NET 10) y su proyecto de pruebas `front.Tests/` (`Synkra.Front.Tests`, xUnit y bUnit); el cliente tipado `ApiSynkra` sobre `HttpClient`, con la URL del backend por configuración (`Synkra:ApiUrl`), las tres lecturas del registro (`GET /api/paises`, `GET /api/paises/{pais}/despliegues`, `GET /api/despliegues/{id}`) con los nombres de campo reales del backend y los errores de la API convertidos en `ErrorApiSynkraException` con su `codigo`, `mensaje` y `detalles` (U09-T01, probado contra un `HttpMessageHandler` simulado); la pantalla «Vista general» en `/`, un renglón por país con lo que devuelve `GET /api/paises` (U09-T02, bUnit, dos países simulados); el `grep` de U09-T06 limpio; y `scripts/dotnet-docker.sh`, que corre `dotnet` en la imagen oficial del SDK porque esta máquina no tiene SDK.

**Fuera** (lo que el codificador debe rechazar aunque lo vea roto):

- Las pantallas «Catálogo» (U09-T03), «Desplegar» (U09-T04) y «Detalle» (U09-T05), y en `ApiSynkra` cualquier método de las rutas de catálogo (`/api/cores`, `/api/cores/{nombre}`, `/api/cores/{nombre}/plugins`, `/api/cores/{nombre}/plugins/{plugin}/{version}`, `/api/artefactos/{sha256}`) o de despliegue (`POST /api/verificaciones`, `POST /api/despliegues`, `POST /api/despliegues/{id}/confirmacion`, `POST /api/despliegues/{id}/reversion`). Los añaden U09-B y U09-C. Ningún `POST`, `PUT` ni `DELETE` en esta tarea, y ninguna cabecera `X-Synkra-Usuario`.
- La imagen del front y su `Dockerfile` (U09-T07), el manifiesto en `deploy/base/front/` (U09-T08) y el resumen `aidlc-docs/construction/U09-front/code/summary.md` con el recorrido de la demo (U09-T09).
- **Nada** en `backend/`, `deploy/`, `Makefile`, `verificador/`, `core/`, `plugin-api/`, `plugins/`, `init-descarga/`, `settings.gradle.kts`, `build.gradle.kts`, el `.gitignore` de la raíz, `.gitattributes`, ni en otro archivo de `scripts/` que no sea `scripts/dotnet-docker.sh`. Si el contrato de la API parece mal (un nombre de campo, un código), se para y se reporta: el front se adapta a la API, nunca al revés.
- Ninguna regla de versión ni de compatibilidad en el front (P5, D3): ni comparar versiones, ni filtrar plugins por `core=`, ni calcular la concordancia. Todo lo que se muestra sale tal cual de la API.
- Instalar el SDK de .NET en la máquina, o correr `dotnet` fuera de `scripts/dotnet-docker.sh` (decisión 3).
- Versionar `bin/`, `obj/`, `TestResults/`, `wwwroot/lib/` (Bootstrap de la plantilla) o las páginas de ejemplo de la plantilla (`Counter`, `Weather`).
- Nada de `kubectl`, de clúster ni de backend arrancado (AUTONOMIA-01): todo se prueba con el `HttpMessageHandler` simulado.
- Tocar `CANDIDATAS.md`, el plan de U09, `unidades-y-tareas.md`, `aidlc-docs/aidlc-state.md` o `aidlc-docs/audit.md`: lo hace el orquestador.

---

## Archivos de contexto

- `aidlc-docs/construction/plans/U09-front-code-generation-plan.md` (U09-T01, T02 y T06; T03 a T09 solo para saber qué queda fuera) y `unidades-y-tareas.md` (sección U09).
- `Modulo 3/specs/prd.md` §9.4 (las cuatro pantallas; la «Vista general»: «cada país con su core, sus plugins, versiones y estado, leídos del clúster»), y M6 en la tabla de módulos (P5).
- `aidlc-docs/inception/user-stories/stories.md` (H-25) y `aidlc-docs/inception/application-design/components.md` (C12) y `application-design.md` (D9: Blazor Server, sin CORS).
- El contrato de la API en `main` `8306ee7` (léase con `git show 8306ee7:<ruta>`):
  - `backend/src/main/java/io/synkra/registro/RutasVistaGeneral.java` (`VistaPaisDto`) y `registro/api/VistaPais.java`, `PluginCorriendo.java`, `LecturaCluster.java`;
  - `backend/src/main/java/io/synkra/registro/RutasRegistro.java` (`ResumenDespliegueDto`, `DespliegueDto`, `ContextoDto`), `registro/api/PluginDesplegado.java`, `EstadoDespliegue.java`, `TipoDespliegue.java`, `registro/CodigosRegistro.java` y `MotivosRegistro.java`;
  - `backend/src/main/java/io/synkra/plataforma/ErrorApi.java`, `DetalleError.java`, `CodigosError.java` y `ManejadorErrores.java`;
  - `backend/src/main/java/io/synkra/registro/VistaGeneralServicio.java` (cuándo `imagen` es nula, `plugins` vacía y `concuerda` nula).
- `aidlc-docs/construction/U06-registro/code/summary.md` («Qué expone», desviación 4: `confirmadoEn` en camelCase) y `aidlc-docs/construction/U07-despliegue/code/summary.md` (rutas de despliegue, para el «Fuera»).
- Plantillas de estilo de tarea: `tareas/U06-A-registro-base.md` y `tareas/U07-G-fronteras-resumen.md`.

---

## Decisiones del orquestador

1. **Layout y nombres.**
   - `front/Synkra.Front.csproj` (`Microsoft.NET.Sdk.Web`), namespace raíz `Synkra.Front`. Sale de la plantilla `dotnet new blazor --interactivity Server --empty` (o de la normal, quitando `Counter`, `Weather` y `wwwroot/lib/`). Interactividad Server global: `<Routes @rendermode="InteractiveServer" />` y `<HeadOutlet @rendermode="InteractiveServer" />` en `Components/App.razor`.
   - Cliente en `front/Api/`, namespace `Synkra.Front.Api`: `ApiSynkra.cs`, `ConfiguracionApi.cs`, `ErrorApiSynkraException.cs`, `CodigosFront.cs` y `Modelos.cs` (los records y enums de la decisión 5).
   - Pantalla en `front/Components/Pages/VistaGeneral.razor` (`@page "/"`), y el menú en `front/Components/Layout/` con un solo enlace, «Vista general».
   - `front.Tests/Synkra.Front.Tests.csproj` (`Microsoft.NET.Sdk.Razor`, `<IsPackable>false</IsPackable>`), con `<ProjectReference Include="../front/Synkra.Front.csproj" />`, namespace `Synkra.Front.Tests`: `ApiSynkraTest.cs`, `VistaGeneralTest.cs`, `ManejadorHttpSimulado.cs` y la carpeta `Fixtures/` con los JSON (copiados al directorio de salida con `<None Include="Fixtures/**" CopyToOutputDirectory="PreserveNewest" />`).
   - `front/.gitignore` (`bin/`, `obj/`) y `front.Tests/.gitignore` (`bin/`, `obj/`, `TestResults/`). Sin `.sln` ni `global.json`: los comandos apuntan al directorio del proyecto (`front.Tests`), como el plan.
   - Los dos `.csproj`: `<TargetFramework>net10.0</TargetFramework>`, `<Nullable>enable</Nullable>`, `<ImplicitUsings>enable</ImplicitUsings>`, `<TreatWarningsAsErrors>true</TreatWarningsAsErrors>`.
2. **Versiones fijas** (comprobadas en nuget.org y en `mcr.microsoft.com` al escribir esta tarea; todas con más de dos semanas):
   - SDK: imagen `mcr.microsoft.com/dotnet/sdk:10.0.404` (trae `DOTNET_VERSION=10.0.15` y `ASPNET_VERSION=10.0.15`; manifiesto arm64 `sha256:127301bc788c53f4557c99163460954f6a2f40869b59e58b71baafabcacd1fa9`). La bitácora anota el `RepoDigests` real de `docker image inspect`.
   - `front/Synkra.Front.csproj`: **ningún** `PackageReference` (solo el framework compartido de ASP.NET Core).
   - `front.Tests/Synkra.Front.Tests.csproj`, exactamente: `bunit` `2.15.2`, `xunit` `2.9.3`, `xunit.runner.visualstudio` `3.1.5`, `Microsoft.NET.Test.Sdk` `18.12.0`. Nada más (ni WireMock.Net, ni Moq, ni FluentAssertions).
   - **Desviación del plan, que se anota en la bitácora:** el plan dice «.NET 8». .NET 8 salió de soporte el 2026-11-10; se usa .NET 10 (LTS). bUnit 2.15.2 trae grupo `net10.0`.
3. **Entorno: no hay SDK de .NET en esta máquina (aarch64).** Todo `dotnet` se corre con `scripts/dotnet-docker.sh`, que queda **exactamente** así (con su bit de ejecución, `git update-index --chmod=+x` si hace falta):
   ```bash
   #!/usr/bin/env bash
   # U09-A: corre «dotnet <args>» en la imagen oficial del SDK, sobre el directorio actual (la raíz del repo o una copia).
   # Esta máquina no tiene SDK de .NET. Uso: scripts/dotnet-docker.sh test front.Tests --filter ApiSynkraTest
   set -u
   IMAGEN='mcr.microsoft.com/dotnet/sdk:10.0.404'
   docker run --rm \
     -v "$PWD":/src -w /src \
     -v synkra-nuget:/root/.nuget/packages \
     -e DOTNET_CLI_TELEMETRY_OPTOUT=1 -e DOTNET_NOLOGO=1 -e DOTNET_CLI_UI_LANGUAGE=en \
     -e DUENO="$(id -u):$(id -g)" \
     "$IMAGEN" bash -c 'dotnet "$@"; s=$?; for d in front front.Tests; do [ -d "/src/$d" ] && chown -R "$DUENO" "/src/$d"; done; exit $s' dotnet "$@"
   ```
   - Se llama desde la raíz del repositorio (o de la copia de `mktemp -d`), porque monta `$PWD`. El volumen `synkra-nuget` guarda la caché de NuGet entre corridas; el `chown` devuelve `bin/`, `obj/` y `TestResults/` al usuario, para que `rm -rf` de las copias funcione.
   - La salida de `dotnet test` sale en inglés (`DOTNET_CLI_UI_LANGUAGE=en`): el equivalente de «Omitido: 0» es `Skipped: 0` en la línea `Passed!  - Failed: ...` de VSTest. Ojo: con un `--filter` que no casa con nada, VSTest imprime `No test matches the given testcase filter` y sale con 0; por eso cada CA exige un mínimo de pruebas en el `.trx`.
4. **Conteo de las pruebas por `.trx`.** Cada corrida de los CA lleva `--logger "trx;LogFileName=<nombre>.trx" --results-directory front.Tests/TestResults`. El guion `bitacoras/U09-A-evidencia/trx.py` (se versiona) queda así y se corre con `python3 -I`:
   ```python
   import sys, xml.etree.ElementTree as E
   n = "{http://microsoft.com/schemas/VisualStudio/TeamTest/2010}"
   r = E.parse(sys.argv[1]).getroot()
   c = r.find(f".//{n}Counters").attrib
   print(" ".join(f"{k}={c[k]}" for k in ("total", "executed", "passed", "failed", "notExecuted")))
   for t in sorted(r.iter(f"{n}UnitTestResult"), key=lambda t: t.get("testName")):
       print(("FALLA " if t.get("outcome") != "Passed" else "") + t.get("testName").rsplit(".", 1)[-1])
   ```
   Solo `[Fact]`, sin `[Theory]` ni `Skip`: así cada nombre de prueba es un nombre de método.
5. **El contrato: nombres de campo reales del backend** (de `8306ee7`; Spring Boot serializa en camelCase, los `Instant` como texto ISO-8601 y los nulos se incluyen, porque `application.properties` no cambia la inclusión de Jackson). En C#, records posicionales en `Modelos.cs`, cuyos nombres de propiedad son los del JSON en PascalCase:
   - `GET /api/paises` → lista de `VistaPaisDto`: `pais`, `lectura`, `imagen`, `plugins`, `concuerda`, `ultimoDespliegueId`, `core`, `versionCore`; cada plugin (`PluginCorriendo`): `nombre`, `version`, `sha256`. `lectura` es `vacio`, `leido`, `inconsistente` o `no_disponible` (`name().toLowerCase()` de `LecturaCluster`). Con `lectura` distinta de `leido`, `imagen` es `null` y `plugins` es `[]`; `concuerda` es `null` solo con `no_disponible`; `ultimoDespliegueId`, `core` y `versionCore` son del último `listo` del registro, o `null`.
     C#: `VistaPais(string Pais, LecturaCluster Lectura, string? Imagen, IReadOnlyList<PluginCorriendo> Plugins, bool? Concuerda, long? UltimoDespliegueId, string? Core, string? VersionCore)`, `PluginCorriendo(string Nombre, string Version, string Sha256)`, `enum LecturaCluster { Vacio, Leido, Inconsistente, NoDisponible }`.
   - `GET /api/paises/{pais}/despliegues` → lista de `ResumenDespliegueDto`, del más reciente al más antiguo (R-R5): `id`, `pais`, `tipo`, `origenId`, `core`, `versionCore`, `imagen`, `plugins`, `creadoPor`, `confirmadoPor`, `confirmadoEn`, `estado`, `terminadoEn`; cada plugin (`PluginDesplegado`): `nombre`, `version`, `sha256`, `etiquetas` (objeto texto→texto). `tipo` es `despliegue` o `reversion`; `estado` es `aplicando`, `listo` o `fallido`; `origenId` es `null` salvo en una reversión; `terminadoEn` es `null` mientras está `aplicando`.
     C#: `ResumenDespliegue(long Id, string Pais, TipoDespliegue Tipo, long? OrigenId, string Core, string VersionCore, string Imagen, IReadOnlyList<PluginDesplegado> Plugins, string CreadoPor, string ConfirmadoPor, DateTimeOffset ConfirmadoEn, EstadoDespliegue Estado, DateTimeOffset? TerminadoEn)`, `PluginDesplegado(string Nombre, string Version, string Sha256, IReadOnlyDictionary<string, string> Etiquetas)`, `enum TipoDespliegue { Despliegue, Reversion }`, `enum EstadoDespliegue { Aplicando, Listo, Fallido }`.
   - `GET /api/despliegues/{id}` → `DespliegueDto`: los trece campos de arriba, más `veredicto` (el JSON guardado, como objeto) y `contexto` (`null` salvo en un `fallido`), con `logCore`, `logInit` y `eventos`.
     C#: `Despliegue(` los trece de `ResumenDespliegue` `, JsonElement Veredicto, ContextoFallo? Contexto)` y `ContextoFallo(string LogCore, string LogInit, string Eventos)`. El veredicto se guarda como `JsonElement` sin interpretarlo (decisión 9).
   - Error (todas las rutas, `ManejadorErrores`): `ErrorApi` con `codigo`, `mensaje`, `detalles`; cada detalle (`DetalleError`): `campo`, `motivo`. C#: `ErrorApi(string Codigo, string Mensaje, IReadOnlyList<DetalleError> Detalles)` y `DetalleError(string Campo, string Motivo)`. Ejemplos reales: 404 `NO_ENCONTRADO` / `Recurso no encontrado` (`GET /api/despliegues/{id}` sin ese id); 422 `PAIS_INVALIDO` / `El país no es válido` con el detalle `pais` / `El país debe ser un código de dos letras en minúscula (S4)`; 500 `ERROR_INTERNO` / `Error interno del servidor`.
6. **Deserialización estricta** (el contrato muerde si un nombre está mal). Una sola instancia estática de `JsonSerializerOptions` en `ApiSynkra`:
   `new JsonSerializerOptions(JsonSerializerDefaults.Web) { UnmappedMemberHandling = JsonUnmappedMemberHandling.Disallow, RespectNullableAnnotations = true, RespectRequiredConstructorParameters = true, Converters = { new JsonStringEnumConverter(JsonNamingPolicy.SnakeCaseLower, allowIntegerValues: false) } }`.
   Con esto, un campo del JSON que el record no tiene, un campo que falta o un `null` en una propiedad no anulable lanzan `JsonException`, que `ApiSynkra` convierte en `RESPUESTA_INVALIDA` (decisión 7). Sin `[JsonPropertyName]`: los nombres salen de la política camelCase de `JsonSerializerDefaults.Web`. Si con el backend de U09-D aparece un campo nuevo, se añade al record; no se afloja la opción.
7. **`ApiSynkra`** (clase pública, no `sealed` obligatorio, sin interfaz):
   - Constructor `public ApiSynkra(HttpClient http)`; usa la `BaseAddress` del `HttpClient` y rutas **relativas sin barra inicial** (`api/paises`), para que un prefijo en la URL base se respete.
   - Métodos: `Task<IReadOnlyList<VistaPais>> VistaGeneralAsync(CancellationToken ct = default)` (`GET api/paises`), `Task<IReadOnlyList<ResumenDespliegue>> HistorialAsync(string pais, CancellationToken ct = default)` (`GET api/paises/{Uri.EscapeDataString(pais)}/despliegues`, sin validar el país: lo valida la API) y `Task<Despliegue> DespliegueAsync(long id, CancellationToken ct = default)` (`GET api/despliegues/{id}`). Solo `GET`, cabecera `Accept: application/json`.
   - Respuesta 2xx: se deserializa con las opciones de la decisión 6.
   - Respuesta no 2xx: si el cuerpo es un `ErrorApi` válido (con las mismas opciones), lanza `ErrorApiSynkraException` con `Estado` = el código HTTP y `Codigo`, `Mensaje` y `Detalles` del cuerpo, tal cual. Si el cuerpo no lo es (vacío, HTML de un proxy, otro JSON): `Codigo = CodigosFront.RespuestaInvalida`, `Mensaje = $"La API respondió {estado} sin un cuerpo de error válido"`, `Detalles` vacía.
   - 2xx cuyo cuerpo no casa con el record (`JsonException`): `Codigo = CodigosFront.RespuestaInvalida`, `Estado` = el código HTTP, `Mensaje = "La respuesta de la API no tiene la forma esperada"`.
   - `HttpRequestException`, o `TaskCanceledException` sin que `ct` se haya pedido (tiempo agotado): `Codigo = CodigosFront.ApiNoDisponible`, `Estado = null`, `Mensaje = "No se pudo conectar con la API de SynKra"`. Si `ct` se pidió, la cancelación se propaga sin envolver.
   - `ErrorApiSynkraException : Exception`, con `int? Estado`, `string Codigo`, `IReadOnlyList<DetalleError> Detalles`, y `Message` = el `mensaje`. `CodigosFront` (clase estática): `RespuestaInvalida = "RESPUESTA_INVALIDA"` y `ApiNoDisponible = "API_NO_DISPONIBLE"`. Son los únicos códigos que inventa el front, y no colisionan con los del backend (`CodigosError`, `CodigosRegistro`).
8. **URL del backend por configuración.** Clave `Synkra:ApiUrl` (variable de entorno `Synkra__ApiUrl`); en `front/appsettings.json`, `"Synkra": { "ApiUrl": "http://localhost:8080/" }`. `ConfiguracionApi.UrlBase(IConfiguration)` (estática, pública) devuelve un `Uri` absoluto `http` o `https` que siempre termina en `/` (la añade si falta); si la clave falta, está vacía, es relativa o tiene otro esquema, lanza `InvalidOperationException` con el mensaje `"Synkra:ApiUrl falta o no es una URL absoluta http o https"`. En `Program.cs` se calcula **antes** de `builder.Build()` (así una URL mala impide arrancar) y se registra `builder.Services.AddHttpClient<ApiSynkra>(c => c.BaseAddress = urlApi)`. El valor real del clúster (el Service del backend) lo fija U09-C en el manifiesto.
9. **El servidor HTTP simulado es un `HttpMessageHandler` falso, no WireMock.Net.** `ManejadorHttpSimulado : HttpMessageHandler` (en `front.Tests`), con:
   - respuestas por `(método, ruta con query)` exacta: `Responde(HttpMethod metodo, string ruta, HttpStatusCode estado, string cuerpo, string tipo = "application/json")`; `Lanza(HttpMethod, string ruta, Exception)` para los fallos de transporte;
   - una ruta no registrada responde **404 con el cuerpo `NO_ENCONTRADO` real** (`{"codigo":"NO_ENCONTRADO","mensaje":"Recurso no encontrado","detalles":[]}`), como el backend;
   - `Peticiones`: la lista de `(método, ruta)` recibidas, en orden, para afirmar qué se llamó.
   Motivo: sin puertos ni paquetes nuevos, determinista dentro del contenedor, y pasa por la misma tubería de `HttpClient` que usa el front. Los cuerpos son JSON **escritos a mano con la forma exacta de los DTO del backend** (decisión 5) en `front.Tests/Fixtures/`; ninguna prueba serializa un record de C# para fabricar la respuesta (eso escondería un nombre mal puesto).
10. **Fixtures** (nombres fijos; los sha256 son 64 veces la misma letra, `a`, `b` o `c`):
    - `paises-dos.json`, los dos países simulados de U09-T02:
      `co`: `"lectura":"leido"`, `"imagen":"localhost:5001/synkra-core@sha256:<64 a>"`, `"plugins":[{"nombre":"kyc-co","version":"1.0.0","sha256":"<64 b>"}]`, `"concuerda":true`, `"ultimoDespliegueId":7`, `"core":"synkra-core"`, `"versionCore":"2.1.0"`;
      `pe`: `"lectura":"no_disponible"`, `"imagen":null`, `"plugins":[]`, `"concuerda":null`, `"ultimoDespliegueId":3`, `"core":"synkra-core"`, `"versionCore":"2.0.0"`.
    - `paises-sin-despliegue.json`: un solo país, `mx`, `"lectura":"vacio"`, `"imagen":null`, `"plugins":[]`, `"concuerda":true`, `"ultimoDespliegueId":null`, `"core":null`, `"versionCore":null`.
    - `paises-campo-desconocido.json`: `paises-dos.json` con un campo más, `"otro":1`, en `co`.
    - `historial-pe.json`: dos resúmenes de `pe`, el `id` 9 (`"tipo":"reversion"`, `"origenId":5`, `"estado":"aplicando"`, `"terminadoEn":null`) y el `id` 5 (`"tipo":"despliegue"`, `"origenId":null`, `"estado":"listo"`, `"terminadoEn":"2026-11-27T10:05:00Z"`), con `"confirmadoEn":"2026-11-27T11:00:00.123Z"` y `"2026-11-27T10:00:00Z"`, y en cada uno el plugin `kyc-pe` `1.0.0` con sha256 `<64 c>` y `"etiquetas":{"core":"2.x","pais":"pe","version":"1.0.0"}`.
    - `despliegue-fallido.json` (`id` 5, `"estado":"fallido"`, `"veredicto":{"valido":false,"motivos":[]}`, `"contexto":{"logCore":"línea 1\n","logInit":"","eventos":"BackOff"}`) y `despliegue-listo.json` (`id` 7, `"estado":"listo"`, `"contexto":null`).
    - `error-422-pais.json` (el 422 `PAIS_INVALIDO` de la decisión 5, con su detalle) y `error-500.json` (`ERROR_INTERNO`, `"detalles":[]`).
    El contenido del `veredicto` es opaco para el front: la prueba solo afirma que llega como objeto con la propiedad `valido`, y su forma la fija U09-C con `VeredictoDto`.
11. **`ApiSynkraTest`** (en `front.Tests`, `[Fact]`), con estos nombres exactos:
    - `VistaGeneralLeeLosDosPaisesConTodosSusCampos`: `paises-dos.json`; afirma cada campo de los dos países (los ocho de `co` y los de `pe`, nulos incluidos), los dos valores de enum, y que se pidió solo `GET /api/paises`.
    - `VistaGeneralConListaVaciaDevuelveListaVacia`.
    - `HistorialPideLaRutaDelPaisYLeeTipoOrigenYFechas`: `GET /api/paises/pe/despliegues`, el orden de la respuesta se conserva (9 y luego 5), `Tipo`, `OrigenId`, `Estado`, `ConfirmadoEn` (con sus milisegundos), `TerminadoEn` nulo y no nulo, y las tres etiquetas.
    - `HistorialEscapaElPais`: `HistorialAsync("p e")` pide `/api/paises/p%20e/despliegues`.
    - `DespliegueFallidoTraeVeredictoYContexto` y `DespliegueListoNoTraeContexto` (`GET /api/despliegues/5` y `/7`).
    - `Error404DaCodigoYMensajeDeLaApi`: ruta no registrada; `Estado` 404, `NO_ENCONTRADO`, `Recurso no encontrado`, `Detalles` vacía.
    - `Error422DaLosDetallesConCampoYMotivo`: `error-422-pais.json`, con el detalle `pais` y su motivo literal.
    - `ErrorSinCuerpoDeLaApiDaRespuestaInvalida`: 503 con `<html>…</html>` y `text/html`; `RESPUESTA_INVALIDA`, `Estado` 503 y el mensaje de la decisión 7.
    - `CampoDesconocidoEnLaRespuestaDaRespuestaInvalida`: `paises-campo-desconocido.json` con 200.
    - `FalloDeConexionDaApiNoDisponible` (`HttpRequestException`) y `TiempoAgotadoDaApiNoDisponible` (`TaskCanceledException`, sin cancelar `ct`).
    - `LaUrlBaseConPrefijoSeRespeta`: con `BaseAddress = http://synkra.test/x/`, `VistaGeneralAsync` pide `/x/api/paises`.
    - `ConfiguracionSinUrlNoArranca`, `ConfiguracionConUrlRelativaNoArranca` y `ConfiguracionConUrlValidaTerminaEnBarra` (`http://synkra-api:8080` da `http://synkra-api:8080/`), con `ConfigurationBuilder().AddInMemoryCollection(...)`.
    Son 16. Cada prueba de error afirma `Codigo`, `Estado`, `Message` y `Detalles`.
12. **Pantalla «Vista general»** (`VistaGeneral.razor`, `@page "/"`, `<PageTitle>Vista general · SynKra</PageTitle>`, `<h1>Vista general</h1>`). Inyecta `ApiSynkra` y llama **una sola vez** a `VistaGeneralAsync` en `OnInitializedAsync`; no llama a ninguna otra ruta. Marcado fijo, para que las pruebas (y U09-B a U09-D) busquen por atributo y no por texto suelto:
    - Mientras carga: `<p data-estado="cargando">Cargando…</p>`.
    - Con datos: `<table id="vista-general">` con cabecera `País | Core | Versión | Imagen | Plugins activos | Clúster | Concuerda | Último despliegue`, y un `<tr data-pais="{pais}">` por país, **en el orden de la API**, con una celda `<td data-campo="...">` por columna: `pais`, `core`, `versionCore`, `imagen`, `plugins`, `lectura`, `concuerda`, `ultimoDespliegueId`.
    - Textos: un valor nulo, y `plugins` vacía, se muestran como `—` (U+2014). `plugins`: un `<li>` por plugin con `«nombre» «version»` (por ejemplo `kyc-co 1.0.0`), en el orden de la API. `lectura`: `vacio` → `Vacío`, `leido` → `Leído`, `inconsistente` → `Inconsistente`, `no_disponible` → `No disponible`. `concuerda`: `true` → `Sí`, `false` → `No`, `null` → `No se sabe`. `ultimoDespliegueId`: `#7` (sin enlace todavía: la pantalla «Detalle» es de U09-D, que añade el enlace).
    - Lista vacía: `<p data-estado="vacio">No hay países configurados en la API.</p>`, sin tabla.
    - Error (`ErrorApiSynkraException`): `<div data-estado="error">` con `<p data-campo="error">{Codigo}: {Message}</p>` y, si hay detalles, un `<li>` por detalle con `{campo}: {motivo}`. Sin tabla.
    - Esto es presentación, no regla: «Clúster» y «Concuerda» son `lectura` y `concuerda` de la API, que los calcula (R-R6, R-R7). **Interpretación del plan, que se anota en la bitácora:** U09-T02 dice «estado del último despliegue»; `GET /api/paises` no trae el estado de un despliegue (trae el id del último `listo`, la lectura del clúster y la concordancia, que es lo que el PRD §9.4 pide «leídos del clúster»). La pantalla no hace una llamada por país al historial para sacar otro estado; el estado de cada despliegue lo muestra «Detalle» (U09-D).
13. **`VistaGeneralTest`** (bUnit 2.15.2: la clase hereda de `BunitContext`, registra en `Services` un `ApiSynkra` sobre `new HttpClient(manejador) { BaseAddress = new Uri("http://synkra.test/") }` y usa `Render<VistaGeneral>()` con `WaitForAssertion`; el `ApiSynkra` es el real, con el `ManejadorHttpSimulado`, no un doble), con estos nombres exactos:
    - `MuestraUnRenglonPorPaisConLosDatosDeLaApi`: `paises-dos.json`; dos `tr[data-pais]`, `co` y luego `pe`, y en `co` cada celda de la decisión 12 (`synkra-core`, `2.1.0`, la imagen entera, `kyc-co 1.0.0`, `Leído`, `Sí`, `#7`).
    - `PaisSinLecturaMuestraNoDisponibleYNoSeSabe`: el renglón `pe` (`—` en imagen y plugins, `No disponible`, `No se sabe`, `#3`).
    - `PaisSinDespliegueMuestraGuiones`: `paises-sin-despliegue.json` (`—` en core, versión, imagen, plugins y último despliegue; `Vacío`; `Sí`).
    - `SinPaisesMuestraAviso`: `[]`.
    - `ErrorDeLaApiMuestraCodigoYMensaje`: `error-500.json` con 500; `ERROR_INTERNO: Error interno del servidor`, sin tabla.
    - `ApiNoDisponibleMuestraElCodigoDelFront`: `HttpRequestException`; `API_NO_DISPONIBLE: No se pudo conectar con la API de SynKra`.
    - `SoloLlamaAGetApiPaises`: tras renderizar con `paises-dos.json`, `Peticiones` es exactamente `[(GET, /api/paises)]`.
    Son 7.
14. **U09-T06, sin reglas de versión.** El `grep` del plan, literal, sobre el árbol después de compilar: `grep -rniE "semver|compatib" front/ --include='*.cs' --include='*.razor'`. En esta tarea el front no tiene llamada a la verificación (es de U09-C), así que «solo encuentra llamadas a `ApiSynkra`» se cumple con **ninguna** línea. Cuidado con comentarios y textos en español («compatible», «compatibilidad»): no se escriben en `front/`. Además, ninguna lectura de versiones: ni `System.Version`, ni `Version.Parse`, ni `NuGet.Versioning`, ni nada con `Semver` en `front/` (`.cs`, `.razor`, `.csproj`).
15. **Cómo sigue U09** (para que esta tarea no lo adelante): U09-B, «Catálogo» (T03) y sus métodos en `ApiSynkra`; U09-C, «Desplegar» (T04) con la verificación en vivo, la imagen (T07) y el manifiesto (T08); U09-D, «Detalle» (T05) y el resumen con el recorrido (T09). El reparto exacto lo fija cada tarea.
16. **Rojo primero.** Un primer commit trae `front/`, `front.Tests/` (las 23 pruebas y los fixtures), `scripts/dotnet-docker.sh` y `trx.py`, con los tres métodos de `ApiSynkra` y `ConfiguracionApi.UrlBase` lanzando `NotImplementedException` y `VistaGeneral.razor` con solo el `@page` y el `<h1>`. Con ese commit, CA-2 y CA-3 dan `Failed!` (la bitácora pega el sha, su `git show --stat` y la salida de `trx.py`, con las pruebas que caen). Después, la implementación. CA-6 muestra con el código terminado que cada regla muerde.
17. **Bitácora en `bitacoras/U09-A.md`, evidencia en `bitacoras/U09-A-evidencia/`** (un `caN.txt` por criterio, con fecha, sha, el `RepoDigests` de la imagen, comando y salida completa; los `.trx` no se versionan, solo la salida de `trx.py`; los guiones `caN.sh` y `trx.py` se versionan), y el revisor en `revisiones/U09-A/`. Español, sin emojis. Las desviaciones de la bitácora son, como mínimo: .NET 10 en vez de 8 (decisión 2), el `HttpMessageHandler` en vez de un servidor con puerto (decisión 9) y la interpretación de «estado del último despliegue» (decisión 12).

---

## Criterios de aceptación

Todos se corren desde la raíz del repositorio. En las líneas `Passed!` / `Failed!` de VSTest los espacios entre la etiqueta y el número pueden variar; lo que cuenta es el número.

- [ ] **CA-1**: el proyecto compila sin avisos, con las versiones fijas
  ```bash
  docker pull -q mcr.microsoft.com/dotnet/sdk:10.0.404
  docker image inspect --format '{{index .RepoDigests 0}} {{.Architecture}}' mcr.microsoft.com/dotnet/sdk:10.0.404
  scripts/dotnet-docker.sh --version
  scripts/dotnet-docker.sh build front.Tests
  grep -c '<TargetFramework>net10.0</TargetFramework>' front/Synkra.Front.csproj front.Tests/Synkra.Front.Tests.csproj
  grep -c 'PackageReference' front/Synkra.Front.csproj
  grep -o 'PackageReference Include="[^"]*" Version="[^"]*"' front.Tests/Synkra.Front.Tests.csproj | sort
  grep -c '<TreatWarningsAsErrors>true</TreatWarningsAsErrors>' front/Synkra.Front.csproj front.Tests/Synkra.Front.Tests.csproj
  grep -c "^IMAGEN='mcr.microsoft.com/dotnet/sdk:10.0.404'$" scripts/dotnet-docker.sh; test -x scripts/dotnet-docker.sh && echo ejecutable
  git ls-files front front.Tests | grep -iE '(^|/)(bin|obj|TestResults)/|wwwroot/lib/|Counter|Weather'; echo "sobrantes=$?"
  ```
  Esperado:
  - el `RepoDigests` (se anota) y `amd64` (los criterios se corren en la máquina de David, x86_64; enmienda del 2026-11-28 05:32 UTC); `10.0.404` (es la versión del contenedor: el SDK del sistema de esa máquina es otro, 10.0.115, y no se usa).
  - `Build succeeded.`, `0 Warning(s)` y `0 Error(s)`.
  - `1` y `1` (los dos `net10.0`); `0` (el front no tiene paquetes); exactamente cuatro líneas: `PackageReference Include="Microsoft.NET.Test.Sdk" Version="18.12.0"`, `PackageReference Include="bunit" Version="2.15.2"`, `PackageReference Include="xunit" Version="2.9.3"`, `PackageReference Include="xunit.runner.visualstudio" Version="3.1.5"`; `1` y `1`.
  - `1` y `ejecutable`; `sobrantes=1`.

- [ ] **CA-2**: U09-T01. `ApiSynkra` contra el servidor HTTP simulado
  ```bash
  scripts/dotnet-docker.sh test front.Tests --filter ApiSynkraTest --logger "trx;LogFileName=api.trx" --results-directory front.Tests/TestResults; echo "salida=$?"
  python3 -I bitacoras/U09-A-evidencia/trx.py front.Tests/TestResults/api.trx
  grep -o '"[a-zA-Z0-9]*":' front.Tests/Fixtures/paises-dos.json front.Tests/Fixtures/historial-pe.json front.Tests/Fixtures/despliegue-fallido.json front.Tests/Fixtures/error-422-pais.json | sort -u
  grep -nE 'UnmappedMemberHandling\.Disallow|RespectNullableAnnotations = true|RespectRequiredConstructorParameters = true|SnakeCaseLower' front/Api/ApiSynkra.cs
  grep -rn 'JsonPropertyName' front/; echo "renombres=$?"
  grep -rnE 'Http(Method\.)?(Post|Put|Delete|Patch)|PostAsync|PutAsync|DeleteAsync|PatchAsync|X-Synkra-Usuario' front/ --include='*.cs' --include='*.razor'; echo "escrituras=$?"
  grep -n 'Synkra:ApiUrl\|ApiUrl' front/appsettings.json front/Api/ConfiguracionApi.cs
  grep -n 'ConfiguracionApi.UrlBase\|AddHttpClient' front/Program.cs
  ```
  Esperado:
  - la línea de VSTest `Passed!  - Failed:     0, Passed:    16, Skipped:     0, Total:    16` (o con más pruebas, siempre `Failed: 0` y `Skipped: 0`), sin `No test matches`, y `salida=0`.
  - `trx.py`: `total=16 executed=16 passed=16 failed=0 notExecuted=0` (o más, con `passed` = `total`), ninguna línea `FALLA` y los 16 nombres de la decisión 11.
  - Los nombres de campo de los fixtures son **exactamente** los de la decisión 5 para cada archivo (el revisor los coteja con `git show 8306ee7:backend/src/main/java/io/synkra/registro/RutasRegistro.java`, `RutasVistaGeneral.java`, `registro/api/PluginCorriendo.java`, `registro/api/PluginDesplegado.java`, `plataforma/ErrorApi.java` y `plataforma/DetalleError.java`): `ultimoDespliegueId`, `versionCore`, `origenId`, `creadoPor`, `confirmadoPor`, `confirmadoEn`, `terminadoEn`, `logCore`, `logInit`, `codigo`, `mensaje`, `detalles`, `campo`, `motivo`…, más las claves de `etiquetas` (`core`, `pais`, `version`) y del `veredicto` (`valido`, `motivos`) de la decisión 10; ningún nombre en snake_case.
  - Las cuatro opciones de la decisión 6, `renombres=1`, `escrituras=1`, la clave `Synkra:ApiUrl` en `appsettings.json` y en `ConfiguracionApi.cs`, y `Program.cs` usa `ConfiguracionApi.UrlBase` y `AddHttpClient` (enmienda del orquestador del 2026-11-28, ronda 2: se comprueba el uso, no el texto de la clave; ver `aidlc-docs/audit.md`).

- [ ] **CA-3**: U09-T02. «Vista general» con bUnit y dos países simulados
  ```bash
  scripts/dotnet-docker.sh test front.Tests --filter VistaGeneralTest --logger "trx;LogFileName=vista.trx" --results-directory front.Tests/TestResults; echo "salida=$?"
  python3 -I bitacoras/U09-A-evidencia/trx.py front.Tests/TestResults/vista.trx
  python3 -I -c 'import json,sys; print([(p["pais"], p["lectura"], p["concuerda"]) for p in json.load(open(sys.argv[1]))])' front.Tests/Fixtures/paises-dos.json
  grep -n '@page\|@inject\|VistaGeneralAsync\|HistorialAsync\|DespliegueAsync\|HttpClient' front/Components/Pages/VistaGeneral.razor
  grep -rn 'rendermode="InteractiveServer"' front/Components/App.razor
  ```
  Esperado:
  - `Passed!  - Failed:     0, Passed:     7, Skipped:     0, Total:     7` (o más, con `Failed: 0` y `Skipped: 0`), sin `No test matches`, y `salida=0`.
  - `trx.py`: `total=7 executed=7 passed=7 failed=0 notExecuted=0` (o más), sin `FALLA`, con los 7 nombres de la decisión 13.
  - `[('co', 'leido', True), ('pe', 'no_disponible', None)]`.
  - En la página: `@page "/"`, un `@inject` de `ApiSynkra`, **una** línea con `VistaGeneralAsync` y ninguna con `HistorialAsync`, `DespliegueAsync` ni `HttpClient` (la pantalla solo habla con `ApiSynkra`). Dos líneas de `rendermode` (`Routes` y `HeadOutlet`).

- [ ] **CA-4**: U09-T06. Sin reglas de versión en el front
  ```bash
  scripts/dotnet-docker.sh build front
  grep -rniE "semver|compatib" front/ --include='*.cs' --include='*.razor'; echo "grep-plan=$?"
  grep -rniE "semver|compatib" front/ --exclude-dir=bin --exclude-dir=obj; echo "grep-todo=$?"
  grep -rnE 'System\.Version|Version\.(Try)?Parse|NuGet\.Versioning|Semver' front/ --include='*.cs' --include='*.razor' --include='*.csproj'; echo "versiones=$?"
  ```
  Esperado: `Build succeeded.` y luego `grep-plan=1`, `grep-todo=1` y `versiones=1` (ninguna línea: en esta tarea no hay llamada a la verificación, decisión 14). El primer `grep` es el del plan, literal, y corre **después** de compilar, con `bin/` y `obj/` presentes.

- [ ] **CA-5**: toda la suite pasa, sin saltos
  ```bash
  rm -rf front.Tests/TestResults
  scripts/dotnet-docker.sh test front.Tests --logger "trx;LogFileName=todo.trx" --results-directory front.Tests/TestResults; echo "salida=$?"
  python3 -I bitacoras/U09-A-evidencia/trx.py front.Tests/TestResults/todo.trx
  grep -rnE '\[(Fact|Theory)\([^)]*Skip|\[Theory' front.Tests --include='*.cs'; echo "saltos=$?"
  ```
  Esperado: `Passed!  - Failed:     0, Passed:    23, Skipped:     0, Total:    23` (o más, siempre `Failed: 0` y `Skipped: 0`), `salida=0`; `trx.py` con `passed` = `total` ≥ 23, `failed=0`, `notExecuted=0` y sin `FALLA`; `saltos=1`.

- [ ] **CA-6**: rojo primero y cada regla muerde, demostrado en una copia
  Guardar como `bitacoras/U09-A-evidencia/ca6.sh` (se versiona). Una copia por mutación, sin `git stash` ni cambios en el worktree:
  ```bash
  D=$(mktemp -d); git archive HEAD | tar -x -C "$D"; cd "$D"
  # Mutación anotada literalmente en la bitácora (sed o parche); una por copia:
  #   ruta:          "api/paises" pasa a "api/pais" en VistaGeneralAsync de front/Api/ApiSynkra.cs
  #   nombre:        "versionCore" pasa a "version_core" en front.Tests/Fixtures/paises-dos.json
  #   estricto:      se quita UnmappedMemberHandling = JsonUnmappedMemberHandling.Disallow de ApiSynkra.cs
  #   error:         ApiSynkra deja de leer el cuerpo de error y siempre lanza RESPUESTA_INVALIDA
  #   escape:        se quita Uri.EscapeDataString en HistorialAsync
  #   config:        ConfiguracionApi.UrlBase acepta una URL relativa (UriKind.RelativeOrAbsolute)
  #   concuerda:     en VistaGeneral.razor, null pasa a mostrarse como "No"
  #   otra llamada:  VistaGeneral.razor llama además a HistorialAsync("co") en OnInitializedAsync
  sed -i '<mutación>' "$D/<ruta>"
  scripts/dotnet-docker.sh test front.Tests --logger "trx;LogFileName=m.trx" --results-directory front.Tests/TestResults; echo "salida=$?"
  python3 -I bitacoras/U09-A-evidencia/trx.py front.Tests/TestResults/m.trx | grep '^FALLA'
  cd - >/dev/null; rm -rf "$D"
  ```
  Esperado: con cada una de las ocho mutaciones, `salida` distinto de 0 y `Failed!`, y entre las líneas `FALLA` está la prueba de esa regla:
  - «ruta»: `VistaGeneralLeeLosDosPaisesConTodosSusCampos` (y las de `VistaGeneralTest` que leen países);
  - «nombre»: `VistaGeneralLeeLosDosPaisesConTodosSusCampos` y `MuestraUnRenglonPorPaisConLosDatosDeLaApi`;
  - «estricto»: `CampoDesconocidoEnLaRespuestaDaRespuestaInvalida`;
  - «error»: `Error404DaCodigoYMensajeDeLaApi`, `Error422DaLosDetallesConCampoYMotivo` y `ErrorDeLaApiMuestraCodigoYMensaje`;
  - «escape»: `HistorialEscapaElPais`;
  - «config»: `ConfiguracionConUrlRelativaNoArranca`;
  - «concuerda»: `PaisSinLecturaMuestraNoDisponibleYNoSeSabe`;
  - «otra llamada»: `SoloLlamaAGetApiPaises`.
  Una mutación que no compila no cuenta como mordida: se reescribe para que compile y se anota. Además, la bitácora pega el rojo de la decisión 16: el sha del commit con los stubs, su `git show --stat`, y la salida de CA-2 y CA-3 sobre una copia de ese commit (`Failed!`, con las líneas `FALLA`).

- [ ] **CA-7**: el diff queda acotado
  Guardar como `bitacoras/U09-A-evidencia/ca7.sh`; `ca7.txt` se genera justo antes de su propio commit, con el árbol limpio y sin commits de código después: ese commit solo trae `ca7.sh`, `ca7.txt` y la línea de la bitácora que los cita.
  ```bash
  git rev-parse --short HEAD; git status --porcelain | wc -l
  git diff --stat 8306ee7...HEAD
  git diff --name-only 8306ee7...HEAD | grep -v -E '^(front/|front\.Tests/|scripts/dotnet-docker\.sh$|bitacoras/U09-A\.md$|bitacoras/U09-A-evidencia/|revisiones/U09-A/)'; echo "fuera=$?"
  git diff --name-only 8306ee7...HEAD -- backend deploy Makefile verificador core plugin-api plugins init-descarga build.gradle.kts settings.gradle.kts .gitignore .gitattributes | wc -l
  git diff --name-status --diff-filter=DM 8306ee7...HEAD | wc -l
  ```
  Esperado: `fuera=1` (solo esas rutas), `0` (nada en backend, deploy, Makefile ni el resto) y `0` (solo se añaden archivos: nada que existía en `8306ee7` se borra ni cambia). El sha impreso es el del padre del commit de `ca7.txt`, y el `wc -l` de `git status` cuenta solo `ca7.sh` y `ca7.txt` (se anota). Si hace falta evidencia que no cae en esas rutas, se para y se pregunta.

---

**Rojo primero:** el commit de la decisión 16 trae las 23 pruebas con `ApiSynkra`, `ConfiguracionApi.UrlBase` y la pantalla sin implementar; CA-2 y CA-3 dan `Failed!` sobre una copia de ese commit (la bitácora pega su sha, su `git show --stat` y las líneas `FALLA` de `trx.py`). Después se implementa, y CA-6 demuestra con el código terminado que cada regla muerde. Todo `dotnet` corre en `mcr.microsoft.com/dotnet/sdk:10.0.404` vía `scripts/dotnet-docker.sh`; nada de `kubectl`.
