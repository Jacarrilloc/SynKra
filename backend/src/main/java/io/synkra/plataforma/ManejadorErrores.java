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
        return respuesta(HttpStatus.INTERNAL_SERVER_ERROR,
                new ErrorApi("ERROR_INTERNO", "Error interno del servidor", List.of()));
    }

    /** Las excepciones propias de Spring (cuerpo ilegible, metodo, ruta...) pasan por aqui. */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
            HttpStatusCode statusCode, WebRequest request) {
        HttpStatus estado = HttpStatus.valueOf(statusCode.value());
        if (estado.is5xxServerError()) {
            LOG.error("Error de Spring", ex);
        }
        ErrorApi error = new ErrorApi(codigoDe(estado), mensajeDe(estado), List.of());
        return respuesta(estado, error, headers);
    }

    private static String codigoDe(HttpStatus estado) {
        return switch (estado) {
            case NOT_FOUND -> "NO_ENCONTRADO";
            case CONFLICT -> "CONFLICTO";
            case PAYLOAD_TOO_LARGE -> "DEMASIADO_GRANDE";
            case UNPROCESSABLE_ENTITY -> "ENTIDAD_NO_PROCESABLE";
            case INTERNAL_SERVER_ERROR -> "ERROR_INTERNO";
            default -> estado.is4xxClientError() ? "SOLICITUD_INVALIDA" : "ERROR_INTERNO";
        };
    }

    private static String mensajeDe(HttpStatus estado) {
        return switch (estado) {
            case NOT_FOUND -> "Recurso no encontrado";
            case PAYLOAD_TOO_LARGE -> "El archivo supera el tamano permitido";
            case METHOD_NOT_ALLOWED -> "Metodo no permitido";
            case UNSUPPORTED_MEDIA_TYPE -> "Tipo de contenido no soportado";
            case BAD_REQUEST -> "La solicitud es invalida o su cuerpo no se puede leer";
            case INTERNAL_SERVER_ERROR -> "Error interno del servidor";
            default -> estado.is4xxClientError() ? "Solicitud invalida" : "Error interno del servidor";
        };
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
