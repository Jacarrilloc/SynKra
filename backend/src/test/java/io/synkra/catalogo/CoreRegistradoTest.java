package io.synkra.catalogo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.synkra.catalogo.api.CoreRegistrado;
import io.synkra.verificador.Version;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Invariante de {@link CoreRegistrado}: mismas versiones en la lista y en el mapa, e inmutable. */
class CoreRegistradoTest {

    private static final Version V1 = new Version(1, 0, 0);
    private static final Version V2 = new Version(2, 0, 0);

    @Test
    void aceptaVersionesEImagenesCoincidentes() {
        var c = new CoreRegistrado("a", List.of(V1, V2), Map.of(V1, "i1", V2, "i2"));
        assertThat(c.imagenes()).containsEntry(V1, "i1").containsEntry(V2, "i2");
    }

    @Test
    void rechazaUnaVersionSinImagen() {
        assertThatThrownBy(() -> new CoreRegistrado("a", List.of(V1, V2), Map.of(V1, "i1")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rechazaUnaImagenSinVersion() {
        assertThatThrownBy(() -> new CoreRegistrado("a", List.of(V1), Map.of(V1, "i1", V2, "i2")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void elMapaEsInmutableYNoSeAlteraDesdeFuera() {
        var origen = new HashMap<Version, String>(Map.of(V1, "i1"));
        var c = new CoreRegistrado("a", List.of(V1), origen);
        origen.put(V2, "i2");
        assertThat(c.imagenes()).hasSize(1);
        assertThatThrownBy(() -> c.imagenes().put(V2, "i2")).isInstanceOf(UnsupportedOperationException.class);
    }
}
