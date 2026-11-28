using System.Net;
using System.Text;
using Synkra.Front.Api;

namespace Synkra.Front.Tests;

public class ApiSynkraEscriturasTest
{
    private static readonly string A = new('a', 64);
    private static readonly string B = new('b', 64);
    private static readonly string C = new('c', 64);

    private static readonly SeleccionVerificacion Seleccion =
        new("synkra-core", "2.1.0", [new PluginPedido("kyc-pe", "1.3.0")]);

    private static readonly SolicitudPlan Solicitud =
        new("pe", "synkra-core", "2.1.0", [new PluginPedido("kyc-pe", "1.3.0")]);

    private static ApiSynkra Cliente(ManejadorHttpSimulado m)
        => new(new HttpClient(m) { BaseAddress = new Uri("http://synkra.test/") });

    private static async Task<ErrorApiSynkraException> Falla(Func<Task> accion)
        => await Assert.ThrowsAsync<ErrorApiSynkraException>(accion);

    private static MemoryStream Jar(string texto = "contenido-del-jar") => new(Encoding.UTF8.GetBytes(texto));

    [Fact]
    public async Task VerificarEnviaLaSeleccionYLeeElVeredicto()
    {
        var m = new ManejadorHttpSimulado().Responde(HttpMethod.Post, "/api/verificaciones", HttpStatusCode.OK,
            ManejadorHttpSimulado.Fixture("desplegar-veredicto-aprobado.json"));

        var v = await Cliente(m).VerificarAsync(Seleccion);

        Assert.Equal([(HttpMethod.Post, "/api/verificaciones")], m.Peticiones.ToArray());
        Assert.Equal("{\"core\":\"synkra-core\",\"versionCore\":\"2.1.0\",\"plugins\":[{\"nombre\":\"kyc-pe\",\"version\":\"1.3.0\"}]}", m.Cuerpos[0]);
        Assert.Equal("application/json; charset=utf-8", m.TiposContenido[0]);
        Assert.Null(m.Usuarios[0]);
        Assert.Equal("application/json", m.Accept[0]);
        Assert.Equal("APROBADA", v.Global);
        var p = Assert.Single(v.Plugins);
        Assert.Equal("kyc-pe", p.Nombre);
        Assert.Equal("1.3.0", p.Version);
        Assert.Equal("synkra-core", p.Core);
        Assert.True(p.Compatible);
        Assert.Empty(p.Motivos);
        Assert.Empty(p.Alternativas);
        Assert.Empty(v.MotivosGlobales);
    }

    [Fact]
    public async Task VerificarRechazadaTraeMotivosYAlternativas()
    {
        var m = new ManejadorHttpSimulado().Responde(HttpMethod.Post, "/api/verificaciones", HttpStatusCode.OK,
            ManejadorHttpSimulado.Fixture("desplegar-veredicto-rechazado.json"));

        var v = await Cliente(m).VerificarAsync(Seleccion with { Plugins = [new PluginPedido("kyc-pe", "2.0.0")] });

        Assert.Equal("RECHAZADA", v.Global);
        var p = Assert.Single(v.Plugins);
        Assert.False(p.Compatible);
        var motivo = Assert.Single(p.Motivos);
        Assert.Equal(new MotivoVeredicto("VERSION_INCOMPATIBLE", "kyc-pe",
            "El plugin 'kyc-pe' 2.0.0 requiere el core 'synkra-core' en 3.x y se pidió 2.1.0", "coreCompatible"), motivo);
        Assert.Equal(
            [new AlternativaVeredicto("PLUGIN", "kyc-pe", "1.3.0"), new AlternativaVeredicto("CORE", "synkra-core", "3.0.0")],
            p.Alternativas.ToArray());
    }

    [Fact]
    public async Task VerificarConSeleccionInvalidaDaLosDetalles()
    {
        var m = new ManejadorHttpSimulado().Responde(HttpMethod.Post, "/api/verificaciones", HttpStatusCode.UnprocessableEntity,
            ManejadorHttpSimulado.Fixture("desplegar-error-seleccion.json"));

        var e = await Falla(() => Cliente(m).VerificarAsync(Seleccion));

        Assert.Equal(422, e.Estado);
        Assert.Equal("SELECCION_INVALIDA", e.Codigo);
        Assert.Equal("La selección no es válida", e.Message);
        Assert.Equal([new DetalleError("core", "El core 'otro' no existe")], e.Detalles.ToArray());
    }

