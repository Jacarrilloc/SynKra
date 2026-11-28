using System.Net.Http.Headers;
using System.Net.Http.Json;
using System.Text;

namespace Synkra.Front.Api;

public partial class ApiSynkra
{
    private const string CabeceraUsuario = "X-Synkra-Usuario";

    public Task<Veredicto> VerificarAsync(SeleccionVerificacion seleccion, CancellationToken ct = default)
        => EnviarAsync<Veredicto>("api/verificaciones", JsonContent.Create(seleccion, options: Opciones), null, ct);

    public Task<PlanDespliegue> CrearPlanAsync(SolicitudPlan solicitud, string? usuario, CancellationToken ct = default)
        => EnviarAsync<PlanDespliegue>("api/despliegues", JsonContent.Create(solicitud, options: Opciones), usuario, ct);

    public Task<Confirmacion> ConfirmarAsync(long planId, string? usuario, CancellationToken ct = default)
        => EnviarAsync<Confirmacion>($"api/despliegues/{planId}/confirmacion", null, usuario, ct);

    public Task<PlanDespliegue> RevertirAsync(long origenId, string? usuario, CancellationToken ct = default)
        => EnviarAsync<PlanDespliegue>($"api/despliegues/{origenId}/reversion", null, usuario, ct);

    public Task<PluginRegistrado> SubirPluginAsync(string core, Stream jar, string nombreArchivo, string? etiquetas, CancellationToken ct = default)
    {
        var contenido = new MultipartFormDataContent();
        var archivo = new StreamContent(jar);
        archivo.Headers.ContentType = new MediaTypeHeaderValue("application/java-archive");
        contenido.Add(archivo, "archivo", nombreArchivo);
        if (!string.IsNullOrWhiteSpace(etiquetas))
        {
            var parte = new StringContent(etiquetas, Encoding.UTF8);
            parte.Headers.ContentType = new MediaTypeHeaderValue("application/json");
            contenido.Add(parte, "etiquetas");
        }

        return EnviarAsync<PluginRegistrado>($"api/cores/{Uri.EscapeDataString(core)}/plugins", contenido, null, ct);
    }

    private async Task<T> EnviarAsync<T>(string ruta, HttpContent? contenido, string? usuario, CancellationToken ct)
    {
        using var peticion = new HttpRequestMessage(HttpMethod.Post, ruta) { Content = contenido };
        peticion.Headers.Accept.Add(new MediaTypeWithQualityHeaderValue("application/json"));
        if (!string.IsNullOrWhiteSpace(usuario))
        {
            peticion.Headers.TryAddWithoutValidation(CabeceraUsuario, usuario);
        }

        try
        {
            using var respuesta = await _http.SendAsync(peticion, ct);
            var cuerpo = await respuesta.Content.ReadAsStringAsync(ct);
            return Interpretar<T>((int)respuesta.StatusCode, respuesta.IsSuccessStatusCode, cuerpo);
        }
        catch (HttpRequestException e)
        {
            throw NoDisponible(e);
        }
        catch (TaskCanceledException e) when (!ct.IsCancellationRequested)
        {
            throw NoDisponible(e);
        }
    }
}
