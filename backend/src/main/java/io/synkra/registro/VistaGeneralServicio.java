package io.synkra.registro;

import io.synkra.kubernetes.api.AdaptadorKubernetes;
import io.synkra.kubernetes.api.ClienteNoDisponibleException;
import io.synkra.kubernetes.api.LecturaFallidaException;
import io.synkra.kubernetes.api.LecturaPais;
import io.synkra.kubernetes.api.LecturaPaisInconsistenteException;
import io.synkra.registro.api.DespliegueRegistrado;
import io.synkra.registro.api.LecturaCluster;
import io.synkra.registro.api.PluginCorriendo;
import io.synkra.registro.api.RegistroApi;
import io.synkra.registro.api.VistaGeneralApi;
import io.synkra.registro.api.VistaPais;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * H-19, R-R6 y R-R7. A propósito no es {@code @Transactional}: el pool tiene una sola conexión, y retenerla mientras
 * se lee el clúster bloquearía cualquier otra petición a la base. Cada lectura del registro va por el proxy de
 * {@link RegistroApi}, con su transacción de solo lectura, que termina al volver.
 */
@Service
class VistaGeneralServicio implements VistaGeneralApi {

    private final RegistroApi registro;
    private final AdaptadorKubernetes k8s;
    private final PaisesConfigurados paises;

    VistaGeneralServicio(RegistroApi registro, AdaptadorKubernetes k8s, PaisesConfigurados paises) {
        this.registro = registro;
        this.k8s = k8s;
        this.paises = paises;
    }

    @Override
    public List<VistaPais> vistaGeneral() {
        var salida = new ArrayList<VistaPais>();
        for (var pais : paises.lista()) {
            Optional<DespliegueRegistrado> ultimo = registro.ultimoListo(pais);
            salida.add(vistaDe(pais, ultimo));
        }
        return List.copyOf(salida);
    }

    private VistaPais vistaDe(String pais, Optional<DespliegueRegistrado> ultimo) {
        Long id = ultimo.map(DespliegueRegistrado::id).orElse(null);
        String core = ultimo.map(DespliegueRegistrado::core).orElse(null);
        String versionCore = ultimo.map(DespliegueRegistrado::versionCore).orElse(null);
        Optional<LecturaPais> leido;
        try {
            leido = k8s.leerPais(pais);
        } catch (LecturaPaisInconsistenteException e) {
            return new VistaPais(pais, LecturaCluster.INCONSISTENTE, null, List.of(), false, id, core, versionCore);
        } catch (ClienteNoDisponibleException | LecturaFallidaException e) {
            return new VistaPais(pais, LecturaCluster.NO_DISPONIBLE, null, List.of(), null, id, core, versionCore);
        }
        boolean concuerda = Concordancia.concuerda(ultimo, leido);
        if (leido.isEmpty()) {
            return new VistaPais(pais, LecturaCluster.VACIO, null, List.of(), concuerda, id, core, versionCore);
        }
        var lectura = leido.get();
        List<PluginCorriendo> plugins = lectura.plugins().stream()
                .map(p -> new PluginCorriendo(p.nombre(), p.version(), p.sha256()))
                .sorted(Comparator.comparing(PluginCorriendo::nombre).thenComparing(PluginCorriendo::version))
                .toList();
        return new VistaPais(pais, LecturaCluster.LEIDO, lectura.imagenCore(), plugins, concuerda, id, core,
                versionCore);
    }
}
