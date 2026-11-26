package io.synkra.verificador;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/** Compara {@link CodigoMotivo} con la tabla «Códigos de motivo» del diseño, leída del archivo. */
class CodigoMotivoTest {

    private static final String RUTA =
            "aidlc-docs/construction/U01-verificador/functional-design/domain-entities.md";
    private static final Pattern FILA = Pattern.compile("^\\|\\s*`?([A-Za-z0-9_]+)`?\\s*\\|.*");

    private static Path raiz() {
        Path p = Path.of("").toAbsolutePath();
        while (p != null && !Files.exists(p.resolve("settings.gradle.kts"))) {
            p = p.getParent();
        }
        if (p == null) {
            fail("No se encontró settings.gradle.kts subiendo desde " + Path.of("").toAbsolutePath());
        }
        return p;
    }

    private static List<String> nombresDeLaTabla() throws IOException {
        Path archivo = raiz().resolve(RUTA);
        if (!Files.exists(archivo)) {
            fail("No existe el archivo de diseño: " + archivo);
        }
        List<String> lineas = Files.readAllLines(archivo);
        int i = lineas.indexOf("## Códigos de motivo");
        if (i < 0) {
            fail("No se encontró la sección '## Códigos de motivo' en " + archivo);
        }
        List<String> nombres = new ArrayList<>();
        boolean enTabla = false;
        for (String linea : lineas.subList(i + 1, lineas.size())) {
            if (linea.startsWith("|")) {
                enTabla = true;
                Matcher m = FILA.matcher(linea);
                if (m.matches() && !m.group(1).equals("Código") && !linea.matches("^\\|\\s*-.*")) {
                    nombres.add(m.group(1));
                }
            } else if (enTabla) {
                break;
            }
        }
        if (nombres.isEmpty()) {
            fail("No se encontró la tabla de códigos tras '## Códigos de motivo' en " + archivo);
        }
        return nombres;
    }

    @Test
    void laTablaTieneDiezFilas() throws IOException {
        assertEquals(10, nombresDeLaTabla().size(), "filas de la tabla: " + nombresDeLaTabla());
    }

    @Test
    void elEnumCoincideConLaTabla() throws IOException {
        Set<String> tabla = new TreeSet<>(nombresDeLaTabla());
        Set<String> enumerado = new TreeSet<>();
        for (CodigoMotivo c : CodigoMotivo.values()) {
            enumerado.add(c.name());
        }
        Set<String> sobran = new TreeSet<>(enumerado);
        sobran.removeAll(tabla);
        Set<String> faltan = new TreeSet<>(tabla);
        faltan.removeAll(enumerado);
        assertTrue(sobran.isEmpty() && faltan.isEmpty(),
                "CodigoMotivo difiere de la tabla. Sobran en el enum: " + sobran
                        + "; faltan en el enum (están en la tabla): " + faltan);
    }

    @Test
    void sinNombresRepetidos() throws IOException {
        List<String> tabla = nombresDeLaTabla();
        assertEquals(tabla.size(), new HashSet<>(tabla).size(), "nombres repetidos en la tabla: " + tabla);
        List<String> enumerado = Arrays.stream(CodigoMotivo.values()).map(Enum::name).toList();
        assertEquals(enumerado.size(), new HashSet<>(enumerado).size(), "nombres repetidos en el enum");
    }
}
