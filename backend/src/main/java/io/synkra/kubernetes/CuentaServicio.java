package io.synkra.kubernetes;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.core.env.Environment;

/** Dónde está la cuenta de servicio del pod y a qué clúster apunta; nada sale de otro sitio. */
record CuentaServicio(Path directorio, String url) {

    static final String RUTA_POR_DEFECTO = "/var/run/secrets/kubernetes.io/serviceaccount";

    static CuentaServicio de(Environment env) {
        var directorio = Path.of(env.getProperty("synkra.kubernetes.cuenta", RUTA_POR_DEFECTO));
        String url = env.getProperty("synkra.kubernetes.url", "");
        if (url.isBlank()) {
            String host = env.getProperty("KUBERNETES_SERVICE_HOST", "");
            String puerto = env.getProperty("KUBERNETES_SERVICE_PORT", "443");
            url = host.isBlank() ? "" : "https://" + (host.contains(":") ? "[" + host + "]" : host) + ":" + puerto;
        }
        return new CuentaServicio(directorio, url);
    }

    Path archivoToken() {
        return directorio.resolve("token");
    }

    Path archivoCa() {
        return directorio.resolve("ca.crt");
    }

    /** Hay token no vacío y clúster al que hablar. */
    boolean completa() {
        return !url.isBlank() && !token().isBlank();
    }

    /** Se relee en cada uso: el token del pod rota. Vacío si falta o no se puede leer. */
    String token() {
        try {
            return Files.isReadable(archivoToken()) ? Files.readString(archivoToken()).strip() : "";
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
