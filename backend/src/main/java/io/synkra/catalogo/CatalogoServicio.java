package io.synkra.catalogo;

import io.synkra.catalogo.api.CatalogoApi;
import io.synkra.catalogo.api.CoreRegistrado;
import io.synkra.catalogo.api.PluginSubido;
import io.synkra.plataforma.CodigosError;
import io.synkra.plataforma.DetalleError;
import io.synkra.plataforma.ErrorApi;
import io.synkra.plataforma.ExcepcionApi;
import io.synkra.verificador.Descriptor;
import io.synkra.verificador.Resultado;
import io.synkra.verificador.Version;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.data.jdbc.core.JdbcAggregateTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** R-C1 y R-C2; la subida de plugins la hace {@link SubidaPlugin}. La «carpeta» de un core es su fila de {@code cat_core}. */
@Service
class CatalogoServicio implements CatalogoApi {

    /** Longitud máxima de la imagen completa (campo {@code imagen}). */
    static final int IMAGEN_MAX = 255;

    private static final String REGISTRO = "[0-9a-z]+([.-][0-9a-z]+)*(:[0-9]+)?";
    private static final String REPO = "[0-9a-z]+([._-][0-9a-z]+)*";
    private static final String TAG = "[A-Za-z0-9_][A-Za-z0-9._-]{0,127}";
    /** registro[:puerto]/repo[/repo...][:tag]@sha256:64 hex en minúsculas; el tag solo al final. */
    private static final Pattern IMAGEN =
            Pattern.compile(REGISTRO + "(/" + REPO + ")+(:" + TAG + ")?@sha256:[0-9a-f]{64}");
    /** {@code registrado_por} y {@code subido_por} mientras no haya usuarios. */
    static final String AUTOR = "anonimo";

    private final JdbcAggregateTemplate plantilla;
    private final CoreRepositorio cores;
    private final VersionCoreRepositorio versiones;
    private final SubidaPlugin subida;
    private final ListadoPlugins listado;
    private final io.synkra.verificador.Catalogo catalogoSqlite;

    CatalogoServicio(JdbcAggregateTemplate plantilla, CoreRepositorio cores, VersionCoreRepositorio versiones,
            SubidaPlugin subida, ListadoPlugins listado, PluginRepositorio plugins, EtiquetaRepositorio etiquetas) {
        this.catalogoSqlite = new CatalogoSqlite(versiones, plugins, etiquetas);
        this.listado = listado;
        this.plantilla = plantilla;
        this.cores = cores;
        this.versiones = versiones;
        this.subida = subida;
    }

    @Override
    @Transactional
    public CoreRegistrado registrarCore(String nombre, String version, String imagen) {
        var detalles = new ArrayList<DetalleError>();
        if (nombre == null || !Descriptor.NOMBRE.matcher(nombre).matches()) {
            detalles.add(new DetalleError("nombre", MotivosCatalogo.NOMBRE_INVALIDO));
        }
        Version v = null;
        switch (Version.parsear(version)) {
            case Resultado.Exito<Version> ok -> v = ok.valor();
            case Resultado.Fallo<Version> fallo ->
                detalles.add(new DetalleError("version", fallo.motivos().get(0).mensaje()));
        }
        if (imagen == null || imagen.length() > IMAGEN_MAX || !IMAGEN.matcher(imagen).matches()) {
            detalles.add(new DetalleError("imagen", MotivosCatalogo.IMAGEN_INVALIDA));
        }
        if (!detalles.isEmpty()) {
            throw new ExcepcionApi(HttpStatus.UNPROCESSABLE_ENTITY,
                    new ErrorApi(CodigosError.ENTIDAD_NO_PROCESABLE, CodigosError.MSG_ENTIDAD_NO_PROCESABLE, detalles));
        }

        var texto = v.toString();
        if (versiones.findByCoreAndVersion(nombre, texto).isPresent()) {
            throw new ExcepcionApi(HttpStatus.CONFLICT, new ErrorApi(CodigosError.CONFLICTO,
                    CodigosError.MSG_CONFLICTO,
                    List.of(new DetalleError("version", MotivosCatalogo.VERSION_REPETIDA.formatted(texto)))));
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

    @Override
    @Transactional(readOnly = true)
    public List<CoreRegistrado> listarCores() {
        return cores.findAll().stream().map(Core::nombre).sorted().map(this::leer).toList();
    }

    @Override
    public PluginSubido subirPlugin(String core, InputStream jar, Map<String, String> etiquetas) throws IOException {
        return subida.subir(core, jar, etiquetas);
    }

    @Override
    public PluginSubido obtenerPlugin(String core, String nombre, String version) {
        return subida.obtener(core, nombre, version);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PluginSubido> listarPlugins(String core, Map<String, String> filtro) {
        obtenerCore(core);
        return listado.listar(core, filtro);
    }

    private CoreRegistrado leer(String nombre) {
        var imagenes = new HashMap<Version, String>();
        for (var f : versiones.findByCore(nombre)) {
            imagenes.put(leerVersion(nombre, f.version()), f.imagen());
        }
        var ordenadas = imagenes.keySet().stream().sorted().toList();
        return new CoreRegistrado(nombre, ordenadas, imagenes);
    }

    private static Version leerVersion(String core, String texto) {
        return switch (Version.parsear(texto)) {
            case Resultado.Exito<Version> ok -> ok.valor();
            case Resultado.Fallo<Version> fallo -> throw new IllegalStateException(
                    "Versión no válida en la base para el core " + core + ": '" + texto + "'");
        };
    }

    @Override
    public io.synkra.verificador.Catalogo catalogoParaVerificar() {
        return catalogoSqlite;
    }
}
