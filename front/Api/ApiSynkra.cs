namespace Synkra.Front.Api;

public class ApiSynkra
{
    public ApiSynkra(HttpClient http)
    {
    }

    public Task<IReadOnlyList<VistaPais>> VistaGeneralAsync(CancellationToken ct = default)
        => throw new NotImplementedException();

    public Task<IReadOnlyList<ResumenDespliegue>> HistorialAsync(string pais, CancellationToken ct = default)
        => throw new NotImplementedException();

    public Task<Despliegue> DespliegueAsync(long id, CancellationToken ct = default)
        => throw new NotImplementedException();
}
