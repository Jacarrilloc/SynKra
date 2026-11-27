package io.synkra.catalogo;

import io.synkra.catalogo.api.PluginSubido;
import io.synkra.plataforma.CodigosError;
import io.synkra.plataforma.DetalleError;
import io.synkra.plataforma.ErrorApi;
import io.synkra.plataforma.ExcepcionApi;
import io.synkra.verificador.Descriptor;
import io.synkra.verificador.RangoVersion;
import io.synkra.verificador.Resultado;
import io.synkra.verificador.Version;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * R-C12: una sola consulta con parámetros enlazados. Cada pareja del filtro es un {@code EXISTS} sobre
 * {@code cat_etiqueta} (AND, cualquier origen); {@code =} de SQLite compara en binario, es decir exacto y con
 * mayúsculas. Las etiquetas de cada plugin vienen en las mismas filas (LEFT JOIN), así que no hay consulta por plugin.
 */
@Component
class ListadoPlugins {

    /** El {@code campo} de los 422 del filtro. */
    static final String CAMPO_FILTRO = "filtro";

    /**
     * Tope de parejas del filtro: cada una añade un EXISTS y SQLite limita la profundidad de la expresión a 1000. */
    static final int MAX_PAREJAS = 50;
    // Igual a SubidaPlugin.MAX_DETALLES a propósito: las claves malas caben todas y acotar no recorta en el servicio.

    private static final String SELECT = "SELECT p.nombre, p.version, p.core_compatible, p.sha256, p.tamano, "
            + "t.clave, t.valor FROM cat_plugin p LEFT JOIN cat_etiqueta t "
            + "ON t.core = p.core AND t.plugin = p.nombre AND t.version = p.version WHERE p.core = ?";
    private static final String PAREJA = " AND EXISTS (SELECT 1 FROM cat_etiqueta f WHERE f.core = p.core "
            + "AND f.plugin = p.nombre AND f.version = p.version AND f.clave = ? AND f.valor = ?)";

    private final JdbcTemplate jdbc;

    ListadoPlugins(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    List<PluginSubido> listar(String core, Map<String, String> filtro) {
        var parejas = validar(filtro);
        var sql = new StringBuilder(SELECT);
        var args = new ArrayList<Object>();
        args.add(core);
        parejas.forEach((k, v) -> {
            sql.append(PAREJA);
            args.add(k);
            args.add(v);
        });
        var porClave = new LinkedHashMap<String, Fila>();
        jdbc.query(sql.toString(), rs -> {
            var nombre = rs.getString("nombre");
            var version = rs.getString("version");
            var f = porClave.computeIfAbsent(nombre + "\0" + version, k -> {
                try {
                    return new Fila(nombre, version, rs.getString("core_compatible"), rs.getString("sha256"),
                            rs.getLong("tamano"), new TreeMap<>());
                } catch (java.sql.SQLException e) {
                    throw new IllegalStateException(e);
                }
            });
            var clave = rs.getString("clave");
            if (clave != null) {
                f.etiquetas().put(clave, rs.getString("valor"));
            }
        }, args.toArray());
        return porClave.values().stream()
                .map(f -> new PluginSubido(core, f.nombre(), deLaBase(Version.parsear(f.version()), f),
                        deLaBase(RangoVersion.parsear(f.coreCompatible()), f), f.etiquetas(), f.sha256(), f.tamano()))
                .sorted(Comparator.comparing(PluginSubido::nombre).thenComparing(PluginSubido::version))
                .toList();
    }

    private static Map<String, String> validar(Map<String, String> filtro) {
        if (filtro == null) {
            return Map.of();
        }
        if (filtro.size() > MAX_PAREJAS) {
            throw new ExcepcionApi(HttpStatus.UNPROCESSABLE_ENTITY, new ErrorApi(CodigosError.ENTIDAD_NO_PROCESABLE,
                    CodigosError.MSG_ENTIDAD_NO_PROCESABLE,
                    List.of(new DetalleError(CAMPO_FILTRO, MotivosCatalogo.FILTRO_LARGO.formatted(MAX_PAREJAS)))));
        }
        var detalles = new ArrayList<DetalleError>();
        var parejas = new TreeMap<String, String>();
        filtro.forEach((clave, valor) -> {
            if (clave == null || !Descriptor.NOMBRE.matcher(clave).matches()) {
                var corta = RecepcionArtefacto.recortar(String.valueOf(clave));
                detalles.add(new DetalleError(corta, MotivosCatalogo.ETIQUETA_CLAVE_INVALIDA.formatted(corta)));
            } else if (valor == null) {
                detalles.add(new DetalleError(clave, MotivosCatalogo.ETIQUETA_SIN_VALOR.formatted(clave)));
            } else {
                parejas.put(clave, valor);
            }
        });
        if (!detalles.isEmpty()) {
            throw new ExcepcionApi(HttpStatus.UNPROCESSABLE_ENTITY, new ErrorApi(CodigosError.ENTIDAD_NO_PROCESABLE,
                    CodigosError.MSG_ENTIDAD_NO_PROCESABLE, SubidaPlugin.acotar(detalles, CAMPO_FILTRO)));
        }
        return parejas;
    }

    private static <T> T deLaBase(Resultado<T> r, Fila f) {
        return switch (r) {
            case Resultado.Exito<T> ok -> ok.valor();
            case Resultado.Fallo<T> fallo ->
                throw new IllegalStateException("Plugin no válido en la base: " + f.nombre() + "/" + f.version());
        };
    }

    private record Fila(String nombre, String version, String coreCompatible, String sha256, long tamano,
            Map<String, String> etiquetas) {}
}
