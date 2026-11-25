package io.synkra.plataforma.prueba;

import io.synkra.plataforma.DetalleError;
import io.synkra.plataforma.ErrorApi;
import io.synkra.plataforma.ExcepcionApi;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** Controlador solo de prueba: provoca cada estado del formato de error. */
@RestController
public class ControladorDePrueba {

    @GetMapping("/prueba/400")
    void e400() {
        throw new ExcepcionApi(HttpStatus.BAD_REQUEST, new ErrorApi("SOLICITUD_INVALIDA", "Solicitud invalida",
                List.of(new DetalleError("pais", "obligatorio"))));
    }

    @GetMapping("/prueba/404")
    void e404() {
        throw new ExcepcionApi(HttpStatus.NOT_FOUND, new ErrorApi("NO_ENCONTRADO", "No existe", List.of()));
    }

    @GetMapping("/prueba/409")
    void e409() {
        throw new ExcepcionApi(HttpStatus.CONFLICT, new ErrorApi("CONFLICTO", "Ya existe", List.of()));
    }

    @GetMapping("/prueba/413")
    void e413() {
        throw new ExcepcionApi(HttpStatus.PAYLOAD_TOO_LARGE,
                new ErrorApi("DEMASIADO_GRANDE", "Archivo demasiado grande", List.of()));
    }

    @GetMapping("/prueba/422")
    void e422() {
        throw new ExcepcionApi(HttpStatus.UNPROCESSABLE_ENTITY, new ErrorApi("INCOMPATIBLE", "No compatible",
                List.of(new DetalleError("plugin", "version_core"), new DetalleError("core", "rango"))));
    }

    @GetMapping("/prueba/500")
    void e500() {
        throw new IllegalStateException("secreto interno");
    }

    @GetMapping("/prueba/tamano")
    void tamano() {
        throw new org.springframework.web.multipart.MaxUploadSizeExceededException(10);
    }

    @PostMapping("/prueba/json")
    Map<String, String> json(@RequestBody Map<String, String> cuerpo) {
        return cuerpo;
    }
}
