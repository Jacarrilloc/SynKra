package io.synkra.plataforma;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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

@WebMvcTest(ControladorDePrueba.class)
@Import(ManejadorErrores.class)
class ErrorApiTest {

    @Autowired
    MockMvc mvc;

    private ResultActions formato(String ruta, int estado, String codigo) throws Exception {
        return mvc.perform(get(ruta))
                .andExpect(status().is(estado))
                .andExpect(jsonPath("$.codigo").value(codigo))
                .andExpect(jsonPath("$.mensaje").isString())
                .andExpect(jsonPath("$.detalles").isArray());
    }

    @Test
    void estado400() throws Exception {
        formato("/prueba/400", 400, CodigosError.SOLICITUD_INVALIDA)
                .andExpect(jsonPath("$.detalles[0].campo").value("pais"))
                .andExpect(jsonPath("$.detalles[0].motivo").value("obligatorio"));
    }

    @Test
    void estado404() throws Exception {
        formato("/prueba/404", 404, "NO_ENCONTRADO").andExpect(jsonPath("$.detalles", hasSize(0)));
    }

    @Test
    void estado409() throws Exception {
        formato("/prueba/409", 409, "CONFLICTO");
    }

    @Test
    void estado413() throws Exception {
        formato("/prueba/413", 413, "DEMASIADO_GRANDE");
    }

    @Test
    void tamanoExcedidoDeSpringDa413ConElMismoFormato() throws Exception {
        formato("/prueba/tamano", 413, CodigosError.DEMASIADO_GRANDE)
                .andExpect(jsonPath("$.mensaje").value(CodigosError.MSG_DEMASIADO_GRANDE));
    }

    @Test
    void estado422() throws Exception {
        formato("/prueba/422", 422, "INCOMPATIBLE").andExpect(jsonPath("$.detalles", hasSize(2)));
    }

    @Test
    void jsonMalFormadoDa400ConElMismoFormato() throws Exception {
        mvc.perform(post("/prueba/json").contentType(MediaType.APPLICATION_JSON).content("{no es json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value(CodigosError.SOLICITUD_INVALIDA))
                .andExpect(jsonPath("$.mensaje").isString())
                .andExpect(jsonPath("$.detalles").isArray());
    }

    @Test
    void excepcionNoPrevistaDa500SinTraza() throws Exception {
        formato("/prueba/500", 500, CodigosError.ERROR_INTERNO)
                .andExpect(content().string(not(containsString("secreto interno"))))
                .andExpect(content().string(not(containsString("IllegalStateException"))))
                .andExpect(content().string(not(containsString("at io.synkra"))));
    }

    @Test
    void rutaInexistenteDa404ConElMismoFormato() throws Exception {
        formato("/no/existe", 404, CodigosError.NO_ENCONTRADO);
    }

    @Test
    void metodoNoPermitidoDa405ConElMismoFormato() throws Exception {
        mvc.perform(post("/prueba/404")).andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.codigo").value(CodigosError.METODO_NO_PERMITIDO))
                .andExpect(jsonPath("$.mensaje").value(CodigosError.MSG_METODO_NO_PERMITIDO))
                .andExpect(jsonPath("$.detalles").isArray());
    }

    @Test
    void tipoNoSoportadoDa415ConElMismoFormato() throws Exception {
        mvc.perform(post("/prueba/json").contentType(MediaType.TEXT_PLAIN).content("x"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.codigo").value(CodigosError.TIPO_NO_SOPORTADO))
                .andExpect(jsonPath("$.mensaje").value(CodigosError.MSG_TIPO_NO_SOPORTADO))
                .andExpect(jsonPath("$.detalles").isArray());
    }

    @Test
    void elErrorInternoUsaElMensajeDePlataforma() throws Exception {
        formato("/prueba/500", 500, CodigosError.ERROR_INTERNO)
                .andExpect(jsonPath("$.mensaje").value(CodigosError.MSG_ERROR_INTERNO));
    }

    @Test
    void losMensajesDePlataformaLlevanTildes() throws Exception {
        mvc.perform(post("/prueba/404")).andExpect(jsonPath("$.mensaje").value("Método no permitido"));
    }
}
