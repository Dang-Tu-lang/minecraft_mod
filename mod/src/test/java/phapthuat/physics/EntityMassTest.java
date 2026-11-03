package phapthuat.physics;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class EntityMassTest {
    @Test
    void table() {
        assertEquals(70, EntityMass.kg("minecraft:zombie", 0.7));
        assertEquals(600, EntityMass.kg("minecraft:cow", 1.1));
        assertEquals(50, EntityMass.kg("minecraft:unknown_thing", 0.5), 1e-9);
    }
}
