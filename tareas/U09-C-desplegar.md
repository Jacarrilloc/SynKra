# U09-C: pantalla «Desplegar» con verificación en vivo, vuelta atrás, subida de JAR y arranque del front

**Unidad:** U09: `Front`
**Tareas del plan que agrupa:** U09-T04, y las escrituras de U09-T03 (subida de JAR) y de U09-T05 («Volver al anterior» con confirmación); además, el arranque de la aplicación que dejó pendiente la revisión de U09-A (F-04)
**Historias que implementa:** H-25
**Depende de:** U09-A (VERDE, fusionada en #110). Sale de `<main con U09-A>`: `0910a09` o un `main` posterior en el que `front/`, `front.Tests/` y `scripts/dotnet-docker.sh` no hayan cambiado (CA-1 lo comprueba). Corre **en paralelo con U09-B**, desde la misma base, con el reparto de archivos de la decisión 1: las dos ramas tocan tres archivos en común y los dejan **idénticos byte por byte**, así que la fusión de las dos no puede tener conflicto. Después viene U09-D (imagen y manifiesto, desde la unión de B y C).

---

## Alcance

**Dentro** (una línea, concreta):

> Las escrituras de `ApiSynkra` en un archivo `partial` propio (`POST /api/verificaciones`, `POST /api/despliegues`, `POST /api/despliegues/{id}/confirmacion`, `POST /api/despliegues/{id}/reversion` y la subida `multipart` a `POST /api/cores/{core}/plugins`, con la cabecera `X-Synkra-Usuario` cuando hay usuario) y sus modelos; la pantalla «Desplegar» en `/desplegar` (elegir país, core, versión y plugins, filtrar por etiqueta, verificar en vivo en cada cambio, preparar el plan solo con un veredicto `APROBADA` y confirmar solo con un plan de la selección actual); la página de vuelta atrás en `/despliegues/{id}/volver` (lee el despliegue, prepara la reversión solo cuando se pide y la confirma solo después de confirmar); la página de subida en `/catalogo/{core}/subir` (el motivo de rechazo de la API, visible); y una prueba que arranca la aplicación compilada (F-04 de U09-A). Todo con bUnit y el `ManejadorHttpSimulado`, que esta tarea amplía.

**Fuera** (lo que el codificador debe rechazar aunque lo vea roto):

- Los archivos de U09-B (decisión 1): las páginas `Catalogo.razor`, `Detalle.razor` y `VistaGeneral.razor`, el menú `NavMenu.razor` (lo toca solo U09-B, que ya pone el enlace a «Desplegar»), y las pruebas `ApiSynkraCatalogoTest`, `CatalogoTest`, `DetalleTest`, `MenuTest` y `VistaGeneralTest`.
- `front/Program.cs`, `front/Components/_Imports.razor`, `App.razor`, `Routes.razor`, `front/wwwroot/app.css`, los dos `.csproj`, `front/appsettings.json` y los fixtures de U09-A (se pueden leer, no cambiar). Si una página necesita un `@using` que no está en `_Imports.razor`, va en la propia página.
- La imagen, el `Dockerfile`, `deploy/`, el `Makefile` y los scripts (U09-D), y el resumen de U09 (U09-E). Nada en `backend/`. Si el contrato de la API parece mal (un nombre de campo, un código), se para y se reporta: el front se adapta a la API.
- Ninguna regla de versión ni de compatibilidad en el front (P5, D3): ni comparar versiones, ni filtrar los plugins por `coreCompatible`, ni impedir elegir dos versiones del mismo plugin, ni deducir del catálogo si una combinación sirve. El veredicto, el plan y los errores salen de la API; lo único que el front lee del veredicto para habilitar un botón es su campo `global` (decisión 7).
- Validar en el front el país, el usuario o las etiquetas: los valida la API (422 `PAIS_NO_PERMITIDO`, `USUARIO_INVALIDO`, `ENTIDAD_NO_PROCESABLE`) y el front muestra su error.
- Correr `dotnet` fuera de `scripts/dotnet-docker.sh`, `kubectl`, un clúster o un backend arrancado (AUTONOMIA-01). La prueba de arranque (decisión 12) levanta solo el front, dentro del contenedor del SDK, contra una URL de la API en la que no escucha nadie.
- Paquetes nuevos (ni `Microsoft.AspNetCore.Mvc.Testing`, ni WireMock.Net, ni Moq): los cuatro de U09-A y ninguno más.
- Tocar `CANDIDATAS.md`, el plan de U09, `unidades-y-tareas.md`, `aidlc-docs/`: lo hace el orquestador.

---

## Archivos de contexto

- `tareas/U09-A-proyecto-cliente-vista.md` (decisiones 1 a 13) y `bitacoras/U09-A.md`; `revisiones/U09-A/ronda-1.md` (F-01: comportamientos sin prueba; **F-04: «Ninguna prueba arranca la aplicación», que el arbitraje dejó para esta tarea**) y `ronda-1-respuesta.md`.
- `tareas/U09-B-catalogo-detalle.md` (decisiones 1, 2 y 8: el mismo reparto y las rutas que enlaza U09-B).
- `aidlc-docs/construction/plans/U09-front-code-generation-plan.md` (U09-T03, T04 y T05) y `unidades-y-tareas.md`; `Modulo 3/specs/prd.md` §9.4 (pantalla 3: «elegir país, core y plugins (filtrando por etiqueta), ver la verificación en vivo, revisar el resumen y confirmar»), CU2, CU4 y CU5.
- `aidlc-docs/construction/U07-despliegue/code/summary.md` («Qué expone», tabla R-D1 a R-D13, desviaciones 8 y 9: `X-Synkra-Usuario` es un marcador, no un control de acceso).
- El contrato de la API (léase con `git show 0910a09:<ruta>`):
  - `backend/src/main/java/io/synkra/despliegue/RutasVerificacion.java` (`SolicitudVerificacion`: `core`, `versionCore`, `plugins`; 200 también con `RECHAZADA`), `VeredictoDto.java` (`global`, `plugins` con `nombre`, `version`, `core`, `compatible`, `motivos`, `alternativas`; `motivosGlobales`; motivo: `codigo`, `plugin`, `mensaje`, `campo`, los dos últimos anulables; alternativa: `tipo`, `nombre`, `version`), `PluginPedido.java`;
  - `RutasPlanes.java` (`SolicitudPlan`: `pais`, `core`, `versionCore`, `plugins`; `PlanDto`: `id`, `pais`, `tipo`, `origenId`, `estado`, `core`, `versionCore`, `creadoPor`, `creadoEn`, `venceEn`, `resumen`, `cambios`; 201), `Resumen.java`, `Cambios.java`, `CambioCore.java` (`antes` anulable), `PluginResumen.java`;
  - `RutasConfirmacion.java` (`ConfirmacionDto`: `id`, `pais`, `estado`, `creadoPor`, `confirmadoPor`, `confirmadoEn`; 202) y `RutasReversion.java` (sin cuerpo; 201 con un `PlanDto` de `tipo` `reversion`);
  - `Usuario.java` (cabecera opcional `X-Synkra-Usuario`; sin ella, `anonimo`), `CodigosDespliegue.java`, `MotivosDespliegue.java`, `PlanServicio.java` (`rechazaSiNoCompatible`: 422 `COMBINACION_RECHAZADA` con un detalle `plugins.<nombre>` por motivo), `GuardaConfirmacion.java` (410 `PLAN_VENCIDO`, 409 `PLAN_NO_PENDIENTE`, 404), `Plan.java` (`pendiente`, `confirmado`, `vencido`);
  - `catalogo/RutasPlugins.java` (subida: partes `archivo` y `etiquetas`, esta opcional y un objeto JSON de texto a texto) y `catalogo/SubidaPlugin.java` (un descriptor inválido es 422 `ENTIDAD_NO_PROCESABLE` con un detalle por motivo de U01, `campo` el del descriptor y `motivo` `<CODIGO>: <mensaje>`; repetido, 409 `CONFLICTO`); `verificador/.../LectorDescriptor.java` (el texto real de `DESCRIPTOR_FALTA_CAMPO`: `Falta el campo obligatorio '<campo>'`) y `Verificador.java` (el de `VERSION_INCOMPATIBLE`).
- `CANDIDATAS.md`: C-133, C-134, C-135.

---

## Decisiones del orquestador

1. **Reparto de archivos con U09-B** (corren a la vez desde la misma base; la unión de las dos ramas tiene que fusionar sin conflicto).
   - **Comunes, idénticos en las dos ramas** (git fusiona sin conflicto dos cambios iguales):
     - `front/Api/ApiSynkra.cs`: cambian **solo** las líneas 7 y 8, que quedan exactamente así (y ninguna otra línea del archivo cambia):
       ```
       /// <summary>Cliente tipado de la API REST de SynKra; todo lo que devuelve sale tal cual de la API.</summary>
       public partial class ApiSynkra
       ```
       Blob resultante: `899488ab7d398e08ac192166d114e672bacf350c` (el de la base es `9ca7f0a3a5035ab68763ea29613a449acd0e1f40`). `LeerAsync`, `Interpretar` y `NoDisponible` siguen privados en ese archivo y se usan desde el `partial` nuevo; no se mueven ni se hacen públicos.
     - `front/Api/ModelosCatalogo.cs` y `front/Api/ApiSynkra.Catalogo.cs`, **congelados**: su contenido es el de la decisión 2 de `tareas/U09-B-catalogo-detalle.md`, carácter por carácter (LF, una línea final), con blob `85479d56ec36af6804401d7a23343a576612c611` y `d5d7babb78d10f8d445bf2ea6c0a6d78cafaab77`. Se copian de ahí; esta tarea no los prueba aparte (lo hace `ApiSynkraCatalogoTest` de U09-B) pero los usa. Si algo en ellos está mal, **no se cambia**: se para y se reporta, y el orquestador enmienda las dos tareas a la vez.
   - **Solo U09-C (esta)**: `front/Api/ApiSynkra.Escrituras.cs`, `front/Api/ModelosDespliegue.cs`; `front/Components/Pages/Desplegar.razor`, `Reversion.razor` y `SubirPlugin.razor`; `front.Tests/ManejadorHttpSimulado.cs` (decisión 5), `ApiSynkraEscriturasTest.cs`, `DesplegarTest.cs`, `ReversionDetalleTest.cs`, `SubidaCatalogoTest.cs` y `ArranqueTest.cs`; fixtures nuevos con prefijo `front.Tests/Fixtures/desplegar-*.json`, `reversion-*.json` y `subida-*.json`; `bitacoras/U09-C.md`, `bitacoras/U09-C-evidencia/`, `revisiones/U09-C/`.
   - **Solo U09-B**: las páginas `Catalogo.razor`, `Detalle.razor`, `VistaGeneral.razor`, el menú y sus pruebas y fixtures (`catalogo-*`, `detalle-*`).
   - **Las rutas que U09-B enlaza** son exactamente `@page "/catalogo/{Core}/subir"` (`SubirPlugin.razor`) y `@page "/despliegues/{Id:long}/volver"` (`Reversion.razor`), más `@page "/desplegar"` (`Desplegar.razor`, que enlaza el menú). Los enlaces de vuelta desde esta tarea a las páginas de U09-B (`despliegues/{id}`, `catalogo`) son `href` relativos.
   - **Nombres de las clases de prueba y filtros del plan.** VSTest, con `--filter X` sin operador, filtra por `FullyQualifiedName` que **contiene** `X`. Por eso `SubidaCatalogoTest` entra en `--filter CatalogoTest` (criterio del plan de T03, con el descriptor inválido) y `ReversionDetalleTest` en `--filter DetalleTest` (criterio de T05: «la reversión llama a la API solo después de confirmar»). En la unión, esos dos filtros juntan las pruebas de U09-B y de U09-C; U09-D lo comprueba.
2. **Versiones, entorno y conteo: los de U09-A, sin cambios.** `scripts/dotnet-docker.sh` (imagen `mcr.microsoft.com/dotnet/sdk:10.0.404`, ya en la caché), los cuatro paquetes de prueba, `--logger "trx;LogFileName=<nombre>.trx" --results-directory front.Tests/TestResults` y `python3 -I bitacoras/U09-A-evidencia/trx.py` (el de U09-A, sin copiarlo). Solo `[Fact]`. **Todo se corre en la máquina de David (x86_64)**: ningún esperado lleva arquitectura y ninguno comprueba si hay un SDK del sistema. Ninguna descarga nueva.
3. **Modelos** en `front/Api/ModelosDespliegue.cs` (namespace `Synkra.Front.Api`, records posicionales con los nombres del JSON en PascalCase, sin `[JsonPropertyName]`, con la deserialización estricta de U09-A):
   - Respuestas: `Veredicto(string Global, IReadOnlyList<VeredictoPlugin> Plugins, IReadOnlyList<MotivoVeredicto> MotivosGlobales)`; `VeredictoPlugin(string Nombre, string Version, string Core, bool Compatible, IReadOnlyList<MotivoVeredicto> Motivos, IReadOnlyList<AlternativaVeredicto> Alternativas)`; `MotivoVeredicto(string Codigo, string? Plugin, string Mensaje, string? Campo)`; `AlternativaVeredicto(string Tipo, string Nombre, string Version)`; `PlanDespliegue(long Id, string Pais, TipoDespliegue Tipo, long? OrigenId, EstadoPlan Estado, string Core, string VersionCore, string CreadoPor, DateTimeOffset CreadoEn, DateTimeOffset VenceEn, ResumenPlan Resumen, CambiosPlan Cambios)`; `ResumenPlan(string Namespace, string Core, string VersionCore, string Imagen, IReadOnlyList<PluginResumen> Plugins)`; `PluginResumen(string Nombre, string Version, string Sha256)`; `CambiosPlan(CambioCore Core, IReadOnlyList<PluginResumen> Anadidos, IReadOnlyList<PluginResumen> Quitados, IReadOnlyList<PluginResumen> Iguales, bool SinCambios)`; `CambioCore(string? Antes, string Despues, bool Igual)`; `Confirmacion(long Id, string Pais, EstadoDespliegue Estado, string CreadoPor, string ConfirmadoPor, DateTimeOffset ConfirmadoEn)`; `enum EstadoPlan { Pendiente, Confirmado, Vencido }`. `TipoDespliegue` y `EstadoDespliegue` son los de U09-A.
   - `Global`, `Codigo` y `Tipo` de la alternativa quedan como `string`: la API los manda en mayúsculas (`APROBADA`, `VERSION_INCOMPATIBLE`, `PLUGIN`) y el conversor de enums de U09-A es `snake_case` en minúscula; no se cambia el conversor ni se añade otro.
   - Peticiones: `PluginPedido(string Nombre, string Version)`, `SeleccionVerificacion(string Core, string VersionCore, IReadOnlyList<PluginPedido> Plugins)` y `SolicitudPlan(string Pais, string Core, string VersionCore, IReadOnlyList<PluginPedido> Plugins)`, serializados con las mismas `Opciones` de `ApiSynkra` (camelCase).
4. **`front/Api/ApiSynkra.Escrituras.cs`** (`public partial class ApiSynkra`). Métodos, con `string? usuario` y `CancellationToken ct = default` al final:
   - `Task<Veredicto> VerificarAsync(SeleccionVerificacion seleccion, CancellationToken ct = default)`: `POST api/verificaciones` con el JSON de la selección (`application/json; charset=utf-8`). Sin cabecera de usuario (la ruta no la lee).
   - `Task<PlanDespliegue> CrearPlanAsync(SolicitudPlan solicitud, string? usuario, CancellationToken ct = default)`: `POST api/despliegues`.
   - `Task<Confirmacion> ConfirmarAsync(long planId, string? usuario, CancellationToken ct = default)`: `POST api/despliegues/{planId}/confirmacion`, **sin cuerpo**.
   - `Task<PlanDespliegue> RevertirAsync(long origenId, string? usuario, CancellationToken ct = default)`: `POST api/despliegues/{origenId}/reversion`, sin cuerpo.
   - `Task<PluginRegistrado> SubirPluginAsync(string core, Stream jar, string nombreArchivo, string? etiquetas, CancellationToken ct = default)`: `POST api/cores/{Uri.EscapeDataString(core)}/plugins` con `MultipartFormDataContent`: la parte `archivo` (el flujo, con ese nombre de archivo y `Content-Type: application/java-archive`) y, solo si `etiquetas` no es nulo ni está en blanco, la parte `etiquetas` con ese texto **tal cual** (`application/json`). El front no parsea ni valida las etiquetas.
   - La cabecera `X-Synkra-Usuario` va **solo** si `usuario` no es nulo ni está en blanco, con el valor tal como llegó (sin recortarlo ni pasarlo a minúscula: la forma la valida la API, `USUARIO_INVALIDO`).
   - Todas con `Accept: application/json`, y la respuesta pasa por el mismo `Interpretar<T>` y los mismos `catch` que `LeerAsync` (2xx deserializado estricto; error de la API con su `codigo`, `mensaje` y `detalles`; `RESPUESTA_INVALIDA`; `API_NO_DISPONIBLE`; la cancelación pedida se propaga). Un solo método privado de envío en este archivo; nada se duplica de `ApiSynkra.cs`.
   - Las escrituras viven **solo** en este archivo: ninguna página construye una petición HTTP (CA-6 lo busca).
5. **`ManejadorHttpSimulado`** (solo esta tarea lo toca; las pruebas de U09-A y de U09-B lo usan tal como está en la base, así que **nada de lo que ya hace cambia**: `Responde`, `Lanza`, `Peticiones`, `Accept`, `Fixture`, el 404 por defecto y el token cancelado siguen igual). Se añade:
   - Por cada petición recibida, en listas paralelas a `Peticiones`: el cuerpo como texto (`""` sin contenido), el `Content-Type` del contenido (o `null`), y el valor de `X-Synkra-Usuario` (o `null` si no vino). Para un `multipart/form-data`, además, sus partes: nombre, nombre de archivo (o `null`), `Content-Type` y cuerpo como texto.
   - `RespondeSegunCuerpo(HttpMethod, string ruta, string cuerpoEsperado, HttpStatusCode, string cuerpo)`: para la misma ruta, la respuesta depende del cuerpo JSON recibido (comparado como JSON, sin depender de espacios). Un cuerpo que no casa con ninguna responde el 404 por defecto. Lo necesita la verificación en vivo, que manda varias selecciones a la misma ruta.
   - `Retiene(...)`: deja una respuesta registrada en espera hasta que la prueba la suelte, para probar respuestas que llegan fuera de orden (decisión 7, última regla).
   La forma exacta de la API del handler la decide el codificador; la bitácora la anota.
6. **Fixtures nuevos** (escritos a mano con la forma de los DTO del backend; ninguna prueba serializa un record para fabricar una respuesta; sha256 de 64 veces la misma letra):
   - `desplegar-paises.json`: `co` y `pe` (forma de `GET /api/paises`, U09-A).
   - `desplegar-cores.json`: `synkra-core` con `2.1.0` y `3.0.0`.
   - `desplegar-plugins.json`: `kyc-co` `1.0.0` (`2.x`, `{"pais":"co"}`), `kyc-pe` `1.3.0` (`2.x`, `{"pais":"pe"}`) y `kyc-pe` `2.0.0` (`3.x`, `{"pais":"pe"}`); `desplegar-plugins-pe.json`: solo los dos `kyc-pe` (lo que la API devuelve para `?pais=pe`).
   - `desplegar-veredicto-aprobado.json`: `{"global":"APROBADA","plugins":[{"nombre":"kyc-pe","version":"1.3.0","core":"synkra-core","compatible":true,"motivos":[],"alternativas":[]}],"motivosGlobales":[]}`; `desplegar-veredicto-vacio.json`: `APROBADA` sin plugins.
   - `desplegar-veredicto-rechazado.json` (CU4): `kyc-pe` `2.0.0`, `"compatible":false`, un motivo `{"codigo":"VERSION_INCOMPATIBLE","plugin":"kyc-pe","mensaje":"El plugin 'kyc-pe' 2.0.0 requiere el core 'synkra-core' en 3.x y se pidió 2.1.0","campo":"coreCompatible"}` y las alternativas `{"tipo":"PLUGIN","nombre":"kyc-pe","version":"1.3.0"}` y `{"tipo":"CORE","nombre":"synkra-core","version":"3.0.0"}`; `"global":"RECHAZADA"`.
   - `desplegar-plan-pe.json`: `PlanDto` `id` 12, `pe`, `"tipo":"despliegue"`, `"origenId":null`, `"estado":"pendiente"`, `"creadoPor":"ana"`, `creadoEn` y `venceEn` con milisegundos, `resumen` con `"namespace":"synkra-pe"`, la imagen por digest y `kyc-pe` `1.3.0`, y `cambios` de un país vacío (`"core":{"antes":null,"despues":"<imagen>","igual":false}`, `anadidos` con `kyc-pe`, `quitados` e `iguales` vacíos, `"sinCambios":false`).
   - `desplegar-confirmacion.json`: `{"id":12,"pais":"pe","estado":"aplicando","creadoPor":"ana","confirmadoPor":"luis","confirmadoEn":"..."}`.
   - `desplegar-error-combinacion.json` (422 `COMBINACION_RECHAZADA`, `La combinación no es compatible`, detalle `plugins.kyc-pe` con el mensaje de U01), `desplegar-error-seleccion.json` (422 `SELECCION_INVALIDA`, `La selección no es válida`), `desplegar-error-vencido.json` (410 `PLAN_VENCIDO`, `El plan venció sin confirmarse`), `desplegar-error-usuario.json` (422 `USUARIO_INVALIDO`, detalle `X-Synkra-Usuario`).
   - `reversion-origen-3.json` (el `DespliegueDto` `id` 3, `co`, `listo`), `reversion-plan.json` (`PlanDto` `id` 13, `"tipo":"reversion"`, `"origenId":3`, con `cambios` que vuelven de `kyc-pe` a `kyc-co`), `reversion-error-origen.json` (422 `ORIGEN_NO_LISTO`, `Solo se puede revertir a un despliegue listo`), `reversion-error-en-curso.json` (409 `DESPLIEGUE_EN_CURSO`, `Ya hay un despliegue en curso en el país`).
   - `subida-plugin.json` (`PluginDto` de `kyc-co` `1.0.0`, `coreCompatible` `2.x`, etiquetas `{"cliente":"demo","pais":"co"}`), `subida-error-descriptor.json` (422 `ENTIDAD_NO_PROCESABLE`, `La solicitud no se puede procesar`, detalle `{"campo":"version","motivo":"DESCRIPTOR_FALTA_CAMPO: Falta el campo obligatorio 'version'"}`), `subida-error-conflicto.json` (409 `CONFLICTO`, `La solicitud entra en conflicto con el estado actual`).
7. **Pantalla «Desplegar»** (`Desplegar.razor`, `@page "/desplegar"`, `<PageTitle>Desplegar · SynKra</PageTitle>`, `<h1>Desplegar</h1>`). Inyecta `ApiSynkra`. Marcado fijo (las pruebas buscan por atributo):
   - **Carga:** en `OnInitializedAsync`, `VistaGeneralAsync` (los países salen de la API, no de una lista del front) y `CoresAsync`. `<select data-campo="pais">`, `<select data-campo="core">` y `<select data-campo="versionCore">` (las versiones del core elegido, en el orden de la API), cada uno con una primera opción vacía; `<input data-campo="usuario">` (opcional). Un error de carga se muestra con el bloque de error de U09-A y no hay formulario.
   - **Plugins:** al elegir core, `PluginsAsync(core)`. `<input data-campo="filtro">` con `clave=valor` y `<button data-accion="filtrar">`: con texto, se parte por el **primer** `=` y se llama a `PluginsConEtiquetaAsync(core, clave, valor)`; vacío, `PluginsAsync(core)`; sin `=`, se manda la clave con valor vacío (la API decide). Un `<input type="checkbox" data-plugin="{nombre}" data-version="{version}">` por plugin que devolvió la API, **todos**, en su orden, con su `coreCompatible` y sus etiquetas a la vista. Cambiar de core o de filtro desmarca los plugins que ya no están en la lista.
   - **Verificación en vivo:** cada vez que cambian el core, la versión o los plugins marcados, y hay core y versión elegidos, se llama a `VerificarAsync` con `core`, `versionCore` y los plugins marcados **en el orden de la lista** (sin plugins, la lista va vacía). El país no entra en la verificación (la ruta no lo lee). El veredicto se muestra en `<section id="veredicto" data-global="{global}">`: un `<tr data-plugin="{nombre}" data-version="{version}">` por plugin del veredicto con `compatible` como `Sí`/`No`, un `<li data-campo="motivo">{codigo}: {mensaje}</li>` por motivo, un `<li data-campo="alternativa">{tipo} {nombre} {version}</li>` por alternativa (CU4: «qué versión del core o del plugin sí sirve»), y los `motivosGlobales` igual. Un error de la verificación se muestra en el bloque de error dentro de la sección y deja la selección sin veredicto. **Solo cuenta la respuesta de la última verificación pedida**: una respuesta que llega después de otra más nueva se descarta.
   - **Preparar:** `<button data-accion="preparar">Preparar plan</button>`, **habilitado solo si** hay país elegido, el veredicto mostrado es el de la selección actual y su `Global` es exactamente `"APROBADA"`, y no hay una petición en curso. Es la única lectura del veredicto que decide algo, y es leer la API, no calcular. Al pulsarlo, `CrearPlanAsync` con `pais`, `core`, `versionCore`, los plugins marcados y el usuario. Con 201, `<section id="plan" data-id="{id}">` con el resumen (`namespace`, imagen, plugins con sha256) y los cambios (`core` `antes` → `despues`, con `—` si `antes` es nulo; listas `anadidos`, `quitados` e `iguales`; `sinCambios` como `Sin cambios`), `creadoPor` y `venceEn` (formato UTC de U09-B: `yyyy-MM-dd HH:mm:ss UTC`). Con error (422 `COMBINACION_RECHAZADA`, 409, 503…), el bloque de error con sus detalles y sin plan.
   - **Confirmar:** `<button data-accion="confirmar">Confirmar</button>`, **habilitado solo si** hay un plan preparado para la selección actual (país, core, versión y plugins sin cambios desde que se preparó) y no se ha confirmado todavía. Cualquier cambio de la selección **descarta el plan** en pantalla (y deshabilita Confirmar) y verifica de nuevo. Al pulsarlo, `ConfirmarAsync(plan.Id, usuario)`. Con 202, `<p data-estado="confirmado">` con `<a href="despliegues/{id}">Ver el despliegue #{id}</a>`, y los dos botones quedan deshabilitados (no hay segunda confirmación del mismo plan). Con error (410 `PLAN_VENCIDO`, 409 `DESPLIEGUE_EN_CURSO`, 409 `PLAN_NO_PENDIENTE`, 422 `PLAN_DESACTUALIZADO`, 503, 502), el bloque de error, y el plan se descarta.
   - Con veredicto `RECHAZADA`, Preparar está deshabilitado y, como no hay plan, Confirmar también (criterio del plan de T04).
8. **Página de vuelta atrás** (`Reversion.razor`, `@page "/despliegues/{Id:long}/volver"`, `<PageTitle>Volver a un despliegue · SynKra</PageTitle>`, `<h1>Volver al despliegue #{Id}</h1>`). Es la confirmación de CU5 paso 3 («elige el despliegue anterior y confirma la vuelta atrás»):
   - Al abrirla, **solo** `DespliegueAsync(Id)` (lectura): se muestra el origen (país, core, versión, imagen, plugins, estado) y `<input data-campo="usuario">`. Ninguna escritura al abrir.
   - `<button data-accion="preparar-vuelta">Preparar la vuelta</button>`: `RevertirAsync(Id, usuario)` solo al pulsarlo. Con 201, `<section id="plan" data-id="{id}">` como en «Desplegar» (resumen y cambios, `tipo` `Reversión`, `origenId` `#{n}`). Con error (422 `ORIGEN_NO_LISTO`, 422 `ORIGEN_DESACTUALIZADO`, 404, 503), el bloque de error.
   - `<button data-accion="confirmar">Confirmar la vuelta</button>`, habilitado solo con el plan de reversión preparado: `ConfirmarAsync(plan.Id, usuario)` **solo después de pulsarlo**. Con 202, `<p data-estado="confirmado">` con el enlace a `despliegues/{id}`; con error, su bloque.
   - `<a data-accion="cancelar" href="despliegues/{Id}">Cancelar</a>`: vuelve al detalle sin llamar a nada.
   - La página no decide si el origen se puede revertir (lo dice la API con `ORIGEN_NO_LISTO`): Preparar está habilitado siempre que el origen se leyó.
   - **Interpretación del plan, a la bitácora:** U09-T05 dice «la reversión llama a la API solo después de confirmar». La reversión es un plan nuevo (R-D11) que no toca el clúster hasta confirmarse (R-D13); aquí ninguna escritura sale sin un clic explícito: abrir la página solo lee, «Preparar la vuelta» crea el plan y «Confirmar la vuelta» lo aplica.
9. **Página de subida** (`SubirPlugin.razor`, `@page "/catalogo/{Core}/subir"`, `<PageTitle>Subir un plugin · SynKra</PageTitle>`, `<h1>Subir un plugin a {Core}</h1>`):
   - `<InputFile accept=".jar">`, `<textarea data-campo="etiquetas">` (opcional, JSON de texto a texto; se manda tal cual) y `<button data-accion="subir">Subir</button>`, deshabilitado sin archivo o con una subida en curso.
   - Al pulsar: `SubirPluginAsync(Core, flujo, nombre, etiquetas)`. El flujo es `OpenReadStream(maxAllowedSize: 25 * 1024 * 1024)`: por encima del tope de la API (`synkra.max-artefacto`, 20 MB, R-C5) para que el 413 lo dé la API y no el navegador; se anota en la bitácora que es un tope técnico de transporte y no una regla.
   - Con 201: `<section id="subido">` con los siete campos del plugin (las etiquetas como en el catálogo) y `<a href="catalogo">Volver al catálogo</a>`. Con error: el bloque de error con **cada detalle** `{campo}: {motivo}` visible (criterio del plan de T03: «el caso de descriptor inválido con el motivo visible»).
10. **U09-T06 con campos que nombran la compatibilidad.** `coreCompatible` (catálogo) y `compatible` (veredicto) son campos de la API; el `grep` literal del plan los encuentra como `CoreCompatible` y `Compatible`. Interpretación (a la bitácora, igual que en U09-B): «solo encuentra llamadas a `ApiSynkra`» se cumple si toda línea encontrada es uno de esos dos identificadores del contrato, declarado en un record o leído como propiedad (`.Compatible`, `.CoreCompatible`) para mostrarlo. Ningún texto ni comentario con «compatib» o «incompatib» en `front/` (los textos que se muestran salen de la API). Los comportamientos los fijan `DesplegarTest.NoFiltraNiComparaVersiones` y `ConVeredictoRechazadoLosBotonesEstanDeshabilitados`.
11. **Pruebas** (bUnit como en U09-A: heredan de `BunitContext`, `ApiSynkra` real sobre el handler simulado, `WaitForAssertion`; nombres exactos):
    - **`ApiSynkraEscriturasTest`** (16): `VerificarEnviaLaSeleccionYLeeElVeredicto` (cuerpo JSON exacto `{"core":"synkra-core","versionCore":"2.1.0","plugins":[{"nombre":"kyc-pe","version":"1.3.0"}]}`, `Content-Type` JSON, sin cabecera de usuario, y cada campo del veredicto); `VerificarRechazadaTraeMotivosYAlternativas` (200 con `RECHAZADA`: no es un error); `VerificarConSeleccionInvalidaDaLosDetalles`; `CrearPlanEnviaElPaisYElUsuarioYLeeElPlan` (cuerpo con `pais`, cabecera `ana`, los doce campos del plan con `Resumen` y `Cambios`, `Antes` nulo); `SinUsuarioNoMandaLaCabecera` (con `null`); `UsuarioEnBlancoNoMandaLaCabecera` (con `"  "`); `UsuarioSeMandaTalCual` (`"Ana "` llega como `"Ana "`, y el 422 `USUARIO_INVALIDO` de la API se lee con su detalle); `CrearPlanRechazadoDaLosDetallesDeLaApi` (422 `COMBINACION_RECHAZADA`, detalle `plugins.kyc-pe`); `ConfirmarPideLaRutaDelPlanSinCuerpo` (`POST /api/despliegues/12/confirmacion`, cuerpo vacío, 202 leído en `Confirmacion` con `Aplicando`); `ConfirmarVencidoDaElErrorDeLaApi` (410); `RevertirPideLaRutaDelOrigenSinCuerpo` (`POST /api/despliegues/3/reversion`, `Tipo` `Reversion`, `OrigenId` 3); `SubirPluginMandaArchivoYEtiquetas` (ruta, las dos partes con su nombre, nombre de archivo y tipo, el cuerpo del archivo y las etiquetas tal cual); `SubirPluginSinEtiquetasNoMandaLaParte`; `SubirPluginEscapaElCore` (`a/b?c` da `/api/cores/a%2Fb%3Fc/plugins`, C-133); `SubidaConDescriptorInvalidoDaElMotivo` (422 con el detalle `version`); `EscrituraSinConexionDaApiNoDisponible` (`HttpRequestException` en `POST /api/verificaciones`).
    - **`DesplegarTest`** (17): `CargaPaisesYCoresDeLaApi` (opciones en el orden de la API); `ElegirCoreYVersionVerificaSinPlugins` (cuerpo con `"plugins":[]`); `MarcarUnPluginVuelveAVerificarConLaSeleccion`; `ElPaisNoEntraEnLaVerificacion` (cambiar solo el país no manda otra verificación, y el cuerpo no tiene `pais`); `ConVeredictoRechazadoLosBotonesEstanDeshabilitados` (criterio del plan; y el motivo y las dos alternativas a la vista); `ConVeredictoAprobadoPrepararSeHabilitaYConfirmarNo`; `SinPaisPrepararSigueDeshabilitado`; `PrepararCreaElPlanYMuestraResumenYCambios` (cuerpo del plan, cabecera de usuario, y la sección del plan); `ConfirmarLlamaALaConfirmacionDelPlanYEnlazaAlDetalle` (`POST /api/despliegues/12/confirmacion` solo después del clic, enlace `despliegues/12`, botones deshabilitados después); `SinClicNoHayConfirmacion` (con el plan preparado, `Peticiones` no tiene ninguna ruta `/confirmacion`); `CambiarLaSeleccionDescartaElPlan` (tras preparar, desmarcar un plugin quita la sección del plan, deshabilita Confirmar y verifica de nuevo); `ErrorAlPrepararSeMuestraYNoHayPlan` (422 `COMBINACION_RECHAZADA` con su detalle); `ErrorAlConfirmarSeMuestraYDescartaElPlan` (410 `PLAN_VENCIDO`); `VerificacionConErrorDeshabilitaLosBotones` (422 `SELECCION_INVALIDA`); `FiltroPorEtiquetaPideLaConsultaALaApi` (`pais=pe` pide `?pais=pe` y la lista es exactamente la que devolvió la API); `NoFiltraNiComparaVersiones` (los tres plugins de `desplegar-plugins.json` están en la lista, también `kyc-pe` `2.0.0` con `3.x`, y marcar las dos versiones de `kyc-pe` las manda las dos); `RespuestaViejaNoPisaLaNueva` (la primera verificación, retenida, llega después de la segunda: la pantalla muestra el veredicto de la segunda).
    - **`ReversionDetalleTest`** (7): `AbrirLaPaginaSoloLee` (`Peticiones` es exactamente `[(GET, /api/despliegues/3)]`); `PrepararLaVueltaCreaElPlanDeReversion` (`POST /api/despliegues/3/reversion` con la cabecera, y el plan `#13` con `Reversión` y `#3`); `LaVueltaSoloSeConfirmaDespuesDeConfirmar` (antes del clic ninguna `/confirmacion`; después, `POST /api/despliegues/13/confirmacion` y el enlace `despliegues/13`); `ConfirmarLaVueltaEstaDeshabilitadoSinPlan`; `CancelarNoLlamaANada` (el `href` es `despliegues/3` y no hay ningún `POST`); `OrigenNoListoMuestraElErrorDeLaApi` (422 `ORIGEN_NO_LISTO`); `ConfirmarLaVueltaConErrorLoMuestra` (409 `DESPLIEGUE_EN_CURSO`).
    - **`SubidaCatalogoTest`** (6), con `InputFile` de bUnit (`FindComponent<InputFile>().UploadFiles(...)`): `SubirMandaElJarYMuestraElPlugin`; `DescriptorInvalidoMuestraElMotivo` (criterio del plan de T03: `version: DESCRIPTOR_FALTA_CAMPO: Falta el campo obligatorio 'version'` visible); `EtiquetasSeMandanTalCual` (el texto del `textarea`, sin tocar, es el cuerpo de la parte); `SinEtiquetasNoSeMandaLaParte`; `SinArchivoElBotonEstaDeshabilitado`; `ConflictoMuestraElCodigoDeLaApi` (409 `CONFLICTO`).
    - **`ArranqueTest`** (2, decisión 12): `ConUrlValidaArrancaYSirveLaVistaGeneral` y `ConUrlMalaNoArranca`.
    Son 16 + 17 + 7 + 6 + 2 = **48**; la suite de la base (29 en `0910a09`) pasa a 77.
12. **Arranque de la aplicación (F-04 de U09-A).** `ArranqueTest` arranca el front **compilado** como un proceso aparte, dentro del mismo contenedor del SDK en el que corre `dotnet test` (sin puertos en el host, sin paquetes nuevos):
    - Busca la raíz del repositorio subiendo desde `AppContext.BaseDirectory` hasta el directorio que tiene `front/Synkra.Front.csproj`, y lanza `dotnet front/bin/Debug/net10.0/Synkra.Front.dll` con directorio de trabajo `front/` (así `appsettings.json` es el del proyecto), `ASPNETCORE_URLS=http://127.0.0.1:<puerto fijo, distinto en cada prueba>` y `ASPNETCORE_ENVIRONMENT=Production`. Si el `.dll` no está, la prueba falla con un mensaje que lo dice (no se salta).
    - `ConUrlValidaArrancaYSirveLaVistaGeneral`: con `Synkra__ApiUrl=http://127.0.0.1:9/` (ahí no escucha nadie), espera hasta 60 s a que `GET /` responda; afirma `200`, que el HTML trae `<h1>Vista general</h1>` y el bloque `data-estado="error"` con `API_NO_DISPONIBLE` (el prerender de la «Vista general» llamó a la API y mostró el error del front). Así se prueba que `Program.cs` registra `ApiSynkra` con la URL de la configuración.
    - `ConUrlMalaNoArranca`: con `Synkra__ApiUrl=ftp://h/`, el proceso termina solo en menos de 60 s con un código distinto de 0 y su salida (estándar o de error) contiene `Synkra:ApiUrl falta o no es una URL absoluta http o https` (decisión 8 de U09-A: una URL mala impide arrancar).
    - El proceso se mata en el `Dispose` de la prueba aunque falle. La imagen del front (U09-D) repite las dos comprobaciones con `docker run`.
13. **Rojo primero.** Un primer commit trae las 48 pruebas y los fixtures, la ampliación del handler simulado, los dos archivos congelados, el cambio de las líneas 7 y 8 de `ApiSynkra.cs`, `ModelosDespliegue.cs` completo, `ApiSynkra.Escrituras.cs` con los cinco métodos lanzando `NotImplementedException`, y las tres páginas con solo su `@page`, `<PageTitle>` y `<h1>`. Con ese commit, CA-3 a CA-5 dan `Failed!` (las dos de `ArranqueTest` pueden pasar: prueban código de U09-A, y se anota). La bitácora pega su sha, su `git show --stat` y la salida de `trx.py`. Después, la implementación. CA-7 muestra con el código terminado que cada regla muerde.
14. **Bitácora en `bitacoras/U09-C.md`, evidencia en `bitacoras/U09-C-evidencia/`** (un `caN.sh` versionado y su `caN.txt` con fecha, sha, `uname -m`, el `RepoDigests` de la imagen del SDK, comando y salida completa; los `.trx` no se versionan). Toda la evidencia sale del **último commit que toca código** (`git log -1 --format=%h -- front front.Tests`). Desviaciones mínimas de la bitácora: la interpretación de U09-T05 (decisión 8), la de U09-T06 (decisión 10), el tope de 25 MB de la subida (decisión 9), que la subida y la reversión viven en esta tarea y no en las pantallas de U09-B, y la forma del handler ampliado (decisión 5).

---

## Criterios de aceptación

Todos se corren desde la raíz del worktree. En las líneas `Passed!` / `Failed!` de VSTest los espacios pueden variar; lo que cuenta es el número.

- [ ] **CA-1**: la base es la de U09-A y los tres archivos comunes son los acordados con U09-B
  ```bash
  B=$(git merge-base HEAD main); git merge-base --is-ancestor 0910a09 "$B"; echo "base=$?"
  git diff --stat 0910a09 "$B" -- front front.Tests scripts/dotnet-docker.sh | wc -l
  git rev-parse HEAD:front/Api/ApiSynkra.cs HEAD:front/Api/ModelosCatalogo.cs HEAD:front/Api/ApiSynkra.Catalogo.cs
  git diff -U0 "$B" HEAD -- front/Api/ApiSynkra.cs | grep -E '^[-+][^-+]'
  ```
  Esperado: `base=0`; `0`; exactamente `899488ab7d398e08ac192166d114e672bacf350c`, `85479d56ec36af6804401d7a23343a576612c611` y `d5d7babb78d10f8d445bf2ea6c0a6d78cafaab77`; cuatro líneas: las dos viejas con `-` y las dos de la decisión 1 con `+`.

- [ ] **CA-2**: compila sin avisos y sin paquetes nuevos; las escrituras solo en su archivo
  ```bash
  docker image inspect --format '{{index .RepoDigests 0}}' mcr.microsoft.com/dotnet/sdk:10.0.404
  scripts/dotnet-docker.sh build front.Tests
  git diff --name-only "$(git merge-base HEAD main)" HEAD -- '*.csproj' | wc -l
  grep -rlE 'Http(Method\.)?(Post|Put|Delete|Patch)|PostAsync|PutAsync|DeleteAsync|PatchAsync|X-Synkra-Usuario|MultipartFormDataContent|HttpRequestMessage|HttpClient' front/ --include='*.cs' --include='*.razor' | sort
  ```
  Esperado: el `RepoDigests` (se anota); `Build succeeded.`, `0 Warning(s)`, `0 Error(s)`; `0`; exactamente `front/Api/ApiSynkra.Escrituras.cs`, `front/Api/ApiSynkra.cs` y `front/Program.cs` (este por `AddHttpClient`; ninguna página ni otro archivo construye peticiones ni nombra la cabecera).

- [ ] **CA-3**: las escrituras de `ApiSynkra` contra el handler simulado
  ```bash
  scripts/dotnet-docker.sh test front.Tests --filter ApiSynkraEscriturasTest --logger "trx;LogFileName=escrituras.trx" --results-directory front.Tests/TestResults; echo "salida=$?"
  python3 -I bitacoras/U09-A-evidencia/trx.py front.Tests/TestResults/escrituras.trx
  for f in front.Tests/Fixtures/desplegar-plan-pe.json front.Tests/Fixtures/desplegar-veredicto-rechazado.json front.Tests/Fixtures/desplegar-confirmacion.json front.Tests/Fixtures/subida-plugin.json; do printf '%s ' "$f"; python3 -I -c 'import json,sys
  def k(o,p=""):
      if isinstance(o,dict):
          for a,b in o.items(): yield a; yield from k(b)
      elif isinstance(o,list):
          for b in o: yield from k(b)
  print(sorted(set(k(json.load(open(sys.argv[1]))))))' "$f"; done
  ```
  Esperado: `Passed!` con `Failed: 0`, `Skipped: 0` y 16 pruebas, sin `No test matches`, `salida=0`; `trx.py` con los 16 nombres de la decisión 11 y sin `FALLA`; las claves de cada fixture son las del contrato (el revisor las coteja con `RutasPlanes.java`, `Resumen.java`, `Cambios.java`, `CambioCore.java`, `PluginResumen.java`, `VeredictoDto.java`, `RutasConfirmacion.java` y `RutasPlugins.java` de `0910a09`; en `subida-plugin.json` aparecen además las claves de sus etiquetas), sin `snake_case`.

- [ ] **CA-4**: U09-T04. «Desplegar»: con veredicto incompatible el botón está deshabilitado
  ```bash
  scripts/dotnet-docker.sh test front.Tests --filter DesplegarTest --logger "trx;LogFileName=desplegar.trx" --results-directory front.Tests/TestResults; echo "salida=$?"
  python3 -I bitacoras/U09-A-evidencia/trx.py front.Tests/TestResults/desplegar.trx
  ```
  Esperado: `Passed!` con `Failed: 0`, `Skipped: 0` y 17 pruebas, `salida=0`; los 17 nombres de la decisión 11, entre ellos `ConVeredictoRechazadoLosBotonesEstanDeshabilitados`, sin `FALLA`.

- [ ] **CA-5**: las escrituras de U09-T03 y U09-T05 con los filtros del plan, y el arranque
  ```bash
  scripts/dotnet-docker.sh test front.Tests --filter CatalogoTest --logger "trx;LogFileName=catalogo.trx" --results-directory front.Tests/TestResults; echo "catalogo=$?"
  python3 -I bitacoras/U09-A-evidencia/trx.py front.Tests/TestResults/catalogo.trx
  scripts/dotnet-docker.sh test front.Tests --filter DetalleTest --logger "trx;LogFileName=detalle.trx" --results-directory front.Tests/TestResults; echo "detalle=$?"
  python3 -I bitacoras/U09-A-evidencia/trx.py front.Tests/TestResults/detalle.trx
  scripts/dotnet-docker.sh test front.Tests --filter ArranqueTest --logger "trx;LogFileName=arranque.trx" --results-directory front.Tests/TestResults; echo "arranque=$?"
  python3 -I bitacoras/U09-A-evidencia/trx.py front.Tests/TestResults/arranque.trx
  ```
  Esperado: `catalogo=0` con las 6 de `SubidaCatalogoTest` (en esta rama sola; en la unión se suman las de U09-B), entre ellas `DescriptorInvalidoMuestraElMotivo`; `detalle=0` con las 7 de `ReversionDetalleTest`, entre ellas `LaVueltaSoloSeConfirmaDespuesDeConfirmar`; `arranque=0` con las 2 de `ArranqueTest`. Todas con `Skipped: 0`, sin `No test matches` y sin `FALLA`.

- [ ] **CA-6**: toda la suite pasa, sin saltos, y U09-T06
  ```bash
  rm -rf front.Tests/TestResults
  scripts/dotnet-docker.sh test front.Tests --logger "trx;LogFileName=todo.trx" --results-directory front.Tests/TestResults; echo "salida=$?"
  python3 -I bitacoras/U09-A-evidencia/trx.py front.Tests/TestResults/todo.trx | head -1
  grep -rnE '\[(Fact|Theory)\([^)]*Skip|\[Theory' front.Tests --include='*.cs'; echo "saltos=$?"
  grep -rniE "semver|compatib" front/ --include='*.cs' --include='*.razor'; echo "grep-plan=$?"
  grep -rniE "semver|compatib" front/ --exclude-dir=bin --exclude-dir=obj | grep -vE '(\bstring |\bbool |\.)(Core)?Compatible\b' | grep -vE '>[^<>]*[Cc]ompatib[^<>]*</(th|td|span|label|caption|h[1-6])>'; echo "resto=$?"
  grep -rnE 'System\.Version|Version\.(Try)?Parse|NuGet\.Versioning|Semver' front/ --include='*.cs' --include='*.razor' --include='*.csproj'; echo "versiones=$?"
  ```
  Esperado: `Passed!` con `Failed: 0`, `Skipped: 0` y la suite completa (la de la base más 48; con `0910a09`, 77), `salida=0`; `trx.py` con `passed` = `total`, `failed=0 notExecuted=0`; `saltos=1`; el `grep` del plan, corrido después de compilar, solo da las líneas de `CoreCompatible` y `Compatible` (declaraciones en `ModelosCatalogo.cs` y `ModelosDespliegue.cs`, y lecturas en las páginas) y `resto=1` (decisión 10; enmienda del orquestador del 2026-11-28: el texto visible de una cabecera o etiqueta no cuenta como regla de versión); `versiones=1` (que el front no ordena ni compara versiones lo prueban `NoFiltraNiComparaVersiones` y las pruebas de orden, no un `grep`).

- [ ] **CA-7**: rojo primero y cada regla muerde, demostrado en copias
  Guardar como `bitacoras/U09-C-evidencia/ca7.sh` (se versiona). Una copia por mutación (`git archive HEAD | tar -x -C "$D"` y `git init -q` dentro), sin `git stash` ni cambios en el worktree:
  ```bash
  D=$(mktemp -d); git archive HEAD | tar -x -C "$D"; (cd "$D" && git init -q)
  # Mutación anotada literalmente en la bitácora (sed o parche); una por copia:
  #   ruta-verificar:   "api/verificaciones" pasa a "api/verificacion"
  #   usuario-siempre:  la cabecera X-Synkra-Usuario se manda también con usuario en blanco
  #   usuario-recorta:  el usuario se manda con Trim()
  #   confirmar-cuerpo: ConfirmarAsync manda el plan como cuerpo JSON
  #   etiquetas-parte:  SubirPluginAsync manda la parte etiquetas también vacía
  #   escape-core:      SubirPluginAsync deja de escapar el core
  #   preparar-global:  Preparar se habilita con cualquier veredicto (no mira Global)
  #   confirmar-plan:   Confirmar se habilita sin plan preparado
  #   descarta-plan:    cambiar la selección deja el plan en pantalla
  #   pais-verifica:    la verificación se pide también al cambiar el país
  #   filtra:           Desplegar.razor oculta los plugins cuyo CoreCompatible no empieza por la versión mayor elegida
  #   vieja-pisa:       Desplegar.razor muestra la última respuesta que llega, no la de la última petición
  #   revertir-al-abrir: Reversion.razor llama a RevertirAsync en OnInitializedAsync
  #   confirma-solo:    Reversion.razor confirma justo después de preparar, sin el segundo clic
  #   motivo-oculto:    SubirPlugin.razor muestra solo el código y el mensaje del error, sin los detalles
  #   arranque:         Program.cs registra ApiSynkra con una URL fija en vez de ConfiguracionApi.UrlBase
  sed -i '<mutación>' "$D/<ruta>"
  (cd "$D" && scripts/dotnet-docker.sh test front.Tests --logger "trx;LogFileName=m.trx" --results-directory front.Tests/TestResults; echo "salida=$?")
  python3 -I bitacoras/U09-A-evidencia/trx.py "$D/front.Tests/TestResults/m.trx" | grep '^FALLA'
  rm -rf "$D"
  ```
  Esperado: con cada una de las dieciséis mutaciones, `salida` distinto de 0, `Failed!`, y entre las líneas `FALLA` la prueba de esa regla: «ruta-verificar» `VerificarEnviaLaSeleccionYLeeElVeredicto`; «usuario-siempre» `UsuarioEnBlancoNoMandaLaCabecera`; «usuario-recorta» `UsuarioSeMandaTalCual`; «confirmar-cuerpo» `ConfirmarPideLaRutaDelPlanSinCuerpo`; «etiquetas-parte» `SubirPluginSinEtiquetasNoMandaLaParte` o `SinEtiquetasNoSeMandaLaParte`; «escape-core» `SubirPluginEscapaElCore`; «preparar-global» `ConVeredictoRechazadoLosBotonesEstanDeshabilitados`; «confirmar-plan» `ConVeredictoAprobadoPrepararSeHabilitaYConfirmarNo`; «descarta-plan» `CambiarLaSeleccionDescartaElPlan`; «pais-verifica» `ElPaisNoEntraEnLaVerificacion`; «filtra» `NoFiltraNiComparaVersiones`; «vieja-pisa» `RespuestaViejaNoPisaLaNueva`; «revertir-al-abrir» `AbrirLaPaginaSoloLee`; «confirma-solo» `LaVueltaSoloSeConfirmaDespuesDeConfirmar`; «motivo-oculto» `DescriptorInvalidoMuestraElMotivo`; «arranque» `ConUrlValidaArrancaYSirveLaVistaGeneral` (la mutación «arranque» toca `Program.cs` **solo en la copia**; en la rama `Program.cs` no cambia). Una mutación que no compila no cuenta: se reescribe para que compile y se anota. Además, la bitácora pega el rojo de la decisión 13: sha del commit, `git show --stat` y la salida de CA-3 a CA-5 sobre una copia de ese commit (`Failed!` y sus líneas `FALLA`).

- [ ] **CA-8**: el diff queda acotado
  Guardar como `bitacoras/U09-C-evidencia/ca8.sh`; `ca8.txt` se genera justo antes de su propio commit, con el árbol limpio y sin commits de código después: ese commit solo trae `ca8.sh`, `ca8.txt` y la línea de la bitácora que los cita.
  ```bash
  B=$(git merge-base HEAD main)
  git rev-parse --short HEAD; git status --porcelain | wc -l
  git diff --stat "$B"...HEAD
  git diff --name-only "$B"...HEAD | grep -v -E '^(front/Api/(ApiSynkra|ApiSynkra\.Catalogo|ModelosCatalogo|ApiSynkra\.Escrituras|ModelosDespliegue)\.cs|front/Components/Pages/(Desplegar|Reversion|SubirPlugin)\.razor|front\.Tests/(ManejadorHttpSimulado|ApiSynkraEscriturasTest|DesplegarTest|ReversionDetalleTest|SubidaCatalogoTest|ArranqueTest)\.cs|front\.Tests/Fixtures/(desplegar|reversion|subida)-[a-z0-9-]+\.json|bitacoras/U09-C\.md|bitacoras/U09-C-evidencia/.*|revisiones/U09-C/.*)$'; echo "fuera=$?"
  git diff --name-only "$B"...HEAD -- front/Components/Pages/Catalogo.razor front/Components/Pages/Detalle.razor front/Components/Pages/VistaGeneral.razor front/Components/Layout front/Program.cs front/Components/_Imports.razor front/Components/App.razor front/Components/Routes.razor front/wwwroot front/appsettings.json '*.csproj' front.Tests/ApiSynkraTest.cs front.Tests/VistaGeneralTest.cs backend deploy Makefile scripts | wc -l
  git diff --name-status --diff-filter=D "$B"...HEAD | wc -l
  ```
  Esperado: `fuera=1`; `0` (nada de U09-B, de la configuración, de las pruebas de U09-A ni de otras unidades); `0` (nada se borra). El sha impreso es el del padre del commit de `ca8.txt`, y el `wc -l` de `git status` cuenta solo `ca8.sh` y `ca8.txt`. Si hace falta evidencia que no cae en esas rutas, se para y se pregunta.

---

**Rojo primero:** el commit de la decisión 13 trae las 48 pruebas con las escrituras y las páginas sin implementar; CA-3 a CA-5 dan `Failed!` sobre una copia de ese commit. Después se implementa, y CA-7 demuestra con el código terminado que cada regla muerde. Todo `dotnet` corre en `mcr.microsoft.com/dotnet/sdk:10.0.404` vía `scripts/dotnet-docker.sh`; nada de `kubectl`.
