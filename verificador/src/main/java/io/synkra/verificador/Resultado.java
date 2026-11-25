package io.synkra.verificador;

import java.util.List;
import java.util.Objects;

/** Un valor o una lista de motivos; nunca una excepción para errores de negocio. */
public sealed interface Resultado<T> {

    record Exito<T>(T valor) implements Resultado<T> {
        public Exito {
            Objects.requireNonNull(valor, "valor");
        }
    }

    record Fallo<T>(List<Motivo> motivos) implements Resultado<T> {
        public Fallo {
            motivos = List.copyOf(motivos);
            if (motivos.isEmpty()) {
                throw new IllegalArgumentException("Un fallo lleva al menos un motivo");
            }
        }
    }

    static <T> Resultado<T> exito(T valor) {
        return new Exito<>(valor);
    }

    static <T> Resultado<T> fallo(Motivo motivo) {
        return new Fallo<>(List.of(motivo));
    }
}
