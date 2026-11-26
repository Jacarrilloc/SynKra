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
            "tab\taqui", "ñandú", "日本語", "emoji 😀", "é");

    private static final List<String> NOMBRES_ENGANOSOS = List.of("007", "true", "null", "1", "0", "false", "10-20");

    private Generadores() {}

    static Arbitrary<Integer> componente() {
        return Arbitraries.integers().between(0, 50);
    }

    static Arbitrary<Version> versiones() {
        return Combinators.combine(componente(), componente(), componente()).as(Version::new);
    }

    private static Arbitrary<String> espacios() {
        return Arbitraries.integers().between(0, 3).map(" "::repeat);
    }

    /** Un entero estrictamente mayor que {@code x} y de máximo 50 (para {@code x < 50}); reduce hacia {@code x + 1}. */
    private static Arbitrary<Integer> mayorQueAcotado(int x) {
        return Arbitraries.integers().between(0, 49 - x).map(d -> x + 1 + d);
    }

    /** Una versión estrictamente mayor que {@code a} con todos sus componentes en {@code 0..50} (C-10, F-01). */
    private static Arbitrary<Version> mayorQue(Version a) {
        return Arbitraries.oneOf(
                mayorQueAcotado(a.parche()).map(p -> new Version(a.mayor(), a.menor(), p)),
                Combinators.combine(mayorQueAcotado(a.menor()), componente())
                        .as((m, p) -> new Version(a.mayor(), m, p)),
                Combinators.combine(mayorQueAcotado(a.mayor()), componente(), componente())
                        .as(Version::new));
    }

    /** Las cuatro formas de R-V2. El mínimo del intervalo cerrado va en {@code 0..49} para que siempre haya un mayor. */
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
        Arbitrary<Version> minimosCerrados = Arbitraries.integers().between(0, 49).flatMap(x ->
                Combinators.combine(
                                Arbitraries.integers().between(0, 49),
                                Arbitraries.integers().between(0, 49))
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
        return Arbitraries.frequencyOf(
                net.jqwik.api.Tuple.of(3, ambiguo), net.jqwik.api.Tuple.of(3, comun), net.jqwik.api.Tuple.of(2, mezcla));
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
