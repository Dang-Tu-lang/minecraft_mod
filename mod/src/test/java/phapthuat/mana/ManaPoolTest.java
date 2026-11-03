package phapthuat.mana;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ManaPoolTest {
    @Test
    void startsFull() {
        assertEquals(1000, new ManaPool(1000).current());
    }

    @Test
    void spend() {
        var p = new ManaPool(1000);
        assertTrue(p.trySpend(33.3));
        assertEquals(966.7, p.current(), 1e-9);
    }

    @Test
    void cannotOverspend() {
        var p = new ManaPool(1000);
        p.trySpend(999.95);
        assertFalse(p.trySpend(0.1));
        assertEquals(0.05, p.current(), 1e-9);
    }

    @Test
    void addCaps() {
        var p = new ManaPool(1000);
        p.trySpend(100);
        p.add(392.8);
        assertEquals(1000, p.current());
    }

    @Test
    void regen() {
        var p = new ManaPool(1000);
        p.trySpend(10);
        p.regen(1.0, 2);
        assertEquals(992, p.current(), 1e-9);
    }
}
