package io.synkra.despliegue;

import io.synkra.plataforma.DetalleError;
import io.synkra.plataforma.ErrorApi;
import io.synkra.plataforma.ExcepcionApi;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;

/**
 * R-D8: quién crea y quién confirma un plan. Sin autenticación (U10 se cortó): llega en la cabecera opcional
 * {@code X-Synkra-Usuario}; es un marcador de identidad, no un control de acceso.
 */
final class Usuario {

    static final String CABECERA = "X-Synkra-Usuario";
    static final String ANONIMO = "anonimo";

    private static final Pattern FORMA = Pattern.compile("[a-z0-9][a-z0-9._-]{0,63}");

    private Usuario() {
    }

    /** Sin cabecera (nula): {@code anonimo}. Con cabecera, debe cumplir la forma; si no, 422 sin repetir lo recibido. */
    static String de(String cabecera) {
        if (cabecera == null) {
            return ANONIMO;
        }
        if (!FORMA.matcher(cabecera).matches()) {
            throw new ExcepcionApi(HttpStatus.UNPROCESSABLE_ENTITY, new ErrorApi(CodigosDespliegue.USUARIO_INVALIDO,
                    MotivosDespliegue.USUARIO_MENSAJE, List.of(new DetalleError(CABECERA, MotivosDespliegue.USUARIO_FORMA))));
        }
        return cabecera;
    }
}
