package io.synkra.plataforma;

import java.util.List;

/** Cuerpo unico de todo error de la API (H-24). {@code codigo} va en mayusculas. */
public record ErrorApi(String codigo, String mensaje, List<DetalleError> detalles) {

    public ErrorApi {
        detalles = detalles == null ? List.of() : List.copyOf(detalles);
    }
}
