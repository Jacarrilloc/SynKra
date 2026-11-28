using System.Net;
using System.Text;
using System.Text.Json.Nodes;

namespace Synkra.Front.Tests;

/// <summary>Servidor HTTP simulado: responde por (método, ruta con query) exacta y registra lo recibido.</summary>
public sealed class ManejadorHttpSimulado : HttpMessageHandler
{
    private const string NoEncontrado = "{\"codigo\":\"NO_ENCONTRADO\",\"mensaje\":\"Recurso no encontrado\",\"detalles\":[]}";

    private readonly Dictionary<(HttpMethod, string), Func<HttpResponseMessage>> _respuestas = new();
    private readonly List<(HttpMethod Metodo, string Ruta)> _peticiones = new();

    private readonly List<string> _accept = new();
    private readonly List<string> _cuerpos = new();
    private readonly List<string?> _tiposContenido = new();
    private readonly List<string?> _usuarios = new();
    private readonly List<IReadOnlyList<ParteMultipart>> _partes = new();
    private readonly List<(HttpMethod Metodo, string Ruta, JsonNode? Esperado, Func<Task<HttpResponseMessage>> Respuesta)> _segunCuerpo = new();
    private readonly object _candado = new();

    /// <summary>Una parte de un cuerpo multipart/form-data recibido.</summary>
    public sealed record ParteMultipart(string? Nombre, string? NombreArchivo, string? TipoContenido, string Cuerpo);

    /// <summary>Una respuesta en espera: la petición ya llegó (Llego) y no se contesta hasta Suelta().</summary>
    public sealed class Retenida
    {
        private readonly TaskCompletionSource _llego = new(TaskCreationOptions.RunContinuationsAsynchronously);
        private readonly TaskCompletionSource _suelta = new(TaskCreationOptions.RunContinuationsAsynchronously);

        public bool Llego => _llego.Task.IsCompleted;
        public void Suelta() => _suelta.TrySetResult();
        internal void MarcaLlegada() => _llego.TrySetResult();
        internal Task Espera => _suelta.Task;
    }

    public IReadOnlyList<(HttpMethod Metodo, string Ruta)> Peticiones => _peticiones;

    /// <summary>La cabecera Accept de cada petición recibida, en orden.</summary>
    public IReadOnlyList<string> Accept => _accept;

    /// <summary>El cuerpo de cada petición como texto (vacío si no tiene contenido), paralelo a Peticiones.</summary>
    public IReadOnlyList<string> Cuerpos => _cuerpos;

    /// <summary>El Content-Type del contenido de cada petición (null si no tiene), paralelo a Peticiones.</summary>
    public IReadOnlyList<string?> TiposContenido => _tiposContenido;

    /// <summary>El valor de X-Synkra-Usuario de cada petición (null si no vino), paralelo a Peticiones.</summary>
    public IReadOnlyList<string?> Usuarios => _usuarios;

    /// <summary>Las partes de cada petición multipart/form-data (lista vacía si no lo es), paralelo a Peticiones.</summary>
    public IReadOnlyList<IReadOnlyList<ParteMultipart>> Partes => _partes;

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

    /// <summary>Para la misma ruta, la respuesta depende del cuerpo JSON recibido (comparado como JSON, no como texto).</summary>
    public ManejadorHttpSimulado RespondeSegunCuerpo(HttpMethod metodo, string ruta, string cuerpoEsperado, HttpStatusCode estado, string cuerpo)
    {
        _segunCuerpo.Add((metodo, ruta, JsonNode.Parse(cuerpoEsperado), () => Task.FromResult(Crear(estado, cuerpo, "application/json"))));
        return this;
    }

    /// <summary>Como RespondeSegunCuerpo, pero la respuesta no sale hasta que la prueba llama a Suelta() de lo devuelto.</summary>
    public Retenida Retiene(HttpMethod metodo, string ruta, string cuerpoEsperado, HttpStatusCode estado, string cuerpo)
    {
        var retenida = new Retenida();
        _segunCuerpo.Add((metodo, ruta, JsonNode.Parse(cuerpoEsperado), async () =>
        {
            retenida.MarcaLlegada();
            await retenida.Espera;
            return Crear(estado, cuerpo, "application/json");
        }));
        return retenida;
    }

    protected override async Task<HttpResponseMessage> SendAsync(HttpRequestMessage request, CancellationToken cancellationToken)
    {
        if (cancellationToken.IsCancellationRequested)
        {
            throw new TaskCanceledException(null, null, cancellationToken);
        }

        var ruta = request.RequestUri!.PathAndQuery;
        // Un multipart se lee por partes (su flujo puede leerse una sola vez); su «cuerpo» queda vacío.
        var partes = request.Content is MultipartFormDataContent multipart ? await LeePartes(multipart) : [];
        var cuerpo = request.Content is null || request.Content is MultipartFormDataContent
            ? ""
            : await request.Content.ReadAsStringAsync(cancellationToken);
        var usuario = request.Headers.TryGetValues("X-Synkra-Usuario", out var v) ? string.Join(",", v) : null;
        lock (_candado)
        {
            _peticiones.Add((request.Method, ruta));
            _accept.Add(request.Headers.Accept.ToString());
            _cuerpos.Add(cuerpo);
            _tiposContenido.Add(request.Content?.Headers.ContentType?.ToString());
            _usuarios.Add(usuario);
            _partes.Add(partes);
        }

        Func<Task<HttpResponseMessage>>? segun;
        lock (_candado)
        {
            segun = ElegirSegunCuerpo(request.Method, ruta, cuerpo);
        }

        if (segun is not null)
        {
            return await segun();
        }

        if (_segunCuerpo.Any(x => x.Metodo == request.Method && x.Ruta == ruta))
        {
            return Crear(HttpStatusCode.NotFound, NoEncontrado, "application/json");
        }

        return _respuestas.TryGetValue((request.Method, ruta), out var f)
            ? f()
            : Crear(HttpStatusCode.NotFound, NoEncontrado, "application/json");
    }

    private Func<Task<HttpResponseMessage>>? ElegirSegunCuerpo(HttpMethod metodo, string ruta, string cuerpo)
    {
        JsonNode? recibido;
        try
        {
            recibido = JsonNode.Parse(cuerpo);
        }
        catch (System.Text.Json.JsonException)
        {
            return null;
        }

        foreach (var (m, r, esperado, respuesta) in _segunCuerpo)
        {
            if (m == metodo && r == ruta && JsonNode.DeepEquals(esperado, recibido))
            {
                return respuesta;
            }
        }

        return null;
    }

    private static async Task<IReadOnlyList<ParteMultipart>> LeePartes(MultipartFormDataContent multipart)
    {
        var partes = new List<ParteMultipart>();
        foreach (var parte in multipart)
        {
            partes.Add(new ParteMultipart(
                parte.Headers.ContentDisposition?.Name?.Trim('"'),
                parte.Headers.ContentDisposition?.FileName?.Trim('"'),
                parte.Headers.ContentType?.ToString(),
                await parte.ReadAsStringAsync()));
        }

        return partes;
    }

    private static HttpResponseMessage Crear(HttpStatusCode estado, string cuerpo, string tipo)
        => new(estado) { Content = new StringContent(cuerpo, Encoding.UTF8, tipo) };
}
