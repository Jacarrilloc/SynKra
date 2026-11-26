package io.synkra.verificador;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;

/** Generadores compartidos de las propiedades del verificador (PBT-07). Ninguno usa {@code filter} ni {@code Assume}. */
final class Generadores {

    /** Textos que YAML 1.2 Core resuelve como otro tipo o que cambian la sintaxis si salen sin comillas (C-15). */
    static final List<String> AMBIGUOS = List.of(
            "~", "null", "Null", "NULL", "true", "True", "FALSE", "false", "007", "0x1F", "0o17", "12", "-3", "1e3",
            "1.5", ".NaN", "-.Inf", ".inf", "a: b", "a #b", "x # y", "# c", "- a", "? a", "&a", "*a", "!t", "{a}",
            "[b]", "'q'", "\"d\"", "@a", "%d", "|", ">", "---", "...", " ", "", " p", "p ", "l1\nl2", "l1\n\nl3\n",
            "tab\taqui", "ñandú", "日本語", "emoji 😀", "é",
            "palabra ".repeat(15).trim(), "a\rb", "a\u0085b", "a\u2028b", "\uFEFFbom", "c\u0007d", "+.inf", ".NAN",
            "+12", "yes", "no", "on", "Yes", "OFF");

    private static final List<String> NOMBRES_ENGANOSOS = List.of("007", "true", "null", "1", "0", "false", "10-20");

    /** Cota superior (inclusive) de cada componente de versión generado. */
    static final int MAX_COMPONENTE = 50;

    private Generadores() {}

    static Arbitrary<Integer> componente() {
        return Arbitraries.integers().between(0, MAX_COMPONENTE);
    }

    static Arbitrary<Version> versiones() {
        return Combinators.combine(componente(), componente(), componente()).as(Version::new);
    }

    private static Arbitrary<String> espacios() {
        return Arbitraries.integers().between(0, 3).map(" "::repeat);
    }

    /** Un entero estrictamente mayor que {@code x} y de máximo {@code MAX_COMPONENTE} (para {@code x < MAX}); reduce hacia {@code x + 1}. */
    private static Arbitrary<Integer> mayorQueAcotado(int x) {
        return Arbitraries.integers().between(0, MAX_COMPONENTE - 1 - x).map(d -> x + 1 + d);
    }

    /** Una versión estrictamente mayor que {@code a} con todos sus componentes en {@code 0..MAX_COMPONENTE} (C-10, F-01). Requiere {@code a.mayor() < MAX_COMPONENTE}. */
    private static Arbitrary<Version> mayorQue(Version a) {
        var opciones = new java.util.ArrayList<Arbitrary<Version>>();
        if (a.parche() < MAX_COMPONENTE) {
            opciones.add(mayorQueAcotado(a.parche()).map(p -> new Version(a.mayor(), a.menor(), p)));
        }
        if (a.menor() < MAX_COMPONENTE) {
            opciones.add(Combinators.combine(mayorQueAcotado(a.menor()), componente())
                    .as((m, p) -> new Version(a.mayor(), m, p)));
        }
        opciones.add(Combinators.combine(mayorQueAcotado(a.mayor()), componente(), componente()).as(Version::new));
        return Arbitraries.oneOf(opciones);
    }

    /** Las cuatro formas de R-V2. En el intervalo cerrado solo el mayor del mínimo se acota (a {@code < MAX_COMPONENTE}) para que siempre haya un máximo. */
    static Arbitrary<RangoVersion> rangos() {
        Arbitrary<RangoVersion> mayorX = componente().map(n -> new RangoVersion(
                new Version(n, 0, 0), Optional.of(new Version(n + 1, 0, 0)), n + ".x"));
        Arbitrary<RangoVersion> menorX = Combinators.combine(componente(), componente())
                .as((n, m) -> new RangoVersion(
                        new Version(n, m, 0), Optional.of(new Version(n, m + 1, 0)), n + "." + m + ".x"));
        Arbitrary<RangoVersion> exacta = versiones()
                .map(v -> new RangoVersion(
                        v, Optional.of(new Version(v.mayor(), v.menor(), v.parche() + 1)), v.toString()));
        Arbitrary<RangoVersion> abierto = Combinators.combine(versiones(), espacios())
                .as((a, s) -> new RangoVersion(a, Optional.empty(), ">=" + s + a));
        Arbitrary<Version> minimosCerrados = Arbitraries.integers().between(0, MAX_COMPONENTE - 1).flatMap(x ->
                Combinators.combine(componente(), componente())
                        .as((y, z) -> new Version(x, y, z)));
        Arbitrary<RangoVersion> cerrado = minimosCerrados.flatMap(a ->
                Combinators.combine(mayorQue(a), espacios(), espacios(), espacios())
                        .as((b, s1, s2, s3) ->
                                new RangoVersion(a, Optional.of(b), ">=" + s1 + a + s2 + "<" + s3 + b)));
        return Arbitraries.oneOf(mayorX, menorX, exacta, abierto, cerrado);
    }

    /** Nombres que cumplen {@code ^[a-z0-9]+(-[a-z0-9]+)*$}, incluidos los que parecen otro tipo. */
    static Arbitrary<String> nombres() {
        Arbitrary<String> segmento =
                Arbitraries.strings().withCharRange('a', 'z').withCharRange('0', '9').ofMinLength(1).ofMaxLength(6);
        Arbitrary<String> compuesto = segmento.list().ofMinSize(1).ofMaxSize(3).map(l -> String.join("-", l));
        return Arbitraries.oneOf(compuesto, compuesto, Arbitraries.of(NOMBRES_ENGANOSOS));
    }

    /** Textos libres: los ambiguos de C-15 mezclados con texto común. */
    static Arbitrary<String> textos() {
        Arbitrary<String> comun = Arbitraries.strings()
                .withCharRange('a', 'z')
                .withCharRange('A', 'Z')
                .withCharRange('0', '9')
                .withChars(' ', ':', '#', '-', '.', ',', '\n', 'ñ', 'é', '日')
                .ofMaxLength(20);
        Arbitrary<String> ambiguo = Arbitraries.of(AMBIGUOS);
        Arbitrary<String> mezcla = Combinators.combine(ambiguo, comun, Arbitraries.of("", " ", "\n", ": ", " # "))
                .as((a, c, sep) -> a + sep + c);
        // Peso 6/10 a los ambiguos exactos: la prueba de cobertura depende de ello (ver DescriptorPropiedades).
        return Arbitraries.frequencyOf(
                net.jqwik.api.Tuple.of(6, ambiguo), net.jqwik.api.Tuple.of(2, comun), net.jqwik.api.Tuple.of(2, mezcla));
    }

    private static Arbitrary<Map<String, String>> etiquetas() {
        return Arbitraries.maps(nombres(), textos()).ofMinSize(0).ofMaxSize(5);
    }

    private static Arbitrary<Optional<String>> descripciones() {
        return Arbitraries.oneOf(Arbitraries.just(Optional.<String>empty()), textos().map(Optional::of));
    }

    /** Descriptores válidos: 0 a 5 etiquetas y {@code descripcion} presente o ausente. */
    static Arbitrary<Descriptor> descriptores() {
        return Combinators.combine(nombres(), versiones(), nombres(), rangos(), etiquetas(), descripciones())
                .as((nombre, version, core, rango, etiquetas, descripcion) ->
                        new Descriptor(nombre, version, core, rango, etiquetas, descripcion));
    }
}
