package io.synkra.catalogo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.synkra.catalogo.api.CatalogoApi;
import io.synkra.catalogo.api.PluginSubido;
import io.synkra.plataforma.CodigosError;
import io.synkra.plataforma.DetalleError;
import io.synkra.plataforma.ErrorApi;
import io.synkra.plataforma.ExcepcionApi;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Part;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.unit.DataSize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

/** H-02: subida y consulta de plugins sobre {@link CatalogoApi}. Paquete privado: la interfaz pública es {@code api}. */
@RestController
@RequestMapping("/api/cores/{nombre}/plugins")
class RutasPlugins {

    /** C-50: tope de caracteres (puntos de código Unicode) del valor de una etiqueta de la subida. */
    static final int MAX_VALOR_ETIQUETA = 63;

    private static final String PARTE_ARCHIVO = "archivo";
    private static final String PARTE_ETIQUETAS = "etiquetas";
    private static final TypeReference<TreeMap<String, String>> MAPA_DE_TEXTO = new TypeReference<>() {};

    record PluginDto(String core, String nombre, String version, String coreCompatible, Map<String, String> etiquetas,
            String sha256, long tamano) {}

    private final CatalogoApi api;
    private final ObjectMapper json;
    private final long maxEtiquetas;

    RutasPlugins(CatalogoApi api, ObjectMapper json, @Value("${synkra.max-cuerpo-json}") DataSize maxEtiquetas) {
        this.api = api;
        this.json = json;
        this.maxEtiquetas = maxEtiquetas.toBytes();
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<PluginDto> subir(@PathVariable String nombre, HttpServletRequest peticion)
            throws IOException, ServletException {
        var archivo = peticion.getPart(PARTE_ARCHIVO);
        if (archivo == null) {
            throw invalido(PARTE_ARCHIVO, MotivosCatalogo.ARCHIVO_OBLIGATORIO);
        }
        var etiquetas = leerEtiquetas(peticion.getPart(PARTE_ETIQUETAS));
        PluginSubido p;
        try (var flujo = archivo.getInputStream()) {
            p = api.subirPlugin(nombre, flujo, etiquetas);
        }
        var ubicacion = UriComponentsBuilder.fromPath("/api/cores")
                .pathSegment(p.core(), "plugins", p.nombre(), p.version().toString()).build().encode().toUri();
        return ResponseEntity.created(ubicacion).body(dto(p));
    }

    @GetMapping("/{plugin}/{version}")
    PluginDto obtener(@PathVariable String nombre, @PathVariable String plugin, @PathVariable String version) {
        return dto(api.obtenerPlugin(nombre, plugin, version));
    }

    /** Sin parte: mapa vacío. Más de C-38: 413. Que no sea un objeto de texto a texto, o un valor largo (C-50): 422. */
    private Map<String, String> leerEtiquetas(Part parte) throws IOException {
        if (parte == null) {
            return Map.of();
        }
        if (parte.getSize() > maxEtiquetas) {
            throw demasiadoGrande();
        }
        byte[] bytes;
        try (var in = parte.getInputStream()) {
            bytes = in.readNBytes((int) maxEtiquetas + 1);
        }
        if (bytes.length > maxEtiquetas) {
            throw demasiadoGrande();
        }
        TreeMap<String, String> mapa;
        try {
            mapa = json.readerFor(MAPA_DE_TEXTO).with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
                    .readValue(bytes);
        } catch (JsonProcessingException e) {
            throw invalido(PARTE_ETIQUETAS, MotivosCatalogo.ETIQUETAS_NO_ES_OBJETO);
        }
        if (mapa == null) {
            throw invalido(PARTE_ETIQUETAS, MotivosCatalogo.ETIQUETAS_NO_ES_OBJETO);
        }
        var largos = new ArrayList<DetalleError>();
        mapa.forEach((clave, valor) -> {
            if (valor != null && valor.codePointCount(0, valor.length()) > MAX_VALOR_ETIQUETA) {
                var corta = RecepcionArtefacto.recortar(clave);
                largos.add(new DetalleError(PARTE_ETIQUETAS + "." + corta,
                        MotivosCatalogo.ETIQUETA_VALOR_LARGO.formatted(corta)));
            }
        });
        if (!largos.isEmpty()) {
            throw new ExcepcionApi(HttpStatus.UNPROCESSABLE_ENTITY, new ErrorApi(CodigosError.ENTIDAD_NO_PROCESABLE,
                    CodigosError.MSG_ENTIDAD_NO_PROCESABLE, largos.size() > SubidaPlugin.MAX_DETALLES
                            ? largos.subList(0, SubidaPlugin.MAX_DETALLES) : largos));
        }
        return mapa;
    }

    private static ExcepcionApi invalido(String campo, String motivo) {
        return new ExcepcionApi(HttpStatus.UNPROCESSABLE_ENTITY, new ErrorApi(CodigosError.ENTIDAD_NO_PROCESABLE,
                CodigosError.MSG_ENTIDAD_NO_PROCESABLE, List.of(new DetalleError(campo, motivo))));
    }

    private ExcepcionApi demasiadoGrande() {
        return new ExcepcionApi(HttpStatus.PAYLOAD_TOO_LARGE, new ErrorApi(CodigosError.DEMASIADO_GRANDE,
                CodigosError.MSG_CUERPO_DEMASIADO_GRANDE, List.of(new DetalleError(PARTE_ETIQUETAS,
                        CodigosError.MOTIVO_JSON_GRANDE.formatted(maxEtiquetas)))));
    }

    private static PluginDto dto(PluginSubido p) {
        return new PluginDto(p.core(), p.nombre(), p.version().toString(), p.coreCompatible().texto(), p.etiquetas(),
                p.sha256(), p.tamano());
    }
}
