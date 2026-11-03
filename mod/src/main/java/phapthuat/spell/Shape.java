package phapthuat.spell;

import java.util.List;
import java.util.Locale;

/** Bảy thao tác: quyết định đơn vị nào bị phép tác động (spec mục 6.5). Java thuần; codec ở {@link SpellCodecs}. */
public enum Shape {
    DIEM(List.of()),
    TIA(List.of(new SpellParam("length", "phapthuat.unit.m", 1, 16, 8))),
    DUONG(List.of()),
    CAU(List.of(new SpellParam("radius", "phapthuat.unit.m", 1, 5, 2))),
    TUONG(List.of(new SpellParam("width", "phapthuat.unit.blocks", 1, 7, 3),
            new SpellParam("height", "phapthuat.unit.blocks", 1, 5, 2))),
    DAN(List.of(new SpellParam("speed", "phapthuat.unit.m_per_s", 5, 100, 20))),
    BAN_THAN(List.of());

    private final List<SpellParam> params;

    Shape(List<SpellParam> params) {
        this.params = params;
    }

    public List<SpellParam> params() {
        return params;
    }

    public String serializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public String langKey() {
        return "phapthuat.shape." + serializedName();
    }
}
