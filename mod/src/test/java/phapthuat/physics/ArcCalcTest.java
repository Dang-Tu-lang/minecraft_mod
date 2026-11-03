package phapthuat.physics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ArcCalcTest {
    @Test
    void voltageFor5m() {
        assertEquals(15e6, ArcCalc.requiredVoltage(5, false), 1e-3);
        assertEquals(12e6, ArcCalc.requiredVoltage(5, true), 1e-3);
    }

    @Test
    void outcomes() {
        assertEquals(ArcOutcome.HIT, ArcCalc.outcome(15e6, 5, false));
        assertEquals(ArcOutcome.BACKFIRE_CASTER, ArcCalc.outcome(14e6, 5, false));
        assertEquals(ArcOutcome.FIZZLE, ArcCalc.outcome(1e6, 5, false));
        assertEquals(ArcOutcome.HIT, ArcCalc.outcome(12e6, 5, true));
    }

    @Test
    void charge() {
        assertEquals(11_250, ArcCalc.chargeEnergyJ(15e6), 1e-6);
    }

    @Test
    void damage() {
        assertEquals(10, ArcCalc.damageHp(200_000), 1e-9);
        assertTrue(ArcCalc.lightningEffects(500_000, 10_000));
        assertFalse(ArcCalc.lightningEffects(499_000, 10_000));
    }
}
