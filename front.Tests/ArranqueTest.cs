using System.Diagnostics;
using System.Net;
using System.Text;

namespace Synkra.Front.Tests;

/// <summary>Arranca la aplicación compilada como un proceso aparte (F-04 de U09-A) y lo mata al terminar.</summary>
public sealed class ArranqueTest : IDisposable
{
    private readonly StringBuilder _salida = new();
    private Process? _proceso;

    public void Dispose()
    {
        if (_proceso is { HasExited: false })
        {
            _proceso.Kill(entireProcessTree: true);
            _proceso.WaitForExit(10_000);
        }

        _proceso?.Dispose();
    }

    private static string RaizDelRepositorio()
    {
        var dir = new DirectoryInfo(AppContext.BaseDirectory);
        while (dir is not null && !File.Exists(Path.Combine(dir.FullName, "front", "Synkra.Front.csproj")))
        {
            dir = dir.Parent;
        }

        return dir?.FullName ?? throw new InvalidOperationException("No se encontró la raíz del repositorio (front/Synkra.Front.csproj)");
    }

    private Process Arranca(int puerto, string urlApi)
    {
        var raiz = RaizDelRepositorio();
        var dll = Path.Combine(raiz, "front", "bin", "Debug", "net10.0", "Synkra.Front.dll");
        Assert.True(File.Exists(dll), $"Falta la aplicación compilada: {dll}");

        // Con una URL mala la aplicación termina con una excepción no controlada y el sistema volcaría un core de
        // cientos de MB en front/: «ulimit -c 0» lo evita, y «exec» deja a dotnet como el proceso que se mata.
        var info = new ProcessStartInfo("/bin/sh")
        {
            WorkingDirectory = Path.Combine(raiz, "front"),
            RedirectStandardOutput = true,
            RedirectStandardError = true,
            UseShellExecute = false,
        };
        info.ArgumentList.Add("-c");
        info.ArgumentList.Add("ulimit -c 0; exec dotnet \"$0\"");
        info.ArgumentList.Add(dll);
        info.Environment["ASPNETCORE_URLS"] = $"http://127.0.0.1:{puerto}";
        info.Environment["ASPNETCORE_ENVIRONMENT"] = "Production";
        info.Environment["Synkra__ApiUrl"] = urlApi;

        var proceso = new Process { StartInfo = info };
        proceso.OutputDataReceived += (_, e) => { lock (_salida) { _salida.AppendLine(e.Data); } };
        proceso.ErrorDataReceived += (_, e) => { lock (_salida) { _salida.AppendLine(e.Data); } };
        proceso.Start();
        proceso.BeginOutputReadLine();
        proceso.BeginErrorReadLine();
        _proceso = proceso;
        return proceso;
    }

    private string Salida()
    {
        lock (_salida)
        {
            return _salida.ToString();
        }
    }

    [Fact]
    public async Task ConUrlValidaArrancaYSirveLaVistaGeneral()
    {
        var proceso = Arranca(18741, "http://127.0.0.1:9/");
        using var http = new HttpClient { BaseAddress = new Uri("http://127.0.0.1:18741/") };
        var limite = DateTime.UtcNow.AddSeconds(60);
        HttpResponseMessage? respuesta = null;
        while (DateTime.UtcNow < limite && !proceso.HasExited)
        {
            try
            {
                respuesta = await http.GetAsync("/");
                break;
            }
            catch (HttpRequestException)
            {
                await Task.Delay(250);
            }
        }

        Assert.True(respuesta is not null, $"La aplicación no respondió en 60 s. Salida:\n{Salida()}");
        var html = await respuesta.Content.ReadAsStringAsync();
        Assert.Equal(HttpStatusCode.OK, respuesta.StatusCode);
        Assert.Contains("<h1>Vista general</h1>", html);
        Assert.Contains("data-estado=\"error\"", html);
        Assert.Contains("API_NO_DISPONIBLE", html);
    }

    [Fact]
    public void ConUrlMalaNoArranca()
    {
        var proceso = Arranca(18742, "ftp://h/");

        var termino = proceso.WaitForExit(60_000);
        proceso.WaitForExit();

        Assert.True(termino, $"El proceso no terminó en 60 s. Salida:\n{Salida()}");
        Assert.NotEqual(0, proceso.ExitCode);
        Assert.Contains("Synkra:ApiUrl falta o no es una URL absoluta http o https", Salida());
    }
}
