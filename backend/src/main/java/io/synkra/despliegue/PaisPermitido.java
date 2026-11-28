package io.synkra.despliegue;

import io.synkra.plataforma.DetalleError;
import io.synkra.plataforma.ErrorApi;
import io.synkra.plataforma.ExcepcionApi;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * R-D1: el país debe ser de dos letras minúsculas (S4) y estar en {@code SYNKRA_PAISES}; el namespace es siempre
 * {@code synkra-<país>}. No normaliza (ni mayúsculas ni espacios) y no llama a U05.
 */
@Component
class PaisPermitido {

    private static final Pattern FORMA = Pattern.compile("[a-z]{2}");

    private final Set<String> lista;

    PaisPermitido(@Value("${synkra.paises}") String valor) {
        var paises = new LinkedHashSet<String>();
        if (valor != null) {
            for (var entrada : valor.split(",", -1)) {
                var pais = entrada.strip();
                if (!FORMA.matcher(pais).matches() || !paises.add(pais)) {
                    throw invalida();
                }
            }
        }
        if (paises.isEmpty()) {
            throw invalida();
        }
        this.lista = Set.copyOf(paises);
    }

    private static IllegalStateException invalida() {
        return new IllegalStateException("synkra.paises (SYNKRA_PAISES) debe ser una lista, separada por comas, de "
                + "códigos de dos letras en minúscula, sin vacíos ni repetidos, por ejemplo co,pe");
    }

    /** Devuelve el país si está permitido; si no, 422 {@code PAIS_NO_PERMITIDO}. */
    String exigir(String pais) {
        if (pais == null || !FORMA.matcher(pais).matches()) {
            throw rechazo(MotivosDespliegue.PAIS_FORMA);
        }
        if (!lista.contains(pais)) {
            throw rechazo(MotivosDespliegue.PAIS_FUERA_DE_LISTA);
        }
        return pais;
    }

    /** El único modo de armar el namespace de un país. */
    String namespace(String pais) {
        return "synkra-" + exigir(pais);
    }

    private static ExcepcionApi rechazo(String motivo) {
        return new ExcepcionApi(HttpStatus.UNPROCESSABLE_ENTITY, new ErrorApi(CodigosDespliegue.PAIS_NO_PERMITIDO,
                MotivosDespliegue.PAIS_MENSAJE, List.of(new DetalleError("pais", motivo))));
    }
}
