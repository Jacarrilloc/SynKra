package io.synkra.registro;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Países de {@code synkra.paises} (R-R7), en el orden de la propiedad. Misma regla que {@code PaisPermitido} de
 * despliegue (U07-A), repetida a propósito para que {@code registro} no dependa de ese módulo.
 */
@Component
class PaisesConfigurados {

    private static final Pattern PAIS = Pattern.compile("[a-z]{2}");
    private static final String MENSAJE = "synkra.paises (SYNKRA_PAISES) inválida: debe tener al menos un país de dos "
            + "letras en minúscula, separados por coma, sin entradas vacías ni repetidas";

    private final List<String> paises;

    PaisesConfigurados(@Value("${synkra.paises}") String valor) {
        if (valor == null) {
            throw new IllegalStateException(MENSAJE);
        }
        var lista = new ArrayList<String>();
        var vistos = new HashSet<String>();
        for (var entrada : valor.split(",", -1)) {
            var pais = entrada.strip();
            if (!PAIS.matcher(pais).matches() || !vistos.add(pais)) {
                throw new IllegalStateException(MENSAJE);
            }
            lista.add(pais);
        }
        this.paises = List.copyOf(lista);
    }

    /** Los países permitidos, en el orden de la propiedad (sin ordenarlos). */
    List<String> lista() {
        return paises;
    }
}
