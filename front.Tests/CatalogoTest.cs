using System.Net;
using Bunit;
using Microsoft.Extensions.DependencyInjection;
using Synkra.Front.Api;
using Synkra.Front.Components.Pages;

namespace Synkra.Front.Tests;

public class CatalogoTest : BunitContext
{
    private readonly ManejadorHttpSimulado _m = new();

    private IRenderedComponent<Catalogo> Pinta()
    {
        Services.AddSingleton(new ApiSynkra(new HttpClient(_m) { BaseAddress = new Uri("http://synkra.test/") }));
        return Render<Catalogo>();
    }

    private void Ok(string ruta, string fixture) => _m.Responde(HttpMethod.Get, ruta, HttpStatusCode.OK, ManejadorHttpSimulado.Fixture(fixture));

    private void Normal()
    {
        Ok("/api/cores", "catalogo-cores.json");
        Ok("/api/cores/synkra-core/plugins", "catalogo-plugins-synkra-core.json");
        Ok("/api/cores/otro-core/plugins", "catalogo-plugins-otro-core.json");
    }

    private static string[] Textos(IEnumerable<AngleSharp.Dom.IElement> es) => es.Select(e => e.TextContent.Trim()).ToArray();

    private static string Celda(AngleSharp.Dom.IElement fila, string campo) => fila.QuerySelector($"td[data-campo='{campo}']")!.TextContent.Trim();

    [Fact]
    public void MuestraCadaCoreConSusVersionesEImagenes()
    {
        Normal();

        var cut = Pinta();

        cut.WaitForAssertion(() => Assert.Equal(["synkra-core", "otro-core"], cut.FindAll("section[data-core]").Select(s => s.GetAttribute("data-core")!).ToArray()));
        var s = cut.Find("section[data-core='synkra-core']");
        Assert.Equal("synkra-core", s.QuerySelector("h2")!.TextContent.Trim());
        var filas = s.QuerySelectorAll("table[data-tabla='versiones'] tr[data-version]");
        Assert.Equal(["2.1.0", "3.0.0"], filas.Select(f => f.GetAttribute("data-version")!).ToArray());
        Assert.Equal(["Versión", "Imagen"], Textos(s.QuerySelectorAll("table[data-tabla='versiones'] th")));
        Assert.Equal(["2.1.0", $"localhost:5001/synkra-core-ejemplo@sha256:{new string('a', 64)}"], Textos(filas[0].QuerySelectorAll("td")));
        Assert.Equal(["3.0.0", $"localhost:5001/synkra-core-ejemplo@sha256:{new string('f', 64)}"], Textos(filas[1].QuerySelectorAll("td")));
        var o = cut.Find("section[data-core='otro-core'] table[data-tabla='versiones'] tr[data-version]");
        Assert.Equal("1.0.0", o.GetAttribute("data-version"));
        Assert.Contains(new string('d', 64), o.TextContent);
    }

    [Fact]
    public void MuestraLosPluginsDeCadaCoreConSusEtiquetas()
    {
        Normal();

        var cut = Pinta();

        cut.WaitForAssertion(() => cut.Find("section[data-core='synkra-core'] tr[data-plugin]"));
        var s = cut.Find("section[data-core='synkra-core']");
        Assert.Equal(["Plugin", "Versión", "Core compatible", "Etiquetas", "sha256", "Tamaño (bytes)"], Textos(s.QuerySelectorAll("table[data-tabla='plugins'] th")));
        var filas = s.QuerySelectorAll("table[data-tabla='plugins'] tr[data-plugin]");
        Assert.Equal([("kyc-co", "1.0.0"), ("kyc-pe", "1.3.0"), ("kyc-pe", "2.0.0")],
            filas.Select(f => (f.GetAttribute("data-plugin")!, f.GetAttribute("data-version")!)).ToArray());
        Assert.Equal("kyc-co", Celda(filas[0], "nombre"));
        Assert.Equal("1.0.0", Celda(filas[0], "version"));
        Assert.Equal("2.x", Celda(filas[0], "coreCompatible"));
        Assert.Equal(["cliente=e2e", "pais=co"], Textos(filas[0].QuerySelectorAll("td[data-campo='etiquetas'] li")));
        Assert.Equal(new string('b', 64), Celda(filas[0], "sha256"));
        Assert.Equal("1111", Celda(filas[0], "tamano"));
        Assert.Equal("2.x", Celda(filas[1], "coreCompatible"));
        Assert.Equal(["pais=pe"], Textos(filas[1].QuerySelectorAll("td[data-campo='etiquetas'] li")));
        Assert.Equal(new string('c', 64), Celda(filas[1], "sha256"));
        Assert.Equal("2222", Celda(filas[1], "tamano"));
        Assert.Equal("3.x", Celda(filas[2], "coreCompatible"));
        Assert.Equal(new string('e', 64), Celda(filas[2], "sha256"));
        Assert.Equal("3333", Celda(filas[2], "tamano"));
    }

    [Fact]
    public void EtiquetasSeOrdenanPorClave()
    {
        Ok("/api/cores", "catalogo-cores.json");
        Ok("/api/cores/synkra-core/plugins", "catalogo-plugins-etiquetas-desordenadas.json");
        Ok("/api/cores/otro-core/plugins", "catalogo-plugins-otro-core.json");

        var cut = Pinta();

        cut.WaitForAssertion(() => cut.Find("tr[data-plugin='kyc-co']"));
        Assert.Equal(["Zona=norte", "cliente=e2e", "pais=co"], Textos(cut.FindAll("tr[data-plugin='kyc-co'] td[data-campo='etiquetas'] li")));
    }

