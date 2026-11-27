package io.synkra.plataforma;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.AbstractJackson2HttpMessageConverter;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdviceAdapter;

/**
 * C-38: Jackson deja de leer al cerrar el objeto, y lo que sigue (relleno) lo drena el contenedor sin cuenta. Tras
 * deserializar se consume el resto a través del flujo acotado de {@link LimiteCuerpoJson}: como mucho
 * {@code tope + 1} bytes en total, y si hay más del tope, 413.
 */
@ControllerAdvice
class LimiteCuerpoJsonAvance extends RequestBodyAdviceAdapter {

    private static final int BUFFER = 8192;

    @Override
    public boolean supports(MethodParameter parametro, Type tipo, Class<? extends HttpMessageConverter<?>> conversor) {
        return AbstractJackson2HttpMessageConverter.class.isAssignableFrom(conversor);
    }

    @Override
    public Object afterBodyRead(Object cuerpo, HttpInputMessage entrada, MethodParameter parametro, Type tipo,
            Class<? extends HttpMessageConverter<?>> conversor) {
        try {
            InputStream flujo = entrada.getBody();
            var buffer = new byte[BUFFER];
            while (flujo.read(buffer) != -1) {
                // solo se cuenta: el flujo acotado lanza al pasar el tope
            }
        } catch (CuerpoJsonDemasiadoGrande grande) {
            throw new ExcepcionApi(HttpStatus.PAYLOAD_TOO_LARGE, grande.error());
        } catch (IOException e) {
            // el cliente cortó o el flujo falló: lo que ya se leyó es el objeto; el contenedor decide el resto
        }
        return cuerpo;
    }
}
