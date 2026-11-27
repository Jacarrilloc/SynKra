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
import net.jqwik.api.Example;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.Tuple;
import net.jqwik.api.statistics.Statistics;
import org.apache.commons.compress.archivers.zip.UnicodePathExtraField;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.springframework.http.HttpStatus;
import org.springframework.util.unit.DataSize;

/**
 * PBT-03 (R-C6): ninguna entrada que {@link RecepcionArtefacto} acepta resuelve fuera del directorio de destino ni es un
 * enlace simbólico, y todo JAR rechazado deja el disco vacío. Los JAR se generan en memoria con nombres hechos de
 * segmentos normales y hostiles (al menos un 30 % de hostiles), a veces con un extra Unicode 0x7075 y con modos de enlace
 * variados. El componente se construye a mano, sin Spring, con un {@code synkra.data} dentro de una caja temporal propia
 * de cada intento. La semilla la elige jqwik en cada ejecución y queda en el reporte (PBT-08).
 *
 * <p>El oráculo no llama a la comprobación del código: resuelve cada nombre generado contra un destino absoluto
 * ficticio, con {@code resolve(...).normalize()}, en las tres lecturas que puede hacer un extractor: la de Linux (la
 * {@code \} es un carácter más), la que trata la {@code \} como separador (Info-ZIP, Windows) y la de Windows con una
 * letra de unidad al principio (que nunca cae dentro del destino). Los enlaces los dice el modelo del generador.
 *
 * <p>Límite declarado (F-02 de la ronda 1): la expresión de la letra de unidad y el cambio de {@code \} por {@code /}
 * coinciden con los del código, porque en Linux no hay otra forma de leer un nombre como lo haría Windows. Un punto
 * ciego común a las dos copias no lo vería esta propiedad.
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

    /**
     * Una entrada del JAR. {@code modoEnlace} es el modo Unix completo cuando es un enlace (tipo {@code 0120000} con
     * permisos variados), o {@code null}. {@code unicode} es el nombre del extra 0x7075 ({@code UnicodePathExtraField}),
     * o {@code null} si no lleva: puede ser el mismo nombre o uno distinto (otro lector vería ese).
     */
    record Entrada(List<Segmento> segmentos, boolean directorio, Integer modoEnlace, String unicode) {
        String nombre() {
            var nombre = String.join("/", segmentos.stream().map(Segmento::texto).toList());
            return directorio ? nombre + "/" : nombre;
        }

        boolean enlace() {
            return modoEnlace != null;
        }

        /** Los nombres que puede ver algún lector: el crudo y, si lo hay, el del extra Unicode. */
        List<String> nombresVisibles() {
            return unicode == null ? List.of(nombre()) : List.of(nombre(), unicode);
        }

        @Override
        public String toString() {
            return (enlace() ? "ENLACE " + Integer.toOctalString(modoEnlace) + " " : "") + "'" + visible(nombre()) + "'"
                    + (unicode == null ? "" : " (0x7075 '" + visible(unicode) + "')");
        }

        private static String visible(String n) {
            return n.replace("\u0000", "\\0");
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
                    ze.setUnixMode(en.enlace() ? en.modoEnlace() : en.directorio() ? 040755 : 0100644);
                    if (en.unicode() != null) {
                        ze.addExtraField(new UnicodePathExtraField(en.unicode(), nombre.getBytes(StandardCharsets.UTF_8)));
                    }
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

    /** Modos de enlace: el tipo {@code 0120000} con permisos y bits especiales distintos. */
    private static final List<Integer> MODOS_ENLACE = List.of(0120777, 0120755, 0120644, 0120000, 0124777, 0127777);

    static Arbitrary<Entrada> entradas() {
        var segs = segmentos().list().ofMinSize(1).ofMaxSize(3);
        // 1 de cada 10 lleva el bit de enlace simbólico; otra de cada 10, la barra final de directorio.
        var forma = Arbitraries.integers().between(0, 9);
        var modo = Arbitraries.of(MODOS_ENLACE);
        // El extra 0x7075: sin él 8 de cada 10; con el mismo nombre 1; con otro nombre generado 1.
        var extra = Arbitraries.integers().between(0, 9);
        var otroNombre = segmentos().list().ofMinSize(1).ofMaxSize(3)
                .map(l -> String.join("/", l.stream().map(Segmento::texto).toList()));
        return Combinators.combine(segs, forma, modo, extra, otroNombre).as((s, f, m, x, otro) -> {
            var sinExtra = new Entrada(s, f == 1, f == 0 ? m : null, null);
            String unicode = x == 0 ? sinExtra.nombre() : x == 1 ? otro : null;
            return new Entrada(s, f == 1, f == 0 ? m : null, unicode);
        });
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
     * ({@code sun.jnu.encoding}, que con la locale POSIX no admite {@code é}). Hace falta un descendiente estricto: el
     * destino mismo ({@code ""}) no cuenta como dentro. La excepción son las referencias explícitas al destino, que se
     * miran en cada lectura: hechas solo de {@code .} y {@code /} ({@code "."}, {@code "./"} y, en la lectura con
     * {@code \} como separador, {@code "./\"} o {@code ".\"}). El código las acepta, no salen del destino, y quedan
     * anotadas en la bitácora para que se decida.
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
            // F-03 y F-05: el destino mismo no es «dentro», salvo la referencia explícita a él en esta lectura (no
            // vacía, solo de . y /; en la lectura con \ como separador, la \ ya es /). Pendiente de decidir.
            boolean autoReferencia = !lectura.isEmpty() && lectura.chars().allMatch(c -> c == '.' || c == '/');
            if (!ruta.startsWith(DESTINO) || (ruta.equals(DESTINO) && !autoReferencia)) {
                return false;
            }
        }
        return true;
    }

    // ---------------------------------------------------------------- ejecución

    /**
     * {@code caja} es un directorio propio de cada intento; {@code synkra.data} es {@code caja/datos}. Lo que el
     * componente escriba al lado de {@code datos} (en su padre) cae en la caja y se ve. {@code RecepcionArtefacto} no
     * usa {@code java.io.tmpdir}: crea su temporal con {@code Files.createTempFile(dir, ...)} en {@code .tmp}.
     */
    private record Resultado(boolean aceptado, Path caja, Path datos) {}

    /** F-06: los {@code recepcion-*} sueltos en el {@code java.io.tmpdir} de la JVM, por si algo sale de la caja. */
    private static java.util.Set<String> sueltosEnTmp() throws IOException {
        try (Stream<Path> s = Files.list(Path.of(System.getProperty("java.io.tmpdir")))) {
            return s.map(p -> p.getFileName().toString()).filter(n -> n.startsWith("recepcion-"))
                    .collect(java.util.stream.Collectors.toSet());
        }
    }

    private static Resultado recibir(Jar jar) throws IOException {
        var antes = sueltosEnTmp();
        try {
            return recibirEnCaja(jar);
        } finally {
            var nuevos = new java.util.HashSet<>(sueltosEnTmp());
            nuevos.removeAll(antes);
            assertThat(nuevos).as("recepcion-* nuevos en java.io.tmpdir, fuera de la caja").isEmpty();
        }
    }

    private static Resultado recibirEnCaja(Jar jar) throws IOException {
        var caja = Files.createTempDirectory("pbt03-caja-");
        var datos = Files.createDirectory(caja.resolve("datos"));
        var componente = new RecepcionArtefacto(datos.toString(), DataSize.ofMegabytes(20), 10000,
                DataSize.ofMegabytes(200), RecepcionArtefacto::nuevoDigest);
        try {
            var r = componente.recibir(new ByteArrayInputStream(jar.bytes()));
            assertThat(r.temporal()).isRegularFile().hasParent(datos.resolve("artefactos").resolve(".tmp"));
            return new Resultado(true, caja, datos);
        } catch (ExcepcionApi rechazo) {
            // un rechazo es siempre del JAR (422), nunca un fallo del servidor
            assertThat(rechazo.estado()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
            return new Resultado(false, caja, datos);
        }
    }

    /** Todas las rutas bajo {@code raiz}, directorios incluidos, relativas a ella (sin seguir enlaces). */
    private static java.util.Set<String> rutas(Path raiz) throws IOException {
        try (Stream<Path> s = Files.walk(raiz)) {
            return s.map(p -> raiz.relativize(p).toString()).collect(java.util.stream.Collectors.toSet());
        }
    }

    /** Lo único que puede quedar en la caja tras un rechazo: los directorios de {@code synkra.data} y su {@code .tmp}. */
    private static final java.util.Set<String> CAJA_TRAS_RECHAZO =
            java.util.Set.of("", "datos", "datos/artefactos", "datos/artefactos/.tmp");

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
            Statistics.label("extra unicode").collect(en.unicode() == null ? "sin extra"
                    : en.unicode().equals(en.nombre()) ? "mismo nombre" : "otro nombre");
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
                    for (var nombre : en.nombresVisibles()) {
                        assertThat(dentroDelDestino(nombre)).as("aceptada y fuera del destino: %s", en).isTrue();
                    }
                    assertThat(en.enlace()).as("aceptada y es un enlace simbólico: %s", en).isFalse();
                }
            }
        } finally {
            borrar(r.caja());
        }
    }

    @Property(tries = 1000)
    void unJarRechazadoDejaElDiscoVacio(@ForAll("jars") Jar jar) throws IOException {
        var r = recibir(jar);
        try {
            estadisticas(jar, r.aceptado());
            var quedan = rutas(r.caja());
            if (r.aceptado()) {
                // los directorios de siempre y exactamente un archivo más: el temporal, directo en .tmp
                assertThat(quedan).containsAll(CAJA_TRAS_RECHAZO).hasSize(CAJA_TRAS_RECHAZO.size() + 1);
                var extra = quedan.stream().filter(q -> !CAJA_TRAS_RECHAZO.contains(q)).findFirst().orElseThrow();
                assertThat(r.caja().resolve(extra)).isRegularFile().hasParent(r.datos().resolve("artefactos/.tmp"));
            } else {
                assertThat(quedan).as("rechazado y quedó algo en disco: %s", jar.entradas())
                        .containsExactlyInAnyOrderElementsOf(CAJA_TRAS_RECHAZO);
            }
        } finally {
            borrar(r.caja());
        }
    }

    // ---------------------------------------------------------------- casos de ejemplo (F-05)

    /**
     * F-05 de la ronda 2: referencias al destino mismo escritas con {@code \}. El código las acepta y en ninguna lectura
     * salen del destino: el oráculo tiene que decir «dentro». La semilla -4345582717168822446 las encontraba.
     */
    @Example
    void autoReferenciasConBarraInvertida_seAceptanYElOraculoLasDaDentro() throws IOException {
        for (var nombre : List.of("./\\", ".\\", "./\\/")) {
            var jar = new Jar(List.of(new Entrada(List.of(new Segmento("ejemplo", nombre)), false, null, null)));
            var r = recibir(jar);
            try {
                assertThat(r.aceptado()).as("el código acepta '%s'", nombre).isTrue();
                assertThat(dentroDelDestino(nombre)).as("el oráculo da dentro a '%s'", nombre).isTrue();
            } finally {
                borrar(r.caja());
            }
        }
    }

    /** P7: el nombre vacío resuelve al destino mismo y no es una referencia explícita: para el oráculo está fuera. */
    @Example
    void elNombreVacioNoEstaDentro() {
        assertThat(dentroDelDestino("")).isFalse();
    }
}
