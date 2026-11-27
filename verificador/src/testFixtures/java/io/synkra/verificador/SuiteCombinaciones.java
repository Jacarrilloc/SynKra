package io.synkra.verificador;

import io.synkra.verificador.Veredicto.Global;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * La suite de combinaciones (PRD §11.1): doce casos con su entrada y su resultado exacto. U01, U02 y U07 la usan
 * desde {@code testFixtures(project(":verificador"))}, así que los datos viven solo aquí. Cada llamada devuelve
 * estructuras nuevas e inmutables.
 */
public final class SuiteCombinaciones {

    private SuiteCombinaciones() {}

    /** Los siete grupos del PRD §11.1 y {@link #REPETIDO}. */
    public enum Grupo {
        VALIDAS,
        VERSION_INCOMPATIBLE,
        PLUGIN_AUSENTE,
        CARPETA_EQUIVOCADA,
        DESCRIPTOR_INCOMPLETO,
        INTEGRIDAD,
        PLUGIN_NO_ACTIVADO,
        /** Lo añade el diseño de U01 (Q5); no es uno de los 7 grupos del PRD §11.1. Solo contiene S10. */
        REPETIDO
    }

    /** Lo que se le da al sistema bajo prueba. */
    public sealed interface Entrada {
        /** Se verifica con {@link Verificador} contra {@link SuiteCombinaciones#catalogo()}. */
        record PorSeleccion(Seleccion seleccion) implements Entrada {
            public PorSeleccion {
                Objects.requireNonNull(seleccion, "seleccion");
            }
        }

        /** El texto de {@code synkra-plugin.yaml}: se lee con {@link LectorDescriptor}. */
        record PorDescriptor(String yaml) implements Entrada {
            public PorDescriptor {
                Objects.requireNonNull(yaml, "yaml");
            }
        }

        /** Lo cubre otra unidad: {@code remiteA} dice cuál y con qué prueba. */
        record FueraDeU01(String remiteA) implements Entrada {
            public FueraDeU01 {
                Objects.requireNonNull(remiteA, "remiteA");
            }
        }
    }

    /** Un motivo esperado; {@code enMensaje} es lo que el mensaje debe nombrar (el plugin o el campo). */
    public record MotivoEsperado(CodigoMotivo codigo, Optional<String> plugin, Optional<String> campo, String enMensaje) {}

    /** Quién produce el resultado de un caso. */
    public enum Origen {
        /** {@link Verificador}: {@code global} está presente. */
        VEREDICTO,
        /** {@link LectorDescriptor}: rechaza el descriptor; {@code global} está vacío. */
        LECTOR,
        /** Lo cubre otra unidad: no hay resultado que afirmar en U01; {@code global} está vacío. */
        FUERA_DE_U01
    }

    /**
     * Resultado exacto. {@code origen} dice quién lo produce; no se deduce de {@code global} (vacío en {@code LECTOR}
     * y en {@code FUERA_DE_U01}). {@code motivos} son los de cada plugin, en orden, y después los globales.
     * {@code alternativas}, las de todos los plugins en orden.
     */
    public record Esperado(Origen origen, Optional<Global> global, List<MotivoEsperado> motivos, List<Alternativa> alternativas) {
        public Esperado {
            Objects.requireNonNull(origen, "origen");
            motivos = List.copyOf(motivos);
            alternativas = List.copyOf(alternativas);
            if (origen == Origen.VEREDICTO != global.isPresent()) {
                throw new IllegalArgumentException("global solo está presente cuando el origen es VEREDICTO");
            }
        }

        public boolean rechazaElLector() {
            return origen == Origen.LECTOR;
        }

        public boolean esFueraDeU01() {
            return origen == Origen.FUERA_DE_U01;
        }
    }

    public record Caso(String id, Grupo grupo, String descripcion, Entrada entrada, Esperado esperado) {}

    private static final String CORE = "synkra-core";
    private static final String OTRO = "otro-core";

    private static Version v(int a, int b, int c) {
        return new Version(a, b, c);
    }

    private static Descriptor d(String nombre, Version version, String core, String compatible) {
        var rango = switch (RangoVersion.parsear(compatible)) {
            case Resultado.Exito<RangoVersion> e -> e.valor();
            case Resultado.Fallo<RangoVersion> f -> throw new IllegalStateException(compatible + ": " + f.motivos());
        };
        return new Descriptor(nombre, version, core, rango, Map.of(), Optional.empty());
    }

    private static final Descriptor KYC_CO = d("kyc-co", v(1, 0, 0), CORE, "2.x");
    private static final Descriptor KYC_PE_13 = d("kyc-pe", v(1, 3, 0), CORE, "2.x");
    private static final Descriptor KYC_PE_20 = d("kyc-pe", v(2, 0, 0), CORE, "3.x");
    private static final Descriptor KYC_XX = d("kyc-xx", v(1, 0, 0), OTRO, "1.x");
    /** No está en el catálogo: es el plugin ausente de S6. */
    private static final Descriptor KYC_MX = d("kyc-mx", v(1, 0, 0), CORE, "2.x");

    /** El catálogo fijo de la suite; uno nuevo en cada llamada. */
    public static CatalogoEnMemoria catalogo() {
        return new CatalogoEnMemoria(
                Map.of(CORE, List.of(v(2, 1, 0), v(3, 0, 0)), OTRO, List.of(v(1, 0, 0))),
                List.of(KYC_CO, KYC_PE_13, KYC_PE_20, KYC_XX));
    }

    private static Entrada sel(Version core, Descriptor... plugins) {
        return new Entrada.PorSeleccion(new Seleccion(CORE, core, List.of(plugins)));
    }

    private static Esperado aprobada() {
        return new Esperado(Origen.VEREDICTO, Optional.of(Global.APROBADA), List.of(), List.of());
    }

