package io.synkra.catalogo;

import io.synkra.catalogo.api.PluginSubido;
import io.synkra.plataforma.CodigosError;
import io.synkra.plataforma.DetalleError;
import io.synkra.plataforma.ErrorApi;
import io.synkra.plataforma.ExcepcionApi;
import io.synkra.verificador.CodigoMotivo;
import io.synkra.verificador.Descriptor;
import io.synkra.verificador.LectorDescriptor;
import io.synkra.verificador.RangoVersion;
import io.synkra.verificador.Resultado;
import io.synkra.verificador.Version;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.jdbc.core.JdbcAggregateTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * R-C4 y R-C7 a R-C11: la subida de un plugin, en el orden de {@code business-logic-model.md}; el primer fallo corta.
 * La comprobación de repetido, la colocación y los inserts van en una transacción; además, la colocación es un enlace
 * duro, que falla si el destino existe: dos subidas de la misma versión nunca se pisan el archivo, sea cual sea el pool.
 */
@Component
class SubidaPlugin {

    /** Tope de detalles de un rechazo (C-19); el último avisa de los omitidos. */
    static final int MAX_DETALLES = 50;

    private static final String DESCRIPTOR = "descriptor";
    private static final String ETIQUETAS = "etiquetas";
    private static final String ORIGEN_DESCRIPTOR = "descriptor";
    private static final String ORIGEN_SUBIDA = "subida";

    private final Path datos;
    private final Path raiz;
    private final RecepcionArtefacto recepcion;
    private final CoreRepositorio cores;
    private final PluginRepositorio plugins;
    private final EtiquetaRepositorio etiquetas;
    private final JdbcAggregateTemplate plantilla;
    private final TransactionTemplate transaccion;

    SubidaPlugin(@Value("${synkra.data}") String datos, RecepcionArtefacto recepcion, CoreRepositorio cores,
            PluginRepositorio plugins, EtiquetaRepositorio etiquetas, JdbcAggregateTemplate plantilla,
            TransactionTemplate transaccion) {
        this.datos = Path.of(datos);
        this.raiz = this.datos.resolve("artefactos");
        this.recepcion = recepcion;
        this.cores = cores;
        this.plugins = plugins;
        this.etiquetas = etiquetas;
        this.plantilla = plantilla;
        this.transaccion = transaccion;
    }

    PluginSubido subir(String core, InputStream jar, Map<String, String> deLaPeticion) throws IOException {
        if (core == null || cores.findByNombre(core).isEmpty()) {
            throw noEncontrado();
        }
        var recibido = recepcion.recibir(jar);
        try {
            var d = leerDescriptor(recibido.temporal());
            if (!d.core().equals(core)) {
                throw error(HttpStatus.UNPROCESSABLE_ENTITY, CodigoMotivo.CORE_EQUIVOCADO.name(),
                        CodigosError.MSG_ENTIDAD_NO_PROCESABLE,
                        List.of(new DetalleError("core", MotivosCatalogo.CORE_EQUIVOCADO.formatted(d.core(), core))));
            }
            return guardar(core, d, deLaPeticion, recibido);
        } finally {
            Files.deleteIfExists(recibido.temporal());
        }
    }

    PluginSubido obtener(String core, String nombre, String version) {
        if (core == null || nombre == null || version == null) {
            throw noEncontrado();
        }
        var p = plugins.findByCoreAndNombreAndVersion(core, nombre, version).orElseThrow(SubidaPlugin::noEncontrado);
        var unidas = etiquetas.findByCoreAndPluginAndVersion(core, nombre, version).stream()
                .collect(Collectors.toMap(Etiqueta::clave, Etiqueta::valor));
        return new PluginSubido(core, nombre, deLaBase(Version.parsear(p.version()), p),
                deLaBase(RangoVersion.parsear(p.coreCompatible()), p), unidas, p.sha256(), p.tamano());
    }

