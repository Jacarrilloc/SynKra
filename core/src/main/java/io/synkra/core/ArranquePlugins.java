package io.synkra.core;

import io.synkra.verificador.Catalogo;
import io.synkra.verificador.Descriptor;
import io.synkra.verificador.LectorDescriptor;
import io.synkra.verificador.Motivo;
import io.synkra.verificador.RangoVersion;
import io.synkra.verificador.Resultado;
import io.synkra.verificador.Seleccion;
import io.synkra.verificador.Veredicto;
import io.synkra.verificador.Verificador;
import io.synkra.verificador.Version;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snakeyaml.engine.v2.api.Load;
import org.snakeyaml.engine.v2.api.LoadSettings;
import org.snakeyaml.engine.v2.common.ScalarStyle;
import org.snakeyaml.engine.v2.composer.Composer;
import org.snakeyaml.engine.v2.events.CollectionEndEvent;
import org.snakeyaml.engine.v2.events.CollectionStartEvent;
import org.snakeyaml.engine.v2.nodes.MappingNode;
import org.snakeyaml.engine.v2.nodes.Node;
import org.snakeyaml.engine.v2.nodes.NodeTuple;
import org.snakeyaml.engine.v2.nodes.ScalarNode;
import org.snakeyaml.engine.v2.nodes.SequenceNode;
import org.snakeyaml.engine.v2.nodes.Tag;
import org.snakeyaml.engine.v2.parser.ParserImpl;
import org.snakeyaml.engine.v2.schema.CoreSchema;
import org.snakeyaml.engine.v2.scanner.StreamReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Arranque del core (S8): lee {@code plugins-activos.yaml} y los JAR de plugins, verifica la selección con U01 y
 * la versión compilada, y carga solo los plugins activos. Con {@code RECHAZADA} lanza {@link ArranqueRechazado}:
 * el contexto de Spring falla y el proceso termina con código distinto de 0 (sin {@code System.exit}).
 * Aquí los plugins se cargan; {@link IniciadorPlugins} los inicia al terminar el arranque.
 */
@Component
public class ArranquePlugins {

    // Única fuente de estas constantes dentro del core. Los límites replican los de LectorDescriptor (U01);
    // ese lector no es público, así que no se pueden compartir sin tocar verificador/ (candidata registrada).
    /** Nombre del core al que pertenecen los plugins de esta instalación. */
    static final String CORE = "synkra-core";
    static final String ARCHIVO_ACTIVOS = "plugins-activos.yaml";
    /** Tamaño máximo de plugins-activos.yaml, igual que LectorDescriptor.MAX_BYTES. */
    static final int MAX_BYTES = 64 * 1024;
    /** Alias máximos por colección, igual que LectorDescriptor.MAX_ALIAS. */
    static final int MAX_ALIAS = 10;
    /** Anidación máxima, igual que LectorDescriptor.MAX_PROFUNDIDAD. */
    static final int MAX_PROFUNDIDAD = 32;
    private static final Logger LOG = LoggerFactory.getLogger(ArranquePlugins.class);

    private final VersionCore versionCore;
    private final CargadorPlugins cargador;
    private final Path config;
    private final Path plugins;
    private final List<PluginCargado> cargados = new ArrayList<>();
    private final List<PluginActivo> activosCargados = new ArrayList<>();

    public ArranquePlugins(
            VersionCore versionCore,
            CargadorPlugins cargador,
            @Value("${synkra.core.config:/config}") Path config,
            @Value("${synkra.core.plugins:/plugins}") Path plugins) {
        this.versionCore = versionCore;
        this.cargador = cargador;
        this.config = config;
        this.plugins = plugins;
    }

    @PostConstruct
    void arrancar() {
        var jars = new LinkedHashMap<Descriptor, Path>();
        var catalogo = leerJars(jars);
        var activos = leerActivos(jars);
        var seleccion = new Seleccion(CORE, versionCore.version(), activos);
        Veredicto veredicto = Verificador.verificar(seleccion, catalogo);
        if (veredicto.global() == Veredicto.Global.RECHAZADA) {
            var motivos = new ArrayList<Motivo>(veredicto.motivosGlobales());
            veredicto.porPlugin().forEach(p -> motivos.addAll(p.motivos()));
            var lineas = motivos.stream()
                    .map(m -> m.codigo() + " plugin=" + m.plugin().orElse("-") + ": " + m.mensaje())
                    .toList();
            lineas.forEach(l -> LOG.error("Arranque rechazado: {}", l));
            throw new ArranqueRechazado("Arranque rechazado por el verificador: " + String.join("; ", lineas));
        }
        try {
            for (var d : activos) {
                var c = cargador.cargar(jars.get(d));
                cargados.add(c);
                activosCargados.add(new PluginActivo(d.nombre(), d.version().toString(), c));
            }
        } catch (RuntimeException e) {
            cerrar();
            throw e;
        }
        LOG.info("Plugins cargados: {}", cargados.stream().map(c -> c.plugin().nombre()).toList());
    }

