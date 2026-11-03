package phapthuat.physics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MagnetCalcTest {
    @Test
    void field() {
        assertEquals(99_474, MagnetCalc.energyDensity(0.5), 5);
        assertEquals(416_677, MagnetCalc.fieldEnergyJ(0.5, 1), 20);
    }

    @Test
    void accel() {
        assertEquals(0.4 * 0.25 * 24 / 70, MagnetCalc.accel(0.4, 0.5, 24, 70), 1e-12);
        assertEquals(0, MagnetCalc.accel(0.4, 0.5, 0, 70), 1e-12);
        assertFalse(MagnetCalc.accel(0.4, 0.5, 5, 0) > 0);
    }
}
