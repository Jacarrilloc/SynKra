package io.synkra.verificador;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.zip.ZipFile;
import org.snakeyaml.engine.v2.api.Dump;
import org.snakeyaml.engine.v2.api.DumpSettings;
import org.snakeyaml.engine.v2.api.LoadSettings;
import org.snakeyaml.engine.v2.common.FlowStyle;
import org.snakeyaml.engine.v2.schema.CoreSchema;
import org.snakeyaml.engine.v2.composer.Composer;
import org.snakeyaml.engine.v2.nodes.MappingNode;
import org.snakeyaml.engine.v2.nodes.Node;
import org.snakeyaml.engine.v2.nodes.NodeTuple;
import org.snakeyaml.engine.v2.nodes.ScalarNode;
import org.snakeyaml.engine.v2.nodes.Tag;
import org.snakeyaml.engine.v2.parser.ParserImpl;
import org.snakeyaml.engine.v2.scanner.StreamReader;

/**
 * Lee y valida el {@code synkra-plugin.yaml} (R-V3). El YAML es dato de un tercero: se compone como árbol de nodos
 * (nunca se construyen objetos), solo se admiten las etiquetas básicas de YAML y hay límites de alias y de tamaño.
 * Nunca lanza por culpa del archivo.
 */
public final class LectorDescriptor {

    public static final String ARCHIVO = "synkra-plugin.yaml";
    // Límites de R-V3 (business-rules.md, «Límites del lector»): si cambian, cambian allí también.
    static final int MAX_BYTES = 64 * 1024;
    static final int MAX_ALIAS = 10;
    static final int MAX_PROFUNDIDAD = 32;
    private static final Set<String> CONOCIDOS =
            Set.of("nombre", "version", "core", "coreCompatible", "etiquetas", "descripcion");
    private static final Set<Tag> TAGS_BASICOS =
            Set.of(Tag.MAP, Tag.SEQ, Tag.STR, Tag.INT, Tag.FLOAT, Tag.BOOL, Tag.NULL);

    private LectorDescriptor() {}

    public static Resultado<Descriptor> leer(Path jar) {
        if (jar == null) {
            return invalido("No se indicó el JAR del plugin", null);
        }
        try (var zip = new ZipFile(jar.toFile())) {
            var entrada = zip.getEntry(ARCHIVO);
            if (entrada == null || entrada.isDirectory()) {
                return Resultado.fallo(Motivo.de(
                        CodigoMotivo.DESCRIPTOR_SIN_ARCHIVO, "El JAR no trae " + ARCHIVO + " en su raíz"));
            }
            try (InputStream in = zip.getInputStream(entrada)) {
                var bytes = in.readNBytes(MAX_BYTES + 1);
                if (bytes.length > MAX_BYTES) {
                    return invalido(ARCHIVO + " supera el tamaño máximo de " + MAX_BYTES + " bytes", null);
                }
                var texto = StandardCharsets.UTF_8
                        .newDecoder()
                        .onMalformedInput(CodingErrorAction.REPORT)
                        .onUnmappableCharacter(CodingErrorAction.REPORT)
                        .decode(java.nio.ByteBuffer.wrap(bytes))
                        .toString();
                return desdeYaml(texto);
            }
        } catch (CharacterCodingException e) {
            return invalido(ARCHIVO + " no es UTF-8 válido", null);
        } catch (IOException | RuntimeException e) {
            return invalido("No se pudo leer el JAR como archivo ZIP: " + e.getMessage(), null);
        }
    }

