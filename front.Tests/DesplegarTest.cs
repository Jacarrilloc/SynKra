using System.Net;
using AngleSharp.Dom;
using Bunit;
using Microsoft.Extensions.DependencyInjection;
using Synkra.Front.Api;
using Synkra.Front.Components.Pages;

namespace Synkra.Front.Tests;

public class DesplegarTest : BunitContext
{
    private static readonly string A = new('a', 64);
    private static readonly string C = new('c', 64);
    private static readonly string Imagen = $"localhost:5001/synkra-core-ejemplo@sha256:{A}";

    private readonly ManejadorHttpSimulado _m = new();

    public DesplegarTest()
    {
        _m.Responde(HttpMethod.Get, "/api/paises", HttpStatusCode.OK, ManejadorHttpSimulado.Fixture("desplegar-paises.json"))
          .Responde(HttpMethod.Get, "/api/cores", HttpStatusCode.OK, ManejadorHttpSimulado.Fixture("desplegar-cores.json"))
          .Responde(HttpMethod.Get, "/api/cores/synkra-core/plugins", HttpStatusCode.OK, ManejadorHttpSimulado.Fixture("desplegar-plugins.json"))
          .Responde(HttpMethod.Get, "/api/cores/synkra-core/plugins?pais=pe", HttpStatusCode.OK, ManejadorHttpSimulado.Fixture("desplegar-plugins-pe.json"));
    }

    private static string Cuerpo(string version, params (string Nombre, string Version)[] plugins)
        => "{\"core\":\"synkra-core\",\"versionCore\":\"" + version + "\",\"plugins\":["
           + string.Join(",", plugins.Select(p => "{\"nombre\":\"" + p.Nombre + "\",\"version\":\"" + p.Version + "\"}")) + "]}";

    private static string CuerpoPlan(string pais, string version, params (string Nombre, string Version)[] plugins)
        => "{\"pais\":\"" + pais + "\",\"core\":\"synkra-core\",\"versionCore\":\"" + version + "\",\"plugins\":["
           + string.Join(",", plugins.Select(p => "{\"nombre\":\"" + p.Nombre + "\",\"version\":\"" + p.Version + "\"}")) + "]}";

    private void Verificacion(string cuerpo, string fixture, HttpStatusCode estado = HttpStatusCode.OK)
        => _m.RespondeSegunCuerpo(HttpMethod.Post, "/api/verificaciones", cuerpo, estado, ManejadorHttpSimulado.Fixture(fixture));

    private IRenderedComponent<Desplegar> Pinta()
    {
        Services.AddSingleton(new ApiSynkra(new HttpClient(_m) { BaseAddress = new Uri("http://synkra.test/") }));
        var cut = Render<Desplegar>();
        cut.WaitForAssertion(() => Assert.True(cut.FindAll("select[data-campo='core'] option").Count > 1));
        return cut;
    }

    private static void Pais(IRenderedComponent<Desplegar> cut, string valor) => cut.Find("select[data-campo='pais']").Change(valor);

    private static void Core(IRenderedComponent<Desplegar> cut, string valor)
    {
        cut.Find("select[data-campo='core']").Change(valor);
        cut.WaitForAssertion(() => Assert.NotEmpty(cut.FindAll("input[type='checkbox'][data-plugin]")));
    }

    private static void Version(IRenderedComponent<Desplegar> cut, string valor) => cut.Find("select[data-campo='versionCore']").Change(valor);

    private static void Marca(IRenderedComponent<Desplegar> cut, string nombre, string version, bool marcado = true)
        => cut.Find($"input[type='checkbox'][data-plugin='{nombre}'][data-version='{version}']").Change(marcado);

    private static string[] Opciones(IRenderedComponent<Desplegar> cut, string campo)
        => cut.FindAll($"select[data-campo='{campo}'] option").Select(o => o.GetAttribute("value")!).ToArray();

    private static bool Deshabilitado(IRenderedComponent<Desplegar> cut, string accion)
        => cut.Find($"button[data-accion='{accion}']").HasAttribute("disabled");

    private int Verificaciones()
        => _m.Peticiones.Count(p => p.Metodo == HttpMethod.Post && p.Ruta == "/api/verificaciones");

    private string[] CuerposDeVerificacion()
        => _m.Peticiones.Select((p, i) => (p, i)).Where(x => x.p.Metodo == HttpMethod.Post && x.p.Ruta == "/api/verificaciones")
            .Select(x => _m.Cuerpos[x.i]).ToArray();

