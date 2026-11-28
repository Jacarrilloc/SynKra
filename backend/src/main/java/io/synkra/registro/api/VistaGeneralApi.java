package io.synkra.registro.api;

import java.util.List;

/** H-19: la vista general de los países configurados. */
public interface VistaGeneralApi {

    /** Un {@link VistaPais} por país de {@code synkra.paises}, en el orden de la propiedad (R-R7). */
    List<VistaPais> vistaGeneral();
}
