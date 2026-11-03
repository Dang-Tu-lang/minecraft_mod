package phapthuat.physics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static phapthuat.physics.Materials.ICE;
import static phapthuat.physics.Materials.LIVING_CORE;
import static phapthuat.physics.Materials.SAND;
import static phapthuat.physics.Materials.STONE;
import static phapthuat.physics.Materials.WATER;
import static phapthuat.physics.Materials.WOOD;

import org.junit.jupiter.api.Test;

class HeatCalcTest {
    @Test
    void woodIgnitesAt333kJ() {
        assertEquals(HeatTier.IGNITE, HeatCalc.heat(WOOD, 0.7, 20, 0, 333_200).tier());
    }

    @Test
    void woodShortStaysWarm() {
        var r = HeatCalc.heat(WOOD, 0.7, 20, 0, 316_000);
        assertEquals(HeatTier.NONE, r.tier());
        assertEquals(285.5, r.finalTempC(), 0.1);
    }

    @Test
    void partialCastsAccumulate() {
        var a = HeatCalc.heat(WOOD, 0.7, 20, 0, 200_000);
        assertEquals(HeatTier.NONE, a.tier());
        assertEquals(HeatTier.IGNITE, HeatCalc.heat(WOOD, 0.7, a.finalTempC(), a.phaseProgressJ(), 133_200).tier());
    }

    @Test
    void stoneCracksAt25K() {
        assertEquals(HeatTier.CRACK, HeatCalc.heat(STONE, 2.7, 20, 0, 53_400).tier());
        assertEquals(HeatTier.NONE, HeatCalc.heat(STONE, 2.7, 20, 0, 53_000).tier());
    }

    @Test
    void stoneMelts() {
        assertEquals(HeatTier.MELT, HeatCalc.heat(STONE, 2.7, 20, 0, 3_629_000).tier());
        assertEquals(HeatTier.CRACK, HeatCalc.heat(STONE, 2.7, 20, 0, 3_628_000).tier());
    }

    @Test
    void stoneShortOfMeltIsNotBacklash() {
        var r = HeatCalc.heat(STONE, 2.7, 20, 0, 3_000_000);
        assertEquals(HeatTier.CRACK, r.tier());
        assertFalse(r.backlash());
    }

    @Test
    void iceTiers() {
        assertEquals(HeatTier.CRACK, HeatCalc.heat(ICE, 0.917, 0, 0, 4_500).tier());
        assertEquals(HeatTier.MELT, HeatCalc.heat(ICE, 0.917, 0, 0, 306_400).tier());
    }

    @Test
    void sandToGlass() {
        assertEquals(HeatTier.MELT, HeatCalc.heat(SAND, 1.6, 20, 0, 2_498_000).tier());
        assertEquals(HeatTier.NONE, HeatCalc.heat(SAND, 1.6, 20, 0, 2_497_000).tier());
    }

    @Test
    void waterVaporizes() {
        var r = HeatCalc.heat(WATER, 1.0, 20, 0, 2_592_000);
        assertEquals(HeatTier.VAPORIZE, r.tier());
        assertEquals(120, r.excessJ(), 1);
    }

    @Test
    void steamPower() {
        assertEquals(1.72, HeatCalc.steamBurst(1.0, 0).power(), 0.01);
        assertEquals(172_213, HeatCalc.steamBurst(1.0, 0).workJ(), 50);
        assertEquals(4.03, HeatCalc.steamBurst(1.0, 1_000_000).power(), 0.01);
        assertEquals(8.0, HeatCalc.steamBurst(2.0, 5_000_000).power(), 1e-9);
    }

    @Test
    void airBurst() {
        assertEquals(28_660, HeatCalc.airBurst(7, 500_000, 20).deltaPPa(), 50);
        assertTrue(HeatCalc.airBurst(7, 611_000, 20).deltaPPa() >= 35_000);
    }

    @Test
    void woodBacklashRadius() {
        assertEquals(1, HeatCalc.heat(WOOD, 0.7, 20, 0, 999_600).backlashRadius());
        assertEquals(2, HeatCalc.heat(WOOD, 0.7, 20, 0, 1_332_800).backlashRadius());
        assertEquals(4, HeatCalc.heat(WOOD, 0.7, 20, 0, 2_000_000).backlashRadius());
        assertFalse(HeatCalc.heat(WOOD, 0.7, 20, 0, 900_000).backlash());
    }

    @Test
    void livingCoreIgnites() {
        assertEquals(HeatTier.IGNITE, HeatCalc.heat(LIVING_CORE, 1.0, 37, 0, 1_270_600).tier());
    }

    @Test
    void burnSeconds() {
        assertEquals(3, HeatCalc.burnSeconds(0, 10_000));
        assertEquals(4, HeatCalc.burnSeconds(100_000, 10_000));
        assertEquals(10, HeatCalc.burnSeconds(1e7, 10_000));
    }

    /** Nước đã bị Thu Nhiệt rút 216 280 J nhiệt đông đặc ở 0°C (tiến độ âm) phải tan lại trước khi đun. */
    @Test
    void frozenProgressMustRemeltFirst() {
        assertEquals(HeatTier.NONE, HeatCalc.heat(WATER, 1, 0, -216_280, 2_500_000).tier());
        assertEquals(HeatTier.VAPORIZE, HeatCalc.heat(WATER, 1, 0, -216_280, 2_892_000).tier());
        var partial = HeatCalc.heat(WATER, 1, 0, -216_280, 100_000);
        assertEquals(0, partial.finalTempC(), 1e-9);
        assertEquals(-116_280, partial.phaseProgressJ(), 1e-6);
    }
}
