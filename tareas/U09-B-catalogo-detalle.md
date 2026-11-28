# U09-B: pantallas «Catálogo» y «Detalle de un despliegue» (solo lectura)

**Unidad:** U09: `Front`
**Tareas del plan que agrupa:** U09-T03 (la parte de lectura) y U09-T05 (la parte de lectura)
**Historias que implementa:** H-25
**Depende de:** U09-A (VERDE, fusionada en #110). Sale de `<main con U09-A>`: `0910a09` o un `main` posterior en el que `front/`, `front.Tests/` y `scripts/dotnet-docker.sh` no hayan cambiado (CA-1 lo comprueba). Corre **en paralelo con U09-C**, desde la misma base, con el reparto de archivos de la decisión 1: las dos ramas tocan tres archivos en común y los dejan **idénticos byte por byte**, así que la fusión de las dos no puede tener conflicto. Después viene U09-D (imagen y manifiesto, desde la unión de B y C).

---

## Alcance

**Dentro** (una línea, concreta):

> El cliente `ApiSynkra` pasa a `partial` y gana las lecturas del catálogo (`GET /api/cores` y `GET /api/cores/{core}/plugins`, con y sin una etiqueta de filtro) en dos archivos congelados que U09-C escribe igual; la pantalla «Catálogo» en `/catalogo` (cores, sus versiones con su imagen y, por core, sus plugins con etiquetas, `coreCompatible` tal cual, sha256 y tamaño, más el enlace a la subida que pone U09-C); la pantalla «Detalle» en `/despliegues/{id}` y `/paises/{pais}` (el despliegue con su veredicto, sus plugins y el contexto de un fallo, y el historial del país con un enlace «Volver a este» por renglón, que lleva a la página de U09-C); en «Vista general», el país enlaza a su historial y el último despliegue a su detalle; y el menú con «Vista general», «Catálogo» y «Desplegar». Todo con bUnit y el `ManejadorHttpSimulado` de U09-A, sin ningún `POST`.

**Fuera** (lo que el codificador debe rechazar aunque lo vea roto):

- **Toda escritura**, que es de U09-C: la subida de JAR (`POST /api/cores/{core}/plugins`), la verificación, el plan, la confirmación y la reversión. Ningún `POST`, `PUT`, `DELETE` ni `PATCH`, ninguna cabecera `X-Synkra-Usuario`, ningún `MultipartFormDataContent` en esta tarea. La pantalla «Catálogo» solo **enlaza** a `catalogo/{core}/subir` y «Detalle» solo **enlaza** a `despliegues/{id}/volver`: las dos páginas las crea U09-C (decisión 8).
- Los archivos de U09-C (decisión 1): `front.Tests/ManejadorHttpSimulado.cs`, `front/Api/ApiSynkra.Escrituras.cs`, `front/Api/ModelosDespliegue.cs` y las páginas `Desplegar.razor`, `Reversion.razor` y `SubirPlugin.razor`. Si al handler simulado le falta algo para una prueba de esta tarea, se escribe la prueba sin eso o se para y se pregunta; no se toca.
- `front/Program.cs`, `front/Components/_Imports.razor`, `front/Components/App.razor`, `front/Components/Routes.razor`, `front/wwwroot/app.css`, los dos `.csproj`, `front/appsettings.json` y los fixtures de U09-A (se pueden **leer** desde las pruebas, no cambiar).
- La imagen, el `Dockerfile`, `deploy/`, el `Makefile` y los scripts (U09-D), y el resumen de U09 (U09-E). Nada en `backend/` ni en otro directorio fuera de `front/`, `front.Tests/` y la evidencia. Si el contrato de la API parece mal, se para y se reporta: el front se adapta a la API.
- Ninguna regla de versión ni de compatibilidad (P5, D3): ni comparar versiones, ni ordenar por versión, ni filtrar plugins por `coreCompatible`, ni decidir qué renglón del historial se puede revertir. Lo que se muestra sale tal cual de la API.
- Correr `dotnet` fuera de `scripts/dotnet-docker.sh`, `kubectl`, un clúster o un backend arrancado (AUTONOMIA-01).
- Tocar `CANDIDATAS.md`, el plan de U09, `unidades-y-tareas.md`, `aidlc-docs/`: lo hace el orquestador.

---

## Archivos de contexto

- `tareas/U09-A-proyecto-cliente-vista.md` (decisiones 1 a 13: layout, versiones, `scripts/dotnet-docker.sh`, `trx.py`, contrato, deserialización estricta, `ApiSynkra`, handler simulado, fixtures, marcado `data-*`) y `bitacoras/U09-A.md` (sus desviaciones), `revisiones/U09-A/ronda-1.md` (F-01: comportamientos sin prueba; F-02).
- `tareas/U09-C-desplegar.md` (decisión 1: el mismo reparto, visto desde el otro lado).
- `aidlc-docs/construction/plans/U09-front-code-generation-plan.md` (U09-T03 y U09-T05) y `unidades-y-tareas.md` (sección U09); `Modulo 3/specs/prd.md` §9.4 (pantallas 2 y 4) y CU5.
- El código real en `main`: `front/` y `front.Tests/` enteros.
- El contrato de la API (léase con `git show 0910a09:<ruta>`):
  - `backend/src/main/java/io/synkra/catalogo/RutasCores.java` (`CoreDto`: `nombre`, `versiones`; `VersionDto`: `version`, `imagen`) y `RutasPlugins.java` (`PluginDto`: `core`, `nombre`, `version`, `coreCompatible`, `etiquetas`, `sha256`, `tamano`; cada parámetro de la consulta es una pareja del filtro; un core inexistente da 404 antes que cualquier 422), `catalogo/api/CoreRegistrado.java` (cada versión tiene imagen) y `ListadoPlugins.java`;
  - `backend/src/main/java/io/synkra/registro/RutasRegistro.java` (`DespliegueDto`, `ContextoDto`) y `despliegue/VeredictoDto.java` (la forma del `veredicto` guardado: `global`, `plugins`, `motivosGlobales`), `despliegue/ConfirmacionServicio.java` (línea que guarda `VeredictoDto.de(...)` como veredicto).
- `CANDIDATAS.md`: C-133 (los escapes se prueban con `/` y `?`), C-134 (los criterios comprueban usos, no textos), C-135 (los criterios se corren en la máquina de David).

---

## Decisiones del orquestador

1. **Reparto de archivos con U09-C** (corren a la vez desde la misma base; la unión de las dos ramas tiene que fusionar sin conflicto).
   - **Comunes, idénticos en las dos ramas** (cada rama los escribe; git fusiona sin conflicto dos cambios iguales):
     - `front/Api/ApiSynkra.cs`: cambian **solo** las líneas 7 y 8, que quedan exactamente así (y ninguna otra línea del archivo cambia):
       ```
       /// <summary>Cliente tipado de la API REST de SynKra; todo lo que devuelve sale tal cual de la API.</summary>
       public partial class ApiSynkra
       ```
       Con eso el blob del archivo es `899488ab7d398e08ac192166d114e672bacf350c` (`git rev-parse HEAD:front/Api/ApiSynkra.cs`; el de la base es `9ca7f0a3a5035ab68763ea29613a449acd0e1f40`).
     - `front/Api/ModelosCatalogo.cs` y `front/Api/ApiSynkra.Catalogo.cs`, **congelados**: su contenido es el de la decisión 2, carácter por carácter (LF, una línea final), con blob `85479d56ec36af6804401d7a23343a576612c611` y `d5d7babb78d10f8d445bf2ea6c0a6d78cafaab77`. Si una prueba de esta tarea muestra que alguno de los tres está mal, **no se cambia**: se para y se reporta al orquestador, que enmienda las dos tareas a la vez.
   - **Solo U09-B (esta)**: `front/Components/Pages/Catalogo.razor`, `front/Components/Pages/Detalle.razor`, `front/Components/Pages/VistaGeneral.razor`, `front/Components/Layout/NavMenu.razor`; `front.Tests/ApiSynkraCatalogoTest.cs`, `CatalogoTest.cs`, `DetalleTest.cs`, `MenuTest.cs` y `VistaGeneralTest.cs` (solo se **añaden** métodos; ninguna línea de U09-A se borra ni cambia); fixtures nuevos con prefijo `front.Tests/Fixtures/catalogo-*.json` y `front.Tests/Fixtures/detalle-*.json`; `bitacoras/U09-B.md`, `bitacoras/U09-B-evidencia/`, `revisiones/U09-B/`.
   - **Solo U09-C**: `front.Tests/ManejadorHttpSimulado.cs`, `front/Api/ApiSynkra.Escrituras.cs`, `front/Api/ModelosDespliegue.cs`, las páginas `Desplegar.razor`, `Reversion.razor` y `SubirPlugin.razor`, sus pruebas y sus fixtures (`desplegar-*`, `reversion-*`, `subida-*`).
   - El menú lo toca solo esta tarea y ya lleva el enlace a «Desplegar» (decisión 7): dos inserciones de líneas contiguas en el mismo punto del menú dan conflicto en git (se probó), así que U09-C no toca `NavMenu.razor`.
2. **Los dos archivos congelados** (namespace `Synkra.Front.Api`; los usan las dos ramas). `front/Api/ModelosCatalogo.cs`:
   ```csharp
   namespace Synkra.Front.Api;

   public record VersionRegistrada(string Version, string Imagen);

   public record CoreRegistrado(string Nombre, IReadOnlyList<VersionRegistrada> Versiones);

   public record PluginRegistrado(
       string Core,
       string Nombre,
       string Version,
       string CoreCompatible,
       IReadOnlyDictionary<string, string> Etiquetas,
       string Sha256,
       long Tamano);
   ```
   `front/Api/ApiSynkra.Catalogo.cs`:
   ```csharp
   namespace Synkra.Front.Api;

   public partial class ApiSynkra
   {
       public Task<IReadOnlyList<CoreRegistrado>> CoresAsync(CancellationToken ct = default)
           => LeerAsync<IReadOnlyList<CoreRegistrado>>("api/cores", ct);

       public Task<IReadOnlyList<PluginRegistrado>> PluginsAsync(string core, CancellationToken ct = default)
           => LeerAsync<IReadOnlyList<PluginRegistrado>>($"api/cores/{Uri.EscapeDataString(core)}/plugins", ct);

       public Task<IReadOnlyList<PluginRegistrado>> PluginsConEtiquetaAsync(string core, string clave, string valor, CancellationToken ct = default)
           => LeerAsync<IReadOnlyList<PluginRegistrado>>(
               $"api/cores/{Uri.EscapeDataString(core)}/plugins?{Uri.EscapeDataString(clave)}={Uri.EscapeDataString(valor)}", ct);
   }
   ```
   Compilan sobre `0910a09` con `TreatWarningsAsErrors` (comprobado al escribir la tarea, `Build succeeded. 0 Warning(s)`). Los nombres de propiedad son los del JSON en PascalCase, sin `[JsonPropertyName]` (decisión 5 de U09-A); `CoreCompatible` es el nombre del campo de la API (decisión 10).
3. **Versiones, entorno y conteo: los de U09-A, sin cambios.** `scripts/dotnet-docker.sh` (imagen `mcr.microsoft.com/dotnet/sdk:10.0.404`, ya en la caché), los cuatro paquetes de prueba de U09-A y ninguno más, `--logger "trx;LogFileName=<nombre>.trx" --results-directory front.Tests/TestResults` y `python3 -I bitacoras/U09-A-evidencia/trx.py` (se usa el de U09-A, no se copia). Solo `[Fact]`. **Todo se corre en la máquina de David (x86_64)**, decisión del responsable del proyecto: ningún esperado lleva arquitectura y ninguno comprueba si hay o no un SDK del sistema (el `dotnet` del sistema, 10.0.115, no se usa). Ninguna descarga nueva: si la restauración pide un paquete que no está en el volumen `synkra-nuget`, se para.
4. **`ApiSynkraCatalogoTest`** (prueba los dos archivos congelados contra el handler simulado; nombres exactos):
   - `CoresLeeNombreVersionesEImagen`: `catalogo-cores.json` con `GET /api/cores`; afirma los dos cores, sus versiones **en el orden de la API** y la imagen de cada una, y que se pidió solo `GET /api/cores`.
   - `CoresConListaVaciaDevuelveListaVacia`.
   - `PluginsLeeTodosLosCamposConEtiquetas`: `catalogo-plugins-synkra-core.json` con `GET /api/cores/synkra-core/plugins`; los siete campos de cada plugin, `coreCompatible` como texto (`2.x`, `3.x`), `tamano` y las etiquetas.
   - `PluginsEscapaElCore`: `PluginsAsync("a/b?c")` pide `/api/cores/a%2Fb%3Fc/plugins` (C-133).
   - `PluginsConEtiquetaPideLaConsulta`: `("pais", "pe")` pide `/api/cores/synkra-core/plugins?pais=pe`.
   - `PluginsConEtiquetaEscapaClaveYValor`: `("a&b", "x y/?")` pide `/api/cores/synkra-core/plugins?a%26b=x%20y%2F%3F` (C-133).
   - `CampoDesconocidoEnPluginDaRespuestaInvalida`: `catalogo-plugins-campo-desconocido.json` con 200 da `RESPUESTA_INVALIDA`.
   - `CoreInexistenteDaElErrorDeLaApi`: ruta no registrada; `Estado` 404, `NO_ENCONTRADO`, `Recurso no encontrado`, `Detalles` vacía.
   Son 8.
5. **Fixtures nuevos** (escritos a mano con la forma exacta de los DTO del backend; ninguna prueba serializa un record para fabricar una respuesta; sha256 de 64 veces la misma letra):
   - `catalogo-cores.json`: `synkra-core` con `2.1.0` y `3.0.0` (en ese orden, cada una con `"imagen":"localhost:5001/synkra-core-ejemplo@sha256:<64 a>"`) y `otro-core` con `1.0.0` (`<64 d>`).
   - `catalogo-plugins-synkra-core.json`: tres plugins de `synkra-core`, en este orden: `kyc-co` `1.0.0` `"coreCompatible":"2.x"` con `"etiquetas":{"cliente":"e2e","pais":"co"}`; `kyc-pe` `1.3.0` `2.x` con `{"pais":"pe"}`; `kyc-pe` `2.0.0` **`3.x`** con `{"pais":"pe"}`; sha256 `<64 b>`, `<64 c>`, `<64 e>` y `tamano` distinto en cada uno.
   - `catalogo-plugins-otro-core.json` (`[]`) y `catalogo-plugins-campo-desconocido.json` (el primero de los tres con `"otro":1`).
   - `detalle-despliegue-7.json`: `DespliegueDto` `id` 7, `co`, `"tipo":"despliegue"`, `"estado":"listo"`, `"contexto":null`, con un `veredicto` con la forma real de `VeredictoDto` (`{"global":"APROBADA","plugins":[{"nombre":"kyc-co","version":"1.0.0","core":"synkra-core","compatible":true,"motivos":[],"alternativas":[]}],"motivosGlobales":[]}`).
   - `detalle-despliegue-4.json`: `id` 4, `co`, `"estado":"fallido"`, `"versionCore":"3.0.0"`, el mismo tipo de veredicto con `kyc-pe` `2.0.0`, y `"contexto":{"logCore":"<dos líneas que nombran kyc-pe>","logInit":"descargado kyc-pe-2.0.0.jar\n","eventos":"Warning BackOff: Back-off restarting failed container core\n"}`.
   - `detalle-despliegue-8.json`: `id` 8, `co`, `"estado":"fallido"` con `"contexto":null` (el registro puede no tener contexto de un fallo; la pantalla no lo deduce del estado).
   - `detalle-historial-co.json`: cuatro resúmenes de `co`, del más reciente al más antiguo: `5` (`reversion`, `"origenId":3`, `aplicando`, `terminadoEn` nulo), `4` (`despliegue`, `fallido`), `7` (`despliegue`, `listo`) y `3` (`despliegue`, `listo`). El orden no es el de los `id`: así una pantalla que reordena por `id` cae.
   - El `veredicto` es opaco para esta tarea (decisión 9): su forma solo importa para que sea realista.
6. **Pantalla «Catálogo»** (`Catalogo.razor`, `@page "/catalogo"`, `<PageTitle>Catálogo · SynKra</PageTitle>`, `<h1>Catálogo</h1>`). Inyecta `ApiSynkra`; en `OnInitializedAsync` llama una vez a `CoresAsync` y después, **en el orden de la API y uno tras otro**, a `PluginsAsync(core.Nombre)` por cada core. Marcado fijo:
   - Cargando: `<p data-estado="cargando">Cargando…</p>`. Sin cores: `<p data-estado="vacio">No hay cores registrados en la API.</p>`. Error de `CoresAsync`: el bloque de error de U09-A (`<div data-estado="error">` con `<p data-campo="error">{Codigo}: {Message}</p>` y un `<li>{campo}: {motivo}</li>` por detalle), y nada más.
   - Por core: `<section data-core="{nombre}">` con `<h2>{nombre}</h2>`; una tabla `<table data-tabla="versiones">` (cabecera `Versión | Imagen`) con un `<tr data-version="{version}">` por versión en el orden de la API; y una tabla `<table data-tabla="plugins">` (cabecera `Plugin | Versión | Core compatible | Etiquetas | sha256 | Tamaño (bytes)`) con un `<tr data-plugin="{nombre}" data-version="{version}">` por plugin en el orden de la API y celdas `<td data-campo="...">`: `nombre`, `version`, `coreCompatible` (el texto de la API, tal cual), `etiquetas` (un `<li>{clave}={valor}</li>` por etiqueta, ordenadas por clave con `StringComparer.Ordinal`), `sha256` (entero) y `tamano` (el número, sin formato).
   - Core sin plugins: `<p data-estado="sin-plugins">Este core no tiene plugins todavía.</p>` en lugar de la tabla de plugins. Error en `PluginsAsync` de un core: el bloque de error **dentro de su `section`**, y las demás secciones se muestran igual.
   - En cada `section`, el enlace `<a data-accion="subir" href="catalogo/{Uri.EscapeDataString(nombre)}/subir">Subir un plugin</a>` (página de U09-C, decisión 8).
   - Nada de reglas: un plugin con `coreCompatible` `3.x` en un core que solo tiene versiones `2.x` se muestra como cualquier otro.
7. **Menú y «Vista general».**
   - `NavMenu.razor`: después de la línea de «Vista general», dos líneas nuevas: `<NavLink href="catalogo">Catálogo</NavLink>` y `<NavLink href="desplegar">Desplegar</NavLink>`, en ese orden. La pantalla «Desplegar» es de U09-C: hasta que se fusione, el enlace lleva a la página «No existe esta página» de `Routes.razor` (se anota en la bitácora).
   - `VistaGeneral.razor`: la celda `pais` pasa a `<a href="paises/{Uri.EscapeDataString(pais)}">{pais}</a>` y la de `ultimoDespliegueId`, cuando no es nula, a `<a href="despliegues/{id}">#{id}</a>`; cuando es nula sigue `—` sin enlace. El texto de las celdas no cambia, así que las pruebas de U09-A siguen igual (CA-5 las corre todas). Nada más cambia en el archivo.
8. **Enlaces a las páginas de U09-C, con estas rutas exactas** (U09-C las crea con esas plantillas; las dos tareas las citan igual): `catalogo/{core}/subir` (`@page "/catalogo/{Core}/subir"`) y `despliegues/{id}/volver` (`@page "/despliegues/{Id:long}/volver"`). Los `href` son relativos, sin barra inicial (la `<base href="/">` de `App.razor` los resuelve).
9. **Pantalla «Detalle»** (`Detalle.razor`, con dos rutas: `@page "/despliegues/{Id:long}"` y `@page "/paises/{Pais}"`; `<PageTitle>Detalle · SynKra</PageTitle>`, `<h1>Detalle</h1>`). Parámetros `[Parameter] public long? Id` y `[Parameter] public string? Pais`.
   - Con `Id`: `DespliegueAsync(Id)`, y con el despliegue leído, `HistorialAsync(d.Pais)`; con `Pais`: solo `HistorialAsync(Pais)`. Dos llamadas como máximo, en ese orden. Si `DespliegueAsync` falla, se muestra su error y **no** se pide el historial.
   - El despliegue: `<section id="despliegue" data-id="{id}">` con un `<dl>` cuyos `<dd data-campo="...">` son `id` (`#{id}`), `pais`, `tipo` (`Despliegue` o `Reversión`), `origenId` (`#{n}` o `—`), `estado` (`Aplicando`, `Listo`, `Fallido`), `core`, `versionCore`, `imagen`, `creadoPor`, `confirmadoPor`, `confirmadoEn` y `terminadoEn`; las fechas en UTC con el formato `yyyy-MM-dd HH:mm:ss` más ` UTC` (`2026-11-27 11:00:00 UTC`, cultura invariante; nula, `—`). Los plugins en `<table data-tabla="plugins">` con `nombre`, `version`, `sha256` y `etiquetas` (como en el catálogo). El veredicto en `<pre data-campo="veredicto">` con el JSON tal como llegó (`JsonElement` escrito con sangría); la pantalla no lo interpreta.
   - El contexto, **solo si la API lo trae** (no se decide por el estado): `<section id="contexto">` con tres `<pre>`: `data-campo="logCore"`, `"logInit"` y `"eventos"` (vacío se muestra vacío).
   - El historial: `<table id="historial">` (cabecera `Despliegue | Tipo | Origen | Estado | Core | Versión | Plugins | Creado por | Confirmado por | Confirmado | Terminado | Acción`) con un `<tr data-id="{id}">` por resumen **en el orden de la API**; el renglón del despliegue abierto lleva además `data-actual="true"`. La celda `id` enlaza a `despliegues/{id}`; la celda `accion` trae `<a data-accion="volver" href="despliegues/{id}/volver">Volver a este</a>` en **todos** los renglones: qué despliegue se puede revertir lo decide la API (R-D11, 422 `ORIGEN_NO_LISTO`), no el front. Historial vacío: `<p data-estado="sin-historial">Este país no tiene despliegues registrados.</p>`.
   - Un botón `<button data-accion="actualizar">Actualizar</button>` vuelve a hacer las mismas lecturas (para ver un `aplicando` pasar a `listo` o `fallido`, CU2 paso 5). Sin temporizadores.
   - Cargando y errores: como en el catálogo; un error del historial se muestra dentro de su sección y deja ver el despliegue.
10. **U09-T06 con campos que nombran la compatibilidad.** El campo `coreCompatible` del catálogo es de la API y llega como `CoreCompatible` al record congelado; el `grep` literal del plan lo encuentra. Interpretación (a la bitácora): «solo encuentra llamadas a `ApiSynkra`» se cumple si toda línea que el `grep` encuentra es el identificador `CoreCompatible` del contrato (su declaración en `ModelosCatalogo.cs` o su lectura `.CoreCompatible` para mostrarlo). Ningún texto, comentario ni otro identificador con «compatib» en `front/`. El comportamiento lo fija `CatalogoTest.NoFiltraNiComparaVersiones`.
11. **`CatalogoTest`** (bUnit como en U09-A: hereda de `BunitContext`, el `ApiSynkra` real sobre el handler simulado, `Render<Catalogo>()` y `WaitForAssertion`; nombres exactos):
    - `MuestraCadaCoreConSusVersionesEImagenes`: dos `section[data-core]`, `synkra-core` y luego `otro-core`, con sus versiones en orden y la imagen entera.
    - `MuestraLosPluginsDeCadaCoreConSusEtiquetas`: los tres renglones de `synkra-core` en el orden del fixture, cada celda; las etiquetas de `kyc-co` son `cliente=e2e` y `pais=co`, en ese orden.
    - `EtiquetasSeOrdenanPorClave`: un fixture `catalogo-plugins-etiquetas-desordenadas.json` cuyas claves llegan `pais`, `cliente`, `Zona` se muestran `Zona`, `cliente`, `pais` (orden ordinal).
    - `NoFiltraNiComparaVersiones`: `kyc-pe` `2.0.0` (`3.x`) se muestra en `synkra-core` aunque el core tenga `2.1.0` y `3.0.0`, con `3.x` en su celda; hay tres renglones, ni uno menos.
    - `CoreSinPluginsMuestraAviso` (`otro-core`).
    - `SinCoresMuestraAviso`.
    - `ErrorAlLeerCoresMuestraCodigoYMensaje`: `error-500.json` con 500; `ERROR_INTERNO: Error interno del servidor`, ninguna `section`, y no se pidió ningún `plugins`.
    - `ErrorEnLosPluginsDeUnCoreNoEscondeLosDemas`: los plugins de `synkra-core` dan 500; su `section` trae el bloque de error y la de `otro-core` su aviso.
    - `EnlaceDeSubidaPorCore`: `href` de `a[data-accion=subir]` es `catalogo/synkra-core/subir` y `catalogo/otro-core/subir`; con un core `a/b` (fixture `catalogo-cores-raro.json`), `catalogo/a%2Fb/subir`.
    - `SoloLlamaALecturasDelCatalogo`: `Peticiones` es exactamente `[(GET, /api/cores), (GET, /api/cores/synkra-core/plugins), (GET, /api/cores/otro-core/plugins)]`.
    Son 10.
12. **`DetalleTest`** (con `Render<Detalle>(p => p.Add(x => x.Id, 7L))` o `p.Add(x => x.Pais, "co")`; nombres exactos):
    - `DespliegueMuestraSusCamposYSusPlugins`: `detalle-despliegue-7.json`; cada `dd[data-campo]` (incluidas `confirmadoEn` y `terminadoEn` con el formato UTC) y la tabla de plugins.
    - `FechasSeMuestranEnUtcYNulaComoGuion`: `historial-pe.json` de U09-A: `2026-11-27 11:00:00 UTC` (los milisegundos `.123` no se muestran) y `terminadoEn` nulo como `—`.
    - `DespliegueFallidoMuestraElContexto`: `detalle-despliegue-4.json`; los tres `pre` con el texto exacto del fixture.
    - `DespliegueSinContextoNoMuestraLaSeccion`: `detalle-despliegue-7.json`; ningún `section#contexto`.
    - `FallidoSinContextoNoMuestraLaSeccion`: `detalle-despliegue-8.json` (un `fallido` con `"contexto":null`, decisión 5); la pantalla muestra `Fallido`, ningún `section#contexto` y ningún error.
    - `VeredictoSeMuestraTalCual`: el texto del `pre[data-campo=veredicto]` se parsea como JSON y es igual (`JsonElement.DeepEquals`) al `veredicto` del fixture.
    - `MuestraElHistorialDelPaisDelDespliegue`: con `Id` 7, `Peticiones` es exactamente `[(GET, /api/despliegues/7), (GET, /api/paises/co/despliegues)]`; los renglones son `5`, `4`, `7`, `3` en ese orden y solo el `7` lleva `data-actual="true"`.
    - `HistorialDeUnPaisSinDespliegueAbierto`: con `Pais` `co`, una sola petición (`/api/paises/co/despliegues`), ninguna `section#despliegue`.
    - `HistorialEscapaElPais`: con `Pais` `a/b?c`, pide `/api/paises/a%2Fb%3Fc/despliegues` (C-133).
    - `CadaRenglonEnlazaAlDetalleYAVolver`: los cuatro renglones (también el `fallido` y el `aplicando`) traen `despliegues/{id}` y `despliegues/{id}/volver`.
    - `ActualizarVuelveALeer`: tras el primer render se registra otra respuesta para `/api/despliegues/7` (el mismo con `"estado":"fallido"`) y se pulsa `button[data-accion=actualizar]`; la pantalla dice `Fallido` y `Peticiones` tiene cuatro entradas, en el orden despliegue, historial, despliegue, historial.
    - `DespliegueInexistenteMuestraElErrorYNoPideHistorial`: ruta no registrada; `NO_ENCONTRADO: Recurso no encontrado` y una sola petición.
    - `ErrorDelHistorialDejaVerElDespliegue`: el historial da 500; `section#despliegue` está y el bloque de error también.
    - `SoloHaceLecturas`: después de render y de `Actualizar`, todos los métodos de `Peticiones` son `GET`.
    Son 14.
13. **`VistaGeneralTest`** gana dos pruebas (se añaden al final de la clase): `PaisEnlazaASuHistorial` (`paises-dos.json`: la celda `pais` de `co` es un `a` con `href` `paises/co`) y `UltimoDespliegueEnlazaAlDetalle` (`#7` enlaza a `despliegues/7`; con `paises-sin-despliegue.json` la celda es `—` sin `a`). **`MenuTest`** (`Render<NavMenu>()`): `MenuEnlazaLasTresPantallas`: los `href` de los `a` del menú son, en orden, `""` (o el que dé `NavLink` para la raíz), `catalogo` y `desplegar`, con los textos `Vista general`, `Catálogo` y `Desplegar`. En total esta tarea añade 8 + 10 + 14 + 2 + 1 = **35** pruebas; la suite pasa de 29 (19 de `ApiSynkraTest` y 10 de `VistaGeneralTest` en `0910a09`) a 64.
14. **Rojo primero.** Un primer commit trae las 35 pruebas y los fixtures, los dos archivos congelados, el cambio de las líneas 7 y 8 de `ApiSynkra.cs`, y `Catalogo.razor` y `Detalle.razor` con solo sus `@page`, `<PageTitle>` y `<h1>`; `VistaGeneral.razor` y `NavMenu.razor` sin tocar. Con ese commit, CA-3 y CA-4 dan `Failed!` con las pruebas de pantalla en `FALLA` (las de `ApiSynkraCatalogoTest` pueden pasar: los dos archivos congelados ya vienen escritos, y se anota). La bitácora pega su sha, su `git show --stat` y la salida de `trx.py`. Después, la implementación. CA-6 muestra con el código terminado que cada regla muerde.
15. **Bitácora en `bitacoras/U09-B.md`, evidencia en `bitacoras/U09-B-evidencia/`** (un `caN.sh` versionado y su `caN.txt` con fecha, sha, `uname -m`, el `RepoDigests` de la imagen del SDK, comando y salida completa; los `.trx` no se versionan). Toda la evidencia sale del **último commit que toca código** (`git log -1 --format=%h -- front front.Tests`); si después cambia código, se vuelve a correr. Las desviaciones de la bitácora son, como mínimo: la interpretación de U09-T06 (decisión 10), el enlace a «Desplegar» antes de que exista (decisión 7) y que la subida de JAR de U09-T03 y la reversión de U09-T05 están en U09-C (el criterio del plan de T03 y T05 se cumple en la unión: lo comprueba U09-D).

---

## Criterios de aceptación

Todos se corren desde la raíz del worktree. En las líneas `Passed!` / `Failed!` de VSTest los espacios pueden variar; lo que cuenta es el número.

- [ ] **CA-1**: la base es la de U09-A y los tres archivos comunes son los acordados con U09-C
  ```bash
  B=$(git merge-base HEAD main); git merge-base --is-ancestor 0910a09 "$B"; echo "base=$?"
  git diff --stat 0910a09 "$B" -- front front.Tests scripts/dotnet-docker.sh | wc -l
  git rev-parse HEAD:front/Api/ApiSynkra.cs HEAD:front/Api/ModelosCatalogo.cs HEAD:front/Api/ApiSynkra.Catalogo.cs
  git diff -U0 "$B" HEAD -- front/Api/ApiSynkra.cs | grep -E '^[-+][^-+]'
  ```
  Esperado: `base=0`; `0` (U09-A no cambió en la base); exactamente `899488ab7d398e08ac192166d114e672bacf350c`, `85479d56ec36af6804401d7a23343a576612c611` y `d5d7babb78d10f8d445bf2ea6c0a6d78cafaab77`; cuatro líneas: las dos viejas con `-` y las dos de la decisión 1 con `+`.

- [ ] **CA-2**: compila sin avisos y sin paquetes nuevos
  ```bash
  docker image inspect --format '{{index .RepoDigests 0}}' mcr.microsoft.com/dotnet/sdk:10.0.404
  scripts/dotnet-docker.sh build front.Tests
  git diff --name-only "$(git merge-base HEAD main)" HEAD -- '*.csproj' | wc -l
  ```
  Esperado: el `RepoDigests` (se anota); `Build succeeded.`, `0 Warning(s)` y `0 Error(s)`; `0` (ningún `.csproj` cambia).

- [ ] **CA-3**: U09-T03 (lectura). Las lecturas del catálogo y la pantalla «Catálogo»
  ```bash
  scripts/dotnet-docker.sh test front.Tests --filter CatalogoTest --logger "trx;LogFileName=catalogo.trx" --results-directory front.Tests/TestResults; echo "salida=$?"
  python3 -I bitacoras/U09-A-evidencia/trx.py front.Tests/TestResults/catalogo.trx
  python3 -I -c 'import json,sys; print([(p["nombre"], p["version"], p["coreCompatible"], sorted(p["etiquetas"])) for p in json.load(open(sys.argv[1]))])' front.Tests/Fixtures/catalogo-plugins-synkra-core.json
  ```
  Esperado: `Passed!` con `Failed: 0`, `Skipped: 0` y 18 pruebas (las 8 de `ApiSynkraCatalogoTest` y las 10 de `CatalogoTest`: el filtro del plan casa por contenido con los dos nombres), sin `No test matches`, y `salida=0`; `trx.py` con `passed` = `total` = 18, sin `FALLA`, y los 18 nombres de las decisiones 4 y 11; `[('kyc-co', '1.0.0', '2.x', ['cliente', 'pais']), ('kyc-pe', '1.3.0', '2.x', ['pais']), ('kyc-pe', '2.0.0', '3.x', ['pais'])]`. El revisor coteja los nombres de campo de los fixtures `catalogo-*` con `RutasCores.java` y `RutasPlugins.java` (`git show 0910a09:...`). El caso del descriptor inválido del criterio del plan está en U09-C (`SubidaCatalogoTest`); U09-D lo corre en la unión.

- [ ] **CA-4**: U09-T05 (lectura). La pantalla «Detalle», y los enlaces de «Vista general» y del menú
  ```bash
  scripts/dotnet-docker.sh test front.Tests --filter "FullyQualifiedName~DetalleTest|FullyQualifiedName~VistaGeneralTest|FullyQualifiedName~MenuTest" --logger "trx;LogFileName=detalle.trx" --results-directory front.Tests/TestResults; echo "salida=$?"
  python3 -I bitacoras/U09-A-evidencia/trx.py front.Tests/TestResults/detalle.trx
  ```
  Esperado: `Passed!` con `Failed: 0`, `Skipped: 0` y 27 pruebas (14 de `DetalleTest`, las 10 de `VistaGeneralTest` que trae `0910a09` más las 2 nuevas, y 1 de `MenuTest`; si la base es posterior y trae otras, el número sube y el revisor lo cuenta en la base con `trx.py`), `salida=0`; sin `FALLA` y con los nombres de las decisiones 12 y 13. (La parte de la reversión del criterio del plan de T05 está en U09-C, `ReversionDetalleTest`.)

- [ ] **CA-5**: toda la suite pasa, U09-T06, y ninguna escritura
  ```bash
  rm -rf front.Tests/TestResults
  scripts/dotnet-docker.sh test front.Tests --logger "trx;LogFileName=todo.trx" --results-directory front.Tests/TestResults; echo "salida=$?"
  python3 -I bitacoras/U09-A-evidencia/trx.py front.Tests/TestResults/todo.trx | head -1
  grep -rnE '\[(Fact|Theory)\([^)]*Skip|\[Theory' front.Tests --include='*.cs'; echo "saltos=$?"
  grep -rniE "semver|compatib" front/ --include='*.cs' --include='*.razor'; echo "grep-plan=$?"
  grep -rniE "semver|compatib" front/ --exclude-dir=bin --exclude-dir=obj | grep -vE '(\bstring |\.)CoreCompatible\b'; echo "resto=$?"
  grep -rnE 'System\.Version|Version\.(Try)?Parse|NuGet\.Versioning|Semver' front/ --include='*.cs' --include='*.razor' --include='*.csproj'; echo "versiones=$?"
  grep -rnE 'Http(Method\.)?(Post|Put|Delete|Patch)|PostAsync|PutAsync|DeleteAsync|PatchAsync|X-Synkra-Usuario|MultipartFormDataContent' front/ --include='*.cs' --include='*.razor'; echo "escrituras=$?"
  ```
  Esperado: `Passed!` con `Failed: 0`, `Skipped: 0` y la suite completa (la de la base más 35; con `0910a09`, 64), `salida=0`; `trx.py` con `passed` = `total` y `failed=0 notExecuted=0`; `saltos=1`; el `grep` del plan, corrido **después** de compilar, solo da líneas con `CoreCompatible` (la declaración en `front/Api/ModelosCatalogo.cs` y la lectura en `Catalogo.razor`) y el segundo `grep` da `resto=1` (decisión 10); `versiones=1`; `escrituras=1`.

- [ ] **CA-6**: rojo primero y cada regla muerde, demostrado en copias
  Guardar como `bitacoras/U09-B-evidencia/ca6.sh` (se versiona). Una copia por mutación (`git archive HEAD | tar -x -C "$D"` y `git init -q` dentro), sin `git stash` ni cambios en el worktree:
  ```bash
  D=$(mktemp -d); git archive HEAD | tar -x -C "$D"; (cd "$D" && git init -q)
  # Mutación anotada literalmente en la bitácora (sed o parche); una por copia:
  #   orden-versiones:  Catalogo.razor ordena las versiones de cada core (OrderBy) antes de mostrarlas
  #   filtra:           Catalogo.razor oculta los plugins cuyo CoreCompatible no empieza por la versión mayor de alguna versión del core
  #   etiquetas:        Catalogo.razor deja de ordenar las etiquetas por clave
  #   error-local:      un error en PluginsAsync de un core se muestra en lugar de toda la página
  #   subir:            el href de subida deja de escapar el nombre del core
  #   historial-orden:  Detalle.razor ordena el historial por Id descendente
  #   contexto-estado:  Detalle.razor muestra la sección de contexto solo si Estado es Fallido (y no si la API trae contexto)
  #   volver-listo:     Detalle.razor solo pone «Volver a este» en los renglones Listo
  #   sin-historial:    con Id, Detalle.razor no pide el historial
  #   fecha-local:      Detalle.razor muestra la fecha con ToLocalTime()
  #   veredicto:        Detalle.razor muestra solo la propiedad "global" del veredicto
  #   enlace-vista:     VistaGeneral.razor deja #id sin enlace
  #   menu:             NavMenu.razor pierde el enlace a «Desplegar»
  sed -i '<mutación>' "$D/<ruta>"
  (cd "$D" && scripts/dotnet-docker.sh test front.Tests --logger "trx;LogFileName=m.trx" --results-directory front.Tests/TestResults; echo "salida=$?")
  python3 -I bitacoras/U09-A-evidencia/trx.py "$D/front.Tests/TestResults/m.trx" | grep '^FALLA'
  rm -rf "$D"
  ```
  Esperado: con cada una de las trece mutaciones, `salida` distinto de 0, `Failed!`, y entre las líneas `FALLA` la prueba de esa regla: «orden-versiones» `MuestraCadaCoreConSusVersionesEImagenes` (el fixture trae `2.1.0` antes que `3.0.0`; la mutación ordena descendente, se anota cuál); «filtra» `NoFiltraNiComparaVersiones`; «etiquetas» `EtiquetasSeOrdenanPorClave`; «error-local» `ErrorEnLosPluginsDeUnCoreNoEscondeLosDemas`; «subir» `EnlaceDeSubidaPorCore`; «historial-orden» `MuestraElHistorialDelPaisDelDespliegue`; «contexto-estado» `FallidoSinContextoNoMuestraLaSeccion`; «volver-listo» `CadaRenglonEnlazaAlDetalleYAVolver`; «sin-historial» `MuestraElHistorialDelPaisDelDespliegue`; «fecha-local» `FechasSeMuestranEnUtcYNulaComoGuion` (el contenedor corre en UTC: la mutación usa `ToOffset(TimeSpan.FromHours(-5))` y se anota); «veredicto» `VeredictoSeMuestraTalCual`; «enlace-vista» `UltimoDespliegueEnlazaAlDetalle`; «menu» `MenuEnlazaLasTresPantallas`. Una mutación que no compila no cuenta: se reescribe para que compile y se anota. Además, la bitácora pega el rojo de la decisión 14: sha del commit, `git show --stat` y la salida de CA-3 y CA-4 sobre una copia de ese commit (`Failed!` y sus líneas `FALLA`).

- [ ] **CA-7**: el diff queda acotado
  Guardar como `bitacoras/U09-B-evidencia/ca7.sh`; `ca7.txt` se genera justo antes de su propio commit, con el árbol limpio y sin commits de código después: ese commit solo trae `ca7.sh`, `ca7.txt` y la línea de la bitácora que los cita.
  ```bash
  B=$(git merge-base HEAD main)
  git rev-parse --short HEAD; git status --porcelain | wc -l
  git diff --stat "$B"...HEAD
  git diff --name-only "$B"...HEAD | grep -v -E '^(front/Api/(ApiSynkra|ApiSynkra\.Catalogo|ModelosCatalogo)\.cs|front/Components/Pages/(Catalogo|Detalle|VistaGeneral)\.razor|front/Components/Layout/NavMenu\.razor|front\.Tests/(ApiSynkraCatalogoTest|CatalogoTest|DetalleTest|MenuTest|VistaGeneralTest)\.cs|front\.Tests/Fixtures/(catalogo|detalle)-[a-z0-9-]+\.json|bitacoras/U09-B\.md|bitacoras/U09-B-evidencia/.*|revisiones/U09-B/.*)$'; echo "fuera=$?"
  git diff --name-only "$B"...HEAD -- front.Tests/ManejadorHttpSimulado.cs front/Program.cs front/Components/_Imports.razor front/Components/App.razor front/Components/Routes.razor front/wwwroot front/appsettings.json '*.csproj' backend deploy Makefile scripts | wc -l
  git diff --numstat "$B"...HEAD -- front.Tests/VistaGeneralTest.cs front.Tests/Fixtures | awk '$2 != 0' | wc -l
  git diff --name-status --diff-filter=D "$B"...HEAD | wc -l
  ```
  Esperado: `fuera=1`; `0` (nada de U09-C, de la configuración ni de otras unidades); `0` (en `VistaGeneralTest.cs` y en los fixtures solo se añaden líneas); `0` (nada se borra). El sha impreso es el del padre del commit de `ca7.txt`, y el `wc -l` de `git status` cuenta solo `ca7.sh` y `ca7.txt`. Si hace falta evidencia que no cae en esas rutas, se para y se pregunta.

---

**Rojo primero:** el commit de la decisión 14 trae las 35 pruebas sin las pantallas; CA-3 y CA-4 dan `Failed!` sobre una copia de ese commit. Después se implementa, y CA-6 demuestra con el código terminado que cada regla muerde. Todo `dotnet` corre en `mcr.microsoft.com/dotnet/sdk:10.0.404` vía `scripts/dotnet-docker.sh`; nada de `kubectl` ni de escrituras a la API.
