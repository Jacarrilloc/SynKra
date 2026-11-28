package io.synkra.registro;

import io.synkra.registro.api.PluginCorriendo;
import io.synkra.registro.api.VistaGeneralApi;
import io.synkra.registro.api.VistaPais;
import java.util.List;
import java.util.Locale;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** H-19: la vista general por país, solo lectura, sobre {@link VistaGeneralApi}. Sin rutas de escritura. */
@RestController
class RutasVistaGeneral {

    record VistaPaisDto(String pais, String lectura, String imagen, List<PluginCorriendo> plugins, Boolean concuerda,
            Long ultimoDespliegueId, String core, String versionCore) {}

    private final VistaGeneralApi api;

    RutasVistaGeneral(VistaGeneralApi api) {
        this.api = api;
    }

    @GetMapping("/api/paises")
    List<VistaPaisDto> vistaGeneral() {
        return api.vistaGeneral().stream().map(RutasVistaGeneral::dto).toList();
    }

    private static VistaPaisDto dto(VistaPais v) {
        return new VistaPaisDto(v.pais(), v.lectura().name().toLowerCase(Locale.ROOT), v.imagen(), v.plugins(),
                v.concuerda(), v.ultimoDespliegueId(), v.core(), v.versionCore());
    }
}
