package io.synkra.plataforma;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.synkra.plataforma.prueba.ControladorDePrueba;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** C-07: las validaciones de Spring pasan a {@code detalles} con campo y motivo reales. */
@WebMvcTest(ControladorDePrueba.class)
@Import(ManejadorErrores.class)
class ValidacionErroresTest {

    @Autowired
    MockMvc mvc;

    private ResultActions json(String cuerpo) throws Exception {
        return mvc.perform(post("/prueba/valido").contentType(MediaType.APPLICATION_JSON).content(cuerpo));
    }

    private ResultActions unico(ResultActions r, String campo, String motivo) throws Exception {
        return r.andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value(CodigosError.ENTIDAD_NO_PROCESABLE))
                .andExpect(jsonPath("$.mensaje").value(CodigosError.MSG_ENTIDAD_NO_PROCESABLE))
                .andExpect(jsonPath("$.detalles", hasSize(1)))
                .andExpect(jsonPath("$.detalles[0].campo").value(campo))
                .andExpect(jsonPath("$.detalles[0].motivo").value(motivo));
    }

    @Test
    void losTextosDeRespaldoQuedanFijados() {
        org.assertj.core.api.Assertions.assertThat(CodigosError.MOTIVO_OBLIGATORIO).isEqualTo("El campo es obligatorio");
        org.assertj.core.api.Assertions.assertThat(CodigosError.MOTIVO_TIPO_INVALIDO)
                .isEqualTo("El valor no tiene el tipo esperado");
        org.assertj.core.api.Assertions.assertThat(CodigosError.PARAMETRO_SIN_NOMBRE).isEqualTo("parametro");
    }

    @Test
    void cuerpoValidadoSinCampoObligatorioDa422ConCampoYMotivo() throws Exception {
        unico(json("{\"cantidad\":3}"), "nombre", "El campo es obligatorio");
    }

    @Test
    void cuerpoValidadoConDosFallosLosDevuelveTodos() throws Exception {
        json("{\"cantidad\":0}").andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detalles", hasSize(2)))
                .andExpect(jsonPath("$.detalles[?(@.campo=='nombre')].motivo").value("El campo es obligatorio"))
                .andExpect(jsonPath("$.detalles[?(@.campo=='cantidad')].motivo").value("debe ser al menos 1"));
    }

    @Test
    void parametroConRestriccionDa422ConElNombreDelParametro() throws Exception {
        unico(mvc.perform(get("/prueba/param").param("n", "0")), "n", "debe ser al menos 1");
    }

    @Test
    void parametroConMensajePorDefectoUsaElDeBeanValidation() throws Exception {
        mvc.perform(get("/prueba/param-defecto").param("m", "1")).andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detalles", hasSize(1)))
                .andExpect(jsonPath("$.detalles[0].campo").value("m"))
                .andExpect(jsonPath("$.detalles[0].motivo").value(org.hamcrest.Matchers.containsString("5")));
    }

    @Test
    void tipoEquivocadoDa422ConLaRutaDelCampo() throws Exception {
        unico(json("{\"nombre\":[\"a\"]}"), "nombre", "El valor no tiene el tipo esperado");
        unico(json("{\"nombre\":{\"a\":1}}"), "nombre", "El valor no tiene el tipo esperado");
        unico(json("{\"nombre\":\"x\",\"cantidad\":\"abc\"}"), "cantidad", "El valor no tiene el tipo esperado");
    }

    @Test
    void unErrorDeTipoEnLaRaizNoTieneCampoYDa400() throws Exception {
        for (var cuerpo : new String[] {"[]", "\"x\"", "5"}) {
            json(cuerpo).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.codigo").value(CodigosError.SOLICITUD_INVALIDA))
                    .andExpect(jsonPath("$.detalles", hasSize(0)));
        }
    }

    @Test
    void jsonMalformadoSigueDando400() throws Exception {
        json("{\"nombre\":").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value(CodigosError.SOLICITUD_INVALIDA));
    }
}