    private static string Texto(IRenderedComponent<Desplegar> cut, string selector) => cut.Find(selector).TextContent.Trim();

    // Deja la pantalla con país pe, core 2.1.0 y kyc-pe 1.3.0 marcado: veredicto aprobado.
    private IRenderedComponent<Desplegar> Aprobada()
    {
        Verificacion(Cuerpo("2.1.0"), "desplegar-veredicto-vacio.json");
        Verificacion(Cuerpo("2.1.0", ("kyc-pe", "1.3.0")), "desplegar-veredicto-aprobado.json");
        var cut = Pinta();
        Pais(cut, "pe");
        Core(cut, "synkra-core");
        Version(cut, "2.1.0");
        Marca(cut, "kyc-pe", "1.3.0");
        cut.WaitForAssertion(() => Assert.Equal("kyc-pe", cut.Find("#veredicto tr[data-plugin]").GetAttribute("data-plugin")));
        return cut;
    }

    private IRenderedComponent<Desplegar> ConPlan()
    {
        _m.Responde(HttpMethod.Post, "/api/despliegues", HttpStatusCode.Created, ManejadorHttpSimulado.Fixture("desplegar-plan-pe.json"));
        _m.Responde(HttpMethod.Post, "/api/despliegues/12/confirmacion", HttpStatusCode.Accepted, ManejadorHttpSimulado.Fixture("desplegar-confirmacion.json"));
        var cut = Aprobada();
        cut.Find("button[data-accion='preparar']").Click();
        cut.WaitForAssertion(() => cut.Find("section#plan"));
        return cut;
    }

    [Fact]
    public void CargaPaisesYCoresDeLaApi()
    {
        var cut = Pinta();

        Assert.Equal(["", "co", "pe"], Opciones(cut, "pais"));
        Assert.Equal(["", "synkra-core"], Opciones(cut, "core"));
        Assert.Equal([""], Opciones(cut, "versionCore"));
        Assert.Equal([(HttpMethod.Get, "/api/paises"), (HttpMethod.Get, "/api/cores")], _m.Peticiones.ToArray());

        Core(cut, "synkra-core");

        Assert.Equal(["", "2.1.0", "3.0.0"], Opciones(cut, "versionCore"));
    }

    [Fact]
    public void ElegirCoreYVersionVerificaSinPlugins()
    {
        Verificacion(Cuerpo("2.1.0"), "desplegar-veredicto-vacio.json");
        var cut = Pinta();
        Core(cut, "synkra-core");
        Assert.Equal(0, Verificaciones());

        Version(cut, "2.1.0");

        cut.WaitForAssertion(() => Assert.Equal("APROBADA", cut.Find("section#veredicto").GetAttribute("data-global")));
        Assert.Equal([Cuerpo("2.1.0")], CuerposDeVerificacion());
        Assert.Equal("{\"core\":\"synkra-core\",\"versionCore\":\"2.1.0\",\"plugins\":[]}", CuerposDeVerificacion()[0]);
    }

    [Fact]
    public void MarcarUnPluginVuelveAVerificarConLaSeleccion()
    {
        Verificacion(Cuerpo("2.1.0"), "desplegar-veredicto-vacio.json");
        Verificacion(Cuerpo("2.1.0", ("kyc-pe", "1.3.0")), "desplegar-veredicto-aprobado.json");
        var cut = Pinta();
        Core(cut, "synkra-core");
        Version(cut, "2.1.0");
        cut.WaitForAssertion(() => Assert.Equal(1, Verificaciones()));

        Marca(cut, "kyc-pe", "1.3.0");

        cut.WaitForAssertion(() => Assert.Equal("kyc-pe", cut.Find("#veredicto tr[data-plugin]").GetAttribute("data-plugin")));
        Assert.Equal([Cuerpo("2.1.0"), Cuerpo("2.1.0", ("kyc-pe", "1.3.0"))], CuerposDeVerificacion());
        Assert.Equal("1.3.0", cut.Find("#veredicto tr[data-plugin='kyc-pe']").GetAttribute("data-version"));
        Assert.Equal("Sí", Texto(cut, "#veredicto tr[data-plugin='kyc-pe'] td[data-campo='aceptado']"));
    }

