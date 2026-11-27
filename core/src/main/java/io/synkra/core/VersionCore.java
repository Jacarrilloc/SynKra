package io.synkra.core;

import io.synkra.verificador.Resultado;
import io.synkra.verificador.Version;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Properties;
import org.springframework.stereotype.Component;

/**
 * Versión del core (D4): se escribe en tiempo de build en {@code synkra-core.properties} y se lee del
 * JAR con {@code getResourceAsStream}; ni el entorno ni las propiedades de Spring la alcanzan.
 */
@Component
public class VersionCore {

    static final String RECURSO = "synkra-core.properties";

    private final Version version;

    public VersionCore() {
        var props = new Properties();
        try (InputStream in = VersionCore.class.getClassLoader().getResourceAsStream(RECURSO)) {
            if (in == null) {
                throw new IllegalStateException("Falta " + RECURSO + " en el JAR del core");
            }
            props.load(in);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        this.version = switch (Version.parsear(props.getProperty("version"))) {
            case Resultado.Exito<Version> ok -> ok.valor();
            case Resultado.Fallo<Version> f -> throw new IllegalStateException(
                    "Versión del core inválida en " + RECURSO + ": " + f.motivos());
        };
    }

    public Version version() {
        return version;
    }
}
