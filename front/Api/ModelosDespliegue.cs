namespace Synkra.Front.Api;

public enum EstadoPlan { Pendiente, Confirmado, Vencido }

public record AlternativaVeredicto(string Tipo, string Nombre, string Version);

public record MotivoVeredicto(string Codigo, string? Plugin, string Mensaje, string? Campo);

public record VeredictoPlugin(
    string Nombre,
    string Version,
    string Core,
    bool Compatible,
    IReadOnlyList<MotivoVeredicto> Motivos,
    IReadOnlyList<AlternativaVeredicto> Alternativas);

public record Veredicto(string Global, IReadOnlyList<VeredictoPlugin> Plugins, IReadOnlyList<MotivoVeredicto> MotivosGlobales);

public record PluginResumen(string Nombre, string Version, string Sha256);

public record ResumenPlan(string Namespace, string Core, string VersionCore, string Imagen, IReadOnlyList<PluginResumen> Plugins);

public record CambioCore(string? Antes, string Despues, bool Igual);

public record CambiosPlan(
    CambioCore Core,
    IReadOnlyList<PluginResumen> Anadidos,
    IReadOnlyList<PluginResumen> Quitados,
    IReadOnlyList<PluginResumen> Iguales,
    bool SinCambios);

public record PlanDespliegue(
    long Id,
    string Pais,
    TipoDespliegue Tipo,
    long? OrigenId,
    EstadoPlan Estado,
    string Core,
    string VersionCore,
    string CreadoPor,
    DateTimeOffset CreadoEn,
    DateTimeOffset VenceEn,
    ResumenPlan Resumen,
    CambiosPlan Cambios);

public record Confirmacion(
    long Id,
    string Pais,
    EstadoDespliegue Estado,
    string CreadoPor,
    string ConfirmadoPor,
    DateTimeOffset ConfirmadoEn);

public record PluginPedido(string Nombre, string Version);

public record SeleccionVerificacion(string Core, string VersionCore, IReadOnlyList<PluginPedido> Plugins);

public record SolicitudPlan(string Pais, string Core, string VersionCore, IReadOnlyList<PluginPedido> Plugins);