    [Fact]
    public void ElPaisNoEntraEnLaVerificacion()
    {
        Verificacion(Cuerpo("2.1.0"), "desplegar-veredicto-vacio.json");
        Verificacion(Cuerpo("2.1.0", ("kyc-pe", "1.3.0")), "desplegar-veredicto-aprobado.json");
        var cut = Pinta();
        Core(cut, "synkra-core");
        Version(cut, "2.1.0");
        cut.WaitForAssertion(() => Assert.Equal(1, Verificaciones()));

        Pais(cut, "pe");
        Marca(cut, "kyc-pe", "1.3.0");

        cut.WaitForAssertion(() => Assert.Equal("kyc-pe", cut.Find("#veredicto tr[data-plugin]").GetAttribute("data-plugin")));
        Assert.Equal(2, Verificaciones());
        Assert.All(CuerposDeVerificacion(), c => Assert.DoesNotContain("pais", c));
    }

    [Fact]
    public void ConVeredictoRechazadoLosBotonesEstanDeshabilitados()
    {
        Verificacion(Cuerpo("2.1.0", ("kyc-pe", "2.0.0")), "desplegar-veredicto-rechazado.json");
        var cut = Pinta();
        Pais(cut, "pe");
        Core(cut, "synkra-core");
        Version(cut, "2.1.0");

        Marca(cut, "kyc-pe", "2.0.0");

        cut.WaitForAssertion(() => Assert.Equal("RECHAZADA", cut.Find("section#veredicto").GetAttribute("data-global")));
        Assert.True(Deshabilitado(cut, "preparar"));
        Assert.True(Deshabilitado(cut, "confirmar"));
        Assert.Equal("No", Texto(cut, "#veredicto tr[data-plugin='kyc-pe'] td[data-campo='aceptado']"));
        Assert.Equal(
            ["VERSION_INCOMPATIBLE: El plugin 'kyc-pe' 2.0.0 requiere el core 'synkra-core' en 3.x y se pidió 2.1.0"],
            cut.FindAll("#veredicto li[data-campo='motivo']").Select(l => l.TextContent.Trim()).ToArray());
        Assert.Equal(
            ["PLUGIN kyc-pe 1.3.0", "CORE synkra-core 3.0.0"],
            cut.FindAll("#veredicto li[data-campo='alternativa']").Select(l => l.TextContent.Trim()).ToArray());
    }

    [Fact]
    public void ConVeredictoAprobadoPrepararSeHabilitaYConfirmarNo()
    {
        var cut = Aprobada();

        Assert.Equal("APROBADA", cut.Find("section#veredicto").GetAttribute("data-global"));
        Assert.False(Deshabilitado(cut, "preparar"));
        Assert.True(Deshabilitado(cut, "confirmar"));
    }

    [Fact]
    public void SinPaisPrepararSigueDeshabilitado()
    {
        Verificacion(Cuerpo("2.1.0"), "desplegar-veredicto-vacio.json");
        var cut = Pinta();
        Core(cut, "synkra-core");
        Version(cut, "2.1.0");
        cut.WaitForAssertion(() => Assert.Equal("APROBADA", cut.Find("section#veredicto").GetAttribute("data-global")));

        Assert.True(Deshabilitado(cut, "preparar"));

        Pais(cut, "pe");

        Assert.False(Deshabilitado(cut, "preparar"));
    }

