using System.Net;
using Microsoft.Extensions.Configuration;
using Synkra.Front.Api;

namespace Synkra.Front.Tests;

public class ApiSynkraTest
{
    private static readonly string A = new('a', 64);
    private static readonly string B = new('b', 64);
    private static readonly string C = new('c', 64);

    private static ApiSynkra Cliente(ManejadorHttpSimulado m, string baseUrl = "http://synkra.test/")
        => new(new HttpClient(m) { BaseAddress = new Uri(baseUrl) });

    private static async Task<ErrorApiSynkraException> Falla(Func<Task> accion)
        => await Assert.ThrowsAsync<ErrorApiSynkraException>(accion);

    [Fact]
    public async Task VistaGeneralLeeLosDosPaisesConTodosSusCampos()
    {
        var m = new ManejadorHttpSimulado().Responde(HttpMethod.Get, "/api/paises", HttpStatusCode.OK, ManejadorHttpSimulado.Fixture("paises-dos.json"));

        var paises = await Cliente(m).VistaGeneralAsync();

        Assert.Equal(2, paises.Count);
        var co = paises[0];
        Assert.Equal("co", co.Pais);
        Assert.Equal(LecturaCluster.Leido, co.Lectura);
        Assert.Equal($"localhost:5001/synkra-core@sha256:{A}", co.Imagen);
        var plugin = Assert.Single(co.Plugins);
        Assert.Equal(new PluginCorriendo("kyc-co", "1.0.0", B), plugin);
        Assert.True(co.Concuerda);
        Assert.Equal(7L, co.UltimoDespliegueId);
        Assert.Equal("synkra-core", co.Core);
        Assert.Equal("2.1.0", co.VersionCore);

        var pe = paises[1];
        Assert.Equal("pe", pe.Pais);
        Assert.Equal(LecturaCluster.NoDisponible, pe.Lectura);
        Assert.Null(pe.Imagen);
        Assert.Empty(pe.Plugins);
        Assert.Null(pe.Concuerda);
        Assert.Equal(3L, pe.UltimoDespliegueId);
        Assert.Equal("synkra-core", pe.Core);
        Assert.Equal("2.0.0", pe.VersionCore);

        Assert.Equal([(HttpMethod.Get, "/api/paises")], m.Peticiones);
    }

    [Fact]
    public async Task VistaGeneralConListaVaciaDevuelveListaVacia()
    {
        var m = new ManejadorHttpSimulado().Responde(HttpMethod.Get, "/api/paises", HttpStatusCode.OK, "[]");

        var paises = await Cliente(m).VistaGeneralAsync();

        Assert.Empty(paises);
    }

    [Fact]
    public async Task HistorialPideLaRutaDelPaisYLeeTipoOrigenYFechas()
    {
        var m = new ManejadorHttpSimulado().Responde(HttpMethod.Get, "/api/paises/pe/despliegues", HttpStatusCode.OK, ManejadorHttpSimulado.Fixture("historial-pe.json"));

        var historial = await Cliente(m).HistorialAsync("pe");

        Assert.Equal([9L, 5L], historial.Select(h => h.Id).ToArray());
        var reversion = historial[0];
        Assert.Equal(TipoDespliegue.Reversion, reversion.Tipo);
        Assert.Equal(5L, reversion.OrigenId);
        Assert.Equal(EstadoDespliegue.Aplicando, reversion.Estado);
        Assert.Equal(new DateTimeOffset(2026, 11, 27, 11, 0, 0, 123, TimeSpan.Zero), reversion.ConfirmadoEn);
        Assert.Null(reversion.TerminadoEn);
        var despliegue = historial[1];
        Assert.Equal(TipoDespliegue.Despliegue, despliegue.Tipo);
        Assert.Null(despliegue.OrigenId);
        Assert.Equal(EstadoDespliegue.Listo, despliegue.Estado);
        Assert.Equal(new DateTimeOffset(2026, 11, 27, 10, 0, 0, TimeSpan.Zero), despliegue.ConfirmadoEn);
        Assert.Equal(new DateTimeOffset(2026, 11, 27, 10, 5, 0, TimeSpan.Zero), despliegue.TerminadoEn);
        var plugin = Assert.Single(despliegue.Plugins);
        Assert.Equal("kyc-pe", plugin.Nombre);
        Assert.Equal(C, plugin.Sha256);
        Assert.Equal("2.x", plugin.Etiquetas["core"]);
        Assert.Equal("pe", plugin.Etiquetas["pais"]);
        Assert.Equal("1.0.0", plugin.Etiquetas["version"]);
        Assert.Equal([(HttpMethod.Get, "/api/paises/pe/despliegues")], m.Peticiones);
    }

