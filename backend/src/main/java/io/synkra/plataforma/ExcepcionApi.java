package io.synkra.plataforma;

import org.springframework.http.HttpStatus;

/** Lo que lanzan los controladores: estado HTTP mas el cuerpo {@link ErrorApi}. */
public class ExcepcionApi extends RuntimeException {

    private final HttpStatus estado;
    private final ErrorApi error;

    public ExcepcionApi(HttpStatus estado, ErrorApi error) {
        super(error.mensaje());
        this.estado = estado;
        this.error = error;
    }

    public HttpStatus estado() {
        return estado;
    }

    public ErrorApi error() {
        return error;
    }
}
