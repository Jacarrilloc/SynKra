package io.synkra.core;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * P3 (H-21): el core no conoce países. Recorre {@code core/src/main} y falla si algún nombre de archivo
 * o línea de Java, .properties, .yaml o .yml nombra un país.
 *
 * <p>La línea {@code PATRON} es la única fuente de la expresión: {@code scripts/control-core-sin-paises.sh}
 * la extrae de este archivo y la pasa a {@code grep -rniE}. Se mantiene en sintaxis común a Java y a ERE.
 */
class CoreSinPaisesTest {

    // PATRON-INICIO (no editar este marcador: lo lee el script)
    static final String PATRON = "colombia|per[uú]|[\"']co[\"']|[\"']pe[\"']|pa[ií]s *[:=] *(co|pe)([^a-z]|$)";
    // PATRON-FIN

    private static final Pattern PAIS = Pattern.compile(PATRON, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final List<String> EXTENSIONES = List.of(".java", ".properties", ".yaml", ".yml");

    private static Path raizMain() {
        // Gradle ejecuta las pruebas con el directorio del proyecto (core) como directorio de trabajo.
        Path p = Path.of("src/main");
        return Files.isDirectory(p) ? p : Path.of("core/src/main");
    }

    static List<String> hallazgos(Path raiz) throws IOException {
        List<String> res = new ArrayList<>();
        try (Stream<Path> s = Files.walk(raiz)) {
            for (Path f : s.filter(x -> !x.equals(raiz)).sorted().toList()) {
                String rel = raiz.relativize(f).toString();
                if (PAIS.matcher(rel).find()) {
                    res.add(f + ": el nombre del archivo nombra un país");
                }
                String nombre = f.getFileName().toString().toLowerCase();
                if (Files.isRegularFile(f) && EXTENSIONES.stream().anyMatch(nombre::endsWith)) {
                    List<String> lineas = Files.readAllLines(f, StandardCharsets.UTF_8);
                    for (int i = 0; i < lineas.size(); i++) {
                        if (PAIS.matcher(lineas.get(i)).find()) {
                            res.add(f + ":" + (i + 1) + ": " + lineas.get(i).strip());
                        }
                    }
                }
            }
        }
        return res;
    }

    @Test
    void elCoreNoNombraPaises() throws IOException {
        Path raiz = raizMain();
        assertThat(raiz).isDirectory();
        assertThat(hallazgos(raiz))
                .as("P3: el core no puede nombrar países (patrón: %s)", PATRON)
                .isEmpty();
    }
}
