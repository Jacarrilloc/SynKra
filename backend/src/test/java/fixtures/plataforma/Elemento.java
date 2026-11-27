package fixtures.plataforma;

import java.math.BigDecimal;
import org.springframework.data.annotation.Id;

public record Elemento(
        @Id Long id,
        int x,
        String texto,
        boolean activo,
        BigDecimal monto) {

    public static Elemento con(int x) {
        return new Elemento(null, x, "t" + x, x % 2 == 0, new BigDecimal("12.50"));
    }
}
