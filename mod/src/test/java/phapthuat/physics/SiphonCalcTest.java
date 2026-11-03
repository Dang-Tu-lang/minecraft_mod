package phapthuat.physics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static phapthuat.physics.Materials.FIRE;
import static phapthuat.physics.Materials.LAVA;
import static phapthuat.physics.Materials.LIVING_CORE;
import static phapthuat.physics.Materials.WATER;

import org.junit.jupiter.api.Test;

class SiphonCalcTest {
    @Test
    void lavaFullDrain() {
        var r = SiphonCalc.siphon(LAVA, 2.7, 1200, 0, 5_000_000, 20);
        assertEquals(4_903_200, r.heatRemovedJ(), 1);
        assertTrue(r.capped());
        assertTrue(r.transformed());
        assertEquals(3_927_500, r.manaJoulesDelta(), 2_000);
    }

    @Test
    void lavaPartialLatentFirst() {
        var r = SiphonCalc.siphon(LAVA, 2.7, 1200, 0, 1_000_000, 20);
        assertFalse(r.transformed());
        assertEquals(1200, r.finalTempC(), 1e-9);
        assertEquals(1_000_000, r.phaseProgressJ(), 1);
        assertEquals(801_000, r.manaJoulesDelta(), 1_000);
    }

    @Test
    void lavaYieldsLessAsItCools() {
        var a = SiphonCalc.siphon(LAVA, 2.7, 1200, 1_000_000, 1_000_000, 20);
        assertEquals(916.0, a.finalTempC(), 0.5);
        var b = SiphonCalc.siphon(LAVA, 2.7, a.finalTempC(), a.phaseProgressJ(), 1_000_000, 20);
        assertTrue(b.manaJoulesDelta() < a.manaJoulesDelta());
    }

    @Test
    void waterFreezesCheaply() {
        var r = SiphonCalc.siphon(WATER, 1.0, 20, 0, 417_720, 20);
        assertTrue(r.transformed());
        assertEquals(-30_585, r.manaJoulesDelta(), 100);
    }

    @Test
    void fireCapped() {
        var r = SiphonCalc.siphon(FIRE, 0, 800, 0, 50_000, 20);
        assertEquals(20_000, r.heatRemovedJ(), 1e-9);
        assertTrue(r.capped());
        assertTrue(r.transformed());
        assertEquals(14_537, r.manaJoulesDelta(), 20);
    }

    @Test
    void livingCoreGivesLittle() {
        var r = SiphonCalc.siphon(LIVING_CORE, 1.0, 37, 0, 17_500, 20);
        assertEquals(32, r.finalTempC(), 1e-6);
        assertEquals(959, r.manaJoulesDelta(), 5);
    }

    @Test
    void undeadCoolingCosts() {
        var r = SiphonCalc.siphon(LIVING_CORE, 1.0, 20, 0, 17_500, 20);
        assertEquals(15, r.finalTempC(), 1e-6);
        assertEquals(-303.7, r.manaJoulesDelta(), 1);
    }

    @Test
    void coldTiers() {
        assertEquals(ColdTier.NONE, SiphonCalc.coldTier(1.9));
        assertEquals(ColdTier.SLOW, SiphonCalc.coldTier(2));
        assertEquals(ColdTier.FREEZE, SiphonCalc.coldTier(5));
        assertEquals(ColdTier.SEVERE, SiphonCalc.coldTier(9));
    }
}
