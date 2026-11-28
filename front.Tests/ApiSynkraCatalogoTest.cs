using System.Net;
using Synkra.Front.Api;

namespace Synkra.Front.Tests;

public class ApiSynkraCatalogoTest
{
    private static ApiSynkra Cliente(ManejadorHttpSimulado m)
        => new(new HttpClient(m) { BaseAddress = new Uri("http://synkra.test/") });

    private static ManejadorHttpSimulado Con(string ruta, string fixture)
        => new ManejadorHttpSimulado().Responde(HttpMethod.Get, ruta, HttpStatusCode.OK, ManejadorHttpSimulado.Fixture(fixture));

    [Fact]
    public async Task CoresLeeNombreVersionesEImagen()
    {
        var m = Con("/api/cores", "catalogo-cores.json");

        var cores = await Cliente(m).CoresAsync();

        Assert.Equal(["synkra-core", "otro-core"], cores.Select(c => c.Nombre).ToArray());
        Assert.Equal(["2.1.0", "3.0.0"], cores[0].Versiones.Select(v => v.Version).ToArray());
        Assert.Equal($"localhost:5001/synkra-core-ejemplo@sha256:{new string('a', 64)}", cores[0].Versiones[0].Imagen);
        Assert.Equal($"localhost:5001/synkra-core-ejemplo@sha256:{new string('f', 64)}", cores[0].Versiones[1].Imagen);
        var otra = Assert.Single(cores[1].Versiones);
        Assert.Equal(new VersionRegistrada("1.0.0", $"localhost:5001/otro-core-ejemplo@sha256:{new string('d', 64)}"), otra);
        Assert.Equal([(HttpMethod.Get, "/api/cores")], m.Peticiones.ToArray());
    }

    [Fact]
    public async Task CoresConListaVaciaDevuelveListaVacia()
    {
        var m = new ManejadorHttpSimulado().Responde(HttpMethod.Get, "/api/cores", HttpStatusCode.OK, "[]");

        Assert.Empty(await Cliente(m).CoresAsync());
    }

    [Fact]
    public async Task PluginsLeeTodosLosCamposConEtiquetas()
    {
        var m = Con("/api/cores/synkra-core/plugins", "catalogo-plugins-synkra-core.json");

        var plugins = await Cliente(m).PluginsAsync("synkra-core");

        Assert.Equal(3, plugins.Count);
        var p = plugins[0];
        Assert.Equal("synkra-core", p.Core);
        Assert.Equal("kyc-co", p.Nombre);
        Assert.Equal("1.0.0", p.Version);
        Assert.Equal("2.x", p.CoreCompatible);
        Assert.Equal(new Dictionary<string, string> { ["cliente"] = "e2e", ["pais"] = "co" }, p.Etiquetas.ToDictionary(x => x.Key, x => x.Value));
        Assert.Equal(new string('b', 64), p.Sha256);
        Assert.Equal(1111L, p.Tamano);
        Assert.Equal("3.x", plugins[2].CoreCompatible);
        Assert.Equal(new string('e', 64), plugins[2].Sha256);
        Assert.Equal(3333L, plugins[2].Tamano);
        Assert.Equal(2222L, plugins[1].Tamano);
    }

    [Fact]
    public async Task PluginsEscapaElCore()
    {
        var m = new ManejadorHttpSimulado().Responde(HttpMethod.Get, "/api/cores/a%2Fb%3Fc/plugins", HttpStatusCode.OK, "[]");

        await Cliente(m).PluginsAsync("a/b?c");

        Assert.Equal([(HttpMethod.Get, "/api/cores/a%2Fb%3Fc/plugins")], m.Peticiones.ToArray());
    }

    [Fact]
    public async Task PluginsConEtiquetaPideLaConsulta()
    {
        var m = Con("/api/cores/synkra-core/plugins?pais=pe", "catalogo-plugins-synkra-core.json");

        var plugins = await Cliente(m).PluginsConEtiquetaAsync("synkra-core", "pais", "pe");

        Assert.Equal(3, plugins.Count);
        Assert.Equal([(HttpMethod.Get, "/api/cores/synkra-core/plugins?pais=pe")], m.Peticiones.ToArray());
    }

    [Fact]
    public async Task PluginsConEtiquetaEscapaClaveYValor()
    {
        var m = new ManejadorHttpSimulado().Responde(HttpMethod.Get, "/api/cores/synkra-core/plugins?a%26b=x%20y%2F%3F", HttpStatusCode.OK, "[]");

        await Cliente(m).PluginsConEtiquetaAsync("synkra-core", "a&b", "x y/?");

        Assert.Equal([(HttpMethod.Get, "/api/cores/synkra-core/plugins?a%26b=x%20y%2F%3F")], m.Peticiones.ToArray());
    }

    [Fact]
    public async Task CampoDesconocidoEnPluginDaRespuestaInvalida()
    {
        var m = Con("/api/cores/synkra-core/plugins", "catalogo-plugins-campo-desconocido.json");

        var e = await Assert.ThrowsAsync<ErrorApiSynkraException>(() => Cliente(m).PluginsAsync("synkra-core"));

        Assert.Equal("RESPUESTA_INVALIDA", e.Codigo);
    }

    [Fact]
    public async Task CoreInexistenteDaElErrorDeLaApi()
    {
        var m = new ManejadorHttpSimulado();

        var e = await Assert.ThrowsAsync<ErrorApiSynkraException>(() => Cliente(m).PluginsAsync("nada"));

        Assert.Equal(404, e.Estado);
        Assert.Equal("NO_ENCONTRADO", e.Codigo);
        Assert.Equal("Recurso no encontrado", e.Message);
        Assert.Empty(e.Detalles);
    }
}
