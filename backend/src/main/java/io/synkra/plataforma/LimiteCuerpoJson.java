package io.synkra.plataforma;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.unit.DataSize;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * C-38: tope de todo cuerpo JSON de la API, sin mirar la ruta (la URI cruda admite {@code ;x=1} y percent-encoding). El flujo se corta al leer el byte tope + 1, sin leer
 * el resto, y {@link ManejadorErrores} lo traduce a un 413 con {@link ErrorApi}. No toca los cuerpos multipart, que
 * acota la configuración de multipart.
 */
@Component
class LimiteCuerpoJson extends OncePerRequestFilter {

    private final long limite;
    private final ObjectMapper json;

    LimiteCuerpoJson(@Value("${synkra.max-cuerpo-json}") DataSize limite, ObjectMapper json) {
        this.limite = limite.toBytes();
        this.json = json;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        var tipo = request.getContentType();
        boolean json = false;
        if (tipo != null) {
            try {
                var t = MediaType.parseMediaType(tipo);
                json = t.isCompatibleWith(MediaType.APPLICATION_JSON) || t.getSubtype().endsWith("+json");
            } catch (RuntimeException tipoRoto) {
                json = false;
            }
        }
        return !json;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (request.getContentLengthLong() > limite) {
            // el tamaño declarado ya lo pasa: se responde sin leer nada del cuerpo
            response.setStatus(HttpStatus.PAYLOAD_TOO_LARGE.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setHeader(HttpHeaders.CONNECTION, "close");
            json.writeValue(response.getOutputStream(), new CuerpoJsonDemasiadoGrande(limite).error());
            return;
        }
        chain.doFilter(new Acotada(request, limite), response);
    }

    private static final class Acotada extends HttpServletRequestWrapper {

        private final long limite;
        private ServletInputStream flujo;

        Acotada(HttpServletRequest request, long limite) {
            super(request);
            this.limite = limite;
        }

        @Override
        public ServletInputStream getInputStream() throws IOException {
            if (flujo == null) {
                flujo = new FlujoAcotado(super.getInputStream(), limite);
            }
            return flujo;
        }

        @Override
        public BufferedReader getReader() throws IOException {
            var nombre = getCharacterEncoding();
            Charset c = nombre == null ? StandardCharsets.UTF_8 : Charset.forName(nombre);
            return new BufferedReader(new InputStreamReader(getInputStream(), c));
        }
    }

    static final class FlujoAcotado extends ServletInputStream {

        private final ServletInputStream origen;
        private final long limite;
        private long leidos;

        FlujoAcotado(ServletInputStream origen, long limite) {
            this.origen = origen;
            this.limite = limite;
        }

        @Override
        public int read() throws IOException {
            int b = origen.read();
            if (b != -1 && ++leidos > limite) {
                throw new CuerpoJsonDemasiadoGrande(limite);
            }
            return b;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            if (len == 0) {
                return 0;
            }
            // nunca se pide más que el byte tope + 1: ahí se corta, sin drenar el resto
            int n = origen.read(b, off, (int) Math.min(len, limite + 1 - leidos));
            if (n > 0) {
                leidos += n;
                if (leidos > limite) {
                    throw new CuerpoJsonDemasiadoGrande(limite);
                }
            }
            return n;
        }

        @Override
        public boolean isFinished() {
            return origen.isFinished();
        }

        @Override
        public boolean isReady() {
            return origen.isReady();
        }

        @Override
        public void setReadListener(ReadListener listener) {
            origen.setReadListener(listener);
        }
    }
}
