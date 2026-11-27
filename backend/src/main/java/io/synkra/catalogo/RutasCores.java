package io.synkra.catalogo;

import io.synkra.catalogo.api.CatalogoApi;
import io.synkra.catalogo.api.CoreRegistrado;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
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

    static final String OBLIGATORIO = "El campo es obligatorio";

    record SolicitudCore(
            @NotNull(message = OBLIGATORIO) String nombre,
            @NotNull(message = OBLIGATORIO) String version,
            @NotNull(message = OBLIGATORIO) String imagen) {}

    record VersionDto(String version, String imagen) {}

    record CoreDto(String nombre, List<VersionDto> versiones) {}

    private final CatalogoApi api;

    RutasCores(CatalogoApi api) {
        this.api = api;
    }

    @PostMapping
    ResponseEntity<CoreDto> registrar(@Valid @RequestBody SolicitudCore s) {
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

    /** Hasta U04-T07 y U04-T08 no hay plugins: vacía, pero 404 si el core no existe. */
    @GetMapping("/{nombre}/plugins")
    List<Object> plugins(@PathVariable String nombre) {
        api.obtenerCore(nombre);
        return List.of();
    }

    private static CoreDto dto(CoreRegistrado c) {
        return new CoreDto(c.nombre(), c.versiones().stream()
                .map(v -> new VersionDto(v.toString(), c.imagenes().get(v))).toList());
    }
}
