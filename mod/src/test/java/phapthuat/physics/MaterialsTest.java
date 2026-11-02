package phapthuat.physics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MaterialsTest {
    @Test
    void woodData() {
        assertEquals(700, Materials.WOOD.density());
        assertEquals(1700, Materials.WOOD.specificHeat());
        assertEquals(300, Materials.WOOD.ignitionC());
    }

    @Test
    void coreMass() {
        assertEquals(0.7, Materials.WOOD.coreMassKg(), 1e-9);
        assertEquals(2.7, Materials.STONE.coreMassKg(), 1e-9);
    }

    @Test
    void crackDeltas() {
        assertEquals(25.0, Materials.STONE.crackDeltaK(), 1e-9);
        assertEquals(47.619, Materials.GLASS.crackDeltaK(), 1e-3);
        assertEquals(2.222, Materials.ICE.crackDeltaK(), 1e-3);
        assertTrue(Double.isNaN(Materials.WOOD.crackDeltaK()));
    }

    @Test
    void estimatesFlagged() {
        assertTrue(Materials.LEAVES.estimated());
        assertTrue(Materials.LIVING_CORE.estimated());
        assertFalse(Materials.WOOD.estimated());
    }

    @Test
    void sevenElements() {
        assertEquals(7, Element.values().length);
    }
}
