package io.synkra.registro;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import io.synkra.registro.api.ContextoFallo;
import io.synkra.registro.api.DespliegueRegistrado;
import io.synkra.registro.api.EstadoDespliegue;
import io.synkra.registro.api.NuevoDespliegue;
import io.synkra.registro.api.PluginDesplegado;
import io.synkra.registro.api.RegistroApi;
import io.synkra.registro.api.TipoDespliegue;
import io.synkra.registro.api.TransicionNoPermitidaException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.NoSuchElementException;
import java.util.TreeMap;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jdbc.core.JdbcAggregateTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** R-R1 a R-R4. Sin bean {@code Clock}: el reloj es privado (U07 corre en paralelo y no debe chocar). */
@Service
class RegistroServicio implements RegistroApi {

    private static final Pattern PAIS = Pattern.compile("[a-z]{2}");
    private static final Pattern SHA256 = Pattern.compile("[0-9a-f]{64}");

    private final JdbcAggregateTemplate plantilla;
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;
    private final Clock reloj;

    @Autowired
    RegistroServicio(JdbcAggregateTemplate plantilla, JdbcTemplate jdbc, ObjectMapper json) {
        this(plantilla, jdbc, json, Clock.systemUTC());
    }

    RegistroServicio(JdbcAggregateTemplate plantilla, JdbcTemplate jdbc, ObjectMapper json, Clock reloj) {
        this.plantilla = plantilla;
        this.jdbc = jdbc;
        this.json = json;
        this.reloj = reloj;
    }

    /** R-R1: se une a la transacción de quien llama (REQUIRED); nunca REQUIRES_NEW. */
    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void registrar(NuevoDespliegue nuevo) {
        var plugins = validar(nuevo);
        plantilla.insert(new Despliegue(nuevo.id(), nuevo.pais(), minusculas(nuevo.tipo()), nuevo.origenId(),
                nuevo.core(), nuevo.versionCore(), nuevo.imagen(), nuevo.creadoPor(), nuevo.confirmadoPor(),
                nuevo.confirmadoEn().toEpochMilli(), minusculas(EstadoDespliegue.APLICANDO), null,
                nuevo.veredicto()));
        for (var p : plugins) {
            plantilla.insert(new PluginRegistrado(nuevo.id(), p.nombre(), p.version(), p.sha256(), etiquetas(p)));
        }
    }

    /** R-R2 y R-R3. La guarda vive en el WHERE del UPDATE. */
    @Override
    @Transactional
    public void actualizarResultado(long id, EstadoDespliegue resultado, ContextoFallo contexto) {
        if (resultado == null) {
            throw new IllegalArgumentException("resultado obligatorio");
        }
        if (resultado == EstadoDespliegue.APLICANDO) {
            throw new TransicionNoPermitidaException(
                    "despliegue " + id + ": no se puede pedir 'aplicando' como resultado");
        }
        if (resultado == EstadoDespliegue.FALLIDO && contexto == null) {
            throw new IllegalArgumentException("un despliegue fallido exige contexto (R-R3)");
        }
        if (resultado == EstadoDespliegue.LISTO && contexto != null) {
            throw new IllegalArgumentException("un despliegue listo no guarda contexto (R-R3)");
        }
        int filas = jdbc.update("update reg_despliegue set estado = ?, terminado_en = ? "
                + "where id = ? and estado = 'aplicando'", minusculas(resultado), reloj.millis(), id);
        if (filas == 0) {
            var actual = jdbc.query("select estado from reg_despliegue where id = ?",
                    (rs, n) -> rs.getString(1), id);
            if (actual.isEmpty()) {
                throw new NoSuchElementException("no existe el despliegue " + id);
            }
            throw new TransicionNoPermitidaException("despliegue " + id + " está en " + actual.get(0)
                    + ": no se puede pasar a " + minusculas(resultado));
        }
        if (resultado == EstadoDespliegue.FALLIDO) {
            plantilla.insert(new ContextoGuardado(id, TruncadoContexto.logCore(contexto.logCore()),
                    TruncadoContexto.texto(contexto.logInit()), TruncadoContexto.texto(contexto.eventos())));
        }
    }

    private static final String COLUMNAS = "id, pais, tipo, origen_id, core, version_core, imagen, creado_por, "
            + "confirmado_por, confirmado_en, estado, terminado_en, veredicto";

    @Override
    @Transactional(readOnly = true)
    public Optional<DespliegueRegistrado> obtener(long id) {
        if (id <= 0) {
            return Optional.empty();
        }
        var filas = jdbc.query("select " + COLUMNAS + " from reg_despliegue where id = ?",
                RegistroServicio::fila, id);
        if (filas.isEmpty()) {
            return Optional.empty();
        }
        var base = filas.get(0);
        var plugins = pluginsDe(List.of(id)).getOrDefault(id, List.of());
        ContextoFallo contexto = null;
        if (base.estado() == EstadoDespliegue.FALLIDO) {
            var c = jdbc.query("select log_core, log_init, eventos from reg_contexto where despliegue_id = ?",
                    (rs, n) -> new ContextoFallo(rs.getString(1), rs.getString(2), rs.getString(3)), id);
            contexto = c.isEmpty() ? null : c.get(0);
        }
        return Optional.of(con(base, plugins, contexto));
    }

