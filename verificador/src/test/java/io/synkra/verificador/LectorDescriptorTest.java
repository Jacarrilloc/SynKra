package io.synkra.verificador;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LectorDescriptorTest {

    @TempDir
    Path tmp;

    private static final String VALIDO = "nombre: kyc-pe\nversion: 1.2.3\ncore: banco\ncoreCompatible: 2.x\n";

    private Path jar(String archivo, String contenido) throws IOException {
        var p = Files.createTempFile(tmp, "plugin", ".jar");
        try (OutputStream o = Files.newOutputStream(p);
                var z = new ZipOutputStream(o)) {
            z.putNextEntry(new ZipEntry(archivo));
            z.write(contenido.getBytes(StandardCharsets.UTF_8));
            z.closeEntry();
        }
        return p;
    }

    private static List<Motivo> motivos(Resultado<Descriptor> r) {
        return switch (r) {
            case Resultado.Fallo<Descriptor> f -> f.motivos();
            case Resultado.Exito<Descriptor> e -> fail("Se esperaba un fallo");
        };
    }

    private static Descriptor valor(Resultado<Descriptor> r) {
        return switch (r) {
            case Resultado.Exito<Descriptor> e -> e.valor();
            case Resultado.Fallo<Descriptor> f -> fail("Se esperaba éxito: " + f.motivos());
        };
    }

    private static List<CodigoMotivo> codigos(Resultado<Descriptor> r) {
        return motivos(r).stream().map(Motivo::codigo).toList();
    }

    @Test
    void jarSinArchivo() throws IOException {
        var r = LectorDescriptor.leer(jar("otro.yaml", VALIDO));
        assertEquals(List.of(CodigoMotivo.DESCRIPTOR_SIN_ARCHIVO), codigos(r));
    }

    @Test
    void jarInexistenteNoLanza() {
        assertEquals(1, motivos(LectorDescriptor.leer(tmp.resolve("no-existe.jar"))).size());
    }

    @Test
    void leeDesdeJar() throws IOException {
        var d = valor(LectorDescriptor.leer(jar("synkra-plugin.yaml", VALIDO)));
        assertEquals("kyc-pe", d.nombre());
    }

    @Test
    void faltaCoreCompatibleS8() {
        var m = motivos(LectorDescriptor.desdeYaml("nombre: kyc\nversion: 1.0.0\ncore: banco\n"));
        assertEquals(1, m.size());
        assertEquals(CodigoMotivo.DESCRIPTOR_FALTA_CAMPO, m.get(0).codigo());
        assertEquals("coreCompatible", m.get(0).campo().orElseThrow());
    }

    @Test
    void campoMalEscritoS9DaDosMotivos() {
        var m = motivos(LectorDescriptor.desdeYaml(
                "nombre: kyc\nversion: 1.0.0\ncore: banco\ncoreCompatibel: 2.x\n"));
        assertEquals(2, m.size());
        var desconocido = m.stream()
                .filter(x -> x.codigo() == CodigoMotivo.DESCRIPTOR_CAMPO_DESCONOCIDO)
                .findFirst()
                .orElseThrow();
        assertEquals("coreCompatibel", desconocido.campo().orElseThrow());
        var falta = m.stream()
                .filter(x -> x.codigo() == CodigoMotivo.DESCRIPTOR_FALTA_CAMPO)
                .findFirst()
                .orElseThrow();
        assertEquals("coreCompatible", falta.campo().orElseThrow());
    }

    @Test
    void faltanNombreYVersion() {
        var m = motivos(LectorDescriptor.desdeYaml("core: banco\ncoreCompatible: 2.x\n"));
        assertEquals(
                List.of(CodigoMotivo.DESCRIPTOR_FALTA_CAMPO, CodigoMotivo.DESCRIPTOR_FALTA_CAMPO),
                m.stream().map(Motivo::codigo).toList());
        assertEquals(
                java.util.Set.of("nombre", "version"),
                m.stream().map(x -> x.campo().orElseThrow()).collect(java.util.stream.Collectors.toSet()));
    }

    @Test
    void versionInvalida() {
        var r = LectorDescriptor.desdeYaml("nombre: kyc\nversion: 2.1\ncore: banco\ncoreCompatible: 2.x\n");
        assertEquals(List.of(CodigoMotivo.VERSION_INVALIDA), codigos(r));
    }

    @Test
    void rangoInvalido() {
        var r = LectorDescriptor.desdeYaml("nombre: kyc\nversion: 2.1.0\ncore: banco\ncoreCompatible: ^2.1\n");
        assertEquals(List.of(CodigoMotivo.RANGO_INVALIDO), codigos(r));
    }

    @Test
    void etiquetaDeTipoJava() {
        var r = LectorDescriptor.desdeYaml(
                "nombre: kyc\nversion: 1.0.0\ncore: banco\ncoreCompatible: !!java.net.URL \"http://x\"\n");
        assertEquals(List.of(CodigoMotivo.DESCRIPTOR_INVALIDO), codigos(r));
    }

    @Test
    void etiquetaLocal() {
        var r = LectorDescriptor.desdeYaml("nombre: !algo kyc\nversion: 1.0.0\ncore: banco\ncoreCompatible: 2.x\n");
        assertEquals(List.of(CodigoMotivo.DESCRIPTOR_INVALIDO), codigos(r));
    }

    @Test
    void aliasRecursivo() {
        // Un alias recursivo solo puede estar en una secuencia o un mapa, y ningún campo admite eso: o es
        // «campo desconocido» o «debe ser un texto». Aquí, en un campo obligatorio, lo segundo.
        var m = motivos(LectorDescriptor.desdeYaml(
                "nombre: &a [*a]\nversion: 1.0.0\ncore: banco\ncoreCompatible: 2.x\n"));
        assertEquals(1, m.size());
        assertEquals(CodigoMotivo.DESCRIPTOR_INVALIDO, m.get(0).codigo());
        assertEquals("nombre", m.get(0).campo().orElseThrow());
        assertTrue(m.get(0).mensaje().contains("texto"));
    }

    @Test
    void aliasMalFormado() {
        var r = LectorDescriptor.desdeYaml("nombre: *nada\nversion: 1.0.0\ncore: banco\ncoreCompatible: 2.x\n");
        assertEquals(List.of(CodigoMotivo.DESCRIPTOR_INVALIDO), codigos(r));
    }

    @Test
    void nombreFueraDePatron() {
        var r = LectorDescriptor.desdeYaml("nombre: Kyc_PE\nversion: 1.0.0\ncore: banco\ncoreCompatible: 2.x\n");
        var m = motivos(r);
        assertEquals(1, m.size());
        assertEquals(CodigoMotivo.DESCRIPTOR_INVALIDO, m.get(0).codigo());
        assertEquals("nombre", m.get(0).campo().orElseThrow());
    }

    @Test
    void yamlIlegibleYVacioNoLanzan() {
        assertEquals(List.of(CodigoMotivo.DESCRIPTOR_INVALIDO), codigos(LectorDescriptor.desdeYaml("a: [")));
        assertEquals(List.of(CodigoMotivo.DESCRIPTOR_INVALIDO), codigos(LectorDescriptor.desdeYaml("")));
        assertEquals(List.of(CodigoMotivo.DESCRIPTOR_INVALIDO), codigos(LectorDescriptor.desdeYaml("- a\n- b\n")));
        assertEquals(List.of(CodigoMotivo.DESCRIPTOR_INVALIDO), codigos(LectorDescriptor.desdeYaml(null)));
    }

    @Test
    void demasiadoGrande() {
        var r = LectorDescriptor.desdeYaml(VALIDO + "descripcion: " + "x".repeat(200_000) + "\n");
        assertEquals(List.of(CodigoMotivo.DESCRIPTOR_INVALIDO), codigos(r));
    }

    @Test
    void validoConEtiquetasYDescripcion() {
        var d = valor(LectorDescriptor.desdeYaml(VALIDO + "etiquetas:\n  pais: pe\ndescripcion: Verifica identidad\n"));
        assertEquals("kyc-pe", d.nombre());
        assertEquals(new Version(1, 2, 3), d.version());
        assertEquals("banco", d.core());
        assertEquals("2.x", d.coreCompatible().texto());
        assertEquals(Map.of("pais", "pe"), d.etiquetas());
        assertEquals("Verifica identidad", d.descripcion().orElseThrow());
    }

    @Test
    void claveDeEtiquetaInvalida() {
        var m = motivos(LectorDescriptor.desdeYaml(VALIDO + "etiquetas:\n  Pais: pe\n"));
        assertEquals(1, m.size());
        assertEquals(CodigoMotivo.DESCRIPTOR_INVALIDO, m.get(0).codigo());
        assertEquals("etiquetas", m.get(0).campo().orElseThrow());
    }

    @Test
    void idaYVuelta() {
        var d = valor(LectorDescriptor.desdeYaml(VALIDO + "etiquetas:\n  pais: pe\ndescripcion: 'a: b'\n"));
        assertEquals(d, valor(LectorDescriptor.desdeYaml(LectorDescriptor.aYaml(d))));
    }

    private static String anidado(int n) {
        return VALIDO + "extra: " + "[".repeat(n) + "]".repeat(n) + "\n";
    }

    @Test
    void anidacionProfundaNoLanza() throws IOException {
        for (int n : new int[] {2000, 32000}) {
            assertEquals(List.of(CodigoMotivo.DESCRIPTOR_INVALIDO), codigos(LectorDescriptor.desdeYaml(anidado(n))));
            assertEquals(
                    List.of(CodigoMotivo.DESCRIPTOR_INVALIDO),
                    codigos(LectorDescriptor.leer(jar("synkra-plugin.yaml", anidado(n)))));
        }
        var mapas = VALIDO + "extra: " + "{a: ".repeat(3000) + "1" + "}".repeat(3000) + "\n";
        assertEquals(List.of(CodigoMotivo.DESCRIPTOR_INVALIDO), codigos(LectorDescriptor.desdeYaml(mapas)));
    }

    @Test
    void clavesDuplicadasEnRaizSeAcumulan() {
        var m = motivos(LectorDescriptor.desdeYaml(VALIDO + "nombre: otro\nextra: 1\n"));
        assertEquals(
                List.of(CodigoMotivo.DESCRIPTOR_CAMPO_DESCONOCIDO, CodigoMotivo.DESCRIPTOR_INVALIDO),
                m.stream().map(Motivo::codigo).sorted().toList());
        assertTrue(m.stream().anyMatch(x -> x.codigo() == CodigoMotivo.DESCRIPTOR_INVALIDO
                && x.campo().equals(Optional.of("nombre"))));
    }

    @Test
    void clavesDuplicadasEnEtiquetas() {
        var m = motivos(LectorDescriptor.desdeYaml(VALIDO + "etiquetas: {pais: pe, pais: mx}\n"));
        assertEquals(1, m.size());
        assertEquals(CodigoMotivo.DESCRIPTOR_INVALIDO, m.get(0).codigo());
        assertEquals("etiquetas", m.get(0).campo().orElseThrow());
    }

    @Test
    void descripcionNulaCuentaComoAusente() {
        var d = valor(LectorDescriptor.desdeYaml(VALIDO + "descripcion: ~\n"));
        assertEquals(Optional.empty(), d.descripcion());
        assertEquals(d, valor(LectorDescriptor.desdeYaml(VALIDO)));
        assertEquals(Optional.empty(), valor(LectorDescriptor.desdeYaml(VALIDO + "descripcion: null\n")).descripcion());
    }

    @Test
    void descripcionNoStrEsInvalida() {
        assertEquals(List.of(CodigoMotivo.DESCRIPTOR_INVALIDO), codigos(LectorDescriptor.desdeYaml(VALIDO + "descripcion: 12\n")));
        assertEquals("12", valor(LectorDescriptor.desdeYaml(VALIDO + "descripcion: '12'\n")).descripcion().orElseThrow());
    }

    @Test
    void valorDeEtiquetaNuloONoStrEsInvalido() {
        for (var y : List.of("{pais: ~}", "{pais: null}", "{pais: true}", "{pais: 5}")) {
            assertEquals(
                    List.of(CodigoMotivo.DESCRIPTOR_INVALIDO),
                    codigos(LectorDescriptor.desdeYaml(VALIDO + "etiquetas: " + y + "\n")),
                    y);
        }
        assertEquals(Map.of("pais", "5"), valor(LectorDescriptor.desdeYaml(VALIDO + "etiquetas: {pais: '5'}\n")).etiquetas());
    }

    @Test
    void nombreYCoreResueltosComoOtroTipoSonInvalidos() {
        for (var v : List.of("true", "123", "1.5")) {
            var m = motivos(LectorDescriptor.desdeYaml(
                    "nombre: " + v + "\nversion: 1.0.0\ncore: banco\ncoreCompatible: 2.x\n"));
            assertEquals(List.of(CodigoMotivo.DESCRIPTOR_INVALIDO), m.stream().map(Motivo::codigo).toList(), v);
            assertEquals("nombre", m.get(0).campo().orElseThrow());
            var c = motivos(LectorDescriptor.desdeYaml(
                    "nombre: kyc\nversion: 1.0.0\ncore: " + v + "\ncoreCompatible: 2.x\n"));
            assertEquals("core", c.get(0).campo().orElseThrow(), v);
        }
        assertEquals("123", valor(LectorDescriptor.desdeYaml(
                "nombre: '123'\nversion: 1.0.0\ncore: banco\ncoreCompatible: 2.x\n")).nombre());
        // nulo en nombre sigue siendo campo faltante
        assertEquals(
                List.of(CodigoMotivo.DESCRIPTOR_FALTA_CAMPO),
                codigos(LectorDescriptor.desdeYaml("nombre: ~\nversion: 1.0.0\ncore: banco\ncoreCompatible: 2.x\n")));
    }

    @Test
    void versionYRangoConEtiquetaExplicitaNoStrSonInvalidos() {
        assertEquals(
                List.of(CodigoMotivo.DESCRIPTOR_INVALIDO),
                codigos(LectorDescriptor.desdeYaml("nombre: kyc\nversion: !!int 1.0.0\ncore: banco\ncoreCompatible: 2.x\n")));
        assertEquals(
                List.of(CodigoMotivo.DESCRIPTOR_INVALIDO),
                codigos(LectorDescriptor.desdeYaml("nombre: kyc\nversion: 1.0.0\ncore: banco\ncoreCompatible: !!bool 2.x\n")));
        assertEquals(1, valor(LectorDescriptor.desdeYaml("nombre: kyc\nversion: !!str 1.0.0\ncore: banco\ncoreCompatible: 2.x\n")).version().mayor());
    }

    @Test
    void idaYVueltaConTextosQueYamlResuelveComoOtroTipo() {
        for (var texto : List.of("~", "True", "007", "Null", "0x1F", ".NaN")) {
            var d = new Descriptor(
                    "kyc",
                    new Version(1, 0, 0),
                    "banco",
                    valorRango("2.x"),
                    Map.of("cliente", texto),
                    Optional.of(texto));
            assertEquals(d, valor(LectorDescriptor.desdeYaml(LectorDescriptor.aYaml(d))), texto);
        }
    }

    private static RangoVersion valorRango(String t) {
        return switch (RangoVersion.parsear(t)) {
            case Resultado.Exito<RangoVersion> e -> e.valor();
            case Resultado.Fallo<RangoVersion> f -> fail();
        };
    }

    @Test
    void claveDeEtiquetaResueltaComoOtroTipoEsInvalida() {
        for (var clave : List.of("1", "true", "~")) {
            var m = motivos(LectorDescriptor.desdeYaml(VALIDO + "etiquetas: {" + clave + ": pe}\n"));
            assertEquals(1, m.size(), clave);
            assertEquals(CodigoMotivo.DESCRIPTOR_INVALIDO, m.get(0).codigo());
            assertEquals("etiquetas", m.get(0).campo().orElseThrow());
        }
        assertEquals(Map.of("1", "pe"), valor(LectorDescriptor.desdeYaml(VALIDO + "etiquetas: {'1': pe}\n")).etiquetas());
    }

    /** C-06: Version y RangoVersion fijan los nombres de campo, que son los del descriptor. */
    @Test
    void versionYCoreCompatibleInvalidosLlevanSuCampo() {
        var m = motivos(LectorDescriptor.desdeYaml("nombre: kyc-pe\nversion: 1.2\ncore: banco\ncoreCompatible: dos\n"));
        assertEquals(
                List.of(CodigoMotivo.VERSION_INVALIDA, CodigoMotivo.RANGO_INVALIDO),
                m.stream().map(Motivo::codigo).toList());
        assertEquals("version", m.get(0).campo().orElseThrow());
        assertEquals("coreCompatible", m.get(1).campo().orElseThrow());
    }
}
