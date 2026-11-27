package io.synkra.catalogo;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.zip.ZipException;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;

/**
 * Lectura del fin del directorio central (EOCD) y comprobación de que las cabeceras locales coinciden con él,
 * sin descomprimir nada (R-C6). Un lector en flujo ve las cabeceras locales; el directorio central, uno de acceso
 * aleatorio. Si no dicen lo mismo, el JAR es ambiguo y se rechaza.
 */
final class EstructuraZip {

    /** Longitud máxima de un descriptor de datos (firma, CRC y dos tamaños de 8 bytes). */
    static final int DESCRIPTOR_MAX = 24;
    private static final int LOCAL = 0x04034b50;
    private static final int EOCD = 0x06054b50;
    private static final int LOCALIZADOR64 = 0x07064b50;
    private static final int EOCD64 = 0x06064b50;
    private static final int MAX_COMENTARIO = 65535;

    /** Entradas según el EOCD y dónde empieza el directorio central. */
    record Fin(long entradas, long inicioDirectorio) {}

    private EstructuraZip() {
    }

    /** @throws ZipException si no hay fin de directorio central (no es un ZIP legible). */
    static Fin leerFin(Path jar) throws IOException {
        try (var canal = FileChannel.open(jar, StandardOpenOption.READ)) {
            long tam = canal.size();
            int n = (int) Math.min(tam, 22L + MAX_COMENTARIO);
            var cola = leer(canal, tam - n, n);
            int p = -1;
            for (int i = n - 22; i >= 0; i--) {
                if (cola.getInt(i) == EOCD) {
                    p = i;
                    break;
                }
            }
            if (p < 0) {
                throw new ZipException("no se encuentra el fin del directorio central (central directory)");
            }
            long entradas = Short.toUnsignedLong(cola.getShort(p + 10));
            long inicio = Integer.toUnsignedLong(cola.getInt(p + 16));
            if (entradas == 0xFFFF || inicio == 0xFFFFFFFFL) {
                long posEocd = tam - n + p;
                if (posEocd < 20) {
                    throw new ZipException("localizador ZIP64 ausente (central directory)");
                }
                var loc = leer(canal, posEocd - 20, 20);
                if (loc.getInt(0) != LOCALIZADOR64) {
                    throw new ZipException("localizador ZIP64 ausente (central directory)");
                }
                var e64 = leer(canal, loc.getLong(8), 56);
                if (e64.getInt(0) != EOCD64) {
                    throw new ZipException("fin ZIP64 no válido (central directory)");
                }
                entradas = e64.getLong(32);
                inicio = e64.getLong(48);
            }
            return new Fin(entradas, inicio);
        }
    }

    /**
     * Devuelve el motivo del primer desajuste, o {@code null} si las cabeceras locales, en orden de posición,
     * coinciden con el directorio central: empieza en el byte 0, los nombres crudos son iguales y entre una entrada
     * y la siguiente solo cabe su descriptor de datos.
     */
    static String desajuste(Path jar, List<ZipArchiveEntry> entradas, long inicioDirectorio) throws IOException {
        var porPosicion = entradas.stream()
                .sorted(Comparator.comparingLong(ZipArchiveEntry::getLocalHeaderOffset)).toList();
        try (var canal = FileChannel.open(jar, StandardOpenOption.READ)) {
            long esperado = 0;
            var descriptorAntes = false;
            for (var en : porPosicion) {
                long pos = en.getLocalHeaderOffset();
                if (!huecoValido(pos - esperado, descriptorAntes)) {
                    return MotivosCatalogo.ESTRUCTURA_AMBIGUA;
                }
                int largoNombre;
                int largoExtra;
                try {
                    var cab = leer(canal, pos, 30);
                    if (cab.getInt(0) != LOCAL) {
                        return MotivosCatalogo.ESTRUCTURA_AMBIGUA;
                    }
                    largoNombre = Short.toUnsignedInt(cab.getShort(26));
                    largoExtra = Short.toUnsignedInt(cab.getShort(28));
                    var nombreLocal = new byte[largoNombre];
                    leer(canal, pos + 30, largoNombre).get(nombreLocal);
                    if (!Arrays.equals(nombreLocal, en.getRawName())) {
                        return MotivosCatalogo.ENTRADA_LOCAL_DISTINTA.formatted(
                                RecepcionArtefacto.recortar(en.getName()));
                    }
                } catch (EOFException fuera) {
                    return MotivosCatalogo.ESTRUCTURA_AMBIGUA;
                }
                esperado = pos + 30 + largoNombre + largoExtra + Math.max(0, en.getCompressedSize());
                descriptorAntes = en.getGeneralPurposeBit().usesDataDescriptor();
            }
            return huecoValido(inicioDirectorio - esperado, descriptorAntes) ? null : MotivosCatalogo.ESTRUCTURA_AMBIGUA;
        }
    }

    /** Sin descriptor de datos no cabe nada entre dos entradas; con él, hasta {@link #DESCRIPTOR_MAX} bytes. */
    private static boolean huecoValido(long hueco, boolean descriptorAntes) {
        return hueco == 0 || (descriptorAntes && hueco > 0 && hueco <= DESCRIPTOR_MAX);
    }

    private static ByteBuffer leer(FileChannel canal, long pos, int n) throws IOException {
        var b = ByteBuffer.allocate(n).order(ByteOrder.LITTLE_ENDIAN);
        if (pos < 0) {
            throw new EOFException();
        }
        while (b.hasRemaining()) {
            int r = canal.read(b, pos + b.position());
            if (r < 0) {
                throw new EOFException();
            }
        }
        return b.flip();
    }
}
