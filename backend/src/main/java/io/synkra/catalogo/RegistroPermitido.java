package io.synkra.catalogo;

import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** R-C3 (H-31): registro de imágenes permitido; vacío significa que no se aplica. */
@Component
class RegistroPermitido {

    /** El host del registro es insensible a mayúsculas: se compara en minúsculas. */
    private static final Pattern FORMA = Pattern.compile(CatalogoServicio.REGISTRO);

    private final String permitido;

    RegistroPermitido(@Value("${synkra.registro-permitido:}") String valor) {
        var limpio = valor == null ? "" : valor.strip().toLowerCase(Locale.ROOT);
        if (!limpio.isEmpty() && !FORMA.matcher(limpio).matches()) {
            throw new IllegalStateException("synkra.registro-permitido (SYNKRA_REGISTRO_PERMITIDO) no es un registro válido: '"
                    + (valor.length() > 100 ? valor.substring(0, 100) + "..." : valor)
                    + "'; debe ser host[:puerto], por ejemplo localhost:5001");
        }
        this.permitido = limpio;
    }

    boolean activo() {
        return !permitido.isEmpty();
    }

    /** Verdadero si no hay valor configurado o si la imagen viene exactamente de ese registro. */
    boolean permite(String imagen) {
        if (!activo()) {
            return true;
        }
        int barra = imagen.indexOf('/');
        return barra > 0 && imagen.substring(0, barra).toLowerCase(Locale.ROOT).equals(permitido);
    }

    String valor() {
        return permitido;
    }
}