    /** Los plugins activos ya cargados (no iniciados). */
    public List<PluginCargado> cargados() {
        return List.copyOf(cargados);
    }

    /** Los plugins activos ya cargados, con nombre y versión del descriptor, en orden de carga. */
    public List<PluginActivo> activos() {
        return List.copyOf(activosCargados);
    }

    @PreDestroy
    void cerrar() {
        for (var c : cargados) {
            try {
                c.close();
            } catch (IOException e) {
                LOG.warn("No se pudo cerrar el cargador de {}: {}", c.plugin().nombre(), e.toString());
            }
        }
        cargados.clear();
        activosCargados.clear();
    }

    /** Descriptor de cada JAR de la carpeta; un JAR inválido se registra por nombre y no entra al catálogo. */
    private Catalogo leerJars(Map<Descriptor, Path> jars) {
        var descriptores = new ArrayList<Descriptor>();
        if (Files.isDirectory(plugins)) {
            try (Stream<Path> s = Files.list(plugins)) {
                for (Path jar : s.filter(p -> p.getFileName().toString().endsWith(".jar")).sorted().toList()) {
                    switch (LectorDescriptor.leer(jar)) {
                        case Resultado.Exito<Descriptor> ok -> {
                            var d = ok.valor();
                            boolean repetido = descriptores.stream()
                                    .anyMatch(x -> x.nombre().equals(d.nombre()) && x.version().equals(d.version())
                                            && x.core().equals(d.core()));
                            if (repetido) {
                                LOG.warn("JAR {} repite {} {}; se ignora", jar.getFileName(), d.nombre(), d.version());
                            } else {
                                descriptores.add(d);
                                jars.put(d, jar);
                            }
                        }
                        case Resultado.Fallo<Descriptor> f -> LOG.warn(
                                "JAR {} con descriptor inválido, no entra al catálogo: {}",
                                jar.getFileName(),
                                f.motivos().stream().map(m -> m.codigo() + " " + m.mensaje()).toList());
                    }
                }
            } catch (IOException e) {
                throw new ArranqueRechazado("No se pudo leer la carpeta de plugins " + plugins + ": " + e);
            }
        }
        var version = versionCore.version();
        return new Catalogo() {
            @Override
            public List<Version> versionesCore(String nombre) {
                return CORE.equals(nombre) ? List.of(version) : List.of();
            }

            @Override
            public List<Version> versionesPlugin(String core, String plugin) {
                return descriptores.stream()
                        .filter(d -> d.core().equals(core) && d.nombre().equals(plugin))
                        .map(Descriptor::version)
                        .toList();
            }

            @Override
            public Optional<Descriptor> descriptor(String core, String plugin, Version v) {
                return descriptores.stream()
                        .filter(d -> d.core().equals(core) && d.nombre().equals(plugin) && d.version().equals(v))
                        .findFirst();
            }
        };
    }

