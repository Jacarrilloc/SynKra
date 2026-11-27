package io.synkra.verificador;

import static org.junit.jupiter.api.Assertions.*;

import io.synkra.verificador.SuiteCombinaciones.Caso;
import io.synkra.verificador.SuiteCombinaciones.Entrada;
import io.synkra.verificador.SuiteCombinaciones.Esperado;
import io.synkra.verificador.SuiteCombinaciones.Grupo;
import io.synkra.verificador.SuiteCombinaciones.MotivoEsperado;
import io.synkra.verificador.SuiteCombinaciones.Origen;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** Corre la suite S1 a S12 (PRD §11.1) contra {@link Verificador} y {@link LectorDescriptor}. */
class SuiteCombinacionesTest {

    @TempDir
    Path tmp;

    /** Los casos que U01 puede ejecutar: todos menos los que remiten a otra unidad. */
    static Stream<Arguments> ejecutables() {
        return SuiteCombinaciones.casos().stream()
                .filter(c -> !(c.entrada() instanceof Entrada.FueraDeU01))
                .map(c -> Arguments.of(c.id(), c));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("ejecutables")
    void casoEjecutable(String id, Caso caso) throws IOException {
        switch (caso.entrada()) {
            case Entrada.PorSeleccion p -> porSeleccion(caso, p.seleccion());
            case Entrada.PorDescriptor p -> porDescriptor(caso, p.yaml());
            case Entrada.FueraDeU01 f -> fail(id + " no es ejecutable en U01");
        }
    }

    private static void porSeleccion(Caso caso, Seleccion s) {
        var c = SuiteCombinaciones.catalogo();
        var base = Verificador.verificar(s, c);
        var v = Verificador.alternativas(s, base, c);
        var e = caso.esperado();
        assertEquals(e.global().orElseThrow(), v.global(), caso.id() + ": global");
        assertEquals(e.global().orElseThrow(), base.global(), caso.id() + ": global sin alternativas");
        var reales = new ArrayList<Motivo>();
        v.porPlugin().forEach(p -> reales.addAll(p.motivos()));
        reales.addAll(v.motivosGlobales());
        comparaMotivos(caso.id(), e.motivos(), reales);
        var alternativas = v.porPlugin().stream().flatMap(p -> p.alternativas().stream()).toList();
        assertEquals(e.alternativas(), alternativas, caso.id() + ": alternativas exactas");
        // Sin alternativas el veredicto trae los mismos códigos: alternativas solo añade datos.
        assertEquals(
                reales.stream().map(Motivo::codigo).toList(),
                Stream.concat(
                                base.porPlugin().stream().flatMap(p -> p.motivos().stream()),
                                base.motivosGlobales().stream())
                        .map(Motivo::codigo)
                        .toList());
    }

    private void porDescriptor(Caso caso, String yaml) throws IOException {
        var e = caso.esperado();
        assertEquals(Origen.LECTOR, e.origen(), caso.id() + ": rechaza el lector");
        assertTrue(e.global().isEmpty(), caso.id() + ": el lector rechaza, no hay Global");
        var jar = tmp.resolve(caso.id() + ".jar");
        try (OutputStream o = Files.newOutputStream(jar);
                var z = new ZipOutputStream(o)) {
            z.putNextEntry(new ZipEntry("synkra-plugin.yaml"));
            z.write(yaml.getBytes(StandardCharsets.UTF_8));
            z.closeEntry();
        }
        for (var entrada : List.of("desdeYaml", "leer(jar)")) {
            var r = entrada.equals("desdeYaml") ? LectorDescriptor.desdeYaml(yaml) : LectorDescriptor.leer(jar);
            List<Motivo> motivos;
            if (r instanceof Resultado.Fallo<Descriptor> f) {
                motivos = f.motivos();
            } else {
                motivos = fail(caso.id() + " " + entrada + ": se esperaba un rechazo");
            }
            comparaMotivos(caso.id() + " " + entrada, e.motivos(), motivos);
        }
    }

    private static void comparaMotivos(String donde, List<MotivoEsperado> esperados, List<Motivo> reales) {
        assertEquals(
                esperados.stream().map(m -> List.of(m.codigo(), m.plugin(), m.campo())).toList(),
                reales.stream().map(m -> List.of(m.codigo(), m.plugin(), m.campo())).toList(),
                donde + ": lista exacta de (código, plugin, campo)");
        for (int i = 0; i < esperados.size(); i++) {
            var fragmento = esperados.get(i).enMensaje();
            assertTrue(
                    reales.get(i).mensaje().contains(fragmento),
                    donde + ": el mensaje '" + reales.get(i).mensaje() + "' debe nombrar '" + fragmento + "'");
        }
    }

    // ---------------------------------------------------------------- forma de la suite

    @Test
    void hayDoceCasosEnOrdenSinRepetidos() {
        var ids = SuiteCombinaciones.casos().stream().map(Caso::id).toList();
        assertEquals(IntStream.rangeClosed(1, 12).mapToObj(i -> "S" + i).toList(), ids);
    }

    @Test
    void aparecenLosSieteGruposDelPrdMasRepetido() {
        var grupos = EnumSet.noneOf(Grupo.class);
        SuiteCombinaciones.casos().forEach(c -> grupos.add(c.grupo()));
        var delPrd = EnumSet.complementOf(EnumSet.of(Grupo.REPETIDO));
        assertEquals(7, delPrd.size());
        assertTrue(grupos.containsAll(delPrd), "faltan grupos del PRD: " + delPrd);
        assertEquals(EnumSet.allOf(Grupo.class), grupos);
        assertEquals(
                List.of("S10"),
                SuiteCombinaciones.casos().stream()
                        .filter(c -> c.grupo() == Grupo.REPETIDO)
                        .map(Caso::id)
                        .toList());
    }

    @Test
    void s11YS12DeclaranQuienLosCubre() {
        var casos = SuiteCombinaciones.casos();
        var s11 = assertInstanceOf(Entrada.FueraDeU01.class, casos.get(10).entrada());
        var s12 = assertInstanceOf(Entrada.FueraDeU01.class, casos.get(11).entrada());
        assertAll(
                () -> assertTrue(s11.remiteA().contains("U04"), s11.remiteA()),
                () -> assertTrue(s11.remiteA().contains("U05"), s11.remiteA()),
                () -> assertTrue(s12.remiteA().contains("U02"), s12.remiteA()),
                () -> assertEquals(Grupo.INTEGRIDAD, casos.get(10).grupo()),
                () -> assertEquals(Grupo.PLUGIN_NO_ACTIVADO, casos.get(11).grupo()));
        // Solo S11 y S12 son FueraDeU01: los otros diez son los ejecutables.
        assertAll(
                () -> assertEquals(Origen.FUERA_DE_U01, casos.get(10).esperado().origen()),
                () -> assertEquals(Origen.FUERA_DE_U01, casos.get(11).esperado().origen()),
                () -> assertTrue(s11.remiteA().contains("pendiente"), s11.remiteA()),
                () -> assertTrue(s12.remiteA().contains("pendiente"), s12.remiteA()));
        assertEquals(
                10,
                casos.stream().filter(c -> !(c.entrada() instanceof Entrada.FueraDeU01)).count());
        assertEquals(
                List.of("S8", "S9"),
                casos.stream().filter(c -> c.esperado().rechazaElLector()).map(Caso::id).toList());
    }

    @Test
    void casosNoSePuedeModificar() {
        var casos = SuiteCombinaciones.casos();
        assertThrows(UnsupportedOperationException.class, () -> casos.remove(0));
        assertThrows(UnsupportedOperationException.class, () -> casos.add(casos.get(0)));
        assertThrows(UnsupportedOperationException.class, casos::clear);
    }

    @Test
    void catalogoNuevoEnCadaLlamadaConElMismoContenido() {
        var a = SuiteCombinaciones.catalogo();
        var b = SuiteCombinaciones.catalogo();
        assertNotSame(a, b);
        for (var core : List.of("synkra-core", "otro-core", "no-existe")) {
            assertEquals(a.versionesCore(core), b.versionesCore(core), core);
        }
        assertEquals(
                List.of(new Version(2, 1, 0), new Version(3, 0, 0)),
                a.versionesCore("synkra-core").stream().sorted().toList());
        for (var plugin : List.of("kyc-co", "kyc-pe", "kyc-mx")) {
            assertEquals(a.versionesPlugin("synkra-core", plugin), b.versionesPlugin("synkra-core", plugin), plugin);
        }
        assertEquals(
                List.of(new Version(1, 3, 0), new Version(2, 0, 0)),
                a.versionesPlugin("synkra-core", "kyc-pe").stream().sorted().toList());
        assertEquals(a.versionesPlugin("otro-core", "kyc-xx"), b.versionesPlugin("otro-core", "kyc-xx"));
        assertEquals(List.of(new Version(1, 0, 0)), a.versionesPlugin("otro-core", "kyc-xx"));
        for (var core : List.of("synkra-core", "otro-core")) {
            for (var plugin : List.of("kyc-co", "kyc-pe", "kyc-xx", "kyc-mx")) {
                for (var version : List.of(new Version(1, 0, 0), new Version(1, 3, 0), new Version(2, 0, 0))) {
                    assertEquals(
                            a.descriptor(core, plugin, version),
                            b.descriptor(core, plugin, version),
                            core + "/" + plugin + " " + version);
                }
            }
        }
        assertEquals("3.x", a.descriptor("synkra-core", "kyc-pe", new Version(2, 0, 0)).orElseThrow().coreCompatible().texto());
        assertTrue(a.versionesPlugin("synkra-core", "kyc-mx").isEmpty());
        assertTrue(a.versionesPlugin("synkra-core", "kyc-xx").isEmpty());
    }

    @Test
    void esperadoYEntradaIncoherentesSeRechazan() {
        var motivo = new MotivoEsperado(CodigoMotivo.PLUGIN_AUSENTE, java.util.Optional.of("kyc-mx"), java.util.Optional.empty(), "kyc-mx");
        var alternativa = new Alternativa(Alternativa.TipoAlternativa.CORE, "synkra-core", new Version(3, 0, 0));
        var vacio = java.util.Optional.<Veredicto.Global>empty();
        var aprobada = java.util.Optional.of(Veredicto.Global.APROBADA);
        assertAll(
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new Esperado(Origen.FUERA_DE_U01, vacio, List.of(motivo), List.of())),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new Esperado(Origen.FUERA_DE_U01, vacio, List.of(), List.of(alternativa))),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new Esperado(Origen.LECTOR, vacio, List.of(motivo), List.of(alternativa))),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new Esperado(Origen.LECTOR, vacio, List.of(), List.of())),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new Esperado(Origen.VEREDICTO, aprobada, List.of(motivo), List.of())),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new Esperado(Origen.VEREDICTO, java.util.Optional.of(Veredicto.Global.RECHAZADA), List.of(), List.of())),
                () -> assertThrows(NullPointerException.class, () -> new Caso(
                        null, Grupo.INTEGRIDAD, "d", new Entrada.FueraDeU01("U04"),
                        new Esperado(Origen.FUERA_DE_U01, vacio, List.of(), List.of()))),
                () -> assertThrows(NullPointerException.class, () -> new Caso(
                        "X3", null, "d", new Entrada.FueraDeU01("U04"),
                        new Esperado(Origen.FUERA_DE_U01, vacio, List.of(), List.of()))),
                () -> assertThrows(NullPointerException.class,
                        () -> new SuiteCombinaciones.CoreDelCatalogo(null, List.of(), List.of())),
                () -> assertThrows(IllegalArgumentException.class, () -> new SuiteCombinaciones.CoreDelCatalogo(
                        "synkra-core", List.of(), List.of(SuiteCombinaciones.contenido().get(1).plugins().get(0)))),
                () -> assertThrows(IllegalArgumentException.class, () -> new Caso(
                        "X1", Grupo.INTEGRIDAD, "d", new Entrada.FueraDeU01("U04"),
                        new Esperado(Origen.VEREDICTO, aprobada, List.of(), List.of()))),
                () -> assertThrows(IllegalArgumentException.class, () -> new Caso(
                        "X2", Grupo.DESCRIPTOR_INCOMPLETO, "d", new Entrada.PorDescriptor("nombre: x\n"),
                        new Esperado(Origen.FUERA_DE_U01, vacio, List.of(), List.of()))));
    }

    @Test
    void elCatalogoNoTieneMasQueElContenido() {
        var catalogo = SuiteCombinaciones.catalogo();
        var contenido = SuiteCombinaciones.contenido();
        var cores = new java.util.ArrayList<>(contenido.stream().map(SuiteCombinaciones.CoreDelCatalogo::core).toList());
        cores.add("core-que-no-existe");
        var nombres = contenido.stream()
                .flatMap(c -> c.plugins().stream())
                .map(Descriptor::nombre)
                .distinct()
                .toList();
        for (var core : cores) {
            var del = contenido.stream().filter(c -> c.core().equals(core)).findFirst();
            assertEquals(
                    del.map(SuiteCombinaciones.CoreDelCatalogo::versiones).orElse(List.of()),
                    catalogo.versionesCore(core),
                    "versiones de " + core);
            for (var nombre : nombres) {
                var esperadas = del.stream()
                        .flatMap(c -> c.plugins().stream())
                        .filter(d -> d.nombre().equals(nombre))
                        .toList();
                var reales = catalogo.versionesPlugin(core, nombre).stream()
                        .map(v -> catalogo.descriptor(core, nombre, v).orElseThrow())
                        .toList();
                assertEquals(
                        java.util.Set.copyOf(esperadas), java.util.Set.copyOf(reales), "plugins " + nombre + " de " + core);
                assertEquals(esperadas.size(), reales.size(), "sin repetidos: " + nombre + " de " + core);
            }
        }
    }
}
