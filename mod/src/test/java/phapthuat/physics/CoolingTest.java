package phapthuat.physics;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class CoolingTest {
    @Test
    void halfLife() {
        assertEquals(160, Cooling.relax(300, 20, 20, 20), 1e-9);
        assertEquals(90, Cooling.relax(300, 20, 40, 20), 1e-9);
        assertEquals(300, Cooling.relax(300, 20, 0, 20), 1e-9);
    }
}
