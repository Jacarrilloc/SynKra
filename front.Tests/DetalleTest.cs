using System.Net;
using System.Text.Json;
using Bunit;
using Microsoft.Extensions.DependencyInjection;
using Synkra.Front.Api;
using Synkra.Front.Components.Pages;

namespace Synkra.Front.Tests;

public class DetalleTest : BunitContext
{
    private readonly ManejadorHttpSimulado _m = new();

    private IRenderedComponent<Detalle> PintaId(long id)
    {
        Services.AddSingleton(new ApiSynkra(new HttpClient(_m) { BaseAddress = new Uri("http://synkra.test/") }));
        return Render<Detalle>(p => p.Add(x => x.Id, id));
    }

    private IRenderedComponent<Detalle> PintaPais(string pais)
    {
        Services.AddSingleton(new ApiSynkra(new HttpClient(_m) { BaseAddress = new Uri("http://synkra.test/") }));
        return Render<Detalle>(p => p.Add(x => x.Pais, pais));
    }

    private void Ok(string ruta, string fixture) => _m.Responde(HttpMethod.Get, ruta, HttpStatusCode.OK, ManejadorHttpSimulado.Fixture(fixture));

    private void Siete()
    {
        Ok("/api/despliegues/7", "detalle-despliegue-7.json");
        Ok("/api/paises/co/despliegues", "detalle-historial-co.json");
    }

    private static string Dd(IRenderedComponent<Detalle> cut, string campo) => cut.Find($"section#despliegue dd[data-campo='{campo}']").TextContent.Trim();

    private static string[] Textos(IEnumerable<AngleSharp.Dom.IElement> es) => es.Select(e => e.TextContent.Trim()).ToArray();

    [Fact]
    public void DespliegueMuestraSusCamposYSusPlugins()
    {
        Siete();

        var cut = PintaId(7);

        cut.WaitForAssertion(() => cut.Find("section#despliegue"));
        Assert.Equal("7", cut.Find("section#despliegue").GetAttribute("data-id"));
        Assert.Equal("#7", Dd(cut, "id"));
        Assert.Equal("co", Dd(cut, "pais"));
        Assert.Equal("Despliegue", Dd(cut, "tipo"));
        Assert.Equal("—", Dd(cut, "origenId"));
        Assert.Equal("Listo", Dd(cut, "estado"));
        Assert.Equal("synkra-core", Dd(cut, "core"));
        Assert.Equal("2.1.0", Dd(cut, "versionCore"));
        Assert.Equal($"localhost:5001/synkra-core-ejemplo@sha256:{new string('a', 64)}", Dd(cut, "imagen"));
        Assert.Equal("ana", Dd(cut, "creadoPor"));
        Assert.Equal("luis", Dd(cut, "confirmadoPor"));
        Assert.Equal("2026-11-27 11:00:00 UTC", Dd(cut, "confirmadoEn"));
        Assert.Equal("2026-11-27 11:05:00 UTC", Dd(cut, "terminadoEn"));
        var fila = Assert.Single(cut.FindAll("section#despliegue table[data-tabla='plugins'] tbody tr"));
        Assert.Equal(["kyc-co", "1.0.0", new string('b', 64)], Textos(fila.QuerySelectorAll("td:not([data-campo='etiquetas'])")));
        Assert.Equal(["pais=co"], Textos(fila.QuerySelectorAll("td[data-campo='etiquetas'] li")));
    }

    [Fact]
    public void FechasSeMuestranEnUtcYNulaComoGuion()
    {
        Ok("/api/paises/pe/despliegues", "historial-pe.json");

        var cut = PintaPais("pe");

        cut.WaitForAssertion(() => cut.Find("table#historial tr[data-id='9']"));
        var fila = cut.Find("table#historial tr[data-id='9']");
        Assert.Equal("2026-11-27 11:00:00 UTC", fila.QuerySelector("td[data-campo='confirmadoEn']")!.TextContent.Trim());
        Assert.Equal("—", fila.QuerySelector("td[data-campo='terminadoEn']")!.TextContent.Trim());
        var otra = cut.Find("table#historial tr[data-id='5']");
        Assert.Equal("2026-11-27 10:00:00 UTC", otra.QuerySelector("td[data-campo='confirmadoEn']")!.TextContent.Trim());
        Assert.Equal("2026-11-27 10:05:00 UTC", otra.QuerySelector("td[data-campo='terminadoEn']")!.TextContent.Trim());
    }

