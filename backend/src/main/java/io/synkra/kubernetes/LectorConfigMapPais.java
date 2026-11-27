package io.synkra.kubernetes;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.synkra.kubernetes.api.LecturaPais;
import io.synkra.kubernetes.api.LecturaPais.PluginLeido;
import io.synkra.kubernetes.api.LecturaPaisInconsistenteException;
import io.synkra.kubernetes.api.PlanPais;
import io.synkra.kubernetes.api.PlanPais.PluginPlan;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.snakeyaml.engine.v2.api.Load;
import org.snakeyaml.engine.v2.api.LoadSettings;
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

/**
 * Convierte el Deployment y el ConfigMap leídos en una {@link LecturaPais}. El ConfigMap puede haberlo cambiado
 * alguien a mano (H-19): se lee con SnakeYAML Engine sin etiquetas de tipo y con tope, y cualquier inconsistencia
 * lanza; nunca se devuelve una lectura parcial.
 */
final class LectorConfigMapPais {

    /** Tope de cada archivo del ConfigMap, igual que el del core. */
    static final int MAX_BYTES = 64 * 1024;
    static final int MAX_ALIAS = 10;
    static final int MAX_PROFUNDIDAD = 32;
    private static final Pattern LINEA_ARTEFACTO = Pattern.compile("([^ ]+)" + Pattern.quote(ConstructorObjetosPais.SEPARADOR_ARTEFACTO) + "([^ ]+" + Pattern.quote(ConstructorObjetosPais.SUFIJO_JAR) + ")");

    private LectorConfigMapPais() {}

    static LecturaPais leer(String pais, Deployment d, ConfigMap c) {
        String imagen = imagenCore(pais, d);
        String yaml = dato(pais, c, ConstructorObjetosPais.CLAVE_PLUGINS);
        String txt = dato(pais, c, ConstructorObjetosPais.CLAVE_ARTEFACTOS);
        List<String[]> activos = activos(pais, yaml);
        Map<String, String> shaPorJar = artefactos(pais, txt);
        List<PluginPlan> plugins = new ArrayList<>();
        for (String[] a : activos) {
            String jar = ConstructorObjetosPais.nombreJar(a[0], a[1]);
            String sha = shaPorJar.remove(jar);
            if (sha == null) {
                throw new LecturaPaisInconsistenteException(pais, "el plugin " + a[0] + " " + a[1] + " está en "
                        + ConstructorObjetosPais.CLAVE_PLUGINS + " y no tiene línea " + jar + " en "
                        + ConstructorObjetosPais.CLAVE_ARTEFACTOS);
            }
            plugins.add(valida(pais, a[0], () -> new PluginPlan(a[0], a[1], sha)));
        }
        if (!shaPorJar.isEmpty()) {
            throw new LecturaPaisInconsistenteException(pais, ConstructorObjetosPais.CLAVE_ARTEFACTOS + " lista "
                    + shaPorJar.keySet() + " y no está en " + ConstructorObjetosPais.CLAVE_PLUGINS);
        }
        // PlanPais aplica la misma regla de formato que al construir (país, imagen por digest).
        PlanPais plan = valida(pais, "core", () -> new PlanPais(pais, imagen, plugins));
        return new LecturaPais(pais, plan.imagenCore(), plan.plugins().stream()
                .map(p -> new PluginLeido(p.nombre(), p.version(), p.sha256())).toList());
    }

    private static <T> T valida(String pais, String que, java.util.function.Supplier<T> s) {
        try {
            return s.get();
        } catch (IllegalArgumentException e) {
            throw new LecturaPaisInconsistenteException(pais, "valor inválido en " + que + ": " + e.getMessage(), e);
        }
    }

    private static String imagenCore(String pais, Deployment d) {
        var spec = d.getSpec() != null && d.getSpec().getTemplate() != null ? d.getSpec().getTemplate().getSpec() : null;
        List<Container> contenedores = spec != null && spec.getContainers() != null ? spec.getContainers() : List.of();
        return contenedores.stream().filter(k -> ConstructorObjetosPais.CONTENEDOR_CORE.equals(k.getName()))
                .map(Container::getImage).filter(i -> i != null).findFirst()
                .orElseThrow(() -> new LecturaPaisInconsistenteException(pais,
                        "el Deployment no tiene el contenedor " + ConstructorObjetosPais.CONTENEDOR_CORE + " con imagen"));
    }

    private static String dato(String pais, ConfigMap c, String clave) {
        String v = c.getData() == null ? null : c.getData().get(clave);
        if (v == null) {
            throw new LecturaPaisInconsistenteException(pais, "el ConfigMap no tiene la clave " + clave);
        }
        if (v.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES) {
            throw new LecturaPaisInconsistenteException(pais, clave + " supera el tamaño máximo de " + MAX_BYTES + " bytes");
        }
        return v;
    }

