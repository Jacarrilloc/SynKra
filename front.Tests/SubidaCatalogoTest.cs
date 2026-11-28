using System.Net;
using Bunit;
using Microsoft.AspNetCore.Components.Forms;
using Microsoft.Extensions.DependencyInjection;
using Synkra.Front.Api;
using Synkra.Front.Components.Pages;

namespace Synkra.Front.Tests;

public class SubidaCatalogoTest : BunitContext
{
    private const string Ruta = "/api/cores/synkra-core/plugins";

    private readonly ManejadorHttpSimulado _m = new();

    private IRenderedComponent<SubirPlugin> Pinta()
    {
        Services.AddSingleton(new ApiSynkra(new HttpClient(_m) { BaseAddress = new Uri("http://synkra.test/") }));
        return Render<SubirPlugin>(p => p.Add(x => x.Core, "synkra-core"));
    }

    private static void ElegirArchivo(IRenderedComponent<SubirPlugin> cut)
        => cut.FindComponent<InputFile>().UploadFiles(InputFileContent.CreateFromText("contenido-del-jar", "kyc-co.jar"));

    private static string Texto(IRenderedComponent<SubirPlugin> cut, string selector) => cut.Find(selector).TextContent.Trim();

    private static bool Deshabilitado(IRenderedComponent<SubirPlugin> cut) => cut.Find("button[data-accion='subir']").HasAttribute("disabled");

    private void RespondeSubida(HttpStatusCode estado, string fixture)
        => _m.Responde(HttpMethod.Post, Ruta, estado, ManejadorHttpSimulado.Fixture(fixture));

    [Fact]
    public void SubirMandaElJarYMuestraElPlugin()
    {
        RespondeSubida(HttpStatusCode.Created, "subida-plugin.json");
        var cut = Pinta();
        Assert.Equal("Subir un plugin a synkra-core", Texto(cut, "h1"));
        ElegirArchivo(cut);

        cut.Find("button[data-accion='subir']").Click();

        cut.WaitForAssertion(() => cut.Find("section#subido"));
        Assert.Equal([(HttpMethod.Post, Ruta)], _m.Peticiones.ToArray());
        var partes = _m.Partes[0];
        Assert.Equal(new ManejadorHttpSimulado.ParteMultipart("archivo", "kyc-co.jar", "application/java-archive", "contenido-del-jar"), Assert.Single(partes));
        Assert.Equal("synkra-core", Texto(cut, "#subido [data-campo='core']"));
        Assert.Equal("kyc-co", Texto(cut, "#subido [data-campo='nombre']"));
        Assert.Equal("1.0.0", Texto(cut, "#subido [data-campo='version']"));
        Assert.Equal("2.x", Texto(cut, "#subido [data-campo='core-requerido']"));
        Assert.Equal(["cliente=demo", "pais=co"], cut.FindAll("#subido [data-campo='etiquetas'] li").Select(l => l.TextContent.Trim()).ToArray());
        Assert.Equal(new string('b', 64), Texto(cut, "#subido [data-campo='sha256']"));
        Assert.Equal("2048", Texto(cut, "#subido [data-campo='tamano']"));
        Assert.Equal("catalogo", cut.Find("#subido a").GetAttribute("href"));
    }

    [Fact]
    public void DescriptorInvalidoMuestraElMotivo()
    {
        RespondeSubida(HttpStatusCode.UnprocessableEntity, "subida-error-descriptor.json");
        var cut = Pinta();
        ElegirArchivo(cut);

        cut.Find("button[data-accion='subir']").Click();

        cut.WaitForAssertion(() => cut.Find("[data-estado='error']"));
        Assert.Equal("ENTIDAD_NO_PROCESABLE: La solicitud no se puede procesar", Texto(cut, "[data-estado='error'] [data-campo='error']"));
        Assert.Equal(
            ["version: DESCRIPTOR_FALTA_CAMPO: Falta el campo obligatorio 'version'"],
            cut.FindAll("[data-estado='error'] li").Select(l => l.TextContent.Trim()).ToArray());
        Assert.Empty(cut.FindAll("section#subido"));
    }

    [Fact]
    public void EtiquetasSeMandanTalCual()
    {
        RespondeSubida(HttpStatusCode.Created, "subida-plugin.json");
        var cut = Pinta();
        ElegirArchivo(cut);
        const string etiquetas = "{ \"pais\" :  \"co\",\n \"cliente\":\"demo\" }";
        cut.Find("textarea[data-campo='etiquetas']").Change(etiquetas);

        cut.Find("button[data-accion='subir']").Click();

        cut.WaitForAssertion(() => cut.Find("section#subido"));
        var partes = _m.Partes[0];
        Assert.Equal(2, partes.Count);
        Assert.Equal(new ManejadorHttpSimulado.ParteMultipart("etiquetas", null, "application/json", etiquetas), partes[1]);
    }

    [Fact]
    public void SinEtiquetasNoSeMandaLaParte()
    {
        RespondeSubida(HttpStatusCode.Created, "subida-plugin.json");
        var cut = Pinta();
        ElegirArchivo(cut);

        cut.Find("button[data-accion='subir']").Click();

        cut.WaitForAssertion(() => cut.Find("section#subido"));
        Assert.Equal("archivo", Assert.Single(_m.Partes[0]).Nombre);
    }

    [Fact]
    public void SinArchivoElBotonEstaDeshabilitado()
    {
        var cut = Pinta();

        Assert.True(Deshabilitado(cut));

        ElegirArchivo(cut);

        Assert.False(Deshabilitado(cut));
        Assert.Empty(_m.Peticiones);
    }

    [Fact]
    public void ConflictoMuestraElCodigoDeLaApi()
    {
        RespondeSubida(HttpStatusCode.Conflict, "subida-error-conflicto.json");
        var cut = Pinta();
        ElegirArchivo(cut);

        cut.Find("button[data-accion='subir']").Click();

        cut.WaitForAssertion(() => cut.Find("[data-estado='error']"));
        Assert.Equal("CONFLICTO: La solicitud entra en conflicto con el estado actual", Texto(cut, "[data-estado='error'] [data-campo='error']"));
        Assert.Empty(cut.FindAll("section#subido"));
    }

    [Fact]
    public void ArchivoPorEncimaDelTopeMuestraUnErrorYNoLlamaALaApi()
    {
        var cut = Pinta();
        cut.FindComponent<InputFile>().UploadFiles(InputFileContent.CreateFromBinary(new byte[26 * 1024 * 1024], "grande.jar"));

        cut.Find("button[data-accion='subir']").Click();

        cut.WaitForAssertion(() => cut.Find("#error-archivo [data-campo='error']"));
        Assert.StartsWith("No se pudo leer el archivo:", Texto(cut, "#error-archivo [data-campo='error']"));
        Assert.Empty(_m.Peticiones);
        Assert.Empty(cut.FindAll("section#subido"));
        Assert.False(Deshabilitado(cut));
    }
}
