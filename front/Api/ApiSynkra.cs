using System.Net.Http.Headers;
using System.Text.Json;
using System.Text.Json.Serialization;

namespace Synkra.Front.Api;

/// <summary>Cliente tipado de la API REST de SynKra; todo lo que devuelve sale tal cual de la API.</summary>
public partial class ApiSynkra
{
    private static readonly JsonSerializerOptions Opciones = new(JsonSerializerDefaults.Web)
    {
        UnmappedMemberHandling = JsonUnmappedMemberHandling.Disallow,
        RespectNullableAnnotations = true,
        RespectRequiredConstructorParameters = true,
        Converters = { new JsonStringEnumConverter(JsonNamingPolicy.SnakeCaseLower, allowIntegerValues: false) },
    };

    private readonly HttpClient _http;

    public ApiSynkra(HttpClient http)
    {
        _http = http;
    }

    public Task<IReadOnlyList<VistaPais>> VistaGeneralAsync(CancellationToken ct = default)
        => LeerAsync<IReadOnlyList<VistaPais>>("api/paises", ct);

    public Task<IReadOnlyList<ResumenDespliegue>> HistorialAsync(string pais, CancellationToken ct = default)
        => LeerAsync<IReadOnlyList<ResumenDespliegue>>($"api/paises/{Uri.EscapeDataString(pais)}/despliegues", ct);

    public Task<Despliegue> DespliegueAsync(long id, CancellationToken ct = default)
        => LeerAsync<Despliegue>($"api/despliegues/{id}", ct);

    private async Task<T> LeerAsync<T>(string ruta, CancellationToken ct)
    {
        using var peticion = new HttpRequestMessage(HttpMethod.Get, ruta);
        peticion.Headers.Accept.Add(new MediaTypeWithQualityHeaderValue("application/json"));

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

    private static T Interpretar<T>(int estado, bool exito, string cuerpo)
    {
        if (exito)
        {
            try
            {
                var valor = JsonSerializer.Deserialize<T>(cuerpo, Opciones);
                if (valor is null)
                {
                    throw new JsonException("cuerpo nulo");
                }

                return valor;
            }
            catch (JsonException e)
            {
                throw new ErrorApiSynkraException(estado, CodigosFront.RespuestaInvalida,
                    "La respuesta de la API no tiene la forma esperada", [], e);
            }
        }

        ErrorApi? error = null;
        try
        {
            error = JsonSerializer.Deserialize<ErrorApi>(cuerpo, Opciones);
        }
        catch (JsonException)
        {
            // el cuerpo no es un error de la API: se informa abajo
        }

        if (error is null)
        {
            throw new ErrorApiSynkraException(estado, CodigosFront.RespuestaInvalida,
                $"La API respondió {estado} sin un cuerpo de error válido", []);
        }

        throw new ErrorApiSynkraException(estado, error.Codigo, error.Mensaje, error.Detalles);
    }

    private static ErrorApiSynkraException NoDisponible(Exception causa)
        => new(null, CodigosFront.ApiNoDisponible, "No se pudo conectar con la API de SynKra", [], causa);
}