    /** Líneas {@code <sha256> <nombre>-<version>.jar}; clave: el nombre del jar. */
    private static Map<String, String> artefactos(String pais, String txt) {
        Map<String, String> r = new HashMap<>();
        for (String linea : txt.split("\n")) {
            if (linea.isBlank()) {
                continue;
            }
            var m = LINEA_ARTEFACTO.matcher(linea);
            if (!m.matches() || r.put(m.group(2), m.group(1)) != null) {
                throw new LecturaPaisInconsistenteException(pais, ConstructorObjetosPais.CLAVE_ARTEFACTOS
                        + ": línea mal formada o repetida");
            }
        }
        return r;
    }

    /** Lista en la raíz de mapas {nombre, version}, ambos texto; el mismo contrato que el core. */
    private static List<String[]> activos(String pais, String yaml) {
        String clave = ConstructorObjetosPais.CLAVE_PLUGINS;
        Object raiz;
        try {
            var ajustes = LoadSettings.builder().setSchema(new CoreSchema()).setMaxAliasesForCollections(MAX_ALIAS).build();
            exigeProfundidad(ajustes, yaml);
            var nodo = new Composer(ajustes, new ParserImpl(ajustes, new StreamReader(ajustes, yaml))).getSingleNode().orElse(null);
            if (nodo != null) {
                Tag etiqueta = etiquetaNoPermitida(nodo, new IdentityHashMap<>(), ajustes);
                if (etiqueta != null) {
                    throw new LecturaPaisInconsistenteException(pais, clave + ": etiqueta de tipo '" + etiqueta.getValue()
                            + "' no permitida");
                }
            }
            raiz = new Load(ajustes).loadFromString(yaml);
        } catch (LecturaPaisInconsistenteException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new LecturaPaisInconsistenteException(pais, clave + " no es un YAML válido o no es seguro", e);
        }
        if (raiz == null) {
            return List.of();
        }
        if (!(raiz instanceof List<?> lista)) {
            throw new LecturaPaisInconsistenteException(pais, clave + " debe ser una lista de {nombre, version}");
        }
        List<String[]> r = new ArrayList<>();
        for (Object o : lista) {
            if (!(o instanceof Map<?, ?> m) || m.size() != 2
                    || !(m.get(ConstructorObjetosPais.CLAVE_NOMBRE) instanceof String n) || !(m.get(ConstructorObjetosPais.CLAVE_VERSION) instanceof String v)) {
                throw new LecturaPaisInconsistenteException(pais,
                        clave + ": cada entrada debe ser {nombre, version} con ambos como texto");
            }
            r.add(new String[] {n, v});
        }
        return r;
    }

    private static void exigeProfundidad(LoadSettings ajustes, String yaml) {
        var parser = new ParserImpl(ajustes, new StreamReader(ajustes, yaml));
        int profundidad = 0;
        while (parser.hasNext()) {
            var ev = parser.next();
            if (ev instanceof CollectionStartEvent) {
                if (++profundidad > MAX_PROFUNDIDAD) {
                    throw new IllegalArgumentException("anidación excesiva");
                }
            } else if (ev instanceof CollectionEndEvent) {
                profundidad--;
            }
        }
    }

    /** Primera etiqueta que no sea mapa, secuencia o texto. */
    private static Tag etiquetaNoPermitida(Node nodo, IdentityHashMap<Node, Boolean> vistos, LoadSettings ajustes) {
        if (vistos.put(nodo, Boolean.TRUE) != null) {
            return null;
        }
        Tag t = nodo.getTag();
        switch (nodo) {
            case MappingNode m -> {
                if (!t.equals(Tag.MAP)) {
                    return t;
                }
                for (NodeTuple tupla : m.getValue()) {
                    Tag r = etiquetaNoPermitida(tupla.getKeyNode(), vistos, ajustes);
                    if (r == null) {
                        r = etiquetaNoPermitida(tupla.getValueNode(), vistos, ajustes);
                    }
                    if (r != null) {
                        return r;
                    }
                }
                return null;
            }
            case SequenceNode s -> {
                if (!t.equals(Tag.SEQ)) {
                    return t;
                }
                for (Node hijo : s.getValue()) {
                    Tag r = etiquetaNoPermitida(hijo, vistos, ajustes);
                    if (r != null) {
                        return r;
                    }
                }
                return null;
            }
            case ScalarNode e -> {
                // Un escalar sin comillas solo es válido si su etiqueta es la que el esquema le da a ese valor (la
                // regla del core): "1.0" implícito (FLOAT) pasa y falla luego por no ser texto; "!!null x" no.
                Tag implicita = e.isPlain() ? ajustes.getSchema().getScalarResolver().resolve(e.getValue(), true) : Tag.STR;
                return t.equals(Tag.STR) || t.equals(implicita) ? null : t;
            }
            default -> {
                return t;
            }
        }
    }
}
