package io.synkra.kubernetes.api;

import java.util.List;
import java.util.regex.Pattern;

/** Lo que debe correr en un país, ya resuelto por quien llama (U07): core por digest y plugins con hash. */
public record PlanPais(String pais, String imagenCore, List<PluginPlan> plugins) {

    private static final Pattern PAIS = Pattern.compile("[a-z]{2}");
    private static final Pattern IMAGEN_CON_DIGEST = Pattern.compile("[^\\s@]+@sha256:[0-9a-f]{64}");
    private static final Pattern SHA256 = Pattern.compile("[0-9a-f]{64}");
    private static final Pattern SEGURO = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._+-]*");

    public PlanPais {
        if (pais == null || !PAIS.matcher(pais).matches()) {
            throw new IllegalArgumentException("País inválido: debe ser de dos letras minúsculas");
        }
        if (imagenCore == null || !IMAGEN_CON_DIGEST.matcher(imagenCore).matches()) {
            throw new IllegalArgumentException("La imagen del core debe ir por digest (@sha256:<64 hex>)");
        }
        plugins = plugins == null ? List.of() : List.copyOf(plugins);
    }

    /** Un plugin del país: nombre, versión y sha256 (64 hexadecimales en minúsculas) de su jar. */
    public record PluginPlan(String nombre, String version, String sha256) {

        public PluginPlan {
            if (nombre == null || !SEGURO.matcher(nombre).matches()) {
                throw new IllegalArgumentException("Nombre de plugin inválido");
            }
            if (version == null || !SEGURO.matcher(version).matches()) {
                throw new IllegalArgumentException("Versión de plugin inválida");
            }
            if (sha256 == null || !SHA256.matcher(sha256).matches()) {
                throw new IllegalArgumentException("sha256 inválido: 64 hexadecimales en minúsculas");
            }
        }
    }
}
