package phapthuat.physics;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

class IronContentTest {
    @Test
    void ironArmor() {
        assertEquals(24, IronContent.kg("minecraft:zombie", List.of("minecraft:iron_helmet",
                "minecraft:iron_chestplate", "minecraft:iron_leggings", "minecraft:iron_boots")));
        assertEquals(500, IronContent.kg("minecraft:iron_golem", List.of()));
        assertEquals(5, IronContent.kg("minecraft:minecart", List.of()));
        assertEquals(9, IronContent.itemKg("minecraft:iron_block", 1));
    }

    @Test
    void items() {
        assertEquals(3, IronContent.itemKg("minecraft:iron_ingot", 3), 1e-9);
        assertEquals(1, IronContent.itemKg("minecraft:iron_nugget", 9), 1e-9);
        assertEquals(0, IronContent.itemKg("minecraft:iron_sword", 1), 1e-9);
        assertEquals(0, IronContent.kg("minecraft:zombie", List.of("minecraft:diamond_helmet")), 1e-9);
    }
}
