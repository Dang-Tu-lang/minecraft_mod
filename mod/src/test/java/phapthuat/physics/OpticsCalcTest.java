package phapthuat.physics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class OpticsCalcTest {
    @Test
    void eye() {
        assertEquals(1.0144, OpticsCalc.luminosity(555), 1e-3);
        assertEquals(0.0343, OpticsCalc.luminosity(450), 1e-3);
        assertEquals(0.0959, OpticsCalc.luminosity(650), 1e-3);
    }

    @Test
    void levels() {
        assertEquals(6, OpticsCalc.lightLevel(1));
        assertEquals(11, OpticsCalc.lightLevel(23.4));
        assertEquals(14, OpticsCalc.lightLevel(100));
        assertEquals(15, OpticsCalc.lightLevel(692.8));
        assertEquals(0, OpticsCalc.lightLevel(0.01));
        assertEquals(0, OpticsCalc.lightLevel(0));
    }

    @Test
    void intensities() {
        assertEquals(10.0, OpticsCalc.intensity(3142, 5), 0.01);
        assertEquals(31_831, OpticsCalc.laserIntensity(10), 1);
    }

    @Test
    void bands() {
        assertTrue(OpticsCalc.isVisible(390) && OpticsCalc.isUv(390));
        assertTrue(OpticsCalc.isUvc(250));
        assertTrue(OpticsCalc.isInfrared(1000));
        assertFalse(OpticsCalc.isVisible(1000));
    }
}
