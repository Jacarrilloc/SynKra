namespace Synkra.Front.Api;

public record VersionRegistrada(string Version, string Imagen);

public record CoreRegistrado(string Nombre, IReadOnlyList<VersionRegistrada> Versiones);

public record PluginRegistrado(
    string Core,
    string Nombre,
    string Version,
    string CoreCompatible,
    IReadOnlyDictionary<string, string> Etiquetas,
    string Sha256,
    long Tamano);
