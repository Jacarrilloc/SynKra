package io.synkra.verificador;

import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Rango de versiones (R-V2) como intervalo {@code [minimo, maximo)}; sin máximo es {@code [minimo, ∞)}.
 * Formas: {@code N.x}, {@code N.M.x}, exacta {@code N.M.P}, e intervalo {@code >=A [<B]}.
 */
public record RangoVersion(Version minimo, Optional<Version> maximo, String texto) {

    private static final String NUM = "(?:0|[1-9][0-9]*)";
    private static final Pattern MAYOR_X = Pattern.compile("(" + NUM + ")\\.x");
    private static final Pattern MENOR_X = Pattern.compile("(" + NUM + ")\\.(" + NUM + ")\\.x");
    private static final Pattern EXACTA = Pattern.compile(NUM + "\\." + NUM + "\\." + NUM);
    private static final Pattern INTERVALO =
            Pattern.compile(">= *(" + NUM + "\\." + NUM + "\\." + NUM + ")(?: *< *(" + NUM + "\\." + NUM + "\\."
                    + NUM + "))?");

    public RangoVersion {
        Objects.requireNonNull(minimo, "minimo");
        Objects.requireNonNull(maximo, "maximo");
        Objects.requireNonNull(texto, "texto");
        if (maximo.isPresent() && minimo.compareTo(maximo.get()) >= 0) {
            throw new IllegalArgumentException("El mínimo debe ser menor que el máximo");
        }
    }

    /** Nunca lanza: un texto fuera de las cuatro formas (o nulo, o de intervalo vacío) da {@code RANGO_INVALIDO}. */
    public static Resultado<RangoVersion> parsear(String texto) {
        if (texto != null) {
            try {
                var r = intentar(texto);
                if (r != null) {
                    return Resultado.exito(r);
                }
            } catch (ArithmeticException | IllegalArgumentException fueraDeRango) {
                // cae al error de abajo
            }
        }
        var mensaje = texto == null
                ? "Falta el rango de versiones del core compatible"
                : "El rango '" + texto
                        + "' no es válido: use N.x, N.M.x, N.M.P o >=N.M.P [<N.M.P] con un mínimo menor que el máximo";
        return Resultado.fallo(Motivo.de(CodigoMotivo.RANGO_INVALIDO, mensaje, "coreCompatible"));
    }

    private static RangoVersion intentar(String texto) {
        var m = MAYOR_X.matcher(texto);
        if (m.matches()) {
            int n = Integer.parseInt(m.group(1));
            return new RangoVersion(new Version(n, 0, 0), Optional.of(new Version(Math.incrementExact(n), 0, 0)), texto);
        }
        m = MENOR_X.matcher(texto);
        if (m.matches()) {
            int n = Integer.parseInt(m.group(1));
            int k = Integer.parseInt(m.group(2));
            return new RangoVersion(
                    new Version(n, k, 0), Optional.of(new Version(n, Math.incrementExact(k), 0)), texto);
        }
        if (EXACTA.matcher(texto).matches()) {
            var min = version(texto);
            return new RangoVersion(
                    min, Optional.of(new Version(min.mayor(), min.menor(), Math.incrementExact(min.parche()))), texto);
        }
        m = INTERVALO.matcher(texto);
        if (m.matches()) {
            var max = m.group(2) == null ? Optional.<Version>empty() : Optional.of(version(m.group(2)));
            return new RangoVersion(version(m.group(1)), max, texto);
        }
        return null;
    }

    private static Version version(String texto) {
        return switch (Version.parsear(texto)) {
            case Resultado.Exito<Version> e -> e.valor();
            case Resultado.Fallo<Version> f -> throw new IllegalArgumentException(texto);
        };
    }

    /** {@code minimo ≤ v} y (sin máximo o {@code v < maximo}). */
    public boolean incluye(Version v) {
        return minimo.compareTo(v) <= 0 && maximo.map(max -> v.compareTo(max) < 0).orElse(true);
    }
}
