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
    private static final Pattern FILA = Pattern.compile("^\\|\\s*(?:`([A-Za-z0-9_]+)`|([A-Za-z0-9_]+))\\s*\\|.*");
    private static final Pattern ENCABEZADO = Pattern.compile("^\\|\\s*Código\\s*\\|.*");
    private static final Pattern PIPE_SIN_ESCAPAR = Pattern.compile("(?<!\\\\)\\|");
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

    private static int columnas(String linea) {
        String t = linea.strip();
        t = t.substring(1);
        if (t.endsWith("|") && !t.endsWith("\\|")) {
            t = t.substring(0, t.length() - 1);
        }
        return PIPE_SIN_ESCAPAR.split(t, -1).length;
    }

    /**
     * Lee la tabla de la sección «Códigos de motivo». La tabla empieza en la primera línea que empieza por
     * {@code |} (encabezado) y termina SOLO en una línea en blanco o en un {@code ## }. Fuera de ella, en la sección, toda línea con un
     * {@code |} sin escapar y toda apertura de bloque de código fallan. Toda línea no vacía
     * entre medias, tras el separador, es una fila de datos y debe empezar por {@code |} y un único nombre de
     * código (sin sangría); si no, falla citando la línea. Una segunda tabla en la sección también falla.
     */
    static List<String> nombresDeLasLineas(List<String> lineas, String origen) {
        int i = lineas.indexOf("## Códigos de motivo");
        if (i < 0) {
            fail("No se encontró la sección '## Códigos de motivo' en " + origen);
        }
        List<String> nombres = new ArrayList<>();
        int posicion = 0; // 0 = antes de la tabla, 1 = encabezado, 2 = separador, 3+ = datos
        boolean terminada = false;
        int columnas = 0;
        for (int n = i + 1; n < lineas.size(); n++) {
            String linea = lineas.get(n);
            if (linea.startsWith("## ")) {
                break;
            }
            String sinSangria = linea.stripLeading();
            if (sinSangria.startsWith("```") || sinSangria.startsWith("~~~")) {
                fail("La sección '## Códigos de motivo' no admite bloques de código; apertura en la línea "
                        + (n + 1) + ": " + linea);
            }
            if (terminada || posicion == 0) {
                boolean empiezaTabla = posicion == 0 && linea.startsWith("|");
                if (!empiezaTabla && PIPE_SIN_ESCAPAR.matcher(linea).find()) {
                    fail("La sección '## Códigos de motivo' trae un '|' fuera de la tabla leída (más de una tabla "
                            + "o tabla mal formada), línea " + (n + 1) + ": " + linea);
                }
                if (empiezaTabla) {
                    if (!ENCABEZADO.matcher(linea).matches()) {
                        fail("El encabezado debe empezar por '| Código |', línea " + (n + 1) + ": " + linea);
                    }
                    columnas = columnas(linea);
                    posicion = 1;
                }
                continue;
            }
            if (linea.isBlank()) {
                terminada = true;
                continue;
            }
            posicion++;
            if (posicion == 2) {
                if (!SEPARADOR.matcher(linea).matches()) {
                    fail("Se esperaba el separador '|---|' en la línea " + (n + 1) + ": " + linea);
                }
                if (columnas(linea) != columnas) {
                    fail("El separador tiene " + columnas(linea) + " columnas y el encabezado " + columnas
                            + ", línea " + (n + 1) + ": " + linea);
                }
                continue;
            }
            Matcher m = FILA.matcher(linea);
            if (!m.matches()) {
                fail("Fila de la tabla que no empieza por '|' y un único nombre de código, línea "
                        + (n + 1) + ": " + linea);
            }
            nombres.add(m.group(1) != null ? m.group(1) : m.group(2));
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
                nombresDeLasLineas(lineas("|  B_C  | x |", "| `D` | y |", "", "texto posterior"), "mem"));
    }

    @Test
    void lectorFallaSiLaFilaNoEsUnNombre() {
        for (String fila : List.of("| X-INVENTADO | x |", "| `X_INVENTADO` (nuevo) | x |",
                "| `PLUGIN_AUSENTE`, `X_INVENTADO` | x |", "`X_INVENTADO` | x | y |",
                "  | `X_INVENTADO` | x | y |", "  `X_INVENTADO` | x | y |", "texto suelto sin pipe", "> cita",
                "### otro titulo", "---", "<!-- c -->")) {
            AssertionError e = assertThrows(AssertionError.class,
                    () -> nombresDeLasLineas(lineas(fila), "mem"), fila);
            assertTrue(e.getMessage().contains("línea 8") && e.getMessage().contains(fila),
                    "el mensaje debe citar la línea: " + e.getMessage());
        }
    }

    @Test
    void lectorFallaSiLaLineaSinPipeCaeEnMedioDeLaTabla() {
        List<String> l = lineas("texto sin pipe", "| `D` | y |");
        AssertionError e = assertThrows(AssertionError.class, () -> nombresDeLasLineas(l, "mem"));
        assertTrue(e.getMessage().contains("línea 8") && e.getMessage().contains("texto sin pipe"),
                e.getMessage());
    }

    @Test
    void lectorFallaSiHayMasDeUnaTabla() {
        List<String> l = lineas("", "| Otro | Enc |", "|---|---|", "| `X_INVENTADO` | x |");
        AssertionError e = assertThrows(AssertionError.class, () -> nombresDeLasLineas(l, "mem"));
        assertTrue(e.getMessage().contains("más de una tabla") && e.getMessage().contains("línea 9"),
                e.getMessage());
    }

    private static void falla(List<String> l, String... trozos) {
        AssertionError e = assertThrows(AssertionError.class, () -> nombresDeLasLineas(l, "mem"));
        for (String z : trozos) {
            assertTrue(e.getMessage().contains(z), "falta '" + z + "' en: " + e.getMessage());
        }
    }

    private static List<String> filasReales() {
        return List.of("| `PLUGIN_AUSENTE` | a |", "| `B` | b |");
    }

    @Test
    void encabezadoSinPipe() {
        List<String> l = List.of("## Códigos de motivo", "", "Código | Cuándo | Lo detecta", "--- | --- | ---",
                "| `X_INVENTADO` | x | y |", "| | | |", "| `B` | b | c |");
        falla(l, "línea 3", "Código | Cuándo | Lo detecta");
    }

    @Test
    void segundaSinPipe() {
        List<String> l = new ArrayList<>(lineas("", "Otro | A | B", "--- | --- | ---", "`X_INVENTADO` | x | y"));
        falla(l, "línea 9", "Otro | A | B");
    }

    @Test
    void fence9() {
        List<String> l = List.of("## Códigos de motivo", "", "```", "| Código | S |", "|---|---|", "| `A` | b |",
                "```", "", "Código | S", "--- | ---", "`B` | c");
        falla(l, "línea 3", "```");
        falla(List.of("## Códigos de motivo", "~~~", "| Código | S |"), "línea 2", "~~~");
    }

    @Test
    void pipeAntesDeLaTablaYEscapado() {
        falla(List.of("## Códigos de motivo", "prosa | con pipe", "| Código | S |", "|---|---|", "| A | b |"),
                "línea 2", "prosa | con pipe");
        assertEquals(List.of("A"), nombresDeLasLineas(List.of("## Códigos de motivo", "prosa con \\| escapado",
                "| Código | S |", "|---|---|", "| A | b |", "", "tras \\| tabla"), "mem"));
    }

    @Test
    void encabezadoSeparadorYCeldaMalFormados() {
        falla(List.of("## Códigos de motivo", "| `X_INVENTADO` | S |", "|---|---|", "| A | b |"), "línea 2", "Código");
        falla(List.of("## Códigos de motivo", "| Código | A | B |", "|---|---|", "| A | b | c |"), "línea 3", "columnas");
        falla(lineas("| `B_C | x |"), "línea 8", "`B_C");
        falla(lineas("| B_C` | x |"), "línea 8", "B_C`");
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
