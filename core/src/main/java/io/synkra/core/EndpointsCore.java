package io.synkra.core;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.cfg.CoercionAction;
import com.fasterxml.jackson.databind.cfg.CoercionInputShape;
import com.fasterxml.jackson.databind.type.LogicalType;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.synkra.plugin.api.PuntoKyc;
import io.synkra.plugin.api.ResultadoKyc;
import io.synkra.plugin.api.SolicitudKyc;
import java.io.IOException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints del core (H-21): {@code GET /plugins} y {@code POST /kyc/{plugin}}. Solo atienden los plugins activos
 * e <em>iniciados</em>; el core no conoce países, solo delega en el {@link PuntoKyc} del plugin pedido.
 */
@RestController
public class EndpointsCore {

    /** Elemento de {@code GET /plugins}: nombre y versión del descriptor del JAR. */
    public record InfoPlugin(String nombre, String version) {}

    static final String PREFIJO_FALLO_KYC = "Plugin fallo al validar KYC plugin=";

    private static final Logger LOG = LoggerFactory.getLogger(EndpointsCore.class);

    private final IniciadorPlugins iniciador;
    private final ObjectMapper json;

    public EndpointsCore(IniciadorPlugins iniciador, ObjectMapper json) {
        this.iniciador = iniciador;
        // Estricto: un número no pasa por texto ni al revés.
        this.json = json.copy();
        // Cuerpo ambiguo o con basura no es una SolicitudKyc valida.
        this.json.enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
        this.json.enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        this.json.enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION);
        for (var forma : List.of(CoercionInputShape.Integer, CoercionInputShape.Float, CoercionInputShape.Boolean)) {
            this.json.coercionConfigFor(LogicalType.Textual).setCoercion(forma, CoercionAction.Fail);
        }
    }

    @GetMapping("/plugins")
    public List<InfoPlugin> plugins() {
        return iniciador.iniciados().stream().map(p -> new InfoPlugin(p.nombre(), p.version())).toList();
    }

    @PostMapping("/kyc/{plugin}")
    public ResponseEntity<?> kyc(@PathVariable("plugin") String nombre, @RequestBody(required = false) byte[] cuerpo) {
        var activo = iniciador.iniciados().stream().filter(p -> p.nombre().equals(nombre)).findFirst();
        if (activo.isEmpty()) {
            return error(HttpStatus.NOT_FOUND, ErrorCore.PLUGIN_NO_ACTIVO, ErrorCore.MENSAJE_NO_ACTIVO + nombre);
        }
        var cargado = activo.get().cargado();
        if (!(cargado.plugin() instanceof PuntoKyc punto)) {
            return error(HttpStatus.NOT_FOUND, ErrorCore.PLUGIN_SIN_KYC, ErrorCore.MENSAJE_SIN_KYC + nombre);
        }
        SolicitudKyc solicitud;
        try {
            solicitud = cuerpo == null ? null : json.readValue(cuerpo, SolicitudKyc.class);
        } catch (IOException | RuntimeException e) {
            solicitud = null;
        }
        if (solicitud == null) {
            return error(HttpStatus.BAD_REQUEST, ErrorCore.SOLICITUD_INVALIDA, ErrorCore.MENSAJE_SOLICITUD_INVALIDA);
        }
        return invocar(nombre, cargado, punto, solicitud);
    }

    /**
     * Llama al plugin con el context class loader del hilo puesto en el cargador del plugin y lo restaura en un
     * {@code finally}. Visible en el paquete para probar la restauración sin depender de Tomcat, que ya restaura
     * por su cuenta el cargador del hilo al terminar la petición.
     */
    ResponseEntity<?> invocar(String nombre, PluginCargado cargado, PuntoKyc punto, SolicitudKyc solicitud) {
        var hilo = Thread.currentThread();
        var anterior = hilo.getContextClassLoader();
        try {
            hilo.setContextClassLoader(cargado.cargador());
            ResultadoKyc resultado = punto.validar(solicitud);
            if (resultado == null) {
                LOG.error(PREFIJO_FALLO_KYC + nombre + ": el plugin devolvio null");
                return error(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCore.PLUGIN_FALLO, ErrorCore.MENSAJE_PLUGIN_FALLO + nombre);
            }
            return ResponseEntity.ok(resultado);
        } catch (Exception | LinkageError e) {
            LOG.error(PREFIJO_FALLO_KYC + nombre + ": " + e, e);
            return error(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCore.PLUGIN_FALLO, ErrorCore.MENSAJE_PLUGIN_FALLO + nombre);
        } finally {
            hilo.setContextClassLoader(anterior);
        }
    }

    private static ResponseEntity<ErrorCore> error(HttpStatus estado, String codigo, String mensaje) {
        return ResponseEntity.status(estado).contentType(MediaType.APPLICATION_JSON).body(new ErrorCore(codigo, mensaje));
    }
}
