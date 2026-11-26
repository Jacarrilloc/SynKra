package io.synkra.verificador;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.synkra.verificador.Alternativa.TipoAlternativa;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.Tuple;
import org.junit.jupiter.api.Test;

/**
 * Propiedades del verificador (PBT-03, PBT-07, PBT-08). La semilla la elige jqwik en cada ejecución y queda en el
 * reporte. Ningún generador usa {@code filter} ni {@code Assume}: {@code checks = tries}.
 */
class VerificadorPropiedades {

    /** Lo que hay subido, como datos: el oráculo recorre esto y nunca llama a {@code alternativas}. */
    record Datos(Map<String, List<Version>> cores, List<Descriptor> descriptores) {
        CatalogoEnMemoria catalogo() {
            return new CatalogoEnMemoria(cores, descriptores);
        }
    }

    /** Un catálogo y dos selecciones sobre él (la segunda se usa «en medio» del determinismo). */
    record Caso(Seleccion seleccion, Seleccion otra, Datos datos) {}

    private static final Comparator<Version> NUMERICO =
            Comparator.comparingInt(Version::mayor).thenComparingInt(Version::menor).thenComparingInt(Version::parche);

    private static final List<String> CORES = List.of("core-a", "core-b");
    private static final List<String> PLUGINS = List.of("kyc-pe", "kyc-co", "pagos", "firma");
    /** Con dos cifras (9, 10, 11) para que comparar por texto no se confunda con comparar por versión. */
    private static final List<Integer> COMPONENTES = List.of(0, 1, 2, 3, 4, 5, 9, 10, 11);

    // ---------------------------------------------------------------- generadores

    private static Arbitrary<Integer> comp() {
        return Arbitraries.of(COMPONENTES);
    }

    private static Arbitrary<Version> versiones() {
        return Combinators.combine(comp(), comp(), comp()).as(Version::new);
    }

    /** Las cuatro formas de R-V2 con componentes pequeños, para que los rangos acierten a menudo. */
    private static Arbitrary<RangoVersion> rangosPequenos() {
        Arbitrary<RangoVersion> mayorX = comp().map(n -> new RangoVersion(
                new Version(n, 0, 0), Optional.of(new Version(n + 1, 0, 0)), n + ".x"));
        Arbitrary<RangoVersion> menorX = Combinators.combine(comp(), comp()).as((n, m) -> new RangoVersion(
                new Version(n, m, 0), Optional.of(new Version(n, m + 1, 0)), n + "." + m + ".x"));
        Arbitrary<RangoVersion> exacta = versiones().map(v -> new RangoVersion(
                v, Optional.of(new Version(v.mayor(), v.menor(), v.parche() + 1)), v.toString()));
        Arbitrary<RangoVersion> abierto =
                versiones().map(a -> new RangoVersion(a, Optional.empty(), ">=" + a));
        Arbitrary<RangoVersion> cerrado = Combinators.combine(
                        versiones(), Arbitraries.integers().between(0, 2), Arbitraries.integers().between(1, 4), comp())
                .as((a, donde, k, otro) -> {
                    var b = switch (donde) {
                        case 0 -> new Version(a.mayor() + k, otro, otro);
                        case 1 -> new Version(a.mayor(), a.menor() + k, otro);
                        default -> new Version(a.mayor(), a.menor(), a.parche() + k);
                    };
                    return new RangoVersion(a, Optional.of(b), ">=" + a + " <" + b);
                });
        return Arbitraries.oneOf(mayorX, menorX, exacta, abierto, cerrado);
    }

    private static Arbitrary<RangoVersion> rangos() {
        return Arbitraries.frequencyOf(Tuple.of(9, rangosPequenos()), Tuple.of(1, Generadores.rangos()));
    }

    private static Arbitrary<Descriptor> descriptor(String core, String nombre, Version version) {
        return rangos().map(r -> new Descriptor(nombre, version, core, r, Map.of(), Optional.empty()));
    }

