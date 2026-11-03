package phapthuat.physics;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class LiftCalcTest {
    @Test
    void liftStone() {
        assertEquals(132_435, LiftCalc.energy(2700, 5), 1e-6);
        assertEquals(6_867, LiftCalc.energy(70, 10), 1e-6);
    }

    @Test
    void impact() {
        assertEquals(132.435, LiftCalc.impactDamageHp(2700, 5), 1e-6);
    }
}