    [Fact]
    public void NoFiltraNiComparaVersiones()
    {
        Normal();

        var cut = Pinta();

        cut.WaitForAssertion(() => cut.Find("tr[data-plugin='kyc-pe'][data-version='2.0.0']"));
        var filas = cut.FindAll("section[data-core='synkra-core'] tr[data-plugin]");
        Assert.Equal(3, filas.Count);
        Assert.Equal("3.x", Celda(cut.Find("tr[data-plugin='kyc-pe'][data-version='2.0.0']"), "coreCompatible"));

        // otro-core solo tiene la version 1.0.0: sus plugins 2.x y 3.x se muestran igual
        Ok("/api/cores/otro-core/plugins", "catalogo-plugins-synkra-core.json");
        var otra = Render<Catalogo>();
        otra.WaitForAssertion(() => Assert.Equal(3, otra.FindAll("section[data-core='otro-core'] tr[data-plugin]").Count));
        Assert.Equal(["2.x", "2.x", "3.x"], otra.FindAll("section[data-core='otro-core'] td[data-campo='coreCompatible']").Select(c => c.TextContent.Trim()).ToArray());
    }

    [Fact]
    public void CoreSinPluginsMuestraAviso()
    {
        Normal();

        var cut = Pinta();

        cut.WaitForAssertion(() => cut.Find("section[data-core='otro-core'] p[data-estado='sin-plugins']"));
        var s = cut.Find("section[data-core='otro-core']");
        Assert.Equal("Este core no tiene plugins todavía.", s.QuerySelector("p[data-estado='sin-plugins']")!.TextContent.Trim());
        Assert.Null(s.QuerySelector("table[data-tabla='plugins']"));
    }

    [Fact]
    public void SinCoresMuestraAviso()
    {
        _m.Responde(HttpMethod.Get, "/api/cores", HttpStatusCode.OK, "[]");

        var cut = Pinta();

        cut.WaitForAssertion(() => Assert.Equal("No hay cores registrados en la API.", cut.Find("p[data-estado='vacio']").TextContent.Trim()));
        Assert.Empty(cut.FindAll("section"));
    }

    [Fact]
    public void ErrorAlLeerCoresMuestraCodigoYMensaje()
    {
        _m.Responde(HttpMethod.Get, "/api/cores", HttpStatusCode.InternalServerError, ManejadorHttpSimulado.Fixture("error-500.json"));

        var cut = Pinta();

        cut.WaitForAssertion(() => Assert.Equal("ERROR_INTERNO: Error interno del servidor", cut.Find("[data-estado='error'] p[data-campo='error']").TextContent.Trim()));
        Assert.Empty(cut.FindAll("section"));
        Assert.Equal([(HttpMethod.Get, "/api/cores")], _m.Peticiones.ToArray());
    }

    [Fact]
    public void ErrorEnLosPluginsDeUnCoreNoEscondeLosDemas()
    {
        Ok("/api/cores", "catalogo-cores.json");
        _m.Responde(HttpMethod.Get, "/api/cores/synkra-core/plugins", HttpStatusCode.InternalServerError, ManejadorHttpSimulado.Fixture("error-500.json"));
        Ok("/api/cores/otro-core/plugins", "catalogo-plugins-otro-core.json");

        var cut = Pinta();

        cut.WaitForAssertion(() => cut.Find("section[data-core='otro-core'] p[data-estado='sin-plugins']"));
        var s = cut.Find("section[data-core='synkra-core']");
        Assert.Equal("ERROR_INTERNO: Error interno del servidor", s.QuerySelector("[data-estado='error'] p[data-campo='error']")!.TextContent.Trim());
        Assert.Null(s.QuerySelector("table[data-tabla='plugins']"));
        Assert.NotNull(s.QuerySelector("table[data-tabla='versiones']"));
        Assert.Null(cut.Find("section[data-core='otro-core']").QuerySelector("[data-estado='error']"));
    }

    [Fact]
    public void EnlaceDeSubidaPorCore()
    {
        Normal();

        var cut = Pinta();

        cut.WaitForAssertion(() => cut.Find("section[data-core='otro-core'] p[data-estado='sin-plugins']"));
        Assert.Equal("catalogo/synkra-core/subir", cut.Find("section[data-core='synkra-core'] a[data-accion='subir']").GetAttribute("href"));
        Assert.Equal("catalogo/otro-core/subir", cut.Find("section[data-core='otro-core'] a[data-accion='subir']").GetAttribute("href"));
        Assert.Equal("Subir un plugin", cut.Find("section[data-core='otro-core'] a[data-accion='subir']").TextContent.Trim());

        Ok("/api/cores", "catalogo-cores-raro.json");
        Ok("/api/cores/a%2Fb/plugins", "catalogo-plugins-otro-core.json");
        var raro = Render<Catalogo>();
        raro.WaitForAssertion(() => raro.Find("section[data-core='a/b'] p[data-estado='sin-plugins']"));
        Assert.Equal("catalogo/a%2Fb/subir", raro.Find("a[data-accion='subir']").GetAttribute("href"));
        Assert.Contains((HttpMethod.Get, "/api/cores/a%2Fb/plugins"), _m.Peticiones.ToArray());
    }

    [Fact]
    public void SoloLlamaALecturasDelCatalogo()
    {
        Normal();

        var cut = Pinta();

        cut.WaitForAssertion(() => cut.Find("section[data-core='otro-core'] p[data-estado='sin-plugins']"));
        Assert.Equal([(HttpMethod.Get, "/api/cores"), (HttpMethod.Get, "/api/cores/synkra-core/plugins"), (HttpMethod.Get, "/api/cores/otro-core/plugins")],
            _m.Peticiones.ToArray());
    }
}