    private static Arbitrary<List<Descriptor>> pluginDeCarpeta(String core, String nombre) {
        return versiones().list().ofMinSize(1).ofMaxSize(3).uniqueElements().flatMap(vs ->
                Combinators.combine(vs.stream().map(v -> descriptor(core, nombre, v)).toList()).as(l -> l));
    }

    private static Arbitrary<List<Descriptor>> carpeta(String core) {
        return Arbitraries.of(PLUGINS).list().ofMinSize(1).ofMaxSize(4).uniqueElements().flatMap(ns ->
                Combinators.combine(ns.stream().map(n -> pluginDeCarpeta(core, n)).toList())
                        .as(ls -> ls.stream().flatMap(List::stream).toList()));
    }

    private static Arbitrary<Datos> catalogosGenerados() {
        return Arbitraries.of(CORES).list().ofMinSize(1).ofMaxSize(2).uniqueElements().flatMap(cores -> {
            Arbitrary<List<List<Version>>> versionesCore = Combinators.combine(cores.stream()
                            .map(c -> versiones().list().ofMinSize(1).ofMaxSize(4).uniqueElements())
                            .toList())
                    .as(l -> l);
            Arbitrary<List<Descriptor>> descriptores = Combinators.combine(
                            cores.stream().map(VerificadorPropiedades::carpeta).toList())
                    .as(ls -> ls.stream().flatMap(List::stream).toList());
            return Combinators.combine(versionesCore, descriptores).as((vs, ds) -> {
                var mapa = new LinkedHashMap<String, List<Version>>();
                for (int i = 0; i < cores.size(); i++) {
                    mapa.put(cores.get(i), vs.get(i));
                }
                return new Datos(mapa, ds);
            });
        });
    }

    /** El catálogo de la suite (el de AlternativasTest: S4, S5...) como uno de los posibles. */
    private static Datos catalogoDeLaSuite() {
        return new Datos(
                Map.of(
                        "synkra-core", List.of(new Version(2, 1, 0), new Version(3, 0, 0)),
                        "otro-core", List.of(new Version(1, 0, 0))),
                List.of(
                        AlternativasTest.KYC_CO,
                        AlternativasTest.KYC_PE_13,
                        AlternativasTest.KYC_PE_20,
                        AlternativasTest.KYC_XX));
    }

    private static Arbitrary<Datos> catalogos() {
        return Arbitraries.frequencyOf(Tuple.of(9, catalogosGenerados()), Tuple.of(1, Arbitraries.just(catalogoDeLaSuite())));
    }

    /** Un plugin de una selección: del catálogo (cualquier carpeta), con otro rango, ausente o con versión que no está. */
    private static Arbitrary<Descriptor> pluginDe(Datos datos, String core) {
        Arbitrary<Descriptor> delCatalogo = Arbitraries.of(datos.descriptores());
        Arbitrary<Descriptor> otroRango = Combinators.combine(delCatalogo, rangos())
                .as((d, r) -> new Descriptor(d.nombre(), d.version(), d.core(), r, Map.of(), Optional.empty()));
        Arbitrary<Descriptor> sinNombre = Combinators.combine(
                        Arbitraries.of("fantasma", "sombra"), versiones(), Arbitraries.of(core, "core-z"), rangos())
                .as((n, v, c, r) -> new Descriptor(n, v, c, r, Map.of(), Optional.empty()));
        Arbitrary<Descriptor> otraVersion = Combinators.combine(delCatalogo, Arbitraries.of(6, 7, 8))
                .as((d, x) -> new Descriptor(
                        d.nombre(), new Version(x, x, x), d.core(), d.coreCompatible(), Map.of(), Optional.empty()));
        return Arbitraries.frequencyOf(
                Tuple.of(12, delCatalogo), Tuple.of(2, otroRango), Tuple.of(1, sinNombre), Tuple.of(2, otraVersion));
    }