    /** R-C7: un detalle por motivo de U01, en su orden, sin traducir sus textos. */
    private static Descriptor leerDescriptor(Path temporal) {
        return switch (LectorDescriptor.leer(temporal)) {
            case Resultado.Exito<Descriptor> ok -> ok.valor();
            case Resultado.Fallo<Descriptor> fallo -> throw error(HttpStatus.UNPROCESSABLE_ENTITY,
                    CodigosError.ENTIDAD_NO_PROCESABLE, CodigosError.MSG_ENTIDAD_NO_PROCESABLE,
                    acotar(fallo.motivos().stream()
                            .map(m -> new DetalleError(m.campo().orElse(DESCRIPTOR),
                                    m.codigo().name() + ": " + m.mensaje()))
                            .toList(), DESCRIPTOR));
        };
    }

    /**
     * R-C9, R-C10 y R-C11 dentro de la transacción. El JAR se coloca con un enlace duro, que nunca reemplaza: si el
     * destino ya existe (otra subida de la misma versión), es un 409 y no se toca. Si algo falla después, se borran
     * solo el enlace y los directorios que creó esta subida.
     */
    private PluginSubido guardar(String core, Descriptor d, Map<String, String> deLaPeticion,
            ArtefactoRecibido recibido) throws IOException {
        var version = d.version().toString();
        var destino = rutaFinal(raiz, core, d.nombre(), version);
        var creados = new ArrayList<Path>();
        var colocado = new boolean[1];
        try {
            return transaccion.execute(estado -> {
                if (plugins.findByCoreAndNombreAndVersion(core, d.nombre(), version).isPresent()) {
                    throw repetido(d.nombre(), version);
                }
                var unidas = unir(d.etiquetas(), deLaPeticion);
                colocar(recibido.temporal(), destino, creados, d.nombre(), version);
                colocado[0] = true;
                plantilla.insert(new Plugin(core, d.nombre(), version, d.coreCompatible().texto(), recibido.sha256(),
                        recibido.tamano(), datos.relativize(destino).toString().replace('\\', '/'),
                        d.descripcion().orElse(null), CatalogoServicio.AUTOR, Instant.now().toEpochMilli()));
                unidas.forEach((clave, valorYOrigen) -> plantilla.insert(
                        new Etiqueta(core, d.nombre(), version, clave, valorYOrigen[0], valorYOrigen[1])));
                return new PluginSubido(core, d.nombre(), d.version(), d.coreCompatible(),
                        unidas.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue()[0])),
                        recibido.sha256(), recibido.tamano());
            });
        } catch (RuntimeException fallo) {
            if (colocado[0]) {
                try {
                    Files.deleteIfExists(destino);
                } catch (IOException e) {
                    fallo.addSuppressed(e);
                }
            }
            for (var dir : creados.reversed()) {
                try {
                    Files.delete(dir);
                } catch (DirectoryNotEmptyException otraSubidaLoUsa) {
                    // ya no es solo de esta subida
                } catch (IOException e) {
                    fallo.addSuppressed(e);
                }
            }
            if (fallo instanceof UncheckedIOException io) {
                throw io.getCause();
            }
            throw fallo;
        }
    }

    /** Crea {@code <core>/<nombre>} si falta (anotando lo creado) y enlaza el temporal en el destino. */
    private static void colocar(Path temporal, Path destino, List<Path> creados, String nombre, String version) {
        try {
            for (var dir : List.of(destino.getParent().getParent(), destino.getParent())) {
                try {
                    Files.createDirectory(dir);
                    creados.add(dir);
                } catch (FileAlreadyExistsException existe) {
                    if (!Files.isDirectory(dir, LinkOption.NOFOLLOW_LINKS)) {
                        throw existe;
                    }
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        try {
            Files.createLink(destino, temporal);
        } catch (FileAlreadyExistsException ocupado) {
            throw repetido(nombre, version);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static ExcepcionApi repetido(String nombre, String version) {
        return error(HttpStatus.CONFLICT, CodigosError.CONFLICTO, CodigosError.MSG_CONFLICTO,
                List.of(new DetalleError("version", MotivosCatalogo.PLUGIN_REPETIDO.formatted(nombre, version))));
    }

    /**
     * R-C10: las de la petición se validan con el patrón de U01; luego, una clave con dos valores es un choque. Da
     * clave → {valor, origen}, en orden de clave; la misma clave con el mismo valor queda una vez, del descriptor.
     */
    private static TreeMap<String, String[]> unir(Map<String, String> delDescriptor, Map<String, String> deLaPeticion) {
        var ordenadas = (deLaPeticion == null ? Map.<String, String>of() : deLaPeticion).entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.nullsFirst(Comparator.naturalOrder()))).toList();
        var invalidas = new ArrayList<DetalleError>();
        for (var e : ordenadas) {
            var clave = e.getKey();
            if (clave == null || !Descriptor.NOMBRE.matcher(clave).matches()) {
                invalidas.add(new DetalleError(ETIQUETAS,
                        MotivosCatalogo.ETIQUETA_CLAVE_INVALIDA.formatted(RecepcionArtefacto.recortar(String.valueOf(clave)))));
            } else if (e.getValue() == null) {
                invalidas.add(new DetalleError(ETIQUETAS + "." + clave, MotivosCatalogo.ETIQUETA_SIN_VALOR.formatted(clave)));
            }
        }
        if (!invalidas.isEmpty()) {
            throw error(HttpStatus.UNPROCESSABLE_ENTITY, CodigosError.ENTIDAD_NO_PROCESABLE,
                    CodigosError.MSG_ENTIDAD_NO_PROCESABLE, acotar(invalidas, ETIQUETAS));
        }
        var unidas = new TreeMap<String, String[]>();
        delDescriptor.forEach((clave, valor) -> unidas.put(clave, new String[] {valor, ORIGEN_DESCRIPTOR}));
        var choques = new ArrayList<DetalleError>();
        for (var e : ordenadas) {
            var previo = delDescriptor.get(e.getKey());
            if (previo == null) {
                unidas.put(e.getKey(), new String[] {e.getValue(), ORIGEN_SUBIDA});
            } else if (!previo.equals(e.getValue())) {
                choques.add(new DetalleError(ETIQUETAS + "." + e.getKey(), MotivosCatalogo.ETIQUETA_EN_CONFLICTO
                        .formatted(e.getKey(), RecepcionArtefacto.recortar(previo),
                                RecepcionArtefacto.recortar(e.getValue()))));
            }
        }
        if (!choques.isEmpty()) {
            throw error(HttpStatus.UNPROCESSABLE_ENTITY, CodigosCatalogo.ETIQUETA_EN_CONFLICTO,
                    CodigosError.MSG_ENTIDAD_NO_PROCESABLE, acotar(choques, ETIQUETAS));
        }
        return unidas;
    }

    /** {@code raiz/<core>/<nombre>/<version>.jar}, con nombres ya validados; además, nunca fuera de la raíz. */
    static Path rutaFinal(Path raiz, String core, String nombre, String version) {
        var ruta = raiz.resolve(core).resolve(nombre).resolve(version + ".jar").normalize();
        if (!ruta.startsWith(raiz.normalize())) {
            throw new IllegalStateException("La ruta del artefacto sale de su carpeta: " + ruta);
        }
        return ruta;
    }

    /** Como mucho {@link #MAX_DETALLES}: si hay más, los primeros y uno que dice cuántos se omitieron (C-19). */
    static List<DetalleError> acotar(List<DetalleError> detalles, String campo) {
        if (detalles.size() <= MAX_DETALLES) {
            return detalles;
        }
        var acotados = new ArrayList<>(detalles.subList(0, MAX_DETALLES - 1));
        acotados.add(new DetalleError(campo, MotivosCatalogo.MOTIVOS_OMITIDOS.formatted(detalles.size() - acotados.size())));
        return acotados;
    }

    private static <T> T deLaBase(Resultado<T> r, Plugin p) {
        return switch (r) {
            case Resultado.Exito<T> ok -> ok.valor();
            case Resultado.Fallo<T> fallo -> throw new IllegalStateException("Plugin no válido en la base: "
                    + p.core() + "/" + p.nombre() + "/" + p.version());
        };
    }

    private static ExcepcionApi noEncontrado() {
        return error(HttpStatus.NOT_FOUND, CodigosError.NO_ENCONTRADO, CodigosError.MSG_NO_ENCONTRADO, List.of());
    }

    private static ExcepcionApi error(HttpStatus estado, String codigo, String mensaje, List<DetalleError> detalles) {
        return new ExcepcionApi(estado, new ErrorApi(codigo, mensaje, detalles));
    }
}