    [Fact]
    public async Task CrearPlanEnviaElPaisYElUsuarioYLeeElPlan()
    {
        var m = new ManejadorHttpSimulado().Responde(HttpMethod.Post, "/api/despliegues", HttpStatusCode.Created,
            ManejadorHttpSimulado.Fixture("desplegar-plan-pe.json"));

        var plan = await Cliente(m).CrearPlanAsync(Solicitud, "ana");

        Assert.Equal([(HttpMethod.Post, "/api/despliegues")], m.Peticiones.ToArray());
        Assert.Equal("{\"pais\":\"pe\",\"core\":\"synkra-core\",\"versionCore\":\"2.1.0\",\"plugins\":[{\"nombre\":\"kyc-pe\",\"version\":\"1.3.0\"}]}", m.Cuerpos[0]);
        Assert.Equal("application/json; charset=utf-8", m.TiposContenido[0]);
        Assert.Equal("ana", m.Usuarios[0]);
        var imagen = $"localhost:5001/synkra-core-ejemplo@sha256:{A}";
        Assert.Equal(12L, plan.Id);
        Assert.Equal("pe", plan.Pais);
        Assert.Equal(TipoDespliegue.Despliegue, plan.Tipo);
        Assert.Null(plan.OrigenId);
        Assert.Equal(EstadoPlan.Pendiente, plan.Estado);
        Assert.Equal("synkra-core", plan.Core);
        Assert.Equal("2.1.0", plan.VersionCore);
        Assert.Equal("ana", plan.CreadoPor);
        Assert.Equal(DateTimeOffset.Parse("2026-11-28T10:00:00.123Z"), plan.CreadoEn);
        Assert.Equal(DateTimeOffset.Parse("2026-11-28T10:15:00.123Z"), plan.VenceEn);
        Assert.Equal("synkra-pe", plan.Resumen.Namespace);
        Assert.Equal("synkra-core", plan.Resumen.Core);
        Assert.Equal("2.1.0", plan.Resumen.VersionCore);
        Assert.Equal(imagen, plan.Resumen.Imagen);
        Assert.Equal([new PluginResumen("kyc-pe", "1.3.0", C)], plan.Resumen.Plugins.ToArray());
        Assert.Null(plan.Cambios.Core.Antes);
        Assert.Equal(imagen, plan.Cambios.Core.Despues);
        Assert.False(plan.Cambios.Core.Igual);
        Assert.Equal([new PluginResumen("kyc-pe", "1.3.0", C)], plan.Cambios.Anadidos.ToArray());
        Assert.Empty(plan.Cambios.Quitados);
        Assert.Empty(plan.Cambios.Iguales);
        Assert.False(plan.Cambios.SinCambios);
    }

    [Fact]
    public async Task SinUsuarioNoMandaLaCabecera()
    {
        var m = new ManejadorHttpSimulado().Responde(HttpMethod.Post, "/api/despliegues", HttpStatusCode.Created,
            ManejadorHttpSimulado.Fixture("desplegar-plan-pe.json"));

        await Cliente(m).CrearPlanAsync(Solicitud, null);

        Assert.Single(m.Peticiones);
        Assert.Null(m.Usuarios[0]);
    }

    [Fact]
    public async Task UsuarioEnBlancoNoMandaLaCabecera()
    {
        var m = new ManejadorHttpSimulado().Responde(HttpMethod.Post, "/api/despliegues", HttpStatusCode.Created,
            ManejadorHttpSimulado.Fixture("desplegar-plan-pe.json"));

        await Cliente(m).CrearPlanAsync(Solicitud, "  ");

        Assert.Single(m.Peticiones);
        Assert.Null(m.Usuarios[0]);
    }

