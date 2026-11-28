using System.Net;
using Bunit;
using Microsoft.Extensions.DependencyInjection;
using Synkra.Front.Api;
using Synkra.Front.Components.Pages;

namespace Synkra.Front.Tests;

public class ReversionDetalleTest : BunitContext
{
    private static readonly string A = new('a', 64);
    private static readonly string B = new('b', 64);
    private static readonly string C = new('c', 64);
    private static readonly string Imagen = $"localhost:5001/synkra-core-ejemplo@sha256:{A}";

    private readonly ManejadorHttpSimulado _m = new();

    public ReversionDetalleTest()
    {
        _m.Responde(HttpMethod.Get, "/api/despliegues/3", HttpStatusCode.OK, ManejadorHttpSimulado.Fixture("reversion-origen-3.json"));
    }

    private IRenderedComponent<Reversion> Pinta()
    {
        Services.AddSingleton(new ApiSynkra(new HttpClient(_m) { BaseAddress = new Uri("http://synkra.test/") }));
        var cut = Render<Reversion>(p => p.Add(x => x.Id, 3L));
        cut.WaitForAssertion(() => cut.Find("button[data-accion='preparar-vuelta']"));
        return cut;
    }

    private static string Texto(IRenderedComponent<Reversion> cut, string selector) => cut.Find(selector).TextContent.Trim();

    private static bool Deshabilitado(IRenderedComponent<Reversion> cut, string accion)
        => cut.Find($"button[data-accion='{accion}']").HasAttribute("disabled");

    private void RespondePlan()
        => _m.Responde(HttpMethod.Post, "/api/despliegues/3/reversion", HttpStatusCode.Created, ManejadorHttpSimulado.Fixture("reversion-plan.json"));

    private static void Prepara(IRenderedComponent<Reversion> cut)
    {
        cut.Find("button[data-accion='preparar-vuelta']").Click();
        cut.WaitForAssertion(() => cut.Find("section#plan"));
    }

    [Fact]
    public void AbrirLaPaginaSoloLee()
    {
        var cut = Pinta();

        Assert.Equal([(HttpMethod.Get, "/api/despliegues/3")], _m.Peticiones.ToArray());
        Assert.Equal("Volver al despliegue #3", Texto(cut, "h1"));
        Assert.Equal("co", Texto(cut, "#origen [data-campo='pais']"));
        Assert.Equal("synkra-core", Texto(cut, "#origen [data-campo='core']"));
        Assert.Equal("2.1.0", Texto(cut, "#origen [data-campo='versionCore']"));
        Assert.Equal(Imagen, Texto(cut, "#origen [data-campo='imagen']"));
        Assert.Equal(["kyc-co 1.0.0"], cut.FindAll("#origen [data-campo='plugins'] li").Select(l => l.TextContent.Trim()).ToArray());
        Assert.Equal("Listo", Texto(cut, "#origen [data-campo='estado']"));
        Assert.False(Deshabilitado(cut, "preparar-vuelta"));
    }

    [Fact]
    public void PrepararLaVueltaCreaElPlanDeReversion()
    {
        RespondePlan();
        var cut = Pinta();
        cut.Find("input[data-campo='usuario']").Change("ana");

        Prepara(cut);

        var i = _m.Peticiones.ToList().FindIndex(p => p.Metodo == HttpMethod.Post);
        Assert.Equal("/api/despliegues/3/reversion", _m.Peticiones[i].Ruta);
        Assert.Equal("ana", _m.Usuarios[i]);
        Assert.Equal("", _m.Cuerpos[i]);
        Assert.Equal("13", cut.Find("section#plan").GetAttribute("data-id"));
        Assert.Equal("Reversión", Texto(cut, "#plan [data-campo='tipo']"));
        Assert.Equal("#3", Texto(cut, "#plan [data-campo='origen']"));
        Assert.Equal("synkra-co", Texto(cut, "#plan [data-campo='namespace']"));
        Assert.Equal($"Core: {Imagen} → {Imagen}", Texto(cut, "#plan [data-campo='cambio-core']"));
        Assert.Equal([$"kyc-co 1.0.0 (sha256 {B})"], cut.FindAll("#plan [data-campo='anadidos'] li").Select(l => l.TextContent.Trim()).ToArray());
        Assert.Equal([$"kyc-pe 1.3.0 (sha256 {C})"], cut.FindAll("#plan [data-campo='quitados'] li").Select(l => l.TextContent.Trim()).ToArray());
        Assert.Equal("2026-11-28 11:15:00 UTC", Texto(cut, "#plan [data-campo='venceEn']"));
        Assert.False(Deshabilitado(cut, "confirmar"));
    }

