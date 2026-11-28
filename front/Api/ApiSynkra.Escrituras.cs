namespace Synkra.Front.Api;

public partial class ApiSynkra
{
    public Task<Veredicto> VerificarAsync(SeleccionVerificacion seleccion, CancellationToken ct = default)
        => throw new NotImplementedException();

    public Task<PlanDespliegue> CrearPlanAsync(SolicitudPlan solicitud, string? usuario, CancellationToken ct = default)
        => throw new NotImplementedException();

    public Task<Confirmacion> ConfirmarAsync(long planId, string? usuario, CancellationToken ct = default)
        => throw new NotImplementedException();

    public Task<PlanDespliegue> RevertirAsync(long origenId, string? usuario, CancellationToken ct = default)
        => throw new NotImplementedException();

    public Task<PluginRegistrado> SubirPluginAsync(string core, Stream jar, string nombreArchivo, string? etiquetas, CancellationToken ct = default)
        => throw new NotImplementedException();
}