    [Fact]
    public async Task UsuarioSeMandaTalCual()
    {
        var m = new ManejadorHttpSimulado().Responde(HttpMethod.Post, "/api/despliegues", HttpStatusCode.UnprocessableEntity,
            ManejadorHttpSimulado.Fixture("desplegar-error-usuario.json"));

        var e = await Falla(() => Cliente(m).CrearPlanAsync(Solicitud, "Ana "));

        Assert.Equal("Ana ", m.Usuarios[0]);
        Assert.Equal(422, e.Estado);
        Assert.Equal("USUARIO_INVALIDO", e.Codigo);
        Assert.Equal("El usuario no es válido", e.Message);
        Assert.Equal([new DetalleError("X-Synkra-Usuario", "El usuario debe estar en minúsculas, sin espacios")], e.Detalles.ToArray());
    }

    [Fact]
    public async Task CrearPlanRechazadoDaLosDetallesDeLaApi()
    {
        var m = new ManejadorHttpSimulado().Responde(HttpMethod.Post, "/api/despliegues", HttpStatusCode.UnprocessableEntity,
            ManejadorHttpSimulado.Fixture("desplegar-error-combinacion.json"));

        var e = await Falla(() => Cliente(m).CrearPlanAsync(Solicitud, "ana"));

        Assert.Equal(422, e.Estado);
        Assert.Equal("COMBINACION_RECHAZADA", e.Codigo);
        Assert.Equal("La combinación no es compatible", e.Message);
        var d = Assert.Single(e.Detalles);
        Assert.Equal("plugins.kyc-pe", d.Campo);
        Assert.StartsWith("VERSION_INCOMPATIBLE: ", d.Motivo);
    }

    [Fact]
    public async Task ConfirmarPideLaRutaDelPlanSinCuerpo()
    {
        var m = new ManejadorHttpSimulado().Responde(HttpMethod.Post, "/api/despliegues/12/confirmacion", HttpStatusCode.Accepted,
            ManejadorHttpSimulado.Fixture("desplegar-confirmacion.json"));

        var c = await Cliente(m).ConfirmarAsync(12, "luis");

        Assert.Equal([(HttpMethod.Post, "/api/despliegues/12/confirmacion")], m.Peticiones.ToArray());
        Assert.Equal("", m.Cuerpos[0]);
        Assert.Null(m.TiposContenido[0]);
        Assert.Equal("luis", m.Usuarios[0]);
        Assert.Equal(12L, c.Id);
        Assert.Equal("pe", c.Pais);
        Assert.Equal(EstadoDespliegue.Aplicando, c.Estado);
        Assert.Equal("ana", c.CreadoPor);
        Assert.Equal("luis", c.ConfirmadoPor);
        Assert.Equal(DateTimeOffset.Parse("2026-11-28T10:05:00.456Z"), c.ConfirmadoEn);
    }

    [Fact]
    public async Task ConfirmarVencidoDaElErrorDeLaApi()
    {
        var m = new ManejadorHttpSimulado().Responde(HttpMethod.Post, "/api/despliegues/12/confirmacion", HttpStatusCode.Gone,
            ManejadorHttpSimulado.Fixture("desplegar-error-vencido.json"));

        var e = await Falla(() => Cliente(m).ConfirmarAsync(12, null));

        Assert.Equal(410, e.Estado);
        Assert.Equal("PLAN_VENCIDO", e.Codigo);
        Assert.Equal("El plan venció sin confirmarse", e.Message);
        Assert.Empty(e.Detalles);
    }

    [Fact]
    public async Task RevertirPideLaRutaDelOrigenSinCuerpo()
    {
        var m = new ManejadorHttpSimulado().Responde(HttpMethod.Post, "/api/despliegues/3/reversion", HttpStatusCode.Created,
            ManejadorHttpSimulado.Fixture("reversion-plan.json"));

        var plan = await Cliente(m).RevertirAsync(3, "ana");

        Assert.Equal([(HttpMethod.Post, "/api/despliegues/3/reversion")], m.Peticiones.ToArray());
        Assert.Equal("", m.Cuerpos[0]);
        Assert.Null(m.TiposContenido[0]);
        Assert.Equal("ana", m.Usuarios[0]);
        Assert.Equal(13L, plan.Id);
        Assert.Equal(TipoDespliegue.Reversion, plan.Tipo);
        Assert.Equal(3L, plan.OrigenId);
        Assert.Equal([new PluginResumen("kyc-pe", "1.3.0", C)], plan.Cambios.Quitados.ToArray());
    }