    /**
     * Lista de {@code {nombre, version}}. Cada entrada se resuelve con el descriptor del JAR presente; si no hay
     * JAR, queda un descriptor provisional que el verificador juzga como {@code PLUGIN_AUSENTE}.
     */
    private List<Descriptor> leerActivos(Map<Descriptor, Path> jars) {
        Path archivo = config.resolve(ARCHIVO_ACTIVOS);
        if (!Files.isRegularFile(archivo)) {
            LOG.info("No hay {}: el core arranca sin plugins", archivo);
            return List.of();
        }
        Object raiz;
        try {
            byte[] bytes;
            try (var in = Files.newInputStream(archivo)) {
                bytes = in.readNBytes(MAX_BYTES + 1);
            }
            if (bytes.length > MAX_BYTES) {
                throw new ArranqueRechazado(ARCHIVO_ACTIVOS + " supera el tamaño máximo de " + MAX_BYTES + " bytes");
            }
            String texto = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(java.nio.ByteBuffer.wrap(bytes))
                    .toString();
            var ajustes = LoadSettings.builder()
                    .setSchema(new CoreSchema())
                    .setMaxAliasesForCollections(MAX_ALIAS)
                    .build();
            var excesiva = profundidadExcesiva(ajustes, texto);
            if (excesiva != null) {
                throw new ArranqueRechazado(ARCHIVO_ACTIVOS + ": " + excesiva);
            }
            var nodo = new Composer(ajustes, new ParserImpl(ajustes, new StreamReader(ajustes, texto)))
                    .getSingleNode()
                    .orElse(null);
            if (nodo != null) {
                var etiqueta = etiquetaNoPermitida(nodo, new IdentityHashMap<>(), ajustes);
                if (etiqueta != null) {
                    throw new ArranqueRechazado(ARCHIVO_ACTIVOS + ": etiqueta de tipo '" + etiqueta.getValue()
                            + "' no permitida");
                }
            }
            raiz = new Load(ajustes).loadFromString(texto);
        } catch (CharacterCodingException e) {
            throw new ArranqueRechazado(ARCHIVO_ACTIVOS + " no es UTF-8 válido");
        } catch (IOException e) {
            throw new ArranqueRechazado("No se pudo leer " + archivo + ": " + e);
        } catch (RuntimeException e) {
            if (e instanceof ArranqueRechazado a) {
                throw a;
            }
            throw new ArranqueRechazado(ARCHIVO_ACTIVOS + " no es válido o no es seguro: " + e.getMessage());
        }
        if (raiz == null) {
            return List.of();
        }
        if (!(raiz instanceof List<?> lista)) {
            throw new ArranqueRechazado(ARCHIVO_ACTIVOS + " debe ser una lista de {nombre, version}");
        }
        var activos = new ArrayList<Descriptor>();
        for (Object o : lista) {
            if (!(o instanceof Map<?, ?> m) || m.size() != 2
                    || !(m.get("nombre") instanceof String nombre) || !(m.get("version") instanceof String texto)) {
                throw new ArranqueRechazado(ARCHIVO_ACTIVOS
                        + ": cada entrada debe ser {nombre, version} con ambos como texto; recibido " + o);
            }
            if (!(Version.parsear(texto) instanceof Resultado.Exito<Version> v)) {
                throw new ArranqueRechazado(ARCHIVO_ACTIVOS + ": versión inválida '" + texto + "' para " + nombre);
            }
            if (!Descriptor.NOMBRE.matcher(nombre).matches()) {
                throw new ArranqueRechazado(ARCHIVO_ACTIVOS + ": nombre inválido '" + nombre + "'");
            }
            var presente = jars.keySet().stream()
                    .filter(d -> d.nombre().equals(nombre) && d.version().equals(v.valor()))
                    .findFirst();
            activos.add(presente.orElseGet(() -> provisional(nombre, v.valor())));
        }
        return activos;
    }

    /** Cuenta la anidación con los eventos del parser (iterativo) antes de componer, que es recursivo. */
    private static String profundidadExcesiva(LoadSettings ajustes, String yaml) {
        var parser = new ParserImpl(ajustes, new StreamReader(ajustes, yaml));
        int profundidad = 0;
        while (parser.hasNext()) {
            var ev = parser.next();
            if (ev instanceof CollectionStartEvent) {
                if (++profundidad > MAX_PROFUNDIDAD) {
                    return "supera la anidación máxima de " + MAX_PROFUNDIDAD + " niveles";
                }
            } else if (ev instanceof CollectionEndEvent) {
                profundidad--;
            }
        }
        return null;
    }

    /**
     * Primera etiqueta que no es mapa, secuencia ni la que el YAML resolvería solo para un escalar (como en
     * LectorDescriptor: se rechaza cualquier etiqueta explícita que no sea la implícita, p. ej. {@code !!int}).
     */
    private static Tag etiquetaNoPermitida(Node nodo, IdentityHashMap<Node, Boolean> vistos, LoadSettings ajustes) {
        if (vistos.put(nodo, Boolean.TRUE) != null) {
            return null;
        }
        switch (nodo) {
            case MappingNode m -> {
                if (!nodo.getTag().equals(Tag.MAP)) {
                    return nodo.getTag();
                }
                for (NodeTuple t : m.getValue()) {
                    var r = etiquetaNoPermitida(t.getKeyNode(), vistos, ajustes);
                    if (r == null) {
                        r = etiquetaNoPermitida(t.getValueNode(), vistos, ajustes);
                    }
                    if (r != null) {
                        return r;
                    }
                }
            }
            case SequenceNode s -> {
                if (!nodo.getTag().equals(Tag.SEQ)) {
                    return nodo.getTag();
                }
                for (Node hijo : s.getValue()) {
                    var r = etiquetaNoPermitida(hijo, vistos, ajustes);
                    if (r != null) {
                        return r;
                    }
                }
            }
            case ScalarNode e -> {
                var implicita = e.getScalarStyle() == ScalarStyle.PLAIN
                        ? ajustes.getSchema().getScalarResolver().resolve(e.getValue(), true)
                        : Tag.STR;
                if (!e.getTag().equals(implicita)) {
                    return e.getTag();
                }
            }
            default -> {
                return nodo.getTag();
            }
        }
        return null;
    }

    private static Descriptor provisional(String nombre, Version version) {
        var rango = ((Resultado.Exito<RangoVersion>) RangoVersion.parsear(">=0.0.0")).valor();
        return new Descriptor(nombre, version, CORE, rango, Map.of(), Optional.empty());
    }
}
