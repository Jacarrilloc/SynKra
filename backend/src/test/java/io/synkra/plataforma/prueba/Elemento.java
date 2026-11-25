package io.synkra.plataforma.prueba;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.springframework.data.annotation.Id;

public record Elemento(
        @Id Long id,
        int x,
        String texto,
        boolean activo,
        Instant instante,
        LocalDate fecha,
        LocalDateTime fechaHora,
        BigDecimal monto) {

    public static Elemento con(int x) {
        return new Elemento(null, x, "t" + x, x % 2 == 0, Instant.parse("2026-01-02T03:04:05Z"),
                LocalDate.of(2026, 1, 2), LocalDateTime.of(2026, 1, 2, 3, 4, 5), new BigDecimal("12.50"));
    }
}
