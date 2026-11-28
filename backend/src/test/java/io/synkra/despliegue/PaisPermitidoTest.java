package io.synkra.despliegue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;

import io.synkra.kubernetes.api.AdaptadorKubernetes;
import io.synkra.plataforma.ExcepcionApi;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/** U07-T02, R-D1: país permitido, sin ninguna llamada a U05. */
@SpringBootTest
class PaisPermitidoTest {

    private static final String FORMA = "El país debe ser un código de dos letras en minúscula (S4)";
    private static final String LISTA = "El país no está entre los configurados (SYNKRA_PAISES)";

    @TempDir
    static Path datos;

    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry r) {
        r.add("synkra.data", () -> datos.toString());
        r.add("synkra.paises", () -> "co,pe");
    }

    @MockitoBean AdaptadorKubernetes adaptador;
    @Autowired PaisPermitido paises;

    @Test
    void aceptaLosPaisesConfiguradosYDerivaElNamespace() {
        assertThat(paises.exigir("co")).isEqualTo("co");
        assertThat(paises.exigir("pe")).isEqualTo("pe");
        assertThat(paises.namespace("co")).isEqualTo("synkra-co");
        assertThat(paises.namespace("pe")).isEqualTo("synkra-pe");
        verifyNoInteractions(adaptador);
    }

    private void rechazado(String pais, String motivo) {
        assertThatThrownBy(() -> paises.exigir(pais)).isInstanceOfSatisfying(ExcepcionApi.class, e -> verifica(e, pais, motivo));
        assertThatThrownBy(() -> paises.namespace(pais)).isInstanceOfSatisfying(ExcepcionApi.class, e -> verifica(e, pais, motivo));
    }

    private void verifica(ExcepcionApi e, String pais, String motivo) {
        assertThat(e.estado()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(e.error().codigo()).isEqualTo("PAIS_NO_PERMITIDO");
        assertThat(e.error().detalles()).hasSize(1);
        var d = e.error().detalles().get(0);
        assertThat(d.campo()).isEqualTo("pais");
        assertThat(d.motivo()).isEqualTo(motivo);
        // El motivo es una constante exacta (arriba): no repite lo recibido (C-34). Los largos, además, se comprueban.
        if (pais != null && pais.strip().length() > 3) {
            assertThat(d.motivo()).doesNotContain(pais.strip());
            assertThat(e.error().mensaje()).doesNotContain(pais.strip());
        }
    }

    @Test
    void rechazaUnPaisConFormaValidaFueraDeLaLista() {
        rechazado("mx", LISTA);
    }

    @Test
    void rechazaTodoLoQueNoTieneLaForma() {
        for (var p : new String[] {"default", "kube-system", "PE", "synkra-co", "c", "col", " co", "", "сo", "cс"}) {
            rechazado(p, FORMA);
        }
        rechazado(null, FORMA);
    }

    @Test
    void nadaDeLoAnteriorLlamaAlAdaptadorDeKubernetes() {
        for (var p : List.of("co", "pe", "mx", "PE", " co", "default")) {
            try {
                paises.namespace(p);
            } catch (ExcepcionApi ignorada) {
                // esperado en los rechazados
            }
        }
        verifyNoInteractions(adaptador);
    }

    @Test
    void laListaAceptaEspaciosAlrededorDeCadaEntrada() {
        assertThat(new PaisPermitido("co, pe").exigir("pe")).isEqualTo("pe");
    }

    @Test
    void unaListaInvalidaFallaAlArrancarNombrandoLaPropiedad() {
        for (var v : new String[] {"", "co,,pe", "co,co", "CO", "col", null}) {
            assertThatThrownBy(() -> new PaisPermitido(v)).isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("SYNKRA_PAISES").hasMessageContaining("synkra.paises");
        }
    }
}
