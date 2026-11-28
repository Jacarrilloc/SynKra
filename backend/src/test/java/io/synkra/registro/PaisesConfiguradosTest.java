package io.synkra.registro;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

/** R-R7 y decisión 3: {@code synkra.paises} se valida con las mismas reglas que {@code PaisPermitido} (U07-A). */
class PaisesConfiguradosTest {

    @Test
    void separaPorComaYRecortaLosEspacios() {
        assertThat(new PaisesConfigurados("co, pe").lista()).containsExactly("co", "pe");
    }

    @Test
    void conservaElOrdenDeLaPropiedad() {
        assertThat(new PaisesConfigurados("pe,co,ar").lista()).containsExactly("pe", "co", "ar");
    }

    @Test
    void unSoloPaisEsValido() {
        assertThat(new PaisesConfigurados("pe").lista()).containsExactly("pe");
    }

    @Test
    void valoresInvalidosImpidenArrancarSinRepetirLoRecibido() {
        for (var malo : List.of("", "co,,pe", "co,co", "CO", "col", "c")) {
            assertThatThrownBy(() -> new PaisesConfigurados(malo)).as("[" + malo + "]")
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("synkra.paises (SYNKRA_PAISES)");
            if (malo.length() > 1) {
                assertThatThrownBy(() -> new PaisesConfigurados(malo))
                        .hasMessageNotContaining(malo);
            }
        }
    }

    @Test
    void laListaEsInmutable() {
        var lista = new PaisesConfigurados("co,pe").lista();
        assertThatThrownBy(() -> lista.add("ar")).isInstanceOf(UnsupportedOperationException.class);
    }
}