    private static Arbitrary<Seleccion> selecciones(Datos datos) {
        var nombresDeCores = new ArrayList<>(datos.cores().keySet());
        Arbitrary<String> core = Arbitraries.frequencyOf(
                Tuple.of(9, Arbitraries.of(nombresDeCores)), Tuple.of(1, Arbitraries.just("core-z")));
        return core.flatMap(c -> {
            var delCore = datos.cores().getOrDefault(c, List.of());
            Arbitrary<Version> versionCore = delCore.isEmpty()
                    ? versiones()
                    : Arbitraries.frequencyOf(Tuple.of(8, Arbitraries.of(delCore)), Tuple.of(2, versiones()));
            Arbitrary<List<Descriptor>> normal =
                    pluginDe(datos, c).list().ofMinSize(0).ofMaxSize(5);
            Arbitrary<List<Descriptor>> vacia = Arbitraries.just(List.<Descriptor>of());
            Arbitrary<List<Descriptor>> triple = Combinators.combine(
                            pluginDe(datos, c), pluginDe(datos, c).list().ofMinSize(0).ofMaxSize(2), Arbitraries.integers().between(0, 2))
                    .as((d, otros, donde) -> {
                        var l = new ArrayList<>(otros);
                        l.add(Math.min(donde, l.size()), d);
                        l.add(d);
                        l.add(d);
                        return l;
                    });
            Arbitrary<List<Descriptor>> plugins =
                    Arbitraries.frequencyOf(Tuple.of(8, normal), Tuple.of(1, vacia), Tuple.of(2, triple));
            return Combinators.combine(versionCore, plugins).as((vc, ps) -> new Seleccion(c, vc, ps));
        });
    }

    @Provide
    Arbitrary<Caso> casos() {
        return catalogos().flatMap(datos ->
                Combinators.combine(selecciones(datos), selecciones(datos)).as((s, o) -> new Caso(s, o, datos)));
    }

    // ---------------------------------------------------------------- oráculo (recorre los datos, sin alternativas)

    private static boolean incluye(RangoVersion r, Version v) {
        return NUMERICO.compare(r.minimo(), v) <= 0
                && (r.maximo().isEmpty() || NUMERICO.compare(v, r.maximo().get()) < 0);
    }

    /** Versiones del plugin en la carpeta del core cuyo rango incluye la versión del core pedida. */
    private static List<Version> pluginesQueSirven(Datos datos, Seleccion s, String nombre) {
        return datos.descriptores().stream()
                .filter(d -> d.core().equals(s.core()) && d.nombre().equals(nombre))
                .filter(d -> incluye(d.coreCompatible(), s.versionCore()))
                .map(Descriptor::version)
                .toList();
    }

    private static List<Version> coresQueCaben(Datos datos, Seleccion s, RangoVersion rango) {
        return datos.cores().getOrDefault(s.core(), List.of()).stream()
                .filter(v -> incluye(rango, v))
                .toList();
    }

    private static Optional<Alternativa> alt(VeredictoPlugin p, TipoAlternativa tipo) {
        return p.alternativas().stream().filter(a -> a.tipo() == tipo).findFirst();
    }

    private static boolean esIncompatible(VeredictoPlugin p) {
        return p.motivos().stream().anyMatch(m -> m.codigo() == CodigoMotivo.VERSION_INCOMPATIBLE);
    }

    private static VeredictoPlugin de(Veredicto v, String nombre) {
        return v.porPlugin().stream()
                .filter(p -> p.descriptor().nombre().equals(nombre))
                .findFirst()
                .orElseThrow();
    }

    private static Veredicto conAlternativas(Seleccion s, Datos datos) {
        var c = datos.catalogo();
        return Verificador.alternativas(s, Verificador.verificar(s, c), c);
    }

    // ---------------------------------------------------------------- propiedades

    @Property(tries = 1000)
    void determinista(@ForAll("casos") Caso caso) {
        var s = caso.seleccion();
        var c = caso.datos().catalogo();
        var primero = Verificador.verificar(s, c);
        assertEquals(primero, Verificador.verificar(s, c), "segunda llamada");
        assertEquals(primero, Verificador.verificar(s, caso.datos().catalogo()), "sobre una copia nueva del catálogo");
        Verificador.verificar(caso.otra(), c);
        conAlternativas(caso.otra(), caso.datos());
        assertEquals(primero, Verificador.verificar(s, c), "tras verificar otra selección en medio");
    }

