package io.synkra.catalogo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.synkra.catalogo.api.CatalogoApi;
import io.synkra.catalogo.api.PluginSubido;
import io.synkra.plataforma.CodigosError;
import io.synkra.plataforma.DetalleError;
import io.synkra.plataforma.ExcepcionApi;
import io.synkra.verificador.CodigoMotivo;
import io.synkra.verificador.Descriptor;
import io.synkra.verificador.LectorDescriptor;
import io.synkra.verificador.RangoVersion;
import io.synkra.verificador.Resultado;
import io.synkra.verificador.Version;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** R-C4 y R-C7 a R-C11 contra la API pública del catálogo, con SQLite y disco reales. */
@SpringBootTest
class SubirPluginTest {

    private static final String CORE = "synkra-core";
    private static final String IMAGEN = "registro.local/synkra/core:2.0.0@sha256:" + "a".repeat(64);
    private static final String YAML = "synkra-plugin.yaml";

    @TempDir
    static Path datos;

    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry r) {
        r.add("synkra.data", () -> datos.toString());
    }

    @Autowired CatalogoApi api;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void limpiar() throws IOException {
        jdbc.execute("drop trigger if exists sin_etiquetas");
        jdbc.execute("delete from cat_etiqueta");
        jdbc.execute("delete from cat_plugin");
        jdbc.execute("delete from cat_version_core");
        jdbc.execute("delete from cat_core");
        var artefactos = datos.resolve("artefactos");
        if (Files.exists(artefactos)) {
            try (Stream<Path> s = Files.walk(artefactos)) {
                for (var p : s.sorted(Comparator.reverseOrder()).toList()) {
                    Files.delete(p);
                }
            }
        }
        api.registrarCore(CORE, "2.0.0", IMAGEN);
    }

    // ---------- ayudas ----------

    private static byte[] jar(Map<String, byte[]> entradas) throws IOException {
        var salida = new ByteArrayOutputStream();
        try (var z = new ZipArchiveOutputStream(salida)) {
            for (var en : entradas.entrySet()) {
                z.putArchiveEntry(new ZipArchiveEntry(en.getKey()));
                z.write(en.getValue());
                z.closeArchiveEntry();
            }
        }
        return salida.toByteArray();
    }

    /** Un JAR con el descriptor dado como texto y una clase cualquiera. */
    private static byte[] jarConYaml(String yaml) throws IOException {
        var m = new LinkedHashMap<String, byte[]>();
        m.put("META-INF/MANIFEST.MF", "Manifest-Version: 1.0\n".getBytes(StandardCharsets.UTF_8));
        m.put(YAML, yaml.getBytes(StandardCharsets.UTF_8));
        m.put("io/x/Plugin.class", new byte[] {(byte) 0xca, (byte) 0xfe});
        return jar(m);
    }

    private static Descriptor descriptor(String nombre, String version, String core, String rango,
            Map<String, String> etiquetas, String descripcion) {
        return new Descriptor(nombre, ok(Version.parsear(version)), core, ok(RangoVersion.parsear(rango)), etiquetas,
                Optional.ofNullable(descripcion));
    }

    private static byte[] jarDe(Descriptor d) throws IOException {
        return jarConYaml(LectorDescriptor.aYaml(d));
    }

    private static byte[] pago(String descripcion) throws IOException {
        return jarDe(descriptor("pago", "1.2.0", CORE, "2.x", Map.of("pais", "co", "region", "andina"), descripcion));
    }

    private static <T> T ok(Resultado<T> r) {
        return ((Resultado.Exito<T>) r).valor();
    }

    private static String sha256(byte[] bytes) throws Exception {
        return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }

    private PluginSubido subir(byte[] bytes, Map<String, String> etiquetas) throws IOException {
        return api.subirPlugin(CORE, new ByteArrayInputStream(bytes), etiquetas);
    }

    private ExcepcionApi rechazo(String core, byte[] bytes, Map<String, String> etiquetas) {
        var capturada = new ExcepcionApi[1];
        assertThatThrownBy(() -> api.subirPlugin(core, new ByteArrayInputStream(bytes), etiquetas))
                .isInstanceOfSatisfying(ExcepcionApi.class, x -> capturada[0] = x);
        return capturada[0];
    }

    private ExcepcionApi rechazo(byte[] bytes) {
        return rechazo(CORE, bytes, Map.of());
    }

    /** 422 con los detalles de U01 (descriptor no válido), como R-C7. */
    private ExcepcionApi descriptorInvalido(byte[] bytes) throws IOException {
        var ex = rechazo(bytes);
        assertThat(ex.estado()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(ex.error().codigo()).isEqualTo(CodigosError.ENTIDAD_NO_PROCESABLE);
        assertThat(ex.error().mensaje()).isEqualTo(CodigosError.MSG_ENTIDAD_NO_PROCESABLE);
        nadaEnDisco();
        return ex;
    }

    /** Los detalles que debe dar R-C7 para estos bytes, sacados del propio lector de U01. */
    private static List<DetalleError> deU01(byte[] bytes, Path dir) throws IOException {
        var f = Files.write(dir.resolve("esperado.jar"), bytes);
        var fallo = (Resultado.Fallo<Descriptor>) LectorDescriptor.leer(f);
        return fallo.motivos().stream()
                .map(m -> new DetalleError(m.campo().orElse("descriptor"), m.codigo().name() + ": " + m.mensaje()))
                .toList();
    }

    private static List<Path> archivos(Path raiz) throws IOException {
        if (!Files.exists(raiz)) {
            return List.of();
        }
        try (Stream<Path> s = Files.walk(raiz)) {
            return s.filter(Files::isRegularFile).toList();
        }
    }

    private void nadaEnDisco() throws IOException {
        assertThat(archivos(datos.resolve("artefactos"))).isEmpty();
        assertThat(jdbc.queryForObject("select count(*) from cat_plugin", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("select count(*) from cat_etiqueta", Integer.class)).isZero();
    }

    private Path ficheroFinal(String nombre, String version) {
        return datos.resolve("artefactos").resolve(CORE).resolve(nombre).resolve(version + ".jar");
    }

    private Map<String, String> origenes(String nombre, String version) {
        return jdbc.queryForList("select clave, valor, origen from cat_etiqueta where core = ? and plugin = ? "
                        + "and version = ?", CORE, nombre, version).stream()
                .collect(Collectors.toMap(f -> (String) f.get("clave"), f -> f.get("valor") + "/" + f.get("origen")));
    }

    // ---------- el caso feliz ----------

    @Test
    void pluginValido_devuelveElDescriptorYElSha_loMueveYGuardaFilaYEtiquetasConSuOrigen() throws Exception {
        var bytes = pago("Cobros con tarjeta");
        long antes = Instant.now().toEpochMilli();
        var r = subir(bytes, Map.of("cliente", "acme"));

        assertThat(r).isEqualTo(new PluginSubido(CORE, "pago", new Version(1, 2, 0), ok(RangoVersion.parsear("2.x")),
                Map.of("pais", "co", "region", "andina", "cliente", "acme"), sha256(bytes), bytes.length));

        var fin = ficheroFinal("pago", "1.2.0");
        assertThat(fin).exists();
        assertThat(sha256(Files.readAllBytes(fin))).isEqualTo(sha256(bytes));
        assertThat(archivos(datos.resolve("artefactos"))).containsExactly(fin);

        var fila = jdbc.queryForMap("select * from cat_plugin");
        assertThat(fila).containsEntry("core", CORE).containsEntry("nombre", "pago").containsEntry("version", "1.2.0")
                .containsEntry("core_compatible", "2.x").containsEntry("sha256", sha256(bytes))
                .containsEntry("ruta", "artefactos/synkra-core/pago/1.2.0.jar")
                .containsEntry("descripcion", "Cobros con tarjeta").containsEntry("subido_por", "anonimo");
        assertThat(((Number) fila.get("tamano")).longValue()).isEqualTo(bytes.length);
        assertThat(((Number) fila.get("subido_en")).longValue()).isBetween(antes, Instant.now().toEpochMilli());

        assertThat(origenes("pago", "1.2.0")).isEqualTo(Map.of(
                "pais", "co/descriptor", "region", "andina/descriptor", "cliente", "acme/subida"));

        assertThat(api.obtenerPlugin(CORE, "pago", "1.2.0")).isEqualTo(r);
    }

    @Test
    void sinDescripcion_laFilaLlevaNull_ySinEtiquetasNoHayFilasDeEtiqueta() throws Exception {
        var bytes = jarDe(descriptor("pago", "1.0.0", CORE, ">=2.0.0 <3.0.0", Map.of(), null));
        var r = subir(bytes, Map.of());
        assertThat(r.etiquetas()).isEmpty();
        assertThat(r.coreCompatible().texto()).isEqualTo(">=2.0.0 <3.0.0");
        assertThat(jdbc.queryForObject("select descripcion from cat_plugin", String.class)).isNull();
        assertThat(jdbc.queryForObject("select count(*) from cat_etiqueta", Integer.class)).isZero();
        assertThat(api.obtenerPlugin(CORE, "pago", "1.0.0")).isEqualTo(r);
    }

    // ---------- R-C4 ----------

    @Test
    void coreInexistente_da404_sinLeerElFlujo_yNadaEnDisco() throws Exception {
        var noSeLee = new InputStream() {
            @Override
            public int read() {
                throw new AssertionError("se leyó el flujo de un core que no existe");
            }

            @Override
            public int read(byte[] b, int off, int len) {
                throw new AssertionError("se leyó el flujo de un core que no existe");
            }
        };
        for (var core : java.util.Arrays.asList("no-existe", null)) {
            var capturada = new ExcepcionApi[1];
            assertThatThrownBy(() -> api.subirPlugin(core, noSeLee, Map.of()))
                    .isInstanceOfSatisfying(ExcepcionApi.class, x -> capturada[0] = x);
            assertThat(capturada[0].estado()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(capturada[0].error().codigo()).isEqualTo(CodigosError.NO_ENCONTRADO);
            assertThat(capturada[0].error().detalles()).isEmpty();
        }
        nadaEnDisco();
    }

    // ---------- R-C7 ----------

    @Test
    void jarSinDescriptor_da422ConDescriptorSinArchivo(@TempDir Path dir) throws Exception {
        var bytes = jar(Map.of("io/x/Plugin.class", new byte[] {1}));
        var ex = descriptorInvalido(bytes);
        assertThat(ex.error().detalles()).isEqualTo(deU01(bytes, dir));
        assertThat(ex.error().detalles()).singleElement().satisfies(d -> {
            assertThat(d.campo()).isEqualTo("descriptor");
            assertThat(d.motivo()).startsWith("DESCRIPTOR_SIN_ARCHIVO: ");
        });
    }

    @Test
    void descriptorSinVersion_da422ConCampoVersion(@TempDir Path dir) throws Exception {
        var bytes = jarConYaml("nombre: pago\ncore: synkra-core\ncoreCompatible: 2.x\n");
        var ex = descriptorInvalido(bytes);
        assertThat(ex.error().detalles()).isEqualTo(deU01(bytes, dir));
        assertThat(ex.error().detalles()).singleElement().satisfies(d -> {
            assertThat(d.campo()).isEqualTo("version");
            assertThat(d.motivo()).startsWith("DESCRIPTOR_FALTA_CAMPO: ");
        });
    }

    @Test
    void campoDesconocido_da422ConDescriptorCampoDesconocido(@TempDir Path dir) throws Exception {
        var bytes = jarConYaml("nombre: pago\nversion: 1.0.0\ncore: synkra-core\ncoreCompatible: 2.x\nautor: yo\n");
        var ex = descriptorInvalido(bytes);
        assertThat(ex.error().detalles()).isEqualTo(deU01(bytes, dir));
        assertThat(ex.error().detalles()).singleElement().satisfies(d -> {
            assertThat(d.campo()).isEqualTo("autor");
            assertThat(d.motivo()).startsWith("DESCRIPTOR_CAMPO_DESCONOCIDO: ");
        });
    }

    @Test
    void rangoMalFormado_da422ConRangoInvalido(@TempDir Path dir) throws Exception {
        var bytes = jarConYaml("nombre: pago\nversion: 1.0.0\ncore: synkra-core\ncoreCompatible: \">=2.x<\"\n");
        var ex = descriptorInvalido(bytes);
        assertThat(ex.error().detalles()).isEqualTo(deU01(bytes, dir));
        assertThat(ex.error().detalles()).singleElement().satisfies(d -> {
            assertThat(d.campo()).isEqualTo("coreCompatible");
            assertThat(d.motivo()).startsWith("RANGO_INVALIDO: ");
        });
    }

    @Test
    void variosMotivos_unDetallePorMotivoEnSuOrden(@TempDir Path dir) throws Exception {
        var bytes = jarConYaml("version: 1.x\ncore: Otro\nzeta: 1\nalfa: 2\n");
        var ex = descriptorInvalido(bytes);
        var esperados = deU01(bytes, dir);
        assertThat(esperados).hasSizeGreaterThan(3);
        assertThat(ex.error().detalles()).isEqualTo(esperados);
        assertThat(ex.error().detalles().get(0)).isEqualTo(new DetalleError("zeta", esperados.get(0).motivo()));
    }

    @Test
    void motivoSinCampo_vaConCampoDescriptor(@TempDir Path dir) throws Exception {
        var bytes = jarConYaml("- a\n- b\n");
        var ex = descriptorInvalido(bytes);
        assertThat(ex.error().detalles()).isEqualTo(deU01(bytes, dir)).singleElement()
                .satisfies(d -> assertThat(d.campo()).isEqualTo("descriptor"));
    }

    private static String desconocidos(int n) {
        return "nombre: pago\nversion: 1.0.0\ncore: synkra-core\ncoreCompatible: 2.x\n"
                + IntStream.range(0, n).mapToObj(i -> "x" + i + ": 1\n").collect(Collectors.joining());
    }

    @Test
    void cincuentaMotivosCaben_sinAviso(@TempDir Path dir) throws Exception {
        var bytes = jarConYaml(desconocidos(50));
        var ex = descriptorInvalido(bytes);
        assertThat(ex.error().detalles()).hasSize(50).isEqualTo(deU01(bytes, dir));
    }

    @Test
    void masDeCincuentaMotivos_seQuedanCuarentaYNueveYElUltimoDiceCuantosSeOmitieron(@TempDir Path dir)
            throws Exception {
        var bytes = jarConYaml(desconocidos(60));
        var ex = descriptorInvalido(bytes);
        var esperados = deU01(bytes, dir);
        assertThat(ex.error().detalles()).hasSize(50);
        assertThat(ex.error().detalles().subList(0, 49)).isEqualTo(esperados.subList(0, 49));
        assertThat(ex.error().detalles().get(49))
                .isEqualTo(new DetalleError("descriptor", MotivosCatalogo.MOTIVOS_OMITIDOS.formatted(11)));

        var ex51 = descriptorInvalido(jarConYaml(desconocidos(51)));
        assertThat(ex51.error().detalles().get(49))
                .isEqualTo(new DetalleError("descriptor", MotivosCatalogo.MOTIVOS_OMITIDOS.formatted(2)));
    }

    // ---------- C-44 ----------

    @Test
    void jarConDosDescriptores_da422RutaNoPermitida_antesDeLeerlos_yNadaEnDisco() throws Exception {
        var salida = new ByteArrayOutputStream();
        try (var z = new ZipArchiveOutputStream(salida)) {
            for (var d : List.of(descriptor("pago", "1.0.0", CORE, "2.x", Map.of(), null),
                    descriptor("pago", "1.0.0", "otro-core", "2.x", Map.of(), null))) {
                z.putArchiveEntry(new ZipArchiveEntry(YAML));
                z.write(LectorDescriptor.aYaml(d).getBytes(StandardCharsets.UTF_8));
                z.closeArchiveEntry();
            }
        }
        var ex = rechazo(salida.toByteArray());
        assertThat(ex.estado()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(ex.error().codigo()).isEqualTo(CodigosCatalogo.RUTA_NO_PERMITIDA);
        assertThat(ex.error().detalles())
                .containsExactly(new DetalleError("archivo", MotivosCatalogo.ENTRADAS_DUPLICADAS));
        nadaEnDisco();
    }

    // ---------- R-C8 ----------

    @Test
    void coreDelDescriptorDistintoDeLaCarpeta_da422CoreEquivocado() throws Exception {
        api.registrarCore("otro-core", "2.0.0", IMAGEN);
        var ex = rechazo(jarDe(descriptor("pago", "1.0.0", "otro-core", "2.x", Map.of(), null)));
        assertThat(ex.estado()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(ex.error().codigo()).isEqualTo("CORE_EQUIVOCADO").isEqualTo(CodigoMotivo.CORE_EQUIVOCADO.name());
        assertThat(ex.error().mensaje()).isEqualTo(CodigosError.MSG_ENTIDAD_NO_PROCESABLE);
        assertThat(ex.error().detalles()).containsExactly(
                new DetalleError("core", MotivosCatalogo.CORE_EQUIVOCADO.formatted("otro-core", CORE)));
        nadaEnDisco();
    }

    @Test
    void coreEquivocadoVaAntesQueRepetido() throws Exception {
        subir(jarDe(descriptor("pago", "1.0.0", CORE, "2.x", Map.of(), null)), Map.of());
        var ex = rechazo(jarDe(descriptor("pago", "1.0.0", "otro-core", "2.x", Map.of(), null)));
        assertThat(ex.error().codigo()).isEqualTo("CORE_EQUIVOCADO");
    }

    // ---------- R-C9 ----------

    @Test
    void versionRepetidaConOtrosBytes_da409_yElArchivoYLaFilaOriginalesQuedanIntactos() throws Exception {
        var original = pago("primera");
        subir(original, Map.of("cliente", "acme"));
        var etiquetasAntes = origenes("pago", "1.2.0");
        var filaAntes = jdbc.queryForMap("select * from cat_plugin");

        var otra = pago("segunda");
        assertThat(sha256(otra)).isNotEqualTo(sha256(original));
        var ex = rechazo(CORE, otra, Map.of("cliente", "otro"));
        assertThat(ex.estado()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(ex.error().codigo()).isEqualTo(CodigosError.CONFLICTO);
        assertThat(ex.error().mensaje()).isEqualTo(CodigosError.MSG_CONFLICTO);
        assertThat(ex.error().detalles())
                .containsExactly(new DetalleError("version", MotivosCatalogo.PLUGIN_REPETIDO.formatted("pago", "1.2.0")));

        assertThat(sha256(Files.readAllBytes(ficheroFinal("pago", "1.2.0")))).isEqualTo(sha256(original));
        assertThat(jdbc.queryForList("select * from cat_plugin")).containsExactly(filaAntes);
        assertThat(origenes("pago", "1.2.0")).isEqualTo(etiquetasAntes);
        assertThat(archivos(datos.resolve("artefactos"))).containsExactly(ficheroFinal("pago", "1.2.0"));
    }

    @Test
    void repetidoVaAntesQueLasEtiquetas() throws Exception {
        subir(pago(null), Map.of());
        var ex = rechazo(CORE, pago("otra"), Map.of("pais", "mx", "Mala", "x"));
        assertThat(ex.estado()).isEqualTo(HttpStatus.CONFLICT);
    }

    // ---------- R-C10 ----------

    @Test
    void etiquetaEnConflicto_da422ConUnDetallePorClave_yNadaEnDisco() throws Exception {
        var bytes = jarDe(descriptor("pago", "1.2.0", CORE, "2.x", Map.of("pais", "co", "region", "andina",
                "zona", "norte"), null));
        var ex = rechazo(CORE, bytes, Map.of("zona", "sur", "pais", "mx", "region", "andina", "cliente", "acme"));
        assertThat(ex.estado()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(ex.error().codigo()).isEqualTo("ETIQUETA_EN_CONFLICTO")
                .isEqualTo(CodigosCatalogo.ETIQUETA_EN_CONFLICTO);
        assertThat(ex.error().mensaje()).isEqualTo(CodigosError.MSG_ENTIDAD_NO_PROCESABLE);
        assertThat(ex.error().detalles()).containsExactly(
                new DetalleError("etiquetas.pais", MotivosCatalogo.ETIQUETA_EN_CONFLICTO.formatted("pais", "co", "mx")),
                new DetalleError("etiquetas.zona",
                        MotivosCatalogo.ETIQUETA_EN_CONFLICTO.formatted("zona", "norte", "sur")));
        nadaEnDisco();
    }

    @Test
    void etiquetaEnConflicto_losValoresLargosSeRecortanA200() throws Exception {
        var largo = "v".repeat(500);
        var ex = rechazo(CORE, pago(null), Map.of("pais", largo));
        assertThat(ex.error().detalles()).containsExactly(new DetalleError("etiquetas.pais",
                MotivosCatalogo.ETIQUETA_EN_CONFLICTO.formatted("pais", "co", "v".repeat(200))));
    }

    @Test
    void etiquetasEnConflicto_comoMuchoCincuentaDetalles_yElValorDelDescriptorTambienSeRecorta() throws Exception {
        var delDescriptor = new HashMap<String, String>();
        var deLaPeticion = new HashMap<String, String>();
        IntStream.range(10, 70).forEach(i -> {
            delDescriptor.put("k" + i, "d");
            deLaPeticion.put("k" + i, "p");
        });
        var ex = rechazo(CORE, jarDe(descriptor("pago", "1.0.0", CORE, "2.x", delDescriptor, null)), deLaPeticion);
        assertThat(ex.error().codigo()).isEqualTo(CodigosCatalogo.ETIQUETA_EN_CONFLICTO);
        assertThat(ex.error().detalles()).hasSize(50);
        assertThat(ex.error().detalles().get(0))
                .isEqualTo(new DetalleError("etiquetas.k10", MotivosCatalogo.ETIQUETA_EN_CONFLICTO.formatted("k10", "d", "p")));
        assertThat(ex.error().detalles().get(49))
                .isEqualTo(new DetalleError("etiquetas", MotivosCatalogo.MOTIVOS_OMITIDOS.formatted(11)));

        var largo = "d".repeat(500);
        var ex2 = rechazo(CORE, jarDe(descriptor("pago", "1.0.0", CORE, "2.x", Map.of("pais", largo), null)),
                Map.of("pais", "co"));
        assertThat(ex2.error().detalles()).containsExactly(new DetalleError("etiquetas.pais",
                MotivosCatalogo.ETIQUETA_EN_CONFLICTO.formatted("pais", "d".repeat(200), "co")));
    }

    @Test
    void laMismaEtiquetaConElMismoValor_seAceptaYSeGuardaUnaVezConOrigenDescriptor() throws Exception {
        var r = subir(pago(null), Map.of("pais", "co"));
        assertThat(r.etiquetas()).isEqualTo(Map.of("pais", "co", "region", "andina"));
        assertThat(jdbc.queryForObject("select count(*) from cat_etiqueta where clave = 'pais'", Integer.class))
                .isEqualTo(1);
        assertThat(origenes("pago", "1.2.0")).isEqualTo(Map.of("pais", "co/descriptor", "region", "andina/descriptor"));
    }

    @Test
    void etiquetasDeLaPeticionInvalidas_da422ConLasReglasDeU01_yNadaEnDisco() throws Exception {
        var peticion = new HashMap<String, String>();
        peticion.put("Pais", "co");
        peticion.put("-x", "1");
        peticion.put(null, "1");
        peticion.put("cliente", null);
        peticion.put("ok", "");
        var ex = rechazo(CORE, pago(null), peticion);
        assertThat(ex.estado()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(ex.error().codigo()).isEqualTo(CodigosError.ENTIDAD_NO_PROCESABLE);
        assertThat(ex.error().detalles()).containsExactly(
                new DetalleError("etiquetas", MotivosCatalogo.ETIQUETA_CLAVE_INVALIDA.formatted("null")),
                new DetalleError("etiquetas", MotivosCatalogo.ETIQUETA_CLAVE_INVALIDA.formatted("-x")),
                new DetalleError("etiquetas", MotivosCatalogo.ETIQUETA_CLAVE_INVALIDA.formatted("Pais")),
                new DetalleError("etiquetas.cliente", MotivosCatalogo.ETIQUETA_SIN_VALOR.formatted("cliente")));
        nadaEnDisco();
        // la clave sigue el patrón de U01, no uno propio
        assertThat(Descriptor.NOMBRE.matcher("-x").matches()).isFalse();
    }

    @Test
    void etiquetasDeLaPeticionInvalidas_comoMuchoCincuentaDetalles_conNombreRecortado() throws Exception {
        var peticion = new HashMap<String, String>();
        IntStream.range(10, 70).forEach(i -> peticion.put("X" + i, "v"));
        var ex = rechazo(CORE, pago(null), peticion);
        assertThat(ex.error().detalles()).hasSize(50);
        assertThat(ex.error().detalles().get(49))
                .isEqualTo(new DetalleError("etiquetas", MotivosCatalogo.MOTIVOS_OMITIDOS.formatted(11)));

        var larga = "K".repeat(500);
        var ex2 = rechazo(CORE, pago(null), Map.of(larga, "v"));
        assertThat(ex2.error().detalles()).containsExactly(
                new DetalleError("etiquetas", MotivosCatalogo.ETIQUETA_CLAVE_INVALIDA.formatted("K".repeat(200))));
    }

    @Test
    void mapaDeEtiquetasNulo_esUnMapaVacio() throws Exception {
        var r = api.subirPlugin(CORE, new ByteArrayInputStream(pago(null)), null);
        assertThat(r.etiquetas()).isEqualTo(Map.of("pais", "co", "region", "andina"));
        assertThat(origenes("pago", "1.2.0")).isEqualTo(Map.of("pais", "co/descriptor", "region", "andina/descriptor"));
    }

    /** F-01: la clave de la petición se acepta si y solo si casa con el patrón de U01, no con una copia. */
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"a-", "a--b", "á", "a_b", "-a", "A", "", " ", "a b",
            "a-\n", "pais", "a-b", "a1", "1", "x-1-y"})
    void claveDeLaPeticion_seAceptaSiYSoloSiCasaConElPatronDeU01(String clave) throws Exception {
        var bytes = jarDe(descriptor("pago", "1.0.0", CORE, "2.x", Map.of(), null));
        if (Descriptor.NOMBRE.matcher(clave).matches()) {
            var r = api.subirPlugin(CORE, new ByteArrayInputStream(bytes), Map.of(clave, "v"));
            assertThat(r.etiquetas()).isEqualTo(Map.of(clave, "v"));
            assertThat(origenes("pago", "1.0.0")).isEqualTo(Map.of(clave, "v/subida"));
        } else {
            var ex = rechazo(CORE, bytes, Map.of(clave, "v"));
            assertThat(ex.estado()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
            assertThat(ex.error().detalles())
                    .containsExactly(new DetalleError("etiquetas", MotivosCatalogo.ETIQUETA_CLAVE_INVALIDA.formatted(clave)));
            nadaEnDisco();
        }
    }

    @Test
    void unValorVacioEsValido_comoEnU01() throws Exception {
        var r = subir(pago(null), Map.of("cliente", ""));
        assertThat(r.etiquetas()).containsEntry("cliente", "");
    }

    // ---------- R-C11: mover y guardar ----------

    @Test
    void atomicidad_siFallaElInsertDeEtiquetas_noQuedaNiLaFilaNiElArchivoFinal() throws Exception {
        jdbc.execute("create trigger sin_etiquetas before insert on cat_etiqueta begin "
                + "select raise(abort, 'prueba de atomicidad'); end");
        try {
            assertThatThrownBy(() -> subir(pago(null), Map.of()))
                    .isNotInstanceOf(ExcepcionApi.class)
                    .hasStackTraceContaining("prueba de atomicidad");
            nadaEnDisco();
            assertThat(ficheroFinal("pago", "1.2.0")).doesNotExist();
        } finally {
            jdbc.execute("drop trigger if exists sin_etiquetas");
        }
    }

    @Test
    void siNoSePuedeCrearLaCarpeta_noSeInsertaNada_elTemporalSeBorra_yLoQueHabiaQueda() throws Exception {
        var estorbo = datos.resolve("artefactos").resolve(CORE).resolve("pago");
        Files.createDirectories(estorbo.getParent());
        Files.writeString(estorbo, "no soy un directorio");
        assertThatThrownBy(() -> subir(pago(null), Map.of())).isInstanceOf(FileSystemException.class)
                .isNotInstanceOf(ExcepcionApi.class);
        assertThat(jdbc.queryForObject("select count(*) from cat_plugin", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("select count(*) from cat_etiqueta", Integer.class)).isZero();
        assertThat(archivos(datos.resolve("artefactos"))).containsExactly(estorbo);
        assertThat(Files.readString(estorbo)).isEqualTo("no soy un directorio");
    }

    // ---------- F-02 y F-04: la colocación nunca reemplaza ----------

    @Test
    void destinoYaOcupadoPorUnArchivoSinFila_da409_yNoSeReemplazaNiSeBorra() throws Exception {
        // es lo que ve el perdedor de una carrera: el ganador ya colocó su JAR y aún no hizo commit
        var delGanador = "bytes del ganador".getBytes(StandardCharsets.UTF_8);
        var fin = ficheroFinal("pago", "1.2.0");
        Files.createDirectories(fin.getParent());
        Files.write(fin, delGanador);

        var ex = rechazo(CORE, pago(null), Map.of());
        assertThat(ex.estado()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(ex.error().codigo()).isEqualTo(CodigosError.CONFLICTO);
        assertThat(ex.error().detalles())
                .containsExactly(new DetalleError("version", MotivosCatalogo.PLUGIN_REPETIDO.formatted("pago", "1.2.0")));
        assertThat(sha256(Files.readAllBytes(fin))).isEqualTo(sha256(delGanador));
        assertThat(archivos(datos.resolve("artefactos"))).containsExactly(fin);
        assertThat(jdbc.queryForObject("select count(*) from cat_plugin", Integer.class)).isZero();
    }

    @Test
    void destinoOcupadoPorUnDirectorio_da409_yElDirectorioQueda() throws Exception {
        var fin = ficheroFinal("pago", "1.2.0");
        Files.createDirectories(fin.resolve("dentro"));
        var ex = rechazo(CORE, pago(null), Map.of());
        assertThat(ex.estado()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(fin.resolve("dentro")).isDirectory();
        nadaEnDisco();
    }

    @Test
    void dosSubidasSimultaneasDeLaMismaVersion_unaGanaYLaOtraDa409_yElArchivoEsElDelGanador() throws Exception {
        for (int ronda = 0; ronda < 10; ronda++) {
            limpiar();
            var barrera = new java.util.concurrent.CyclicBarrier(2);
            var bytes = List.of(pago("hilo a"), pago("hilo b"));
            var hilos = java.util.concurrent.Executors.newFixedThreadPool(2);
            try {
                var futuros = bytes.stream().map(b -> hilos.submit(() -> {
                    barrera.await();
                    try {
                        return (Object) subir(b, Map.of());
                    } catch (ExcepcionApi e) {
                        return e;
                    }
                })).toList();
                var ganadores = new java.util.ArrayList<PluginSubido>();
                var perdedores = new java.util.ArrayList<ExcepcionApi>();
                for (var f : futuros) {
                    var r = f.get(60, java.util.concurrent.TimeUnit.SECONDS);
                    if (r instanceof PluginSubido p) {
                        ganadores.add(p);
                    } else {
                        perdedores.add((ExcepcionApi) r);
                    }
                }
                assertThat(ganadores).hasSize(1);
                assertThat(perdedores).singleElement()
                        .satisfies(e -> assertThat(e.estado()).isEqualTo(HttpStatus.CONFLICT));
                var fin = ficheroFinal("pago", "1.2.0");
                assertThat(sha256(Files.readAllBytes(fin))).isEqualTo(ganadores.get(0).sha256())
                        .isEqualTo(jdbc.queryForObject("select sha256 from cat_plugin", String.class));
                assertThat(archivos(datos.resolve("artefactos"))).containsExactly(fin);
            } finally {
                hilos.shutdownNow();
            }
        }
    }

    // ---------- F-03: sin directorios huérfanos ----------

    private void conTriggerQueFalla(org.junit.jupiter.api.function.Executable cuerpo) throws Throwable {
        jdbc.execute("create trigger sin_etiquetas before insert on cat_etiqueta begin "
                + "select raise(abort, 'prueba de atomicidad'); end");
        try {
            cuerpo.execute();
        } finally {
            jdbc.execute("drop trigger if exists sin_etiquetas");
        }
    }

    private List<Path> directorios() throws IOException {
        var artefactos = datos.resolve("artefactos");
        try (Stream<Path> s = Files.walk(artefactos)) {
            return s.filter(Files::isDirectory).filter(p -> !p.equals(artefactos)).map(artefactos::relativize).toList();
        }
    }

    @Test
    void alFallar_seBorranLosDirectoriosQueCreoEstaSubida() throws Throwable {
        conTriggerQueFalla(() -> assertThatThrownBy(() -> subir(pago(null), Map.of())));
        assertThat(directorios()).containsExactly(Path.of(".tmp"));
    }

    @Test
    void alFallar_noSeBorraElDirectorioDelCoreQueYaExistiaVacio() throws Throwable {
        var delCore = datos.resolve("artefactos").resolve(CORE);
        Files.createDirectories(delCore);
        conTriggerQueFalla(() -> assertThatThrownBy(() -> subir(pago(null), Map.of())));
        assertThat(directorios()).containsExactlyInAnyOrder(Path.of(".tmp"), Path.of(CORE));
    }

    @Test
    void alFallar_noSeTocaLaCarpetaDelPluginConOtraVersion() throws Throwable {
        subir(jarDe(descriptor("pago", "1.0.0", CORE, "2.x", Map.of(), null)), Map.of());
        conTriggerQueFalla(() -> assertThatThrownBy(() -> subir(pago(null), Map.of())));
        assertThat(archivos(datos.resolve("artefactos"))).containsExactly(ficheroFinal("pago", "1.0.0"));
        assertThat(directorios()).containsExactlyInAnyOrder(Path.of(".tmp"), Path.of(CORE), Path.of(CORE, "pago"));
    }

    @Test
    void laRutaFinalSeComprueba_dentroDeLaRaiz() {
        var raiz = datos.resolve("artefactos");
        assertThat(SubidaPlugin.rutaFinal(raiz, "c", "p", "1.0.0")).isEqualTo(raiz.resolve("c/p/1.0.0.jar"));
        assertThatThrownBy(() -> SubidaPlugin.rutaFinal(raiz, "c", "../..", "1.0.0"))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> SubidaPlugin.rutaFinal(raiz, "..", "..", "x"))
                .isInstanceOf(IllegalStateException.class);
    }

    // ---------- obtenerPlugin ----------

    @Test
    void obtenerPluginInexistente_da404() throws Exception {
        subir(pago(null), Map.of());
        for (var clave : new String[][] {{CORE, "pago", "9.9.9"}, {CORE, "otro", "1.2.0"}, {"no-existe", "pago", "1.2.0"},
                {null, "pago", "1.2.0"}, {CORE, null, "1.2.0"}, {CORE, "pago", null}}) {
            var capturada = new ExcepcionApi[1];
            assertThatThrownBy(() -> api.obtenerPlugin(clave[0], clave[1], clave[2]))
                    .isInstanceOfSatisfying(ExcepcionApi.class, x -> capturada[0] = x);
            assertThat(capturada[0].estado()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(capturada[0].error().codigo()).isEqualTo(CodigosError.NO_ENCONTRADO);
            assertThat(capturada[0].error().detalles()).isEmpty();
        }
    }

    // ---------- textos ----------

    @Test
    void textosDeMotivoFijados() {
        assertThat(MotivosCatalogo.CORE_EQUIVOCADO)
                .isEqualTo("El descriptor es del core '%s' y la carpeta es del core '%s'");
        assertThat(MotivosCatalogo.PLUGIN_REPETIDO).isEqualTo("El plugin %s ya tiene la versión %s en este core");
        assertThat(MotivosCatalogo.ETIQUETA_EN_CONFLICTO)
                .isEqualTo("La etiqueta '%s' vale '%s' en el descriptor y '%s' en la petición");
        assertThat(MotivosCatalogo.ETIQUETA_CLAVE_INVALIDA).isEqualTo(
                "La clave de etiqueta '%s' debe llevar solo minúsculas, dígitos y guiones medios, sin guion al "
                        + "principio ni al final");
        assertThat(MotivosCatalogo.ETIQUETA_SIN_VALOR).isEqualTo("La etiqueta '%s' no tiene valor");
        assertThat(MotivosCatalogo.MOTIVOS_OMITIDOS).isEqualTo("Se omitieron %d motivos más");
        assertThat(MotivosCatalogo.ENTRADAS_DUPLICADAS).isEqualTo("El JAR tiene entradas con el mismo nombre");
        assertThat(CodigosCatalogo.ETIQUETA_EN_CONFLICTO).isEqualTo("ETIQUETA_EN_CONFLICTO");
        assertThat(SubidaPlugin.MAX_DETALLES).isEqualTo(50);
    }
}
