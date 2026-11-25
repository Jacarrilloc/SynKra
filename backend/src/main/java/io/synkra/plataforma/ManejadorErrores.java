package io.synkra.plataforma;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/** Convierte toda excepcion al formato {@link ErrorApi}, sin trazas en el cuerpo. */
@RestControllerAdvice
public class ManejadorErrores extends ResponseEntityExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ManejadorErrores.class);

    @ExceptionHandler(ExcepcionApi.class)
    ResponseEntity<Object> deApi(ExcepcionApi e) {
        return respuesta(e.estado(), e.error());
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<Object> imprevista(Exception e) {
        LOG.error("Error no previsto", e);
        return respuesta(HttpStatus.INTERNAL_SERVER_ERROR, errorInterno());
    }

    /** Las excepciones propias de Spring (cuerpo ilegible, metodo, ruta...) pasan por aqui. */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
            HttpStatusCode statusCode, WebRequest request) {
        HttpStatus estado = HttpStatus.resolve(statusCode.value());
        if (estado == null) {
            estado = statusCode.is4xxClientError() ? HttpStatus.BAD_REQUEST : HttpStatus.INTERNAL_SERVER_ERROR;
        }
        if (estado.is5xxServerError()) {
            LOG.error("Error de Spring", ex);
        }
        return respuesta(estado, errorDe(estado), headers);
    }

    private static ErrorApi errorDe(HttpStatus estado) {
        return switch (estado) {
            case NOT_FOUND -> sinDetalles(CodigosError.NO_ENCONTRADO, CodigosError.MSG_NO_ENCONTRADO);
            case METHOD_NOT_ALLOWED ->
                sinDetalles(CodigosError.METODO_NO_PERMITIDO, CodigosError.MSG_METODO_NO_PERMITIDO);
            case CONFLICT -> sinDetalles(CodigosError.CONFLICTO, CodigosError.MSG_CONFLICTO);
            case PAYLOAD_TOO_LARGE -> sinDetalles(CodigosError.DEMASIADO_GRANDE, CodigosError.MSG_DEMASIADO_GRANDE);
            case UNSUPPORTED_MEDIA_TYPE ->
                sinDetalles(CodigosError.TIPO_NO_SOPORTADO, CodigosError.MSG_TIPO_NO_SOPORTADO);
            case UNPROCESSABLE_ENTITY ->
                sinDetalles(CodigosError.ENTIDAD_NO_PROCESABLE, CodigosError.MSG_ENTIDAD_NO_PROCESABLE);
            default -> estado.is4xxClientError()
                    ? sinDetalles(CodigosError.SOLICITUD_INVALIDA, CodigosError.MSG_SOLICITUD_INVALIDA)
                    : errorInterno();
        };
    }

    private static ErrorApi errorInterno() {
        return sinDetalles(CodigosError.ERROR_INTERNO, CodigosError.MSG_ERROR_INTERNO);
    }

    private static ErrorApi sinDetalles(String codigo, String mensaje) {
        return new ErrorApi(codigo, mensaje, List.of());
    }

    private static ResponseEntity<Object> respuesta(HttpStatus estado, ErrorApi error) {
        return respuesta(estado, error, new HttpHeaders());
    }

    private static ResponseEntity<Object> respuesta(HttpStatus estado, ErrorApi error, HttpHeaders cabeceras) {
        HttpHeaders h = new HttpHeaders();
        h.putAll(cabeceras);
        h.setContentType(MediaType.APPLICATION_JSON);
        return new ResponseEntity<>(error, h, estado);
    }
}
