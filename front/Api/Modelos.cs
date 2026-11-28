using System.Text.Json;

namespace Synkra.Front.Api;

public enum LecturaCluster { Vacio, Leido, Inconsistente, NoDisponible }

public enum TipoDespliegue { Despliegue, Reversion }

public enum EstadoDespliegue { Aplicando, Listo, Fallido }

public record PluginCorriendo(string Nombre, string Version, string Sha256);

public record VistaPais(
    string Pais,
    LecturaCluster Lectura,
    string? Imagen,
    IReadOnlyList<PluginCorriendo> Plugins,
    bool? Concuerda,
    long? UltimoDespliegueId,
    string? Core,
    string? VersionCore);

public record PluginDesplegado(string Nombre, string Version, string Sha256, IReadOnlyDictionary<string, string> Etiquetas);

public record ResumenDespliegue(
    long Id,
    string Pais,
    TipoDespliegue Tipo,
    long? OrigenId,
    string Core,
    string VersionCore,
    string Imagen,
    IReadOnlyList<PluginDesplegado> Plugins,
    string CreadoPor,
    string ConfirmadoPor,
    DateTimeOffset ConfirmadoEn,
    EstadoDespliegue Estado,
    DateTimeOffset? TerminadoEn);

public record ContextoFallo(string LogCore, string LogInit, string Eventos);

public record Despliegue(
    long Id,
    string Pais,
    TipoDespliegue Tipo,
    long? OrigenId,
    string Core,
    string VersionCore,
    string Imagen,
    IReadOnlyList<PluginDesplegado> Plugins,
    string CreadoPor,
    string ConfirmadoPor,
    DateTimeOffset ConfirmadoEn,
    EstadoDespliegue Estado,
    DateTimeOffset? TerminadoEn,
    JsonElement Veredicto,
    ContextoFallo? Contexto);

public record DetalleError(string Campo, string Motivo);

public record ErrorApi(string Codigo, string Mensaje, IReadOnlyList<DetalleError> Detalles);