    [Fact]
    public async Task HistorialEscapaElPais()
    {
        var m = new ManejadorHttpSimulado().Responde(HttpMethod.Get, "/api/paises/p%20e/despliegues", HttpStatusCode.OK, "[]");

        await Cliente(m).HistorialAsync("p e");

        Assert.Equal([(HttpMethod.Get, "/api/paises/p%20e/despliegues")], m.Peticiones);
    }

    [Fact]
    public async Task DespliegueFallidoTraeVeredictoYContexto()
    {
        var m = new ManejadorHttpSimulado().Responde(HttpMethod.Get, "/api/despliegues/5", HttpStatusCode.OK, ManejadorHttpSimulado.Fixture("despliegue-fallido.json"));

        var d = await Cliente(m).DespliegueAsync(5);

        Assert.Equal(5L, d.Id);
        Assert.Equal(EstadoDespliegue.Fallido, d.Estado);
        Assert.Equal(System.Text.Json.JsonValueKind.Object, d.Veredicto.ValueKind);
        Assert.False(d.Veredicto.GetProperty("valido").GetBoolean());
        Assert.NotNull(d.Contexto);
        Assert.Equal("línea 1\n", d.Contexto.LogCore);
        Assert.Equal("", d.Contexto.LogInit);
        Assert.Equal("BackOff", d.Contexto.Eventos);
    }

    [Fact]
    public async Task DespliegueListoNoTraeContexto()
    {
        var m = new ManejadorHttpSimulado().Responde(HttpMethod.Get, "/api/despliegues/7", HttpStatusCode.OK, ManejadorHttpSimulado.Fixture("despliegue-listo.json"));

        var d = await Cliente(m).DespliegueAsync(7);

        Assert.Equal(7L, d.Id);
        Assert.Equal(EstadoDespliegue.Listo, d.Estado);
        Assert.Null(d.Contexto);
        Assert.True(d.Veredicto.GetProperty("valido").GetBoolean());
    }

    [Fact]
    public async Task Error404DaCodigoYMensajeDeLaApi()
    {
        var m = new ManejadorHttpSimulado();

        var e = await Falla(() => Cliente(m).DespliegueAsync(99));

        Assert.Equal(404, e.Estado);
        Assert.Equal("NO_ENCONTRADO", e.Codigo);
        Assert.Equal("Recurso no encontrado", e.Message);
        Assert.Empty(e.Detalles);
    }

    [Fact]
    public async Task Error422DaLosDetallesConCampoYMotivo()
    {
        var m = new ManejadorHttpSimulado().Responde(HttpMethod.Get, "/api/paises/PE/despliegues", HttpStatusCode.UnprocessableEntity, ManejadorHttpSimulado.Fixture("error-422-pais.json"));

        var e = await Falla(() => Cliente(m).HistorialAsync("PE"));

        Assert.Equal(422, e.Estado);
        Assert.Equal("PAIS_INVALIDO", e.Codigo);
        Assert.Equal("El país no es válido", e.Message);
        var detalle = Assert.Single(e.Detalles);
        Assert.Equal("pais", detalle.Campo);
        Assert.Equal("El país debe ser un código de dos letras en minúscula (S4)", detalle.Motivo);
    }

