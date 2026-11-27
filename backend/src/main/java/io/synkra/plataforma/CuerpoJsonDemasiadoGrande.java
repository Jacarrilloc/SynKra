package io.synkra.plataforma;

import java.io.IOException;
import java.util.List;

/** El cuerpo JSON de una petición pasó el tope (C-38); lo lanza el flujo acotado y {@link ManejadorErrores} lo hace 413. */
class CuerpoJsonDemasiadoGrande extends IOException {

    private final long limite;

    CuerpoJsonDemasiadoGrande(long limite) {
        super("El cuerpo JSON supera " + limite + " bytes");
        this.limite = limite;
    }

    ErrorApi error() {
        return new ErrorApi(CodigosError.DEMASIADO_GRANDE, CodigosError.MSG_DEMASIADO_GRANDE,
                List.of(new DetalleError(CodigosError.CAMPO_CUERPO, CodigosError.MOTIVO_JSON_GRANDE.formatted(limite))));
    }

    long limite() {
        return limite;
    }
}
