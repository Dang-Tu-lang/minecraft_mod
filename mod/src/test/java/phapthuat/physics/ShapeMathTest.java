package phapthuat.physics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

class ShapeMathTest {
    @Test
    void sphereCounts() {
        assertEquals(List.of(7, 33, 123, 257, 515),
                IntStream.rangeClosed(1, 5).mapToObj(r -> ShapeMath.sphere(r).size()).toList());
    }

    @Test
    void wallAlongX() {
        var w = ShapeMath.wall(3, 2, true);
        assertEquals(6, w.size());
        assertTrue(w.stream().allMatch(o -> o.dx() == 0));
    }

    @Test
    void groundLine() {
        assertEquals(6, ShapeMath.groundLine(0, 0, 5, 0).size());
        assertEquals(new ShapeMath.Column(5, 3), ShapeMath.groundLine(0, 0, 5, 3).getLast());
    }

    @Test
    void split() {
        assertEquals(100, EnergySplit.perUnit(700, 7), 1e-9);
        assertEquals(0, EnergySplit.perUnit(700, 0), 1e-9);
    }

    /** Bán kính lẻ dùng đúng luật "mọi ô cách tâm ≤ r", không làm tròn r. */
    @Test
    void fractionalRadius() {
        assertEquals(81, ShapeMath.sphere(2.5).size());
    }
}
