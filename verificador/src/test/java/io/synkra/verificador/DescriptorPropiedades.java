package io.synkra.verificador;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.Tuple;
import org.junit.jupiter.api.Test;

/** Propiedades del descriptor (PBT-02, PBT-07, PBT-08). La semilla la elige jqwik en cada ejecución y queda en el reporte. */
class DescriptorPropiedades {

    @Provide
    Arbitrary<Descriptor> descriptores() {
        return Generadores.descriptores();
    }

    /** Textos cualquiera, y un YAML válido estropeado: cortado, con una línea duplicada o con símbolos insertados. */
    @Provide
    Arbitrary<String> entradas() {
        var simbolos = Arbitraries.of("!!", "!!python/object:x ", "&a ", "*a", "{", "[", "}", "]", ": ", "- ", "\t", "\0");
        Arbitrary<String> cortado = Combinators.combine(Generadores.descriptores(), Arbitraries.integers().between(0, 400))
                .as((d, n) -> {
                    var y = LectorDescriptor.aYaml(d);
                    return y.substring(0, Math.min(n, y.length()));
                });
        Arbitrary<String> duplicado = Combinators.combine(Generadores.descriptores(), Arbitraries.integers().between(0, 12))
                .as((d, n) -> {
                    var lineas = LectorDescriptor.aYaml(d).split("\n", -1);
                    var i = n % lineas.length;
                    var salida = new java.util.ArrayList<>(List.of(lineas));
                    salida.add(i, lineas[i]);
                    return String.join("\n", salida);
                });
        Arbitrary<String> insertado = Combinators.combine(
                        Generadores.descriptores(), Arbitraries.integers().between(0, 400), simbolos)
                .as((d, n, s) -> {
                    var y = LectorDescriptor.aYaml(d);
                    var i = Math.min(n, y.length());
                    return y.substring(0, i) + s + y.substring(i);
                });
        return Arbitraries.frequencyOf(
                Tuple.of(3, Arbitraries.strings().ofMaxLength(200)),
                Tuple.of(2, cortado),
                Tuple.of(2, duplicado),
                Tuple.of(2, insertado),
                Tuple.of(5, hostiles()));
    }

    private static int max() {
        return LectorDescriptor.MAX_PROFUNDIDAD;
    }

    /** La clase hostil que protege el lector: anidación, alias, varios documentos, directivas, claves complejas, {@code <<} y tamaños en {@code MAX_BYTES}. */
    private static Arbitrary<String> hostiles() {
        // Alrededor del tope (28..36) y muy por encima (hasta 20000 en flujo, 250 en bloque para no pasar de MAX_BYTES).
        Arbitrary<Integer> profFlujo = Arbitraries.oneOf(
                Arbitraries.integers().between(max() - 4, max() + 4), Arbitraries.integers().between(max() + 5, 20000));
        Arbitrary<Integer> profBloque = Arbitraries.oneOf(
                Arbitraries.integers().between(max() - 4, max() + 4), Arbitraries.integers().between(max() + 5, 250));
        Arbitrary<String> flujoSecuencia = profFlujo.map(n -> "a: " + "[".repeat(n) + "]".repeat(n) + "\n");
        Arbitrary<String> flujoSinCerrar = profFlujo.map(n -> "a: " + "[".repeat(n));
        Arbitrary<String> flujoMapa = profFlujo.map(n -> "a: " + "{k: ".repeat(n) + "v" + "}".repeat(n) + "\n");
        Arbitrary<String> bloqueMapa = profBloque.map(n -> {
            var sb = new StringBuilder();
            for (var i = 0; i < n; i++) {
                sb.append("  ".repeat(i)).append("k:\n");
            }
            return sb.append("  ".repeat(n)).append("v: 1\n").toString();
        });
        Arbitrary<String> bloqueSecuencia = profBloque.map(n -> {
            var sb = new StringBuilder("etiquetas:\n");
            for (var i = 0; i < n; i++) {
                sb.append("  ".repeat(i + 1)).append("-\n");
            }
            return sb.append("  ".repeat(n + 1)).append("- x\n").toString();
        });
        Arbitrary<String> bomba = Arbitraries.integers().between(1, 14).map(n -> {
            var sb = new StringBuilder("l0: &a0 [x, x, x, x, x, x, x, x, x]\n");
            for (var i = 1; i <= n; i++) {
                var p = "*a" + (i - 1);
                sb.append("l").append(i).append(": &a").append(i).append(" [")
                        .append(String.join(", ", java.util.Collections.nCopies(9, p))).append("]\n");
            }
            return sb.toString();
        });
        Arbitrary<String> bombaAnidada = Arbitraries.integers().between(1, 14).map(n -> {
            var sb = new StringBuilder("a0: &a0 [x]\n");
            for (var i = 1; i <= n; i++) {
                sb.append("a").append(i).append(": &a").append(i).append(" [*a").append(i - 1).append(", *a")
                        .append(i - 1).append("]\n");
            }
            return sb.append("etiquetas: *a").append(n).append('\n').toString();
        });
        var base = Generadores.descriptores().map(LectorDescriptor::aYaml);
        Arbitrary<String> documentos = Combinators.combine(base, base, Arbitraries.of("---\n", "\n---\n", "\n...\n---\n"))
                .as((a, b, sep) -> a + sep + b);
        Arbitrary<String> directivas = Combinators.combine(
                        base, Arbitraries.of("%YAML 1.2\n---\n", "%TAG ! tag:x,2000:\n---\n", "%YAML 9.9\n---\n", "%\n"))
                .as((b, dir) -> dir + b);
        Arbitrary<String> complejas = Combinators.combine(
                        base, Arbitraries.of("? [a, b]\n: c\n", "? a\n", "? {x: y}\n: z\n", "?\n  - a\n  - b\n:\n  - c\n"))
                .as((b, clave) -> clave + b);
        Arbitrary<String> mezcla = Combinators.combine(
                        base, Arbitraries.of("b: &b {x: 1}\nm:\n  <<: *b\n", "<<: {x: 1}\n", "<<: [*a]\n",
                                "etiquetas:\n  <<: {pais: pe}\n", "etiquetas: {<<: x}\n"))
                .as((b, m) -> m + b);
        // Tamaños en MAX_BYTES y a su alrededor: se rellena con un comentario hasta el largo pedido.
        Arbitrary<String> tamanos = Combinators.combine(base, Arbitraries.integers().between(-60, 60))
                .as((b, delta) -> {
                    var objetivo = LectorDescriptor.MAX_BYTES + delta;
                    var sb = new StringBuilder(b);
                    sb.append("#");
                    while (sb.length() < objetivo) {
                        sb.append('x');
                    }
                    return sb.toString();
                });
        Arbitrary<String> enormes = Arbitraries.integers().between(1, 4).map(k -> "a: " + "x".repeat(LectorDescriptor.MAX_BYTES * k));
        return Arbitraries.oneOf(flujoSecuencia, flujoSinCerrar, flujoMapa, bloqueMapa, bloqueSecuencia, bomba, bombaAnidada,
                documentos, directivas, complejas, mezcla, tamanos, enormes);
    }

