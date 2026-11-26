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
        return Arbitraries.oneOf(Arbitraries.strings().ofMaxLength(200), cortado, duplicado, insertado);
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

    /** CA-3 (C-15): el generador de {@code idaYVuelta} produce los textos ambiguos y las formas límite. */
    @Test
    void generadorCubreTextosAmbiguos() {
        List<Descriptor> muestra = Generadores.descriptores().sampleStream().limit(1000).collect(Collectors.toList());
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
