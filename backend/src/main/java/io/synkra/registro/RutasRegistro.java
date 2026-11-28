package io.synkra.registro;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.synkra.plataforma.CodigosError;
import io.synkra.plataforma.DetalleError;
import io.synkra.plataforma.ErrorApi;
import io.synkra.plataforma.ExcepcionApi;
import io.synkra.registro.api.ContextoFallo;
import io.synkra.registro.api.DespliegueRegistrado;
import io.synkra.registro.api.PluginDesplegado;
import io.synkra.registro.api.RegistroApi;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/** H-18: historial por país y detalle, solo lectura, sobre {@link RegistroApi}. Sin rutas de escritura. */
@RestController
class RutasRegistro {

    private static final Pattern PAIS = Pattern.compile("[a-z]{2}");

    record ResumenDespliegueDto(long id, String pais, String tipo, Long origenId, String core, String versionCore,
            String imagen, List<PluginDesplegado> plugins, String creadoPor, String confirmadoPor,
            Instant confirmadoEn, String estado, Instant terminadoEn) {}

    record ContextoDto(String logCore, String logInit, String eventos) {}

    record DespliegueDto(long id, String pais, String tipo, Long origenId, String core, String versionCore,
            String imagen, List<PluginDesplegado> plugins, String creadoPor, String confirmadoPor,
            Instant confirmadoEn, String estado, Instant terminadoEn, JsonNode veredicto, ContextoDto contexto) {}

    private final RegistroApi api;
    private final ObjectMapper json;

    RutasRegistro(RegistroApi api, ObjectMapper json) {
        this.api = api;
        this.json = json;
    }

    @GetMapping("/api/paises/{pais}/despliegues")
    List<ResumenDespliegueDto> historial(@PathVariable String pais) {
        if (!PAIS.matcher(pais).matches()) {
            throw new ExcepcionApi(HttpStatus.UNPROCESSABLE_ENTITY, new ErrorApi(CodigosRegistro.PAIS_INVALIDO,
                    MotivosRegistro.MSG_PAIS_INVALIDO, List.of(new DetalleError("pais", MotivosRegistro.PAIS_FORMA))));
        }
        return api.historial(pais).stream().map(RutasRegistro::resumen).toList();
    }

    @GetMapping("/api/despliegues/{id}")
    DespliegueDto obtener(@PathVariable long id) {
        var d = api.obtener(id).orElseThrow(() -> new ExcepcionApi(HttpStatus.NOT_FOUND,
                new ErrorApi(CodigosError.NO_ENCONTRADO, CodigosError.MSG_NO_ENCONTRADO, List.of())));
        ContextoFallo c = d.contexto();
        return new DespliegueDto(d.id(), d.pais(), minusculas(d.tipo()), d.origenId(), d.core(), d.versionCore(),
                d.imagen(), d.plugins(), d.creadoPor(), d.confirmadoPor(), d.confirmadoEn(), minusculas(d.estado()),
                d.terminadoEn(), veredicto(d.veredicto()),
                c == null ? null : new ContextoDto(c.logCore(), c.logInit(), c.eventos()));
    }

    private static ResumenDespliegueDto resumen(DespliegueRegistrado d) {
        return new ResumenDespliegueDto(d.id(), d.pais(), minusculas(d.tipo()), d.origenId(), d.core(),
                d.versionCore(), d.imagen(), d.plugins(), d.creadoPor(), d.confirmadoPor(), d.confirmadoEn(),
                minusculas(d.estado()), d.terminadoEn());
    }

    private JsonNode veredicto(String texto) {
        try {
            return json.readTree(texto);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("veredicto guardado ilegible", e);
        }
    }

    private static String minusculas(Enum<?> e) {
        return e.name().toLowerCase(Locale.ROOT);
    }
}
