package io.synkra.catalogo;

import io.synkra.catalogo.api.CatalogoApi;
import io.synkra.catalogo.api.CoreRegistrado;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** H-01: rutas de cores sobre {@link CatalogoApi}. Paquete privado: la interfaz pública es {@code api}. */
@RestController
@RequestMapping("/api/cores")
class RutasCores {

    /** Sin {@code @Valid}: el servicio valida los tres campos y devuelve todos los detalles en un 422 (R-C1). */
    record SolicitudCore(String nombre, String version, String imagen) {}

    record VersionDto(String version, String imagen) {}

    record CoreDto(String nombre, List<VersionDto> versiones) {}

    private final CatalogoApi api;

    RutasCores(CatalogoApi api) {
        this.api = api;
    }

    @PostMapping
    ResponseEntity<CoreDto> registrar(@RequestBody SolicitudCore s) {
        var core = api.registrarCore(s.nombre(), s.version(), s.imagen());
        return ResponseEntity.created(URI.create("/api/cores/" + core.nombre())).body(dto(core));
    }

    @GetMapping
    List<CoreDto> listar() {
        return api.listarCores().stream().map(RutasCores::dto).toList();
    }

    @GetMapping("/{nombre}")
    CoreDto obtener(@PathVariable String nombre) {
        return dto(api.obtenerCore(nombre));
    }

    private static CoreDto dto(CoreRegistrado c) {
        return new CoreDto(c.nombre(), c.versiones().stream()
                .map(v -> new VersionDto(v.toString(), c.imagenes().get(v))).toList());
    }
}
