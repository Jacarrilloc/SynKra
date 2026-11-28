using System.Net;
using System.Text;

namespace Synkra.Front.Tests;

/// <summary>Servidor HTTP simulado: responde por (método, ruta con query) exacta y registra lo recibido.</summary>
public sealed class ManejadorHttpSimulado : HttpMessageHandler
{
    private const string NoEncontrado = "{\"codigo\":\"NO_ENCONTRADO\",\"mensaje\":\"Recurso no encontrado\",\"detalles\":[]}";

    private readonly Dictionary<(HttpMethod, string), Func<HttpResponseMessage>> _respuestas = new();
    private readonly List<(HttpMethod Metodo, string Ruta)> _peticiones = new();

    public IReadOnlyList<(HttpMethod Metodo, string Ruta)> Peticiones => _peticiones;

    public static string Fixture(string nombre)
        => File.ReadAllText(Path.Combine(AppContext.BaseDirectory, "Fixtures", nombre));

    public ManejadorHttpSimulado Responde(HttpMethod metodo, string ruta, HttpStatusCode estado, string cuerpo, string tipo = "application/json")
    {
        _respuestas[(metodo, ruta)] = () => Crear(estado, cuerpo, tipo);
        return this;
    }

    public ManejadorHttpSimulado Lanza(HttpMethod metodo, string ruta, Exception excepcion)
    {
        _respuestas[(metodo, ruta)] = () => throw excepcion;
        return this;
    }

    protected override Task<HttpResponseMessage> SendAsync(HttpRequestMessage request, CancellationToken cancellationToken)
    {
        var ruta = request.RequestUri!.PathAndQuery;
        _peticiones.Add((request.Method, ruta));
        try
        {
            var respuesta = _respuestas.TryGetValue((request.Method, ruta), out var f)
                ? f()
                : Crear(HttpStatusCode.NotFound, NoEncontrado, "application/json");
            return Task.FromResult(respuesta);
        }
        catch (Exception e)
        {
            return Task.FromException<HttpResponseMessage>(e);
        }
    }

    private static HttpResponseMessage Crear(HttpStatusCode estado, string cuerpo, string tipo)
        => new(estado) { Content = new StringContent(cuerpo, Encoding.UTF8, tipo) };
}
