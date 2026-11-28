package io.synkra.registro;

import io.synkra.kubernetes.api.AdaptadorKubernetes;
import io.synkra.registro.api.RegistroApi;
import io.synkra.registro.api.VistaGeneralApi;
import io.synkra.registro.api.VistaPais;
import java.util.List;
import org.springframework.stereotype.Service;

/** H-19. Firma vacía del rojo. */
@Service
class VistaGeneralServicio implements VistaGeneralApi {

    VistaGeneralServicio(RegistroApi registro, AdaptadorKubernetes k8s, PaisesConfigurados paises) {}

    @Override
    public List<VistaPais> vistaGeneral() {
        throw new UnsupportedOperationException("pendiente");
    }
}
