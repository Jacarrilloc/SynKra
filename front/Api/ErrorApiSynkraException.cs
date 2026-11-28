namespace Synkra.Front.Api;

/// <summary>Error de la API de SynKra (o del camino hasta ella) con su código, mensaje y detalles.</summary>
public class ErrorApiSynkraException : Exception
{
    public ErrorApiSynkraException(int? estado, string codigo, string mensaje, IReadOnlyList<DetalleError> detalles, Exception? causa = null)
        : base(mensaje, causa)
    {
        Estado = estado;
        Codigo = codigo;
        Detalles = detalles;
    }

    public int? Estado { get; }
    public string Codigo { get; }
    public IReadOnlyList<DetalleError> Detalles { get; }
}