    [Fact]
    public async Task ErrorSinCuerpoDeLaApiDaRespuestaInvalida()
    {
        var m = new ManejadorHttpSimulado().Responde(HttpMethod.Get, "/api/paises", HttpStatusCode.ServiceUnavailable, "<html>503</html>", "text/html");

        var e = await Falla(() => Cliente(m).VistaGeneralAsync());

        Assert.Equal(503, e.Estado);
        Assert.Equal("RESPUESTA_INVALIDA", e.Codigo);
        Assert.Equal("La API respondió 503 sin un cuerpo de error válido", e.Message);
        Assert.Empty(e.Detalles);
    }

    [Fact]
    public async Task CampoDesconocidoEnLaRespuestaDaRespuestaInvalida()
    {
        var m = new ManejadorHttpSimulado().Responde(HttpMethod.Get, "/api/paises", HttpStatusCode.OK, ManejadorHttpSimulado.Fixture("paises-campo-desconocido.json"));

        var e = await Falla(() => Cliente(m).VistaGeneralAsync());

        Assert.Equal(200, e.Estado);
        Assert.Equal("RESPUESTA_INVALIDA", e.Codigo);
        Assert.Equal("La respuesta de la API no tiene la forma esperada", e.Message);
        Assert.Empty(e.Detalles);
    }

    [Fact]
    public async Task FalloDeConexionDaApiNoDisponible()
    {
        var m = new ManejadorHttpSimulado().Lanza(HttpMethod.Get, "/api/paises", new HttpRequestException("rechazada"));

        var e = await Falla(() => Cliente(m).VistaGeneralAsync());

        Assert.Null(e.Estado);
        Assert.Equal("API_NO_DISPONIBLE", e.Codigo);
        Assert.Equal("No se pudo conectar con la API de SynKra", e.Message);
        Assert.Empty(e.Detalles);
    }

    [Fact]
    public async Task TiempoAgotadoDaApiNoDisponible()
    {
        var m = new ManejadorHttpSimulado().Lanza(HttpMethod.Get, "/api/paises", new TaskCanceledException("tiempo agotado"));

        var e = await Falla(() => Cliente(m).VistaGeneralAsync());

        Assert.Null(e.Estado);
        Assert.Equal("API_NO_DISPONIBLE", e.Codigo);
        Assert.Equal("No se pudo conectar con la API de SynKra", e.Message);
        Assert.Empty(e.Detalles);
    }

    [Fact]
    public async Task LaUrlBaseConPrefijoSeRespeta()
    {
        var m = new ManejadorHttpSimulado().Responde(HttpMethod.Get, "/x/api/paises", HttpStatusCode.OK, "[]");

        await Cliente(m, "http://synkra.test/x/").VistaGeneralAsync();

        Assert.Equal([(HttpMethod.Get, "/x/api/paises")], m.Peticiones);
    }

    private static IConfiguration Config(params (string, string?)[] valores)
        => new ConfigurationBuilder()
            .AddInMemoryCollection(valores.Select(v => new KeyValuePair<string, string?>(v.Item1, v.Item2)))
            .Build();

    [Fact]
    public void ConfiguracionSinUrlNoArranca()
    {
        var e = Assert.Throws<InvalidOperationException>(() => ConfiguracionApi.UrlBase(Config()));

        Assert.Equal("Synkra:ApiUrl falta o no es una URL absoluta http o https", e.Message);
    }

    [Fact]
    public void ConfiguracionConUrlRelativaNoArranca()
    {
        var e = Assert.Throws<InvalidOperationException>(() => ConfiguracionApi.UrlBase(Config(("Synkra:ApiUrl", "/api"))));

        Assert.Equal("Synkra:ApiUrl falta o no es una URL absoluta http o https", e.Message);
    }

    [Fact]
    public void ConfiguracionConUrlValidaTerminaEnBarra()
    {
        var url = ConfiguracionApi.UrlBase(Config(("Synkra:ApiUrl", "http://synkra-api:8080")));

        Assert.Equal("http://synkra-api:8080/", url.ToString());
    }
}
