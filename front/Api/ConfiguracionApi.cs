namespace Synkra.Front.Api;

public static class ConfiguracionApi
{
    public const string Clave = "Synkra:ApiUrl";

    /// <summary>La URL base del backend: absoluta, http o https, y siempre terminada en «/».</summary>
    public static Uri UrlBase(IConfiguration configuracion)
    {
        var texto = configuracion[Clave];
        if (string.IsNullOrWhiteSpace(texto)
            || !Uri.TryCreate(texto.Trim(), UriKind.Absolute, out var url)
            || (url.Scheme != Uri.UriSchemeHttp && url.Scheme != Uri.UriSchemeHttps))
        {
            throw new InvalidOperationException("Synkra:ApiUrl falta o no es una URL absoluta http o https");
        }

        return url.AbsoluteUri.EndsWith('/') ? url : new Uri(url.AbsoluteUri + "/");
    }
}
