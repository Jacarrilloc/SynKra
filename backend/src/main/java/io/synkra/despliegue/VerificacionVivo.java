package io.synkra.despliegue;

import io.synkra.catalogo.api.CatalogoApi;
import io.synkra.plataforma.DetalleError;
import io.synkra.plataforma.ErrorApi;
import io.synkra.plataforma.ExcepcionApi;
import io.synkra.verificador.Catalogo;
import io.synkra.verificador.Descriptor;
import io.synkra.verificador.RangoVersion;
import io.synkra.verificador.Resultado;
import io.synkra.verificador.Seleccion;
import io.synkra.verificador.Verificador;
import io.synkra.verificador.Veredicto;
import io.synkra.verificador.Version;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * R-D2: valida la selección pedida, la convierte en descriptores del catálogo y la verifica con U01 (con
 * alternativas). No toca {@code dep_plan} ni Kubernetes.
 */
@Service
class VerificacionVivo {

    record Verificado(Seleccion seleccion, Veredicto veredicto) {}

    private final CatalogoApi catalogo;

    VerificacionVivo(CatalogoApi catalogo) {
        this.catalogo = catalogo;
    }

    /**
     * Una sola transacción de lectura: {@code verificar} y {@code alternativas} leen el mismo catálogo.
     *
     * @throws ExcepcionApi 422 {@code SELECCION_INVALIDA} o {@code CORE_NO_REGISTRADO}
     */
    @Transactional(readOnly = true)
    Verificado verificar(String core, String versionCore, List<PluginPedido> plugins) {
        var pedidos = plugins == null ? List.<PluginPedido>of() : plugins;
        var detalles = new ArrayList<DetalleError>();
        if (core == null || !Descriptor.NOMBRE.matcher(core).matches()) {
            detalles.add(new DetalleError("core", MotivosDespliegue.SELECCION_CORE));
        }
        var version = versionDe(versionCore);
        if (version.isEmpty()) {
            detalles.add(new DetalleError("versionCore", MotivosDespliegue.SELECCION_VERSION_CORE));
        }
        var versiones = new ArrayList<Optional<Version>>();
        for (int i = 0; i < pedidos.size(); i++) {
            var p = pedidos.get(i);
            if (p == null) {
                detalles.add(new DetalleError("plugins[" + i + "]", MotivosDespliegue.SELECCION_PLUGIN_NULO));
                versiones.add(Optional.empty());
                continue;
            }
            if (p.nombre() == null || !Descriptor.NOMBRE.matcher(p.nombre()).matches()) {
                detalles.add(new DetalleError("plugins[" + i + "].nombre", MotivosDespliegue.SELECCION_NOMBRE));
            }
            var v = versionDe(p.version());
            if (v.isEmpty()) {
                detalles.add(new DetalleError("plugins[" + i + "].version", MotivosDespliegue.SELECCION_VERSION));
            }
            versiones.add(v);
        }
        if (!detalles.isEmpty()) {
            throw new ExcepcionApi(HttpStatus.UNPROCESSABLE_ENTITY, new ErrorApi(
                    CodigosDespliegue.SELECCION_INVALIDA, MotivosDespliegue.SELECCION_MENSAJE, detalles));
        }

        var vista = catalogo.catalogoParaVerificar();
        var registradas = vista.versionesCore(core);
        if (registradas.isEmpty()) {
            throw coreNoRegistrado("core", MotivosDespliegue.CORE_SIN_NOMBRE);
        }
        if (!registradas.contains(version.get())) {
            throw coreNoRegistrado("versionCore", MotivosDespliegue.CORE_SIN_VERSION);
        }

        var descriptores = new ArrayList<Descriptor>();
        for (int i = 0; i < pedidos.size(); i++) {
            descriptores.add(descriptorDe(vista, core, version.get(), pedidos.get(i).nombre(), versiones.get(i).get()));
        }
        var seleccion = new Seleccion(core, version.get(), descriptores);
        var veredicto = Verificador.alternativas(seleccion, Verificador.verificar(seleccion, vista), vista);
        return new Verificado(seleccion, veredicto);
    }

    /** Decisión 4: la carpeta del core pedido, luego las demás (por nombre); si no, uno provisional (ausente). */
    private Descriptor descriptorDe(Catalogo vista, String core, Version versionCore, String nombre, Version version) {
        var propio = vista.descriptor(core, nombre, version);
        if (propio.isPresent()) {
            return propio.get();
        }
        for (var otro : catalogo.listarCores()) {
            if (otro.nombre().equals(core)) {
                continue;
            }
            var ajeno = vista.descriptor(otro.nombre(), nombre, version);
            if (ajeno.isPresent()) {
                return ajeno.get();
            }
        }
        var exacta = switch (RangoVersion.parsear(versionCore.toString())) {
            case Resultado.Exito<RangoVersion> e -> e.valor();
            case Resultado.Fallo<RangoVersion> f -> throw new IllegalStateException("versión de core sin rango exacto");
        };
        return new Descriptor(nombre, version, core, exacta, Map.of(), Optional.empty());
    }

    private static Optional<Version> versionDe(String texto) {
        return switch (Version.parsear(texto)) {
            case Resultado.Exito<Version> e -> Optional.of(e.valor());
            case Resultado.Fallo<Version> f -> Optional.empty();
        };
    }

    private static ExcepcionApi coreNoRegistrado(String campo, String motivo) {
        return new ExcepcionApi(HttpStatus.UNPROCESSABLE_ENTITY, new ErrorApi(CodigosDespliegue.CORE_NO_REGISTRADO,
                MotivosDespliegue.CORE_MENSAJE, List.of(new DetalleError(campo, motivo))));
    }
}
