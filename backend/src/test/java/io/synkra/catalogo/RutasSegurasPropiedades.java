package io.synkra.catalogo;

import static org.assertj.core.api.Assertions.assertThat;

import io.synkra.plataforma.ExcepcionApi;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.Tuple;
import net.jqwik.api.statistics.Statistics;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.springframework.http.HttpStatus;
import org.springframework.util.unit.DataSize;

/**
 * PBT-03 (R-C6): ninguna entrada que {@link RecepcionArtefacto} acepta resuelve fuera del directorio de destino ni es un
 * enlace simbólico, y todo JAR rechazado deja el disco vacío. Los JAR se generan en memoria con nombres hechos de
 * segmentos normales y hostiles (al menos un 30 % de hostiles) y el componente se construye a mano, sin Spring, con un
 * {@code synkra.data} temporal por intento. La semilla la elige jqwik en cada ejecución y queda en el reporte (PBT-08).
 *
 * <p>El oráculo no llama a la comprobación del código: resuelve cada nombre generado contra un destino absoluto
 * ficticio, con {@code resolve(...).normalize()}, en las tres lecturas que puede hacer un extractor: la de Linux (la
 * {@code \} es un carácter más), la que trata la {@code \} como separador (Info-ZIP, Windows) y la de Windows con una
 * letra de unidad al principio (que nunca cae dentro del destino). Los enlaces los dice el modelo del generador.
 */
class RutasSegurasPropiedades {

    /** Destino ficticio del oráculo: nunca se escribe en él. */
    private static final Path DESTINO = Path.of("/srv/synkra-oraculo/destino");

    /** En Windows, {@code C:x} y {@code C:\x} dependen de la unidad, no del destino. */
    private static final Pattern UNIDAD_WINDOWS = Pattern.compile("^[A-Za-z]:.*", Pattern.DOTALL);

    private static final String NORMAL = "normal";

    // ---------------------------------------------------------------- modelo

    record Segmento(String tipo, String texto) {
        boolean hostil() {
            return !NORMAL.equals(tipo);
        }
    }

    record Entrada(List<Segmento> segmentos, boolean directorio, boolean enlace) {
        String nombre() {
            var nombre = String.join("/", segmentos.stream().map(Segmento::texto).toList());
            return directorio ? nombre + "/" : nombre;
        }

        @Override
        public String toString() {
            return (enlace ? "ENLACE " : "") + "'" + nombre().replace("\u0000", "\\0") + "'";
        }
    }

    record Jar(List<Entrada> entradas) {
        byte[] bytes() {
            var salida = new ByteArrayOutputStream();
            try (var z = new ZipArchiveOutputStream(salida)) {
                for (var en : entradas) {
                    var nombre = en.nombre();
                    var ze = new ZipArchiveEntry("x") {
                        {
                            setPlatform(PLATFORM_UNIX); // el nombre va crudo: sin normalizar la \ a /
                            setName(nombre, nombre.getBytes(StandardCharsets.UTF_8));
                        }
                    };
                    ze.setUnixMode(en.enlace() ? 0120777 : en.directorio() ? 040755 : 0100644);
                    z.putArchiveEntry(ze);
                    z.write(en.enlace() ? "../../../etc/passwd".getBytes(StandardCharsets.UTF_8) : new byte[] {1, 2, 3});
                    z.closeArchiveEntry();
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            return salida.toByteArray();
        }
    }

    // ---------------------------------------------------------------- generadores

    private static Arbitrary<Segmento> de(String tipo, Arbitrary<String> textos) {
        return textos.map(t -> new Segmento(tipo, t));
    }

    private static Arbitrary<Segmento> de(String tipo, String... textos) {
        return de(tipo, Arbitraries.of(textos));
    }

    /** Pesos sobre 100: 60 normales y 40 hostiles, repartidos en los siete tipos de la decisión 3. */
    static Arbitrary<Segmento> segmentos() {
        var normales = de(NORMAL, Arbitraries.oneOf(
                Arbitraries.strings().withChars("abcxyz019_-").ofMinLength(1).ofMaxLength(8),
                Arbitraries.of("META-INF", "Clase.class", "a..b", "...", "x.y", "..a", "b..")));
        var puntos = de("puntos", "..", ".");
        var vacio = de("vacio", "");
        var barra = de("barra invertida", "..\\..", "a\\b", "\\", "..\\x", "x\\..", "\\..\\", "a\\..\\..\\b", "\\etc");
        var unidad = de("unidad", "C:", "c:", "Z:\\", "C:x", "d:..", "C:\\..", "z:/");
        var absoluto = de("absoluto", "/etc", "/", "\\x", "//servidor");
        var unicode = de("unicode", Arbitraries.oneOf(
                Arbitraries.of("é", "ñandú", "日本語", "\u202Ex", "\uFF0E\uFF0E", "\u2024\u2024", "\u2215..\u2215",
                        "\uFF0F", "a\u0301", "\uFEFF..", "..\u200B"),
                Arbitraries.strings().withCharRange('\u00A0', '\uD7FF').ofMinLength(1).ofMaxLength(4)));
        var nul = de("NUL", "\u0000", "a\u0000b", "\u0000..", "..\u0000", "x.class\u0000/../..");
        return Arbitraries.frequencyOf(
                Tuple.of(60, normales),
                Tuple.of(7, puntos),
                Tuple.of(5, vacio),
                Tuple.of(7, barra),
                Tuple.of(5, unidad),
                Tuple.of(5, absoluto),
                Tuple.of(6, unicode),
                Tuple.of(5, nul));
    }

    static Arbitrary<Entrada> entradas() {
        var segs = segmentos().list().ofMinSize(1).ofMaxSize(3);
        // 1 de cada 10 lleva el bit de enlace simbólico; otra de cada 10, la barra final de directorio.
        var forma = Arbitraries.integers().between(0, 9);
        return Combinators.combine(segs, forma).as((s, f) -> new Entrada(s, f == 1, f == 0));
    }

    @Provide
    Arbitrary<Jar> jars() {
        return entradas().list().ofMinSize(1).ofMaxSize(3).map(Jar::new);
    }

    // ---------------------------------------------------------------- oráculo

    /**
     * Dentro del destino en las tres lecturas; un nombre que no es una ruta válida (NUL) no está dentro. Antes de
     * resolver, cada carácter fuera de ASCII pasa a {@code u}: para la resolución léxica solo cuentan {@code /},
     * {@code \}, {@code .}, {@code :} y NUL, y así el oráculo no depende de la codificación de nombres de la JVM
     * ({@code sun.jnu.encoding}, que con la locale POSIX no admite {@code é}).
     */
    static boolean dentroDelDestino(String nombre) {
        var ascii = nombre.codePoints().map(c -> c < 0x80 ? c : 'u')
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append).toString();
        var conBarras = ascii.replace('\\', '/');
        if (UNIDAD_WINDOWS.matcher(conBarras).matches()) {
            return false;
        }
        for (var lectura : List.of(ascii, conBarras)) {
            Path ruta;
            try {
                ruta = DESTINO.resolve(lectura).normalize();
            } catch (java.nio.file.InvalidPathException noEsRuta) {
                return false;
            }
            if (!ruta.startsWith(DESTINO)) {
                return false;
            }
        }
        return true;
    }