    public static Resultado<Descriptor> desdeYaml(String yaml) {
        if (yaml == null) {
            return invalido("El YAML del descriptor es nulo", null);
        }
        if (yaml.length() > MAX_BYTES) {
            return invalido("El descriptor supera el tamaño máximo de " + MAX_BYTES + " caracteres", null);
        }
        Node raiz;
        try {
            var ajustes = LoadSettings.builder()
                    .setSchema(new CoreSchema()) // el esquema JSON por defecto no resuelve «~» como nulo
                    // Solo se compone (no se construye): las claves duplicadas se detectan en validar(), y las
                    // claves no escalares (incluidas las recursivas) las rechaza el Composer por defecto.
                    .setMaxAliasesForCollections(MAX_ALIAS)
                    .build();
            var excesiva = profundidadExcesiva(ajustes, yaml);
            if (excesiva != null) {
                return invalido(excesiva, null);
            }
            var composer = new Composer(ajustes, new ParserImpl(ajustes, new StreamReader(ajustes, yaml)));
            raiz = composer.getSingleNode().orElse(null);
            if (raiz != null) {
                var cortes = comprobarEtiquetas(raiz, new java.util.IdentityHashMap<>());
                if (cortes != null) {
                    return invalido(cortes, null);
                }
            }
        } catch (RuntimeException e) { // YamlEngineException y similares: el YAML es ilegible
            return invalido("El YAML del descriptor no es válido o no es seguro: " + e.getMessage(), null);
        }
        if (!(raiz instanceof MappingNode mapa)) {
            return invalido("El descriptor debe ser un mapa de campos", null);
        }
        return validar(mapa);
    }

    /** Cuenta la anidación con los eventos del parser (iterativo) antes de componer, que es recursivo. */
    private static String profundidadExcesiva(LoadSettings ajustes, String yaml) {
        var parser = new ParserImpl(ajustes, new StreamReader(ajustes, yaml));
        int profundidad = 0;
        while (parser.hasNext()) {
            var ev = parser.next();
            if (ev instanceof org.snakeyaml.engine.v2.events.CollectionStartEvent) {
                if (++profundidad > MAX_PROFUNDIDAD) {
                    return "El descriptor supera la anidación máxima de " + MAX_PROFUNDIDAD + " niveles";
                }
            } else if (ev instanceof org.snakeyaml.engine.v2.events.CollectionEndEvent) {
                profundidad--;
            }
        }
        return null;
    }

    /** Devuelve un mensaje si hay una etiqueta no básica; recorre sin repetir nodos (los alias comparten nodo). */
    private static String comprobarEtiquetas(Node nodo, java.util.IdentityHashMap<Node, Boolean> vistos) {
        if (vistos.put(nodo, Boolean.TRUE) != null) {
            return null;
        }
        if (!TAGS_BASICOS.contains(nodo.getTag())) {
            return "El descriptor usa la etiqueta de tipo '" + nodo.getTag().getValue() + "', que no está permitida";
        }
        if (nodo instanceof MappingNode m) {
            for (NodeTuple t : m.getValue()) {
                var r = comprobarEtiquetas(t.getKeyNode(), vistos);
                if (r == null) {
                    r = comprobarEtiquetas(t.getValueNode(), vistos);
                }
                if (r != null) {
                    return r;
                }
            }
        } else if (nodo instanceof org.snakeyaml.engine.v2.nodes.SequenceNode s) {
            for (Node hijo : s.getValue()) {
                var r = comprobarEtiquetas(hijo, vistos);
                if (r != null) {
                    return r;
                }
            }
        }
        return null;
    }

    /** Clase de un escalar según su etiqueta (resuelta o explícita). */
    private static boolean esStr(ScalarNode n) {
        return n.getTag().equals(Tag.STR);
    }

    private static boolean esNulo(Node n) {
        return n instanceof ScalarNode s && s.getTag().equals(Tag.NULL);
    }

    /** Una etiqueta explícita que no es str ni lo que el YAML resolvería solo (p. ej. {@code !!int 1.0.0}). */
    private static boolean etiquetaExplicitaNoStr(ScalarNode n, LoadSettings ajustes) {
        if (esStr(n)) {
            return false;
        }
        var implicita = n.getScalarStyle() == org.snakeyaml.engine.v2.common.ScalarStyle.PLAIN
                ? ajustes.getSchema().getScalarResolver().resolve(n.getValue(), true)
                : Tag.STR;
        return !n.getTag().equals(implicita);
    }

