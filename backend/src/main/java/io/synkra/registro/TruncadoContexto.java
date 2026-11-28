package io.synkra.registro;

import java.nio.charset.StandardCharsets;

/** R-R4: últimas 200 líneas del log del core y cada texto a 64 KB (UTF-8) como máximo, con la marca. */
final class TruncadoContexto {

    static final int LINEAS_LOG_CORE = 200;
    static final int BYTES_MAX = 64 * 1024;
    static final String MARCA = "[truncado]";

    private TruncadoContexto() {}

    /** Se queda con las últimas 200 líneas (cada una conserva su salto) y aplica {@link #texto(String)}. */
    static String logCore(String log) {
        if (log == null) {
            return "";
        }
        int limite = log.endsWith("\n") ? log.length() - 1 : log.length();
        int inicio = 0;
        int saltos = 0;
        for (int i = limite - 1; i >= 0; i--) {
            if (log.charAt(i) == '\n' && ++saltos == LINEAS_LOG_CORE) {
                inicio = i + 1;
                break;
            }
        }
        return texto(log.substring(inicio));
    }

    /** Si pasa de 64 KB: {@code MARCA + "\n"} y el final del texto; todo junto, 65 536 bytes como máximo. */
    static String texto(String texto) {
        if (texto == null) {
            return "";
        }
        byte[] bytes = texto.getBytes(StandardCharsets.UTF_8);
        if (bytes.length <= BYTES_MAX) {
            return texto;
        }
        String prefijo = MARCA + "\n";
        int cupo = BYTES_MAX - prefijo.getBytes(StandardCharsets.UTF_8).length;
        int inicio = bytes.length - cupo;
        while ((bytes[inicio] & 0xC0) == 0x80) { // no empezar en mitad de un carácter multibyte
            inicio++;
        }
        return prefijo + new String(bytes, inicio, bytes.length - inicio, StandardCharsets.UTF_8);
    }
}
