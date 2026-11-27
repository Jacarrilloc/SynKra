package io.synkra.verificador;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.synkra.verificador.Alternativa.TipoAlternativa;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.EdgeCasesMode;
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
    record Datos(String principal, Map<String, List<Version>> cores, List<Descriptor> descriptores) {
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

    private static Arbitrary<Version> versionesLibres() {
        return Combinators.combine(comp(), comp(), comp()).as(Version::new);
    }

    /** Versiones que comparten mayor o menor y mezclan una y dos cifras: el texto y el número ordenan distinto. */
    private static Arbitrary<Version> versionesDeTexto() {
        return Combinators.combine(
                        Arbitraries.of(1, 2, 9, 10, 11), Arbitraries.of(0, 1), Arbitraries.of(0, 1))
                .as(Version::new);
    }

    private static Arbitrary<Version> versiones() {
        return Arbitraries.frequencyOf(Tuple.of(1, versionesLibres()), Tuple.of(1, versionesDeTexto()));
    }

    /** Rangos que abarcan versiones de una y de dos cifras a la vez (varias candidatas que sirven). */
    private static Arbitrary<RangoVersion> rangosAmplios() {
        return Arbitraries.of(0, 1, 2).map(n -> new RangoVersion(new Version(n, 0, 0), Optional.empty(), ">=" + n + ".0.0"));
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

    /** Rangos que dejan fuera lo bajo y abarcan 9.x, 10.x y 11.x: varias candidatas con orden de texto distinto. */
    private static Arbitrary<RangoVersion> rangosDeTexto() {
        Arbitrary<RangoVersion> abierto = Arbitraries.of(2, 9, 10).map(n ->
                new RangoVersion(new Version(n, 0, 0), Optional.empty(), ">=" + n + ".0.0"));
        Arbitrary<RangoVersion> cerrado = Combinators.combine(Arbitraries.of(2, 9), Arbitraries.of(11, 12)).as((a, b) ->
                new RangoVersion(new Version(a, 0, 0), Optional.of(new Version(b, 0, 0)), ">=" + a + ".0.0 <" + b + ".0.0"));
        return Arbitraries.oneOf(abierto, cerrado);
    }

    private static Arbitrary<RangoVersion> rangos() {
        return Arbitraries.frequencyOf(
                Tuple.of(5, rangosPequenos()),
                Tuple.of(2, rangosAmplios()),
                Tuple.of(4, rangosDeTexto()),
                Tuple.of(1, Generadores.rangos()));
    }

    private static Arbitrary<Descriptor> descriptor(String core, String nombre, Version version) {
        return rangos().map(r -> new Descriptor(nombre, version, core, r, Map.of(), Optional.empty()));
    }

    private static Arbitrary<List<Descriptor>> pluginDeCarpeta(String core, String nombre) {
        // Un rango común a todas las versiones del plugin (a menudo) o uno independiente por versión.
        return versiones().list().ofMinSize(1).ofMaxSize(3).uniqueElements().flatMap(vs -> {
            Arbitrary<List<Descriptor>> independientes =
                    Combinators.combine(vs.stream().map(v -> descriptor(core, nombre, v)).toList()).as(l -> l);
            Arbitrary<List<Descriptor>> compartido = rangos().map(r -> vs.stream()
                    .map(v -> new Descriptor(nombre, v, core, r, Map.of(), Optional.empty()))
                    .toList());
            return Arbitraries.frequencyOf(Tuple.of(1, independientes), Tuple.of(1, compartido));
        });
    }

    private static Arbitrary<List<Descriptor>> carpeta(String core) {
        return Arbitraries.of(PLUGINS).list().ofMinSize(1).ofMaxSize(4).uniqueElements().flatMap(ns ->
                Combinators.combine(ns.stream().map(n -> pluginDeCarpeta(core, n)).toList())
                        .as(ls -> ls.stream().flatMap(List::stream).toList()));
    }

    private static Arbitrary<Datos> catalogosGenerados() {
        return Arbitraries.of(CORES).list().ofMinSize(1).ofMaxSize(2).uniqueElements().flatMap(cores -> {
            Arbitrary<List<List<Version>>> versionesCore = Combinators.combine(cores.stream()
                            .map(c -> Arbitraries.frequencyOf(
                                    Tuple.of(1, versiones().list().ofMinSize(1).ofMaxSize(4).uniqueElements()),
                                    Tuple.of(2, versionesDeTexto().list().ofMinSize(2).ofMaxSize(4).uniqueElements())))
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
                return new Datos(cores.get(0), mapa, ds);
            });
        });
    }

    /**
     * Catálogos hechos para separar el texto del número: un core con 9.x, 10.x y 11.x entre sus versiones, y plugins
     * con 2.0.0, 9.0.0, 10.0.0 y 11.0.0 cuyos rangos a menudo sirven a varias versiones a la vez.
     */
    private static Arbitrary<Datos> catalogosDeTexto() {
        Arbitrary<Version> mayores = Arbitraries.of(2, 9, 10, 11).map(n -> new Version(n, 0, 0));
        Arbitrary<List<Version>> versionesCore = Arbitraries.of(
                List.of(new Version(2, 0, 0), new Version(9, 0, 0), new Version(10, 0, 0)),
                List.of(new Version(2, 0, 0), new Version(9, 0, 0), new Version(10, 0, 0), new Version(11, 0, 0)));
        Arbitrary<RangoVersion> rango = Arbitraries.of(">=1.0.0", "2.x", ">=9.0.0", ">=9.0.0", ">=9.0.0", ">=10.0.0", ">=9.0.0 <11.0.0")
                .map(t -> switch (RangoVersion.parsear(t)) {
                    case Resultado.Exito<RangoVersion> e -> e.valor();
                    case Resultado.Fallo<RangoVersion> f -> throw new AssertionError(t);
                });
        Arbitrary<List<Descriptor>> plugins = Arbitraries.of(PLUGINS).list().ofMinSize(1).ofMaxSize(2).uniqueElements().flatMap(ns ->
                Combinators.combine(ns.stream()
                                .map(n -> mayores.list().ofMinSize(2).ofMaxSize(4).uniqueElements().flatMap(vs ->
                                        Combinators.combine(vs.stream()
                                                        .map(v -> rango.map(r -> new Descriptor(
                                                                n, v, "core-a", r, Map.of(), Optional.empty())))
                                                        .toList())
                                                .as(l -> l)))
                                .toList())
                        .as(ls -> ls.stream().flatMap(List::stream).toList()));
        // Una carpeta «core-b» con los mismos nombres de plugin y un rango que sirve: la versión compatible está solo en otra
        // carpeta. «core-b» tiene una versión registrada en 3 de cada 4 catálogos y ninguna en el resto.
        var abierto = new RangoVersion(new Version(1, 0, 0), Optional.empty(), ">=1.0.0");
        return Combinators.combine(versionesCore, plugins, Arbitraries.of(true, true, true, false)).as((vs, ds, core_b_registrado) -> {
            var todos = new ArrayList<>(ds);
            ds.stream().map(Descriptor::nombre).distinct().forEach(n -> todos.add(
                    new Descriptor(n, new Version(1, 0, 0), "core-b", abierto, Map.of(), Optional.empty())));
            var cores = new LinkedHashMap<String, List<Version>>();
            cores.put("core-a", vs);
            if (core_b_registrado) {
                cores.put("core-b", List.of(new Version(1, 0, 0)));
            }
            return new Datos("core-a", cores, todos);
        });
    }

    /**
     * El catálogo de la suite (S1 a S12) como uno de los posibles. Los datos salen de {@code SuiteCombinaciones}: los
     * nombres se leen de sus selecciones y los descriptores y versiones, de su catálogo.
     */
    /** El core sobre el que viven S1 a S10: el principal de este catálogo (no se deduce del orden de nada). */
    private static final String CORE_DE_LA_SUITE = "synkra-core";

    private static Datos catalogoDeLaSuite() {
        var catalogo = SuiteCombinaciones.catalogo();
        var cores = new java.util.TreeSet<String>();
        var plugins = new java.util.TreeSet<String>();
        for (var caso : SuiteCombinaciones.casos()) {
            if (caso.entrada() instanceof SuiteCombinaciones.Entrada.PorSeleccion p) {
                cores.add(p.seleccion().core());
                p.seleccion().plugins().forEach(d -> {
                    cores.add(d.core());
                    plugins.add(d.nombre());
                });
            }
        }
        var versionesCore = new LinkedHashMap<String, List<Version>>();
        var descriptores = new ArrayList<Descriptor>();
        for (var core : cores) {
            versionesCore.put(core, catalogo.versionesCore(core));
            for (var plugin : plugins) {
                for (var version : catalogo.versionesPlugin(core, plugin)) {
                    descriptores.add(catalogo.descriptor(core, plugin, version).orElseThrow());
                }
            }
        }
        return new Datos(CORE_DE_LA_SUITE, versionesCore, descriptores);
    }

    private static Arbitrary<Datos> catalogos() {
        return Arbitraries.frequencyOf(
                Tuple.of(3, catalogosGenerados()),
                Tuple.of(6, catalogosDeTexto()),
                Tuple.of(1, Arbitraries.just(catalogoDeLaSuite())));
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
        // El core principal de cada catálogo (Datos.principal) pesa más: registrar «core-b» no diluye los casos que
        // dependen de él (synkra-core en la suite, core-a en los de texto).
        Arbitrary<String> core = Arbitraries.frequencyOf(
                Tuple.of(8, Arbitraries.just(datos.principal())),
                Tuple.of(1, Arbitraries.of(nombresDeCores)),
                Tuple.of(1, Arbitraries.just("core-z")));
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

    /** El motivo que debe tener cada plugin (sin repetidos) según el catálogo, calculado recorriendo los datos. */
    private static Map<String, Optional<CodigoMotivo>> motivosEsperados(Seleccion s, Datos datos) {
        var esperado = new LinkedHashMap<String, Optional<CodigoMotivo>>();
        for (var d : s.plugins()) {
            if (esperado.containsKey(d.nombre())) {
                continue;
            }
            boolean existe = datos.descriptores().stream().anyMatch(e -> e.core().equals(d.core())
                    && e.nombre().equals(d.nombre())
                    && e.version().equals(d.version()));
            esperado.put(d.nombre(), !existe
                    ? Optional.of(CodigoMotivo.PLUGIN_AUSENTE)
                    : !d.core().equals(s.core())
                            ? Optional.of(CodigoMotivo.CORE_EQUIVOCADO)
                            : !incluye(d.coreCompatible(), s.versionCore())
                                    ? Optional.of(CodigoMotivo.VERSION_INCOMPATIBLE)
                                    : Optional.<CodigoMotivo>empty());
        }
        return esperado;
    }

    private static void afirmaMotivos(Veredicto v, Seleccion s, Datos datos, String donde) {
        var obtenido = new LinkedHashMap<String, Optional<CodigoMotivo>>();
        v.porPlugin().forEach(p -> obtenido.put(
                p.descriptor().nombre(), p.motivos().stream().map(Motivo::codigo).findFirst()));
        assertEquals(motivosEsperados(s, datos), obtenido, "motivo de cada plugin " + donde);
    }

    private static Veredicto conAlternativas(Seleccion s, Datos datos) {
        var c = datos.catalogo();
        return Verificador.alternativas(s, Verificador.verificar(s, c), c);
    }

    // ---------------------------------------------------------------- propiedades

    @Provide
    Arbitrary<Datos> otrosCatalogos() {
        return catalogos();
    }

    @Property(tries = 1000, edgeCases = EdgeCasesMode.NONE)
    void determinista(@ForAll("casos") Caso caso, @ForAll("otrosCatalogos") Datos otro) {
        var s = caso.seleccion();
        var c = caso.datos().catalogo();
        var primero = Verificador.verificar(s, c);
        afirmaMotivos(primero, s, caso.datos(), "(catálogo del caso)");
        assertEquals(primero, Verificador.verificar(s, c), "segunda llamada");
        assertEquals(primero, Verificador.verificar(s, caso.datos().catalogo()), "sobre una copia nueva del catálogo");
        Verificador.verificar(caso.otra(), c);
        conAlternativas(caso.otra(), caso.datos());
        // La misma selección contra otro catálogo (generado y vacío) no puede arrastrar nada del primero.
        afirmaMotivos(Verificador.verificar(s, otro.catalogo()), s, otro, "(otro catálogo generado)");
        var vacio = new Datos("core-z", Map.of(), List.of());
        afirmaMotivos(Verificador.verificar(s, vacio.catalogo()), s, vacio, "(catálogo vacío)");
        assertEquals(primero, Verificador.verificar(s, c), "tras verificar otra selección y otros catálogos en medio");
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
        var base = Verificador.verificar(s, datos.catalogo());
        assertEquals(
                base.porPlugin().stream().map(VeredictoPlugin::descriptor).toList(),
                conAlt.porPlugin().stream().map(VeredictoPlugin::descriptor).toList(),
                "alternativas conserva el orden de porPlugin");
        assertEquals(base.motivosGlobales(), conAlt.motivosGlobales(), "alternativas conserva los globales");
        assertEquals(base.global(), conAlt.global());
        for (var p : conAlt.porPlugin()) {
            if (alt(p, TipoAlternativa.PLUGIN).isPresent() && alt(p, TipoAlternativa.CORE).isPresent()) {
                assertEquals(TipoAlternativa.PLUGIN, p.alternativas().get(0).tipo(), "la de plugin va antes que la de core");
            }
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

    private static void cuenta(Map<String, Integer> vistos, String clave) {
        vistos.merge(clave, 1, Integer::sum);
    }

    /** Cuántos casos de cada clase hay en la muestra (lo usan la prueba de cobertura y la medición de la bitácora). */
    static Map<String, Integer> frecuencias(List<Caso> casos) {
        var vistos = new java.util.TreeMap<String, Integer>();
        for (var caso : casos) {
            var s = caso.seleccion();
            var datos = caso.datos();
            var v = conAlternativas(s, datos);
            if (v.global() == Veredicto.Global.APROBADA) {
                cuenta(vistos, "APROBADA");
            }
            if (s.plugins().isEmpty()) {
                cuenta(vistos, "seleccion vacia");
            }
            s.plugins().stream()
                    .collect(Collectors.groupingBy(Descriptor::nombre, Collectors.counting()))
                    .values()
                    .stream()
                    .filter(n -> n >= 3)
                    .findAny()
                    .ifPresent(n -> cuenta(vistos, "nombre repetido 3 veces"));
            if (datos.cores().containsKey("otro-core") && s.core().equals(CORE_DE_LA_SUITE)) {
                cuenta(vistos, "catalogo de la suite en synkra-core");
                if (v.porPlugin().stream().anyMatch(VerificadorPropiedades::esIncompatible)) {
                    cuenta(vistos, "catalogo de la suite en synkra-core con VERSION_INCOMPATIBLE");
                }
            }
            v.motivosGlobales().forEach(m -> cuenta(vistos, m.codigo().name()));
            for (var p : v.porPlugin()) {
                p.motivos().forEach(m -> cuenta(vistos, m.codigo().name()));
                if (!esIncompatible(p)) {
                    continue;
                }
                boolean plugin = alt(p, TipoAlternativa.PLUGIN).isPresent();
                boolean core = alt(p, TipoAlternativa.CORE).isPresent();
                cuenta(vistos, plugin && core ? "alternativa de plugin y de core"
                        : plugin ? "alternativa solo de plugin"
                        : core ? "alternativa solo de core"
                        : "sin ninguna alternativa");
                var nombre = p.descriptor().nombre();
                var sirven = pluginesQueSirven(datos, s, nombre);
                if (sirven.size() >= 2
                        && !sirven.stream().max(NUMERICO).equals(sirven.stream().max(Comparator.comparing(Version::toString)))) {
                    cuenta(vistos, "2 o más de plugin con texto distinto de número");
                }
                var caben = coresQueCaben(datos, s, p.descriptor().coreCompatible());
                if (caben.size() >= 2
                        && !caben.stream().min(NUMERICO).equals(caben.stream().min(Comparator.comparing(Version::toString)))) {
                    cuenta(vistos, "2 o más de core con texto distinto de número");
                }
                boolean enOtraCarpeta = datos.descriptores().stream()
                        .anyMatch(d -> !d.core().equals(s.core())
                                && d.nombre().equals(nombre)
                                && incluye(d.coreCompatible(), s.versionCore()));
                if (!plugin && enOtraCarpeta) {
                    cuenta(vistos, "compatible solo en la carpeta de otro core");
                }
                boolean enOtraCarpetaSinCoreRegistrado = datos.descriptores().stream()
                        .anyMatch(d -> !d.core().equals(s.core())
                                && d.nombre().equals(nombre)
                                && incluye(d.coreCompatible(), s.versionCore())
                                && datos.cores().getOrDefault(d.core(), List.of()).isEmpty());
                if (!plugin && enOtraCarpetaSinCoreRegistrado) {
                    cuenta(vistos, "compatible solo en la carpeta de otro core NO registrado");
                }
                boolean enOtraCarpetaConCoreRegistrado = datos.descriptores().stream()
                        .anyMatch(d -> !d.core().equals(s.core())
                                && d.nombre().equals(nombre)
                                && incluye(d.coreCompatible(), s.versionCore())
                                && !datos.cores().getOrDefault(d.core(), List.of()).isEmpty());
                if (!plugin && enOtraCarpetaConCoreRegistrado) {
                    cuenta(vistos, "compatible solo en la carpeta de otro core registrado");
                }
            }
        }
        return vistos;
    }

    @Test
    void catalogoDeLaSuiteCubreTodoElCatalogo() {
        var datos = catalogoDeLaSuite();
        assertEquals(CORE_DE_LA_SUITE, datos.principal());
        assertEquals(Set.of("synkra-core", "otro-core"), datos.cores().keySet());
        assertEquals(
                Set.of("synkra-core/kyc-co 1.0.0", "synkra-core/kyc-pe 1.3.0", "synkra-core/kyc-pe 2.0.0", "otro-core/kyc-xx 1.0.0"),
                datos.descriptores().stream()
                        .map(d -> d.core() + "/" + d.nombre() + " " + d.version())
                        .collect(Collectors.toSet()));
    }

    @Test
    void generadorCubreLosCodigos() {
        var casos = casos().sampleStream().limit(1000).collect(Collectors.toList());
        assertEquals(1000, casos.size());
        var vistos = frecuencias(casos);
        var esperados = Set.of(
                "APROBADA", "PLUGIN_AUSENTE", "CORE_EQUIVOCADO", "VERSION_INCOMPATIBLE", "PLUGIN_REPETIDO",
                "seleccion vacia", "nombre repetido 3 veces", "alternativa de plugin y de core",
                "alternativa solo de plugin", "alternativa solo de core", "sin ninguna alternativa",
                "compatible solo en la carpeta de otro core", "compatible solo en la carpeta de otro core registrado",
                "compatible solo en la carpeta de otro core NO registrado", "catalogo de la suite en synkra-core",
                "catalogo de la suite en synkra-core con VERSION_INCOMPATIBLE");
        assertTrue(vistos.keySet().containsAll(esperados), "faltan: " + esperados.stream().filter(e -> !vistos.containsKey(e)).toList());
        // Los casos que separan el máximo del mínimo, el texto del número y una carpeta de otra: que no dependan de la
        // suerte. Cada umbral es menos de la tercera parte de la media medida en 100 lotes de 1000 (ver la bitácora).
        assertTrue(vistos.getOrDefault("2 o más de plugin con texto distinto de número", 0) >= 15, "frecuencias: " + vistos);
        assertTrue(vistos.getOrDefault("2 o más de core con texto distinto de número", 0) >= 6, "frecuencias: " + vistos);
        assertTrue(vistos.getOrDefault("compatible solo en la carpeta de otro core", 0) >= 10, "frecuencias: " + vistos);
        assertTrue(vistos.getOrDefault("compatible solo en la carpeta de otro core registrado", 0) >= 8, "frecuencias: " + vistos);
        // El catálogo de la suite en synkra-core (donde viven S1 a S10) y su región de S4 y S5: la base daba ~29 y ~12,8.
        assertTrue(vistos.getOrDefault("catalogo de la suite en synkra-core", 0) >= 20, "frecuencias: " + vistos);
        assertTrue(vistos.getOrDefault("catalogo de la suite en synkra-core con VERSION_INCOMPATIBLE", 0) >= 8, "frecuencias: " + vistos);
    }
}