    private static final LoadSettings AJUSTES = LoadSettings.builder().setSchema(new CoreSchema()).build();

    private static Resultado<Descriptor> validar(MappingNode mapa) {
        var motivos = new ArrayList<Motivo>();
        var campos = new LinkedHashMap<String, Node>();
        for (NodeTuple t : mapa.getValue()) {
            if (!(t.getKeyNode() instanceof ScalarNode clave)) {
                motivos.add(Motivo.de(CodigoMotivo.DESCRIPTOR_INVALIDO, "Las claves del descriptor deben ser texto"));
                continue;
            }
            if (!CONOCIDOS.contains(clave.getValue())) {
                motivos.add(Motivo.de(
                        CodigoMotivo.DESCRIPTOR_CAMPO_DESCONOCIDO,
                        "El campo '" + clave.getValue() + "' no existe en el descriptor",
                        clave.getValue()));
                continue;
            }
            if (campos.put(clave.getValue(), t.getValueNode()) != null) {
                motivos.add(Motivo.de(
                        CodigoMotivo.DESCRIPTOR_INVALIDO,
                        "El campo '" + clave.getValue() + "' está repetido",
                        clave.getValue()));
            }
        }
        for (var obligatorio : OBLIGATORIOS_EN_ORDEN) {
            var nodo = campos.get(obligatorio);
            if (nodo == null || esNulo(nodo)) {
                motivos.add(Motivo.de(
                        CodigoMotivo.DESCRIPTOR_FALTA_CAMPO,
                        "Falta el campo obligatorio '" + obligatorio + "'",
                        obligatorio));
                campos.remove(obligatorio);
            }
        }
        // nombre y core: str; un escalar que YAML resuelve como bool, int o float es del tipo equivocado.
        var nombre = texto(campos, "nombre", true, motivos);
        var core = texto(campos, "core", true, motivos);
        patron(nombre, "nombre", motivos);
        patron(core, "core", motivos);
        // version y coreCompatible: el texto del escalar plano (2.1 llega como "2.1" y da VERSION_INVALIDA).
        var version = texto(campos, "version", false, motivos);
        var rango = texto(campos, "coreCompatible", false, motivos);
        Version v = null;
        if (version != null) {
            switch (Version.parsear(version)) {
                case Resultado.Exito<Version> e -> v = e.valor();
                case Resultado.Fallo<Version> f -> motivos.addAll(f.motivos());
            }
        }
        RangoVersion r = null;
        if (rango != null) {
            switch (RangoVersion.parsear(rango)) {
                case Resultado.Exito<RangoVersion> e -> r = e.valor();
                case Resultado.Fallo<RangoVersion> f -> motivos.addAll(f.motivos());
            }
        }
        // descripcion: str; un nulo explícito cuenta como ausente.
        String descripcion = null;
        var nodoDescripcion = campos.get("descripcion");
        if (nodoDescripcion != null && !esNulo(nodoDescripcion)) {
            descripcion = texto(campos, "descripcion", true, motivos);
        }
        var etiquetas = etiquetas(campos.get("etiquetas"), motivos);
        if (!motivos.isEmpty()) {
            return new Resultado.Fallo<>(motivos);
        }
        return Resultado.exito(new Descriptor(nombre, v, core, r, etiquetas, Optional.ofNullable(descripcion)));
    }

    private static final List<String> OBLIGATORIOS_EN_ORDEN = List.of("nombre", "version", "core", "coreCompatible");

