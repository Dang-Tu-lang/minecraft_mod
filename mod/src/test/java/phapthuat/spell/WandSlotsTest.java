package phapthuat.spell;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;

class WandSlotsTest {
    @Test
    void emptyHasFive() {
        assertEquals(5, WandSlots.EMPTY.slots().size());
        assertTrue(WandSlots.EMPTY.get(0).isEmpty());
    }

    @Test
    void cycleWraps() {
        assertEquals(4, WandSlots.EMPTY.cycle(-1).selected());
        assertEquals(0, WandSlots.EMPTY.cycle(5).selected());
    }

    @Test
    void withReplaces() {
        var s = new InscribedSpell("gia_nhiet", Shape.DIEM, Map.of("energy", 33.3));
        assertEquals(s, WandSlots.EMPTY.with(2, s).get(2).orElseThrow());
    }
}