    [Fact]
    public void PrepararCreaElPlanYMuestraResumenYCambios()
    {
        _m.Responde(HttpMethod.Post, "/api/despliegues", HttpStatusCode.Created, ManejadorHttpSimulado.Fixture("desplegar-plan-pe.json"));
        var cut = Aprobada();
        cut.Find("input[data-campo='usuario']").Change("ana");

        cut.Find("button[data-accion='preparar']").Click();

        cut.WaitForAssertion(() => Assert.Equal("12", cut.Find("section#plan").GetAttribute("data-id")));
        var i = _m.Peticiones.ToList().FindIndex(p => p.Metodo == HttpMethod.Post && p.Ruta == "/api/despliegues");
        Assert.Equal(CuerpoPlan("pe", "2.1.0", ("kyc-pe", "1.3.0")), _m.Cuerpos[i]);
        Assert.Equal("ana", _m.Usuarios[i]);
        Assert.Equal("synkra-pe", Texto(cut, "#plan [data-campo='namespace']"));
        Assert.Equal(Imagen, Texto(cut, "#plan [data-campo='imagen']"));
        Assert.Equal([$"kyc-pe 1.3.0 (sha256 {C})"], cut.FindAll("#plan [data-campo='plugins'] li").Select(l => l.TextContent.Trim()).ToArray());
        Assert.Equal($"Core: — → {Imagen}", Texto(cut, "#plan [data-campo='cambio-core']"));
        Assert.Equal([$"kyc-pe 1.3.0 (sha256 {C})"], cut.FindAll("#plan [data-campo='anadidos'] li").Select(l => l.TextContent.Trim()).ToArray());
        Assert.Empty(cut.FindAll("#plan [data-campo='quitados'] li"));
        Assert.Empty(cut.FindAll("#plan [data-campo='iguales'] li"));
        Assert.Empty(cut.FindAll("#plan [data-campo='sin-cambios']"));
        Assert.Equal("ana", Texto(cut, "#plan [data-campo='creadoPor']"));
        Assert.Equal("2026-11-28 10:15:00 UTC", Texto(cut, "#plan [data-campo='venceEn']"));
        Assert.False(Deshabilitado(cut, "confirmar"));
    }

    [Fact]
    public void ConfirmarLlamaALaConfirmacionDelPlanYEnlazaAlDetalle()
    {
        var cut = ConPlan();
        Assert.DoesNotContain(_m.Peticiones, p => p.Ruta.EndsWith("/confirmacion"));
        cut.Find("input[data-campo='usuario']").Change("luis");

        cut.Find("button[data-accion='confirmar']").Click();

        cut.WaitForAssertion(() => cut.Find("p[data-estado='confirmado']"));
        Assert.Contains((HttpMethod.Post, "/api/despliegues/12/confirmacion"), _m.Peticiones.ToArray());
        var i = _m.Peticiones.ToList().FindIndex(p => p.Ruta == "/api/despliegues/12/confirmacion");
        Assert.Equal("luis", _m.Usuarios[i]);
        var enlace = cut.Find("p[data-estado='confirmado'] a");
        Assert.Equal("despliegues/12", enlace.GetAttribute("href"));
        Assert.Equal("Ver el despliegue #12", enlace.TextContent.Trim());
        Assert.True(Deshabilitado(cut, "confirmar"));
        Assert.True(Deshabilitado(cut, "preparar"));
    }

    [Fact]
    public void SinClicNoHayConfirmacion()
    {
        var cut = ConPlan();

        Assert.Equal("12", cut.Find("section#plan").GetAttribute("data-id"));
        Assert.DoesNotContain(_m.Peticiones, p => p.Ruta.Contains("/confirmacion"));
        Assert.Empty(cut.FindAll("p[data-estado='confirmado']"));
    }

    [Fact]
    public void CambiarLaSeleccionDescartaElPlan()
    {
        Verificacion(Cuerpo("2.1.0"), "desplegar-veredicto-vacio.json");
        var cut = ConPlan();
        var antes = Verificaciones();

        Marca(cut, "kyc-pe", "1.3.0", false);

        cut.WaitForAssertion(() => Assert.Equal(antes + 1, Verificaciones()));
        Assert.Empty(cut.FindAll("section#plan"));
        Assert.True(Deshabilitado(cut, "confirmar"));
        Assert.Equal(Cuerpo("2.1.0"), CuerposDeVerificacion()[^1]);
    }

    [Fact]
    public void ErrorAlPrepararSeMuestraYNoHayPlan()
    {
        _m.Responde(HttpMethod.Post, "/api/despliegues", HttpStatusCode.UnprocessableEntity, ManejadorHttpSimulado.Fixture("desplegar-error-combinacion.json"));
        var cut = Aprobada();

        cut.Find("button[data-accion='preparar']").Click();

        cut.WaitForAssertion(() => cut.Find("#error-accion"));
        Assert.Equal("COMBINACION_RECHAZADA: La combinación no es compatible", Texto(cut, "#error-accion [data-campo='error']"));
        Assert.StartsWith("plugins.kyc-pe: VERSION_INCOMPATIBLE: ", Texto(cut, "#error-accion li"));
        Assert.Empty(cut.FindAll("section#plan"));
        Assert.True(Deshabilitado(cut, "confirmar"));
    }

