package phapthuat.thermal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class PlacedLightsTest {
    @Test
    void expiredReturned() {
        var t = new LightTable();
        t.add(1L, 100);
        t.add(2L, 300);
        assertEquals(List.of(1L), t.expired(200));
    }

    @Test
    void removeForgetsEntry() {
        var t = new LightTable();
        t.add(1L, 100);
        t.remove(1L);
        assertTrue(t.expired(1_000).isEmpty());
    }
}