    /** R-R5: el desempate por id es parte de la regla (dos confirmaciones en el mismo milisegundo). */
    @Override
    @Transactional(readOnly = true)
    public List<DespliegueRegistrado> historial(String pais) {
        if (pais == null || !PAIS.matcher(pais).matches()) {
            throw new IllegalArgumentException("pais: dos letras minúsculas");
        }
        var filas = jdbc.query("select " + COLUMNAS + " from reg_despliegue where pais = ? "
                + "order by confirmado_en desc, id desc", RegistroServicio::fila, pais);
        if (filas.isEmpty()) {
            return List.of();
        }
        var plugins = pluginsDe(filas.stream().map(DespliegueRegistrado::id).toList());
        var salida = new ArrayList<DespliegueRegistrado>(filas.size());
        for (var f : filas) {
            salida.add(con(f, plugins.getOrDefault(f.id(), List.of()), null));
        }
        return List.copyOf(salida);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DespliegueRegistrado> ultimoListo(String pais) {
        throw new UnsupportedOperationException("pendiente");
    }

    private static DespliegueRegistrado con(DespliegueRegistrado f, List<PluginDesplegado> plugins,
            ContextoFallo contexto) {
        return new DespliegueRegistrado(f.id(), f.pais(), f.tipo(), f.origenId(), f.core(), f.versionCore(),
                f.imagen(), f.creadoPor(), f.confirmadoPor(), f.confirmadoEn(), f.estado(), f.terminadoEn(),
                f.veredicto(), plugins, contexto);
    }

    private static DespliegueRegistrado fila(ResultSet rs, int n) throws SQLException {
        long origen = rs.getLong("origen_id");
        Long origenId = rs.wasNull() ? null : origen;
        long fin = rs.getLong("terminado_en");
        Instant terminado = rs.wasNull() ? null : Instant.ofEpochMilli(fin);
        return new DespliegueRegistrado(rs.getLong("id"), rs.getString("pais"),
                TipoDespliegue.valueOf(rs.getString("tipo").toUpperCase(java.util.Locale.ROOT)), origenId,
                rs.getString("core"), rs.getString("version_core"), rs.getString("imagen"),
                rs.getString("creado_por"), rs.getString("confirmado_por"),
                Instant.ofEpochMilli(rs.getLong("confirmado_en")),
                EstadoDespliegue.valueOf(rs.getString("estado").toUpperCase(java.util.Locale.ROOT)), terminado,
                rs.getString("veredicto"), List.of(), null);
    }

    /** Una sola consulta por llamada, ordenada por nombre. */
    private Map<Long, List<PluginDesplegado>> pluginsDe(List<Long> ids) {
        var marcas = String.join(",", java.util.Collections.nCopies(ids.size(), "?"));
        var salida = new LinkedHashMap<Long, List<PluginDesplegado>>();
        jdbc.query("select despliegue_id, nombre, version, sha256, etiquetas from reg_plugin "
                + "where despliegue_id in (" + marcas + ") order by despliegue_id, nombre", rs -> {
            salida.computeIfAbsent(rs.getLong(1), k -> new ArrayList<>()).add(new PluginDesplegado(
                    rs.getString(2), rs.getString(3), rs.getString(4), leerEtiquetas(rs.getString(5))));
        }, ids.toArray());
        return salida;
    }

    private Map<String, String> leerEtiquetas(String texto) {
        try {
            return json.readValue(texto, new TypeReference<TreeMap<String, String>>() {});
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("etiquetas guardadas ilegibles", e);
        }
    }

    private static String minusculas(Enum<?> e) {
        return e.name().toLowerCase(java.util.Locale.ROOT);
    }

    private java.util.List<PluginDesplegado> validar(NuevoDespliegue n) {
        if (n == null) {
            throw new IllegalArgumentException("despliegue obligatorio");
        }
        if (n.id() <= 0) {
            throw new IllegalArgumentException("id debe ser mayor que 0");
        }
        if (n.pais() == null || !PAIS.matcher(n.pais()).matches()) {
            throw new IllegalArgumentException("pais: dos letras minúsculas");
        }
        if (n.tipo() == null) {
            throw new IllegalArgumentException("tipo obligatorio");
        }
        if ((n.tipo() == TipoDespliegue.REVERSION) != (n.origenId() != null)) {
            throw new IllegalArgumentException("origenId solo y siempre en una reversión");
        }
        obligatorio("core", n.core());
        obligatorio("versionCore", n.versionCore());
        obligatorio("imagen", n.imagen());
        obligatorio("creadoPor", n.creadoPor());
        obligatorio("confirmadoPor", n.confirmadoPor());
        if (n.confirmadoEn() == null) {
            throw new IllegalArgumentException("confirmadoEn obligatorio");
        }
        obligatorio("veredicto", n.veredicto());
        try {
            json.readerFor(JsonNode.class).with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
                    .readTree(n.veredicto());
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("veredicto no es JSON", e);
        }
        var plugins = n.plugins() == null ? java.util.List.<PluginDesplegado>of() : n.plugins();
        for (var p : plugins) {
            if (p == null) {
                throw new IllegalArgumentException("plugin nulo");
            }
            obligatorio("plugin.nombre", p.nombre());
            obligatorio("plugin.version", p.version());
            if (p.sha256() == null || !SHA256.matcher(p.sha256()).matches()) {
                throw new IllegalArgumentException("plugin.sha256: 64 hexadecimales en minúsculas");
            }
            if (p.etiquetas() != null && p.etiquetas().entrySet().stream()
                    .anyMatch(e -> e.getKey() == null || e.getValue() == null)) {
                throw new IllegalArgumentException("etiquetas con clave o valor nulo");
            }
        }
        return plugins;
    }

    private static void obligatorio(String campo, String valor) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(campo + " obligatorio");
        }
    }

    private String etiquetas(PluginDesplegado p) {
        Map<String, String> ordenadas = new TreeMap<>(p.etiquetas() == null ? Map.of() : p.etiquetas());
        try {
            return json.writeValueAsString(ordenadas);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }
}
