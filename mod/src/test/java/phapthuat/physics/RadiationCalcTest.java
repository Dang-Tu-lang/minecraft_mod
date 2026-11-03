package phapthuat.physics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RadiationCalcTest {
    @Test
    void dose() {
        assertEquals(420, RadiationCalc.absorbedJ(210, 10_000), 1e-9);
        assertEquals(6.0, RadiationCalc.doseGy(420, 70), 1e-9);
    }

    @Test
    void shielding() {
        assertEquals(0.5, RadiationCalc.attenuation(1, 0), 1e-12);
        assertEquals(0.1, RadiationCalc.attenuation(0, 1), 1e-12);
        assertEquals(0.025, RadiationCalc.attenuation(2, 1), 1e-12);
    }

    @Test
    void tiers() {
        assertEquals(DoseTier.NONE, RadiationCalc.tier(0.49));
        assertEquals(DoseTier.NAUSEA, RadiationCalc.tier(0.5));
        assertEquals(DoseTier.POISON, RadiationCalc.tier(2));
        assertEquals(DoseTier.WITHER, RadiationCalc.tier(6));
        assertEquals(DoseTier.SEVERE, RadiationCalc.tier(10));
    }

    @Test
    void scatter() {
        assertEquals(0.3, RadiationCalc.scatterDoseGy(420, 70), 1e-9);
    }
}