    private static String texto(Map<String, Node> campos, String campo, boolean soloStr, List<Motivo> motivos) {
        var nodo = campos.get(campo);
        if (nodo == null) {
            return null;
        }
        if (nodo instanceof ScalarNode s) {
            if (soloStr ? !esStr(s) : etiquetaExplicitaNoStr(s, AJUSTES)) {
                motivos.add(Motivo.de(
                        CodigoMotivo.DESCRIPTOR_INVALIDO,
                        "El campo '" + campo + "' debe ser un texto, no '" + s.getValue() + "' de tipo "
                                + s.getTag().getValue(),
                        campo));
                return null;
            }
            return s.getValue();
        }
        motivos.add(Motivo.de(CodigoMotivo.DESCRIPTOR_INVALIDO, "El campo '" + campo + "' debe ser un texto", campo));
        return null;
    }

    private static void patron(String valor, String campo, List<Motivo> motivos) {
        if (valor != null && !Descriptor.NOMBRE.matcher(valor).matches()) {
            motivos.add(Motivo.de(
                    CodigoMotivo.DESCRIPTOR_INVALIDO,
                    "El campo '" + campo + "' vale '" + valor + "' y debe cumplir " + Descriptor.NOMBRE.pattern(),
                    campo));
        }
    }

    private static Map<String, String> etiquetas(Node nodo, List<Motivo> motivos) {
        var salida = new TreeMap<String, String>();
        if (nodo == null || esNulo(nodo)) {
            return salida;
        }
        if (!(nodo instanceof MappingNode m)) {
            motivos.add(Motivo.de(
                    CodigoMotivo.DESCRIPTOR_INVALIDO, "El campo 'etiquetas' debe ser un mapa de texto a texto", "etiquetas"));
            return salida;
        }
        for (NodeTuple t : m.getValue()) {
            if (t.getKeyNode() instanceof ScalarNode k && esStr(k) && t.getValueNode() instanceof ScalarNode v && esStr(v)) {
                if (!Descriptor.NOMBRE.matcher(k.getValue()).matches()) {
                    motivos.add(Motivo.de(
                            CodigoMotivo.DESCRIPTOR_INVALIDO,
                            "La clave de etiqueta '" + k.getValue() + "' debe cumplir " + Descriptor.NOMBRE.pattern(),
                            "etiquetas"));
                } else if (salida.put(k.getValue(), v.getValue()) != null) {
                    motivos.add(Motivo.de(
                            CodigoMotivo.DESCRIPTOR_INVALIDO,
                            "La etiqueta '" + k.getValue() + "' está repetida",
                            "etiquetas"));
                }
            } else {
                motivos.add(Motivo.de(
                        CodigoMotivo.DESCRIPTOR_INVALIDO,
                        "Cada etiqueta debe ser un par de texto a texto (clave y valor son str: los nulos, números y booleanos no valen)",
                        "etiquetas"));
            }
        }
        return salida;
    }

    /** Serializa un descriptor al YAML que {@link #desdeYaml} lee de vuelta. */
    public static String aYaml(Descriptor d) {
        var m = new LinkedHashMap<String, Object>();
        m.put("nombre", d.nombre());
        m.put("version", d.version().toString());
        m.put("core", d.core());
        m.put("coreCompatible", d.coreCompatible().texto());
        if (!d.etiquetas().isEmpty()) {
            m.put("etiquetas", new TreeMap<>(d.etiquetas()));
        }
        d.descripcion().ifPresent(x -> m.put("descripcion", x));
        var ajustes = DumpSettings.builder()
                .setDefaultFlowStyle(FlowStyle.BLOCK)
                .setSchema(new CoreSchema()) // el mismo esquema que desdeYaml: los textos ambiguos salen entre comillas
                .build();
        return new Dump(ajustes).dumpToString(m);
    }

    private static Resultado<Descriptor> invalido(String mensaje, String campo) {
        return Resultado.fallo(
                campo == null
                        ? Motivo.de(CodigoMotivo.DESCRIPTOR_INVALIDO, mensaje)
                        : Motivo.de(CodigoMotivo.DESCRIPTOR_INVALIDO, mensaje, campo));
    }
}
