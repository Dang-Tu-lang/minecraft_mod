package phapthuat.spell;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** 5 ô phép của Pháp Trượng và ô đang chọn. Bất biến, dùng làm data component. Java thuần; codec ở {@link SpellCodecs}. */
public record WandSlots(List<Optional<InscribedSpell>> slots, int selected) {
    public static final int SIZE = 5;
    public static final WandSlots EMPTY = new WandSlots(Collections.nCopies(SIZE, Optional.empty()), 0);

    public WandSlots {
        List<Optional<InscribedSpell>> fixed = new ArrayList<>(slots.subList(0, Math.min(SIZE, slots.size())));
        while (fixed.size() < SIZE) fixed.add(Optional.empty());
        slots = List.copyOf(fixed);
        selected = Math.floorMod(selected, SIZE);
    }

    public Optional<InscribedSpell> get(int i) {
        return i >= 0 && i < SIZE ? slots.get(i) : Optional.empty();
    }

    public Optional<InscribedSpell> current() {
        return get(selected);
    }

    public WandSlots with(int i, InscribedSpell spell) {
        List<Optional<InscribedSpell>> copy = new ArrayList<>(slots);
        copy.set(i, Optional.of(spell));
        return new WandSlots(copy, selected);
    }

    public WandSlots cycle(int delta) {
        return new WandSlots(slots, selected + delta);
    }

    Map<String, InscribedSpell> asMap() {
        Map<String, InscribedSpell> map = new HashMap<>();
        for (int i = 0; i < SIZE; i++) {
            int index = i;
            slots.get(i).ifPresent(s -> map.put(Integer.toString(index), s));
        }
        return map;
    }

    static WandSlots fromMap(Map<String, InscribedSpell> map, int selected) {
        List<Optional<InscribedSpell>> list = new ArrayList<>(Collections.nCopies(SIZE, Optional.empty()));
        map.forEach((k, v) -> {
            try {
                int i = Integer.parseInt(k);
                if (i >= 0 && i < SIZE) list.set(i, Optional.of(v));
            } catch (NumberFormatException ignored) {
                // Khóa lạ trong dữ liệu lưu: bỏ qua.
            }
        });
        return new WandSlots(list, selected);
    }
}