    [Fact]
    public async Task SubirPluginMandaArchivoYEtiquetas()
    {
        var m = new ManejadorHttpSimulado().Responde(HttpMethod.Post, "/api/cores/synkra-core/plugins", HttpStatusCode.Created,
            ManejadorHttpSimulado.Fixture("subida-plugin.json"));

        var plugin = await Cliente(m).SubirPluginAsync("synkra-core", Jar(), "kyc-co.jar", "{ \"pais\":\"co\" }");

        Assert.Equal([(HttpMethod.Post, "/api/cores/synkra-core/plugins")], m.Peticiones.ToArray());
        Assert.StartsWith("multipart/form-data", m.TiposContenido[0]);
        Assert.Null(m.Usuarios[0]);
        var partes = m.Partes[0];
        Assert.Equal(2, partes.Count);
        Assert.Equal(new ManejadorHttpSimulado.ParteMultipart("archivo", "kyc-co.jar", "application/java-archive", "contenido-del-jar"), partes[0]);
        Assert.Equal(new ManejadorHttpSimulado.ParteMultipart("etiquetas", null, "application/json", "{ \"pais\":\"co\" }"), partes[1]);
        Assert.Equal("synkra-core", plugin.Core);
        Assert.Equal("kyc-co", plugin.Nombre);
        Assert.Equal("1.0.0", plugin.Version);
        Assert.Equal("2.x", plugin.CoreCompatible);
        Assert.Equal(new Dictionary<string, string> { ["cliente"] = "demo", ["pais"] = "co" }, plugin.Etiquetas);
        Assert.Equal(B, plugin.Sha256);
        Assert.Equal(2048L, plugin.Tamano);
    }

    [Fact]
    public async Task SubirPluginSinEtiquetasNoMandaLaParte()
    {
        var m = new ManejadorHttpSimulado().Responde(HttpMethod.Post, "/api/cores/synkra-core/plugins", HttpStatusCode.Created,
            ManejadorHttpSimulado.Fixture("subida-plugin.json"));
        var api = Cliente(m);

        await api.SubirPluginAsync("synkra-core", Jar(), "kyc-co.jar", null);
        await api.SubirPluginAsync("synkra-core", Jar(), "kyc-co.jar", "   ");

        Assert.Equal(2, m.Partes.Count);
        foreach (var partes in m.Partes)
        {
            Assert.Equal("archivo", Assert.Single(partes).Nombre);
        }
    }

    [Fact]
    public async Task SubirPluginEscapaElCore()
    {
        var m = new ManejadorHttpSimulado().Responde(HttpMethod.Post, "/api/cores/a%2Fb%3Fc/plugins", HttpStatusCode.Created,
            ManejadorHttpSimulado.Fixture("subida-plugin.json"));

        await Cliente(m).SubirPluginAsync("a/b?c", Jar(), "kyc-co.jar", null);

        Assert.Equal([(HttpMethod.Post, "/api/cores/a%2Fb%3Fc/plugins")], m.Peticiones.ToArray());
    }

    [Fact]
    public async Task SubidaConDescriptorInvalidoDaElMotivo()
    {
        var m = new ManejadorHttpSimulado().Responde(HttpMethod.Post, "/api/cores/synkra-core/plugins", HttpStatusCode.UnprocessableEntity,
            ManejadorHttpSimulado.Fixture("subida-error-descriptor.json"));

        var e = await Falla(() => Cliente(m).SubirPluginAsync("synkra-core", Jar(), "kyc-co.jar", null));

        Assert.Equal(422, e.Estado);
        Assert.Equal("ENTIDAD_NO_PROCESABLE", e.Codigo);
        Assert.Equal("La solicitud no se puede procesar", e.Message);
        Assert.Equal([new DetalleError("version", "DESCRIPTOR_FALTA_CAMPO: Falta el campo obligatorio 'version'")], e.Detalles.ToArray());
    }

    [Fact]
    public async Task EscrituraSinConexionDaApiNoDisponible()
    {
        var m = new ManejadorHttpSimulado().Lanza(HttpMethod.Post, "/api/verificaciones", new HttpRequestException("sin red"));

        var e = await Falla(() => Cliente(m).VerificarAsync(Seleccion));

        Assert.Null(e.Estado);
        Assert.Equal("API_NO_DISPONIBLE", e.Codigo);
        Assert.IsType<HttpRequestException>(e.InnerException);
    }
}