    /** PBT-02: todo descriptor válido vuelve igual de {@code aYaml} a {@code desdeYaml}. */
    @Property(tries = 1000)
    void idaYVuelta(@ForAll("descriptores") Descriptor d) {
        var yaml = LectorDescriptor.aYaml(d);
        switch (LectorDescriptor.desdeYaml(yaml)) {
            case Resultado.Exito<Descriptor> e -> {
                assertEquals(d, e.valor(), "YAML:\n" + yaml);
                assertEquals(d.coreCompatible().texto(), e.valor().coreCompatible().texto());
            }
            case Resultado.Fallo<Descriptor> f -> throw new AssertionError("Debía leerse. Motivos: " + f + "\nYAML:\n" + yaml);
        }
    }

    /** Entrada hostil: {@code desdeYaml} nunca lanza y todo fallo trae un mensaje. */
    @Property(tries = 1000)
    void desdeYamlNuncaLanza(@ForAll("entradas") String texto) {
        Resultado<Descriptor> r = LectorDescriptor.desdeYaml(texto);
        if (r instanceof Resultado.Fallo<Descriptor> f) {
            assertFalse(f.motivos().isEmpty());
            for (var m : f.motivos()) {
                assertTrue(m.mensaje() != null && !m.mensaje().isBlank(), "Mensaje vacío en " + m);
            }
        }
    }

    /**
     * CA-3 (C-15): el generador de {@code idaYVuelta} produce los textos ambiguos y las formas límite.
     *
     * <p>Probabilidad de fallo: el caso más raro son los textos de {@code bool} y {@code float} (2 grafías de las
     * {@code AMBIGUOS.size()} = 62). Un texto es ambiguo exacto con p = 6/10; la descripción existe con p = 1/2.
     * Por descriptor, p(descripción de bool) = 0,5 * 0,6 * 2/62 = 0,0097 (el valor de etiqueta es más probable).
     * Con 5000 descriptores: (1 - 0,0097)^5000 = e^-48,7 = 6e-22, y por 16 comprobaciones sigue en ~1e-20,
     * muy por debajo de 1 en un millón. Las claves 007/true/null salen con p >= 1/21 por clave. Sin semilla fija.
     */
    @Test
    void generadorCubreTextosAmbiguos() {
        List<Descriptor> muestra = Generadores.descriptores().sampleStream().limit(5000).collect(Collectors.toList());
        var descripciones = muestra.stream().flatMap(d -> d.descripcion().stream()).toList();
        var valores = muestra.stream().flatMap(d -> d.etiquetas().values().stream()).toList();
        var claves = muestra.stream().flatMap(d -> d.etiquetas().keySet().stream()).toList();

        for (var donde : List.of(descripciones, valores)) {
            hay(donde, "nulo", s -> s.equals("~") || s.equals("null"));
            hay(donde, "bool", s -> s.equals("True") || s.equals("false"));
            hay(donde, "entero", s -> s.equals("007") || s.equals("0x1F") || s.equals("0o17"));
            hay(donde, "float", s -> s.equals(".NaN") || s.equals("-.Inf"));
            hay(donde, "con ': '", s -> s.contains(": "));
            hay(donde, "con '# '", s -> s.contains("# "));
            hay(donde, "multilínea", s -> s.contains("\n"));
            hay(donde, "fuera de ASCII", s -> s.chars().anyMatch(c -> c > 127));
        }
        for (var k : List.of("007", "true", "null")) {
            hay(claves, "clave " + k, s -> s.equals(k));
        }
        assertTrue(muestra.stream().anyMatch(d -> d.descripcion().isEmpty()), "sin descripcion ausente");
        assertTrue(muestra.stream().anyMatch(d -> d.etiquetas().isEmpty()), "sin 0 etiquetas");
        assertTrue(muestra.stream().anyMatch(d -> d.etiquetas().size() == 5), "sin 5 etiquetas");
    }

    private static void hay(List<String> textos, String que, Predicate<String> p) {
        assertTrue(textos.stream().anyMatch(p), "El generador no produjo: " + que);
    }
}