    [Fact]
    public void LaVueltaSoloSeConfirmaDespuesDeConfirmar()
    {
        RespondePlan();
        _m.Responde(HttpMethod.Post, "/api/despliegues/13/confirmacion", HttpStatusCode.Accepted, ManejadorHttpSimulado.Fixture("reversion-confirmacion.json"));
        var cut = Pinta();
        Prepara(cut);

        Assert.DoesNotContain(_m.Peticiones, p => p.Ruta.Contains("/confirmacion"));
        Assert.Empty(cut.FindAll("p[data-estado='confirmado']"));
        cut.Find("input[data-campo='usuario']").Change("luis");

        cut.Find("button[data-accion='confirmar']").Click();

        cut.WaitForAssertion(() => cut.Find("p[data-estado='confirmado']"));
        Assert.Contains((HttpMethod.Post, "/api/despliegues/13/confirmacion"), _m.Peticiones.ToArray());
        var i = _m.Peticiones.ToList().FindIndex(p => p.Ruta == "/api/despliegues/13/confirmacion");
        Assert.Equal("luis", _m.Usuarios[i]);
        Assert.Equal("despliegues/13", cut.Find("p[data-estado='confirmado'] a").GetAttribute("href"));
        Assert.True(Deshabilitado(cut, "confirmar"));
    }

    [Fact]
    public void ConfirmarLaVueltaEstaDeshabilitadoSinPlan()
    {
        var cut = Pinta();

        Assert.True(Deshabilitado(cut, "confirmar"));
        Assert.Empty(cut.FindAll("section#plan"));
    }

    [Fact]
    public void CancelarNoLlamaANada()
    {
        var cut = Pinta();

        // Es un enlace sin manejador de clic: navegar no llama a la API. bUnit rechaza el clic si no hay manejador.
        var cancelar = cut.Find("a[data-accion='cancelar']");
        Assert.Throws<MissingEventHandlerException>(() => cancelar.Click());

        Assert.Equal("despliegues/3", cancelar.GetAttribute("href"));
        Assert.Equal([(HttpMethod.Get, "/api/despliegues/3")], _m.Peticiones.ToArray());
    }

    [Fact]
    public void OrigenNoListoMuestraElErrorDeLaApi()
    {
        _m.Responde(HttpMethod.Post, "/api/despliegues/3/reversion", HttpStatusCode.UnprocessableEntity, ManejadorHttpSimulado.Fixture("reversion-error-origen.json"));
        var cut = Pinta();

        cut.Find("button[data-accion='preparar-vuelta']").Click();

        cut.WaitForAssertion(() => cut.Find("#error-accion"));
        Assert.Equal("ORIGEN_NO_LISTO: Solo se puede revertir a un despliegue listo", Texto(cut, "#error-accion [data-campo='error']"));
        Assert.Empty(cut.FindAll("section#plan"));
        Assert.True(Deshabilitado(cut, "confirmar"));
    }

    [Fact]
    public void ConfirmarLaVueltaConErrorLoMuestra()
    {
        RespondePlan();
        _m.Responde(HttpMethod.Post, "/api/despliegues/13/confirmacion", HttpStatusCode.Conflict, ManejadorHttpSimulado.Fixture("reversion-error-en-curso.json"));
        var cut = Pinta();
        Prepara(cut);

        cut.Find("button[data-accion='confirmar']").Click();

        cut.WaitForAssertion(() => cut.Find("#error-accion"));
        Assert.Equal("DESPLIEGUE_EN_CURSO: Ya hay un despliegue en curso en el país", Texto(cut, "#error-accion [data-campo='error']"));
        Assert.Empty(cut.FindAll("p[data-estado='confirmado']"));
    }
}
