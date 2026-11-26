package io.synkra.verificador;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
    private static final Pattern SEPARADOR = Pattern.compile("^\\|[\\s:|-]+$");

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
        return nombresDeLasLineas(Files.readAllLines(archivo), archivo.toString());
    }

    /**
     * Lee la tabla de la sección «Códigos de motivo». La primera línea de la tabla es el encabezado y la
     * segunda el separador; se saltan por posición. Toda fila de datos debe empezar por un único nombre de
     * código; si no, falla citando la línea.
     */
    static List<String> nombresDeLasLineas(List<String> lineas, String origen) {
        int i = lineas.indexOf("## Códigos de motivo");
        if (i < 0) {
            fail("No se encontró la sección '## Códigos de motivo' en " + origen);
        }
        List<String> nombres = new ArrayList<>();
        int posicion = 0; // 0 = encabezado, 1 = separador, 2+ = datos
        boolean enTabla = false;
        for (int n = i + 1; n < lineas.size(); n++) {
            String linea = lineas.get(n);
            if (linea.startsWith("## ") && !enTabla) {
                break;
            }
            if (linea.startsWith("|")) {
                enTabla = true;
                posicion++;
                if (posicion <= 2) {
                    if (posicion == 2 && !SEPARADOR.matcher(linea).matches()) {
                        fail("Se esperaba el separador '|---|' en la línea " + (n + 1) + ": " + linea);
                    }
                    continue;
                }
                Matcher m = FILA.matcher(linea);
                if (!m.matches()) {
                    fail("Fila de la tabla que no empieza por un único nombre de código, línea "
                            + (n + 1) + ": " + linea);
                }
                nombres.add(m.group(1));
            } else if (enTabla) {
                break;
            }
        }
        if (nombres.isEmpty()) {
            fail("La sección '## Códigos de motivo' en " + origen + " no trae una tabla con filas de datos");
        }
        return nombres;
    }

    private static List<String> lineas(String... l) {
        List<String> r = new ArrayList<>(List.of("# Doc", "", "## Códigos de motivo", "",
                "| Código | Significado |", "|---|---|", "| `PLUGIN_AUSENTE` | a |"));
        r.addAll(Arrays.asList(l));
        return r;
    }

    @Test
    void lectorAceptaFilasBienFormadas() {
        assertEquals(List.of("PLUGIN_AUSENTE", "B_C", "D"),
                nombresDeLasLineas(lineas("|  B_C  | x |", "| `D` | y |", "", "| otra | tabla |"), "mem"));
    }

    @Test
    void lectorFallaSiLaFilaNoEsUnNombre() {
        for (String fila : List.of("| X-INVENTADO | x |", "| `X_INVENTADO` (nuevo) | x |",
                "| `PLUGIN_AUSENTE`, `X_INVENTADO` | x |")) {
            AssertionError e = assertThrows(AssertionError.class,
                    () -> nombresDeLasLineas(lineas(fila), "mem"), fila);
            assertTrue(e.getMessage().contains("línea 8") && e.getMessage().contains(fila),
                    "el mensaje debe citar la línea: " + e.getMessage());
        }
    }

    @Test
    void lectorFallaSiLaSeccionNoTrajoTabla() {
        List<String> sinTabla = List.of("## Códigos de motivo", "texto", "## Otra", "| X | y |", "|---|---|", "| Z | w |");
        AssertionError e = assertThrows(AssertionError.class, () -> nombresDeLasLineas(sinTabla, "mem"));
        assertTrue(e.getMessage().contains("no trae una tabla"), e.getMessage());
    }

    @Test
    void lectorFallaSiFaltaElSeparador() {
        List<String> l = List.of("## Códigos de motivo", "| Código | S |", "| A | b |");
        AssertionError e = assertThrows(AssertionError.class, () -> nombresDeLasLineas(l, "mem"));
        assertTrue(e.getMessage().contains("separador"), e.getMessage());
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
