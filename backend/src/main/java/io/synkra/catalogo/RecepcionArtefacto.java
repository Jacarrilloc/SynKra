package io.synkra.catalogo;

import io.synkra.plataforma.CodigosError;
import io.synkra.plataforma.DetalleError;
import io.synkra.plataforma.ErrorApi;
import io.synkra.plataforma.ExcepcionApi;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.zip.ZipException;
import org.apache.commons.compress.archivers.zip.UnicodePathExtraField;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.springframework.beans.factory.annotation.Autowired;
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

    private static final Charset CP437 = Charset.forName("IBM437");
    private static final int BUFFER = 8192;

    private final Path temporales;
    private final DataSize maximo;
    private final int maxEntradas;
    private final Supplier<MessageDigest> digest;

    @Autowired
    RecepcionArtefacto(@Value("${synkra.data}") String datos, @Value("${synkra.max-artefacto}") DataSize maximo,
            @Value("${synkra.max-entradas-artefacto}") int maxEntradas) {
        this(datos, maximo, maxEntradas, RecepcionArtefacto::nuevoDigest);
    }

    /** Para las pruebas: permite observar cómo se alimenta el digest. */
    RecepcionArtefacto(String datos, DataSize maximo, int maxEntradas, Supplier<MessageDigest> digest) {
        this.temporales = Path.of(datos).resolve("artefactos").resolve(".tmp");
        this.maximo = maximo;
        this.maxEntradas = maxEntradas;
        this.digest = digest;
    }

    ArtefactoRecibido recibir(InputStream entrada) throws IOException {
        Files.createDirectories(temporales);
        try {
            Files.setPosixFilePermissions(temporales, PosixFilePermissions.fromString("rwx------"));
        } catch (UnsupportedOperationException sinPosix) {
            // sistema de archivos sin permisos POSIX: no hay nada que fijar
        }
        var temporal = Files.createTempFile(temporales, "recepcion-", ".jar");
        var recibido = false;
        try {
            var resumen = digest.get();
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
        var buffer = new byte[BUFFER];
        try (var in = new DigestInputStream(entrada, resumen); var out = Files.newOutputStream(destino)) {
            int n;
            // nunca se pide más que el byte límite + 1: ahí se corta, sin drenar el flujo
            while ((n = in.read(buffer, 0, (int) Math.min(BUFFER, limite + 1 - total))) != -1) {
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
        String desajuste;
        try {
            var fin = EstructuraZip.leerFin(jar);
            if (fin.entradas() > maxEntradas) {
                throw demasiadasEntradas();
            }
            // Sin los extras Unicode: el nombre es el crudo, el que ven java.util.zip y los extractores.
            try (var zip = ZipFile.builder().setPath(jar).setCharset(CP437).setUseUnicodeExtraFields(false).get()) {
                var entradas = Collections.list(zip.getEntries());
                if (entradas.size() > maxEntradas) {
                    throw demasiadasEntradas();
                }
                for (var en : entradas) {
                    var motivo = motivoDeRechazo(en.getName());
                    if (motivo == null && en.isUnixSymlink()) {
                        motivo = MotivosCatalogo.ENTRADA_ENLACE.formatted(recortar(en.getName()));
                    }
                    if (motivo == null && unicodeDistinto(en)) {
                        motivo = MotivosCatalogo.ENTRADA_UNICODE_DISTINTA.formatted(recortar(en.getName()));
                    }
                    if (motivo != null && culpables.size() < MAX_DETALLES) {
                        culpables.add(detalle(motivo));
                    }
                }
                desajuste = culpables.isEmpty() ? EstructuraZip.desajuste(jar, entradas, fin.inicioDirectorio()) : null;
            }
        } catch (IOException e) {
            if (esFormatoInvalido(e)) {
                throw error(HttpStatus.UNPROCESSABLE_ENTITY, CodigosCatalogo.ARTEFACTO_INVALIDO,
                        CodigosError.MSG_ENTIDAD_NO_PROCESABLE, List.of(detalle(MotivosCatalogo.ARTEFACTO_NO_ES_ZIP)));
            }
            throw e; // E/S del servidor: sube como error interno, con su causa
        }
        if (desajuste != null) {
            culpables.add(detalle(desajuste));
        }
        if (!culpables.isEmpty()) {
            throw error(HttpStatus.UNPROCESSABLE_ENTITY, CodigosCatalogo.RUTA_NO_PERMITIDA,
                    CodigosError.MSG_ENTIDAD_NO_PROCESABLE, culpables);
        }
    }

    private ExcepcionApi demasiadasEntradas() {
        return error(HttpStatus.UNPROCESSABLE_ENTITY, CodigosCatalogo.ARTEFACTO_INVALIDO,
                CodigosError.MSG_ENTIDAD_NO_PROCESABLE,
                List.of(detalle(MotivosCatalogo.DEMASIADAS_ENTRADAS.formatted(maxEntradas))));
    }

    /** Formato inválido (culpa del cliente): ZIP mal formado o truncado. Cualquier otra E/S es del servidor. */
    static boolean esFormatoInvalido(IOException e) {
        // commons envuelve los fallos de formato en un IOException genérico: se mira toda la cadena de causas
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof ZipException || t instanceof EOFException
                    || (t.getMessage() != null && t.getMessage().toLowerCase().contains("central directory"))) {
                return true;
            }
        }
        return false;
    }

    /** El extra 0x7075 dice otro nombre que el crudo: otros lectores verían un nombre distinto. */
    private static boolean unicodeDistinto(org.apache.commons.compress.archivers.zip.ZipArchiveEntry en) {
        return en.getExtraField(UnicodePathExtraField.UPATH_ID) instanceof UnicodePathExtraField u
                && !new String(u.getUnicodeName(), StandardCharsets.UTF_8).equals(en.getName());
    }

    private static String motivoDeRechazo(String nombre) {
        var normal = nombre.replace('\\', '/');
        var malo = nombre.isEmpty() || normal.startsWith("/") || UNIDAD.matcher(normal).matches()
                || nombre.indexOf('\0') >= 0 || List.of(normal.split("/")).contains("..");
        return malo ? MotivosCatalogo.ENTRADA_NO_PERMITIDA.formatted(recortar(nombre)) : null;
    }

    static String recortar(String nombre) {
        return nombre.length() <= MAX_NOMBRE ? nombre : nombre.substring(0, MAX_NOMBRE);
    }

    private static String texto(DataSize tamano) {
        long b = tamano.toBytes();
        if (b % (1024 * 1024) == 0) {
            return (b / (1024 * 1024)) + " " + MotivosCatalogo.UNIDAD_MB;
        }
        return b % 1024 == 0 ? (b / 1024) + " " + MotivosCatalogo.UNIDAD_KB : b + " " + MotivosCatalogo.UNIDAD_BYTES;
    }

    private static DetalleError detalle(String motivo) {
        return new DetalleError("archivo", motivo);
    }

    private static ExcepcionApi error(HttpStatus estado, String codigo, String mensaje, List<DetalleError> detalles) {
        return new ExcepcionApi(estado, new ErrorApi(codigo, mensaje, detalles));
    }

    static MessageDigest nuevoDigest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
