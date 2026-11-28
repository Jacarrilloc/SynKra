package io.synkra.despliegue;

import io.synkra.verificador.Alternativa;
import io.synkra.verificador.Motivo;
import io.synkra.verificador.Veredicto;
import io.synkra.verificador.VeredictoPlugin;
import java.util.List;

/** El veredicto de U01 como JSON (R-D2): versiones como texto; {@code plugin} y {@code campo} son texto o nulos. */
record VeredictoDto(String global, List<PluginDto> plugins, List<MotivoDto> motivosGlobales) {

    record PluginDto(String nombre, String version, String core, boolean compatible, List<MotivoDto> motivos,
            List<AlternativaDto> alternativas) {

        static PluginDto de(VeredictoPlugin p) {
            return new PluginDto(p.descriptor().nombre(), p.descriptor().version().toString(), p.descriptor().core(),
                    p.compatible(), p.motivos().stream().map(MotivoDto::de).toList(),
                    p.alternativas().stream().map(AlternativaDto::de).toList());
        }
    }

    record MotivoDto(String codigo, String plugin, String mensaje, String campo) {

        static MotivoDto de(Motivo m) {
            return new MotivoDto(m.codigo().name(), m.plugin().orElse(null), m.mensaje(), m.campo().orElse(null));
        }
    }

    record AlternativaDto(String tipo, String nombre, String version) {

        static AlternativaDto de(Alternativa a) {
            return new AlternativaDto(a.tipo().name(), a.nombre(), a.version().toString());
        }
    }

    static VeredictoDto de(Veredicto v) {
        return new VeredictoDto(v.global().name(), v.porPlugin().stream().map(PluginDto::de).toList(),
                v.motivosGlobales().stream().map(MotivoDto::de).toList());
    }
}
