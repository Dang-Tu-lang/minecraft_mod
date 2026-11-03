package phapthuat.spell;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Danh sách phép, theo thứ tự đăng ký. */
public final class SpellRegistry {
    private static final Map<String, Spell> SPELLS = new LinkedHashMap<>();

    private SpellRegistry() {}

    public static synchronized void register(Spell spell) {
        SPELLS.put(spell.id(), spell);
    }

    public static synchronized Optional<Spell> get(String id) {
        return Optional.ofNullable(SPELLS.get(id));
    }

    public static synchronized List<Spell> all() {
        return new ArrayList<>(SPELLS.values());
    }
}
