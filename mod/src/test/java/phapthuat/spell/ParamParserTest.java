package phapthuat.spell;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class ParamParserTest {
    @Test
    void choiceClampsToIndex() {
        var p = SpellParam.choice("direction", List.of("a", "b"), 0);
        assertEquals(1, p.clamp(7.6));
        assertEquals(0, p.clamp(-3));
        assertEquals(1, p.clamp(0.6));
    }

    @Test
    void parseVietnameseComma() {
        assertEquals(33.3, ParamParser.parse(" 33,3 ").getAsDouble(), 1e-9);
        assertEquals(33.3, ParamParser.parse("33.3").getAsDouble(), 1e-9);
    }

    @Test
    void parseRejectsJunk() {
        for (String s : List.of("", "abc", "NaN", "Infinity", "1,2,3")) {
            assertTrue(ParamParser.parse(s).isEmpty(), s);
        }
    }

    @Test
    void clamp() {
        var p = new SpellParam("energy", "phapthuat.unit.mana", 0, 1000, 10);
        assertEquals(1000, p.clamp(1e9));
        assertEquals(0, p.clamp(-5));
    }
}
