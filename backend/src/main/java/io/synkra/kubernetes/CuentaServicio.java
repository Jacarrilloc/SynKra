package io.synkra.kubernetes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.core.env.Environment;

/** Dónde está la cuenta de servicio del pod y a qué clúster apunta; nada sale de otro sitio. */
record CuentaServicio(Path directorio, String url) {

    static final String RUTA_POR_DEFECTO = "/var/run/secrets/kubernetes.io/serviceaccount";

    /**
     * La ruta de la cuenta es configurable (pruebas); la URL sale solo de KUBERNETES_SERVICE_HOST y
     * KUBERNETES_SERVICE_PORT del entorno del proceso, siempre por https.
     */
    static CuentaServicio de(Environment env, VariablesPod variables) {
        var directorio = Path.of(env.getProperty("synkra.kubernetes.cuenta", RUTA_POR_DEFECTO));
        String host = variables.valores().getOrDefault("KUBERNETES_SERVICE_HOST", "");
        String puerto = variables.valores().getOrDefault("KUBERNETES_SERVICE_PORT", "443");
        String url = host.isBlank() ? "" : "https://" + (host.contains(":") ? "[" + host + "]" : host) + ":" + puerto;
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
        } catch (IOException | RuntimeException e) {
            return ""; // un error al leer cuenta como «sin token»: el arranque no debe fallar
        }
    }
}
