package io.synkra.verificador;

import java.util.regex.Pattern;

/** Versión {@code MAYOR.MENOR.PARCHE} (R-V1): tres enteros sin ceros a la izquierda. */
public record Version(int mayor, int menor, int parche) implements Comparable<Version> {

    private static final Pattern FORMA = Pattern.compile("(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)");

    public Version {
        if (mayor < 0 || menor < 0 || parche < 0) {
            throw new IllegalArgumentException("Los componentes de una versión no pueden ser negativos");
        }
    }

    /** Nunca lanza: un texto inválido (o nulo) devuelve {@code VERSION_INVALIDA}. */
    public static Resultado<Version> parsear(String texto) {
        if (texto != null) {
            var m = FORMA.matcher(texto);
            if (m.matches()) {
                try {
                    return Resultado.exito(new Version(
                            Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)), Integer.parseInt(m.group(3))));
                } catch (NumberFormatException desborde) {
                    // cae al error de abajo
                }
            }
        }
        return Resultado.fallo(Motivo.de(CodigoMotivo.VERSION_INVALIDA,
                "La versión '" + texto + "' no tiene la forma MAYOR.MENOR.PARCHE (por ejemplo 2.1.0)", "version"));
    }

    @Override
    public int compareTo(Version otra) {
        int c = Integer.compare(mayor, otra.mayor);
        if (c == 0) c = Integer.compare(menor, otra.menor);
        return c != 0 ? c : Integer.compare(parche, otra.parche);
    }

    @Override
    public String toString() {
        return mayor + "." + menor + "." + parche;
    }
}
