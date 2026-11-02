package phapthuat.physics;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class BallisticsTest {
    @Test
    void range45() {
        assertEquals(40.77, Ballistics.range(20, Math.PI / 4), 0.01);
    }

    @Test
    void gravityPerSecond() {
        double vy = 0;
        for (int i = 0; i < 20; i++) vy = Ballistics.vyAfterTick(vy);
        assertEquals(-9.81, vy, 1e-9);
    }
}