    @Property(tries = 1000)
    void ordenYRepetidos(@ForAll("casos") Caso caso) {
        var s = caso.seleccion();
        var v = Verificador.verificar(s, caso.datos().catalogo());
        var primeros = new LinkedHashMap<String, Descriptor>();
        var cuentas = new LinkedHashMap<String, Integer>();
        for (var d : s.plugins()) {
            primeros.putIfAbsent(d.nombre(), d);
            cuentas.merge(d.nombre(), 1, Integer::sum);
        }
        assertEquals(
                List.copyOf(primeros.values()),
                v.porPlugin().stream().map(VeredictoPlugin::descriptor).toList(),
                "porPlugin = plugins sin repeticiones y en el mismo orden");
        var repetidos = cuentas.entrySet().stream()
                .filter(e -> e.getValue() > 1)
                .map(Map.Entry::getKey)
                .toList();
        assertTrue(v.motivosGlobales().stream().allMatch(m -> m.codigo() == CodigoMotivo.PLUGIN_REPETIDO));
        assertEquals(
                repetidos,
                v.motivosGlobales().stream().map(m -> m.plugin().orElseThrow()).toList(),
                "un PLUGIN_REPETIDO global por cada nombre repetido, en orden");
    }

    @Property(tries = 1000)
    void unMotivoPorPluginYGlobalEquivaleASinMotivos(@ForAll("casos") Caso caso) {
        var v = Verificador.verificar(caso.seleccion(), caso.datos().catalogo());
        for (var p : v.porPlugin()) {
            assertTrue(p.motivos().size() <= 1, "a lo sumo un motivo por plugin: " + p);
            assertEquals(p.motivos().isEmpty(), p.compatible());
        }
        boolean sinMotivos = v.motivosGlobales().isEmpty()
                && v.porPlugin().stream().allMatch(p -> p.motivos().isEmpty());
        assertEquals(sinMotivos, v.global() == Veredicto.Global.APROBADA);
    }

    @Property(tries = 1000)
    void alternativaAplicadaAprueba(@ForAll("casos") Caso caso) {
        var s = caso.seleccion();
        var datos = caso.datos();
        var conAlt = conAlternativas(s, datos);
        for (var p : conAlt.porPlugin()) {
            if (!esIncompatible(p)) {
                assertTrue(p.alternativas().isEmpty(), "solo VERSION_INCOMPATIBLE trae alternativas: " + p);
                continue;
            }
            var nombre = p.descriptor().nombre();
            for (var a : p.alternativas()) {
                Seleccion aplicada;
                if (a.tipo() == TipoAlternativa.PLUGIN) {
                    assertEquals(nombre, a.nombre());
                    var nuevo = datos.catalogo()
                            .descriptor(s.core(), nombre, a.version())
                            .orElseThrow(() -> new AssertionError("la alternativa no existe en el catálogo: " + a));
                    var plugins = new ArrayList<>(s.plugins());
                    plugins.set(plugins.indexOf(p.descriptor()), nuevo);
                    aplicada = new Seleccion(s.core(), s.versionCore(), plugins);
                } else {
                    assertEquals(s.core(), a.nombre());
                    aplicada = new Seleccion(s.core(), a.version(), s.plugins());
                }
                var despues = de(Verificador.verificar(aplicada, datos.catalogo()), nombre);
                assertTrue(despues.compatible(), "alternativa " + a + " no deja compatible a " + nombre + ": " + despues);
            }
        }
    }

