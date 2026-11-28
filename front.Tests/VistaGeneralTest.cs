using System.Net;
using Bunit;
using Microsoft.Extensions.DependencyInjection;
using Synkra.Front.Api;
using Synkra.Front.Components.Pages;

namespace Synkra.Front.Tests;

public class VistaGeneralTest : BunitContext
{
    private readonly ManejadorHttpSimulado _m = new();

    private IRenderedComponent<VistaGeneral> Pinta()
    {
        Services.AddSingleton(new ApiSynkra(new HttpClient(_m) { BaseAddress = new Uri("http://synkra.test/") }));
        return Render<VistaGeneral>();
    }

    private void Paises(string cuerpo) => _m.Responde(HttpMethod.Get, "/api/paises", HttpStatusCode.OK, cuerpo);

    private static string Celda(IRenderedComponent<VistaGeneral> cut, string pais, string campo)
        => cut.Find($"tr[data-pais='{pais}'] td[data-campo='{campo}']").TextContent.Trim();

    [Fact]
    public void MuestraUnRenglonPorPaisConLosDatosDeLaApi()
    {
        Paises(ManejadorHttpSimulado.Fixture("paises-dos.json"));

        var cut = Pinta();

        cut.WaitForAssertion(() =>
        {
            var filas = cut.FindAll("table#vista-general tr[data-pais]");
            Assert.Equal(["co", "pe"], filas.Select(f => f.GetAttribute("data-pais")!).ToArray());
        });
        Assert.Equal("co", Celda(cut, "co", "pais"));
        Assert.Equal("synkra-core", Celda(cut, "co", "core"));
        Assert.Equal("2.1.0", Celda(cut, "co", "versionCore"));
        Assert.Equal($"localhost:5001/synkra-core@sha256:{new string('a', 64)}", Celda(cut, "co", "imagen"));
        Assert.Equal("kyc-co 1.0.0", Celda(cut, "co", "plugins"));
        Assert.Equal("Leído", Celda(cut, "co", "lectura"));
        Assert.Equal("Sí", Celda(cut, "co", "concuerda"));
        Assert.Equal("#7", Celda(cut, "co", "ultimoDespliegueId"));
        var cabecera = cut.FindAll("table#vista-general th").Select(t => t.TextContent.Trim()).ToArray();
        Assert.Equal(["País", "Core", "Versión", "Imagen", "Plugins activos", "Clúster", "Concuerda", "Último despliegue"], cabecera);
    }

    [Fact]
    public void PaisSinLecturaMuestraNoDisponibleYNoSeSabe()
    {
        Paises(ManejadorHttpSimulado.Fixture("paises-dos.json"));

        var cut = Pinta();

        cut.WaitForAssertion(() => cut.Find("tr[data-pais='pe']"));
        Assert.Equal("—", Celda(cut, "pe", "imagen"));
        Assert.Equal("—", Celda(cut, "pe", "plugins"));
        Assert.Equal("No disponible", Celda(cut, "pe", "lectura"));
        Assert.Equal("No se sabe", Celda(cut, "pe", "concuerda"));
        Assert.Equal("#3", Celda(cut, "pe", "ultimoDespliegueId"));
    }

    [Fact]
    public void PaisSinDespliegueMuestraGuiones()
    {
        Paises(ManejadorHttpSimulado.Fixture("paises-sin-despliegue.json"));

        var cut = Pinta();

        cut.WaitForAssertion(() => cut.Find("tr[data-pais='mx']"));
        Assert.Equal("—", Celda(cut, "mx", "core"));
        Assert.Equal("—", Celda(cut, "mx", "versionCore"));
        Assert.Equal("—", Celda(cut, "mx", "imagen"));
        Assert.Equal("—", Celda(cut, "mx", "plugins"));
        Assert.Equal("—", Celda(cut, "mx", "ultimoDespliegueId"));
        Assert.Equal("Vacío", Celda(cut, "mx", "lectura"));
        Assert.Equal("Sí", Celda(cut, "mx", "concuerda"));
    }

    [Fact]
    public void SinPaisesMuestraAviso()
    {
        Paises("[]");

        var cut = Pinta();

        cut.WaitForAssertion(() => Assert.Equal("No hay países configurados en la API.", cut.Find("p[data-estado='vacio']").TextContent.Trim()));
        Assert.Empty(cut.FindAll("table"));
    }

    [Fact]
    public void ErrorDeLaApiMuestraCodigoYMensaje()
    {
        _m.Responde(HttpMethod.Get, "/api/paises", HttpStatusCode.InternalServerError, ManejadorHttpSimulado.Fixture("error-500.json"));

        var cut = Pinta();

        cut.WaitForAssertion(() => Assert.Equal("ERROR_INTERNO: Error interno del servidor", cut.Find("div[data-estado='error'] p[data-campo='error']").TextContent.Trim()));
        Assert.Empty(cut.FindAll("table"));
    }

    [Fact]
    public void ApiNoDisponibleMuestraElCodigoDelFront()
    {
        _m.Lanza(HttpMethod.Get, "/api/paises", new HttpRequestException("rechazada"));

        var cut = Pinta();

        cut.WaitForAssertion(() => Assert.Equal("API_NO_DISPONIBLE: No se pudo conectar con la API de SynKra", cut.Find("p[data-campo='error']").TextContent.Trim()));
    }

    [Fact]
    public void SoloLlamaAGetApiPaises()
    {
        Paises(ManejadorHttpSimulado.Fixture("paises-dos.json"));

        var cut = Pinta();

        cut.WaitForAssertion(() => cut.Find("table#vista-general"));
        Assert.Equal([(HttpMethod.Get, "/api/paises")], _m.Peticiones);
    }

    [Fact]
    public void PaisQueNoConcuerdaMuestraNo()
    {
        Paises(ManejadorHttpSimulado.Fixture("paises-no-concuerda.json"));

        var cut = Pinta();

        cut.WaitForAssertion(() => cut.Find("tr[data-pais='cl']"));
        Assert.Equal("No", Celda(cut, "cl", "concuerda"));
        Assert.Equal("Leído", Celda(cut, "cl", "lectura"));
    }

    [Fact]
    public void PaisInconsistenteMuestraInconsistente()
    {
        Paises(ManejadorHttpSimulado.Fixture("paises-no-concuerda.json"));

        var cut = Pinta();

        cut.WaitForAssertion(() => cut.Find("tr[data-pais='uy']"));
        Assert.Equal("Inconsistente", Celda(cut, "uy", "lectura"));
        Assert.Equal("—", Celda(cut, "uy", "imagen"));
        Assert.Equal("—", Celda(cut, "uy", "plugins"));
    }

    [Fact]
    public void ErrorConDetallesMuestraCampoYMotivo()
    {
        _m.Responde(HttpMethod.Get, "/api/paises", HttpStatusCode.UnprocessableEntity, ManejadorHttpSimulado.Fixture("error-422-pais.json"));

        var cut = Pinta();

        cut.WaitForAssertion(() => cut.Find("div[data-estado='error']"));
        Assert.Equal("PAIS_INVALIDO: El país no es válido", cut.Find("p[data-campo='error']").TextContent.Trim());
        var items = cut.FindAll("div[data-estado='error'] li").Select(l => l.TextContent.Trim()).ToArray();
        Assert.Equal(["pais: El país debe ser un código de dos letras en minúscula (S4)"], items);
        Assert.Empty(cut.FindAll("table"));
    }
}
