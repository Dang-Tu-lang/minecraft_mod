package phapthuat.physics;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ImpulseCalcTest {
    @Test
    void zombieSpeed() {
        assertEquals(16.903, ImpulseCalc.speed(10_000, 70), 1e-3);
    }

    @Test
    void golemImpulse() {
        assertEquals(6.3246, ImpulseCalc.speed(10_000, 500), 1e-4);
        assertEquals(3162.3, ImpulseCalc.impulse(10_000, 500), 0.1);
    }

    @Test
    void recoilCappedAt40() {
        assertEquals(40, ImpulseCalc.recoilSpeed(3162.3), 1e-9);
        assertEquals(16.903, ImpulseCalc.recoilSpeed(1183.2), 1e-3);
    }

    @Test
    void speedCappedAt40() {
        assertEquals(40, ImpulseCalc.speed(1e9, 1), 1e-9);
    }
}