    // ---------------------------------------------------------------- ejecución

    private record Resultado(boolean aceptado, Path datos) {}

    private static Resultado recibir(Jar jar) throws IOException {
        var datos = Files.createTempDirectory("pbt03-");
        var componente = new RecepcionArtefacto(datos.toString(), DataSize.ofMegabytes(20), 10000,
                DataSize.ofMegabytes(200), RecepcionArtefacto::nuevoDigest);
        try {
            var r = componente.recibir(new ByteArrayInputStream(jar.bytes()));
            assertThat(r.temporal()).isRegularFile().hasParent(datos.resolve("artefactos").resolve(".tmp"));
            return new Resultado(true, datos);
        } catch (ExcepcionApi rechazo) {
            // un rechazo es siempre del JAR (422), nunca un fallo del servidor
            assertThat(rechazo.estado()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
            return new Resultado(false, datos);
        }
    }

    private static List<Path> archivos(Path raiz) throws IOException {
        try (Stream<Path> s = Files.walk(raiz)) {
            return s.filter(p -> !Files.isDirectory(p, java.nio.file.LinkOption.NOFOLLOW_LINKS)).toList();
        }
    }

    private static void borrar(Path raiz) throws IOException {
        try (Stream<Path> s = Files.walk(raiz)) {
            for (var p : s.sorted(Comparator.reverseOrder()).toList()) {
                Files.delete(p);
            }
        }
    }

    /** Cuenta y pone las puertas: sin un 20 % de aceptados y un 20 % de rechazados, la propiedad sería vacía. */
    private static void estadisticas(Jar jar, boolean aceptado) {
        Statistics.label("resultado").collect(aceptado ? "aceptado" : "rechazado");
        for (var en : jar.entradas()) {
            for (var s : en.segmentos()) {
                Statistics.label("segmento").collect(s.hostil() ? "hostil" : NORMAL);
                Statistics.label("tipo de segmento").collect(s.tipo());
            }
            Statistics.label("entrada").collect(en.enlace() ? "enlace" : en.directorio() ? "directorio" : "archivo");
        }
        Statistics.label("resultado").coverage(c -> {
            c.check("aceptado").percentage(p -> p >= 20);
            c.check("rechazado").percentage(p -> p >= 20);
        });
        Statistics.label("segmento").coverage(c -> c.check("hostil").percentage(p -> p >= 30));
    }

    // ---------------------------------------------------------------- propiedades

    @Property(tries = 1000)
    void lasEntradasAceptadasResuelvenDentroDelDestinoYNoSonEnlaces(@ForAll("jars") Jar jar) throws IOException {
        var r = recibir(jar);
        try {
            estadisticas(jar, r.aceptado());
            if (r.aceptado()) {
                for (var en : jar.entradas()) {
                    assertThat(dentroDelDestino(en.nombre())).as("aceptada y fuera del destino: %s", en).isTrue();
                    assertThat(en.enlace()).as("aceptada y es un enlace simbólico: %s", en).isFalse();
                }
            }
        } finally {
            borrar(r.datos());
        }
    }

    @Property(tries = 1000)
    void unJarRechazadoDejaElDiscoVacio(@ForAll("jars") Jar jar) throws IOException {
        var r = recibir(jar);
        try {
            estadisticas(jar, r.aceptado());
            var quedan = archivos(r.datos());
            if (r.aceptado()) {
                assertThat(quedan).singleElement()
                        .satisfies(p -> assertThat(p.getParent()).isEqualTo(r.datos().resolve("artefactos/.tmp")));
            } else {
                assertThat(quedan).as("rechazado y quedó algo en disco: %s", jar.entradas()).isEmpty();
            }
        } finally {
            borrar(r.datos());
        }
    }
}
