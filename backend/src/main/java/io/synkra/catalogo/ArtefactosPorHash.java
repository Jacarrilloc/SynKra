package io.synkra.catalogo;

import io.synkra.catalogo.api.ArtefactoAbierto;
import io.synkra.plataforma.CodigosError;
import io.synkra.plataforma.DetalleError;
import io.synkra.plataforma.ErrorApi;
import io.synkra.plataforma.ExcepcionApi;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * R-C13: abre el JAR guardado a partir de su SHA-256. El hash no se recalcula en cada descarga; se comprueban que el
 * archivo exista, que no salga de la raíz de datos ni pase por enlaces simbólicos y que su tamaño sea el guardado.
 */
@Component
class ArtefactosPorHash {

    private static final Pattern HASH = Pattern.compile("[0-9a-f]{64}");
    private static final String CAMPO = "artefacto";

    private final Path raiz;
    private final PluginRepositorio plugins;

    ArtefactosPorHash(@Value("${synkra.data}") String datos, PluginRepositorio plugins) {
        this.raiz = Path.of(datos).normalize();
        this.plugins = plugins;
    }

    ArtefactoAbierto abrir(String sha256) throws IOException {
        if (sha256 == null || !HASH.matcher(sha256).matches()) {
            throw noEncontrado();
        }
        var fila = plugins.findBySha256(sha256).stream()
                .min(Comparator.comparing(Plugin::core).thenComparing(Plugin::nombre).thenComparing(Plugin::version))
                .orElseThrow(ArtefactosPorHash::noEncontrado);
        var ruta = raiz.resolve(fila.ruta()).normalize();
        if (fila.ruta().isEmpty() || !ruta.startsWith(raiz) || ruta.equals(raiz) || hayEnlace(ruta)) {
            throw interno(MotivosCatalogo.ARTEFACTO_RUTA_FUERA);
        }
        BasicFileAttributes atributos;
        try {
            atributos = Files.readAttributes(ruta, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        } catch (NoSuchFileException e) {
            throw interno(MotivosCatalogo.ARTEFACTO_AUSENTE);
        }
        if (!atributos.isRegularFile()) {
            throw interno(MotivosCatalogo.ARTEFACTO_AUSENTE);
        }
        if (atributos.size() != fila.tamano()) {
            throw interno(MotivosCatalogo.ARTEFACTO_TAMANO);
        }
        var nombre = fila.nombre() + "-" + fila.version() + ".jar";
        try {
            return new ArtefactoAbierto(sha256, fila.tamano(), nombre,
                    Files.newInputStream(ruta, LinkOption.NOFOLLOW_LINKS));
        } catch (NoSuchFileException e) {
            throw interno(MotivosCatalogo.ARTEFACTO_AUSENTE);
        }
    }

    /** Algún componente de la ruta, por debajo de la raíz, es un enlace simbólico. */
    private boolean hayEnlace(Path ruta) {
        var actual = raiz;
        for (var parte : raiz.relativize(ruta)) {
            actual = actual.resolve(parte);
            if (Files.isSymbolicLink(actual)) {
                return true;
            }
        }
        return false;
    }

    private static ExcepcionApi noEncontrado() {
        return new ExcepcionApi(HttpStatus.NOT_FOUND,
                new ErrorApi(CodigosError.NO_ENCONTRADO, CodigosError.MSG_NO_ENCONTRADO, List.of()));
    }

    private static ExcepcionApi interno(String motivo) {
        return new ExcepcionApi(HttpStatus.INTERNAL_SERVER_ERROR, new ErrorApi(CodigosError.ERROR_INTERNO,
                CodigosError.MSG_ERROR_INTERNO, List.of(new DetalleError(CAMPO, motivo))));
    }
}
