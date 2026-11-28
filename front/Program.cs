using Synkra.Front.Api;
using Synkra.Front.Components;

var builder = WebApplication.CreateBuilder(args);

// La URL se calcula antes de Build(): una URL mala impide arrancar.
var urlApi = ConfiguracionApi.UrlBase(builder.Configuration);

builder.Services.AddRazorComponents().AddInteractiveServerComponents();
builder.Services.AddHttpClient<ApiSynkra>(c => c.BaseAddress = urlApi);

var app = builder.Build();

app.UseStaticFiles();
app.UseAntiforgery();

app.MapRazorComponents<App>().AddInteractiveServerRenderMode();

app.Run();