    @Property(tries = 1000)
    void completitud(@ForAll("casos") Caso caso) {
        var s = caso.seleccion();
        var datos = caso.datos();
        for (var p : conAlternativas(s, datos).porPlugin()) {
            if (!esIncompatible(p)) {
                continue;
            }
            var nombre = p.descriptor().nombre();
            assertEquals(
                    !pluginesQueSirven(datos, s, nombre).isEmpty(),
                    alt(p, TipoAlternativa.PLUGIN).isPresent(),
                    "alternativa de plugin para " + nombre);
            assertEquals(
                    !coresQueCaben(datos, s, p.descriptor().coreCompatible()).isEmpty(),
                    alt(p, TipoAlternativa.CORE).isPresent(),
                    "alternativa de core para " + nombre);
        }
    }

    @Property(tries = 1000)
    void minimoYMaximo(@ForAll("casos") Caso caso) {
        var s = caso.seleccion();
        var datos = caso.datos();
        for (var p : conAlternativas(s, datos).porPlugin()) {
            if (!esIncompatible(p)) {
                continue;
            }
            var nombre = p.descriptor().nombre();
            alt(p, TipoAlternativa.PLUGIN).ifPresent(a -> assertEquals(
                    pluginesQueSirven(datos, s, nombre).stream().max(NUMERICO).orElseThrow(),
                    a.version(),
                    "la de plugin es la más alta que sirve"));
            alt(p, TipoAlternativa.CORE).ifPresent(a -> assertEquals(
                    coresQueCaben(datos, s, p.descriptor().coreCompatible()).stream().min(NUMERICO).orElseThrow(),
                    a.version(),
                    "la de core es la más baja que cabe"));
        }
    }

    // ---------------------------------------------------------------- cobertura del generador (CA-3)

    @Test
    void generadorCubreLosCodigos() {
        var vistos = new LinkedHashSet<String>();
        var casos = casos().sampleStream().limit(1000).collect(Collectors.toList());
        assertEquals(1000, casos.size());
        for (var caso : casos) {
            var s = caso.seleccion();
            var datos = caso.datos();
            var v = conAlternativas(s, datos);
            if (v.global() == Veredicto.Global.APROBADA) {
                vistos.add("APROBADA");
            }
            if (s.plugins().isEmpty()) {
                vistos.add("seleccion vacia");
            }
            s.plugins().stream()
                    .collect(Collectors.groupingBy(Descriptor::nombre, Collectors.counting()))
                    .values()
                    .stream()
                    .filter(n -> n >= 3)
                    .findAny()
                    .ifPresent(n -> vistos.add("nombre repetido 3 veces"));
            v.motivosGlobales().forEach(m -> vistos.add(m.codigo().name()));
            for (var p : v.porPlugin()) {
                p.motivos().forEach(m -> vistos.add(m.codigo().name()));
                if (!esIncompatible(p)) {
                    continue;
                }
                boolean plugin = alt(p, TipoAlternativa.PLUGIN).isPresent();
                boolean core = alt(p, TipoAlternativa.CORE).isPresent();
                vistos.add(plugin && core ? "alternativa de plugin y de core"
                        : plugin ? "alternativa solo de plugin"
                        : core ? "alternativa solo de core"
                        : "sin ninguna alternativa");
                var nombre = p.descriptor().nombre();
                boolean enOtraCarpeta = datos.descriptores().stream()
                        .anyMatch(d -> !d.core().equals(s.core())
                                && d.nombre().equals(nombre)
                                && incluye(d.coreCompatible(), s.versionCore()));
                if (!plugin && enOtraCarpeta) {
                    vistos.add("compatible solo en la carpeta de otro core");
                }
            }
        }
        var esperados = Set.of(
                "APROBADA", "PLUGIN_AUSENTE", "CORE_EQUIVOCADO", "VERSION_INCOMPATIBLE", "PLUGIN_REPETIDO",
                "seleccion vacia", "nombre repetido 3 veces", "alternativa de plugin y de core",
                "alternativa solo de plugin", "alternativa solo de core", "sin ninguna alternativa",
                "compatible solo en la carpeta de otro core");
        assertTrue(vistos.containsAll(esperados), "faltan: " + esperados.stream().filter(e -> !vistos.contains(e)).toList());
        assertFalse(vistos.isEmpty());
    }
}
