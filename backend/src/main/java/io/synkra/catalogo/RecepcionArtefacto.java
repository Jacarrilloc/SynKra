package io.synkra.catalogo;

import io.synkra.plataforma.CodigosError;
import io.synkra.plataforma.DetalleError;
import io.synkra.plataforma.ErrorApi;
import io.synkra.plataforma.ExcepcionApi;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;
import java.util.regex.Pattern;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.unit.DataSize;

/** R-C5, R-C6 y R-C11: copia el JAR a un temporal con límite, SHA-256 en la misma pasada y revisa sus entradas. */
@Component
class RecepcionArtefacto {

    static final int MAX_DETALLES = 10;
    static final int MAX_NOMBRE = 200;
    private static final Pattern UNIDAD = Pattern.compile("^[A-Za-z]:.*", Pattern.DOTALL);

    private final Path temporales;
    private final DataSize maximo;

    RecepcionArtefacto(@Value("${synkra.data}") String datos, @Value("${synkra.max-artefacto}") DataSize maximo) {
        this.temporales = Path.of(datos).resolve("artefactos").resolve(".tmp");
        this.maximo = maximo;
    }

    ArtefactoRecibido recibir(InputStream entrada) throws IOException {
        Files.createDirectories(temporales);
        var temporal = Files.createTempFile(temporales, "recepcion-", ".jar");
        var recibido = false;
        try {
            var resumen = nuevoDigest();
            long tamano = copiar(entrada, temporal, resumen);
            revisarEntradas(temporal);
            recibido = true;
            return new ArtefactoRecibido(temporal, HexFormat.of().formatHex(resumen.digest()), tamano);
        } finally {
            if (!recibido) {
                Files.deleteIfExists(temporal);
            }
        }
    }

    private long copiar(InputStream entrada, Path destino, MessageDigest resumen) throws IOException {
        long limite = maximo.toBytes();
        long total = 0;
        var buffer = new byte[8192];
        try (var in = new DigestInputStream(entrada, resumen); var out = Files.newOutputStream(destino)) {
            int n;
            while ((n = in.read(buffer)) != -1) {
                total += n;
                if (total > limite) {
                    throw error(HttpStatus.PAYLOAD_TOO_LARGE, CodigosError.DEMASIADO_GRANDE,
                            CodigosError.MSG_DEMASIADO_GRANDE,
                            List.of(detalle(MotivosCatalogo.ARTEFACTO_GRANDE.formatted(texto(maximo)))));
                }
                out.write(buffer, 0, n);
            }
        }
        return total;
    }

    private void revisarEntradas(Path jar) throws IOException {
        var culpables = new ArrayList<DetalleError>();
        try (var zip = ZipFile.builder().setPath(jar).get()) {
            for (var en : Collections.list(zip.getEntries())) {
                var motivo = en.isUnixSymlink()
                        ? MotivosCatalogo.ENTRADA_ENLACE.formatted(recortar(en.getName()))
                        : motivoDeRechazo(en.getName());
                if (motivo != null && culpables.size() < MAX_DETALLES) {
                    culpables.add(detalle(motivo));
                }
            }
        } catch (IOException noEsZip) {
            throw error(HttpStatus.UNPROCESSABLE_ENTITY, CodigosCatalogo.ARTEFACTO_INVALIDO,
                    CodigosError.MSG_ENTIDAD_NO_PROCESABLE, List.of(detalle(MotivosCatalogo.ARTEFACTO_NO_ES_ZIP)));
        }
        if (!culpables.isEmpty()) {
            throw error(HttpStatus.UNPROCESSABLE_ENTITY, CodigosCatalogo.RUTA_NO_PERMITIDA,
                    CodigosError.MSG_ENTIDAD_NO_PROCESABLE, culpables);
        }
    }

    private static String motivoDeRechazo(String nombre) {
        var normal = nombre.replace('\\', '/');
        var malo = nombre.isEmpty() || normal.startsWith("/") || UNIDAD.matcher(normal).matches()
                || nombre.indexOf('\0') >= 0 || List.of(normal.split("/")).contains("..");
        return malo ? MotivosCatalogo.ENTRADA_NO_PERMITIDA.formatted(recortar(nombre)) : null;
    }

    private static String recortar(String nombre) {
        return nombre.length() <= MAX_NOMBRE ? nombre : nombre.substring(0, MAX_NOMBRE);
    }

    private static String texto(DataSize tamano) {
        long b = tamano.toBytes();
        if (b % (1024 * 1024) == 0) {
            return (b / (1024 * 1024)) + " MB";
        }
        return b % 1024 == 0 ? (b / 1024) + " KB" : b + " bytes";
    }

    private static DetalleError detalle(String motivo) {
        return new DetalleError("archivo", motivo);
    }

    private static ExcepcionApi error(HttpStatus estado, String codigo, String mensaje, List<DetalleError> detalles) {
        return new ExcepcionApi(estado, new ErrorApi(codigo, mensaje, detalles));
    }

    private static MessageDigest nuevoDigest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
