package io.synkra.verificador;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import org.junit.jupiter.api.Test;

class HumoMotoresTest {

    @Test
    void motorJUnitCarga() {
        assertEquals(2, 1 + 1);
    }

    @Property
    void motorJqwikCarga(@ForAll int n) {
        assertTrue(n + 0 == n);
    }
}
