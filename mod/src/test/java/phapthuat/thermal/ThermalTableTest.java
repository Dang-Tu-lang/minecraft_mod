package phapthuat.thermal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ThermalTableTest {
    private static BlockThermal hot(double temp) {
        return new BlockThermal("wood", temp, 0, 20, 20);
    }

    @Test
    void decaysTowardNatural() {
        var t = new ThermalTable(10_000);
        t.put(1L, hot(300), 0);
        assertEquals(160, t.get(1L, 400).orElseThrow().tempC(), 1e-9);
    }

    @Test
    void settledEntriesRemoved() {
        var t = new ThermalTable(10_000);
        t.put(1L, hot(20.5), 0);
        t.sweep(20);
        assertTrue(t.get(1L, 20).isEmpty());
    }

    @Test
    void capEvictsOldest() {
        var t = new ThermalTable(2);
        t.put(1L, hot(300), 0);
        t.put(2L, hot(300), 0);
        t.put(3L, hot(300), 0);
        assertEquals(2, t.size());
        assertTrue(t.get(1L, 0).isEmpty());
    }

    @Test
    void entityDoseAndCoreDecayIndependently() {
        var e = new EntityThermal(47, 6.0, 0).decayedTo(1200, 37, 20, 60);
        assertEquals(38.25, e.coreTempC(), 1e-9);
        assertEquals(3.0, e.doseGy(), 1e-9);
    }
}
