package phapthuat.physics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CombustionCalcTest {
    @Test
    void fuels() {
        assertEquals(8.2e6, CombustionCalc.itemEnergyJ(1600), 1e-6);
        assertEquals(1_537_500, CombustionCalc.itemEnergyJ(300), 1e-6);
        assertEquals(512_500, CombustionCalc.itemEnergyJ(100), 1e-6);
    }

    @Test
    void oxygen() {
        assertEquals(0.62595, CombustionCalc.oxygenNeededKg(8.2e6), 1e-4);
    }

    @Test
    void enclosed() {
        var c = CombustionCalc.burn(8.2e6, 2);
        assertTrue(c.carbonMonoxide());
        assertEquals(7.231e6, c.releasedJ(), 1e4);
        var open = CombustionCalc.burn(8.2e6, 64);
        assertFalse(open.carbonMonoxide());
        assertEquals(8.2e6, open.releasedJ(), 1e-6);
        assertFalse(CombustionCalc.burn(8.2e6, 3).carbonMonoxide());
    }
}
