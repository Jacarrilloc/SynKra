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
        return new RecepcionArtefacto(datos.toString(), limite);
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

    private static Throwable catchThrowable(org.assertj.core.api.ThrowableAssert.ThrowingCallable c) {
        return org.assertj.core.api.Assertions.catchThrowable(c);
    }
}