    [Fact]
    public void DespliegueFallidoMuestraElContexto()
    {
        Ok("/api/despliegues/4", "detalle-despliegue-4.json");
        Ok("/api/paises/co/despliegues", "detalle-historial-co.json");

        var cut = PintaId(4);

        cut.WaitForAssertion(() => cut.Find("section#contexto"));
        Assert.Equal("Fallido", Dd(cut, "estado"));
        Assert.Equal("Error al cargar el plugin kyc-pe\nkyc-pe-2.0.0.jar no es compatible con el core\n", cut.Find("section#contexto pre[data-campo='logCore']").TextContent);
        Assert.Equal("descargado kyc-pe-2.0.0.jar\n", cut.Find("section#contexto pre[data-campo='logInit']").TextContent);
        Assert.Equal("Warning BackOff: Back-off restarting failed container core\n", cut.Find("section#contexto pre[data-campo='eventos']").TextContent);
    }

    [Fact]
    public void DespliegueSinContextoNoMuestraLaSeccion()
    {
        Siete();

        var cut = PintaId(7);

        cut.WaitForAssertion(() => cut.Find("section#despliegue"));
        Assert.Empty(cut.FindAll("section#contexto"));
    }

    [Fact]
    public void FallidoSinContextoNoMuestraLaSeccion()
    {
        Ok("/api/despliegues/8", "detalle-despliegue-8.json");
        Ok("/api/paises/co/despliegues", "detalle-historial-co.json");

        var cut = PintaId(8);

        cut.WaitForAssertion(() => cut.Find("section#despliegue"));
        Assert.Equal("Fallido", Dd(cut, "estado"));
        Assert.Empty(cut.FindAll("section#contexto"));
        Assert.Empty(cut.FindAll("[data-estado='error']"));
    }

    [Fact]
    public void VeredictoSeMuestraTalCual()
    {
        Siete();

        var cut = PintaId(7);

        cut.WaitForAssertion(() => cut.Find("pre[data-campo='veredicto']"));
        using var mostrado = JsonDocument.Parse(cut.Find("pre[data-campo='veredicto']").TextContent);
        using var fixture = JsonDocument.Parse(ManejadorHttpSimulado.Fixture("detalle-despliegue-7.json"));
        Assert.True(JsonElement.DeepEquals(fixture.RootElement.GetProperty("veredicto"), mostrado.RootElement));
    }

    [Fact]
    public void MuestraElHistorialDelPaisDelDespliegue()
    {
        Siete();

        var cut = PintaId(7);

        cut.WaitForAssertion(() => cut.Find("table#historial tr[data-id]"));
        Assert.Equal([(HttpMethod.Get, "/api/despliegues/7"), (HttpMethod.Get, "/api/paises/co/despliegues")], _m.Peticiones.ToArray());
        var filas = cut.FindAll("table#historial tr[data-id]");
        Assert.Equal(["5", "4", "7", "3"], filas.Select(f => f.GetAttribute("data-id")!).ToArray());
        Assert.Equal(["7"], filas.Where(f => f.GetAttribute("data-actual") == "true").Select(f => f.GetAttribute("data-id")!).ToArray());
        Assert.Equal(["Despliegue", "Tipo", "Origen", "Estado", "Core", "Versión", "Plugins", "Creado por", "Confirmado por", "Confirmado", "Terminado", "Acción"],
            Textos(cut.FindAll("table#historial th")));
        var cinco = filas[0];
        Assert.Equal("Reversión", cinco.QuerySelector("td[data-campo='tipo']")!.TextContent.Trim());
        Assert.Equal("#3", cinco.QuerySelector("td[data-campo='origenId']")!.TextContent.Trim());
        Assert.Equal("Aplicando", cinco.QuerySelector("td[data-campo='estado']")!.TextContent.Trim());
        Assert.Equal("—", cinco.QuerySelector("td[data-campo='terminadoEn']")!.TextContent.Trim());
        Assert.Equal("kyc-co 1.0.0", cinco.QuerySelector("td[data-campo='plugins']")!.TextContent.Trim());
        Assert.Equal("Fallido", filas[1].QuerySelector("td[data-campo='estado']")!.TextContent.Trim());
    }

    [Fact]
    public void HistorialDeUnPaisSinDespliegueAbierto()
    {
        Ok("/api/paises/co/despliegues", "detalle-historial-co.json");

        var cut = PintaPais("co");

        cut.WaitForAssertion(() => cut.Find("table#historial"));
        Assert.Equal([(HttpMethod.Get, "/api/paises/co/despliegues")], _m.Peticiones.ToArray());
        Assert.Empty(cut.FindAll("section#despliegue"));
        Assert.Empty(cut.FindAll("tr[data-actual]"));
    }

