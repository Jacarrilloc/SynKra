package io.synkra.catalogo;

import io.synkra.catalogo.api.CatalogoApi;
import io.synkra.catalogo.api.CoreRegistrado;
import io.synkra.plataforma.CodigosError;
import io.synkra.plataforma.DetalleError;
import io.synkra.plataforma.ErrorApi;
import io.synkra.plataforma.ExcepcionApi;
import io.synkra.verificador.Descriptor;
import io.synkra.verificador.Resultado;
import io.synkra.verificador.Version;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.data.jdbc.core.JdbcAggregateTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** R-C1 y R-C2. La «carpeta» de un core es su fila de {@code cat_core}. */
@Service
class CatalogoServicio implements CatalogoApi {

    /** registro/repo[:tag]@sha256:&lt;64 hex en minúsculas&gt; */
    private static final Pattern IMAGEN =
            Pattern.compile("[^\\s@/]+(/[^\\s@/]+)+@sha256:[0-9a-f]{64}");
    private static final String AUTOR = "anonimo";

    private final JdbcAggregateTemplate plantilla;
    private final CoreRepositorio cores;
    private final VersionCoreRepositorio versiones;

    CatalogoServicio(JdbcAggregateTemplate plantilla, CoreRepositorio cores, VersionCoreRepositorio versiones) {
        this.plantilla = plantilla;
        this.cores = cores;
        this.versiones = versiones;
    }

    @Override
    @Transactional
    public CoreRegistrado registrarCore(String nombre, String version, String imagen) {
        var detalles = new ArrayList<DetalleError>();
        if (nombre == null || !Descriptor.NOMBRE.matcher(nombre).matches()) {
            detalles.add(new DetalleError("nombre", "El nombre debe ser minúsculas, dígitos y guiones medios"));
        }
        Version v = null;
        switch (Version.parsear(version)) {
            case Resultado.Exito<Version> ok -> v = ok.valor();
            case Resultado.Fallo<Version> fallo ->
                detalles.add(new DetalleError("version", "La versión debe ser MAYOR.MENOR.PARCHE (por ejemplo 2.1.0)"));
        }
        if (imagen == null || !IMAGEN.matcher(imagen).matches()) {
            detalles.add(new DetalleError("imagen",
                    "La imagen debe ser registro/repo[:tag]@sha256:<64 hex en minúsculas>"));
        }
        if (!detalles.isEmpty()) {
            throw new ExcepcionApi(HttpStatus.UNPROCESSABLE_ENTITY,
                    new ErrorApi(CodigosError.ENTIDAD_NO_PROCESABLE, CodigosError.MSG_ENTIDAD_NO_PROCESABLE, detalles));
        }

        var texto = v.toString();
        if (versiones.findByCoreAndVersion(nombre, texto).isPresent()) {
            throw new ExcepcionApi(HttpStatus.CONFLICT, new ErrorApi(CodigosError.CONFLICTO,
                    CodigosError.MSG_CONFLICTO,
                    List.of(new DetalleError("version", "La versión " + texto + " del core ya está registrada"))));
        }
        if (cores.findByNombre(nombre).isEmpty()) {
            plantilla.insert(new Core(nombre));
        }
        plantilla.insert(new VersionCore(nombre, texto, imagen, AUTOR, Instant.now().toEpochMilli()));
        return leer(nombre);
    }

    @Override
    @Transactional(readOnly = true)
    public CoreRegistrado obtenerCore(String nombre) {
        if (nombre == null || cores.findByNombre(nombre).isEmpty()) {
            throw new ExcepcionApi(HttpStatus.NOT_FOUND,
                    new ErrorApi(CodigosError.NO_ENCONTRADO, CodigosError.MSG_NO_ENCONTRADO, List.of()));
        }
        return leer(nombre);
    }

    private CoreRegistrado leer(String nombre) {
        var ordenadas = versiones.findByCore(nombre).stream()
                .map(f -> ((Resultado.Exito<Version>) Version.parsear(f.version())).valor())
                .sorted()
                .toList();
        return new CoreRegistrado(nombre, ordenadas);
    }
}
