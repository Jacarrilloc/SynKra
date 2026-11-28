namespace Synkra.Front.Api;

public partial class ApiSynkra
{
    public Task<IReadOnlyList<CoreRegistrado>> CoresAsync(CancellationToken ct = default)
        => LeerAsync<IReadOnlyList<CoreRegistrado>>("api/cores", ct);

    public Task<IReadOnlyList<PluginRegistrado>> PluginsAsync(string core, CancellationToken ct = default)
        => LeerAsync<IReadOnlyList<PluginRegistrado>>($"api/cores/{Uri.EscapeDataString(core)}/plugins", ct);

    public Task<IReadOnlyList<PluginRegistrado>> PluginsConEtiquetaAsync(string core, string clave, string valor, CancellationToken ct = default)
        => LeerAsync<IReadOnlyList<PluginRegistrado>>(
            $"api/cores/{Uri.EscapeDataString(core)}/plugins?{Uri.EscapeDataString(clave)}={Uri.EscapeDataString(valor)}", ct);
}