    [Fact]
    public void HistorialEscapaElPais()
    {
        _m.Responde(HttpMethod.Get, "/api/paises/a%2Fb%3Fc/despliegues", HttpStatusCode.OK, "[]");

        var cut = PintaPais("a/b?c");

        cut.WaitForAssertion(() => cut.Find("p[data-estado='sin-historial']"));
        Assert.Equal([(HttpMethod.Get, "/api/paises/a%2Fb%3Fc/despliegues")], _m.Peticiones.ToArray());
        Assert.Equal("Este país no tiene despliegues registrados.", cut.Find("p[data-estado='sin-historial']").TextContent.Trim());
    }

    [Fact]
    public void CadaRenglonEnlazaAlDetalleYAVolver()
    {
        Siete();

        var cut = PintaId(7);

        cut.WaitForAssertion(() => cut.Find("table#historial tr[data-id]"));
        foreach (var id in new[] { "5", "4", "7", "3" })
        {
            var fila = cut.Find($"table#historial tr[data-id='{id}']");
            Assert.Equal($"despliegues/{id}", fila.QuerySelector("td[data-campo='id'] a")!.GetAttribute("href"));
            var volver = fila.QuerySelector("td[data-campo='accion'] a[data-accion='volver']")!;
            Assert.Equal($"despliegues/{id}/volver", volver.GetAttribute("href"));
            Assert.Equal("Volver a este", volver.TextContent.Trim());
        }
    }

    [Fact]
    public void ActualizarVuelveALeer()
    {
        Siete();
        var cut = PintaId(7);
        cut.WaitForAssertion(() => cut.Find("table#historial"));
        Assert.Equal("Listo", Dd(cut, "estado"));
        _m.Responde(HttpMethod.Get, "/api/despliegues/7", HttpStatusCode.OK,
            ManejadorHttpSimulado.Fixture("detalle-despliegue-7.json").Replace("\"estado\": \"listo\"", "\"estado\": \"fallido\""));

        cut.Find("button[data-accion='actualizar']").Click();

        cut.WaitForAssertion(() => Assert.Equal("Fallido", Dd(cut, "estado")));
        cut.WaitForAssertion(() => Assert.Equal(4, _m.Peticiones.Count));
        Assert.Equal([(HttpMethod.Get, "/api/despliegues/7"), (HttpMethod.Get, "/api/paises/co/despliegues"),
            (HttpMethod.Get, "/api/despliegues/7"), (HttpMethod.Get, "/api/paises/co/despliegues")], _m.Peticiones.ToArray());
    }

    [Fact]
    public void DespliegueInexistenteMuestraElErrorYNoPideHistorial()
    {
        var cut = PintaId(99);

        cut.WaitForAssertion(() => Assert.Equal("NO_ENCONTRADO: Recurso no encontrado", cut.Find("[data-estado='error'] p[data-campo='error']").TextContent.Trim()));
        Assert.Equal([(HttpMethod.Get, "/api/despliegues/99")], _m.Peticiones.ToArray());
        Assert.Empty(cut.FindAll("table#historial"));
    }

    [Fact]
    public void ErrorDelHistorialDejaVerElDespliegue()
    {
        Ok("/api/despliegues/7", "detalle-despliegue-7.json");
        _m.Responde(HttpMethod.Get, "/api/paises/co/despliegues", HttpStatusCode.InternalServerError, ManejadorHttpSimulado.Fixture("error-500.json"));

        var cut = PintaId(7);

        cut.WaitForAssertion(() => cut.Find("[data-estado='error']"));
        Assert.NotNull(cut.Find("section#despliegue"));
        Assert.Equal("ERROR_INTERNO: Error interno del servidor", cut.Find("[data-estado='error'] p[data-campo='error']").TextContent.Trim());
        Assert.Empty(cut.FindAll("table#historial"));
    }

    [Fact]
    public void SoloHaceLecturas()
    {
        Siete();
        var cut = PintaId(7);
        cut.WaitForAssertion(() => cut.Find("table#historial"));

        cut.Find("button[data-accion='actualizar']").Click();

        cut.WaitForAssertion(() => Assert.Equal(4, _m.Peticiones.Count));
        Assert.All(_m.Peticiones, p => Assert.Equal(HttpMethod.Get, p.Metodo));
    }
}