    private static Esperado rechazada(List<MotivoEsperado> motivos, List<Alternativa> alternativas) {
        return new Esperado(Origen.VEREDICTO, Optional.of(Global.RECHAZADA), motivos, alternativas);
    }

    /** Para S11 y S12: no se ejecutan en U01, así que no hay resultado que afirmar. */
    private static Esperado sinResultado() {
        return new Esperado(Origen.FUERA_DE_U01, Optional.empty(), List.of(), List.of());
    }

    private static MotivoEsperado deplugin(CodigoMotivo codigo, String plugin, String campo) {
        return new MotivoEsperado(codigo, Optional.of(plugin), Optional.ofNullable(campo), plugin);
    }

    private static MotivoEsperado delcampo(CodigoMotivo codigo, String campo) {
        return new MotivoEsperado(codigo, Optional.empty(), Optional.of(campo), campo);
    }

    private static final String YAML_SIN_CORE_COMPATIBLE =
            "nombre: kyc-co\nversion: 1.0.0\ncore: synkra-core\n";
    private static final String YAML_CAMPO_MAL_ESCRITO =
            "nombre: kyc-co\nversion: 1.0.0\ncore: synkra-core\ncoreCompatibel: 2.x\n";

    /** Los doce casos, S1 a S12, en orden. */
    public static List<Caso> casos() {
        return List.of(
                new Caso("S1", Grupo.VALIDAS, "synkra-core 2.1.0 con kyc-co 1.0.0",
                        sel(v(2, 1, 0), KYC_CO), aprobada()),
                new Caso("S2", Grupo.VALIDAS, "synkra-core 2.1.0 con kyc-pe 1.3.0",
                        sel(v(2, 1, 0), KYC_PE_13), aprobada()),
                new Caso("S3", Grupo.VALIDAS, "synkra-core 2.1.0 sin plugins", sel(v(2, 1, 0)), aprobada()),
                new Caso("S4", Grupo.VERSION_INCOMPATIBLE, "kyc-pe 2.0.0 (3.x) sobre synkra-core 2.1.0",
                        sel(v(2, 1, 0), KYC_PE_20),
                        rechazada(
                                List.of(deplugin(CodigoMotivo.VERSION_INCOMPATIBLE, "kyc-pe", "coreCompatible")),
                                List.of(
                                        new Alternativa(Alternativa.TipoAlternativa.PLUGIN, "kyc-pe", v(1, 3, 0)),
                                        new Alternativa(Alternativa.TipoAlternativa.CORE, CORE, v(3, 0, 0))))),
                new Caso("S5", Grupo.VERSION_INCOMPATIBLE, "kyc-co 1.0.0 (2.x) sobre synkra-core 3.0.0",
                        sel(v(3, 0, 0), KYC_CO),
                        rechazada(
                                List.of(deplugin(CodigoMotivo.VERSION_INCOMPATIBLE, "kyc-co", "coreCompatible")),
                                List.of(new Alternativa(Alternativa.TipoAlternativa.CORE, CORE, v(2, 1, 0))))),
                new Caso("S6", Grupo.PLUGIN_AUSENTE, "kyc-mx 1.0.0 no está en la carpeta de synkra-core",
                        sel(v(2, 1, 0), KYC_MX),
                        rechazada(List.of(deplugin(CodigoMotivo.PLUGIN_AUSENTE, "kyc-mx", null)), List.of())),
                new Caso("S7", Grupo.CARPETA_EQUIVOCADA, "kyc-xx 1.0.0 es de la carpeta de otro-core",
                        sel(v(2, 1, 0), KYC_XX),
                        rechazada(List.of(deplugin(CodigoMotivo.CORE_EQUIVOCADO, "kyc-xx", "core")), List.of())),
                new Caso("S8", Grupo.DESCRIPTOR_INCOMPLETO, "descriptor sin coreCompatible",
                        new Entrada.PorDescriptor(YAML_SIN_CORE_COMPATIBLE),
                        new Esperado(
                                Origen.LECTOR,
                                Optional.empty(),
                                List.of(delcampo(CodigoMotivo.DESCRIPTOR_FALTA_CAMPO, "coreCompatible")),
                                List.of())),
                new Caso("S9", Grupo.DESCRIPTOR_INCOMPLETO, "descriptor con coreCompatibel en vez de coreCompatible",
                        new Entrada.PorDescriptor(YAML_CAMPO_MAL_ESCRITO),
                        new Esperado(
                                Origen.LECTOR,
                                Optional.empty(),
                                List.of(
                                        delcampo(CodigoMotivo.DESCRIPTOR_CAMPO_DESCONOCIDO, "coreCompatibel"),
                                        delcampo(CodigoMotivo.DESCRIPTOR_FALTA_CAMPO, "coreCompatible")),
                                List.of())),
                new Caso("S10", Grupo.REPETIDO, "kyc-pe 1.3.0 y kyc-pe 2.0.0 a la vez",
                        sel(v(2, 1, 0), KYC_PE_13, KYC_PE_20),
                        rechazada(List.of(deplugin(CodigoMotivo.PLUGIN_REPETIDO, "kyc-pe", null)), List.of())),
                new Caso("S11", Grupo.INTEGRIDAD, "el hash del artefacto no coincide",
                        new Entrada.FueraDeU01("U04 (hash al subir) y U05 (hash al descargar en el init container); prueba de integridad pendiente en U04 y U05"),
                        sinResultado()),
                new Caso("S12", Grupo.PLUGIN_NO_ACTIVADO, "plugin en el catálogo pero no elegido",
                        new Entrada.FueraDeU01("U02 (el core no carga lo que no pide el ConfigMap); prueba de plugin no activado pendiente en U02"),
                        sinResultado()));
    }
}
