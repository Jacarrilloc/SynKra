package io.synkra.catalogo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.synkra.plataforma.CodigosError;
import io.synkra.plataforma.DetalleError;
import io.synkra.plataforma.ExcepcionApi;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.apache.commons.compress.archivers.zip.UnicodePathExtraField;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.util.unit.DataSize;

/** R-C5, R-C6 y R-C11: la recepción del JAR, con entradas hostiles y flujos que fallan. */
@SpringBootTest(properties = "synkra.max-artefacto=1KB")
class RecepcionArtefactoTest {

    private static final DataSize VEINTE_MB = DataSize.ofMegabytes(20);
    private static final DataSize DESC = DataSize.ofMegabytes(200);

    @TempDir
    static Path datosSpring;

    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry r) {
        r.add("synkra.data", () -> datosSpring.toString());
    }

    @TempDir
    Path datos;

    @Autowired RecepcionArtefacto desdeSpring;

    // ---------- ayudas ----------

    private record Entrada(String nombre, Integer modoUnix, byte[] contenido) {}

    private static Entrada e(String nombre) {
        return new Entrada(nombre, null, new byte[] {1, 2, 3});
    }

    private static byte[] zip(Entrada... entradas) throws IOException {
        var salida = new ByteArrayOutputStream();
        try (var z = new ZipArchiveOutputStream(salida)) {
            for (var en : entradas) {
                var nombre = en.nombre();
                var ze = new ZipArchiveEntry("x") {
                    {
                        setPlatform(PLATFORM_UNIX); // sin normalizar \ a /
                        setName(nombre, nombre.getBytes(StandardCharsets.UTF_8));
                    }
                };
                ze.setUnixMode(en.modoUnix() == null ? 0100644 : en.modoUnix());
                z.putArchiveEntry(ze);
                z.write(en.contenido());
                z.closeArchiveEntry();
            }
        }
        return salida.toByteArray();
    }

    private RecepcionArtefacto componente(DataSize limite) {
        return new RecepcionArtefacto(datos.toString(), limite, 10000, DESC, RecepcionArtefacto::nuevoDigest);
    }

    private ArtefactoRecibido recibir(RecepcionArtefacto c, byte[] bytes) throws IOException {
        return c.recibir(new ByteArrayInputStream(bytes));
    }

    private ExcepcionApi rechazo(RecepcionArtefacto c, byte[] bytes) {
        var capturada = new ExcepcionApi[1];
        assertThatThrownBy(() -> recibir(c, bytes)).isInstanceOfSatisfying(ExcepcionApi.class,
                x -> capturada[0] = x);
        return capturada[0];
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
    }

    private static InputStream ceros(long total, AtomicLong leidos) {
        return new InputStream() {
            long resto = total;

            @Override
            public int read() {
                if (resto-- <= 0) {
                    return -1;
                }
                leidos.incrementAndGet();
                return 0;
            }

            @Override
            public int read(byte[] b, int off, int len) {
                if (resto <= 0) {
                    return -1;
                }
                int n = (int) Math.min(len, resto);
                resto -= n;
                leidos.addAndGet(n);
                return n;
            }
        };
    }

    // ---------- el caso feliz ----------

    @Test
    void jarValido_calculaSha256YDejaElTemporalEnElDirectorioTmp() throws Exception {
        var bytes = zip(e("META-INF/synkra.json"), e("a/b/Clase.class"), e("dir/"), e("a..b/x"));
        var r = recibir(componente(VEINTE_MB), bytes);

        var esperado = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        assertThat(r.sha256()).isEqualTo(esperado).matches("[0-9a-f]{64}");
        assertThat(r.tamano()).isEqualTo(bytes.length);
        assertThat(r.temporal()).exists().hasParent(datos.resolve("artefactos").resolve(".tmp"));
        assertThat(Files.readAllBytes(r.temporal())).isEqualTo(bytes);
        assertThat(Files.size(r.temporal())).isEqualTo(bytes.length);
    }

    @Test
    void creaElDirectorioTmpSiNoExiste() throws Exception {
        assertThat(datos.resolve("artefactos")).doesNotExist();
        recibir(componente(VEINTE_MB), zip(e("a")));
        assertThat(datos.resolve("artefactos/.tmp")).isDirectory();
    }

    @Test
    void zipSinEntradas_seAceptaPorqueNingunaEntradaEsCulpable() throws Exception {
        // Un JAR sin descriptor lo rechaza U04-T06; aquí solo se revisan las entradas.
        var r = recibir(componente(VEINTE_MB), zip());
        assertThat(r.temporal()).exists();
        assertThat(r.tamano()).isEqualTo(22); // solo el fin del directorio central
    }

    // ---------- R-C5: el límite ----------

    @Test
    void flujoDe25MbConLimiteDe20Mb_da413_nombraElLimite_noLeeTodoYNoDejaNada() throws Exception {
        var leidos = new AtomicLong();
        var c = componente(VEINTE_MB);
        var ex = (ExcepcionApi) catchThrowable(() -> c.recibir(ceros(25L * 1024 * 1024, leidos)));
        assertThat(ex.estado()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
        assertThat(ex.error().codigo()).isEqualTo(CodigosError.DEMASIADO_GRANDE);
        assertThat(ex.error().detalles()).containsExactly(
                new DetalleError("archivo", MotivosCatalogo.ARTEFACTO_GRANDE.formatted("20 MB")));
        assertThat(ex.error().detalles().get(0).motivo()).contains("20 MB");
        assertThat(leidos.get()).as("se corta al pasar el límite, no se drena el flujo")
                .isLessThan(20L * 1024 * 1024 + 64 * 1024);
        nadaEnDisco();
    }

    @Test
    void exactamenteElLimiteSeAcepta_limiteMasUnoDa413() throws Exception {
        var bytes = zip(e("a"));
        var r = recibir(componente(DataSize.ofBytes(bytes.length)), bytes);
        assertThat(r.tamano()).isEqualTo(bytes.length);

        var ex = rechazo(componente(DataSize.ofBytes(bytes.length - 1)), bytes);
        assertThat(ex.estado()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
        assertThat(ex.error().detalles().get(0).motivo())
                .isEqualTo(MotivosCatalogo.ARTEFACTO_GRANDE.formatted((bytes.length - 1) + " bytes"));
    }

    @Test
    void propiedadSynkraMaxArtefacto_seEnlazaDesdeSpring_conUnKb() throws Exception {
        var ex = (ExcepcionApi) catchThrowable(() -> desdeSpring.recibir(ceros(2048, new AtomicLong())));
        assertThat(ex.estado()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
        assertThat(ex.error().detalles().get(0).motivo()).contains("1 KB");
        assertThat(archivos(datosSpring.resolve("artefactos"))).isEmpty();
    }

    @Test
    void elValorPorDefectoEs20MbConVariableDeEntorno() throws Exception {
        var p = new Properties();
        try (var in = getClass().getResourceAsStream("/application.properties")) {
            p.load(new InputStreamReader(in, StandardCharsets.UTF_8));
        }
        assertThat(p.getProperty("synkra.max-artefacto")).isEqualTo("${SYNKRA_MAX_ARTEFACTO:20MB}");
    }

    @Test
    void textosDeMotivoFijados() {
        assertThat(MotivosCatalogo.ARTEFACTO_GRANDE).isEqualTo("El artefacto supera el límite de %s");
        assertThat(MotivosCatalogo.ARTEFACTO_NO_ES_ZIP).isEqualTo("El archivo no es un JAR (ZIP) legible");
        assertThat(MotivosCatalogo.ENTRADA_NO_PERMITIDA)
                .isEqualTo("La entrada '%s' tiene una ruta no permitida");
        assertThat(MotivosCatalogo.UNIDAD_MB).isEqualTo("MB");
        assertThat(MotivosCatalogo.UNIDAD_KB).isEqualTo("KB");
        assertThat(MotivosCatalogo.UNIDAD_BYTES).isEqualTo("bytes");
        assertThat(MotivosCatalogo.DEMASIADAS_ENTRADAS).isEqualTo("El artefacto tiene más de %d entradas");
        assertThat(MotivosCatalogo.ENTRADA_UNICODE_DISTINTA)
                .isEqualTo("La entrada '%s' lleva un nombre Unicode distinto del nombre crudo");
        assertThat(MotivosCatalogo.ENTRADAS_NO_COINCIDEN)
                .isEqualTo("El JAR tiene entradas que no coinciden entre su índice y su contenido");
        assertThat(MotivosCatalogo.DESCOMPRIMIDO_EXCEDIDO)
                .isEqualTo("El contenido descomprimido supera el límite de %s");
        assertThat(MotivosCatalogo.ENTRADA_ENLACE).isEqualTo("La entrada '%s' es un enlace simbólico");
        assertThat(CodigosCatalogo.RUTA_NO_PERMITIDA).isEqualTo("RUTA_NO_PERMITIDA");
        assertThat(CodigosCatalogo.ARTEFACTO_INVALIDO).isEqualTo("ARTEFACTO_INVALIDO");
    }

    // ---------- R-C6: entradas hostiles ----------

    private void rutaNoPermitida(Entrada... entradas) throws IOException {
        var ex = rechazo(componente(VEINTE_MB), zip(entradas));
        assertThat(ex.estado()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(ex.error().codigo()).isEqualTo(CodigosCatalogo.RUTA_NO_PERMITIDA);
        assertThat(ex.error().detalles()).isNotEmpty()
                .allSatisfy(d -> assertThat(d.campo()).isEqualTo("archivo"));
        nadaEnDisco();
    }

    @Test
    void entradaConPuntoPunto_da422YNadaEnDisco() throws Exception {
        rutaNoPermitida(e("ok"), e("../../etc/x"));
    }

    @Test
    void entradaAbsoluta_barra() throws Exception {
        rutaNoPermitida(e("/etc/x"));
    }

    @Test
    void entradaAbsoluta_barraInvertida() throws Exception {
        rutaNoPermitida(e("\\etc\\x"));
    }

    @Test
    void entradaConLetraDeUnidad() throws Exception {
        rutaNoPermitida(e("C:\\x"));
        rutaNoPermitida(e("d:x"));
    }

    @Test
    void entradaConBarrasInvertidasYPuntoPunto() throws Exception {
        rutaNoPermitida(e("a\\..\\..\\x"));
    }

    @Test
    void puntoPuntoComoUltimoSegmento() throws Exception {
        rutaNoPermitida(e("a/.."));
    }

    @Test
    void entradaConNul() throws Exception {
        rutaNoPermitida(e("a\u0000b/x.class"));
    }

    @Test
    void entradaConNombreVacio() throws Exception {
        rutaNoPermitida(e(""));
    }

    @Test
    void enlaceSimbolico_aunqueSuNombreSeaInocente() throws Exception {
        var enlace = new Entrada("lib/enlace", 0120777, "/etc/passwd".getBytes(StandardCharsets.UTF_8));
        var ex = rechazo(componente(VEINTE_MB), zip(e("ok"), enlace));
        assertThat(ex.estado()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(ex.error().codigo()).isEqualTo(CodigosCatalogo.RUTA_NO_PERMITIDA);
        assertThat(ex.error().detalles()).containsExactly(
                new DetalleError("archivo", MotivosCatalogo.ENTRADA_ENLACE.formatted("lib/enlace")));
        nadaEnDisco();
    }

    @Test
    void ficheroRegularConModoUnixNoEsEnlace() throws Exception {
        var r = recibir(componente(VEINTE_MB), zip(new Entrada("bin/run", 0100755, new byte[] {1})));
        assertThat(r.temporal()).exists();
    }

    @Test
    void unDetallePorEntradaCulpable_hastaDiez_conNombreRecortadoA200() throws Exception {
        var entradas = IntStream.range(0, 12).mapToObj(i -> e("../x" + i)).toArray(Entrada[]::new);
        var ex = rechazo(componente(VEINTE_MB), zip(entradas));
        assertThat(ex.error().detalles()).hasSize(10);
        assertThat(ex.error().detalles().get(0).motivo())
                .isEqualTo(MotivosCatalogo.ENTRADA_NO_PERMITIDA.formatted("../x0"));

        var larga = "../" + "n".repeat(500);
        var ex2 = rechazo(componente(VEINTE_MB), zip(e(larga)));
        assertThat(ex2.error().detalles().get(0).motivo())
                .isEqualTo(MotivosCatalogo.ENTRADA_NO_PERMITIDA.formatted(larga.substring(0, 200)));
    }

    @Test
    void entradaCulpableRepetida_tambienSeRechaza() throws Exception {
        rutaNoPermitida(e("../x"), e("../x"));
    }

    // ---------- no es un ZIP ----------

    private void noEsZip(byte[] bytes) throws IOException {
        var ex = rechazo(componente(VEINTE_MB), bytes);
        assertThat(ex.estado()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(ex.error().codigo()).isEqualTo(CodigosCatalogo.ARTEFACTO_INVALIDO);
        assertThat(ex.error().detalles())
                .containsExactly(new DetalleError("archivo", MotivosCatalogo.ARTEFACTO_NO_ES_ZIP));
        nadaEnDisco();
    }

    @Test
    void archivoQueNoEsZip() throws Exception {
        noEsZip("esto no es un jar".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void flujoVacio() throws Exception {
        noEsZip(new byte[0]);
    }

    @Test
    void zipTruncado_sinDirectorioCentral() throws Exception {
        var completo = zip(e("a"), e("b"));
        noEsZip(java.util.Arrays.copyOf(completo, completo.length - 30));
    }

    // ---------- fallos del flujo ----------

    @Test
    void ioExceptionAMitad_suboYNoQuedaTemporal() throws Exception {
        var parcial = zip(e("a"));
        var roto = new InputStream() {
            int i;

            @Override
            public int read() throws IOException {
                if (i >= 10) {
                    throw new IOException("se cayó la conexión");
                }
                return parcial[i++];
            }
        };
        assertThatThrownBy(() -> componente(VEINTE_MB).recibir(roto))
                .isInstanceOf(IOException.class).hasMessage("se cayó la conexión");
        nadaEnDisco();
    }

    @Test
    void excepcionInesperadaAMitad_tambienBorraElTemporal() throws Exception {
        var roto = new InputStream() {
            int i;

            @Override
            public int read() {
                if (i++ >= 10) {
                    throw new IllegalStateException("inesperada");
                }
                return 7;
            }
        };
        assertThatThrownBy(() -> componente(VEINTE_MB).recibir(roto))
                .isInstanceOf(IllegalStateException.class);
        nadaEnDisco();
    }

    // ---------- ronda 2 ----------

    private static byte[] zipConUpath(String crudo, String unicode) throws IOException {
        var salida = new ByteArrayOutputStream();
        try (var z = new ZipArchiveOutputStream(salida)) {
            // sin el bit 11 (UTF-8): solo entonces commons sustituye el nombre por el del extra 0x7075
            z.setUseLanguageEncodingFlag(false);
            var ze = new ZipArchiveEntry("x") {
                {
                    setPlatform(PLATFORM_UNIX);
                    setName(crudo, crudo.getBytes(StandardCharsets.UTF_8));
                }
            };
            ze.addExtraField(new UnicodePathExtraField(unicode, crudo.getBytes(StandardCharsets.UTF_8)));
            z.putArchiveEntry(ze);
            z.write(1);
            z.closeArchiveEntry();
        }
        return salida.toByteArray();
    }

    private static byte[] reemplazar(byte[] z, String de, String por, boolean ultima) {
        var s = new String(z, StandardCharsets.ISO_8859_1);
        int i = ultima ? s.lastIndexOf(de) : s.indexOf(de);
        assertThat(i).isGreaterThanOrEqualTo(0);
        System.arraycopy(por.getBytes(StandardCharsets.ISO_8859_1), 0, z, i, por.length());
        return z;
    }

    private static int indice(byte[] z, int b0, int b1, int b2, int b3, boolean ultimo) {
        int r = -1;
        for (int i = 0; i + 3 < z.length; i++) {
            if ((z[i] & 255) == b0 && (z[i + 1] & 255) == b1 && (z[i + 2] & 255) == b2 && (z[i + 3] & 255) == b3) {
                r = i;
                if (!ultimo) {
                    return r;
                }
            }
        }
        return r;
    }

    @Test
    void f01_nombreCrudoMaloConExtraUnicodeBueno_seRechaza() throws Exception {
        // java.util.zip y JarInputStream ven '../../evil'; solo commons veía 'ok/evil123'
        var ex = rechazo(componente(VEINTE_MB), zipConUpath("../../evil", "ok/evil123"));
        assertThat(ex.estado()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(ex.error().codigo()).isEqualTo(CodigosCatalogo.RUTA_NO_PERMITIDA);
        assertThat(ex.error().detalles().get(0).motivo())
                .isEqualTo(MotivosCatalogo.ENTRADA_NO_PERMITIDA.formatted("../../evil"));
        nadaEnDisco();
    }

    @Test
    void f01_nombreCrudoBuenoConExtraUnicodeMalo_seRechaza() throws Exception {
        var ex = rechazo(componente(VEINTE_MB), zipConUpath("ok/evil123", "../../evil"));
        assertThat(ex.error().codigo()).isEqualTo(CodigosCatalogo.RUTA_NO_PERMITIDA);
        assertThat(ex.error().detalles())
                .containsExactly(new DetalleError("archivo", MotivosCatalogo.ENTRADA_UNICODE_DISTINTA.formatted("ok/evil123")));
        nadaEnDisco();
    }

    @Test
    void f01_extraUnicodeIgualAlCrudo_seAcepta() throws Exception {
        assertThat(recibir(componente(VEINTE_MB), zipConUpath("ok/x", "ok/x")).temporal()).exists();
    }

    @Test
    void f02_ioExceptionAlAbrirElZip_noEsFormatoInvalido_subeConSuCausa() throws Exception {
        var bytes = zip(e("a"));
        var entrada = new ByteArrayInputStream(bytes) {
            @Override
            public synchronized int read(byte[] b, int off, int len) {
                int n = super.read(b, off, len);
                if (n == -1) {
                    Thread.currentThread().interrupt(); // el siguiente acceso al disco falla
                }
                return n;
            }
        };
        try {
            assertThatThrownBy(() -> componente(VEINTE_MB).recibir(entrada))
                    .isInstanceOf(java.nio.channels.ClosedByInterruptException.class);
        } finally {
            Thread.interrupted();
        }
        nadaEnDisco();
    }

    @Test
    void f02_soloLaInterrupcionEsDelServidor() {
        assertThat(RecepcionArtefacto.esInterrupcion(new java.nio.channels.ClosedByInterruptException())).isTrue();
        assertThat(RecepcionArtefacto.esInterrupcion(new java.io.InterruptedIOException())).isTrue();
        assertThat(RecepcionArtefacto.esInterrupcion(
                new IOException("envuelto", new java.nio.channels.ClosedByInterruptException()))).isTrue();
        assertThat(RecepcionArtefacto.esInterrupcion(new java.util.zip.ZipException("x"))).isFalse();
        assertThat(RecepcionArtefacto.esInterrupcion(new IOException("Too many disks"))).isFalse();
        assertThat(RecepcionArtefacto.esInterrupcion(new IllegalArgumentException())).isFalse();
    }

    private void estructuraAmbigua(byte[] z) throws IOException {
        var ex = rechazo(componente(VEINTE_MB), z);
        assertThat(ex.estado()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(ex.error().codigo()).isEqualTo(CodigosCatalogo.RUTA_NO_PERMITIDA);
        nadaEnDisco();
    }

    @Test
    void f04_cabeceraLocalConOtroNombreQueElCentral_seRechaza() throws Exception {
        var z = reemplazar(zip(e("ok/evil123")), "ok/evil123", "../../evil", false);
        var ex = rechazo(componente(VEINTE_MB), z);
        assertThat(ex.error().codigo()).isEqualTo(CodigosCatalogo.RUTA_NO_PERMITIDA);
        assertThat(ex.error().detalles()).containsExactly(
                new DetalleError("archivo", MotivosCatalogo.ENTRADAS_NO_COINCIDEN));
        nadaEnDisco();
    }

    @Test
    void f04_zipMaloAntepuestoAUnoBueno_seRechaza() throws Exception {
        var a = zip(e("../../evil"));
        var b = zip(e("ok"));
        var junto = new byte[a.length + b.length];
        System.arraycopy(a, 0, junto, 0, a.length);
        System.arraycopy(b, 0, junto, a.length, b.length);
        estructuraAmbigua(junto);
    }

    @Test
    void f04_datosAntepuestosCualesquiera_seRechazan() throws Exception {
        var b = zip(e("ok"));
        var junto = new byte[100 + b.length];
        System.arraycopy(b, 0, junto, 100, b.length);
        estructuraAmbigua(junto);
    }

    @Test
    void f04_entradaLocalOcultaAntesDeLaPrimeraDelCentral_seRechaza() throws Exception {
        var z = zip(e("oculta"), e("ok"));
        int cd1 = indice(z, 'P', 'K', 1, 2, false);
        int cd2 = -1;
        for (int i = cd1 + 4; i + 3 < z.length && cd2 < 0; i++) {
            if (z[i] == 'P' && z[i + 1] == 'K' && z[i + 2] == 1 && z[i + 3] == 2) {
                cd2 = i;
            }
        }
        int eocd = indice(z, 'P', 'K', 5, 6, true);
        var nuevo = new byte[z.length - (cd2 - cd1)];
        System.arraycopy(z, 0, nuevo, 0, cd1);
        System.arraycopy(z, cd2, nuevo, cd1, z.length - cd2);
        int e2 = eocd - (cd2 - cd1);
        var b = java.nio.ByteBuffer.wrap(nuevo).order(java.nio.ByteOrder.LITTLE_ENDIAN);
        b.putShort(e2 + 8, (short) 1).putShort(e2 + 10, (short) 1).putInt(e2 + 12, b.getInt(e2 + 12) - (cd2 - cd1));
        estructuraAmbigua(nuevo);
    }

    @Test
    void f04_unJarNormalConVariasEntradasYDescriptores_seAcepta() throws Exception {
        var r = recibir(componente(VEINTE_MB), zip(e("a/"), e("a/b.class"), e("c"), e("d")));
        assertThat(r.temporal()).exists();
    }

    private static final class Espia extends MessageDigest {
        final MessageDigest real;
        final java.util.List<Integer> llamadas = new java.util.ArrayList<>();

        Espia() throws Exception {
            super("SHA-256");
            real = MessageDigest.getInstance("SHA-256");
        }

        @Override
        protected void engineUpdate(byte b) {
            llamadas.add(1);
            real.update(b);
        }

        @Override
        protected void engineUpdate(byte[] in, int off, int len) {
            llamadas.add(len);
            real.update(in, off, len);
        }

        @Override
        protected byte[] engineDigest() {
            return real.digest();
        }

        @Override
        protected void engineReset() {
            real.reset();
        }
    }

    @Test
    void f05_elHashSeAlimentaEnLaCopia_porTrozos_nuncaDeUnaSegundaLectura() throws Exception {
        var relleno = new byte[60_000];
        new java.util.Random(7).nextBytes(relleno);
        var bytes = zip(new Entrada("datos.bin", null, relleno));
        var espia = new Espia();
        var c = new RecepcionArtefacto(datos.toString(), VEINTE_MB, 10000, DESC, () -> espia);
        var r = recibir(c, bytes);
        assertThat(espia.llamadas).as("alimentado mientras se copia, en trozos del búfer")
                .hasSizeGreaterThanOrEqualTo(bytes.length / 8192)
                .allSatisfy(n -> assertThat(n).isLessThanOrEqualTo(8192));
        assertThat(espia.llamadas.stream().mapToLong(Integer::longValue).sum()).isEqualTo(bytes.length);
        assertThat(r.sha256()).isEqualTo(
                HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)));
    }

    /** Digest que ignora lo que recibe y devuelve siempre el mismo valor: el hash devuelto tiene que ser el suyo. */
    private static final class Centinela extends MessageDigest {
        static final byte[] VALOR = new byte[32];

        static {
            java.util.Arrays.fill(VALOR, (byte) 0xAB);
        }

        Centinela() {
            super("SHA-256");
        }

        @Override
        protected void engineUpdate(byte b) {
        }

        @Override
        protected void engineUpdate(byte[] in, int off, int len) {
        }

        @Override
        protected byte[] engineDigest() {
            return VALOR.clone();
        }

        @Override
        protected void engineReset() {
        }
    }

    @Test
    void f11_elSha256DevueltoSaleDelDigestDeLaCopia() throws Exception {
        var c = new RecepcionArtefacto(datos.toString(), VEINTE_MB, 10000, DESC, Centinela::new);
        var r = recibir(c, zip(e("a")));
        assertThat(r.sha256()).isEqualTo(HexFormat.of().formatHex(Centinela.VALOR));
    }

    @Test
    void f06_noSeLeeMasQueLimiteMasUnByte() throws Exception {
        var leidos = new AtomicLong();
        var c = componente(DataSize.ofBytes(1000));
        var ex = (ExcepcionApi) catchThrowable(() -> c.recibir(ceros(50_000, leidos)));
        assertThat(ex.estado()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
        assertThat(leidos.get()).isEqualTo(1001);
        nadaEnDisco();
    }

    @Test
    void f07_elDirectorioTmpSoloLoVeElDueno() throws Exception {
        org.junit.jupiter.api.Assumptions.assumeTrue(
                java.nio.file.FileSystems.getDefault().supportedFileAttributeViews().contains("posix"));
        recibir(componente(VEINTE_MB), zip(e("a")));
        assertThat(java.nio.file.attribute.PosixFilePermissions.toString(
                Files.getPosixFilePermissions(datos.resolve("artefactos/.tmp")))).isEqualTo("rwx------");
    }

    @Test
    void f07_topeDeEntradas_enElLimiteSeAcepta_unoMasSeRechaza() throws Exception {
        var tres = new RecepcionArtefacto(datos.toString(), VEINTE_MB, 3, DESC, RecepcionArtefacto::nuevoDigest);
        assertThat(recibir(tres, zip(e("a"), e("b"), e("c"))).temporal()).exists();
        var ex = rechazo(tres, zip(e("a"), e("b"), e("c"), e("d")));
        assertThat(ex.estado()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(ex.error().codigo()).isEqualTo(CodigosCatalogo.ARTEFACTO_INVALIDO);
        assertThat(ex.error().detalles()).containsExactly(
                new DetalleError("archivo", MotivosCatalogo.DEMASIADAS_ENTRADAS.formatted(3)));
    }

    @Test
    void f07_topeDeEntradas_tambienSiElEocdMiente() throws Exception {
        var z = zip(e("a"), e("b"), e("c"), e("d"));
        int eocd = indice(z, 'P', 'K', 5, 6, true);
        java.nio.ByteBuffer.wrap(z).order(java.nio.ByteOrder.LITTLE_ENDIAN).putShort(eocd + 10, (short) 1);
        var tres = new RecepcionArtefacto(datos.toString(), VEINTE_MB, 3, DESC, RecepcionArtefacto::nuevoDigest);
        var ex = rechazo(tres, z);
        assertThat(ex.error().detalles().get(0).motivo()).isEqualTo(MotivosCatalogo.DEMASIADAS_ENTRADAS.formatted(3));
    }

    @Test
    void f07_topeDeEntradas_seDecideAntesDeLeerElDirectorioCentral() throws Exception {
        var z = zip(e("a"), e("b"), e("c"), e("d"));
        int cd = indice(z, 'P', 'K', 1, 2, false);
        z[cd] = 'X'; // commons ya no podría leerlo, pero el EOCD dice 4 entradas
        var tres = new RecepcionArtefacto(datos.toString(), VEINTE_MB, 3, DESC, RecepcionArtefacto::nuevoDigest);
        var ex = rechazo(tres, z);
        assertThat(ex.error().detalles().get(0).motivo()).isEqualTo(MotivosCatalogo.DEMASIADAS_ENTRADAS.formatted(3));
    }

    @Test
    void f07_topePorDefectoDiezMil() throws Exception {
        var p = new Properties();
        try (var in = getClass().getResourceAsStream("/application.properties")) {
            p.load(new InputStreamReader(in, StandardCharsets.UTF_8));
        }
        assertThat(p.getProperty("synkra.max-entradas-artefacto"))
                .isEqualTo("${SYNKRA_MAX_ENTRADAS_ARTEFACTO:10000}");
    }

    // ---------- ronda 3: consenso de lectores, con ZIP armados byte a byte ----------

    /** Constructor mínimo de ZIP para fixtures hostiles: cada parte se escribe como la dice el caso. */
    private static final class Zb {
        static byte[] deflate(byte[] d) {
            var df = new java.util.zip.Deflater(9, true);
            df.setInput(d);
            df.finish();
            var o = new ByteArrayOutputStream();
            var b = new byte[4096];
            while (!df.finished()) {
                o.write(b, 0, df.deflate(b));
            }
            df.end();
            return o.toByteArray();
        }

        static int crc(byte[] d) {
            var c = new java.util.zip.CRC32();
            c.update(d);
            return (int) c.getValue();
        }

        static java.nio.ByteBuffer buf(int n) {
            return java.nio.ByteBuffer.allocate(n).order(java.nio.ByteOrder.LITTLE_ENDIAN);
        }

        static byte[] cat(byte[]... partes) {
            var o = new ByteArrayOutputStream();
            for (var p : partes) {
                o.writeBytes(p);
            }
            return o.toByteArray();
        }

        static byte[] loc(String nombre, int metodo, int flag, int crc, long cs, long us, byte[] extra) {
            var n = nombre.getBytes(StandardCharsets.UTF_8);
            var b = buf(30).putInt(0x04034b50).putShort((short) 20).putShort((short) flag).putShort((short) metodo)
                    .putShort((short) 0).putShort((short) 0).putInt(crc).putInt((int) cs).putInt((int) us)
                    .putShort((short) n.length).putShort((short) extra.length);
            return cat(b.array(), n, extra);
        }

        static byte[] cen(String nombre, long off, int metodo, int flag, int crc, long cs, long us, byte[] extra) {
            var n = nombre.getBytes(StandardCharsets.UTF_8);
            var b = buf(46).putInt(0x02014b50).putShort((short) ((3 << 8) | 20)).putShort((short) 20)
                    .putShort((short) flag).putShort((short) metodo).putShort((short) 0).putShort((short) 0)
                    .putInt(crc).putInt((int) cs).putInt((int) us).putShort((short) n.length)
                    .putShort((short) extra.length).putShort((short) 0).putShort((short) 0).putShort((short) 0)
                    .putInt(0100644 << 16).putInt((int) off);
            return cat(b.array(), n, extra);
        }

        static byte[] eocd(int n, long cdSize, long cdOff, byte[] comentario) {
            var b = buf(22).putInt(0x06054b50).putShort((short) 0).putShort((short) 0).putShort((short) n)
                    .putShort((short) n).putInt((int) cdSize).putInt((int) cdOff).putShort((short) comentario.length);
            return cat(b.array(), comentario);
        }

        static byte[] build(byte[] cuerpo, byte[] cd, int nEocd, byte[] comentario) {
            return cat(cuerpo, cd, eocd(nEocd, cd.length, cuerpo.length, comentario));
        }

        static byte[] e64(byte[] base, byte[] cd, long nEntradas, long cdOff, long posLocalizador) {
            long pos64 = base.length + cd.length;
            var rec = buf(56).putInt(0x06064b50).putLong(44).putShort((short) 45).putShort((short) 45).putInt(0)
                    .putInt(0).putLong(nEntradas).putLong(nEntradas).putLong(cd.length).putLong(cdOff).array();
            var locr = buf(20).putInt(0x07064b50).putInt(0).putLong(posLocalizador < 0 ? pos64 : posLocalizador)
                    .putInt(1).array();
            var fin = buf(22).putInt(0x06054b50).putShort((short) 0).putShort((short) 0).putShort((short) -1)
                    .putShort((short) -1).putInt(-1).putInt(-1).putShort((short) 0).array();
            return cat(base, cd, rec, locr, fin);
        }
    }

    private static final byte[] NADA = new byte[0];
    private static final byte[] CONTENIDO = "contenido de a".repeat(10).getBytes(StandardCharsets.UTF_8);

    private static byte[] unaEntrada(String nombre) {
        var d = Zb.deflate(CONTENIDO);
        return Zb.cat(Zb.loc(nombre, 8, 0, Zb.crc(CONTENIDO), d.length, CONTENIDO.length, NADA), d);
    }

    private static byte[] centralDe(String nombre, long off, long csExtra) {
        var d = Zb.deflate(CONTENIDO);
        return Zb.cen(nombre, off, 8, 0, Zb.crc(CONTENIDO), d.length + csExtra, CONTENIDO.length, NADA);
    }

    private static byte[] cabeceraOculta() {
        var h = "malo".getBytes(StandardCharsets.UTF_8);
        return Zb.cat(Zb.loc("../../evil", 0, 0, Zb.crc(h), h.length, h.length, NADA), h);
    }

    private ExcepcionApi rechazado422(byte[] z) throws IOException {
        var ex = rechazo(componente(VEINTE_MB), z);
        assertThat(ex.estado()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        nadaEnDisco();
        return ex;
    }

    private void noCoinciden(byte[] z) throws IOException {
        var ex = rechazado422(z);
        assertThat(ex.error().codigo()).isEqualTo(CodigosCatalogo.RUTA_NO_PERMITIDA);
        assertThat(ex.error().detalles())
                .containsExactly(new DetalleError("archivo", MotivosCatalogo.ENTRADAS_NO_COINCIDEN));
    }

    @Test
    void r3_soloCabecerasLocalesConEocdEnCero_seRechaza() throws Exception {
        noCoinciden(Zb.cat(cabeceraOculta(), Zb.eocd(0, 0, 0, NADA)));
    }

    @Test
    void r3_eocdFalsoDentroDelComentarioDelVerdadero_seRechaza() throws Exception {
        var fake = Zb.eocd(0, 0, 0, NADA);
        var comentario = Zb.cat("hola".getBytes(StandardCharsets.UTF_8), fake);
        var cuerpo = unaEntrada("../../evil");
        noCoinciden(Zb.build(cuerpo, centralDe("../../evil", 0, 0), 1, comentario));
    }

    @Test
    void r3_entradaOcultaDentroDelCsizeDelCentral_sinBit3_seRechaza() throws Exception {
        var d = Zb.deflate(CONTENIDO);
        var oculta = cabeceraOculta();
        var cuerpo = Zb.cat(Zb.loc("a.txt", 8, 0, Zb.crc(CONTENIDO), d.length, CONTENIDO.length, NADA), d, oculta);
        noCoinciden(Zb.build(cuerpo, centralDe("a.txt", 0, oculta.length), 1, NADA));
    }

    @Test
    void r3_entradaOcultaDentroDelCsizeDelCentral_conBit3YDescriptor_seRechaza() throws Exception {
        var d = Zb.deflate(CONTENIDO);
        var dd = Zb.buf(16).putInt(0x08074b50).putInt(Zb.crc(CONTENIDO)).putInt(d.length).putInt(CONTENIDO.length)
                .array();
        var oculta = cabeceraOculta();
        var cuerpo = Zb.cat(Zb.loc("a.txt", 8, 8, 0, 0, 0, NADA), d, dd, oculta);
        var central = Zb.cen("a.txt", 0, 8, 8, Zb.crc(CONTENIDO), d.length + dd.length + oculta.length,
                CONTENIDO.length, NADA);
        noCoinciden(Zb.build(cuerpo, central, 1, NADA));
    }

    @Test
    void r3_metodoLocalDistintoDelCentral_seRechaza422() throws Exception {
        // STORED en la cabecera local y DEFLATED en el central: el lector en flujo lee otros bytes (CRC no cuadra)
        var d = Zb.deflate(CONTENIDO);
        var cuerpo = Zb.cat(Zb.loc("a.txt", 0, 0, Zb.crc(CONTENIDO), d.length, d.length, NADA), d);
        var ex = rechazado422(Zb.build(cuerpo, centralDe("a.txt", 0, 0), 1, NADA));
        assertThat(ex.error().codigo()).isEqualTo(CodigosCatalogo.ARTEFACTO_INVALIDO);
    }

    @Test
    void r3_topeDeEntradasTambienEnElIndiceDeLaJdk_antesDeComparar() throws Exception {
        // una sola cabecera local, pero el directorio central (y su conteo, honesto) declara 4 entradas
        var cuerpo = unaEntrada("a");
        var cd = Zb.cat(centralDe("a", 0, 0), centralDe("b", 0, 0), centralDe("c", 0, 0), centralDe("d", 0, 0));
        var tres = new RecepcionArtefacto(datos.toString(), VEINTE_MB, 3, DESC, RecepcionArtefacto::nuevoDigest);
        var ex = rechazo(tres, Zb.build(cuerpo, cd, 4, NADA));
        assertThat(ex.error().codigo()).isEqualTo(CodigosCatalogo.ARTEFACTO_INVALIDO);
        assertThat(ex.error().detalles())
                .containsExactly(new DetalleError("archivo", MotivosCatalogo.DEMASIADAS_ENTRADAS.formatted(3)));
    }

    @Test
    void r3_nombreLocalMaloYCentralBueno_seRechaza() throws Exception {
        noCoinciden(reemplazar(zip(e("ok/evil123")), "ok/evil123", "../../evil", false));
    }

    @Test
    void r3_zipMaloAntepuestoAUnoBueno_seRechaza() throws Exception {
        var malo = Zb.build(unaEntrada("../../evil"), centralDe("../../evil", 0, 0), 1, NADA);
        var bueno = Zb.build(unaEntrada("ok"), centralDe("ok", 0, 0), 1, NADA);
        var ex = rechazado422(Zb.cat(malo, bueno));
        assertThat(ex.error().codigo()).isEqualTo(CodigosCatalogo.RUTA_NO_PERMITIDA);
    }

    @Test
    void r3_zip64_conOffsetDelCentralNegativo_da422YNoDa500() throws Exception {
        var base = unaEntrada("a.txt");
        var cd = centralDe("a.txt", 0, 0);
        var ex = rechazado422(Zb.e64(base, cd, 1, -100L, -1));
        assertThat(ex.error().codigo()).isEqualTo(CodigosCatalogo.ARTEFACTO_INVALIDO);
    }

    @Test
    void r3_zip64_conEntradasNegativas_da422YNoDa500() throws Exception {
        var base = unaEntrada("a.txt");
        var ex = rechazado422(Zb.e64(base, centralDe("a.txt", 0, 0), -1L, base.length, -1));
        assertThat(ex.error().codigo()).isEqualTo(CodigosCatalogo.ARTEFACTO_INVALIDO);
    }

    @Test
    void r3_zip64_valido_seAcepta() throws Exception {
        var base = unaEntrada("a.txt");
        var r = recibir(componente(VEINTE_MB), Zb.e64(base, centralDe("a.txt", 0, 0), 1, base.length, -1));
        assertThat(r.temporal()).exists();
    }

    @Test
    void r3_jarValidoConComentarioLargo_seAcepta() throws Exception {
        var base = unaEntrada("a.txt");
        var comentario = "x".repeat(1000).getBytes(StandardCharsets.UTF_8);
        var r = recibir(componente(VEINTE_MB), Zb.build(base, centralDe("a.txt", 0, 0), 1, comentario));
        assertThat(r.temporal()).exists();
    }

    @Test
    void r3_bombaDeDescompresion_seRechazaPorElTopeDeDescomprimido() throws Exception {
        var salida = new ByteArrayOutputStream();
        try (var z = new java.util.zip.ZipOutputStream(salida)) {
            z.putNextEntry(new java.util.zip.ZipEntry("bomba.bin"));
            var ceros = new byte[1 << 20];
            for (int i = 0; i < 5; i++) {
                z.write(ceros);
            }
            z.closeEntry();
        }
        var bytes = salida.toByteArray();
        assertThat(bytes.length).as("comprime muy por debajo de los 5 MB").isLessThan(100_000);
        var c = new RecepcionArtefacto(datos.toString(), VEINTE_MB, 10000, DataSize.ofMegabytes(1),
                RecepcionArtefacto::nuevoDigest);
        var ex = rechazo(c, bytes);
        assertThat(ex.estado()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(ex.error().codigo()).isEqualTo(CodigosCatalogo.ARTEFACTO_INVALIDO);
        assertThat(ex.error().detalles()).containsExactly(
                new DetalleError("archivo", MotivosCatalogo.DESCOMPRIMIDO_EXCEDIDO.formatted("1 MB")));
        nadaEnDisco();
        // y justo en el tope se acepta
        var justo = new RecepcionArtefacto(datos.toString(), VEINTE_MB, 10000, DataSize.ofMegabytes(5),
                RecepcionArtefacto::nuevoDigest);
        assertThat(recibir(justo, bytes).temporal()).exists();
    }

    @Test
    void r3_elTopeDeDescomprimidoPorDefectoEs200Mb() throws Exception {
        var p = new Properties();
        try (var in = getClass().getResourceAsStream("/application.properties")) {
            p.load(new InputStreamReader(in, StandardCharsets.UTF_8));
        }
        assertThat(p.getProperty("synkra.max-descomprimido-artefacto"))
                .isEqualTo("${SYNKRA_MAX_DESCOMPRIMIDO_ARTEFACTO:200MB}");
    }

    @Test
    void r3_cualquierExcepcionDelLectorEnLaRevision_es422() throws Exception {
        // nombre local no UTF-8 válido: la JDK lanza IllegalArgumentException, que no puede salir como 500
        var d = Zb.deflate(CONTENIDO);
        var malo = Zb.cat(Zb.loc("a", 8, 0, Zb.crc(CONTENIDO), d.length, CONTENIDO.length, NADA), d);
        malo[30] = (byte) 0xC3; // 0xC3 solo, sin continuación
        var cd = centralDe("a", 0, 0);
        var ex = rechazado422(Zb.build(malo, cd, 1, NADA));
        assertThat(ex.error().codigo()).isIn(CodigosCatalogo.ARTEFACTO_INVALIDO, CodigosCatalogo.RUTA_NO_PERMITIDA);
    }

    /** Proceso aparte con -Xmx256m: el EOCD que miente no puede cargar el directorio central en memoria. */
    public static final class Sonda {
        public static void main(String[] args) throws Exception {
            var c = new RecepcionArtefacto(args[0], DataSize.ofMegabytes(20), 10000, DataSize.ofMegabytes(200),
                    RecepcionArtefacto::nuevoDigest);
            try (var in = Files.newInputStream(Path.of(args[1]))) {
                c.recibir(in);
                System.out.println("ACEPTADO");
            } catch (ExcepcionApi e) {
                System.out.println("RECHAZADO " + e.estado().value() + " " + e.error().codigo());
            }
        }
    }

    @Test
    void r3_eocdQueMiente_con20MbDeDirectorioCentral_noAgotaLaMemoriaDeUnaJvmDe256Mb() throws Exception {
        var base = unaEntrada("a");
        var una = centralDe("a", 0, 0);
        int n = (20 * 1024 * 1024 - base.length - 22) / una.length;
        var cd = java.nio.ByteBuffer.allocate(n * una.length);
        for (int i = 0; i < n; i++) {
            cd.put(una);
        }
        var jar = datos.resolve("miente.zip");
        Files.write(jar, Zb.build(base, cd.array(), 1, NADA));
        var java = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        var proceso = new ProcessBuilder(java, "-Xmx256m", "-cp", System.getProperty("java.class.path"),
                Sonda.class.getName(), datos.resolve("sonda").toString(), jar.toString())
                .redirectErrorStream(true).start();
        var salida = new String(proceso.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertThat(proceso.waitFor()).as(salida).isZero();
        assertThat(salida).contains("RECHAZADO 422");
        assertThat(archivos(datos.resolve("sonda/artefactos"))).isEmpty();
    }

    private static Throwable catchThrowable(org.assertj.core.api.ThrowableAssert.ThrowingCallable c) {
        return org.assertj.core.api.Assertions.catchThrowable(c);
    }
}