    [Fact]
    public void ErrorAlConfirmarSeMuestraYDescartaElPlan()
    {
        var cut = ConPlan();
        _m.Responde(HttpMethod.Post, "/api/despliegues/12/confirmacion", HttpStatusCode.Gone, ManejadorHttpSimulado.Fixture("desplegar-error-vencido.json"));

        cut.Find("button[data-accion='confirmar']").Click();

        cut.WaitForAssertion(() => cut.Find("#error-accion"));
        Assert.Equal("PLAN_VENCIDO: El plan venció sin confirmarse", Texto(cut, "#error-accion [data-campo='error']"));
        Assert.Empty(cut.FindAll("section#plan"));
        Assert.Empty(cut.FindAll("p[data-estado='confirmado']"));
        Assert.True(Deshabilitado(cut, "confirmar"));
    }

    [Fact]
    public void VerificacionConErrorDeshabilitaLosBotones()
    {
        Verificacion(Cuerpo("2.1.0"), "desplegar-error-seleccion.json", HttpStatusCode.UnprocessableEntity);
        var cut = Pinta();
        Pais(cut, "pe");
        Core(cut, "synkra-core");

        Version(cut, "2.1.0");

        cut.WaitForAssertion(() => cut.Find("#veredicto [data-estado='error']"));
        Assert.Equal("SELECCION_INVALIDA: La selección no es válida", Texto(cut, "#veredicto [data-campo='error']"));
        Assert.Equal("core: El core debe cumplir el patrón de nombres (minúsculas, dígitos y guiones)", Texto(cut, "#veredicto li"));
        Assert.Null(cut.Find("section#veredicto").GetAttribute("data-global"));
        Assert.True(Deshabilitado(cut, "preparar"));
        Assert.True(Deshabilitado(cut, "confirmar"));
    }

    [Fact]
    public void FiltroPorEtiquetaPideLaConsultaALaApi()
    {
        var cut = Pinta();
        Core(cut, "synkra-core");
        Assert.Equal(3, cut.FindAll("input[type='checkbox'][data-plugin]").Count);

        cut.Find("input[data-campo='filtro']").Change("pais=pe");
        cut.Find("button[data-accion='filtrar']").Click();

        cut.WaitForAssertion(() => Assert.Equal(2, cut.FindAll("input[type='checkbox'][data-plugin]").Count));
        Assert.Contains((HttpMethod.Get, "/api/cores/synkra-core/plugins?pais=pe"), _m.Peticiones.ToArray());
        Assert.Equal(
            [("kyc-pe", "1.3.0"), ("kyc-pe", "2.0.0")],
            cut.FindAll("input[type='checkbox'][data-plugin]").Select(c => (c.GetAttribute("data-plugin")!, c.GetAttribute("data-version")!)).ToArray());
    }

    [Fact]
    public void NoFiltraNiComparaVersiones()
    {
        Verificacion(Cuerpo("2.1.0"), "desplegar-veredicto-vacio.json");
        Verificacion(Cuerpo("2.1.0", ("kyc-pe", "1.3.0"), ("kyc-pe", "2.0.0")), "desplegar-veredicto-rechazado.json");
        var cut = Pinta();
        Core(cut, "synkra-core");

        Version(cut, "2.1.0");

        cut.WaitForAssertion(() => Assert.Equal(1, Verificaciones()));
        Assert.Equal(
            [("kyc-co", "1.0.0"), ("kyc-pe", "1.3.0"), ("kyc-pe", "2.0.0")],
            cut.FindAll("input[type='checkbox'][data-plugin]").Select(c => (c.GetAttribute("data-plugin")!, c.GetAttribute("data-version")!)).ToArray());
        Assert.Contains("3.x", cut.Find("li[data-plugin='kyc-pe'][data-version='2.0.0']").TextContent);
        Assert.Equal(["", "2.1.0", "3.0.0"], Opciones(cut, "versionCore"));

        Marca(cut, "kyc-pe", "2.0.0");
        Marca(cut, "kyc-pe", "1.3.0");

        cut.WaitForAssertion(() => Assert.Equal("RECHAZADA", cut.Find("section#veredicto").GetAttribute("data-global")));
        Assert.Equal(Cuerpo("2.1.0", ("kyc-pe", "1.3.0"), ("kyc-pe", "2.0.0")), CuerposDeVerificacion()[^1]);
    }

    [Fact]
    public async Task RespuestaViejaNoPisaLaNueva()
    {
        var primera = _m.Retiene(HttpMethod.Post, "/api/verificaciones", Cuerpo("2.1.0"), HttpStatusCode.OK,
            ManejadorHttpSimulado.Fixture("desplegar-veredicto-rechazado.json"));
        Verificacion(Cuerpo("2.1.0", ("kyc-pe", "1.3.0")), "desplegar-veredicto-aprobado.json");
        var cut = Pinta();
        Core(cut, "synkra-core");
        Version(cut, "2.1.0");
        cut.WaitForAssertion(() => Assert.True(primera.Llego));

        Marca(cut, "kyc-pe", "1.3.0");
        cut.WaitForAssertion(() => Assert.Equal("APROBADA", cut.Find("section#veredicto").GetAttribute("data-global")));

        primera.Suelta();
        await Task.Delay(300);

        Assert.Equal("APROBADA", cut.Find("section#veredicto").GetAttribute("data-global"));
        Assert.Equal("kyc-pe", cut.Find("#veredicto tr[data-plugin]").GetAttribute("data-plugin"));
        Assert.Empty(cut.FindAll("#veredicto li[data-campo='motivo']"));
    }

    [Fact]
    public void CambiarElPaisDescartaElPlan()
    {
        var cut = ConPlan();

        Pais(cut, "co");

        Assert.Empty(cut.FindAll("section#plan"));
        Assert.True(Deshabilitado(cut, "confirmar"));
        Assert.DoesNotContain(_m.Peticiones, p => p.Ruta.Contains("/confirmacion"));
    }

    [Fact]
    public void UnFiltroQueOcultaUnPluginMarcadoLoDesmarcaYVuelveAVerificar()
    {
        Verificacion(Cuerpo("2.1.0"), "desplegar-veredicto-vacio.json");
        Verificacion(Cuerpo("2.1.0", ("kyc-co", "1.0.0")), "desplegar-veredicto-aprobado.json");
        var cut = Pinta();
        Core(cut, "synkra-core");
        Version(cut, "2.1.0");
        Marca(cut, "kyc-co", "1.0.0");
        cut.WaitForAssertion(() => Assert.Equal(2, Verificaciones()));

        cut.Find("input[data-campo='filtro']").Change("pais=pe");
        cut.Find("button[data-accion='filtrar']").Click();

        cut.WaitForAssertion(() => Assert.Equal(3, Verificaciones()));
        Assert.Equal(Cuerpo("2.1.0"), CuerposDeVerificacion()[^1]);
        Assert.Empty(cut.FindAll("input[data-plugin='kyc-co']"));
        Assert.DoesNotContain(cut.FindAll("input[type='checkbox'][data-plugin]"), c => c.HasAttribute("checked"));
    }

    [Fact]
    public void UnFiltroSinIgualMandaLaClaveConValorVacio()
    {
        _m.Responde(HttpMethod.Get, "/api/cores/synkra-core/plugins?pais=", HttpStatusCode.OK, ManejadorHttpSimulado.Fixture("desplegar-plugins-pe.json"));
        var cut = Pinta();
        Core(cut, "synkra-core");

        cut.Find("input[data-campo='filtro']").Change("pais");
        cut.Find("button[data-accion='filtrar']").Click();

        cut.WaitForAssertion(() => Assert.Equal(2, cut.FindAll("input[type='checkbox'][data-plugin]").Count));
        Assert.Contains((HttpMethod.Get, "/api/cores/synkra-core/plugins?pais="), _m.Peticiones.ToArray());
    }

    [Fact]
    public async Task UnPlanQueLlegaTrasCambiarLaSeleccionNoSeMuestra()
    {
        var plan = _m.Retiene(HttpMethod.Post, "/api/despliegues", CuerpoPlan("pe", "2.1.0", ("kyc-pe", "1.3.0")), HttpStatusCode.Created,
            ManejadorHttpSimulado.Fixture("desplegar-plan-pe.json"));
        Verificacion(Cuerpo("2.1.0"), "desplegar-veredicto-vacio.json");
        var cut = Aprobada();
        cut.Find("button[data-accion='preparar']").Click();
        cut.WaitForAssertion(() => Assert.True(plan.Llego));

        Marca(cut, "kyc-pe", "1.3.0", false);
        plan.Suelta();
        await Task.Delay(300);

        Assert.Empty(cut.FindAll("section#plan"));
        Assert.True(Deshabilitado(cut, "confirmar"));
    }
}
